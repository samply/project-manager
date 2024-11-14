package de.samply.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class ProjectManagerAsyncConfiguration {

    private final int emailSenderCorePoolSize;
    private final int emailSenderMaxPoolSize;
    private final int emailSenderQueueCapacity;


    public ProjectManagerAsyncConfiguration(
            @Value(ProjectManagerConst.EMAIL_SENDER_CORE_POOL_SIZE_SV) int emailSenderCorePoolSize,
            @Value(ProjectManagerConst.EMAIL_SENDER_MAX_POOL_SIZE_SV) int emailSenderMaxPoolSize,
            @Value(ProjectManagerConst.EMAIL_SENDER_QUEUE_CAPACITY_SV) int emailSenderQueueCapacity) {
        this.emailSenderCorePoolSize = emailSenderCorePoolSize;
        this.emailSenderMaxPoolSize = emailSenderMaxPoolSize;
        this.emailSenderQueueCapacity = emailSenderQueueCapacity;
    }

    @Bean(name = ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(emailSenderCorePoolSize);
        executor.setMaxPoolSize(emailSenderMaxPoolSize);
        executor.setQueueCapacity(emailSenderQueueCapacity);
        executor.setThreadNamePrefix(ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR + "-");
        executor.initialize();
        return executor;
    }

}
