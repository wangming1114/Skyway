package com.skyway.web.service.ws;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WsLegoTest {
    @TempDir Path dir;

    @Test
    void explicitMissingToolReturnsAnActionableErrorWithoutInstalling() {
        Path tools = dir.resolve("tools");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WsLego.resolve(dir.resolve("missing").toString(), tools));
        assertTrue(error.getMessage().contains("skyway.ws.lego"));
        assertFalse(Files.exists(tools));
    }

    @Test
    void verifiedArchiveProducesAnExecutableAndCorruptDownloadPreservesIt() throws Exception {
        Path source = Files.createDirectory(dir.resolve("fixture"));
        byte[] original = "test signer".getBytes(StandardCharsets.US_ASCII);
        Files.write(source.resolve("lego"), original);
        Path archive = dir.resolve("release.tgz");
        Process tar = new ProcessBuilder("tar", "-czf", archive.toString(), "-C", source.toString(), "lego").start();
        assertTrue(tar.waitFor(15, TimeUnit.SECONDS));
        assertEquals(0, tar.exitValue());
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(archive));
        String checksum = String.format(Locale.ROOT, "%064x", new BigInteger(1, digest));
        Path tool = dir.resolve("installed-lego");
        WsLego.install(archive.toUri().toURL(), checksum, tool, false);
        assertTrue(Files.isExecutable(tool));
        assertArrayEquals(original, Files.readAllBytes(tool));
        assertEquals(tool, WsLego.find("installed-lego", dir.toString(), false));
        assertEquals(tool.toString(), WsLego.resolve(tool.toString(), dir));

        Files.write(archive, new byte[]{0});
        IOException failure = assertThrows(IOException.class,
                () -> WsLego.install(archive.toUri().toURL(), checksum, tool, false));
        assertTrue(failure.getMessage().contains("SHA-256"));
        assertArrayEquals(original, Files.readAllBytes(tool));
    }
}
