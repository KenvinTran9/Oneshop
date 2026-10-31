-- Additive, repeatable restock subscription schema. Does not change existing catalog or orders.
CREATE TABLE IF NOT EXISTS product_watches (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL,
 active BIT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 user_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL,
 delivery_id VARCHAR(36) NOT NULL,
 email_enabled BIT NOT NULL DEFAULT 0,
 available_at DATETIME(6) NULL,
 read_at DATETIME(6) NULL,
 notified_at DATETIME(6) NULL,
 last_attempt_at DATETIME(6) NULL,
 CONSTRAINT uk_watch_user_product UNIQUE (user_id,product_id),
 CONSTRAINT fk_watch_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_watch_product FOREIGN KEY (product_id) REFERENCES products(id),
 INDEX idx_watch_pending (active,notified_at,last_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @oneshop_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='product_watches' AND column_name='email_enabled'), 'SELECT 1', 'ALTER TABLE product_watches ADD COLUMN email_enabled BIT NOT NULL DEFAULT 0');
PREPARE oneshop_stmt FROM @oneshop_ddl;
EXECUTE oneshop_stmt;
DEALLOCATE PREPARE oneshop_stmt;

SET @oneshop_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='product_watches' AND column_name='available_at'), 'SELECT 1', 'ALTER TABLE product_watches ADD COLUMN available_at DATETIME(6) NULL');
PREPARE oneshop_stmt FROM @oneshop_ddl;
EXECUTE oneshop_stmt;
DEALLOCATE PREPARE oneshop_stmt;

SET @oneshop_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='product_watches' AND column_name='read_at'), 'SELECT 1', 'ALTER TABLE product_watches ADD COLUMN read_at DATETIME(6) NULL');
PREPARE oneshop_stmt FROM @oneshop_ddl;
EXECUTE oneshop_stmt;
DEALLOCATE PREPARE oneshop_stmt;
