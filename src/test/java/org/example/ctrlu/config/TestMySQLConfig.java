package org.example.ctrlu.config;

import org.testcontainers.containers.MySQLContainer;


public class TestMySQLConfig {
    public static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>("mysql:8")
                .withDatabaseName("foureach_test")
                .withUsername("root")
                .withPassword("");
        MYSQL_CONTAINER.start();
    }
}
