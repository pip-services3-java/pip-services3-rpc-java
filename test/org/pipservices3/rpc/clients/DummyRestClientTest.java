package org.pipservices3.rpc.clients;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.commons.refer.References;
import org.pipservices3.rpc.DummyController;
import org.pipservices3.rpc.services.DummyRestService;

import static org.junit.Assert.assertNotNull;

public class DummyRestClientTest {
    static DummyRestService service;
    static DummyRestClient client;
    static DummyClientFixture fixture;

    static ConfigParams restConfig = ConfigParams.fromTuples(
            "connection.protocol", "http",
            "connection.host", "localhost",
            "connection.port", 3000,
            "options.correlation_id_place", "headers"
    );

    @BeforeClass
    public static void setupClass() throws ApplicationException {
        var ctrl = new DummyController();

        service = new DummyRestService();
        service.configure(restConfig);

        References references = References.fromTuples(
                new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), ctrl,
                new Descriptor("pip-services-dummies", "service", "rest", "default", "1.0"), service
        );

        service.setReferences(references);

        service.open(null);
    }

    @AfterClass
    public static void teardown() throws ApplicationException {
        service.close(null);
    }

    @Before
    public void setup() throws ApplicationException {
        client = new DummyRestClient();
        fixture = new DummyClientFixture(client);

        client.configure(restConfig);
        client.setReferences(new References());

        client.open(null);
    }

    @Test
    public void testCrudOperations() throws ApplicationException {
        fixture.testCrudOperations();
    }

    @Test
    public void testExceptionPropagation() {
        ApplicationException err = null;
        try {
            client.raiseException("123");
            //_client.wait();
        } catch (ApplicationException ex) {
            err = ex;
        }

        assertNotNull(err);
        assertEquals(err.getCode(), "TEST_ERROR");
    }
}
