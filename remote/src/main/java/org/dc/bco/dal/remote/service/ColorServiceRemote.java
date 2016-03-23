/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import com.google.protobuf.InvalidProtocolBufferException;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.VerificationFailedException;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.service.ServiceTemplateType;
import rst.vision.HSVColorType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ColorServiceRemote extends AbstractServiceRemote<ColorService> implements ColorService {

    public ColorServiceRemote() {
        super(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_SERVICE);
    }

    @Override
    public void setColor(final HSVColorType.HSVColor color) throws CouldNotPerformException {
        for (ColorService service : getServices()) {
            service.setColor(color);
        }
    }

    @Override
    public HSVColorType.HSVColor getColor() throws CouldNotPerformException {
        for (ColorService service : getServices()) {
            return service.getColor();
        }
        throw new CouldNotPerformException("Not supported yet.");
    }

    @Override
    public void applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        try {
            if (!actionConfig.getServiceType().equals(getServiceType())) {
                throw new VerificationFailedException("Service type is not compatible to given action config!");
            }
            setColor(HSVColorType.HSVColor.parseFrom(actionConfig.getServiceAttributeBytes()));
        } catch (InvalidProtocolBufferException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        }
    }
}
