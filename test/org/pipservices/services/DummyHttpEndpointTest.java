package org.pipservices.services;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.convert.JsonConverter;
import org.pipservices.commons.errors.ApplicationException;
import org.pipservices.commons.refer.Descriptor;
import org.pipservices.commons.refer.References;
import org.pipservices.Dummy;
import org.pipservices.DummyController;
import org.pipservices.services.HttpEndpoint;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
//import com.sun.jersey.api.json.JSONConfiguration;

public class DummyHttpEndpointTest {

	private static final ConfigParams RestConfig = ConfigParams.fromTuples(
		"connection.protocol", "http",
		"connection.host", "localhost",
		"connection.port", 3000
	);

	private final DummyController _ctrl;
	private final DummyCommandableHttpService _serviceV1;
	private final DummyCommandableHttpService _serviceV2;

	private final HttpEndpoint _httpEndpoint;

	public DummyHttpEndpointTest() throws ApplicationException, InterruptedException {
		_ctrl = new DummyController();
		_serviceV1 = new DummyCommandableHttpService();
		_serviceV2 = new DummyCommandableHttpService();

		_httpEndpoint = new HttpEndpoint();

		References references = References.fromTuples(
			new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl,
			new Descriptor("pip-services", "endpoint", "http", "default", "1.0"), _httpEndpoint
		);

		_serviceV1.configure(ConfigParams.fromTuples("base_route", "/v1/dummy"));

		_serviceV2.configure(ConfigParams.fromTuples("base_route", "/v2/dummy"));

		_httpEndpoint.configure(RestConfig);

		_serviceV1.setReferences(references);
		_serviceV2.setReferences(references);

		_httpEndpoint.open(null);
		_serviceV1.open(null);
		_serviceV2.open(null);
		
		//_httpEndpoint.wait();
	}

	public void close() throws ApplicationException, InterruptedException {
		_serviceV1.close(null);
		_serviceV2.close(null);
		_httpEndpoint.open(null);
	}

	@Test
	public void itShouldPerformCRUDOperations() throws IOException {
		itShouldBeOpened();

		itShouldCreateDummy();

		itShouldGetDummy();

		itShouldPingDummy();
	}

	public void itShouldBeOpened() {
		assertTrue(_httpEndpoint.isOpen());
	}

	public void itShouldCreateDummy() throws IOException {
		Dummy newDummy = new Dummy("1", "Key 1", "Content 1", true);

		String result = sendPostRequest("/v1/dummy/create_dummy", newDummy);

		Dummy resultDummy = JsonConverter.fromJson(Dummy.class, result);

		assertNotNull(resultDummy);
		assertNotNull(resultDummy.getId());
		assertEquals(newDummy.getKey(), resultDummy.getKey());
		assertEquals(newDummy.getContent(), resultDummy.getContent());
	}

	public void itShouldGetDummy() throws JsonMappingException, JsonParseException, IOException {
		Dummy existingDummy = new Dummy("1", "Key 1", "Content 1", true);

		String result = sendPostRequest("/v1/dummy/get_dummy_by_id", existingDummy.getId());

		Dummy resultDummy = JsonConverter.fromJson(Dummy.class, result);

		assertNotNull(resultDummy);
		assertNotNull(resultDummy.getId());
		assertEquals(existingDummy.getKey(), resultDummy.getKey());
		assertEquals(existingDummy.getContent(), resultDummy.getContent());
	}

	public void itShouldPingDummy() throws JsonMappingException, JsonParseException, IOException {
		String result = sendPostRequest("/v2/dummy/ping_dummy", new Dummy());

		assertNotNull(result);

		boolean resultPing = JsonConverter.fromJson(Boolean.class, result);

		assertTrue(resultPing);
	}

	//Todo 
	private static String sendPostRequest(String route, Object entity) throws JsonProcessingException {
		ClientConfig clientConfig = new ClientConfig();
		//clientConfig.getProperties().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		clientConfig.register(new JacksonFeature());
		Client httpClient = ClientBuilder.newClient(clientConfig);
		ClientRequest request = new ClientRequest(null);
		request.setEntity(entity);

		// String content = JsonConverter.toJson(request); //, Encoding.UTF8,
		// "application/json"))
		Response response = httpClient.target("http://localhost:3000" + route).request(request.getMediaType()).get();

		return response.getMetadata().toString(); /// .Content.ReadAsStringAsync().Result;

	}

}
