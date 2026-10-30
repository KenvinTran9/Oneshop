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
    const searchToggle = document.querySelector("[data-header-search-toggle]");
    const searchPanel = document.getElementById("site-search-panel");
    if (searchToggle && searchPanel && headerSearch) {
        function setSearchOpen(open, focus = false) {
            searchPanel.hidden = !open;
            searchToggle.setAttribute("aria-expanded", String(open));
            searchToggle.setAttribute("aria-label", open ? "Đóng tìm kiếm" : "Mở tìm kiếm");
            if (focus) (open ? headerSearch : searchToggle).focus();
        }
        searchToggle.hidden = false;
        setSearchOpen(Boolean(headerSearch.value));
        searchToggle.addEventListener("click", () => setSearchOpen(searchPanel.hidden, true));
        searchPanel.addEventListener("keydown", event => {
            if (event.key === "Escape") { event.preventDefault(); setSearchOpen(false, true); }
        });
    }
    mainMenu?.querySelectorAll("a.nav-link").forEach(link => {
        const destination = new URL(link.href);
        const sameCategory = (destination.searchParams.get("category") || "") === (currentUrl.searchParams.get("category") || "");
        const isDetail = currentUrl.pathname.startsWith("/products/") && destination.pathname === "/products" && !destination.search;
        if ((destination.pathname === currentUrl.pathname && sameCategory) || isDetail) link.setAttribute("aria-current", "page");
        // The header is the catalog's single category menu. Keep the customer's search and ordering.
        if (currentUrl.pathname === "/products" && destination.pathname === "/products") {
            for (const key of ["q", "sort"]) {
                const value = currentUrl.searchParams.get(key);
                if (value) destination.searchParams.set(key, value);
            }
            link.href = destination.pathname + destination.search;
        }
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
    // One session-backed request supplies both account navigation and the cart badge.
    fetch("/account/header", {
        headers: { "Accept": "application/json" }
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        }
        return null;
    })
    .then(data => {
        if (cartCountBadge && data) {
            cartCountBadge.textContent = data.itemCount > 99 ? "99+" : String(data.itemCount);
            cartCountBadge.hidden = data.itemCount < 1;
        }
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
