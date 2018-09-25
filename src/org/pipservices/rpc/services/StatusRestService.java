package org.pipservices.rpc.services;

import java.time.*;
import java.util.*;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.*;
import org.pipservices.commons.config.*;
import org.pipservices.commons.convert.*;
import org.pipservices.commons.data.*;
import org.pipservices.commons.errors.*;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.*;
import org.pipservices.components.info.*;

public class StatusRestService extends RestService {
	private ZonedDateTime _startTime = ZonedDateTime.now();
	private IReferences _references;
	private ContextInfo _contextInfo;
	private String _route = "status";

	public StatusRestService() {
		_dependencyResolver.put("context-info", new Descriptor("pip-services", "context-info", "default", "*", "1.0"));
	}

	public void configure(ConfigParams config) throws ConfigException {
		super.configure(config);

		_route = config.getAsStringWithDefault("route", _route);
	}

	public void setReferences(IReferences references) throws ReferenceException, ConfigException {
		_references = references;
		super.setReferences(references);

		_contextInfo = (ContextInfo) _dependencyResolver.getOneOptional("context-info");
	}

	public void register() {
		registerRoute("get", _route, new Inflector<ContainerRequestContext, Response>() {
			@Override
            public Response apply(ContainerRequestContext request) {
            	return status(request);
            }
		});
	}
	
	private Response status(ContainerRequestContext request) {
		String id = _contextInfo != null ? _contextInfo.getContextId() : "";
		String name = _contextInfo != null ? _contextInfo.getName() : "Unknown";
		String description = _contextInfo != null ? _contextInfo.getDescription() : "";
		long uptime = Duration.between(_startTime, ZonedDateTime.now()).toMillis();
		StringValueMap properties = _contextInfo.getProperties();

		List<String> components = new ArrayList<String>();
		if (_references != null) {
			for (Object locator : _references.getAllLocators())
				components.add(locator.toString());
		}

		Parameters status = Parameters.fromTuples(
			"id", id,
			"name", name,
			"description", description,
			"start_time", StringConverter.toString(_startTime),
			"current_time", StringConverter.toString(ZonedDateTime.now()),
			"uptime", uptime,
			"properties", properties,
			"components", components
		);
				
		return sendResult(status);
	}

}
