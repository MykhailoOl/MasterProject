(() => {
  if (!simplifyEnabled) {
    return;
  }

  const questionEl = document.getElementById("question-text");
  const menu = document.getElementById("simplify-menu");
  const action = document.getElementById("simplify-action");
  const popup = document.getElementById("simplify-popup");
  const closeButton = document.getElementById("simplify-close");
  const output = document.getElementById("simplify-output");
  const errorEl = document.getElementById("simplify-error");
  if (!questionEl || !menu || !action || !popup || !closeButton) {
    return;
  }

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  let selectedText = "";
  let anchorRect = null;

  const hidePopup = () => {
    popup.hidden = true;
  };

  const positionPopup = (rect) => {
    const width = popup.offsetWidth || 280;
    const height = popup.offsetHeight || 96;
    const margin = 8;
    let left = window.scrollX + rect.left + rect.width / 2 - width / 2;
    let top = window.scrollY + rect.top - height - margin;
    left = Math.max(window.scrollX + 8, Math.min(left, window.scrollX + window.innerWidth - width - 8));
    if (top < window.scrollY + 8) {
      top = window.scrollY + rect.bottom + margin;
      popup.classList.add("simplify-popup-below");
    } else {
      popup.classList.remove("simplify-popup-below");
    }
    popup.style.left = `${left}px`;
    popup.style.top = `${top}px`;
  };

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
    const range = selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
    anchorRect = range ? range.getBoundingClientRect() : null;
    event.preventDefault();
    menu.hidden = false;
    menu.style.left = `${event.pageX}px`;
    menu.style.top = `${event.pageY}px`;
  });

  document.addEventListener("click", (event) => {
    if (!menu.hidden && !menu.contains(event.target)) {
      menu.hidden = true;
    }
    if (!popup.hidden && !popup.contains(event.target) && event.target !== action) {
      hidePopup();
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      hidePopup();
      menu.hidden = true;
    }
  });

  closeButton.addEventListener("click", (event) => {
    event.stopPropagation();
    hidePopup();
  });

  action.addEventListener("click", async (event) => {
    event.stopPropagation();
    menu.hidden = true;
    output.textContent = "Simplifying…";
    errorEl.hidden = true;
    popup.hidden = false;
    if (anchorRect) {
      positionPopup(anchorRect);
    }
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
      if (anchorRect) {
        positionPopup(anchorRect);
      }
    } catch (error) {
      output.textContent = "";
      errorEl.hidden = false;
      errorEl.textContent = error.message || "Simplify failed";
      if (anchorRect) {
        positionPopup(anchorRect);
      }
    }
  });
})();
