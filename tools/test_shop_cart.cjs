// Run with: node tools/test_shop_cart.cjs
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = readFileSync('src/main/resources/static/js/shop.js', 'utf8');
function session(storage) {
    const sandbox = { window: { localStorage: storage, addEventListener() {}, dispatchEvent() {} },
        document: { addEventListener() {} }, Intl, CustomEvent: class {}, setTimeout, clearTimeout };
    vm.runInNewContext(source, sandbox);
    return sandbox.window.OneShopCart;
}
let saved;
const storage = { getItem: () => saved, setItem: (_, value) => { saved = value; } };
const cart = session(storage);
const item = { sku: 'CATALOG-063', name: 'Soft Pinch Liquid Blush', price: 675000,
    image: '/images/catalog/catalog-063.jpg', url: '/products/rare-beauty-soft-pinch-liquid-blush',
    size: '7,5 ml', shade: 'Hope', quantity: 2 };
assert.equal(cart.add(item), true);
cart.add({ ...item, quantity: 3 });
assert.equal(cart.read().length, 1);
assert.equal(cart.read()[0].quantity, 5);
cart.add({ ...item, quantity: 1, image: '/images/catalog/shades/catalog-063-hope.png' });
assert.equal(cart.read()[0].image, '/images/catalog/shades/catalog-063-hope.png');
cart.setQuantity(cart.itemKey(cart.read()[0]), 5);
cart.add({ ...item, shade: 'Happy', quantity: 1 });
assert.equal(cart.read().length, 2);
assert.equal(cart.totals(cart.read()).subtotal, 4050000);
assert.equal(cart.totals(cart.read()).count, 6);
const reloaded = session(storage);
assert.equal(reloaded.read().length, 2);
const key = reloaded.itemKey(reloaded.read()[0]);
reloaded.setQuantity(key, 1000);
assert.equal(reloaded.read()[0].quantity, 99);
reloaded.setQuantity(key, -4);
assert.equal(reloaded.read()[0].quantity, 1);
reloaded.remove(key);
assert.equal(reloaded.read().length, 1);
assert.equal(reloaded.read()[0].shade, 'Happy');
assert.equal(cart.add({ ...item, price: -1 }), false);
assert.equal(cart.add({ ...item, sku: 'INVALID' }), false);
assert.equal(cart.normalizeCart({}).length, 0);
const sanitized = cart.normalizeCart([{ ...item, image: 'https://evil.invalid/a', url: 'javascript:alert(1)' }])[0];
assert.equal(sanitized.image, '/images/product-placeholder.svg');
assert.equal(sanitized.url, '/products');
saved = '{invalid-json';
assert.equal(session(storage).read().length, 0);
const quotaStorage = { getItem: () => '[]', setItem: () => { throw Error('Quota exceeded'); } };
const privateCart = session(quotaStorage);
assert.equal(privateCart.add(item), true);
assert.equal(privateCart.read()[0].quantity, 2);
privateCart.add(item);
assert.equal(privateCart.read()[0].quantity, 4);
console.log('Cart tests passed: variant merging, totals, persistence, limits, removal, invalid data and storage fallback.');
