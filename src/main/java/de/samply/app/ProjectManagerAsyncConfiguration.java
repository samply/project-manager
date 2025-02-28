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
    private final int notificationCorePoolSize;
    private final int notificationMaxPoolSize;
    private final int notificationQueueCapacity;
    private final int exporterCorePoolSize;
    private final int exporterMaxPoolSize;
    private final int exporterQueueCapacity;


    public ProjectManagerAsyncConfiguration(
            @Value(ProjectManagerConst.EMAIL_SENDER_CORE_POOL_SIZE_SV) int emailSenderCorePoolSize,
            @Value(ProjectManagerConst.EMAIL_SENDER_MAX_POOL_SIZE_SV) int emailSenderMaxPoolSize,
            @Value(ProjectManagerConst.EMAIL_SENDER_QUEUE_CAPACITY_SV) int emailSenderQueueCapacity,
            @Value(ProjectManagerConst.NOTIFICATION_CORE_POOL_SIZE_SV) int notificationCorePoolSize,
            @Value(ProjectManagerConst.NOTIFICATION_MAX_POOL_SIZE_SV) int notificationMaxPoolSize,
            @Value(ProjectManagerConst.NOTIFICATION_QUEUE_CAPACITY_SV) int notificationQueueCapacity,
            @Value(ProjectManagerConst.EXPORTER_CORE_POOL_SIZE_SV) int exporterCorePoolSize,
            @Value(ProjectManagerConst.EXPORTER_MAX_POOL_SIZE_SV) int exporterMaxPoolSize,
            @Value(ProjectManagerConst.EXPORTER_QUEUE_CAPACITY_SV) int exporterQueueCapacity) {
        this.emailSenderCorePoolSize = emailSenderCorePoolSize;
        this.emailSenderMaxPoolSize = emailSenderMaxPoolSize;
        this.emailSenderQueueCapacity = emailSenderQueueCapacity;
        this.notificationCorePoolSize = notificationCorePoolSize;
        this.notificationMaxPoolSize = notificationMaxPoolSize;
        this.notificationQueueCapacity = notificationQueueCapacity;
        this.exporterCorePoolSize = exporterCorePoolSize;
        this.exporterMaxPoolSize = exporterMaxPoolSize;
        this.exporterQueueCapacity = exporterQueueCapacity;
    }

    @Bean(name = ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    public Executor emailSenderExecutor() {
        return createEmailSenderExecutor(emailSenderCorePoolSize, emailSenderMaxPoolSize,
                emailSenderQueueCapacity, ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR);
    }

    @Bean(name = ProjectManagerConst.ASYNC_NOTIFICATION_EXECUTOR)
    public Executor notificationExecutor() {
        return createEmailSenderExecutor(notificationCorePoolSize, notificationMaxPoolSize,
                notificationQueueCapacity, ProjectManagerConst.ASYNC_NOTIFICATION_EXECUTOR);
    }

    @Bean(name = ProjectManagerConst.ASYNC_EXPORTER_EXECUTOR)
    public Executor exporterExecutor() {
        return createEmailSenderExecutor(exporterCorePoolSize, exporterMaxPoolSize,
                exporterQueueCapacity, ProjectManagerConst.ASYNC_EXPORTER_EXECUTOR);
    }

    private Executor createEmailSenderExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(prefix + "-");
        executor.initialize();
        return executor;
    }

}
