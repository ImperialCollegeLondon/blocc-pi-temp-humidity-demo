package uk.ac.ic.doc.blocc;

import java.time.Instant;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.SubmitException;

public class TemperatureHumidityReadingContractApp {

  private final Contract contract;

  public TemperatureHumidityReadingContractApp(Contract contract) {
    this.contract = contract;
  }

  public String addReading(
      final float temperature, final float relativeHumidity, final long timestamp)
      throws EndorseException, CommitException, SubmitException, CommitStatusException {
    System.out.printf("Adding a new reading at %s\n", Instant.ofEpochSecond(timestamp));

    // TODO: catch exceptions

    return new String(
        contract.submitTransaction(
            "addReading",
            String.valueOf(temperature),
            String.valueOf(relativeHumidity),
            String.valueOf(timestamp)));
  }
}
