
const API_BASE_URL = "http://localhost:8080/api";

async function fetchApi(path) {
  const response = await fetch(`${API_BASE_URL}${path}`);

  if (!response.ok) {
    throw new Error(`API request failed: ${path} (${response.status})`);
  }

  return response.json();
}

function formatTime(value) {
  if (!value) return "Time unavailable";

  // Scheduled-dose timestamps are returned in UTC.
  const date = new Date(value);

  return date.toLocaleTimeString("en-IN", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
    timeZone: "Asia/Kolkata"
  });
}

function getStatusClass(status) {
  switch ((status || "").toUpperCase()) {
    case "DISPENSED":
    case "ACKNOWLEDGED":
      return "status-done";
    case "PENDING":
      return "status-pending";
    default:
      return "status-upcoming";
  }
}

async function renderSchedule() {
  const tbody = document.getElementById("scheduleTableBody");
  if (!tbody) return;

  tbody.innerHTML =
    '<tr><td colspan="4">Loading medication data...</td></tr>';

  try {
    const [doses, medications, schedules] = await Promise.all([
      fetchApi("/scheduled-doses"),
      fetchApi("/medications"),
      fetchApi("/medication-schedules")
    ]);

    const medicationMap = new Map(
      medications.map(m => [m.medicationId, m])
    );

    const scheduleMap = new Map(
      schedules.map(s => [s.scheduleId, s])
    );

    if (!doses.length) {
      tbody.innerHTML =
        '<tr><td colspan="4">No scheduled doses found.</td></tr>';
      return;
    }

    tbody.innerHTML = doses.map(dose => {
      const schedule = scheduleMap.get(dose.scheduleId);
      const medication = schedule
        ? medicationMap.get(schedule.medicationId)
        : null;

      const status = dose.doseStatus || "UNKNOWN";
      const statusClass = getStatusClass(status);
      const medicationName = medication
        ? medication.medicationName
        : "Medication unavailable";
      const slotText = schedule
        ? `Slot ${schedule.slotId}`
        : "Slot unavailable";

      return `
        <tr>
          <td class="dose-time">${formatTime(dose.dueTime)}</td>
          <td>
            <div class="med-name">${medicationName}</div>
            <div class="med-sub">${medication?.dosage || "Dosage unavailable"}</div>
          </td>
          <td><span class="slot-badge">${slotText}</span></td>
          <td>
            <span class="status-tag ${statusClass}">${status}</span>
          </td>
        </tr>
      `;
    }).join("");

  } catch (error) {
    console.error("Dashboard schedule error:", error);
    tbody.innerHTML =
      '<tr><td colspan="4">Could not load data. Check that the backend is running.</td></tr>';
  }
}

async function renderAlerts() {
  const list = document.getElementById("alertList");
  if (!list) return;

  try {
    const alerts = await fetchApi("/alerts");

    if (!alerts.length) {
      list.innerHTML = "<p>No alerts found.</p>";
      return;
    }

    list.innerHTML = alerts.map(alert => `
      <div class="alert-row">
        <div class="alert-symbol">!</div>
        <div>
          <strong>${alert.alertType || "Alert"}</strong>
          <p>${alert.alertMessage || "No description available"}</p>
        </div>
      </div>
    `).join("");
  } catch (error) {
    console.error("Dashboard alerts error:", error);
    list.innerHTML = "<p>Could not load alerts from the backend.</p>";
  }
}

document.addEventListener("DOMContentLoaded", () => {
  renderSchedule();
  renderAlerts();
});
