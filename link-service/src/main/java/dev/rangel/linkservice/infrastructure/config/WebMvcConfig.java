package dev.rangel.linkservice.infrastructure.config;

import dev.rangel.linkservice.infrastructure.web.interceptor.LinkCreationRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(LinkCreationRateLimitProperties.class)
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LinkCreationRateLimitInterceptor linkCreationRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(linkCreationRateLimitInterceptor).addPathPatterns("/api/v1/links");
    }
}
