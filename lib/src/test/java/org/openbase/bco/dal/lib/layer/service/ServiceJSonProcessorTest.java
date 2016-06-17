package org.openbase.bco.dal.lib.layer.service;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rst.homeautomation.state.PowerStateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ServiceJSonProcessorTest {

    public ServiceJSonProcessorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getServiceAttributeType method, of class ServiceJSonProcessor.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetServiceAttributeType() throws Exception {
        System.out.println("getServiceAttributeType");
        Object serviceAttribute = 3.141d;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.DOUBLE.getProtoType().toString(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = true;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.BOOLEAN.getProtoType().toString(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 2.7f;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.FLOAT.getProtoType().toString(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 12;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.INTEGER.getProtoType().toString(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 42l;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.LONG.getProtoType().toString(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = "This is a test!";
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.STRING.getProtoType().toString(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = PowerStateType.PowerState.newBuilder().setValue(PowerStateType.PowerState.State.ON).build();
        assertEquals(serviceAttribute.getClass().getName(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = PowerStateType.PowerState.State.OFF;
        assertEquals(serviceAttribute.getClass().getName(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = ServiceJSonProcessor.JavaTypeToProto.BOOLEAN;
        assertEquals(serviceAttribute.getClass().getName(), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));
    }

    /**
     * Test of de-/serialize methods, of class ServiceJSonProcessor.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testSerializationPipeline() throws Exception {
        System.out.println("SerializationPipeline");
        Object serviceAttribute = 3.141d;
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = true;
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 2.7f;
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 12;
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 42l;
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = "This is a test!";
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = PowerStateType.PowerState.newBuilder().setValue(PowerStateType.PowerState.State.ON).build();
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = PowerStateType.PowerState.State.OFF;
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = ServiceJSonProcessor.JavaTypeToProto.BOOLEAN;
        assertEquals(serviceAttribute, ServiceJSonProcessor.deserialize(ServiceJSonProcessor.serialize(serviceAttribute), ServiceJSonProcessor.getServiceAttributeType(serviceAttribute)));
    }
}