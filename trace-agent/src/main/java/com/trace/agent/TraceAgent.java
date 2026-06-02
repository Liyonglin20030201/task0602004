package com.trace.agent;

import com.trace.agent.config.AgentConfig;
import com.trace.agent.transformer.DubboFilterTransformer;
import com.trace.agent.transformer.HttpClientTransformer;
import com.trace.agent.transformer.ServletTransformer;
import com.trace.common.context.TraceContext;
import com.trace.reporter.SpanReporter;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class TraceAgent {

    public static void premain(String arguments, Instrumentation instrumentation) {
        AgentConfig config = AgentConfig.parse(arguments);
        TraceContext.setServiceName(config.getServiceName());
        SpanReporter.initialize(config.getKafkaBrokers(), config.getQueueCapacity(),
                config.getBatchSize(), config.getFlushIntervalMs());

        new AgentBuilder.Default()
                .ignore(ElementMatchers.nameStartsWith("com.trace.agent.shaded."))
                .type(ElementMatchers.hasSuperType(
                        ElementMatchers.named("javax.servlet.http.HttpServlet")))
                .transform(new ServletTransformer())
                .installOn(instrumentation);

        new AgentBuilder.Default()
                .ignore(ElementMatchers.nameStartsWith("com.trace.agent.shaded."))
                .type(ElementMatchers.hasSuperType(
                        ElementMatchers.named("org.apache.dubbo.rpc.Filter"))
                        .or(ElementMatchers.hasSuperType(
                                ElementMatchers.named("com.alibaba.dubbo.rpc.Filter"))))
                .transform(new DubboFilterTransformer())
                .installOn(instrumentation);

        new AgentBuilder.Default()
                .ignore(ElementMatchers.nameStartsWith("com.trace.agent.shaded."))
                .type(ElementMatchers.hasSuperType(
                        ElementMatchers.named("org.apache.http.impl.client.CloseableHttpClient")))
                .transform(new HttpClientTransformer())
                .installOn(instrumentation);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SpanReporter reporter = SpanReporter.getInstance();
            if (reporter != null) {
                reporter.shutdown();
            }
        }));

        System.out.println("[Trace-Agent] Initialized for service: " + config.getServiceName());
    }
}
