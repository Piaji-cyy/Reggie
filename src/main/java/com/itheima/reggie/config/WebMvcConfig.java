package com.itheima.reggie.config;

import com.itheima.reggie.common.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

/*
 * 静态资源一般建议存储于static或template下，但此时的静态资源放置于backend和front下，所以在没有该配置类规定映射时，backend和front下的所有静态资源都无法访问，会报404
 * 该配置文件目的是告诉MVC框架，backend和front文件夹下放置的资源就是静态资源
 * */
@Slf4j
@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {
    /*
    * 设置静态资源映射
    * @param registry
    * */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始进行静态资源映射");
        //只要路径中带有backend，就会映射到相应的路径
        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
    }

    //扩展mvc框架的消息转换器
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转化器...");
        //创建消息转换器对象
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        //设置对象转换器，底层使用Jackson将java转化为json
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        //将上面的消息转化器对象追加到mvc框架的消息转化器集合中
        converters.add(0,messageConverter);
    }
}
