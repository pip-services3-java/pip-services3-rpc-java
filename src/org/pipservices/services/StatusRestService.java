package org.pipservices.services;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.errors.ConfigException;
import org.pipservices.components.info.ContextInfo;
import org.pipservices.commons.refer.Descriptor;
import org.pipservices.commons.refer.IReferences;
import org.pipservices.commons.refer.ReferenceException;

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
		ContextInfo context = new ContextInfo();
		context.setContextId(_contextInfo != null ? _contextInfo.getContextId() : "");
		context.setName(_contextInfo != null ? _contextInfo.getName() : "Unknown");
		context.setDescription(_contextInfo != null ? _contextInfo.getDescription() : "");
		context.setUptime(ChronoUnit.MILLIS.between(ZonedDateTime.now(), _startTime));
		context.setProperties(_contextInfo.getProperties());
		List<String> components = new ArrayList<String>();
		if (_references != null) {
			for (Object locator : _references.getAllLocators())
				components.add(locator.toString());
		}
		context.setComponents(components);

		return sendResult(context);
	}

}
