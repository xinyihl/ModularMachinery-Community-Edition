package hellfirepvp.modularmachinery.common.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtils {
    public static final int KB = 1024;
    public static final int MB = 1024 * 1024;

    public static String readFile(File file) throws IOException {
        InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
        char[] chars = new char[formatBufferSize(file.length())];
        StringBuilder sb = new StringBuilder((int) file.length());
        int length;
        while ((length = reader.read(chars)) != -1) {
            sb.append(chars, 0, length);
        }

        return sb.toString();
    }

    public static int formatBufferSize(long size) {
        if (size <= KB) {
            return (int) size;
        } else if (size <= MB) {
            return KB * 8;
        } else if (size <= MB * 128) {
            return KB * 32;
        } else {
            return KB * 128;
        }
    }
}
