package org.pipservices.clients;

import org.junit.Test;
import org.pipservices.commons.config.ConfigParams;
import org.pipservices.commons.errors.ApplicationException;
import org.pipservices.commons.refer.Descriptor;
import org.pipservices.commons.refer.References;
import org.pipservices.*;
import org.pipservices.example.*;
import org.pipservices.services.DummyCommandableHttpService;

public class DummyCommandableHttpByUriClientTest { ///implements IDisposable

	private static final ConfigParams RestConfig = ConfigParams.fromTuples(
            "connection.protocol", "http",
            "connection.uri", "localhost:3000"
        );

        private final DummyController _ctrl;
        private final DummyCommandableHttpClient _client;
        private final DummyClientFixture _fixture;
        //private final CancellationTokenSource _source;

        private final DummyCommandableHttpService _service;

        public DummyCommandableHttpByUriClientTest() throws InterruptedException, ApplicationException {
            _ctrl = new DummyController();

            _service = new DummyCommandableHttpService();

            _client = new DummyCommandableHttpClient();

            References references = References.fromTuples(
                new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl,
                new Descriptor("pip-services-dummies", "service", "rest", "default", "1.0"), _service,
                new Descriptor("pip-services-dummies", "client", "rest", "default", "1.0"), _client
            );
            _service.configure(RestConfig);
            _client.configure(RestConfig);

            _client.setReferences(references);
            _service.setReferences(references);

            _service.open(null);
            _service.wait();

            _fixture = new DummyClientFixture(_client);

            //_source = new CancellationTokenSource();

            _client.open(null);
            _client.wait();
        }

        @Test
        public void testCrudOperations() throws ApplicationException
        {
            _fixture.testCrudOperations();
            
        }

        public void close() throws ApplicationException, InterruptedException
        {
            _client.close(null);
            _client.wait();

            _service.close(null);
            _service.wait();
        }
}
