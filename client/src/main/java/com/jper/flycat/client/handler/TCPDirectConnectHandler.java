package com.jper.flycat.client.handler;

import com.jper.flycat.client.proxy.SocksProxyRequest;
import com.jper.flycat.client.proxy.SocksReqPublisher;
import com.jper.flycat.client.proxy.SubPubCentral;
import com.jper.flycat.client.proxy.SubPubCentralImpl;
import com.jper.flycat.core.util.SpringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author ywxiang
 * @date 2020/12/19 下午1:08
 */
public class TCPDirectConnectHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private SocksProxyRequest request;

    public TCPDirectConnectHandler(SocksProxyRequest request) {
        this.request = request;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        request.setMsg(msg);
        SocksReqPublisher publisher = new SocksReqPublisher(request.getHost() + request.getPort(), request);
        SubPubCentral subPubCentral = SpringUtil.getBean(SubPubCentralImpl.class);
        subPubCentral.publish(publisher, null);
    }
}
