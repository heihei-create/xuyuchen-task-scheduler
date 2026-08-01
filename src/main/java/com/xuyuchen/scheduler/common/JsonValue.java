package com.xuyuchen.scheduler.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonValue {
    private JsonValue() {}
    public static Map<String, Object> object(Object... values) {
        if (values.length % 2 != 0) throw new IllegalArgumentException("key/value pairs required");
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
