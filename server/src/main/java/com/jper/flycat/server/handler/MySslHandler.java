package com.jper.flycat.server.handler;

import com.jper.flycat.server.codec.ProxyMessageRequestDecoder;
import com.jper.flycat.server.codec.ProxyMessageResponseEncoder;
import com.jper.flycat.core.handler.DirectClientHandler;
import com.jper.flycat.core.handler.RelayHandler;
import com.jper.flycat.core.protocol.ProxyMessageRequest;
import com.jper.flycat.core.protocol.ProxyMessageResponse;
import com.jper.flycat.core.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;

/**
 * @author ywxiang
 * @date 2020/12/30 下午8:55
 */
@Slf4j
public class MySslHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProxyMessageRequest message = (ProxyMessageRequest) msg;
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                (GenericFutureListener<Future<Channel>>) future -> {
                    final Channel outboundChannel = future.getNow();
                    if (future.isSuccess()) {
                        outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                        ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                    } else {
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
        b.connect(message.getHost(), message.getPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("connect establish success, from {}:{}", message.getHost(), message.getPort());
                ProxyMessageResponse response = new ProxyMessageResponse(1, "success");
                ctx.writeAndFlush(response).addListener(future1 -> {
                    try {
                        ctx.channel().pipeline().remove(ProxyMessageRequestDecoder.class);
                        ctx.channel().pipeline().remove(ProxyMessageResponseEncoder.class);
                        ctx.channel().pipeline().remove(MySslHandler.class);
                    } catch (NoSuchElementException e) {
                        log.info("remove handlers");
                    }

                });
            } else {
                log.info("connect establish failed, from {}:{}", message.getHost(), message.getPort());
                ProxyMessageResponse response = new ProxyMessageResponse(0, "failed");
                ctx.writeAndFlush(response);
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
        });
        ctx.channel().pipeline().remove(this);
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("The client connects to the server successfully.");
    }
}
