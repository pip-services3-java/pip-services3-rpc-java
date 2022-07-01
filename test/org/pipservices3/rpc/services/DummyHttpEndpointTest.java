package org.pipservices3.rpc.services;

import org.junit.*;

import static org.junit.Assert.*;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.pipservices3.commons.config.ConfigParams;
import org.pipservices3.commons.convert.JsonConverter;
import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.commons.refer.References;
import org.pipservices3.commons.run.Parameters;
import org.pipservices3.rpc.Dummy;
import org.pipservices3.rpc.DummyController;

import java.util.ArrayList;

public class DummyHttpEndpointTest {

    static int port = 3003;
    private static final ConfigParams RestConfig = ConfigParams.fromTuples(
            "connection.protocol", "http",
            "connection.host", "localhost",
            "connection.port", port
    );

    private DummyController _ctrl;
    private DummyCommandableHttpService _serviceV1;
    private DummyCommandableHttpService _serviceV2;

    private HttpEndpoint _httpEndpoint;

    @Before
    public void setUp() throws Exception {
        _ctrl = new DummyController();
        _serviceV1 = new DummyCommandableHttpService();
        _serviceV2 = new DummyCommandableHttpService();

        _httpEndpoint = new HttpEndpoint();

        References references = References.fromTuples(
                new Descriptor("pip-services-dummies", "controller", "default", "default", "1.0"), _ctrl,
                new Descriptor("pip-services3", "endpoint", "http", "default", "1.0"), _httpEndpoint);

        _serviceV1.configure(ConfigParams.fromTuples("base_route", "/v1/dummy"));

        _serviceV2.configure(ConfigParams.fromTuples("base_route", "/v2/dummy"));

        _httpEndpoint.configure(RestConfig);

        _serviceV1.setReferences(references);
        _serviceV2.setReferences(references);

        _httpEndpoint.open(null);
        _serviceV1.open(null);
        _serviceV2.open(null);
    }

    @After
    public void close() throws Exception {
        _serviceV1.close(null);
        _serviceV2.close(null);
        _httpEndpoint.open(null);
    }

    @Test
    public void testCrudOperations() throws Exception {
        itShouldBeOpened();

        itShouldCreateDummy();

        itShouldGetDummy();
    }

    public void itShouldBeOpened() {
        assertTrue(_httpEndpoint.isOpen());
    }

    public void itShouldCreateDummy() throws Exception {
        Dummy newDummy = new Dummy("1", "Key 1", "Content 1", new ArrayList<>());

        Dummy resultDummy = invoke(
                Dummy.class,
                "/v1/dummy/create_dummy",
                Parameters.fromTuples("dummy", newDummy)
        );

        assertNotNull(resultDummy);
        assertNotNull(resultDummy.getId());
        assertEquals(newDummy.getKey(), resultDummy.getKey());
        assertEquals(newDummy.getContent(), resultDummy.getContent());
    }

    public void itShouldGetDummy() throws Exception {
        Dummy existingDummy = new Dummy("1", "Key 1", "Content 1", new ArrayList<>());

        Dummy resultDummy = invoke(
                Dummy.class,
                "/v1/dummy/get_dummy_by_id",
                Parameters.fromTuples("dummy_id", existingDummy.getId())
        );

        assertNotNull(resultDummy);
        assertNotNull(resultDummy.getId());
        assertEquals(existingDummy.getKey(), resultDummy.getKey());
        assertEquals(existingDummy.getContent(), resultDummy.getContent());
    }

    private static <T> T invoke(Class<T> type, String route, Object entity) throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JacksonFeature());
        Client httpClient = ClientBuilder.newClient(clientConfig);

        String content = JsonConverter.toJson(entity);
        try (Response response = httpClient.target("http://localhost:" + port + route)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(content, MediaType.APPLICATION_JSON))) {
            try {
                return response.readEntity(type);
            } catch (Exception ex) {
                System.err.println("EXCEEEEEEEEEEEEEEEEEEEEEEEEEEEEPT");
                System.err.println(ex.getMessage());

                StackTraceElement[] ste = ex.getStackTrace();
                StringBuilder builder = new StringBuilder();
                if (ste != null) {
                    for (StackTraceElement stackTraceElement : ste) {
                        if (builder.length() > 0)
                            builder.append(" ");
                        builder.append(stackTraceElement.toString());
                    }
                }

                System.err.println(builder.toString());
                throw ex;
            }
        }
    }

}
