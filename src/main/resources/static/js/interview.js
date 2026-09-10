document.querySelectorAll('[data-busy-form]').forEach(form => {
    form.addEventListener('submit', event => {
        if (form.dataset.submitting === 'true') {
            event.preventDefault();
            return;
        }
        form.dataset.submitting = 'true';
        form.setAttribute('aria-busy', 'true');
        const status = form.querySelector('.busy-message');
        if (status) status.hidden = false;
    });
});
window.addEventListener('pageshow', () => {
    document.querySelectorAll('[data-busy-form]').forEach(form => {
        delete form.dataset.submitting;
        form.removeAttribute('aria-busy');
        const status = form.querySelector('.busy-message');
        if (status) status.hidden = true;
    });
});
document.getElementById("simplify-question")?.addEventListener("click", async (event) => {
    const button = event.currentTarget;
    const output = document.getElementById("simplified-question");
    button.disabled = true;
    output.textContent = "Finding simpler words…";
    try {
        const response = await fetch(button.dataset.url, {
            method: "POST",
            headers: {"Content-Type": "application/json", "X-CSRF-TOKEN": document.querySelector('input[name="_csrf"]')?.value ?? ""},
            body: JSON.stringify({selectedText: document.getElementById("question-text").textContent})
        });
        const result = await response.json();
        output.textContent = response.ok ? result.simplifiedText : (result.error || "Please try again.");
    } catch {
        output.textContent = "The wording helper is unavailable. Your answer is still here.";
    } finally {
        button.disabled = false;
    }
});
