package nl.homeserver.energie.meterstand;

import static java.time.Month.FEBRUARY;
import static nl.homeserver.util.TimeMachine.timeTravelTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UNKNOWN;
import static org.springframework.boot.actuate.health.Status.UP;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;

import nl.homeserver.energie.meterstand.Meterstand;
import nl.homeserver.energie.meterstand.MeterstandBuilder;
import nl.homeserver.energie.meterstand.MeterstandHealth;
import nl.homeserver.energie.meterstand.MeterstandService;

@RunWith(MockitoJUnitRunner.class)
public class MeterstandHealthTest {

    @InjectMocks
    private MeterstandHealth meterstandHealth;

    @Mock
    private MeterstandService meterstandService;
    @Mock
    private Clock clock;

    @Test
    public void givenNoMeterstandExistsWhenGetHealthThenHealthIsUnknown() {
        timeTravelTo(clock, LocalDate.of(2017, FEBRUARY, 5).atTime(10, 0, 0));

        when(meterstandService.getMostRecent()).thenReturn(null);

        final Health health = meterstandHealth.health();

        assertThat(health.getDetails().get("message")).isEqualTo("No Meterstand registered yet");
        assertThat(health.getStatus()).isEqualTo(UNKNOWN);
    }

    @Test
    public void givenMostRecentMeterstandIsFiveMinutesOldWhenGetHealthThenHealthIsUp() {
        final LocalDateTime fixedLocalDateTime = LocalDate.of(2017, FEBRUARY, 5).atTime(10, 5);
        timeTravelTo(clock, fixedLocalDateTime);

        final Meterstand meterstandThatIsFiveMinutesOld = MeterstandBuilder.aMeterstand().withDateTime(fixedLocalDateTime.minusMinutes(5)).build();
        when(meterstandService.getMostRecent()).thenReturn(meterstandThatIsFiveMinutesOld);

        final Health health = meterstandHealth.health();

        assertThat(health.getDetails().get("message")).isEqualTo("Most recent valid Meterstand was saved at 2017-02-05T10:00:00");
        assertThat(health.getStatus()).isEqualTo(UP);
    }

    @Test
    public void givenMostRecentMeterstandIsMoreThenFiveMinutesOldWhenGetHealthThenHealthIsDown() {
        final LocalDateTime fixedLocalDateTime = LocalDate.of(2017, FEBRUARY, 5).atTime(10, 5);
        timeTravelTo(clock, fixedLocalDateTime);

        final Meterstand meterstandThatIsFiveMinutesOld = MeterstandBuilder.aMeterstand().withDateTime(fixedLocalDateTime.minusMinutes(5).minusSeconds(1)).build();
        when(meterstandService.getMostRecent()).thenReturn(meterstandThatIsFiveMinutesOld);

        final Health health = meterstandHealth.health();

        assertThat(health.getDetails().get("message")).isEqualTo("Most recent valid Meterstand was saved at 2017-02-05T09:59:59. Which is more than 5 minutes ago.");
        assertThat(health.getStatus()).isEqualTo(DOWN);
    }
}