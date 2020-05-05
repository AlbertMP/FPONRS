package com.albert.fponrs;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;

public class Tensorflow {
    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String MODEL_FILE2 = "file:///android_asset/classify_Triple.pb";

    private static final String INPUT_NODE = "input";
    private static final String OUTPUT_NODE = "head1_bottleneck/reshape";

    private static final String INPUT_NODE2 = "BottleneckInputPlaceholder";
    private static final String OUTPUT_NODE2 = "logits_eval";

    private static final int NUM_CLASSES = 2048;
    public static final int NUM_CLASSES2 = 3;
    public static final int HEIGHT = 224;
    public static final int WIDTH = 224;
    public static final int MAXRSULT = 3;
    private int[] intValues = new int[WIDTH * HEIGHT];
    private float[] floatValues = new float[WIDTH * HEIGHT * 3];
    private float[] outputs = new float[NUM_CLASSES];
    private float[] outputs2 = new float[NUM_CLASSES2];
    TensorFlowInferenceInterface inferenceInterface;
    TensorFlowInferenceInterface inferenceInterface2;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    Tensorflow(AssetManager assetManager) {
        //接口定义
        inferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_FILE);
        inferenceInterface2 = new TensorFlowInferenceInterface(assetManager, MODEL_FILE2);
    }

    public String[][] recognize(Bitmap bitmap, ArrayList<String> label) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) / (float) 255);
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) / (float) 255);
            floatValues[i * 3 + 2] = ((val & 0xFF) / (float) 255);
        }
        Trace.beginSection("feed");
        inferenceInterface.feed(INPUT_NODE, floatValues, new long[]{1, WIDTH, HEIGHT, 3});
        Trace.endSection();
        //进行模型的推理
        Trace.beginSection("run");
        String[] output = new String[]{OUTPUT_NODE};
        inferenceInterface.run(output);
        Trace.endSection();
        //获取输出节点的输出信息
        Trace.beginSection("fetch");
        inferenceInterface.fetch(OUTPUT_NODE, outputs);
        Trace.endSection();
        Trace.beginSection("feed2");
        inferenceInterface2.feed(INPUT_NODE2, outputs, new long[]{1, NUM_CLASSES});
        Trace.endSection();
        Trace.beginSection("run2");
        String[] output2 = new String[]{OUTPUT_NODE2};
        inferenceInterface2.run(output2);
        Trace.endSection();
        Trace.beginSection("fetch2");
        inferenceInterface2.fetch(OUTPUT_NODE2, outputs2);
        Trace.endSection();
        float[] newarray = new float[NUM_CLASSES2];
        for (int i = 0; i < NUM_CLASSES2; i++) newarray[i] = outputs2[i];
        String[][] result = findTopThree(newarray, label);
        return result;
    }

    private String[][] findTopThree(float[] output, ArrayList<String> label) {
        int[] index = new int[MAXRSULT];
        String[][] result = new String[MAXRSULT][2];
        for (int j = 0; j < MAXRSULT; j++) {
            float max = output[0];
            for (int i = 0; i < NUM_CLASSES2; i++) {
                if (max < output[i]) {
                    max = output[i];
                    index[j] = i;
                }
            }
            float probability = (float) Math.round(outputs2[index[j]] * 10000) / 100;
            result[j][0] = label.get(index[j]) + "(" + String.valueOf(probability) + "%)";
            result[j][1] = String.valueOf(index[j]);
            output[index[j]] = 0;
        }
        return result;
    }
}
