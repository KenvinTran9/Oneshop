document.addEventListener("DOMContentLoaded", () => {
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

    // 2. Xu ly tuong tac them vao gio hang
    document.addEventListener("click", (event) => {
        const addButton = event.target.closest("[data-add-cart], .btn-add-bag");
        if (!addButton) return;
        
        addButton.classList.add("btn-added");
        const icon = addButton.querySelector("i");
        if (icon) {
            icon.className = "bi bi-check2";
        }
        setTimeout(() => {
            addButton.classList.remove("btn-added");
            if (icon) icon.className = "bi bi-plus";
        }, 1500);
    });
});
