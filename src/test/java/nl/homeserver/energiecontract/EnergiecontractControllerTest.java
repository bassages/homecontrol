package nl.homeserver.energiecontract;

import static java.time.Month.MARCH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnergiecontractControllerTest {

    @InjectMocks
    private EnergiecontractController energiecontractController;

    @Mock
    private EnergiecontractService energiecontractService;

    @Test
    public void givenANewEnergyContractWhenSaveThenDelegatedToService() {
        when(energiecontractService.save(any())).then(returnsFirstArg());
        Energiecontract energieContract = mock(Energiecontract.class);
        when(energieContract.getId()).thenReturn(null);

        Energiecontract savedEnergieContract = energiecontractController.save(energieContract);

        assertThat(savedEnergieContract).isSameAs(energieContract);
    }

    @Test
    public void givenAnExistingEnergyContractWhenSaveThenValidToSetAndSaveIsDelegatedToService() {
        when(energiecontractService.save(any())).then(returnsFirstArg());

        long id = 13451L;
        LocalDate validTo = LocalDate.of(2018, MARCH, 13);

        Energiecontract existingEnergiecontract = mock(Energiecontract.class);
        when(existingEnergiecontract.getId()).thenReturn(id);
        when(existingEnergiecontract.getValidTo()).thenReturn(validTo);

        Energiecontract energiecontractToUpdate = mock(Energiecontract.class);
        when(energiecontractToUpdate.getId()).thenReturn(id);

        when(energiecontractService.getById(id)).thenReturn(existingEnergiecontract);

        Energiecontract savedEnergieContract = energiecontractController.save(energiecontractToUpdate);

        assertThat(savedEnergieContract).isSameAs(energiecontractToUpdate);
        verify(energiecontractToUpdate).setValidTo(validTo);
    }

    @Test
    public void whenDeleteThenDelegatedToService() {
        long id = 1234L;

        energiecontractController.delete(id);

        verify(energiecontractService).delete(id);
    }

    @Test
    public void whenGetAllThenDelegatedToService() {
        List<Energiecontract> allEnergieContracts = asList(mock(Energiecontract.class), mock(Energiecontract.class));
        when(energiecontractService.getAll()).thenReturn(allEnergieContracts);

        assertThat(energiecontractController.getAll()).isSameAs(allEnergieContracts);
    }

    @Test
    public void whenGetCurrentlyValidEnergiecontractThenDelegatedToService() {
        Energiecontract currentlyValid = mock(Energiecontract.class);
        when(energiecontractService.getCurrent()).thenReturn(currentlyValid);

        assertThat(energiecontractController.getCurrentlyValid()).isSameAs(currentlyValid);
    }
}