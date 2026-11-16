#include "graph/pass_order.hpp"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <optional>
#include <queue>
#include <span>
#include <string>

#include "graph/graph_error.hpp"

namespace minemod::renderer {

namespace {

class PassDependencies {
public:
    explicit PassDependencies(std::size_t pass_count) : dependencies_(pass_count), dependents_(pass_count) {}

    void add(std::uint32_t dependency, std::uint32_t dependent) {
        dependencies_[dependent].push_back(dependency);
        dependents_[dependency].push_back(dependent);
    }

    std::span<const std::uint32_t> of(std::uint32_t pass) const {
        return dependencies_[pass];
    }

    std::span<const std::uint32_t> dependents_of(std::uint32_t pass) const {
        return dependents_[pass];
    }

private:
    std::vector<std::vector<std::uint32_t>> dependencies_;
    std::vector<std::vector<std::uint32_t>> dependents_;
};

struct ImageHistory {
    std::optional<std::uint32_t> writer;
    std::vector<std::uint32_t> readers;
};

void add_access_dependencies(const RenderGraph& graph, PassDependencies& dependencies) {
    std::vector<ImageHistory> histories(graph.images().size());
    for (std::uint32_t pass = 0; pass < graph.passes().size(); ++pass) {
        for (const ImageAccess& access : graph.passes()[pass].accesses) {
            ImageHistory& history = histories[access.image.index];
            if (history.writer) {
                dependencies.add(*history.writer, pass);
            }
            if (!writes_image(access.use)) {
                history.readers.push_back(pass);
                continue;
            }
            for (const std::uint32_t reader : history.readers) {
                dependencies.add(reader, pass);
            }
            history.writer = pass;
            history.readers.clear();
        }
    }
}

void add_declared_dependencies(const RenderGraph& graph, PassDependencies& dependencies) {
    for (std::uint32_t pass = 0; pass < graph.passes().size(); ++pass) {
        for (const PassId dependency : graph.passes()[pass].dependencies) {
            dependencies.add(dependency.index, pass);
        }
    }
}

std::string describe_cycle(
    const RenderGraph& graph, const PassDependencies& dependencies, std::span<const std::size_t> unmet_dependencies) {
    const auto is_blocked = [&](std::uint32_t pass) { return unmet_dependencies[pass] != 0; };

    std::vector<std::uint32_t> trail;
    std::uint32_t pass = 0;
    while (!is_blocked(pass)) {
        ++pass;
    }
    while (std::ranges::find(trail, pass) == trail.end()) {
        trail.push_back(pass);
        pass = *std::ranges::find_if(dependencies.of(pass), is_blocked);
    }

    std::string description;
    for (auto blocked = std::ranges::find(trail, pass); blocked != trail.end(); ++blocked) {
        description += "'" + graph.passes()[*blocked].name + "' after ";
    }
    return description + "'" + graph.passes()[pass].name + "'";
}

}

std::vector<PassId> order_passes(const RenderGraph& graph) {
    const std::size_t pass_count = graph.passes().size();
    PassDependencies dependencies(pass_count);
    add_access_dependencies(graph, dependencies);
    add_declared_dependencies(graph, dependencies);

    std::vector<std::size_t> unmet_dependencies(pass_count);
    std::priority_queue<std::uint32_t, std::vector<std::uint32_t>, std::greater<>> ready;
    for (std::uint32_t pass = 0; pass < pass_count; ++pass) {
        unmet_dependencies[pass] = dependencies.of(pass).size();
        if (unmet_dependencies[pass] == 0) {
            ready.push(pass);
        }
    }

    std::vector<PassId> order;
    order.reserve(pass_count);
    while (!ready.empty()) {
        const std::uint32_t pass = ready.top();
        ready.pop();
        order.push_back(PassId{pass});
        for (const std::uint32_t dependent : dependencies.dependents_of(pass)) {
            if (--unmet_dependencies[dependent] == 0) {
                ready.push(dependent);
            }
        }
    }

    if (order.size() != pass_count) {
        throw GraphError(
            "the passes wait on each other in a cycle: " + describe_cycle(graph, dependencies, unmet_dependencies));
    }
    return order;
}

}
