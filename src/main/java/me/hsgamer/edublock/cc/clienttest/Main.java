package me.hsgamer.edublock.cc.clienttest;

import com.owlike.genson.Genson;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import me.hsgamer.edublock.cc.clienttest.model.*;
import me.hsgamer.edublock.cc.clienttest.model.Record;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;
import org.hyperledger.fabric.protos.gateway.ErrorDetail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Genson genson = new Genson();

    // Path to crypto materials.
    private static final Path cryptoPath = Paths.get("..", "EduBlock.MiniNet", "vars", "keyfiles", "peerOrganizations", "org0.example.com");
    // Path to user certificate.
    private static final Path certPath = cryptoPath.resolve(Paths.get("users", "Admin@org0.example.com", "msp", "signcerts", "Admin@org0.example.com-cert.pem"));
    // Path to user private key directory.
    private static final Path keyDirPath = cryptoPath.resolve(Paths.get("users", "Admin@org0.example.com", "msp", "keystore"));
    // Path to peer tls certificate.
    private static final Path tlsCertPath = cryptoPath.resolve(Paths.get("peers", "peer1.org0.example.com", "tls", "ca.crt"));

    public static void main(String[] args) throws IOException, CertificateException, InvalidKeyException, InterruptedException {
        var certReader = Files.newBufferedReader(certPath);
        var certificate = Identities.readX509Certificate(certReader);
        Identity identity = new X509Identity("org0-example-com", certificate);

        Path keyPath;
        try (var keyFiles = Files.list(keyDirPath)) {
            keyPath = keyFiles.findFirst().orElseThrow();
        }
        var keyReader = Files.newBufferedReader(keyPath);
        var privateKey = Identities.readPrivateKey(keyReader);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        var tlsCertReader = Files.newBufferedReader(tlsCertPath);
        var tlsCert = Identities.readX509Certificate(tlsCertReader);
        SocketAddress endpointAddress = new InetSocketAddress("127.0.0.1", 7002);
        ManagedChannel grpcChannel = NettyChannelBuilder.forAddress(endpointAddress)
                .sslContext(GrpcSslContexts.forClient().trustManager(tlsCert).build()).overrideAuthority("peer1.org0.example.com")
                .build();

        Gateway.Builder builder = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(grpcChannel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork(args.length > 0 ? args[0] : "mychannel");
            Contract contract = network.getContract("edublockcc");

            var personal1 = new Personal();
            personal1.setFirstName("Hieu");
            personal1.setLastName("Nguyen");
            var updatePersonalTransaction = contract.newProposal("updateStudentPersonal")
                    .addArguments("1")
                    .putTransient("personal", genson.serialize(personal1))
                    .build()
                    .endorse()
                    .submitAsync();

            System.out.println("Submitted update outputPersonal2 1: " + updatePersonalTransaction.getStatus());

            var personal2 = new Personal();
            personal2.setFirstName("Hieu");
            personal2.setLastName("Nguyen Huu");
            var updatePersonalTransaction2 = contract.newProposal("updateStudentPersonal")
                    .addArguments("2")
                    .putTransient("personal", genson.serialize(personal2))
                    .build()
                    .endorse()
                    .submitAsync();

            System.out.println("Submitted update outputPersonal2 2: " + updatePersonalTransaction2.getStatus());

            byte[] personalBytes1 = contract.evaluateTransaction("getStudentPersonal", "1");
            var outputPersonal1 = genson.deserialize(new String(personalBytes1, StandardCharsets.UTF_8), Personal.class);
            System.out.println("Personal 1: " + outputPersonal1);

            byte[] personalBytes2 = contract.evaluateTransaction("getStudentPersonal", "2");
            var outputPersonal2 = genson.deserialize(new String(personalBytes2, StandardCharsets.UTF_8), Personal.class);
            System.out.println("Personal 2: " + outputPersonal2);

            byte[] allPersonalBytes = contract.evaluateTransaction("getAllStudentPersonals");
            var outputAllPersonal = genson.deserialize(new String(allPersonalBytes, StandardCharsets.UTF_8), PersonalMap.class);
            System.out.println("All personals: " + outputAllPersonal);

            var record1 = new Record();
            var classRecordMap1 = new HashMap<Long, ClassRecord>();
            record1.setClassRecords(classRecordMap1);
            var classRecord1 = new ClassRecord();
            classRecord1.setYear(2020);
            classRecord1.setGrade(10);
            classRecord1.setClassName("10A1");
            var subjectMap1 = new HashMap<Long, Subject>();
            classRecord1.setSubjects(subjectMap1);
            var subject1 = new Subject();
            subject1.setName("Math");
            subject1.setFirstHalfScore(ThreadLocalRandom.current().nextFloat(0, 10));
            subject1.setSecondHalfScore(ThreadLocalRandom.current().nextFloat(0, 10));
            subject1.setFinalScore(ThreadLocalRandom.current().nextFloat(0, 10));
            subjectMap1.put(1L, subject1);
            classRecordMap1.put(1L, classRecord1);
            var updateRecordTransaction = contract.newProposal("updateStudentRecord")
                    .addArguments("1")
                    .putTransient("record", genson.serialize(record1))
                    .build()
                    .endorse()
                    .submitAsync();

            System.out.println("Submitted update record 1: " + updateRecordTransaction.getStatus());

            byte[] recordBytes1 = contract.evaluateTransaction("getStudentRecord", "1");
            var outputRecord1 = genson.deserialize(new String(recordBytes1, StandardCharsets.UTF_8), Record.class);
            System.out.println("Record 1: " + outputRecord1);

            byte[] allRecordBytes = contract.evaluateTransaction("getAllStudentRecords");
            var outputAllRecord = genson.deserialize(new String(allRecordBytes, StandardCharsets.UTF_8), RecordMap.class);
            System.out.println("All records: " + outputAllRecord);

            byte[] recordHistoryBytes = contract.evaluateTransaction("getStudentRecordHistory", "1");
            var outputRecordHistory = genson.deserialize(new String(recordHistoryBytes, StandardCharsets.UTF_8), RecordHistoryList.class);
            System.out.println("Record history: " + outputRecordHistory);
        } catch (EndorseException e) {
            e.printStackTrace();
            System.out.println("Endorse error: " + e.getMessage());
            System.out.println("Endorse error details: " + e.getDetails());
            System.out.println("Endorse status: " + e.getStatus());
            printErrorDetails(e.getDetails());
        } catch (SubmitException e) {
            e.printStackTrace();
            System.out.println("Submit error: " + e.getMessage());
            System.out.println("Submit details: " + e.getDetails());
            System.out.println("Submit status: " + e.getStatus());
            printErrorDetails(e.getDetails());
        } catch (CommitStatusException e) {
            e.printStackTrace();
            System.out.println("Commit Status error: " + e.getMessage());
            System.out.println("Commit Status details: " + e.getDetails());
            System.out.println("Commit Status status: " + e.getStatus());
            printErrorDetails(e.getDetails());
        } catch (GatewayException e) {
            e.printStackTrace();
            System.out.println("Gateway error: " + e.getMessage());
            System.out.println("Gateway error details: " + e.getDetails());
            System.out.println("Gateway status: " + e.getStatus());
            printErrorDetails(e.getDetails());
        } finally {
            grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public static void printErrorDetails(List<ErrorDetail> errorDetails) {
        for (ErrorDetail errorDetail : errorDetails) {
            System.out.println("Error: " + errorDetail.getMessage());
            System.out.println("Error address: " + errorDetail.getAddress());
            System.out.println("Error msp id: " + errorDetail.getMspId());
            System.out.println("Error fields: " + errorDetail.getAllFields());
        }
    }
}