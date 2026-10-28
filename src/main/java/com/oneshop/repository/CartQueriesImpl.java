package com.oneshop.repository;
import com.oneshop.entity.Cart;
import com.oneshop.entity.User;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
public class CartQueriesImpl implements CartQueries {
    private final MongoTemplate mongo;
    public CartQueriesImpl(MongoTemplate mongo) { this.mongo = mongo; }
    public Optional<Cart> findByUserEmail(String email) { return findByUserEmailIgnoreCase(email); }
    public Optional<Cart> findByUserEmailIgnoreCase(String email) {
        User user = mongo.findOne(Query.query(Criteria.where("email").regex("^" + Pattern.quote(email) + "$", "i")), User.class);
        return user == null ? Optional.empty() : Optional.ofNullable(mongo.findOne(Query.query(Criteria.where("user").is(user.getId())), Cart.class));
    }
}
