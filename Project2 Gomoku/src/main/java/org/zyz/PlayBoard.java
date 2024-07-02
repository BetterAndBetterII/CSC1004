package org.zyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;


public class PlayBoard {
    private static final Logger logger = LogManager.getLogger(PlayBoard.class);
    public static final int ROW = 15;
    private static final int[][] board = new int[ROW][ROW];
    private static final int WIN_CONDITION = 5;
    private static final Stack<int[]> undoStack = new Stack<>();
    private static final Stack<int[]> redoStack = new Stack<>();
    private static int currentPlayer = 1;
    private static boolean isEnded = false;
    private static boolean isAI = false;
    private static final Lock placeLock = new ReentrantLock();

    public static void init() {
        reset();
        PlayBoard.isAI = false;
    }

    public static void init(boolean isAI) {
        reset();
        if (isAI) {
            logger.info("AI mode enabled");
        }
        AIPlace(7, 7);
        PlayBoard.isAI = isAI;
    }

    public static boolean isAI() {
        return isAI;
    }

    public static boolean isEnded() {
        return isEnded;
    }

    public static int getCurrentPlayer() {
        return currentPlayer;
    }

    public static boolean AIPlace() {
        logger.info("AI start thinking...");
        Stack<int[]> clone = (Stack<int[]>) undoStack.clone();
        int[] aiMove = AIRequest.solve(clone, currentPlayer);
        if (aiMove != null) {
            board[aiMove[1]][aiMove[0]] = currentPlayer;
            logger.info("AI move: " + aiMove[0] + " " + aiMove[1]);
        } else {
            System.out.println("AI move failed");
            return false;
        }
        undoStack.push(new int[]{aiMove[1], aiMove[0], currentPlayer}); // Record move for undo
        switchPlayer();
        return true;
    }

    public static boolean AIPlace(int x, int y) {
        board[y][x] = currentPlayer;
        undoStack.push(new int[]{y, x, currentPlayer}); // Record move for undo
        switchPlayer();
        logger.info("AI move: " + x + " " + y);
        return true;
    }

    public static boolean place(int x, int y) {
        placeLock.lock(); // 尝试获取锁
        try {
            if (validate(x, y)) {
                board[x][y] = currentPlayer;
                undoStack.push(new int[]{x, y, currentPlayer}); // Record move for undo
                redoStack.clear(); // Clear redo stack on new move
                switchPlayer();
                if (PlayBoard.isAI()) {
                    return AIPlace();
                }
                return true;
            }
            System.out.println("Invalid move");
            return false;
        } finally {
            placeLock.unlock(); // 释放锁
        }
    }

    public static int[][] getBoard() {
        return board;
    }

    public static boolean validate(int x, int y) {
        return board[x][y] == 0;
    }

