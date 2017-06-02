package org.openbase.bco.dal.lib.layer.unit.user;

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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.unit.BaseUnit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.state.UserActivityStateType.UserActivityState;
import rst.domotic.state.UserPresenceStateType.UserPresenceState;
import rst.domotic.unit.user.UserDataType.UserData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface User extends BaseUnit<UserData> {

    public final static String TYPE_FIELD_USER_NAME = "user_name";

    public String getUserName() throws NotAvailableException;

    //TODO: services for Users have to registered, see dal issue 44
    @RPCMethod
    public UserActivityState getUserActivityState() throws NotAvailableException;

    @RPCMethod
    public Future<Void> setUserActivityState(UserActivityState UserActivityState) throws CouldNotPerformException;

    @RPCMethod
    public UserPresenceState getUserPresenceState() throws NotAvailableException;

    @RPCMethod
    public Future<Void> setUserPresenceState(UserPresenceState userPresenceState) throws CouldNotPerformException;
}
