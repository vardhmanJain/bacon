package org.jboss.pnc.bacon.pig.impl.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsSourceTreeSymlinkValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void passesWhenSymlinksResolveInsideTree() throws Exception {
        Path root = tempDir.resolve("src-root");
        Files.createDirectories(root.resolve("pkg"));
        Files.writeString(root.resolve("pkg/AGENTS.md"), "x");
        Files.createSymbolicLink(root.resolve("pkg/CLAUDE.md"), Path.of("AGENTS.md"));

        FileUtils.validateSourceTreeSymlinks(root);
    }

    @Test
    void failsWhenSymlinkTargetMissing() throws Exception {
        Path root = tempDir.resolve("src-root");
        Files.createDirectories(root);
        Files.createSymbolicLink(root.resolve("link.md"), Path.of("missing.md"));

        assertThatThrownBy(() -> FileUtils.validateSourceTreeSymlinks(root))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("symlink validation failed")
                .hasMessageContaining("Broken symlink");
    }

    @Test
    void failsWhenSymlinkPointsOutsideTree() throws Exception {
        Path root = tempDir.resolve("src-root");
        Files.createDirectories(root.resolve("nested"));
        Path outside = tempDir.resolve("outside-secret.txt");
        Files.writeString(outside, "secret");
        Files.createSymbolicLink(root.resolve("nested/leak"), Path.of("../../outside-secret.txt"));

        assertThatThrownBy(() -> FileUtils.validateSourceTreeSymlinks(root))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("escapes sources root");
    }

    @Test
    void notADirectoryThrowsIllegalArgumentException() {
        Path file = tempDir.resolve("not-a-dir");
        assertThatThrownBy(() -> FileUtils.validateSourceTreeSymlinks(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a directory");
    }
}
