package nl.wiegman.home.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import nl.wiegman.home.model.Klimaat;
import nl.wiegman.home.model.KlimaatSensor;
import nl.wiegman.home.model.SensorType;
import nl.wiegman.home.realtime.UpdateEvent;
import nl.wiegman.home.repository.KlimaatRepos;
import nl.wiegman.home.repository.KlimaatSensorRepository;

@Service
public class KlimaatService {
    private static final Logger LOG = LoggerFactory.getLogger(KlimaatService.class);

    private static final String EVERY_15_MINUTES_PAST_THE_HOUR = "0 0/15 * * * ?";

    private static final int TEMPERATURE_SCALE = 2;
    private static final int HUMIDITY_SCALE = 1;

    private final Map<String, List<Klimaat>> receivedInLastQuarter = new ConcurrentHashMap<>();

    @Autowired
    KlimaatServiceCached klimaatServiceCached;

    @Autowired
    KlimaatRepos klimaatRepository;
    @Autowired
    KlimaatSensorRepository klimaatSensorRepository;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = EVERY_15_MINUTES_PAST_THE_HOUR)
    public void save() {
        LOG.debug("Running " + this.getClass().getSimpleName() + ".save()");

        Date now = new Date();
        now = DateUtils.truncate(now, Calendar.MINUTE);

        for (Map.Entry<String, List<Klimaat>> receivedItem : receivedInLastQuarter.entrySet()) {
            List<BigDecimal> validTemperaturesFromLastQuarter = getValidTemperaturesFromLastQuarter(receivedItem.getKey());
            List<BigDecimal> validHumiditiesFromLastQuarter = getValidHumiditiesFromLastQuarter(receivedItem.getKey());

            KlimaatSensor klimaatSensor = receivedItem.getValue().get(0).getKlimaatSensor();

            BigDecimal averageTemperature = getAverage(validTemperaturesFromLastQuarter);
            if (averageTemperature != null) {
                averageTemperature = averageTemperature.setScale(TEMPERATURE_SCALE, RoundingMode.CEILING);
            }

            BigDecimal averageHumidity = getAverage(validHumiditiesFromLastQuarter);
            if (averageHumidity != null) {
                averageHumidity = averageHumidity.setScale(HUMIDITY_SCALE, RoundingMode.CEILING);
            }

            if (averageTemperature != null || averageHumidity != null) {
                Klimaat klimaatToSave = new Klimaat();
                klimaatToSave.setDatumtijd(now);
                klimaatToSave.setTemperatuur(averageTemperature);
                klimaatToSave.setLuchtvochtigheid(averageHumidity);
                klimaatToSave.setKlimaatSensor(klimaatSensor);
                klimaatRepository.save(klimaatToSave);
            }
        }
        receivedInLastQuarter.clear();
    }

    public KlimaatSensor getKlimaatSensorByCode(String code) {
        return klimaatSensorRepository.findFirstByCode(code);
    }

    public List<Klimaat> getInPeriod(Date from, Date to) {
        if (to.before(new Date())) {
            return klimaatServiceCached.getInPeriod(from, to);
        } else {
            return klimaatRepository.findByDatumtijdBetweenOrderByDatumtijd(from, to);
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

    public Klimaat getMostRecent() {
        LOG.info("getMostRecent()");
        return klimaatRepository.getMostRecent();
    }

    public void add(Klimaat klimaat) {
        LOG.info("Recieved klimaat");
        if (!receivedInLastQuarter.containsKey(klimaat.getKlimaatSensor().getCode())) {
            receivedInLastQuarter.put(klimaat.getKlimaatSensor().getCode(), new ArrayList<>());
        }
        receivedInLastQuarter.get(klimaat.getKlimaatSensor().getCode()).add(klimaat);
        eventPublisher.publishEvent(new UpdateEvent(klimaat));
    }

    public List<BigDecimal> getValidHumiditiesFromLastQuarter(String sensorId) {
        return receivedInLastQuarter.get(sensorId).stream()
                .filter(klimaat -> klimaat.getLuchtvochtigheid() != null && !BigDecimal.ZERO.equals(klimaat.getLuchtvochtigheid()))
                .map(klimaat -> klimaat.getLuchtvochtigheid())
                .collect(Collectors.toList());
    }

    public List<Klimaat> getHighest(SensorType sensortype, Date from, Date to, int limit) {
        switch (sensortype) {
            case TEMPERATUUR:
                return getHighestTemperature(from, to, limit);
            case LUCHTVOCHTIGHEID:
                return getHighestHumidity(from, to, limit);
            default:
                return Collections.emptyList();
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

    private List<BigDecimal> getValidTemperaturesFromLastQuarter(String sensorId) {
        return receivedInLastQuarter.get(sensorId).stream()
                .filter(klimaat -> klimaat.getTemperatuur() != null && !BigDecimal.ZERO.equals(klimaat.getTemperatuur()))
                .map(klimaat -> klimaat.getTemperatuur())
                .collect(Collectors.toList());
    }

    private List<Klimaat> getLowestTemperature(Date from, Date to, int limit) {
        return klimaatRepository.getPeakLowTemperatureDates(from, to, limit)
                    .stream()
                    .map(date -> klimaatRepository.firstLowestTemperatureOnDay(date))
                    .collect(Collectors.toList());
    }

    private List<Klimaat> getLowestHumidity(Date from, Date to, int limit) {
        return klimaatRepository.getPeakLowHumidityDates(from, to, limit)
                    .stream()
                    .map(date -> klimaatRepository.firstLowestHumidityOnDay(date))
                    .collect(Collectors.toList());
    }

    private BigDecimal getAverage(List<BigDecimal> decimals) {
        BigDecimal average = null;
        if (!decimals.isEmpty()) {
            BigDecimal total = decimals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            average = total.divide(BigDecimal.valueOf(decimals.size()), RoundingMode.CEILING);
        }
        return average;
    }

    private List<Klimaat> getHighestTemperature(Date from, Date to, int limit) {
        return klimaatRepository.getPeakHighTemperatureDates(from, to, limit)
                .stream()
                .map(date -> klimaatRepository.firstHighestTemperatureOnDay(date))
                .collect(Collectors.toList());
    }

    private List<Klimaat> getHighestHumidity(Date from, Date to, int limit) {
        return klimaatRepository.getPeakHighHumidityDates(from, to, limit)
                .stream()
                .map(date -> klimaatRepository.firstHighestHumidityOnDay(date))
                .collect(Collectors.toList());
    }

}
