package org.openbase.bco.dal.lib.layer.service.operation;

/*
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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.provider.ColorStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.state.ColorStateType.ColorState;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ColorStateOperationService extends OperationService, ColorStateProviderService {

    public static final String NEUTRAL_WHITE_KEY = "NEUTRAL_WHITE";
    public static final RGBColor DEFAULT_NEUTRAL_WHITE = RGBColor.newBuilder().setRed(200).setGreen(200).setBlue(200).build();

    @RPCMethod
    public Future<Void> setColorState(final ColorState colorState) throws CouldNotPerformException;

    @RPCMethod
    public Future<Void> setNeutralWhite() throws CouldNotPerformException;

    @RPCMethod
    public Future<Void> setColor(final HSBColor color) throws CouldNotPerformException;

    public Future<Void> setColor(final Color color) throws CouldNotPerformException;

    public Future<Void> setColor(final RGBColor color) throws CouldNotPerformException;

    @Deprecated
    public Future<Void> setColor(final java.awt.Color color) throws CouldNotPerformException;

//    @RPCMethod
//    default public Future<Void> setNeutralWhite() throws CouldNotPerformException {
//        return setColor(DEFAULT_NEUTRAL_WHITE);
//    }
//
//    default public Future<Void> setColor(final Color color) throws CouldNotPerformException {
//        return setColorState(ColorState.newBuilder().setColor(color).build());
//    }
//
//    @RPCMethod
//    default public Future<Void> setColor(final HSBColor color) throws CouldNotPerformException {
//        return setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(color).build());
//    }
//
//    default public Future<Void> setColor(final RGBColor color) throws CouldNotPerformException {
//        return setColor(Color.newBuilder().setType(Color.Type.RGB).setRgbColor(color).build());
//    }
//
//    default public Future<Void> setColor(final java.awt.Color color) throws CouldNotPerformException {
//        return setColor(HSBColorToRGBColorTransformer.transform(color));
//    }
//
//    default public Future<Void> setColor(final Color color) throws CouldNotPerformException {
//        return setColorState(ColorState.newBuilder().setColor(color).build());
//    }
//
//    default public Future<Void> setColor(final RGBColor color) throws CouldNotPerformException {
//        return setColor(Color.newBuilder().setType(Color.Type.RGB).setRgbColor(color).build());
//    }

//    /**
//     *
//     * @param color
//     * @return
//     * @throws CouldNotPerformException
//     * @deprecated Please use the org.openbase.jul.visual.swing.transform.AWTColorToHSBColorTransformer or org.openbase.jul.visual.javafx.transform.JFXColorToHSBColorTransformer to tranform colors into compatible types.
//     */
//    @Deprecated
//    default public Future<Void> setColor(final java.awt.Color color) throws CouldNotPerformException {
//        try {
//            float[] hsb = new float[3];
//            java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
//            return setColor(HSBColor.newBuilder().setHue(hsb[0] * 360).setSaturation(hsb[1] * 100).setBrightness(hsb[2] * 100).build());
//        } catch (Exception ex) {
//            throw new CouldNotTransformException("Could not transform " + java.awt.Color.class.getName() + " to " + HSBColor.class.getName() + "!", ex);
//        }
//    }
}
