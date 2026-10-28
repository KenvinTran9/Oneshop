package com.oneshop.repository;

import com.oneshop.entity.Brand;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class ProductQueriesImpl implements ProductQueries {
    private final MongoTemplate mongo;
    public ProductQueriesImpl(MongoTemplate mongo) { this.mongo = mongo; }

    public Page<Product> searchCatalog(ProductStatus status, String category, String search, Pageable pageable) {
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("status").is(status));
        if (category != null && !category.isBlank()) {
            Category match = mongo.findOne(Query.query(Criteria.where("slug").is(category)), Category.class);
            if (match == null) return Page.empty(pageable);
            filters.add(Criteria.where("category").is(match.getId()));
        }
        if (search != null && !search.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(search.trim()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            var brandIds = mongo.find(Query.query(Criteria.where("name").regex(pattern)), Brand.class).stream().map(Brand::getId).toList();
            filters.add(new Criteria().orOperator(Criteria.where("name").regex(pattern), Criteria.where("brand").in(brandIds)));
        }
        Criteria criteria = new Criteria().andOperator(filters.toArray(Criteria[]::new));
        long count = mongo.count(Query.query(criteria), Product.class);
        var rows = mongo.find(Query.query(criteria).with(pageable), Product.class);
        return new PageImpl<>(rows, pageable, count);
    }
    public Page<Product> findByStatusAndCategory_Slug(ProductStatus status, String category, Pageable pageable) {
        return searchCatalog(status, category, "", pageable);
    }
    private List<Document> aggregate(ProductStatus status, String... stages) {
        var pipeline = new ArrayList<Document>();
        pipeline.add(new Document("$match", new Document("status", status.name())));
        for (String stage : stages) pipeline.add(Document.parse(stage));
        return mongo.getCollection("products").aggregate(pipeline).into(new ArrayList<>());
    }
    public List<ProductRepository.HomeCategory> summarizeHomeCategories(ProductStatus status) {
        return aggregate(status, "{'$match':{'category':{'$ne':null}}}",
                "{'$group':{'_id':'$category','count':{'$sum':1}}}",
                "{'$lookup':{'from':'categories','localField':'_id','foreignField':'_id','as':'category'}}",
                "{'$unwind':'$category'}", "{'$sort':{'_id':1}}")
                .stream().map(row -> (ProductRepository.HomeCategory) new CategorySummary(
                        ((Number) row.get("_id")).longValue(), row.get("category", Document.class).getString("name"),
                        row.get("category", Document.class).getString("slug"), ((Number) row.get("count")).longValue())).toList();
    }
    public List<ProductRepository.HomeBrand> summarizeHomeBrands(ProductStatus status, Pageable pageable) {
        return aggregate(status, "{'$match':{'brand':{'$ne':null}}}", "{'$group':{'_id':'$brand','count':{'$sum':1}}}",
                "{'$lookup':{'from':'brands','localField':'_id','foreignField':'_id','as':'brand'}}",
                "{'$unwind':'$brand'}", "{'$sort':{'count':-1,'brand.name':1}}",
                "{'$skip':" + pageable.getOffset() + "}", "{'$limit':" + pageable.getPageSize() + "}")
                .stream().map(row -> (ProductRepository.HomeBrand) new BrandSummary(
                        row.get("brand", Document.class).getString("name"), ((Number) row.get("count")).longValue())).toList();
    }
    public ProductRepository.HomeCounts summarizeHomeCounts(ProductStatus status) {
        var rows = aggregate(status, "{'$group':{'_id':null,'count':{'$sum':1},'brands':{'$addToSet':'$brand'}}}");
        if (rows.isEmpty()) return new Counts(0, 0);
        var row = rows.getFirst();
        return new Counts(((Number) row.get("count")).longValue(), row.getList("brands", Object.class).stream().filter(java.util.Objects::nonNull).count());
    }
    private record CategorySummary(Long id, String name, String slug, long count) implements ProductRepository.HomeCategory {
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getSlug() { return slug; }
        public long getProductCount() { return count; }
    }
    private record BrandSummary(String name, long count) implements ProductRepository.HomeBrand {
        public String getName() { return name; }
        public long getProductCount() { return count; }
    }
    private record Counts(long products, long brands) implements ProductRepository.HomeCounts {
        public long getProductCount() { return products; }
        public long getBrandCount() { return brands; }
    }
}
