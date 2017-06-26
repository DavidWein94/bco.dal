package org.openbase.bco.dal.remote.unit.unitgroup;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.service.collection.*;
import org.openbase.bco.dal.lib.layer.unit.unitgroup.UnitGroup;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteManager;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType;
import rst.domotic.action.SnapshotType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.*;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupRemote extends AbstractUnitRemote<UnitGroupData> implements UnitGroup {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationDataType.LocationData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColorType.HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorStateType.ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorType.Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColorType.RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerStateType.PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmStateType.AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionStateType.MotionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionStateType.PowerConsumptionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindStateType.BlindState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeStateType.SmokeState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyStateType.StandbyState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperStateType.TamperState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessStateType.BrightnessState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureStateType.TemperatureState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceStateType.PresenceState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionConfigType.ActionConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SnapshotType.Snapshot.getDefaultInstance()));
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitGroupRemote.class);
    private final ServiceRemoteManager serviceRemoteManager;

    public UnitGroupRemote() throws InstantiationException {
        super(UnitGroupData.class);
        this.serviceRemoteManager = new ServiceRemoteManager(this) {
            @Override
            protected Set<ServiceTemplateType.ServiceTemplate.ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException {
                return getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(Observable source, Object data) throws NotAvailableException, InterruptedException {
                updateUnitData();
            }
        };
    }

    @Override
    public void init(final UnitConfig unitGroupUnitConfig) throws InitializationException, InterruptedException {
        try {
            Registries.getUnitRegistry().waitForData();

            if (!unitGroupUnitConfig.hasUnitGroupConfig()) {
                throw new VerificationFailedException("Given unit config does not contain a unit group config!");
            }

            if (unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList().isEmpty()) {
                throw new VerificationFailedException("UnitGroupConfig has no unit members!");
            }

            List<UnitConfig> unitConfigs = new ArrayList<>();
            for (String unitConfigId : unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList()) {
                unitConfigs.add(Registries.getUnitRegistry().getUnitConfigById(unitConfigId));
            }

            if (unitConfigs.isEmpty()) {
                throw new CouldNotPerformException("Could not resolve any unit members!");
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(unitGroupUnitConfig);
    }
    
    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        serviceRemoteManager.applyConfigUpdate(unitConfig.getUnitGroupConfig().getMemberIdList());
        return unitConfig;
    }
    

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        serviceRemoteManager.activate();
        updateUnitData();
        // super activation is disabled because unit remote is not a RSBRemoteService
        // TODO: Refactor to support UnitRemotes which are not extended RSBRemoteService instances.
        // super.activate();
    }

    @Override
    public boolean isActive() {
        return serviceRemoteManager.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        serviceRemoteManager.deactivate();
    }

    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws NotAvailableException, InterruptedException {
        //todo reimplement with respect to the given timeout.
        try {
            super.waitForData(timeout, timeUnit);
            for (AbstractServiceRemote remote : serviceRemoteManager.getServiceRemoteList()) {
                remote.waitForData(timeout, timeUnit);
            }
            updateUnitData();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceData", ex);
        }
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        // super waitForData is disabled because unit remote is not a RSBRemoteService
        // TODO: Refactor to support UnitRemotes which are not extended RSBRemoteService instances.
        for (AbstractServiceRemote remote : serviceRemoteManager.getServiceRemoteList()) {
            remote.waitForData();
        }
        updateUnitData();
    }
    
    private void updateUnitData() throws InterruptedException {
        try {
            UnitGroupData.Builder dataBuilder = UnitGroupData.newBuilder();
            dataBuilder.setId(getConfig().getId());
            dataBuilder.setLabel(getConfig().getLabel());
            applyExternalDataUpdate(serviceRemoteManager.updateBuilderWithAvailableServiceStates(dataBuilder, getDataClass(), getSupportedServiceTypes()).build());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update current status!", ex), LOGGER, LogLevel.WARN);
        }
    }
    
    @Override
    public Future<SnapshotType.Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.recordSnapshot();
    }

    @Override
    public Future<SnapshotType.Snapshot> recordSnapshot(final UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.recordSnapshot(unitType);
    }

    @Override
    public Future<Void> restoreSnapshot(final SnapshotType.Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.restoreSnapshot(snapshot);
    }

    @Override
    public Future<Void> applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.applyAction(actionConfig);
    }
    
    @Override
    public ServiceRemote getServiceRemote(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws NotAvailableException {
        return serviceRemoteManager.getServiceRemote(serviceType);
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

    @Override
    public Future<Void> setBlindState(BlindStateType.BlindState blindState, UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState, unitType);
    }

    @Override
    public Future<Void> setBlindState(final BlindStateType.BlindState blindState) throws CouldNotPerformException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState);
    }

    @Override
    public BlindStateType.BlindState getBlindState() throws NotAvailableException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).getBlindState();
    }

    @Override
    public BlindStateType.BlindState getBlindState(final UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).getBlindState(unitType);
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessStateType.BrightnessState brightnessState) throws CouldNotPerformException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState);
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessStateType.BrightnessState brightnessState, UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState, unitType);
    }

    @Override
    public BrightnessStateType.BrightnessState getBrightnessState() throws NotAvailableException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState();
    }

    @Override
    public BrightnessStateType.BrightnessState getBrightnessState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState(unitType);
    }

    @Override
    public Future<Void> setColorState(ColorStateType.ColorState colorState) throws CouldNotPerformException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState);
    }

    @Override
    public Future<Void> setColorState(ColorStateType.ColorState colorState, UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState, unitType);
    }

    @Override
    public ColorStateType.ColorState getColorState() throws NotAvailableException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).getColorState();
    }

    @Override
    public ColorStateType.ColorState getColorState(final UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).getColorState(unitType);
    }

    @Override
    public Future<Void> setPowerState(final PowerStateType.PowerState powerState) throws CouldNotPerformException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState);
    }

    @Override
    public Future<Void> setPowerState(final PowerStateType.PowerState powerState, final UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState, unitType);
    }

    public Future<Void> setPowerState(final PowerStateType.PowerState.State state) throws CouldNotPerformException {
        return setPowerState(PowerStateType.PowerState.newBuilder().setValue(state).build());
    }

    public Future<Void> setPowerState(final PowerStateType.PowerState.State state, final UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return setPowerState(PowerStateType.PowerState.newBuilder().setValue(state).build(), unitType);
    }

    @Override
    public PowerStateType.PowerState getPowerState() throws NotAvailableException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).getPowerState();
    }

    @Override
    public PowerStateType.PowerState getPowerState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).getPowerState(unitType);
    }

    @Override
    public Future<Void> setStandbyState(StandbyStateType.StandbyState standbyState) throws CouldNotPerformException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(standbyState);
    }

    @Override
    public Future<Void> setStandbyState(StandbyStateType.StandbyState state, UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(state, unitType);
    }

    @Override
    public StandbyStateType.StandbyState getStandbyState() throws NotAvailableException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).getStandbyState();
    }

    @Override
    public StandbyStateType.StandbyState getStandbyState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).getStandbyState(unitType);
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureStateType.TemperatureState temperatureState) throws CouldNotPerformException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState);
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureStateType.TemperatureState temperatureState, UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState, unitType);
    }

    @Override
    public TemperatureStateType.TemperatureState getTargetTemperatureState() throws NotAvailableException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState();
    }

    @Override
    public TemperatureStateType.TemperatureState getTargetTemperatureState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState(unitType);
    }

    @Override
    public MotionStateType.MotionState getMotionState() throws NotAvailableException {
        return ((MotionStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.MOTION_STATE_SERVICE)).getMotionState();
    }

    @Override
    public MotionStateType.MotionState getMotionState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((MotionStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.MOTION_STATE_SERVICE)).getMotionState(unitType);
    }

    @Override
    public AlarmStateType.AlarmState getSmokeAlarmState() throws NotAvailableException {
        return ((SmokeAlarmStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState();
    }

    @Override
    public AlarmStateType.AlarmState getSmokeAlarmState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((SmokeAlarmStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState(unitType);
    }

    @Override
    public SmokeStateType.SmokeState getSmokeState() throws NotAvailableException {
        return ((SmokeStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_STATE_SERVICE)).getSmokeState();
    }

    @Override
    public SmokeStateType.SmokeState getSmokeState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((SmokeStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_STATE_SERVICE)).getSmokeState(unitType);
    }

    @Override
    public TemperatureStateType.TemperatureState getTemperatureState() throws NotAvailableException {
        return ((TemperatureStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState();
    }

    @Override
    public TemperatureStateType.TemperatureState getTemperatureState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((TemperatureStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState(unitType);
    }

    @Override
    public PowerConsumptionStateType.PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        return ((PowerConsumptionStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState();
    }

    @Override
    public PowerConsumptionStateType.PowerConsumptionState getPowerConsumptionState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((PowerConsumptionStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState(unitType);
    }

    @Override
    public TamperStateType.TamperState getTamperState() throws NotAvailableException {
        return ((TamperStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.TAMPER_STATE_SERVICE)).getTamperState();
    }

    @Override
    public TamperStateType.TamperState getTamperState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((TamperStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.TAMPER_STATE_SERVICE)).getTamperState(unitType);
    }

    @Override
    public IlluminanceStateType.IlluminanceState getIlluminanceState() throws NotAvailableException {
        return ((IlluminanceStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.ILLUMINANCE_STATE_SERVICE)).getIlluminanceState();
    }

    @Override
    public IlluminanceStateType.IlluminanceState getIlluminanceState(UnitTemplateType.UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((IlluminanceStateProviderServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.ILLUMINANCE_STATE_SERVICE)).getIlluminanceState(unitType);
    }

    @Override
    public Set<ServiceTemplateType.ServiceTemplate.ServiceType> getSupportedServiceTypes() throws NotAvailableException, InterruptedException {
        final Set<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypeSet = new HashSet<>();
        try {
            for (final ServiceTemplateType.ServiceTemplate serviceTemplate : getConfig().getUnitGroupConfig().getServiceTemplateList()) {
                serviceTypeSet.add(serviceTemplate.getType());
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SupportedServiceTypes", new CouldNotPerformException("Could not generate supported service type list!", ex));
        }
        return serviceTypeSet;
    }

    // END DEFAULT INTERFACE METHODS
    //////////
}
