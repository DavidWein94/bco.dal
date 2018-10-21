package org.openbase.app.test.agent;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 *
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
import org.openbase.bco.dal.lib.layer.unit.agent.AgentManager;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;

public class AgentManagerLauncher extends AbstractLauncher<org.openbase.app.test.agent.AgentManagerImpl> {

    public AgentManagerLauncher() throws InstantiationException {
        super(AgentManager.class, org.openbase.app.test.agent.AgentManagerImpl.class);
    }

    @Override
    protected void loadProperties() {

    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {
        BCO.printLogo();
        AbstractLauncher.main(args, AgentManager.class, AgentManagerLauncher.class);
    }
}