/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.philips.PH_Hue_E27Controller;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.InstantiationException;
import java.awt.Color;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.states.PowerType;
import rst.vision.HSVColorType;

/**
 *
 * @author thuxohl
 */
public class AmbientLightRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Ambient_Light_Unit_Test";
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AmbientLightRemoteTest.class);
    
    private AmbientLightRemote ambientLightRemote;
    private DALService dalService;
    

    public AmbientLightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new DeviceInitializerImpl());
        dalService.activate();

        ambientLightRemote = new AmbientLightRemote();
        ambientLightRemote.init(LABEL, LOCATION);
        ambientLightRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            ambientLightRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate ambient light remote: ", ex);
        }
    }

    /**
     * Test of setColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetColor_Color() throws Exception {
        System.out.println("setColor");
        Color color = Color.MAGENTA;
        ambientLightRemote.setColor(color);
        while (!ambientLightRemote.getData().getColor().equals(color)) {
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", ambientLightRemote.getData().getColor().equals(color));
    }

    /**
     * Test of setColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 30000)
    public void testSetColor_HSVColorTypeHSVColor() throws Exception {
        System.out.println("setColor");
        HSVColorType.HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(50).setSaturation(50).setValue(50).build();
        ambientLightRemote.setColor(color);
        while (!ambientLightRemote.getData().getColor().equals(color)) {
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", ambientLightRemote.getData().getColor().equals(color));
    }
    
    /**
     * Test of getColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGSetColor() throws Exception {
        System.out.println("getColor");
        HSVColorType.HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(66).setSaturation(63).setValue(33).build();
        ambientLightRemote.setColor(color);
        while (!ambientLightRemote.getData().getColor().equals(color)) {
            Thread.yield();
        }
        assertTrue("Color has not been set in time or the return value from the getter is different!", ambientLightRemote.getColor().equals(color));
    }

    /**
     * Test of setPowerState method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerType.Power.PowerState state = PowerType.Power.PowerState.ON;
        ambientLightRemote.setPowerState(state);
        while (!ambientLightRemote.getData().getPowerState().equals(state)) {
            Thread.yield();
        }
        assertTrue("Power state has not been set in time!", ambientLightRemote.getData().getPowerState().equals(state));
    }
    
    /**
     * Test of getPowerState method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerType.Power.PowerState state = PowerType.Power.PowerState.OFF;
        ambientLightRemote.setPowerState(state);
        while (!ambientLightRemote.getData().getPowerState().equals(state)) {
            Thread.yield();
        }
        assertTrue("Power state has not been set in time or the return value from the getter is different!", ambientLightRemote.getPowerState().equals(state));
    }

    /**
     * Test of setBrightness method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 75d;
        ambientLightRemote.setBrightness(brightness);
        while (!(ambientLightRemote.getData().getColor().getValue() == brightness)) {
            Thread.yield();
        }
        assertTrue("Power state has not been set in time!", ambientLightRemote.getData().getColor().getValue() == brightness);
    }
    
    /**
     * Test of getBrightness method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        Double brightness = 25d;
        ambientLightRemote.setBrightness(brightness);
        while (!(ambientLightRemote.getData().getColor().getValue() == brightness)) {
            Thread.yield();
        }
        assertTrue("Brightness has not been set in time or the return value from the getter is different!", ambientLightRemote.getBrightness() == brightness);
    }

    /**
     * Test of notifyUpdated method, of class AmbientLightRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new PH_Hue_E27Controller("PH_Hue_E27_000", LABEL, LOCATION));
            } catch (InstantiationException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }

}