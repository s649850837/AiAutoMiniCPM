cmake_minimum_required(VERSION 3.22.1)

project("aiautominitest")

# Define lib path relative to CMakeLists.txt (src/main/cpp)
set(LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
set(INCLUDE_DIR ${CMAKE_SOURCE_DIR}/include)

include_directories(${INCLUDE_DIR})

# MNN (Monolithic)
add_library(MNN SHARED IMPORTED)
set_target_properties(MNN PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/libMNN.so)

# MNN LLM (Included in libMNN.so)
# add_library(MNN_LLM SHARED IMPORTED)
# set_target_properties(MNN_LLM PROPERTIES IMPORTED_LOCATION
#         ${LIB_DIR}/libMNN_LLM.so)

# If OpenCL backend is needed/provided
# add_library(MNN_CL SHARED IMPORTED)
# set_target_properties(MNN_CL PROPERTIES IMPORTED_LOCATION ${LIB_DIR}/libMNN_CL.so)

add_library(
        aiautominitest
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        aiautominitest
        MNN
        # MNN_LLM
        ${log-lib})
