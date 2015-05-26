/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util.configgen;

import de.citec.dal.bindings.openhab.util.configgen.jp.JPOpenHABItemConfig;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.lm.remote.LocationRegistryRemote;
import java.io.File;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceRegistryType;
import rst.spatial.LocationRegistryType;

/**
 *
 * @author mpohling
 */
public class OpenHABConfigGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABConfigGenerator.class);

    private final OpenHABItemConfigGenerator itemConfigGenerator;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final LocationRegistryRemote locationRegistryRemote;

    public OpenHABConfigGenerator() throws InstantiationException {
        try {
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.itemConfigGenerator = new OpenHABItemConfigGenerator(deviceRegistryRemote, locationRegistryRemote);
            this.deviceRegistryRemote.addObserver(new Observer<DeviceRegistryType.DeviceRegistry>() {

                @Override
                public void update(Observable<DeviceRegistryType.DeviceRegistry> source, DeviceRegistryType.DeviceRegistry data) throws Exception {
                    try {
                        generate();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(logger, ex);
                    }
                }
            });
            this.locationRegistryRemote.addObserver(new Observer<LocationRegistryType.LocationRegistry>() {

                @Override
                public void update(Observable<LocationRegistryType.LocationRegistry> source, LocationRegistryType.LocationRegistry data) throws Exception {
                    try {
                        generate();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(logger, ex);
                    }
                }
            });

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void init() throws InitializationException, InterruptedException, CouldNotPerformException {
        logger.info("init");
        deviceRegistryRemote.init();
        deviceRegistryRemote.activate();
        locationRegistryRemote.init();
        locationRegistryRemote.activate();
        itemConfigGenerator.init();
    }

    private synchronized void generate() throws CouldNotPerformException {
        try {
            logger.info("generate");
            itemConfigGenerator.generate();

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate ex.", ex);
        }
    }

    private void shutdown() {
        logger.info("shutdown");
        try {
            itemConfigGenerator.generate();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(logger, ex);
        }
        deviceRegistryRemote.shutdown();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        JPService.setApplicationName("dal-openhab-config-generator");
        JPService.registerProperty(JPOpenHABItemConfig.class, new File("/tmp/itemconfig.txt"));
        JPService.parseAndExitOnError(args);

        try {
            final OpenHABConfigGenerator openHABConfigGenerator = new OpenHABConfigGenerator();
            openHABConfigGenerator.init();
            openHABConfigGenerator.generate();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                @Override
                public void run() {
                    openHABConfigGenerator.shutdown();
                }
            }));
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        }
    }
}
