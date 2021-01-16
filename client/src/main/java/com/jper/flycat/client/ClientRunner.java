package com.jper.flycat.client;

import com.jper.flycat.client.config.FlyCatConfig;
import com.jper.flycat.client.handler.SocksRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;
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

import java.net.InetSocketAddress;

/**
 * 初始化Netty客户端服务
 *
 * @author ywxiang
 * @date 2020/12/16 下午8:26
 */
@Component
@Slf4j
public class ClientRunner implements ApplicationRunner, ApplicationListener<ContextClosedEvent>, ApplicationContextAware {

    @Autowired
    private FlyCatConfig config;

    @Autowired
    private SocksRequestHandler socksRequestHandler;

    private ApplicationContext applicationContext;

    private Channel channel;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Override
    public void run(ApplicationArguments args) {
        log.info("启动客户端服务");
        String localAddr = config.getLocalAddr();
        int localPort = config.getLocalPort();

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBoot = new ServerBootstrap();
            serverBoot.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new SocksInitRequestDecoder());
                            p.addLast(new SocksMessageEncoder());
                            p.addLast(socksRequestHandler);
                        }
                    });
            ChannelFuture future = serverBoot.bind(new InetSocketAddress(localAddr, localPort)).sync();
            this.channel = future.channel();
            future.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    log.info("Server bound");
                } else {
                    log.info("Bound attempt failed");
                    channelFuture.cause().printStackTrace();
                }
            });
        } catch (InterruptedException i) {
            log.error("ClientServer 启动出现异常，端口：{}，cause：{}", localPort, i.getMessage());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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
        log.info("客户端服务停止");
    }

}
