package com.trace.agent.transformer;

import com.trace.agent.interceptor.HttpClientInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

public class HttpClientTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                            TypeDescription typeDescription,
                                            ClassLoader classLoader,
                                            JavaModule module) {
        return builder.visit(
                Advice.to(HttpClientInterceptor.class)
                        .on(ElementMatchers.named("doExecute")
                                .and(ElementMatchers.takesArguments(3)))
        );
    }
}
