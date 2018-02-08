package cn.faury.android.library.zxing.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Vector;

import cn.faury.android.library.dialog.material.FMaterialDialogUtils;
import cn.faury.android.library.zxing.R;
import cn.faury.android.library.zxing.bean.ErrorBean;
import cn.faury.android.library.zxing.camera.CameraManager;
import cn.faury.android.library.zxing.utils.ImageUtils;
import cn.faury.android.library.zxing.utils.RGBLuminanceSource;
import cn.faury.android.library.zxing.view.ViewfinderView;

/**
 * 扫描二维码界面
 */
public class CaptureActivity extends Activity implements Callback,
        Handler.Callback {

    /**
     * 扫码返回结果
     */
    public static final int RESULT_CODE_OK = 1;
    /**
     * 扫码返回结果保存key
     */
    public static final String RESULT_KEY = "result";

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 0;

    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 1;

    public String TAG = this.getClass().getName();

    private static Context mContext;

    private CaptureActivityHandler handler;

    private ViewfinderView viewfinderView;

    private boolean hasSurface;

    private Vector<BarcodeFormat> decodeFormats;

    private String characterSet;

    private InactivityTimer inactivityTimer;

    private Handler mHandler = new Handler(this);

    private String strResult;

    public static int cameraHeight;

    public static int sWidth;

    public static int sHeight;

    private Camera m_Camera;

    boolean isLight = false;

    private ImageView back;

    private static ImageView light;
    private static ImageView mIvAlbum;// 相册

    private boolean mHavaCamarePermission = false;

    private SurfaceHolder surfaceHolder;

    //识别图片路径
    private String mPicPath;

    //传入的handler
    private static Handler mCallbackHandler;

    //传入的message.what的值
    private static int mCallbackMeesageWhat;

    //是否支持闪光灯
    private static boolean mHaveLight = true;

    //是否可识别图片
    private static boolean mCanChoosePic = true;

    //扫描结果广播
    public static final String mDecodeAction = "f.broadcast.action.decode";

    //广播，扫码结果key
    public static final String BROADCAST_INTENT_EXTRE_RESULT_NAME = "code";

    //没有相机权限code
    public static final String SCAN_QR_RESULT_NO_CAMERA_PERMISSION = "0101";

    //其他错误code
    public static final String SCAN_QR_RESULT_OTHER_ERROR = "0199";

    //回调-是否成功标识，0：成功，obj为扫码内容；1：失败，obj为错误信息实体类
    public static int SCAN_QR_RESULT_SUCCESS = 0;

    private static final int FINISH_ACTIVITY = 10;

    private MaterialDialog mNoCameraPermissionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.f_library_zxing_activity_capture);
        mContext = this;
        initUI();
        initListener();
        Rect frame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        sWidth = display.getWidth();
        sHeight = display.getHeight();
        cameraHeight = display.getHeight() - statusBarHeight;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                //权限被拒绝
                if (mNoCameraPermissionDialog != null) {
                    mNoCameraPermissionDialog.show();
                }
            }
            //CAMERA权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);

            mHavaCamarePermission = false;
        } else {
            mHavaCamarePermission = true;
        }
        CameraManager.init(getApplication());
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        ViewfinderView.isFinish = false;
        isLight = false;
        SurfaceView surfaceView = findViewById(R.id.preview_view);
