#include "tsdemux.h"
#include <jni.h>

#define MAX_DEMUXERS 512

typedef struct TsDemuxerJNI {
    TSDemuxContext context;
    int isClosed;
    JNIEnv* env;
    jobject obj;
    jmethodID mid;
} KLVParserJNI;

static TsDemuxerJNI demuxers[MAX_DEMUXERS];

const char *TAG = "tsdemuxer";
static void event_cb(TSDemuxContext *ctx, uint16_t pid, TSDEventId event_id, void *data);
static JNIEnv* jniEnv;
static JavaVM* javaVm;

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    for(auto & demuxer : demuxers) {
        demuxer = (TsDemuxerJNI) {
            .context = {nullptr},
            .isClosed = 1,
        };
    }

    jniEnv = env;
    javaVm = vm;
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_kusius_klvmp_JVMPlatformKLVMP_newTsDemuxer(JNIEnv *env, jobject thiz) {
    int index = 0;
    while(index < MAX_DEMUXERS) {
        if(demuxers[index].isClosed) {
            break;
        }
        index++;
    }
    // No free parser slot found, return negative
    if(index >= MAX_DEMUXERS) return -1;

    tsd_context_init(&demuxers[index].context);
    demuxers[index].isClosed = 0;

    tsd_set_event_callback(&demuxers[index].context, event_cb);

    return (jint) index;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_kusius_klvmp_JVMTsKLVDemuxer_demuxKLVNative(JNIEnv *env, jobject thiz,
                                                        jint native_handle,
                                                        jbyteArray stream_data) {
    if(native_handle < 0 || native_handle >= MAX_DEMUXERS) return;

    TsDemuxerJNI* demuxer = &demuxers[native_handle];
    if(demuxer->isClosed) return;

    int arrayLength = (*env).GetArrayLength(stream_data);
    size_t demuxedSize = -1;
    jboolean isCopy;
    jbyte* b = (*env).GetByteArrayElements(stream_data, &isCopy);

    TSDCode result = tsd_demux(&demuxer->context, (char*)b, arrayLength, &demuxedSize);

    // during 'demux' the callback may be called. We can safely discard the buffer
    (*env).ReleaseByteArrayElements(stream_data, b, 0);

    if(result != TSD_OK) return;

}

static void event_cb(TSDemuxContext *ctx, uint16_t pid, TSDEventId event_id, void *data) {
    int index;
    for(index = 0; index < MAX_DEMUXERS; index++) {
        if(&demuxers[index].context == ctx) {
            break;
        }
    }

    TsDemuxerJNI* demuxer = &demuxers[index];

    if(index >= 0 && index < MAX_DEMUXERS) {
        if(event_id == TSD_EVENT_PAT) {
            // do nothing
        } else if(event_id == TSD_EVENT_PMT) {
            auto *pmt = (TSDPMTData*)data;
            size_t i;

            for(i=0;i<pmt->program_elements_length; ++i) {
                TSDProgramElement *prog = &pmt->program_elements[i];
                size_t j;
                for(j=0;j<prog->descriptors_length;++j) {
                    TSDDescriptor *des = &prog->descriptors[j];
                    if(des->tag == 0x05) { // registration descriptor
                        TSDDescriptorRegistration res;
                        if(TSD_OK == tsd_parse_descriptor_registration(des->data, des->data_length, &res)) {
                            // we register for PES packets only for KLVA format identifier
                            if(res.format_identifier == 0x4B4C5641) { // KLVA
                                tsd_register_pid(ctx, prog->elementary_pid, TSD_REG_PES);
                            }
                        }
                    }
                }
            }
        } else if(event_id == TSD_EVENT_PES) {
            auto *pes = (TSDPESPacket*) data;
            // Data from our registered KLV stream was received. Pass it to Java callback
            jbyteArray bytes = demuxer->env->NewByteArray((int)pes->data_bytes_length);
            demuxer->env->SetByteArrayRegion(bytes, 0, (int)pes->data_bytes_length,
                                       reinterpret_cast<const jbyte *>(pes->data_bytes));

            // Call back into java with the env, obj and method set by the registerCallback function
            if(pes->data_bytes_length > 0) {
                (demuxer->env)->CallVoidMethod(demuxer->obj, demuxer->mid, bytes);

                if(demuxer->env->ExceptionOccurred()) {
                    demuxer->env->ExceptionDescribe();
                }
            }
        } else {
            // unkown packet type. Do nothing
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_kusius_klvmp_JVMTsKLVDemuxer_disposeDemuxer(JNIEnv *env, jobject thiz,
                                                        jint native_handle) {
    TsDemuxerJNI* demuxer = &demuxers[native_handle];
    TSDemuxContext *context = &demuxer->context;

    if(context != nullptr) {
        tsd_demux_end(context);
        tsd_context_destroy(context);
    }

    (demuxer->env)->DeleteGlobalRef(demuxer->obj);

    demuxers[native_handle] = (TsDemuxerJNI) {
            .context = {nullptr},
            .isClosed = 1
    };

    // we detach and clear global reference because we attached in registerCallback function
//    javaVm->DetachCurrentThread();
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_kusius_klvmp_JVMTsKLVDemuxer_registerCallback(JNIEnv *env, jobject thiz,
                                                          jint native_handle) {
    // get our parser
    if(native_handle < 0 || native_handle >= MAX_DEMUXERS) return;

    // Slight differentiation of JNI interface for "AttachCurrentThread" between
    // android and rest of JNI platforms.

    // seems like this is not needed
//#if __ANDROID__
//    javaVm->AttachCurrentThread(&env, nullptr);
//#else
//    javaVm->AttachCurrentThread((void**)&env, nullptr);
//#endif

    TsDemuxerJNI* demuxer = &demuxers[native_handle];
    demuxer->env = env;
    demuxer->obj = (env)->NewGlobalRef(thiz);;

    // AndroidTsKLVDemuxer::onKLVBytesReceived
    jclass javaClass = (env)->GetObjectClass(demuxer->obj);
    jmethodID mid = (env)->GetMethodID(javaClass, "onKLVBytesReceived", "([B)V");
    demuxer->mid = mid;
}