package org.pipservices.rpc.services;

import org.pipservices.commons.refer.Descriptor;
import org.pipservices.rpc.services.CommandableHttpService;

public class DummyCommandableHttpService extends CommandableHttpService{

	public DummyCommandableHttpService() {
		super("dummy");
		_dependencyResolver.put("controller", new Descriptor("pip-services-dummies", "controller", "default", "*", "1.0"));
	}

}
