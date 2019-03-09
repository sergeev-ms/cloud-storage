package ru.sms.cloud.server.data;

import ru.sms.cloud.server.auth.Auth;

import java.sql.*;

public class JdbcAuthImplementation implements Auth {
    private Connection connection;

    public JdbcAuthImplementation(Connection connection) {
        this.connection = connection;
    }

    @Override
    public String getPass(String userName) {
        return null;
    }

    @Override
    public void putUser(String userName, String pass) {

    }

    @Override
    public boolean tryAuth(String userName, String pass) throws SQLException {
        String sqlStatement = "SELECT PASS FROM Users WHERE NAME = ?";
        PreparedStatement statement = connection.prepareStatement(sqlStatement);
        statement.setString(1, userName);
        ResultSet resultSet = statement.executeQuery();
        boolean isAuth = false;
        if (resultSet.next()) {
            isAuth = resultSet.getString(1).equals(pass);
            resultSet.close();
        }
        statement.close();
        return isAuth;
    }

    public void createUsersTable() throws SQLException {
        final Statement statement = connection.createStatement();
        final String sqlStatement = "CREATE TABLE IF NOT EXISTS Users(" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "NAME TEXT NOT NULL," +
                "PASS INTEGER NOT NULL" +
                ");";
        statement.execute(sqlStatement);
        statement.close();
    }

    public void insertTestData(String userName, String pass) throws SQLException {
        PreparedStatement statementSelect = connection.prepareStatement("SELECT * FROM Users WHERE NAME = ?");
        statementSelect.setString(1, userName);
        ResultSet resultSet = statementSelect.executeQuery();
        boolean next = resultSet.next();
        resultSet.close();
        statementSelect.close();
        if (next) return;


        PreparedStatement statementInsert = connection.prepareStatement("INSERT INTO Users (NAME, PASS) VALUES (?, ?)");
        statementInsert.setString(1, userName);
        statementInsert.setString(2, pass);
        int res = statementInsert.executeUpdate();
        statementInsert.close();
    }
}
