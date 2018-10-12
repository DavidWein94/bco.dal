package org.openbase.bco.dal.remote.layer.service;

/*-
 * #%L
 * BCO DAL Remote
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

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.iface.provider.PingProvider;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionParameterType.ActionParameter.Builder;
import rst.domotic.action.SnapshotType;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class ServiceRemoteManager<D extends Message> implements Activatable, Snapshotable<Snapshot>, PingProvider, DataProvider<D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRemoteManager.class);

    private boolean active;
    private long connectionPing;
    private final SyncObject serviceRemoteMapLock = new SyncObject("ServiceRemoteMapLock");
    private final ServiceRemoteFactory serviceRemoteFactory;
    private final Map<ServiceType, AbstractServiceRemote> serviceRemoteMap;
    private final Observer<Unit, Message> serviceDataObserver;
    private final Unit<D> responsibleInstance;
    private boolean filterInfrastructureUnits;

    public ServiceRemoteManager(final Unit<D> responsibleInstance) {
        this(responsibleInstance, true);
    }

    public ServiceRemoteManager(final Unit<D> responsibleInstance, final boolean filterInfrastructureUnits) {
        this.responsibleInstance = responsibleInstance;
        this.filterInfrastructureUnits = filterInfrastructureUnits;
        this.serviceRemoteMap = new HashMap<>();
        this.serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();
        this.serviceDataObserver = (source, data) -> notifyServiceUpdate(source, data);
    }

    public synchronized void applyConfigUpdate(final List<String> unitIDList) throws CouldNotPerformException, InterruptedException {
        Registries.waitForData();
        synchronized (serviceRemoteMapLock) {
            // shutdown all existing instances.
            for (final AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.removeDataObserver(serviceDataObserver);
                serviceRemote.shutdown();
            }
            serviceRemoteMap.clear();

            // init a new set for each supported service type.
            Map<ServiceType, Set<UnitConfig>> serviceMap = new HashMap<>();
            for (final ServiceType serviceType : ServiceType.values()) {
                serviceMap.put(serviceType, new HashSet<>());
            }

            // init service unit map
            for (final String unitId : unitIDList) {
                try {
                    // resolve unit config by unit registry
                    UnitConfig unitConfig;
                    try {
                        unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
                    } catch (NotAvailableException ex) {
                        LOGGER.warn("Unit[" + unitId + "] not available for [" + responsibleInstance + "]");
                        continue;
                    }

                    // filter non dal units and disabled units
                    try {
                        if (!UnitConfigProcessor.isDalUnit(unitConfig) || !UnitConfigProcessor.isEnabled(unitConfig)) {
                            continue;
                        }
                    } catch (VerificationFailedException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("UnitConfig[" + unitConfig + "] could not be verified as a dal unit!", ex), LOGGER);
                    }

                    // sort dal unit by service type
                    unitConfig.getServiceConfigList().stream().forEach((serviceConfig) -> {
                        // register unit for each service type. UnitConfigs can be added twice because of duplicated types with different service patterns but are filtered by the set.
                        serviceMap.get(serviceConfig.getServiceDescription().getServiceType()).add(unitConfig);
                    });
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not process unit config update of Unit[" + unitId + "] for " + responsibleInstance + "!", ex), LOGGER);
                }
            }

            // initialize service remotes
            for (final ServiceType serviceType : getManagedServiceTypes()) {
                final AbstractServiceRemote serviceRemote = serviceRemoteFactory.newInitializedInstance(serviceType, serviceMap.get(serviceType), filterInfrastructureUnits);
                serviceRemoteMap.put(serviceType, serviceRemote);

                // if already active than update the current location state.
                synchronized (serviceRemoteMapLock) {
                    if (isActive()) {
                        serviceRemote.addDataObserver(serviceDataObserver);
                        serviceRemote.activate();
                    }
                }
            }
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        synchronized (serviceRemoteMapLock) {
            active = true;
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.addDataObserver(serviceDataObserver);
                serviceRemote.activate();
            }
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            active = false;
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.removeDataObserver(serviceDataObserver);
                serviceRemote.deactivate();
            }
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public List<AbstractServiceRemote> getServiceRemoteList() {
        synchronized (serviceRemoteMapLock) {
            return new ArrayList<>(serviceRemoteMap.values());
        }
    }

    /**
     * Method checks if the given {@code ServiceType} is currently available by this {@code ServiceRemoteManager}
     *
     * @param serviceType the {@code ServiceType} to check.
     *
     * @return returns true if the {@code ServiceType} is available, otherwise false.
     */
    public boolean isServiceAvailable(final ServiceType serviceType) {
        try {
            return getServiceRemote(serviceType).hasInternalRemotes();
        } catch (NotAvailableException ex) {
            // no service entry means the service is not available.
            return false;
        }
    }

    public AbstractServiceRemote getServiceRemote(final ServiceType serviceType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            AbstractServiceRemote serviceRemote = serviceRemoteMap.get(serviceType);
            if (serviceRemote == null) {
                final String responsible = (responsibleInstance != null ? responsibleInstance.toString() : "the underlying instance");
                throw new NotAvailableException("ServiceRemote", serviceType.name(), new NotSupportedException("ServiceType[" + serviceType + "]", responsible));
            }
            return serviceRemote;
        }
    }

    public <B> B updateBuilderWithAvailableServiceStates(final B builder) throws InterruptedException, CouldNotPerformException {
        return updateBuilderWithAvailableServiceStates(builder, responsibleInstance.getDataClass(), getManagedServiceTypes());
    }

    public <B> B updateBuilderWithAvailableServiceStates(final B builder, final Class dataClass, final Set<ServiceType> supportedServiceTypeSet) throws InterruptedException {
        try {
            for (final ServiceType serviceType : supportedServiceTypeSet) {

                final Object serviceState;

                try {
                    final AbstractServiceRemote serviceRemote = getServiceRemote(serviceType);
                    /* When the locationRemote is active and a config update occurs the serviceRemoteManager clears
                     * its map of service remotes and fills it with new ones. When they are activated an update is triggered while
                     * the map is not completely filled. Therefore the serviceRemote can be null.
                     */
                    if (serviceRemote == null) {
                        continue;
                    }
                    if (!serviceRemote.isDataAvailable()) {
                        continue;
                    }

                    serviceState = Services.invokeProviderServiceMethod(serviceType, serviceRemote);
                } catch (NotAvailableException ex) {
                    ExceptionPrinter.printHistory("No service data for type[" + serviceType + "] on location available!", ex, LOGGER);
                    continue;
                } catch (NotSupportedException | IllegalArgumentException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException(this, ex), LOGGER);
                    continue;
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update ServiceState[" + serviceType.name() + "] for " + this, ex, LOGGER);
                    continue;
                }

                try {
                    Services.invokeOperationServiceMethod(serviceType, builder, serviceState);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new NotSupportedException("Field[" + serviceType.name().toLowerCase().replace("_service", "") + "] is missing in protobuf type " + dataClass + "!", this, ex), LOGGER);
                }
            }
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                throw (InterruptedException) ex;
            }
            new CouldNotPerformException("Could not update current status!", ex);
        }
        return builder;
    }

    @Override
    public Future<SnapshotType.Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return recordSnapshot(UnitTemplateType.UnitTemplate.UnitType.UNKNOWN);
    }

    public Future<Snapshot> recordSnapshot(final UnitType unitType) throws CouldNotPerformException, InterruptedException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                SnapshotType.Snapshot.Builder snapshotBuilder = SnapshotType.Snapshot.newBuilder();
                Set<UnitRemote> unitRemoteSet = new HashSet<>();

                if (unitType == UnitType.UNKNOWN) {
                    // if the type is unknown then take the snapshot for all units
                    getServiceRemoteList().stream().forEach((serviceRemote) -> {
                        unitRemoteSet.addAll(serviceRemote.getInternalUnits());
                    });
                } else {
                    // for efficiency reasons only one serviceType implemented by the unitType is regarded because the unitRemote is part of
                    // every abstractServiceRemotes internal units if the serviceType is implemented by the unitType
                    ServiceType serviceType;
                    try {
                        serviceType = Registries.getTemplateRegistry().getUnitTemplateByType(unitType).getServiceDescriptionList().get(0).getServiceType();
                    } catch (IndexOutOfBoundsException ex) {
                        // if there is not at least one serviceType for the unitType then the snapshot is empty
                        return snapshotBuilder.build();
                    }

                    for (final AbstractServiceRemote abstractServiceRemote : getServiceRemoteList()) {
                        if (!(serviceType == abstractServiceRemote.getServiceType())) {
                            continue;
                        }

                        Collection<UnitRemote> internalUnits = abstractServiceRemote.getInternalUnits();
                        for (final UnitRemote unitRemote : internalUnits) {
                            // just add units with the according type
                            if (unitRemote.getUnitType() == unitType) {
                                unitRemoteSet.add(unitRemote);
                            }
                        }
                    }
                }

                // take the snapshot
                final Map<UnitRemote, Future<SnapshotType.Snapshot>> snapshotFutureMap = new HashMap<UnitRemote, Future<SnapshotType.Snapshot>>();
                for (final UnitRemote<?> remote : unitRemoteSet) {
                    try {
                        if (UnitProcessor.isDalUnit(remote)) {
                            if (!remote.isConnected()) {
                                throw new NotAvailableException("Unit[" + remote.getLabel() + "] is currently not reachable!");
                            }
                            snapshotFutureMap.put(remote, remote.recordSnapshot());
                        }
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + remote.getLabel(), ex), LOGGER, LogLevel.WARN);
                    }
                }

                // build snapshot
                for (final Map.Entry<UnitRemote, Future<SnapshotType.Snapshot>> snapshotFutureEntry : snapshotFutureMap.entrySet()) {
                    try {
                        snapshotBuilder.addAllServiceStateDescription(snapshotFutureEntry.getValue().get(5, TimeUnit.SECONDS).getServiceStateDescriptionList());
                    } catch (ExecutionException | TimeoutException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + snapshotFutureEntry.getKey().getLabel(), ex), LOGGER);
                    }
                }
                return snapshotBuilder.build();
            } catch (final CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not record snapshot!", ex);
            }
        });
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        return restoreSnapshotAuthenticated(snapshot, null);
    }

    public Future<Void> restoreSnapshotAuthenticated(final Snapshot snapshot, final AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException {
        try {
            if (authenticationBaseData != null) {
                try {
                    final TicketAuthenticatorWrapper initializedTicket = AuthenticationClientHandler.initServiceServerRequest(authenticationBaseData.getSessionKey(), authenticationBaseData.getTicketAuthenticatorWrapper());

                    return GlobalCachedExecutorService.allOf(input -> {
                        try {
                            for (Future<AuthenticatedValue> authenticatedValueFuture : input) {
                                AuthenticationClientHandler.handleServiceServerResponse(authenticationBaseData.getSessionKey(), initializedTicket, authenticatedValueFuture.get().getTicketAuthenticatorWrapper());
                            }
                        } catch (ExecutionException ex) {
                            throw new FatalImplementationErrorException("AllOf called result processable even though some futures did not finish", GlobalCachedExecutorService.getInstance(), ex);
                        }
                        return null;
                    }, generateSnapshotActions(snapshot, initializedTicket, authenticationBaseData.getSessionKey()));
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not update ticket for further requests", ex);
                }
            } else {
                return GlobalCachedExecutorService.allOf(input -> null, generateSnapshotActions(snapshot, null, null));
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot authenticated!", ex);
        }
    }

    private Collection<Future<AuthenticatedValue>> generateSnapshotActions(final Snapshot snapshot, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper, final byte[] sessionKey) throws CouldNotPerformException {
        final Map<String, UnitRemote<?>> unitRemoteMap = new HashMap<>();
        for (AbstractServiceRemote<?, ?> serviceRemote : this.getServiceRemoteList()) {
            for (UnitRemote<?> unitRemote : serviceRemote.getInternalUnits()) {
                unitRemoteMap.put(unitRemote.getId(), unitRemote);
            }
        }

        final ServiceJSonProcessor serviceJSonProcessor = new ServiceJSonProcessor();
        final Collection<Future<AuthenticatedValue>> futureCollection = new ArrayList<>();
        for (final ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
            final UnitRemote unitRemote = unitRemoteMap.get(serviceStateDescription.getUnitId());


            final Builder actionParameterBuilder = ActionDescriptionProcessor.generateDefaultActionParameter(serviceStateDescription);
            final ActionDescription.Builder actionDescriptionBuilder = ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameterBuilder);

            // todo: why is this needed?
//            ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
//            ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
//
//            // TODO: discuss if the responsible action shall be moved to the action chain, if yes a snapshot could already contain a list
//            // of action descriptions which are initialized accordingly, this way the deserialization does not have to be done here
//            // Furthermore restoring a snapshot itself should be have an action description which is the cause
//            Message.Builder serviceAttribute = serviceJSonProcessor.deserialize(serviceStateDescription.getServiceAttribute(), serviceStateDescription.getServiceAttributeType()).toBuilder();
//            if (Services.hasResponsibleAction(serviceAttribute)) {
//                ActionDescription responsibleAction = Services.getResponsibleAction(serviceAttribute);
//                Services.clearResponsibleAction(serviceAttribute);
//
//                ActionDescriptionProcessor.updateActionChain(actionDescription, responsibleAction);
//            }
//            ActionDescriptionProcessor.updateActionDescription(actionDescription, serviceAttribute.build(), serviceStateDescription.getServiceType(), unitRemote);

            AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
            if (ticketAuthenticatorWrapper != null) {
                // prepare authenticated value to request action
                authenticatedValue.setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper);
                authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(actionDescriptionBuilder.build(), sessionKey));
            } else {
                authenticatedValue.setValue(actionDescriptionBuilder.build().toByteString());
            }
            futureCollection.add(unitRemote.applyActionAuthenticated(authenticatedValue.build()));
        }

        return futureCollection;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Long> ping() {
        synchronized (serviceRemoteMapLock) {
            if (serviceRemoteMap.isEmpty()) {
                return CompletableFuture.completedFuture(0l);
            }

            final List<Future<Long>> futurePings = new ArrayList<>();

            for (final Remote<?> remote : serviceRemoteMap.values()) {
                if (remote.isConnected()) {
                    futurePings.add(remote.ping());
                }
            }

            return GlobalCachedExecutorService.allOf(input -> {
                try {
                    long sum = 0;
                    for (final Future<Long> future : input) {
                        sum += future.get();
                    }

                    long ping;
                    if (!input.isEmpty()) {
                        ping = sum / input.size();
                    } else {
                        ping = 0;
                    }
                    connectionPing = ping;
                    return ping;
                } catch (ExecutionException ex) {
                    throw new CouldNotPerformException("Could not compute ping!", ex);
                }
            }, futurePings);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Long getPing() {
        return connectionPing;
    }

    public Future<ActionDescription> applyAction(ActionDescription actionDescription) throws CouldNotPerformException {
        if (actionDescription.getServiceStateDescription().getUnitType().equals(responsibleInstance.getUnitType())) {
            ActionDescription.Builder builder = actionDescription.toBuilder();
            builder.getServiceStateDescriptionBuilder().setUnitType(UnitType.UNKNOWN);
            actionDescription = builder.build();
        }
        return getServiceRemote(actionDescription.getServiceStateDescription().getServiceType()).applyAction(actionDescription);
    }

    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        if (!SessionManager.getInstance().isLoggedIn()) {
            throw new CouldNotPerformException("Could not apply authenticated action because default session manager not logged in");
        }

        ActionDescription actionDescription;
        try {
            actionDescription = EncryptionHelper.decrypt(authenticatedValue.getValue(), SessionManager.getInstance().getSessionKey(), ActionDescription.class, SessionManager.getInstance().getUserId() == null);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply authenticated action because internal action description could not be decrypted using the default session manager", ex);
        }

        return getServiceRemote(actionDescription.getServiceStateDescription().getServiceType()).applyActionAuthenticated(authenticatedValue, actionDescription);
    }

    protected abstract Set<ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException;

    protected abstract void notifyServiceUpdate(final Unit source, final Message data) throws NotAvailableException, InterruptedException;

    @Override
    public boolean isDataAvailable() {
        return responsibleInstance.isDataAvailable();
    }

    @Override
    public Class<D> getDataClass() {
        return responsibleInstance.getDataClass();
    }

    @Override
    public D getData() throws NotAvailableException {
        return responsibleInstance.getData();
    }

    @Override
    public CompletableFuture<D> getDataFuture() {
        return responsibleInstance.getDataFuture();
    }

    @Override
    public void addDataObserver(final Observer<DataProvider<D>, D> observer) {
        synchronized (serviceRemoteMapLock) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.addDataObserver(observer);
            }
        }
    }

    @Override
    public void removeDataObserver(Observer<DataProvider<D>, D> observer) {
        synchronized (serviceRemoteMapLock) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.removeDataObserver(observer);
            }
        }
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        synchronized (serviceRemoteMapLock) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.waitForData();
            }
        }
    }

    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        synchronized (serviceRemoteMapLock) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.waitForData(timeout, timeUnit);
            }
        }
    }

    public <B> Future<B> requestData(final B builder) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            final List<Future> futureData = new ArrayList<>();

            for (final Remote<?> remote : serviceRemoteMap.values()) {
                futureData.add(remote.requestData());
            }

            return GlobalCachedExecutorService.allOf(() -> {
                try {
                    return updateBuilderWithAvailableServiceStates(builder);
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not generate data!", ex), LOGGER);
                }
            }, futureData);
        }
    }

    public void validateMiddleware() throws InvalidStateException {
        synchronized (serviceRemoteMapLock) {
            for (AbstractServiceRemote value : serviceRemoteMap.values()) {
                for (Object internalUnit : value.getInternalUnits()) {
                    ((RSBRemoteService) internalUnit).validateMiddleware();
                }
            }
        }
    }
}
