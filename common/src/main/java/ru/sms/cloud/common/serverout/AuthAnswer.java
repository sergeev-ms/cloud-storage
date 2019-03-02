package ru.sms.cloud.common.serverout;

import ru.sms.cloud.common.AbstractMessage;

public class AuthAnswer extends AbstractMessage {
    boolean result;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public AuthAnswer(boolean result, String message) {
        this.result = result;
        this.message = message;
    }
}
