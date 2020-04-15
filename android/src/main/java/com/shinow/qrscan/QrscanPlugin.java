package com.shinow.qrscan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.io.ByteArrayOutputStream;

import static com.uuzuche.lib_zxing.activity.CodeUtils.RESULT_SUCCESS;
import static com.uuzuche.lib_zxing.activity.CodeUtils.RESULT_TYPE;

public class QrscanPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {

    private Result result = null;
    private Activity activity;
    private int REQUEST_CODE = 100;
    private int REQUEST_IMAGE = 101;

    public static void registerWith(Registrar registrar) {
        MethodChannel channel = new MethodChannel(registrar.messenger(), "qr_scan");
        QrscanPlugin plugin = new QrscanPlugin(registrar.activity());
        channel.setMethodCallHandler(plugin);
        registrar.addActivityResultListener(plugin);

        ZXingLibrary.initDisplayOpinion(registrar.activity());
    }

    public QrscanPlugin(Activity activity) {
        this.activity = activity;
        CheckPermissionUtils.initPermission(this.activity);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "scan":
                this.result = result;
                showBarcodeView();
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void showBarcodeView() {
        Intent intent = new Intent(activity, SecondActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public boolean onActivityResult(int code, int resultCode, Intent intent) {
        if (code == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                Bundle secondBundle = intent.getBundleExtra("secondBundle");
                if (secondBundle != null) {
                    try {
                        CodeUtils.AnalyzeCallback analyzeCallback = new CustomAnalyzeCallback(this.result, intent);
                        CodeUtils.analyzeBitmap(secondBundle.getString("path"), analyzeCallback);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        if (bundle.getInt(RESULT_TYPE) == RESULT_SUCCESS) {
                            String barcode = bundle.getString(CodeUtils.RESULT_STRING);
                            this.result.success(barcode);
                        }else{
                            this.result.success(null);
                        }
                    }
                }
            } else {
                String errorCode = intent != null ? intent.getStringExtra("ERROR_CODE") : null;
                if (errorCode != null) {
                    this.result.error(errorCode, null, null);
                }
            }
            return true;
        } else if (code == REQUEST_IMAGE) {
            if (intent != null) {
                Uri uri = intent.getData();
                try {
                    CodeUtils.AnalyzeCallback analyzeCallback = new CustomAnalyzeCallback(this.result, intent);
                    CodeUtils.analyzeBitmap(ImageUtil.getImageAbsolutePath(activity, uri), analyzeCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }
}