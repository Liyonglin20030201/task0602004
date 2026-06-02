package com.trace.agent.transformer;

import com.trace.agent.interceptor.DubboInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

public class DubboFilterTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                            TypeDescription typeDescription,
                                            ClassLoader classLoader,
                                            JavaModule module) {
        return builder.visit(
                Advice.to(DubboInterceptor.class)
                        .on(ElementMatchers.named("invoke")
                                .and(ElementMatchers.takesArguments(2)))
        );
    }
}
