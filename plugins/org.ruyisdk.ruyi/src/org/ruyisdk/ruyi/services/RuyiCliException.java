package org.ruyisdk.ruyi.services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.ruyisdk.core.exception.PluginException;

/**
 * Exception thrown when a Ruyi CLI operation fails.
 *
 * <p>
 * This is a sealed hierarchy: each permitted subclass represents one specific failure mode and
 * carries only the structured fields meaningful to that mode. Callers should branch with
 * {@code instanceof} pattern matching (or a {@code switch} on the sealed type) rather than parsing
 * {@link #getMessage()}.
 * </p>
 */
public abstract sealed class RuyiCliException extends PluginException
        permits RuyiCliException.NotFound, RuyiCliException.ExecutionFailed,
        RuyiCliException.InvalidArgument, RuyiCliException.Timeout, RuyiCliException.Cancelled,
        RuyiCliException.IoError, RuyiCliException.ExecutionError,
        RuyiCliException.TerminalUnavailable, RuyiCliException.UnsupportedVersion {
    private static final long serialVersionUID = 1L;

    private RuyiCliException(String message) {
        super(message);
    }

    private RuyiCliException(String message, Throwable cause) {
        super(message, cause);
    }

    /** The {@code ruyi} executable could not be located. */
    public static final class NotFound extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private NotFound() {
            super("ruyi executable not found in configured or default install path");
        }
    }

    /** The CLI ran but returned a non-zero exit code. */
    public static final class ExecutionFailed extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private final int exitCode;
        private final String commandHint;
        private final String output;

        private ExecutionFailed(String commandHint, int exitCode, String output) {
            super(String.format("""
                ruyi command execution failed with code: %d
                Command: %s
                CLI Output:
                %s""", exitCode, commandHint, output));
            this.exitCode = exitCode;
            this.commandHint = commandHint;
            this.output = output;
        }

        /** Returns the process exit code. */
        public int exitCode() {
            return exitCode;
        }

        /** Returns a human-readable rendering of the command that failed. */
        public String commandHint() {
            return commandHint;
        }

        /** Returns the captured CLI output (may be partial). */
        public String output() {
            return output;
        }
    }

    /** A caller passed an invalid argument to the CLI wrapper (programmer bug). */
    public static final class InvalidArgument extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private InvalidArgument(String message) {
            super(message);
        }
    }

    /** The CLI did not finish within the configured timeout. */
    public static final class Timeout extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private final int timeoutSeconds;

        private Timeout(int timeoutSeconds) {
            super("ruyi command timed out after " + timeoutSeconds + " second(s)");
            this.timeoutSeconds = timeoutSeconds;
        }

        /** Returns the configured timeout in seconds. */
        public int timeoutSeconds() {
            return timeoutSeconds;
        }
    }

    /** The user (or progress monitor) cancelled the operation. */
    public static final class Cancelled extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private Cancelled() {
            super("ruyi command was cancelled");
        }
    }

    /** I/O failure while spawning or reading from the process. */
    public static final class IoError extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private IoError(IOException cause) {
            super("I/O error during ruyi command execution", cause);
        }
    }

    /** Unexpected error from {@link java.util.concurrent.ExecutionException}. */
    public static final class ExecutionError extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private ExecutionError(ExecutionException e) {
            super("Unexpected error during ruyi command execution", e.getCause());
        }
    }

    /** Eclipse terminal service is unavailable. */
    public static final class TerminalUnavailable extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private TerminalUnavailable() {
            super("Eclipse terminal service is unavailable");
        }
    }

    /**   */
    public static final class UnsupportedVersion extends RuyiCliException {
        private static final long serialVersionUID = 1L;

        private final String minimumVersion;
        private final String currentVersion;

        private UnsupportedVersion(String minimumVersion) {
            super(String.format(
                    "Unable to detect installed ruyi version. Minimum required version is %s",
                    minimumVersion));
            this.minimumVersion = minimumVersion;
            this.currentVersion = null;
        }

        private UnsupportedVersion(String minimumVersion, String currentVersion) {
            super(String.format(
                    "Installed ruyi version %s is unsupported. Minimum required version is %s",
                    currentVersion, minimumVersion));
            this.minimumVersion = minimumVersion;
            this.currentVersion = currentVersion;
        }

        /** Returns the configured timeout in seconds. */
        public String minimumVersion() {
            return minimumVersion;
        }

        /** Returns the configured timeout in seconds. */
        public String currentVersion() {
            return currentVersion;
        }
    }

    /** Ruyi executable not found. */
    public static NotFound ruyiNotFound() {
        return new NotFound();
    }

    /** CLI command returned non-zero exit code. CLI output is included in the message. */
    public static ExecutionFailed executionFailed(String commandHint, int exitCode, String output) {
        return new ExecutionFailed(commandHint, exitCode, output);
    }

    /** Invalid argument provided to CLI. */
    public static InvalidArgument invalidArgument(String message) {
        return new InvalidArgument(message);
    }

    /** Operation timed out. */
    public static Timeout timeout(int timeoutSeconds) {
        return new Timeout(timeoutSeconds);
    }

    /** Operation was cancelled. */
    public static Cancelled cancelled() {
        return new Cancelled();
    }

    /** I/O error during CLI execution. */
    public static IoError ioError(IOException cause) {
        return new IoError(cause);
    }

    /** {@link ExecutionException} during CLI execution. */
    public static ExecutionError executionError(ExecutionException e) {
        return new ExecutionError(e);
    }

    /** Eclipse terminal service is not available. */
    public static TerminalUnavailable terminalUnavailable() {
        return new TerminalUnavailable();
    }

    /** Installed ruyi version is unsupported or cannot be detected. */
    public static RuyiCliException unsupportedVersion(String minimumVersion,
            String currentVersion) {
        return currentVersion == null ? new UnsupportedVersion(minimumVersion)
                : new UnsupportedVersion(minimumVersion, currentVersion);
    }
}
