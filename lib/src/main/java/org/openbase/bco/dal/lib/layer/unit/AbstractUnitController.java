package org.openbase.bco.dal.lib.layer.unit;

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
import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import java.util.Map.Entry;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.service.ServiceFactoryProvider;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.processing.StringProcessor;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.control.action.ActionConfigType.ActionConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractConfigurableController<M, MB, UnitConfig> implements UnitController, ServiceFactoryProvider {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionConfig.getDefaultInstance()));
    }


    private final UnitHost unitHost;
    private final List<Service> serviceList;
    private final ServiceFactory serviceFactory;
    private UnitTemplate template;

    public AbstractUnitController(final Class unitClass, final UnitHost unitHost, final MB builder) throws CouldNotPerformException {
        super(builder);
        try {

            if (unitHost.getServiceFactory() == null) {
                throw new NotAvailableException("service factory");
            }

            this.unitHost = unitHost;
            this.serviceFactory = unitHost.getServiceFactory();
            this.serviceList = new ArrayList<>();

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(Scope scope) throws InitializationException, InterruptedException {
        try {
            init(ScopeTransformer.transform(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void init(ScopeType.Scope scope) throws InitializationException, InterruptedException {
        try {
            super.init(unitHost.getDeviceRegistry().getUnitConfigByScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init(final String label, final ScopeProvider location) throws InitializationException, InterruptedException {
        try {
            init(ScopeGenerator.generateScope(label, getClass().getSimpleName(), location.getScope()));
        } catch (CouldNotPerformException | NullPointerException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            if (config == null) {
                throw new NotAvailableException("config");
            }

            if (!config.hasId()) {
                throw new NotAvailableException("config.id");
            }

            if (config.getId().isEmpty()) {
                throw new NotAvailableException("Field config.id is empty!");
            }

            if (!config.hasLabel()) {
                throw new NotAvailableException("config.label");
            }

            if (config.getLabel().isEmpty()) {
                throw new NotAvailableException("Field config.label is emty!");
            }

            super.init(config);
            try {
                verifyUnitConfig();
            } catch (VerificationFailedException ex) {
                ExceptionPrinter.printHistory(new InvalidStateException(this + " is not valid!", ex), logger);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        template = CachedDeviceRegistryRemote.getRegistry().getUnitTemplateByType(config.getType());
        return super.applyConfigUpdate(config);
    }

    @Override
    public final String getId() throws NotAvailableException {
        try {
            UnitConfig tmpConfig = getConfig();
            if (!tmpConfig.hasId()) {
                throw new NotAvailableException("unitconfig.id");
            }

            if (tmpConfig.getId().isEmpty()) {
                throw new InvalidStateException("unitconfig.id is empty");
            }

            return tmpConfig.getId();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("id", ex);
        }
    }

    @Override
    public String getLabel() throws NotAvailableException {
        try {
            UnitConfig tmpConfig = getConfig();
            if (!tmpConfig.hasId()) {
                throw new NotAvailableException("unitconfig.label");
            }

            if (tmpConfig.getId().isEmpty()) {
                throw new InvalidStateException("unitconfig.label is empty");
            }
            return getConfig().getLabel();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("label", ex);
        }
    }

    @Override
    public UnitTemplate.UnitType getType() throws NotAvailableException {
        return getConfig().getType();
    }

    @Override
    public UnitTemplate getTemplate() throws NotAvailableException {
        if (template == null) {
            throw new NotAvailableException("UnitTemplate");
        }
        return template;
    }

    public UnitHost getUnitHost() {
        return unitHost;
    }

    public Collection<Service> getServices() {
        return Collections.unmodifiableList(serviceList);
    }

    public void registerService(final Service service) {
        serviceList.add(service);
    }

    @Override
    public void registerMethods(RSBLocalServerInterface server) throws CouldNotPerformException {

        // collect service interface methods
        HashMap<String, ServiceTemplate.ServiceType> serviceInterfaceMap = new HashMap<>();
        for (ServiceTemplate.ServiceType serviceType : getTemplate().getServiceTypeList()) {
            serviceInterfaceMap.put(StringProcessor.transformUpperCaseToCamelCase(serviceType.name()), serviceType);
        }

        for (Entry<String, ServiceTemplate.ServiceType> serviceInterfaceMapEntry : serviceInterfaceMap.entrySet()) {
            Class<? extends Service> serviceInterfaceClass = null;

            try {
                // Identify package
                Package servicePackage;
                if (serviceInterfaceMapEntry.getKey().contains(Service.CONSUMER_SERVICE_LABEL)) {
                    servicePackage = ConsumerService.class.getPackage();
//                } else if (serviceInterfaceMapEntry.getKey().contains(Service.OPERATION_SERVICE_LABEL)) {
//                    servicePackage = OperationService.class.getPackage();
//                } else if (serviceInterfaceMapEntry.getKey().contains(Service.PROVIDER_SERVICE_LABEL)) {
//                    servicePackage = ProviderService.class.getPackage();
                } else if (serviceInterfaceMapEntry.getKey().contains("Service")) {
                    servicePackage = OperationService.class.getPackage();
                } else if (serviceInterfaceMapEntry.getKey().contains("Provider")) {
                    servicePackage = ProviderService.class.getPackage();
                } else {
                    throw new NotSupportedException(serviceInterfaceMapEntry.getKey(), this);
                }

                // Identify interface class
                try {
                    if (servicePackage.equals(ProviderService.class.getPackage())) {
                        serviceInterfaceClass = (Class<? extends Service>) Class.forName(servicePackage.getName() + "." + serviceInterfaceMapEntry.getKey() + "Service");
                    } else if (servicePackage.equals(OperationService.class.getPackage())) {
                        serviceInterfaceClass = (Class<? extends Service>) Class.forName(servicePackage.getName() + "." + serviceInterfaceMapEntry.getKey().replaceAll("Service", "") + "OperationService");
                    }
                    if (serviceInterfaceClass == null) {
                        throw new NotAvailableException(serviceInterfaceMapEntry.getKey());
                    }
                } catch (ClassNotFoundException | ClassCastException ex) {
                    throw new CouldNotPerformException("Could not load service interface!", ex);
                }

                if (!serviceInterfaceClass.isAssignableFrom(this.getClass())) {
                    // interface not supported dummy.
                }

                try {
                    Class<? extends Service> asSubclass = getClass().asSubclass(serviceInterfaceClass);
                } catch (ClassCastException ex) {
                    throw new CouldNotPerformException("Could not register methods for serviceInterface [" + serviceInterfaceClass.getName() + "]", ex);
                }

//                RPCHelper.registerServiceInterface(serviceInterfaceClass, this, server);
                RPCHelper.registerInterface((Class) serviceInterfaceClass, this, server);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register Interface[" + serviceInterfaceClass + "] Method [" + serviceInterfaceMapEntry.getKey() + "] for Unit[" + this.getLabel() + "].", ex), logger);
            }
        }
//        for (ServiceType serviceType : ServiceType.getServiceTypeList(this)) {
//            for (Method method : serviceType.getDeclaredMethods()) {
//                try {
//                    server.addMethod(method.getName(), getCallback(method, this, serviceType));
//                } catch (CouldNotPerformException ex) {
//                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register callback for service methode " + method.toGenericString(), ex), logger);
//                }
//            }
//        }
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        return serviceFactory;
    }

    @Override
    public void applyUpdate(ServiceTemplate.ServiceType serviceType, Object serviceArgument) throws CouldNotPerformException {
        try {

            if (serviceArgument == null) {
                throw new NotAvailableException("ServiceArgument");
            }

            final Method updateMethod = getUpdateMethod(serviceType, serviceArgument.getClass());

            try {
                updateMethod.invoke(this, serviceArgument);
            } catch (IllegalAccessException ex) {
                throw new CouldNotPerformException("Cannot access related Method [" + updateMethod.getName() + "]", ex);
            } catch (IllegalArgumentException ex) {
                throw new CouldNotPerformException("Does not match [" + updateMethod.getParameterTypes()[0].getName() + "] which is needed by [" + updateMethod.getName() + "]!", ex);
            } catch (InvocationTargetException ex) {
                throw new CouldNotPerformException("The related method [" + updateMethod.getName() + "] throws an exceptioin during invocation!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply " + serviceType.name() + " Update[" + serviceArgument + "] for Unit[" + getLabel() + "]!", ex);
        }
    }

    @Override
    public Method getUpdateMethod(final ServiceTemplate.ServiceType serviceType, Class serviceArgumentClass) throws CouldNotPerformException {
        try {
            Method updateMethod;
            String updateMethodName = ProviderService.getUpdateMethodName(serviceType);
            try {
                updateMethod = getClass().getMethod(updateMethodName, serviceArgumentClass);
                if (updateMethod == null) {
                    throw new NotAvailableException(updateMethod);
                }
            } catch (NoSuchMethodException | SecurityException | NotAvailableException ex) {
                throw new NotAvailableException("Method " + this + "." + updateMethodName + "(" + serviceArgumentClass + ")", ex);
            }
            return updateMethod;
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Unit not compatible!", ex);
        }
    }

    /**
     * Verify if all provider service update methods are registered for given configuration.
     *
     * @throws VerificationFailedException is thrown if the check fails or at least on update method is not available.
     */
    private void verifyUnitConfig() throws VerificationFailedException {
        try {
            logger.debug("Validating unit update methods...");

            MultiException.ExceptionStack exceptionStack = null;
            List<String> unitMethods = new ArrayList<>();
            String updateMethod;

            // === Load unit methods. ===
            for (Method medhod : getClass().getMethods()) {
                unitMethods.add(medhod.getName());
            }

            // === Verify if all update methods are registered. ===
            for (ServiceTemplate.ServiceType service : getTemplate().getServiceTypeList()) {

                // TODO: replace by service type filer if availbale.
                // filter other services than provider
                if (!service.name().contains("Provider")) {
                    continue;
                }

                // verify
                updateMethod = ProviderService.getUpdateMethodName(service);
                if (!unitMethods.contains(updateMethod)) {
                    exceptionStack = MultiException.push(service, new NotAvailableException("Method", updateMethod), exceptionStack);
                }
            }

            // === throw multi exception in error case. ===
            MultiException.checkAndThrow("At least one update method missing!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException("UnitTemplate is not compatible for configured unit controller!", ex);
        }
    }

    @Override
    public Future<Void> applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        try {
            logger.info("applyAction: "+actionConfig.getLabel());
            Object attribute = ServiceJSonProcessor.deserialize(actionConfig.getServiceAttribute(), actionConfig.getServiceAttributeType());
            Service.invokeServiceMethod(actionConfig.getServiceType(), this, attribute);
            return CompletableFuture.completedFuture(null); // TODO Should be asynchron!
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        }
    }

    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + "[" + getConfig().getType() + "[" + getLabel() + "]]";
        } catch (NotAvailableException e) {
            return getClass().getSimpleName() + "[?]";
        }
    }
}