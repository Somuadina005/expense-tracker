# Expense Tracker

A lightweight Java web application for tracking and analyzing personal expenses.

## Features
- Add expenses with category, amount, date, and description
- Doughnut chart of spending by category
- Bar chart of monthly spending trend
- Total expenses, entry count, highest & lowest category stats
- Delete individual expense entries
- Persistent CSV storage (survives restarts)
- Auto-seeded with 15 sample expenses on first run

# Approach & Design
## Architecture
The application is a single-process Java program using com.sun.net.httpserver — Java's built-in HTTP server. There is no Spring Boot, no Maven, no Gradle, and no external JARs. The entire app compiles and runs with one javac command.

## Design Pattern
The app follows a clean 3-class architecture:
Expense.java — Data model (what an expense looks like)
ExpenseStore.java — Business logic and analytics (how data is stored and calculated)
ExpenseTrackerApp.java — HTTP server, REST API, and the full web UI

## Data Storage
All data is stored in an ArrayList in memory while the app runs. Every time an expense is added or deleted, it is immediately written to a CSV file. On startup the app reads the file back so no data is lost between restarts. No database is needed.

## Frontend
The entire web UI is a single HTML page embedded inside the Java file. JavaScript uses the Fetch API to call the REST endpoints and update the dashboard without page reloads. Chart.js renders the doughnut and bar charts.

## How to Run

### Prerequisites
- Java JDK 25 or higher — [Download Eclipse Adoptium/Temurin here](https://adoptium.net)

### Steps

```powershell
# 1. Add Java to your PATH (once per terminal session)
# Replace <your-jdk-folder> with your actual JDK folder name, e.g. jdk-25.0.3.9-hotspot
$env:PATH = "C:\Users\$env:USERNAME\AppData\Local\Programs\Eclipse Adoptium\<your-jdk-folder>\bin;" + $env:PATH

## Verify Java is working
javac -version

## 2. Navigate to the project folder
cd path\to\expense-tracker

## 3. Create output directories (first time only)
New-Item -ItemType Directory -Force -Path out\expense-tracker, data

## 4. Compile
javac -d out\expense-tracker `
  src\main\java\com\expensetracker\Expense.java `
  src\main\java\com\expensetracker\ExpenseStore.java `
  src\main\java\com\expensetracker\ExpenseTrackerApp.java

## 5. Run
java -cp out\expense-tracker com.expensetracker.ExpenseTrackerApp
```

Open your browser and go to: http://localhost:8080

> On first run, 15 sample expenses are seeded automatically so the dashboard loads with data right away.

### Finding Your JDK Folder Name
If you are unsure of your exact JDK folder name, run this in PowerShell to list what is installed:
```powershell
ls "C:\Users\$env:USERNAME\AppData\Local\Programs\Eclipse Adoptium"
```
Use the folder name shown in the output to replace `<your-jdk-folder>` in step 1.
````

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/expenses` | Get all expenses |
| POST | `/api/expenses` | Add new expense |
| DELETE | `/api/expenses/{id}` | Delete an expense |
| GET | `/api/stats` | Get totals, category breakdown, trend |

### POST /api/expenses body
```json
{
  "category": "Food",
  "amount": 45.50,
  "date": "2026-06-05",
  "description": "Weekly groceries"
}
```

## Project Structure

```
expense-tracker/
├── run.sh                          # Build + run script
├── data/
│   └── expenses.csv                # Persistent storage (auto-created)
└── src/main/java/com/expensetracker/
    ├── Expense.java                # Data model + CSV serialization
    ├── ExpenseStore.java           # In-memory store + file persistence + analytics
    └── ExpenseTrackerApp.java      # HTTP server + REST API + HTML/JS UI
```

## Categories
Food, Transport, Entertainment, Utilities, Health, Shopping, Other
