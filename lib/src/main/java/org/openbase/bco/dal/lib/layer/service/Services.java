package org.openbase.bco.dal.lib.layer.service;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ProtocolMessageEnum;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.openbase.bco.dal.lib.layer.service.Service.SERVICE_STATE_PACKAGE;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @author <a href="mailto:agatting@techfak.uni-bielefeld.de">Andreas Gatting</a>
 */
public class Services extends ServiceStateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Services.class);

    /**
     * This method returns the service base name of the given service type.
     * <p>
     * The base name is the service name without service suffix.
     * e.g. the base name of service PowerStateService is PowerState.
     *
     * @param serviceType the service type to extract the base name.
     *
     * @return the service base name.
     */
    public static String getServiceBaseName(ServiceType serviceType) {
        return StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll(Service.SERVICE_LABEL, "");
    }

    public static String getServiceMethodPrefix(final ServicePattern pattern) throws CouldNotPerformException {
        switch (pattern) {
            case CONSUMER:
                return "";
            case OPERATION:
                return "set";
            case PROVIDER:
                return "get";
            default:
                throw new NotSupportedException(pattern, Services.class);
        }
    }

    /**
     * Method returns the state name of the appurtenant service.
     *
     * @param serviceType the service type which is used to generate the service name.
     *
     * @return The state type name as string.
     *
     * @throws org.openbase.jul.exception.NotAvailableException is thrown in case the given serviceType is null.
     */
    public static String getServiceStateName(final ServiceType serviceType) throws NotAvailableException {
        try {
            if (serviceType == null) {
                assert false;
                throw new NotAvailableException("ServiceState");
            }
            return StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll("Service", "");
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceStateName", ex);
        }
    }

    /**
     * Method returns the state name of the appurtenant service.
     *
     * @param template The service template.
     *
     * @return The state type name as string.
     *
     * @throws org.openbase.jul.exception.NotAvailableException is thrown in case the given template is null.
     *
     */
    public static String getServiceStateName(final ServiceTemplate template) throws NotAvailableException {
        try {
            if (template == null) {
                assert false;
                throw new NotAvailableException("ServiceTemplate");
            }
            return getServiceStateName(template.getType());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceStateName", ex);
        }
    }

    /**
     * Method returns a collection of service state values.
     *
     * @param serviceType the service type to identify the service state class.
     *
     * @return a collection of enum values of the service state.
     *
     * @throws NotAvailableException is thrown in case the referred service state does not contain any state values.
     */
    public static Collection<? extends ProtocolMessageEnum> getServiceStateValues(final ServiceType serviceType) throws NotAvailableException {
        final String serviceBaseName = getServiceBaseName(serviceType);
        final String serviceEnumName = SERVICE_STATE_PACKAGE.getName() + "." + serviceBaseName + "Type$" + serviceBaseName + "$State";
        try {
            return Arrays.asList((ProtocolMessageEnum[]) (Class.forName(serviceEnumName).getMethod("values").invoke(null)));
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            throw new NotAvailableException("ServiceStateValues", serviceEnumName, ex);
        }
    }

    /**
     * Method generates a new service state builder related to the given {@code serviceType}.
     *
     * @param serviceType the service type of the service state.
     *
     * @throws CouldNotPerformException is thrown if something went wrong during the generation.
     */
    public static GeneratedMessage.Builder generateServiceStateBuilder(final ServiceType serviceType) throws CouldNotPerformException {
        try {
            // create new service state builder
            return (GeneratedMessage.Builder) Services.getServiceStateClass(serviceType).getMethod("newBuilder").invoke(null);
        } catch (final IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException | NotAvailableException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not generate service state builder!", ex);
        }
    }

    /**
     * Method generates a new service state builder related to the given {@code serviceType} and initializes this instance with the given {@code stateValue}.
     *
     * @param <SC>        the service class of the service state.
     * @param <SV>        the state enum of the service.
     * @param serviceType the service type of the service state.
     * @param stateValue  a compatible state value related to the given service state.
     *
     * @return a new service state initialized with the state value.
     *
     * @throws CouldNotPerformException is thrown in case the given arguments are not compatible with each other or something else went wrong during the build.
     */
    public static <SC extends GeneratedMessage.Builder, SV extends ProtocolMessageEnum> SC generateServiceStateBuilder(final ServiceType serviceType, SV stateValue) throws CouldNotPerformException {
        try {
            // create new service state builder
            GeneratedMessage.Builder serviceStateBuilder = generateServiceStateBuilder(serviceType);

            // set service state value
            serviceStateBuilder.getClass().getMethod("setValue", stateValue.getClass()).invoke(serviceStateBuilder, stateValue);

            // return
            return (SC) serviceStateBuilder;
        } catch (final IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException | NotAvailableException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not build service state!", ex);
        }
    }

    /**
     * Method builds a new service state related to the given {@code serviceType} and initializes this instance with the given {@code stateValue}.
     *
     * @param <SC>        the service class of the service state.
     * @param <SV>        the state enum of the service.
     * @param serviceType the service type of the service state.
     * @param stateValue  a compatible state value related to the given service state.
     *
     * @return a new service state initialized with the state value.
     *
     * @throws CouldNotPerformException is thrown in case the given arguments are not compatible with each other or something else went wrong during the build.
     */
    public static <SC extends GeneratedMessage, SV extends ProtocolMessageEnum> SC buildServiceState(final ServiceType serviceType, SV stateValue) throws CouldNotPerformException {
        try {
            // create new service state builder with new state and build.
            return (SC) generateServiceStateBuilder(serviceType, stateValue).build();
        } catch (IllegalArgumentException | SecurityException | NotAvailableException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not build service state!", ex);
        }
    }

    /**
     * @deprecated please use {@code getServiceStateClass(final ServiceType serviceType)} instead.
     */
    @Deprecated
    public static Class<? extends GeneratedMessage> detectServiceDataClass(final ServiceType serviceType) throws NotAvailableException {
        return getServiceStateClass(serviceType);
    }

    /**
     * Method detects and returns the service state class.
     *
     * @param serviceType the given service type to resolve the class.
     *
     * @return the service state class.
     *
     * @throws NotAvailableException is thrown in case the class could not be detected.
     */
    public static Class<? extends GeneratedMessage> getServiceStateClass(final ServiceType serviceType) throws NotAvailableException {
        String serviceStateName;
        try {
            serviceStateName = StringProcessor.transformUpperCaseToCamelCase(Registries.getTemplateRegistry().getServiceTemplateByType(serviceType).getCommunicationType().name());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("CommunicationType for serviceType[" + serviceType + "]");
        }

        final String serviceClassName = SERVICE_STATE_PACKAGE.getName() + "." + serviceStateName + "Type$" + serviceStateName;
        try {
            return (Class<? extends GeneratedMessage>) Class.forName(serviceClassName);
        } catch (NullPointerException | ClassNotFoundException | ClassCastException ex) {
            throw new NotAvailableException("ServiceStateClass", serviceClassName, new CouldNotPerformException("Could not detect class!", ex));
        }
    }

    public static Method detectServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(serviceType, servicePattern, ServiceTempus.CURRENT, instanceClass, argumentClasses);
    }

    public static Method detectServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final ServiceTempus serviceTempus, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(serviceType, getServiceMethodPrefix(servicePattern), serviceTempus, instanceClass, argumentClasses);
    }

    public static Method detectServiceMethod(final ServiceType serviceType, final String serviceMethodPrefix, final ServiceTempus serviceTempus, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        String messageName = "?";
        try {
            messageName = serviceMethodPrefix + getServiceStateName(serviceType) + StringProcessor.transformUpperCaseToCamelCase(serviceTempus.name().replace(serviceTempus.CURRENT.name(), ""));
            return instanceClass.getMethod(messageName, argumentClasses);
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new CouldNotPerformException("Could not detect service method[" + messageName + "]!", ex);
        }
    }


    public static Method detectServiceMethod(final ServiceDescription description, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(description, ServiceTempus.CURRENT, instanceClass, argumentClasses);
    }

    public static Method detectServiceMethod(final ServiceDescription description, final ServiceTempus serviceTempus, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(description.getServiceType(), description.getPattern(), serviceTempus, instanceClass, argumentClasses);
    }

    public static Object invokeServiceMethod(final ServiceDescription description, final Service instance, final Object... arguments) throws CouldNotPerformException {
        return invokeServiceMethod(description, ServiceTempus.CURRENT, instance, arguments);
    }

    public static Object invokeServiceMethod(final ServiceDescription description, final ServiceTempus serviceTempus, final Service instance, final Object... arguments) throws CouldNotPerformException {
        try {
            return detectServiceMethod(description, serviceTempus, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | ExceptionInInitializerError ex) {
            throw new NotSupportedException("ServiceType[" + description.getServiceType().name() + "] with Pattern[" + description.getPattern() + "]", instance, ex);
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Invocation failed because given instance is not available!", ex);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof CouldNotPerformException) {
                throw (CouldNotPerformException) ex.getTargetException();
            } else {
                throw new CouldNotPerformException("Invocation failed!", ex.getTargetException());
            }
        }
    }

    public static Object invokeServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final Object instance, final Object... arguments) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        return invokeServiceMethod(serviceType, servicePattern, ServiceTempus.CURRENT, instance, arguments);
    }

    public static Object invokeServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final ServiceTempus serviceTempus, final Object instance, final Object... arguments) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        try {
            return detectServiceMethod(serviceType, servicePattern, serviceTempus, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | ExceptionInInitializerError | ClassCastException ex) {
            throw new NotSupportedException("ServiceType[" + serviceType.name() + "] with Pattern[" + servicePattern + "]", instance, ex);
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Invocation failed because given instance is not available!", ex);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof CouldNotPerformException) {
                throw (CouldNotPerformException) ex.getTargetException();
            } else {
                throw new CouldNotPerformException("Invocation failed!", ex.getTargetException());
            }
        }
    }

    public static Message invokeProviderServiceMethod(final ServiceType serviceType, final Object instance) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        return (Message) invokeServiceMethod(serviceType, ServicePattern.PROVIDER, instance);
    }

    public static Object invokeOperationServiceMethod(final ServiceType serviceType, final Object instance, final Object... arguments) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        return invokeServiceMethod(serviceType, ServicePattern.OPERATION, instance, arguments);
    }

    public static Class[] getArgumentClasses(final Object[] arguments) {
        Class[] classes = new Class[arguments.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = arguments[i].getClass();
        }
        return classes;
    }

    public static String getServiceFieldName(final ServiceType serviceType, final ServiceTempus serviceTempus) {
        String result = serviceType.name().replace(Service.SERVICE_LABEL.toUpperCase(), "").toLowerCase();
        switch (serviceTempus) {
            case REQUESTED:
            case LAST:
                // add service tempus postfix
                result += serviceTempus.name().toLowerCase();
                break;
            case CURRENT:
            case UNKNOWN:
                // remove underscore at the end
                result = result.substring(0, result.length() - 1);
                break;
        }
        return result;
    }

    public static Boolean hasServiceState(final ServiceType serviceType, final ServiceTempus serviceTempus, final MessageOrBuilder instance, final Object... arguments) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        try {
            return (Boolean) detectServiceMethod(serviceType, "has", serviceTempus, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | ExceptionInInitializerError ex) {
            throw new NotSupportedException("ServiceType[" + serviceType.name() + "] not provided by Message[" + instance.getClass().getSimpleName() + "]!", instance, ex);
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Invocation failed because given instance is not available!", ex);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof CouldNotPerformException) {
                throw (CouldNotPerformException) ex.getTargetException();
            } else {
                throw new CouldNotPerformException("Invocation failed!", ex.getTargetException());
            }
        }
    }

    public static void verifyServiceState(final Message serviceState) throws VerificationFailedException {

        if (serviceState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        final Method valueMethod;
        try {
            valueMethod = serviceState.getClass().getMethod("getValue");
            try {
                verifyServiceStateValue((Enum) valueMethod.invoke(serviceState));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException ex) {
                ExceptionPrinter.printHistory("Operation service verification phase failed of ServiceState[ " + serviceState.getClass().getSimpleName() + "]!", ex, LOGGER);
            }
        } catch (NoSuchMethodException ex) {
            // service state does contain any value so verification is not needed.
        }
    }

    public static void verifyServiceStateValue(final Enum value) throws VerificationFailedException {
        if (value == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceStateValue"));
        }

        if (value.name().equals("UNKNOWN")) {
            throw new VerificationFailedException(value.getClass().getSimpleName() + ".UNKNOWN" + " is an invalid operation service state!");
        }
    }

    /**
     * Verification of the given service state inclusive consistency revalidation.
     * This means field are recalculated in case they are not consistent against each other.
     *
     * @param serviceState the state type to validate.
     *
     * @return the given state or an updated version of it.
     *
     * @throws VerificationFailedException is thrown if the state is invalid and no repair functions are available.
     */
    public static Message verifyAndRevalidateServiceState(final Message serviceState) throws VerificationFailedException {
        try {
            try {
                final Object verifiedState = detectServiceStateVerificationMethod(serviceState).invoke(null, serviceState);
                if (verifiedState != null && verifiedState instanceof Message) {
                    return (Message) verifiedState;
                }
                return serviceState;
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory("Verification of ServiceState[ " + serviceState.getClass().getSimpleName() + "] skipped because verification method not supported yet.!", ex, LOGGER, LogLevel.DEBUG);
            } catch (InvocationTargetException ex) {
                if (ex.getTargetException() instanceof VerificationFailedException) {
                    throw (VerificationFailedException) ex.getTargetException();
                } else {
                    throw ex;
                }
            }
        } catch (VerificationFailedException ex) {
            throw ex;
        } catch (NullPointerException | IllegalAccessException | ExceptionInInitializerError | CouldNotPerformException | InvocationTargetException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("Verification of service state could no be performed!", Services.class, ex), LOGGER, LogLevel.WARN);
        }
        return serviceState;
    }

    public static Method detectServiceStateVerificationMethod(final Message serviceState) throws CouldNotPerformException, NotAvailableException {
        String methodeName = "?";
        try {
            methodeName = "verify" + serviceState.getClass().getSimpleName();
            return detectProviderServiceInterface(serviceState).getMethod(methodeName, serviceState.getClass());
        } catch (SecurityException | ClassNotFoundException ex) {
            throw new CouldNotPerformException("Could not detect service method[" + methodeName + "]!", ex);
        } catch (NoSuchMethodException ex) {
            throw new NotAvailableException("service state verification method", ex);
        }
    }

    public static Class detectProviderServiceInterface(final Message serviceState) throws ClassNotFoundException {
        return Class.forName(ProviderService.class.getPackage().getName() + "." + serviceState.getClass().getSimpleName() + ProviderService.class.getSimpleName());
    }

    public static Class detectOperationServiceInterface(final GeneratedMessage serviceState) throws ClassNotFoundException {
        return Class.forName(OperationService.class.getPackage().getName() + "." + serviceState.getClass().getSimpleName() + OperationService.class.getSimpleName());
    }

    public static Class detectConsumerServiceInterface(final GeneratedMessage serviceState) throws ClassNotFoundException {
        return Class.forName(ConsumerService.class.getPackage().getName() + "." + serviceState.getClass().getSimpleName() + ConsumerService.class.getSimpleName());
    }

    /**
     * Method returns the action which is responsible for the given state.
     *
     * @param serviceState the state used to resolve the responsible action.
     *
     * @return the responsible action.
     *
     * @throws NotAvailableException is thrown if the related action can not be determine.
     */
    public static ActionDescription getResponsibleAction(final MessageOrBuilder serviceState) throws NotAvailableException {
        final Object actionDescription = serviceState.getField(ProtoBufFieldProcessor.getFieldDescriptor(serviceState, Service.RESPONSIBLE_ACTION_FIELD_NAME));
        if (actionDescription instanceof ActionDescription) {
            return (ActionDescription) actionDescription;
        }

        throw new NotAvailableException("ActionDescription");
    }

    /**
     * Method set the responsible action of the service state.
     *
     * @param responsibleAction the action to setup.
     * @param serviceState      the message which is updated with the given responsible action.
     * @param <M>               the type of the service state message.
     *
     * @return the modified message instance.
     */
    public static <M extends Message> M setResponsibleAction(final ActionDescription responsibleAction, final M serviceState) {
        return (M) setResponsibleAction(responsibleAction, serviceState.toBuilder()).build();
    }

    /**
     * Method set the responsible action of the service state.
     *
     * @param responsibleAction   the action to setup.
     * @param serviceStateBuilder the builder which is updated with the given responsible action.
     * @param <B>                 the type of the service state builder.
     *
     * @return the modified builder instance.
     */
    public static <B extends Message.Builder> B setResponsibleAction(final ActionDescription responsibleAction, final B serviceStateBuilder) {
        return (B) serviceStateBuilder.setField(ProtoBufFieldProcessor.getFieldDescriptor(serviceStateBuilder, Service.RESPONSIBLE_ACTION_FIELD_NAME), responsibleAction);
    }

    public static Class<?> loadOperationServiceClass(final ServiceType serviceType) throws ClassNotFoundException {
        final String className = StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replace("Service", "") + OperationService.class.getSimpleName();
        final String packageString = OperationService.class.getPackage().getName();
        return Services.class.getClassLoader().loadClass(packageString + "." + className);
    }
}


