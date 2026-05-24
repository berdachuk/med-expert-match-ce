package com.berdachuk.medexpertmatch.core.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiUsageInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;

    public ApiUsageInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("apiUsageStart", System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startNanos = (Long) request.getAttribute("apiUsageStart");
        if (startNanos == null) {
            return;
        }
        long durationNs = System.nanoTime() - startNanos;
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        Counter.builder("medexpertmatch.api.usage.requests")
                .description("API request count by endpoint")
                .tags("method", method, "endpoint", uri)
                .register(meterRegistry)
                .increment();

        Timer.builder("medexpertmatch.api.usage.latency")
                .description("API request latency by endpoint")
                .tags("method", method, "endpoint", uri)
                .register(meterRegistry)
                .record(durationNs, java.util.concurrent.TimeUnit.NANOSECONDS);

        if (status >= 400) {
            Counter.builder("medexpertmatch.api.usage.errors")
                    .description("API error count by endpoint")
                    .tags("method", method, "endpoint", uri, "status", String.valueOf(status))
                    .register(meterRegistry)
                    .increment();
        }
    }
}
