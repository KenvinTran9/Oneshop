package com.oneshop.config;

import com.oneshop.entity.BaseEntity;
import com.oneshop.entity.User;
import java.time.LocalDateTime;
import java.util.Locale;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class MongoEntityCallback implements BeforeConvertCallback<BaseEntity> {
    private final org.springframework.beans.factory.ObjectProvider<MongoTemplate> mongo;

    public MongoEntityCallback(org.springframework.beans.factory.ObjectProvider<MongoTemplate> mongo) { this.mongo = mongo; }

    @Override
    public BaseEntity onBeforeConvert(BaseEntity entity, String collection) {
        if (entity.getId() == null) {
            Document counter = mongo.getObject().findAndModify(Query.query(Criteria.where("_id").is(collection)),
                    new Update().inc("sequence", 1L), FindAndModifyOptions.options().upsert(true).returnNew(true),
                    Document.class, "_sequences");
            entity.setId(((Number) counter.get("sequence")).longValue());
        }
        if (entity.getCreatedAt() == null) entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity instanceof User user && user.getEmail() != null)
            user.setEmail(user.getEmail().trim().toLowerCase(Locale.ROOT));
        return entity;
    }
}
