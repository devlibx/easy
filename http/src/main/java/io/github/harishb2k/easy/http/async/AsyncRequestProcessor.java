package io.github.harishb2k.easy.http.async;

import io.github.harishb2k.easy.http.IRequestProcessor;
import io.github.harishb2k.easy.http.RequestObject;
import io.github.harishb2k.easy.http.ResponseObject;
import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsyncRequestProcessor implements IRequestProcessor {

    @Override
    public Observable<ResponseObject> process(RequestObject requestObject) {
        return null;
    }

    @Override
    public void shutdown() {

    }
}
