import java.sql.*;
import java.util.Map;
import java.util.stream.Collectors;
import com.sun.net.httpserver.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ServerTest0605 {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // server.createContext("/api/hello", new TableDataHandler("select * from
        // order_v1"));
        // server.createContext("/api/customers", new TableDataHandler("select * from
        // customers"));
        server.createContext("/api/1/employee", new TableDataHandler("select * from employee")); // demoAPI0510
        server.createContext("/api/1/member", new TableDataHandler("select * from member ORDER BY idmember ASC"));
        server.createContext("/api/1/orders", new TableDataHandler("select * from orders"));
        server.createContext("/api/1/product2", new TableDataHandler("select * from product2 ORDER BY idproduct2 ASC"));
        server.createContext("/api/1/length", new TableDataHandler("select * from length"));
        server.createContext("/api/1/weight", new TableDataHandler("select * from weight"));

        server.createContext("/api/2/insert", new InsertDataHandler()); // demoinsert系列
        server.createContext("/api/2/update", new UpdateDataHandler());

        server.createContext("/chat/send", new ChatHandler.SendHandler()); // 聊天
        server.createContext("/chat/messages", new ChatHandler.MessageHandler());

        server.createContext("/postdemo", new postdemo());

        server.createContext("/getdemo", new getdemo());

        server.start();
        System.out.println("Server started at http://localhost:8000");
    }

    static class getdemo implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello Welcome!";
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            try (OutputStream os = exchange.getResponseBody()) {
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                os.write(responseBytes);
            }
        }
    }

    static class postdemo implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String file = "temppost.txt";
            String body;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining());
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(body);
            } catch (IOException e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
                return;
            }

            exchange.sendResponseHeaders(200, -1);
        }
    }

    static class TableDataHandler implements HttpHandler {
        String sql = "";

        public TableDataHandler(String sql) {
            this.sql = sql;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder();
            json.append("[");

            try (
                    Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/shippingsystem", "root",
                            "123456");
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {

                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                boolean firstRow = true;

                while (rs.next()) {
                    if (!firstRow)
                        json.append(",");
                    firstRow = false;
                    json.append("{");
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnLabel(i);
                        String value = rs.getString(i);
                        if (i > 1)
                            json.append(",");
                        json.append("\"").append(columnName).append("\":");
                        json.append("\"").append(value == null ? "" : escapeJson(value)).append("\"");
                    }
                    json.append("}");
                }
            } catch (Exception e) {
                json = new StringBuilder("{\"error\":\"" + e.getMessage() + "\"}");
            }

            json.append("]");
            String response = json.toString();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            try (OutputStream os = exchange.getResponseBody()) {
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                os.write(responseBytes);
            }
        }

        private static String escapeJson(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }
    }

    static class InsertDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {

                String body = reader.lines().collect(Collectors.joining());
                Map<String, String> map = DBConnect02.parseJson(body);
                String targetTable = map.get("table");

                switch (targetTable) {
                    case "member":
                        String name = map.get("name");
                        int memberid = DBConnect02.getNextId("member", "idmember");
                        String gender = map.get("gender");
                        String rawBirth = map.get("birthday");
                        String birthday = null;

                        // 將 yyyy-MM-dd 轉成 yyyyMMdd
                        if (rawBirth != null && rawBirth.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            birthday = rawBirth.replace("-", ""); // 1990-01-15 → 19900115
                        } else {
                            throw new RuntimeException("生日格式錯誤，請使用 yyyy-MM-dd（HTML 日期欄位預設）");
                        }

                        String email = map.get("email");
                        String phone = map.get("phone");
                        String country = map.get("country");
                        String creditcard = map.get("creditcard");
                        DBConnect02.executeUpdate(
                                "INSERT INTO member (idmember, membername, gender, birthday, email, phone, country, creditcard) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                memberid, name, gender, birthday, email, phone, country, creditcard);
                        exchange.sendResponseHeaders(200, -1);
                        break;

                    case "employee":
                        String employeeName = map.get("employeeName");
                        int employeeid = DBConnect02.getNextId("employee", "idemployee");
                        String employeeGender = map.get("employeeGender");
                        String employeeBirthday = map.get("employeeBirthday");
                        String employeePhone = map.get("employeePhone");
                        DBConnect02.executeUpdate(
                                "INSERT INTO employee (idemployee, employeeName, employeeGender, employeeBirthday, employeePhone) VALUES (?, ?, ?, ?, ?)",
                                employeeid, employeeName, employeeGender, employeeBirthday, employeePhone);
                        exchange.sendResponseHeaders(200, -1);
                        break;

                    case "orders":
                        String o_pname = map.get("pname");
                        String o_cname = map.get("cname");
                        String o_sname = map.get("sname");
                        String customerName = map.get("customerName");
                        String customerPhone = map.get("customerPhone");
                        String orderEmployeeName = map.get("employeeName");
                        String creditCardNumber = map.get("creditCardNumber");
                        String plan = map.get("plan");

                        int price = 0;
                        try {
                            price = Integer.parseInt(map.getOrDefault("price", "0"));
                        } catch (NumberFormatException e) {
                            price = 0;
                        }

                        int oid = DBConnect02.getNextId("orders", "idorders");
                        DBConnect02.executeUpdate(
                                "INSERT INTO orders (idorders, ProductID, memberID, EmployeeID, DateTime, CustomerName, CustomerPhone, EmployeeName, `Credit Card Number`, Plan, Price) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(), ?, ?, ?, ?, ?, ?)",
                                oid, o_pname, o_cname, o_sname, customerName, customerPhone, orderEmployeeName,
                                creditCardNumber, plan, price);
                        exchange.sendResponseHeaders(200, -1);
                        break;

                    default:
                        String response = "{\"error\": \"Unknown table: " + targetTable + "\"}";
                        exchange.sendResponseHeaders(400, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes(StandardCharsets.UTF_8));
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                String response = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    static class UpdateDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {

                String body = reader.lines().collect(Collectors.joining());
                Map<String, String> map = DBConnect02.parseJson(body);

                String targetTable = map.get("table");
                if (targetTable == null) {
                    String response = "{\"error\": \"缺少必要欄位 table\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }

                switch (targetTable) {
                    case "employee":
                        String empId = map.get("idemployee");
                        String empState = map.get("state"); // 你前端傳過來的 -1
                        DBConnect02.executeUpdate(
                                "UPDATE employee SET status = ? WHERE idemployee = ?", empState, empId);
                        exchange.sendResponseHeaders(200, -1);
                        break;

                    case "customers":
                        String name = map.get("name");
                        int id = DBConnect02.getNextId("customers", "idCustomers");
                        DBConnect02.executeUpdate(
                                "INSERT INTO customers (idCustomers, CustmerName) VALUES (?, ?)", id, name);
                        exchange.sendResponseHeaders(200, -1);
                        break;

                    case "products":
                        String pname = map.get("pname");
                        String price = map.get("price");
                        int pid = DBConnect02.getNextId("products", "idProducts");
                        DBConnect02.executeUpdate(
                                "INSERT INTO products (idProducts, ProductName, Price, CategoryID) VALUES (?, ?, ?, 1)",
                                pid, pname, Integer.parseInt(price));
                        exchange.sendResponseHeaders(200, -1);
                        break;

                    case "orders":
                        String oid = map.get("id");
                        String o_pname = map.get("pname");
                        String odate = map.get("odate");
                        String o_cname = map.get("cname");
                        String o_sname = map.get("sname");
                        int o_num = Integer.parseInt(map.get("num"));
                        String state = map.get("state");// {state:-1} {state:0}
                        int result = DBConnect02.executeUpdate(
                                "UPDATE `test0310`.`orders` SET `ProductID` = ?, `DateTime` = ?, `Num` = ?, `CustomerID` = ?, `ShipperID` = ?, `state` = ? WHERE (`idOrders` = ?)"
                                        + //
                                        "",
                                o_pname, odate, o_num, o_cname, o_sname, state, oid);
                        exchange.sendResponseHeaders(200, -1);
                        break;

                    default:
                        String response = "{\"error\": \"Unknown table: " + targetTable + "\"}";
                        exchange.sendResponseHeaders(400, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes(StandardCharsets.UTF_8));
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                String response = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

            }
        }
    }
}