package uk.ac.ic.doc.blocc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.SubmitException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TemperatureHumidityReadingContractAppTest {

  private final Contract contract = mock(Contract.class);
  private final TemperatureHumidityReadingContractApp app =
      new TemperatureHumidityReadingContractApp(contract);

  @Nested
  class InvokeAddReadingTransaction {

    @Test
    public void addsNewReadingToLedger()
        throws EndorseException, CommitException, SubmitException, CommitStatusException {
      when(contract.submitTransaction(
              "addReading", String.valueOf(20f), String.valueOf(0.5f), String.valueOf(21L)))
          .thenReturn("{\"temperature\":20,\"relativeHumidity\":0.5,\"timestamp\":21}".getBytes());
      String result = app.addReading(20f, 0.5f, 21L);

      verify(contract, times(1))
          .submitTransaction(
              "addReading", String.valueOf(20f), String.valueOf(0.5f), String.valueOf(21L));

      assertThat(result)
          .isEqualTo("{\"temperature\":20,\"relativeHumidity\":0.5,\"timestamp\":21}");
    }

    @Test
    public void throwsWhenAddingDuplicatedReadingToLedger()
        throws EndorseException, CommitException, SubmitException, CommitStatusException {
      addsNewReadingToLedger();

      when(contract.submitTransaction(
              "addReading", String.valueOf(20f), String.valueOf(0.5f), String.valueOf(21L)))
          .thenThrow(EndorseException.class);
      Throwable thrown = catchThrowable(() -> app.addReading(20f, 0.5f, 21L));

      verify(contract, times(2))
          .submitTransaction(
              "addReading", String.valueOf(20f), String.valueOf(0.5f), String.valueOf(21L));

      assertThat(thrown).isInstanceOf(EndorseException.class);
    }
  }
}
