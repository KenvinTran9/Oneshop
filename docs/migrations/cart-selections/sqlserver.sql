SET XACT_ABORT ON;
BEGIN TRANSACTION;
ALTER TABLE shopping_cart_items ADD selected_size NVARCHAR(100) NOT NULL
    CONSTRAINT df_cart_selected_size DEFAULT N'' WITH VALUES;
ALTER TABLE shopping_cart_items ADD selected_shade NVARCHAR(100) NOT NULL
    CONSTRAINT df_cart_selected_shade DEFAULT N'' WITH VALUES;
ALTER TABLE shopping_cart_items DROP CONSTRAINT uk_cart_item_sku;
ALTER TABLE shopping_cart_items ADD CONSTRAINT uk_cart_item_selection
    UNIQUE (cart_id, product_id, selected_size, selected_shade);
COMMIT TRANSACTION;
