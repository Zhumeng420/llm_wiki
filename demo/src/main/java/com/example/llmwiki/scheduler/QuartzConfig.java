package com.example.llmwiki.scheduler;

import com.example.llmwiki.config.IngestProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.JobFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;

/**
 * Quartz 调度配置：注入 Spring 管理的 Job、按 cron 表达式启动 SourceWatcherJob。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuartzConfig {

    private static final String JOB_NAME = "sourceWatcherJob";
    private static final String TRIGGER_NAME = "sourceWatcherTrigger";

    private final IngestProperties ingestProperties;

    /**
     * 让 Quartz 通过 Spring 容器实例化 Job，从而支持依赖注入。
     */
    @Bean
    public JobFactory springJobFactory(ApplicationContext applicationContext) {
        return (bundle, scheduler) -> {
            Class<?> jobClass = bundle.getJobDetail().getJobClass();
            AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
            Object job = null;
            try {
                job = jobClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            factory.autowireBean(job);
            factory.initializeBean(job, jobClass.getSimpleName());
            return (org.quartz.Job) job;
        };
    }

    @Bean
    public JobDetail sourceWatcherJobDetail() {
        return JobBuilder.newJob(SourceWatcherJob.class)
                .withIdentity(JOB_NAME)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger sourceWatcherTrigger(JobDetail sourceWatcherJobDetail) {
        String cron = ingestProperties.getScheduler().getCron();
        return TriggerBuilder.newTrigger()
                .forJob(sourceWatcherJobDetail)
                .withIdentity(TRIGGER_NAME)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    /**
     * 调度器初始化后，将 Spring JobFactory 注入。采用 Customizer 避免 Scheduler 循环依赖。
     */
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerJobFactoryCustomizer(JobFactory jobFactory) {
        return factoryBean -> factoryBean.setJobFactory(jobFactory);
    }
}
