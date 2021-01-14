package com.jper.flycat.client.handler;

import com.jper.flycat.core.codec.ProxyMessageRequestEncoder;
import com.jper.flycat.core.codec.ProxyMessageResponseDecoder;
import com.jper.flycat.core.protocol.ProxyMessageRequest;
import com.jper.flycat.core.protocol.ProxyMessageResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ywxiang
 * @date 2021/1/11 下午8:46
 */
@Slf4j
public class ProxyHandler extends ChannelInboundHandlerAdapter {
    private final ProxyMessageRequest message;
    private final Channel inBoundChannel;
    private final Promise<Channel> promise;

    public ProxyHandler(ProxyMessageRequest message, Channel inBoundChannel, Promise<Channel> promise) {
        this.message = message;
        this.inBoundChannel = inBoundChannel;
        this.promise = promise;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        ProxyMessageResponse response = (ProxyMessageResponse) msg;
        if (response.getResult() == 1) {
            log.info("Server proxy success.");
            ctx.channel().pipeline().remove(this);
            ctx.channel().pipeline().remove(ProxyMessageResponseDecoder.class);
            ctx.channel().pipeline().remove(ProxyMessageRequestEncoder.class);
            promise.setSuccess(ctx.channel());
        } else {
            log.info("Server proxy failed");
            inBoundChannel.close();
            ctx.channel().close();
        }
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
