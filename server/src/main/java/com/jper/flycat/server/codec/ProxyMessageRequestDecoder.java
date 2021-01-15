package com.jper.flycat.server.codec;

import com.jper.flycat.core.protocol.ProxyMessageRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LineBasedFrameDecoder;

/**
 * @author ywxiang
 * @date 2021/1/6 下午8:50
 */
public class ProxyMessageRequestDecoder extends LineBasedFrameDecoder {
    private final String shaPwd;

    public ProxyMessageRequestDecoder(int maxLength, String shaPwd) {
        super(maxLength);
        this.shaPwd = shaPwd;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, buffer);
        if (frame == null) {
            return null;
        }
        int index = frame.indexOf(frame.readerIndex(), frame.writerIndex(), (byte) ' ');
        byte[] bytesPwd = new byte[index];
        frame.getBytes(0, bytesPwd);
        String pwd = new String(bytesPwd);

        int index2 = frame.indexOf(index + 1, frame.writerIndex(), (byte) ' ');
        byte[] bytesHost = new byte[index2 - index - 1];

        frame.getBytes(index + 1, bytesHost);
        int p = frame.getInt(index2 + 1);
        ProxyMessageRequest message = new ProxyMessageRequest();
        message.setPassword(pwd);
        message.setHost(new String(bytesHost));
        message.setPort(p);
        return message;
    }
}
