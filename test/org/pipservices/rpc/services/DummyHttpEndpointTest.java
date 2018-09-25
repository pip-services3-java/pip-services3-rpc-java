package org.pipservices.rpc.services;

import org.junit.*;

import static org.junit.Assert.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.convert.JsonConverter;
import org.pipservices.commons.refer.Descriptor;
import org.pipservices.commons.refer.References;
import org.pipservices.commons.run.Parameters;
import org.pipservices.rpc.Dummy;
import org.pipservices.rpc.DummyController;
import org.pipservices.rpc.services.HttpEndpoint;

public class DummyHttpEndpointTest {

	private static final ConfigParams RestConfig = ConfigParams.fromTuples(
		"connection.protocol", "http",
		"connection.host", "localhost",
		"connection.port", 3001
	);

	private DummyController _ctrl;
	private DummyCommandableHttpService _serviceV1;
	private DummyCommandableHttpService _serviceV2;

	private HttpEndpoint _httpEndpoint;

	@Before
	public void setUp() throws Exception {
		_ctrl = new DummyController();
		_serviceV1 = new DummyCommandableHttpService();
		_serviceV2 = new DummyCommandableHttpService();

		_httpEndpoint = new HttpEndpoint();

		References references = References.fromTuples(
				new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl,
				new Descriptor("pip-services", "endpoint", "http", "default", "1.0"), _httpEndpoint);

		_serviceV1.configure(ConfigParams.fromTuples("base_route", "/v1/dummy"));

		_serviceV2.configure(ConfigParams.fromTuples("base_route", "/v2/dummy"));

		_httpEndpoint.configure(RestConfig);

		_serviceV1.setReferences(references);
		_serviceV2.setReferences(references);

		_httpEndpoint.open(null);
		_serviceV1.open(null);
		_serviceV2.open(null);

		// _httpEndpoint.wait();
	}

	@After
	public void close() throws Exception {
		_serviceV1.close(null);
		_serviceV2.close(null);
		_httpEndpoint.open(null);
	}

	@Test
	public void testCrudOperations() throws Exception {
		itShouldBeOpened();

		itShouldCreateDummy();

		itShouldGetDummy();

//		itShouldPingDummy();
	}

	public void itShouldBeOpened() {
		assertTrue(_httpEndpoint.isOpen());
	}

	public void itShouldCreateDummy() throws Exception {
		Dummy newDummy = new Dummy("1", "Key 1", "Content 1", true);

		Dummy resultDummy = invoke(
			Dummy.class,
			"/v1/dummy/create_dummy",
			Parameters.fromTuples("dummy", newDummy)
		);

		assertNotNull(resultDummy);
		assertNotNull(resultDummy.getId());
		assertEquals(newDummy.getKey(), resultDummy.getKey());
		assertEquals(newDummy.getContent(), resultDummy.getContent());
	}

	public void itShouldGetDummy() throws Exception {
		Dummy existingDummy = new Dummy("1", "Key 1", "Content 1", true);

		Dummy resultDummy = invoke(
			Dummy.class,
			"/v1/dummy/get_dummy_by_id",
			Parameters.fromTuples("dummy_id", existingDummy.getId())
		);

		assertNotNull(resultDummy);
		assertNotNull(resultDummy.getId());
		assertEquals(existingDummy.getKey(), resultDummy.getKey());
		assertEquals(existingDummy.getContent(), resultDummy.getContent());
	}

//	public void itShouldPingDummy() throws JsonMappingException, JsonParseException, IOException {
//		// Todo: There is no operation ping_dummy
//		String result = sendPostRequest(
//			"/v2/dummy/ping_dummy",
//			Parameters.fromTuples("dummy", new Dummy())
//		);
//
//		assertNotNull(result);
//
//		boolean resultPing = JsonConverter.fromJson(Boolean.class, result);
//
//		assertTrue(resultPing);
//	}

	private static <T> T invoke(Class<T> type, String route, Object entity) throws Exception {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(new JacksonFeature());
		Client httpClient = ClientBuilder.newClient(clientConfig);

		String content = JsonConverter.toJson(entity);
		Response response = httpClient.target("http://localhost:3001" + route)
			.request(MediaType.APPLICATION_JSON)
			.post(Entity.entity(content, MediaType.APPLICATION_JSON));

		return response.readEntity(type);

	}

}
