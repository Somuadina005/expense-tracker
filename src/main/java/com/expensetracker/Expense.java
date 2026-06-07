package com.expensetracker;

import java.time.LocalDate;
import java.util.UUID;

public class Expense {
    private String id;
    private String category;
    private double amount;
    private LocalDate date;
    private String description;

    public Expense(String category, double amount, LocalDate date, String description) {
        this.id = UUID.randomUUID().toString();
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    public Expense(String id, String category, double amount, LocalDate date, String description) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }

    public String toCsv() {
        return id + "," + category + "," + amount + "," + date + "," + description.replace(",", ";");
    }

    public static Expense fromCsv(String line) {
        String[] parts = line.split(",", 5);
        return new Expense(parts[0], parts[1], Double.parseDouble(parts[2]),
                LocalDate.parse(parts[3]), parts.length > 4 ? parts[4] : "");
    }

    @Override
    public String toString() {
        return String.format("{\"id\":\"%s\",\"category\":\"%s\",\"amount\":%.2f,\"date\":\"%s\",\"description\":\"%s\"}",
                id, category, amount, date, description.replace("\"", "\\\""));
    }
}
