#pragma once

#include <cstddef>
#include <stdexcept>
#include <string>

namespace minemod::shaders {

class SourceError : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

class SyntaxError : public std::runtime_error {
public:
    SyntaxError(std::size_t line, const std::string& message) : std::runtime_error(message), line_(line) {}

    std::size_t line() const noexcept {
        return line_;
    }

private:
    std::size_t line_;
};

}
