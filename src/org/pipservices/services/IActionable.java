package org.pipservices.services;

import org.glassfish.jersey.server.*;

public interface IActionable {
	void registerAction(ContainerRequest request, ContainerResponse response);
}
