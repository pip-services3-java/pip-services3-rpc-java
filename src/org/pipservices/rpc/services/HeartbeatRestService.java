package org.pipservices.services;

import java.time.ZonedDateTime;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.errors.ConfigException;

public class HeartbeatRestService extends RestService {
	private String _route = "heartbeat";

	public HeartbeatRestService() {}

	public void configure(ConfigParams config) throws ConfigException {
		super.configure(config);

		_route = config.getAsStringWithDefault("route", _route);
	}

	public void register() {
		registerRoute("get", _route, new Inflector<ContainerRequestContext, Response>() {
			@Override
            public Response apply(ContainerRequestContext request) {
            	return heartbeat(request);
            }
		});
	}
	
	private Response heartbeat(ContainerRequestContext request) {
		return sendResult(ZonedDateTime.now()) ;
	}
}
