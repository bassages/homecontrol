package nl.homeserver.klimaat;

import static java.time.LocalDate.now;
import static java.time.Month.JANUARY;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static nl.homeserver.DatePeriod.aPeriodWithToDate;
import static nl.homeserver.klimaat.SensorType.LUCHTVOCHTIGHEID;
import static nl.homeserver.klimaat.SensorType.TEMPERATUUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import nl.homeserver.ResourceNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class KlimaatControllerTest {

    @InjectMocks
    private KlimaatController klimaatController;

    @Mock
    private KlimaatService klimaatService;

    @Mock
    private KlimaatSensor klimaatSensor;

    private static final String EXISTING_SENSOR_CODE = "LIVINGROOM";
    private static final String NOT_EXISTING_SENSOR_CODE = "DOES_NOT_EXISTS";
    private static final String EXPECTED_MESSAGE_WHEN_KLIMAATSENSOR_DOES_NOT_EXIST = "KlimaatSensor [" + NOT_EXISTING_SENSOR_CODE + "] does not exist";

    @Test
    public void whenAddThenKlimaatSensorSetAndDelegatedToService() {
        final String sensorCode = "LIVINGROOM";
        when(klimaatService.getKlimaatSensorByCode(sensorCode)).thenReturn(Optional.of(klimaatSensor));

        final Klimaat klimaat = new Klimaat();

        klimaatController.add(sensorCode, klimaat);

        assertThat(klimaat.getKlimaatSensor()).isSameAs(klimaatSensor);
        verify(klimaatService).add(klimaat);
    }

    @Test
    public void givenNoKlimaatSensorExistsWithGivenCodeWhenAddThenExceptionIsThrown() {
        when(klimaatService.getKlimaatSensorByCode(NOT_EXISTING_SENSOR_CODE)).thenReturn(empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> klimaatController.add(NOT_EXISTING_SENSOR_CODE, new Klimaat()))
                .withMessage(EXPECTED_MESSAGE_WHEN_KLIMAATSENSOR_DOES_NOT_EXIST);
    }

    @Test
    public void whenGetMostRecentThenDelegatedToService() {
        when(klimaatService.getKlimaatSensorByCode(EXISTING_SENSOR_CODE)).thenReturn(Optional.of(klimaatSensor));

        final RealtimeKlimaat realtimeKlimaat = mock(RealtimeKlimaat.class);
        when(klimaatService.getMostRecent(EXISTING_SENSOR_CODE)).thenReturn(realtimeKlimaat);

        assertThat(klimaatController.getMostRecent(EXISTING_SENSOR_CODE)).isSameAs(realtimeKlimaat);
    }

    @Test
    public void givenNoKlimaatSensorExistsWithGivenCodeWhenGetMostRecentThenExceptionIsThrown() {
        when(klimaatService.getKlimaatSensorByCode(NOT_EXISTING_SENSOR_CODE)).thenReturn(empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> klimaatController.getMostRecent(NOT_EXISTING_SENSOR_CODE))
                .withMessage(EXPECTED_MESSAGE_WHEN_KLIMAATSENSOR_DOES_NOT_EXIST);
    }

    @Test
    public void whenGetHighestThenDelegatedToService() {
        when(klimaatService.getKlimaatSensorByCode(EXISTING_SENSOR_CODE)).thenReturn(Optional.of(klimaatSensor));

        final List<Klimaat> higestKlimaats = asList(mock(Klimaat.class), mock(Klimaat.class));

        final SensorType sensorType = TEMPERATUUR;
        final LocalDate from = LocalDate.of(2018, JANUARY, 12);
        final LocalDate to = LocalDate.of(2018, JANUARY, 27);
        final int limit = 20;

        when(klimaatService.getHighest(eq(EXISTING_SENSOR_CODE), eq(sensorType), eq(aPeriodWithToDate(from, to)), eq(limit))).thenReturn(higestKlimaats);

        assertThat(klimaatController.getHighest(EXISTING_SENSOR_CODE, sensorType.name(), from, to, limit)).isSameAs(higestKlimaats);
    }

    @Test
    public void givenNoKlimaatSensorExistsWithGivenCodeWhenGetHighestThenExceptionIsThrown() {
        when(klimaatService.getKlimaatSensorByCode(NOT_EXISTING_SENSOR_CODE)).thenReturn(empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> klimaatController.getHighest(NOT_EXISTING_SENSOR_CODE, TEMPERATUUR.name(), now(), now(), 1234))
                .withMessage(EXPECTED_MESSAGE_WHEN_KLIMAATSENSOR_DOES_NOT_EXIST);
    }

    @Test
    public void whenGetLowestThenDelegatedToService() {
        when(klimaatService.getKlimaatSensorByCode(EXISTING_SENSOR_CODE)).thenReturn(Optional.of(klimaatSensor));

        final List<Klimaat> higestKlimaats = asList(mock(Klimaat.class), mock(Klimaat.class));

        final SensorType sensorType = TEMPERATUUR;
        final LocalDate from = LocalDate.of(2018, JANUARY, 12);
        final LocalDate to = LocalDate.of(2018, JANUARY, 27);
        final int limit = 20;

        when(klimaatService.getLowest(eq(EXISTING_SENSOR_CODE), eq(sensorType), eq(aPeriodWithToDate(from, to)), eq(limit))).thenReturn(higestKlimaats);

        assertThat(klimaatController.getLowest(EXISTING_SENSOR_CODE, sensorType.name(), from , to, limit)).isSameAs(higestKlimaats);
    }

    @Test
    public void givenNoKlimaatSensorExistsWithGivenCodeWhenGetLowestThenExceptionIsThrown() {
        when(klimaatService.getKlimaatSensorByCode(NOT_EXISTING_SENSOR_CODE)).thenReturn(empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> klimaatController.getLowest(NOT_EXISTING_SENSOR_CODE, TEMPERATUUR.name(), now(), now(), 1234))
                .withMessage(EXPECTED_MESSAGE_WHEN_KLIMAATSENSOR_DOES_NOT_EXIST);
    }

    @Test
    public void whenGetAveragePerMonthThenDelegatedToService() {
        final SensorType sensorType = LUCHTVOCHTIGHEID;
        final int[] years = new int[] {2017, 2018};

        when(klimaatService.getKlimaatSensorByCode(EXISTING_SENSOR_CODE)).thenReturn(Optional.of(klimaatSensor));

        final GemiddeldeKlimaatPerMaand month1 = mock(GemiddeldeKlimaatPerMaand.class);
        final GemiddeldeKlimaatPerMaand month2 = mock(GemiddeldeKlimaatPerMaand.class);
        final List<GemiddeldeKlimaatPerMaand> year = asList(month1, month2);

        final List<List<GemiddeldeKlimaatPerMaand>> monthsInYears = singletonList(year);
        when(klimaatService.getAveragePerMonthInYears(EXISTING_SENSOR_CODE, sensorType, years)).thenReturn(monthsInYears);

        assertThat(klimaatController.getAverage(EXISTING_SENSOR_CODE, sensorType.name(), years)).isSameAs(monthsInYears);
    }

    @Test
    public void whenFindAllInPeriodThenDelegatedToSerice() {
        final LocalDate from = LocalDate.of(2018, JANUARY, 12);
        final LocalDate to = LocalDate.of(2018, JANUARY, 27);

        when(klimaatService.getKlimaatSensorByCode(EXISTING_SENSOR_CODE)).thenReturn(Optional.of(klimaatSensor));

        final List<Klimaat> allInPeriod = asList(mock(Klimaat.class), mock(Klimaat.class));

        when(klimaatService.getInPeriod(eq(EXISTING_SENSOR_CODE), eq(aPeriodWithToDate(from, to)))).thenReturn(allInPeriod);

        assertThat(klimaatController.findAllInPeriod(EXISTING_SENSOR_CODE, from, to)).isSameAs(allInPeriod);
    }

    @Test
    public void givenNoKlimaatSensorExistsWithGivenCodeWhenFindAllInPeriodThenExceptionIsThrown() {
        when(klimaatService.getKlimaatSensorByCode(NOT_EXISTING_SENSOR_CODE)).thenReturn(empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> klimaatController.findAllInPeriod(NOT_EXISTING_SENSOR_CODE, now(), now()))
                .withMessage(EXPECTED_MESSAGE_WHEN_KLIMAATSENSOR_DOES_NOT_EXIST);
    }

    @Test
    public void whenGetAllKlimaatSensorsThenDelegatedToService() {
        final List<KlimaatSensor> allKlimaatSensors = singletonList(klimaatSensor);
        when(klimaatService.getAllKlimaatSensors()).thenReturn(allKlimaatSensors);

        assertThat(klimaatController.getAllKlimaatSensors()).isEqualTo(allKlimaatSensors);
    }

    @Test
    public void givenExistingKlimaatSensorCodeWhenUpdateThenUpdatedByServiceAndReturned() {
        final KlimaatSensor existingKlimaatSensor = mock(KlimaatSensor.class);

        final String omschrijving = "The new Description";
        final KlimaatSensorDto klimaatSensorDto = new KlimaatSensorDto();
        klimaatSensorDto.setOmschrijving(omschrijving);

        when(klimaatService.getKlimaatSensorByCode(EXISTING_SENSOR_CODE)).thenReturn(Optional.of(existingKlimaatSensor));

        final KlimaatSensor updatedKlimaatSensor = mock(KlimaatSensor.class);
        when(klimaatService.update(existingKlimaatSensor)).thenReturn(updatedKlimaatSensor);

        assertThat(klimaatController.update(EXISTING_SENSOR_CODE, klimaatSensorDto)).isSameAs(updatedKlimaatSensor);

        verify(existingKlimaatSensor).setOmschrijving(omschrijving);
    }

    @Test
    public void givenNonExistingKlimaatSensorCodeWhenUpdateThenException() {
        when(klimaatService.getKlimaatSensorByCode(NOT_EXISTING_SENSOR_CODE)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(() -> klimaatController.update(NOT_EXISTING_SENSOR_CODE, mock(KlimaatSensorDto.class)))
            .withMessage("KlimaatSensor [DOES_NOT_EXISTS] does not exist");
    }

    @Test
    public void givenExistingKlimaatSensorCodeWhenDeleteThenDeletedByService() {
        when(klimaatSensor.getCode()).thenReturn(EXISTING_SENSOR_CODE);
        when(klimaatService.getKlimaatSensorByCode(EXISTING_SENSOR_CODE)).thenReturn(Optional.of(klimaatSensor));

        klimaatController.delete(klimaatSensor.getCode());

        verify(klimaatService).delete(klimaatSensor);
    }

    @Test
    public void givenNonExistingKlimaatSensorCodeWheDeleteThenException() {
        when(klimaatSensor.getCode()).thenReturn(NOT_EXISTING_SENSOR_CODE);
        when(klimaatService.getKlimaatSensorByCode(NOT_EXISTING_SENSOR_CODE)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> klimaatController.delete(klimaatSensor.getCode()))
                .withMessage("KlimaatSensor [DOES_NOT_EXISTS] does not exist");
    }
}