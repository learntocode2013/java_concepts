package com.github.learntocode2013.cqrsmodelupdater;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.write")
    public DataSource postgresqlDataSource() {
        return DataSourceBuilder.create().build();
    }
}
