package uk.ac.ic.doc.blocc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.SubmitException;
import uk.ac.ic.doc.blocc.sensor.TemperatureHumiditySensor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BloccPiDemoApp {
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

    public static void main(String[] args) {
        Logger logger = LogManager.getLogger();

        int containerNum = Integer.parseInt(System.getenv("FABRIC_CONTAINER_NUM"));
        float mean = Float.parseFloat(System.getenv("FABRIC_SENSOR_MEAN_TEMPERATURE"));
        int interval = Integer.parseInt(System.getenv("FABRIC_SENSOR_INTERVAL_SECOND"));
        String fabricOrgPath = System.getenv("FABRIC_ORG_PATH");
        String fabricPeerAddress = System.getenv("FABRIC_PEER_ADDRESS");

        String mspId = String.format("Container%dMSP", containerNum);
        String channelId = String.format("channel%d", containerNum);
        Path cryptoPath = Paths.get(
                String.format("%s/peerOrganizations/container%d.blocc.doc.ic.ac.uk", fabricOrgPath, containerNum));
        Path certPath = Paths.get(
                String.format("users/User1@container%d.blocc.doc.ic.ac.uk/msp/signcerts/User1@container%d.blocc.doc.ic.ac.uk-cert.pem",
                        containerNum, containerNum));
        Path keyDirPath = Paths.get(String.format("users/User1@container%d.blocc.doc.ic.ac.uk/msp/keystore", containerNum));
        Path tlsCertPath = Paths.get(String.format("peers/peer0.container%d.blocc.doc.ic.ac.uk/tls/ca.crt", containerNum));
        String overrideAuth = String.format("blocc-container%d", containerNum);

        logger.info("Creating Gateway");

        // Create gateway
        BloccContractGateway gateway =
                new BloccContractGateway(
                        mspId,
                        channelId,
                        "sensor_chaincode",
                        cryptoPath,
                        certPath,
                        keyDirPath,
                        tlsCertPath,
                        fabricPeerAddress,
                        overrideAuth);

        logger.info("Creating App");

        TemperatureHumidityReadingContractApp app =
                new TemperatureHumidityReadingContractApp(gateway.getContract(), new MockedSensor(mean));

        logger.info("Creating executor");
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                app.addReading();
            } catch (EndorseException | CommitException | SubmitException | CommitStatusException e) {
                logger.error(e);
            }
        }, 0, interval, TimeUnit.SECONDS);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("Uncaught exception in thread " + thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        });

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown initiated...");
            gateway.close();
            executor.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait for existing tasks to terminate
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Cancel currently executing tasks
                }
            } catch (InterruptedException e) {
                // If awaitTermination is interrupted, cancel tasks
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Executor shut down...");
        }));

    }
}
