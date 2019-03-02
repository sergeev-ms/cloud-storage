package ru.sms.cloud.client;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import ru.sms.cloud.common.AbstractMessage;
import ru.sms.cloud.common.serverin.AuthRequest;
import ru.sms.cloud.common.serverin.FileDeleteRequest;
import ru.sms.cloud.common.serverin.FileListRequest;
import ru.sms.cloud.common.serverin.FileRequest;
import ru.sms.cloud.common.serverout.AuthAnswer;
import ru.sms.cloud.common.serverout.FileListAnswer;
import ru.sms.cloud.common.serverout.FileMessage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final String CLIENT_STORAGE = "client_storage/";
    private boolean isLoggedIn = false;

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
    @FXML
    Button authBtn;
    @FXML
    Label statusBar;

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
        authBtn.disableProperty().bind(Bindings.createBooleanBinding(() -> isLoggedIn));
        refreshLocalFilesList();

        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof AuthAnswer) {
                        publishStatus(((AuthAnswer) am).getMessage());
                        this.isLoggedIn = true;
                    }

                    if (am instanceof FileListAnswer){
                        FileListAnswer answer = (FileListAnswer) am;
                        refreshFilesListFromServer(answer.getFileNames());
                        publishStatus("Files was refreshed");
                    }
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get(CLIENT_STORAGE + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        publishStatus("File was downloaded: " + fm.getFilename());
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
                    publishStatus("File was deleted from disk: " + fileName);
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

    public void upBtnAuth(ActionEvent actionEvent) {
        Dialog<Pair<String, String>> dialog = getLoginDialog();
        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(credentials -> {
            System.out.println("Username=" + credentials.getKey() + ", Password=" + credentials.getValue());
            Network.sendMsg(new AuthRequest(credentials.getKey(), credentials.getValue()));
        });
    }

    private Dialog<Pair<String, String>> getLoginDialog() {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText("Look, a Custom Login Dialog");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(username::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });
        return dialog;
    }

    private void publishStatus(String message){
        Platform.runLater(() -> statusBar.setText(message));
    }
}
