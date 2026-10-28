// Progressive enhancement only: prices and quantities are applied by the server on submit.
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-server-quantity-form]').forEach(form => {
        const input = form.querySelector('input[name="quantity"]');
        const initial = input.value;
        const buttons = form.querySelectorAll('[data-server-step]');
        const hint = form.querySelector('[data-quantity-hint]');
        const refresh = () => {
            const value = Number(input.value);
            buttons.forEach(button => {
                button.hidden = false;
                button.disabled = button.dataset.serverStep === '-1' ? value <= 1 : value >= 99;
            });
            hint.hidden = input.value === initial;
        };
        buttons.forEach(button => button.addEventListener('click', () => {
            const value = Number(input.value);
            input.value = String(Math.max(1, Math.min(99, (Number.isFinite(value) ? Math.trunc(value) : 1) + Number(button.dataset.serverStep))));
            refresh();
        }));
        input.addEventListener('input', refresh);
        refresh();
    });
});
