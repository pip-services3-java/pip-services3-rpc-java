package org.pipservices3.rpc.services;

import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.rpc.services.CommandableHttpService;

public class DummyCommandableHttpService extends CommandableHttpService{

	public DummyCommandableHttpService() {
		super("dummy");
		_dependencyResolver.put("controller", new Descriptor("pip-services-dummies", "controller", "default", "*", "1.0"));
	}

}
