package io.github.devlibx.easy.database.mysql.debug;

import com.mysql.cj.api.ProfilerEvent;
import com.mysql.cj.api.ProfilerEventHandler;
import com.mysql.cj.api.log.Log;
import lombok.extern.slf4j.Slf4j;

@Deprecated
@Slf4j
public class DoNotUseProfilerEventHandler implements ProfilerEventHandler {
    @Override
    public void init(Log log) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void consumeEvent(ProfilerEvent profilerEvent) {
        log.debug("MySQL Profile Event - " + profilerEvent.toString());
    }
}
