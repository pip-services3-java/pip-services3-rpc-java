package org.pipservices3.rpc.clients;


import org.pipservices3.commons.data.*;
import org.pipservices3.commons.errors.*;
import org.pipservices3.rpc.Dummy;


public interface IDummyClient {	
	DataPage<Dummy> getDummies(String correlationId, FilterParams filter, PagingParams paging) throws ApplicationException;
    Dummy getDummyById(String correlationId, String id) throws ApplicationException;
    Dummy createDummy(String correlationId, Dummy entity) throws ApplicationException;
    Dummy updateDummy(String correlationId, Dummy entity) throws ApplicationException;
    Dummy deleteDummy(String correlationId, String id) throws ApplicationException;
    String checkCorrelationId(String correlationId) throws ApplicationException;
    void raiseException(String correlationId) throws ApplicationException;
}
