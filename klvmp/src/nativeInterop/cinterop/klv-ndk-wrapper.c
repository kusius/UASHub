#define GMK_KLV_IMPLEMENTATION
#include "klv.h"
#include <jni.h>

#define MAX_PARSERS 512

typedef struct KLVParserJNI {
    gmk_KLVParser nativeParser;
    int isClosed;
} KLVParserJNI;

static KLVParserJNI parsers[MAX_PARSERS];
static jfieldID enumStringFID;
static jfieldID enumIntFID ;
static jfieldID enumFloatFID;
static jfieldID enumDoubleFID;
static jfieldID enumLongFID ;
static jfieldID enumUnknownFID;
static jfieldID enumParseErrorFID;

const char* TAG = "klvmp";
void setValue(JNIEnv* env, gmk_KLVElement* nativeKLV, jclass class, jobject obj);

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    for(int i = 0; i < MAX_PARSERS; i++) {
       parsers[i] = (KLVParserJNI){
               .nativeParser = gmk_newKlvParser(),
               .isClosed = 1
       };
    }

    // Map the JNI value type enum fields for later use when converting native enum to JNI enum
    jclass clValueType = (*env)->FindClass(env, "io/github/kusius/klvmp/ValueType");
     enumStringFID = (*env)->GetStaticFieldID(env, clValueType, "STRING", "Lio/github/kusius/klvmp/ValueType;");

     enumIntFID = (*env)->GetStaticFieldID(env, clValueType, "INT", "Lio/github/kusius/klvmp/ValueType;");

     enumFloatFID = (*env)->GetStaticFieldID(env, clValueType, "FLOAT", "Lio/github/kusius/klvmp/ValueType;");

     enumDoubleFID = (*env)->GetStaticFieldID(env, clValueType, "DOUBLE", "Lio/github/kusius/klvmp/ValueType;");

     enumLongFID = (*env)->GetStaticFieldID(env, clValueType, "LONG", "Lio/github/kusius/klvmp/ValueType;");

     enumUnknownFID = (*env)->GetStaticFieldID(env, clValueType, "UNKNOWN", "Lio/github/kusius/klvmp/ValueType;");

     enumParseErrorFID = (*env)->GetStaticFieldID(env, clValueType, "PARSE_ERROR", "Lio/github/kusius/klvmp/ValueType;");

    return JNI_VERSION_1_6;
}

JNIEXPORT jint JNICALL
Java_io_github_kusius_klvmp_JVMPlatformKLVMP_newKLVParser(JNIEnv *env, jobject obj) {
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

jfieldID mapValueType(enum gmk_KLVValueType nativeType) {
    switch (nativeType) {
        case GMK_KLV_VALUE_DOUBLE:
            return enumDoubleFID;
            break;
        case GMK_KLV_VALUE_FLOAT:
            return enumFloatFID;
            break;
        case GMK_KLV_VALUE_INT:
            return enumIntFID;
            break;
        case GMK_KLV_VALUE_STRING:
            return enumStringFID;
            break;
        case GMK_KLV_VALUE_UINT64:
            return enumLongFID;
            break;
        case GMK_KLV_VALUE_UNKNOWN:
            return enumUnknownFID;
            break;
        case GMK_KLV_VALUE_PARSE_ERROR:
            return enumParseErrorFID;
            break;
        default:
            return enumUnknownFID;
            break;
    }
}

JNIEXPORT jint JNICALL
Java_io_github_kusius_klvmp_JVMKLVParser_parseKLV(JNIEnv *env, jobject obj,
                                               jint index,
                                               jbyteArray bytes,
                                               jobjectArray result,
                                               jint resultSize) {
    int arrayLength = (*env)->GetArrayLength(env, bytes);
    jboolean isCopy = -1;
    jbyte* b = (*env)->GetByteArrayElements(env, bytes, &isCopy);

    struct gmk_KLVElement nativeResult[512] = {0};
    int parsedCount = gmk_klvParseResult(&(parsers[index].nativeParser), (unsigned char *)b, arrayLength, nativeResult, 512);

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
        jclass clValueType = (*env)->FindClass(env, "io/github/kusius/klvmp/ValueType");
        jfieldID enumTypeFID = mapValueType(nativeKlv.valueType);
        jobject enumType = (*env)->GetStaticObjectField(env, clValueType, enumTypeFID);

        fieldId = (*env)->GetFieldID(env, klElementClass, "valueType", "Lio/github/kusius/klvmp/ValueType;");
        (*env)->SetObjectField(env, klv, fieldId, enumType);

        // Value
        // TODO: Figure out how to set based on ValueType
        fieldId = (*env)->GetFieldID(env, klElementClass, "valueBytes", "[B");
        jbyteArray byteArrayField = (*env)->GetObjectField(env, klv, fieldId);
        byteArrayField = (*env)->NewByteArray(env, nativeKlv.length);
        (*env)->SetByteArrayRegion(env, byteArrayField, 0, nativeKlv.length, (jbyte*)nativeKlv.value);
        (*env)->SetObjectField(env, klv, fieldId, byteArrayField);
        setValue(env, &nativeKlv, klElementClass, klv);
    }

    return (jint) parsedCount;
}

