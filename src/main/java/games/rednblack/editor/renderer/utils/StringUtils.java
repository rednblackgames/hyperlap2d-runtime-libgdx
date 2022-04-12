package games.rednblack.editor.renderer.utils;

/**
 * Extracted StringUtils class from org.apache.commons.io.FilenameUtils
 */
public class StringUtils {
    private static final int NOT_FOUND = -1;
    private static final char UNIX_SEPARATOR = '/';
    private static final char WINDOWS_SEPARATOR = '\\';
    public static final char EXTENSION_SEPARATOR = '.';

    public static String getBaseName(String filename) {
        return removeExtension(getName(filename));
    }

    public static int indexOfLastSeparator(String filename) {
        if (filename == null) return NOT_FOUND;

        int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
        int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
        return Math.max(lastUnixPos, lastWindowsPos);
    }

    public static String getName(String filename) {
        if (filename == null) return null;

        failIfNullBytePresent(filename);
        final int index = indexOfLastSeparator(filename);
        return filename.substring(index + 1);
    }

    public static String removeExtension(String filename) {
        if (filename == null) return null;
        failIfNullBytePresent(filename);

        int index = indexOfExtension(filename);
        return index == NOT_FOUND ? filename : filename.substring(0, index);
    }

    public static int indexOfExtension(String filename) {
        if (filename == null) return NOT_FOUND;

        int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
        int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? NOT_FOUND : extensionPos;
    }

    private static void failIfNullBytePresent(final String path) {
        final int len = path.length();
        for (int i = 0; i < len; i++) {
            if (path.charAt(i) == 0) {
                throw new IllegalArgumentException("Null byte present in file/path name. There are no " +
                        "known legitimate use cases for such data, but several injection attacks may use it");
            }
        }
    }
}
