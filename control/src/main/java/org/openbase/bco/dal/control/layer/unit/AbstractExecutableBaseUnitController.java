package org.openbase.bco.dal.control.layer.unit;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ActivationStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.iface.Enableable;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.ACTIVATION_STATE_SERVICE;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractExecutableBaseUnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends AbstractBaseUnitController<D, DB> implements Enableable, ActivationStateProviderService {

    public static final String FIELD_ACTIVATION_STATE = "activation_state";
    public static final String FIELD_AUTOSTART = "autostart";

    private final SyncObject enablingLock = new SyncObject(AbstractExecutableBaseUnitController.class);
    private final SyncObject executionLock = new SyncObject("ExecutionLock");
    private Future<ActionFuture> executionFuture;
    private boolean enabled;
    private final ActivationStateOperationServiceImpl activationStateOperationService;

    public AbstractExecutableBaseUnitController(final Class unitClass, final DB builder) throws org.openbase.jul.exception.InstantiationException {
        super(unitClass, builder);
        try {
            this.activationStateOperationService = new ActivationStateOperationServiceImpl(this);
            registerOperationService(ServiceType.ACTIVATION_STATE_SERVICE, activationStateOperationService);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
    }

    public Future<ActionFuture> setActivationState(final ActivationState activationState) throws CouldNotPerformException {
        return applyUnauthorizedAction(activationState, ACTIVATION_STATE_SERVICE);
    }

    public boolean isExecuting() {
        synchronized (executionLock) {
            return executionFuture != null && !executionFuture.isDone();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() throws CouldNotPerformException, InterruptedException {
        try {
            synchronized (enablingLock) {
                enabled = true;
                activate();
                if (detectAutostart()) {
                    activationStateOperationService.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
                } else {
                    activationStateOperationService.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build()).get();
                }
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not enable " + this, ex);
        }
    }

    @Override
    public void disable() throws CouldNotPerformException, InterruptedException {
        try {
            synchronized (enablingLock) {
                cancelExecution();
                enabled = false;
                activationStateOperationService.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build()).get();
                deactivate();
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not disable " + this, ex);
        }
    }

    private boolean detectAutostart() {
        try {
            return isAutostartEnabled();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new NotSupportedException("autostart", (AbstractExecutableBaseUnitController) this, (Throwable) ex), logger, LogLevel.WARN);
            return true;
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        stop();
        super.deactivate();
    }

    public void cancelExecution() {
        synchronized (executionLock) {
            if (isExecuting() && !executionFuture.isDone()) {
                executionFuture.cancel(true);
            }
            executionFuture = null;
        }
    }

    protected abstract boolean isAutostartEnabled() throws CouldNotPerformException;

    protected abstract void execute() throws CouldNotPerformException, InterruptedException;

    protected abstract void stop() throws CouldNotPerformException, InterruptedException;

    public class ActivationStateOperationServiceImpl implements ActivationStateOperationService {

        private final ServiceProvider serviceProvider;

        public ActivationStateOperationServiceImpl(ServiceProvider serviceProvider) {
            this.serviceProvider = serviceProvider;
        }

        @Override
        public Future<ActionFuture> setActivationState(ActivationStateType.ActivationState activationState) throws CouldNotPerformException {
            synchronized (executionLock) {
                // filter events that do not change anything
                if (activationState.getValue() == getActivationState().getValue()) {
                    return CompletableFuture.completedFuture(null);
                }

                if (activationState.getValue() == ActivationStateType.ActivationState.State.ACTIVE) {

                    // filter duplicated execution
                    if (isExecuting()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    executionFuture = GlobalCachedExecutorService.submit(() -> {
                        try {
                            execute();
                        } catch (CouldNotPerformException ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not execute [" + getLabel() + "]", ex), logger);
                        }
                        return null;
                    });
                } else {
                    if (isExecuting()) {
                        cancelExecution();
                    }
                    // call stop even if execution has already finished
                    // many components just register observer etc. in execute and this it is done quickly
                    try {
                        stop();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                try {
                    applyDataUpdate(activationState.toBuilder().setTimestamp(TimestampProcessor.getCurrentTimestamp()).build(), ServiceType.ACTIVATION_STATE_SERVICE);
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not " + StringProcessor.transformUpperCaseToCamelCase(activationState.getValue().name()) + " " + this, ex);
                }
            }

            return CompletableFuture.completedFuture(null);
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return serviceProvider;
        }
    }
}
