package com.samoy.chuanbillserver.config;

import com.samoy.chuanbillserver.job.BillReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail billReminderJobDetail() {
        return JobBuilder.newJob(BillReminderJob.class)
                .withIdentity("billReminderJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger billReminderTrigger(JobDetail billReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(billReminderJobDetail)
                .withIdentity("billReminderTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))
                .build();
    }
}
