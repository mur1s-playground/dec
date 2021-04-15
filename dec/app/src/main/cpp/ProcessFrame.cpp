//
// Created by mur1 on 22/10/20.
//

#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <opencv2/opencv.hpp>
#include <opencv2/dnn.hpp>

#include <math.h>

#include <string.h>
#include <malloc.h>

#include <sstream>

#ifdef __cplusplus
    extern "C" {
#endif

cv::Mat blob;
cv::dnn::Net net;
cv::Mat input_pixels;
cv::Mat orig_pixels;


JNIEXPORT void JNICALL Java_de_mur1_dec_ProcessFrame_init(JNIEnv *env, jclass obj) {
    input_pixels = cv::Mat(300, 300, CV_8UC3);
    //test_mat = cv::Mat(300, 300, CV_8UC3);
    //test_mat.setTo(0);
}

JNIEXPORT void JNICALL Java_de_mur1_dec_ProcessFrame_initNet(JNIEnv *env, jclass obj, jbyteArray config, jint config_size, jbyteArray weights, jint weights_size) {
    jboolean c_c;
    jbyte *config_p = env->GetByteArrayElements(config, &c_c);

    __android_log_write(ANDROID_LOG_ERROR, "process_frame_jni", "start copy config");
    char *config_ptr = new char[config_size];
    for (int i = 0; i < config_size; i++) {
        config_ptr[i] = config_p[i];
    }

    jboolean w_c;
    jbyte *weights_p = env->GetByteArrayElements(weights, &w_c);
    __android_log_write(ANDROID_LOG_ERROR, "process_frame_jni", "start copy weights");
    char *weights_ptr = new char[weights_size];
    for (int i = 0; i < weights_size; i++) {
        weights_ptr[i] = weights_p[i];
    }

    __android_log_write(ANDROID_LOG_ERROR, "process_frame_jni", "try load net");
    net = cv::dnn::readNetFromCaffe(config_ptr, config_size, weights_ptr, weights_size);
    //net = cv::dnn::readNetFromTensorflow(weights_ptr, weights_size, config_ptr, config_size);
    net.setPreferableBackend(cv::dnn::DNN_BACKEND_DEFAULT);
    net.setPreferableTarget(cv::dnn::DNN_TARGET_CPU);

    __android_log_write(ANDROID_LOG_ERROR, "process_frame_jni", "net loaded");

    env->ReleaseByteArrayElements(config, config_p, 0);
    env->ReleaseByteArrayElements(weights, weights_p, 0);

    delete config_ptr;
    delete weights_ptr;
    __android_log_write(ANDROID_LOG_ERROR, "process_frame_jni", "initnet done");
}

JNIEXPORT void JNICALL Java_de_mur1_dec_ProcessFrame_destroy(JNIEnv *env, jclass obj) {

}

JNIEXPORT void JNICALL Java_de_mur1_dec_ProcessFrame_process(JNIEnv *env, jclass obj, jobject bitmap) {
    AndroidBitmapInfo  info;
    uint32_t          *pixels;
    int                ret;

    AndroidBitmap_getInfo(env, bitmap, &info);

    if(info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        __android_log_write(ANDROID_LOG_ERROR, "process_frame_jni", "bitmap not RGBA_8888");
        return;
    }

    int width = info.width;
    int height = info.height;

    AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&pixels));

    int min_size = height * (height <= width) + width * (height > width);
    float scale_factor = min_size / 300.0f;
    int width_offset = (width - min_size) / 2;
    int height_offset = (height - min_size) / 2;
    unsigned char *pixel_c = (unsigned char *) pixels;
    for (int r = 0; r < 300; r++) {
        for (int c = 0; c < 300; c++) {
            input_pixels.data[r * 300 * 3 + c * 3 + 0] = pixel_c[((int)(r * scale_factor) + height_offset) * width * 4 + ((int)(c * scale_factor) + width_offset) * 4 + 2];
            input_pixels.data[r * 300 * 3 + c * 3 + 1] = pixel_c[((int)(r * scale_factor) + height_offset) * width * 4 + ((int)(c * scale_factor) + width_offset) * 4 + 1];
            input_pixels.data[r * 300 * 3 + c * 3 + 2] = pixel_c[((int)(r * scale_factor) + height_offset) * width * 4 + ((int)(c * scale_factor) + width_offset) * 4 + 0];
        }
    }

    cv::dnn::blobFromImage(input_pixels, blob, 1.0, cv::Size(300, 300), cv::Scalar(104.0, 177.0, 123.0));

    AndroidBitmap_unlockPixels(env, bitmap);

    std::vector<cv::Mat> outs;

    net.setInput(blob);

    detections = net.forward("detection_out");

    //__android_log_print(ANDROID_LOG_ERROR, "process_frame", "%i %i %i %i", test_mat.size[0], test_mat.size[1], test_mat.size[2], test_mat.size[3]);

/*
    pixel_c = (unsigned char *) pixels;
    for (int r = 0; r < 300; r++) {
        for (int c = 0; c < 300; c++) {
            pixel_c[r * width * 4 + c * 4 + 0] = blob.data[0 * 300 * 300 + r * 300 + c];
            pixel_c[r * width * 4 + c * 4 + 1] = blob.data[1 * 300 * 300 + r * 300 + c];
            pixel_c[r * width * 4 + c * 4 + 2] = blob.data[2 * 300 * 300 + r * 300 + c];

            pixel_c[r * width * 4 + c * 4 + 0] = input_pixels.data[r * 300 * 3 + c * 3 + 0];
            pixel_c[r * width * 4 + c * 4 + 1] = input_pixels.data[r * 300 * 3 + c * 3 + 1];
            pixel_c[r * width * 4 + c * 4 + 2] = input_pixels.data[r * 300 * 3 + c * 3 + 2];

        }
    }
*/
/*
    orig_pixels = cv::Mat(1080, 1920, CV_8UC4, pixels);
    for (int d = 0; d < detections.size[2]; d++) {
        int confidence = detections.data[d * 7 + 2];
        if (confidence > 230) {
            int x1 = (int)((float)detections.data[d * 7 + 3] * 6.4);
            int y1 = (int)((float)detections.data[d * 7 + 4] * 3.6);
            int x2 = (int)((float)detections.data[d * 7 + 5] * 6.4);
            int y2 = (int)((float)detections.data[d * 7 + 6] * 3.6);

            cv::Rect r(cv::Point(x1, y1), cv::Point(x2, y2));
            cv::rectangle(orig_pixels, r, (255, 0, 0, 255));
            //__android_log_print(ANDROID_LOG_ERROR, "process_frame", "%i %i %i %i", x1, y1, x2, y2);
        }
    }
    AndroidBitmap_unlockPixels(env, bitmap);
*/
}

#ifdef __cplusplus
    }
#endif