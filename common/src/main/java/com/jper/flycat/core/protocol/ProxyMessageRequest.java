package com.jper.flycat.core.protocol;

import lombok.Data;

/**
 * @author ywxiang
 * @date 2021/1/5 下午8:11
 */
@Data
public class ProxyMessageRequest {

    public ProxyMessageRequest() {
    }

    public ProxyMessageRequest(String password, String host, int port) {
        this.password = password;
        this.host = host;
        this.port = port;
    }

    private String password;

    private String host;

    private int port;
}
