package org.openbase.bco.dal.test.layer.unit;

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
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.layer.unit.DimmableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmableLightRemoteTest extends AbstractBCODeviceManagerTest {

    private static DimmableLightRemote dimmableLightRemote;

    public DimmableLightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        dimmableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.DIMMABLE_LIGHT), true, DimmableLightRemote.class);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class DimmerRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of setPower method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetPower() throws Exception {
        System.out.println("setPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        Actions.waitForExecution(dimmableLightRemote.setPowerState(state));
        assertEquals("Power has not been set in time!", state.getValue(), dimmableLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPower method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPower() throws Exception {
        System.out.println("getPowerState");
        final PowerState.Builder powerStateBuilder = PowerState.newBuilder().setValue(State.ON);
        powerStateBuilder.setTimestamp(TimestampProcessor.getCurrentTimestamp());
        final ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(powerStateBuilder.build(), ServiceType.POWER_STATE_SERVICE, dimmableLightRemote);
        actionParameter.setInterruptible(false);
        actionParameter.setSchedulable(false);
        actionParameter.setExecutionTimePeriod(TimeUnit.MINUTES.toMicros(15));
        powerStateBuilder.setResponsibleAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameter));
        final PowerState powerState = powerStateBuilder.build();

        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dimmableLightRemote.getId()).applyDataUpdate(powerState, ServiceType.POWER_STATE_SERVICE);
        dimmableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", powerState.getValue(), dimmableLightRemote.getPowerState().getValue());
    }

    /**
     * Test of setDimm method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 66d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        Actions.waitForExecution(dimmableLightRemote.setBrightnessState(brightnessState));
        assertEquals("Brightness has not been set in time!", brightness, dimmableLightRemote.getBrightnessState().getBrightness(), 0.1);
    }

    /**
     * Test of getBrightness method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");

        final Double brightness = 70.0d;
        final BrightnessState.Builder brightnessStateBuilder = BrightnessState.newBuilder().setBrightness(brightness);
        brightnessStateBuilder.setTimestamp(TimestampProcessor.getCurrentTimestamp());
        final ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(brightnessStateBuilder.build(), ServiceType.BRIGHTNESS_STATE_SERVICE, dimmableLightRemote);
        actionParameter.setInterruptible(false);
        actionParameter.setSchedulable(false);
        actionParameter.setExecutionTimePeriod(TimeUnit.MINUTES.toMicros(15));
        brightnessStateBuilder.setResponsibleAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameter));
        final BrightnessState brightnessState = brightnessStateBuilder.build();

        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dimmableLightRemote.getId()).applyDataUpdate(brightnessState, ServiceType.BRIGHTNESS_STATE_SERVICE);
        dimmableLightRemote.requestData().get();
        assertEquals("Brightness has not been set in time!", brightnessState.getBrightness(), dimmableLightRemote.getBrightnessState().getBrightness(), 0.1);
    }
}
