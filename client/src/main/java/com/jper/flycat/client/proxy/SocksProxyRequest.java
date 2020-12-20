package com.jper.flycat.client.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * @author ywxiang
 * @date 2020/12/18 下午8:26
 */
@Getter
@Setter
public class SocksProxyRequest {

    /**
     * 目标服务器
     */
    private String host;

    /**
     * 目标端口
     */
    private int port;

    /**
     * 客户端Channel通道
     */
    private Channel clientChannel;

    /**
     * 服务器Channel通道
     */
    private Channel serverChannel;

    /**
     * 其次请求的消息体
     */
    private ByteBuf msg;

    /**
     * 当前服务器连接状态
     * 0：未连接
     * 1：已连接且存活
     * 2：正在连接
     * -1：已连接但连接未存活
     * -2：连接失败
     */
    private int serverChannelStatus = 0;

    /**
     * 消息队列
     */
    private LinkedBlockingQueue<ByteBuf> msgQueue;

    public SocksProxyRequest(String host, int port, Channel clientChannel) {
        this.host = host;
        this.port = port;
        this.clientChannel = clientChannel;
        this.msgQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public String toString() {
        return "SocksProxyRequest{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", clientChannel=" + clientChannel +
                ", serverChannel=" + serverChannel +
                '}';
    }
}
