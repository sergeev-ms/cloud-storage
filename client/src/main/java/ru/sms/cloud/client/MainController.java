package ru.sms.cloud.client;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import ru.sms.cloud.common.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    public static final String CLIENT_STORAGE = "client_storage/";

    @FXML
    ListView<String> clFilesList;

    @FXML
    ListView<String> serverFilesList;

    @FXML
    Button upBtn;

    @FXML
    Button downBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        upBtn.disableProperty().bind(Bindings.size(clFilesList.getSelectionModel().getSelectedItems()).isEqualTo(0));
        downBtn.disableProperty().bind(Bindings.size(serverFilesList.getSelectionModel().getSelectedItems()).isEqualTo(0));
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

    public void refreshFilesOnServerAction(ActionEvent actionEvent){
        refreshFilesOnServer();
    }

    private void refreshFilesOnServer() {
        Network.sendMsg(new FileListRequest());
    }

    public void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            getFileList();
        }
        else {
            Platform.runLater(this::getFileList);
        }
    }

    public void refreshFilesListFromServer(List<String> names) {
        ObservableList<String> items = serverFilesList.getItems();
        items.clear();
        items.addAll(names);
    }
    private void getFileList() {
        try {
            clFilesList.getItems().clear();
            Files.list(Paths.get(CLIENT_STORAGE)).map(p -> p.getFileName().toString()).forEach(o -> clFilesList.getItems().add(o));
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
        refreshFilesOnServer();
    }

    public void downBtnAction(ActionEvent actionEvent) {
        ObservableList<String> selectedItems = serverFilesList.getSelectionModel().getSelectedItems();
        selectedItems.forEach(fileName -> Network.sendMsg(new FileRequest(fileName)));
    }
}
