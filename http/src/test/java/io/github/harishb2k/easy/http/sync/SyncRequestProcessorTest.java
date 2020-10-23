package io.github.harishb2k.easy.http.sync;

import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Ignore;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("All")
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
        Config config = YamlUtils.readYamlCamelCase("sync_processor_config.yaml", Config.class);

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
        System.out.println("\n\n\n Calling Sync");

        try {
            Map resultSync = EasyHttp.callSync(
                    "jsonplaceholder",  // Name of the service
                    "getPosts",         // Name of the API
                    qp,                 // Path Params
                    null,               // Query Params
                    null,               // Header
                    null,               // Request Body
                    Map.class           // Response Class
            );
            System.out.println(resultSync);
        } catch (EasyHttpRequestException e) {
            System.out.println(e.getResponseAsString());
            System.out.println(e);
        }
    }

    public static void testWithYaml_Resillience() {
        Config config = YamlUtils.readYamlCamelCase("sync_processor_config.yaml", Config.class);

        Map<String, Object> qp = new HashMap<>();
        qp.put("id", 1);

        EasyHttp.setup(config);
        try {
            Map resultSync = EasyHttp.callSync(
                    "jsonplaceholder",  // Name of the service
                    "getPosts",         // Name of the API
                    qp,                 // Path Params
                    null,               // Query Params
                    null,               // Header
                    null,               // Request Body
                    Map.class           // Response Class
            );
            System.out.println(resultSync);
        } catch (EasyHttpRequestException e) {
            System.out.println(e.getResponseAsString());
            System.out.println(e);
        } catch (Exception e) {
            System.out.println("Got Exception");
            System.out.println(e);
            e.printStackTrace();
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
        Logger.getLogger("io.github.harishb2k.easy.http.sync").setLevel(Level.OFF);

        testWithYaml_Resillience();

        System.exit(0);
    }
}