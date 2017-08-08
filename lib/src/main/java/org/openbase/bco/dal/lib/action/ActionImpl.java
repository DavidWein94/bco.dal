package org.openbase.bco.dal.lib.action;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.unit.AbstractUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitAllocation;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotInitializedException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.state.ActionStateType.ActionState;

/**
 *
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ActionImpl implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionImpl.class);

    private final SyncObject executionSync = new SyncObject(ActionImpl.class);
    private final AbstractUnitController unit;
    private final ServiceJSonProcessor serviceJSonProcessor;
    private ActionDescription.Builder actionDescriptionBuilder;
    private Object serviceAttribute;
    private ServiceDescription serviceDescription;

    public ActionImpl(final AbstractUnitController unit) {
        this.unit = unit;
        this.serviceJSonProcessor = new ServiceJSonProcessor();
    }

    @Override
    public void init(final ActionDescription actionDescription) throws InitializationException, InterruptedException {
        try {
            // verify
            this.verifyActionDescription(actionDescription);

            // prepare
            this.actionDescriptionBuilder = actionDescription.toBuilder();
            this.serviceAttribute = serviceJSonProcessor.deserialize(actionDescription.getServiceStateDescription().getServiceAttribute(), actionDescription.getServiceStateDescription().getServiceAttributeType());

            // Since its an action it has to be an operation service pattern
            this.serviceDescription = ServiceDescription.newBuilder().setType(actionDescription.getServiceStateDescription().getServiceType()).setPattern(ServicePattern.OPERATION).build();

            // Set resource allocation interval if not defined yet
            if (!actionDescription.getResourceAllocation().getSlot().hasBegin()) {
                ActionDescriptionProcessor.updateResourceAllocationSlot(actionDescriptionBuilder);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void verifyActionDescription(final ActionDescription actionDescription) throws VerificationFailedException {
        try {

            if (actionDescription == null) {
                throw new NotAvailableException("ActionDescription");
            }

            if (!actionDescription.hasServiceStateDescription()) {
                throw new NotAvailableException("ActionDescription.ServiceStateDescription");
            }

            if (!actionDescription.getServiceStateDescription().hasUnitId() || actionDescription.getServiceStateDescription().getUnitId().isEmpty()) {
                throw new NotAvailableException("ActionDescription.ServiceStateDescription.UnitId");
            }

            if (!actionDescription.getServiceStateDescription().getUnitId().equals(unit.getId())) {
                throw new InvalidStateException("Referred unit is not compatible with the registered unit controller!");
            }
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException("Given ActionDescription[" + actionDescription.getLabel() + "] is invalid!", ex);
        }
    }

    @Override
    public Future<ActionFuture> execute() throws CouldNotPerformException {
        try {
            synchronized (executionSync) {

                if (actionDescriptionBuilder == null) {
                    throw new NotInitializedException("Action");
                }

                // Initiate
                updateActionState(ActionState.State.INITIATING);
                UnitAllocation unitAllocation;

                try {
                    // Verify authority
                    unit.verifyAuthority(actionDescriptionBuilder.getActionAuthority());

                    // Resource Allocation
//                    unitAllocation = UnitAllocator.allocate(actionDescriptionBuilder, () -> {
//                        try {
//                            // Execute
//                            updateActionState(ActionState.State.EXECUTING);
//
//                            try {
//                                Service.invokeServiceMethod(serviceDescription, unit, serviceAttribute);
//                            } catch (CouldNotPerformException ex) {
//                                if (ex.getCause() instanceof InterruptedException) {
//                                    updateActionState(ActionState.State.ABORTED);
//                                } else {
//                                    updateActionState(ActionState.State.EXECUTION_FAILED);
//                                }
//                                throw new ExecutionException(ex);
//                            }
//                            updateActionState(ActionState.State.FINISHING);
//                            return null;
//                        } catch (final CancellationException ex) {
//                            updateActionState(ActionState.State.ABORTED);
//                            throw ex;
//                        }
//                    });
//                    
//                    // register allocation update handler
//                    unitAllocation.getTaskExecutor().getRemote().addSchedulerListener((allocation) -> {
//                        actionDescriptionBuilder.getResourceAllocationBuilder().mergeFrom(allocation);
//                    });
//                    
//                    return unitAllocation.getTaskExecutor().getFuture();
                    return GlobalCachedExecutorService.submit(() -> {
                        Service.invokeServiceMethod(serviceDescription, unit, serviceAttribute);
                        return null;
                    });
                } catch (CouldNotPerformException ex) {
                    updateActionState(ActionState.State.REJECTED);
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not execute action!", ex);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    public ActionDescription getActionDescription() throws NotAvailableException {
        return actionDescriptionBuilder.build();
    }

    private void updateActionState(ActionState.State state) {
        actionDescriptionBuilder.setActionState(ActionState.newBuilder().setValue(state));
        LOGGER.debug("StateUpdate[" + state.name() + "] of " + this);
    }

    @Override
    public String toString() {
        if (actionDescriptionBuilder == null) {
            return getClass().getSimpleName() + "[?]";
        }
        return getClass().getSimpleName() + "[" + actionDescriptionBuilder.getServiceStateDescription().getUnitId() + "|" + actionDescriptionBuilder.getServiceStateDescription().getServiceType() + "|" + actionDescriptionBuilder.getServiceStateDescription().getServiceAttribute() + "|" + actionDescriptionBuilder.getServiceStateDescription().getUnitId() + "]";
    }
}
