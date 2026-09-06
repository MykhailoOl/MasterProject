(() => {
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  const flash = document.getElementById("llm-flash");
  const errorFlash = document.getElementById("llm-error");

  const showMessage = (ok, message) => {
    if (flash) {
      flash.hidden = !ok;
      flash.textContent = ok ? message || "" : "";
    }
    if (errorFlash) {
      errorFlash.hidden = ok;
      errorFlash.textContent = ok ? "" : message || "";
    }
  };

  document.querySelectorAll(".check-connection").forEach((button) => {
    button.addEventListener("click", async () => {
      const provider = button.getAttribute("data-provider");
      if (!provider) {
        return;
      }
      button.disabled = true;
      const original = button.textContent;
      button.textContent = "Checking…";
      try {
        const headers = {};
        if (csrfToken && csrfHeader) {
          headers[csrfHeader] = csrfToken;
        }
        const response = await fetch(`/settings/llm/verify-live?provider=${encodeURIComponent(provider)}`, {
          method: "POST",
          headers,
        });
        const data = await response.json();
        const card = document.querySelector(`.provider-card[data-provider="${provider}"]`);
        if (card) {
          const status = card.querySelector(".provider-status");
          const checked = card.querySelector(".provider-checked");
          const checkedTime = card.querySelector(".provider-checked-time");
          if (status && data.statusLabel) {
            status.textContent = data.statusLabel;
          }
          if (checked && checkedTime && data.lastVerifiedLabel) {
            checked.hidden = false;
            checkedTime.textContent = data.lastVerifiedLabel;
          }
        }
        showMessage(Boolean(data.ok), data.message || (data.ok ? "Connection looks good." : "Check failed."));
      } catch (error) {
        showMessage(false, "The saved API key could not be checked.");
      } finally {
        button.disabled = false;
        button.textContent = original;
      }
    });
  });
})();
