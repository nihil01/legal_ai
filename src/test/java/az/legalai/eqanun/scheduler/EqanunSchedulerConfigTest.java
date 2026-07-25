package az.legalai.eqanun.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;

class EqanunSchedulerConfigTest {

    @Test
    void schedulesDailySyncAtTwentyOneThirtyFiveInBaku() {
        EqanunSchedulerConfig config = new EqanunSchedulerConfig();
        var job = config.eqanunLawSyncJobDetail();
        CronTrigger trigger =
                (CronTrigger) config.eqanunLawSyncTrigger(job, "0 35 21 * * ?", "Asia/Baku");

        assertThat(trigger.getCronExpression()).isEqualTo("0 35 21 * * ?");
        assertThat(trigger.getTimeZone().toZoneId()).isEqualTo(ZoneId.of("Asia/Baku"));

        Date next = trigger.getFireTimeAfter(trigger.getStartTime());
        ZonedDateTime nextInBaku = next.toInstant().atZone(ZoneId.of("Asia/Baku"));
        assertThat(nextInBaku.getHour()).isEqualTo(21);
        assertThat(nextInBaku.getMinute()).isEqualTo(35);
        assertThat(nextInBaku.getSecond()).isZero();
    }
}
