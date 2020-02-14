package xjunz.tool.gobangcheater;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChessboardView extends View {

    private Paint mGridPaint, mWhiteChessPaint, mBlackChessPaint;
    private int mScreenWidth, mScreenHeight;
    private static final float DEFAULT_GRID_SPEC = 50f;
    private float mGridSpec;


    private int mOffsetX;
    private int mOffsetY;

    public ChessboardView(@NonNull Context context) {
        super(context);
        initMetrics(context);
        initPaints();
    }

    public ChessboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initMetrics(context);
        initPaints();
    }

    private void initMetrics(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics outMetric = new DisplayMetrics();
        display.getRealMetrics(outMetric);
        mScreenWidth = outMetric.widthPixels;
        mScreenHeight = outMetric.heightPixels;
        mGridSpec = DEFAULT_GRID_SPEC;
    }

    private void initPaints() {
        mGridPaint = new Paint();
        mGridPaint.setColor(0xffff0000);
        mGridPaint.setStyle(Paint.Style.FILL);
        mGridPaint.setAntiAlias(true);
        // mGridPaint.setStrokeWidth(5);

        mWhiteChessPaint = new Paint();
        mWhiteChessPaint.setColor(0xffffffff);
        mWhiteChessPaint.setStyle(Paint.Style.FILL);
        mWhiteChessPaint.setAntiAlias(true);

        mBlackChessPaint = new Paint();
        mBlackChessPaint.set(mWhiteChessPaint);
        mBlackChessPaint.setColor(0xff000000);
    }

    private boolean mDrawChess = false;


    public int getOffsetX() {
        return mOffsetX;
    }

    public int getOffsetY() {
        return mOffsetY;
    }

    public void setOffset(int offsetX, int offsetY) {
        this.mOffsetX = offsetX;
        this.mOffsetY = offsetY;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float startX = mScreenWidth / 2f - mGridSpec * 7;
        float endX = startX + 14 * mGridSpec;
        float startY = mScreenHeight / 2f - mGridSpec * 7;
        float endY = startY + 14 * mGridSpec;
        for (int i = 0; i < 15; i++) {
            float curY = startY + i * mGridSpec;
            float curX = startX + i * mGridSpec;
            canvas.drawLine(startX + mOffsetX, curY + mOffsetY, endX + mOffsetX, curY + mOffsetY, mGridPaint);
            canvas.drawLine(curX + mOffsetX, startY + mOffsetY, curX + mOffsetX, endY + mOffsetY, mGridPaint);
            if (mDrawChess) {
                canvas.drawCircle(curX, curY, 30, mWhiteChessPaint);
            }
        }
    }

    public void scale(@FloatRange(from = 0.5f, to = 2f) float scale) {
        mGridSpec = scale * DEFAULT_GRID_SPEC;
        invalidate();
    }

    public void shift(int x, int y) {
        mOffsetX = x;
        mOffsetY = y;
        invalidate();
    }

    public void setGridSpec(float spec) {
        this.mGridSpec = spec;
        invalidate();
    }

    public float getGridSpec() {
        return mGridSpec;
    }

    public PointF getStartPoint() {
        PointF point = new PointF();
        float startX = mScreenWidth / 2f - mGridSpec * 7;
        float startY = mScreenHeight / 2f - mGridSpec * 7;
        point.x = startX + mOffsetX;
        point.y = startY + mOffsetY;
        return point;
    }

}
