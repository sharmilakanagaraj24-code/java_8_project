/*
 * API integration placeholder.
 * Update API_BASE_URL and endpoint paths after your teammate shares the real API contract.
 * Do not put MySQL credentials in frontend code.
 */
const API_BASE_URL = "http://localhost:8080/api";

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });

  if (!response.ok) {
    const message = `API request failed (${response.status})`;
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

// Example for later, once your friend's endpoint is confirmed:
// async function getElderlyUsers() {
//   return apiRequest("/elderly-users");
// }
