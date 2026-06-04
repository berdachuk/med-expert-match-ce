package com.berdachuk.medexpertmatch.core.shutdown;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RequestInFlightFilter implements Filter {

    private static final AtomicInteger inFlightCount = new AtomicInteger(0);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request instanceof HttpServletRequest h ? h.getRequestURI() : "unknown";
        inFlightCount.incrementAndGet();
        try {
            log.debug("Request started [path={}, inFlight={}]", path, inFlightCount.get());
            chain.doFilter(request, response);
        } finally {
            int remaining = inFlightCount.decrementAndGet();
            log.debug("Request completed [path={}, inFlight={}]", path, remaining);
        }
    }

    public static int getInFlightCount() {
        return inFlightCount.get();
    }
}
