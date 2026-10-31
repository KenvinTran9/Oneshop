CREATE TABLE IF NOT EXISTS stock_movements (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BIT(1) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL,
 store_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
 actor_email VARCHAR(120) NOT NULL, reason VARCHAR(300) NOT NULL,
 request_token VARCHAR(40) NOT NULL UNIQUE, delta INT NOT NULL, quantity_before INT NOT NULL, quantity_after INT NOT NULL,
 CONSTRAINT fk_movement_store FOREIGN KEY(store_id) REFERENCES stores(id),
 CONSTRAINT fk_movement_product FOREIGN KEY(product_id) REFERENCES products(id),
 INDEX idx_movement_store_id(store_id,id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
INSERT INTO roles (active,created_at,updated_at,version,name,description)
 SELECT 1,NOW(6),NOW(6),0,'BRANCH_MANAGER','Quản lý chi nhánh'
 WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='BRANCH_MANAGER');
CREATE TABLE IF NOT EXISTS customer_addresses (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 active BIT(1) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL,
 customer_id BIGINT NOT NULL, label VARCHAR(60) NOT NULL, recipient VARCHAR(120) NOT NULL, phone VARCHAR(20) NOT NULL, address VARCHAR(255) NOT NULL,
 CONSTRAINT fk_address_customer FOREIGN KEY(customer_id) REFERENCES users(id), INDEX idx_address_customer(customer_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
