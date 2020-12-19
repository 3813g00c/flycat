package com.jper.flycat.client.proxy;

import lombok.Getter;
import lombok.Setter;

/**
 * Socks代理请求发布着
 *
 * @author ywxiang
 * @date 2020/12/19 上午10:19
 */
@Getter
@Setter
public class SocksReqPublisher {

    /**
     * 发布者的唯一Id
     */
    private String publisherId;

    /**
     * 发布者的请求
     */
    private SocksProxyRequest socksProxyRequest;

    public SocksReqPublisher(String publisherId, SocksProxyRequest socksProxyRequest) {
        this.publisherId = publisherId;
        this.socksProxyRequest = socksProxyRequest;
    }
}
