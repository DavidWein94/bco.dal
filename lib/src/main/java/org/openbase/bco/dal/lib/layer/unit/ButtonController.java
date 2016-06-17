package org.openbase.bco.dal.lib.layer.unit;

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


import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ButtonStateType.ButtonState;
import rst.homeautomation.unit.ButtonType.Button;
import rst.timing.TimestampType;

/**
 *
 * @author mpohling
 */
public class ButtonController extends AbstractUnitController<Button, Button.Builder> implements ButtonInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Button.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonState.getDefaultInstance()));
    }

    public ButtonController(final UnitHost unitHost, final Button.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(ButtonController.class, unitHost, builder);
    }

    public void updateButtonProvider(final ButtonState state) throws CouldNotPerformException {

        logger.debug("Apply button Update[" + state + "] for " + this + ".");

        try (ClosableDataBuilder<Button.Builder> dataBuilder = getDataBuilder(this)) {
            
            ButtonState.Builder buttonState = dataBuilder.getInternalBuilder().getButtonStateBuilder();
            
            // Update value
            buttonState.setValue(state.getValue());
            
            // Update timestemp if necessary
            if (state.getValue() == ButtonState.State.CLICKED || state.getValue() == ButtonState.State.DOUBLE_CLICKED) {
                //TODO tamino: need to be tested! Please write an unit test.
                buttonState.setLastClicked(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()));
            }

            dataBuilder.getInternalBuilder().setButtonState(buttonState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply button Update[" + state + "] for " + this + "!", ex);
        }
    }

    @Override
    public ButtonState getButton() throws NotAvailableException {
        try {
            return getData().getButtonState();
        } catch(CouldNotPerformException ex) {
            throw new NotAvailableException("buttion state", ex);
        }
    }
}