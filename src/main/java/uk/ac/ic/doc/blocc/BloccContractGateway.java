package uk.ac.ic.doc.blocc;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

public class BloccContractGateway {

  private final String mspId;

  // Path to user certificate.
  private final Path certPath;
  // Path to user private key directory.
  private final Path keyDirPath;
  // Path to peer tls certificate.
  private final Path tlsCertPath;

  // Gateway peer end point.
  private final String peerEndpoint;
  private final String overrideAuth;

  private final Gateway gateway;
  private final ManagedChannel channel;
  private final Contract contract;

  /**
   * Establish gRPC connection to a specified chaincode on a specified channel.
   *
   * @param mspId the MSP ID
   * @param channelName the specified channel
   * @param chaincodeName the specified chaincode name
   * @param cryptoPath path to crypto material
   * @param certPath path to user certificate
   * @param keyDirPath path to user private key directory
   * @param tlsCertPath path to peer TLS certificate
   * @param peerEndpoint gateway peer endpoint
   * @param overrideAuth overrides the authority used with TLS and HTTP virtual hosting
   */
  protected BloccContractGateway(
      String mspId,
      String channelName,
      String chaincodeName,
      Path cryptoPath,
      Path certPath,
      Path keyDirPath,
      Path tlsCertPath,
      String peerEndpoint,
      String overrideAuth) {

    // TODO: refactor with Builder pattern

    this.mspId = mspId;
    this.certPath = cryptoPath.resolve(certPath);
    this.keyDirPath = cryptoPath.resolve(keyDirPath);
    this.tlsCertPath = cryptoPath.resolve(tlsCertPath);
    this.peerEndpoint = peerEndpoint;
    this.overrideAuth = overrideAuth;

    // The gRPC client connection should be shared by all Gateway connections to
    // this endpoint.
    channel = newGrpcConnection();

    gateway =
        Gateway.newInstance()
            .identity(newIdentity())
            .signer(newSigner())
            .connection(channel)
            // Default timeouts for different gRPC calls
            .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
            .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
            .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
            .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES))
            .connect();

    contract = gateway.getNetwork(channelName).getContract(chaincodeName);
  }

  protected ManagedChannel newGrpcConnection() {
    ChannelCredentials credentials;
    try {
      credentials = TlsChannelCredentials.newBuilder().trustManager(tlsCertPath.toFile()).build();
    } catch (IOException e) {
      throw new RuntimeException("IO Error occurred when reading TLS certificate file", e);
    }
    return Grpc.newChannelBuilder(peerEndpoint, credentials)
        .overrideAuthority(overrideAuth)
        .build();
  }

  protected Identity newIdentity() {
    BufferedReader certReader;
    try {
      certReader = Files.newBufferedReader(certPath);
    } catch (IOException e) {
      throw new RuntimeException("IO Error occurred when reading certificate file", e);
    }
    X509Certificate certificate;
    try {
      certificate = Identities.readX509Certificate(certReader);
    } catch (IOException e) {
      throw new RuntimeException("IO Error occurred when reading X509 Certificate file", e);
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }

    return new X509Identity(mspId, certificate);
  }

  protected Signer newSigner() {
    PrivateKey privateKey;
    try {
      BufferedReader keyReader = Files.newBufferedReader(getPrivateKeyPath());
      privateKey = Identities.readPrivateKey(keyReader);
    } catch (IOException e) {
      throw new RuntimeException("IO Error occurred when reading private key file", e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    }

    return Signers.newPrivateKeySigner(privateKey);
  }

  private Path getPrivateKeyPath() throws IOException {
    try (Stream<Path> keyFiles = Files.list(keyDirPath)) {
      return keyFiles.findFirst().orElseThrow();
    }
  }

  public void close() throws Exception {
    gateway.close();
    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
  }

  public Contract getContract() {
    return contract;
  }
}
