package com.oneshop.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@Profile({"prod", "jdbc-session-test"})
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1800)
public class JdbcSessionConfig {
    @Bean
    DataSourceInitializer sessionSchema(DataSource dataSource) {
        var initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("db/session-mysql.sql")));
        return initializer;
    }

    @Bean
    SessionRepositoryCustomizer<JdbcIndexedSessionRepository> sessionRepositoryCustomizer(DataSourceInitializer sessionSchema) {
        // Requiring the initializer guarantees tables exist before the repository/cleanup task starts.
        return repository -> repository.setCreateSessionAttributeQuery("""
                INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)
                VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE ATTRIBUTE_BYTES = VALUES(ATTRIBUTE_BYTES)
                """);
    }

    @Bean
    DefaultCookieSerializer cookieSerializer(Environment environment) {
        var cookie = new DefaultCookieSerializer();
        cookie.setCookieName("JSESSIONID");
        cookie.setCookiePath("/");
        cookie.setUseHttpOnlyCookie(true);
        cookie.setSameSite("Lax");
        cookie.setUseSecureCookie(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, true));
        return cookie;
    }
}
