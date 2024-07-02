package org.zyz;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.kordamp.bootstrapfx.scene.layout.Panel;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

public class PlayPage implements PageInterface {
    private static final Logger logger = LogManager.getLogger(PlayPage.class);
    private static final int PADDING = 0;
    private static final int MARGIN_TOP = -15;
    private static final int BOARD_SIZE = 15;
    private StackPane boardPane;
    private ImageView hoverPiece;
    private ImageView backgroundView;
    private final int[] imagePosition = new int[2];
    private VBox infoPanel;
    private final ImageView currentPlayerIndicator = new ImageView();
    private static final Lock placeLock = new ReentrantLock();

    @Override
    public void render(Stage stage) {
        stage.setWidth(1024);
        stage.setHeight(720);
        stage.setResizable(false);
        stage.setTitle("Gomoku");

        Panel panel = new Panel("Gomoku");
        panel.getStyleClass().add("panel-primary");
        Label header = new Label("Gomoku");
        header.getStyleClass().addAll("h3");
        panel.setHeading(header);
        panel.setPadding(new Insets(0));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10, 20, 10, 20));

        panel.setBody(root);

        // 创建棋盘
        StackPane board = createBoard();
        root.setLeft(board);

        // 创建信息面板
        VBox infoPanel = createInfoPanel();
        root.setRight(infoPanel);

        Scene scene = new Scene(panel, 1024, 720);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        stage.setScene(scene);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        stage.setScene(scene);
        stage.show();
        logger.info("Board created, size: " + boardPane.getWidth() + "x" + boardPane.getHeight());
        // 打印左上角坐标
        logger.info("Board position: " + boardPane.getLayoutX() + ", " + boardPane.getLayoutY());
        logger.info("Image position: " + backgroundView.getLayoutX() + ", " + backgroundView.getLayoutY());
        imagePosition[0] = (int) backgroundView.getLayoutX();
        imagePosition[1] = (int) backgroundView.getLayoutY() + MARGIN_TOP;
        initializeInfoPanel();
        showNewGameDialog();
    }

    private void initializeInfoPanel() {
        // Assuming player 1 starts and has a specific piece image
        Image player1Image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/black.png")));
        currentPlayerIndicator.setImage(player1Image);
        currentPlayerIndicator.setFitWidth(50); // Set appropriate size
        currentPlayerIndicator.setFitHeight(50);

        // Add the currentPlayerIndicator to your infoPanel at the desired position
        if (!infoPanel.getChildren().contains(currentPlayerIndicator)) {
            infoPanel.getChildren().add(1, currentPlayerIndicator); // Example position
        }
    }

    private int[] getRowCol(double x, double y) {
        x -= imagePosition[0];
        y -= imagePosition[1];
        if (x < PADDING || y < PADDING || x >= 450 - PADDING || y >= 450 - PADDING) {
            return new int[]{-1, -1};
        }
        double gridSize = (450.0 - 2 * PADDING) / BOARD_SIZE;
        int col = (int) Math.floor(x / gridSize);
        int row = (int) Math.floor(y / gridSize);
        return new int[]{row, col};
    }

    private StackPane createBoard() {
        boardPane = new StackPane();
        boardPane.setPrefSize(450, 450); // 设置棋盘大小

        // 加载完整棋盘背景图片
        Image backgroundImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/board.jpg")));
        backgroundView = new ImageView(backgroundImage);
        backgroundView.setFitWidth(450); // 设置图片大小以适应棋盘
        backgroundView.setFitHeight(450);

        // 初始化hoverPiece
        hoverPiece = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/hover.png"))));
        hoverPiece.setFitWidth(450.0 / BOARD_SIZE);
        hoverPiece.setFitHeight(450.0 / BOARD_SIZE);
        hoverPiece.setVisible(false); // 一开始设为不可见

        boardPane.getChildren().addAll(backgroundView, hoverPiece); // 将背景图片和预览棋子添加到棋盘


        // 绑定鼠标事件到棋盘
        boardPane.setOnMouseMoved(event -> {
            int[] rowCol = getRowCol(event.getX(), event.getY());
            updateHoverPiece(rowCol[0], rowCol[1]);
        });

        boardPane.setOnMouseClicked(event -> {
            if (hoverPiece.isVisible()) {
                int[] rowCol = getRowCol(event.getX(), event.getY());
                logger.info("Player" + PlayBoard.getCurrentPlayer() + " Clicked on " + rowCol[0] + ", " + rowCol[1]);
                placePiece(rowCol[0], rowCol[1]); // 在对应的位置放置棋子
                hoverPiece.setVisible(false); // 放置棋子后隐藏预览棋子
            }
        });

        boardPane.setOnMouseExited(event -> hoverPiece.setVisible(false)); // 当鼠标离开棋盘时隐藏预览棋子

        return boardPane;
    }

    private void updateHoverPiece(int row, int col) {
        if (row < 0 || col < 0) {
            hoverPiece.setVisible(false);
            return;
        }
        double gridSize = (450.0 - 2 * PADDING) / BOARD_SIZE; // 计算每个格子的尺寸
        double pieceX = col * gridSize + gridSize / 2 + PADDING;
        double pieceY = row * gridSize + gridSize / 2 + PADDING;

        hoverPiece.setTranslateX(pieceX - 225); // 225是棋盘一半的尺寸
        hoverPiece.setTranslateY(pieceY - 225);
        hoverPiece.setVisible(true);
    }

    private void placePiece(int row, int col) {
        placeLock.lock();
        try{
            if (!PlayBoard.validate(row, col)) {
                logger.warn("Invalid move");
                return;
            }
            if (PlayBoard.isEnded()) {
                logger.warn("Game has ended");
                showAlert("Game Over", "Game has ended");
                return;
            }

            renderPiece(row, col, PlayBoard.getCurrentPlayer());

            new Thread(() -> {
                PlayBoard.place(row, col);
                Platform.runLater(() -> {
                    refreshBoard();
                    refreshInfo();

                    int win = PlayBoard.checkWin();
                    if (win != 0 && win != -1) {
                        logger.info("Player " + win + " wins!");
                        showAlert("Game Over", "Player " + win + " wins!");
                    } else if (win == -1) {
                        logger.info("Draw!");
                        showAlert("Game Over", "Draw!");
                    }
                });
            }).start();
        } finally {
            placeLock.unlock();
        }
    }

    private VBox createInfoPanel() {
        infoPanel = new VBox(20); // 增加间距以适应更大的按钮
        infoPanel.setPadding(new Insets(20)); // 增加边距
        infoPanel.setAlignment(Pos.TOP_CENTER);

        Label label = new Label("Game Information");
        label.getStyleClass().addAll("h4");

        // 创建按钮
        Button newGameButton = new Button("New Game");
        Button exitButton = new Button("Exit");
        Button undoButton = new Button("Undo");
        Button redoButton = new Button("Redo");
        Button saveButton = new Button("Save Progress");
        Button loadButton = new Button("Load Save");

        // 设置按钮大小
        Button[] buttons = {newGameButton, exitButton, undoButton, redoButton, saveButton, loadButton};
        for (Button btn : buttons) {
            btn.setMinWidth(150);
            btn.setMinHeight(50);
        }

        // 添加点击事件
        newGameButton.setOnAction(e -> startNewGame());
        exitButton.setOnAction(e -> exitGame());
        undoButton.setOnAction(e -> undoAction());
        redoButton.setOnAction(e -> redoAction());
        saveButton.setOnAction(e -> saveGame());
        loadButton.setOnAction(e -> loadGame());

        infoPanel.getChildren().addAll(label, newGameButton, undoButton, redoButton, saveButton, loadButton, exitButton);
        return infoPanel;
    }

    private void refreshInfo() {
        int currentPlayer = PlayBoard.getCurrentPlayer();
        String imagePath;
        if (currentPlayer == 1) {
            imagePath = "/black.png"; // Path to Player 1's piece image
        } else {
            imagePath = "/white.png"; // Path to Player 2's piece image
        }

        Image currentPlayerImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
        currentPlayerIndicator.setImage(currentPlayerImage);
    }

    private void undoAction() {
        if (!PlayBoard.canUndo()) {
            showAlert("Undo Failed", "No moves to undo");
            return;
        }
        PlayBoard.undo();
        refreshBoard();
        refreshInfo();
    }

    private void redoAction() {
        if (!PlayBoard.canRedo()) {
            showAlert("Redo Failed", "No moves to redo");
            return;
        }
        PlayBoard.redo();
        refreshBoard();
        refreshInfo();
    }


    private void refreshBoard() {
        // Remove all pieces from the board
        boardPane.getChildren().removeIf(node -> node instanceof ImageView && "piece".equals(node.getUserData()));

        // Re-draw all pieces based on the current state of the game board
        int[][] board = PlayBoard.getBoard();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != 0) {
                    renderPiece(i, j, board[i][j]);
                }
            }
        }
    }

    private void renderPiece(int row, int col, int player) {
        // Visual representation is always updated
        String pieceImageFile = (player == 1) ? "/black.png" : "/white.png";
        Image pieceImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(pieceImageFile)));
        ImageView pieceView = new ImageView(pieceImage);
        pieceView.setFitWidth(450.0 / BOARD_SIZE);
        pieceView.setFitHeight(450.0 / BOARD_SIZE);

        double pieceX = col * (450.0 / BOARD_SIZE) + (450.0 / BOARD_SIZE) / 2;
        double pieceY = row * (450.0 / BOARD_SIZE) + (450.0 / BOARD_SIZE) / 2;

        pieceView.setTranslateX(pieceX - (double) 450 / 2);
        pieceView.setTranslateY(pieceY - (double) 450 / 2);
        pieceView.setUserData("piece"); // Mark as a piece for identification

        // boardPane.getChildren().add(pieceView));
        boardPane.getChildren().add(pieceView);
    }

    private void saveGame() {
        PlayBoard.saveProgress();
        showAlert("Game Saved", "Game progress has been saved");
    }

    private void loadGame() {
        boolean res = PlayBoard.loadProgress();
        if (res) {
            showAlert("Game Loaded", "Game progress has been loaded");
            refreshBoard();
            refreshInfo();
        } else {
            showAlert("Load Failed", "No saved progress found");
        }
    }


    private void exitGame() {
        System.exit(0);
    }

    private void showNewGameDialog() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        // cannot be resized and closed. no close button
        dialog.setResizable(false);
        dialog.setOnCloseRequest(e -> startNewGameWithSettings("Player vs AI", 2));
        dialog.setTitle("New Game Settings");
        ButtonType startGameButtonType = new ButtonType("Start Game", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(startGameButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> gameMode = new ComboBox<>();
        gameMode.getItems().addAll("Player vs AI", "Two Player");
        gameMode.setValue("Player vs AI");

        ComboBox<String> playerSide = new ComboBox<>();
        playerSide.getItems().addAll("Black", "White");
        playerSide.setValue("Black");

        // Disable "Side" choice for "Two Player" mode
        gameMode.valueProperty().addListener((obs, oldVal, newVal) -> playerSide.setDisable("Two Player".equals(newVal)));

        grid.add(new Label("Game Mode:"), 0, 0);
        grid.add(gameMode, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == startGameButtonType) {
                return new Pair<>(gameMode.getValue(), playerSide.isDisabled() ? "N/A" : playerSide.getValue());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(gameSettings -> {
            System.out.println("Game Mode= " + gameSettings.getKey() + ", Side= " + gameSettings.getValue());
            startNewGameWithSettings(gameSettings.getKey(), ("Black".equals(gameSettings.getValue()) ? 1 : 2));
        });
        result.ifPresentOrElse(gameSettings -> {
            System.out.println("Game Mode= " + gameSettings.getKey() + ", Side= " + gameSettings.getValue());
            startNewGameWithSettings(gameSettings.getKey(), ("Black".equals(gameSettings.getValue()) ? 1 : 2));
        }, () -> startNewGameWithSettings("Player vs AI", 2));
    }


    private void startNewGameWithSettings(String gameMode, int playerSide) {
        // Reset the board and any necessary game state.
        PlayBoard.reset();

        // Example: Adjust this to actually change the game mode and player side.
        logger.info("Starting new game: " + gameMode + ", Player chooses: " + playerSide);
        if ("Two Player".equals(gameMode)) {
            PlayBoard.init();
        } else if ("Player vs AI".equals(gameMode)) {
            PlayBoard.init(true);
        }
        refreshBoard();
        refreshInfo();
    }

    private void startNewGame() {
        showNewGameDialog();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        alert.showAndWait();
    }
}
