#include <algorithm>
#include <cstdlib>
#include <exception>
#include <iostream>
#include <string_view>
#include <vector>

#include "cli/harness_options.hpp"
#include "cli/harness_report_text.hpp"
#include "run/harness_run.hpp"

namespace {

constexpr int usage_error_status = 2;

}

int main(int argument_count, char** arguments) {
    using namespace minemod::harness;

    const std::vector<std::string_view> option_arguments(arguments + 1, arguments + std::max(argument_count, 1));
    try {
        const HarnessReport report = run_harness(parse_harness_options(option_arguments));
        write_harness_report(std::cout, report);
        return report.validation_messages.empty() ? EXIT_SUCCESS : EXIT_FAILURE;
    } catch (const UsageError& error) {
        std::cerr << "minemod_harness: " << error.what() << '\n' << harness_usage << '\n';
        return usage_error_status;
    } catch (const std::exception& error) {
        std::cerr << "minemod_harness: " << error.what() << '\n';
        return EXIT_FAILURE;
    }
}
