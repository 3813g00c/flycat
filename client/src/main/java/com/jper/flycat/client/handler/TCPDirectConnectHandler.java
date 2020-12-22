package com.jper.flycat.client.handler;

import com.jper.flycat.client.proxy.SocksProxyRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author ywxiang
 * @date 2020/12/19 下午1:08
 */
@Slf4j
public class TCPDirectConnectHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final SocksProxyRequest request;

    public TCPDirectConnectHandler(SocksProxyRequest request) {
        this.request = request;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        msg.retain();
        request.getClientChannel().writeAndFlush(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Channel Active from {}:{}", request.getHost(), request.getPort());
        Channel sc = ctx.channel();
        request.setServerChannel(sc);
        ctx.fireChannelActive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            log.info("Direct TCP Connection force to close, from remote server {}:{}", request.getHost(), request.getPort());
        }
        request.getClientChannel().close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("connection close, from {}:{}", request.getHost(), request.getPort());
        ctx.pipeline().remove(this);
        request.getClientChannel().close();
        ctx.fireChannelInactive();
        ctx.close();
    }
}
