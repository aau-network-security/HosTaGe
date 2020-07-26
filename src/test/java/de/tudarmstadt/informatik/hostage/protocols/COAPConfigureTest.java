package de.tudarmstadt.informatik.hostage.protocols;

import com.mbed.coap.CoapConstants;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.client.CoapClientBuilder;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.MessageType;
import com.mbed.coap.packet.Method;
import com.mbed.coap.server.CoapExchange;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.transmission.CoapTimeout;
import com.mbed.coap.transmission.SingleTimeout;
import com.mbed.coap.transport.InMemoryCoapTransport;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Callback;
import com.mbed.coap.utils.CoapResource;
import com.mbed.coap.utils.FutureCallbackAdapter;
import com.mbed.coap.utils.ReadOnlyCoapResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class COAPConfigureTest {
    private static final ArrayList<CoapPacket> requests = new ArrayList<>();

    private CoapServer server = null;
    private int SERVER_PORT;
    private InetSocketAddress serverAddress;

    @Before
    public void setUp() throws IOException {
        InetAddress address = InetAddress.getByName("192.168.1.3");

        server = CoapServer.builder().transport(address,5683).build();
        server.addRequestHandler("/test/1", new ReadOnlyCoapResource("Dziala"));
        server.addRequestHandler("/resource*", new ReadOnlyCoapResource("Prefix dziala"));
        server.addRequestHandler("/", new ReadOnlyCoapResource("Shortest path"));
        server.useCriticalOptionTest(false);
        server.start();
        SERVER_PORT = server.getLocalSocketAddress().getPort();
        serverAddress = new InetSocketAddress(address, SERVER_PORT);
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void simpleRequest() throws Exception {
        CoapServer cnn = CoapServer.builder().transport(0).build();
        cnn.start();

        CoapPacket request = new CoapPacket(serverAddress);
        request.setMethod(Method.GET);
        request.headers().setUriPath("/test/1");
        request.setMessageId(1647);

        FutureCallbackAdapter<CoapPacket> callback = new FutureCallbackAdapter<>();
        cnn.makeRequest(request, callback);
        assertEquals("Dziala", callback.get().getPayloadString());
        cnn.stop();
    }

    @Test
    public void simpleRequestWithCustomHeader() throws Exception {
        CoapServer cnn = CoapServer.builder().transport(0).build();
        cnn.start();

        CoapPacket request = new CoapPacket(serverAddress);
        request.setMethod(Method.GET);
        request.headers().setUriPath("/test/1");
        request.setMessageId(1647);
        request.headers().put(74, new byte[]{1, 2, 3});

        FutureCallbackAdapter<CoapPacket> callback = new FutureCallbackAdapter<>();
        cnn.makeRequest(request, callback);
        assertEquals("Dziala", callback.get().getPayloadString());
        cnn.stop();
    }

    @Test
    public void simpleRequestWithCriticalCustomHeader() throws Exception {
        server.useCriticalOptionTest(true);
        CoapServer cnn = CoapServer.builder().transport(0).build();
        cnn.start();

        CoapPacket request = new CoapPacket(serverAddress);
        request.setMethod(Method.GET);
        request.headers().setUriPath("/test/1");
        request.setMessageId(1647);
        request.headers().put(71, new byte[]{1, 2, 3});

        FutureCallbackAdapter<CoapPacket> callback = new FutureCallbackAdapter<>();
        cnn.makeRequest(request, callback);
        assertEquals(Code.C402_BAD_OPTION, callback.get().getCode());
        cnn.stop();
    }

    @Test
    public void simpleRequestWithCriticalCustomHeader2() throws Exception {
        server.useCriticalOptionTest(false);
        CoapServer cnn = CoapServer.builder().transport(0).build();
        cnn.start();

        CoapPacket request = new CoapPacket(serverAddress);
        request.setMethod(Method.GET);
        request.headers().setUriPath("/test/1");
        request.setMessageId(1647);
        request.headers().put((byte) 71, new byte[]{1, 2, 3});

        FutureCallbackAdapter<CoapPacket> callback = new FutureCallbackAdapter<>();
        cnn.makeRequest(request, callback);
        assertEquals("Dziala", callback.get().getPayloadString());
        cnn.stop();
    }

    public void simpleRequestToShortestPath() throws Exception {
        CoapServer cnn = CoapServer.builder().transport(0).build();
        cnn.start();

        CoapPacket request = new CoapPacket(serverAddress);
        request.setMethod(Method.GET);
        request.headers().setUriPath("/");
        request.setMessageId(1648);

        assertEquals("Shortest path", cnn.makeRequest(request).join().getPayloadString());
        cnn.stop();
    }

    @Test
    public void simpleRequest2() throws Exception {
        try (CoapClient client = CoapClientBuilder.newBuilder().target(SERVER_PORT).build()) {

            Future<CoapPacket> coapResp = client.resource("/test/1").get();
            assertEquals("Dziala", coapResp.get().getPayloadString());
        }
    }

    @Test
    public void simpleRequest3() throws Exception {
        try (CoapClient client = CoapClientBuilder.newBuilder().target(SERVER_PORT).build()) {

            Future<CoapPacket> coapResp = client.resource("/resource/123").get();
            assertEquals("Prefix dziala", coapResp.get().getPayloadString());

            coapResp = client.resource("/test/1/tre").get();
            assertEquals(Code.C404_NOT_FOUND, coapResp.get().getCode());
        }
    }

    @Test
    public void requestWithPacketLost() throws CoapException, IOException {
        CoapServer serverNode = CoapServerBuilder.newBuilder().transport(InMemoryCoapTransport.create(5683)).build();
        final ReadOnlyCoapResource res = new ReadOnlyCoapResource("Not dropped");
        serverNode.addRequestHandler("/dropping", res);
        serverNode.start();

        try (CoapClient cnn = CoapClientBuilder.newBuilder(InMemoryCoapTransport.createAddress(5683))
                .transport(new DroppingPacketsTransportWrapper(0, (byte) 0) {
                    private boolean hasDropped = false;

                    @Override
                    public void sendPacket0(CoapPacket coapPacket, InetSocketAddress adr, TransportContext tranContext) {
                        //will drop only first packet
                        if (!hasDropped) {
                            hasDropped = true;
                            res.setResourceBody("dropped");
                            System.out.println("dropped");
                        } else {
                            super.sendPacket0(coapPacket, adr, tranContext);
                        }
                    }

                })
                .timeout(new CoapTimeout(100)).build()) {

            CoapPacket resp = cnn.resource("/dropping").sync().get();
            assertEquals("dropped", resp.getPayloadString());
        }
    }

    @Test
    public void simpleRequest4() throws Exception {
        CoapServer cnn = CoapServerBuilder.newBuilder().transport(0).build();
        cnn.start();

        CoapClient client = CoapClientBuilder.clientFor(serverAddress, cnn);
        assertEquals("Dziala", client.resource("/test/1").maxAge(2635593050L).get().join().getPayloadString());
        cnn.stop();
    }

    @Test
    public void simpleRequest5() throws IOException, CoapException {
        CoapServer srv = CoapServerBuilder.newBuilder().transport(InMemoryCoapTransport.create(61601)).build();
        srv.addRequestHandler("/temp", new ReadOnlyCoapResource("23 C"));
        srv.start();

        try (CoapClient cnn = CoapClientBuilder.newBuilder(InMemoryCoapTransport.createAddress(61601)).transport(InMemoryCoapTransport.create(0)).build()) {
            assertEquals("23 C", cnn.resource("/temp").sync().get().getPayloadString());
        }
        srv.stop();
    }

    @Test
    public void simpleRequestWithUnknownCriticalOptionHeader() throws IOException, CoapException {
        CoapServer srv = CoapServerBuilder.newBuilder().transport(InMemoryCoapTransport.create(61601)).build();
        srv.addRequestHandler("/temp", new ReadOnlyCoapResource("23 C"));
        srv.start();

        try (CoapClient client = CoapClientBuilder.newBuilder(InMemoryCoapTransport.createAddress(61601)).transport(new InMemoryCoapTransport()).build()) {
            assertEquals(Code.C402_BAD_OPTION, client.resource("/temp").header(123, "dupa".getBytes()).sync().get().getCode());
        }
        srv.stop();
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void stopNonRunningServer() {
        CoapServer srv = CoapServerBuilder.newBuilder().transport(0).build();
        srv.stop();
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void startRunningServer() throws IOException {
        CoapServer srv = CoapServerBuilder.newBuilder().transport(0).build();
        srv.start();
        srv.start();
    }

    @Test
    public void testRequestWithCustomHandler() throws Exception {
        CoapServer cnn = CoapServerBuilder.newBuilder()
                .transport(new InMemoryCoapTransport(5683)).build();
        cnn.addRequestHandler("/*", new CoapResource() {
            @Override
            public void get(CoapExchange exchange) {
                requests.add(exchange.getRequest());
                exchange.setResponseCode(Code.C405_METHOD_NOT_ALLOWED);
                exchange.setResponseBody("Dziala");
                exchange.sendResponse();
            }

            @Override
            public void put(CoapExchange exchange) {
                exchange.setResponseCode(Code.C204_CHANGED);
                exchange.setResponseBody(Integer.valueOf(exchange.getRequestBody().length).toString());
                exchange.sendResponse();
            }
        });

        cnn.start();

        CoapPacket request = new CoapPacket(InMemoryCoapTransport.createAddress(5683));
        request.setMethod(Method.GET);
        request.headers().setUriPath("/test/1");
        request.setPayload("testing");
        request.setMessageId(1647);

        FutureCallbackAdapter<CoapPacket> callback = new FutureCallbackAdapter<>();
        cnn.makeRequest(request, callback);
        assertEquals("Dziala", callback.get().getPayloadString());
        assertEquals(1, requests.size());
        assertEquals("testing",requests.get(0).getPayloadString());
        cnn.stop();
    }

    @Test
    public void testRequestWithPacketDelay() throws Exception {
        CoapServer serverNode = CoapServerBuilder.newBuilder().transport(InMemoryCoapTransport.create(5683)).build();
        serverNode.addRequestHandler("/test/1", new ReadOnlyCoapResource("Dziala"));
        serverNode.start();

        ExecutorService executorService = Executors.newCachedThreadPool();
        CoapServer cnn = CoapServerBuilder.newBuilder()
                .transport(new InMemoryCoapTransport(0, command -> executorService.execute(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException();
                    }
                    command.run();
                }))).build();

        cnn.start();

        CoapPacket request = new CoapPacket(InMemoryCoapTransport.createAddress(5683));
        request.setMethod(Method.GET);
        request.headers().setUriPath("/test/1");
        request.setMessageId(1647);

        FutureCallbackAdapter<CoapPacket> callback = new FutureCallbackAdapter<>();
        cnn.makeRequest(request, callback);
        assertEquals("Dziala", callback.get().getPayloadString());
        cnn.stop();
    }

    @Test(expected = com.mbed.coap.exception.CoapTimeoutException.class)
    public void testRequestWithPacketDropping() throws IOException, CoapException {
        CoapServer srv = CoapServerBuilder.newBuilder()
                .transport(new DroppingPacketsTransportWrapper(CoapConstants.DEFAULT_PORT, (byte) 100))
                .build();
        srv.addRequestHandler("/test", new ReadOnlyCoapResource("TEST"));
        srv.start();

        CoapClient cnn = CoapClientBuilder.newBuilder(InMemoryCoapTransport.createAddress(CoapConstants.DEFAULT_PORT))
                .transport(InMemoryCoapTransport.create()).timeout(new SingleTimeout(100)).build();

        assertNotNull(cnn.resource("/test").sync().get());
    }

    @Test(expected = NullPointerException.class)
    public void testMakeRequestWithNullCallback() throws CoapException {
        server.makeRequest(new CoapPacket(null), (Callback<CoapPacket>) null);
    }

    @Test(expected = NullPointerException.class)
    public void testMakeRequestWithNullAddress() throws CoapException {
        server.makeRequest(new CoapPacket(Method.GET, MessageType.Confirmable, "", null), new FutureCallbackAdapter<CoapPacket>());
    }

    @Test(expected = NullPointerException.class)
    public void testMakeRequestNullRequest() throws CoapException {
        server.makeRequest(new CoapPacket(Method.GET, MessageType.Confirmable, "", null), new FutureCallbackAdapter<CoapPacket>());
    }


    private static class DroppingPacketsTransportWrapper extends InMemoryCoapTransport {
        private final byte probability; //0-100
        private final Random r = new Random();

        private boolean drop() {
            return probability > 0 && r.nextInt(100) < probability;
        }

        private DroppingPacketsTransportWrapper(int port, int probability) {
            super(port);
            if (probability < 0 || probability > 100) {
                throw new IllegalArgumentException("Value must be in range 0-100");
            }
            this.probability = ((byte) probability);
        }


        @Override
        public void receive(InMemoryCoapTransport.DatagramMessage msg) {
            if (!drop()) {
                super.receive(msg);
            }
        }
    }

}
