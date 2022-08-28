package io.github.devlibx.easy.rule.drools;

import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.experimental.Delegate;

public class ResultMap {
    @Delegate
    private final StringObjectMap stringObjectMap = new StringObjectMap();
}
