#include "host/scene_host.hpp"

namespace minemod::scene {

std::string SceneHost::validation_text() const {
    std::string text;
    for (const std::string& message : instance_.validation_messages()) {
        text += message;
        text += '\n';
    }
    return text;
}

}
