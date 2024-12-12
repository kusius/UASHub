#define GMK_KLV_IMPLEMENTATION
#include "klv.h"
#include <jni.h>
#include <stdio.h>

JNIEXPORT void JNICALL
Java_io_kusius_uashub_AndroidPlatform_parseKLV(JNIEnv *env, jobject obj) {
    struct gmk_KLVParser parser = gmk_newKlvParser();
    return;
}