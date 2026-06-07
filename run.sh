#!/bin/bash
# Build and run the Expense Tracker
set -e

echo "=== Building Expense Tracker ==="
mkdir -p out/expense-tracker data

javac -d out/expense-tracker \
  src/main/java/com/expensetracker/Expense.java \
  src/main/java/com/expensetracker/ExpenseStore.java \
  src/main/java/com/expensetracker/ExpenseTrackerApp.java

echo "Build successful!"
echo "Starting Expense Tracker at http://localhost:8080"
java -cp out/expense-tracker com.expensetracker.ExpenseTrackerApp
