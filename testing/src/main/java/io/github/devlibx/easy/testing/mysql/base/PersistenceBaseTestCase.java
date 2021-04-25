package io.github.devlibx.easy.testing.mysql.base;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.testing.mysql.base.PersistenceTestState.PersistenceTestContext;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public abstract class PersistenceBaseTestCase {
    protected PersistenceTestContext persistenceTestContext;
    protected Injector injector;

    public void setupTest() {
        persistenceTestContext = PersistenceTestState.instance().getContext();
        persistenceTestContext.setLongs(new HashMap<>());
        persistenceTestContext.setUuids(new HashMap<>());
        persistenceTestContext.setStrings(new HashMap<>());
        setupLogging();
        setupPersistenceTestContext();
        setupGuice();
        setupDatabase();
        bootstrapDatabase();
    }

    protected void setupPersistenceTestContext() {
    }

    private void setupLogging() {
    }

    private void setupGuice() {
        injector = Guice.createInjector(getModuleList());
        ApplicationContext.setInjector(injector);
    }

    protected void setupDatabase() {
    }

    protected void bootstrapDatabase() {
    }

    protected List<AbstractModule> getModuleList() {
        List<AbstractModule> modules = new ArrayList<>();
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
            }
        });
        return modules;
    }

    public String getOrCreateString(String key) {
        if (!persistenceTestContext.getStrings().containsKey(key)) {
            persistenceTestContext.getStrings().put(key, UUID.randomUUID().toString());
        }
        return persistenceTestContext.getStrings().get(key);
    }

    public String getOrCreateString(String key, String prefix) {
        if (!persistenceTestContext.getStrings().containsKey(key)) {
            persistenceTestContext.getStrings().put(key, prefix + UUID.randomUUID().toString());
        }
        return persistenceTestContext.getStrings().get(key);
    }

    public Long getOrCreateLong(String key) {
        if (!persistenceTestContext.getLongs().containsKey(key)) {
            Random random = new Random();
            persistenceTestContext.getLongs().put(key, random.nextLong());
        }
        return persistenceTestContext.getLongs().get(key);
    }

    public UUID getOrCreateUUID(String key) {
        if (!persistenceTestContext.getUuids().containsKey(key)) {
            persistenceTestContext.getUuids().put(key, UUID.randomUUID());
        }
        return persistenceTestContext.getUuids().get(key);
    }


    @AfterEach
    public void tearDown() throws Exception {
    }
}
