package org.pipservices.rpc.clients;

import org.junit.*;
import org.pipservices.commons.config.*;
import org.pipservices.commons.errors.*;
import org.pipservices.commons.refer.*;
import org.pipservices.rpc.*;
import org.pipservices.rpc.services.*;

public class DummyCommandableHttpClientTest {

	private static final ConfigParams RestConfig = ConfigParams.fromTuples(
			"connection.uri", "http://localhost:3006"
		// "connection.protocol", "http",
		// "connection.host", "localhost",
		// "connection.port", 3000
	);

	private DummyController _ctrl;
	private DummyCommandableHttpClient _client;
	private DummyClientFixture _fixture;
	private DummyCommandableHttpService _service;

	@Before
	public void setUp() throws Exception {
		_ctrl = new DummyController();

		_service = new DummyCommandableHttpService();
		_client = new DummyCommandableHttpClient();

		_service.configure(RestConfig);
		_client.configure(RestConfig);
		
		References references = References.fromTuples(
			new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl,
			new Descriptor("pip-services-dummies", "service", "rest", "default", "1.0"), _service,
			new Descriptor("pip-services-dummies", "client", "rest", "default", "1.0"), _client
		);

		_client.setReferences(references);
		_service.setReferences(references);

		_service.open(null);
		_client.open(null);

		_fixture = new DummyClientFixture(_client);
	}

	@After
	public void tearDown() throws Exception {
		_client.close(null);
		_service.close(null);
	}

	@Test
	public void testCrudOperations() throws ApplicationException {
		_fixture.testCrudOperations();
	}

	@Test
	public void testExceptionPropagation() {
		try {
			_client.raiseException("123");
			//_client.wait();
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}
	}

}
