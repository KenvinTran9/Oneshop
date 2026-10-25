(() => {
    "use strict";
    const STORAGE_KEY = "oneshop.cart.v1";
    let memoryCart = [];
    let storageWritable = true;
    let lastRemoved = null;
    let toastTimer;
    const money = value => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
    const quantity = value => Math.min(99, Math.max(1, Math.trunc(Number(value)) || 1));
    const itemKey = item => JSON.stringify([item.sku, item.size, item.shade]);
    function normalizeItem(value) {
        if (!value || !/^CATALOG-\d{3}$/.test(value.sku) || typeof value.name !== "string") return null;
        const price = Number(value.price);
        if (!Number.isFinite(price) || price <= 0 || price > 1e9) return null;
        return {
            sku: value.sku, name: value.name.slice(0, 180), price: Math.round(price),
            quantity: quantity(value.quantity), size: String(value.size || "").slice(0, 100),
            shade: String(value.shade || "").slice(0, 100),
            image: /^\/images\/[a-z0-9/_.-]+$/i.test(value.image || "") ? value.image : "/images/product-placeholder.svg",
            url: /^\/products\/[a-z0-9-]+$/.test(value.url || "") ? value.url : "/products"
        };
    }
    function normalizeCart(value) {
        const merged = new Map();
        if (!Array.isArray(value)) return [];
        value.slice(0, 200).forEach(raw => {
            const item = normalizeItem(raw);
            if (!item) return;
            const key = itemKey(item);
            const existing = merged.get(key);
            if (existing) existing.quantity = quantity(existing.quantity + item.quantity);
            else merged.set(key, item);
        });
        return Array.from(merged.values());
    }
    function read() {
        try {
            if (storageWritable) {
                const raw = window.localStorage.getItem(STORAGE_KEY);
                memoryCart = normalizeCart(raw ? JSON.parse(raw) : []);
            }
        } catch (_) { /* Private browsing or damaged storage: keep a usable in-memory cart. */ }
        return memoryCart.map(item => ({ ...item }));
    }
    function totals(items) {
        return items.reduce((total, item) => ({ count: total.count + item.quantity, subtotal: total.subtotal + item.price * item.quantity }), { count: 0, subtotal: 0 });
    }
    function write(items) {
        memoryCart = normalizeCart(items);
        try { window.localStorage.setItem(STORAGE_KEY, JSON.stringify(memoryCart)); } catch (_) { storageWritable = false; }
        window.dispatchEvent(new CustomEvent("oneshop:cart-updated"));
        return read();
    }
    function add(raw) {
        const item = normalizeItem(raw);
        if (!item) return false;
        const items = read();
        const existing = items.find(value => itemKey(value) === itemKey(item));
        if (existing) existing.quantity = quantity(existing.quantity + item.quantity);
        else items.push(item);
        write(items);
        return true;
    }
    function setQuantity(key, value) {
        const items = read();
        const item = items.find(row => itemKey(row) === key);
        if (item) item.quantity = quantity(value);
        write(items);
    }
    function remove(key) {
        const items = read();
        lastRemoved = items.find(item => itemKey(item) === key) || null;
        write(items.filter(item => itemKey(item) !== key));
    }
    window.OneShopCart = { read, add, setQuantity, remove, totals, itemKey, normalizeCart, quantity };

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }
    function notify(message, allowUndo = false) {
        const toast = document.querySelector("[data-cart-toast]");
        if (!toast) return;
        toast.querySelector("[data-toast-message]").textContent = message;
        toast.querySelector("[data-undo-removal]").hidden = !allowUndo;
        toast.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { toast.hidden = true; }, 7000);
    }
    function render() {
        const items = read();
        const total = totals(items);
        document.querySelectorAll("[data-cart-count]").forEach(node => {
            node.textContent = String(total.count);
            node.hidden = total.count === 0;
        });
        const list = document.querySelector("[data-cart-list]");
        if (!list) return;
        document.querySelector("[data-cart-empty]").hidden = items.length !== 0;
        document.querySelector("[data-cart-content]").hidden = items.length === 0;
        document.querySelector("[data-cart-count-label]").textContent = total.count ? ` (${total.count})` : "";
        document.querySelector("[data-cart-subtotal]").textContent = money(total.subtotal);
        document.querySelector("[data-cart-total]").textContent = money(total.subtotal);
        list.replaceChildren();
        items.forEach(item => {
            const key = itemKey(item);
            const row = element("article", "cart-item");
            row.dataset.cartKey = key;
            const imageLink = element("a", "cart-item-image");
            imageLink.href = item.url;
            const image = element("img"); image.src = item.image; image.alt = item.name;
            imageLink.append(image);
            const info = element("div", "cart-item-info");
            const name = element("a", "cart-item-name", item.name); name.href = item.url;
            info.append(name, element("p", "cart-item-options", [item.size, item.shade].filter(Boolean).join(" · ")),
                element("p", "cart-item-unit-price", money(item.price)));
            const controls = element("div", "cart-item-controls");
            const stepper = element("div", "quantity-stepper compact");
            const minus = element("button", "", "−"); minus.type = "button"; minus.dataset.cartStep = "-1";
            minus.disabled = item.quantity <= 1; minus.setAttribute("aria-label", `Giảm số lượng ${item.name}`);
            const input = element("input"); input.type = "number"; input.min = "1"; input.max = "99";
            input.value = String(item.quantity); input.dataset.cartQuantity = ""; input.setAttribute("aria-label", `Số lượng ${item.name}`);
            const plus = element("button", "", "+"); plus.type = "button"; plus.dataset.cartStep = "1";
            plus.disabled = item.quantity >= 99; plus.setAttribute("aria-label", `Tăng số lượng ${item.name}`);
            stepper.append(minus, input, plus);
            const removeButton = element("button", "cart-remove", "Xóa"); removeButton.type = "button";
            removeButton.dataset.cartRemove = ""; removeButton.setAttribute("aria-label", `Xóa ${item.name} khỏi giỏ hàng`);
            controls.append(stepper, removeButton); info.append(controls);
            row.append(imageLink, info, element("strong", "cart-item-total", money(item.price * item.quantity)));
            list.append(row);
        });
    }
    function updateQuantityControl(form) {
        const input = form.querySelector("[data-quantity]");
        if (!input) return;
        const number = quantity(input.value); input.value = String(number);
        form.querySelector('[data-quantity-step="-1"]').disabled = number <= 1;
        form.querySelector('[data-quantity-step="1"]').disabled = number >= 99;
    }
    document.addEventListener("DOMContentLoaded", () => {
        render();
        document.querySelectorAll("[data-product-form]").forEach(updateQuantityControl);
        if (new URLSearchParams(window.location.search).get("checkout") === "1" && read().length) {
            document.querySelector("[data-checkout-dialog]")?.showModal();
        }
        document.addEventListener("submit", event => {
            if (event.target.matches(".checkout-form")) { event.preventDefault(); return; }
            const form = event.target.closest("[data-product-form]");
            if (!form) return;
            event.preventDefault();
            if (!form.reportValidity()) return;
            const fields = new FormData(form);
            const item = { ...form.dataset, quantity: fields.get("quantity"), size: fields.get("size"), shade: fields.get("shade") };
            if (!add(item)) { notify("Chưa thể thêm sản phẩm. Vui lòng thử lại."); return; }
            if (event.submitter?.hasAttribute("data-buy-now")) window.location.assign("/cart?checkout=1");
            else notify("Đã thêm sản phẩm vào giỏ hàng của bạn.");
        });
        document.addEventListener("click", event => {
            const quick = event.target.closest("[data-quick-add]");
            if (quick) {
                const card = quick.closest("[data-product-card]");
                if (card && add({ ...card.dataset, quantity: 1, shade: "" })) notify("Đã thêm sản phẩm vào giỏ hàng của bạn.");
                return;
            }
            const step = event.target.closest("[data-quantity-step]");
            if (step) {
                const form = step.closest("[data-product-form]");
                const input = form.querySelector("[data-quantity]");
                input.value = String(quantity(Number(input.value) + Number(step.dataset.quantityStep)));
                updateQuantityControl(form); return;
            }
            const cartStep = event.target.closest("[data-cart-step]");
            if (cartStep) {
                const row = cartStep.closest("[data-cart-key]");
                setQuantity(row.dataset.cartKey, Number(row.querySelector("[data-cart-quantity]").value) + Number(cartStep.dataset.cartStep)); return;
            }
            const removeButton = event.target.closest("[data-cart-remove]");
            if (removeButton) { remove(removeButton.closest("[data-cart-key]").dataset.cartKey); notify("Đã bỏ sản phẩm khỏi giỏ hàng.", true); return; }
            if (event.target.closest("[data-undo-removal]") && lastRemoved) { add(lastRemoved); lastRemoved = null; notify("Đã khôi phục sản phẩm trong giỏ hàng."); }
            if (event.target.closest("[data-dismiss-toast]")) document.querySelector("[data-cart-toast]").hidden = true;
            if (event.target.closest("[data-open-checkout]") && read().length) document.querySelector("[data-checkout-dialog]").showModal();
            if (event.target.closest("[data-close-checkout]")) document.querySelector("[data-checkout-dialog]").close();
            if (event.target.closest("[data-image-zoom]")) document.querySelector("[data-product-zoom]").showModal();
            if (event.target.closest("[data-close-zoom]")) document.querySelector("[data-product-zoom]").close();
        });
        document.addEventListener("change", event => {
            if (event.target.matches("[data-auto-submit]")) event.target.form.requestSubmit();
            if (event.target.matches("[data-quantity]")) updateQuantityControl(event.target.closest("[data-product-form]"));
            if (event.target.matches("[data-cart-quantity]")) setQuantity(event.target.closest("[data-cart-key]").dataset.cartKey, event.target.value);
            if (event.target.matches('input[name="shade"]')) {
                event.target.closest("[data-product-form]").querySelector("[data-selected-shade]").textContent = event.target.value;
            }
        });
        document.querySelectorAll("dialog").forEach(dialog => dialog.addEventListener("click", event => {
            if (event.target === dialog) {
                const bounds = dialog.getBoundingClientRect();
                if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) dialog.close();
            }
        }));
    });
    window.addEventListener("oneshop:cart-updated", render);
    window.addEventListener("storage", event => { if (event.key === STORAGE_KEY || event.key === null) render(); });
})();
