package ru.sms.cloud.server;

import ru.sms.cloud.server.data.JdbcAuthImplementation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class Database {
    public static final Database INSTANCE = new Database();
    private static final String JDBC = "org.sqlite.JDBC";
    private static final String URL = "jdbc:sqlite:database/users.db";
    private JdbcAuthImplementation auth;


    public Database(){
        try {
            Class.forName(JDBC);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL);
            auth = new JdbcAuthImplementation(connection);
            auth.createUsersTable();
            auth.insertTestData("user1", "12345");
            auth.insertTestData("user2", "23456");
        } catch (SQLException e) {
            auth = null;
            e.printStackTrace();
        }
    }
    public boolean tryAuth(String userName, String pass) throws SQLException {
        if (auth == null)
            return false;
        return auth.tryAuth(userName, pass);
    }
}
