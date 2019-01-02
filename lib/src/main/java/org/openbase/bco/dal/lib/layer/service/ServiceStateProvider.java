package org.openbase.bco.dal.lib.layer.service;

/*-
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

import com.google.protobuf.Message;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

public class ServiceStateProvider<ST extends Message> {

    private final ServiceType serviceType;
    private final ServiceProvider<ST> serviceProvider;

    public ServiceStateProvider(ServiceType serviceType, ServiceProvider<ST> serviceProvider) {
        this.serviceType = serviceType;
        this.serviceProvider = serviceProvider;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public ServiceProvider<ST> getServiceProvider() {
        return serviceProvider;
    }

    public ST getServiceState() throws NotAvailableException {
        return serviceProvider.getServiceState(serviceType);
    }
}
