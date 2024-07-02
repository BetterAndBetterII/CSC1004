package org.zyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

public class AIRequest {
    private static final String baseUrl = "https://www.bytedance.ai11/next_step?";
    private static final Logger logger = LogManager.getLogger(AIRequest.class);
    private static final String gameId = generateGUID();
    private static final int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
    private static boolean networkError = false;

    public static int[] solve(Stack<int[]> boardStack, int color) {
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("stepsString", parseBoard(boardStack));
        paramsMap.put("color", color == 1 ? "BLACK" : "WHITE");
        paramsMap.put("level", "HIGH");
        paramsMap.put("gameId", gameId);
        String params = buildParamsString(paramsMap);
        String urlString = baseUrl + params;
        logger.info("Request: " + urlString);
        if (networkError) {
            try{
                return local_solve(boardStack, color);
            } catch (Exception e2) {
                logger.error("Error in local solve: " + e2.getMessage());
                return null;
            }
        }

        try {
            String response = get(urlString);
            logger.info("Response: " + response);
            JSONObject json = new JSONObject(response);
            return new int[]{json.getInt("x"), json.getInt("y")};
        } catch (Exception e) {
            logger.error("Error in making request: " + e.getMessage());
            networkError = true;
            try{
                return local_solve(boardStack, color);
            } catch (Exception e2) {
                logger.error("Error in local solve: " + e2.getMessage());
            }
        }
        return null;
    }

    public static int[] local_solve(Stack<int[]> boardStack, int color) {
        int[][] board = new int[PlayBoard.ROW][PlayBoard.ROW];
        for (int[] move : boardStack) {
            board[move[0]][move[1]] = move[2];
        }

        int bestScore = -1;
        int[] bestMove = null;

        for (int i = 0; i < PlayBoard.ROW; i++) {
            for (int j = 0; j < PlayBoard.ROW; j++) {
                if (board[i][j] == 0) {
                    int score = evaluatePosition(board, i, j, color);
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = new int[]{j, i};
                    }
                }
            }
        }

        return bestMove;
    }

    private static int evaluatePosition(int[][] board, int x, int y, int color) {
        int score = 0;

         score += 14 - Math.abs(x - PlayBoard.ROW / 2) - Math.abs(y - PlayBoard.ROW / 2);

        for (int[] direction : directions) {
            score += evaluateDirection(board, x, y, color, direction);
        }

        return score;
    }

    private static int evaluateDirection(int[][] board, int x, int y, int color, int[] direction) {
        int opponentColor = 3 - color;
        int op_count = 0;
        int self_count = 0;
        for (int i = 1; i <= 4; i++) {
            int nx = x + direction[0] * i;
            int ny = y + direction[1] * i;
            if (nx >= 0 && nx < PlayBoard.ROW && ny >= 0 && ny < PlayBoard.ROW) {
                if (board[nx][ny] == opponentColor)
                    op_count++;
                else if (board[nx][ny] == color)
                    op_count--;
            } else {
                break;
            }
        }
        for (int i = 1; i <= 4; i++) {
            int nx = x - direction[0] * i;
            int ny = y - direction[1] * i;
            if (nx >= 0 && nx < PlayBoard.ROW && ny >= 0 && ny < PlayBoard.ROW) {
                if (board[nx][ny] == opponentColor)
                    op_count++;
                else if (board[nx][ny] == color)
                    op_count--;
            } else {
                break;
            }
        }
        for (int i = 1; i <= 4; i++) {
            int nx = x + direction[0] * i;
            int ny = y + direction[1] * i;
            if (nx >= 0 && nx < PlayBoard.ROW && ny >= 0 && ny < PlayBoard.ROW) {
                if (board[nx][ny] == color)
                    self_count++;
                else if (board[nx][ny] == opponentColor)
                    self_count--;
            } else {
                break;
            }
        }
        for (int i = 1; i <= 4; i++) {
            int nx = x - direction[0] * i;
            int ny = y - direction[1] * i;
            if (nx >= 0 && nx < PlayBoard.ROW && ny >= 0 && ny < PlayBoard.ROW) {
                if (board[nx][ny] == color)
                    self_count++;
                else if (board[nx][ny] == opponentColor)
                    self_count--;
            } else {
                break;
            }
        }
        // 4 in row, highest score
        if (self_count >=4 || op_count >= 4){
            return 100000;
        }
        if (op_count >= 3){
            return op_count * op_count * 50;
        }
        if (self_count + op_count >= 2) {
            return (self_count + op_count) * 50;
        }

        return 0;
    }

    public static void main(String[] args) {
        String gameId = generateGUID();
        System.out.println("gameId: " + gameId);

        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("stepsString", "_h8_a1_i8_a2_j8_a3");
        paramsMap.put("color", "BLACK");
        paramsMap.put("level", "HIGH");
        paramsMap.put("gameId", gameId);
        // Build the params string from the map
        String params = buildParamsString(paramsMap);
        String urlString = baseUrl + params;
        System.out.println(urlString);

        try {
            String response = get(urlString);
            JSONObject json = new JSONObject(response);
            System.out.println(json.get("x"));
            System.out.println(json.get("y"));
        } catch (Exception e) {
            logger.error("Error in making request: " + e.getMessage());
        }
    }

    private static String parseBoard(Stack<int[]> boardStack) {
        StringBuilder sb = new StringBuilder();
        Stack<int[]> tempStack = new Stack<>();
        Stack<int[]> cloneStack = new Stack<>();
        while (!boardStack.isEmpty()) {
            int[] value = boardStack.pop();
            tempStack.push(value);
            cloneStack.push(value);
        }
        while (!cloneStack.isEmpty()) {
            boardStack.push(cloneStack.pop());
        }
        while (!tempStack.isEmpty()) {
            int[] step = tempStack.pop();
            sb.append('_')
                    .append((char) ('a' + step[1]))
                    .append(step[0] + 1);
        }
        return sb.toString();
    }

    private static String buildParamsString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(entry.getKey())
                    .append('=')
                    .append(entry.getValue());
        }
        return sb.toString();
    }

    // Method to perform a GET request
    public static String get(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Origin", "https://www.bytedance.ai");
        connection.setRequestProperty("Referer", "https://www.bytedance.ai/");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Priority", "u=1, i");

        connection.setConnectTimeout(1000000);
        connection.setReadTimeout(1000000);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        connection.disconnect();
        return content.toString();
    }

    // Method to generate a GUID
    public static String generateGUID() {
        return "b905206d-78f4-4337-8b99-8351c3a4deb7";
//        return UUID.randomUUID().toString();
    }
}
