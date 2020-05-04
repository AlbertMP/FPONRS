package com.albert.fponrs;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import static android.view.View.GONE;
import static com.albert.fponrs.Tensorflow.HEIGHT;
import static com.albert.fponrs.Tensorflow.MAXRSULT;
import static com.albert.fponrs.Tensorflow.NUM_CLASSES2;
import static com.albert.fponrs.Tensorflow.WIDTH;

public class MainActivity extends Activity implements View.OnClickListener {

    public static final int PHOTO_REQUEST_CAREMA = 1;// 拍照
    public static final int CROP_PHOTO = 2;
    public static final int GET_PHOTO = 3;

    public static final String labelfilename = "label.txt";

    private Button takePhoto;
    private Button getPhoto;
    private Button getClass;
    private Button cancel;
    private Button[] button = new Button[MAXRSULT];
    private Button dialog;
    private ImageView pictureReview;
    private ImageView resultSample;
    private Uri imageUri;
    public static File tempFile;
    public ArrayList<String> labelname = new ArrayList<>();
    private String findlabel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        try {
            AssetManager am = getAssets();
            InputStream is = am.open(labelfilename);
            labelname = readTxtFromAssets(is);
            for (int i = 0; i < NUM_CLASSES2; i++) {
                if (i < NUM_CLASSES2 - 1)
                    findlabel = findlabel + labelname.get(i) + "、";
                else
                    findlabel = findlabel + labelname.get(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        takePhoto = (Button) findViewById(R.id.take_photo);
        getPhoto = (Button) findViewById(R.id.get_photo);
        getClass = (Button) findViewById(R.id.get_class);
        cancel = (Button) findViewById(R.id.cancel);
        dialog = (Button) findViewById(R.id.button3);
        button[0] = (Button) findViewById(R.id.first);
        button[1] = (Button) findViewById(R.id.second);
        button[2] = (Button) findViewById(R.id.third);
        pictureReview = (ImageView) findViewById(R.id.pictureReview);
        resultSample = (ImageView) findViewById(R.id.resultSample);
        takePhoto.setOnClickListener(this);
        getClass.setOnClickListener(this);
        dialog.setOnClickListener(this);
        cancel.setOnClickListener(this);
        button[0].setVisibility(GONE);
        button[1].setVisibility(GONE);
        button[2].setVisibility(GONE);
        dialog.setText("查看可区分的种类");
        getPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, GET_PHOTO);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_photo:
                openCamera(this);
                break;
            case R.id.button3:
            case R.id.get_class:
                showDialog();
                break;
            case R.id.cancel:
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("可区分的种类");
        builder.setMessage(findlabel);
        builder.setPositiveButton("我知道了", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Tensorflow tensorflow = new Tensorflow(getAssets());
//        Log.i("", "模型加载成功");
        switch (requestCode) {
            case PHOTO_REQUEST_CAREMA:
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(imageUri, "image/*");
                    intent.putExtra("scale", true);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CROP_PHOTO); // 启动裁剪程序
                }
                break;
            case GET_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver()
                                .openInputStream(imageUri));
                        bitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, false);
                        final String[][] result = tensorflow.recognize(bitmap, labelname);
                        pictureReview.setImageBitmap(bitmap);
                        String showFlower =
                                labelname.get(Integer.parseInt(result[0][1]));
                        int resID = getResources().getIdentifier(showFlower, "drawable", "com.albert.fponrs");
                        resultSample.setImageResource(resID);
                        pictureReview.setImageBitmap(bitmap);
                        for (int i = 0; i < MAXRSULT; i++) {
                            button[i].setVisibility(View.VISIBLE);
                            button[i].setText(result[i][0]);
                        }
                        button[0].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String showFlower2 =
                                        labelname.get(Integer.parseInt(result[0][1]));
                                int resID = getResources().getIdentifier(showFlower2, "drawable", "com.albert.fponrs");
                                resultSample.setImageResource(resID);
                            }
                        });
                        button[1].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String showFlower2 =
                                        labelname.get(Integer.parseInt(result[1][1]));
                                int resID = getResources().getIdentifier(showFlower2, "drawable", "com.albert.fponrs");
                                resultSample.setImageResource(resID);
                            }
                        });
                        button[2].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String showFlower2 =
                                        labelname.get(Integer.parseInt(result[2][1]));
                                int resID = getResources().getIdentifier(showFlower2, "drawable", "com.albert.fponrs");
                                resultSample.setImageResource(resID);
                            }
                        });
                        cancel.setVisibility(View.VISIBLE);
                        resultSample.setVisibility(View.VISIBLE);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver()
                                .openInputStream(imageUri));
                        bitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, false);
                        final String[][] result = tensorflow.recognize(bitmap, labelname);
                        pictureReview.setImageBitmap(bitmap);
                        String showFlower =
                                labelname.get(Integer.parseInt(result[0][1]));
                        int resID = getResources().getIdentifier(showFlower, "drawable", "com.albert.fponrs");
                        resultSample.setImageResource(resID);
                        pictureReview.setImageBitmap(bitmap);
                        for (int i = 0; i < MAXRSULT; i++) {
                            button[i].setVisibility(View.VISIBLE);
                            button[i].setText(result[i][0]);
                        }
                        button[0].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String showFlower2 =
                                        labelname.get(Integer.parseInt(result[0][1]));
                                int resID = getResources().getIdentifier(showFlower2, "drawable", "com.albert.fponrs");
                                resultSample.setImageResource(resID);
                            }
                        });
                        button[1].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String showFlower2 =
                                        labelname.get(Integer.parseInt(result[1][1]));
                                int resID = getResources().getIdentifier(showFlower2, "drawable", "com.albert.fponrs");
                                resultSample.setImageResource(resID);
                            }
                        });
                        button[2].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String showFlower2 =
                                        labelname.get(Integer.parseInt(result[2][1]));
                                int resID = getResources().getIdentifier(showFlower2, "drawable", "com.albert.fponrs");
                                resultSample.setImageResource(resID);
                            }
                        });
                        cancel.setVisibility(View.VISIBLE);
                        resultSample.setVisibility(View.VISIBLE);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    public void openCamera(Activity activity) {
        // 获取系統版本
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        // 激活相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 判断存储卡是否可以用，可用进行存储
        if (hasSdcard()) {
            SimpleDateFormat timeStampFormat = new SimpleDateFormat(
                    "yyyy_MM_dd_HH_mm_ss");
            String filename = timeStampFormat.format(new Date());
            tempFile = new File(Environment.getExternalStorageDirectory(),
                    filename + ".jpg");
            if (currentapiVersion < 24) {
                // 从文件中创建uri
                imageUri = Uri.fromFile(tempFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            } else {
                //兼容android7.0 使用共享文件的形式
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(MediaStore.Images.Media.DATA, tempFile.getAbsolutePath());
                //检查是否有存储权限，以免崩溃
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    //申请WRITE_EXTERNAL_STORAGE权限
                    Toast.makeText(this, "请开启存储权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                imageUri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            }
        }
        // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_CAREMA
        activity.startActivityForResult(intent, PHOTO_REQUEST_CAREMA);
    }

    /*
     * 判断sdcard是否被挂载
     */
    public static boolean hasSdcard() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    private ArrayList<String> readTxtFromAssets(InputStream is) throws Exception {
        InputStreamReader reader = new InputStreamReader(is, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);
        ArrayList<String> buffer = new ArrayList<>();
        String str;
        while ((str = bufferedReader.readLine()) != null) {
            buffer.add(str);
        }
        return buffer;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
