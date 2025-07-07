import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DBConnect02 {
    // MySQL 連線資訊
    private static final String URL = "jdbc:mysql://localhost:3306/shippingsystem";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    static {
        try {
            // 載入 MySQL 驅動
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("⚠️ 無法載入 MySQL 驅動程式", e);
        }
    }

    /**
     * 執行 SELECT 查詢
     * 
     * @param query  SQL 查詢語句
     * @param params 可變參數（用於 PreparedStatement）
     * @return ResultSet 查詢結果
     */
    public static ResultSet selectQuery(String query, Object... params) {
        try {
            // 連線
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            // 下查詢語法
            PreparedStatement stmt = conn.prepareStatement(query);

            // 設定參數
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            // 執行查詢並回傳
            return stmt.executeQuery(); // ⚠️ 注意：ResultSet 需要手動關閉
        } catch (SQLException e) {
            throw new RuntimeException("⚠️ 查詢失敗：" + e.getMessage(), e);
        }
    }

    /**
     * 執行 INSERT / UPDATE / DELETE 操作
     * 
     * @param query  SQL 語句
     * @param params 可變參數（用於 PreparedStatement）
     * @return 受影響的行數
     */
    public static int executeUpdate(String query, Object... params) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(query)) {

            // ===== 加入 Debug 輸出 =====
            System.out.println("執行 SQL： " + query);
            System.out.println("參數數量：" + params.length);
            for (int i = 0; i < params.length; i++) {
                System.out.println("參數 " + (i + 1) + ": " + params[i]);
            }
            // ==========================

            // 設定參數
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("⚠️ 資料更新失敗：" + e.getMessage(), e);
        }
    }

    public static int getNextId(String tableName, String columnName) {
        int nextId = 1;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // 先取得 MAX + 1
            String maxSql = "SELECT COALESCE(MAX(" + columnName + "), 0) + 1 AS nextId FROM " + tableName;
            try (PreparedStatement maxStmt = conn.prepareStatement(maxSql);
                    ResultSet rs = maxStmt.executeQuery()) {
                if (rs.next()) {
                    nextId = rs.getInt("nextId");
                }
            }

            // 再保險: 確保這個 nextId 沒人用
            while (true) {
                String checkSql = "SELECT 1 FROM " + tableName + " WHERE " + columnName + " = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, nextId);
                    try (ResultSet checkRs = checkStmt.executeQuery()) {
                        if (!checkRs.next()) {
                            break; // 找到沒人用的 id
                        }
                    }
                }
                nextId++; // 該 id 已被使用，往後找
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("取得 nextId 失敗：" + e.getMessage(), e);
        }
        return nextId;
    }

    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim().replaceAll("[{}\"]", "");
        for (String pair : json.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2)
                map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    public static Map<String, Object> executeQuerySingle(String sql, Object... params) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();

            if (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    result.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        // 測試 SELECT 查詢
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM product");
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                System.out.println("ID: " + rs.getString("idproduct") + ", Name: " + rs.getString("ProductName"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 測試 INSERT
        int insertResult = executeUpdate("INSERT INTO product (idproduct, ProductName, Price) VALUES (?, ?, ?)", "102",
                "Keyboard", 29.99);
        System.out.println("🔹 插入成功，影響行數：" + insertResult);

        // 測試 UPDATE
        int updateResult = executeUpdate("UPDATE product SET ProductName = ? WHERE idproduct = ?", "Gaming Keyboard",
                "102");
        System.out.println("🔹 更新成功，影響行數：" + updateResult);

        // 測試 DELETE
        int deleteResult = executeUpdate("DELETE FROM product WHERE idproduct = ?", "102");
        System.out.println("🔹 刪除成功，影響行數：" + deleteResult);
    }
}
