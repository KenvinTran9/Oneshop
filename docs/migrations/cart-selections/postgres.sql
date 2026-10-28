BEGIN;
ALTER TABLE shopping_cart_items ADD COLUMN selected_size VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE shopping_cart_items ADD COLUMN selected_shade VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE shopping_cart_items DROP CONSTRAINT uk_cart_item_sku;
ALTER TABLE shopping_cart_items ADD CONSTRAINT uk_cart_item_selection
    UNIQUE (cart_id, product_id, selected_size, selected_shade);
COMMIT;
