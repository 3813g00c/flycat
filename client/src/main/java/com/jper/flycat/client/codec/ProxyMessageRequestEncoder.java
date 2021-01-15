package com.jper.flycat.client.codec;

import com.jper.flycat.core.protocol.ProxyMessageRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author ywxiang
 * @date 2021/1/6 下午8:31
 */
public class ProxyMessageRequestEncoder extends MessageToByteEncoder<ProxyMessageRequest> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ProxyMessageRequest msg, ByteBuf out) throws Exception {
        try {
            if (msg == null) {
                throw new Exception("The encode message is null");
            }
            out.writeBytes(msg.getPassword().getBytes());
            out.writeBytes(" ".getBytes());
            out.writeBytes(msg.getHost().getBytes());
            out.writeBytes(" ".getBytes());
            out.writeInt(msg.getPort());
            out.writeBytes("\r\n".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
