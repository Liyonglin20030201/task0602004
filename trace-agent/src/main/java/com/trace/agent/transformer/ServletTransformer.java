package com.trace.agent.transformer;

import com.trace.agent.interceptor.ServletInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

public class ServletTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                            TypeDescription typeDescription,
                                            ClassLoader classLoader,
                                            JavaModule module) {
        return builder.visit(
                Advice.to(ServletInterceptor.class)
                        .on(ElementMatchers.named("service")
                                .and(ElementMatchers.takesArgument(0,
                                        ElementMatchers.named("javax.servlet.http.HttpServletRequest")))
                                .and(ElementMatchers.takesArgument(1,
                                        ElementMatchers.named("javax.servlet.http.HttpServletResponse"))))
        );
    }
}
