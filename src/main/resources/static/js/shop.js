// Storefront controls; cart contents, pricing and checkout are handled by the backend.
(() => {
    'use strict';
    const quantity = value => Math.min(99, Math.max(1, Math.trunc(Number(value)) || 1));
    function updateQuantity(form) {
        const input = form?.querySelector('[data-quantity]');
        if (!input) return;
        const value = quantity(input.value);
        input.value = String(value);
        form.querySelectorAll('[data-quantity-step]').forEach(button => {
            button.disabled = Number(button.dataset.quantityStep) < 0 ? value <= 1 : value >= 99;
        });
    }
    function openDialog(dialog) {
        if (!dialog) return;
        if (window.OneShopMotion) window.OneShopMotion.openDialog(dialog);
        else dialog.showModal();
    }
    function closeDialog(dialog) {
        if (!dialog) return;
        if (window.OneShopMotion) window.OneShopMotion.closeDialog(dialog);
        else dialog.close();
    }
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-product-form]').forEach(updateQuantity);
        document.addEventListener('click', event => {
            const step = event.target.closest('[data-quantity-step]');
            if (step) {
                const form = step.closest('[data-product-form]');
                const input = form?.querySelector('[data-quantity]');
                if (input) { input.value = String(quantity(Number(input.value) + Number(step.dataset.quantityStep))); updateQuantity(form); }
                return;
            }
            if (event.target.closest('[data-image-zoom]')) openDialog(document.querySelector('[data-product-zoom]'));
            if (event.target.closest('[data-close-zoom]')) closeDialog(document.querySelector('[data-product-zoom]'));
        });
        document.addEventListener('change', event => {
            if (event.target.matches('[data-auto-submit]')) event.target.form.requestSubmit();
            if (event.target.matches('[data-quantity]')) updateQuantity(event.target.closest('[data-product-form]'));
            if (event.target.matches('input[name="shade"]')) {
                const label = event.target.closest('[data-product-form]')?.querySelector('[data-selected-shade]');
                if (label) label.textContent = event.target.value;
            }
        });
        document.querySelectorAll('[data-product-zoom]').forEach(dialog => dialog.addEventListener('click', event => {
            if (event.target !== dialog) return;
            const bounds = dialog.getBoundingClientRect();
            if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom)
                closeDialog(dialog);
        }));
    });
})();
