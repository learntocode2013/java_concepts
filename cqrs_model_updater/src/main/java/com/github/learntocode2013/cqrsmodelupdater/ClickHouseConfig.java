package com.github.learntocode2013.cqrsmodelupdater;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

@Configuration
public class ClickHouseConfig {

    @Value("${spring.clickhouse.jdbc-url}")
    private String url;

    @Value("${spring.clickhouse.user}")
    private String user;

    @Value("${spring.clickhouse.password}")
    private String password;

    @Bean(name = "clickhouseJdbc")
    public JdbcTemplate clickhouseJdbcTemplate() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(com.clickhouse.jdbc.ClickHouseDriver.class);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
    }
}
