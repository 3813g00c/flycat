package com.jper.flycat.client.codec;

import com.jper.flycat.core.protocol.ProxyMessageResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LineBasedFrameDecoder;

/**
 * @author ywxiang
 * @date 2021/1/14 下午7:31
 */
public class ProxyMessageResponseDecoder extends LineBasedFrameDecoder {

    public ProxyMessageResponseDecoder(int maxLength) {
        super(maxLength);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, buffer);
        if (frame == null) {
            return null;
        }
        int result = frame.readInt();
        byte[] message = new byte[frame.readableBytes()];
        frame.readBytes(message);
        String msg = new String(message);
        return new ProxyMessageResponse(result, msg);
    }

}
