package org.openbase.bco.dal.lib.layer.service.operation;

/*
 * #%L
 * BCO DAL Library
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

import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.jul.exception.VerificationFailedException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface OperationService extends Service {

    // todo: move methods to a new jul math module

    /**
     * Method verifies the range of the value.
     *
     * @param name  the name of the value.
     * @param value the value to check.
     * @param min   the lower border of a valid value.
     * @param max   the upper border of a valid value.
     *
     * @throws VerificationFailedException is thrown if the value is not within the expected range.
     */
    static void verifyValueRange(final String name, final double value, final double min, final double max) throws VerificationFailedException {
        if (value < min) {
            throw new VerificationFailedException("The value of " + name + " is " + value + " which is lower than the defined minimum of " + min);
        } else if (value > max) {
            throw new VerificationFailedException("The value of " + name + " is " + value + " which is higher than the defined maximum of " + max);
        }
    }

    /**
     * Method checks if the given value is within the margin of the default value.
     *
     * @param name         the name of the value.
     * @param value        the value to check.
     * @param defaultValue the expected value.
     * @param margin       the still valid distance between the value and the default value.
     *
     * @throws VerificationFailedException is thrown if the value is not within the default value margin.
     */
    static void verifyValue(final String name, final double value, final double defaultValue, final double margin) throws VerificationFailedException {
        if (value < defaultValue - margin || value > defaultValue + margin) {
            throw new VerificationFailedException("The value of " + name + " is " + value + " but should be " + defaultValue);
        }
    }
}
