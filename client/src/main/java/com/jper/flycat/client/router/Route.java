package com.jper.flycat.client.router;

import com.jper.flycat.client.proxy.SocksProxyRequest;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ywxiang
 * @date 2020/12/18 下午9:11
 */
@Getter
public class Route {
    private final Map<String, SocksProxyRequest> routeMap = new ConcurrentHashMap<>();

    /**
     * 添加路由信息
     *
     * @param request 请求
     */
    public void addRule(SocksProxyRequest request) {
        routeMap.put(getKey(request), request);
    }

    /**
     * 获取路由信息
     *
     * @param request 请求
     * @return SocksProxyRequest
     */
    public SocksProxyRequest selRouter(SocksProxyRequest request) {
        return routeMap.get(getKey(request));
    }

    private String getKey(SocksProxyRequest request) {
        String host = request.getHost();
        int port = request.getPort();
        return host + port;
    }

}
