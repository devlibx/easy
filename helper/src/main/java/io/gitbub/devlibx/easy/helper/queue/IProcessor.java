package io.gitbub.devlibx.easy.helper.queue;

public interface IProcessor<T> {
    void process(T t);
}