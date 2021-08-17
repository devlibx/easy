package io.gitbub.devlibx.easy.helper.queue;

public class NoOpRateLimiter implements IRateLimiter {
    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
