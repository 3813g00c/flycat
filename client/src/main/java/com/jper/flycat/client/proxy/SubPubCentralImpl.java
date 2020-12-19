package com.jper.flycat.client.proxy;

import com.jper.flycat.client.router.RouteManagement;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;

/**
 * 发布订阅中心实现类
 *
 * @author ywxiang
 * @date 2020/12/19 上午10:47
 */
@Service
@Slf4j
public class SubPubCentralImpl implements SubPubCentral {

    @Autowired
    private RouteManagement routeManagement;

    @Override
    public boolean subscribe(SocksReqPublisher publisher, SocksReqSubscriber subscriber) {
        try {
            SocksProxyRequest request = routeManagement.selRouter(publisher.getPublisherId());
            if (request == null) {
                routeManagement.addRule(doConnect(publisher.getSocksProxyRequest()));
            } else {
                if (!request.getServerChannel().isActive()) {
                    request.getServerChannel().close();
                    request.setServerChannel(null);
                    request.setServerChannel(doConnect(publisher.getSocksProxyRequest()).getServerChannel());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean unSubscribe(SocksReqPublisher publisher, SocksReqSubscriber subscriber) {
        return false;
    }

    @Override
    public void publish(SocksReqPublisher publisher, SocksReqSubscriber subscriber) {
        SocksProxyRequest request = routeManagement.selRouter(publisher.getPublisherId());
        Channel channel = request.getServerChannel();
        channel.writeAndFlush(publisher.getSocksProxyRequest().getMsg());
    }

    private SocksProxyRequest doConnect(SocksProxyRequest request) {
        String host = request.getHost();
        int port = request.getPort();
        Bootstrap b = new Bootstrap();
        b.connect(host, port).addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                if (f.cause() instanceof ConnectTimeoutException) {
                    if (log.isInfoEnabled()) {
                        log.info("connect to " + request.getHost() + ":" + request.getPort() + " failure, cause connect timeout");
                    }
                } else if (f.cause() instanceof UnknownHostException) {
                    if (log.isWarnEnabled()) {
                        log.warn("Connect failure: Unknown domain {}", request.getHost());
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("connect establish failure, from " + request.getHost() + ":" + request.getPort(), f.cause());
                    }
                }
                request.getClientChannel().close();
                f.channel().close();
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("connect establish success, from {}:{}", request.getHost(), request.getPort());
                }
                request.setServerChannel(f.channel());
            }
        });
        return request;
    }
}
