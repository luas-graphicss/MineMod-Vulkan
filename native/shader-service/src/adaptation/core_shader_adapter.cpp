#include "adaptation/core_shader_adapter.hpp"

#include <algorithm>
#include <array>
#include <charconv>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <map>
#include <span>
#include <string_view>
#include <utility>

#include "source/source_error.hpp"
#include "source/stage.hpp"
#include "syntax/glsl_tokens.hpp"
#include "syntax/interface_declarations.hpp"
#include "syntax/line_map.hpp"

namespace minemod::shaders {

namespace {

constexpr std::string_view adapted_version = "#version 450";
constexpr std::string_view version_directive = "version";
constexpr int oldest_version = 150;
constexpr int newest_version = 460;
constexpr std::size_t max_varyings = 16;
constexpr std::string_view sampler_type = "sampler2D";
constexpr std::string_view color_type = "vec4";
constexpr std::array<std::string_view, 2> reserved_prefixes{"minemod_", "Minemod"};

struct BuiltInRename {
    std::string_view legacy;
    std::string_view current;
};

constexpr std::array built_in_renames{
    BuiltInRename{"gl_VertexID", "gl_VertexIndex"},
    BuiltInRename{"gl_InstanceID", "gl_InstanceIndex"},
};

struct Varying {
    std::string type;
    std::uint32_t location;
};

using Varyings = std::map<std::string, Varying, std::less<>>;

struct Replacement {
    std::size_t first_token;
    std::size_t last_token;
    std::string text;
};

template <typename Binding>
const Binding* find_binding(std::span<const Binding> bindings, std::string_view name) noexcept {
    const auto found = std::ranges::find(bindings, name, &Binding::name);
    return found == bindings.end() ? nullptr : &*found;
}

std::string quoted(std::string_view name) {
    return "'" + std::string(name) + "'";
}

std::optional<int> declared_version(const Token& token) noexcept {
    if (token.kind != TokenKind::directive) {
        return std::nullopt;
    }
    std::string_view rest = token.text.substr(1);
    rest.remove_prefix(std::min(rest.find_first_not_of(" \t"), rest.size()));
    if (!rest.starts_with(version_directive)) {
        return std::nullopt;
    }
    rest.remove_prefix(version_directive.size());
    rest.remove_prefix(std::min(rest.find_first_not_of(" \t"), rest.size()));
    int version = 0;
    const auto parsed = std::from_chars(rest.data(), rest.data() + rest.size(), version);
    return parsed.ec == std::errc{} ? std::optional(version) : std::nullopt;
}

const char* unsupported_form(const InterfaceDeclaration& declaration) noexcept {
    if (declaration.has_layout) {
        return "declares its own layout";
    }
    if (declaration.is_block) {
        return "is an interface block";
    }
    if (declaration.names.size() != 1) {
        return "shares its statement with other names";
    }
    if (declaration.is_array) {
        return "is an array";
    }
    if (declaration.has_initializer) {
        return "has an initializer";
    }
    return nullptr;
}

class StageAdapter {
public:
    StageAdapter(
        const CoreShaderBindings& bindings,
        Stage stage,
        const ExpandedSource& source,
        std::vector<std::string>& problems) noexcept
        : bindings_(bindings), stage_(stage), source_(source), problems_(problems) {}

    std::string adapt(Varyings& varyings) {
        try {
            tokens_ = tokenize(source_.text);
            lines_ = LineMap(tokens_);
            const std::vector<InterfaceDeclaration> declarations = parse_interface(tokens_);
            const bool starts_with_version = check_version();
            report_reserved_names();
            for (const InterfaceDeclaration& declaration : declarations) {
                rewrite(declaration, varyings);
            }
            report_missing_color_output();
            rename_built_ins();
            return starts_with_version ? rewritten() : std::string{};
        } catch (const SyntaxError& error) {
            report(error.line(), error.what());
            return {};
        }
    }

private:
    bool check_version() {
        const std::optional<int> version = tokens_.empty() ? std::nullopt : declared_version(tokens_.front());
        if (!version) {
            report(0, "the shader does not start with a #version directive");
            return false;
        }
        if (*version < oldest_version || *version > newest_version) {
            report(tokens_.front().line, "GLSL " + std::to_string(*version) + " is outside the versions 150 to 460");
        }
        return true;
    }

    void report_reserved_names() {
        for (const Token& token : tokens_) {
            const bool is_reserved = token.kind == TokenKind::identifier
                && std::ranges::any_of(reserved_prefixes, [&token](std::string_view prefix) {
                       return token.text.starts_with(prefix);
                   });
            if (is_reserved) {
                report(token.line, "uses the name " + quoted(token.text) + ", which is reserved for the renderer");
                return;
            }
        }
    }

