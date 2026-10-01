/* Host regression harness: production callback C + real UxPlay/JNI headers.
 * Only the VM, Android logging, audio decoding and socket removal are stubs. */
#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include "android_raop_callbacks.h"

static int stops, closes, resets, plays, removals;
static jlong last_generation;
static float last_value;
static jint get_env(JavaVM *vm, void **env, jint version);
static jboolean exception_check(JNIEnv *env) { return JNI_FALSE; }
static jstring new_string(JNIEnv *env, const char *s) { return (jstring)s; }
static void delete_ref(JNIEnv *env, jobject obj) {}
static void call_void(JNIEnv *env, jobject obj, jmethodID method, ...) {
    const char *name = (const char *)method;
    va_list args;
    va_start(args, method);
    if (!strcmp(name, "onVideoPlay")) {
        plays++;
        (void)va_arg(args, jstring);
        last_value = (float)va_arg(args, double);
        last_generation = va_arg(args, jlong);
    } else if (!strcmp(name, "onVideoScrub") || !strcmp(name, "onVideoRate")) {
        last_value = (float)va_arg(args, double);
        last_generation = va_arg(args, jlong);
    } else if (!strcmp(name, "onVideoStop")) {
        stops++;
        last_generation = va_arg(args, jlong);
    } else if (!strcmp(name, "onVideoConnectionClosed")) {
        closes++;
        last_generation = va_arg(args, jlong);
    } else if (!strcmp(name, "onVideoReset")) {
        resets++;
        last_value = (float)va_arg(args, int);
        last_generation = va_arg(args, jlong);
    }
    va_end(args);
}
static jmethodID get_method(JNIEnv *env, jclass cls, const char *name, const char *signature) {
    struct { const char *name, *signature; } methods[] = {
        {"onVideoPlay", "(Ljava/lang/String;FJ)V"}, {"onVideoScrub", "(FJ)V"},
        {"onVideoRate", "(FJ)V"}, {"onVideoStop", "(J)V"},
        {"onVideoConnectionClosed", "(J)V"}, {"onVideoReset", "(ZJ)V"},
    };
    for (size_t i = 0; i < sizeof(methods) / sizeof(methods[0]); i++) {
        if (!strcmp(name, methods[i].name)) assert(!strcmp(signature, methods[i].signature));
    }
    return (jmethodID)name;
}
static const struct JNIInvokeInterface_ vm_table = { .GetEnv = get_env };
static JavaVM vm = &vm_table;
static jint get_vm(JNIEnv *env, JavaVM **out) { *out = &vm; return JNI_OK; }
static jobject global_ref(JNIEnv *env, jobject obj) { return obj; }
static jclass get_class(JNIEnv *env, jobject obj) { return (jclass)obj; }
static const struct JNINativeInterface_ env_table = {
    .ExceptionCheck = exception_check, .NewStringUTF = new_string,
    .DeleteLocalRef = delete_ref, .DeleteGlobalRef = delete_ref,
    .CallVoidMethod = call_void, .GetMethodID = get_method,
    .GetJavaVM = get_vm, .NewGlobalRef = global_ref, .GetObjectClass = get_class,
};
static JNIEnv env = &env_table;
static jint get_env(JavaVM *vm, void **out, jint version) { *out = &env; return JNI_OK; }
int __android_log_print(int priority, const char *tag, const char *fmt, ...) { return 0; }
void audio_engine_decode(AudioEngine *e, const uint8_t *data, int len, int ct, int64_t pts) {}
void raop_remove_hls_connections(raop_t *raop) { removals++; }

