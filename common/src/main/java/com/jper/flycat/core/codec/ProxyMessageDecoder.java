package com.jper.flycat.core.codec;

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

        ByteBuf s = frame.slice(frame.readerIndex(), index);
        byte[] d = new byte[s.readableBytes()];
        s.getBytes(0, d);
        String dd = new String(d);
        // ProxyMessage d = new ProxyMessage(s, null, 0);
        return frame.slice(frame.readerIndex(), index);
    }
}
