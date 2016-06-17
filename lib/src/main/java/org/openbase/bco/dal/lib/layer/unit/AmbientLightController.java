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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.AmbientLightType.AmbientLight;
import rst.homeautomation.unit.UnitConfigType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author Tamino Huxohl
 * @author Marian Pohling
 */
public class AmbientLightController extends AbstractUnitController<AmbientLight, AmbientLight.Builder> implements AmbientLightInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AmbientLight.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSVColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    private ColorOperationService colorService;
    private BrightnessOperationService brightnessService;
    private PowerOperationService powerService;

    public AmbientLightController(final UnitHost unitHost, final AmbientLight.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(AmbientLightController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            this.powerService = getServiceFactory().newPowerService(this);
            this.colorService = getServiceFactory().newColorService(this);
            this.brightnessService = getServiceFactory().newBrightnessService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void updatePowerProvider(final PowerState value) throws CouldNotPerformException {
        logger.debug("Apply power Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setPowerState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setPower(final PowerState state) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to PowerState [" + state + "]");
        return powerService.setPower(state);
    }

    @Override
    public PowerState getPower() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power", ex);
        }
    }

    public void updateColorProvider(final HSVColor value) throws CouldNotPerformException {
        logger.debug("Apply color Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setColor(value);
            dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply color Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setColor(final HSVColor color) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to HSVColor[" + color.getHue() + "|" + color.getSaturation() + "|" + color.getValue() + "]");
        return colorService.setColor(color);
    }

    @Override
    public HSVColor getColor() throws NotAvailableException {
        try {
            return getData().getColor();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("color", ex);
        }
    }

    public void updateBrightnessProvider(Double value) throws CouldNotPerformException {
        logger.debug("Apply brightness Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setColor(dataBuilder.getInternalBuilder().getColor().toBuilder().setValue(value).build());
            if (value == 0) {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.OFF);
            } else {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply brightness Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setBrightness(Double brightness) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to Brightness[" + brightness + "]");
        return brightnessService.setBrightness(brightness);
    }

    @Override
    public Double getBrightness() throws NotAvailableException {
        try {
            return getData().getColor().getValue();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("brightness", ex);
        }
    }
}