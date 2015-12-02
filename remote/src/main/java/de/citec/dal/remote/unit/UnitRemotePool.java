package de.citec.dal.remote.unit;

import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import java.util.HashMap;
import java.util.Map;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class UnitRemotePool {

    private Map<Class, Map<String, DALRemoteService>> pool;
    private UnitRemoteFactoryInterface factory;
    private DeviceRegistryRemote deviceRegistryRemote;

    public UnitRemotePool() throws de.citec.jul.exception.InstantiationException, InterruptedException {
        this(UnitRemoteFactory.getInstance());
    }

    public UnitRemotePool(UnitRemoteFactoryInterface factory) throws de.citec.jul.exception.InstantiationException, InterruptedException {
        try {
            this.pool = new HashMap<>();
            this.factory = factory;
            this.deviceRegistryRemote = new DeviceRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();
            initAllUnitRemotes();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void initAllUnitRemotes() throws CouldNotPerformException {
        for (UnitConfig unitConfig : deviceRegistryRemote.getUnitConfigs()) {
            DALRemoteService unitRemote = factory.createAndInitUnitRemote(unitConfig);

            if (!pool.containsKey(unitRemote.getClass())) {
                pool.put(unitRemote.getClass(), new HashMap<>());
            }

            pool.get(unitRemote.getClass()).put(unitRemote.getId(), unitRemote);

        }
    }

    public void activate() throws InterruptedException, CouldNotPerformException {
        for (Map<String, DALRemoteService> unitCollection : pool.values()) {
            for (DALRemoteService remote : unitCollection.values()) {
                remote.activate();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <R extends DALRemoteService> R getUnitRemote(final String unitId, final Class<? extends R> remoteClass) {
        return (R) pool.get(remoteClass).get(unitId);
    }
}
