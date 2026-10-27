document.addEventListener("DOMContentLoaded", () => {
    const headerSearch = document.getElementById("header-search");
    const mainMenu = document.getElementById("mainNav");
    const menuToggle = document.querySelector('[aria-controls="mainNav"]');
    const siteHeader = document.querySelector(".site-header");
    if (siteHeader) {
        const updateHeaderHeight = () => document.documentElement.style.setProperty("--site-header-height", `${siteHeader.offsetHeight}px`);
        updateHeaderHeight();
        if ("ResizeObserver" in window) new ResizeObserver(updateHeaderHeight).observe(siteHeader);
        else window.addEventListener("resize", updateHeaderHeight);
    }
    const currentUrl = new URL(window.location.href);
    if (headerSearch && currentUrl.pathname === "/products") {
        headerSearch.value = currentUrl.searchParams.get("q") || "";
    }
    mainMenu?.querySelectorAll("a.nav-link").forEach(link => {
        const destination = new URL(link.href);
        const sameCategory = (destination.searchParams.get("category") || "") === (currentUrl.searchParams.get("category") || "");
        const isDetail = currentUrl.pathname.startsWith("/products/") && destination.pathname === "/products" && !destination.search;
        if ((destination.pathname === currentUrl.pathname && sameCategory) || isDetail) link.setAttribute("aria-current", "page");
    });
    if (mainMenu && menuToggle) {
        mainMenu.addEventListener("shown.bs.collapse", () => menuToggle.setAttribute("aria-label", "Đóng menu"));
        mainMenu.addEventListener("hidden.bs.collapse", () => menuToggle.setAttribute("aria-label", "Mở menu"));
        siteHeader?.addEventListener("keydown", event => {
            if (event.key !== "Escape" || !mainMenu.classList.contains("show") || !window.bootstrap?.Collapse) return;
            event.preventDefault();
            window.bootstrap.Collapse.getOrCreateInstance(mainMenu, { toggle: false }).hide();
            menuToggle.focus();
        });
    }
    const cartCountBadge = document.querySelector("[data-cart-count]");
    fetch("/cart/count", { headers: { "Accept": "application/json" } })
        .then(response => response.ok ? response.json() : null)
        .then(data => {
            if (cartCountBadge && data) {
                cartCountBadge.textContent = data.itemCount > 99 ? "99+" : String(data.itemCount);
                cartCountBadge.hidden = data.itemCount < 1;
            }
        })
        .catch(() => {});
    // 1. Kiem tra trang thai dang nhap qua API /api/auth/me
    fetch("/api/auth/me", {
        headers: { "Accept": "application/json" }
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        }
        return null;
    })
    .then(data => {
        const guestNav = document.getElementById("navGuest");
        const userDropdown = document.getElementById("navUserDropdown");
        const userEmailLabel = document.getElementById("userEmailLabel");
        const userEmailFull = document.getElementById("userEmailFull");
        const adminMenuItem = document.getElementById("adminMenuItem");
        const adminNavLink = document.getElementById("adminNavLink");

        if (data && data.authenticated) {
            if (guestNav) guestNav.style.setProperty("display", "none", "important");
            if (userDropdown) userDropdown.style.removeProperty("display");
            
            const shortName = data.email ? (data.email.split("@")[0]) : "Tài khoản";
            if (userEmailLabel) userEmailLabel.textContent = shortName;
            if (userEmailFull) userEmailFull.textContent = data.email;

            if (data.isAdmin) {
                if (adminMenuItem) adminMenuItem.style.removeProperty("display");
                if (adminNavLink) adminNavLink.style.removeProperty("display");
            } else {
                if (adminMenuItem) adminMenuItem.style.setProperty("display", "none", "important");
                if (adminNavLink) adminNavLink.style.setProperty("display", "none", "important");
            }
        } else {
            if (guestNav) guestNav.style.removeProperty("display");
            if (userDropdown) userDropdown.style.setProperty("display", "none", "important");
            if (adminNavLink) adminNavLink.style.setProperty("display", "none", "important");
        }
    })
    .catch(() => {
        const guestNav = document.getElementById("navGuest");
        const userDropdown = document.getElementById("navUserDropdown");
        const adminNavLink = document.getElementById("adminNavLink");
        if (guestNav) guestNav.style.removeProperty("display");
        if (userDropdown) userDropdown.style.setProperty("display", "none", "important");
        if (adminNavLink) adminNavLink.style.setProperty("display", "none", "important");
    });
});
