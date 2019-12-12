package eu.aequos.gogas.configuration;

import eu.aequos.gogas.multitenancy.TenantLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class CorsWebMvcConfigurer implements WebMvcConfigurer {

    @Autowired
    private TenantLoggingInterceptor tenantLoggingInterceptor;

    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE")
                .allowedOrigins("http://localhost:61502")
                .allowedHeaders("Authorization", "jquery-ajax-call", "content-type")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantLoggingInterceptor);
    }
}
