// Run with: node tools/test_shop_motion.cjs
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = readFileSync('src/main/resources/static/js/motion.js', 'utf8');
const callbacks = {};
const preference = { matches: false, addEventListener: (_, callback) => { callbacks.preference = callback; } };
const document = { hidden: false, addEventListener: (name, callback) => { callbacks[name] = callback; }, querySelectorAll: () => [] };
const sandbox = { window: { matchMedia: () => preference }, document, Map };
vm.runInNewContext(source, sandbox);
const motion = sandbox.window.OneShopMotion;
const flush = () => new Promise(resolve => setImmediate(resolve));
function node() {
    return {
        hidden: true, open: false, dataset: {}, animations: [],
        showModal() { this.open = true; }, close() { this.open = false; },
        animate(frames, options) {
            let resolve, reject;
            const animation = { frames, options, canceled: false, finished: new Promise((yes, no) => { resolve = yes; reject = no; }),
                finish() { resolve(); }, cancel() { this.canceled = true; reject(Error('Interrupted')); } };
            this.animations.push(animation);
            return animation;
        }
    };
}
(async () => {
    const dialog = node();
    motion.openDialog(dialog);
    assert.equal(dialog.open, true);
    const closing = motion.closeDialog(dialog);
    assert.equal(dialog.open, true, 'Keep native focus/modal behavior until exit finishes');
    motion.openDialog(dialog);
    await closing;
    assert.equal(dialog.open, true, 'Reopening must cancel a pending close');
    assert.equal(dialog.dataset.motionClosing, undefined);

    const finalClose = motion.closeDialog(dialog);
    preference.matches = true;
    callbacks.preference();
    await finalClose;
    assert.equal(dialog.open, false, 'Switching to reduced motion must complete pending closes');
    const count = dialog.animations.length;
    motion.openDialog(dialog);
    await motion.closeDialog(dialog);
    assert.equal(dialog.animations.length, count, 'Reduced motion skips JS animations');

    preference.matches = false;
    const badge = node();
    motion.pulse(badge);
    motion.pulse(badge);
    assert.equal(badge.animations[0].canceled, true, 'Rapid updates replace, rather than accumulate, effects');
    document.hidden = true;
    callbacks.visibilitychange();
    await flush();
    motion.pulse(badge);
    assert.equal(badge.animations.length, 2, 'Background tabs do not start effects');
    document.hidden = false;
    const unsupported = { open: false, dataset: {}, showModal() { this.open = true; }, close() { this.open = false; } };
    motion.openDialog(unsupported);
    assert.equal(unsupported.open, true);
    await motion.closeDialog(unsupported);
    assert.equal(unsupported.open, false);
    console.log('Motion tests passed: interruption, rapid updates, modal focus timing, live reduced-motion changes, background tabs and fallback.');
})().catch(error => { console.error(error); process.exitCode = 1; });
