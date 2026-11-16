// Small progressive enhancements. Every page still works without this file.
(function () {
  "use strict";

  // Like buttons: send the like in the background instead of reloading the page.
  document.addEventListener("submit", function (event) {
    var form = event.target.closest("form[data-like]");
    if (!form || !window.fetch) return;
    event.preventDefault();

    var button = form.querySelector("button");
    var count = form.querySelector("[data-like-count]");
    fetch(form.action, { method: "POST", body: new FormData(form) })
      .then(function (response) {
        if (!response.ok || response.url.indexOf("/login") !== -1) throw new Error("like failed");
        var liked = button.getAttribute("aria-pressed") !== "true";
        button.setAttribute("aria-pressed", liked);
        count.textContent = Math.max(0, Number(count.textContent) + (liked ? 1 : -1));
        button.classList.remove("pop");
        void button.offsetWidth; // restart the animation
        if (liked) button.classList.add("pop");
      })
      .catch(function () {
        form.submit();
      });
  });

  // Selects marked data-autosubmit save as soon as a choice is made.
  document.addEventListener("change", function (event) {
    if (event.target.matches("select[data-autosubmit]")) event.target.form.submit();
  });
})();
