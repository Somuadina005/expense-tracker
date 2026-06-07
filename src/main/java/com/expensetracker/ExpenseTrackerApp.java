package com.expensetracker;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseTrackerApp {

    private static ExpenseStore store;

    public static void main(String[] args) throws IOException {
        String dataFile = "data/expenses.csv";
        new File("data").mkdirs();
        store = new ExpenseStore(dataFile);
        store.seedData();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", ExpenseTrackerApp::handleIndex);
        server.createContext("/api/expenses", ExpenseTrackerApp::handleExpenses);
        server.createContext("/api/stats", ExpenseTrackerApp::handleStats);
        server.start();

        System.out.println("Expense Tracker running at http://localhost:8080");
    }

    private static void handleIndex(HttpExchange ex) throws IOException {
        String html = getHtml();
        sendResponse(ex, 200, "text/html", html);
    }

    private static void handleExpenses(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equals("GET")) {
            List<Expense> all = store.getAll();
            String json = "[" + all.stream().map(Expense::toString).collect(Collectors.joining(",")) + "]";
            sendResponse(ex, 200, "application/json", json);

        } else if (method.equals("POST")) {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseJson(body);
            try {
                String category = params.getOrDefault("category", "Other");
                double amount = Double.parseDouble(params.getOrDefault("amount", "0"));
                LocalDate date = LocalDate.parse(params.getOrDefault("date", LocalDate.now().toString()));
                String desc = params.getOrDefault("description", "");
                Expense e = new Expense(category, amount, date, desc);
                store.add(e);
                sendResponse(ex, 201, "application/json", e.toString());
            } catch (Exception e) {
                sendResponse(ex, 400, "application/json", "{\"error\":\"Invalid data\"}");
            }

        } else if (method.equals("DELETE")) {
            String path = ex.getRequestURI().getPath();
            String id = path.substring(path.lastIndexOf('/') + 1);
            boolean ok = store.delete(id);
            sendResponse(ex, ok ? 200 : 404, "application/json", ok ? "{\"ok\":true}" : "{\"error\":\"Not found\"}");

        } else {
            sendResponse(ex, 405, "text/plain", "Method not allowed");
        }
    }

    private static void handleStats(HttpExchange ex) throws IOException {
        Map<String, Double> byCategory = store.getTotalByCategory();
        Map<String, Double> trend = store.getMonthlyTrend();
        double total = store.getTotal();
        String highest = store.getHighestCategory();
        String lowest = store.getLowestCategory();

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"total\":").append(String.format("%.2f", total)).append(",");
        sb.append("\"highest\":\"").append(highest).append("\",");
        sb.append("\"lowest\":\"").append(lowest).append("\",");
        sb.append("\"byCategory\":{");
        sb.append(byCategory.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + String.format("%.2f", e.getValue()))
                .collect(Collectors.joining(",")));
        sb.append("},\"trend\":{");
        sb.append(trend.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + String.format("%.2f", e.getValue()))
                .collect(Collectors.joining(",")));
        sb.append("}}");

        sendResponse(ex, 200, "application/json", sb.toString());
    }

    private static void sendResponse(HttpExchange ex, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim().replaceAll("[{}\"]", "");
        for (String pair : json.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    private static String getHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Expense Tracker</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: 'Segoe UI', system-ui, sans-serif; background: #f0f4f8; color: #1a202c; }
  header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px 32px; display: flex; align-items: center; gap: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.15); }
  header h1 { font-size: 1.6rem; font-weight: 700; }
  header span { font-size: 1.8rem; }
  .container { max-width: 1200px; margin: 0 auto; padding: 24px; }
  .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
  .stat-card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); border-left: 4px solid #667eea; }
  .stat-card .label { font-size: 0.8rem; color: #718096; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 6px; }
  .stat-card .value { font-size: 1.6rem; font-weight: 700; color: #2d3748; }
  .stat-card.green { border-color: #48bb78; } .stat-card.green .value { color: #276749; }
  .stat-card.red { border-color: #fc8181; } .stat-card.red .value { color: #c53030; }
  .stat-card.orange { border-color: #ed8936; } .stat-card.orange .value { color: #c05621; }
  .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }
  @media(max-width: 768px) { .grid2 { grid-template-columns: 1fr; } }
  .card { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
  .card h2 { font-size: 1.1rem; font-weight: 600; color: #4a5568; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
  .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
  .form-group label { font-size: 0.85rem; font-weight: 600; color: #4a5568; }
  .form-group input, .form-group select { padding: 10px 12px; border: 1.5px solid #e2e8f0; border-radius: 8px; font-size: 0.95rem; transition: border-color 0.2s; outline: none; }
  .form-group input:focus, .form-group select:focus { border-color: #667eea; box-shadow: 0 0 0 3px rgba(102,126,234,0.15); }
  .btn { padding: 11px 24px; border: none; border-radius: 8px; font-size: 0.95rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
  .btn-primary { background: linear-gradient(135deg, #667eea, #764ba2); color: white; width: 100%; margin-top: 4px; }
  .btn-primary:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(102,126,234,0.4); }
  .btn-danger { background: #fed7d7; color: #c53030; padding: 4px 10px; font-size: 0.8rem; border-radius: 6px; }
  .btn-danger:hover { background: #fc8181; color: white; }
  table { width: 100%; border-collapse: collapse; }
  th { padding: 10px 12px; text-align: left; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; color: #718096; background: #f7fafc; }
  td { padding: 12px; border-bottom: 1px solid #edf2f7; font-size: 0.9rem; }
  tr:last-child td { border-bottom: none; }
  tr:hover td { background: #f7fafc; }
  .badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 0.78rem; font-weight: 600; }
  .amount { font-weight: 700; color: #2d3748; }
  .empty { text-align: center; color: #a0aec0; padding: 40px; }
  .toast { position: fixed; bottom: 20px; right: 20px; background: #48bb78; color: white; padding: 12px 20px; border-radius: 8px; font-weight: 600; opacity: 0; transition: opacity 0.3s; pointer-events: none; z-index: 1000; }
  .toast.show { opacity: 1; }
</style>
</head>
<body>
<header><span>💰</span><h1>Expense Tracker</h1></header>
<div class="container">
  <div class="stats-grid">
    <div class="stat-card"><div class="label">Total Expenses</div><div class="value" id="statTotal">$0</div></div>
    <div class="stat-card orange"><div class="label">Total Entries</div><div class="value" id="statCount">0</div></div>
    <div class="stat-card red"><div class="label">Highest Category</div><div class="value" id="statHighest">—</div></div>
    <div class="stat-card green"><div class="label">Lowest Category</div><div class="value" id="statLowest">—</div></div>
  </div>
  <div class="grid2">
    <div class="card">
      <h2>📊 Spending by Category</h2>
      <canvas id="categoryChart" height="220"></canvas>
    </div>
    <div class="card">
      <h2>📈 Monthly Trend</h2>
      <canvas id="trendChart" height="220"></canvas>
    </div>
  </div>
  <div class="grid2">
    <div class="card">
      <h2>➕ Add Expense</h2>
      <div class="form-row">
        <div class="form-group">
          <label>Category</label>
          <select id="category">
            <option>Food</option><option>Transport</option><option>Entertainment</option>
            <option>Utilities</option><option>Health</option><option>Shopping</option><option>Other</option>
          </select>
        </div>
        <div class="form-group">
          <label>Amount ($)</label>
          <input type="number" id="amount" placeholder="0.00" min="0" step="0.01">
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Date</label>
          <input type="date" id="date">
        </div>
        <div class="form-group">
          <label>Description</label>
          <input type="text" id="description" placeholder="Optional note">
        </div>
      </div>
      <button class="btn btn-primary" onclick="addExpense()">Add Expense</button>
    </div>
    <div class="card" style="overflow-x:auto">
      <h2>📋 Recent Expenses</h2>
      <table>
        <thead><tr><th>Date</th><th>Category</th><th>Description</th><th>Amount</th><th></th></tr></thead>
        <tbody id="expenseTable"><tr><td colspan="5" class="empty">Loading...</td></tr></tbody>
      </table>
    </div>
  </div>
</div>
<div class="toast" id="toast"></div>

<script>
const COLORS = {Food:'#667eea',Transport:'#48bb78',Entertainment:'#ed8936',Utilities:'#4299e1',Health:'#fc8181',Shopping:'#9f7aea',Other:'#a0aec0'};
let catChart, trendChart;

function colorFor(cat) { return COLORS[cat] || '#a0aec0'; }

document.getElementById('date').value = new Date().toISOString().split('T')[0];

async function load() {
  const [expRes, statRes] = await Promise.all([fetch('/api/expenses'), fetch('/api/stats')]);
  const expenses = await expRes.json();
  const stats = await statRes.json();

  document.getElementById('statTotal').textContent = '$' + stats.total.toFixed(2);
  document.getElementById('statCount').textContent = expenses.length;
  document.getElementById('statHighest').textContent = stats.highest;
  document.getElementById('statLowest').textContent = stats.lowest;

  renderTable(expenses);
  renderCategoryChart(stats.byCategory);
  renderTrendChart(stats.trend);
}

function renderTable(expenses) {
  const sorted = [...expenses].sort((a,b) => b.date.localeCompare(a.date));
  const tbody = document.getElementById('expenseTable');
  if (!sorted.length) { tbody.innerHTML = '<tr><td colspan="5" class="empty">No expenses yet</td></tr>'; return; }
  tbody.innerHTML = sorted.slice(0, 15).map(e => `
    <tr>
      <td>${e.date}</td>
      <td><span class="badge" style="background:${colorFor(e.category)}22;color:${colorFor(e.category)}">${e.category}</span></td>
      <td style="color:#718096">${e.description || '—'}</td>
      <td class="amount">$${e.amount.toFixed(2)}</td>
      <td><button class="btn btn-danger" onclick="deleteExpense('${e.id}')">✕</button></td>
    </tr>`).join('');
}

function renderCategoryChart(data) {
  const labels = Object.keys(data);
  const values = Object.values(data);
  const ctx = document.getElementById('categoryChart').getContext('2d');
  if (catChart) catChart.destroy();
  catChart = new Chart(ctx, { type: 'doughnut', data: {
    labels, datasets: [{ data: values, backgroundColor: labels.map(colorFor), borderWidth: 2, borderColor: '#fff' }]
  }, options: { plugins: { legend: { position: 'right', labels: { font: { size: 12 } } } }, cutout: '60%' }});
}

function renderTrendChart(data) {
  const labels = Object.keys(data);
  const values = Object.values(data);
  const ctx = document.getElementById('trendChart').getContext('2d');
  if (trendChart) trendChart.destroy();
  trendChart = new Chart(ctx, { type: 'bar', data: {
    labels, datasets: [{ label: 'Monthly Spend ($)', data: values,
      backgroundColor: 'rgba(102,126,234,0.7)', borderColor: '#667eea', borderWidth: 2, borderRadius: 6 }]
  }, options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { callback: v => '$'+v } } } }});
}

async function addExpense() {
  const category = document.getElementById('category').value;
  const amount = parseFloat(document.getElementById('amount').value);
  const date = document.getElementById('date').value;
  const description = document.getElementById('description').value;
  if (!amount || amount <= 0 || !date) { showToast('Please fill in amount and date', '#fc8181'); return; }
  await fetch('/api/expenses', { method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({category, amount, date, description}) });
  document.getElementById('amount').value = '';
  document.getElementById('description').value = '';
  showToast('Expense added!', '#48bb78');
  load();
}

async function deleteExpense(id) {
  await fetch('/api/expenses/' + id, { method: 'DELETE' });
  showToast('Expense deleted', '#ed8936');
  load();
}

function showToast(msg, bg) {
  const t = document.getElementById('toast');
  t.textContent = msg; t.style.background = bg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2500);
}

load();
</script>
</body>
</html>
""";
    }
}
