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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
            ExecutorService executorService = (ExecutorService) SpringUtil.getBean("threadPoolInstance");
            executorService.execute(new My(request));
            executorService.execute(() -> {
                while (request.getServerChannelStatus() != -1) {
                    Channel cc = request.getClientChannel();
                    if (cc != null && !cc.isActive()) {
                        routeManagement.getRouteMap().remove(request.getHost() + request.getPort());
                        break;
                    }

                    Channel sc = request.getServerChannel();
                    if (sc == null) {
                        continue;
                    }

                    if (!sc.isActive()) {
                        routeManagement.getRouteMap().remove(request.getHost() + request.getPort());
                        break;
                    }
                    LinkedBlockingQueue<ByteBuf> queue = request.getMsgQueue();
                    ByteBuf buf;
                    try { //之所以采用循环是为了转发客户端请求时避免消息不完整
                        int time = 0;
                        while ((buf = queue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                            time++;
                            if (time / 4 == 1) {  //每4次写入刷新一次
                                sc.writeAndFlush(buf);
                                time = 0;
                            } else {
                                sc.write(buf);
                            }
                        }

                        if (time > 0) {
                            sc.flush();
                        }

                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            int queueSize = tpe.getQueue().size();
            System.out.println("当前排队线程数：" + queueSize);

            int activeCount = tpe.getActiveCount();
            System.out.println("当前活动线程数：" + activeCount);

            long completedTaskCount = tpe.getCompletedTaskCount();
            System.out.println("执行完成线程数：" + completedTaskCount);

            long taskCount = tpe.getTaskCount();
            System.out.println("总线程数：" + taskCount);

        }
    }

    private class My implements Runnable {

        private final SocksProxyRequest request;

        public My(SocksProxyRequest request) {
            this.request = request;
        }

        @Override
        public void run() {
            doConnect(request);
        }
    }

    private class TestConn implements Runnable {
        private SocksProxyRequest request;

        public TestConn(SocksProxyRequest request) {
            this.request = request;
        }

        @Override
        public void run() {
            Channel sc = request.getServerChannel();
            while (sc != null && sc.isActive()) {
                LinkedBlockingQueue<ByteBuf> queue = request.getMsgQueue();
                ByteBuf buf;
                try { //之所以采用循环是为了转发客户端请求时避免消息不完整
                    int time = 0;
                    while ((buf = queue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                        time++;
                        if (time / 4 == 1) {  //每4次写入刷新一次
                            sc.writeAndFlush(buf);
                            time = 0;
                        } else {
                            sc.write(buf);
                        }
                    }
                    if (time > 0) {
                        sc.flush();
                    }

                    while (!queue.isEmpty()) {
                        sc.writeAndFlush(queue.poll(1, TimeUnit.MILLISECONDS));
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
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
                request.setServerChannelStatus(-1);
                f.channel().close();
            } else {
                log.info("connect establish success, from {}:{}", request.getHost(), request.getPort());
                request.setServerChannel(f.channel());
            }
        });
    }
}
