package com.oneshop;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Every test run owns a newly generated schema, never the application's database. */
public abstract class MySqlIntegrationTestSupport {
    private static final String DATABASE = "oneshop_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final Properties LOCAL = localProperties();
    private static final String USERNAME = setting("MYSQL_USERNAME", "root");
    private static final String PASSWORD = setting("MYSQL_PASSWORD", "");
    private static final String SERVER = serverUrl();
    private static final String URL = SERVER.replace("/?", "/" + DATABASE + "?");
    @Autowired private JdbcTemplate jdbc;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (var connection = DriverManager.getConnection(SERVER, USERNAME, PASSWORD);
                 var statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE `" + DATABASE + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci");
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot create isolated MySQL test database; configure MYSQL_USERNAME/MYSQL_PASSWORD locally", failure);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!DATABASE.matches("oneshop_test_[a-f0-9]{32}")) return;
            try (var connection = DriverManager.getConnection(SERVER, USERNAME, PASSWORD);
                 var statement = connection.createStatement()) {
                statement.execute("DROP DATABASE `" + DATABASE + "`");
            } catch (Exception failure) {
                System.err.println("Could not remove isolated MySQL test schema " + DATABASE);
            }
        }, "oneshop-test-schema-cleanup"));
    }

    private static Properties localProperties() {
        Properties properties = new Properties();
        if (Files.isRegularFile(Path.of(".env"))) {
            try (var reader = Files.newBufferedReader(Path.of(".env"), StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot read local database configuration", failure);
            }
        }
        return properties;
    }

    private static String setting(String name, String fallback) {
        return System.getenv().getOrDefault(name, LOCAL.getProperty(name, fallback));
    }

    private static String serverUrl() {
        String configured = setting("ONESHOP_TEST_MYSQL_URL", setting("MYSQL_URL",
                "jdbc:mysql://127.0.0.1:3306/oneshop_mysql?allowPublicKeyRetrieval=true&useSSL=false"));
        var matcher = java.util.regex.Pattern.compile("^(jdbc:mysql://[^/]+)/[^?]*(\\?.*)?$").matcher(configured);
        if (!matcher.matches()) throw new IllegalStateException("Expected a MySQL JDBC URL for isolated tests");
        String parameters = matcher.group(2) == null ? "?" : matcher.group(2);
        return matcher.group(1) + "/" + parameters
                + (parameters.length() > 1 ? "&" : "") + "connectTimeout=5000&connectionTimeZone=Asia/Ho_Chi_Minh";
    }

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> URL);
        properties.add("spring.datasource.username", () -> USERNAME);
        properties.add("spring.datasource.password", () -> PASSWORD);
    }

    @BeforeEach
    protected void clearOnlyIsolatedTestData() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            if (!DATABASE.equals(connection.getCatalog()) || !DATABASE.matches("oneshop_test_[a-f0-9]{32}"))
                throw new IllegalStateException("Refusing to clear a non-test MySQL database");
            try (var statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                try {
                    for (String table : java.util.List.of("password_reset_accounts", "shopping_cart_items", "shopping_carts", "order_events", "order_items",
                            "orders", "store_stocks", "product_shades", "product_sizes", "users", "products",
                            "brands", "categories", "stores", "roles")) {
                        statement.executeUpdate("DELETE FROM `" + table + "`");
                    }
                } finally {
                    statement.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            }
            return null;
        });
    }
}
