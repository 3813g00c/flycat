package com.jper.flycat.client.handler;

import com.jper.flycat.core.protocol.ProxyMessage;
import com.jper.flycat.core.util.SocksServerUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ywxiang
 * @date 2021/1/11 下午8:28
 */
@Slf4j
public class TestHandler extends ChannelInboundHandlerAdapter {
    private final Channel relayChannel;
    private final ProxyMessage message;

    public TestHandler(Channel relayChannel, ProxyMessage message) {
        this.relayChannel = relayChannel;
        this.message = message;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("dsadsadsadsadsa");
        ctx.writeAndFlush(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (relayChannel.isActive()) {
            relayChannel.writeAndFlush(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (relayChannel.isActive()) {
            SocksServerUtils.closeOnFlush(relayChannel);
        }
        log.info("connection close");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
