package org.openbase.bco.dal.lib.transform;

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

import java.awt.Color;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.vision.HSVColorType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author mpohling
 */
public class HSVColorToRGBColorTransformer {

    public static HSVColorType.HSVColor transform(Color color) throws CouldNotTransformException {
        try {
            float[] hsb = new float[3];
            Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
            return HSVColor.newBuilder().setHue(hsb[0] * 360).setSaturation(hsb[1] * 100).setValue(hsb[2] * 100).build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + Color.class.getName() + " to " + HSVColorType.HSVColor.class.getName() + "!", ex);
        }
    }
    
    public static Color transform(HSVColorType.HSVColor color) throws TypeNotSupportedException, CouldNotTransformException {
        try {
            return Color.getHSBColor((((float)color.getHue()) / 360f), (((float)color.getSaturation()) / 100f), (((float)color.getValue()) / 100f));
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + HSVColorType.HSVColor.class.getName() + " to " + Color.class.getName() + "!", ex);
        }
    }
}
