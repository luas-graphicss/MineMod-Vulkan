#include "support/scratch_directory.hpp"

#include <cstdlib>
#include <stdexcept>
#include <string>
#include <system_error>

namespace minemod::shaders::test_support {

ScratchDirectory::ScratchDirectory() {
    std::string pattern = (std::filesystem::temp_directory_path() / "minemod-shader-tests-XXXXXX").string();
    if (::mkdtemp(pattern.data()) == nullptr) {
        throw std::runtime_error("the tests could not create a scratch directory");
    }
    path_ = pattern;
}

ScratchDirectory::~ScratchDirectory() {
    std::error_code removal;
    std::filesystem::remove_all(path_, removal);
}

}
