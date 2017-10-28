package nl.wiegman.home.klimaat;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class KlimaatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KlimaatService.class);

    private static final String REALTIME_KLIMAAT_TOPIC = "/topic/klimaat";

    private static final int NR_OF_MINUTES_TO_DETERMINE_TREND_FOR = 10;
    private static final int NR_OF_MINUTES_TO_SAVE_AVERAGE_KLIMAAT_FOR = 15;

    private static final String EVERY_15_MINUTES_PAST_THE_HOUR = "0 0/" + NR_OF_MINUTES_TO_SAVE_AVERAGE_KLIMAAT_FOR + " * * * ?";

    private static final int TEMPERATURE_SCALE = 2;
    private static final int HUMIDITY_SCALE = 1;

    private final Map<String, List<Klimaat>> recentlyReceivedKlimaatsPerKlimaatSensorCode = new ConcurrentHashMap<>();

    private final KlimaatServiceCached klimaatServiceCached;
    private final KlimaatRepos klimaatRepository;
    private final KlimaatSensorRepository klimaatSensorRepository;

    private final KlimaatSensorValueTrendService klimaatSensorValueTrendService;

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public KlimaatService(KlimaatServiceCached klimaatServiceCached, KlimaatRepos klimaatRepository, KlimaatSensorRepository klimaatSensorRepository,
            KlimaatSensorValueTrendService klimaatSensorValueTrendService, SimpMessagingTemplate messagingTemplate) {

        this.klimaatServiceCached = klimaatServiceCached;
        this.klimaatRepository = klimaatRepository;
        this.klimaatSensorRepository = klimaatSensorRepository;
        this.klimaatSensorValueTrendService = klimaatSensorValueTrendService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void createDefaultSensor() {
        if (isEmpty(klimaatSensorRepository.findAll())) {
            KlimaatSensor klimaatSensor = new KlimaatSensor();
            klimaatSensor.setCode("WOONKAMER");
            klimaatSensor.setOmschrijving("Huiskamer");
            klimaatSensorRepository.save(klimaatSensor);
        }
    }

    private void cleanUpRecentlyReceivedKlimaatsPerSensorCode() {
        int maxNrOfMinutes = IntStream.of(NR_OF_MINUTES_TO_DETERMINE_TREND_FOR, NR_OF_MINUTES_TO_SAVE_AVERAGE_KLIMAAT_FOR).max().getAsInt();
        Date cleanUpAllBefore = DateUtils.addMinutes(new Date(), -maxNrOfMinutes);
        LOGGER.info("cleanUpRecentlyReceivedKlimaats before {}", cleanUpAllBefore);
        recentlyReceivedKlimaatsPerKlimaatSensorCode.values().forEach(klimaats -> klimaats.removeIf(klimaat -> klimaat.getDatumtijd().before(cleanUpAllBefore)));
    }

    @Scheduled(cron = EVERY_15_MINUTES_PAST_THE_HOUR)
    public void save() {
        final Date referenceDate = DateUtils.truncate(new Date(), Calendar.MINUTE);
        recentlyReceivedKlimaatsPerKlimaatSensorCode
                .forEach((klimaatSensorCode, klimaats) -> this.saveKlimaatWithAveragedRecentSensorValues(referenceDate, klimaatSensorCode));
    }

    private List<Klimaat> getKlimaatsReceivedInLastNumberOfMinutes(String klimaatSensorCode, int nrOfMinutes) {
        Date from = DateUtils.addMinutes(new Date(), -nrOfMinutes);
        return recentlyReceivedKlimaatsPerKlimaatSensorCode.get(klimaatSensorCode).stream()
                .filter(klimaat -> klimaat.getDatumtijd().after(from))
                .collect(toList());
    }

    private void saveKlimaatWithAveragedRecentSensorValues(Date referenceDate, String klimaatSensorCode) {
        List<Klimaat> klimaatsReceivedInLastNumberOfMinutes = getKlimaatsReceivedInLastNumberOfMinutes(klimaatSensorCode,
                NR_OF_MINUTES_TO_SAVE_AVERAGE_KLIMAAT_FOR);

        List<BigDecimal> validTemperaturesFromLastQuarter = getValidTemperatures(klimaatsReceivedInLastNumberOfMinutes);
        List<BigDecimal> validHumiditiesFromLastQuarter = getValidHumidities(klimaatsReceivedInLastNumberOfMinutes);

        KlimaatSensor klimaatSensor = klimaatSensorRepository.findFirstByCode(klimaatSensorCode);

        BigDecimal averageTemperature = getAverage(validTemperaturesFromLastQuarter);
        if (averageTemperature != null) {
            averageTemperature = averageTemperature.setScale(TEMPERATURE_SCALE, HALF_UP);
        }

        BigDecimal averageHumidity = getAverage(validHumiditiesFromLastQuarter);
        if (averageHumidity != null) {
            averageHumidity = averageHumidity.setScale(HUMIDITY_SCALE, HALF_UP);
        }

        if (averageTemperature != null || averageHumidity != null) {
            Klimaat klimaatToSave = new Klimaat();
            klimaatToSave.setDatumtijd(referenceDate);
            klimaatToSave.setTemperatuur(averageTemperature);
            klimaatToSave.setLuchtvochtigheid(averageHumidity);
            klimaatToSave.setKlimaatSensor(klimaatSensor);
            klimaatRepository.save(klimaatToSave);
        }
    }

    public KlimaatSensor getKlimaatSensorByCode(String klimaatSensorCode) {
        return klimaatSensorRepository.findFirstByCode(klimaatSensorCode);
    }

    public List<Klimaat> getInPeriod(String klimaatSensorCode, Date from, Date to) {
        if (to.before(new Date())) {
            return klimaatServiceCached.getInPeriod(klimaatSensorCode, from, to);
        } else {
            return klimaatRepository.findByKlimaatSensorCodeAndDatumtijdBetweenOrderByDatumtijd(klimaatSensorCode, from, to);
        }
    }

    public BigDecimal getAverage(SensorType sensortype, Date from, Date to) {
        switch (sensortype) {
            case TEMPERATUUR:
                return klimaatRepository.getAverageTemperatuur(from, to);
            case LUCHTVOCHTIGHEID:
                return klimaatRepository.getAverageLuchtvochtigheid(from, to);
            default:
                return null;
        }
    }

    public RealtimeKlimaat getMostRecent(String klimaatSensorCode) {
        LOGGER.info("getMostRecent()");
        return getLast(recentlyReceivedKlimaatsPerKlimaatSensorCode.get(klimaatSensorCode))
                .map(this::mapToRealtimeKlimaat)
                .orElse(null);
    }

    private Optional<Klimaat> getLast(List<Klimaat> klimaats) {
        if (isNotEmpty(klimaats)) {
            return Optional.of(klimaats.get(klimaats.size() - 1));
        } else {
            return Optional.empty();
        }
    }

    public void add(Klimaat klimaat) {
        LOGGER.info("Received klimaat");
        recentlyReceivedKlimaatsPerKlimaatSensorCode.computeIfAbsent(klimaat.getKlimaatSensor().getCode(), klimaatSensorCode -> new ArrayList<>()).add(klimaat);
        publishEvent(klimaat);
        cleanUpRecentlyReceivedKlimaatsPerSensorCode();
    }

    public List<BigDecimal> getValidHumidities(List<Klimaat> klimaatList) {
        return klimaatList.stream()
                .filter(klimaat -> klimaat.getLuchtvochtigheid() != null && !ZERO.equals(klimaat.getLuchtvochtigheid()))
                .map(Klimaat::getLuchtvochtigheid)
                .collect(toList());
    }

    private List<BigDecimal> getValidTemperatures(List<Klimaat> klimaatList) {
        return klimaatList.stream()
                .filter(klimaat -> klimaat.getTemperatuur() != null && !ZERO.equals(klimaat.getTemperatuur()))
                .map(Klimaat::getTemperatuur)
                .collect(toList());
    }

    public List<Klimaat> getHighest(SensorType sensortype, Date from, Date to, int limit) {
        switch (sensortype) {
            case TEMPERATUUR:
                return getHighestTemperature(from, to, limit);
            case LUCHTVOCHTIGHEID:
                return getHighestHumidity(from, to, limit);
            default:
                return emptyList();
        }
    }

    public List<Klimaat> getLowest(SensorType sensortype, Date from, Date to, int limit) {
        switch (sensortype) {
            case TEMPERATUUR:
                return getLowestTemperature(from, to, limit);
            case LUCHTVOCHTIGHEID:
                return getLowestHumidity(from, to, limit);
            default:
                return null;
        }
    }

    private List<Klimaat> getLowestTemperature(Date from, Date to, int limit) {
        return klimaatRepository.getPeakLowTemperatureDates(from, to, limit)
                    .stream()
                    .map(klimaatRepository::firstLowestTemperatureOnDay)
                    .collect(toList());
    }

    private List<Klimaat> getLowestHumidity(Date from, Date to, int limit) {
        return klimaatRepository.getPeakLowHumidityDates(from, to, limit)
                    .stream()
                    .map(klimaatRepository::firstLowestHumidityOnDay)
                    .collect(toList());
    }

    private BigDecimal getAverage(List<BigDecimal> decimals) {
        BigDecimal average = null;
        if (!decimals.isEmpty()) {
            BigDecimal total = decimals.stream().reduce(ZERO, BigDecimal::add);
            average = total.divide(BigDecimal.valueOf(decimals.size()), HALF_UP);
        }
        return average;
    }

    private List<Klimaat> getHighestTemperature(Date from, Date to, int limit) {
        return klimaatRepository.getPeakHighTemperatureDates(from, to, limit)
                .stream()
                .map(klimaatRepository::firstHighestTemperatureOnDay)
                .collect(toList());
    }

    private List<Klimaat> getHighestHumidity(Date from, Date to, int limit) {
        return klimaatRepository.getPeakHighHumidityDates(from, to, limit)
                .stream()
                .map(klimaatRepository::firstHighestHumidityOnDay)
                .collect(toList());
    }

    private void publishEvent(Klimaat klimaat) {
        RealtimeKlimaat realtimeKlimaat = mapToRealtimeKlimaat(klimaat);
        messagingTemplate.convertAndSend(REALTIME_KLIMAAT_TOPIC, realtimeKlimaat);
    }

    private RealtimeKlimaat mapToRealtimeKlimaat(Klimaat klimaat) {
        RealtimeKlimaat realtimeKlimaat = new RealtimeKlimaat();
        realtimeKlimaat.setDatumtijd(klimaat.getDatumtijd());
        realtimeKlimaat.setLuchtvochtigheid(klimaat.getLuchtvochtigheid());
        realtimeKlimaat.setTemperatuur(klimaat.getTemperatuur());

        List<Klimaat> klimaatsToDetermineTrendFor = getKlimaatsReceivedInLastNumberOfMinutes(klimaat.getKlimaatSensor().getCode(),
                NR_OF_MINUTES_TO_DETERMINE_TREND_FOR);
        realtimeKlimaat.setTemperatuurTrend(klimaatSensorValueTrendService.determineValueTrend(klimaatsToDetermineTrendFor, Klimaat::getTemperatuur));
        realtimeKlimaat.setLuchtvochtigheidTrend(klimaatSensorValueTrendService.determineValueTrend(klimaatsToDetermineTrendFor, Klimaat::getLuchtvochtigheid));

        return realtimeKlimaat;
    }
}
