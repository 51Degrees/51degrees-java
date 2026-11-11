/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.common.testhelpers;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Every 51Degrees environment variable name in this repository starts with an
 * underscore, because a POSIX shell cannot set a name that starts with a
 * digit. The scan reads every file git tracks, which leaves out build output
 * and the submodules, and fails on any name that starts with the digit.
 */
public class EnvironmentVariableNamesTest {

    /**
     * The start of a name a shell cannot set, built in two parts so this file
     * does not match itself.
     */
    private static final String DIGIT_INITIAL = "5" + "1DEGREES_";

    /**
     * The repository root, being the first folder at or above the working
     * directory that holds a .git entry.
     */
    private static File repositoryRoot() {
        File folder = new File("").getAbsoluteFile();
        while (folder != null) {
            if (new File(folder, ".git").exists()) {
                return folder;
            }
            folder = folder.getParentFile();
        }
        throw new IllegalStateException(
            "No folder at or above " + new File("").getAbsolutePath() +
                " holds a .git entry, so the repository root is unknown.");
    }

    /**
     * The paths git tracks, relative to the root, read as bytes so that a
     * name with a character outside the platform encoding survives.
     */
    private static List<String> trackedFiles(File root) throws Exception {
        Process git = new ProcessBuilder("git", "ls-files", "-z")
            .directory(root)
            .redirectErrorStream(true)
            .start();
        byte[] output;
        InputStream stream = git.getInputStream();
        try {
            output = readAll(stream);
        } finally {
            stream.close();
        }
        int status = git.waitFor();
        assertEquals("git ls-files failed: " +
            new String(output, Charset.forName("UTF-8")), 0, status);
        List<String> paths = new ArrayList<String>();
        int start = 0;
        for (int at = 0; at <= output.length; at++) {
            if (at == output.length || output[at] == 0) {
                if (at > start) {
                    paths.add(new String(output, start, at - start,
                        Charset.forName("UTF-8")));
                }
                start = at + 1;
            }
        }
        return paths;
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            collected.write(buffer, 0, read);
        }
        return collected.toByteArray();
    }

    /**
     * True for a byte that can be part of an environment variable name.
     */
    private static boolean isNameByte(byte value) {
        return (value >= 'a' && value <= 'z')
            || (value >= 'A' && value <= 'Z')
            || (value >= '0' && value <= '9')
            || value == '_';
    }

    private static boolean startsWithDigitInitial(byte[] line, int at) {
        byte[] needle = DIGIT_INITIAL.getBytes(Charset.forName("UTF-8"));
        if (at + needle.length > line.length) {
            return false;
        }
        for (int index = 0; index < needle.length; index++) {
            if (line[at + index] != needle[index]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Each name in the line that starts with the digit form and is not the
     * end of a longer name, such as one with a leading underscore.
     */
    static List<String> digitInitialNames(byte[] line) {
        List<String> names = new ArrayList<String>();
        int at = 0;
        while (at < line.length) {
            if (startsWithDigitInitial(line, at)
                && (at == 0 || !isNameByte(line[at - 1]))) {
                int end = at;
                while (end < line.length && isNameByte(line[end])) {
                    end++;
                }
                names.add(new String(line, at, end - at,
                    Charset.forName("UTF-8")));
                at = end;
            } else {
                at++;
            }
        }
        return names;
    }

    private static List<byte[]> lines(byte[] content) {
        List<byte[]> lines = new ArrayList<byte[]>();
        int start = 0;
        for (int at = 0; at <= content.length; at++) {
            if (at == content.length || content[at] == '\n') {
                lines.add(Arrays.copyOfRange(content, start, at));
                start = at + 1;
            }
        }
        return lines;
    }

    @Test
    public void theNameMatcherFindsOnlyNamesThatStartWithTheDigit() {
        String digitInitial = DIGIT_INITIAL + "DD_PATH";
        String line = "export " + digitInitial + "=a _" + digitInitial +
            "=b X" + digitInitial;
        assertEquals(Collections.singletonList(digitInitial),
            digitInitialNames(line.getBytes(Charset.forName("UTF-8"))));
    }

    @Test
    public void noFileNamesAVariableThatStartsWithADigit() throws Exception {
        File root = repositoryRoot();
        List<String> found = new ArrayList<String>();
        for (String relative : trackedFiles(root)) {
            File file = new File(root, relative);
            // A submodule is tracked as a directory, and its files belong to
            // another repository.
            if (!file.isFile()) {
                continue;
            }
            byte[] content = readFile(file);
            List<byte[]> lines = lines(content);
            for (int index = 0; index < lines.size(); index++) {
                for (String name : digitInitialNames(lines.get(index))) {
                    found.add(relative + ":" + (index + 1) + ": " + name);
                }
            }
        }
        StringBuilder message = new StringBuilder();
        message.append(found.size());
        message.append(" names start with a digit, which a POSIX shell ");
        message.append("cannot set, so each must start with an underscore:");
        for (String line : found) {
            message.append("\n").append(line);
        }
        assertTrue(message.toString(), found.isEmpty());
    }

    private static byte[] readFile(File file) throws IOException {
        InputStream stream = new java.io.FileInputStream(file);
        try {
            return readAll(stream);
        } finally {
            stream.close();
        }
    }
}
