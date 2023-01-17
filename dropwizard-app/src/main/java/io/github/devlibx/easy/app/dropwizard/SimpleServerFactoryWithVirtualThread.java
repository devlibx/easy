package io.github.devlibx.easy.app.dropwizard;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.core.server.SimpleServerFactory;
import io.dropwizard.metrics.jetty11.InstrumentedQueuedThreadPool;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.concurrent.BlockingQueue;

@JsonTypeName("simplewithvt")
public class SimpleServerFactoryWithVirtualThread extends SimpleServerFactory {

    protected ThreadPool createThreadPool(MetricRegistry metricRegistry) {
        int minThreads = 10;
        int maxThreads = 20;
        int maxQueuedRequests = 1000;
        final BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(minThreads, maxThreads, maxQueuedRequests);
        final InstrumentedQueuedThreadPool threadPool =
                new InstrumentedQueuedThreadPool(metricRegistry, maxThreads, minThreads,
                        (int) 10000, queue);
        threadPool.setName("dw");
        threadPool.setUseVirtualThreads(true);
        return threadPool;
    }

}
