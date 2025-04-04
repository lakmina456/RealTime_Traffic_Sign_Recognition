package com.example.signme;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class Cloud_Connection {

    // Google Cloud SQL Database details
    // Database connection details
    private static final String DATABASE_URL = "jdbc:mysql://34.55.25.64:3306/tsr_system3";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    // Method to establish a connection
    public Connection CONN() {
        Connection connection = null;
        try {
            // Load the MySQL driver
            Class.forName("com.mysql.jdbc.Driver");

            // Establish the connection
            connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
            System.out.println("Connection established successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Connection failed!");
            e.printStackTrace();
        }

        return connection;
    }
}
