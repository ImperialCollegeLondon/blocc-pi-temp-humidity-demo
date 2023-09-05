package uk.ac.ic.doc.blocc;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.SubmitException;
import uk.ac.ic.doc.blocc.clock.Clock;
import uk.ac.ic.doc.blocc.clock.SystemClock;
import uk.ac.ic.doc.blocc.sensor.TemperatureHumiditySensor;

public class TemperatureHumidityReadingContractApp {

  private final Contract contract;
  private final TemperatureHumiditySensor sensor;
  private final Clock clock;
  private static final Logger logger = LogManager.getLogger();

  public TemperatureHumidityReadingContractApp(
      Contract contract, TemperatureHumiditySensor sensor) {
    this.contract = contract;
    this.sensor = sensor;
    clock = new SystemClock();
  }

  public TemperatureHumidityReadingContractApp(
      Contract contract, TemperatureHumiditySensor sensor, Clock clock) {
    this.contract = contract;
    this.sensor = sensor;
    this.clock = clock;
  }

  public String addReading()
      throws EndorseException, CommitException, SubmitException, CommitStatusException {

    float temperature = sensor.getTemperature();
    float relativeHumidity = sensor.getRelativeHumidity();
    long timestamp = clock.now();

    logger.info("Adding a new reading at " + Instant.ofEpochSecond(timestamp).toString() +
                    ": temperature=" + temperature + ", relativeHumidity=" + relativeHumidity);

    // TODO: catch exceptions

    return new String(
        contract.submitTransaction(
            "addReading",
            String.valueOf(temperature),
            String.valueOf(relativeHumidity),
            String.valueOf(timestamp)));
  }
}
