package org.openbase.bco.dal.remote.layer.unit.connection;

/*
 * #%L
 * BCO DAL Remote
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
import org.openbase.bco.dal.lib.layer.unit.connection.Connection;
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.state.ContactStateType.ContactState;
import org.openbase.type.domotic.state.HandleStateType.HandleState;
import org.openbase.type.domotic.unit.connection.ConnectionDataType.ConnectionData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionRemote extends AbstractUnitRemote<ConnectionData> implements Connection {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ContactState.getDefaultInstance()));
    }

    public ConnectionRemote() {
        super(ConnectionData.class);
    }
}
