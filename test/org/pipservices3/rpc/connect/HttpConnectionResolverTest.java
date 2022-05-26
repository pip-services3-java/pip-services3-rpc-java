package org.pipservices3.rpc.connect;

import static org.junit.Assert.*;

import org.junit.Test;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.components.connect.ConnectionParams;
import org.pipservices3.commons.errors.ApplicationException;

public class HttpConnectionResolverTest {

	public HttpConnectionResolverTest() {
	}

	@Test
	public void testResolveUri() throws ApplicationException {
		var resolver = new HttpConnectionResolver();
		resolver.configure(ConfigParams.fromTuples(
				"connection.uri", "http://somewhere.com:777"
		));

		var connection = resolver.resolve(null);
		assertEquals("http", connection.getAsString("protocol"));
		assertEquals("somewhere.com", connection.getAsString("host"));
		assertEquals(777, connection.getAsInteger("port"));
	}

	@Test
	public void testResolveParameters() throws ApplicationException {
		var resolver = new HttpConnectionResolver();
		resolver.configure(ConfigParams.fromTuples(
				"connection.protocol", "http",
				"connection.host", "somewhere.com",
				"connection.port", 777
		));

		var connection = resolver.resolve(null);
		assertEquals("http://somewhere.com:777", connection.getAsString("uri"));
	}

	@Test
	public void testHttpsWithCredentialsConnectionParams() throws ApplicationException {
		var resolver = new HttpConnectionResolver();
		resolver.configure(ConfigParams.fromTuples(
				"connection.host", "somewhere.com",
				"connection.port", 123,
				"connection.protocol", "https",
				"credential.ssl_key_file", "ssl_key_file",
				"credential.ssl_crt_file", "ssl_crt_file"
		));

		var connection = resolver.resolve(null);
		assertEquals("https", connection.getAsString("protocol"));
		assertEquals("somewhere.com", connection.getAsString("host"));
		assertEquals(123, connection.getAsInteger("port"));
		assertEquals("https://somewhere.com:123", connection.getAsString("uri"));
		assertEquals("ssl_key_file", connection.getAsString("ssl_key_file"));
		assertEquals("ssl_crt_file", connection.getAsString("ssl_crt_file"));
	}

	@Test
	public void testHttpsWithNoCredentialsConnectionParams() throws ApplicationException {
		var resolver = new HttpConnectionResolver();
		resolver.configure(ConfigParams.fromTuples(
				"connection.host", "somewhere.com",
				"connection.port", 123,
				"connection.protocol", "https",
				"credential.internal_network", "internal_network"
		));

		var connection = resolver.resolve(null);
		assertEquals("https", connection.getAsString("protocol"));
		assertEquals("somewhere.com", connection.getAsString("host"));
		assertEquals(123, connection.getAsInteger("port"));
		assertEquals("https://somewhere.com:123", connection.getAsString("uri"));
		assertNull(connection.getAsString("internal_network"));
	}

	@Test
	public void testHttpsWithMissingCredentialsConnectionParams() throws ApplicationException {
		// section missing
		var resolver = new HttpConnectionResolver();
		resolver.configure(ConfigParams.fromTuples(
				"connection.host", "somewhere.com",
				"connection.port", 123,
				"connection.protocol", "https"
		));

		try {
			resolver.resolve(null);
			fail("Should throw an exception");
		} catch (ApplicationException err) {
			assertEquals("NO_CREDENTIAL", err.getCode());
//			assertEquals("NO_CREDENTIAL", err.name);
			assertEquals("SSL certificates are not configured for HTTPS protocol", err.getMessage());
			assertEquals("Misconfiguration", err.getCategory());
		}

		// ssl_crt_file missing
		resolver = new HttpConnectionResolver();
		resolver.configure(ConfigParams.fromTuples(
				"connection.host", "somewhere.com",
				"connection.port", 123,
				"connection.protocol", "https",
				"credential.ssl_key_file", "ssl_key_file"
		));

		try {
			resolver.resolve(null);
			fail("Should throw an exception");
		} catch (ApplicationException err) {
			assertEquals("NO_SSL_CRT_FILE", err.getCode());
//			assertEquals("NO_SSL_CRT_FILE", err.name);
			assertEquals("SSL crt file is not configured in credentials", err.getMessage());
			assertEquals("Misconfiguration", err.getCategory());
		}

		// ssl_key_file missing
		resolver = new HttpConnectionResolver();
		resolver.configure(ConfigParams.fromTuples(
				"connection.host", "somewhere.com",
				"connection.port", 123,
				"connection.protocol", "https",
				"credential.ssl_crt_file", "ssl_crt_file"
		));

		try {
			resolver.resolve(null);
			fail("Should throw an exception");
		} catch (ApplicationException err) {
			assertEquals("NO_SSL_KEY_FILE", err.getCode());
//			assertEquals("NO_SSL_KEY_FILE", err.name);
			assertEquals("SSL key file is not configured in credentials", err.getMessage());
			assertEquals("Misconfiguration", err.getCategory());
		}

		// ssl_key_file,  ssl_crt_file present
		resolver = new HttpConnectionResolver();
		resolver.configure(ConfigParams.fromTuples(
				"connection.host", "somewhere.com",
				"connection.port", 123,
				"connection.protocol", "https",
				"credential.ssl_key_file", "ssl_key_file",
				"credential.ssl_crt_file", "ssl_crt_file"
		));

		var connection = resolver.resolve(null);
		assertEquals("https", connection.getAsString("protocol"));
		assertEquals("somewhere.com", connection.getAsString("host"));
		assertEquals(123, connection.getAsInteger("port"));
		assertEquals("https://somewhere.com:123", connection.getAsString("uri"));
		assertEquals("ssl_key_file", connection.getAsString("ssl_key_file"));
		assertEquals("ssl_crt_file", connection.getAsString("ssl_crt_file"));
	}
}
