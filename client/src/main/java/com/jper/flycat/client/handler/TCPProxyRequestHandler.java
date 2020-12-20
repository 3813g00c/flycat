package com.jper.flycat.client.handler;

import com.jper.flycat.client.proxy.SocksProxyRequest;
import com.jper.flycat.client.router.RouteManagement;
import com.jper.flycat.core.util.SpringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.UnknownHostException;

/**
 * @author ywxiang
 * @date 2020/12/19 下午1:46
 */
@Slf4j
public class TCPProxyRequestHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final String host;

    private final int port;

    public TCPProxyRequestHandler(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        msg.retain();
        RouteManagement routeManagement = SpringUtil.getBean(RouteManagement.class);
        String key = host + port;
        SocksProxyRequest request;
        if (routeManagement.getRouteMap().containsKey(key)) {
            request = routeManagement.selRouter(key);
            request.getMsgQueue().put(msg);
        } else {
            request = new SocksProxyRequest(host, port, ctx.channel());
            request.getMsgQueue().put(msg);
            routeManagement.addRule(request);
        }
        int status = request.getServerChannelStatus();
        if (status == 0) {
            doConnect(request);
        } else if (status == 2) {
            log.info("connecting！");
        }

    }

    private void doConnect(SocksProxyRequest request) {
        String host = request.getHost();
        int port = request.getPort();
        log.info("start connect to server {}:{}...", host, port);
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addFirst(new TCPDirectConnectHandler(request));
                    }
                });
        // 正在连接
        request.setServerChannelStatus(2);
        b.connect(host, port).addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                if (f.cause() instanceof ConnectTimeoutException) {
                    log.info("connect to " + request.getHost() + ":" + request.getPort() + " failure, cause connect timeout");
                } else if (f.cause() instanceof UnknownHostException) {
                    log.warn("Connect failure: Unknown domain {}", request.getHost());
                } else {
                    log.warn("connect establish failure, from " + request.getHost() + ":" + request.getPort(), f.cause());
                }
                request.getClientChannel().close();
                request.setServerChannelStatus(-2);
                f.channel().close();
            } else {
                log.info("connect establish success, from {}:{}", request.getHost(), request.getPort());
                request.setServerChannelStatus(1);
                request.setServerChannel(f.channel());
            }
        });
    }
}
