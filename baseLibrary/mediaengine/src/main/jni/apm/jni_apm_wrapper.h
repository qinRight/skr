/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_zq_mediaengine_filter_audio_APMWrapper */

#ifndef _Included_com_zq_mediaengine_filter_audio_APMWrapper
#define _Included_com_zq_mediaengine_filter_audio_APMWrapper
#ifdef __cplusplus
extern "C" {
#endif
#undef com_zq_mediaengine_filter_audio_APMWrapper_UNINIT
#define com_zq_mediaengine_filter_audio_APMWrapper_UNINIT -1L
#undef com_zq_mediaengine_filter_audio_APMWrapper_APM_SAMPLE_RATE
#define com_zq_mediaengine_filter_audio_APMWrapper_APM_SAMPLE_RATE 16000L
#undef com_zq_mediaengine_filter_audio_APMWrapper_APM_CHANNEL_NUM
#define com_zq_mediaengine_filter_audio_APMWrapper_APM_CHANNEL_NUM 1L
#undef com_zq_mediaengine_filter_audio_APMWrapper_APM_SAMPLE_FORMAT
#define com_zq_mediaengine_filter_audio_APMWrapper_APM_SAMPLE_FORMAT 1L
/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    create
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_create
        (JNIEnv *, jobject);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    processStream
 * Signature: (JLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_processStream
        (JNIEnv *, jobject, jlong, jint, jobject, jint);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    enableHighPassFilter
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_enableHighPassFilter
        (JNIEnv *, jobject, jlong, jboolean);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    enableNs
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_enableNs
        (JNIEnv *, jobject, jlong, jboolean);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    setNsLevel
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_setNsLevel
        (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    enableVAD
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_enableVAD
        (JNIEnv *, jobject, jlong, jboolean);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    setVADLikelihood
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_setVADLikelihood
        (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    enableAECM
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_enableAECM
        (JNIEnv *, jobject, jlong, jboolean);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    enableAEC
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_enableAEC
        (JNIEnv *, jobject, jlong, jboolean);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    setRoutingMode
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_setRoutingMode
        (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    setStreamDelay
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_setStreamDelay
        (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    config
 * Signature: (JIIII)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_config
        (JNIEnv *, jobject, jlong, jint, jint, jint, jint);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    release
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_release
        (JNIEnv *, jobject, jlong);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    read
 * Signature: (JLjava/nio/ByteBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_com_zq_mediaengine_filter_audio_APMWrapper_read
        (JNIEnv *, jobject, jlong, jobject, jint);

/*
 * Class:     com_zq_mediaengine_filter_audio_APMWrapper
 * Method:    attachTo
 * Signature: (JIJZ)V
 */
JNIEXPORT void Java_com_zq_mediaengine_filter_audio_APMWrapper_attachTo
        (JNIEnv *, jobject, jlong, jint, jlong, jboolean);
#ifdef __cplusplus
}
#endif
#endif