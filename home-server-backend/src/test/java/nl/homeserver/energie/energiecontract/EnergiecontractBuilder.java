package nl.homeserver.energie.energiecontract;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.annotation.Nullable;

@SuppressWarnings({ "FieldMayBeFinal", "WeakerAccess", "CanBeFinal" })
public class EnergiecontractBuilder {

    private BigDecimal gasPerKuub = ZERO;
    private BigDecimal stroomPerKwhNormaalTarief = ZERO;
    private BigDecimal stroomPerKwhDalTarief = ZERO;
    private LocalDate validFrom;
    private LocalDate validTo;

    public static EnergiecontractBuilder anEnergiecontract() {
        return new EnergiecontractBuilder();
    }

    public EnergiecontractBuilder withValidFrom(@Nullable final LocalDate validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    public EnergiecontractBuilder withValidTo(@Nullable final LocalDate validTo) {
        this.validTo = validTo;
        return this;
    }

    public Energiecontract build() {
        final Energiecontract energiecontract = new Energiecontract();
        energiecontract.setGasPerKuub(gasPerKuub);
        energiecontract.setStroomPerKwhNormaalTarief(stroomPerKwhNormaalTarief);
        energiecontract.setStroomPerKwhDalTarief(stroomPerKwhDalTarief);
        energiecontract.setValidFrom(validFrom);
        energiecontract.setValidTo(validTo);
        return energiecontract;
    }
}
