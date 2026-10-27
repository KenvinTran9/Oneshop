// Run with: node tools/test_shade_gallery.cjs
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
function node(dataset = {}) {
    return { dataset, attrs: {}, events: {}, textContent: '', disabled: false,
        classList: { toggle() {} },
        addEventListener(name, callback) { this.events[name] = callback; },
        setAttribute(name, value) { this.attrs[name] = value; },
        getAttribute(name) { return name === 'src' ? this.src : this.attrs[name]; } };
}
const image = node(); image.src = '/images/catalog/product.jpg';
const caption = node(), original = node({ image: image.src, alt: 'Blush' }), shadeButton = node();
const status = node(), zoom = node(), zoomCaption = node();
const mobilePreview = node(), mobileImage = node();
const gallery = node({ productName: 'Blush' });
const form = node({ image: image.src });
gallery.querySelector = selector => ({ '[data-gallery-image]': image, '[data-gallery-caption]': caption,
    '[data-gallery-original]': original, '[data-gallery-shade]': shadeButton })[selector];
form.querySelector = selector => ({ '[data-shade-photo-status]': status,
    '[data-shade-mobile-preview]': mobilePreview, '[data-shade-mobile-image]': mobileImage })[selector];
const lookup = { '[data-product-gallery]': gallery, '[data-product-form]': form,
    '[data-product-zoom] img': zoom, '[data-zoom-caption]': zoomCaption };
const listeners = {};
vm.runInNewContext(readFileSync('src/main/resources/static/js/shade-gallery.js', 'utf8'), {
    document: { querySelector: selector => lookup[selector], addEventListener: (name, cb) => { listeners[name] = cb; } }
});
listeners.DOMContentLoaded();
function select(value, url, type = 'photo') {
    form.events.change({ target: { value, dataset: { shadeImage: url, shadeAlt: `Blush ${value}`, shadeImageType: type },
        matches: selector => selector === 'input[name="shade"]' } });
}
select('Hope', '/images/catalog/shades/hope.png');
assert.equal(image.src, '/images/catalog/shades/hope.png');
assert.equal(zoom.src, image.src);
assert.equal(zoom.alt, 'Blush Hope');
assert.equal(form.dataset.image, image.src);
assert.equal(mobileImage.src, image.src);
assert.equal(mobilePreview.hidden, false);
assert.equal(shadeButton.disabled, false);
assert.equal(shadeButton.attrs['aria-pressed'], 'true');
original.events.click();
assert.equal(image.src, original.dataset.image);
assert.equal(form.dataset.image, '/images/catalog/shades/hope.png', 'Changing the gallery view must retain the selected variant in cart');
mobilePreview.events.click();
assert.equal(zoom.src, '/images/catalog/shades/hope.png', 'The inline mobile preview must zoom the selected shade even after viewing the original');
shadeButton.events.click();
assert.equal(image.src, '/images/catalog/shades/hope.png');
select('Happy', '/images/catalog/shades/happy.png');
select('Joy', '/images/catalog/shades/joy.png');
assert.equal(image.src, '/images/catalog/shades/joy.png', 'Rapid selection must show the last chosen shade');
select('Pound Cake', '/images/catalog/shades/chart.webp', 'chart');
assert.equal(image.src, '/images/catalog/shades/chart.webp');
assert.equal(form.dataset.image, original.dataset.image, 'A shared chart is not a variant product thumbnail');
assert.match(caption.textContent, /Bảng màu/);
select('Missing', '');
assert.equal(image.src, original.dataset.image, 'Missing media must not keep the previous shade image');
assert.equal(shadeButton.disabled, true);
assert.equal(mobilePreview.hidden, true);
select('Unsafe', 'https://example.invalid/wrong.jpg');
assert.equal(image.src, original.dataset.image);
select('Hope', '/images/catalog/shades/hope.png');
image.events.error();
assert.equal(image.src, original.dataset.image);
assert.equal(zoom.src, original.dataset.image);
assert.equal(form.dataset.image, original.dataset.image);
assert.equal(shadeButton.disabled, true);
console.log('Shade gallery tests passed: selection, zoom, view toggles, rapid changes, chart labeling, missing/broken media and cart images.');
