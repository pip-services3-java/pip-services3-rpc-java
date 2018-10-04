package org.pipservices.rpc.build;

import org.pipservices.components.build.*;
import org.pipservices.rpc.services.*;
import org.pipservices.commons.refer.*;

/**
 * Creates RPC components by their descriptors.
 * 
 * @see Factory
 * @see HttpEndpoint
 * @see HeartbeatRestService
 * @see StatusRestService 
 */
public class DefaultRpcFactory extends Factory {

	public static final Descriptor Descriptor = new Descriptor("pip-services", "factory", "net", "default", "1.0");
	public static final Descriptor HttpEndpointDescriptor = new Descriptor("pip-services", "endpoint", "http", "*",
			"1.0");
	public static final Descriptor StatusServiceDescriptor = new Descriptor("pip-services", "status-service", "http",
			"*", "1.0");
	public static final Descriptor HeartbeatServiceDescriptor = new Descriptor("pip-services", "heartbeat-service",
			"http", "*", "1.0");

	/**
	 * Create a new instance of the factory.
	 */
	public DefaultRpcFactory() {
		registerAsType(HttpEndpointDescriptor, HttpEndpoint.class);
		registerAsType(StatusServiceDescriptor, StatusRestService.class);
		registerAsType(HeartbeatServiceDescriptor, HeartbeatRestService.class);
	}
}
