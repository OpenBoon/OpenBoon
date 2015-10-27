package com.zorroa.archivist.event;

import com.zorroa.archivist.domain.Message;
import com.zorroa.archivist.domain.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class EventServerHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(EventServerHandler.class);

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        Channel incoming = ctx.channel();
        channels.add(incoming);
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {
        Channel incoming = ctx.channel();
        channels.remove(incoming);
    }

    public void send(Set<String> sessions, String payload) {
        logger.trace("sending message to: {} {}", sessions, payload);
        for (Channel channel: channels) {
            String _session = (String) channel.attr(AttributeKey.valueOf("session")).get();
            if (sessions.contains(_session)) {
                logger.trace("Found channel: {}", channel.remoteAddress());
                try {
                    logger.info("{}", channel.writeAndFlush(payload + "\r\n").sync());
                } catch (InterruptedException ignore) {
                    //ignore
                }
            }
        }
    }

    public void broadcast(Message message) {
        logger.trace("broadcasting message: {}", message);
        String text = message.toString() + "\r\n";
        channels.writeAndFlush(text);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg)
            throws Exception {

        MessageType type = MessageType.valueOf(msg.substring(0, msg.indexOf(' ')));
        String sessionId = msg.substring(msg.indexOf(' ') + 1);

        switch(type) {
        case SESSION:
            logger.trace("Setting session attr on channel: {}", sessionId);
            ctx.channel().attr(AttributeKey.valueOf("session")).set(sessionId);
            ctx.channel().writeAndFlush("OK\n");
        default:
            // ignore
            return;
        }
    }
}
