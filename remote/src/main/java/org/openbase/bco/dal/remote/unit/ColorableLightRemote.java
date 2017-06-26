package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * BCO DAL Remote
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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.unit.ColorableLight;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.BrightnessStateType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.ColorableLightDataType;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ColorableLightRemote extends AbstractUnitRemote<ColorableLightData> implements ColorableLight {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorableLightDataType.ColorableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorStateType.ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerStateType.PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorType.Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColorType.HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColorType.RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessStateType.BrightnessState.getDefaultInstance()));
    }

    public ColorableLightRemote() {
        super(ColorableLightData.class);
    }

    @Override
    public Future<Void> setColorState(final ColorState colorState) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(colorState, this, Void.class);
    }

    @Override
    public Future<Void> setNeutralWhite() throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(this, Void.class);
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(powerState, this, Void.class);
    }

    public Future<Void> setPowerState(PowerState.State powerState) throws CouldNotPerformException {
        return setPowerState(PowerState.newBuilder().setValue(powerState).build());
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        try {
            return getData().getColorState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ColorState", ex);
        }
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return getData().getBrightnessState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("BrightnessState", ex);
        }
    }


    //////////
    // START DEFAULT INTERFACE METHODS

    public ColorType.Color getColor() throws NotAvailableException {
        return getColorState().getColor();
    }

    public HSBColorType.HSBColor getHSBColor() throws NotAvailableException {
        return getColorState().getColor().getHsbColor();
    }

    public RGBColorType.RGBColor getRGBColor() throws NotAvailableException {
        return getColorState().getColor().getRgbColor();
    }

    public java.awt.Color getJavaAWTColor() throws CouldNotPerformException {
        try {
            final HSBColorType.HSBColor color = getHSBColor();
            return java.awt.Color.getHSBColor((((float) color.getHue()) / 360f), (((float) color.getSaturation()) / 100f), (((float) color.getBrightness()) / 100f));
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + HSBColorType.HSBColor.class.getName() + " to " + java.awt.Color.class.getName() + "!", ex);
        }
    }

    public Future<Void> setColor(final ColorType.Color color) throws CouldNotPerformException {
        return setColorState(ColorState.newBuilder().setColor(color).build());
    }

    public Future<Void> setColor(final HSBColorType.HSBColor color) throws CouldNotPerformException {
        return setColor(ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(color).build());
    }

    public Future<Void> setColor(final RGBColorType.RGBColor color) throws CouldNotPerformException {
        return setColor(ColorType.Color.newBuilder().setType(ColorType.Color.Type.RGB).setRgbColor(color).build());
    }

    public Future<Void> setColor(final java.awt.Color color) throws CouldNotPerformException {
        try {
            float[] hsb = new float[3];
            java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
            return setColor(HSBColorType.HSBColor.newBuilder().setHue(hsb[0] * 360).setSaturation(hsb[1] * 100).setBrightness(hsb[2] * 100).build());
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + java.awt.Color.class.getName() + " to " + HSBColorType.HSBColor.class.getName() + "!", ex);
        }
    }

    // END DEFAULT INTERFACE METHODS
    //////////
}