static android_callback_ctx_t ctx;
static raop_callbacks_t cb;
static playback_info_t snapshot(void) {
    playback_info_t info = {0};
    cb.on_video_acquire_playback_info(cb.cls, &info);
    return info;
}
static void setup(void) {
    memset(&ctx, 0, sizeof(ctx));
    android_callbacks_init(&ctx, &env, (jobject)1);
    android_callbacks_fill(&cb, &ctx);
    ctx.raop = (raop_t *)1;
    stops = closes = resets = plays = removals = 0;
}
static void idle_close_keeps_pause(void) {
    android_callbacks_update_playback_info(&ctx, 0, 42, 120, 0, 1, 0);
    cb.video_reset(cb.cls, RESET_TYPE_HLS_CONN_CLOSED);
    assert(stops == 0);
    playback_info_t info = snapshot();
    assert(info.position == 42 && info.duration == 120 && info.rate == 0 && info.ready_to_play);
    assert(removals == 0);
}
static void sender_stop_waits_for_owner_decision(void) {
    android_callbacks_update_playback_info(&ctx, 0, 42, 120, 0, 1, 0);
    cb.on_video_stop(cb.cls);
    assert(stops == 1);
    /* Kotlin may reject only the narrow local-pause compatibility stop.
       Until that decision, a concurrent playback-info poll must stay valid. */
    assert(snapshot().duration == 120);
    assert(removals == 0);
    /* Accepted stop publishes the sentinel; the next poll cleans sockets. */
    android_callbacks_update_playback_info(&ctx, 0, 0, -1, 0, 0, 0);
    assert(snapshot().duration == -1);
    cb.video_reset(cb.cls, RESET_TYPE_HLS_SHUTDOWN);
    assert(removals == 1);
}
static void play_does_not_wait_for_its_own_http_server(void) {
    struct timespec before, after;
    clock_gettime(CLOCK_MONOTONIC, &before);
    /* No ready callback: Media3 must fetch HLS from this same HTTP thread. */
    cb.on_video_play(cb.cls, "fixture", 7);
    clock_gettime(CLOCK_MONOTONIC, &after);
    double seconds = after.tv_sec - before.tv_sec + (after.tv_nsec - before.tv_nsec) / 1e9;
    assert(seconds < 0.5);
    assert(plays == 1 && last_generation == 1 && last_value == 7);
    playback_info_t info = snapshot();
    assert(info.position == 7 && info.duration == 0 && !info.ready_to_play);
}
static void close_and_reset_have_distinct_callbacks(void) {
    cb.video_reset(cb.cls, RESET_TYPE_HLS_CONN_CLOSED);
    assert(closes == 1 && stops == 0 && resets == 0);
    cb.video_reset(cb.cls, RESET_TYPE_HLS_EOS);
    assert(resets == 1 && stops == 0 && last_value == JNI_TRUE);
    cb.video_reset(cb.cls, RESET_TYPE_HLS_SHUTDOWN);
    assert(resets == 2 && stops == 0 && removals == 1 && last_value == JNI_FALSE);
}
static void new_connection_does_not_erase_accepted_stop(void) {
    android_callbacks_update_playback_info(&ctx, 0, 0, -1, 0, 0, 0);
    cb.conn_init(cb.cls);
    assert(snapshot().duration == -1);
}
/* Publish a snapshot from the named playback lifetime. */
#define PUBLISH(generation, position, duration, rate, ready, buffering) \
    android_callbacks_update_playback_info(&ctx, generation, position, duration, rate, ready, buffering)
static void new_play_rejects_previous_player_snapshot(void) {
    cb.on_video_play(cb.cls, "fixture-a", 7);
    PUBLISH(1, 8, 120, 1, 1, 0);
    cb.on_video_play(cb.cls, "fixture-a", 23); // same URL is still a new lifetime
    PUBLISH(1, 0, -1, 0, 0, 0); // old main-thread stop queued before /play
    assert(snapshot().position == 23 && snapshot().duration == 0);
    PUBLISH(2, 24, 180, 0, 1, 0);
    assert(snapshot().position == 24 && snapshot().duration == 180);
}
static void buffering_snapshot_is_not_full(void) {
    PUBLISH(0, 7, 0, 0, 0, 1);
    playback_info_t info = snapshot();
    assert(info.playback_buffer_empty && !info.playback_buffer_full && !info.playback_likely_to_keep_up);
}
static void commands_are_tagged_with_current_playback(void) {
    cb.on_video_play(cb.cls, "fixture", 7);
    cb.on_video_scrub(cb.cls, 19);
    assert(last_generation == 1 && last_value == 19);
    cb.on_video_rate(cb.cls, 0);
    assert(last_generation == 1 && last_value == 0);
    cb.on_video_play(cb.cls, "fixture", 23);
    cb.on_video_stop(cb.cls);
    assert(last_generation == 2 && stops == 1);
    cb.video_reset(cb.cls, RESET_TYPE_HLS_CONN_CLOSED);
    assert(last_generation == 2 && closes == 1);
}
int main(int argc, char **argv) {
    struct { const char *name; void (*run)(void); } tests[] = {
        {"idle_close_keeps_pause", idle_close_keeps_pause},
        {"sender_stop_waits_for_owner_decision", sender_stop_waits_for_owner_decision},
        {"play_does_not_wait_for_its_own_http_server", play_does_not_wait_for_its_own_http_server},
        {"close_and_reset_have_distinct_callbacks", close_and_reset_have_distinct_callbacks},
        {"new_connection_does_not_erase_accepted_stop", new_connection_does_not_erase_accepted_stop},
        {"new_play_rejects_previous_player_snapshot", new_play_rejects_previous_player_snapshot},
        {"buffering_snapshot_is_not_full", buffering_snapshot_is_not_full},
        {"commands_are_tagged_with_current_playback", commands_are_tagged_with_current_playback},
    };
    for (size_t i = 0; i < sizeof(tests) / sizeof(tests[0]); i++) {
        if (argc > 1 && strcmp(argv[1], tests[i].name)) continue;
        setup();
        tests[i].run();
        android_callbacks_destroy(&ctx, &env);
        printf("PASS %s\n", tests[i].name);
    }
    return 0;
}
