package org.n52.proxy.cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class CachingFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(CachingFilter.class);

    private final SimpleCache cache = new SimpleCache();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("Caching filter initialized");
    }

    /*
     * 1. Check if httprequest
     * 2. Check that it's a data request
     * 3. Check timespan param
     * 4. Go to the cache for that values
     * 5. Return values or continue request processing
     * 6. Write returned values to the cache
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        LOG.info("Doing caching filtering");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String targetPath = request.getPathInfo();
        Map<String, String[]> queryParams = request.getParameterMap();
        if (targetPath == null || !targetPath.endsWith("/data")) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else if (!queryParams.keySet().contains("timespan")) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else if (cache.isResponseCached(targetPath, queryParams)) {
            LOG.info("Found cached response for query {}", SimpleCache.requestParamsToString(targetPath, queryParams));
            servletResponse.getOutputStream().write(cache.getCachedResponse(targetPath, queryParams));
        } else {
            LOG.info("No cached response for query {}. Saving response to cache",
                    SimpleCache.requestParamsToString(targetPath, queryParams));
            ResponseRecordingWrapper wrapper = new ResponseRecordingWrapper((HttpServletResponse) servletResponse);
            filterChain.doFilter(request, wrapper);
            cache.put(targetPath, queryParams, wrapper.getBody());
        }
        LOG.info("End of caching filtering");
    }

    @Override
    public void destroy() {
        LOG.info("Destroying caching filter");
    }

    private static class ResponseRecordingWrapper extends HttpServletResponseWrapper {
        private ByteArrayOutputStream output = new ByteArrayOutputStream();

        ResponseRecordingWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            ServletOutputStream original = super.getOutputStream();
            return new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    original.write(b);
                    output.write(b);
                }
            };
        }

        public byte[] getBody() {
            return output.toByteArray();
        }
    }
}
