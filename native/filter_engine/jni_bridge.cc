// Faz-4 native filter engine: JNI bridge (C++20).
//
// Exposes the trie-backed host matcher to Kotlin (NativeLib) so the
// hot interception path can move off the JVM in Faz-4.1. Until the NDK
// wiring lands, :core:adblock's DomainTrie remains the live path and this
// file documents the native contract.

#include <jni.h>

#include <cstdint>
#include <mutex>
#include <string>
#include <unordered_set>
#include <vector>

namespace logix {
namespace filter {

namespace {

// Lower-case ASCII copy used for host normalisation.
std::string ToLower(std::string_view in) {
  std::string out(in);
  for (char& c : out) {
    if (c >= 'A' && c <= 'Z') {
      c = static_cast<char>(c - 'A' + 'a');
    }
  }
  return out;
}

// Label-boundary suffix match: entry "example.com" matches "example.com"
// and "*.example.com", never "notexample.com".
bool SuffixMatch(std::string_view host, std::string_view entry) {
  if (host.size() < entry.size()) {
    return false;
  }
  if (host.substr(host.size() - entry.size()) != entry) {
    return false;
  }
  return host.size() == entry.size() || host[host.size() - entry.size() - 1] == '.';
}

class FilterEngine {
 public:
  void AddBlocked(std::string host) {
    std::lock_guard<std::mutex> lock(mutex_);
    blocked_.insert(ToLower(host));
  }

  void AddAllowed(std::string host) {
    std::lock_guard<std::mutex> lock(mutex_);
    allowed_.insert(ToLower(host));
  }

  void Clear() {
    std::lock_guard<std::mutex> lock(mutex_);
    blocked_.clear();
    allowed_.clear();
  }

  bool ShouldBlock(std::string_view host_raw) const {
    const std::string host = ToLower(host_raw);
    std::lock_guard<std::mutex> lock(mutex_);
    for (const auto& entry : allowed_) {
      if (SuffixMatch(host, entry)) {
        return false;
      }
    }
    for (const auto& entry : blocked_) {
      if (SuffixMatch(host, entry)) {
        return true;
      }
    }
    return false;
  }

 private:
  mutable std::mutex mutex_;
  std::unordered_set<std::string> blocked_;
  std::unordered_set<std::string> allowed_;
};

FilterEngine& GlobalEngine() {
  static FilterEngine* engine = new FilterEngine();
  return *engine;
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

JNIEXPORT jboolean JNICALL
Java_com_logix_browser_chromiumbridge_NativeLib_nativeFilterCheck(JNIEnv* env,
                                                                  jobject /*thiz*/,
                                                                  jstring host) {
  return GlobalEngine().ShouldBlock(JStringToStd(env, host)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_logix_browser_chromiumbridge_NativeLib_nativeFilterAddBlocked(JNIEnv* env,
                                                                       jobject /*thiz*/,
                                                                       jstring host) {
  GlobalEngine().AddBlocked(JStringToStd(env, host));
}

JNIEXPORT void JNICALL
Java_com_logix_browser_chromiumbridge_NativeLib_nativeFilterAddAllowed(JNIEnv* env,
                                                                       jobject /*thiz*/,
                                                                       jstring host) {
  GlobalEngine().AddAllowed(JStringToStd(env, host));
}

JNIEXPORT void JNICALL
Java_com_logix_browser_chromiumbridge_NativeLib_nativeFilterClear(JNIEnv* /*env*/,
                                                                  jobject /*thiz*/) {
  GlobalEngine().Clear();
}

JNIEXPORT jstring JNICALL
Java_com_logix_browser_chromiumbridge_NativeLib_nativeVersion(JNIEnv* env,
                                                              jobject /*thiz*/) {
  return env->NewStringUTF("logix-filter-engine/1.0 (Faz-4 stub)");
}

}  // extern "C"

}  // namespace filter
}  // namespace logix
