package com.jper.flycat.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Socks命令处理器
 *
 * @author ywxiang
 * @date 2020/12/17 下午9:06
 */
@Component
@Scope("prototype")
public class SocksCommandRequestHandler extends SimpleChannelInboundHandler<SocksCmdRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksCmdRequest msg) {
        String host = msg.host();
        int port = msg.port();
        ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4));
        ctx.pipeline().remove(this);
        System.out.println(host + ":" + port);
    }
}