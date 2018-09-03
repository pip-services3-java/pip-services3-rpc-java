package org.pipservices.services;

import org.pipservices.commons.refer.Descriptor;
import org.pipservices.services.CommandableHttpService;

public class DummyCommandableHttpService extends CommandableHttpService{

	public DummyCommandableHttpService() {
		super("dummy");
		_dependencyResolver.put("controller", new Descriptor("pip-services-dummies", "controller", "default", "*", "1.0"));
	}

}
