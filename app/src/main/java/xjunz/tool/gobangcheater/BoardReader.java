package xjunz.tool.gobangcheater;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;

import ego.gomoku.core.Config;
import ego.gomoku.enumeration.Color;
import wine.Wine;


public class BoardReader {
    static Color currentPlayerColor = Color.BLACK;

    static Color[][] abstractChessboardEGO(Bitmap chessboardBitmap, float startX, float startY, float gridSpec) {
        int size = Config.size;
        int playerFlag = 0;
        Color[][] map = new Color[size][size];
        for (int column = 0; column < size; column++) {
            int x = (int) (startX + column * gridSpec);
            for (int row = 0; row < size; row++) {
                int y = (int) (startY + row * gridSpec);
                int color = chessboardBitmap.getPixel(x - 10, y - 10);
                double luminance = ColorUtils.calculateLuminance(color);
                if (luminance < 0.2) {
                    map[row][column] = Color.BLACK;
                    playerFlag++;
                } else if (luminance > 0.9) {
                    map[row][column] = Color.WHITE;
                    playerFlag--;
                } else {
                    map[row][column] = Color.NULL;
                }
            }
        }
        chessboardBitmap.recycle();
        currentPlayerColor = playerFlag == 0 ? Color.BLACK : Color.WHITE;
        return map;
    }

    static Color[][] abstractChessboardWine(Bitmap chessboardBitmap, float startX, float startY, float gridSpec) {
        int size = Config.size;
        Color[][] map = new Color[size][size];
        ArrayList<int[]> blacks = new ArrayList<>();
        ArrayList<int[]> whites = new ArrayList<>();
        for (int column = 0; column < size; column++) {
            int x = (int) (startX + column * gridSpec);
            for (int row = 0; row < size; row++) {
                int y = (int) (startY + row * gridSpec);
                int color = chessboardBitmap.getPixel(x - 10, y - 10);
                double luminance = ColorUtils.calculateLuminance(color);
                int[] point=new int[]{row, column};
                if (luminance < 0.2) {
                    map[row][column] = Color.BLACK;
                    blacks.add(point);
                } else if (luminance > 0.9) {
                    map[row][column] = Color.WHITE;
                    whites.add(point);
                } else {
                    map[row][column] = Color.NULL;
                }
            }
        }
        if (whites.size() > blacks.size()) {
            throw new RuntimeException("Whites should not be more than blacks");
        }
        boolean equal = whites.size() == blacks.size();
        for (int i = 0; i < whites.size(); i++) {
            int[] black = blacks.get(i);
            int[] white = whites.get(i);
            Wine.move(black[0], black[1]);
            Wine.move(white[0], white[1]);
        }
        if (!equal) {
            int[] black = blacks.get(blacks.size() - 1);
            Wine.move(black[0], black[1]);
        }
        chessboardBitmap.recycle();
        return map;
    }


    public static void printBoard(Color[][] board) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int row = 0; row < Config.size; row++) {
            for (int column = 0; column < Config.size; column++) {
                sb.append(board[row][column]);
                if (column == Config.size - 1) {
                    sb.append("\n");
                }
            }
        }
        Log.i("board", sb.toString());
    }
}
