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
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 请求从Channel读取一个消息
        ctx.channel().read();
    }

    /**
     * 解码第一个消息，有可能是稀有稀有消息，也有可能是正常的Https请求
     * 如果密码验证通过，构造Proxy请求对象发送给下一个处理器
     * 如果是Http请求，发送拷贝的原Buffer
     *
     * @param ctx    ctx
     * @param buffer buffer
     * @return ByteBuf
     * @throws Exception Exception
     */
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        try {
            int index = buffer.indexOf(buffer.readerIndex(), buffer.writerIndex(), (byte) ' ');
            byte[] bytesPwd = new byte[index];
            buffer.getBytes(0, bytesPwd);
            String pwd = new String(bytesPwd);
            if (pwd.equals(shaPwd)) {
                ByteBuf frame = (ByteBuf) super.decode(ctx, buffer);
                int index2 = frame.indexOf(index + 1, frame.writerIndex(), (byte) ' ');
                byte[] bytesHost = new byte[index2 - index - 1];
                buffer.getBytes(index + 1, bytesHost);
                int p = buffer.getInt(index2 + 1);
                ProxyMessageRequest message = new ProxyMessageRequest();
                message.setPassword(pwd);
                message.setHost(new String(bytesHost));
                message.setPort(p);
                return message;
            } else {
                ctx.channel().pipeline().remove(this);
                return buffer;
            }
        } catch (Exception e) {
            ctx.channel().pipeline().remove(this);
            return buffer;
        }
    }
}
