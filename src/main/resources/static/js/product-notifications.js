/* Poll only for signed-in visitors, with no traffic while the tab is hidden. */
(() => {
    let started = false, stopped = false, timer, inFlight = false, delay = 60000;
    const schedule = () => {
        clearTimeout(timer);
        if (started && !stopped && !document.hidden) timer = setTimeout(refresh, delay);
    };
    async function refresh() {
        if (document.hidden || inFlight || stopped) return;
        const bell = document.querySelector('[data-watch-bell]');
        const badge = document.querySelector('[data-watch-unread]');
        if (!bell || !badge) return;
        inFlight = true;
        try {
            const response = await fetch('/account/watches/notifications', {credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
            if (response.redirected || response.status === 401 || response.status === 403) { stopped = true; bell.hidden = true; return; }
            if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('Unavailable');
            const result = await response.json();
            const unread = Math.max(0, Number(result.unread) || 0);
            badge.textContent = unread > 99 ? '99+' : String(unread);
            badge.hidden = unread === 0;
            bell.setAttribute('aria-label', unread ? `Thông báo sản phẩm: ${unread} chưa đọc` : 'Thông báo sản phẩm');
            delay = 60000;
        } catch { delay = Math.min(delay * 2, 300000); }
        finally { inFlight = false; schedule(); }
    }
    window.addEventListener('oneshop-authenticated', () => {
        if (started) return;
        started = true;
        const bell = document.querySelector('[data-watch-bell]');
        if (bell) bell.hidden = false;
        refresh();
    });
    document.addEventListener('visibilitychange', () => { clearTimeout(timer); if (started && !document.hidden && !stopped) refresh(); });
})();
