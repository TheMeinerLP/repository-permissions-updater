package io.jenkins.infra.repository_permissions_updater.helper;

import com.google.gson.JsonArray;
import io.jenkins.infra.repository_permissions_updater.ArtifactoryAPI;
import io.jenkins.infra.repository_permissions_updater.CryptoUtil;
import io.jenkins.infra.repository_permissions_updater.GitHubAPI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ArtifactoryHelperTest {

    private static final Logger LOGGER = Logger.getLogger(ArtifactoryHelper.class.getName());

    @BeforeEach
    void setUp() {
        // Reset any static state if necessary
    }

    @Test
    void submitArtifactoryObjects_validDirectory_createsObjects() {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);
        File[] files = {new File("test1.json"), new File("test2.json")};
        when(payloadsDir.listFiles()).thenReturn(files);

        BiConsumer<String, File> creator = mock(BiConsumer.class);

        ArtifactoryHelper.submitArtifactoryObjects(payloadsDir, "group", creator);

        verify(creator).accept("test1", files[0]);
        verify(creator).accept("test2", files[1]);
    }

    @Test
    void submitArtifactoryObjects_validDirectory_createsObjects_no_dir() {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(false);
        File[] files = {new File("test1.json"), new File("test2.json")};
        when(payloadsDir.listFiles()).thenReturn(files);

        BiConsumer<String, File> creator = mock(BiConsumer.class);

        ArtifactoryHelper.submitArtifactoryObjects(payloadsDir, "group", creator);

        verifyNoInteractions(creator);
    }

    @Test
    void submitArtifactoryObjects_validDirectory_createsObjects_exception() {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);
        File[] files = {new File("test1.json"), new File("test2.json")};
        when(payloadsDir.listFiles()).thenReturn(files);

        BiConsumer<String, File> creator = mock(BiConsumer.class);
        doThrow(new RuntimeException("Test exception")).when(creator).accept(anyString(), any(File.class));

        ArtifactoryHelper.submitArtifactoryObjects(payloadsDir, "group", creator);

        verify(creator).accept("test1", files[0]);
        verify(creator).accept("test2", files[1]);
    }

    @Test
    void submitArtifactoryObjects_validDirectory_createsObjects_files_null() {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);
        when(payloadsDir.listFiles()).thenReturn(null);

        BiConsumer<String, File> creator = mock(BiConsumer.class);

        ArtifactoryHelper.submitArtifactoryObjects(payloadsDir, "group", creator);

        verifyNoInteractions(creator);
    }

    @Test
    void submitArtifactoryObjects_validDirectory_createsObjects_file_not_json() {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);

        File file = spy(new File("test1.yaml"));
        when(file.getName()).thenReturn("test1.yaml");

        File[] files = {new File("test1.json"), file};
        when(payloadsDir.listFiles()).thenReturn(files);

        BiConsumer<String, File> creator = mock(BiConsumer.class);

        ArtifactoryHelper.submitArtifactoryObjects(payloadsDir, "group", creator);

        verify(creator).accept("test1", files[0]);
    }



    @Test
    void submitArtifactoryObjects_invalidDirectory_logsWarning() {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(false);

        BiConsumer<String, File> creator = mock(BiConsumer.class);

        ArtifactoryHelper.submitArtifactoryObjects(payloadsDir, "group", creator);

        verify(creator, never()).accept(anyString(), any(File.class));
    }

    @Disabled
    @Test
    void generateTokens_validFile_generatesTokens() throws IOException {
        File githubReposForCdIndex = mock(File.class);
        JsonArray repos = new JsonArray();
        repos.add("orgname/reponame");
        try (MockedStatic<ArtifactoryAPI> artifactoryAPIMock = mockStatic(ArtifactoryAPI.class);
             MockedStatic<GitHubAPI> gitHubAPIMock = mockStatic(GitHubAPI.class);
             MockedStatic<CryptoUtil> cryptoUtilMock = mockStatic(CryptoUtil.class)) {

            ArtifactoryAPI artifactoryAPI = mock(ArtifactoryAPI.class);
            GitHubAPI gitHubAPI = mock(GitHubAPI.class);
            GitHubAPI.GitHubPublicKey publicKey = mock(GitHubAPI.GitHubPublicKey.class);

            artifactoryAPIMock.when(ArtifactoryAPI::getInstance).thenReturn(artifactoryAPI);
            gitHubAPIMock.when(GitHubAPI::getInstance).thenReturn(gitHubAPI);
            gitHubAPIMock.when(() -> gitHubAPI.getRepositoryPublicKey("orgname/reponame")).thenReturn(publicKey);
            when(publicKey.getKey()).thenReturn("publicKey");
            when(publicKey.getKeyId()).thenReturn("keyId");

            try(MockedConstruction<FileInputStream> stream = mockConstruction(FileInputStream.class)) {
                List<FileInputStream> constructed = stream.constructed();


                ArtifactoryHelper.generateTokens(githubReposForCdIndex);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }



            verify(artifactoryAPI).generateTokenForGroup(anyString(), anyString(), anyLong());
            verify(gitHubAPI).createOrUpdateRepositorySecret(anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void removeExtraArtifactoryObjects_validDirectory_removesObjects() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);

        Path path = mock(Path.class);
        when(path.resolve(anyString())).thenReturn(path);
        when(path.normalize()).thenReturn(path);
        when(path.startsWith((Path) any())).thenReturn(true);

        when(payloadsDir.toPath()).thenReturn(path);

        List<String> objects = List.of("object1", "object2");
        Supplier<List<String>> lister = mock(Supplier.class);
        when(lister.get()).thenReturn(objects);
        Consumer<String> deleter = mock(Consumer.class);

        try (MockedStatic<java.nio.file.Files> filesMock = mockStatic(java.nio.file.Files.class)) {
            filesMock.when(() -> java.nio.file.Files.notExists(any())).thenReturn(true);


            ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

            verify(deleter).accept("object1");
            verify(deleter).accept("object2");
        }
    }

    @Test
    void removeExtraArtifactoryObjects_invalidDirectory_logsWarning() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(false);

        Supplier<List<String>> lister = mock(Supplier.class);
        Consumer<String> deleter = mock(Consumer.class);

        ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

        verify(deleter, never()).accept(anyString());
    }

    @Test
    void removeExtraArtifactoryObjects_payloaddir_exists() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);

        Supplier<List<String>> lister = mock(Supplier.class);
        Consumer<String> deleter = mock(Consumer.class);

        ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

        verify(deleter, never()).accept(anyString());
    }

    @Test
    void not_removeExtraArtifactoryObjects_payloaddir_dir() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(false);
        when(payloadsDir.isDirectory()).thenReturn(false);

        Supplier<List<String>> lister = mock(Supplier.class);
        Consumer<String> deleter = mock(Consumer.class);

        ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

        verify(deleter, never()).accept(anyString());
    }

    @Test
    void removeExtraArtifactoryObjects_payloaddir_dir() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(false);
        when(payloadsDir.isDirectory()).thenReturn(true);

        Supplier<List<String>> lister = mock(Supplier.class);
        Consumer<String> deleter = mock(Consumer.class);

        ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

        verify(deleter, never()).accept(anyString());
    }


    @Test
    void removeExtraArtifactoryObjects_listener_exception() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);

        Supplier<List<String>> lister = mock(Supplier.class);
        doThrow(new RuntimeException("Test exception")).when(lister).get();
        Consumer<String> deleter = mock(Consumer.class);

        ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

        verify(deleter, never()).accept(anyString());
    }

    @Test
    void removeExtraArtifactoryObjects_payloaddir_is_dir() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(false);

        Supplier<List<String>> lister = mock(Supplier.class);
        Consumer<String> deleter = mock(Consumer.class);

        ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

        verify(deleter, never()).accept(anyString());
    }


    @Test
    void removeExtraArtifactoryObjects_not_normalized() {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);

        Path path = mock(Path.class);
        when(path.resolve(anyString())).thenReturn(path);
        when(path.normalize()).thenReturn(path);
        when(path.startsWith((Path) any())).thenReturn(false);

        when(payloadsDir.toPath()).thenReturn(path);

        Supplier<List<String>> lister = mock(Supplier.class);
        List<String> objects = List.of("object1", "object2");
        when(lister.get()).thenReturn(objects);

        Consumer<String> deleter = mock(Consumer.class);

        Assertions.assertThrowsExactly(IOException.class, () -> {
            ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);
        });

        verify(deleter, never()).accept(anyString());
    }

    @Test
    void removeExtraArtifactoryObjects_deleter_accepted() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);

        Path path = mock(Path.class);
        when(path.resolve(anyString())).thenReturn(path);
        when(path.normalize()).thenReturn(path);
        when(path.startsWith((Path) any())).thenReturn(true);

        when(payloadsDir.toPath()).thenReturn(path);

        Supplier<List<String>> lister = mock(Supplier.class);
        List<String> objects = List.of("object1", "object2");
        when(lister.get()).thenReturn(objects);

        Consumer<String> deleter = mock(Consumer.class);

        try (MockedStatic<java.nio.file.Files> filesMock = mockStatic(java.nio.file.Files.class)) {
            filesMock.when(() -> java.nio.file.Files.notExists(any())).thenReturn(true);


            ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

            verify(deleter).accept("object1");
            verify(deleter).accept("object2");
        }
    }

    @Test
    void removeExtraArtifactoryObjects_files_not_exists() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);

        Path path = mock(Path.class);
        when(path.resolve(anyString())).thenReturn(path);
        when(path.normalize()).thenReturn(path);
        when(path.startsWith((Path) any())).thenReturn(true);

        when(payloadsDir.toPath()).thenReturn(path);

        Supplier<List<String>> lister = mock(Supplier.class);
        List<String> objects = List.of("object1", "object2");
        when(lister.get()).thenReturn(objects);

        Consumer<String> deleter = mock(Consumer.class);

        try (MockedStatic<java.nio.file.Files> filesMock = mockStatic(java.nio.file.Files.class)) {
            filesMock.when(() -> java.nio.file.Files.notExists(any())).thenReturn(false);


            ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

            verify(deleter, never()).accept("object1");
            verify(deleter, never()).accept("object2");
        }
    }

    @Test
    void removeExtraArtifactoryObjects_deleter_exception() throws IOException {
        File payloadsDir = mock(File.class);
        when(payloadsDir.exists()).thenReturn(true);
        when(payloadsDir.isDirectory()).thenReturn(true);

        Path path = mock(Path.class);
        when(path.resolve(anyString())).thenReturn(path);
        when(path.normalize()).thenReturn(path);
        when(path.startsWith((Path) any())).thenReturn(true);

        when(payloadsDir.toPath()).thenReturn(path);

        Supplier<List<String>> lister = mock(Supplier.class);
        List<String> objects = List.of("object1", "object2");
        when(lister.get()).thenReturn(objects);

        Consumer<String> deleter = mock(Consumer.class);
        doThrow(new RuntimeException("Test exception")).when(deleter).accept(anyString());

        try (MockedStatic<java.nio.file.Files> filesMock = mockStatic(java.nio.file.Files.class)) {
            filesMock.when(() -> java.nio.file.Files.notExists(any())).thenReturn(true);


            ArtifactoryHelper.removeExtraArtifactoryObjects(payloadsDir, "group", lister, deleter);

            verify(deleter).accept("object1");
            verify(deleter).accept("object2");
        }
    }


}
