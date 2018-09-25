package org.pipservices.build;

import org.pipservices.components.build.*;
import org.pipservices.services.*;
import org.pipservices.commons.refer.*;

public class DefaultRpcFactory extends Factory {

	public static final Descriptor Descriptor = new Descriptor("pip-services", "factory", "net", "default", "1.0");
	public static final Descriptor HttpEndpointDescriptor = new Descriptor("pip-services", "endpoint", "http", "*", "1.0");
	public static final Descriptor StatusServiceDescriptor = new Descriptor("pip-services", "status-service", "http", "*", "1.0");
	public static final Descriptor HeartbeatServiceDescriptor = new Descriptor("pip-services", "heartbeat-service", "http", "*", "1.0");

	public DefaultRpcFactory() {
		registerAsType(HttpEndpointDescriptor, HttpEndpoint.class);
		registerAsType(StatusServiceDescriptor, StatusRestService.class);
		registerAsType(HeartbeatServiceDescriptor, HeartbeatRestService.class);
	}
}
