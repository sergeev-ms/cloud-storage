package ru.sms.cloud.common.serverin;

import ru.sms.cloud.common.AbstractMessage;

public class FileRequest extends AbstractMessage {
    private String filename;

    public String getFilename() {
        return filename;
    }

    public FileRequest(String filename) {
        this.filename = filename;
    }
}
