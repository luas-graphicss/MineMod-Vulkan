#pragma once

#include <array>
#include <cstddef>
#include <cstdint>

namespace minemod::renderer {

enum class SectionFace : std::uint8_t { west, east, down, up, north, south };

inline constexpr std::array section_faces{
    SectionFace::west,
    SectionFace::east,
    SectionFace::down,
    SectionFace::up,
    SectionFace::north,
    SectionFace::south,
};

inline constexpr std::size_t section_face_count = section_faces.size();

constexpr std::size_t face_index(SectionFace face) noexcept {
    return static_cast<std::size_t>(face);
}

class FaceConnections {
public:
    static constexpr FaceConnections open() noexcept {
        FaceConnections connections;
        connections.pairs_ = every_pair;
        return connections;
    }

    constexpr void connect(SectionFace first, SectionFace second) noexcept {
        pairs_ |= pair_bit(first, second) | pair_bit(second, first);
    }

    constexpr bool connects(SectionFace first, SectionFace second) const noexcept {
        return (pairs_ & pair_bit(first, second)) != 0;
    }

    friend constexpr bool operator==(const FaceConnections&, const FaceConnections&) = default;

private:
    static constexpr std::uint64_t every_pair = (std::uint64_t{1} << (section_face_count * section_face_count)) - 1;

    static constexpr std::uint64_t pair_bit(SectionFace first, SectionFace second) noexcept {
        return std::uint64_t{1} << (face_index(first) * section_face_count + face_index(second));
    }

    std::uint64_t pairs_ = 0;
};

}
