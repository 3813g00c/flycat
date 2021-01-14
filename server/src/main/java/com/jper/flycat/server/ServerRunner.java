package com.jper.flycat.server;

import com.jper.flycat.core.codec.ProxyMessageRequestDecoder;
import com.jper.flycat.core.codec.ProxyMessageResponseEncoder;
import com.jper.flycat.core.factory.ContextSslFactory;
import com.jper.flycat.server.handler.MySslHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;

/**
 * @author ywxiang
 * @date 2020/12/30 下午8:30
 */
@Slf4j
@Component
public class ServerRunner implements ApplicationRunner, ApplicationListener<ContextClosedEvent>, ApplicationContextAware {
    @Value("${netty.port}")
    private int port;

    @Value("${netty.host}")
    private String host;

    private Channel channel;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Override
    public void run(ApplicationArguments args) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBoot = new ServerBootstrap();
            serverBoot.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000 * 5 * 60)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new ProxyMessageRequestDecoder(64 * 1024));
                            p.addLast(new ProxyMessageResponseEncoder());
                            p.addLast(new MySslHandler());
                            SSLEngine engine = ContextSslFactory.getSslContext1().createSSLEngine();
                            engine.setUseClientMode(false);
                            engine.setNeedClientAuth(true);
                            p.addFirst("ssl", new SslHandler(engine));
                        }
                    });
            ChannelFuture future = serverBoot.bind(new InetSocketAddress(this.host, this.port)).sync();
            this.channel = future.channel();
            future.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    log.info("Server bound in {}", this.port);
                } else {
                    log.info("Bound attempt failed");
                    channelFuture.cause().printStackTrace();
                }
            });
        } catch (InterruptedException i) {
            log.error("Server 启动出现异常，端口：{}，cause：{}", this.port, i.getMessage());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        if (this.channel != null) {
            try {
                this.channel.close();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
        log.info("服务端服务停止");
    }
}
