#define GMK_KLV_IMPLEMENTATION
#include "klv.h"
#include <jni.h>
#include <malloc.h>

#define MAX_PARSERS 512

typedef struct KLVParserJNI {
    gmk_KLVParser nativeParser;
    int isClosed;
} KLVParserJNI;

static KLVParserJNI parsers[MAX_PARSERS];
static jobject enumString;
static jobject enumInt ;
static jobject enumFloat;
static jobject enumDouble ;
static jobject enumLong ;
static jobject enumUnknown ;
static jobject enumParseError;

JNIEXPORT void JNICALL
Java_io_kusius_klvmp_AndroidPlatformKLVMP_initNative(JNIEnv *env, jobject thiz) {
    for(int i = 0; i < MAX_PARSERS; i++) {
       parsers[i] = (KLVParserJNI){
               .nativeParser = gmk_newKlvParser(),
               .isClosed = 1
       };
    }

    // Map the JNI value type enum fields for later use when converting native enum to JNI enum
    jclass clValueType = (*env)->FindClass(env, "io/kusius/klvmp/ValueType");
    jfieldID enumStringFID = (*env)->GetStaticFieldID(env, clValueType, "STRING", "Lio/kusius/klvmp/ValueType;");
    enumString = (*env)->GetStaticObjectField(env, clValueType, enumStringFID);

    jfieldID enumIntFID = (*env)->GetStaticFieldID(env, clValueType, "INT", "Lio/kusius/klvmp/ValueType;");
    enumInt = (*env)->GetStaticObjectField(env, clValueType, enumIntFID);

    jfieldID enumFloatFID = (*env)->GetStaticFieldID(env, clValueType, "FLOAT", "Lio/kusius/klvmp/ValueType;");
    enumFloat = (*env)->GetStaticObjectField(env, clValueType, enumFloatFID);

    jfieldID enumDoubleFID = (*env)->GetStaticFieldID(env, clValueType, "DOUBLE", "Lio/kusius/klvmp/ValueType;");
    enumDouble = (*env)->GetStaticObjectField(env, clValueType, enumDoubleFID);

    jfieldID enumLongFID = (*env)->GetStaticFieldID(env, clValueType, "LONG", "Lio/kusius/klvmp/ValueType;");
    enumLong = (*env)->GetStaticObjectField(env, clValueType, enumLongFID);

    jfieldID enumUnknownFID = (*env)->GetStaticFieldID(env, clValueType, "UNKNOWN", "Lio/kusius/klvmp/ValueType;");
    enumUnknown = (*env)->GetStaticObjectField(env, clValueType, enumUnknownFID);

    jfieldID enumParseErrorFID = (*env)->GetStaticFieldID(env, clValueType, "PARSE_ERROR", "Lio/kusius/klvmp/ValueType;");
    enumParseError = (*env)->GetStaticObjectField(env, clValueType, enumParseErrorFID);

}

JNIEXPORT jint JNICALL
Java_io_kusius_klvmp_AndroidPlatformKLVMP_newKLVParser(JNIEnv *env, jobject obj) {
    // Find an available slot in the parsers list
    int index = 0;
    while(index < MAX_PARSERS) {
        if(parsers[index].isClosed) {
            break;
        }
        index++;
    }

    // No free parser slot found, return negative
    if(index >= MAX_PARSERS) return -1;

    parsers[index].isClosed = 0;
    return (jint) index;
}

static inline void onEndSetCallback(int size) {
    int a = size + 1;
}

jobject mapValueType(enum gmk_KLVValueType nativeType) {
    switch (nativeType) {
        case GMK_KLV_VALUE_DOUBLE:
            return enumDouble;
            break;
        case GMK_KLV_VALUE_FLOAT:
            return enumFloat;
            break;
        case GMK_KLV_VALUE_INT:
            return enumInt;
            break;
        case GMK_KLV_VALUE_STRING:
            return enumString;
            break;
        case GMK_KLV_VALUE_UINT64:
            return enumLong;
            break;
        case GMK_KLV_VALUE_UNKNOWN:
            return enumUnknown;
            break;
        case GMK_KLV_VALUE_PARSE_ERROR:
            return enumParseError;
            break;
        default:
            return enumUnknown;
            break;
    }
}

JNIEXPORT void JNICALL
Java_io_kusius_klvmp_AndroidKLVParser_parseKLV(JNIEnv *env, jobject obj,
                                               jint index,
                                               jbyteArray bytes,
                                               jint length,
                                               jobjectArray result,
                                               jint resultSize) {
    struct gmk_KLVElement nativeResult[512] = {0};
    int parsedCount = gmk_klvParseResult(&(parsers[index].nativeParser), bytes, length, nativeResult, 512);

    // Transfer results into Java array, taking care not to overflow given result array
    parsedCount = parsedCount < resultSize ? parsedCount : resultSize;
    for(int i = 0; i < parsedCount; i++) {
        gmk_KLVElement nativeKlv = nativeResult[i];

        jobject klv = (*env)->GetObjectArrayElement(env, result, i);
        jclass klElementClass = (*env)->GetObjectClass(env, klv);

        // Key
        jfieldID fieldId = (*env)->GetFieldID(env, klElementClass, "key", "I");
        (*env)->SetIntField(env, klv, fieldId, gmk_klvKey(nativeKlv));

        // Length
        fieldId = (*env)->GetFieldID(env, klElementClass, "length", "I");
        (*env)->SetIntField(env, klv, fieldId, nativeKlv.length);

        // ValueType (enum)
        jobject enumType = mapValueType(nativeKlv.valueType);
        fieldId = (*env)->GetFieldID(env, klElementClass, "valueType", "Lio/kusius/klvmp/ValueType;");
        (*env)->SetObjectField(env, klv, fieldId, enumType);

        // Value
        // TODO: Figure out how to set based on ValueType

    }
}

JNIEXPORT void JNICALL
Java_io_kusius_klvmp_AndroidKLVParser_disposeParser(JNIEnv *env, jobject thiz,
                                                    jint native_handle) {

    parsers[native_handle] = (KLVParserJNI) {
        .isClosed = 1,
        .nativeParser = gmk_newKlvParser()
    };

}
