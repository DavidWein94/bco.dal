package org.openbase.bco.dal.lib.layer.unit;

/*-
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
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.iface.Enableable;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import rst.domotic.state.ActivationStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <D> the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 */
public abstract class AbstractExecutableBaseUnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends AbstractBaseUnitController<D, DB> implements Enableable {

    public static final String FIELD_ACTIVATION_STATE = "activation_state";
    public static final String FIELD_AUTOSTART = "autostart";

    private final SyncObject enablingLock = new SyncObject(AbstractExecutableBaseUnitController.class);
    private Future<Void> executionFuture;
    private boolean executing;

    public AbstractExecutableBaseUnitController(final Class unitClass, final DB builder) throws org.openbase.jul.exception.InstantiationException {
        super(unitClass, builder);
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        this.executing = false;
        super.init(config);
    }

    public ActivationStateType.ActivationState getActivationState() throws NotAvailableException {
        return (ActivationStateType.ActivationState) getDataField(FIELD_ACTIVATION_STATE);
    }

    public Future<Void> setActivationState(final ActivationStateType.ActivationState activation) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                if (activation == null || activation.getValue().equals(ActivationStateType.ActivationState.State.UNKNOWN)) {
                    throw new InvalidStateException("Unknown is not a valid state!");
                }

                try (ClosableDataBuilder<DB> dataBuilder = getDataBuilder(this)) {
                    try {
                        if (activation.getValue() == ActivationStateType.ActivationState.State.ACTIVE) {
                            if (!executing) {
                                executing = true;
                                executionFuture = GlobalCachedExecutorService.submit(new Callable<Void>() {

                                    @Override
                                    public Void call() throws Exception {
                                        execute();
                                        return null;
                                    }
                                });
                            }
                        } else {
                            if (executing) {
                                if (executionFuture != null || !executionFuture.isDone()) {
                                    executionFuture.cancel(true);
                                }
                                executing = false;
                                executionFuture = null;
                                stop();
                            }
                        }

                        // save new activation state
                        Descriptors.FieldDescriptor findFieldByName = dataBuilder.getInternalBuilder().getDescriptorForType().findFieldByName(FIELD_ACTIVATION_STATE);
                        if (findFieldByName == null) {
                            throw new NotAvailableException("Field[" + FIELD_ACTIVATION_STATE + "] does not exist for type " + dataBuilder.getClass().getName());
                        }
                        dataBuilder.getInternalBuilder().setField(findFieldByName, activation);

                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update execution state!", ex), logger);
                    }
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not " + StringProcessor.transformUpperCaseToCamelCase(activation.getValue().name()) + " " + this, ex);
                }
                return null;
            }
        });
    }

    public boolean isExecuting() {
        return executing;
    }

    @Override
    public void enable() throws CouldNotPerformException, InterruptedException {
        try {
            synchronized (enablingLock) {
                activate();
                if (detectAutostart()) {
                    setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.ACTIVE).build()).get();
                } else {
                    setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.DEACTIVE).build()).get();
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
                executing = false;
                setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.DEACTIVE).build()).get();
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

    protected abstract boolean isAutostartEnabled() throws CouldNotPerformException;

    protected abstract void execute() throws CouldNotPerformException, InterruptedException;

    protected abstract void stop() throws CouldNotPerformException, InterruptedException;
}
