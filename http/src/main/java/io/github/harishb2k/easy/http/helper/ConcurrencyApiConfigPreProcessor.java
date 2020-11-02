package io.github.harishb2k.easy.http.helper;

import io.github.harishb2k.easy.http.IApiConfigPreProcessor;
import io.github.harishb2k.easy.http.config.Api;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConcurrencyApiConfigPreProcessor implements IApiConfigPreProcessor {

    @Override
    public void process(String name, Api api) {
        if (api.getRps() <= 0) return;
        int rps = api.getRps();
        int timeout = api.getTimeout();
        float throughputPerSec = 1000.0f / timeout;
        float _concurrency = rps / throughputPerSec;
        int concurrency = (int) Math.floor(_concurrency);
        api.setConcurrency(concurrency);
        log.debug("Updated concurrency - name={}, rps={}, timeout={}, throughputPerSec={} concurrency={}",
                name, rps, timeout, throughputPerSec, concurrency);
    }
}
