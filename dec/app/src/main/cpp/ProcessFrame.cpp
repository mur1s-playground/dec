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
cv::Mat detections;

std::vector<std::string> labels_v;


JNIEXPORT void JNICALL Java_de_mur1_dec_ProcessFrame_init(JNIEnv *env, jclass obj) {
    input_pixels = cv::Mat(300, 300, CV_8UC3);
    //test_mat = cv::Mat(300, 300, CV_8UC3);
    //test_mat.setTo(0);
}

JNIEXPORT void JNICALL Java_de_mur1_dec_ProcessFrame_initNet(JNIEnv *env, jclass obj, jbyteArray labels, jint labels_size, jbyteArray config, jint config_size, jbyteArray weights, jint weights_size) {
    jboolean  l_c;
    jbyte *labels_p = env->GetByteArrayElements(labels, &l_c);

    __android_log_write(ANDROID_LOG_ERROR, "process_frame_jni", "start copy labels");
    char *labels_ptr = new char[labels_size+1];
    for (int i = 0; i < labels_size; i++) {
        labels_ptr[i] = labels_p[i];
    }
    labels_ptr[labels_size] = '\0';
    std::string labels_str(labels_ptr);

    int start = 0;
    int end = labels_str.find_first_of("\n", start);
    while (end != std::string::npos) {
        labels_v.push_back(labels_str.substr(start, end - start));
        start = end + 1;
        end = labels_str.find_first_of("\n", start);
    }
    delete[] labels_ptr;

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
    //net = cv::dnn::readNetFromCaffe(config_ptr, config_size, weights_ptr, weights_size);
    net = cv::dnn::readNetFromTensorflow(weights_ptr, weights_size, config_ptr, config_size);
    net.setPreferableBackend(cv::dnn::DNN_BACKEND_DEFAULT);
    net.setPreferableTarget(cv::dnn::DNN_TARGET_CPU);

    __android_log_write(ANDROID_LOG_ERROR, "process_frame_jni", "net loaded");

    env->ReleaseByteArrayElements(labels, labels_p, 0);
    env->ReleaseByteArrayElements(config, config_p, 0);
    env->ReleaseByteArrayElements(weights, weights_p, 0);

    delete[] config_ptr;
    delete[] weights_ptr;
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

    /*
    int min_size = height * (height <= width) + width * (height > width);
    float scale_factor = min_size / 300.0f;
    int width_offset = (width - min_size) / 2;
    int height_offset = (height - min_size) / 2;
     */
    float height_factor = (height / 300.0);
    float width_factor = (width / 300.0);
    unsigned char *pixel_c = (unsigned char *) pixels;
    for (int r = 0; r < 300; r++) {
        for (int c = 0; c < 300; c++) {
            input_pixels.data[r * 300 * 3 + c * 3 + 0] = pixel_c[((int)(r * height_factor)) * width * 4 + ((int)(c * width_factor)) * 4 + 2];
            input_pixels.data[r * 300 * 3 + c * 3 + 1] = pixel_c[((int)(r * height_factor)) * width * 4 + ((int)(c * width_factor)) * 4 + 1];
            input_pixels.data[r * 300 * 3 + c * 3 + 2] = pixel_c[((int)(r * height_factor)) * width * 4 + ((int)(c * width_factor)) * 4 + 0];
            /*
            input_pixels.data[r * 300 * 3 + c * 3 + 0] = pixel_c[((int)(r * scale_factor) + height_offset) * width * 4 + ((int)(c * scale_factor) + width_offset) * 4 + 2];
            input_pixels.data[r * 300 * 3 + c * 3 + 1] = pixel_c[((int)(r * scale_factor) + height_offset) * width * 4 + ((int)(c * scale_factor) + width_offset) * 4 + 1];
            input_pixels.data[r * 300 * 3 + c * 3 + 2] = pixel_c[((int)(r * scale_factor) + height_offset) * width * 4 + ((int)(c * scale_factor) + width_offset) * 4 + 0];
            */
        }
    }

    //mask rcnn
    //cv::dnn::blobFromImage(input_pixels, blob, 1.0, cv::Size(300, 300), cv::Scalar(), false, false);
    //std::vector<std::string> out_names(2);
    //out_names[0] = "detection_out_final";
    //out_names[1] = "detection_masks";

    //face detection
    //cv::dnn::blobFromImage(input_pixels, blob, 1.0, cv::Size(300, 300), cv::Scalar(104.0, 177.0, 123.0));

    //ssd mobilenet v3 small
    cv::dnn::blobFromImage(input_pixels, blob, 0.007843, cv::Size(300, 300), cv::Scalar(127.5, 127.5, 127.5), false, false);

    AndroidBitmap_unlockPixels(env, bitmap);

    net.setInput(blob);

    std::vector<cv::Mat> outs;
    detections = net.forward("detection_out");

    //__android_log_print(ANDROID_LOG_ERROR, "process_frame", "%i %i %i %i", test_mat.size[0], test_mat.size[1], test_mat.size[2], test_mat.size[3]);
    //__android_log_print(ANDROID_LOG_ERROR, "process_frame", "%i", detections.size[2]);

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

    int num_detections = detections.size[2];
    detections = detections.reshape(1, detections.total() / 7);

    //orig_pixels = cv::Mat(1080, 1920, CV_8UC4, pixels);
    //orig_pixels.data[0] = 255;
    float x_center_avg = 0;
    float y_center_avg = 0;
    int dets = 0;

    for (int d = 0; d < num_detections; d++) {
        int class_id = static_cast<int>(detections.at<float>(d, 1));
        float confidence = detections.at<float>(d, 2);
        //__android_log_print(ANDROID_LOG_ERROR, "process_frame", "class id %i,\t conf %f", class_id, confidence);
        if (confidence > 0.5 && class_id == 1) {
            /*
            orig_pixels.data[0] = 0;
            orig_pixels.data[1] = 0;
            orig_pixels.data[2] = 0;
            orig_pixels.data[3] = 0;
            */
            int x1 = (int)(static_cast<int>(300 * detections.at<float>(d, 3)) * width_factor);
            int y1 = (int)(static_cast<int>(300 * detections.at<float>(d, 4)) * height_factor);
            int x2 = (int)(static_cast<int>(300 * detections.at<float>(d, 5)) * width_factor);
            int y2 = (int)(static_cast<int>(300 * detections.at<float>(d, 6)) * height_factor);

            x1 = 0 * (x1 < 0) + (x1 > 0) * (x1 * (x1 <= width -1) + (width -1) * (x1 > width -1));
            y1 = 0 * (y1 < 0) + (y1 > 0) * (y1 * (y1 <= height -1) + (height -1) * (y1 > height -1));
            x2 = 0 * (x2 < 0) + (x2 > 0) * (x2 * (x2 <= width -1) + (width -1) * (x2 > width -1));
            y2 = 0 * (y2 < 0) + (y2 > 0) * (y2 * (y2 <= height -1) + (height -1) * (y2 > height -1));

            x_center_avg += (x1 + x2) * 0.5f;
            y_center_avg += (y1 + y2) * 0.5f;
            dets++;

            //__android_log_print(ANDROID_LOG_ERROR, "process_frame", "%i %i %i %i %i", x1, y1, x2, y2, class_id);
            /*
            cv::Rect r(cv::Point(x1, y1), cv::Point(x2, y2));
            cv::rectangle(orig_pixels, r, cv::Scalar(255, 0, 0));

            std::stringstream label;
            if (labels_v.size() > class_id) {
                label << labels_v[class_id] << " " << confidence;
            } else {
                label << "unknown classname: " << class_id;
            }
            cv::putText(orig_pixels, label.str(), cv::Point(x1, y1), cv::FONT_HERSHEY_SIMPLEX, 0.75, cv::Scalar(255,255,255), 1);
            */
            //__android_log_print(ANDROID_LOG_ERROR, "process_frame", "%i %i %i %i", x1, y1, x2, y2);
        }
    }
    if (dets > 0) {
        float x_avg = (x_center_avg /(float) dets);
        float y_avg = (y_center_avg /(float) dets);

        __android_log_print(ANDROID_LOG_VERBOSE, "process_frame", "%f %f", x_avg, y_avg);
        float edge_threshold_w = 0.2f;
        float edge_threshold_h = 0.2f;

        if (x_avg < edge_threshold_w * width) {
            __android_log_write(ANDROID_LOG_VERBOSE, "process_frame", "left");
        } else if (x_avg > (1.0f - edge_threshold_w) * width) {
            __android_log_write(ANDROID_LOG_VERBOSE, "process_frame", "right");
        }
        if (y_avg < edge_threshold_h * height) {
            __android_log_write(ANDROID_LOG_VERBOSE, "process_frame", "up");
        } else if (y_avg > (1.0f - edge_threshold_h) * height) {
            __android_log_write(ANDROID_LOG_VERBOSE, "process_frame", "down");
        }
    }
    //AndroidBitmap_unlockPixels(env, bitmap);
}

#ifdef __cplusplus
    }
#endif