//                SurfaceView surfaceView = (SurfaceView) findViewById(ResourceUtil.getId(mContext, "preview_view"));
        surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;
    }

    /**
     * 初始化控件
     */
    protected void initUI() {
        mIvAlbum = findViewById(R.id.open_gallery);
        back = findViewById(R.id.back);
        light = findViewById(R.id.switch_light);
        viewfinderView = findViewById(R.id.viewfinder_view);

        if (mHaveLight) {
            light.setVisibility(View.VISIBLE);
        } else {
            light.setVisibility(View.GONE);
        }

        if (mCanChoosePic) {
            mIvAlbum.setVisibility(View.VISIBLE);
        } else {
            mIvAlbum.setVisibility(View.GONE);
        }

        mNoCameraPermissionDialog = FMaterialDialogUtils.confirmDialog(mContext,
                getResources().getString(R.string.f_library_zxing_camera_permission),
                new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        FMaterialDialogUtils.dismiss(dialog);
                    }
                }, new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        FMaterialDialogUtils.dismiss(dialog);
                        handler2.sendEmptyMessage(FINISH_ACTIVITY);
                    }
                });
    }

    /**
     * 初始化事件
     */
    protected void initListener() {
        mIvAlbum.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (ContextCompat.checkSelfPermission(CaptureActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(CaptureActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //SD卡权限
                    ActivityCompat.requestPermissions(CaptureActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                } else {
                    ImageUtils.openLocalImage(CaptureActivity.this);
                }
            }
        });
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                finish();
            }
        });
        light.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // 打开闪光灯
                PackageManager pm = getPackageManager();
                FeatureInfo[] features = pm.getSystemAvailableFeatures();
                boolean hasLight = false;
                for (FeatureInfo f : features) {
                    if (PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) // 判断设备是否支持闪光灯
                    {
                        hasLight = true;
                    }
                }
                if (hasLight) {
                    if (null == m_Camera) {
                        m_Camera = CameraManager.camera;
                    }
                    if (!isLight) {
                        try {
                            Camera.Parameters parameters = m_Camera.getParameters();
                            parameters
                                    .setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            m_Camera.setParameters(parameters);
                            // m_Camera.startPreview();
                            isLight = true;
                            light.setImageResource(R.drawable.f_library_zxing_capture_light_close);
//                            light.setImageDrawable(getResources().getDrawable(
//                                    ResourceUtil.getDrawableId(mContext, "f_library_zxing_capture_light_close")));
                        } catch (Exception e) {
                            CaptureActivity.this.finish();
                        }
                    } else {
                        Camera.Parameters mParameters;
                        mParameters = m_Camera.getParameters();
                        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        m_Camera.setParameters(mParameters);
                        isLight = false;
                        light.setImageResource(R.drawable.f_library_zxing_capture_light_open);
//                        light.setImageResource(ResourceUtil.getDrawableId(mContext, "f_library_zxing_capture_light_open"));
                    }
                } else {
                    Toast.makeText(mContext, R.string.f_library_zxing_camera_no_lamp, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 传入handler，用于结果回调；
     * 如果不传，可用RESULT_CODE_OK和RESULT_KEY，使用onActivityResult进行接收；
     * 也可以通过注册广播mDecodeAction，通过BROADCAST_INTENT_EXTRE_RESULT_NAME从intent中取值来处理。
     *
     * @param callbackHandler     回调handler
     * @param callbackMessageWhat message.what的值
     */
    public static void setCallbackHandler(Handler callbackHandler, int callbackMessageWhat) {
        mCallbackHandler = callbackHandler;
        mCallbackMeesageWhat = callbackMessageWhat;
    }

    /**
     * 设置是否需要闪光灯功能,默认为true
     *
     * @param haveLight 是否需要闪光灯功能
     */
    public static void setHaveLight(boolean haveLight) {
        mHaveLight = haveLight;
        if (light != null) {
            if (mHaveLight) {
                light.setVisibility(View.VISIBLE);
            } else {
                light.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 设置是否需要图片识别功能，默认为投入为ture
     *
     * @param canChoosePic 是否需要图片识别功能
     */
    public static void setCanChoosePic(boolean canChoosePic) {
        mCanChoosePic = canChoosePic;
        if (mIvAlbum != null) {
            if (mCanChoosePic) {
                mIvAlbum.setVisibility(View.VISIBLE);
            } else {
                mIvAlbum.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        if (mHavaCamarePermission) {
            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }
            CameraManager.get().closeDriver();
        }

    }

    @Override
    protected void onDestroy() {

        inactivityTimer.shutdown();
        super.onDestroy();
    }

    /**
     * Handler scan result
     *
     * @param result
     * @param barcode
     */
    public void handleDecode(Result result, Bitmap barcode) {

        inactivityTimer.onActivity();
        strResult = result.getText();
        ViewfinderView.isFinish = true;
        Log.d(TAG, "Handler scan result:" + strResult);
        if (strResult == null || strResult.equals("")) {
            SCAN_QR_RESULT_SUCCESS = 1;
            if (mNoCameraPermissionDialog != null) {
                mNoCameraPermissionDialog.show();
            }
            ErrorBean errorBean = new ErrorBean();
            errorBean.setErrorCode(SCAN_QR_RESULT_OTHER_ERROR);
            errorBean.setErrorMessage("错误二维码，二维码内容为空，请确认后重试");
            Message message = new Message();
            message.what = mCallbackMeesageWhat;
            message.arg1 = SCAN_QR_RESULT_SUCCESS;
            message.obj = errorBean;
            mCallbackHandler.sendMessage(message);
            Toast.makeText(mContext, "错误二维码数据", Toast.LENGTH_SHORT).show();
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats,
                        characterSet);
            }
            handler2.sendEmptyMessage(RESTART_CARERA);
        } else {
            mHandler.sendEmptyMessage(1);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {

        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats,
                    characterSet);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        hasSurface = false;

    }

    public ViewfinderView getViewfinderView() {

        return viewfinderView;
    }

    public Handler getHandler() {

        return handler;
    }

    public void drawViewfinder() {

        viewfinderView.drawViewfinder();

    }

    //6.0 手动申请权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FMaterialDialogUtils.dismiss(mNoCameraPermissionDialog);
                // 允许相机权限
                if (hasSurface) {
                    initCamera(surfaceHolder);
                } else {
                    surfaceHolder.addCallback(this);
                    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                }
            } else {
                // 拒绝相机权限
                SCAN_QR_RESULT_SUCCESS = 1;
                if (mNoCameraPermissionDialog != null) {
                    mNoCameraPermissionDialog.show();
                }
                ErrorBean errorBean = new ErrorBean();
                errorBean.setErrorCode(SCAN_QR_RESULT_NO_CAMERA_PERMISSION);
                errorBean.setErrorMessage(getResources().getString(R.string.f_library_zxing_camera_permission));
                Message message = new Message();
                message.what = mCallbackMeesageWhat;
                message.arg1 = SCAN_QR_RESULT_SUCCESS;
                message.obj = errorBean;
                mCallbackHandler.sendMessage(message);
//                Toast.makeText(mContext,"对不起，权限被拒绝，无法启用相机",Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ImageUtils.openLocalImage(CaptureActivity.this);
            } else {
                FMaterialDialogUtils.alert(mContext, getResources().getString(R.string.f_library_zxing_camera_permission), null);
//                ShowDialogUtils.createAlertDialog(mContext, "对不起，权限被拒绝，无法选取图片，请设置为允许");
//                Toast.makeText(mContext,"对不起，权限被拒绝，无法选取图片",Toast.LENGTH_LONG).show();
            }
        }
    }

    // 扫描到二维码的事件处理
    @Override
    public boolean handleMessage(Message msg) {

        if (mCallbackHandler != null) {
            SCAN_QR_RESULT_SUCCESS = 0;
            Message message = new Message();
            message.what = mCallbackMeesageWhat;
            message.arg1 = SCAN_QR_RESULT_SUCCESS;
            message.obj = strResult;

            mCallbackHandler.sendMessage(message);
        } else {

            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_KEY, strResult);
            this.setResult(RESULT_CODE_OK, resultIntent);

            //发送广播
            Intent intent = new Intent(mDecodeAction);
            intent.putExtra(BROADCAST_INTENT_EXTRE_RESULT_NAME, strResult);
            sendBroadcast(intent);
        }
        finish();
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
        }
        return super.onKeyDown(keyCode, event);
    }

    public final static int RESTART_CARERA = 5; // 重新启动扫描

    public final int RESTART_CARERA_DELAY_TIME = 1000;

    Handler handler2 = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 777:
                    this.sendEmptyMessageDelayed(RESTART_CARERA,
                            RESTART_CARERA_DELAY_TIME);
                    break;
                case RESTART_CARERA:
                    if (handler != null) {
                        handler.quitSynchronously();
                        handler = null;
                    }
                    // CameraManager.get().closeDriver();
                    onResume();
                    break;
                case FINISH_ACTIVITY:
                    finish();
                    break;
            }
        }

        ;
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ImageUtils.GET_IMAGE_FROM_PHONE) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    String picImg = "";
                    picImg = ImageUtils.getPath(this, uri);
                    picImg = ImageUtils.getImage(picImg);
                    Log.d(TAG, "picImg=" + picImg);
                    identifyPic(picImg);
                }
            }
        }
    }

    ;

    /**
     * 识别图片
     */
    private void identifyPic(String picPath) {
        mPicPath = picPath;
        new Thread(new Runnable() {

            @Override
            public void run() {
                Result result = scanningImage(mPicPath);
                // String result = decode(photo_path);
                if (result == null) {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), "无法识别此二维码", Toast.LENGTH_SHORT)
                            .show();
                    Looper.loop();
                } else {
                    // 数据返回
                    String recode = recode(result.toString());
                    Log.d(TAG, "识别结果：" + recode);
                    strResult = recode;
                    mHandler.sendEmptyMessage(1);
//                    Intent intent = new Intent(mDecodeAction);
//                    intent.putExtra("code", strResult);
//                    sendBroadcast(intent);
                }
            }
        }).start();
    }

    // MultiFormatReader multiFormatReader;
    protected Result scanningImage(String path) {
        if (TextUtils.isEmpty(path)) {

            return null;

        }
        // DecodeHintType 和EncodeHintType
        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8"); // 设置二维码内容的编码
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        Bitmap scanBitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false; // 获取新的大小

        int sampleSize = (int) (options.outHeight / (float) 200);

        if (sampleSize <= 0)
            sampleSize = 1;
        options.inSampleSize = sampleSize;
        scanBitmap = BitmapFactory.decodeFile(path, options);

        RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));

        QRCodeReader reader = new QRCodeReader();
        try {
            return reader.decode(bitmap1, hints);
        } catch (NotFoundException | ChecksumException | FormatException ignored) {
        }
        return null;

    }

    /**
     * 字符码处�?
     *
     * @param str
     * @return
     */
    private String recode(String str) {
        String formart = "";

        try {
            boolean ISO = Charset.forName("ISO-8859-1").newEncoder()
                    .canEncode(str);
            if (ISO) {
                formart = new String(str.getBytes("ISO-8859-1"), "GB2312");
                Log.i("ISO8859-1", formart);
            } else {
                formart = str;
                Log.i("stringExtra", str);
            }
        } catch (UnsupportedEncodingException e) {
        }
        return formart;
    }
}
