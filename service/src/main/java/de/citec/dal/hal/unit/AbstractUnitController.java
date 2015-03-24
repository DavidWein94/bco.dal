/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import com.google.protobuf.GeneratedMessage;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.service.ServiceType;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.RSBInformerInterface;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.MultiException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import rsb.RSBException;
import rsb.Scope;
import rsb.patterns.LocalServer;

/**
 *
 * @author mpohling
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> implements Unit {

    public final static String TYPE_FILED_ID = "id";
    public final static String TYPE_FILED_NAME = "name";
    public final static String TYPE_FILED_LABEL = "label";

    protected final String id;
    protected final String name;
    protected final String label;
    protected final Location location;
    private final Device device;
    private final List<Service> serviceList;

    public AbstractUnitController(final Class unitClass, final String label, final Device device, final MB builder) throws InstantiationException {
        this(unitClass, label, device.getLocation(), device, builder);
    }

    public AbstractUnitController(final Class unitClass, final String label, final Location location, final Device device, final MB builder) throws InstantiationException {
        super(generateScope(generateName(unitClass), label, device), builder);
        try {
            this.id = generateID(scope);
            this.name = generateName();
            this.label = label;
            this.device = device;
            this.location = location;
            this.serviceList = new ArrayList<>();

            setField(TYPE_FILED_ID, id);
            setField(TYPE_FILED_NAME, name);
            setField(TYPE_FILED_LABEL, label);

            try {
                validateUpdateServices();
            } catch (MultiException ex) {
                logger.error(this + " is not valid!", ex);
                ex.printExceptionStack();
            }

            DALService.getRegistryProvider().getUnitRegistry().register(this);

            init(RSBInformerInterface.InformerType.Distributed);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public Device getDevice() {
        return device;
    }

    public Collection<Service> getServices() {
        return Collections.unmodifiableList(serviceList);
    }

    public void registerService(final Service service) {
        serviceList.add(service);
    }

    public static String generateID(final String label, final Location location, final Class clazz) {
        return generateID(generateScope(generateName(clazz), label, location));
    }

    public static String generateID(final Scope scope) {
        return scope.toString().toLowerCase();
    }

    public final String generateName() {
        return generateName(getClass());
    }

    public static final String generateName(final Class clazz) {
        return clazz.getSimpleName().replace("Controller", "");
    }

    public Scope generateScope() {
        return generateScope(generateName(), label, device);
    }

    public static Scope generateScope(final String name, final String label, final Device device) {
        return generateScope(name, label, device.getLocation());
    }

    public static Scope generateScope(final String name, final String label, final Location location) {
        return location.getScope().concat(new Scope(Scope.COMPONENT_SEPARATOR + name).concat(new Scope(Scope.COMPONENT_SEPARATOR + label)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + "[" + label + "]]";
    }

    @Override
    public void registerMethods(LocalServer server) throws RSBException {
        ServiceType.registerServiceMethods(server, this);
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.MULTI;
    }

    /**
     * *
     *
     * @throws MultiException
     */
    private void validateUpdateServices() throws MultiException {
        logger.debug("Validating unit update methods...");

        MultiException.ExceptionStack exceptionStack = null;
        List<String> unitMethods = new ArrayList<>();

        // === Transform unit methods into string list. ===
        for (Method medhod : getClass().getMethods()) {
            unitMethods.add(medhod.getName());
        }

        // === Validate if all update methods are registrated. ===
        for (ServiceType service : ServiceType.getServiceTypeList(this)) {
            for (String serviceMethod : service.getUpdateMethods()) {
                if (!unitMethods.contains(serviceMethod)) {
                    exceptionStack = MultiException.push(service, null, exceptionStack);
                }
            }
        }

        // === throw multi exception in error case. ===
        MultiException.checkAndThrow("Update service not valid!", exceptionStack);
    }
}
