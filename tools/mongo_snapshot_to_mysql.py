"""Render a private canonical-EJSON OneShop snapshot into transactional MySQL inserts.

This does not connect to either database. Import only into a verified empty schema
created from the current JPA mappings. Keep both input and output out of Git.
"""
import argparse
import datetime as dt
from decimal import Decimal
import json
from pathlib import Path
import re


TABLES = {
    "roles": ["name", "description"],
    "stores": ["code", "name", "address", "district", "city", "phone", "latitude", "longitude"],
    "categories": ["name", "slug", "description"],
    "brands": ["name", "slug", "description"],
    "users": ["email", "password", "fullName", "phone", "role", "store"],
    "products": [
        "name", "slug", "sku", "shortDescription", "description", "price", "salePrice", "imageUrl",
        "priceSourceUrl", "imageSourceUrl", "priceVariant", "priceCheckedOn", "priceFrom", "priceCurrency",
        "brandCountry", "packageSize", "ingredients", "suitableFor", "directions", "precautions",
        "sourceUrl", "imageSearchUrl", "status", "category", "brand",
    ],
    "store_stocks": ["store", "product", "quantity", "storePrice"],
    "shopping_carts": ["user"],
    "orders": ["code", "customer", "store", "status", "customerName", "customerPhone", "shippingAddress", "totalAmount"],
    "order_items": ["order", "product", "quantity", "unitPrice", "lineTotal"],
}
REFERENCES = {"role", "store", "category", "brand", "product", "user", "order", "customer", "cart"}
BASE = ["id", "version", "active", "createdAt", "updatedAt"]


def decode(value):
    if isinstance(value, list):
        return [decode(item) for item in value]
    if not isinstance(value, dict):
        return value
    for key in ("$numberLong", "$numberInt"):
        if key in value:
            return int(value[key])
    for key in ("$numberDecimal", "$numberDouble"):
        if key in value:
            return Decimal(value[key])
    if "$date" in value:
        stamp = value["$date"]
        date = (dt.datetime.fromtimestamp(int(stamp["$numberLong"]) / 1000, dt.timezone.utc)
                if isinstance(stamp, dict) else dt.datetime.fromisoformat(stamp.replace("Z", "+00:00")))
        # Mongo's LocalDateTime converter used this shop's Asia/Saigon timezone.
        return date.astimezone(dt.timezone(dt.timedelta(hours=7))).strftime("%Y-%m-%d %H:%M:%S.%f")
    return {key: decode(item) for key, item in value.items()}


def column(name):
    if name in REFERENCES:
        return name + "_id"
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, Decimal)):
        return str(value)
    if not isinstance(value, str):
        raise ValueError("Unsupported snapshot value type")
    # Hex strings preserve Unicode, quotes and backslashes regardless of sql_mode.
    return "CONVERT(X'" + value.encode("utf-8").hex() + "' USING utf8mb4)"


def statement(table, fields, values):
    return ("INSERT INTO `" + table + "` (" + ",".join("`" + column(field) + "`" for field in fields)
            + ") VALUES (" + ",".join(literal(value) for value in values) + ");")


def render(snapshot):
    unknown = set(snapshot) - set(TABLES)
    if unknown:
        raise ValueError("Unexpected snapshot collections: " + ", ".join(sorted(unknown)))
    lines = ["SET NAMES utf8mb4;", "SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';", "START TRANSACTION;"]
    counts = {}
    for table, fields in TABLES.items():
        documents = snapshot.get(table, [])
        counts[table] = len(documents)
        for document in documents:
            identity = document.get("_id")
            if not isinstance(identity, int) or identity < 1:
                raise ValueError("Expected positive numeric OneShop IDs")
            base = [identity, 0, document.get("active", True), document.get("createdAt"), document.get("updatedAt")]
            if not base[3] or not base[4]:
                raise ValueError("Missing audit dates in " + table)
            lines.append(statement(table, BASE + fields, base + [document.get(field) for field in fields]))
        if table == "products":
            sizes = shades = 0
            for document in documents:
                for order, size in enumerate(document.get("sizes", [])):
                    lines.append(statement("product_sizes", ["product", "sizeOrder", "sizeLabel"], [document["_id"], order, size]))
                    sizes += 1
                for order, shade in enumerate(document.get("shades", [])):
                    lines.append(statement("product_shades",
                        ["product", "shadeOrder", "shadeLabel", "shadeColor", "shadeImageUrl", "shadeImageAlt", "shadeImageType"],
                        [document["_id"], order] + [shade.get(field) for field in ("label", "color", "imageUrl", "imageAlt", "imageType")]))
                    shades += 1
            counts.update(product_sizes=sizes, product_shades=shades)
        if table == "shopping_carts":
            item_id = 0
            for cart in documents:
                for item in cart.get("items", []):
                    item_id += 1
                    lines.append(statement("shopping_cart_items",
                        BASE + ["cart", "product", "quantity", "selectedSize", "selectedShade"],
                        [item_id, 0, True, cart["createdAt"], cart["updatedAt"], cart["_id"],
                         item["product"], item["quantity"], item.get("selectedSize", ""), item.get("selectedShade", "")]))
            counts["shopping_cart_items"] = item_id
    lines.append("COMMIT;")
    return "\n".join(lines) + "\n", counts


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshot", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    if args.snapshot.resolve() == args.output.resolve() or args.output.exists():
        raise SystemExit("Refusing to overwrite an existing file")
    data = decode(json.loads(args.snapshot.read_text(encoding="utf-8-sig")))
    sql, counts = render(data)
    args.output.write_text(sql, encoding="utf-8")
    print(json.dumps({"rows": counts}, ensure_ascii=False))
