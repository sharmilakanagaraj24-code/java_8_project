# ElderCare Frontend (Updated Demo)

## Run
1. Open this folder in VS Code.
2. Install Live Server if needed.
3. Right-click `index.html` and choose **Open with Live Server**.

## Included
- Original dashboard and styling preserved from the starter ZIP.
- Elderly user, caregiver, medication, and schedule forms with validation and local demo CRUD (add/edit/delete/search).
- Alerts with local resolve/reset actions.
- Medication history with sample events and filters.
- Device status with simulated stock inputs only.

## Important
This is a frontend-only demo. Form data is saved in browser `localStorage`, not MySQL. Alerts/history/device values are illustrative. No real medication dispensing or ESP32 command is triggered.

Before connecting the Java/Spring Boot backend, confirm the real database schema and API contract with the backend teammate. Then update `js/api.js` and connect each operation to the actual endpoints. Do not put database credentials in frontend code.
