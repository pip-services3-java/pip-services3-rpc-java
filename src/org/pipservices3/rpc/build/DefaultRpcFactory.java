package org.pipservices3.rpc.build;

import org.pipservices3.components.build.*;
import org.pipservices3.rpc.services.*;
import org.pipservices3.commons.refer.*;

/**
 * Creates RPC components by their descriptors.
 * 
 * @see <a href="https://pip-services3-java.github.io/pip-services3-components-java//org/pipservices3/components/build/Factory.html">Factory</a>
 * @see HttpEndpoint
 * @see HeartbeatRestService
 * @see StatusRestService 
 */
public class DefaultRpcFactory extends Factory {

	public static final Descriptor Descriptor = new Descriptor("pip-services3", "factory", "net", "default", "1.0");
	public static final Descriptor HttpEndpointDescriptor = new Descriptor("pip-services3", "endpoint", "http", "*",
			"1.0");
	public static final Descriptor StatusServiceDescriptor = new Descriptor("pip-services3", "status-service", "http",
			"*", "1.0");
	public static final Descriptor HeartbeatServiceDescriptor = new Descriptor("pip-services3", "heartbeat-service",
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
