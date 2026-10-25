
document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "http://localhost:8080/api";

  // =========================================================
  // LOCAL STORAGE HELPERS
  // =========================================================

  const get = (key, fallback = []) => {
    try {
      return JSON.parse(
        localStorage.getItem("eldercare_" + key)
      ) ?? fallback;
    } catch {
      return fallback;
    }
  };

  const set = (key, value) =>
    localStorage.setItem(
      "eldercare_" + key,
      JSON.stringify(value)
    );

  // Escape HTML to avoid inserting unsafe content into tables.
  const esc = (value) =>
    String(value ?? "").replace(/[&<>"']/g, (character) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;"
    }[character]));

  // =========================================================
  // SPRING BOOT API HELPER
  // =========================================================

  async function apiRequest(path, options = {}) {
    const response = await fetch(
      `${API_BASE_URL}${path}`,
      {
        ...options,
        headers: {
          ...(options.body
            ? { "Content-Type": "application/json" }
            : {}),
          ...(options.headers || {})
        }
      }
    );

    if (!response.ok) {
      const message = await response.text();
      throw new Error(
        message || `Request failed: ${response.status}`
      );
    }

    if (response.status === 204) return null;

    const contentType =
      response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
      return response.json();
    }

    return response.text();
  }

  // =========================================================
  // GENERIC PAGES
  // Elderly users use the backend.
  // Other generic pages use local demo storage.
  // Schedules are handled separately.
  // =========================================================

  const form = document.getElementById("recordForm");

  if (
    form &&
    window.PAGE_CONFIG &&
    window.PAGE_CONFIG.columns &&
    window.PAGE_CONFIG.entity !== "schedules"
  ) {
    const entity = window.PAGE_CONFIG.entity;
    const columns = window.PAGE_CONFIG.columns;
    const fields = window.PAGE_CONFIG.fields || [];

    const tbody = document.getElementById("recordsTable");
    const search = document.getElementById("tableSearch");
    const message = document.getElementById("formMessage");
    const heading = document.getElementById("formHeading");
    const saveButton = document.getElementById("saveButton");
    const recordId = document.getElementById("recordId");

    let backendElderlyUsers = [];

    // Reset the form.
    const reset = () => {
      form.reset();

      if (recordId) {
        recordId.value = "";
      }

      if (heading) {
        heading.textContent =
          heading.dataset.original || heading.textContent;
      }

      if (saveButton) {
        saveButton.textContent = "Save details";
      }
    };

    // =======================================================
    // LOAD ELDERLY USERS FROM MYSQL THROUGH SPRING BOOT
    // =======================================================

    async function loadElderlyUsersFromBackend() {
      if (!tbody) return;

      tbody.innerHTML = `
        <tr>
          <td colspan="${columns.length + 1}">
            Loading elderly users from backend...
          </td>
        </tr>
      `;

      try {
        const users = await apiRequest("/elderly-users");

        backendElderlyUsers = (
          Array.isArray(users) ? users : []
        ).map((user) => ({
          ...user,
          id: String(user.elderlyId),
          fullName: user.fullName || "",
          dateOfBirth: user.dateOfBirth || "",
          phone: user.phone || "",
          address: user.address || ""
        }));

        render();
      } catch (error) {
        console.error(
          "Could not load elderly users:",
          error
        );

        tbody.innerHTML = `
          <tr>
            <td colspan="${columns.length + 1}"
                class="empty-state">
              Could not load elderly users.
              Check that Spring Boot is running.
            </td>
          </tr>
        `;

        if (message) {
          message.textContent =
            "Could not load elderly users: " + error.message;
        }
      }
    }

    // =======================================================
    // RENDER TABLE
    // =======================================================

    const render = () => {
      if (!tbody) return;

      let rows =
        entity === "elderlyUsers"
          ? [...backendElderlyUsers]
          : get(entity);

      const query =
        (search?.value || "").toLowerCase();

      rows = rows.filter((row) =>
        columns.some(([key]) =>
          String(row[key] ?? "")
            .toLowerCase()
            .includes(query)
        )
      );

      if (!rows.length) {
        tbody.innerHTML = `
          <tr>
            <td colspan="${columns.length + 1}"
                class="empty-state">
              No matching records. Add a record using the form.
            </td>
          </tr>
        `;
        return;
      }

      tbody.innerHTML = rows.map((row) => `
        <tr>
          ${columns.map(([key]) =>
            `<td>${esc(row[key]) || "—"}</td>`
          ).join("")}

         ${entity === "elderlyUsers" ? "" : `
  <td class="action-cell">
    <button
      type="button"
      class="btn btn-sm btn-outline-care"
      data-edit="${esc(row.id)}">
      Edit
    </button>
    <button
      type="button"
      class="btn btn-sm btn-outline-danger"
      data-delete="${esc(row.id)}">
      Delete
    </button>
  </td>
`}
      `).join("");
    };

    if (heading) {
      heading.dataset.original = heading.textContent;
    }

    // =======================================================
    // FORM SUBMISSION
    // =======================================================

    form.addEventListener("submit", async (event) => {
      event.preventDefault();

      if (!form.reportValidity()) return;

      const data = {};

      fields.forEach((field) => {
        data[field.key] =
          form.elements[field.key]?.value?.trim() ?? "";
      });

      const id = recordId?.value || "";

      // -----------------------------------------------------
      // ELDERLY USERS: SAVE TO MYSQL THROUGH BACKEND
      // -----------------------------------------------------

      if (entity === "elderlyUsers") {
        try {
          if (id) {
            if (message) {
              message.textContent =
                "Editing elderly users is not connected yet.";
            }
            return;
          }

          const payload = {
            fullName: data.fullName,
            dateOfBirth: data.dateOfBirth || null,
            gender: data.gender || null,
            address: data.address || "",
            phone: data.phone || ""
          };

          await apiRequest("/elderly-users", {
            method: "POST",
            body: JSON.stringify(payload)
          });

          reset();

          await loadElderlyUsersFromBackend();

          if (message) {
            message.textContent =
              "Elderly user saved successfully to the backend.";
          }
        } catch (error) {
          console.error(
            "Elderly user save failed:",
            error
          );

          if (message) {
            message.textContent =
              "Could not save elderly user: " + error.message;
          }
        }

        return;
      }

      // -----------------------------------------------------
      // OTHER GENERIC PAGES: LOCAL DEMO STORAGE
      // -----------------------------------------------------

      let rows = get(entity);

      if (id) {
        rows = rows.map((row) =>
          row.id === id
            ? { ...row, ...data, id }
            : row
        );

        if (message) {
          message.textContent =
            "Demo record updated in this browser.";
        }
      } else {
        data.id = String(Date.now());
        rows.unshift(data);

        if (message) {
          message.textContent =
            "Demo record saved locally. It has not been sent to MySQL.";
        }
      }

      set(entity, rows);
      render();
      reset();
    });

    // =======================================================
    // CLEAR FORM
    // =======================================================

    document
      .getElementById("resetButton")
      ?.addEventListener("click", () => {
        reset();

        if (message) {
          message.textContent = "Form cleared.";
        }
      });

    // =======================================================
    // SEARCH TABLE
    // =======================================================

    search?.addEventListener("input", render);

    // =======================================================
    // EDIT AND DELETE FOR LOCAL DEMO PAGES
    // =======================================================

    tbody?.addEventListener("click", (event) => {
      const edit = event.target.closest("[data-edit]");
      const remove = event.target.closest("[data-delete]");

      if (entity === "elderlyUsers") {
        if (edit || remove) {
          if (message) {
            message.textContent =
              "Editing and deleting backend elderly users are not connected yet.";
          }
        }
        return;
      }

      let rows = get(entity);

      if (edit) {
        const row = rows.find(
          (item) => item.id === edit.dataset.edit
        );

        if (!row) return;

        fields.forEach((field) => {
          if (form.elements[field.key]) {
            form.elements[field.key].value =
              row[field.key] ?? "";
          }
        });

        if (recordId) {
          recordId.value = row.id;
        }

        if (heading) {
          heading.textContent = "Edit record";
        }

        if (saveButton) {
          saveButton.textContent = "Update details";
        }

        if (message) {
          message.textContent =
            "Editing selected demo record.";
        }

        window.scrollTo({
          top: 0,
          behavior: "smooth"
        });
      }

      if (
        remove &&
        confirm("Delete this demo record?")
      ) {
        set(
          entity,
          rows.filter(
            (row) => row.id !== remove.dataset.delete
          )
        );

        render();

        if (message) {
          message.textContent =
            "Demo record deleted from this browser.";
        }
      }
    });

    // =======================================================
    // INITIAL LOAD
    // =======================================================

    if (entity === "elderlyUsers") {
      loadElderlyUsersFromBackend();
    } else {
      render();
    }
  }

  // =========================================================
  // REAL BACKEND: MEDICATION SCHEDULES
  // =========================================================

  async function initializeSchedulesPage() {
    const scheduleForm =
      document.getElementById("scheduleForm");

    // Do not run this section on unrelated pages.
    if (
      !scheduleForm &&
      window.PAGE_CONFIG?.entity !== "schedules"
    ) {
      return;
    }

    const table = document.getElementById("recordsTable");
    const message = document.getElementById("formMessage");

    if (!table) return;

    let schedules = [];

    // -------------------------------------------------------
    // LOAD SCHEDULES FROM BACKEND
    // -------------------------------------------------------

    async function loadSchedules() {
      table.innerHTML = `
        <tr>
          <td colspan="8">
            Loading medication schedules...
          </td>
        </tr>
      `;

      try {
        const result = await apiRequest(
          "/medication-schedules"
        );

        schedules = Array.isArray(result) ? result : [];

        renderSchedules();
      } catch (error) {
        console.error(
          "Could not load schedules:",
          error
        );

        table.innerHTML = `
          <tr>
            <td colspan="8">
              Could not load schedules.
              Check the backend API endpoint.
            </td>
          </tr>
        `;

        if (message) {
          message.textContent =
            "Could not load schedules: " + error.message;
        }
      }
    }

    // -------------------------------------------------------
    // DISPLAY SCHEDULES
    // -------------------------------------------------------

    
function renderSchedules() {
  if (!schedules.length) {
    table.innerHTML = `
      <tr>
        <td colspan="8">No medication schedules found.</td>
      </tr>
    `;
    return;
  }

  table.innerHTML = schedules.map((schedule) => {
    const id = schedule.scheduleId ?? schedule.schedule_id ?? "";

    return `
      <tr>
        <td>${esc(schedule.elderlyName ?? schedule.elderly_name ?? "—")}</td>
        <td>${esc(schedule.medicationName ?? schedule.medication_name ?? "—")}</td>
        <td>${esc(schedule.dosage ?? "—")}</td>
        <td>${esc(schedule.scheduledTime ?? schedule.scheduled_time ?? "—")}</td>
        <td>${esc(schedule.frequency ?? "—")}</td>
        <td>${esc(schedule.slotNumber ?? schedule.slot_number ?? "—")}</td>
        <td>${schedule.isActive ?? schedule.is_active ? "Active" : "Inactive"}</td>
        <td>
          <button
            type="button"
            class="btn btn-sm btn-outline-danger"
            data-delete-schedule="${esc(id)}">
            Delete
          </button>
        </td>
      </tr>
    `;
  }).join("");
}


    // -------------------------------------------------------
    // DELETE A SCHEDULE
    // -------------------------------------------------------

    table.addEventListener("click", async (event) => {
      const button =
        event.target.closest("[data-delete-schedule]");

      if (!button) return;

      const id = button.dataset.deleteSchedule;

      if (
        !id ||
        !confirm(
          "Are you sure you want to delete this schedule?"
        )
      ) {
        return;
      }

      try {
        await apiRequest(
          `/medication-schedules/${encodeURIComponent(id)}`,
          { method: "DELETE" }
        );

        if (message) {
          message.textContent =
            "Schedule deleted successfully.";
        }

        await loadSchedules();
      } catch (error) {
        console.error(
          "Could not delete schedule:",
          error
        );

        if (message) {
          message.textContent =
            "Could not delete schedule: " + error.message;
        }
      }
    });

    // Initial schedule load.
    await loadSchedules();
  }

  initializeSchedulesPage();

});
