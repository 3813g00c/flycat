package com.jper.flycat.server.codec;

import com.jper.flycat.core.protocol.ProxyMessageResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author ywxiang
 * @date 2021/1/14 下午7:21
 */
public class ProxyMessageResponseEncoder extends MessageToByteEncoder<ProxyMessageResponse> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProxyMessageResponse msg, ByteBuf out) throws Exception {
        try {
            if (msg == null) {
                throw new Exception("The encode message is null");
            }
            out.writeInt(msg.getResult());
            out.writeBytes(msg.getMessage().getBytes());
            out.writeBytes("\r\n".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
