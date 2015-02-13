/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.hager.HA_TYA628CController;
import de.citec.dal.hal.unit.RollershutterController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.states.ShutterType;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Rollershutter_Unit_Test";
    private static final String[] ROLLERSHUTTER = {"Rollershutter_1", "Rollershutter_2", "Rollershutter_3", "Rollershutter_4", "Rollershutter_5", "Rollershutter_6", "Rollershutter_7", "Rollershutter_8"};

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RollershutterRemoteTest.class);

    private static RollershutterRemote rollershutterRemote;
    private static DALService dalService;

    public RollershutterRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new RollershutterRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        rollershutterRemote = new RollershutterRemote();
        rollershutterRemote.init(ROLLERSHUTTER[ROLLERSHUTTER.length-1], LOCATION);
        rollershutterRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() {
        dalService.deactivate();
        try {
            rollershutterRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate rollershutter remote: ", ex);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetShutterState() throws Exception {
        System.out.println("setShutterState");
        ShutterType.Shutter.ShutterState state = ShutterType.Shutter.ShutterState.DOWN;
        rollershutterRemote.setShutter(state);
        while (true) {
            try {
                if (rollershutterRemote.getData().getShutterState().getState().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", rollershutterRemote.getData().getShutterState().getState().equals(state));
    }

    /**
     * Test of getShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetShutterState() throws Exception {
        System.out.println("getShutterState");
        ShutterType.Shutter.ShutterState state = ShutterType.Shutter.ShutterState.STOP;
        ((RollershutterController) dalService.getRegistry().getUnits(RollershutterController.class).iterator().next()).updateShutter(state);
        while (true) {
            try {
                if (rollershutterRemote.getShutter().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", rollershutterRemote.getShutter().equals(state));
    }

    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetOpeningRatio() throws Exception {
        System.out.println("setOpeningRatio");
        double openingRatio = 34.0D;
        rollershutterRemote.setOpeningRatio(openingRatio);
        while (true) {
            try {
                if (rollershutterRemote.getData().getOpeningRatio() == openingRatio) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", rollershutterRemote.getData().getOpeningRatio() == openingRatio);
    }

    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetOpeningRatio() throws Exception {
        System.out.println("getOpeningRatio");
        double openingRatio = 70.0D;
        ((RollershutterController) dalService.getRegistry().getUnits(RollershutterController.class).iterator().next()).updateOpeningRatio((float)openingRatio);
        while (true) {
            try {
                if (rollershutterRemote.getOpeningRatio() == openingRatio) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", rollershutterRemote.getOpeningRatio() == openingRatio);
    }

    /**
     * Test of notifyUpdated method, of class RollershutterRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new HA_TYA628CController("HA_TYA628C_000", LABEL, ROLLERSHUTTER, LOCATION));
            } catch (InstantiationException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
