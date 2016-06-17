package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.Collection;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.GlobalExecutionService;
import rst.homeautomation.state.StandbyStateType.StandbyState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface StandbyStateOperationServiceCollection extends StandbyOperationService {

    @Override
    default public Future<Void> setStandby(StandbyState state) throws CouldNotPerformException {
        return GlobalExecutionService.allOf((StandbyOperationService input) -> input.setStandby(state), getStandbyStateOperationServices());
    }

    /**
     * Returns running if at least one of the standby services is running and
     * else standby.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public StandbyState getStandby() throws NotAvailableException {
        try {
            for (StandbyOperationService service : getStandbyStateOperationServices()) {
                if (service.getStandby().getValue() == StandbyState.State.RUNNING) {
                    return StandbyState.newBuilder().setValue(StandbyState.State.RUNNING).build();
                }
            }
            return StandbyState.newBuilder().setValue(StandbyState.State.STANDBY).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("StandbyState", ex);
        }
    }

    public Collection<StandbyOperationService> getStandbyStateOperationServices() throws CouldNotPerformException;
}