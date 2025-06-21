import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ShopMenu0430 {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            showMenu();
            int choice = getChoice();
            handleChoice(choice);
        }

    }

    private static void showMenu() {
        System.out.println("\n====== Tim Delivery Company ======");
        System.out.println("1. 運送條件與費用");
        System.out.println("2. 計算運費");
        System.out.println("3. 送貨員列表");
        System.out.println("4. 顯示會員清單");
        System.out.println("5. 新增訂單");
        System.out.println("6. 歷史訂單查詢");
        System.out.println("7. 離開");
        System.out.print("請選擇功能（輸入數字）：");
    }

    private static int getChoice() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("⚠️ 輸入錯誤，請輸入有效的數字！");
            return -1;
        }
    }

    private static void handleChoice(int choice) {
        switch (choice) {
            case 1 -> listProducts();
            case 2 -> calculatePrice();
            case 3 -> listShippers(true);
            case 4 -> listMembers();
            case 5 -> addOrder();
            case 6 -> listOrders();
            case 7 -> {
                System.out.println("👋 感謝使用，系統退出。");
                System.exit(0);
            }
            default -> System.out.println("⚠️ 無效選擇，請重新輸入！");
        }
    }

    private static void listProducts() {
        System.out.println();
        displayDataFromTable("length", "ID | 長寬高總和 | 價格", "idlength", "length", "price");
        System.out.println();
        displayDataFromTable("weight", "ID | 重量限制 | 價格", "idweight", "weight", "price");
        System.out.println();
        displayDataFromTable("product", "ID | 運送距離 | 價格", "idproduct", "ProductName", "Price");

        promptReturnOrExit();
    }

    private static void displayDataFromTable(String tableName, String header, String idColumn, String nameColumn,
            String priceColumn) {
        String query = "SELECT * FROM " + tableName;
        try (ResultSet rs = DBConnect02.selectQuery(query)) {
            System.out.println(header);
            while (rs.next()) {
                System.out.println(
                        rs.getInt(idColumn) + " | " + rs.getString(nameColumn) + " | $" + rs.getInt(priceColumn));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double calculatePriceWithNames(int routeId, int lengthId, int weightId) {
        String routeName = getRouteNameById(routeId);
        String lengthValue = getLengthValueById(lengthId);
        String weightName = getWeightNameById(weightId);

        if (routeName == null || lengthValue == null || weightName == null) {
            System.out.println("⚠️ 查詢條件轉換失敗，請檢查 ID 是否正確！");
            return 0.0;
        }

        String query = "SELECT price FROM product2 WHERE distance = '" + routeName +
                "' AND length = " + lengthValue +
                " AND weight = '" + weightName + "'";

        try (ResultSet rs = DBConnect02.selectQuery(query)) {
            if (rs.next()) {
                return rs.getDouble("price");
            } else {
                System.out.println("⚠️ 沒有找到匹配的運費資料，請確認輸入的條件是否正確！");
                return 0.0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    private static List<Integer> getValidIdsFromTable(String tableName, String idColumn) {
        List<Integer> ids = new ArrayList<>();
        String query = "SELECT " + idColumn + " FROM " + tableName;
        try (ResultSet rs = DBConnect02.selectQuery(query)) {
            while (rs.next()) {
                ids.add(rs.getInt(idColumn));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    private static int promptForValidId(String itemName, String tableName, String idColumn) {
        List<Integer> validIds = getValidIdsFromTable(tableName, idColumn);
        int id;
        while (true) {
            System.out.print("請輸入 " + itemName + " ID：");
            id = getChoice();
            if (validIds.contains(id)) {
                return id;
            } else {
                System.out.println("⚠️ 無效的 " + itemName + " ID，請再選擇一次！");
            }
        }
    }

    private static void calculatePrice() {
        System.out.println("\n====== 計算運費 ======");

        // 顯示並輸入路線
        System.out.println("\n🚚 運送路線選項（請選擇對應的 ID）：");
        displayDataFromTable("product", "ID | 運送距離 | 價格", "idproduct", "ProductName", "Price");
        int routeId = promptForValidId("運送路線", "product", "idproduct");

        // 顯示並輸入長度
        System.out.println("\n📏 長度條件列表（請選擇對應的 ID）：");
        displayDataFromTable("length", "ID | 長寬高總和 | 價格", "idlength", "length", "price");
        int lengthId = promptForValidId("長度", "length", "idlength");

        // 顯示並輸入重量
        System.out.println("\n⚖️ 重量條件列表（請選擇對應的 ID）：");
        displayDataFromTable("weight", "ID | 重量限制 | 價格", "idweight", "weight", "price");
        int weightId = promptForValidId("重量", "weight", "idweight");

        // 查詢價格
        double price = calculatePriceWithNames(routeId, lengthId, weightId);

        if (price > 0) {
            System.out.println("\n✅ 總價格為：$" + price);
        }

        promptReturnOrExit();
    }

    private static String getRouteNameById(int routeId) {
        String query = "SELECT ProductName FROM product WHERE idproduct = " + routeId;
        try (ResultSet rs = DBConnect02.selectQuery(query)) {
            if (rs.next()) {
                return rs.getString("ProductName");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getLengthValueById(int lengthId) {
        String query = "SELECT length FROM length WHERE idlength = " + lengthId;
        try (ResultSet rs = DBConnect02.selectQuery(query)) {
            if (rs.next()) {
                return rs.getString("length");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getWeightNameById(int weightId) {
        String query = "SELECT weight FROM weight WHERE idweight = " + weightId;
        try (ResultSet rs = DBConnect02.selectQuery(query)) {
            if (rs.next()) {
                return rs.getString("weight");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void listShippers(boolean returnToMenu) {
        try (ResultSet rs = DBConnect02.selectQuery("SELECT * FROM employee")) {
            System.out.println("ID | 送貨員名字 | 性別 | 生日 | 電話");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("idemployee") + " | " +
                                rs.getString("employeeName") + " | " +
                                rs.getString("employeeGender") + " | " +
                                rs.getString("employeeBirthday") + " | " +
                                rs.getString("employeePhone"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (returnToMenu) {
            promptReturnOrExit();
        }
    }

    private static String[] promptForMemberInfo() {
        List<Integer> memberIds = new ArrayList<>();
        String query = "SELECT idmember, membername, phone FROM member";

        System.out.println("\n🧑 會員清單：");
        try (ResultSet rs = DBConnect02.selectQuery(query)) {
            while (rs.next()) {
                int id = rs.getInt("idmember");
                String name = rs.getString("membername");
                String phone = rs.getString("phone");
                memberIds.add(id);
                System.out.println(id + " | " + name + " | " + phone);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        int memberId;
        while (true) {
            System.out.print("請輸入會員 ID：");
            memberId = getChoice();
            if (memberIds.contains(memberId)) {
                break;
            } else {
                System.out.println("⚠️ 無效的會員 ID，請重新輸入！");
            }
        }

        // 抓取會員名字與電話
        String name = getValueById("member", "membername", "idmember", memberId);
        String phone = getValueById("member", "phone", "idmember", memberId);
        String creditCard = getValueById("member", "creditcard", "idmember", memberId);
        System.out.println("✅ 選擇會員：" + name + "（電話：" + phone + "）");

        return new String[] { String.valueOf(memberId), name, phone, creditCard };
    }

    private static void addOrder() {
        System.out.println("\n====== 新增訂單 ======");

        int id = getNextId("orders", "idorders");

        // 顯示運送方案
        System.out.println("\n📦 可用運送方案（來自 product2）：");
        String query = "SELECT * FROM product2 ORDER BY idproduct2";
        try (ResultSet rs = DBConnect02.selectQuery(query)) {
            System.out.println("ID | 路線 | 長度 | 重量 | 價格");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("idproduct2") + " | " +
                                rs.getString("distance") + " | " +
                                rs.getString("length") + " | " +
                                rs.getString("weight") + " | $" +
                                rs.getInt("price"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            promptReturnOrExit();
            return;
        }

        // 使用者輸入 ProductID
        int productId = promptForValidId("產品", "product2", "idproduct2");

        // 自動帶出運送方案細節與價格
        String distance = getValueById("product2", "distance", "idproduct2", productId);
        String length = getValueById("product2", "length", "idproduct2", productId);
        String weight = getValueById("product2", "weight", "idproduct2", productId);
        int price = getIntValueById("product2", "price", "idproduct2", productId);

        String plan = distance + " | " + length + " | " + weight;

        // 選擇會員，並取得 ID、姓名、電話
        String[] memberInfo = promptForMemberInfo();
        if (memberInfo == null)
            return;
        int memberId = Integer.parseInt(memberInfo[0]);
        String customerName = memberInfo[1];
        String customerPhone = memberInfo[2];
        String creditCard = memberInfo[3];

        // 選擇送貨員
        System.out.println("\n可用送貨員列表：");
        listShippers(false);
        int employeeId = promptForValidId("送貨員", "employee", "idemployee");
        String employeeName = getValueById("employee", "employeeName", "idemployee", employeeId);

        // 顯示確認資訊
        System.out.println("\n====== 訂單明細確認 ======");
        System.out.println("訂單編號：" + id);
        System.out.println("運送方案：" + plan);
        System.out.println("價格：$" + price);
        System.out.println("會員：" + customerName + "（ID: " + memberId + "）");
        System.out.println("電話：" + customerPhone);
        System.out.println("配送人員：" + employeeName + "（ID: " + employeeId + "）");

        // 寫入訂單資料
        int result = DBConnect02.executeUpdate(
                "INSERT INTO orders (idorders, ProductID, Plan, Price, CustomerName, CustomerPhone, EmployeeID, EmployeeName, memberID, `Credit Card Number`) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, productId, plan, price, customerName, customerPhone, employeeId, employeeName, memberId,
                creditCard);

        if (result > 0) {
            System.out.println("\n🎉 訂單新增成功！");
        } else {
            System.out.println("\n❌ 新增失敗，請檢查資料！");
        }

        promptReturnOrExit();
    }

    private static void listMembers() {
        System.out.println("\n====== 會員清單 ======");
        try (ResultSet rs = DBConnect02.selectQuery("SELECT * FROM member")) {
            System.out.println("ID | 姓名 | 性別 | 生日 | Email | 電話 | 國籍 | 信用卡號");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("idmember") + " | " +
                                rs.getString("membername") + " | " +
                                rs.getString("gender") + " | " +
                                rs.getString("birthday") + " | " +
                                rs.getString("email") + " | " +
                                rs.getString("phone") + " | " +
                                rs.getString("country") + " | " +
                                rs.getString("creditcard"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        promptReturnOrExit();
    }

    private static String getValueById(String tableName, String columnName, String idColumnName, int id) {
        String query = "SELECT " + columnName + " FROM " + tableName + " WHERE " + idColumnName + " = ?";
        try (
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/shippingsystem", "root",
                        "123456");
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(columnName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int getIntValueById(String tableName, String columnName, String idColumnName, int id) {
        String query = "SELECT " + columnName + " FROM " + tableName + " WHERE " + idColumnName + " = ?";
        try (
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/shippingsystem", "root",
                        "123456");
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(columnName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static void listOrders() {
        try (ResultSet rs = DBConnect02.selectQuery("SELECT * FROM orders")) {
            System.out.println("ID | 產品 ID | 價格 | 客戶名 | 送貨員ID | 送貨員名");
            while (rs.next()) {
                System.out.println(rs.getInt("idOrders") + " | " + rs.getInt("ProductID") + " | $" + rs.getInt("Price")
                        + " | " + rs.getString("CustomerName") + " | " + rs.getInt("EmployeeID") + " | "
                        + rs.getString("EmployeeName"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 新增：查詢單筆訂單詳情
        System.out.print("\n是否要查詢某一筆訂單詳細資訊？(Y/N): ");
        String response = scanner.nextLine().trim();
        if (response.equalsIgnoreCase("Y")) {
            System.out.print("請輸入訂單編號 (idorders)：");
            int searchId = getChoice();

            String query = "SELECT * FROM orders WHERE idorders = ?";
            try (
                    Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/shippingsystem", "root",
                            "123456");
                    PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, searchId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    System.out.println("\n====== 訂單詳細資訊 ======");
                    System.out.println("訂單編號：" + rs.getInt("idorders"));
                    System.out.println("運送方案：" + rs.getString("Plan"));
                    System.out.println("產品 ID：" + rs.getInt("ProductID"));
                    System.out.println("價格：$" + rs.getInt("Price"));
                    System.out.println("客戶姓名：" + rs.getString("CustomerName"));
                    System.out.println("客戶電話：" + rs.getString("CustomerPhone"));
                    System.out.println("送貨員姓名：" + rs.getString("EmployeeName"));
                    System.out.println("會員 ID：" + rs.getInt("memberID"));
                    System.out.println("信用卡號：" + rs.getString("Credit Card Number"));
                } else {
                    System.out.println("⚠️ 查無此訂單編號！");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        promptReturnOrExit();
    }

    private static boolean promptReturnOrExit() {
        String response;
        do {
            System.out.print("\n是否回到主選單？(Y/N): ");
            response = scanner.nextLine().trim();
            if (response.equalsIgnoreCase("Y")) {
                return false; // 回到主選單
            } else if (response.equalsIgnoreCase("N")) {
                System.out.println("👋 感謝使用，系統退出。");
                System.exit(0); // 退出程式
            } else {
                System.out.println("⚠️ 無效輸入，請輸入 Y 或 N。");
            }
        } while (!response.equalsIgnoreCase("Y") && !response.equalsIgnoreCase("N"));
        return false;
    }

    private static int getNextId(String tableName, String columnName) {
        try (ResultSet rs = DBConnect02.selectQuery("SELECT MAX(" + columnName + ") FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }
}