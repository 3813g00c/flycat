package com.jper.flycat.core.protocol;

import lombok.Data;

/**
 * @author ywxiang
 * @date 2021/1/5 下午8:11
 */
@Data
public class ProxyMessage {
    private String password;

    private String host;

    private int port;
    // private SocksCmdRequest socksCmdRequest;

}
