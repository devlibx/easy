package io.github.devlibx.easy.rule.drools;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class DroolsHelper {

    private KieContainer kContainer;
    private final Object ONCE = new Object();
    private boolean done;

    public void initializeWithRulesAsString(String rules) throws Exception {
        String tempFile = "/tmp/" + UUID.randomUUID().toString();
        FileUtils.write(new File(tempFile), rules, Charset.defaultCharset());
        initialize(tempFile);
    }

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

    /**
     * Helper function to execute rules
     *
     * @param agenda group agenda to execute
     * @param inputs input objects
     */
    public void execute(String agenda, Object... inputs) {
        KieSession kSession = null;
        try {
            kSession = getKieSessionWithAgenda(agenda);
            for (Object o : inputs) {
                kSession.insert(o);
            }
            kSession.fireAllRules();
        } finally {
            if (kSession != null) {
                kSession.destroy();
            }
        }
    }

    /**
     * Helper function to execute rules
     *
     * @param inputs input objects
     */
    public void execute(Object... inputs) {
        KieSession kSession = null;
        try {
            kSession = kContainer.newKieSession();
            for (Object o : inputs) {
                kSession.insert(o);
            }
            kSession.fireAllRules();
            kSession.destroy();
        } finally {
            if (kSession != null) {
                kSession.destroy();
            }
        }
    }

    String getFileContentFromJar(String ruleFile) {
        String file = ruleFile.replace("jar://", "");
        try (InputStream in = getClass().getResourceAsStream(file)) {
            return IOUtils.toString(in, Charset.defaultCharset());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void internalInitialize(String ruleFile) throws Exception {
        // You can put your DRL here - i am reading it from file to keep the example code clean
        String downloadFile = "/tmp/" + UUID.randomUUID().toString();

        // Download file if required
        String drl = "";
        if (ruleFile.startsWith("s3")) {
            downloadS3File(ruleFile, downloadFile);
            drl = FileUtils.readFileToString(new File(downloadFile));
        } else if (ruleFile.startsWith("/")) {
            downloadFile = ruleFile;
            drl = FileUtils.readFileToString(new File(downloadFile));
        } else if (ruleFile.startsWith("jar://")) {
            drl = getFileContentFromJar(ruleFile);
        }else if (ruleFile.startsWith("pwd://")) {
            String file = ruleFile.replace("pwd://", "");
            Path currentRelativePath = Paths.get("");
            String runningDir = currentRelativePath.toAbsolutePath().toString();
            String filePath = runningDir + "/" + file;
            drl = FileUtils.readFileToString(new File(filePath), Charset.defaultCharset());
        }

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

    void downloadS3File(String file, String outputFile) {
        System.out.format("Downloading %s from S3 to output file %s\n", file, outputFile);
        String bucket = getBucket(file);
        String key = getKey(file);
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

            System.out.format(">>>> [DONE] Downloaded %s from S3 to output file %s\n", file, outputFile);
        } catch (Exception e) {
            System.out.format(">>> [FAILED] Downloaded %s from S3 to output file %s failed\n", file, outputFile);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    String getBucket(String file) {
        file = file.replace("s3://", "");
        String bucket = file.substring(0, file.indexOf('/'));
        return bucket;
    }

    String getKey(String file) {
        file = file.replace("s3://", "");
        return file.substring(file.indexOf('/') + 1);
    }
}
