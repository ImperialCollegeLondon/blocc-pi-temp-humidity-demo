package uk.ac.ic.doc.blocc.example;

import java.nio.file.Paths;
import java.util.Random;
import uk.ac.ic.doc.blocc.BloccContractGateway;
import uk.ac.ic.doc.blocc.TemperatureHumidityReadingContractApp;
import uk.ac.ic.doc.blocc.TemperatureHumiditySensor;

public class TemperatureHumidityExample {
  private static class MockedSensor implements TemperatureHumiditySensor {

    @Override
    public float getTemperature() {
      return new Random().nextFloat() * 40;
    }

    @Override
    public float getRelativeHumidity() {
      return new Random().nextFloat();
    }
  }

  public static void main(String[] args) throws Exception {
    // Create gateway
    BloccContractGateway gateway =
        new BloccContractGateway(
            System.getenv().getOrDefault("MSP_ID", "Org1MSP"),
            System.getenv().getOrDefault("CHANNEL_NAME", "mychannel"),
            System.getenv().getOrDefault("CHAINCODE_NAME", "blocc-temp-humidity-reading"),
            Paths.get(
                "/Users/tonywu/code/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com"),
            Paths.get("users/User1@org1.example.com/msp/signcerts/User1@org1.example.com-cert.pem"),
            Paths.get("users/User1@org1.example.com/msp/keystore"),
            Paths.get("peers/peer0.org1.example.com/tls/ca.crt"),
            "localhost:7051",
            "peer0.org1.example.com");

    // Instantiate app with a contract generated by the gateway
    TemperatureHumidityReadingContractApp app =
        new TemperatureHumidityReadingContractApp(gateway.getContract(), new MockedSensor());

    // Invoke a transaction
    int count = 0;
    while (count < 10) {
      Thread.sleep(1500);
      app.addReading();
      count++;
    }

    gateway.close();
  }
}
