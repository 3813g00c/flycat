package com.jper.flycat.client.handler;

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

/**
 * Socks命令处理器
 *
 * @author ywxiang
 * @date 2020/12/17 下午9:06
 */
@Slf4j
@ChannelHandler.Sharable
public final class SocksCommandRequestHandler extends SimpleChannelInboundHandler<SocksCmdRequest> {

    // private final Bootstrap b = new Bootstrap();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SocksCmdRequest request) {
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                new GenericFutureListener<Future<Channel>>() {
                    @Override
                    public void operationComplete(final Future<Channel> future) throws Exception {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, request.addressType()))
                                    .addListener(new ChannelFutureListener() {
                                        @Override
                                        public void operationComplete(ChannelFuture channelFuture) {
                                            System.out.println("前" + ctx.channel().id() + "：" + ctx.pipeline().names());
                                            ctx.pipeline().remove(SocksCommandRequestHandler.class);
                                            System.out.println("后" + ctx.channel().id() + "：" + ctx.pipeline().names());
                                            outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                            ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                                        }
                                    });
                        } else {
                            ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));
                            SocksServerUtils.closeOnFlush(ctx.channel());
                        }
                    }
                });
        final Channel inBoundChannel = ctx.channel();
        Bootstrap b = new Bootstrap();
        b.group(inBoundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        b.connect(request.host(), request.port()).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
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
            }
        });
    }
}
