(() => {
  const sidebar = document.querySelector(".sidebar");
  const toggle = document.querySelector("[data-sidebar-toggle]");
  toggle?.addEventListener("click", () => sidebar?.classList.toggle("open"));
  document.addEventListener("click", (event) => {
    if (window.innerWidth > 760 || !sidebar?.classList.contains("open")) return;
    if (!sidebar.contains(event.target) && !toggle?.contains(event.target)) sidebar.classList.remove("open");
  });

  const search = document.querySelector("[data-document-search]");
  const status = document.querySelector("[data-status-filter]");
  const rows = [...document.querySelectorAll("[data-document-row]")];
  const filterEmpty = document.querySelector("[data-filter-empty]");
  const filterDocuments = () => {
    const query = (search?.value || "").trim().toLowerCase();
    const selectedStatus = status?.value || "";
    let visible = 0;
    rows.forEach((row) => {
      const matchesText = !query || row.dataset.search.includes(query);
      const matchesStatus = !selectedStatus || row.dataset.status === selectedStatus || (selectedStatus === "PROCESSING" && !["COMPLETED", "FAILED"].includes(row.dataset.status));
      row.hidden = !(matchesText && matchesStatus);
      if (!row.hidden) visible += 1;
    });
    filterEmpty?.classList.toggle("hidden", visible > 0 || rows.length === 0);
  };
  search?.addEventListener("input", filterDocuments);
  status?.addEventListener("change", filterDocuments);

  const dropZone = document.querySelector("[data-drop-zone]");
  const fileInput = document.querySelector("[data-file-input]");
  const selectedFile = document.querySelector("[data-selected-file]");
  const fileName = document.querySelector("[data-file-name]");
  const fileSize = document.querySelector("[data-file-size]");
  const clearFile = document.querySelector("[data-clear-file]");
  const showFile = (file) => {
    if (!file) return;
    fileName.textContent = file.name;
    fileSize.textContent = file.size < 1024 * 1024 ? `${(file.size / 1024).toFixed(1)} KB` : `${(file.size / 1024 / 1024).toFixed(2)} MB`;
    selectedFile?.classList.remove("hidden");
  };
  fileInput?.addEventListener("change", () => showFile(fileInput.files[0]));
  ["dragenter", "dragover"].forEach((name) => dropZone?.addEventListener(name, (event) => { event.preventDefault(); dropZone.classList.add("dragging"); }));
  ["dragleave", "drop"].forEach((name) => dropZone?.addEventListener(name, (event) => { event.preventDefault(); dropZone.classList.remove("dragging"); }));
  dropZone?.addEventListener("drop", (event) => {
    const file = event.dataTransfer.files[0];
    if (!file || !fileInput) return;
    const transfer = new DataTransfer(); transfer.items.add(file); fileInput.files = transfer.files; showFile(file);
  });
  clearFile?.addEventListener("click", () => { if (fileInput) fileInput.value = ""; selectedFile?.classList.add("hidden"); });

  document.querySelectorAll("[data-confirm]").forEach((form) => form.addEventListener("submit", (event) => {
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  }));

  const queryInput = document.querySelector("#search-query");
  document.querySelectorAll("[data-query-example]").forEach((button) => button.addEventListener("click", () => { if (queryInput) { queryInput.value = button.dataset.queryExample; queryInput.focus(); } }));
})();
