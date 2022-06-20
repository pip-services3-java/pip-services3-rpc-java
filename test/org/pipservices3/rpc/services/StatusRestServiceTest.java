package org.pipservices3.rpc.services;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;

import static org.junit.Assert.*;

import org.glassfish.jersey.client.*;
import org.glassfish.jersey.jackson.*;
import org.junit.*;
import org.pipservices3.commons.config.*;
import org.pipservices3.commons.errors.*;
import org.pipservices3.commons.refer.*;
import org.pipservices3.components.info.*;

public class StatusRestServiceTest {
    private StatusRestService _service;

    static int port = 3006;
	@Before
	public void setUp() throws ApplicationException {
        ConfigParams config = ConfigParams.fromTuples(
            "connection.protocol", "http",
            "connection.host", "localhost",
            "connection.port", port
        );
        _service = new StatusRestService();
        _service.configure(config);

        ContextInfo contextInfo = new ContextInfo();
        contextInfo.setName("Test");
        contextInfo.setDescription("This is a test container");

        References references = References.fromTuples(
            new Descriptor("pip-services3", "context-info", "default", "default", "1.0"), contextInfo,
            new Descriptor("pip-services3", "status-service", "http", "default", "1.0"), _service
        );
        _service.setReferences(references);
        
        _service.open(null);
	}

	@After
	public void tearDown() throws ApplicationException {
        _service.close(null);		
	}
	
	@Test
    public void testStatus() throws Exception {
        Object value = invoke(Object.class, "/status");
        assertNotNull(value);
    }
	
	
	private static <T> T invoke(Class<T> responseClass, String route) throws Exception {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(new JacksonFeature());
		Client httpClient = ClientBuilder.newClient(clientConfig);
	
		Response response = httpClient.target("http://localhost:" + port + route)
			.request(MediaType.APPLICATION_JSON)
			.get();
	
		return response.readEntity(responseClass);
	}
	
}
