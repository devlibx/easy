package io.github.devlibx.easy.database.mysql.debug;

import com.mysql.cj.Query;
import com.mysql.cj.Session;
import com.mysql.cj.log.Log;
import com.mysql.cj.log.ProfilerEvent;
import com.mysql.cj.log.ProfilerEventHandler;
import com.mysql.cj.protocol.Resultset;
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

    @Override
    public void processEvent(byte b, Session session, Query query, Resultset resultset, long l, Throwable throwable, String s) {
    }
}
