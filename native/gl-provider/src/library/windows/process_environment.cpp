#include "library/process_environment.hpp"

#include <algorithm>
#include <array>
#include <cstdlib>
#include <optional>

#include <windows.h>

namespace minemod::provider {

namespace {

using PutEnvironment = int (*)(const wchar_t* name, const wchar_t* value);

constexpr std::array<const wchar_t*, 2> runtimes_with_an_environment_copy{L"ucrtbase.dll", L"msvcrt.dll"};

std::optional<std::wstring> widened(const std::string& text) {
    if (text.empty()) {
        return std::wstring();
    }
    const auto narrow_length = static_cast<int>(text.size());
    const int wide_length =
        ::MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, text.data(), narrow_length, nullptr, 0);
    if (wide_length <= 0) {
        return std::nullopt;
    }
    std::wstring wide(static_cast<std::size_t>(wide_length), L'\0');
    ::MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, text.data(), narrow_length, wide.data(), wide_length);
    return wide;
}

bool updates_environment_copy_of(const wchar_t* runtime, const std::wstring& name, const std::wstring& value) {
    const HMODULE module = ::GetModuleHandleW(runtime);
    if (module == nullptr) {
        return true;
    }
    const auto put = reinterpret_cast<PutEnvironment>(
        reinterpret_cast<void (*)()>(::GetProcAddress(module, "_wputenv_s")));
    return put != nullptr && put(name.c_str(), value.c_str()) == 0;
}

}

bool set_process_variable(const std::string& name, const std::string& value) {
    const std::optional<std::wstring> wide_name = widened(name);
    const std::optional<std::wstring> wide_value = widened(value);
    if (!wide_name || !wide_value) {
        return false;
    }
    if (::SetEnvironmentVariableW(wide_name->c_str(), wide_value->c_str()) == FALSE) {
        return false;
    }
    if (::_wputenv_s(wide_name->c_str(), wide_value->c_str()) != 0) {
        return false;
    }
    return std::all_of(
        runtimes_with_an_environment_copy.begin(),
        runtimes_with_an_environment_copy.end(),
        [&](const wchar_t* runtime) { return updates_environment_copy_of(runtime, *wide_name, *wide_value); });
}

}
