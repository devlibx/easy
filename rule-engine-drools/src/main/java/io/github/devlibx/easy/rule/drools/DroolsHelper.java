package io.github.devlibx.easy.rule.drools;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.io.FileUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

public class DroolsHelper {

    private KieContainer kContainer;
    private final Object ONCE = new Object();
    private boolean done;

    public void initialize(String ruleFile) throws Exception {
        synchronized (ONCE) {
            if (!done) {
                internalInitialize(ruleFile);
                done = true;
            }
        }
    }

    public KieSession getKieSessionWithAgenda(String agenda) {
        KieSession kSession = kContainer.newKieSession();
        kSession.getAgenda().getAgendaGroup(agenda).setFocus();
        return kSession;
    }

    private void internalInitialize(String ruleFile) throws Exception {
        // You can put your DRL here - i am reading it from file to keep the example code clean
        String downloadFile = "/tmp/" + UUID.randomUUID().toString();

        // Download file if required
        if (ruleFile.startsWith("s3")) {
            downloadS3File(ruleFile, downloadFile);
        } else if (ruleFile.startsWith("/")) {
            downloadFile = ruleFile;
        }
        String drl = FileUtils.readFileToString(new File(downloadFile));

        // Default setup fro Drools
        KieServices ks = KieServices.Factory.get();
        KieRepository kr = ks.getRepository();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/org/kie/example5/" + UUID.randomUUID() + ".drl", drl);
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
        }

        kContainer = ks.newKieContainer(kr.getDefaultReleaseId());
    }

    private void downloadS3File(String file, String outputFile) {
        System.out.format("Downloading %s from S3 ...\n", file);
        file = file.replace("s3://", "");
        String bucket = file.substring(0, file.indexOf('/'));
        String key = file.substring(file.indexOf('/'));
        System.out.format("Downloading bucket=%s key=%s from S3 ...\n", bucket, key);

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
        try {
            S3Object o = s3.getObject(bucket, key);
            S3ObjectInputStream s3is = o.getObjectContent();
            FileOutputStream fos = new FileOutputStream(new File(outputFile));
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
