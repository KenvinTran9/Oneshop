import datetime as dt
from decimal import Decimal
import unittest

from mongo_snapshot_to_mysql import decode, literal, render


class MySqlMigrationTest(unittest.TestCase):
    def test_canonical_types_preserve_precision_and_local_audit_time(self):
        result = decode({"id": {"$numberLong": "12"}, "price": {"$numberDecimal": "155000.50"},
                         "created": {"$date": {"$numberLong": "0"}}})
        self.assertEqual(result["id"], 12)
        self.assertEqual(result["price"], Decimal("155000.50"))
        self.assertEqual(result["created"], "1970-01-01 07:00:00.000000")

    def test_untrusted_text_is_a_hex_literal(self):
        text = "Son đỏ '); DROP TABLE users; -- \\"
        value = literal(text)
        self.assertEqual(value, "CONVERT(X'" + text.encode("utf-8").hex() + "' USING utf8mb4)")
        self.assertNotIn("DROP", value)
        self.assertEqual(literal(Decimal("155000.50")), "155000.50")

    def test_cart_and_shade_relationships_preserve_ids_and_order(self):
        base = {"createdAt": "2026-10-29 00:00:00.000000", "updatedAt": "2026-10-29 00:00:00.000000"}
        sql, counts = render({
            "products": [{**base, "_id": 16, "sizes": ["3,5 g"], "shades": [{"label": "Bare Grape"}]}],
            "shopping_carts": [{**base, "_id": 7, "user": 4, "items": [
                {"product": 16, "quantity": 2, "selectedSize": "3,5 g", "selectedShade": "Bare Grape"}]}],
        })
        self.assertEqual(counts["shopping_cart_items"], 1)
        self.assertEqual(counts["product_shades"], 1)
        self.assertIn("INSERT INTO `shopping_carts`", sql)
        self.assertIn("`cart_id`,`product_id`", sql)
        self.assertIn("START TRANSACTION;", sql)
        self.assertTrue(sql.endswith("COMMIT;\n"))

    def test_rejects_unexpected_collections_and_missing_audit_data(self):
        with self.assertRaises(ValueError):
            render({"other_customer_database": []})
        with self.assertRaises(ValueError):
            render({"users": [{"_id": 1}]})


if __name__ == "__main__":
    unittest.main()
