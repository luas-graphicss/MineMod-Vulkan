package io.github.luasgraphicss.minemod.verification.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

public final class VerificationCli {
    static final int EXIT_OK = 0;
    static final int EXIT_VIOLATIONS = 1;
    static final int EXIT_USAGE = 2;
    static final int EXIT_FAILURE = 3;

    private static final String USAGE = """
            usage:
              verification check --profile <name> [--root <directory>]
              verification targets refresh [--root <directory>]
              verification build-matrix --manifest <file> [--root <directory>]
              verification hooks refresh [--cache <directory>] [--root <directory>]
              verification adapters refresh [--cache <directory>] [--root <directory>]
              verification evidence refresh [--root <directory>]
              verification scenario <name> [--cache <directory>] [--report <file>] [--root <directory>]
              verification compare --suite <name> [--root <directory>]
              verification capabilities [--context <compatibility|core>] [--root <directory>]
            """;

    private VerificationCli() {}

    public static void main(String[] arguments) {
        System.exit(run(arguments, System.out, System.err));
    }

    static int run(String[] arguments, PrintStream output, PrintStream errors) {
        try {
            Invocation invocation = Invocation.parse(arguments);
            Path repositoryRoot = Path.of(invocation.option("root").orElse("")).toAbsolutePath().normalize();
            List<String> command = invocation.command();
            if (command.size() == 2 && command.get(0).equals(ScenarioCommand.COMMAND)) {
                return ScenarioCommand.run(invocation, command.get(1), repositoryRoot, output);
            }
            return switch (invocation.commandLine()) {
                case "check" -> CheckCommand.run(invocation, repositoryRoot, output);
                case "targets refresh" -> RefreshTargetsCommand.run(invocation, repositoryRoot, output);
                case "hooks refresh" -> RefreshHooksCommand.run(invocation, repositoryRoot, output);
                case "adapters refresh" -> RefreshAdaptersCommand.run(invocation, repositoryRoot, output);
                case "evidence refresh" -> RefreshEvidenceCommand.run(invocation, repositoryRoot, output);
                case "build-matrix" -> BuildMatrixCommand.run(invocation, repositoryRoot, output);
                case "compare" -> CompareCommand.run(invocation, output);
                case "capabilities" -> CapabilitiesCommand.run(invocation, output);
                default -> throw new UsageException("unknown command: " + invocation.commandLine());
            };
        } catch (UsageException exception) {
            errors.println(exception.getMessage());
            errors.print(USAGE);
            return EXIT_USAGE;
        } catch (IOException exception) {
            errors.println("verification failed: " + exception.getMessage());
            return EXIT_FAILURE;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            errors.println("verification interrupted");
            return EXIT_FAILURE;
        }
    }
}
