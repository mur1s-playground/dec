cmake_minimum_required(VERSION 3.4.1)

include_directories(/home/mur1/opencv/opencv/include)
include_directories(/home/mur1/opencv/opencv/modules/core/include)
include_directories(/home/mur1/opencv/opencv/modules/calib3d/include/)

include_directories(/home/mur1/opencv/opencv/modules/cudacodec/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudalegacy/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudawarping/include/)
include_directories(/home/mur1/opencv/opencv/modules/flann/include/)
include_directories(/home/mur1/opencv/opencv/modules/java/include/)
include_directories(/home/mur1/opencv/opencv/modules/photo/include/)
include_directories(/home/mur1/opencv/opencv/modules/superres/include/)
include_directories(/home/mur1/opencv/opencv/modules/videostab/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudafeatures2d/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudaobjdetect/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudev/include/)
include_directories(/home/mur1/opencv/opencv/modules/highgui/include/)
include_directories(/home/mur1/opencv/opencv/modules/js/include/)
include_directories(/home/mur1/opencv/opencv/modules/python/include/)
include_directories(/home/mur1/opencv/opencv/modules/ts/include/)
include_directories(/home/mur1/opencv/opencv/modules/viz/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudaarithm/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudafilters/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudaoptflow/include/)
include_directories(/home/mur1/opencv/opencv/modules/dnn/include/)
include_directories(/home/mur1/opencv/opencv/modules/imgcodecs/include/)
include_directories(/home/mur1/opencv/opencv/modules/ml/include/)
include_directories(/home/mur1/opencv/opencv/modules/shape/include/)
include_directories(/home/mur1/opencv/opencv/modules/video/include/)
include_directories(/home/mur1/opencv/opencv/modules/world/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudabgsegm/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudaimgproc/include/)
include_directories(/home/mur1/opencv/opencv/modules/cudastereo/include/)
include_directories(/home/mur1/opencv/opencv/modules/features2d/include/)
include_directories(/home/mur1/opencv/opencv/modules/imgproc/include/)
include_directories(/home/mur1/opencv/opencv/modules/objdetect/include/)
include_directories(/home/mur1/opencv/opencv/modules/stitching/include/)
include_directories(/home/mur1/opencv/opencv/modules/videoio/include/)

add_library(
             processframe-lib
             SHARED
             ProcessFrame.cpp )


find_library(
              log-lib
              log )

find_library(
		lib_jnigraphics
		jnigraphics )

add_library(lib_opencv SHARED IMPORTED)

set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so)

target_link_libraries(
                       processframe-lib
						android
                       ${log-lib}
						${lib_jnigraphics}
                       lib_opencv
)
