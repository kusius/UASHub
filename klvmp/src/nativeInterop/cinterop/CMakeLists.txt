# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.5)
project(klvmp)

find_package(JNI REQUIRED)

include_directories(${JNI_INCLUDE_DIRS})

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
        klv

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        klv-ndk-wrapper.c)

add_library(
        tsdemux

        SHARED

        tsdemux/tsdemux-ndk-wrapper.cpp tsdemux/tsdemux.c
)

target_link_libraries(klv)

target_link_libraries(tsdemux)