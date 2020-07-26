/**
 * Copyright (C) 2011-2018 ARM Limited. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.informatik.hostage.protocols;

import com.mbed.coap.packet.BlockOption;
import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.DataConvertingUtility;
import com.mbed.coap.packet.MessageType;
import com.mbed.coap.packet.Method;
import com.mbed.coap.packet.SignalingOptions;
import java.net.InetSocketAddress;

public class CoapPacketBuilder {
    public static final InetSocketAddress LOCAL_5683 = new InetSocketAddress("localhost", 5683);
    public static final InetSocketAddress LOCAL_1_5683 = new InetSocketAddress("localhost", 1_5683);
    private final CoapPacket coapPacket;

    private CoapPacketBuilder(InetSocketAddress address) {
        coapPacket = new CoapPacket(address);
    }

    public static CoapPacketBuilder newCoapPacket() {
        return new CoapPacketBuilder(null);
    }

    public static CoapPacketBuilder newCoapPacket(int mid) {
        CoapPacketBuilder coapPacketBuilder = new CoapPacketBuilder(null);
        coapPacketBuilder.mid(mid);
        return coapPacketBuilder;
    }

    public static CoapPacketBuilder newCoapPacket(InetSocketAddress address) {
        return new CoapPacketBuilder(address);
    }

    public CoapPacket build() {
        return coapPacket;
    }

    public CoapPacket emptyAck(int mid) {
        coapPacket.setMessageId(mid);
        coapPacket.setMessageType(MessageType.Acknowledgement);
        coapPacket.setCode(null);
        return build();
    }

    public CoapPacketBuilder get() {
        coapPacket.setMethod(Method.GET);
        return this;
    }

    public CoapPacketBuilder put() {
        coapPacket.setMethod(Method.PUT);
        return this;
    }

    public CoapPacketBuilder post() {
        coapPacket.setMethod(Method.POST);
        return this;
    }

    public CoapPacketBuilder delete() {
        coapPacket.setMethod(Method.DELETE);
        return this;
    }

    public CoapPacketBuilder ack(Code code) {
        coapPacket.setMessageType(MessageType.Acknowledgement);
        coapPacket.setCode(code);
        return this;
    }


    public CoapPacketBuilder uriPath(String uriPath) {
        coapPacket.headers().setUriPath(uriPath);
        return this;
    }

    public CoapPacketBuilder uriQuery(String uriQuery) {
        coapPacket.headers().setUriQuery(uriQuery);
        return this;
    }

    public CoapPacketBuilder locPath(String locPath) {
        coapPacket.headers().setLocationPath(locPath);
        return this;
    }

    public CoapPacketBuilder maxAge(long maxAge) {
        coapPacket.headers().setMaxAge(maxAge);
        return this;
    }

    public CoapPacketBuilder contFormat(short contentFormat) {
        coapPacket.headers().setContentFormat(contentFormat);
        return this;
    }

    public CoapPacketBuilder mid(int mid) {
        coapPacket.setMessageId(mid);
        return this;
    }


    public CoapPacketBuilder payload(String payload) {
        coapPacket.setPayload(payload);
        return this;
    }

    public CoapPacketBuilder payload(byte[] payload) {
        coapPacket.setPayload(payload);
        return this;
    }

    public CoapPacketBuilder obs(int observe) {
        coapPacket.headers().setObserve(observe);
        return this;
    }

    public CoapPacketBuilder token(long token) {
        coapPacket.setToken(DataConvertingUtility.convertVariableUInt(token));
        return this;
    }

    public CoapPacketBuilder block2Res(int blockNr, BlockSize blockSize, boolean more) {
        coapPacket.headers().setBlock2Res(new BlockOption(blockNr, blockSize, more));
        return this;
    }

    public CoapPacketBuilder block1Req(int blockNr, BlockSize blockSize, boolean more) {
        coapPacket.headers().setBlock1Req(new BlockOption(blockNr, blockSize, more));
        return this;
    }

    public CoapPacketBuilder size1(Integer size) {
        coapPacket.headers().setSize1(size);
        return this;
    }

    public CoapPacketBuilder size2Res(Integer size) {
        coapPacket.headers().setSize2Res(size);
        return this;
    }

    public CoapPacketBuilder etag(int etag) {
        coapPacket.headers().setEtag(DataConvertingUtility.convertVariableUInt(etag));
        return this;
    }

    public CoapPacketBuilder proxy(String proxyUri) {
        coapPacket.headers().setProxyUri(proxyUri);
        return this;
    }

    public CoapPacketBuilder code(Code code) {
        coapPacket.setCode(code);
        return this;
    }

    public CoapPacketBuilder con(Code code) {
        coapPacket.setMessageType(MessageType.Confirmable);
        coapPacket.setCode(code);
        return this;
    }

    public CoapPacketBuilder con() {
        coapPacket.setMessageType(MessageType.Confirmable);
        return this;
    }

    public CoapPacketBuilder non() {
        coapPacket.setMessageType(MessageType.NonConfirmable);
        return this;
    }

    public CoapPacketBuilder non(Code code) {
        coapPacket.setMessageType(MessageType.NonConfirmable);
        coapPacket.setCode(code);
        return this;
    }

    public CoapPacketBuilder reset() {
        coapPacket.setMessageType(MessageType.Reset);
        return this;
    }

    public CoapPacket reset(int messageId) {
        coapPacket.setToken(null);
        return mid(messageId).reset().build();
    }

    public CoapPacketBuilder signalling(SignalingOptions signOpt) {
        coapPacket.headers().putSignallingOptions(signOpt);
        return this;
    }
}
