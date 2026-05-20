package com.meituan.demo.gateway.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.demo.gateway.model.GatewayModels.GatewayFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;
import java.util.List;

public class GatewayFrameCodec extends MessageToMessageCodec<String, GatewayFrame> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, GatewayFrame msg, List<Object> out) throws Exception {
        out.add(objectMapper.writeValueAsString(msg) + "\n");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
        out.add(objectMapper.readValue(msg, GatewayFrame.class));
    }
}

