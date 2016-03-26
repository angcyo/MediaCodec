package com.angcyo.mediacodec;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback, TextureView.SurfaceTextureListener {

    Camera mCamera;
    private int camWidth = 1920;
    private int camHeight = 1080;
    private byte[] buf;
    private MediaCodec mediaCodec;
    private String mimeType = "video/avc";

    /**
     * 根据mimeType 返回对应额 MediaCode
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * 显示所有编码器,以及支持的类型
     */
    private static void showAllEncoderInfo() {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            Log.i("-->", codecInfo.getName());
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                Log.e("类型", types[j]);
            }
        }
    }

    /**
     * 显示编码器支持的所有颜色格式
     */
    private static void showAllEncoderColorFormat(MediaCodecInfo mediaCodecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilitiesForType = mediaCodecInfo.getCapabilitiesForType(mimeType);
        int[] colorFormats = capabilitiesForType.colorFormats;
        for (int i = 0; i < colorFormats.length; i++) {
            Log.i("ColorFormat", "" + colorFormats[i]);
        }
    }

    /**
     * 返回是否是棒棒糖以上的系统
     */
    public static boolean isLOLLIPOPAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextureView textureView = (TextureView) findViewById(R.id.textureView);
        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        textureView.setSurfaceTextureListener(this);
//        try {
//            openCamera(textureView.getSurfaceTexture());
//            startPreview();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        showAllEncoderInfo();

        showAllEncoderColorFormat(selectCodec(mimeType), mimeType);
    }

    /**
     * 打开摄像头,并设置参数
     */
    private void openCamera(SurfaceTexture surfaceTexture) throws IOException {
        mCamera = Camera.open();
//        mCamera.setPreviewDisplay(holder);
        mCamera.setPreviewTexture(surfaceTexture);
        mCamera.setDisplayOrientation(90);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode("off"); // 无闪光灯
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setPreviewFormat(ImageFormat.YV12);

        parameters.setPictureSize(camWidth, camHeight);
        parameters.setPreviewSize(camWidth, camHeight);

        //这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错
        mCamera.setParameters(parameters);
    }

    /**
     * 开始预览
     */
    private void startPreview() {
        if (mCamera != null) {
            buf = new byte[camWidth * camHeight * 3 / 2];
            mCamera.addCallbackBuffer(buf);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();
        }
    }

    /**
     * 停止预览
     */
    private void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 初始化编码器
     */
    private void initMediaCodec() throws IOException {

        //COLOR_FormatYUV420SemiPlanar api16
        //COLOR_FormatYUV420Flexible  api21
        int colorFormat = isLOLLIPOPAbove() ? MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                : MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, camWidth, camHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, camWidth * camHeight * 5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        mediaCodec = MediaCodec.createEncoderByType(mimeType);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    public void onFrame(byte[] buf, int offset, int length, int flag) {
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

        /*获取输入数据的管道*/
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            /*输入数据*/
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, 0, 0);
        }

        /*获取输出数据的管道*/
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            /*输出的数据*/
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

            /*释放输出的资源*/
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        byte[] head = new byte[10];
//        System.arraycopy(data, 0, head, 0, 10);
//        int[] ints = ByteUtil.bytesToInts(head);
//        StringBuilder builder = new StringBuilder();
//        for (int i = 0; i < ints.length; i++) {
//            builder.append(ints[i]);
//        }
//
//        Log.e("onPreviewFrame", data.length + "  " + builder.toString());

//        testTime(data);

        mCamera.addCallbackBuffer(buf);
    }

    private void testTime(byte[] data) {
        DebugTime.init();
        test1(data);
        DebugTime.time("test1");

        test2(data);
        DebugTime.time("test2");

        test3(data);
        DebugTime.time("test3");

        test4(data);
        DebugTime.time("test4");
    }

    private void test1(byte[] data) {
        byte[] bytes = new byte[data.length];
        System.arraycopy(data, 0, bytes, 0, data.length);
    }

    private void test2(byte[] data) {
        MappedByteBuffer.wrap(data).array();
    }

    private void test3(byte[] data) {
        MappedByteBuffer byteBuffer = (MappedByteBuffer) MappedByteBuffer.allocateDirect(data.length);
        byteBuffer.put(data).array();
    }

    private void test4(byte[] data) {
        ByteBuffer byteBuffer = MappedByteBuffer.allocate(data.length);
        byteBuffer.put(data).array();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            openCamera(surface);
            startPreview();

//            initMediaCodec();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopPreview();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
