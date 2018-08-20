package org.pipservices.clients;


import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.errors.ApplicationException;
import org.pipservices.commons.errors.ConfigException;
import org.pipservices.commons.refer.Descriptor;
import org.pipservices.commons.refer.References;
import org.pipservices.DummyController;
import org.pipservices.example.*;

public class UninitializedClient {

	@Test
	public void testImproperProtocol() throws InterruptedException, ApplicationException
    {
		ConfigParams restConfig = ConfigParams.fromTuples(
            "connection.protocol", "ftp"
        );

        DummyController _ctrl;
        DummyCommandableHttpClient _client;
        DummyCommandableHttpService _service;


        _ctrl = new DummyController();

        _service = new DummyCommandableHttpService();

        _client = new DummyCommandableHttpClient();

        References references = References.fromTuples(
            new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl,
            new Descriptor("pip-services-dummies", "service", "rest", "default", "1.0"), _service,
            new Descriptor("pip-services-dummies", "client", "rest", "default", "1.0"), _client
        );
        _service.configure(restConfig);
        _client.configure(restConfig);

        _client.setReferences(references);
        _service.setReferences(references);

        _service.open(null);
        _service.wait();

        try {
        	_client.open(null);
        }catch(Exception ex) {
        	assertTrue("WRONG_PROTOCOL",ex instanceof ConfigException);
        }
        
        _client.close(null);
        _client.wait();
        
        _service.close(null);
        _service.wait();
        
    }

	@Test
    public void testNoHostPortUriSet() throws InterruptedException, ApplicationException
    {
    	ConfigParams restConfig = ConfigParams.fromTuples(
            "connection.protocol", "http",
            "connection.port", "0"          // default is 8080
            );

        DummyController _ctrl;
        DummyCommandableHttpClient _client;

        DummyCommandableHttpService _service;


        _ctrl = new DummyController();

        _service = new DummyCommandableHttpService();

        _client = new DummyCommandableHttpClient();

        References references = References.fromTuples(
            new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl,
            new Descriptor("pip-services-dummies", "service", "rest", "default", "1.0"), _service,
            new Descriptor("pip-services-dummies", "client", "rest", "default", "1.0"), _client
        );
        _service.configure(restConfig);
        _client.configure(restConfig);

        _client.setReferences(references);
        _service.setReferences(references);

        _service.open(null);
        _service.wait();

        try {
        	_client.open(null);
        }catch(Exception ex) {
        	assertTrue("NO_PORT",ex instanceof ConfigException);
        }
        
        _client.close(null);
        _client.wait();
        
        _service.close(null);
        _service.wait();
    }
}
