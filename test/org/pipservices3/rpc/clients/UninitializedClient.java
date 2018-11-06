package org.pipservices3.rpc.clients;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.errors.ConfigException;

public class UninitializedClient {

	@Test
	public void testImproperProtocol() throws Exception {
		ConfigParams restConfig = ConfigParams.fromTuples("connection.protocol", "ftp");

		DummyCommandableHttpClient _client;

		_client = new DummyCommandableHttpClient();

		_client.configure(restConfig);

		try {
			_client.open(null);
		} catch (Exception ex) {
			assertTrue("WRONG_PROTOCOL", ex instanceof ConfigException);
		}

		_client.close(null);
	}

	@Test
	public void testNoHostPortUriSet() throws Exception {
		ConfigParams restConfig = ConfigParams.fromTuples(
			"connection.protocol", "http",
			"connection.port", "0" // default
			// is
			// 8080
		);

		DummyCommandableHttpClient _client;

		_client = new DummyCommandableHttpClient();
		_client.configure(restConfig);

		try {
			_client.open(null);
		} catch (Exception ex) {
			assertTrue("NO_PORT", ex instanceof ConfigException);
		}

		_client.close(null);
	}
}
