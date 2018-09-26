package org.pipservices.rpc.clients;

import org.pipservices.commons.config.*;
import org.pipservices.components.count.*;
import org.pipservices.commons.errors.*;
import org.pipservices.components.log.*;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.*;

/**
 * Abstract client that calls controller directly in the same memory space.
 * 
 * It is used when multiple microservices are deployed in a single container (monolyth)
 * and communication between them can be done by direct calls rather then through 
 * the network.
 * 
 * ### Configuration parameters ###
 * 
 * dependencies:
 *   controller:            override controller descriptor
 * 
 * ### References ###
 * 
 * - *:logger:*:*:1.0         (optional) ILogger components to pass log messages
 * - *:counters:*:*:1.0       (optional) ICounters components to pass collected measurements
 * - *:controller:*:*:1.0     controller to call business methods
 * <p>
 * ### Example ###
 * <pre>
 * {@code
 * class MyDirectClient extends DirectClient<IMyController> implements IMyClient {
 * 
 *   public MyDirectClient() {
 *       super();
 *       this._dependencyResolver.put('controller', new Descriptor(
 *           "mygroup", "controller", "*", "*", "*"));
 *    }
 *    ...
 * 
 *    public MyData getData(String correlationId, String id) {        
 *        Timing timing = this.instrument(correlationId, 'myclient.get_data');
 *        MyData result = this._controller.getData(correlationId, id);
 *        timing.endTiming();
 *        return result;        
 *    }
 *    ...
 * }
 * 
 * MyDirectClient client = new MyDirectClient();
 * client.setReferences(References.fromTuples(
 *     new Descriptor("mygroup","controller","default","default","1.0"), controller
 * ));
 * 
 * MyData data = client.getData("123", "1");
 * ...
 * }
 * </pre>
 */
public abstract class DirectClient<T> implements IConfigurable, IOpenable, IReferenceable {
	/**
	 * The controller reference.
	 */
	protected T _controller;
	/**
	 * The logger.
	 */
	protected CompositeLogger _logger = new CompositeLogger();
	/**
	 * The performance counters
	 */
	protected CompositeCounters _counters = new CompositeCounters();
	/**
	 * The dependency resolver to get controller reference.
	 */
	protected DependencyResolver _dependencyResolver = new DependencyResolver();
	/**
	 * The open flag.
	 */
	protected boolean _opened = false;

	/**
	 * Creates a new instance of the client.
	 */
	public DirectClient() {
		_dependencyResolver.put("controller", "none");
	}

	/**
	 * Configures component by passing configuration parameters.
	 * 
	 * @param config configuration parameters to be set.
	 * @throws ConfigException when configuration is wrong.
	 */
	public void configure(ConfigParams config) throws ConfigException {
		_dependencyResolver.configure(config);
	}

	/**
	 * Sets references to dependent components.
	 * 
	 * @param references references to locate the component dependencies.
	 * @throws ReferenceException when no found references.
	 */
	@SuppressWarnings("unchecked")
	public void setReferences(IReferences references) throws ReferenceException {
		_logger.setReferences(references);
		_counters.setReferences(references);

		_dependencyResolver.setReferences(references);
		_controller = (T) _dependencyResolver.getOneRequired("controller");
	}

	/**
	 * Adds instrumentation to log calls and measure call time. It returns a Timing
	 * object that is used to end the time measurement.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @param methodName    a method name.
	 * @return Timing object to end the time measurement.
	 */
	protected Timing instrument(String correlationId, String methodName) {
		String typeName = this.getClass().getName();
		_logger.trace(correlationId, "Calling %s method of %s", methodName, typeName);
		return _counters.beginTiming(typeName + "." + methodName + ".call_time");
	}

	/**
	 * Checks if the component is opened.
	 * 
	 * @return true if the component has been opened and false otherwise.
	 */
	public boolean isOpen() {
		return _opened;
	}

	/**
	 * Opens the component.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 * @throws ConnectionException when controller reference is missing.
	 */
	public void open(String correlationId) throws ConnectionException {
		if (isOpen())
			return;

		if (_controller == null) {
			throw new ConnectionException(correlationId, "NO_CONTROLLER", "Controller reference is missing");
		}

		_logger.info(correlationId, "Opened Direct client {0}", this.getClass().getName());

		_opened = true;
		return;
	}

	/**
	 * Closes component and frees used resources.
	 * 
	 * @param correlationId (optional) transaction id to trace execution through
	 *                      call chain.
	 */
	public void close(String correlationId) {
		if (isOpen()) {
			_logger.debug(correlationId, "Closed Direct client {0}", this.getClass().getName());
		}

		_opened = false;

		return;
	}
}
