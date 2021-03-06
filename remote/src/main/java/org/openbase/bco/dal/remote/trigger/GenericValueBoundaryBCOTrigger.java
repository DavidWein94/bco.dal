package org.openbase.bco.dal.remote.trigger;

/*-
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
import com.google.protobuf.Message;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.state.ConnectionStateType;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.jul.pattern.trigger.AbstractTrigger;
import org.slf4j.Logger;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 * @param <UR> UnitRemote
 * @param <DT> DataType
 */
public class GenericValueBoundaryBCOTrigger<UR extends AbstractUnitRemote, DT extends Message> extends AbstractTrigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericBCOTrigger.class);

    public enum TriggerOperation {
        HIGH_ACTIVE, LOW_ACTIVE
    }

    private final UR unitRemote;
    private final ServiceType serviceType;
    private final double boundary;
    private final Observer<DataProvider<DT>,DT> dataObserver;
    private final Observer<Remote, ConnectionState.State> connectionObserver;
    private final TriggerOperation triggerOperation;
    private final String specificValueCall;
    private boolean active = false;

    public GenericValueBoundaryBCOTrigger(final UR unitRemote, final double boundary, final TriggerOperation triggerOperation, ServiceType serviceType, String specificValueCall) throws InstantiationException {
        super();

        this.unitRemote = unitRemote;
        this.serviceType = serviceType;
        this.boundary = boundary;
        this.triggerOperation = triggerOperation;
        this.specificValueCall = specificValueCall;

        dataObserver = (source, data) -> {
            verifyCondition(data);
        };

        connectionObserver = (Remote source, ConnectionStateType.ConnectionState.State data) -> {
            if (data.equals(ConnectionState.State.CONNECTED)) {
                verifyCondition((DT) unitRemote.getData());
            } else {
                notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.UNKNOWN).build()));
            }
        };
    }

    private void verifyCondition(DT data) {
        try {
            Object serviceState = Services.invokeProviderServiceMethod(serviceType, data);

            Method method = serviceState.getClass().getMethod(specificValueCall);
            double value = (Double) method.invoke(serviceState);

            if (triggerOperation == TriggerOperation.HIGH_ACTIVE) {
                if (value >= boundary) {
                    notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
                } else {
                    notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE).build()));
                }
            } else {
                if (value <= boundary) {
                    notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
                } else {
                    notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE).build()));
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not verify condition " + this, ex, LOGGER);
        } catch (NoSuchMethodException ex) {
            ExceptionPrinter.printHistory("Method not known " + this, ex, LOGGER);
        } catch (SecurityException ex) {
            ExceptionPrinter.printHistory("Security Exception " + this, ex, LOGGER);
        } catch (IllegalAccessException ex) {
            ExceptionPrinter.printHistory("Illegal Access Exception " + this, ex, LOGGER);
        } catch (IllegalArgumentException ex) {
            ExceptionPrinter.printHistory("Illegal Argument Exception " + this, ex, LOGGER);
        } catch (InvocationTargetException ex) {
            ExceptionPrinter.printHistory("Could not invoke method " + this, ex, LOGGER);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        unitRemote.addDataObserver(dataObserver);
        unitRemote.addConnectionStateObserver(connectionObserver);
        active = true;
        verifyCondition((DT) unitRemote.getData());
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        unitRemote.removeDataObserver(dataObserver);
        unitRemote.removeConnectionStateObserver(connectionObserver);
        active = false;
        notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.UNKNOWN).build()));
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void shutdown() {
        try {
            deactivate();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not shutdown " + this, ex, LOGGER);
        }
        super.shutdown();
    }
}
