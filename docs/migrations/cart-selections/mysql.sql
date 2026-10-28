-- MySQL DDL commits automatically; stop writes and back up before applying.
ALTER TABLE shopping_cart_items
    ADD COLUMN selected_size VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN selected_shade VARCHAR(100) NOT NULL DEFAULT '',
    DROP INDEX uk_cart_item_sku,
    ADD CONSTRAINT uk_cart_item_selection UNIQUE (cart_id, product_id, selected_size, selected_shade);
