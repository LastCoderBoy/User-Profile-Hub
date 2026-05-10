package com.jk.User_Profile_Hub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Default executor — used by @Async without a named pool.
     * Fast, lightweight tasks: token revocation, notifications, audit writes.
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // Minimum threads
        executor.setMaxPoolSize(10);           // Maximum threads
        executor.setQueueCapacity(25);         // Queue size
        executor.setThreadNamePrefix("Async-"); // Thread name prefix
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("[ASYNC-CONFIG] Thread pool initialized: core={}, max={}, queue={}",
                5, 10, 25);

        return executor;
    }

    /**
     * Dedicated executor for file processing pipeline.
     * Slow, I/O+CPU-bound tasks: virus scan, thumbnail generation, PDF extraction.
     * Isolated so file bursts never starve the taskExecutor pool.
     */
    @Bean(name = "fileAsyncExecutor")
    public Executor fileAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("file-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // CallerRunsPolicy: back-pressure instead of silent drop on queue full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        log.info("[ASYNC-CONFIG] fileAsyncExecutor initialized: core=4, max=8, queue=50");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("[ASYNC-ERROR] Exception in async method: {} - Error: {}",
                    method.getName(), throwable.getMessage(), throwable);
        };
    }
}
