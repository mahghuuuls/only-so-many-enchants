package com.mahghuuuls.onlysomanyenchants;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {

    private static final String ROOT_PKG = "com.mahghuuuls.onlysomanyenchants";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.*]+)\\s*;");

    @Test
    void productionSourcesRespectPackageAndSideBoundaries() throws IOException {
        Path sourceRoot = Paths.get("src", "main", "java", "com", "mahghuuuls", "onlysomanyenchants");
        assertTrue(Files.isDirectory(sourceRoot), "Expected production source root at " + sourceRoot.toAbsolutePath());

        List<String> violations = new ArrayList<String>();
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            walk.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    checkFile(sourceRoot, path, violations);
                } catch (IOException ex) {
                    violations.add(path + ": failed to read (" + ex.getMessage() + ")");
                }
            });
        }

        assertTrue(
                violations.isEmpty(),
                "Architecture boundary violations detected:\n - " + String.join("\n - ", violations)
        );
    }

    private static void checkFile(Path sourceRoot, Path file, List<String> violations) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String relPath = sourceRoot.relativize(file).toString().replace('\\', '/');
        String pkg = "";
        List<String> imports = new ArrayList<String>();

        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = lines.get(lineIdx);
            Matcher pkgMatcher = PACKAGE_PATTERN.matcher(line);
            if (pkgMatcher.find()) {
                pkg = pkgMatcher.group(1);
            }
            Matcher impMatcher = IMPORT_PATTERN.matcher(line);
            if (impMatcher.find()) {
                imports.add(impMatcher.group(1));
            }
            if (line.contains("System.out.print") || line.contains("System.err.print")) {
                violations.add(relPath + ":" + (lineIdx + 1) + " uses System.out/err instead of Mod Logger");
            }
        }

        boolean isClientFile = pkg.equals(ROOT_PKG + ".client")
                || pkg.startsWith(ROOT_PKG + ".client.")
                || pkg.equals(ROOT_PKG + ".mixin.client")
                || pkg.startsWith(ROOT_PKG + ".mixin.client.")
                || relPath.equals("ClientProxy.java")
                || relPath.endsWith("MixinEasyMagicGuiHints.java");

        if (!isClientFile) {
            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                String trimmed = lines.get(lineIdx).trim();
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
                    continue;
                }
                if (trimmed.contains("net.minecraft.client.") || trimmed.contains("net.minecraftforge.client.")) {
                    violations.add(relPath + ":" + (lineIdx + 1) + " references client-only package directly");
                }
            }
        }

        for (String imp : imports) {
            if (imp.startsWith("com.mahghuuuls.agenttesttoolkit")) {
                violations.add(relPath + ": production source must never import Agent Test Toolkit (" + imp + ")");
            }

            if (!isClientFile) {
                if (imp.startsWith("net.minecraft.client.")
                        || imp.startsWith("net.minecraftforge.client.")
                        || imp.equals(ROOT_PKG + ".ClientProxy")
                        || imp.equals(ROOT_PKG + ".client")
                        || imp.startsWith(ROOT_PKG + ".client.")
                        || imp.startsWith(ROOT_PKG + ".mixin.client.")) {
                    violations.add(relPath + ": common/server source imports client class (" + imp + ")");
                }
            }

            if (!pkg.equals(ROOT_PKG + ".compat.easymagic")
                    && (imp.startsWith(ROOT_PKG + ".compat.easymagic.") || imp.startsWith("com.xy.easymagic."))) {
                violations.add(relPath + ": non-easymagic class imports EasyMagic compat (" + imp + ")");
            }

            if (!pkg.equals(ROOT_PKG + ".compat.ssn")
                    && (imp.startsWith(ROOT_PKG + ".compat.ssn.") || imp.startsWith("mrriegel.storagenetwork."))) {
                violations.add(relPath + ": non-ssn class imports Simple Storage Network compat (" + imp + ")");
            }

            if (pkg.equals(ROOT_PKG + ".config")) {
                if (imp.startsWith(ROOT_PKG + ".")
                        && !imp.equals(ROOT_PKG + ".Tags")
                        && !imp.startsWith(ROOT_PKG + ".config.")) {
                    violations.add(relPath + ": config package has forbidden internal dependency (" + imp + ")");
                }
            }

            if (pkg.equals(ROOT_PKG + ".core")) {
                if (imp.startsWith(ROOT_PKG + ".")
                        && !imp.equals(ROOT_PKG + ".Tags")
                        && !imp.startsWith(ROOT_PKG + ".config.")
                        && !imp.startsWith(ROOT_PKG + ".core.")
                        && !imp.startsWith(ROOT_PKG + ".diag.")) {
                    violations.add(relPath + ": core package has forbidden internal dependency (" + imp + ")");
                }
            }

            if (pkg.equals(ROOT_PKG + ".diag")) {
                if (imp.startsWith(ROOT_PKG + ".")
                        && !imp.equals(ROOT_PKG + ".Tags")
                        && !imp.startsWith(ROOT_PKG + ".config.")
                        && !imp.startsWith(ROOT_PKG + ".core.")
                        && !imp.startsWith(ROOT_PKG + ".diag.")) {
                    violations.add(relPath + ": diag package has forbidden internal dependency (" + imp + ")");
                }
            }

            if (pkg.equals(ROOT_PKG + ".network")) {
                if (imp.startsWith(ROOT_PKG + ".")
                        && !imp.equals(ROOT_PKG + ".Tags")
                        && !imp.equals(ROOT_PKG + ".OnlySoManyEnchantsMod")
                        && !imp.equals(ROOT_PKG + ".CommonProxy")
                        && !imp.startsWith(ROOT_PKG + ".config.")
                        && !imp.startsWith(ROOT_PKG + ".core.")
                        && !imp.startsWith(ROOT_PKG + ".diag.")
                        && !imp.startsWith(ROOT_PKG + ".network.")) {
                    violations.add(relPath + ": network package has forbidden internal dependency (" + imp + ")");
                }
            }

            if (pkg.equals(ROOT_PKG + ".event") || pkg.equals(ROOT_PKG + ".mixin")) {
                if (imp.startsWith(ROOT_PKG + ".")
                        && !imp.equals(ROOT_PKG + ".Tags")
                        && !imp.equals(ROOT_PKG + ".OnlySoManyEnchantsMod")
                        && !imp.equals(ROOT_PKG + ".compat.PlayerExtraSlotAdapters")
                        && !imp.startsWith(ROOT_PKG + ".config.")
                        && !imp.startsWith(ROOT_PKG + ".core.")
                        && !imp.startsWith(ROOT_PKG + ".diag.")
                        && !imp.startsWith(ROOT_PKG + ".network.")
                        && !imp.startsWith(pkg + ".")) {
                    violations.add(relPath + ": " + pkg + " package has forbidden internal dependency (" + imp + ")");
                }
            }
        }
    }
}
