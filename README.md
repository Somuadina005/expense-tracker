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

## How to Run

```bash
# 1. Compile
mkdir -p out/expense-tracker data
javac -d out/expense-tracker \
  src/main/java/com/expensetracker/Expense.java \
  src/main/java/com/expensetracker/ExpenseStore.java \
  src/main/java/com/expensetracker/ExpenseTrackerApp.java

# 2. Run
java -cp out/expense-tracker com.expensetracker.ExpenseTrackerApp

# 3. Open browser
# http://localhost:8080
```

Or use the helper script:
```bash
chmod +x run.sh && ./run.sh
```

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
