package ru.sms.cloud.client;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import ru.sms.cloud.common.*;
import ru.sms.cloud.common.serverin.FileDeleteRequest;
import ru.sms.cloud.common.serverin.FileListRequest;
import ru.sms.cloud.common.serverin.FileRequest;
import ru.sms.cloud.common.serverout.FileListAnswer;
import ru.sms.cloud.common.serverout.FileMessage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final String CLIENT_STORAGE = "client_storage/";

    @FXML
    ListView<String> clFilesList;

    @FXML
    ListView<String> serverFilesList;

    @FXML
    Button upBtn;

    @FXML
    Button downBtn;

    @FXML
    Button removeOnServerBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        upBtn.disableProperty().bind(
                Bindings.size(clFilesList.getSelectionModel().getSelectedItems()).isEqualTo(0));
        downBtn.disableProperty().bind(
                Bindings.size(serverFilesList.getSelectionModel().getSelectedItems()).isEqualTo(0));
        clFilesList.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue && newValue)
                serverFilesList.getSelectionModel().clearSelection();
        });
        serverFilesList.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue && newValue)
                clFilesList.getSelectionModel().clearSelection();
        });
        Network.start();

        refreshFilesOnServer();

        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileListAnswer){
                        FileListAnswer answer = (FileListAnswer) am;
                        refreshFilesListFromServer(answer.getFileNames());
                    }
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get(CLIENT_STORAGE + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
    }

    public void refreshAllFilesAction(ActionEvent actionEvent){
        refreshFilesOnServer();
        refreshLocalFilesList();
    }

    private void refreshFilesOnServer() {
        Network.sendMsg(new FileListRequest());
    }

    private void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            getFileList();
        }
        else {
            Platform.runLater(this::getFileList);
        }
    }

    private void refreshFilesListFromServer(List<String> names) {
        Platform.runLater(() -> {
            serverFilesList.getItems().clear();
            serverFilesList.getItems().addAll(names);
        });
    }

    private void getFileList() {
        try {
            clFilesList.getItems().clear();
            Files.list(Paths.get(CLIENT_STORAGE)).map(p ->
                    p.getFileName().toString()).forEach(o -> clFilesList.getItems().add(o));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void upBtnAction(ActionEvent actionEvent) {
        ObservableList<String> selectedItems = clFilesList.getSelectionModel().getSelectedItems();
        selectedItems.forEach(fileName -> {
            Path path = Paths.get(CLIENT_STORAGE + fileName);
            if (Files.exists(path)) {
                try {
                    Network.sendMsg(new FileMessage(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void downBtnAction(ActionEvent actionEvent) {
        ObservableList<String> selectedItems = serverFilesList.getSelectionModel().getSelectedItems();
        selectedItems.forEach(fileName -> Network.sendMsg(new FileRequest(fileName)));
    }

    private void removeLocalFile(ObservableList<String> items) {
        items.forEach(fileName -> {
            Path path = Paths.get(CLIENT_STORAGE + fileName);
            boolean exists = Files.exists(path);
            if (exists) {
                try {
                    Files.delete(path);
                    refreshLocalFilesList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void removeServerFilesAction(ActionEvent actionEvent) {
        ObservableList<String> selectedServerItems = serverFilesList.getSelectionModel().getSelectedItems();
        if (selectedServerItems.size() > 0)
            selectedServerItems.forEach(fileName -> Network.sendMsg(new FileDeleteRequest(fileName)));
        ObservableList<String> selectedClientItems = clFilesList.getSelectionModel().getSelectedItems();
        if (selectedClientItems.size() > 0)
            removeLocalFile(selectedClientItems);

    }
}
