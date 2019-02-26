package ru.sms.cloud.common.serverout;

import ru.sms.cloud.common.AbstractMessage;

import java.util.List;

public class FileListAnswer extends AbstractMessage {
    private List<String> fileNames;

    public FileListAnswer(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }
}
