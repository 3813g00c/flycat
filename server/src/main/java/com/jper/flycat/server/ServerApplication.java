package com.jper.flycat.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author ywxiang
 * @date 2020/12/30 下午8:28
 */
@SpringBootApplication(scanBasePackages = {"com.jper.flycat"})
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
