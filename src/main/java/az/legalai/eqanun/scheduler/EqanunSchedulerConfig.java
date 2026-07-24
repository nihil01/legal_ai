package az.legalai.eqanun.scheduler;

import az.legalai.eqanun.service.EqanunLawSyncService;
import az.legalai.eqanun.service.PlaceholderEqanunLawSyncService;
import java.time.ZoneId;
import java.util.TimeZone;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "app.eqanun.sync",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EqanunSchedulerConfig {
    @Bean
    @ConditionalOnMissingBean(EqanunLawSyncService.class)
    EqanunLawSyncService placeholderEqanunLawSyncService() {
        return new PlaceholderEqanunLawSyncService();
    }

    @Bean
    JobDetail eqanunLawSyncJobDetail() {
        return JobBuilder.newJob(EqanunLawSyncJob.class)
                .withIdentity("eqanun-law-sync")
                .withDescription("Checks Azerbaijani laws for e-qanun updates")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger eqanunLawSyncTrigger(
            JobDetail eqanunLawSyncJobDetail,
            @Value("${app.eqanun.sync.cron:0 0 3 * * ?}") String cron,
            @Value("${app.eqanun.sync.time-zone:Asia/Baku}") String timeZone) {
        ZoneId zoneId = ZoneId.of(timeZone);
        return TriggerBuilder.newTrigger()
                .forJob(eqanunLawSyncJobDetail)
                .withIdentity("eqanun-law-sync-trigger")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(cron)
                                .inTimeZone(TimeZone.getTimeZone(zoneId))
                                .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
