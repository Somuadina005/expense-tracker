package com.expensetracker;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseStore {
    private final List<Expense> expenses = new ArrayList<>();
    private final String dataFile;

    public ExpenseStore(String dataFile) {
        this.dataFile = dataFile;
        load();
    }

    public synchronized void add(Expense e) {
        expenses.add(e);
        save();
    }

    public synchronized boolean delete(String id) {
        boolean removed = expenses.removeIf(e -> e.getId().equals(id));
        if (removed) save();
        return removed;
    }

    public synchronized List<Expense> getAll() {
        return new ArrayList<>(expenses);
    }

    public synchronized double getTotal() {
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    public synchronized Map<String, Double> getTotalByCategory() {
        return expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)));
    }

    public synchronized Map<String, Double> getMonthlyTrend() {
        Map<String, Double> trend = new TreeMap<>();
        for (Expense e : expenses) {
            String key = e.getDate().getYear() + "-" + String.format("%02d", e.getDate().getMonthValue());
            trend.merge(key, e.getAmount(), Double::sum);
        }
        return trend;
    }

    public synchronized String getHighestCategory() {
        return getTotalByCategory().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
    }

    public synchronized String getLowestCategory() {
        return getTotalByCategory().entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
    }

    private void load() {
        File f = new File(dataFile);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) expenses.add(Expense.fromCsv(line));
            }
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
        }
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(dataFile))) {
            for (Expense e : expenses) pw.println(e.toCsv());
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public synchronized void seedData() {
        if (!expenses.isEmpty()) return;
        LocalDate now = LocalDate.now();
        add(new Expense("Food", 85.50, now.minusDays(1), "Grocery shopping"));
        add(new Expense("Transport", 45.00, now.minusDays(2), "Monthly bus pass"));
        add(new Expense("Entertainment", 120.00, now.minusDays(3), "Concert tickets"));
        add(new Expense("Food", 32.75, now.minusDays(4), "Restaurant lunch"));
        add(new Expense("Utilities", 95.00, now.minusDays(5), "Electric bill"));
        add(new Expense("Health", 60.00, now.minusDays(6), "Gym membership"));
        add(new Expense("Food", 55.20, now.minusDays(10), "Weekly groceries"));
        add(new Expense("Transport", 25.00, now.minusDays(12), "Uber rides"));
        add(new Expense("Entertainment", 15.99, now.minusDays(15), "Streaming service"));
        add(new Expense("Utilities", 78.00, now.minusDays(20), "Internet bill"));
        add(new Expense("Health", 45.00, now.minusDays(22), "Pharmacy"));
        add(new Expense("Food", 110.00, now.minusDays(35), "Grocery shopping"));
        add(new Expense("Entertainment", 200.00, now.minusDays(40), "Weekend trip"));
        add(new Expense("Transport", 60.00, now.minusDays(45), "Fuel"));
        add(new Expense("Utilities", 110.00, now.minusDays(50), "Gas bill"));
    }
}
