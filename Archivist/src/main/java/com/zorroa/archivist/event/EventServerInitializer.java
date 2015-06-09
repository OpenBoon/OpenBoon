package com.zorroa.archivist.event;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import org.springframework.beans.factory.annotation.Autowired;

public class EventServerInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    EventServerHandler eventServerHandler;

    public static ByteBuf[] delimiter() {
        return new ByteBuf[] {
                Unpooled.wrappedBuffer(new byte[] { '\r', '\n' }),
                Unpooled.wrappedBuffer(new byte[] { '\n' }),
        };
    }

    public EventServerInitializer() { }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
         ChannelPipeline pipeline = ch.pipeline();

         pipeline.addLast(new DelimiterBasedFrameDecoder(8192, delimiter()));
         pipeline.addLast(new StringDecoder());
         pipeline.addLast(new StringEncoder());
         pipeline.addLast(eventServerHandler);
    }
}
