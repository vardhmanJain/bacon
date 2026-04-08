package org.jboss.pnc.bacon.pig.impl.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsZipSymlinkTest {

    @TempDir
    Path tempDir;

    @Test
    void zipAndUnzipShouldPreserveSymlinkToExistingFile() throws Exception {
        Path work = tempDir.resolve("work");
        Path root = work.resolve("src-root");
        Files.createDirectories(root);
        Path target = root.resolve("AGENTS.md");
        Files.writeString(target, "hello");
        Path link = root.resolve("CLAUDE.md");
        Files.createSymbolicLink(link, Path.of("AGENTS.md"));

        File zipFile = tempDir.resolve("sources.zip").toFile();
        FileUtils.zip(zipFile, work.toFile(), root.toFile());

        try (org.apache.commons.compress.archivers.zip.ZipFile zf = new org.apache.commons.compress.archivers.zip.ZipFile(
                zipFile.toPath())) {
            ZipArchiveEntry ze = zf.getEntry("src-root/CLAUDE.md");
            assertThat(ze).isNotNull();
            assertThat(ze.isUnixSymlink()).isTrue();
        }

        Path extract = tempDir.resolve("extract");
        Files.createDirectories(extract);
        FileUtils.unzip(zipFile, extract.toFile());

        Path extractedLink = extract.resolve("src-root").resolve("CLAUDE.md");
        assertThat(Files.isSymbolicLink(extractedLink)).isTrue();
        assertThat(Files.readSymbolicLink(extractedLink)).isEqualTo(Path.of("AGENTS.md"));
        assertThat(Files.readString(extract.resolve("src-root").resolve("AGENTS.md"), StandardCharsets.UTF_8))
                .isEqualTo("hello");
    }
}
