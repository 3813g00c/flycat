package com.jper.flycat.client.proxy;

/**
 * 订阅者Id
 *
 * @author ywxiang
 * @date 2020/12/19 上午10:22
 */
public interface SocksReqSubscriber {

    /**
     * 接受代理请求
     *
     * @param request
     */
    void receive(SocksProxyRequest request);
}
