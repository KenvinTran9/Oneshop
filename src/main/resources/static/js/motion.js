(() => {
    "use strict";
    const preference = window.matchMedia("(prefers-reduced-motion: reduce)");
    const running = new Map();
    const easing = "cubic-bezier(.2, .7, .2, 1)";
    let observer;
    const enabled = () => !preference.matches && !document.hidden;

    function cancel(node) {
        const previous = running.get(node);
        if (previous) { running.delete(node); previous.cancel(); }
    }

    function play(node, frames, options = {}) {
        if (!node) return null;
        cancel(node);
        if (!enabled() || typeof node.animate !== "function") return null;
        const animation = node.animate(frames, { duration: 240, easing, ...options });
        running.set(node, animation);
        const release = () => { if (running.get(node) === animation) running.delete(node); };
        // Canceling an interrupted interaction is expected, not an unhandled rejection.
        animation.finished.then(release, release);
        return animation;
    }

    function enter(node, delay = 0) {
        return play(node, [
            { opacity: 0, transform: "translateY(12px)" },
            { opacity: 1, transform: "translateY(0)" }
        ], { duration: 320, delay, fill: "backwards" });
    }

    function pulse(node) {
        play(node, [{ transform: "scale(1)" }, { transform: "scale(1.16)", offset: .4 },
            { transform: "scale(1)" }], { duration: 260 });
    }

    function valueChanged(node) {
        play(node, [{ opacity: .45 }, { opacity: 1 }], { duration: 180 });
    }

    function showToast(toast) {
        const wasHidden = toast.hidden;
        cancel(toast);
        toast.hidden = false;
        if (!wasHidden) {
            play(toast, [{ opacity: .75 }, { opacity: 1 }], { duration: 150 });
            return;
        }
        play(toast, [
            { opacity: 0, transform: "translate(-50%, 10px)" },
            { opacity: 1, transform: "translate(-50%, 0)" }
        ]);
    }

    async function hideToast(toast) {
        if (!toast || toast.hidden) return;
        const animation = play(toast, [
            { opacity: 1, transform: "translate(-50%, 0)" },
            { opacity: 0, transform: "translate(-50%, 8px)" }
        ], { duration: 140 });
        if (animation) {
            try { await animation.finished; } catch (_) { return; }
        }
        toast.hidden = true;
    }

    function openDialog(dialog) {
        if (!dialog) return;
        cancel(dialog);
        delete dialog.dataset.motionClosing;
        if (dialog.open) return;
        dialog.showModal();
        play(dialog, [{ opacity: 0, transform: "translateY(10px) scale(.985)" },
            { opacity: 1, transform: "translateY(0) scale(1)" }]);
    }

    async function closeDialog(dialog) {
        if (!dialog?.open || dialog.dataset.motionClosing) return;
        dialog.dataset.motionClosing = "true";
        const animation = play(dialog, [{ opacity: 1, transform: "translateY(0) scale(1)" },
            { opacity: 0, transform: "translateY(6px) scale(.985)" }], { duration: 140 });
        if (animation) {
            try { await animation.finished; } catch (_) { return; }
        }
        delete dialog.dataset.motionClosing;
        dialog.close();
    }

    function captureRows(list) {
        if (!enabled()) return new Map();
        return new Map(Array.from(list.children, row => [row, row.getBoundingClientRect().top]));
    }

    function settleRows(list, previous) {
        if (!enabled()) return;
        // Read every final position before starting animations; no read/write loop.
        const positions = Array.from(list.children, row => ({ row, top: row.getBoundingClientRect().top }));
        positions.forEach(({ row, top }) => {
            if (!previous.has(row)) { enter(row); return; }
            const delta = previous.get(row) - top;
            if (Math.abs(delta) > 1) play(row, [{ transform: `translateY(${delta}px)` }, { transform: "translateY(0)" }]);
        });
    }

    function finishAll() {
        for (const animation of running.values()) animation.finish();
    }

    preference.addEventListener("change", () => {
        if (preference.matches) { observer?.disconnect(); finishAll(); }
    });
    document.addEventListener("visibilitychange", () => { if (document.hidden) finishAll(); });
    document.addEventListener("DOMContentLoaded", () => {
        if (enabled()) {
            document.querySelectorAll(".shop-collection-heading-inner > div:first-child, .shop-search, .shop-product-gallery, .shop-purchase-panel, .home-heading, .hero-visual, .cart-page-heading, .cart-summary")
                .forEach((node, index) => enter(node, Math.min(index, 3) * 35));
            // Content is visible by default, including when JS/IntersectionObserver is unavailable.
            if ("IntersectionObserver" in window) {
                observer = new IntersectionObserver(entries => {
                    let index = 0;
                    entries.forEach(entry => {
                        if (!entry.isIntersecting) return;
                        observer.unobserve(entry.target);
                        enter(entry.target, Math.min(index++, 3) * 35);
                    });
                }, { threshold: .08 });
                document.querySelectorAll(".shop-card, .store-card").forEach(node => observer.observe(node));
            }
        }
        document.querySelectorAll(".shop-accordions details").forEach(details => {
            details.addEventListener("toggle", () => {
                if (details.open) play(details.querySelector("p"), [{ opacity: 0, transform: "translateY(-4px)" },
                    { opacity: 1, transform: "translateY(0)" }], { duration: 180 });
            });
        });
        document.querySelectorAll(".checkout-dialog, .product-zoom-dialog").forEach(dialog => {
            dialog.addEventListener("cancel", event => { event.preventDefault(); closeDialog(dialog); });
            dialog.addEventListener("close", () => { cancel(dialog); delete dialog.dataset.motionClosing; });
        });
    });

    window.OneShopMotion = { pulse, valueChanged, enter, showToast, hideToast, openDialog, closeDialog, captureRows, settleRows };
})();
