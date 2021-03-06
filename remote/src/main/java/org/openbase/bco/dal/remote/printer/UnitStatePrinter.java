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
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.DefaultInitializable;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.io.PrintStream;
import java.util.List;

public class UnitStatePrinter implements DefaultInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitStatePrinter.class);

    private final CustomUnitPool customUnitPool;
    private final Observer<ServiceStateProvider<Message>, Message> unitStateObserver;
    private final PrintStream printStream;
    private final Consumer<String> outputConsumer;
    private boolean headerPrinted = false;

    public UnitStatePrinter(final PrintStream printStream, final Filter<UnitConfig>... filters) throws InstantiationException {
        try {
            this.outputConsumer = null;
            this.printStream = printStream;
            this.customUnitPool = new CustomUnitPool(filters);
            this.unitStateObserver = (source, data) -> print((Unit) source.getServiceProvider(), source.getServiceType(), data);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public UnitStatePrinter(final Consumer<String> outputConsumer, final Filter<UnitConfig>... filters) throws InstantiationException {
        try {
            this.outputConsumer = outputConsumer;
            this.printStream = null;
            this.customUnitPool = new CustomUnitPool(filters);
            this.unitStateObserver = (source, data) -> print((Unit) source.getServiceProvider(), source.getServiceType(), data);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            // print initial unit states
            for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs()) {
                final UnitRemote<?> unit = Units.getUnit(unitConfig, true);

                try {
                    for (ServiceDescription serviceDescription : unit.getUnitTemplate().getServiceDescriptionList()) {

                        if(serviceDescription.getPattern() != ServicePattern.PROVIDER) {
                            continue;
                        }
                        print(unit, serviceDescription.getServiceType(), Services.invokeProviderServiceMethod(serviceDescription.getServiceType(), ServiceTempus.CURRENT, unit));
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not print " + unit, ex, LOGGER);
                }
            }

            customUnitPool.init();
            customUnitPool.addObserver(unitStateObserver);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void print(Unit<?> unit, Message data) {
        try {
            for (ServiceDescription serviceDescription : unit.getUnitTemplate().getServiceDescriptionList()) {
                print(unit, serviceDescription.getServiceType(), data);
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not print " + unit, ex, LOGGER);
        }
    }


    private void print(Unit<?> unit, ServiceType serviceType, Message serviceState) {

        // print header
        if (printStream != null && !headerPrinted) {
            headerPrinted = true;
            printStream.println("/**\n" +
                    " * Service State Transitions\n" +
                    " * --> syntax: transition(unit_id, unit_alias, unit_type, initiator[system/user], service_type, timestamp, service_value_type=service_value).\n" +
                    " */");
        }
        try {
            String initiator;
            try {
                initiator = Services.getResponsibleAction(serviceState).getActionInitiator().getInitiatorType().name().toLowerCase();
            } catch (NotAvailableException ex) {
                // in this case we use the system as initiator because responsible actions are not available for pure provider services and those are always system generated.
                initiator = "system";
            }

            final List<String> states = Services.generateServiceStateStringRepresentation(serviceState, serviceType);
//            if (!states.isEmpty()) {
//                printStream.println("===========================================================================================================");
//            }
            for (String extractServiceState : states) {

                final String transition = "transition('" + unit.getId() + "', '" + unit.getConfig().getAlias(0) + "', " + unit.getUnitType().name().toLowerCase() + ", " + initiator + ", " + extractServiceState + ").";
                if (printStream != null) {
                    printStream.println(transition);
                }

                if (outputConsumer != null) {
                    outputConsumer.consume(transition);
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not print " + serviceType.name() + " of " + unit, ex, LOGGER);
        }
    }
}
