#pragma once

#include <cstddef>
#include <deque>
#include <mutex>
#include <optional>
#include <stdexcept>
#include <utility>

namespace minemod::renderer {

template <typename Job>
class BoundedQueue {
public:
    explicit BoundedQueue(std::size_t capacity) : capacity_(capacity) {
        if (capacity == 0) {
            throw std::invalid_argument("a job queue needs room for at least one job");
        }
    }

    bool try_push(Job job) {
        const std::scoped_lock lock(mutex_);
        if (jobs_.size() == capacity_) {
            return false;
        }
        jobs_.push_back(std::move(job));
        return true;
    }

    std::optional<Job> try_pop() {
        const std::scoped_lock lock(mutex_);
        if (jobs_.empty()) {
            return std::nullopt;
        }
        std::optional<Job> job(std::move(jobs_.front()));
        jobs_.pop_front();
        return job;
    }

    template <typename IsStale>
    std::size_t discard_if(IsStale is_stale) {
        const std::scoped_lock lock(mutex_);
        return std::erase_if(jobs_, is_stale);
    }

    std::size_t size() const {
        const std::scoped_lock lock(mutex_);
        return jobs_.size();
    }

    std::size_t capacity() const noexcept {
        return capacity_;
    }

private:
    std::size_t capacity_;
    mutable std::mutex mutex_;
    std::deque<Job> jobs_;
};

}
