package me.w1992wishes.kafka.spring.integration.config;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;

/**
  * 如果KafkaSpringIntegrationApplication 中没有配置， 就需使用这个yaml资源加载类
  */
public class YamlPropertyLoaderFactory extends DefaultPropertySourceFactory {

  @Override
  public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
    if (resource == null) {
      super.createPropertySource(name, resource);
    }
    return new YamlPropertySourceLoader().load(resource.getResource().getFilename(), resource.getResource()).get(0);
  }
}