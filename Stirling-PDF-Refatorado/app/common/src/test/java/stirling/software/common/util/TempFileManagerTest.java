package stirling.software.common.util;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import stirling.software.common.model.ApplicationProperties;

@DisplayName("TempFileManager Tests")
class TempFileManagerTest {

    private TempFileManager newManagerWithDefaults() {
        ApplicationProperties props = new ApplicationProperties();
        // default system.tempFileManagement will be used
        TempFileRegistry registry = spy(new TempFileRegistry());
        return new TempFileManager(registry, props);
    }

    @Test
    @DisplayName("createTempFile creates file in default location and register/unregister works")
    void createAndDeleteTempFile() throws IOException {
        TempFileManager mgr = newManagerWithDefaults();
        File f = mgr.createTempFile(".pdf");
        assertThat(f).exists();
        assertThat(mgr.deleteTempFile(f)).isTrue();
        assertThat(f.exists()).isFalse();
    }

    @Test
    @DisplayName("deleteTempFile(Path) works for existent and non-existent paths")
    void deleteByPath() throws IOException {
        TempFileManager mgr = newManagerWithDefaults();
        File f = mgr.createTempFile(".dat");
        Path p = f.toPath();
        assertThat(mgr.deleteTempFile(p)).isTrue();
        assertThat(mgr.deleteTempFile(p)).isFalse();
    }

    @Test
    @DisplayName("createTempDirectory and deleteTempDirectory remove nested content")
    void dirLifecycle() throws IOException {
        TempFileManager mgr = newManagerWithDefaults();
        Path dir = mgr.createTempDirectory();
        assertThat(dir).exists();
        Files.createDirectories(dir.resolve("a/b"));
        Files.writeString(dir.resolve("a/b/file.txt"), "x");
        mgr.deleteTempDirectory(dir);
        assertThat(dir).doesNotExist();
    }

    @Test
    @DisplayName("generateTempFileName prefixes and extension formatting")
    void generateName() {
        TempFileManager mgr = newManagerWithDefaults();
        String name = mgr.generateTempFileName("test", "bin");
        assertThat(name).contains("stirling-pdf");
        assertThat(name).contains("test-");
        assertThat(name).endsWith(".bin");
    }

    @Test
    @DisplayName("cleanupOldTempFiles removes files older than threshold via registry")
    void cleanupOldFiles() throws IOException {
        ApplicationProperties props = new ApplicationProperties();
        TempFileRegistry registry = spy(new TempFileRegistry());
        TempFileManager mgr = new TempFileManager(registry, props);

        // create two files and mark one as old via registry spying
        File f1 = mgr.createTempFile(".pdf");
        File f2 = mgr.createTempFile(".pdf");

        // Simulate registry returning one old file
        doReturn(Set.of(f1.toPath())).when(registry).getFilesOlderThan(anyLong());

        int deleted = mgr.cleanupOldTempFiles(Duration.ofMillis(1).toMillis());
        assertThat(deleted).isEqualTo(1);
        assertThat(f1.exists()).isFalse();
        assertThat(f2.exists()).isTrue();

        // cleanup remaining
        mgr.deleteTempFile(f2);
    }
}
