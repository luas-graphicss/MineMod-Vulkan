#include "process/child_process.hpp"

#include <algorithm>
#include <array>
#include <cerrno>
#include <climits>
#include <system_error>
#include <utility>
#include <vector>

#include <fcntl.h>
#include <poll.h>
#include <signal.h>
#include <spawn.h>
#include <sys/resource.h>
#include <sys/wait.h>
#include <unistd.h>

namespace minemod::shaders {

namespace {

using Clock = std::chrono::steady_clock;

std::string system_message(int error) {
    return std::system_category().message(error);
}

class FileDescriptor {
public:
    explicit FileDescriptor(int descriptor) noexcept : descriptor_(descriptor) {}

    FileDescriptor(const FileDescriptor&) = delete;
    FileDescriptor& operator=(const FileDescriptor&) = delete;

    ~FileDescriptor() {
        close();
    }

    int get() const noexcept {
        return descriptor_;
    }

    void close() noexcept {
        if (descriptor_ >= 0) {
            ::close(descriptor_);
            descriptor_ = -1;
        }
    }

private:
    int descriptor_;
};

class SpawnActions {
public:
    SpawnActions() {
        require(posix_spawn_file_actions_init(&actions_));
    }

    SpawnActions(const SpawnActions&) = delete;
    SpawnActions& operator=(const SpawnActions&) = delete;

    ~SpawnActions() {
        posix_spawn_file_actions_destroy(&actions_);
    }

    void open_as(int target, const char* path, int flags) {
        require(posix_spawn_file_actions_addopen(&actions_, target, path, flags, 0));
    }

    void duplicate_as(int source, int target) {
        require(posix_spawn_file_actions_adddup2(&actions_, source, target));
    }

    const posix_spawn_file_actions_t* get() const noexcept {
        return &actions_;
    }

private:
    static void require(int error) {
        if (error != 0) {
            throw ProcessError("preparing the child process failed: " + system_message(error));
        }
    }

    posix_spawn_file_actions_t actions_;
};

class Child {
public:
    explicit Child(pid_t process) noexcept : process_(process) {}

    Child(const Child&) = delete;
    Child& operator=(const Child&) = delete;

    ~Child() {
        if (!is_reaped_) {
            stop();
            wait_for_exit();
        }
    }

    void limit(decltype(RLIMIT_CPU) resource, std::uint64_t amount) const {
        const rlimit limit{static_cast<rlim_t>(amount), static_cast<rlim_t>(amount)};
        if (::prlimit(process_, resource, &limit, nullptr) != 0) {
            throw ProcessError("limiting the child process failed: " + system_message(errno));
        }
    }

    void stop() const noexcept {
        ::kill(process_, SIGKILL);
    }

    int exit_status(Clock::time_point deadline, bool& timed_out) {
        while (true) {
            int status = 0;
            const pid_t reaped = ::waitpid(process_, &status, WNOHANG);
            if (reaped == process_) {
                is_reaped_ = true;
                return status;
            }
            if (reaped < 0 && errno != EINTR) {
                throw ProcessError("waiting for the child process failed: " + system_message(errno));
            }
            if (Clock::now() >= deadline) {
                timed_out = true;
                stop();
                return wait_for_exit();
            }
            ::poll(nullptr, 0, 1);
        }
    }

private:
    int wait_for_exit() noexcept {
        int status = 0;
        while (::waitpid(process_, &status, 0) < 0 && errno == EINTR) {
        }
        is_reaped_ = true;
        return status;
    }

    pid_t process_;
    bool is_reaped_ = false;
};

std::string read_until(int descriptor, Clock::time_point deadline, std::size_t capacity, bool& timed_out) {
    std::string captured;
    std::array<char, 4096> chunk{};
    while (true) {
        const auto remaining = std::chrono::duration_cast<std::chrono::milliseconds>(deadline - Clock::now());
        if (remaining.count() <= 0) {
            timed_out = true;
            return captured;
        }
        pollfd waited{descriptor, POLLIN, 0};
        const int ready =
            ::poll(&waited, 1, static_cast<int>(std::min<std::chrono::milliseconds::rep>(remaining.count(), INT_MAX)));
        if (ready == 0) {
            timed_out = true;
            return captured;
        }
        const ssize_t count = ready < 0 ? ready : ::read(descriptor, chunk.data(), chunk.size());
        if (count == 0) {
            return captured;
        }
        if (count < 0 && errno != EINTR) {
            throw ProcessError("reading the child process output failed: " + system_message(errno));
        }
        if (count > 0) {
            const std::size_t kept = std::min(static_cast<std::size_t>(count), capacity - captured.size());
            captured.append(chunk.data(), kept);
        }
    }
}

pid_t spawn(const std::filesystem::path& executable, std::span<const std::string> arguments, int output) {
    SpawnActions actions;
    actions.open_as(STDIN_FILENO, "/dev/null", O_RDONLY);
    actions.duplicate_as(output, STDOUT_FILENO);
    actions.duplicate_as(output, STDERR_FILENO);

    std::vector<std::string> words{executable.string()};
    words.insert(words.end(), arguments.begin(), arguments.end());
    std::vector<char*> argument_pointers;
    for (std::string& word : words) {
        argument_pointers.push_back(word.data());
    }
    argument_pointers.push_back(nullptr);
    std::array<char*, 1> environment{nullptr};

    pid_t process = -1;
    const int error = ::posix_spawn(
        &process, words.front().c_str(), actions.get(), nullptr, argument_pointers.data(), environment.data());
    if (error != 0) {
        throw ProcessError("starting " + executable.filename().string() + " failed: " + system_message(error));
    }
    return process;
}

}

ProcessResult run_process(
    const std::filesystem::path& executable, std::span<const std::string> arguments, const ProcessLimits& limits) {
    std::array<int, 2> pipe_ends{};
    if (::pipe2(pipe_ends.data(), O_CLOEXEC) != 0) {
        throw ProcessError("opening the child process output failed: " + system_message(errno));
    }
    FileDescriptor output(pipe_ends[0]);
    FileDescriptor child_output(pipe_ends[1]);

    Child child(spawn(executable, arguments, child_output.get()));
    child_output.close();
    child.limit(RLIMIT_CPU, static_cast<std::uint64_t>(limits.cpu_time.count()));
    child.limit(RLIMIT_AS, limits.address_space_bytes);
    child.limit(RLIMIT_FSIZE, limits.written_file_bytes);
    child.limit(RLIMIT_CORE, 0);

    const Clock::time_point deadline = Clock::now() + limits.wall_time;
    bool timed_out = false;
    std::string captured = read_until(output.get(), deadline, limits.captured_output_bytes, timed_out);
    if (timed_out) {
        child.stop();
    }
    const int status = child.exit_status(deadline, timed_out);
    if (timed_out) {
        return ProcessResult{ProcessEnd::timed_out, 0, std::move(captured)};
    }
    if (WIFEXITED(status)) {
        return ProcessResult{ProcessEnd::exited, WEXITSTATUS(status), std::move(captured)};
    }
    return ProcessResult{ProcessEnd::stopped_by_signal, WIFSIGNALED(status) ? WTERMSIG(status) : 0, std::move(captured)};
}

}
