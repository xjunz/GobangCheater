package xjunz.tool.gobangcheater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;

import ego.gomoku.entity.Point;
import ego.gomoku.enumeration.Color;
import wine.Wine;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    static {
        System.loadLibrary("native-lib");
    }

    private static final String[] EXTERNAL_STORAGE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE_PERMISSION = 0;
    private static final int REQUEST_CODE_OVERLAY = 1;
    private static final int REQUEST_CODE_MEDIA_PROJECTION = 2;
    private int mScreenH;
    private int mScreenW;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private int mScreenDensity;
    private WindowManager mWindowManager;
    private ChessboardView mChessboard;
    private SeekBar mScaleController;
    private ViewGroup mControllerPanel, mCheaterPanel;
    private ToggleButton mToggleController, mToggleCheater;
    private View mChequer;
    private WindowManager.LayoutParams mChequerLp;
    private TextView mAnchor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this, EXTERNAL_STORAGE_PERMISSIONS[0]) == PERMISSION_DENIED
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))) {
            ActivityCompat.requestPermissions(this, EXTERNAL_STORAGE_PERMISSIONS, REQUEST_CODE_PERMISSION);
        } else {
            initMetrics();
            mScreenDensity = (int) getResources().getDisplayMetrics().density;
            mImageReader = ImageReader.newInstance(mScreenW, mScreenH, PixelFormat.RGBA_8888, 2);
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
    }

    private void initMetrics() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetric = new DisplayMetrics();
        display.getRealMetrics(outMetric);
        mScreenW = outMetric.widthPixels;
        mScreenH = outMetric.heightPixels;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_about) {
            new AlertDialog.Builder(this)
                    .setTitle("关于")
                    .setMessage("五子棋辅助程序，仅供学习交流使用。五子棋AI引擎：https://github.com/jinjiebang/AIWine")
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ShowToast")
    private void toast(Object content) {
        Toast toast;
        if (content == null) {
            content = Html.fromHtml("<i>null</i>");
        }
        if (content instanceof Integer) {
            toast = Toast.makeText(this, (Integer) content, Toast.LENGTH_SHORT);
        } else if (content instanceof CharSequence) {
            toast = Toast.makeText(this, (CharSequence) content, Toast.LENGTH_SHORT);
        } else {
            toast = Toast.makeText(this, content.toString(), Toast.LENGTH_SHORT);
        }
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);
        toast.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    toast("悬浮窗权限授权成功！");
                } else {
                    toast("请授予悬浮窗权限，否则应用无法工作");
                    finish();
                }
            }
        } else if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                mImageReader = ImageReader.newInstance(mScreenW, mScreenH, PixelFormat.RGBA_8888, 2);
                if (data != null) {
                    mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
                    mVirtualDisplay = mMediaProjection.createVirtualDisplay("gobang_projection", mScreenW, mScreenH,
                            mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
                }
            } else {
                toast("请允许屏幕捕获，否则应用无法工作");
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_CODE_OVERLAY);
                    }
                }
            } else {
                toast("请授权，否则应用无法工作");
                finish();
            }
        }
    }

    private Bitmap shot(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        image.close();
        return bitmap;
    }

    private ToggleButton mToggleCapture;

    public void toggleCaptureScreen(View view) {
        mToggleCapture = (ToggleButton) view;
        if (!mToggleCapture.isChecked()) {
            mMediaProjection.stop();
            mImageReader.close();
            mVirtualDisplay.release();
            return;
        }
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = new Intent(mMediaProjectionManager.createScreenCaptureIntent());
        startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION);
    }


    @SuppressLint("InflateParams")
    public void toggleOverlay(View view) {
        mToggleController = (ToggleButton) view;
        if (!mToggleController.isChecked()) {
            mWindowManager.removeView(mChessboard);
            mWindowManager.removeView(mControllerPanel);
            return;
        }
        WindowManager.LayoutParams lpChessboard = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lpChessboard.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            lpChessboard.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        lpChessboard.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        lpChessboard.format = PixelFormat.TRANSLUCENT;
        lpChessboard.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lpChessboard.height = ViewGroup.LayoutParams.MATCH_PARENT;
        lpChessboard.gravity = Gravity.TOP | Gravity.START;

        WindowManager.LayoutParams lpController = new WindowManager.LayoutParams();
        lpController.copyFrom(lpChessboard);
        lpController.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        lpController.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lpController.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;


        mChessboard = (ChessboardView) getLayoutInflater().inflate(R.layout.overlay_chessboard, null);
        if (GobangCheater.hasChessboardConfig()) {
            mChessboard.setGridSpec(GobangCheater.getChessboardGridSpec());
            mChessboard.setOffset(GobangCheater.getChessboardOffsetX(), GobangCheater.getChessboardOffsetY());
        }
        mControllerPanel = (ViewGroup) getLayoutInflater().inflate(R.layout.overlay_controller, null);
        mScaleController = mControllerPanel.findViewById(R.id.sb_controller);
        if (GobangCheater.getSeekBarProgress() != -1) {
            mScaleController.setProgress(GobangCheater.getSeekBarProgress());
        }
        mScaleController.setOnSeekBarChangeListener(this);

        mWindowManager.addView(mChessboard, lpChessboard);
        mWindowManager.addView(mControllerPanel, lpController);
    }


    private float downX, downY;

    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    public void toggleCheater(View view) {
        mToggleCheater = (ToggleButton) view;
        if (!mToggleCheater.isChecked()) {
            if (mToggleCheater.getParent() != null) {
                mWindowManager.removeView(mCheaterPanel);
            }
            return;
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            lp.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP;
        lp.x = 100;
        lp.y = (int) (.8f * mScreenH);
        mCheaterPanel = (ViewGroup) getLayoutInflater().inflate(R.layout.overlay_cheater, null);
        mAnchor = mCheaterPanel.findViewById(R.id.anchor);
        mAnchor.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float nowX = event.getRawX();
                    float nowY = event.getRawY();
                    lp.x += nowX - downX;
                    lp.y += nowY - downY;
                    downX = nowX;
                    downY = nowY;
                    mWindowManager.updateViewLayout(mCheaterPanel, lp);
                case MotionEvent.ACTION_UP:
                    break;
            }
            return false;
        });

        mWindowManager.addView(mCheaterPanel, lp);
        mChequer = getLayoutInflater().inflate(R.layout.overlay_chequer, null);
        mChequerLp = new WindowManager.LayoutParams();
        mChequerLp.copyFrom(lp);
        mChequerLp.width = 60;
        mChequerLp.height = 60;
        mChequerLp.verticalMargin = 0;
        mChequerLp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mChequerLp.gravity = Gravity.TOP | Gravity.START;
    }

    public void plus(View view) {
        mScaleController.setProgress(mScaleController.getProgress() + 1);
    }

    public void minus(View view) {
        mScaleController.setProgress(mScaleController.getProgress() - 1);
    }

    public void leftward(View view) {
        mChessboard.shift(mChessboard.getOffsetX() - 1, mChessboard.getOffsetY());
    }

    public void rightward(View view) {
        mChessboard.shift(mChessboard.getOffsetX() + 1, mChessboard.getOffsetY());
    }

    public void downward(View view) {
        mChessboard.shift(mChessboard.getOffsetX(), mChessboard.getOffsetY() + 1);
    }

    public void upward(View view) {
        mChessboard.shift(mChessboard.getOffsetX(), mChessboard.getOffsetY() - 1);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mChessboard.scale(0.5f + 1f * mScaleController.getProgress() / mScaleController.getMax());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void saveChessboardConfig(View view) {
        GobangCheater.putChessboardOffsetX(mChessboard.getOffsetX());
        GobangCheater.putChessboardOffsetY(mChessboard.getOffsetY());
        GobangCheater.putChessboardGridSpec(mChessboard.getGridSpec());
        GobangCheater.notifyChessboardConfigSaved();
        GobangCheater.putSeekBarProgress(mScaleController.getProgress());
        PointF start = mChessboard.getStartPoint();
        GobangCheater.putChessboardLeft(start.x);
        GobangCheater.putChessboardTop(start.y);
        toast("棋盘数据保存成功");
    }


    public void closeOverlay(View view) {
        mWindowManager.removeView(mChessboard);
        mWindowManager.removeView(mControllerPanel);
        mToggleController.setChecked(false);
    }

    private void showPromptChequer(int row, int col) {

        mChequerLp.x = (int) (GobangCheater.getChessboardLeft() + col * GobangCheater.getChessboardGridSpec() - mChequerLp.width / 2);
        mChequerLp.y = (int) (GobangCheater.getChessboardTop() + row * GobangCheater.getChessboardGridSpec() - mChequerLp.height / 2);
        mWindowManager.addView(mChequer, mChequerLp);
    }


    public void prompt(View view) {
        if (mChequer.getParent() != null) {
            mWindowManager.removeViewImmediate(mChequer);
        }

        Button button = (Button) view;
        button.setEnabled(false);
        button.setText("正在思考...");
        new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
           /* Color[][] board = BoardReader.abstractChessboardEGO(shot(mImageReader.acquireLatestImage())
                    , GobangCheater.getChessboardLeft(), GobangCheater.getChessboardTop(), GobangCheater.getChessboardGridSpec());
            GomokuPlayer player = new GomokuPlayer(board, Level.HIGH);
            Result result = player.playGomokuCup(BoardReader.currentPlayerColor, 15 * 1000);*/
            Wine.init();
            Color[][] board = BoardReader.abstractChessboardWine(shot(mImageReader.acquireLatestImage())
                    , GobangCheater.getChessboardLeft(), GobangCheater.getChessboardTop(), GobangCheater.getChessboardGridSpec());
            BoardReader.printBoard(board);
            int[] result = Wine.best();
            runOnUiThread(() -> {
                button.setEnabled(true);
                button.setText("提示");
                Point p = new Point(result[0], result[1]);
                toast(p.getX() + "," + p.getY());
                showPromptChequer(p.getX(), p.getY());
            });
        }).start();

    }

    public void closeCheater(View view) {
        mWindowManager.removeView(mCheaterPanel);
        if (mChequer.getParent() != null) {
            mWindowManager.removeViewImmediate(mChequer);
        }
        mToggleCheater.setChecked(false);
        mMediaProjection.stop();
        mImageReader.close();
        mVirtualDisplay.release();
        mToggleCapture.setChecked(false);
    }

    public void customOp(View view) {
        // BoardReader.move(shot(mImageReader.acquireLatestImage())
        //          , GobangCheater.getChessboardLeft(), GobangCheater.getChessboardTop(), GobangCheater.getChessboardGridSpec());
    }
}
