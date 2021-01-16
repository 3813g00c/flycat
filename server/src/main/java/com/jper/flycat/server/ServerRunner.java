package com.jper.flycat.server;

import com.jper.flycat.core.factory.ContextSslFactory;
import com.jper.flycat.core.util.Sha224Util;
import com.jper.flycat.server.codec.ProxyMessageRequestDecoder;
import com.jper.flycat.server.codec.ProxyMessageResponseEncoder;
import com.jper.flycat.server.config.FlyCatConfig;
import com.jper.flycat.server.handler.DistributeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private FlyCatConfig config;
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * 启动Netty客户端时，为了安全，会先连接本地的SpringBoot端口，如果连接失败，将会拒绝启动代理服务器
     *
     * @param args
     */
    @Override
    public void run(ApplicationArguments args) {

        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");

        String host = config.getLocalAddr();
        int port = config.getLocalPort();

        String remoteAddr = config.getRemoteAddr();
        int remotePort = config.getRemotePort();

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new SimpleChannelInboundHandler<ByteBuf>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

                    }
                });
        b.connect(remoteAddr, remotePort).addListener(future -> {
            if (future.isSuccess()) {
                log.info("Connect with local springboot server successfully, start to start proxy server.");
                ServerBootstrap serverBoot = new ServerBootstrap();
                serverBoot.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000 * 5 * 60)
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) {
                                ChannelPipeline p = socketChannel.pipeline();
                                p.addLast(new ProxyMessageRequestDecoder(64 * 1024, getShaPwd()));
                                p.addLast(new ProxyMessageResponseEncoder());
                                p.addLast(new DistributeHandler());
                                SSLEngine engine = ContextSslFactory.getSslContext1().createSSLEngine();
                                engine.setUseClientMode(false);
                                engine.setNeedClientAuth(false);
                                p.addFirst("ssl", new SslHandler(engine));
                            }
                        })
                        .childOption(ChannelOption.AUTO_READ, false);
                ChannelFuture f = serverBoot.bind(new InetSocketAddress(host, port)).sync();
                this.channel = f.channel();
                f.addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess()) {
                        log.info("Server bound in {}", port);
                    } else {
                        log.info("Bound attempt failed");
                        channelFuture.cause().printStackTrace();
                    }
                });
            } else {
                log.warn("Failed to connect to the local springboot server, unable to start the proxy server.");
            }
        });

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

    /**
     * 获取哈希后的密码
     *
     * @return pwd
     */
    private String getShaPwd() {
        return Sha224Util.getSha224Str(config.getPassword());
    }
}
