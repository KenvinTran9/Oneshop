document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-password-toggle]').forEach(button => {
        const input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;
        button.hidden = false;
        const confirmation = input.id === 'confirmPassword' ? ' xác nhận' : '';
        button.addEventListener('click', () => {
            const visible = input.type === 'password';
            input.type = visible ? 'text' : 'password';
            button.setAttribute('aria-pressed', String(visible));
            button.setAttribute('aria-label', `${visible ? 'Ẩn' : 'Hiện'} mật khẩu${confirmation}`);
            button.querySelector('i').className = visible ? 'bi bi-eye-slash' : 'bi bi-eye';
        });
    });
    const form = document.querySelector('[data-account-form]');
    const submitLabel = form?.querySelector('[type="submit"] span')?.textContent;
    const password = form?.querySelector('#password');
    const confirmation = form?.querySelector('#confirmPassword');
    const checkConfirmation = () => {
        confirmation?.setCustomValidity(confirmation.value && confirmation.value !== password.value
            ? 'Mật khẩu xác nhận chưa khớp.' : '');
    };
    password?.addEventListener('input', checkConfirmation);
    confirmation?.addEventListener('input', checkConfirmation);
    form?.addEventListener('submit', () => {
        const submit = form.querySelector('[type="submit"]');
        submit.disabled = true;
        submit.querySelector('span').textContent = 'Đang xử lý…';
    });
    // Re-enable the form when returning through the browser's back/forward cache.
    window.addEventListener('pageshow', () => {
        const submit = form?.querySelector('[type="submit"]');
        if (!submit) return;
        submit.disabled = false;
        submit.querySelector('span').textContent = submitLabel;
    });
});
