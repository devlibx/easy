package io.github.harishb2k.easy.http.example;

import io.gitbub.harishb2k.easy.helper.string.StringHelper;
import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.config.Server;
import io.github.harishb2k.easy.http.registry.ApiRegistry;
import io.github.harishb2k.easy.http.registry.ServerRegistry;
import io.github.harishb2k.easy.http.sync.SyncRequestProcessor;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

public class SyncHttpRequestExample {

    public static void main(String[] args) {
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


        ServerRegistry serverRegistry = new ServerRegistry();
        serverRegistry.configure(config);
        ApiRegistry apiRegistry = new ApiRegistry();
        apiRegistry.configure(config);

        IRequestProcessor requestProcessor = new SyncRequestProcessor(
                serverRegistry,
                apiRegistry,
                new StringHelper()
        );


        RequestObject requestObject = new RequestObject();
        requestObject.setServer("s1");
        requestObject.setApi("a1");
        Map<String, Object> qp = new HashMap<>();
        qp.put("id", 1);
        requestObject.setPathParam(qp);

        ResponseObject responseObject = requestProcessor.process(requestObject);
        System.out.println(requestObject);
    }
}
