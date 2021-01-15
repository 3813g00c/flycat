package com.jper.flycat.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ywxiang
 * @date 2021/1/15 下午7:32
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "netty")
public class FlyCatConfig {
    private String runType;

    private String localAddr;

    private int localPort;

    private String remoteAddr;

    private int remotePort;

    private String password;
}
