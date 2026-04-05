package com.github.learntocode2013.eventsourcing.order.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;

@Configuration
public class ClickhouseConfig {
    @Value("${spring.clickhouse.jdbc-url}")
    private String url;

    @Value("${spring.clickhouse.user}")
    private String user;

    @Value("${spring.clickhouse.password}")
    private String password;

    @Bean(name = "clickhouseDataSource")
    public DataSource clickhouseDataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(com.clickhouse.jdbc.ClickHouseDriver.class);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean(name = "clickhouseJdbc")
    public JdbcTemplate clickhouseJdbcTemplate(@Qualifier("clickhouseDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
