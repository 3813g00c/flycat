package com.jper.flycat.client.handler;

import com.jper.flycat.core.codec.ProxyMessageEncoder;
import com.jper.flycat.core.factory.ContextSslFactory;
import com.jper.flycat.core.protocol.ProxyMessage;
import com.jper.flycat.core.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
public final class SocksCommandRequestHandler extends SimpleChannelInboundHandler<SocksCmdRequest> {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SocksCmdRequest request) {
        ByteBuf buf = null;
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
                                    ProxyMessage message = new ProxyMessage();
                                    message.setPassword("SDAD");
                                    message.setHost(request.host());
                                    message.setPort(request.port());
                                    // System.out.println();
                                    ctx.pipeline().addLast(new ProxyMessageEncoder());
                                    //ctx.pipeline().addLast(new ProxyMessageDecoder(1024));
                                    // ctx.pipeline().fireChannelRead(message);
                                    System.out.println("dada");
                                    outboundChannel.pipeline().addLast(new ProxyMessageEncoder());
                                    // outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                    outboundChannel.pipeline().addLast(new TestHandler(ctx.channel(), message));
                                    ctx.pipeline().addLast(new TestHandler(outboundChannel, message));
                                });
                    } else {
                        ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                });
        final Channel inBoundChannel = ctx.channel();
//        Bootstrap b = new Bootstrap();
//        b.group(inBoundChannel.eventLoop())
//                .channel(NioSocketChannel.class)
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
//                .option(ChannelOption.SO_KEEPALIVE, true)
//                .handler(new DirectClientHandler(promise));
//
//        b.connect(request.host(), request.port()).addListener((ChannelFutureListener) future -> {
//            if (future.isSuccess()) {
//                // Connection established use handler provided results
//                log.info("connect establish success, from {}:{}", request.host(), request.port());
//            } else {
//                log.info("connect establish failed, from {}:{}", request.host(), request.port());
//                // Close the connection if the connection attempt has failed.
//                ctx.channel().writeAndFlush(
//                        new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));
//                SocksServerUtils.closeOnFlush(ctx.channel());
//            }
//        });
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
                        // p.addLast(new ProxySuccessHandler(promise));
                        ProxyMessage message = new ProxyMessage();
                        message.setPassword("SDAD");
                        message.setHost(request.host());
                        message.setPort(request.port());
                        p.addLast(new ProxyMessageEncoder());
                        p.addLast(new TestHandler1(message));
                        p.addFirst("ssl", new SslHandler(engine));
                    }
                });

        b.connect("127.0.0.1", 9988).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // Connection established use handler provided results
                log.info("connect establish success, from {}:{}", request.host(), request.port());
            } else {
                log.info("connect establish failed, from {}:{}", request.host(), request.port());
                // Close the connection if the connection attempt has failed.
                ctx.channel().writeAndFlush(
                        new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
        });
    }
}
