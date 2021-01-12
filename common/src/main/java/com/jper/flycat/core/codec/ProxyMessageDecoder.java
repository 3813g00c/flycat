package com.jper.flycat.core.codec;

import com.jper.flycat.core.protocol.ProxyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LineBasedFrameDecoder;

/**
 * @author ywxiang
 * @date 2021/1/6 下午8:50
 */
public class ProxyMessageDecoder extends LineBasedFrameDecoder {
    public ProxyMessageDecoder(int maxLength) {
        super(maxLength);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, buffer);
        if (frame == null) {
            return null;
        }
        int index = frame.indexOf(frame.readerIndex(), frame.writerIndex(), (byte) ' ');
        int index2 = frame.indexOf(index + 1, frame.writerIndex(), (byte) ' ');
//        ByteBuf password = frame.slice(frame.readerIndex(), index);
//        ByteBuf host = frame.slice(index + 1, index2);
//        ByteBuf port = frame.slice(index2 + 1, frame.writerIndex());
//        byte[] pwd_ = new byte[password.readableBytes()];
//        byte[] host_ = new byte[host.readableBytes()];
//        byte[] port_ = new byte[port.readableBytes()];
        byte[] pwd_ = new byte[index];
        byte[] host_ = new byte[index2 - index - 1];
//        password.getBytes(0, pwd_);
//        host.getBytes(0, host_);
//        port.getBytes(0, port_);
        frame.getBytes(0, pwd_);
        frame.getBytes(index + 1, host_);
        int p = frame.getInt(index2 + 1);
        ProxyMessage message = new ProxyMessage();
        message.setPassword(new String(pwd_));
        message.setHost(new String(host_));
        message.setPort(p);
        return message;
    }
}
