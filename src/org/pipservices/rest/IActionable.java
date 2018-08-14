package org.pipservices.rest;

import org.glassfish.jersey.server.*;

public interface IActionable {

	void registerAction( ContainerRequest request, ContainerResponse response );
}
