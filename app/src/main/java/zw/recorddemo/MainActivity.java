package zw.recorddemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SurfaceView svCamera;
    private ImageButton start;
    private ImageButton pause;
    private CameraManager cameraManager;
    private SurfaceHolder surfaceHolder;
    private String mCameraID;
    private CameraDevice cameraDevice;
    private Handler mainHandler;
    private CameraCaptureSession mCameraCaptureSession;
    private Handler childHandler;
    private String TAG = "ZWS";
    private ImageReader mImageReader;
    private WindowManager windowManager;
    private int width;
    private int heigth;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final int FILE_REQUESTCODE = 33;
    private static final int CAMERA_REQUESTCODE = 44;
    private Integer mSensorOrientation;
    private File videoFile;
    private String path;
    private SimpleDateFormat dateFormat;
    private CaptureRequest.Builder previewBuilder;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
    }

    private void initEvent() {
        start.setOnClickListener(this);
    }


    private void initView() {
        svCamera = findViewById(R.id.svCamera);
        start = findViewById(R.id.start);
        pause = findViewById(R.id.pause);
        surfaceHolder = svCamera.getHolder();
        path = Environment.getExternalStorageDirectory().getPath() + "/LU";
        dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, FILE_REQUESTCODE);
                    }
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        initCamera();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, CAMERA_REQUESTCODE);
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
    }

    private void initCamera() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        windowManager = getWindowManager();
        width = windowManager.getDefaultDisplay().getWidth();
        heigth = windowManager.getDefaultDisplay().getHeight();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;//后摄像头
        try {
            characteristics = cameraManager.getCameraCharacteristics(mCameraID);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        mImageReader = ImageReader.newInstance(width, heigth, ImageFormat.YUV_420_888, 1);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            cameraManager.openCamera(mCameraID, stateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            cameraDevice = camera;
            takePrevious();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {//关闭摄像头
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {//发生错误
            Log.d("zw", "camera onError error code " + error);
        }
    };

    private void takePrevious() {
        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surfaceHolder.getSurface());
            Surface previewSurface = surfaceHolder.getSurface();
            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) return;
                    mCameraCaptureSession = cameraCaptureSession;
                    updatePreview();

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void takeRecord() {
        if (cameraDevice == null) return;
        closePreviewSession();
        try {
            setUpMediaRecorder();
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "CameraAccessException");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "IOException");
            return;
        }
        List<Surface> surfaces = new ArrayList<>();
        Surface previewSurface = svCamera.getHolder().getSurface();
        surfaces.add(previewSurface);
        previewBuilder.addTarget(previewSurface);

        // Set up Surface for the MediaRecorder
        Surface recorderSurface = mMediaRecorder.getSurface();
        surfaces.add(recorderSurface);
        previewBuilder.addTarget(recorderSurface);
        try {
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) return;
                    Log.d(TAG, "onConfigured");
                    mCameraCaptureSession = cameraCaptureSession;
                    updatePreview();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: 2017/11/25 更新UI
                            // Start recording
//                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "onConfigureFailed");

                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == cameraDevice) {
            return;
        }
        try {

            previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mCameraCaptureSession.setRepeatingRequest(previewBuilder.build(), null, childHandler);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        videoFile = new File(path + "/Video", dateFormat.format(new Date()) + ".mp4");
        mMediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(width, heigth);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    private void closePreviewSession() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUESTCODE:
                if (permissions[0].equals(Manifest.permission.CAMERA) && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        permissions[1].equals(Manifest.permission.RECORD_AUDIO) && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    initCamera();
                }
                break;
            case FILE_REQUESTCODE:
                if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initCamera();
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                takeRecord();
                break;
        }
    }
}
