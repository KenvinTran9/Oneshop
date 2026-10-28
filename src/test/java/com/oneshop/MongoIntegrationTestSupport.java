package com.oneshop;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Tests can only remove data in their own newly generated database, never the app database. */
public abstract class MongoIntegrationTestSupport {
    private static final String DATABASE = "oneshop_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URI = testUri();
    @Autowired private MongoTemplate testMongo;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            var settings = MongoClientSettings.builder().applyConnectionString(new ConnectionString(URI))
                    .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(3, TimeUnit.SECONDS)).build();
            try (var client = MongoClients.create(settings)) {
                client.getDatabase(DATABASE).drop();
            }
        }, "oneshop-test-database-cleanup"));
    }

    private static String testUri() {
        String base = System.getenv().getOrDefault("ONESHOP_TEST_MONGODB_URI", "mongodb://127.0.0.1:27017");
        new ConnectionString(base); // Validate before replacing only the database path.
        int query = base.indexOf('?');
        String parameters = query < 0 ? "?serverSelectionTimeoutMS=5000" : base.substring(query);
        String host = query < 0 ? base : base.substring(0, query);
        int path = host.indexOf('/', host.indexOf("://") + 3);
        if (path >= 0) host = host.substring(0, path);
        return host + "/" + DATABASE + parameters;
    }

    @DynamicPropertySource
    static void isolatedMongo(DynamicPropertyRegistry properties) {
        properties.add("spring.data.mongodb.uri", () -> URI);
        properties.add("spring.data.mongodb.database", () -> DATABASE);
    }

    @BeforeEach
    protected void clearOnlyIsolatedTestData() {
        if (!testMongo.getDb().getName().equals(DATABASE) || !DATABASE.matches("oneshop_test_[a-f0-9]{32}"))
            throw new IllegalStateException("Refusing to clear a non-test MongoDB database");
        for (String collection : testMongo.getCollectionNames()) {
            if (!collection.equals("_sequences") && !collection.startsWith("system."))
                testMongo.remove(new Query(), collection);
        }
    }
}
