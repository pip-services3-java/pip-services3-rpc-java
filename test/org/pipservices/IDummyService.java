package org.pipservices;

import org.pipservices.commons.data.*;
import org.pipservices.commons.errors.*;

public interface IDummyService {
    DataPage<Dummy> getPageByFilter(String correlationId, FilterParams filter, PagingParams paging) throws ApplicationException;
    Dummy getOneById(String correlationId, String id) throws ApplicationException;
    Dummy create(String correlationId, Dummy entity) throws ApplicationException;
    Dummy update(String correlationId, Dummy entity) throws ApplicationException;
    Dummy deleteById(String correlationId, String id) throws ApplicationException;
}
