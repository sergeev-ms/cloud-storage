package ru.sms.cloud.server.auth;

public interface Auth{
    String getPass(String userName);

    void putUser(String userName, String pass);

    boolean tryAuth(String userName, String pass);
}
