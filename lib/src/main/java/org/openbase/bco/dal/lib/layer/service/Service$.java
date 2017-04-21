package org.openbase.bco.dal.lib.layer.service;

/*
 * #%L
 * BCO DAL Library
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

import com.google.protobuf.GeneratedMessage;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.action.ActionConfigType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ContactStateType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @author <a href="mailto:agatting@techfak.uni-bielefeld.de">Andreas Gatting</a>
 */
public class Service$ {

    public static final Package SERVICE_STATE_PACKAGE = ContactStateType.class.getPackage();
    public static final String SERVICE_LABEL = Service$.class.getSimpleName();

    /**
     * This method returns the service base name of the given service type.
     *
     * The base name is the service name without service suffix.
     * e.g. the base name of service PowerStateService is PowerState.
     *
     * @param serviceType the service type to extract the base name.
     * @return the service base name.
     */
    public static String getServiceBaseName(ServiceType serviceType) {
        return StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll(Service$.SERVICE_LABEL, "");
    }

    public static String getServicePrefix(final ServicePattern pattern) throws CouldNotPerformException {
        switch (pattern) {
            case CONSUMER:
                return "";
            case OPERATION:
                return "set";
            case PROVIDER:
                return "get";
            default:
                throw new NotSupportedException(pattern, Service$.class);
        }
    }

    /**
     * Method returns the state name of the appurtenant service.
     *
     * @param serviceType the service type which is used to generate the service name.
     * @return The state type name as string.
     * @throws NotAvailableException is thrown in case the given serviceType is null.
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
     * @return The state type name as string.
     * @throws NotAvailableException is thrown in case the given template is null.
     * //
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
     * Method detects and returns the service data class.
     *
     * @param serviceType the given service type to resolve the class.
     * @return the service data class.
     * @throws NotAvailableException is thrown in case the class could not be detected.
     */
    public static Class<? extends GeneratedMessage> detectServiceDataClass(final ServiceType serviceType) throws NotAvailableException {
        final String serviceBaseName = getServiceBaseName(serviceType);
        final String serviceClassName = SERVICE_STATE_PACKAGE.getName() + "." + serviceBaseName + "Type$" + serviceBaseName;
        try {
            return (Class<? extends GeneratedMessage>) Class.forName(serviceClassName);
        } catch (NullPointerException | ClassNotFoundException | ClassCastException ex) {
            throw new NotAvailableException("ServiceDataClass", serviceClassName, new CouldNotPerformException("Could not detect class!", ex));
        }
    }

    public static Method detectServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        try {
            return instanceClass.getMethod(getServicePrefix(servicePattern) + getServiceStateName(serviceType), argumentClasses);
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new CouldNotPerformException("Could not detect service method!", ex);
        }
    }

    public static Method detectServiceMethod(final ServiceTemplate template, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(template.getType(), template.getPattern(), instanceClass, argumentClasses);
    }

    public static Object invokeServiceMethod(final ServiceTemplate template, final Service instance, final Object... arguments) throws CouldNotPerformException {
        try {
            return detectServiceMethod(template, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | ExceptionInInitializerError ex) {
            throw new NotSupportedException("ServiceType[" + template.getType().name() + "] with Pattern[" + template.getPattern() + "]", instance, ex);
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
        try {
            return detectServiceMethod(serviceType, servicePattern, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | ExceptionInInitializerError ex) {
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

    public static Object invokeProviderServiceMethod(final ServiceType serviceType, final Object instance, final Object... arguments) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        return invokeServiceMethod(serviceType, ServicePattern.PROVIDER, instance, arguments);
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
}
