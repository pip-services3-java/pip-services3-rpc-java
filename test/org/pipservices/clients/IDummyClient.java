package org.pipservices.clients;


import org.pipservices.commons.data.DataPage;
import org.pipservices.commons.data.FilterParams;
import org.pipservices.commons.data.PagingParams;
import org.pipservices.Dummy;


public interface IDummyClient {
	
	DataPage<Dummy> getPageByFilter(String correlationId, FilterParams filter, PagingParams paging);
    Dummy getOneById(String correlationId, String id);
    Dummy create(String correlationId, Dummy entity);
    Dummy update(String correlationId, Dummy entity);
    Dummy deleteById(String correlationId, String id);
    void raiseException(String correlationId);
}
