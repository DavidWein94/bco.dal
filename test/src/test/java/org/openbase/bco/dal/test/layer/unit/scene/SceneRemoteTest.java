package org.openbase.bco.dal.test.layer.unit.scene;

/*
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.junit.*;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.scene.SceneManagerLauncher;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.service.ColorStateServiceRemote;
import org.openbase.bco.dal.remote.layer.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.LightRemote;
import org.openbase.bco.dal.remote.layer.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote;
import org.openbase.bco.dal.remote.layer.unit.unitgroup.UnitGroupRemote;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.scene.SceneConfigType.SceneConfig;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import org.openbase.type.language.LabelType.Label;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneRemoteTest extends AbstractBCOTest {

    public static final ActivationState ACTIVATE = ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();
    public static final String SCENE_TEST = "testScene";
    public static final String SCENE_ROOT_LOCATION = "locationTestScene";
    public static final String SCENE_ROOT_LOCATION_ALL_DEVICES_OFF = "locationDevicesOffTestScene";
    public static final String SCENE_ROOT_LOCATION_ALL_DEVICES_ON = "locationDevicesOnTestScene";
    public static final String SCENE_ROOT_LOCATION_OFF = "locationOffTestScene";
    public static final String SCENE_ROOT_LOCATION_ON = "locationOnTestScene";
    public static final String SCENE_GROUP = "GroupTriggerScene";
    public static final String COLORABLE_LIGHT_GROUP = "AllColorableLights";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SceneRemoteTest.class);
    private static final PowerState.State POWER_ON = PowerState.State.ON;
    private static final PowerState.State POWER_OFF = PowerState.State.OFF;
    private static final PowerState POWER_STATE_ON = PowerState.newBuilder().setValue(POWER_ON).build();
    private static final PowerState POWER_STATE_OFF = PowerState.newBuilder().setValue(POWER_OFF).build();
    private static final HSBColor COLOR_VALUE = HSBColor.newBuilder().setBrightness(100).setSaturation(90).setHue(10).build();
    private static final HSBColor GROUP_COLOR_VALUE = HSBColor.newBuilder().setBrightness(95).setSaturation(55).setHue(110).build();
    private static final double TEMPERATURE = 21.3;
    private static SceneManagerLauncher sceneManagerLauncher;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static LocationManagerLauncher locationManagerLauncher;
    private static PowerStateServiceRemote powerStateServiceRemote;
    private static ColorStateServiceRemote colorStateServiceRemote;
    final SyncObject LOCK = new SyncObject("waitForSceneExecution");
    final Observer notifyChangeObserver = (source, data) -> {
        synchronized (LOCK) {
            LOCK.notifyAll();
        }
    };

    public SceneRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOTest.setUpClass();
            JPService.setupJUnitTestMode();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();

            locationManagerLauncher = new LocationManagerLauncher();
            locationManagerLauncher.launch();
            locationManagerLauncher.getLaunchable();

            sceneManagerLauncher = new SceneManagerLauncher();
            sceneManagerLauncher.launch();

            powerStateServiceRemote = new PowerStateServiceRemote();
            colorStateServiceRemote = new ColorStateServiceRemote();

            registerScenes();

            powerStateServiceRemote.activate();
            colorStateServiceRemote.activate();
        } catch (JPServiceException | CouldNotPerformException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (sceneManagerLauncher != null) {
                sceneManagerLauncher.shutdown();
            }
            if (powerStateServiceRemote != null) {
                powerStateServiceRemote.shutdown();
            }
            if (colorStateServiceRemote != null) {
                colorStateServiceRemote.shutdown();
            }
            if (locationManagerLauncher != null) {
                locationManagerLauncher.shutdown();
            }
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }

            AbstractBCOTest.tearDownClass();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    private static void registerScenes() throws CouldNotPerformException {
        try {
            ServiceJSonProcessor serviceJSonProcessor = new ServiceJSonProcessor();

            List<ServiceStateDescription> serviceStateDescriptionList = new ArrayList<>();

            ServiceStateDescription.Builder serviceStateDescription = ServiceStateDescription.newBuilder();

            PowerState powerState = PowerState.newBuilder().setValue(POWER_ON).build();
            serviceStateDescription.setServiceType(ServiceType.POWER_STATE_SERVICE);
            serviceStateDescription.setServiceAttribute(serviceJSonProcessor.serialize(powerState));
            serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(powerState));
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
                if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (Registries.getTemplateRegistry().getSubUnitTypes(UnitType.LIGHT).contains(unitConfig.getUnitType()) || unitConfig.getUnitType() == UnitType.LIGHT || unitConfig.getUnitType() == UnitType.POWER_SWITCH) {
                    serviceStateDescription.clearUnitId();
                    serviceStateDescription.setUnitId(unitConfig.getId());
                    serviceStateDescriptionList.add(serviceStateDescription.build());
                    powerStateServiceRemote.init(unitConfig);
                }
            }

            ColorType.Color color = ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(COLOR_VALUE).build();
            ColorState colorState = ColorState.newBuilder().setColor(color).build();
            serviceStateDescription.setServiceType(ServiceType.COLOR_STATE_SERVICE);
            serviceStateDescription.setServiceAttribute(serviceJSonProcessor.serialize(colorState));
            serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(colorState));
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
                if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (unitConfig.getUnitType() == UnitType.COLORABLE_LIGHT) {
                    serviceStateDescription.clearUnitId();
                    serviceStateDescription.setUnitId(unitConfig.getId());
                    serviceStateDescriptionList.add(serviceStateDescription.build());
                    colorStateServiceRemote.init(unitConfig);
                }
            }

            String label = SCENE_TEST;
            PlacementConfig placementConfig = PlacementConfig.newBuilder().setLocationId(Registries.getUnitRegistry().getRootLocationConfig().getId()).build();
            SceneConfig sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            UnitConfig unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            serviceStateDescriptionList.clear();
            TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(TEMPERATURE).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();
            serviceStateDescription.setServiceType(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
            serviceStateDescription.setServiceAttribute(serviceJSonProcessor.serialize(temperatureState));
            serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(temperatureState));
            serviceStateDescription.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
            serviceStateDescriptionList.add(serviceStateDescription.build());

            label = SCENE_ROOT_LOCATION;
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ALL_DEVICES_ON;
            LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), false, LocationRemote.class);
            locationRemote.waitForData();
            locationRemote.setPowerState(POWER_ON).get();
            locationRemote.requestData().get();
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(locationRemote.recordSnapshot().get().getServiceStateDescriptionList()).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ALL_DEVICES_OFF;
            locationRemote.setPowerState(POWER_OFF).get();
            locationRemote.requestData().get();
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(locationRemote.recordSnapshot().get().getServiceStateDescriptionList()).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ON;
            serviceStateDescriptionList.clear();
            serviceStateDescription.clear();
            serviceStateDescription.setServiceType(ServiceType.POWER_STATE_SERVICE);
            serviceStateDescription.setServiceAttribute(serviceJSonProcessor.serialize(POWER_STATE_ON));
            serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(POWER_STATE_ON));
            serviceStateDescription.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
            serviceStateDescriptionList.add(serviceStateDescription.build());
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_OFF;
            serviceStateDescriptionList.clear();
            serviceStateDescription.clear();
            serviceStateDescription.setServiceType(ServiceType.POWER_STATE_SERVICE);
            serviceStateDescription.setServiceAttribute(serviceJSonProcessor.serialize(POWER_STATE_OFF));
            serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(POWER_STATE_OFF));
            serviceStateDescription.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
            serviceStateDescriptionList.add(serviceStateDescription.build());
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            // Register Scene which changes a unitGroup
            String unitGroupId = registerUnitGroup();
            color = ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(GROUP_COLOR_VALUE).build();
            colorState = ColorState.newBuilder().setColor(color).build();

            label = SCENE_GROUP;
            serviceStateDescriptionList.clear();
            serviceStateDescription.clear();
            serviceStateDescription.setServiceType(ServiceType.COLOR_STATE_SERVICE);
            serviceStateDescription.setServiceAttribute(serviceJSonProcessor.serialize(colorState));
            serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(colorState));
            serviceStateDescription.setUnitId(unitGroupId);
            serviceStateDescriptionList.add(serviceStateDescription.build());
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not register scene!", ex);
        }
    }

    private static String registerUnitGroup() throws CouldNotPerformException {
        try {
            UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitType.UNIT_GROUP).setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, COLORABLE_LIGHT_GROUP));
            UnitGroupConfig.Builder unitGroup = unitConfig.getUnitGroupConfigBuilder();

            unitGroup.setUnitType(UnitType.COLORABLE_LIGHT);
            for (UnitConfig unit : Registries.getUnitRegistry().getUnitConfigs(UnitType.COLORABLE_LIGHT)) {
                // filter disabled units
                if (unit.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }
                unitGroup.addMemberId(unit.getId());
            }

            return Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get().getId();
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not register unit groups!", ex);
        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test triggering a scene per remote.
     *
     * @throws Exception
     */
    @Test(timeout = 30000)
    public void testTriggerScenePerRemote() throws Exception {
        System.out.println("testTriggerScenePerRemote");

        LOGGER.warn("Await scene execution....");
        SceneRemote sceneRemote = Units.getUnitsByLabel(SCENE_TEST, true, Units.SCENE).get(0);
        Actions.waitForExecution(sceneRemote.setActivationState(State.ACTIVE));
        LOGGER.warn("Scene executed....");

        powerStateServiceRemote.requestData().get();
        colorStateServiceRemote.requestData().get();

        assertEquals("PowerState has not been updated by scene!", POWER_ON, powerStateServiceRemote.getPowerState().getValue());
        // the colorStateServiceRemote computes an average in the rgb space which is why the values have to be compared with a tolerance
        assertEquals("Brightness has not been updated by scene!", COLOR_VALUE.getBrightness(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getBrightness(), 0.5);
        assertEquals("Hue has not been updated by scene!", COLOR_VALUE.getHue(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getHue(), 0.5);
        assertEquals("Saturation has not been updated by scene!", COLOR_VALUE.getSaturation(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getSaturation(), 0.5);
    }

    /**
     * Test triggering a scene with an action regarding a location per remote.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testTriggerSceneWithLocationActionPerRemote() throws Exception {
        System.out.println("testTriggerSceneWithLocationActionPerRemote");

        LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, LocationRemote.class);
        assertTrue("LocationState has the correct temperature to begin with!", locationRemote.getTargetTemperatureState().getTemperature() != TEMPERATURE);

        activateScene(SCENE_ROOT_LOCATION);
        locationRemote.requestData().get();

        while (locationRemote.getTargetTemperatureState().getTemperature() != TEMPERATURE) {
            System.out.println("locationTemperature[" + locationRemote.getTargetTemperatureState().getTemperature() + "] differs!");
            Thread.sleep(50);
        }

        assertEquals("TemperatureState has not been updated in location by scene!", TEMPERATURE, locationRemote.getTargetTemperatureState().getTemperature(), 0.1);
    }

    /**
     * Test triggering a unit group with a scene.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testTriggerUnitGroupByScene() throws Exception {
        System.out.println("testTriggerUnitGroupByScene");

        UnitGroupRemote unitGroupRemote = Units.getUnitsByLabel(COLORABLE_LIGHT_GROUP, true, UnitGroupRemote.class).get(0);

        List<ColorableLightRemote> colorableLightRemotes = new ArrayList<>();
        for (String memberId : unitGroupRemote.getConfig().getUnitGroupConfig().getMemberIdList()) {
            colorableLightRemotes.add(Units.getUnit(memberId, true, ColorableLightRemote.class));
        }
        activateScene(SCENE_GROUP);

        for (ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
            assertEquals("ColorState has not been set for light[" + colorableLightRemote.getLabel() + "]", GROUP_COLOR_VALUE, colorableLightRemote.getColorState().getColor().getHsbColor());
        }
        // for the group the values can be slighlty modified
        assertEquals("Brightness in unitGroupRemote has not been set", GROUP_COLOR_VALUE.getBrightness(), unitGroupRemote.getColorState().getColor().getHsbColor().getBrightness(), 1.0);
        assertEquals("Hue in unitGroupRemote has not been set", GROUP_COLOR_VALUE.getHue(), unitGroupRemote.getColorState().getColor().getHsbColor().getHue(), 1.0);
        assertEquals("Saturation in unitGroupRemote has not been set", GROUP_COLOR_VALUE.getSaturation(), unitGroupRemote.getColorState().getColor().getHsbColor().getSaturation(), 1.0);
    }

    /**
     * Test triggering a scene with an action regarding a location per remote and check if the change has affected units within the location.
     *
     * @throws Exception
     */
    @Test(timeout = 30000)
    public void testTriggerSceneWithAllDevicesOfLocationActionPerRemoteAndVerifiesUnitModification() throws Exception {
        System.out.println("testTriggerSceneWithLocationActionPerRemoteAndVerifiesUnitModification");

        LightRemote internalLight = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.LIGHT), true, Units.LIGHT);
        PowerSwitchRemote internalPowerSwitch = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.POWER_SWITCH), true, Units.POWER_SWITCH);

        internalLight.setPowerState(POWER_ON).get();
        internalPowerSwitch.setPowerState(POWER_ON).get();

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
        assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);

        LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, LocationRemote.class);
        locationRemote.setPowerState(POWER_OFF).get();

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

        int TEST_ITERATIONS = 3;
        for (int i = 0; i <= TEST_ITERATIONS; i++) {
            activateScene(SCENE_ROOT_LOCATION_ALL_DEVICES_ON);
            while (locationRemote.getPowerState().getValue() != POWER_ON) {
                Thread.sleep(100);
                locationRemote.requestData();
            }
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
            assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);
            Thread.sleep(100);
            activateScene(SCENE_ROOT_LOCATION_ALL_DEVICES_OFF);
            while (locationRemote.getPowerState().getValue() != POWER_OFF) {
                System.out.println("location was not yet switched " + POWER_OFF);
                Thread.sleep(100);
                locationRemote.requestData();
            }
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
            assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

            System.out.println("=== " + (int) (((double) i / (double) TEST_ITERATIONS) * 100d) + "% passed with iteration " + i + " of location on off test.");
            Thread.sleep(1000);
        }
    }

    /**
     * Test triggering a scene with an action regarding a location per remote and check if the change has affected units within the location.
     *
     * @throws Exception
     */
    @Test(timeout = 30000)
    public void testTriggerSceneWithLocationActionPerRemoteAndVerifiesUnitModification() throws Exception {
        System.out.println("testTriggerSceneWithLocationActionPerRemoteAndVerifiesUnitModification");

        LightRemote internalLight = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.LIGHT), true, Units.LIGHT);
        PowerSwitchRemote internalPowerSwitch = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.POWER_SWITCH), true, Units.POWER_SWITCH);

        internalLight.setPowerState(POWER_ON).get();
        internalPowerSwitch.setPowerState(POWER_ON).get();

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_ON);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);

        LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, LocationRemote.class);
        locationRemote.setPowerState(POWER_OFF).get();

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

        int TEST_ITERATIONS = 3;
        for (int i = 0; i <= TEST_ITERATIONS; i++) {
            System.out.println("Current iteration: " + i);
            activateScene(SCENE_ROOT_LOCATION_ON);
            while (locationRemote.getPowerState().getValue() != POWER_ON) {
                System.out.println("location was not yet switched " + POWER_ON);
                Thread.sleep(100);
                locationRemote.requestData();
            }
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
            assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);
            Thread.sleep(100);
            activateScene(SCENE_ROOT_LOCATION_OFF);
            while (locationRemote.getPowerState().getValue() != POWER_OFF) {
                System.out.println("location was not yet switched " + POWER_OFF);
                Thread.sleep(100);
                locationRemote.requestData();
            }
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
            assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

            System.out.println("=== " + (int) (((double) i / (double) TEST_ITERATIONS) * 100d) + "% passed with iteration " + i + " of location on off test.");
            Thread.sleep(1000);
        }
    }

    public void activateScene(final String sceneLabel) throws CouldNotPerformException, InterruptedException, ExecutionException {
        SceneRemote sceneRemote = Units.getUnitsByLabel(sceneLabel, true, Units.SCENE).get(0);
        sceneRemote.addDataObserver(notifyChangeObserver);
        waitForSceneExecution(sceneRemote.setActivationState(ACTIVATE));
        //waitForSceneExecution(sceneRemote.setActivationState(State.DEACTIVE));
        sceneRemote.requestData().get();
//        assertEquals("Scene has not been deactivated after execution!", ActivationState.State.DEACTIVE, sceneRemote.getActivationState().getValue());
        sceneRemote.removeDataObserver(notifyChangeObserver);
    }

    private void waitForSceneExecution(final Future<ActionDescription> actionFuture) throws CouldNotPerformException {
        try {
            new RemoteAction(actionFuture).waitUntilDone();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
