package uk.ac.ic.doc.blocc.example;

import java.nio.file.Paths;
import java.util.Random;
import uk.ac.ic.doc.blocc.BloccContractGateway;
import uk.ac.ic.doc.blocc.TemperatureHumidityReadingContractApp;
import uk.ac.ic.doc.blocc.sensor.TemperatureHumiditySensor;

public class TemperatureHumidityExample {
  private static class MockedSensor implements TemperatureHumiditySensor {

    private final float mean;

    private MockedSensor(float mean) {
      this.mean = mean;
    }

    @Override
    public float getTemperature() {
      return (float) (new Random().nextGaussian() * 2 + mean);
    }

    @Override
    public float getRelativeHumidity() {
      return new Random().nextFloat();
    }
  }

  public static void main(String[] args) throws Exception {

    int containerNum = Integer.parseInt(System.getenv().getOrDefault("FABRIC_CONTAINER_NUM", String.valueOf(1)));
    float mean = Float.parseFloat(System.getenv().getOrDefault("FABRIC_SENSOR_MEAN_TEMPERATURE", String.valueOf(20F)));
    int interval = Integer.parseInt(System.getenv().getOrDefault("FABRIC_SENSOR_INTERVAL", String.valueOf(3000)));
    int totalCount = Integer.parseInt(System.getenv().getOrDefault("FABRIC_SENSOR_NUM", String.valueOf(20)));

    // Create gateway
    BloccContractGateway gateway =
        new BloccContractGateway(
            System.getenv().getOrDefault("MSP_ID", String.format("Container%dMSP", containerNum)),
            System.getenv().getOrDefault("CHANNEL_NAME", String.format("channel%d", containerNum)),
            System.getenv().getOrDefault("CHAINCODE_NAME", "sensor_chaincode"),
            Paths.get(
                String.format("/home/tonywu/blocc/blocc-test-network/organizations/peerOrganizations/container%d.blocc.doc.ic.ac.uk", containerNum)),
            Paths.get(String.format("users/User1@container%d.blocc.doc.ic.ac.uk/msp/signcerts/User1@container%d.blocc.doc.ic.ac.uk-cert.pem", containerNum, containerNum)),
            Paths.get(String.format("users/User1@container%d.blocc.doc.ic.ac.uk/msp/keystore", containerNum)),
            Paths.get(String.format("peers/peer0.container%d.blocc.doc.ic.ac.uk/tls/ca.crt", containerNum)),
            String.format("localhost:%d051", containerNum),
            String.format("blocc-container%d", containerNum));

    TemperatureHumidityReadingContractApp app =
        new TemperatureHumidityReadingContractApp(gateway.getContract(), new MockedSensor(mean));

    // Invoke a transaction
    try {
      for (int count = 0; count < totalCount; count++) {
        Thread.sleep(interval);
        app.addReading();
      }
    } catch (Exception e) {
      gateway.close();
    }
  }
}
