package com.jper.flycat.client;

import com.jper.flycat.client.handler.SocksRequestHandler;
import com.jper.flycat.client.proxy.SocksProxyRequest;
import com.jper.flycat.client.router.RouteManagement;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 初始化Netty客户端服务
 *
 * @author ywxiang
 * @date 2020/12/16 下午8:26
 */
@Component
@Slf4j
public class ClientRunner implements ApplicationRunner, ApplicationListener<ContextClosedEvent>, ApplicationContextAware {

    @Value("${netty.port}")
    private int port;

    @Value("${netty.host}")
    private String host;

    @Resource(name = "threadPoolInstance")
    private ExecutorService executorService;

    @Autowired
    private SocksRequestHandler socksRequestHandler;

    @Autowired
    private RouteManagement routeManagement;

    private ApplicationContext applicationContext;

    private Channel channel;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Override
    public void run(ApplicationArguments args) {
        log.info("启动客户端服务");
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBoot = new ServerBootstrap();
            serverBoot.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new SocksInitRequestDecoder());
                            p.addLast(new SocksMessageEncoder());
                            p.addLast(socksRequestHandler);
                        }
                    });
            ChannelFuture future = serverBoot.bind(new InetSocketAddress(this.host, this.port)).sync();
            this.channel = future.channel();
            future.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    log.info("Server bound");
                } else {
                    log.info("Bound attempt failed");
                    channelFuture.cause().printStackTrace();
                }
            });
        } catch (InterruptedException i) {
            log.error("ClientServer 启动出现异常，端口：{}，cause：{}", this.port, i.getMessage());
        }

//        MyThread mt1 = new MyThread();
//        Thread t1 = new Thread(mt1);
//        t1.start();
        executorService.execute(new MessageForward());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        if (this.channel != null) {
            try {
                this.channel.close();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
        log.info("客户端服务停止");
    }

    private class MessageForward implements Runnable {
        @Override
        public void run() {
            for (SocksProxyRequest request : routeManagement.getRouteMap().values()) {
                System.out.println(routeManagement.getRouteMap().size());
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
            }
        }
    }

    private class MyThread implements Runnable {
        @Override
        public void run() {
            Thread t = Thread.currentThread();
            while (!t.isInterrupted()) {
                for (SocksProxyRequest request : routeManagement.getRouteMap().values()) {
                    Channel cc = request.getClientChannel();
                    if (cc != null && !cc.isActive()) {
                        routeManagement.getRouteMap().remove(request.getHost() + request.getPort());
                        continue;
                    }

                    Channel sc = request.getServerChannel();
                    if (sc == null) {
                        continue;
                    }

                    if (!sc.isActive()) {
                        routeManagement.getRouteMap().remove(request.getHost() + request.getPort());
                        continue;
                    }

                    LinkedBlockingQueue<ByteBuf> queue = request.getMsgQueue();
                    ByteBuf buf;
                    try { //之所以采用循环是为了转发客户端请求时避免消息不完整
//                        int time = 0;
//                        int s = 0;
//                        while ((buf = queue.poll(1, TimeUnit.MILLISECONDS)) != null) {
//                            time++;
//                            s++;
//                            if(time / 4 == 1) {  //每4次写入刷新一次
//                                sc.writeAndFlush(buf);
//                                time = 0;
//                            } else {
//                                sc.write(buf);
//                            }
//                            if (s > 12) {
//                                break;
//                            }
//                        }
//                        if(time > 0) {
//                            sc.flush();
//                        }

                        int s = 0;
                        while (!queue.isEmpty()) {
                            sc.writeAndFlush(queue.poll(1, TimeUnit.MILLISECONDS));
                            s++;
                            if (s > 12) {
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
    }
}
