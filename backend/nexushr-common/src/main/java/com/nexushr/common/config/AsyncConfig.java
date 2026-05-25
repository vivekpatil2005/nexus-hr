package com.nexushr.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * Async configuration leveraging Java 25 virtual threads.
 * Uses {@link SimpleAsyncTaskExecutor} with virtual threads enabled,
 * providing lightweight concurrency for {@code @Async} annotated methods
 * without the overhead of a traditional thread pool.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Creates an async task executor backed by virtual threads.
     * Virtual threads are ideal for I/O-bound tasks such as sending emails,
     * calling external APIs, and performing audit logging.
     *
     * @return the virtual-thread-based executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("Configuring async executor with virtual threads");
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("nexushr-vt-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
