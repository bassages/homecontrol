package nl.homeserver.energie;

import static java.math.BigDecimal.ROUND_HALF_UP;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public class VerbruikKostenOverzichten {

    private final Collection<VerbruikKostenOverzicht> all;

    public VerbruikKostenOverzichten(final Collection<VerbruikKostenOverzicht> all) {
        this.all = all;
    }

    public VerbruikKostenOverzicht averageToSingle() {
        final VerbruikKostenOverzicht verbruikKostenOverzicht = new VerbruikKostenOverzicht();
        verbruikKostenOverzicht.setStroomVerbruikDal(getAverage(all, VerbruikKostenOverzicht::getStroomVerbruikDal, 3));
        verbruikKostenOverzicht.setStroomVerbruikNormaal(getAverage(all, VerbruikKostenOverzicht::getStroomVerbruikNormaal, 3));
        verbruikKostenOverzicht.setGasVerbruik(getAverage(all, VerbruikKostenOverzicht::getGasVerbruik, 3));
        verbruikKostenOverzicht.setStroomKostenDal(getAverage(all, VerbruikKostenOverzicht::getStroomKostenDal, 2));
        verbruikKostenOverzicht.setStroomKostenNormaal(getAverage(all, VerbruikKostenOverzicht::getStroomKostenNormaal, 2));
        verbruikKostenOverzicht.setGasKosten(getAverage(all, VerbruikKostenOverzicht::getGasKosten, 2));
        return verbruikKostenOverzicht;
    }

    private BigDecimal getAverage(final Collection<VerbruikKostenOverzicht> verbruikKostenOverzichten,
                                  final Function<VerbruikKostenOverzicht, BigDecimal> attributeToAverageGetter,
                                  final int scale) {

        final BigDecimal sum = verbruikKostenOverzichten.stream()
                                                        .map(attributeToAverageGetter)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(new BigDecimal(verbruikKostenOverzichten.size()), ROUND_HALF_UP).setScale(scale, ROUND_HALF_UP);
    }
}
