package org.company;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<CustomResponseHeaderFilter> cxfHeaderFilter(
            CustomResponseHeaderFilter filter) {

        FilterRegistrationBean<CustomResponseHeaderFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(filter);
        registration.addUrlPatterns("/example/cxf/*");
        registration.setOrder(Integer.MAX_VALUE);

        return registration;
    }
}
