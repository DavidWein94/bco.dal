package org.openbase.bco.dal.remote.service;

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
import java.util.Collection;
import java8.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.jul.extension.rst.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.iface.Processable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ColorStateType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType;
import rst.vision.RGBColorType.RGBColor;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ColorStateServiceRemote extends AbstractServiceRemote<ColorStateOperationService, ColorState> implements ColorStateOperationServiceCollection {

    public ColorStateServiceRemote() {
        super(ServiceType.COLOR_STATE_SERVICE, ColorState.class);
    }

    public Collection<ColorStateOperationService> getColorStateOperationServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the average RGB color.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected ColorState computeServiceState() throws CouldNotPerformException {
        return getColorState(UnitType.UNKNOWN);
    }

    @Override
    public Future<Void> setColorState(ColorState colorState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf(getServices(), new Processable<ColorStateOperationService, Future<Void>>() {
            @Override
            public Future<Void> process(ColorStateOperationService input) throws CouldNotPerformException, InterruptedException {
                return input.setColorState(colorState);
            }
        });
    }

    @Override
    public Future<Void> setColorState(final ColorState colorState, final UnitType unitType) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf(getServices(unitType), new Processable<ColorStateOperationService, Future<Void>>() {
            @Override
            public Future<Void> process(ColorStateOperationService input) throws CouldNotPerformException, InterruptedException {
                return input.setColorState(colorState);
            }
        });
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public ColorState getColorState(final UnitType unitType) throws NotAvailableException {
        try {
            double averageRed = 0;
            double averageGreen = 0;
            double averageBlue = 0;
            int amount = getColorStateOperationServices().size();
            long timestamp = 0;
            Collection<ColorStateOperationService> colorStateOperationServiceCollection = getServices(unitType);
            for (ColorStateOperationService service : colorStateOperationServiceCollection) {
                if (!((UnitRemote) service).isDataAvailable()) {
                    amount--;
                    continue;
                }

                RGBColor rgbColor = HSBColorToRGBColorTransformer.transform(service.getColorState().getColor().getHsbColor());
                averageRed += rgbColor.getRed();
                averageGreen += rgbColor.getGreen();
                averageBlue += rgbColor.getBlue();
                timestamp = Math.max(timestamp, service.getColorState().getTimestamp().getTime());
            }
            averageRed = averageRed / amount;
            averageGreen = averageGreen / amount;
            averageBlue = averageBlue / amount;

            HSBColor hsbColor = HSBColorToRGBColorTransformer.transform(RGBColor.newBuilder().setRed((int) averageRed).setGreen((int) averageGreen).setBlue((int) averageBlue).build());
            return TimestampProcessor.updateTimestamp(timestamp, ColorState.newBuilder().setColor(ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(hsbColor)), TimeUnit.MICROSECONDS, logger).build();
        } catch (CouldNotTransformException ex) {
            throw new NotAvailableException("Could not transform from HSB to RGB or vice-versa!", ex);
        }
    }

    /////////////
    // START DEFAULT INTERFACE METHODS
    /////////////
    public void activate(boolean waitForData) throws CouldNotPerformException, InterruptedException {
        activate();
        waitForData();
    }

    public CompletableFuture<ColorState> requestData() throws CouldNotPerformException {
        return requestData(true);
    }

    public void addConnectionStateObserver(Observer<ConnectionState> observer) {
        for (Remote remote : getInternalUnits()) {
            remote.addConnectionStateObserver(observer);
        }
    }

    public ConnectionState getConnectionState() {
        boolean disconnectedRemoteDetected = false;
        boolean connectedRemoteDetected = false;

        for (final Remote remote : getInternalUnits()) {
            switch (remote.getConnectionState()) {
                case CONNECTED:
                    connectedRemoteDetected = true;
                    break;
                case CONNECTING:
                case DISCONNECTED:
                    disconnectedRemoteDetected = true;
                    break;
                default:
                    //ignore unknown connection state";
            }
        }

        if (disconnectedRemoteDetected && connectedRemoteDetected) {
            return ConnectionState.CONNECTING;
        } else if (disconnectedRemoteDetected) {
            return ConnectionState.DISCONNECTED;
        } else if (connectedRemoteDetected) {
            return ConnectionState.CONNECTED;
        } else {
            return ConnectionState.UNKNOWN;
        }
    }

    public void removeConnectionStateObserver(Observer<ConnectionState> observer) {
        for (final Remote remote : getInternalUnits()) {
            remote.removeConnectionStateObserver(observer);
        }
    }

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
            final HSBColor color = getHSBColor();
            return java.awt.Color.getHSBColor((((float) color.getHue()) / 360f), (((float) color.getSaturation()) / 100f), (((float) color.getBrightness()) / 100f));
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + HSBColor.class.getName() + " to " + java.awt.Color.class.getName() + "!", ex);
        }
    }

    public Future<Void> setNeutralWhite() throws CouldNotPerformException {
        return setColor(DEFAULT_NEUTRAL_WHITE);
    }

    public Future<Void> setColor(final ColorType.Color color) throws CouldNotPerformException {
        return setColorState(ColorStateType.ColorState.newBuilder().setColor(color).build());
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

    /////////////
    // END DEFAULT INTERFACE METHODS
    /////////////
}
