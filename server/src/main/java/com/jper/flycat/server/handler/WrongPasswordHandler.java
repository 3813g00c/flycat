package com.jper.flycat.server.handler;

import com.jper.flycat.core.handler.RelayHandler;
import com.jper.flycat.core.util.SocksServerUtils;
import com.jper.flycat.core.util.SpringUtil;
import com.jper.flycat.server.config.FlyCatConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 密码验证失败Handler
 *
 * @author ywxiang
 * @date 2021/1/16 上午9:49
 */
@Slf4j
public class WrongPasswordHandler extends ChannelInboundHandlerAdapter {

    private Channel outBoundChannel;

    private Object firstMsg;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        FlyCatConfig catConfig = SpringUtil.getBean(FlyCatConfig.class);
        String remoteAddr = catConfig.getRemoteAddr();
        int remotePort = catConfig.getRemotePort();
        final Channel inBoundChannel = ctx.channel();
        Bootstrap b = new Bootstrap();
        b.group(inBoundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpRequestEncoder());
                        p.addLast(new RelayHandler(inBoundChannel));
                    }
                });
        ChannelFuture future1 = b.connect(remoteAddr, remotePort);
        outBoundChannel = future1.channel();
        future1.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ReferenceCountUtil.refCnt(firstMsg);
                outBoundChannel.writeAndFlush(firstMsg).addListener((ChannelFutureListener) future2 -> {
                    inBoundChannel.read();
                });
                log.warn("Received other HTTPS request message, proxy to local springboot service port.");
            } else {
                SocksServerUtils.closeOnFlush(inBoundChannel);
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        // 缓存第一个消息
        firstMsg = msg;
        if (outBoundChannel.isActive()) {
            outBoundChannel.writeAndFlush(msg).addListener(future -> {
                if (future.isSuccess()) {
                    // ReferenceCountUtil.release(msg);
                    ctx.channel().read();
                } else {
                    outBoundChannel.close();
                }
            });
        }
    }

}
