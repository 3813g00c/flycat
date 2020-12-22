package com.jper.flycat.client.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 线程池配置类
 *
 * @author ywxiang
 * @date 2020/12/21 下午8:09
 */
@Configuration
public class ThreadPoolConfig {

    @Bean(value = "threadPoolInstance")
    public ExecutorService createThreadPoolInstance() {
        int cpuNum = Runtime.getRuntime().availableProcessors();
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("direct-connect-pool-%d").build();
        return new ThreadPoolExecutor(
                (int) Math.pow(2, cpuNum), (int) Math.pow(2, cpuNum), 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
