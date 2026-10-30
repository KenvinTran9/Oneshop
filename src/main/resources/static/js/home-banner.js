(() => {
    "use strict";
    document.querySelectorAll("[data-home-carousel]").forEach(carousel => {
        const slides = [...carousel.querySelectorAll("[data-banner-slide]")];
        const controls = carousel.querySelector("[data-banner-controls]");
        const dots = [...carousel.querySelectorAll("[data-banner-dot]")];
        const toggle = carousel.querySelector("[data-banner-toggle]");
        const status = carousel.querySelector("[data-banner-status]");
        if (slides.length < 2 || !controls || !toggle) return;
        const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
        let current = 0, timer, userPaused = false, hovering = false, focused = false, visible = true;
        let animation;
        function stop() { window.clearTimeout(timer); timer = undefined; }
        function schedule() {
            stop();
            if (!userPaused && !reduced.matches && !hovering && !focused && visible && !document.hidden)
                timer = window.setTimeout(() => show(current + 1, false), 5000);
        }
        function show(index, manual = true) {
            animation?.cancel();
            current = (index + slides.length) % slides.length;
            slides.forEach((slide, i) => { slide.hidden = i !== current; });
            dots.forEach((dot, i) => {
                if (i === current) dot.setAttribute("aria-current", "true");
                else dot.removeAttribute("aria-current");
            });
            if (!reduced.matches && typeof slides[current].animate === "function") {
                animation = slides[current].animate([{ opacity: .4 }, { opacity: 1 }], { duration: 300, easing: "ease-out" });
                animation.finished.catch(() => {});
            }
            if (manual) status.textContent = slides[current].getAttribute("aria-label");
            schedule();
        }
        function updateToggle() {
            const paused = userPaused || reduced.matches;
            toggle.setAttribute("aria-pressed", String(paused));
            toggle.setAttribute("aria-label", reduced.matches ? "Tự chuyển banner đã tắt theo cài đặt giảm chuyển động" : paused ? "Tiếp tục chuyển banner" : "Tạm dừng chuyển banner");
            toggle.querySelector("i").className = paused ? "bi bi-play-fill" : "bi bi-pause-fill";
            toggle.querySelector("span").textContent = reduced.matches ? "Tự chuyển đã tắt" : paused ? "Tiếp tục" : "Tạm dừng";
            toggle.disabled = reduced.matches;
            schedule();
        }
        carousel.querySelector("[data-banner-prev]").addEventListener("click", () => show(current - 1));
        carousel.querySelector("[data-banner-next]").addEventListener("click", () => show(current + 1));
        dots.forEach((dot, i) => dot.addEventListener("click", () => show(i)));
        toggle.addEventListener("click", () => {
            userPaused = !userPaused;
            // An explicit restart takes effect even while the play button still has focus.
            if (!userPaused) { focused = false; hovering = false; }
            updateToggle();
        });
        carousel.addEventListener("pointerenter", event => { if (event.pointerType === "mouse") { hovering = true; stop(); } });
        carousel.addEventListener("pointerleave", () => { hovering = false; schedule(); });
        carousel.addEventListener("focusin", () => { focused = true; stop(); });
        carousel.addEventListener("focusout", event => {
            if (!carousel.contains(event.relatedTarget)) { focused = false; schedule(); }
        });
        controls.addEventListener("keydown", event => {
            if (event.altKey || event.ctrlKey || event.metaKey) return;
            if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
                event.preventDefault(); show(current + (event.key === "ArrowLeft" ? -1 : 1));
            }
        });
        document.addEventListener("visibilitychange", schedule);
        reduced.addEventListener("change", () => { animation?.cancel(); updateToggle(); });
        if ("IntersectionObserver" in window) {
            new IntersectionObserver(entries => { visible = entries[0].isIntersecting; schedule(); }, { threshold: .15 }).observe(carousel);
        }
        controls.hidden = false;
        updateToggle();
    });
})();
