package nl.homeserver.energie.slimmemeter;

import nl.homeserver.energie.StroomTariefIndicator;
import nl.homeserver.energie.meterstand.Meterstand;
import nl.homeserver.energie.meterstand.MeterstandService;
import nl.homeserver.energie.opgenomenvermogen.OpgenomenVermogen;
import nl.homeserver.energie.opgenomenvermogen.OpgenomenVermogenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create",
                                  "cache.warmup.on-application-start:false" })
public class SlimmeMeterControllerIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .alwaysDo(print())
                .build();
    }

    @MockBean
    OpgenomenVermogenService opgenomenVermogenService;
    @MockBean
    MeterstandService meterstandService;

    @Captor
    ArgumentCaptor<Meterstand> meterstandCaptor;
    @Captor
    ArgumentCaptor<OpgenomenVermogen> opgenomenVermogenCaptor;

    @WithMockUser
    @Test
    void whenPostValidRequestToMeterstandEndpointThenMeterstandAndOpgenomenVermogenSaved() throws Exception {
        final String content = """
                {"datumtijd":"2018-05-03T13:14:15","stroomOpgenomenVermogenInWatt":640,"stroomTarief1":12.422,"stroomTarief2":26.241,"gas":664.242,"stroomTariefIndicator":2}
                """;

        mockMvc.perform(post("/api/slimmemeter").contentType(MediaType.APPLICATION_JSON).content(content))
               .andExpect(status().isCreated());

        verify(meterstandService).save(meterstandCaptor.capture());
        verify(opgenomenVermogenService).save(opgenomenVermogenCaptor.capture());

        final Meterstand savedMeterstand = meterstandCaptor.getValue();
        assertThat(savedMeterstand.getDateTime()).isEqualTo(LocalDateTime.of(2018, 5, 3, 13, 14, 15));
        assertThat(savedMeterstand.getStroomTariefIndicator()).isEqualTo(StroomTariefIndicator.NORMAAL);
        assertThat(savedMeterstand.getStroomTarief1()).isEqualTo(new BigDecimal("12.422"));
        assertThat(savedMeterstand.getStroomTarief2()).isEqualTo(new BigDecimal("26.241"));
        assertThat(savedMeterstand.getGas()).isEqualTo(new BigDecimal("664.242"));

        final OpgenomenVermogen savedOpgenomenVermogen = opgenomenVermogenCaptor.getValue();
        assertThat(savedOpgenomenVermogen.getDatumtijd()).isEqualTo(LocalDateTime.of(2018, 5, 3, 13, 14, 15));
        assertThat(savedOpgenomenVermogen.getWatt()).isEqualTo(640);
        assertThat(savedOpgenomenVermogen.getTariefIndicator()).isEqualTo(StroomTariefIndicator.NORMAAL);
    }
}