void setValue(JNIEnv* env, gmk_KLVElement* nativeKLV, jclass class, jobject obj) {
    jfieldID fieldId = (*env)->GetFieldID(env, class, "value", "Lio/github/kusius/klvmp/KLVValue;");
    // based on value type, we allocate a new object of the type KLVValue (sealed class), set its value
    // and set it on the object

    switch (nativeKLV->valueType) {

        case GMK_KLV_VALUE_STRING:
        {
            jclass klvValueClass = (*env)->FindClass(env, "io/github/kusius/klvmp/StringValue");
            jmethodID constructor = (*env)->GetMethodID(env, klvValueClass, "<init>",
                                                        "(Ljava/lang/String;)V");
            jstring javaValue = (*env)->NewStringUTF(env, (const char *) nativeKLV->value);
            jobject klvValueObj = (*env)->NewObject(env, klvValueClass, constructor, javaValue);

            jfieldID valueFID = (*env)->GetFieldID(env, class, "value",
                                                   "Lio/github/kusius/klvmp/KLVValue;");
            (*env)->SetObjectField(env, obj, valueFID, klvValueObj);
        }
            break;
        case GMK_KLV_VALUE_INT:
        {
            jclass klvValueClass = (*env)->FindClass(env, "io/github/kusius/klvmp/IntValue");
            jmethodID constructor = (*env)->GetMethodID(env, klvValueClass, "<init>",
                                                        "(I)V");
            jint javaValue = nativeKLV->intValue;
            jobject klvValueObj = (*env)->NewObject(env, klvValueClass, constructor, javaValue);

            jfieldID valueFID = (*env)->GetFieldID(env, class, "value",
                                                   "Lio/github/kusius/klvmp/KLVValue;");
            (*env)->SetObjectField(env, obj, valueFID, klvValueObj);
        }
            break;
        case GMK_KLV_VALUE_FLOAT:
        {
            jclass klvValueClass = (*env)->FindClass(env, "io/github/kusius/klvmp/FloatValue");
            jmethodID constructor = (*env)->GetMethodID(env, klvValueClass, "<init>",
                                                        "(F)V");
            jfloat javaValue = nativeKLV->floatValue;
            jobject klvValueObj = (*env)->NewObject(env, klvValueClass, constructor, javaValue);

            jfieldID valueFID = (*env)->GetFieldID(env, class, "value",
                                                   "Lio/github/kusius/klvmp/KLVValue;");
            (*env)->SetObjectField(env, obj, valueFID, klvValueObj);
        }
            break;
        case GMK_KLV_VALUE_DOUBLE:
        {
            jclass klvValueClass = (*env)->FindClass(env, "io/github/kusius/klvmp/DoubleValue");
            jmethodID constructor = (*env)->GetMethodID(env, klvValueClass, "<init>",
                                                        "(D)V");
            jdouble javaValue = nativeKLV->doubleValue;
            jobject klvValueObj = (*env)->NewObject(env, klvValueClass, constructor, javaValue);

            jfieldID valueFID = (*env)->GetFieldID(env, class, "value",
                                                   "Lio/github/kusius/klvmp/KLVValue;");
            (*env)->SetObjectField(env, obj, valueFID, klvValueObj);
        }
            break;
        case GMK_KLV_VALUE_UINT64:
        {
            jclass klvValueClass = (*env)->FindClass(env, "io/github/kusius/klvmp/LongValue");
            jmethodID constructor = (*env)->GetMethodID(env, klvValueClass, "<init>",
                                                        "(J)V");
            jlong javaValue = (jlong)nativeKLV->uint64Value;
            jobject klvValueObj = (*env)->NewObject(env, klvValueClass, constructor, javaValue);

            jfieldID valueFID = (*env)->GetFieldID(env, class, "value",
                                                   "Lio/github/kusius/klvmp/KLVValue;");
            (*env)->SetObjectField(env, obj, valueFID, klvValueObj);
        }
            break;
        case GMK_KLV_VALUE_UNKNOWN:
        {
            jclass klvValueClass = (*env)->FindClass(env, "io/github/kusius/klvmp/UnknownValue");
            jmethodID constructor = (*env)->GetMethodID(env, klvValueClass, "<init>",
                                                        "()V");
            jobject klvValueObj = (*env)->NewObject(env, klvValueClass, constructor);

            jfieldID valueFID = (*env)->GetFieldID(env, class, "value",
                                                   "Lio/github/kusius/klvmp/KLVValue;");
            (*env)->SetObjectField(env, obj, valueFID, klvValueObj);
        }
            break;
        case GMK_KLV_VALUE_PARSE_ERROR:
        {
            jclass klvValueClass = (*env)->FindClass(env, "io/github/kusius/klvmp/ParseErrorValue");
            jmethodID constructor = (*env)->GetMethodID(env, klvValueClass, "<init>",
                                                        "()V");
            jobject klvValueObj = (*env)->NewObject(env, klvValueClass, constructor);

            jfieldID valueFID = (*env)->GetFieldID(env, class, "value",
                                                   "Lio/github/kusius/klvmp/KLVValue;");
            (*env)->SetObjectField(env, obj, valueFID, klvValueObj);
        }
            break;
    }

}

JNIEXPORT void JNICALL
Java_io_github_kusius_klvmp_JVMKLVParser_disposeParser(JNIEnv *env, jobject thiz,
                                                    jint native_handle) {

    parsers[native_handle] = (KLVParserJNI) {
        .isClosed = 1,
        .nativeParser = gmk_newKlvParser()
    };

}