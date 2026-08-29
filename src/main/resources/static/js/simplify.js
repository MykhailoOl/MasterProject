(() => {
  if (!simplifyEnabled) {
    return;
  }

  const questionEl = document.getElementById("question-text");
  const menu = document.getElementById("simplify-menu");
  const action = document.getElementById("simplify-action");
  const dialog = document.getElementById("simplify-dialog");
  const output = document.getElementById("simplify-output");
  const errorEl = document.getElementById("simplify-error");
  if (!questionEl || !menu || !action || !dialog) {
    return;
  }

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  let selectedText = "";

  questionEl.addEventListener("contextmenu", (event) => {
    const selection = window.getSelection();
    if (!selection || selection.isCollapsed || !questionEl.contains(selection.anchorNode)) {
      menu.hidden = true;
      return;
    }
    selectedText = selection.toString().trim();
    if (!selectedText) {
      menu.hidden = true;
      return;
    }
    event.preventDefault();
    menu.hidden = false;
    menu.style.left = `${event.pageX}px`;
    menu.style.top = `${event.pageY}px`;
  });

  document.addEventListener("click", () => {
    menu.hidden = true;
  });

  action.addEventListener("click", async () => {
    menu.hidden = true;
    output.textContent = "Simplifying…";
    errorEl.hidden = true;
    dialog.showModal();
    try {
      const headers = {
        "Content-Type": "application/json",
      };
      if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
      }
      const response = await fetch(`/api/projects/${projectId}/simplify`, {
        method: "POST",
        headers,
        body: JSON.stringify({ selectedText }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || "Simplify failed");
      }
      output.textContent = data.simplifiedText;
    } catch (error) {
      output.textContent = "";
      errorEl.hidden = false;
      errorEl.textContent = error.message || "Simplify failed";
    }
  });
})();
