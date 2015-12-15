package com.zorroa.archivist.event;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zorroa.archivist.sdk.domain.Message;
import com.zorroa.archivist.sdk.domain.MessageType;
import com.zorroa.archivist.sdk.domain.Session;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class EventServerHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(EventServerHandler.class);

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static final String END_MESSAGE = "\r\n";

    /**
     * Stores a session ID to channel mapping.
     */
    private final Cache<String, Channel> channelMap = CacheBuilder.newBuilder()
            .weakValues()
            .removalListener((c) -> {
                logger.info("Channel {} was removed", c);
            })
            .build();

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

    public void send(Collection<Session> sessions, Message message) {
        String text = message.serialize(END_MESSAGE);

        for (Session session: sessions) {
            Channel channel = channelMap.asMap().get(session.getCookieId());
            if (channel == null) {
                continue;
            }
            try {
                channel.writeAndFlush(text).sync();
            } catch (InterruptedException e) {
                logger.warn("Failed to send '{}' to channel '{}',", text, channel, e);
            }
        }
    }

    public void broadcast(Message message) {
        String text = message.serialize(END_MESSAGE);
        channels.writeAndFlush(text);
    }

    public void send(Session session, Message message) {
        Channel channel = channelMap.asMap().get(session.getCookieId());
        if (channel == null) {
            return;
        }

        String text = message.serialize(END_MESSAGE);
        try {
            channel.writeAndFlush(text).sync();
        } catch (InterruptedException e) {
            logger.warn("Failed to send '{}' to channel '{}',", text, channel, e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg)
            throws Exception {

        MessageType type = MessageType.valueOf(msg.substring(0, msg.indexOf(' ')));
        String cookieId = msg.substring(msg.indexOf(' ') + 1);

        switch(type) {
        case SESSION:
            logger.trace("Setting cookie attr on channel: {}", cookieId);
            channelMap.put(cookieId, ctx.channel());
            ctx.channel().attr(AttributeKey.valueOf("cookie")).set(cookieId);
            ctx.channel().writeAndFlush("OK" + END_MESSAGE);
            break;
        default:
            // ignore
            return;
        }
    }
}
