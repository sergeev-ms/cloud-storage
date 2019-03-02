package ru.sms.cloud.common.serverin;

import ru.sms.cloud.common.AbstractMessage;

public class AuthRequest extends AbstractMessage {
    private String userName;
    private String pass;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public AuthRequest(String userName, String pass) {
        this.userName = userName;
        this.pass = pass;
    }
}
