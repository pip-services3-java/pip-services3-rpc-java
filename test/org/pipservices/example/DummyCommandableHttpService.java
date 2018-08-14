package org.pipservices.example;

import org.pipservices.commons.refer.Descriptor;
import org.pipservices.rest.CommandableHttpService;

public class DummyCommandableHttpService extends CommandableHttpService{

	public DummyCommandableHttpService() {
		super("dummy");
		_dependencyResolver.put("controller", new Descriptor("pip-services-dummies", "controller", "default", "*", "1.0"));
	}

}
