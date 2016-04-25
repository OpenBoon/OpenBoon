package com.zorroa.archivist;

import com.google.common.collect.Queues;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

/**
 * Created by chambers on 12/14/15.
 */
public class TestMessagingClient {

    private static final Logger logger = LoggerFactory.getLogger(TestMessagingClient.class);

    private String host;
    private int port = Integer.parseInt(System.getProperty("port", "8087"));
    private SslContext sslContext;

    Channel channel;
    EventLoopGroup group;

    private static final Queue<String> queue = Queues.newLinkedBlockingQueue();

    public TestMessagingClient(int port, SslContext sslContext) throws Exception {
        this.port = port;
        this.host = "127.0.0.1";
        this.sslContext = sslContext;
        connect();
    }

    public void sendSession(String id) {
        channel.writeAndFlush("SESSION " + id + "\r\n");
    }

    public void shutdown() {
        group.shutdownGracefully();
    }

    public String pop() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return queue.poll();
    }

    public void connect () throws Exception {

        group = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new TestMesssagingClientInitializer(sslContext));

        // Start the connection attempt.
        channel = b.connect(host, port).sync().channel();
    }

    public static class TestMesssagingClientHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            logger.info(msg);
            queue.add(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static class TestMesssagingClientInitializer extends ChannelInitializer<SocketChannel> {

        private final SslContext sslContext;
        public TestMesssagingClientInitializer(SslContext sslContext) {
            this.sslContext = sslContext;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            if (sslContext != null) {
                pipeline.addLast(sslContext.newHandler(ch.alloc()));
            }

            // On top of the SSL handler, add the text line codec.
            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            pipeline.addLast(new StringDecoder());
            pipeline.addLast(new StringEncoder());

            // and then business logic.
            pipeline.addLast(new TestMesssagingClientHandler());
        }
    }
}
