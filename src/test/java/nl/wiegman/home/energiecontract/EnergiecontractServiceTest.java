package nl.wiegman.home.energiecontract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnergiecontractServiceTest {

    @Mock
    private EnergiecontractRepository energiecontractRepositoryMock;

    @InjectMocks
    private EnergiecontractService energiecontractService;

    @Test
    public void recalculateTotEnMetWithChanges() {
        Energiecontract k1 = new Energiecontract();
        k1.setVan(4L);

        Energiecontract k2 = new Energiecontract();
        k2.setVan(7L);

        Energiecontract k3 = new Energiecontract();
        k3.setVan(30L);

        Energiecontract k4 = new Energiecontract();
        k4.setVan(345L);

        when(energiecontractRepositoryMock.findAll(any(Sort.class))).thenReturn(Arrays.asList(k1, k2, k3, k4));

        energiecontractService.recalculateTotEnMet();

        assertThat(k1.getTotEnMet(), is(equalTo(6L)));
        assertThat(k2.getTotEnMet(), is(equalTo(29L)));
        assertThat(k3.getTotEnMet(), is(equalTo(344L)));
        assertThat(k4.getTotEnMet(), is(equalTo(EnergiecontractService.SINT_JUTTEMIS)));

        verify(energiecontractRepositoryMock, times(4)).save(any(Energiecontract.class));
    }

    @Test
    public void recalculateTotEnMetWithoutChanges() {
        Energiecontract k1 = new Energiecontract();
        k1.setVan(4L);
        k1.setTotEnMet(6L);

        Energiecontract k2 = new Energiecontract();
        k2.setVan(7L);
        k2.setTotEnMet(29L);

        Energiecontract k3 = new Energiecontract();
        k3.setVan(30L);
        k3.setTotEnMet(344L);

        Energiecontract k4 = new Energiecontract();
        k4.setVan(345L);
        k4.setTotEnMet(EnergiecontractService.SINT_JUTTEMIS);

        when(energiecontractRepositoryMock.findAll(any(Sort.class))).thenReturn(Arrays.asList(k1, k2, k3, k4));

        energiecontractService.recalculateTotEnMet();

        assertThat(k1.getTotEnMet(), is(equalTo(6L)));
        assertThat(k2.getTotEnMet(), is(equalTo(29L)));
        assertThat(k3.getTotEnMet(), is(equalTo(344L)));
        assertThat(k4.getTotEnMet(), is(equalTo(EnergiecontractService.SINT_JUTTEMIS)));

        verify(energiecontractRepositoryMock, never()).save(any(Energiecontract.class));
    }

}
