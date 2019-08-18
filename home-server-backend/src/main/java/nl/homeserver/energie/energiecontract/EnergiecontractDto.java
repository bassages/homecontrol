package nl.homeserver.energie.energiecontract;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EnergiecontractDto {

    private Long id;
    private LocalDate validFrom;
    private LocalDate validTo;
    private BigDecimal stroomPerKwhNormaalTarief;
    private BigDecimal stroomPerKwhDalTarief;
    private BigDecimal gasPerKuub;
    private String leverancier;
    private String remark;
}
