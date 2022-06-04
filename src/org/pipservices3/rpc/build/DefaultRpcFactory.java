package org.pipservices3.rpc.build;

import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.components.build.Factory;
import org.pipservices3.rpc.services.HeartbeatRestService;
import org.pipservices3.rpc.services.HttpEndpoint;
import org.pipservices3.rpc.services.StatusRestService;

/**
 * Creates RPC components by their descriptors.
 * 
 * @see <a href="https://pip-services3-java.github.io/pip-services3-components-java//org/pipservices3/components/build/Factory.html">Factory</a>
 * @see HttpEndpoint
 * @see HeartbeatRestService
 * @see StatusRestService 
 */
public class DefaultRpcFactory extends Factory {
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
