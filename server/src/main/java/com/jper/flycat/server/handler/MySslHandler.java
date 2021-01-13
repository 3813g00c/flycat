package com.jper.flycat.server.handler;

import com.jper.flycat.core.codec.ProxyMessageDecoder;
import com.jper.flycat.core.handler.DirectClientHandler;
import com.jper.flycat.core.handler.RelayHandler;
import com.jper.flycat.core.protocol.ProxyMessage;
import com.jper.flycat.core.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ywxiang
 * @date 2020/12/30 下午8:55
 */
@Slf4j
public class MySslHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProxyMessage message = null;
        try {
            message = (ProxyMessage) msg;
        } catch (Exception e) {
            System.out.println(msg);
        }
        if (message != null) {
            ctx.channel().pipeline().remove(ProxyMessageDecoder.class);
            ctx.channel().pipeline().addLast(new TestHandler3());
            System.out.println("添加后：" + ctx.channel().pipeline().names());
            System.out.println(message);
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    (GenericFutureListener<Future<Channel>>) future -> {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            System.out.println("dadsadasda");
                            outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                            // ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                            System.out.println(ctx.channel());

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
                    log.info("success");
                    // Connection established use handler provided results
                    //log.info("connect establish success, from {}:{}", message.getHost(), message.getPort());
                } else {
                    //log.info("connect establish failed, from {}:{}", message.getHost(), message.getPort());
                    // Close the connection if the connection attempt has failed.
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
            //ctx.channel().pipeline().remove(this);

        } else {
            System.out.println("收到其他消息" + ctx.pipeline().names());
            System.out.println("else" + msg);
            ctx.fireChannelRead(msg);
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("连接激活，握手成功！");
    }
}
