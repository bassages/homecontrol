package nl.wiegman.home.repository;

import nl.wiegman.home.model.Klimaat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Transactional
public interface KlimaatRepos extends JpaRepository<Klimaat, Long> {

    // JPQL queries
    String MOST_RECENT = "SELECT k FROM Klimaat k WHERE k.datumtijd = (SELECT MAX(mostrecent.datumtijd) FROM Klimaat mostrecent)";

    // Native queries
    String PEAK_HIGH_TEMPERATURE_DATES = "SELECT datum FROM (SELECT date(datumtijd) AS datum, MAX(temperatuur) AS temperatuur FROM klimaat GROUP BY date(datumtijd) HAVING datum >= :van AND datum < :tot ORDER BY temperatuur DESC LIMIT :limit) datums";
    String FIRST_HIGHEST_TEMPERATURE_ON_DAY = "SELECT * FROM klimaat WHERE date(datumtijd) = :date ORDER BY temperatuur DESC, datumtijd ASC LIMIT 1";

    String PEAK_LOW_TEMPERATURE_DATES = "SELECT datum FROM (SELECT date(datumtijd) AS datum, MIN(temperatuur) AS temperatuur FROM klimaat GROUP BY date(datumtijd) HAVING datum >= :van AND datum < :tot ORDER BY temperatuur ASC LIMIT :limit) datums";
    String FIRST_LOWEST_TEMPERATURE_ON_DAY = "SELECT * FROM klimaat WHERE date(datumtijd) = :date ORDER BY temperatuur ASC, datumtijd ASC LIMIT 1";

    String PEAK_HIGH_HUMIDITY_DATES = "SELECT datum FROM (SELECT date(datumtijd) AS datum, MAX(luchtvochtigheid) AS luchtvochtigheid FROM klimaat GROUP BY date(datumtijd) HAVING luchtvochtigheid IS NOT NULL AND datum >= :van AND datum < :tot ORDER BY luchtvochtigheid DESC LIMIT :limit) datums";
    String FIRST_HIGHEST_HUMIDITY_ON_DAY = "SELECT * FROM klimaat WHERE luchtvochtigheid IS NOT NULL AND date(datumtijd) = :date ORDER BY luchtvochtigheid DESC, datumtijd ASC LIMIT 1";

    String PEAK_LOW_HUMIDITY_DATES = "SELECT datum FROM (SELECT date(datumtijd) AS datum, MIN(luchtvochtigheid) AS luchtvochtigheid FROM klimaat GROUP BY date(datumtijd) HAVING luchtvochtigheid IS NOT NULL AND datum >= :van AND datum < :tot ORDER BY luchtvochtigheid ASC LIMIT :limit) datums";
    String FIRST_LOWEST_HUMIDITY_ON_DAY = "SELECT * FROM klimaat WHERE luchtvochtigheid IS NOT NULL AND date(datumtijd) = :date ORDER BY luchtvochtigheid ASC, datumtijd ASC LIMIT 1";

    List<Klimaat> findByDatumtijdBetweenOrderByDatumtijd(@Param("van") Date van, @Param("tot") Date tot);

    @Query(value = MOST_RECENT)
    Klimaat getMostRecent();

    @Query(value = PEAK_HIGH_TEMPERATURE_DATES, nativeQuery = true)
    List<Date> getPeakHighTemperatureDates(@Param("van") Date van, @Param("tot") Date tot, @Param("limit") int limit);

    @Query(value = FIRST_HIGHEST_TEMPERATURE_ON_DAY, nativeQuery = true)
    Klimaat firstHighestTemperatureOnDay(@Param("date") Date day);

    @Query(value = PEAK_LOW_TEMPERATURE_DATES, nativeQuery = true)
    List<Date> getPeakLowTemperatureDates(@Param("van") Date van, @Param("tot") Date tot, @Param("limit") int limit);

    @Query(value = FIRST_LOWEST_TEMPERATURE_ON_DAY, nativeQuery = true)
    Klimaat firstLowestTemperatureOnDay(@Param("date") Date day);

    @Query(value = PEAK_HIGH_HUMIDITY_DATES, nativeQuery = true)
    List<Date> getPeakHighHumidityDates(@Param("van") Date van, @Param("tot") Date tot, @Param("limit") int limit);

    @Query(value = FIRST_HIGHEST_HUMIDITY_ON_DAY, nativeQuery = true)
    Klimaat firstHighestHumidityOnDay(@Param("date") Date day);

    @Query(value = PEAK_LOW_HUMIDITY_DATES, nativeQuery = true)
    List<Date> getPeakLowHumidityDates(@Param("van") Date van, @Param("tot") Date tot, @Param("limit") int limit);

    @Query(value = FIRST_LOWEST_HUMIDITY_ON_DAY, nativeQuery = true)
    Klimaat firstLowestHumidityOnDay(@Param("date") Date day);

    @Query(value = "SELECT avg(temperatuur) FROM Klimaat WHERE datumtijd >= :van AND datumtijd < :tot")
    BigDecimal getAverageTemperatuur(@Param("van") Date van, @Param("tot") Date tot);

    @Query(value = "SELECT avg(luchtvochtigheid) FROM Klimaat WHERE datumtijd >= :van AND datumtijd < :tot")
    BigDecimal getAverageLuchtvochtigheid(@Param("van") Date van, @Param("tot") Date tot);
}