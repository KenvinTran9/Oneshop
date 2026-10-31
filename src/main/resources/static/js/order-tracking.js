/* Poll only the signed-in customer's order; hidden tabs do not generate traffic. */
document.addEventListener("DOMContentLoaded", () => {
    const root = document.querySelector("[data-order-tracking]");
    if (!root) return;
    const notice = root.querySelector("[data-progress-notice]");
    const url = root.dataset.progressUrl;
    const originalVersion = Number(root.dataset.orderVersion);
    let timer, inFlight = false, stopped = false, delay = 15000;
    let editing = false;
    root.querySelectorAll("form").forEach(form => form.addEventListener("input", () => { editing = true; }));
    const schedule = () => {
        clearTimeout(timer);
        if (!stopped && !document.hidden) timer = setTimeout(check, delay);
    };
    async function check() {
        if (document.hidden || inFlight || stopped) return;
        inFlight = true;
        try {
            const response = await fetch(url, { credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" } });
            if (response.status === 401 || response.status === 403 || response.status === 404 || response.redirected) {
                stopped = true; return;
            }
            if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) throw new Error("Progress unavailable");
            const progress = await response.json();
            if (Number(progress.version) !== originalVersion) {
                if (editing || root.querySelector("input:focus,textarea:focus,select:focus")) {
                    if (notice) notice.hidden = false;
                    stopped = true;
                } else {
                    stopped = true; window.location.reload();
                }
            }
            delay = 15000;
        } catch { delay = Math.min(delay * 2, 60000); }
        finally { inFlight = false; schedule(); }
    }
    document.addEventListener("visibilitychange", () => {
        clearTimeout(timer);
        if (!document.hidden && !stopped) { delay = 15000; check(); }
    });
    schedule();
});
