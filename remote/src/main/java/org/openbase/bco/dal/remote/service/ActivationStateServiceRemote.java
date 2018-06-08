package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.ActivationStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ActivationStateType.ActivationState.State;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ActivationStateServiceRemote extends AbstractServiceRemote<ActivationStateOperationService, ActivationState> implements ActivationStateOperationServiceCollection {

    public ActivationStateServiceRemote() {
        super(ServiceType.ACTIVATION_STATE_SERVICE, ActivationState.class);
    }

    public Collection<ActivationStateOperationService> getActivationStateOperationServices() {
        return getServices();
    }

    /**
     * {@inheritDoc} Computes the activation state as on if at least one underlying service is on and else off.
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected ActivationState computeServiceState() throws CouldNotPerformException {
        return getActivationState(UnitType.UNKNOWN);
    }

    @Override
    public ActivationState getActivationState() throws NotAvailableException {
        return getData();
    }

    @Override
    public ActivationState getActivationState(final UnitType unitType) throws NotAvailableException {
        try {
            return (ActivationState) generateFusedState(unitType, State.DEACTIVE, State.ACTIVE).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(Services.getServiceStateName(getServiceType()), ex);
        }
    }

    @Override
    public Future<ActionFuture> setActivationState(final ActivationState activationState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(activationState, getServiceType()));
    }

    @Override
    public Future<ActionFuture> setActivationState(final ActivationState activationState, final UnitType unitType) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(activationState, getServiceType(), unitType));
    }
}
