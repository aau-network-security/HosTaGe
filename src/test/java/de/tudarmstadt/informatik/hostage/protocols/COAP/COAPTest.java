package de.tudarmstadt.informatik.hostage.protocols.COAP;

import com.mbed.coap.exception.ObservationNotEstablishedException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.internal.CoapMessaging;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Callback;
import com.mbed.coap.utils.RequestCallback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import static de.tudarmstadt.informatik.hostage.protocols.COAP.CoapPacketBuilder.LOCAL_5683;
import static de.tudarmstadt.informatik.hostage.protocols.COAP.CoapPacketBuilder.newCoapPacket;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class COAPTest {
    private CoapMessaging msg = mock(CoapMessaging.class);
    private CoapServer server;

    @Before
    public void setUp() throws Exception {
        server = new CoapServer(msg).start();

    }

    @Test
    public void shouldStartAndStop() throws Exception {
        verify(msg).start(any());
        assertTrue(server.isRunning());
        System.out.println();

        server.stop();
        verify(msg).stop();
        assertFalse(server.isRunning());
    }

    @Test
    public void shouldPassMakeRequest_toMessaging() throws ExecutionException, InterruptedException {
        final CoapPacket req = newCoapPacket().get().uriPath("/test").build();
        final ArgumentCaptor<Callback> callback = ArgumentCaptor.forClass(Callback.class);

        //when
        final CompletableFuture<CoapPacket> resp = server.makeRequest(req);

        //then
        verify(msg).makeRequest(eq(req), callback.capture(), eq(TransportContext.NULL));
        assertFalse(resp.isDone());

        //verify callback
        callback.getValue().call(newCoapPacket().ack(Code.C400_BAD_REQUEST).build());
        assertTrue(resp.isDone());
        assertEquals(Code.C400_BAD_REQUEST, resp.get().getCode());
    }


    @Test
    public void shouldSendObservationRequest() {
        Callback<CoapPacket> callback = mock(Callback.class);
        server.observe("/test", LOCAL_5683, callback, "aa".getBytes(), TransportContext.NULL);

        verify(msg).makeRequest(argThat(cp -> cp.headers().getUriPath().equals("/test") && cp.headers().getObserve() != null), any(), eq(TransportContext.NULL));
    }

    @Test
    public void shouldSendObservationRequest_andAddObservationHeader() {
        Callback<CoapPacket> callback = mock(Callback.class);
        server.observe(newCoapPacket(LOCAL_5683).get().uriPath("/test").build(), callback, TransportContext.NULL);

        verify(msg).makeRequest(argThat(cp -> cp.headers().getUriPath().equals("/test") && cp.headers().getObserve() != null), any(), eq(TransportContext.NULL));
    }

    @Test
    public void shouldRespondToObservationRequest() {
        CoapPacket resp = newCoapPacket().ack(Code.C205_CONTENT).obs(0).build();
        RequestCallback respCallback = mock(RequestCallback.class);
        server.observe("/test", LOCAL_5683, respCallback, "aa".getBytes(), TransportContext.NULL);

        verifyMakeRequest_andThen().onSent();
        verifyMakeRequest_andThen().call(resp);

        verify(respCallback).onSent();
        verify(respCallback).call(eq(resp));
    }

    @Test
    public void shouldRespondToObservationRequest_notObserved() {
        Callback<CoapPacket> respCallback = mock(Callback.class);
        server.observe("/test", LOCAL_5683, respCallback, "aa".getBytes(), TransportContext.NULL);

        verifyMakeRequest_andThen().onSent();
        verifyMakeRequest_andThen().call(newCoapPacket().ack(Code.C205_CONTENT).build());

        verify(respCallback).callException(isA(ObservationNotEstablishedException.class));
    }

    @Test
    public void shouldRespondToObservationRequest_errorResponse() {
        Callback<CoapPacket> respCallback = mock(Callback.class);
        server.observe("/test", LOCAL_5683, respCallback, "aa".getBytes(), TransportContext.NULL);

        verifyMakeRequest_andThen().call(newCoapPacket().ack(Code.C404_NOT_FOUND).build());

        verify(respCallback).callException(isA(ObservationNotEstablishedException.class));
    }

    @Test
    public void shouldRespondToObservationRequest_exception() {
        Callback<CoapPacket> respCallback = mock(Callback.class);
        server.observe("/test", LOCAL_5683, respCallback, "aa".getBytes(), TransportContext.NULL);

        verifyMakeRequest_andThen().callException(new IOException());

        verify(respCallback).callException(isA(IOException.class));
    }

    @Test()
    public void shouldSendNotification() {
        CoapPacket notif = newCoapPacket(LOCAL_5683).obs(12).token(11).ack(Code.C205_CONTENT).build();
        server.sendNotification(notif, mock(Callback.class), TransportContext.NULL);

        verify(msg).makeRequest(eq(notif), any(), any());
    }


    private RequestCallback verifyMakeRequest_andThen() {
        ArgumentCaptor<RequestCallback> callback = ArgumentCaptor.forClass(RequestCallback.class);
        verify(msg).makeRequest(any(), callback.capture(), any());
        return callback.getValue();
    }
}
