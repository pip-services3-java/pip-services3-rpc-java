package org.pipservices3.rpc.services;

import org.pipservices3.commons.refer.Descriptor;

public class DummyCommandableHttpService extends CommandableHttpService {

    public DummyCommandableHttpService() {
        super("dummy");
        _dependencyResolver.put("controller", new Descriptor("pip-services-dummies", "controller", "default", "*", "1.0"));
    }

    @Override
    public void register() {
        if (!this._swaggerAuto && this._swaggerEnable)
            this.registerOpenApiSpec("swagger yaml content");

        super.register();
    }

}
