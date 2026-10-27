(() => {
    "use strict";
    const localImage = value => /^\/images\/[a-z0-9/_.-]+$/i.test(value || "");
    document.addEventListener("DOMContentLoaded", () => {
        const gallery = document.querySelector("[data-product-gallery]");
        const form = document.querySelector("[data-product-form]");
        if (!gallery || !form) return;
        const image = gallery.querySelector("[data-gallery-image]");
        const caption = gallery.querySelector("[data-gallery-caption]");
        const originalButton = gallery.querySelector("[data-gallery-original]");
        const shadeButton = gallery.querySelector("[data-gallery-shade]");
        const status = form.querySelector("[data-shade-photo-status]");
        const mobilePreview = form.querySelector("[data-shade-mobile-preview]");
        const mobileImage = form.querySelector("[data-shade-mobile-image]");
        if (!originalButton || !shadeButton) return;
        const original = { url: originalButton.dataset.image, alt: originalButton.dataset.alt };
        const zoom = document.querySelector("[data-product-zoom] img");
        const zoomCaption = document.querySelector("[data-zoom-caption]");
        let selected = null;
        let showingShade = false;

        function show(shade) {
            const photo = shade && selected ? selected : original;
            showingShade = Boolean(shade && selected);
            if (image.getAttribute("src") !== photo.url) image.src = photo.url;
            image.alt = photo.alt;
            caption.textContent = showingShade ? photo.caption : "Hình ảnh sản phẩm";
            if (zoom) { zoom.src = photo.url; zoom.alt = photo.alt; }
            if (zoomCaption) zoomCaption.textContent = caption.textContent;
            originalButton.classList.toggle("active", !showingShade);
            shadeButton.classList.toggle("active", showingShade);
            originalButton.setAttribute("aria-pressed", String(!showingShade));
            shadeButton.setAttribute("aria-pressed", String(showingShade));
        }

        function choose(input) {
            const data = input.dataset;
            const available = localImage(data.shadeImage);
            const chart = data.shadeImageType === "chart";
            selected = available ? {
                url: data.shadeImage,
                alt: data.shadeAlt || `${gallery.dataset.productName} — ${input.value}`,
                caption: chart ? `Bảng màu sản phẩm · Đã chọn ${input.value}` : `Ảnh màu ${input.value}`
            } : null;
            shadeButton.disabled = !available;
            if (mobilePreview) mobilePreview.hidden = !available;
            if (mobileImage && available) { mobileImage.src = selected.url; mobileImage.alt = selected.alt; }
            shadeButton.textContent = chart ? "Bảng màu sản phẩm" : "Ảnh màu đã chọn";
            // A shared shade chart must never become the cart's product thumbnail.
            form.dataset.image = available && !chart ? selected.url : original.url;
            if (status) status.textContent = available
                ? chart ? `Tìm mã ${input.value} trên bảng màu. Bấm ảnh lớn để phóng to.` : `Đang xem ảnh màu ${input.value}. Bấm ảnh lớn để xem gần hơn.`
                : `Ảnh riêng của màu ${input.value} đang được cập nhật. Hiện đang xem ảnh sản phẩm.`;
            show(available);
        }

        originalButton.addEventListener("click", () => show(false));
        shadeButton.addEventListener("click", () => { if (selected) show(true); });
        mobilePreview?.addEventListener("click", () => { if (selected) show(true); });
        form.addEventListener("change", event => {
            if (event.target.matches('input[name="shade"]')) choose(event.target);
        });
        image.addEventListener("error", () => {
            if (!showingShade) return;
            selected = null;
            shadeButton.disabled = true;
            if (mobilePreview) mobilePreview.hidden = true;
            form.dataset.image = original.url;
            if (status) status.textContent = "Ảnh màu chưa tải được. Bạn đang xem ảnh sản phẩm.";
            show(false);
        });
        const checked = form.querySelector('input[name="shade"]:checked');
        if (checked) choose(checked);
    });
})();
