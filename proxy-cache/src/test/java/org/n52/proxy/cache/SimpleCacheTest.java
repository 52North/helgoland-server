package org.n52.proxy.cache;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SimpleCacheTest {
    private SimpleCache cache = new SimpleCache();

    @Test
    public void shouldReturnTrueIfCachedResponsePresent() {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put("param1", new String[]{"value1"});
        queryParams.put("param2", new String[]{"value2"});
        byte[] responceBody = "Response".getBytes(StandardCharsets.UTF_8);
        String path = "/api/v1/datasets/measurement_3/data";

        cache.put(path, queryParams, responceBody);

        assertTrue(cache.isResponseCached(path, queryParams));
    }
}