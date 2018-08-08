package nl.homeserver.energiecontract;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.notEqual;

@Service
public class EnergiecontractToDateRecalculator {

    private final EnergiecontractRepository energiecontractRepository;

    public EnergiecontractToDateRecalculator(final EnergiecontractRepository energiecontractRepository) {
        this.energiecontractRepository = energiecontractRepository;
    }

    public void recalculate() {
        final Sort sortByVanAsc = new Sort(Sort.Direction.ASC, "validFrom");

        final List<Energiecontract> energiecontracts = energiecontractRepository.findAll(sortByVanAsc);

        Energiecontract previousEnergiecontract = null;
        for (int i = 0; i < energiecontracts.size(); i++) {
            final Energiecontract currentEnergiecontract = energiecontracts.get(i);

            if (previousEnergiecontract != null) {
                final LocalDate validTo = currentEnergiecontract.getValidFrom();
                if (notEqual(previousEnergiecontract.getValidTo(), validTo)) {
                    previousEnergiecontract.setValidTo(validTo);
                    energiecontractRepository.save(previousEnergiecontract);
                }
            }

            if (i == (energiecontracts.size() - 1) && currentEnergiecontract.getValidTo() != null) {
                currentEnergiecontract.setValidTo(null);
                energiecontractRepository.save(currentEnergiecontract);
            }
            previousEnergiecontract = currentEnergiecontract;
        }
    }
}
