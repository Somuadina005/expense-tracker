# Build and run Expense Tracker
$ErrorActionPreference = "Stop"

Write-Host "=== Building Expense Tracker ==="

New-Item -ItemType Directory -Force -Path "out\expense-tracker" | Out-Null
New-Item -ItemType Directory -Force -Path "data" | Out-Null

javac -d out\expense-tracker `
  src\main\java\com\expensetracker\Expense.java `
  src\main\java\com\expensetracker\ExpenseStore.java `
  src\main\java\com\expensetracker\ExpenseTrackerApp.java

Write-Host "Build successful!"
Write-Host "Starting Expense Tracker at http://localhost:8080"

java -cp out\expense-tracker com.expensetracker.ExpenseTrackerApp
