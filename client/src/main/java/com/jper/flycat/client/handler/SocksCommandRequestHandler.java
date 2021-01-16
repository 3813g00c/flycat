package com.jper.flycat.client.handler;

import com.jper.flycat.client.config.FlyCatConfig;
import com.jper.flycat.client.codec.ProxyMessageRequestEncoder;
import com.jper.flycat.client.codec.ProxyMessageResponseDecoder;
import com.jper.flycat.core.factory.ContextSslFactory;
import com.jper.flycat.core.handler.RelayHandler;
import com.jper.flycat.core.protocol.ProxyMessageRequest;
import com.jper.flycat.core.util.Sha224Util;
import com.jper.flycat.core.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLEngine;
import java.util.NoSuchElementException;

/**
 * Socks命令处理器
 *
 * @author ywxiang
 * @date 2020/12/17 下午9:06
 */
@Slf4j
@ChannelHandler.Sharable
@Component
public final class SocksCommandRequestHandler extends SimpleChannelInboundHandler<SocksCmdRequest> {

    @Autowired
    private FlyCatConfig config;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SocksCmdRequest request) {
        String remoteAddr = config.getRemoteAddr();
        int remotePort = config.getRemotePort();
        String password = config.getPassword();

        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                (GenericFutureListener<Future<Channel>>) future -> {
                    final Channel outboundChannel = future.getNow();
                    if (future.isSuccess()) {
                        ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, request.addressType()))
                                .addListener((ChannelFutureListener) channelFuture -> {
                                    try {
                                        ctx.pipeline().remove("socksCommandRequestHandler");
                                    } catch (NoSuchElementException e) {
                                        log.warn("socksCommandRequestHandler remove failed");
                                    }
                                    ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                                    outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                });
                    } else {
                        ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                });
        final Channel inBoundChannel = ctx.channel();
        ProxyMessageRequest message = new ProxyMessageRequest();
        String shaPwd = Sha224Util.getSha224Str(password);
        System.out.println(shaPwd);
        message.setPassword(shaPwd);
        message.setHost(request.host());
        message.setPort(request.port());
        Bootstrap b = new Bootstrap();
        b.group(inBoundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        SSLEngine engine = ContextSslFactory.getSslContext2().createSSLEngine();
                        engine.setUseClientMode(true);
                        p.addLast(new ProxyMessageRequestEncoder());
                        p.addLast(new ProxyMessageResponseDecoder(64 * 1024));
                        p.addLast(new ProxyHandler(message, inBoundChannel, promise));
                        p.addFirst("ssl", new SslHandler(engine));
                    }
                });

        b.connect(remoteAddr, remotePort).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // Connection established use handler provided results
                log.info("Successfully connected to the proxy server");
            } else {
                log.info("Failed to connect to proxy server");
                // Close the connection if the connection attempt has failed.
                ctx.channel().writeAndFlush(
                        new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
        });
    }
}
