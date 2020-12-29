package com.jper.flycat.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Socks请求处理类
 *
 * @author ywxiang
 * @date 2020/12/17 下午8:51
 */
@Component
@ChannelHandler.Sharable
public class SocksRequestHandler extends SimpleChannelInboundHandler<SocksRequest> {

    private final SocksCommandRequestHandler socksCommandRequestHandler = new SocksCommandRequestHandler();

    @Value("${netty.auth}")
    private volatile boolean auth;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksRequest request) {

        switch (request.requestType()) {
            case INIT: {
                // 如果是初始化报文
                if (!auth) {
                    ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                    ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                } else {
                    ctx.pipeline().addFirst(new SocksAuthRequestDecoder());
                    ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.AUTH_PASSWORD));
                }
                break;
            }

            case CMD: {
                SocksCmdRequest req = (SocksCmdRequest) request;
                SocksCmdType type = req.cmdType();
                //如果是TCP代理
                if (type == SocksCmdType.CONNECT) {
                    //添加SocksCommandRequestHandler，并移除当前Handler
                    ctx.pipeline().addLast("socksCommandRequestHandler", socksCommandRequestHandler).remove(this);
                    //传递给SocksCommandRequestHandler处理
                    ctx.fireChannelRead(req);
                } else {
                    //如果是UDP或者其他类型的代理，则返回COMMAND_NOT_SUPPORTED并关闭连接
                    ctx.writeAndFlush(new SocksCmdResponse(SocksCmdStatus.COMMAND_NOT_SUPPORTED, SocksAddressType.IPv4));
                    ctx.close();
                    return;
                }
                break;
            }

            case UNKNOWN: {
                ctx.close();
            }

            default:
                ctx.close();
        }
    }
}
