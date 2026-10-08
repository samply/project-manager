package de.samply.app;

import de.samply.annotations.ConditionalOnModule;
import de.samply.modules.OptionalModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

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
    private final int actionsBatchCorePoolSize;
    private final int actionsBatchMaxPoolSize;
    private final int actionsBatchQueueCapacity;

    public ProjectManagerAsyncConfiguration(
            @Value(ProjectManagerConst.EMAIL_SENDER_CORE_POOL_SIZE_SV) int emailSenderCorePoolSize,
            @Value(ProjectManagerConst.EMAIL_SENDER_MAX_POOL_SIZE_SV) int emailSenderMaxPoolSize,
            @Value(ProjectManagerConst.EMAIL_SENDER_QUEUE_CAPACITY_SV) int emailSenderQueueCapacity,
            @Value(ProjectManagerConst.NOTIFICATION_CORE_POOL_SIZE_SV) int notificationCorePoolSize,
            @Value(ProjectManagerConst.NOTIFICATION_MAX_POOL_SIZE_SV) int notificationMaxPoolSize,
            @Value(ProjectManagerConst.NOTIFICATION_QUEUE_CAPACITY_SV) int notificationQueueCapacity,
            @Value(ProjectManagerConst.EXPORTER_CORE_POOL_SIZE_SV) int exporterCorePoolSize,
            @Value(ProjectManagerConst.EXPORTER_MAX_POOL_SIZE_SV) int exporterMaxPoolSize,
            @Value(ProjectManagerConst.EXPORTER_QUEUE_CAPACITY_SV) int exporterQueueCapacity,
            @Value(ProjectManagerConst.ACTIONS_BATCH_CORE_POOL_SIZE_SV) int actionsBatchCorePoolSize,
            @Value(ProjectManagerConst.ACTIONS_BATCH_MAX_POOL_SIZE_SV) int actionsBatchMaxPoolSize,
            @Value(ProjectManagerConst.ACTIONS_BATCH_QUEUE_CAPACITY_SV) int actionsBatchQueueCapacity) {
        this.emailSenderCorePoolSize = emailSenderCorePoolSize;
        this.emailSenderMaxPoolSize = emailSenderMaxPoolSize;
        this.emailSenderQueueCapacity = emailSenderQueueCapacity;
        this.notificationCorePoolSize = notificationCorePoolSize;
        this.notificationMaxPoolSize = notificationMaxPoolSize;
        this.notificationQueueCapacity = notificationQueueCapacity;
        this.exporterCorePoolSize = exporterCorePoolSize;
        this.exporterMaxPoolSize = exporterMaxPoolSize;
        this.exporterQueueCapacity = exporterQueueCapacity;
        this.actionsBatchCorePoolSize = actionsBatchCorePoolSize;
        this.actionsBatchMaxPoolSize = actionsBatchMaxPoolSize;
        this.actionsBatchQueueCapacity = actionsBatchQueueCapacity;
    }

    @Bean(name = ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    @ConditionalOnModule(OptionalModule.EMAILS)
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

    // Runs the entries of actions batches. A batch waits for its entries, so when threads and queue are full the
    // entry runs on the thread of its batch request instead of being rejected.
    @Bean(name = ProjectManagerConst.ASYNC_ACTIONS_BATCH_EXECUTOR)
    public ThreadPoolTaskExecutor actionsBatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(actionsBatchCorePoolSize);
        executor.setMaxPoolSize(actionsBatchMaxPoolSize);
        executor.setQueueCapacity(actionsBatchQueueCapacity);
        executor.setThreadNamePrefix(ProjectManagerConst.ASYNC_ACTIONS_BATCH_EXECUTOR + "-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
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
