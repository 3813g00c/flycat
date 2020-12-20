package com.jper.flycat.client.proxy;

/**
 * 发布订阅中心
 *
 * @author ywxiang
 * @date 2020/12/19 上午10:44
 */
public interface SubPubCentral {

    /**
     * 订阅
     *
     * @param publisher
     * @param subscriber
     * @return
     */
    boolean subscribe(SocksReqPublisher publisher, SocksReqSubscriber subscriber);

    /**
     * 取消订阅
     *
     * @param publisher
     * @param subscriber
     * @return
     */
    boolean unSubscribe(SocksReqPublisher publisher, SocksReqSubscriber subscriber);

    /**
     * 发布消息
     *
     * @param publisher
     * @param subscriber
     */
    void publish(SocksReqPublisher publisher, SocksReqSubscriber subscriber);
}
