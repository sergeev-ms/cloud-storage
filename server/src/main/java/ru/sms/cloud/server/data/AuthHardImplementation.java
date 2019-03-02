package ru.sms.cloud.server.data;

import ru.sms.cloud.server.auth.Auth;

import java.util.HashMap;
import java.util.Map;

public class AuthHardImplementation implements Auth {
    private static Map<String, String> users = new HashMap<>();
    {
        users.put("user1", "12345");
        users.put("user2", "23456");
    }
     public String getPass(String username){
        return users.get(username);
     }

     public void putUser(String username, String pass){
        users.put(username, pass);
     }

    @Override
    public boolean tryAuth(String userName, String pass) {
        String password = getPass(userName);
        if (password == null)
            return false;
        else return password.equals(pass);
    }
}
