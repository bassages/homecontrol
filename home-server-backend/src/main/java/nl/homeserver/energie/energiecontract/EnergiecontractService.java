package nl.homeserver.energie.energiecontract;

import static java.time.LocalDateTime.now;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import nl.homeserver.DateTimePeriod;
import nl.homeserver.cache.CacheService;

@Service
@AllArgsConstructor
public class EnergiecontractService {

    private static final String CACHE_NAME_ENERGIECONTRACTEN_IN_PERIOD = "energiecontractenInPeriod";

    private final EnergiecontractToDateRecalculator energiecontractToDateRecalculator;
    private final EnergiecontractRepository energiecontractRepository;
    private final CacheService cacheService;
    private final Clock clock;

    List<Energiecontract> getAll() {
        return energiecontractRepository.findAll();
    }

    Energiecontract getCurrent() {
        final LocalDateTime now = now(clock);
        return energiecontractRepository.findFirstByValidFromLessThanEqualOrderByValidFromDesc(now.toLocalDate());
    }

    Energiecontract save(final Energiecontract energiecontract) {
        final Energiecontract savedEnergieContract = energiecontractRepository.save(energiecontract);
        energiecontractToDateRecalculator.recalculate();
        cacheService.clearAll();
        return savedEnergieContract;
    }

    void delete(final long id) {
        energiecontractRepository.deleteById(id);
        energiecontractToDateRecalculator.recalculate();
        cacheService.clearAll();
    }

    @Cacheable(cacheNames = CACHE_NAME_ENERGIECONTRACTEN_IN_PERIOD)
    public List<Energiecontract> findAllInInPeriod(final DateTimePeriod period) {
        return energiecontractRepository.findValidInPeriod(period.getFromDateTime().toLocalDate(), period.getToDateTime().toLocalDate());
    }

    Energiecontract getById(final long id) {
        return energiecontractRepository.getOne(id);
    }
}
