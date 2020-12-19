package com.jper.flycat.client.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;


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

//    /**
//     * 客户端Channel状态
//     */
//    private int clientChannelStatus;
//
//    /**
//     * 服务器Channel状态
//     */
//    private int serverChannelStatus;

    public SocksProxyRequest(String host, int port, Channel clientChannel, Channel serverChannel) {
        this.host = host;
        this.port = port;
        this.clientChannel = clientChannel;
        this.serverChannel = serverChannel;
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
