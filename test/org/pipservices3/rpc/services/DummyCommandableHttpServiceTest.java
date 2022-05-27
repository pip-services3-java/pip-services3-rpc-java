package org.pipservices3.rpc.services;

import org.junit.*;

import static org.junit.Assert.*;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;

import org.glassfish.jersey.client.*;
import org.glassfish.jersey.jackson.*;
import org.pipservices3.commons.config.*;
import org.pipservices3.commons.convert.JsonConverter;
import org.pipservices3.commons.data.*;
import org.pipservices3.commons.errors.ApplicationException;
import org.pipservices3.commons.errors.ErrorDescription;
import org.pipservices3.commons.refer.*;
import org.pipservices3.commons.run.*;
import org.pipservices3.components.log.ConsoleLogger;
import org.pipservices3.rpc.*;

import java.util.List;

public class DummyCommandableHttpServiceTest {
    private final Dummy DUMMY1 = new Dummy(null, "Key 1", "Content 1",
            List.of(new SubDummy("SubKey 1", "SubContent 1")));
    private final Dummy DUMMY2 = new Dummy(null, "Key 2", "Content 2",
            List.of(new SubDummy("SubKey 2", "SubContent 2")));

    private static final ConfigParams restConfig = ConfigParams.fromTuples(
            "connection.protocol", "http",
            "connection.host", "localhost",
            "connection.port", 3000,
            "swagger.enable", "true"
    );

    private DummyController _ctrl;
    private DummyCommandableHttpService _service;

    @Before
    public void setUp() throws Exception {
        _ctrl = new DummyController();
        _service = new DummyCommandableHttpService();

        _service.configure(restConfig);

        References references = References.fromTuples(
                new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl,
                new Descriptor("pip-services-dummies", "service", "http", "default", "1.0"), _service,
                new Descriptor("pip-services-dummies", "logger", "*", "*", "*"), new ConsoleLogger()
        );

        _service.setReferences(references);

        _service.open(null);
    }

    @After
    public void close() throws Exception {
        _service.close(null);
    }

    @Test
    public void testCrudOperations() throws Exception {
        // Create one dummy
        Dummy dummy1 = invoke(
                Dummy.class,
                "/dummy/create_dummy",
                Parameters.fromTuples("dummy", DUMMY1)
        );

        assertNotNull(dummy1);
        assertNotNull(dummy1.getId());
        assertEquals(DUMMY1.getKey(), dummy1.getKey());
        assertEquals(DUMMY1.getContent(), dummy1.getContent());

        // Create another dummy
        Dummy dummy2 = invoke(
                Dummy.class,
                "/dummy/create_dummy",
                Parameters.fromTuples("dummy", DUMMY2)
        );

        assertNotNull(dummy2);
        assertNotNull(dummy2.getId());
        assertEquals(DUMMY2.getKey(), dummy2.getKey());
        assertEquals(DUMMY2.getContent(), dummy2.getContent());

        // Get all dummies
        DataPage<Dummy> dummies = invoke(
                new GenericType<DataPage<Dummy>>() {
                },
                "/dummy/get_dummies",
                null
        );
        assertNotNull(dummies);
        assertEquals(2, dummies.getData().size());

        // Update the dummy
        dummy1.setContent("Updated Content 1");
        Dummy dummy = invoke(
                Dummy.class,
                "/dummy/update_dummy",
                Parameters.fromTuples("dummy", dummy1)
        );

        assertNotNull(dummy);
        assertEquals(dummy1.getId(), dummy.getId());
        assertEquals(dummy1.getKey(), dummy.getKey());
        assertEquals("Updated Content 1", dummy.getContent());

        dummy1 = dummy;

        // Get the dummy by id
        dummy = invoke(
                Dummy.class,
                "/dummy/get_dummy_by_id",
                Parameters.fromTuples("dummy_id", dummy1.getId())
        );

        assertNotNull(dummy);
        assertEquals(dummy.getId(), dummy1.getId());
        assertEquals(dummy.getKey(), dummy1.getKey());

        // Delete the dummy
        invoke(
                Dummy.class,
                "/dummy/delete_dummy",
                Parameters.fromTuples("dummy_id", dummy1.getId())
        );

        // Try to get deleted dummy
        dummy = invoke(
                Dummy.class,
                "/dummy/get_dummy_by_id",
                Parameters.fromTuples("dummy_id", dummy1.getId())
        );
        assertNull(dummy);
    }

