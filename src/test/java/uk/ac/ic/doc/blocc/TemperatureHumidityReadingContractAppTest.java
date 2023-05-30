package uk.ac.ic.doc.blocc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.SubmitException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.ic.doc.blocc.clock.Clock;

class TemperatureHumidityReadingContractAppTest {

  private final Contract contract = mock(Contract.class);
  private final TemperatureHumiditySensor sensor = mock(TemperatureHumiditySensor.class);
  private final AdjustableClock adjustableClock = new AdjustableClock(20L);
  private final TemperatureHumidityReadingContractApp app =
      new TemperatureHumidityReadingContractApp(contract, sensor, adjustableClock);

  @Nested
  class InvokeAddReadingTransaction {

    @Test
    public void addsNewReadingToLedger()
        throws EndorseException, CommitException, SubmitException, CommitStatusException {
      when(sensor.getTemperature()).thenReturn(20f);
      when(sensor.getRelativeHumidity()).thenReturn(0.5f);
      when(contract.submitTransaction(
              "addReading", String.valueOf(20f), String.valueOf(0.5f), String.valueOf(20L)))
          .thenReturn("{\"temperature\":20,\"relativeHumidity\":0.5,\"timestamp\":20}".getBytes());

      String result = app.addReading();

      verify(contract, times(1))
          .submitTransaction(
              "addReading", String.valueOf(20f), String.valueOf(0.5f), String.valueOf(20L));

      assertThat(result)
          .isEqualTo("{\"temperature\":20,\"relativeHumidity\":0.5,\"timestamp\":20}");
    }

    @Test
    public void throwsWhenAddingDuplicatedReadingToLedger()
        throws EndorseException, CommitException, SubmitException, CommitStatusException {
      // Add a reading
      addsNewReadingToLedger();

      // Replay the reading
      when(contract.submitTransaction(
              "addReading", String.valueOf(20f), String.valueOf(0.5f), String.valueOf(20L)))
          .thenThrow(EndorseException.class);
      Throwable thrown = catchThrowable(app::addReading);

      verify(contract, times(2))
          .submitTransaction(
              "addReading", String.valueOf(20f), String.valueOf(0.5f), String.valueOf(20L));

      assertThat(thrown).isInstanceOf(EndorseException.class);

      // Adding a new reading
      when(sensor.getTemperature()).thenReturn(40f);
      when(sensor.getRelativeHumidity()).thenReturn(0.2f);
      adjustableClock.plusSeconds(3);
      when(contract.submitTransaction(
              "addReading", String.valueOf(40f), String.valueOf(0.2f), String.valueOf(23L)))
          .thenReturn("{\"temperature\":40,\"relativeHumidity\":0.2,\"timestamp\":23}".getBytes());

      String result = app.addReading();

      verify(contract, times(1))
          .submitTransaction(
              "addReading", String.valueOf(40f), String.valueOf(0.2f), String.valueOf(23L));
      assertThat(result)
          .isEqualTo("{\"temperature\":40,\"relativeHumidity\":0.2,\"timestamp\":23}");
    }
  }

  private static class AdjustableClock implements Clock {

    private Instant time;

    public AdjustableClock(long seconds) {
      time = Instant.ofEpochSecond(seconds);
    }

    @Override
    public long now() {
      return time.getEpochSecond();
    }

    public void plusSeconds(long seconds) {
      time = time.plusSeconds(seconds);
    }
  }
}
