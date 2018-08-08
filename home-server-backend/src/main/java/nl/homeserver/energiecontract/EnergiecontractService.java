package nl.homeserver.energiecontract;

import nl.homeserver.DateTimePeriod;
import nl.homeserver.cache.CacheService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.LocalDateTime.now;

@Service
public class EnergiecontractService {

    private static final String CACHE_NAME_ENERGIECONTRACTEN_IN_PERIOD = "energiecontractenInPeriod";

    private final EnergiecontractToDateRecalculator energiecontractToDateRecalculator;
    private final EnergiecontractRepository energiecontractRepository;
    private final CacheService cacheService;
    private final Clock clock;

    public EnergiecontractService(final EnergiecontractToDateRecalculator energiecontractToDateRecalculator,
                                  final EnergiecontractRepository energiecontractRepository,
                                  final CacheService cacheService,
                                  final Clock clock) {

        this.energiecontractToDateRecalculator = energiecontractToDateRecalculator;
        this.energiecontractRepository = energiecontractRepository;
        this.cacheService = cacheService;
        this.clock = clock;
    }

    public List<Energiecontract> getAll() {
        return energiecontractRepository.findAll();
    }

    public Energiecontract getCurrent() {
        final LocalDateTime now = now(clock);
        return energiecontractRepository.findFirstByValidFromLessThanEqualOrderByValidFromDesc(now.toLocalDate());
    }

    public Energiecontract save(final Energiecontract energiecontract) {
        final Energiecontract savedEnergieContract = energiecontractRepository.save(energiecontract);
        energiecontractToDateRecalculator.recalculate();
        cacheService.clearAll();
        return savedEnergieContract;
    }

    public void delete(final long id) {
        energiecontractRepository.deleteById(id);
        energiecontractToDateRecalculator.recalculate();
        cacheService.clearAll();
    }

    @Cacheable(cacheNames = CACHE_NAME_ENERGIECONTRACTEN_IN_PERIOD)
    public List<Energiecontract> findAllInInPeriod(final DateTimePeriod period) {
        return energiecontractRepository.findValidInPeriod(period.getFromDateTime().toLocalDate(), period.getToDateTime().toLocalDate());
    }

    public Energiecontract getById(final long id) {
        return energiecontractRepository.getOne(id);
    }
}
