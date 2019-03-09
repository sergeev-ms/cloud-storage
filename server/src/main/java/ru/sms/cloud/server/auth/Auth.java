package ru.sms.cloud.server.auth;

import java.sql.SQLException;

public interface Auth{
    String getPass(String userName);

    void putUser(String userName, String pass);

    boolean tryAuth(String userName, String pass) throws SQLException;
}