    void rewrite(const InterfaceDeclaration& declaration, Varyings& varyings) {
        const std::size_t line = tokens_[declaration.first_token].line;
        if (const char* form = unsupported_form(declaration)) {
            const std::string_view label = declaration.names.empty() ? declaration.type : declaration.names.front();
            report(line, quoted(label) + " " + form + ", which a core shader replacement cannot do");
            return;
        }
        replacements_.push_back(
            Replacement{declaration.first_token, declaration.last_token, rewritten_declaration(declaration, varyings)});
    }

    std::string rewritten_declaration(const InterfaceDeclaration& declaration, Varyings& varyings) {
        if (declaration.kind == InterfaceKind::uniform) {
            return bound_uniform(declaration);
        }
        if (declaration.kind == InterfaceKind::input) {
            return stage_ == Stage::vertex ? vertex_element(declaration) : read_varying(declaration, varyings);
        }
        return stage_ == Stage::vertex ? written_varying(declaration, varyings) : color_output(declaration);
    }

    std::string bound_uniform(const InterfaceDeclaration& declaration) {
        const std::string_view name = declaration.names.front();
        const std::size_t line = tokens_[declaration.first_token].line;
        if (const SamplerBinding* sampler = find_binding(bindings_.samplers, name)) {
            return bound_sampler(declaration, *sampler);
        }
        const UniformBinding* uniform = find_binding(bindings_.uniforms, name);
        if (uniform == nullptr) {
            if (is_used(declaration)) {
                report(line, "uses the uniform " + quoted(name) + ", which the renderer does not provide");
            }
            return {};
        }
        if (declaration.type != uniform->type) {
            report(
                line,
                "declares " + quoted(name) + " as " + std::string(declaration.type) + ", but the renderer provides "
                    + std::string(uniform->type));
            return {};
        }
        defines_ += "#define " + std::string(name) + " (" + std::string(uniform->expression) + ")\n";
        return {};
    }

    std::string bound_sampler(const InterfaceDeclaration& declaration, const SamplerBinding& sampler) {
        const std::size_t line = tokens_[declaration.first_token].line;
        if (declaration.type != sampler_type) {
            report(
                line,
                "declares " + quoted(sampler.name) + " as " + std::string(declaration.type)
                    + ", but the renderer binds a sampler2D");
            return {};
        }
        if (sampler.stage != stage_) {
            if (is_used(declaration)) {
                report(
                    line,
                    "samples " + quoted(sampler.name) + ", which is not bound for the "
                        + std::string(stage_name(stage_)) + " stage");
            }
            return {};
        }
        return "layout(set = " + std::to_string(sampler.set) + ", binding = " + std::to_string(sampler.binding)
            + ") uniform sampler2D " + std::string(sampler.name) + ";";
    }

    std::string vertex_element(const InterfaceDeclaration& declaration) {
        const std::string_view name = declaration.names.front();
        const std::size_t line = tokens_[declaration.first_token].line;
        const AttributeBinding* attribute = find_binding(bindings_.attributes, name);
        if (attribute == nullptr) {
            if (is_used(declaration)) {
                report(line, "reads the vertex element " + quoted(name) + ", which no vertex format provides");
            }
            return {};
        }
        if (declaration.type != attribute->type) {
            report(
                line,
                "declares " + quoted(name) + " as " + std::string(declaration.type)
                    + ", but the vertex format provides " + std::string(attribute->type));
            return {};
        }
        return located(attribute->location, declaration, "in");
    }

    std::string written_varying(const InterfaceDeclaration& declaration, Varyings& varyings) {
        const std::string_view name = declaration.names.front();
        const std::size_t line = tokens_[declaration.first_token].line;
        if (varyings.contains(name)) {
            report(line, "writes " + quoted(name) + " to the fragment stage twice");
            return {};
        }
        if (varyings.size() == max_varyings) {
            report(line, "writes more than " + std::to_string(max_varyings) + " values to the fragment stage");
            return {};
        }
        const auto location = static_cast<std::uint32_t>(varyings.size());
        varyings.emplace(std::string(name), Varying{std::string(declaration.type), location});
        return located(location, declaration, "out");
    }

    std::string read_varying(const InterfaceDeclaration& declaration, const Varyings& varyings) {
        const std::string_view name = declaration.names.front();
        const std::size_t line = tokens_[declaration.first_token].line;
        const auto varying = varyings.find(name);
        if (varying == varyings.end()) {
            report(line, "reads " + quoted(name) + ", which the vertex shader does not write");
            return {};
        }
        if (varying->second.type != declaration.type) {
            report(
                line,
                "reads " + quoted(name) + " as " + std::string(declaration.type) + ", but the vertex shader writes "
                    + varying->second.type);
            return {};
        }
        return located(varying->second.location, declaration, "in");
    }

