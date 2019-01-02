package org.openbase.bco.dal.visual.action;

/*-
 * #%L
 * BCO DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
 */

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import org.openbase.jul.visual.javafx.fxml.FXMLProcessor;
import org.openbase.jul.visual.javafx.launch.AbstractFXMLApplication;

public class BCOActionInspector extends AbstractFXMLApplication {

    public BCOActionInspector() {
        super(UnitAllocationPane.class);
    }

    @Override
    protected void registerProperties() {
    }

    public static void main(String[] args) {
        BCOActionInspector.launch(args);
    }
}
