package com.czipperz.musicmanager;

import com.mpatric.mp3agic.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;
import se.vidstige.jadb.RemoteFile;

import java.io.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hello world!
 */
public class App extends Application {
    private static final Color primaryColor = Color.web("#59B55D");
    private ThreadPoolExecutor executor = null;

    public static void main(String[] args) {
        Application.launch(App.class);
    }

    public void stop() {
        if (executor != null) {
            try {
                while (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("Waiting to finish execute");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Font font = Font.font("Arial", 20);

        SimpleBooleanProperty disableWorld = new SimpleBooleanProperty(true);

        VBox frame = new VBox();
        frame.setPadding(new Insets(12));
        frame.setSpacing(12);

        HBox directoryPicker = new HBox();
        directoryPicker.setSpacing(12);
        directoryPicker.setPrefWidth(Double.MAX_VALUE);
        frame.getChildren().add(directoryPicker);

        File[] selectedDirectory = new File[]{null};
        TextField directoryField = new TextField("Click on me to select a directory");
        directoryField.setEditable(false);
        directoryField.setFont(font);
        directoryPicker.setHgrow(directoryField, Priority.ALWAYS);
        disableWorld.addListener((observable, oldValue, newValue) -> directoryField.setDisable(newValue));
        directoryPicker.getChildren().add(directoryField);

        HBox modeSelectBox = new HBox();
        modeSelectBox.setSpacing(12);
        frame.getChildren().add(modeSelectBox);

        Button youtubeManagerButton = new Button("Youtube");
        setButtonGraphic(youtubeManagerButton, "youtube.png");
        youtubeManagerButton.setFont(font);
        youtubeManagerButton.setMaxWidth(Double.MAX_VALUE);
        youtubeManagerButton.disableProperty().bind(disableWorld);
        modeSelectBox.setHgrow(youtubeManagerButton, Priority.ALWAYS);
        modeSelectBox.getChildren().add(youtubeManagerButton);

        Button musicManagerButton = new Button("Music Manager");
        setButtonGraphic(musicManagerButton, "music.png");
        musicManagerButton.setFont(font);
        musicManagerButton.setMaxWidth(Double.MAX_VALUE);
        musicManagerButton.disableProperty().bind(disableWorld);
        modeSelectBox.setHgrow(musicManagerButton, Priority.ALWAYS);
        modeSelectBox.getChildren().add(musicManagerButton);

        Button syncManagerButton = new Button("Sync");
        setButtonGraphic(syncManagerButton, "sync.png");
        syncManagerButton.setFont(font);
        syncManagerButton.setMaxWidth(Double.MAX_VALUE);
        syncManagerButton.disableProperty().bind(disableWorld);
        modeSelectBox.setHgrow(syncManagerButton, Priority.ALWAYS);
        modeSelectBox.getChildren().add(syncManagerButton);

        Node youtubeManager = makeYoutubeManager(youtubeManagerButton);
        frame.getChildren().add(youtubeManager);
        Parent musicManager = makeMusicManager(font, primaryStage, selectedDirectory, musicManagerButton, disableWorld);
        frame.getChildren().add(musicManager);
        Parent syncManager = makeSyncManager(syncManagerButton);
        frame.getChildren().add(syncManager);

        directoryField.setOnMouseClicked(ignore -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(selectedDirectory[0]);
            directoryChooser.setTitle("Music Directory");
            File folder = directoryChooser.showDialog(primaryStage);
            if (folder != null) {
                disableWorld.set(false);
                youtubeManager.setVisible(false);
                musicManager.setVisible(false);
                syncManager.setVisible(false);
                selectedDirectory[0] = folder;
                directoryField.setText(folder.toString());
            }
        });

        EventHandler<? super MouseEvent> youtubeButtonOnMouseClicked = youtubeManagerButton.getOnMouseClicked();
        youtubeManagerButton.setOnMouseClicked(e -> {
            youtubeManager.setVisible(true);
            musicManager.setVisible(false);
            syncManager.setVisible(false);
            if (youtubeButtonOnMouseClicked != null) {
                youtubeButtonOnMouseClicked.handle(e);
            }
        });
        EventHandler<? super MouseEvent> musicManagerButtonOnMouseClicked = musicManagerButton.getOnMouseClicked();
        musicManagerButton.setOnMouseClicked(e -> {
            youtubeManager.setVisible(false);
            musicManager.setVisible(true);
            syncManager.setVisible(false);
            if (musicManagerButtonOnMouseClicked != null) {
                musicManagerButtonOnMouseClicked.handle(e);
            }
        });
        EventHandler<? super MouseEvent> synchronizeButtonOnMouseClicked = syncManagerButton.getOnMouseClicked();
        syncManagerButton.setOnMouseClicked(e -> {
            youtubeManager.setVisible(false);
            musicManager.setVisible(false);
            syncManager.setVisible(true);
            if (synchronizeButtonOnMouseClicked != null) {
                synchronizeButtonOnMouseClicked.handle(e);
            }
        });

        Scene scene = new Scene(frame);
        scene.getStylesheets().add("style.css");
        primaryStage.setScene(scene);
        primaryStage.setWidth(840);
        primaryStage.setHeight(640);
        primaryStage.setTitle("Music Manager");
        primaryStage.show();
        primaryStage.getIcons().add(new Image("ic_launcher48.png"));
        primaryStage.getIcons().add(new Image("ic_launcher72.png"));
        primaryStage.getIcons().add(new Image("ic_launcher96.png"));
        primaryStage.getIcons().add(new Image("ic_launcher144.png"));
        primaryStage.getIcons().add(new Image("ic_launcher192.png"));
        primaryStage.getIcons().add(new Image("ic_launcher512.png"));

        new Thread(() -> {
            try {
                new ProcessBuilder("adb.exe", "start-server").start().waitFor();
                JadbConnection adb = new JadbConnection();
                List<JadbDevice> devices = adb.getDevices();
                for (JadbDevice device : devices) {
                    List<RemoteFile> list = device.list("/storage/emulated/0/Music");
                    for (RemoteFile f : list) {
                        if (!f.getPath().equals(".") && !f.getPath().equals("..")) {
                            System.out.printf("'%s'\n", f.getPath());
                        }
                    }
                }
                new ProcessBuilder("adb.exe", "kill-server").start().waitFor();
            } catch (IOException | JadbException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setButtonGraphic(Button button, String url) {
        ImageView imageView = new ImageView(new Image(url));

        ColorAdjust monochrome = new ColorAdjust();
        monochrome.setSaturation(-1.0);

        imageView.setEffect(new Blend(
                BlendMode.SRC_ATOP,
                monochrome,
                new ColorInput(
                        0, 0,
                        imageView.getImage().getWidth(), imageView.getImage().getHeight(),
                        primaryColor)));

        imageView.setCache(true);
        imageView.setCacheHint(CacheHint.SPEED);

        button.setGraphic(imageView);
    }

    private VBox makeYoutubeManager(Node directorySelectButton) {
        VBox grid = new VBox();
        grid.managedProperty().bind(grid.visibleProperty());
        grid.setVisible(false);
        grid.setSpacing(12);
        return grid;
    }

    private VBox makeSyncManager(Node directorySelectButton) {
        VBox grid = new VBox();
        grid.managedProperty().bind(grid.visibleProperty());
        grid.setVisible(false);
        grid.setSpacing(12);
        return grid;
    }

    private VBox makeMusicManager(Font font, Stage primaryStage, File[] selectedDirectory,
                                  Node directorySelectButton, SimpleBooleanProperty disableWorld) {
        VBox grid = new VBox();
        grid.managedProperty().bind(grid.visibleProperty());
        grid.setVisible(false);
        grid.setSpacing(12);

        HBox middlePane = new HBox();
        middlePane.setSpacing(12);
        grid.setVgrow(middlePane, Priority.ALWAYS);
        grid.getChildren().add(middlePane);

        VBox lists = new VBox();
        lists.setSpacing(12);
        middlePane.setHgrow(lists, Priority.SOMETIMES);
        middlePane.getChildren().add(lists);

        VBox errorsPane = new VBox();
        errorsPane.setSpacing(4);
        errorsPane.managedProperty().bind(errorsPane.visibleProperty());
        errorsPane.setVisible(false);
        lists.getChildren().add(errorsPane);

        Label errorsLabel = new Label("Errors");
        errorsLabel.setFont(font);
        errorsPane.getChildren().add(errorsLabel);

        ListView<String> errors = new ListView<>();
        errors.setCellFactory(cell -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setFont(font);
            }
        });
        errors.setItems(FXCollections.observableArrayList());
        errorsPane.getChildren().add(errors);

        VBox warningsPane = new VBox();
        warningsPane.setSpacing(4);
        warningsPane.managedProperty().bind(warningsPane.visibleProperty());
        warningsPane.setVisible(false);
        lists.getChildren().add(warningsPane);

        Label warningsLabel = new Label("Warnings");
        warningsLabel.setFont(font);
        warningsPane.getChildren().add(warningsLabel);

        ListView<String> warnings = new ListView<>();
        warnings.setCellFactory(cell -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setFont(font);
            }
        });
        warnings.setItems(FXCollections.observableArrayList());
        warnings.getItems().addListener((ListChangeListener<String>) ignore -> warningsPane.setVisible(!warnings.getItems().isEmpty()));
        warningsPane.getChildren().add(warnings);

        int[] editingSong = new int[]{-1, -1};
        BorderPane editPane = new BorderPane();
        editPane.managedProperty().bind(editPane.visibleProperty());
        editPane.setVisible(false);
        middlePane.setHgrow(editPane, Priority.SOMETIMES);
        middlePane.getChildren().add(editPane);

        VBox editBox = new VBox();
        editBox.setSpacing(12);
        editPane.setTop(editBox);

        VBox fileNameBox = new VBox();
        editBox.getChildren().add(fileNameBox);
        Label fileNameLabel = new Label("File name");
        fileNameLabel.setFont(font);
        fileNameBox.getChildren().add(fileNameLabel);
        TextField fileNameField = new TextField();
        fileNameField.setFont(font);
        fileNameBox.getChildren().add(fileNameField);

        VBox suggestedBox = new VBox();
        suggestedBox.managedProperty().bind(suggestedBox.visibleProperty());
        suggestedBox.setVisible(false);
        editBox.getChildren().add(suggestedBox);
        Label suggestedLabel = new Label("Suggested file name");
        suggestedBox.getChildren().add(suggestedLabel);
        TextField suggestedField = new TextField("Artist - Title.mp3");
        suggestedField.setEditable(false);
        suggestedField.setOnMouseClicked(ignore -> {
            fileNameField.setText(suggestedField.getText());
            suggestedField.setText("");
            suggestedBox.setVisible(false);
        });
        suggestedBox.getChildren().add(suggestedField);

        VBox errorBox = new VBox();
        errorBox.managedProperty().bind(errorBox.visibleProperty());
        Label errorLabel = new Label("Error, must follow file format");
        errorBox.getChildren().add(errorLabel);
        TextField errorField = new TextField("Artist - Title.mp3");
        errorField.setEditable(false);
        errorBox.getChildren().add(errorField);
        editBox.getChildren().add(errorBox);

        VBox artistBox = new VBox();
        editBox.getChildren().add(artistBox);
        Label artistLabel = new Label("Artist");
        artistLabel.setFont(font);
        artistBox.getChildren().add(artistLabel);
        TextField artistField = new TextField();
        artistField.setFont(font);
        artistField.setEditable(false);
        artistBox.getChildren().add(artistField);

        VBox titleBox = new VBox();
        editBox.getChildren().add(titleBox);
        Label titleLabel = new Label("Title");
        titleLabel.setFont(font);
        titleBox.getChildren().add(titleLabel);
        TextField titleField = new TextField();
        titleField.setFont(font);
        titleField.setEditable(false);
        titleBox.getChildren().add(titleField);

        HBox moveFileBox = new HBox();
        editPane.setBottom(moveFileBox);
        Button moveFileButton = new Button("Move File");
        moveFileButton.setFont(font);
        moveFileButton.setVisible(true);
        moveFileButton.setMaxWidth(Double.MAX_VALUE);
        moveFileBox.setHgrow(moveFileButton, Priority.ALWAYS);
        moveFileBox.getChildren().add(moveFileButton);

        Button applyButton = new Button("Apply");
        applyButton.setFont(font);
        applyButton.disableProperty().bind(disableWorld);
        applyButton.setMaxWidth(Double.MAX_VALUE);
        grid.getChildren().add(applyButton);

        ProgressBar directorySelectProgress = new ProgressBar();
        directorySelectProgress.setProgress(0);
        directorySelectProgress.setMaxWidth(Double.MAX_VALUE);
        grid.getChildren().add(directorySelectProgress);

        errors.getItems().addListener((ListChangeListener<String>) ignore -> {
            errorsPane.setVisible(!errors.getItems().isEmpty());
            disableWorld.set(!errors.getItems().isEmpty());
        });

        moveFileButton.setOnMouseClicked(ignore -> {
            String oldName;
            if (editingSong[0] == -1) {
                oldName = warnings.getItems().get(editingSong[1]);
            } else {
                oldName = errors.getItems().get(editingSong[0]);
            }
            File oldFile = new File(selectedDirectory[0], oldName);
            File newFile = new File(selectedDirectory[0], fileNameField.getText());
            System.out.printf("Move '%s' to '%s'\n", oldFile, newFile);
            oldFile.renameTo(newFile);
            if (editingSong[0] == -1) {
                warnings.getItems().remove(editingSong[1]);
                warnings.getSelectionModel().clearSelection();
            } else {
                errors.getItems().remove(editingSong[0]);
                errors.getSelectionModel().clearSelection();
            }
            editPane.setVisible(false);
        });

        fileNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            ArtistTitlePair pair = parseArtistTitle(newValue);
            if (pair != null) {
                artistField.setText(pair.artist);
                titleField.setText(pair.title);
                errorBox.setVisible(false);
                moveFileButton.setDisable(false);
            } else {
                artistField.setText("");
                titleField.setText("");
                errorBox.setVisible(true);
                moveFileButton.setDisable(true);
            }
        });

        errors.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            editingSong[0] = newValue.intValue();
            editingSong[1] = -1;
            // This happens when we remove the selected song.
            // This happens when a new folder is loaded via list.clear() or save is clicked, calling list.delete().
            if (editingSong[0] == -1) {
                editPane.setVisible(false);
            } else {
                editPane.setVisible(true);
                String fileName = errors.getItems().get(editingSong[0]);
                try {
                    Mp3File mp3File = new Mp3File(new File(selectedDirectory[0], fileName).toString());
                    ID3v1 tags;
                    if (mp3File.hasId3v2Tag()) {
                        tags = mp3File.getId3v2Tag();
                    } else if (mp3File.hasId3v1Tag()) {
                        tags = mp3File.getId3v1Tag();
                    } else {
                        throw new IOException(String.format("%s: Metadata is not readable", fileName));
                    }
                    String artist = tags.getArtist();
                    String title = tags.getTitle();
                    StringBuilder builder = new StringBuilder(artist).append(" - ").append(title).append(".mp3");
                    removeColons(builder);
                    suggestedField.setText(builder.toString());
                    suggestedBox.setVisible(!suggestedField.getText().equals(fileName));
                } catch (IOException | UnsupportedTagException | InvalidDataException e) {
                    suggestedField.setText("");
                    suggestedBox.setVisible(false);
                    e.printStackTrace();
                }
                fileNameField.setText(fileName);
            }
        });

        warnings.getFocusModel().focusedIndexProperty().addListener((observable, oldValue, newValue) -> {
            System.out.printf("Select %d\n", newValue);
        });
        warnings.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            editingSong[0] = -1;
            editingSong[1] = newValue.intValue();
            // This happens when we remove the selected song.
            // This happens when a new folder is loaded via list.clear() or save is clicked, calling list.delete().
            if (editingSong[1] == -1) {
                editPane.setVisible(false);
            } else {
                editPane.setVisible(true);
                String fileName = warnings.getItems().get(editingSong[1]);
                try {
                    Mp3File mp3File = new Mp3File(new File(selectedDirectory[0], fileName).toString());
                    ID3v1 tags;
                    if (mp3File.hasId3v2Tag()) {
                        tags = mp3File.getId3v2Tag();
                    } else if (mp3File.hasId3v1Tag()) {
                        tags = mp3File.getId3v1Tag();
                    } else {
                        throw new IOException(String.format("%s: Metadata is not readable", fileName));
                    }
                    String artist = tags.getArtist();
                    String title = tags.getTitle();
                    StringBuilder builder = new StringBuilder(artist).append(" - ").append(title).append(".mp3");
                    removeColons(builder);
                    suggestedField.setText(builder.toString());
                    suggestedBox.setVisible(!suggestedField.getText().equals(fileName));
                } catch (IOException | UnsupportedTagException | InvalidDataException e) {
                    suggestedField.setText("");
                    suggestedBox.setVisible(false);
                    e.printStackTrace();
                }
                fileNameField.setText(fileName);
            }
        });

        directorySelectButton.setOnMouseClicked(ignore -> {
            if (selectedDirectory[0] != null) {
                File[] files = selectedDirectory[0].listFiles();
                if (files != null) {
                    editPane.setVisible(false);
                    errors.getItems().clear();
                    warnings.getItems().clear();
                    disableWorld.set(true);
                    final int numThreads = 4;
                    AtomicInteger runningThreads = new AtomicInteger(numThreads);
                    Task[] tasks = new Task[numThreads];
                    for (int i = 0; i < numThreads; ++i) {
                        final int finalI = i;
                        tasks[i] = new Task() {
                            @Override
                            protected Object call() {
                                int numProcessed = 0;
                                for (int j = finalI; j < files.length; j += numThreads) {
                                    File file = files[j];
                                    if (file.isFile() && file.toString().endsWith(".mp3")) {
                                        try {
                                            file = removeStartingBrackets(file);
                                            file = removeEndingBrackets(file);
                                            file = processStartingNumber(file);
                                        } catch (InvalidDataException | IOException | UnsupportedTagException | NotSupportedException e) {
                                            System.out.println(file);
                                            e.printStackTrace();
                                        } catch (IllegalArgumentException e) {
                                            Stage dialog = new Stage();
                                            dialog.initModality(Modality.APPLICATION_MODAL);
                                            dialog.initOwner(primaryStage);
                                            VBox dialogPane = new VBox();
                                            dialogPane.setPadding(new Insets(12));

                                            Label label = new Label("Corrupted file");
                                            label.setWrapText(true);
                                            label.setFont(font);
                                            dialogPane.getChildren().add(label);

                                            TextField textField = new TextField(file.toString());
                                            textField.setFont(font);
                                            dialogPane.getChildren().add(textField);

                                            dialog.setScene(new Scene(dialogPane, 400, 200));
                                            dialog.setTitle("Error");
                                            dialog.show();

                                            e.printStackTrace();
                                        }
                                        String name = file.getName();
                                        if (!name.contains(" - ")) {
                                            Platform.runLater(() -> {
                                                errors.getItems().add(name);
                                            });
                                        } else if (name.toLowerCase().contains("lyric") || name.toLowerCase().contains("hq")) {
                                            Platform.runLater(() -> {
                                                warnings.getItems().add(name);
                                            });
                                        }
                                    }
                                    ++numProcessed;
                                    updateProgress(numProcessed, files.length);
                                }
                                if (runningThreads.decrementAndGet() == 1) {
                                    Platform.runLater(() -> {
                                        disableWorld.set(!errors.getItems().isEmpty());
                                        System.out.println("Finish Load Directory");
                                        directorySelectProgress.progressProperty().unbind();
                                        directorySelectProgress.progressProperty().setValue(1);
                                    });
                                }
                                return null;
                            }
                        };
                    }
                    DoubleBinding progress = new ReadOnlyDoubleWrapper(0).add(0);
                    for (int i = 0; i < numThreads; ++i) {
                        progress = progress.add(tasks[i].progressProperty());
                        new Thread(tasks[i]).start();
                    }
                    directorySelectProgress.progressProperty().bind(progress);
                }
            }
        });

        applyButton.setOnMouseClicked(ignore -> {
            disableWorld.set(true);
            System.out.println("Apply");
            final File[] files = selectedDirectory[0].listFiles();
            if (files != null) {
                final File playlistFile = new File(selectedDirectory[0], String.format("%s.m3u", selectedDirectory[0].getName()));
                final int taskLength = files.length + (playlistFile.exists() ? 0 : 1);
                final int numThreads = 4;
                AtomicInteger runningThreads = new AtomicInteger(numThreads + 1);
                Task[] tasks = new Task[numThreads];
                for (int i = 0; i < numThreads; ++i) {
                    final int i1 = i;
                    tasks[i] = new Task() {
                        @Override
                        protected Object call() {
                            updateProgress(0, taskLength);
                            int numProcessed = 0;
                            for (int j = i1; j < files.length; j += numThreads) {
                                File file = files[j];
                                if (file.toString().endsWith(".mp3")) {
                                    try {
                                        saveArtistTitle(file);
                                    } catch (InvalidDataException | IOException | NotSupportedException | UnsupportedTagException e) {
                                        System.out.println(file);
                                        e.printStackTrace();
                                    }
                                } else if (file.equals(playlistFile)) {
                                    continue;
                                }
                                ++numProcessed;
                                updateProgress(numProcessed, taskLength);
                            }
                            if (runningThreads.decrementAndGet() == 1) {
                                Platform.runLater(() -> {
                                    System.out.println("Finish Apply Changes");
                                    directorySelectProgress.progressProperty().unbind();
                                    directorySelectProgress.progressProperty().setValue(1);
                                    disableWorld.set(false);
                                });
                            }
                            return null;
                        }
                    };
                }
                Task<Void> playlistTask = new Task<Void>() {
                    @Override
                    protected Void call() {
                        updateProgress(0, taskLength);
                        System.out.printf("Playlist: %s\n", playlistFile);
                        try (Writer fileWriter = new BufferedWriter(new FileWriter(playlistFile))) {
                            for (File file : files) {
                                if (file.getName().endsWith(".mp3")) {
                                    fileWriter.write(file.getName());
                                    fileWriter.write('\n');
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        updateProgress(1, taskLength);
                        if (runningThreads.decrementAndGet() == 1) {
                            Platform.runLater(() -> {
                                System.out.println("Finish Apply Changes");
                                directorySelectProgress.progressProperty().unbind();
                                directorySelectProgress.progressProperty().setValue(1);
                                disableWorld.set(false);
                            });
                        }
                        return null;
                    }
                };
                new Thread(playlistTask).start();
                DoubleBinding progress = playlistTask.progressProperty().add(0);
                for (int i = 0; i < numThreads; ++i) {
                    progress = progress.add(tasks[i].progressProperty());
                    new Thread(tasks[i]).start();
                }
                directorySelectProgress.progressProperty().bind(progress);
            }
        });

        grid.requestFocus();

        return grid;
    }

    private static File removeStartingBrackets(File file) {
        StringBuilder name = new StringBuilder(file.getName());
        if (name.charAt(0) == '[') {
            int end = name.indexOf("]");
            for (++end; end < name.length(); ++end) {
                if (!Character.isWhitespace(name.charAt(end)) && name.charAt(end) != '-') {
                    break;
                }
            }
            name.delete(0, end);
            File newFile = new File(file.getParent(), name.toString());
            System.out.printf("removeStartingBrackets(): Rename '%s' to '%s'\n", file, newFile);
            file.renameTo(newFile);
            return newFile;
        }
        return file;
    }

    private static File removeEndingBrackets(File file) {
        StringBuilder name = new StringBuilder(file.getName());
        if (name.substring(name.length() - "].mp3".length()).equals("].mp3")) {
            int start = name.lastIndexOf("[");
            if (start != -1) {
                for (--start; start >= 0; --start) {
                    if (!Character.isWhitespace(name.charAt(start))) {
                        ++start;
                        break;
                    }
                }
                name.delete(start, name.length() - ".mp3".length());
                File newFile = new File(file.getParent(), name.toString());
                System.out.printf("removeEndingBrackets(): Rename '%s' to '%s'\n", file, newFile);
                file.renameTo(newFile);
                return newFile;
            }
        }
        return file;
    }

    private static File processStartingNumber(File file) throws UnsupportedTagException, NotSupportedException, InvalidDataException, IOException {
        String name = file.getName();
        if (Character.isDigit(name.charAt(0)) && Character.isDigit(name.charAt(1)) &&
                Character.isDigit(name.charAt(2)) && name.substring(3, 6).equals(" - ")) {
            Mp3File mp3File = new Mp3File(file);
            ID3v1 tag;
            if (mp3File.hasId3v2Tag()) {
                tag = mp3File.getId3v2Tag();
            } else if (mp3File.hasId3v1Tag()) {
                tag = mp3File.getId3v1Tag();
            } else {
                tag = new ID3v1Tag();
                mp3File.setId3v1Tag(tag);
            }
            if (tag.getTrack().equals(name.substring(0, 3))) {
                File dest = new File(String.format("%s/%s", file.getParent(), name.substring(6)));
                System.out.printf("processStartingNumber: Rename %s to %s\n", file, dest);
                tag.setTrack("");
                mp3File.save(dest.toString());
                if (!file.delete()) {
                    throw new IOException();
                }
                return dest;
            }
        }
        return file;
    }

    private void saveArtistTitle(File file) throws UnsupportedTagException, NotSupportedException, InvalidDataException, IOException {
        Mp3File mp3File = new Mp3File(file);
        ID3v1 tag;
        if (mp3File.hasId3v2Tag()) {
            tag = mp3File.getId3v2Tag();
        } else if (mp3File.hasId3v1Tag()) {
            tag = mp3File.getId3v1Tag();
        } else {
            tag = new ID3v1Tag();
            mp3File.setId3v1Tag(tag);
        }
        String name = file.getName();
        ArtistTitlePair pair = parseArtistTitle(name);
        if (pair == null) {
            throw new IOException(String.format("%s: No ' - ' found to separate the artist and the title", file));
        }
        boolean changed = false;
        if (!pair.artist.equals(tag.getArtist())) {
            System.out.printf("OA: '%s' ", tag.getArtist());
            tag.setArtist(pair.artist);
            changed = true;
        }
        if (!pair.title.equals(tag.getTitle())) {
            System.out.printf("OT: '%s' ", tag.getTitle());
            tag.setTitle(pair.title);
            changed = true;
        }
        if (changed) {
            System.out.printf("A: '%s' T: '%s'\n", pair.artist, pair.title);
            File temp = File.createTempFile("temp", ".mp3");
            mp3File.save(temp.toString());
            if (!file.delete()) {
                temp.delete();
                throw new IOException();
            }
            if (!temp.renameTo(file)) {
                temp.delete();
                throw new IOException();
            }
        }
    }

    private static class ArtistTitlePair {
        public String artist;
        public String title;

        public ArtistTitlePair(String artist, String title) {
            this.artist = artist;
            this.title = title;
        }
    }

    private ArtistTitlePair parseArtistTitle(String name) {
        int endArtist = name.indexOf(" - ");
        if (endArtist == -1 || !name.endsWith(".mp3")) {
            return null;
        }
        String artist = name.substring(0, endArtist);
        String title = name.substring(endArtist + " - ".length(), name.length() - ".mp3".length());
        return new ArtistTitlePair(artist, title);
    }

    private class FileProcessor {
        private File file;
        private Mp3File mp3File;
        private ID3v1 tag = null;

        public FileProcessor(File file) throws InvalidDataException, IOException, UnsupportedTagException, NotSupportedException {
            setup(file);
        }

        private void setup(File file) throws InvalidDataException, IOException, UnsupportedTagException, NotSupportedException {
            boolean failed = false;
            while (true) {
                this.mp3File = new Mp3File(file);
                if (this.mp3File.hasId3v2Tag()) {
                    this.tag = this.mp3File.getId3v2Tag();
                    break;
                } else if (this.mp3File.hasId3v1Tag()) {
                    this.tag = this.mp3File.getId3v1Tag();
                    break;
                } else if (failed) {
                    throw new IOException(String.format("%s: Unable to read or write metadata", file));
                } else {
                    this.mp3File.setId3v1Tag(new ID3v1Tag());
                    File temp = File.createTempFile("temp", ".mp3");
                    this.mp3File.save(temp.toString());
                    file.delete();
                    temp.renameTo(file);
                    file = temp;
                    failed = true;
                }
            }
            this.file = file;
        }
    }

    private static void removeColons(StringBuilder builder) {
        for (int i = 0; i < builder.length(); ) {
            if (builder.charAt(i) == ':') {
                builder.deleteCharAt(i);
            } else {
                ++i;
            }
        }
    }
}
