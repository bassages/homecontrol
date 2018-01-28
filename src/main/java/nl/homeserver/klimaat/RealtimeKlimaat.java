package nl.homeserver.klimaat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import nl.homeserver.Trend;

public class RealtimeKlimaat {

    private LocalDateTime datumtijd;
    private BigDecimal temperatuur;
    private BigDecimal luchtvochtigheid;
    private Trend temperatuurTrend;
    private Trend luchtvochtigheidTrend;

    public LocalDateTime getDatumtijd() {
        return datumtijd;
    }

    public void setDatumtijd(LocalDateTime datumtijd) {
        this.datumtijd = datumtijd;
    }

    public BigDecimal getTemperatuur() {
        return temperatuur;
    }

    public void setTemperatuur(BigDecimal temperatuur) {
        this.temperatuur = temperatuur;
    }

    public BigDecimal getLuchtvochtigheid() {
        return luchtvochtigheid;
    }

    public void setLuchtvochtigheid(BigDecimal luchtvochtigheid) {
        this.luchtvochtigheid = luchtvochtigheid;
    }

    public Trend getTemperatuurTrend() {
        return temperatuurTrend;
    }

    public void setTemperatuurTrend(Trend temperatuurTrend) {
        this.temperatuurTrend = temperatuurTrend;
    }

    public Trend getLuchtvochtigheidTrend() {
        return luchtvochtigheidTrend;
    }

    public void setLuchtvochtigheidTrend(Trend luchtvochtigheidTrend) {
        this.luchtvochtigheidTrend = luchtvochtigheidTrend;
    }
}