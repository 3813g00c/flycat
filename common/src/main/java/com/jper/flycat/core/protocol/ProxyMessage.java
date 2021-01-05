package com.jper.flycat.core.protocol;

import io.netty.handler.codec.socks.SocksCmdRequest;

/**
 * @author ywxiang
 * @date 2021/1/5 下午8:11
 */
public class ProxyMessage {
    private String password;

    private SocksCmdRequest socksCmdRequest;
}
