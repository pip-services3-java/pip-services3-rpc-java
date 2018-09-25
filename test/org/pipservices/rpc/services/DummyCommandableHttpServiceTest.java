package org.pipservices.rpc.services;

import org.junit.*;

import static org.junit.Assert.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.client.*;
import org.glassfish.jersey.jackson.*;
import org.pipservices.commons.config.*;
import org.pipservices.commons.data.*;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.*;
import org.pipservices.rpc.*;

public class DummyCommandableHttpServiceTest {
    private final Dummy DUMMY1 = new Dummy(null, "Key 1", "Content 1", true);
    private final Dummy DUMMY2 = new Dummy(null, "Key 2", "Content 2", true);

	private static final ConfigParams RestConfig = ConfigParams.fromTuples(
		"connection.protocol", "http",
		"connection.host", "localhost",
		"connection.port", 3003
	);

	private DummyController _ctrl;
	private DummyCommandableHttpService _service;

	@Before
	public void setUp() throws Exception {
		_ctrl = new DummyController();
		_service = new DummyCommandableHttpService();

		References references = References.fromTuples(
			new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl
		);

		_service.configure(RestConfig);
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
    		new GenericType<DataPage<Dummy>>() {},
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

	private static Response performInvoke(String route, Object entity) throws Exception {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(new JacksonFeature());
		Client httpClient = ClientBuilder.newClient(clientConfig);

		Response response = httpClient.target("http://localhost:3003" + route)
			.request(MediaType.APPLICATION_JSON)
			.post(Entity.entity(entity, MediaType.APPLICATION_JSON));

		return response;
	}

	private static <T> T invoke(Class<T> type, String route, Object entity) throws Exception {
		Response response = performInvoke(route, entity);
		return response.readEntity(type);
	}

	private static <T> T invoke(GenericType<T> type, String route, Object entity) throws Exception {
		Response response = performInvoke(route, entity);
		return response.readEntity(type);
	}
	
}
