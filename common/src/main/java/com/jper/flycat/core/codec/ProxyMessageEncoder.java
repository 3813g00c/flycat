package com.jper.flycat.core.codec;

import com.jper.flycat.core.protocol.ProxyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author ywxiang
 * @date 2021/1/6 下午8:31
 */
public class ProxyMessageEncoder extends MessageToByteEncoder<ProxyMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ProxyMessage msg, ByteBuf out) throws Exception {
        try {
            if (msg == null) {
                throw new Exception("The encode message is null");
            }
            out.writeBytes(msg.getPassword().getBytes());
            out.writeBytes(" ".getBytes());
            out.writeBytes(msg.getHost().getBytes());
            out.writeBytes(" ".getBytes());
            out.writeByte(msg.getPort());
            out.writeBytes("\r\n".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
