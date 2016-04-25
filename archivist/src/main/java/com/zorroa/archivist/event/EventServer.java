package com.zorroa.archivist.event;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EventServer {

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ServerBootstrap bootstrap =  new ServerBootstrap();
    private final Executor thread = Executors.newSingleThreadExecutor();

    @Autowired
    EventServerInitializer eventServerInitializer;

    @Value("${archivist.events.port}")
    private int port;

    public EventServer() {
        bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO));
    }

    @PostConstruct
    public void init() {
        bootstrap.childHandler(eventServerInitializer);
        start();
    }

    public void start() {
        thread.execute(() -> {
            try {
                bootstrap.bind(port).sync().channel().closeFuture().sync();
            } catch (InterruptedException ignore) {
                //
            }
            finally {
                shutdown();
            }
        });
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
