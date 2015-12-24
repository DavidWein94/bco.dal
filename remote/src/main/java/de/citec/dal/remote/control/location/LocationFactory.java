package de.citec.dal.remote.control.location;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.pattern.Factory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface LocationFactory extends Factory<Location, LocationConfig> {

    @Override
    public LocationController newInstance(final LocationConfig config) throws CouldNotPerformException;

}