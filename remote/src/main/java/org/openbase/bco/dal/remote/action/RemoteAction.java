package org.openbase.bco.dal.remote.action;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class RemoteAction implements Action {

    public final static long AUTO_EXTENSION_INTERVAL = Action.MAX_EXECUTION_TIME_PERIOD / 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAction.class);
    private final SyncObject executionSync = new SyncObject("ExecutionSync");
    private final SyncObject extensionSync = new SyncObject("ExtensionSync");
    private final ActionParameter.Builder actionParameterBuilder;
    private ActionDescription actionDescription;
    private UnitRemote<?> targetUnit;
    private Future<ActionDescription> futureObservationTask;
    private ScheduledFuture<?> autoExtensionTask;
    private final ObservableImpl<RemoteAction, ActionDescription> actionDescriptionObservable;

    private final AuthToken authToken;

    private final Observer unitObserver = (source, data) -> {

        // check if initial actionDescription is available
        if (RemoteAction.this.actionDescription == null) {
            return;
        }

        try {
            updateActionDescription((Collection<ActionDescription>) ProtoBufFieldProcessor.getRepeatedFieldList("action", (Message) data));
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Incoming DataType[" + data.getClass().getSimpleName() + "] does not provide an action list!", ex, LOGGER, LogLevel.WARN);
        }
    };
    private final List<RemoteAction> impactedRemoteActions = new ArrayList<>();

    private Callable<Boolean> autoExtendCheckCallback;


    public RemoteAction(final Future<ActionDescription> actionFuture) {
        this(actionFuture, null, null);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter) throws InstantiationException, InterruptedException {
        this(executorUnit, actionParameter, null, null);
    }

    public RemoteAction(final ActionDescription actionDescription) {
        this(ActionDescriptionProcessor.generateActionReference(actionDescription), null, null);
    }

    public RemoteAction(final ActionReference actionReference) {
        this(actionReference, null, null);
    }

    public RemoteAction(final Future<ActionDescription> actionFuture, Callable<Boolean> autoExtendCheckCallback) {
        this(actionFuture, null, autoExtendCheckCallback);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException, InterruptedException {
        this(executorUnit, actionParameter, null, autoExtendCheckCallback);
    }

    public RemoteAction(final ActionDescription actionDescription, Callable<Boolean> autoExtendCheckCallback) {
        this(ActionDescriptionProcessor.generateActionReference(actionDescription), null, autoExtendCheckCallback);
    }

    public RemoteAction(final ActionReference actionReference, Callable<Boolean> autoExtendCheckCallback) {
        this(actionReference, null, autoExtendCheckCallback);
    }

    public RemoteAction(final Future<ActionDescription> actionFuture, final AuthToken authToken) {
        this(actionFuture, authToken, null);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter, final AuthToken authToken) throws InstantiationException, InterruptedException {
        this(executorUnit, actionParameter, authToken, null);
    }

    public RemoteAction(final ActionDescription actionDescription, final AuthToken authToken) {
        this(ActionDescriptionProcessor.generateActionReference(actionDescription), authToken, null);
    }

    public RemoteAction(final ActionReference actionReference, final AuthToken authToken) {
        this(actionReference, authToken, null);
    }

    public RemoteAction(final Future<ActionDescription> actionFuture, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) {
        this.actionParameterBuilder = null;
        this.actionDescriptionObservable = new ObservableImpl<>();
        this.autoExtendCheckCallback = autoExtendCheckCallback;
        this.authToken = authToken;
        this.initFutureObservationTask(actionFuture);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException, InterruptedException {
        this.actionParameterBuilder = actionParameter.toBuilder();
        this.autoExtendCheckCallback = autoExtendCheckCallback;
        this.authToken = authToken;
        this.actionDescriptionObservable = new ObservableImpl<>(this);
        try {
            // setup initiator
            this.actionParameterBuilder.getActionInitiatorBuilder().setInitiatorId(executorUnit.getId());

            // prepare target unit
            this.targetUnit = Units.getUnit(actionParameter.getServiceStateDescription().getUnitId(), false);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public RemoteAction(final ActionDescription actionDescription, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) {
        this(ActionDescriptionProcessor.generateActionReference(actionDescription), authToken, autoExtendCheckCallback);
    }

    public RemoteAction(final ActionReference actionReference, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) {
        this.actionParameterBuilder = null;
        this.actionDescriptionObservable = new ObservableImpl<>();
        this.autoExtendCheckCallback = autoExtendCheckCallback;
        this.authToken = authToken;
        this.initFutureObservationTask(actionReference);
    }

    public Future<ActionDescription> execute(final ActionDescription causeActionDescription) {
        try {
            // check if action remote was instantiated via task future.
            if (actionParameterBuilder == null) {
                throw new NotAvailableException("ActionParameter");
            }

            if (isRunning()) {
                throw new InvalidStateException("Action is still running and can not be executed twice!");
            }

            synchronized (executionSync) {
                if (causeActionDescription == null) {
                    actionParameterBuilder.clearCause();
                } else {
                    actionParameterBuilder.setCause(causeActionDescription);
                }
                synchronized (executionSync) {
                    return initFutureObservationTask(targetUnit.applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameterBuilder).build()));
                }
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new CouldNotPerformException("Could not execute " + this + "!", ex));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> execute() {
        return execute(null);
    }

    private Future<ActionDescription> initFutureObservationTask(final Future<ActionDescription> future) {
        futureObservationTask = GlobalCachedExecutorService.submit(() -> {
            try {
                final ActionDescription actionDescription = future.get();
                synchronized (executionSync) {
                    RemoteAction.this.actionDescription = actionDescription;

                    // configure target unit if needed. This is the case if this remote action was instantiated via a future object.
                    if (targetUnit == null) {
                        targetUnit = Units.getUnit(actionDescription.getServiceStateDescription().getUnitId(), false);
                    }

                    if (actionDescription.getIntermediary()) {
                        // observe impact actions and register callback if available to support action auto extension.
                        for (ActionReference actionReference : actionDescription.getActionImpactList()) {
                            RemoteAction remoteAction = new RemoteAction(actionReference, authToken, autoExtendCheckCallback);
                            remoteAction.addActionDescriptionObserver((source, observable) -> actionDescriptionObservable.notifyObservers(source, observable));
                            impactedRemoteActions.add(remoteAction);
                        }
                    } else {
                        setupActionObservation();
                    }
                }
                return actionDescription;
            } catch (InterruptedException ex) {
                // this is useful to cancel actions through cancelling the execute task of a remote actions
                // it allows to easily handle remote actions through the provided future because remote actions pools do not allow to access them
                // it is used to cancel optional actions from scenes
                targetUnit.cancelAction(actionDescription, authToken);
                throw ex;
            } catch (CancellationException ex) {
                // in case the action is canceled, this is done via the futureObservationTask which than causes this cancellation exception.
                // But in this case we need to cancel the initial future as well.
                future.cancel(true);
                throw new ExecutionException(ex);
            } catch (ExecutionException ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable("Could not observe " + this + "!", ex, LOGGER);
            }
        });
        return futureObservationTask;
    }

    private Future<ActionDescription> initFutureObservationTask(final ActionReference actionReference) {
        futureObservationTask = GlobalCachedExecutorService.submit(() -> {
            try {
                synchronized (executionSync) {
                    // Note: this action can never be intermediary because else it cannot be retrieved from an action reference
                    // therefore, the difference between this initialization and the one above
                    targetUnit = Units.getUnit(actionReference.getServiceStateDescription().getUnitId(), true);

                    // resolve action description via the action reference.
                    for (final ActionDescription actionDescription : targetUnit.getActionList()) {
                        if (actionDescription.getId().equals(actionReference.getActionId())) {
                            RemoteAction.this.actionDescription = actionDescription;
                        }
                    }

                    if (RemoteAction.this.actionDescription == null) {
                        throw ExceptionPrinter.printHistoryAndReturnThrowable("Initial action sync failed!", new NotAvailableException("ActionDescription[" + actionReference.getActionId() + "] on unit[" + targetUnit + "]"), LOGGER);
                    }
                    setupActionObservation();
                }
                return actionDescription;
            } catch (InterruptedException ex) {
                throw ex;
            }
        });
        return futureObservationTask;
    }

    private void setupActionObservation() throws CouldNotPerformException {

        // register action update observation
        targetUnit.addDataObserver(ServiceTempus.UNKNOWN, unitObserver);

        synchronized (executionSync) {
            executionSync.notifyAll();
        }

        // because future can already be outdated but the update not received because
        // the action id was not yet available we need to trigger an manual update.
        updateActionDescription(targetUnit.getActionList());

        // setup auto extension if needed
        setupAutoExtension(getActionDescription());
    }

    private void setupAutoExtension(final ActionDescription actionDescription) {

        // check if auto extension was requested, otherwise return.
        if (autoExtendCheckCallback == null) {
            return;
        }

        // check if auto extension is required, otherwise return.
        if (TimeUnit.MICROSECONDS.toMillis(actionDescription.getExecutionTimePeriod()) <= Action.MAX_EXECUTION_TIME_PERIOD) {
            return;
        }

        try {
            assert autoExtensionTask == null;
            autoExtensionTask = GlobalScheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    // auto extend if not done
                    if (!isDone() && autoExtendCheckCallback.call()) {
                        // update last extension time and reapply
                        extend().get();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory("Could not auto extend " + RemoteAction.this.toString(), ex, LOGGER);
                }
            }, AUTO_EXTENSION_INTERVAL, AUTO_EXTENSION_INTERVAL, TimeUnit.MILLISECONDS);

            final Observer<RemoteAction, ActionDescription> observer = new Observer<RemoteAction, ActionDescription>() {
                @Override
                public void update(RemoteAction remote, ActionDescription description) {
                    if (remote.isDone()) {
                        RemoteAction.this.removeActionDescriptionObserver(this);
                        if (autoExtensionTask != null) {
                            autoExtensionTask.cancel(false);
                        }
                    }
                }
            };
            addActionDescriptionObserver(observer);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not auto extend " + RemoteAction.this.toString(), ex, LOGGER);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public ActionDescription getActionDescription() throws NotAvailableException {
        if (actionDescription == null) {
            throw new NotAvailableException(this.getClass().getSimpleName(), "ActionDescription");
        }
        return actionDescription;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return (actionParameterBuilder != null || futureObservationTask != null) && actionDescription != null && Action.super.isValid();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        if (isValid() && futureObservationTask != null) {
            if (!futureObservationTask.isDone()) {
                return true;
            }

            try {
                if (getActionDescription().getIntermediary()) {
                    for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                        if (!impactedRemoteAction.isRunning()) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return Action.super.isRunning();
                }
            } catch (NotAvailableException ex) {
                return false;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        try {
            if (getActionDescription().getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    if (!impactedRemoteAction.isDone()) {
                        return false;
                    }
                }
                return true;
            } else {
                return Action.super.isDone();
            }
        } catch (NotAvailableException ex) {
            return Action.super.isDone();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> cancel() {
        Future future = null;
        try {
            synchronized (executionSync) {
                if (futureObservationTask == null) {
                    future = FutureProcessor.canceledFuture(new InvalidStateException(this + " has never been executed!"));
                    return future;
                }

                if (!futureObservationTask.isDone()) {
                    // task is not yet done, so cancel it which will result in cancelling the action on the controller
                    futureObservationTask.cancel(true);

                    future = targetUnit.cancelAction(getActionDescription(), authToken);
                } else {
                    if (getActionDescription().getIntermediary()) {
                        // cancel all impacts of this actions and return the current action description
                        future = FutureProcessor.allOf(impactedRemoteActions, input -> getActionDescription(), remoteAction -> {
                            remoteAction.waitForSubmission();
                            return remoteAction.cancel();
                        });
                    } else {
                        // cancel the action on the controller
                        future = targetUnit.cancelAction(getActionDescription(), authToken);
                    }
                }
                return future;
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        } finally {
            // cleanup when cancellation is done
            if (future == null || future.isDone()) {
                cleanup();
            } else {
                // otherwise create cleanup task
                final Future passthroughFuture = future;
                GlobalCachedExecutorService.submit(() -> {
                    try {
                        passthroughFuture.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        // just continue with cleanup
                        // controller will cancel the action anyway when not extended any longer by this remote.
                    }
                    cleanup();
                });
            }
        }
    }

    private void cleanup() {
        // cleanup synchronisation and observation tasks
        actionDescriptionObservable.shutdown();
        if (autoExtensionTask != null) {
            autoExtensionTask.cancel(false);
        }
        targetUnit.removeDataObserver(ServiceTempus.UNKNOWN, unitObserver);

        // check if already done, otherwise force cancellation
        if (!isDone()) {
            final ActionDescription.Builder builder;
            if (actionDescription == null) {
                builder = ActionDescription.newBuilder();
            } else {
                builder = actionDescription.toBuilder();
            }
            // force termination
            builder.getActionStateBuilder().setValue(State.UNKNOWN);
            actionDescription = builder.build();
        }
    }


    private void updateActionDescription(final Collection<ActionDescription> actionDescriptions) {
        if (actionDescriptions == null) {
            LOGGER.warn("Update skipped because no action descriptions passed!");
            return;
        }

        if (RemoteAction.this.actionDescription == null) {
            LOGGER.warn("Update skipped because no action description is not available!");
            return;
        }

        // update action description and notify
        for (ActionDescription actionDescription : actionDescriptions) {

            if (actionDescription.getId().equals(RemoteAction.this.actionDescription.getId())) {

                synchronized (executionSync) {
                    final boolean actionExtended = RemoteAction.this.actionDescription.getLastExtensionTimestamp().getTime() < actionDescription.getLastExtensionTimestamp().getTime();
                    RemoteAction.this.actionDescription = actionDescription;

                    // cleanup observation if action is done.
                    if (isDone()) {
                        cleanup();
                    }

                    // notify about update
                    executionSync.notifyAll();

                    // inform about action extension
                    if(actionExtended) {
                        synchronized (extensionSync) {
                            extensionSync.notifyAll();
                        }
                    }
                }

                try {
                    actionDescriptionObservable.notifyObservers(this, actionDescription);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not notify all observers!", ex, LOGGER);
                    return;
                }
                return;
            }
        }

        // when action not listed and we are expired anyway, than we can cleanup
        if (isExpired()) {
            cleanup();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void waitUntilDone() throws CouldNotPerformException, InterruptedException {
        waitForSubmission();

        try {
            if (getActionDescription().getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitUntilDone();
                }
                return;
            }
        } catch (NotAvailableException ex) {
            // if the action description is not available, than we just continue and wait for it.
        }

        synchronized (executionSync) {
            // wait until done
            while (actionDescription == null || isRunning() || !isDone()) {
                executionSync.wait();
            }
        }
    }

    /**
     * Wait until this action reaches a provided action state. It is only possible to wait for states which will
     * certainly be notified (see {@link #isNotifiedActionState(State)}).
     *
     * @param actionState the state on which is waited.
     *
     * @throws CouldNotPerformException if the action was not yet executed, the provided action state is not certainly notified
     *                                  or the provided state cannot be reached anymore.
     * @throws InterruptedException     if the thread was externally interrupted.
     */
    public void waitForActionState(final ActionState.State actionState) throws CouldNotPerformException, InterruptedException {
        if (!isNotifiedActionState(actionState)) {
            throw new CouldNotPerformException("Cannot wait for state[" + actionState + "] because it is not always notified");
        }
        waitForSubmission();

        try {
            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForActionState(actionState);
                }
                return;
            }

        } catch (NotAvailableException ex) {
            // if the action description is not available, than we just continue and wait for it.
        }

        synchronized (executionSync) {
            // wait until state is reached
            while (actionDescription == null || actionDescription.getActionState().getValue() != actionState) {
                // Waiting makes no sense if the action is done but the state is still not reached.
                if (actionDescription != null && isDone()) {
                    throw new CouldNotPerformException("Stop waiting because state[" + actionState.name() + "] cannot be reached from state[" + actionDescription.getActionState().getValue().name() + "]");
                }
                executionSync.wait();
            }
        }
    }

    public void waitForExecution() throws CouldNotPerformException, InterruptedException {
        waitForSubmission();
        try {
            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForExecution();
                }
                return;
            }
        } catch (NotAvailableException ex) {
            // if the action description is not available, than we just continue and wait for it.
        }

        // wait on this action
        synchronized (executionSync) {
            // wait until state is reached
            while (!isStateExecuting()) {
                if (isDone()) {
                    throw new CouldNotPerformException("Action is done [" + actionDescription + "] but state was never executed");
                }

                executionSync.wait();
            }
        }
    }

    private boolean isStateExecuting() throws CouldNotPerformException {
        final Message serviceState = Services.invokeProviderServiceMethod(actionDescription.getServiceStateDescription().getServiceType(), targetUnit);
        final Descriptors.FieldDescriptor descriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
        final ActionDescription responsibleAction = (ActionDescription) serviceState.getField(descriptor);
        return actionDescription.getId().equals(responsibleAction.getId());
    }

    /**
     * Method blocks until the next successful action extension or the timeout was reached.
     *
     * @param timeout  the maximal time to wait for the extension.
     * @param timeUnit the time unit of the timeout.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public void waitForExtension(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        synchronized (extensionSync) {
            extensionSync.wait(timeUnit.toMillis(timeout));
        }
    }

    /**
     * Method blocks until the next successful action extension.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public void waitForExtension() throws InterruptedException {
        synchronized (extensionSync) {
            extensionSync.wait();
        }
    }

    public void waitForSubmission() throws CouldNotPerformException, InterruptedException {
        synchronized (executionSync) {
            if (futureObservationTask == null) {
                throw new InvalidStateException("Action was never executed!");
            }
        }

        try {
            futureObservationTask.get();

            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForSubmission();
                }
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not wait for submission!", ex);
        }
    }

    public void waitForSubmission(long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException, TimeoutException {
        synchronized (executionSync) {
            if (futureObservationTask == null) {
                throw new InvalidStateException("Action was never executed!");
            }
        }

        try {
            futureObservationTask.get(timeout, timeUnit);

            //TODO: split timeout
            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForSubmission(timeout, timeUnit);
                }
            }
        } catch (ExecutionException | CancellationException ex) {
            throw new CouldNotPerformException("Could not wait for submission!", ex);
        }
    }

    public boolean isSubmissionDone() {
        if (futureObservationTask != null && futureObservationTask.isDone()) {
            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    if (!impactedRemoteAction.isSubmissionDone()) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> extend() {
        try {
            // set extend flag and reapply
            return targetUnit.extendAction(getActionDescription(), authToken);
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    public void addActionDescriptionObserver(final Observer<RemoteAction, ActionDescription> observer) {
        actionDescriptionObservable.addObserver(observer);
    }

    public void removeActionDescriptionObserver(final Observer<RemoteAction, ActionDescription> observer) {
        actionDescriptionObservable.removeObserver(observer);
    }

    /**
     * Method returns the related unit which will be affected by this action.
     *
     * @return the target unit.
     */
    public UnitRemote<?> getTargetUnit() {
        return targetUnit;
    }

    /**
     * Generates a string representation of this action.
     *
     * @return a description of this unit.
     */
    @Override
    public String toString() {
        return Action.toString(this);
    }
}