    @Test
    public void testFailedValidation() {
        // Create one dummy with an invalid id
        var err = invoke(
                ErrorDescription.class,
                "/dummy/create_dummy",
                Parameters.fromTuples()
        );

        assertNotNull(err);
        assertEquals(err.getCode(), "INVALID_DATA");
    }

    @Test
    public void testCheckCorrelationId() throws Exception {
        // check transmit correllationId over params
        String result = invoke(String.class,
                "/dummy/check_correlation_id?correlation_id=test_cor_id",
                null);
        var mapRes = JsonConverter.toMap(result);
        assertEquals("test_cor_id", mapRes.get("correlation_id"));

        var headers = new MultivaluedHashMap<String, Object>();

        headers.add("correlation_id", "test_cor_id_header");
        // check transmit correllationId over header
        result = invoke(String.class,
                "/dummy/check_correlation_id",
                null, headers);
        mapRes = JsonConverter.toMap(result);

        assertEquals("test_cor_id_header", mapRes.get("correlation_id"));
    }

    @Test
    public void testGetOpenApiSpec() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JacksonFeature());
        Client httpClient = ClientBuilder.newClient(clientConfig);

        var url = "http://localhost:3000";
        var response = httpClient.target(url + "/dummy/swagger")
                .request(MediaType.APPLICATION_JSON).get();
        var res = response.readEntity(String.class);
        assertTrue(res.startsWith("openapi"));
    }

    @Test
    public void testOpenApiSpecOverride() throws ApplicationException {
        var openApiContent = "swagger yaml content";

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JacksonFeature());
        Client httpClient = ClientBuilder.newClient(clientConfig);

        var url = "http://localhost:3000";

        // recreate service with new configuration
        _service.close(null);

        var config = restConfig.setDefaults(ConfigParams.fromTuples("swagger.auto", false));

        var ctrl = new DummyController();

        _service = new DummyCommandableHttpService();
        _service.configure(config);

        var references = References.fromTuples(
                new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), ctrl,
                new Descriptor("pip-services-dummies", "service", "http", "default", "1.0"), _service
        );

        _service.setReferences(references);

        _service.open(null);

        var response = httpClient.target(url + "/dummy/swagger")
                .request(MediaType.APPLICATION_JSON).get();
        var res = response.readEntity(String.class);

        assertEquals(openApiContent, res);

        _service.close(null);
    }

    private static Response performInvoke(String route, Object entity, MultivaluedMap<String, Object> headers) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JacksonFeature());
        Client httpClient = ClientBuilder.newClient(clientConfig);

        return httpClient.target("http://localhost:3003" + route)
                .request(MediaType.APPLICATION_JSON).headers(headers)
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON));
    }

    private static Response performInvoke(String route, Object entity) {
        return performInvoke(route, entity, null);
    }

    private static <T> T invoke(Class<T> type, String route, Object entity) {
        Response response = performInvoke(route, entity);
        return response.readEntity(type);
    }

    private static <T> T invoke(Class<T> type, String route, Object entity, MultivaluedMap<String, Object> headers) {
        Response response = performInvoke(route, entity, headers);
        return response.readEntity(type);
    }

    private static <T> T invoke(GenericType<T> type, String route, Object entity) throws Exception {
        Response response = performInvoke(route, entity);
        return response.readEntity(type);
    }

}
