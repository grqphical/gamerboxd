// Formats <time datetime="...ISO..."> elements with the visitor's local timezone,
// falling back to the server-rendered text if the Date is invalid.
(function () {
  "use strict";
  var fmt = new Intl.DateTimeFormat(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric"
  });
  document.querySelectorAll("time[datetime]").forEach(function (el) {
    var d = new Date(el.getAttribute("datetime"));
    if (!isNaN(d.getTime())) {
      el.textContent = fmt.format(d);
    }
  });
})();
