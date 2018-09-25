package org.pipservices.rpc.clients;

import org.pipservices.commons.config.*;
import org.pipservices.components.count.*;
import org.pipservices.commons.errors.*;
import org.pipservices.components.log.*;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.*;

public abstract class DirectClient<T> implements IConfigurable, IOpenable, IReferenceable {
	protected T _controller;
	protected CompositeLogger _logger = new CompositeLogger();
	protected CompositeCounters _counters = new CompositeCounters();
	protected DependencyResolver _dependencyResolver = new DependencyResolver();
	protected boolean _opened = false;

	public DirectClient() {
		_dependencyResolver.put("controller", "none");
	}

	public void configure(ConfigParams config) throws ConfigException {
		_dependencyResolver.configure(config);
	}

	@SuppressWarnings("unchecked")
	public void setReferences(IReferences references) throws ReferenceException {
		_logger.setReferences(references);
		_counters.setReferences(references);

		_dependencyResolver.setReferences(references);
		_controller = (T) _dependencyResolver.getOneRequired("controller");
	}

	public boolean isOpen() {
		return _opened;
	}

	public void open(String correlationId) throws ConnectionException {
		if (isOpen()) return;

		if (_controller == null) {
			throw new ConnectionException(correlationId, "NO_CONTROLLER", "Controller reference is missing");
		}

		_logger.info(correlationId, "Opened Direct client {0}", this.getClass().getName());

		_opened = true;
		return;
	}

	public void close(String correlationId) {
		if (isOpen()) {
			_logger.debug(correlationId, "Closed Direct client {0}", this.getClass().getName());
		}

		_opened = false;

		return;
	}

	protected Timing instrument(String correlationId, String methodName) {
		String typeName = this.getClass().getName();
		_logger.trace(correlationId, "Calling %s method of %s", methodName, typeName);
		return _counters.beginTiming(typeName + "." + methodName + ".call_time");
	}

}
