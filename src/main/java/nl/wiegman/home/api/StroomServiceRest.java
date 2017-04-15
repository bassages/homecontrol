package nl.wiegman.home.api;

import nl.wiegman.home.model.OpgenomenVermogen;
import nl.wiegman.home.api.dto.VerbruikOpDag;
import nl.wiegman.home.api.dto.VerbruikPerMaandInJaar;
import nl.wiegman.home.api.dto.VerbruikPerUurOpDag;
import nl.wiegman.home.model.Energiesoort;
import nl.wiegman.home.service.VerbruikService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stroom")
public class StroomServiceRest {

    private final VerbruikService verbruikService;

    @Autowired
    public StroomServiceRest(VerbruikService verbruikService) {
        this.verbruikService = verbruikService;
    }

    @GetMapping(path = "verbruik-per-maand-in-jaar/{jaar}")
    public List<VerbruikPerMaandInJaar> getVerbruikPerMaandInJaar(@PathVariable("jaar") int jaar) {
        return verbruikService.getVerbruikPerMaandInJaar(Energiesoort.STROOM, jaar);
    }

    @GetMapping(path = "verbruik-per-dag/{van}/{totEnMet}")
    public List<VerbruikOpDag> getVerbruikPerDag(@PathVariable("van") long van, @PathVariable("totEnMet") long totEnMet) {
        return verbruikService.getVerbruikPerDag(Energiesoort.STROOM, van, totEnMet);
    }

    @GetMapping(path = "verbruik-per-uur-op-dag/{dag}")
    public List<VerbruikPerUurOpDag> getVerbruikPerUurOpDag(@PathVariable("dag") long dag) {
        return verbruikService.getVerbruikPerUurOpDag(Energiesoort.STROOM, dag);
    }

    @GetMapping(path = "opgenomen-vermogen-historie/{from}/{to}")
    public List<OpgenomenVermogen> getOpgenomenVermogenHistory(@PathVariable("from") long from, @PathVariable("to") long to, @RequestParam("subPeriodLength") long subPeriodLength) {
        return verbruikService.getOpgenomenStroomVermogenHistory(from, to, subPeriodLength);
    }
}