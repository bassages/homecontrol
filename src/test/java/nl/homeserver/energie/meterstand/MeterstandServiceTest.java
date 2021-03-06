package nl.homeserver.energie.meterstand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.Month.JANUARY;
import static java.time.Month.MARCH;
import static nl.homeserver.DatePeriod.aPeriodWithToDate;
import static nl.homeserver.energie.meterstand.MeterstandBuilder.aMeterstand;
import static nl.homeserver.util.TimeMachine.timeTravelTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class MeterstandServiceTest {

    @InjectMocks
    MeterstandService meterstandService;

    @Mock
    MeterstandRepository meterstandRepository;
    @Mock
    Clock clock;
    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Test
    void whenSaveThenDelegatedToRepositoryAndEventSend() {
        // given
        final Meterstand meterstand = aMeterstand().build();
        when(meterstandRepository.save(meterstand)).thenReturn(meterstand);

        // when
        final Meterstand savedMeterstand = meterstandService.save(meterstand);

        // then
        assertThat(savedMeterstand).isSameAs(meterstand);
        verify(messagingTemplate).convertAndSend(MeterstandService.TOPIC, meterstand);
    }

    @Test
    void givenMeterstandAlreadySavedWhenGetMostRecentThenMostRecentReturned() {
        // given
        when(meterstandRepository.save(any(Meterstand.class))).then(returnsFirstArg());

        // when
        final Meterstand meterstand = aMeterstand().build();
        final Meterstand savedMeterstand = meterstandService.save(meterstand);

        // then
        assertThat(meterstandService.getMostRecent()).isEqualTo(savedMeterstand);
    }

    @Test
    void givenNoMeterstandSavedYetWhenGetMostRecentThenMostRecentIsRetrievedFromRepository() {
        // given
        final Meterstand meterstand = mock(Meterstand.class);
        when(meterstandRepository.getMostRecent()).thenReturn(meterstand);

        // when
        final Meterstand mostRecent = meterstandService.getMostRecent();

        // then
        assertThat(mostRecent).isEqualTo(meterstand);
    }

    @Test
    void whenGetOldestDelegatedToRepository() {
        // given
        final Meterstand oldestMeterstand = aMeterstand().build();

        // when
        when(meterstandRepository.getOldest()).thenReturn(oldestMeterstand);

        // then
        assertThat(meterstandService.getOldest()).isSameAs(oldestMeterstand);
    }

    @Test
    void whenGetOldestOfTodayThenReturned() {
        // given
        final LocalDate today = LocalDate.of(2017, JANUARY, 8);

        timeTravelTo(clock, today.atStartOfDay());

        final Meterstand oldestMeterstandElectricity = aMeterstand().withStroomTarief1(new BigDecimal("100.000"))
                                                              .withStroomTarief2(new BigDecimal("200.000"))
                                                              .build();

        when(meterstandRepository.getOldestInPeriod(today.atStartOfDay(), today.atStartOfDay().plusDays(1).minusNanos(1)))
                                 .thenReturn(oldestMeterstandElectricity);

        final Meterstand oldestMeterstandGas = aMeterstand().withGas(new BigDecimal("965.000")).build();
        when(meterstandRepository.getOldestInPeriod(today.atStartOfDay().plusHours(1), today.atStartOfDay().plusDays(1).plusHours(1).minusNanos(1)))
                                 .thenReturn(oldestMeterstandGas);

        // when
        final Meterstand oldestOfToday = meterstandService.getOldestOfToday();

        // then
        assertThat(oldestOfToday.getStroomTarief1()).isEqualTo(oldestMeterstandElectricity.getStroomTarief1());
        assertThat(oldestOfToday.getStroomTarief2()).isEqualTo(oldestMeterstandElectricity.getStroomTarief2());
        assertThat(oldestOfToday.getGas()).isEqualTo(oldestMeterstandGas.getGas());
    }

    @Test
    void givenNoMeterstandExistsInNextHourWhenGetOldestOfTodayThenGasNotOverWritten() {
        // given
        final LocalDate today = LocalDate.of(2017, JANUARY, 8);

        timeTravelTo(clock, today.atStartOfDay());

        final Meterstand oldestMeterstandElectricity = aMeterstand().withStroomTarief1(new BigDecimal("100.000"))
                                                              .withStroomTarief2(new BigDecimal("200.000"))
                                                              .withGas(new BigDecimal("999.000"))
                                                              .build();

        when(meterstandRepository.getOldestInPeriod(today.atStartOfDay(), today.atStartOfDay().plusDays(1).minusNanos(1)))
                                 .thenReturn(oldestMeterstandElectricity);
        when(meterstandRepository.getOldestInPeriod(today.atStartOfDay().plusHours(1), today.atStartOfDay().plusDays(1).plusHours(1).minusNanos(1)))
                                 .thenReturn(null);

        // when
        final Meterstand oldestOfToday = meterstandService.getOldestOfToday();

        // then
        assertThat(oldestOfToday.getStroomTarief1()).isEqualTo(oldestMeterstandElectricity.getStroomTarief1());
        assertThat(oldestOfToday.getStroomTarief2()).isEqualTo(oldestMeterstandElectricity.getStroomTarief2());
        assertThat(oldestOfToday.getGas()).isEqualTo(oldestMeterstandElectricity.getGas());
    }

    @Test
    void givenNoMeterstandExistsInPeriodWhenGetOldestOfTodayThenNullReturned() {
        // given
        final LocalDate today = LocalDate.of(2017, JANUARY, 8);
        timeTravelTo(clock, today.atStartOfDay());
        when(meterstandRepository.getOldestInPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
                                 .thenReturn(null);

        // when
        final Meterstand oldestOfToday = meterstandService.getOldestOfToday();

        // then
        assertThat(oldestOfToday).isNull();
        verify(meterstandRepository).getOldestInPeriod(today.atStartOfDay(), today.plusDays(1).atStartOfDay().minusNanos(1));
        verifyNoMoreInteractions(meterstandRepository);
    }

    @Test
    void whenGetPerDagForTodayThenNonCachedMeterstandReturned() {
        // given
        setCachedMeterstandService(null);

        final LocalDate today = LocalDate.of(2017, JANUARY, 13);

        timeTravelTo(clock, today.atStartOfDay());

        final Meterstand mostRecentMeterstandOfToday = mock(Meterstand.class);
        when(meterstandRepository.getMostRecentInPeriod(today.atStartOfDay(), today.plusDays(1).atStartOfDay().minusNanos(1)))
                .thenReturn(mostRecentMeterstandOfToday);

        // when
        final List<MeterstandOpDag> meterstandPerDag = meterstandService.getPerDag(aPeriodWithToDate(today, today.plusDays(1)));

        // then
        assertThat(meterstandPerDag).satisfiesExactly(meterstandOpDag -> {
            assertThat(meterstandOpDag.getDag()).isEqualTo(today);
            assertThat(meterstandOpDag.getMeterstand()).isSameAs(mostRecentMeterstandOfToday);
        });
    }

    @Test
    void whenGetPerDagForYesterdayThenCachedMeterstandReturned() {
        // given
        final MeterstandService cachedMeterstandService = mock(MeterstandService.class);
        setCachedMeterstandService(cachedMeterstandService);

        final LocalDate today = LocalDate.of(2017, JANUARY, 13);
        timeTravelTo(clock, today.atStartOfDay());

        final LocalDate yesterday = today.minusDays(1);

        final Meterstand mostRecentMeterstandOfYesterday = mock(Meterstand.class);
        when(cachedMeterstandService.getPotentiallyCachedMeestRecenteMeterstandOpDag(yesterday)).thenReturn(mostRecentMeterstandOfYesterday);

        // when
        final List<MeterstandOpDag> meterstandPerDag = meterstandService.getPerDag(aPeriodWithToDate(yesterday, yesterday.plusDays(1)));

        // then
        assertThat(meterstandPerDag).satisfiesExactly(meterstandOpDag -> {
            assertThat(meterstandOpDag.getDag()).isEqualTo(yesterday);
            assertThat(meterstandOpDag.getMeterstand()).isSameAs(mostRecentMeterstandOfYesterday);
        });
    }

    @Test
    void whenGetPerDagForFutureThenNullReturned() {
        // given
        final LocalDate today = LocalDate.of(2017, JANUARY, 13);
        timeTravelTo(clock, today.atStartOfDay());

        final LocalDate tomorrow = today.plusDays(1);

        // when
        final List<MeterstandOpDag> meterstandPerDag = meterstandService.getPerDag(aPeriodWithToDate(tomorrow, tomorrow.plusDays(1)));

        // then
        assertThat(meterstandPerDag).satisfiesExactly(meterstandOpDag -> {
            assertThat(meterstandOpDag.getDag()).isEqualTo(tomorrow);
            assertThat(meterstandOpDag.getMeterstand()).isNull();
        });
    }

    @Test
    void whenGetPotentiallyCachedMeestRecenteMeterstandOpDagThenDelegatedToRepository() {
        // given
        final LocalDate day = LocalDate.of(2016, MARCH, 12);

        final Meterstand meterstand = mock(Meterstand.class);
        when(meterstandRepository.getMostRecentInPeriod(day.atStartOfDay(), day.plusDays(1).atStartOfDay().minusNanos(1))).thenReturn(meterstand);

        // when
        final Meterstand potentiallyCachedMeestRecenteMeterstandOpDag = meterstandService.getPotentiallyCachedMeestRecenteMeterstandOpDag(day);

        // then
        assertThat(potentiallyCachedMeestRecenteMeterstandOpDag).isSameAs(meterstand);
    }

    private void setCachedMeterstandService(@Nullable final MeterstandService cachedMeterstandService) {
        setField(meterstandService, "meterstandServiceProxyWithEnabledCaching", cachedMeterstandService);
    }
}
