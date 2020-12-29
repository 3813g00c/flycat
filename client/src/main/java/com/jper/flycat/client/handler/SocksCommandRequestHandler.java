package com.jper.flycat.client.handler;

import com.jper.flycat.core.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

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
                                    outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                    ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                                });
                    } else {
                        ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                });
        final Channel inBoundChannel = ctx.channel();
        Bootstrap b = new Bootstrap();
        b.group(inBoundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        b.connect(request.host(), request.port()).addListener((ChannelFutureListener) future -> {
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
