(() => {
    "use strict";
    document.querySelectorAll("[data-home-carousel]").forEach(carousel => {
        const slides = [...carousel.querySelectorAll("[data-banner-slide]")];
        const controls = carousel.querySelector("[data-banner-controls]");
        const dots = [...carousel.querySelectorAll("[data-banner-dot]")];
        const status = carousel.querySelector("[data-banner-status]");
        if (slides.length < 2 || !controls) return;
        const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
        let current = 0, timer, hovering = false, focused = false, visible = true;
        let animation;
        function stop() { window.clearTimeout(timer); timer = undefined; }
        function schedule() {
            stop();
            if (!reduced.matches && !hovering && !focused && visible && !document.hidden)
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
        carousel.querySelector("[data-banner-prev]").addEventListener("click", () => show(current - 1));
        carousel.querySelector("[data-banner-next]").addEventListener("click", () => show(current + 1));
        dots.forEach((dot, i) => dot.addEventListener("click", () => show(i)));
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
        reduced.addEventListener("change", () => { animation?.cancel(); schedule(); });
        if ("IntersectionObserver" in window) {
            new IntersectionObserver(entries => { visible = entries[0].isIntersecting; schedule(); }, { threshold: .15 }).observe(carousel);
        }
        controls.hidden = false;
        schedule();
    });
})();
