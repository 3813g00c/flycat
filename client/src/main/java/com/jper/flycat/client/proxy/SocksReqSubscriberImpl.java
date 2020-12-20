package com.jper.flycat.client.proxy;

import com.jper.flycat.client.handler.TCPDirectConnectHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.UnknownHostException;

/**
 * @author ywxiang
 * @date 2020/12/20 下午1:46
 */
@Slf4j
public class SocksReqSubscriberImpl implements SocksReqSubscriber {
    @Override
    public void receive(SocksProxyRequest request) {
        String host = request.getHost();
        int port = request.getPort();

        log.info("start connect to server {}:{}...", host, port);
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addFirst(new TCPDirectConnectHandler(request));
                    }
                });
        b.connect(host, port).addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                if (f.cause() instanceof ConnectTimeoutException) {
                    if (log.isInfoEnabled()) {
                        log.info("connect to " + request.getHost() + ":" + request.getPort() + " failure, cause connect timeout");
                    }
                } else if (f.cause() instanceof UnknownHostException) {
                    if (log.isWarnEnabled()) {
                        log.warn("Connect failure: Unknown domain {}", request.getHost());
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("connect establish failure, from " + request.getHost() + ":" + request.getPort(), f.cause());
                    }
                }
                request.getClientChannel().close();
                f.channel().close();
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("connect establish success, from {}:{}", request.getHost(), request.getPort());
                }
                request.setServerChannel(f.channel());
            }
        });
    }
}
