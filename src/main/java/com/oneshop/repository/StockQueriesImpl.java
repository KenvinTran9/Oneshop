package com.oneshop.repository;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
public class StockQueriesImpl implements StockQueries {
    private final MongoTemplate mongo;
    public StockQueriesImpl(MongoTemplate mongo) { this.mongo = mongo; }
    public long sumAvailableQuantityByProductId(Long productId) {
        var rows = mongo.getCollection("store_stocks").aggregate(List.of(
            new Document("$match", new Document("product", productId).append("active", true)),
            Document.parse("{'$lookup': {'from':'stores','localField':'store','foreignField':'_id','as':'shop'}}"),
            Document.parse("{'$match': {'shop.active':true}}"),
            Document.parse("{'$group': {'_id':null,'quantity':{'$sum':'$quantity'}}}")
        )).into(new ArrayList<>());
        return rows.isEmpty() ? 0 : ((Number) rows.getFirst().get("quantity")).longValue();
    }
}
