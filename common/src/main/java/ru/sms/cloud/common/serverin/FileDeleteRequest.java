package ru.sms.cloud.common.serverin;

import ru.sms.cloud.common.AbstractMessage;

public class FileDeleteRequest extends AbstractMessage {
    private String filename;

    public String getFilename() {
        return filename;
    }

    public FileDeleteRequest(String filename) {
        this.filename = filename;
    }
}
