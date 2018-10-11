package org.openbase.bco.dal.lib.layer.service.provider;

/*
 * #%L
 * BCO DAL Library
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.annotation.RPCMethod;
import rst.domotic.state.BlindStateType.BlindState;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface BlindStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default BlindState getBlindState() throws NotAvailableException {
        return (BlindState) getServiceProvider().getServiceState(BLIND_STATE_SERVICE);
    }

    static void verifyBlindState(final BlindState blindState) throws VerificationFailedException {
        try {
            if (!(blindState.hasValue() || blindState.hasOpeningRatio())) {
                throw new VerificationFailedException("MovementState or OpeningRatio not available!", new InvalidStateException(blindState.toString()));
            }

            if (blindState.hasOpeningRatio() && blindState.getOpeningRatio() < 0) {
                throw new VerificationFailedException("Opening ratio of blind state out of range with value[" + blindState.getOpeningRatio() + "]!", new InvalidStateException(blindState.toString()));
            }

            if (blindState.hasValue()) {
                switch (blindState.getValue()) {
                    case UNKNOWN:
                        throw new VerificationFailedException("MovementState unknown!", new InvalidStateException(blindState.toString()));
                    default:
                        break;
                }
            }
        } catch (final CouldNotPerformException ex) {
            throw new VerificationFailedException("BlindState not valid!", ex);
        }
    }
}
