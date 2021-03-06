package org.openbase.bco.dal.control.action;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.openbase.bco.dal.control.layer.unit.AbstractUnitController;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.action.SchedulableAction;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ActionImpl implements SchedulableAction {

    /**
     * Timeout how long it is waited on execution failure until a rescheduling process is triggered.
     */
    private static final long EXECUTION_FAILURE_TIMEOUT = TimeUnit.SECONDS.toMillis(15);
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionImpl.class);

    protected final AbstractUnitController<?, ?> unit;
    private final SyncObject executionSync = new SyncObject(ActionImpl.class);
    protected ActionDescription.Builder actionDescriptionBuilder;
    private Message serviceState;
    private ServiceDescription serviceDescription;
    private Future<ActionDescription> actionTask;

    public ActionImpl(final ActionDescription actionDescription, final AbstractUnitController<?, ?> unit) throws InstantiationException {
        try {
            this.unit = unit;
            this.init(actionDescription);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public ActionImpl(final ActionDescription actionDescription, final AbstractUnitController<?, ?> unit, final boolean setInitialState) throws InstantiationException {
        try {
            this.unit = unit;
            this.init(actionDescription, setInitialState);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }


    @Override
    public void init(final ActionDescription actionDescription) throws InitializationException {
        init(actionDescription, true);
    }

    private void init(final ActionDescription actionDescription, final boolean setInitialState) throws InitializationException {
        LOGGER.trace("================================================================================");
        try {
            actionDescriptionBuilder = actionDescription.toBuilder();

            // verify and prepare action description
            serviceState = ActionDescriptionProcessor.verifyActionDescription(actionDescriptionBuilder, unit, true);
            // initially set last extension to creation time
            actionDescriptionBuilder.setLastExtensionTimestamp(actionDescriptionBuilder.getTimestamp());
            // set actions description as responsible in service state
            final FieldDescriptor responsibleActionField = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
            serviceState = serviceState.toBuilder().setField(responsibleActionField, actionDescriptionBuilder.build()).build();

            // since its an action it has to be an operation service pattern
            serviceDescription = ServiceDescription.newBuilder().setServiceType(actionDescriptionBuilder.getServiceStateDescription().getServiceType()).setPattern(ServicePattern.OPERATION).build();

            // mark action as initialized.
            if (setInitialState) {
                updateActionState(State.INITIALIZED);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * checks if the execution task is finished.
     *
     * @return true if the execution task is finish otherwise true.
     */
    private boolean isExecutionTaskFinish() {
        synchronized (executionSync) {
            return actionTask == null || actionTask.isDone();
        }
    }

    /**
     * checks if this action is currently executing or the execution task is not done yet.
     *
     * @return true if execution is still in progress.
     */
    private boolean isExecuting() {
        synchronized (executionSync) {
            return !isExecutionTaskFinish() || getActionState() == State.EXECUTING;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> execute() {
        synchronized (executionSync) {
            if (isExecuting()) {
                return actionTask;
            }

            actionTask = GlobalCachedExecutorService.submit(() -> {
                try {
                    // Initiate
                    updateActionState(ActionState.State.INITIATING);

                    try {
                        while (!Thread.interrupted() && !actionTask.isCancelled()) {
                            try {
                                boolean hasOperationService = false;
                                for (ServiceDescription description : unit.getUnitTemplate().getServiceDescriptionList()) {
                                    if (description.getServiceType() == serviceDescription.getServiceType() && description.getPattern() == ServicePattern.OPERATION) {
                                        hasOperationService = true;
                                        break;
                                    }
                                }

                                if (isDone()) {
                                    LOGGER.error(ActionImpl.this + " was done before executed!");
                                    return getActionDescription();
                                }

                                // Execute
                                updateActionState(ActionState.State.EXECUTING);

                                // only update requested state if it is an operation state, else throw an exception if not in provider control mode
                                if (!hasOperationService) {
                                    if (!JPService.getValue(JPProviderControlMode.class, false)) {
                                        throw new NotAvailableException("Operation service " + serviceDescription.getServiceType().name() + " of unit " + unit);
                                    }
                                } else {
                                    setRequestedState();
                                }

                                LOGGER.debug("Wait for execution...");
                                unit.performOperationService(serviceState, serviceDescription.getServiceType()).get(EXECUTION_FAILURE_TIMEOUT, TimeUnit.SECONDS);
                                LOGGER.debug("Execution finished!");

                                // action can be finished if not done yet and time has expired or execution time was never required.
                                if (!isDone() && (isExpired() || getExecutionTimePeriod(TimeUnit.MICROSECONDS) == 0)) {
                                    updateActionState(State.FINISHED);
                                }
                                break;
                            } catch (CouldNotPerformException | ExecutionException | RuntimeException ex) {
                                if(!isDone()) {
                                    updateActionState(ActionState.State.EXECUTION_FAILED);
                                }
                                ExceptionPrinter.printHistory("Action execution failed", ex, LOGGER, LogLevel.WARN);
                                Thread.sleep(EXECUTION_FAILURE_TIMEOUT);
                            }
                        }
                        return getActionDescription();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                } finally {
                    synchronized (executionSync) {
                        actionTask = null;
                        executionSync.notifyAll();
                    }
                }
            });
            return actionTask;
        }
    }

    /**
     * Method blocks until the action reaches a terminated state.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public void waitUntilDone() throws InterruptedException {
        synchronized (executionSync) {
            while (!isDone()) {
                executionSync.wait();
            }
        }
    }

    /**
     * Method blocks until the action reaches a terminated state or the timeout is reached.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public void waitUntilDone(final long timeout) throws InterruptedException {
        synchronized (executionSync) {
            while (!isDone()) {
                executionSync.wait(timeout);
            }
        }
    }

    private void waitForExecutionTaskFinalization(final long timeout) throws InterruptedException {
        synchronized (executionSync) {
            while (!isExecutionTaskFinish()) {
                executionSync.wait(timeout);
            }
        }
    }

    private void waitForExecutionTaskFinalization() throws InterruptedException {
        synchronized (executionSync) {
            while (!isExecutionTaskFinish()) {
                executionSync.wait();
            }
        }
    }

    private void setRequestedState() throws CouldNotPerformException {
        try (ClosableDataBuilder dataBuilder = unit.getDataBuilder(this)) {
            // set the new service attribute as requested state in the unit data builder
            Services.invokeServiceMethod(serviceDescription.getServiceType(), serviceDescription.getPattern(), ServiceTempus.REQUESTED, dataBuilder.getInternalBuilder(), serviceState);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     */
    @Override
    public ActionDescription getActionDescription() {
        return actionDescriptionBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> cancel() {
        // if action not executing, set to canceled if not already done and finish
        if (!isExecuting()) {
            if (!isDone()) {
                updateActionState(State.CANCELED);
            }
            return FutureProcessor.completedFuture(getActionDescription());
        }

        // action is currently executing, so set to canceling, wait till its done, set to canceled and trigger reschedule
        updateActionState(State.CANCELING);
        return GlobalCachedExecutorService.submit(() -> {
            if (!isExecutionTaskFinish()) {
                actionTask.cancel(true);
                waitForExecutionTaskFinalization();
            }
            updateActionState(State.CANCELED);
            unit.reschedule();
            return actionDescriptionBuilder.build();
        });
    }

    @Override
    public Future<ActionDescription> abort(boolean forceReject) {
        if (!isExecuting()) {
            // this should never happen since a task should be executing before it is aborted
            LOGGER.error("Aborted action was not executing before");
            return FutureProcessor.completedFuture(getActionDescription());
        }

        updateActionState(State.ABORTING);
        return GlobalCachedExecutorService.submit(() -> {
            if (!isExecutionTaskFinish()) {
                actionTask.cancel(true);
                waitForExecutionTaskFinalization();
            }

            // if action is interruptible it can be scheduled otherwise it is rejected
            if (!forceReject && getActionDescription().getInterruptible() && getActionDescription().getSchedulable()) {
                updateActionState(State.SCHEDULED);
            } else {
                updateActionState(State.REJECTED);
            }

            // rescheduling is not necessary because aborting is only done when rescheduling
            return actionDescriptionBuilder.build();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void schedule() {
        updateActionState(State.SCHEDULED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reject() {
        if (actionTask != null && !actionTask.isDone()) {
            actionTask.cancel(true);
        }
        if (isExecuting()) {
            abort(true);
        }
        updateActionState(State.REJECTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish() {

        // finalize if still running
        if (!isExecutionTaskFinish()) {
            // try a smooth finishing if not already failed.
            actionTask.cancel(getActionState() == State.EXECUTION_FAILED);

            try {
                waitForExecutionTaskFinalization(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // check if finished yet
            if (!isExecutionTaskFinish()) {
                LOGGER.warn("Execution of " + this + " can not be finished smoothly! Force finalization...");
                actionTask.cancel(true);
            }

            try {
                waitForExecutionTaskFinalization(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // check if finished after force
            if (!isExecutionTaskFinish()) {
                LOGGER.error("Can not finalize " + this + " it seems the execution has stuck.");
            }
        }

        // if not already finished than we force the state.
        if (!isDone()) {
            updateActionState(State.FINISHED);
        }
    }

    private void updateActionState(ActionState.State state) {
        synchronized (executionSync) {

            // duplicated termination in same state should be ok, but than skip the update.
            if (getActionDescription().getActionState().getValue() == state && isDone()) {
                return;
            }

            // check duplicated termination
            if (isDone()) {
                LOGGER.warn("Can not change the state to {} of an already {} action!", state.name(), actionDescriptionBuilder.getActionState().getValue().name().toLowerCase());
                StackTracePrinter.printStackTrace(LOGGER, LogLevel.WARN);
                return;
            }

            // inform about execution
            if (state == State.EXECUTING) {
                LOGGER.info(MultiLanguageTextProcessor.getBestMatch(actionDescriptionBuilder.getDescription(), this + " State[" + state.name() + "]"));
            }

            // print update in debug mode
            if (JPService.debugMode()) {
                LOGGER.info(this + " State[" + state.name() + "]" + MultiLanguageTextProcessor.getBestMatch(getActionDescription().getDescription(), "?"));
                //StackTracePrinter.printStackTrace(LOGGER, LogLevel.INFO);
            }

            // perform the update
            actionDescriptionBuilder.setActionState(ActionState.newBuilder().setValue(state));
            try {
                ServiceStateProcessor.updateLatestValueOccurrence(state.getValueDescriptor(), TimestampProcessor.getCurrentTimestamp(), actionDescriptionBuilder.getActionStateBuilder());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            // setup termination time if needed
            if(isDone()) {
                actionDescriptionBuilder.setTerminationTimestamp(TimestampProcessor.getCurrentTimestamp());
            }

            executionSync.notifyAll();
        }

        // make sure that state changes to finishing states, scheduled and executing always trigger a notification
        if (isNotifiedActionState(state)) {
            unit.notifyScheduledActionList();
        }
    }

    @Override
    public Future<ActionDescription> extend() {
        actionDescriptionBuilder.setLastExtensionTimestamp(TimestampProcessor.getCurrentTimestamp());
        return FutureProcessor.completedFuture(actionDescriptionBuilder.build());
    }

    @Override
    public String toString() {
        return Action.toString(this);
    }
}
