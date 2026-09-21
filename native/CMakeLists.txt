# Faz-4 native libs. NOT wired into Gradle yet (Faz-4.1 adds
# externalNativeBuild + NDK ABI filters); kept as the canonical contract
# for the JNI signatures in :core:chromium-bridge NativeLib.kt.
cmake_minimum_required(VERSION 3.22)
project(logix_native CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(logix_filter_engine SHARED filter_engine/jni_bridge.cc)
add_library(logix_content_bridge SHARED content_bridge.cc)
