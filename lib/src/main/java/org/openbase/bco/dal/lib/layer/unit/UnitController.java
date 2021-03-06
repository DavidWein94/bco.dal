package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.pattern.controller.MessageController;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.concurrent.Future;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitController<D extends AbstractMessage, DB extends D.Builder<DB>> extends Unit<D>, MessageController<D, DB> {

    /**
     * Method initialize this controller with the given unit configuration.
     *
     * @param config the unit configuration
     *
     * @throws InitializationException is throw if any error occurs during the initialization phase.
     * @throws InterruptedException    is thrown if the current thread was externally interrupted.
     */
    void init(final UnitConfig config) throws InitializationException, InterruptedException;

    /**
     * Applies the given service state update for this unit.
     *
     * @param serviceType the type of the service to update.
     * @param serviceState service state to apply.
     *
     * @throws CouldNotPerformException
     */
    void applyDataUpdate(final Message serviceState, final ServiceType serviceType) throws CouldNotPerformException;

    /**
     * Applies the given service state update for this unit.
     *
     * @param serviceType the type of the service to update.
     * @param serviceStateBuilder the service state to apply.
     *
     * @throws CouldNotPerformException
     */
    default void applyDataUpdate(final Message.Builder serviceStateBuilder, final ServiceType serviceType) throws CouldNotPerformException {
        applyDataUpdate(serviceStateBuilder.build(), serviceType);
    }

    /**
     * This method is called if an authorized and scheduled action causes a new service state.
     *
     * @param serviceState the new service state to apply.
     * @param serviceType  The type of the modified service.
     *
     * @return a future object representing the progress of the service state transition.
     */
    Future<Void> performOperationService(final Message serviceState, final ServiceType serviceType);
}
