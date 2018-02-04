package nl.homeserver.klimaat;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static nl.homeserver.DateTimeUtil.toMillisSinceEpoch;
import static nl.homeserver.Trend.DOWN;
import static nl.homeserver.Trend.STABLE;
import static nl.homeserver.Trend.UNKNOWN;
import static nl.homeserver.Trend.UP;
import static org.apache.commons.collections4.CollectionUtils.size;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import nl.homeserver.Trend;

@Service
public class KlimaatSensorValueTrendService {

    private static final int MINIMUM_NUMBER_OF_ITEMS_TO_DETERMINE_TREND = 3;

    private static final Map<Integer, Trend> SIGNUM_OF_SLOPE_TO_TREND_MAPPING = new HashMap<>();

    public KlimaatSensorValueTrendService() {
        SIGNUM_OF_SLOPE_TO_TREND_MAPPING.put(-1, DOWN);
        SIGNUM_OF_SLOPE_TO_TREND_MAPPING.put(0, STABLE);
        SIGNUM_OF_SLOPE_TO_TREND_MAPPING.put(1, UP);
    }

    public Trend determineValueTrend(List<Klimaat> klimaats, Function<Klimaat, BigDecimal> sensorValueGetter) {
        List<Klimaat> validklimaats = getValidKlimaats(klimaats, sensorValueGetter);

        if (size(validklimaats) < MINIMUM_NUMBER_OF_ITEMS_TO_DETERMINE_TREND) {
            return UNKNOWN;
        }

        BigDecimal slopeOfSensorValue = calculateSlopeOfSensorValue(validklimaats, sensorValueGetter);
        return SIGNUM_OF_SLOPE_TO_TREND_MAPPING.get(slopeOfSensorValue.signum());
    }

    private List<Klimaat> getValidKlimaats(List<Klimaat> klimaats, Function<Klimaat, BigDecimal> sensorValueGetter) {
        return klimaats.stream()
                       .filter(klimaat -> nonNull(sensorValueGetter.apply(klimaat)))
                       .collect(toList());
    }

    private BigDecimal calculateSlopeOfSensorValue(List<Klimaat> klimaats, Function<Klimaat, BigDecimal> sensorValueGetter) {
        SimpleRegression simpleRegression = new SimpleRegression();
        klimaats.forEach(klimaat -> simpleRegression.addData(toMillisSinceEpoch(klimaat.getDatumtijd()), sensorValueGetter.apply(klimaat).doubleValue()));
        return BigDecimal.valueOf(simpleRegression.getSlope());
    }
}