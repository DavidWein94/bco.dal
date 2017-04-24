package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * BCO DAL Remote
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openbase.bco.dal.lib.layer.service.Service$;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.processing.GenericMessageProcessor;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.domotic.action.ActionAuthorityType;
import rst.domotic.action.ActionConfigType;
import rst.domotic.action.ActionPriorityType;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M>
 */
public abstract class AbstractUnitRemote<M extends GeneratedMessage> extends AbstractConfigurableRemote<M, UnitConfig> implements UnitRemote<M> {

    private UnitTemplate template;
    private UnitRegistry unitRegistry;

    public AbstractUnitRemote(final Class<M> dataClass) {
        super(dataClass, UnitConfig.class);
    }

    protected UnitRegistry getUnitRegistry() throws InterruptedException, CouldNotPerformException {
        if (unitRegistry == null) {
            unitRegistry = Registries.getUnitRegistry();
            Registries.getUnitRegistry().waitForData();
        }
        return unitRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param id
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void initById(final String id) throws InitializationException, InterruptedException {
        try {
            init(getUnitRegistry().getUnitConfigById(id));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param label
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void initByLabel(final String label) throws InitializationException, InterruptedException {
        try {
            List<UnitConfig> unitConfigList = getUnitRegistry().getUnitConfigsByLabel(label);

            if (unitConfigList.isEmpty()) {
                throw new NotAvailableException("Unit with Label[" + label + "]");
            } else if (unitConfigList.size() > 1) {
                throw new InvalidStateException("Unit with Label[" + label + "] is not unique!");
            }

            init(unitConfigList.get(0));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(ScopeType.Scope scope) throws InitializationException, InterruptedException {
        try {
            init(getUnitRegistry().getUnitConfigByScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(String scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeGenerator.generateScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init(final String label, final ScopeProvider location) throws InitializationException, InterruptedException {
        try {
            init(ScopeGenerator.generateScope(label, getDataClass().getSimpleName(), location.getScope()));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InitializationException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        this.setMessageProcessor(new GenericMessageProcessor<>(getDataClass()));
        ((UnitRegistryRemote) unitRegistry).addDataObserver((Observable<UnitRegistryData> source, UnitRegistryData data) -> {
            try {
                final UnitConfig newUnitConfig = unitRegistry.getUnitConfigById(getId());
                if (!newUnitConfig.equals(getConfig())) {
                    applyConfigUpdate(newUnitConfig);
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not update unit config of " + this, ex, logger);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param config {@inheritDoc}
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        if (config == null) {
            throw new NotAvailableException("UnitConfig");
        }

        try {
            if (getConfig().equals(config)) {
                logger.debug("Skip config update because no config change detected!");
                return config;
            }
        } catch (NotAvailableException ex) {
            // change check failed.
        }
        template = getUnitRegistry().getUnitTemplateByType(config.getType());
        return super.applyConfigUpdate(config);
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (!isEnabled()) {
            throw new InvalidStateException("The activation of an remote is not allowed if the referred unit is disabled!");
        }
        if (!Units.contains(this)) {
            logger.warn("You are using a unit remote which is not maintained by the global unit remote pool! This is extremely inefficient! Please use \"Units.getUnit(...)\" instead creating your own instances!");
        }
        super.activate();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        verifyEnablingState();
        super.waitForData();
    }

    /**
     * {@inheritDoc}
     *
     * @param timeout {@inheritDoc}
     * @param timeUnit {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        verifyEnablingState();
        super.waitForData(timeout, timeUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        try {
            assert (getConfig() instanceof UnitConfig);
            return getConfig().getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED);
        } catch (CouldNotPerformException ex) {
            LoggerFactory.getLogger(org.openbase.bco.dal.lib.layer.unit.UnitRemote.class).warn("isEnabled() was called on non initialized unit!");
            assert false;
        }
        return false;
    }

    private void verifyEnablingState() throws FatalImplementationErrorException {
        if (!isEnabled()) {
            if (JPService.testMode()) {
                throw new FatalImplementationErrorException("Waiting for data of a disabled unit should be avoided!", "Calling instance");
            } else {
                logger.warn("Waiting for data of an disabled unit should be avoided! Probably this method will block forever!");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public UnitTemplate.UnitType getType() throws NotAvailableException {
        try {
            return getConfig().getType();
        } catch (NullPointerException | NotAvailableException ex) {
            throw new NotAvailableException("unit type", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public UnitTemplate getTemplate() throws NotAvailableException {
        if (template == null) {
            throw new NotAvailableException("UnitTemplate");
        }
        return template;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public String getLabel() throws NotAvailableException {
        try {
            return getConfig().getLabel();
        } catch (NullPointerException | NotAvailableException ex) {
            throw new NotAvailableException("unit label", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public ScopeType.Scope getScope() throws NotAvailableException {
        try {
            return getConfig().getScope();
        } catch (NullPointerException | CouldNotPerformException ex) {
            throw new NotAvailableException("unit label", ex);
        }
    }

    public UnitConfig getLocationConfig() throws NotAvailableException {
        try {
            // TODO implement get unit config by type and id;
            return unitRegistry.getUnitConfigById(getConfig().getPlacementConfig().getLocationId());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("LocationConfig", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param actionConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public Future<Void> applyAction(ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(actionConfig, this, Void.class);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(this, Snapshot.class);
    }

    ///////////
    // START DEFAULT INTERFACE METHODS

    public void verifyOperationServiceState(final Object serviceState) throws VerificationFailedException {

        if (serviceState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        final Method valueMethod;
        try {
            valueMethod = serviceState.getClass().getMethod("getValue");
        } catch (NoSuchMethodException ex) {
            // service state does contain any value so verification is not possible.
            return;
        }

        try {
            verifyOperationServiceStateValue((Enum) valueMethod.invoke(serviceState));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException ex) {
            ExceptionPrinter.printHistory("Operation service verification phase failed!", ex, LoggerFactory.getLogger(getClass()));
        }
    }

    public void verifyOperationServiceStateValue(final Enum value) throws VerificationFailedException {

        if (value == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceStateValue"));
        }

        if (value.name().equals("UNKNOWN")) {
            throw new VerificationFailedException("UNKNOWN." + value.getClass().getSimpleName() + " is an invalid operation service state of " + this + "!");
        }
    }

    @RPCMethod
    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        try {
            Collection<Future> futureCollection = new ArrayList<>();
            for (final ActionConfigType.ActionConfig actionConfig : snapshot.getActionConfigList()) {
                futureCollection.add(applyAction(actionConfig));
            }
            return GlobalCachedExecutorService.allOf(futureCollection);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    // END DEFAULT INTERFACE METHODS
    /////////////
}