    public static int checkWin() {
        // 检查行、列
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < ROW - WIN_CONDITION + 1; j++) {
                if (board[i][j] != 0 && checkLine(i, j, 0, 1)) {
                    isEnded = true;
                    return board[i][j];
                }
                if (board[j][i] != 0 && checkLine(j, i, 1, 0)) {
                    isEnded = true;
                    return board[j][i];
                }
            }
        }

        // 检查对角线（左上到右下）
        for (int i = 0; i < ROW - WIN_CONDITION + 1; i++) {
            for (int j = 0; j < ROW - WIN_CONDITION + 1; j++) {
                if (board[i][j] != 0 && checkLine(i, j, 1, 1)) {
                    isEnded = true;
                    return board[i][j];
                }
            }
        }

        // 检查对角线（左下到右上）
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < ROW - WIN_CONDITION + 1; j++) {
                if (board[i][j] != 0 && checkLine(i, j, -1, 1)) {
                    isEnded = true;
                    return board[i][j];
                }
            }
        }

        if (undoStack.size() == ROW * ROW) {
            isEnded = true;
            return -1; // 平局
        }

        return 0; // 无获胜方
    }

    // 新增一个方法来检查从某个点出发的连续相同颜色的棋子数量
    private static boolean checkLine(int startX, int startY, int dx, int dy) {
        int color = board[startX][startY];
        int count = 1; // 已经有1个棋子了
        int x = startX + dx;
        int y = startY + dy;

        while (x >= 0 && x < ROW && y >= 0 && y < ROW && board[x][y] == color) {
            count++;
            if (count == WIN_CONDITION) return true; // 找到连续5个同色棋子
            x += dx;
            y += dy;
        }

        return false;
    }

    public static void reset() {
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < ROW; j++) {
                board[i][j] = 0;
            }
        }
        undoStack.clear();
        redoStack.clear();
        currentPlayer = 1;
        isEnded = false;
    }

    private static void switchPlayer() {
        currentPlayer = (currentPlayer % 2) + 1;
    }

    public static void undo() {
        if (!undoStack.isEmpty()) {
            if (isAI()) {
                int[] move = undoStack.pop();
                board[move[0]][move[1]] = 0;
                redoStack.push(move);
                if (!undoStack.isEmpty()) {
                    move = undoStack.pop();
                    board[move[0]][move[1]] = 0;
                    redoStack.push(move);
                }
                isEnded = false;
            } else {
                int[] move = undoStack.pop();
                board[move[0]][move[1]] = 0;
                redoStack.push(move);
                switchPlayer();
                isEnded = false;
            }
        }
    }

    public static void redo() {
        if (!redoStack.isEmpty()) {
            if (isAI()) {
                int[] move = redoStack.pop();
                board[move[0]][move[1]] = move[2];
                undoStack.push(move);
                if (!redoStack.isEmpty()) {
                    move = redoStack.pop();
                    board[move[0]][move[1]] = move[2];
                    undoStack.push(move);
                }
            } else {
                int[] move = redoStack.pop();
                board[move[0]][move[1]] = move[2];
                undoStack.push(move);
                switchPlayer();
            }
            if (checkWin() != 0) {
                isEnded = true;
            }
        }
    }

    public static void saveProgress() {
        JSONObject json = new JSONObject();
        json.put("board", board); // Directly putting the 2D array might not work as expected.
        json.put("currentPlayer", currentPlayer);
        json.put("isEnded", isEnded);
        json.put("isAI", isAI);

        // Convert stacks to JSON arrays of arrays
        json.put("undoStack", new JSONArray(undoStack.toArray()));
        json.put("redoStack", new JSONArray(redoStack.toArray()));

        try (FileWriter file = new FileWriter("progress.json")) {
            file.write(json.toString(2));
            file.flush();
        } catch (IOException e) {
            logger.error("failed to save progress to file: " + e.getMessage());
        }
    }


    public static boolean loadProgress() {
        if (new File("progress.json").exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get("progress.json")));
                JSONObject json = new JSONObject(content);

                // Load the board
                JSONArray boardArray = json.getJSONArray("board");
                for (int i = 0; i < boardArray.length(); i++) {
                    JSONArray row = boardArray.getJSONArray(i);
                    for (int j = 0; j < row.length(); j++) {
                        board[i][j] = row.getInt(j);
                    }
                }

                // Correctly handle loading of undoStack
                undoStack.clear();
                JSONArray undoArray = json.getJSONArray("undoStack");
                logger.info("undoArray: " + undoArray.toString());
                for (int i = 0; i < undoArray.length(); i++) {
                    // Expecting each element of undoArray to be an array itself
                    JSONArray step = undoArray.getJSONArray(i);
                    int[] move = new int[3]; // Assuming each move consists of 3 integers
                    for (int j = 0; j < step.length(); j++) {
                        move[j] = step.getInt(j);
                    }
                    undoStack.push(move);
                }

                // Similar handling for redoStack
                redoStack.clear();
                JSONArray redoArray = json.getJSONArray("redoStack");
                for (int i = 0; i < redoArray.length(); i++) {
                    JSONArray step = redoArray.getJSONArray(i);
                    int[] move = new int[3]; // Assuming each move consists of 3 integers
                    for (int j = 0; j < step.length(); j++) {
                        move[j] = step.getInt(j);
                    }
                    redoStack.push(move);
                }


                currentPlayer = json.getInt("currentPlayer");
                isEnded = json.getBoolean("isEnded");
                isAI = json.getBoolean("isAI");
                return true;
            } catch (IOException e) {
                logger.error("Failed to load progress from file: " + e.getMessage());
                return false;
            }
        } else {
            logger.error("Progress.json not found");
            return false;
        }
    }

    public static boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public static boolean canRedo() {
        return !redoStack.isEmpty();
    }

}
