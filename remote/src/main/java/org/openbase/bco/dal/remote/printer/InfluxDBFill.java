package org.openbase.bco.dal.remote.printer;

/*-
 * #%L
 * BCO DAL Remote
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

import com.google.protobuf.Message;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.printer.jp.JPOutputDirectory;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.jp.JPRSBHost;
import org.openbase.jul.extension.rsb.com.jp.JPRSBPort;
import org.openbase.jul.extension.rsb.com.jp.JPRSBTransport;
import org.openbase.jul.iface.DefaultInitializable;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.Observer;
import org.openbase.type.domotic.service.ServiceTempusTypeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.Map;
import java.util.concurrent.TimeUnit;


public class InfluxDBFill implements DefaultInitializable {
    public static final String APP_NAME = InfluxDBFill.class.getSimpleName();
    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxDBFill.class);
    private final CustomUnitPool customUnitPool;
    private final Observer<ServiceStateProvider<Message>, Message> unitStateObserver;
    private static String databaseURL = "http://localhost:8086";
    private static String userName = "test";
    private static String password = "test";
    private static String dbName = "bco";
    private final InfluxDB influxDB;


    public InfluxDBFill(final Filter<UnitConfig>... filters) throws InstantiationException {
        try {

            this.influxDB = InfluxDBFactory.connect(databaseURL, userName, password);
            this.influxDB.setLogLevel(InfluxDB.LogLevel.BASIC);
            if (!this.influxDB.describeDatabases().contains(dbName)) {
                this.influxDB.createDatabase(dbName);

            }
            this.influxDB.createRetentionPolicy(
                    "defaultPolicy", dbName, "90d", 1, true);
            this.influxDB.enableBatch(100, 1000, TimeUnit.MILLISECONDS);
            this.influxDB.setRetentionPolicy("defaultPolicy");
            this.influxDB.setDatabase(dbName);
            Pong response = this.influxDB.ping();
            if (response.getVersion().equalsIgnoreCase("unknown")) {
                LOGGER.error("Error pinging server.");

                System.exit(1);
            }

            this.customUnitPool = new CustomUnitPool(filters);
            this.unitStateObserver = (source, data) -> saveInDB((Unit) source.getServiceProvider(), source.getServiceType(), data);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public static void main(String[] args) throws InstantiationException, InterruptedException, InitializationException {
        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.registerProperty(JPOutputDirectory.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
        JPService.registerProperty(JPRSBHost.class);
        JPService.registerProperty(JPRSBPort.class);
        JPService.registerProperty(JPRSBTransport.class);

        try {
            JPService.parseAndExitOnError(args);
        } catch (IllegalStateException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            LOGGER.info(APP_NAME + " finished unexpected.");
        }


        LOGGER.info("Start " + APP_NAME + "...");

        try {
            BCOLogin.autoLogin(true);
            new InfluxDBFill().init();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        }
        LOGGER.info(APP_NAME + " successfully started.");
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            // saveInDB initial unit states
            for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs()) {
                final UnitRemote<?> unit = Units.getUnit(unitConfig, true);

                try {
                    for (ServiceDescription serviceDescription : unit.getUnitTemplate().getServiceDescriptionList()) {

                        if (serviceDescription.getPattern() != ServicePattern.PROVIDER) {
                            continue;
                        }
                        saveInDB(unit, serviceDescription.getServiceType(), Services.invokeProviderServiceMethod(serviceDescription.getServiceType(), ServiceTempusTypeType.ServiceTempusType.ServiceTempus.CURRENT, unit));
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not saveInDB " + unit, ex, LOGGER);
                }
            }

            customUnitPool.init();
            customUnitPool.addObserver(unitStateObserver);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }


    private void saveInDB(Unit<?> unit, ServiceTemplate.ServiceType serviceType, Message serviceState) {
        try {
            String initiator;
            try {
                initiator = Services.getResponsibleAction(serviceState).getActionInitiator().getInitiatorType().name().toLowerCase();
            } catch (NotAvailableException ex) {
                // in this case we use the system as initiator because responsible actions are not available for pure provider services and those are always system generated.
                initiator = "system";
            }
            Map<String, String> stateValuesMap = Services.resolveStateValueToMap(serviceState);
            Point.Builder builder = Point.measurement(serviceType.toString().toLowerCase()).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("alias", unit.getConfig().getAlias(0))
                    .addField("initiator", initiator)
                    .addField("unitId", unit.getId())
                    .addField("unitType", unit.getUnitType().name().toLowerCase());


            for (Map.Entry<String, String> entry : stateValuesMap.entrySet()) {
                if (entry.getValue().matches("-?\\d+(\\.\\d+)?")) {
                    builder.addField(entry.getKey(), Double.valueOf(entry.getValue()));

                } else {
                    builder.addField(entry.getKey(), entry.getValue());
                }
            }
            Point point = builder.build();
            this.influxDB.write(point);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not saveInDB " + serviceType.name() + " of " + unit, ex, LOGGER);
        }
    }


}
