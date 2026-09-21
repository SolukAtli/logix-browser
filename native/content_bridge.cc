// Faz-4 content bridge: per-tab native engine handles (C++20).
//
// Each handle owns a lightweight navigation state. Rendering stays on the
// WebView fallback until the Trichrome embedding lands (Faz-4.1); this file
// fixes the handle lifecycle + JNI signatures the Kotlin side binds to.

#include <jni.h>

#include <cstdint>
#include <mutex>
#include <string>
#include <unordered_map>

namespace logix {
namespace content {

namespace {

struct EngineState {
  std::string current_url;
  bool can_go_back = false;
  bool can_go_forward = false;
};

std::mutex& RegistryMutex() {
  static std::mutex* mutex = new std::mutex();
  return *mutex;
}

std::unordered_map<std::int64_t, EngineState>& Registry() {
  static std::unordered_map<std::int64_t, EngineState>* registry =
      new std::unordered_map<std::int64_t, EngineState>();
  return *registry;
}

std::int64_t& NextHandle() {
  static std::int64_t next = 1;
  return next;
}

std::string JStringToStd(JNIEnv* env, jstring value) {
  if (value == nullptr) {
    return {};
  }
  const char* chars = env->GetStringUTFChars(value, nullptr);
  std::string out(chars != nullptr ? chars : "");
  if (chars != nullptr) {
    env->ReleaseStringUTFChars(value, chars);
  }
  return out;
}

}  // namespace

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* /*vm*/, void* /*reserved*/) {
  return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL
Java_com_logix_browser_chromiumbridge_NativeLib_nativeCreate(JNIEnv* /*env*/,
                                                             jobject /*thiz*/) {
  std::lock_guard<std::mutex> lock(RegistryMutex());
  const std::int64_t handle = NextHandle()++;
  Registry().emplace(handle, EngineState{});
  return static_cast<jlong>(handle);
}

JNIEXPORT void JNICALL
Java_com_logix_browser_chromiumbridge_NativeLib_nativeDestroy(JNIEnv* /*env*/,
                                                              jobject /*thiz*/,
                                                              jlong handle) {
  std::lock_guard<std::mutex> lock(RegistryMutex());
  Registry().erase(static_cast<std::int64_t>(handle));
}

JNIEXPORT jboolean JNICALL
Java_com_logix_browser_chromiumbridge_NativeLib_nativeNavigate(JNIEnv* env,
                                                               jobject /*thiz*/,
                                                               jlong handle,
                                                               jstring url) {
  std::lock_guard<std::mutex> lock(RegistryMutex());
  auto it = Registry().find(static_cast<std::int64_t>(handle));
  if (it == Registry().end()) {
    return JNI_FALSE;
  }
  it->second.current_url = JStringToStd(env, url);
  it->second.can_go_back = true;
  it->second.can_go_forward = false;
  return JNI_TRUE;
}

}  // extern "C"

}  // namespace content
}  // namespace logix