    std::string color_output(const InterfaceDeclaration& declaration) {
        const std::string_view name = declaration.names.front();
        const std::size_t line = tokens_[declaration.first_token].line;
        ++color_outputs_;
        if (color_outputs_ > 1) {
            report(line, "writes " + quoted(name) + " besides the one color output of the pass");
            return {};
        }
        if (declaration.type != color_type) {
            report(line, "declares the color output " + quoted(name) + " as " + std::string(declaration.type));
            return {};
        }
        return located(0, declaration, "out");
    }

    void report_missing_color_output() {
        if (stage_ == Stage::fragment && color_outputs_ == 0) {
            problems_.push_back(source_.files.front() + ": the fragment shader writes no color output");
        }
    }

    void rename_built_ins() {
        for (std::size_t index = 0; index < tokens_.size(); ++index) {
            const auto rename = std::ranges::find(built_in_renames, tokens_[index].text, &BuiltInRename::legacy);
            if (tokens_[index].kind == TokenKind::identifier && rename != built_in_renames.end()) {
                replacements_.push_back(Replacement{index, index, std::string(rename->current)});
            }
        }
    }

    bool is_used(const InterfaceDeclaration& declaration) const noexcept {
        const std::string_view name = declaration.names.front();
        for (std::size_t index = 0; index < tokens_.size(); ++index) {
            const bool is_declaration = index >= declaration.first_token && index <= declaration.last_token;
            if (!is_declaration && tokens_[index].kind == TokenKind::identifier && tokens_[index].text == name) {
                return true;
            }
        }
        return false;
    }

    static std::string located(
        std::uint32_t location, const InterfaceDeclaration& declaration, std::string_view direction) {
        std::string text = "layout(location = " + std::to_string(location) + ") ";
        if (!declaration.interpolation.empty()) {
            text += std::string(declaration.interpolation) + " ";
        }
        return text + std::string(direction) + " " + std::string(declaration.type) + " "
            + std::string(declaration.names.front()) + ";";
    }

    std::string rewritten() {
        replacements_.push_back(Replacement{0, 0, prelude()});
        std::ranges::sort(replacements_, {}, &Replacement::first_token);
        const std::string_view source = source_.text;
        std::string text;
        std::size_t copied = 0;
        for (const Replacement& replacement : replacements_) {
            const std::size_t begin = offset_of(tokens_[replacement.first_token]);
            const Token& last = tokens_[replacement.last_token];
            const std::size_t end = offset_of(last) + last.text.size();
            if (begin < copied) {
                continue;
            }
            text.append(source.substr(copied, begin - copied));
            text.append(replacement.text);
            text.append(static_cast<std::size_t>(std::ranges::count(source.substr(begin, end - begin), '\n')), '\n');
            copied = end;
        }
        text.append(source.substr(copied));
        return text;
    }

    std::string prelude() const {
        const SourceOrigin following = lines_.origin_of(tokens_.front().line + 1);
        std::string text(adapted_version);
        text += '\n';
        text += bindings_.constant_blocks;
        text += defines_;
        text += "#line " + std::to_string(following.line) + " " + std::to_string(following.file);
        return text;
    }

    std::size_t offset_of(const Token& token) const noexcept {
        return static_cast<std::size_t>(token.text.data() - source_.text.data());
    }

    void report(std::size_t line, const std::string& problem) {
        problems_.push_back(describe(lines_.origin_of(line), source_.files) + ": " + problem);
    }

    const CoreShaderBindings& bindings_;
    Stage stage_;
    const ExpandedSource& source_;
    std::vector<std::string>& problems_;
    std::vector<Token> tokens_;
    LineMap lines_;
    std::vector<Replacement> replacements_;
    std::string defines_;
    std::size_t color_outputs_ = 0;
};

}

Adaptation adapt_core_shader(
    const CoreShaderBindings& bindings, const ExpandedSource& vertex, const ExpandedSource& fragment) {
    Adaptation adaptation;
    Varyings varyings;
    std::string adapted_vertex = StageAdapter(bindings, Stage::vertex, vertex, adaptation.problems).adapt(varyings);
    std::string adapted_fragment =
        StageAdapter(bindings, Stage::fragment, fragment, adaptation.problems).adapt(varyings);
    if (adaptation.problems.empty()) {
        adaptation.program = AdaptedProgram{std::move(adapted_vertex), std::move(adapted_fragment)};
    }
    return adaptation;
}

}
