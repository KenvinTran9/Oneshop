document.addEventListener("click", (event) => {
    const addButton = event.target.closest("[data-add-cart]");
    if (!addButton) {
        return;
    }

    addButton.classList.add("disabled");
    addButton.textContent = "Da them";
});
