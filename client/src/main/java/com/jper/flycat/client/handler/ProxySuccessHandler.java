package com.jper.flycat.client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ywxiang
 * @date 2021/1/11 下午8:18
 */
@Slf4j
public class ProxySuccessHandler extends ChannelInboundHandlerAdapter {
    private final Promise<Channel> promise;

    public ProxySuccessHandler(Promise<Channel> promise) {
        this.promise = promise;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().remove(this);
        promise.setSuccess(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        promise.setFailure(cause);
    }

}
