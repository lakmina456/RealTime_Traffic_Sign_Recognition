package com.example.signme;

import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;

public class ConnectionClass {
    protected static String db = "tsr_system3";
    protected static String ip = "127.0.0.1"; // Replace with server IP if not on the same device
    protected static String port = "3306";
    protected static String username = "root";
    protected static String password = "admin";

    public Connection CONN() {
        Connection conn = null;
        try {
            // Use the previous MySQL JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            // Add useSSL=false to remove SSL warning
            String connectionString = "jdbc:mysql://" + ip + ":" + port + "/" + db + "?useSSL=false";
            conn = DriverManager.getConnection(connectionString, username, password);
        } catch (Exception e) {
            Log.e("ERROR", Objects.requireNonNull(e.getMessage()));
        }
        return conn;
    }
}
