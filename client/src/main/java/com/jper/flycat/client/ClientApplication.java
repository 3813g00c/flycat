package com.jper.flycat.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 客户端启动类
 *
 * @author ywxiang
 * @date 2020/12/16 下午8:19
 */
@SpringBootApplication(scanBasePackages = {"com.jper.flycat"})
public class ClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
