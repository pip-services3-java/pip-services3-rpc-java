package org.pipservices.direct;

import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.config.IConfigurable;
import org.pipservices.components.count.CompositeCounters;
import org.pipservices.components.count.Timing;
import org.pipservices.commons.errors.ConfigException;
import org.pipservices.commons.errors.ConnectionException;
import org.pipservices.components.log.CompositeLogger;
import org.pipservices.commons.refer.*;
import org.pipservices.commons.run.IOpenable;

public abstract class DirectClient<T> implements IConfigurable, IOpenable, IReferenceable{
	
	protected T _controller;
    protected CompositeLogger _logger = new CompositeLogger();
    protected CompositeCounters _counters = new CompositeCounters();
    protected DependencyResolver _dependencyResolver = new DependencyResolver();
    protected boolean _opened = false;
    
    public DirectClient()
    {
        _dependencyResolver.put("controller", "none");
    }
    
    public void configure(ConfigParams config) throws ConfigException
    {
        _dependencyResolver.configure(config);
    }

    @SuppressWarnings("unchecked")
	public void setReferences(IReferences references) throws ReferenceException
    {
        _logger.setReferences(references);
        _counters.setReferences(references);

        _dependencyResolver.setReferences(references);
        _controller = (T)_dependencyResolver.getOneRequired("controller");
    }
    
    public boolean IsOpened()
    {
        return _opened;
    }
    
    public void open(String correlationId) throws ConnectionException
    {
        if (IsOpened())
        {
            return;
        }

        if (_controller == null)
        {
            throw new ConnectionException(correlationId, "NO_CONTROLLER", "Controller reference is missing");
        }

        _logger.info(correlationId, "Opened Direct client {0}", this.getClass().getName());

        _opened = true;
        return;
    }
    
    public void close(String correlationId)
    {
        if (IsOpened())
        {
            _logger.debug(correlationId, "Closed Direct client {0}", this.getClass().getName());
        }

        _opened = false;

        return;
    }
    
    protected Timing instrument(String correlationId, String methodName)
    {
        String typeName = this.getClass().getName();
        _logger.trace(correlationId, "Calling {0} method of {1}", methodName, typeName);
        return _counters.beginTiming(typeName + "." + methodName + ".call_time");
    }
    
}
