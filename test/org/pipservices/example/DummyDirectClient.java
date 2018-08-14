package org.pipservices.example;

import org.pipservices.commons.data.DataPage;
import org.pipservices.commons.data.FilterParams;
import org.pipservices.commons.data.PagingParams;
import org.pipservices.commons.refer.Descriptor;
import org.pipservices.Dummy;
import org.pipservices.direct.DirectClient;

public class DummyDirectClient extends DirectClient<IDummyController> implements IDummyClient{

	public DummyDirectClient()
    {
        _dependencyResolver.put("controller", new Descriptor("pip-services-dummies", "controller", "*", "*", "*"));
    }
	
	@Override
	public boolean isOpened() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DataPage<Dummy> getPageByFilter(String correlationId, FilterParams filter, PagingParams paging) {
		filter = filter != null ? filter : new FilterParams();
        paging = paging != null ? paging : new PagingParams();
        
//        Timing timing = instrument(correlationId, "dummy.get_page_by_filter");
        {
            return _controller.getPageByFilter(correlationId, filter, paging);
        }       
	}

	@Override
	public Dummy getOneById(String correlationId, String id) {
		return _controller.getOneById(correlationId, id);
	}

	@Override
	public Dummy create(String correlationId, Dummy entity) {
		return _controller.create(correlationId, entity);
	}

	@Override
	public Dummy update(String correlationId, Dummy entity) {
		return _controller.update(correlationId, entity);
	}

	@Override
	public Dummy deleteById(String correlationId, String id) {
		return _controller.deleteById(correlationId, id);
	}

	@Override
	public void raiseException(String correlationId) {
		_controller.raiseException(correlationId);
		
	}

}
