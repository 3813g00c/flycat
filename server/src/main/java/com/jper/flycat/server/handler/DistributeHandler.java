package com.jper.flycat.server.handler;

import com.jper.flycat.core.protocol.ProxyMessageRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequestDecoder;

/**
 * @author ywxiang
 * @date 2021/1/16 上午10:42
 */
public class DistributeHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ProxyMessageRequest) {
            ctx.channel().pipeline().addLast(new ServiceProxyHandler());
            ctx.fireChannelRead(msg);
        } else {
            ctx.channel().pipeline().addLast(new HttpRequestDecoder());
            ctx.channel().pipeline().addLast(new WrongPasswordHandler());
            ctx.fireChannelActive();
        }
        ctx.channel().pipeline().remove(this);
    }
}
