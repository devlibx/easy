package io.github.harishb2k.easy.resilience;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResilienceManager implements IResilienceManager {
    private final Map<String, IResilienceProcessor> processorMap;

    public ResilienceManager() {
        this.processorMap = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized IResilienceProcessor getOrCreate(ResilienceCallConfig config) {
        if (!processorMap.containsKey(config.getId())) {
            IResilienceProcessor processor;
            try {
                processor = ApplicationContext.getInstance(IResilienceProcessor.class);
            } catch (Exception e) {
                processor = new ResilienceProcessor();
            }
            processor.initialized(config);
            processorMap.put(config.getId(), processor);
        }
        return processorMap.get(config.getId());
    }
}
