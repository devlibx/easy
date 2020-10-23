package io.github.harishb2k.easy.http.sync;

import io.gitbub.harishb2k.easy.helper.file.FileHelper;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.config.Server;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyHttpRequestException;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.github.harishb2k.easy.http.util.EasyHttp;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Ignore;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Ignore
public class SyncRequestProcessorTest extends TestCase {

    private static Config buildConfig() {
        Api api = new Api();
        api.setName("a1");
        api.setPath("/posts/${id}");
        api.setServer("s1");

        Server server = new Server();
        server.setName("s1");
        server.setHttps(true);
        server.setHost("jsonplaceholder.typicode.com");
        server.setPort(443);

        Config config = new Config();
        config.addServer(server);
        config.addApi(api);

        return config;
    }

    private static void runSimple() {
        Config config = buildConfig();
        ServerRegistry serverRegistry = new ServerRegistry();
        serverRegistry.configure(config);
        ApiRegistry apiRegistry = new ApiRegistry();
        apiRegistry.configure(config);
        IRequestProcessor requestProcessor = new SyncRequestProcessor(serverRegistry, apiRegistry);

        RequestObject requestObject = new RequestObject();
        requestObject.setServer("s1");
        requestObject.setApi("a1");
        Map<String, Object> qp = new HashMap<>();
        qp.put("id", 1);
        requestObject.setPathParam(qp);
        ResponseObject responseObject = requestProcessor.process(requestObject).blockingFirst();
        requestProcessor.shutdown();
    }

    private static void runWithEasyHttp() {
        Config config = buildConfig();
        Map<String, Object> qp = new HashMap<>();
        qp.put("id", 1);

        EasyHttp.setup(config);
        EasyHttp.call(
                "s1",
                "a1",
                qp,
                null,
                null,
                null,
                Map.class
        ).subscribe(map -> {
            System.out.println("\n\n In onNext block");
            System.out.println(map);
        }, throwable -> {
            System.out.println("\n\n In onError block");
            System.out.println(throwable);
        });
    }

    public static void testWithYaml() {
        Yaml yaml = new Yaml();

        InputStream inputStream = SyncRequestProcessorTest.class
                .getClassLoader()
                .getResourceAsStream("sync_processor_config.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
        Config config = JsonUtils.getCamelCase().readObject(new StringHelper().stringify(obj), Config.class);

        Map<String, Object> qp = new HashMap<>();
        qp.put("id", 1);

        EasyHttp.setup(config);
        EasyHttp.call(
                "jsonplaceholder",
                "getPosts",
                qp,
                null,
                null,
                null,
                Map.class
        ).subscribe(map -> {
            System.out.println("\n\n In onNext block");
            System.out.println(map);
        }, throwable -> {
            System.out.println("\n\n In onError block");
            if (throwable instanceof EasyHttpRequestException) {
                EasyHttpRequestException httpRequestException = (EasyHttpRequestException) throwable;
                System.out.println(httpRequestException.getResponseAsString());
            } else {
                System.out.println(throwable);
            }
        });


        try {
            System.out.println("\n\n\n Calling Sync");
            Map resultSync = EasyHttp.callSync(
                    "jsonplaceholder",
                    "getPosts",
                    qp,
                    null,
                    null,
                    null,
                    Map.class
            );
            System.out.println(resultSync);
        } catch (EasyHttpRequestException e) {
            System.out.println(e.getResponseAsString());
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(org.apache.log4j.Level.DEBUG);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
        Logger.getLogger("io.github.harishb2k.easy.http.sync").setLevel(org.apache.log4j.Level.DEBUG);

        testWithYaml();

        System.exit(0);
    }
}