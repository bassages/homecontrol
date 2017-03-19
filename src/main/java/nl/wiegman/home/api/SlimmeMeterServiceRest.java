package nl.wiegman.home.api;

import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.wiegman.home.api.dto.Dsmr42ReadingDto;
import nl.wiegman.home.model.Meterstand;
import nl.wiegman.home.service.MeterstandService;

@RestController
@RequestMapping("/api/slimmemeter")
public class SlimmeMeterServiceRest {

    private static final Logger LOG = LoggerFactory.getLogger(SlimmeMeterServiceRest.class);

    private final MeterstandService meterstandService;
    private final ObjectMapper objectMapper;

    private static final int GAS_SCALE = 3;
    private static final int STROOM_SCALE = 3;

    @Autowired
    public SlimmeMeterServiceRest(MeterstandService meterstandService, ObjectMapper objectMapper) {
        this.meterstandService = meterstandService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void save(@RequestBody Dsmr42ReadingDto slimmeMeterBericht) {
        try {
            LOG.info(objectMapper.writeValueAsString(slimmeMeterBericht));
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to serialize recieved object", e);
        }
        meterstandService.save(mapToMeterStand(slimmeMeterBericht));
        meterstandService.save(mapToMeterStand(slimmeMeterBericht));
    }

    private Meterstand mapToMeterStand(Dsmr42ReadingDto slimmeMeterBericht) {
        Meterstand meterstand = new Meterstand();
        meterstand.setDatumtijd(slimmeMeterBericht.getDatumtijd());
        meterstand.setStroomOpgenomenVermogenInWatt(slimmeMeterBericht.getStroomOpgenomenVermogenInWatt());
        meterstand.setGas(slimmeMeterBericht.getGas().setScale(GAS_SCALE, RoundingMode.CEILING));
        meterstand.setStroomTarief1(slimmeMeterBericht.getStroomTarief1().setScale(STROOM_SCALE, RoundingMode.CEILING));
        meterstand.setStroomTarief2(slimmeMeterBericht.getStroomTarief2().setScale(STROOM_SCALE, RoundingMode.CEILING));
        return meterstand;
    }
}