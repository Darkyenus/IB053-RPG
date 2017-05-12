package ib053.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 */
public class PersistenceUtil {

    /** Save given CharSequence into a file in a way that fails gracefully and does not lose data on failure. */
    public static void saveSecurely(CharSequence data, File to) throws PersistenceException {
        if (to == null || data == null) {
            throw new IllegalArgumentException("to and data can't be null");
        }
        final File canonicalTo;
        try {
            canonicalTo = to.getCanonicalFile();
        } catch (IOException e) {
            throw new PersistenceException("Can't get canonical version of "+to, e);
        }

        final File parentFile = canonicalTo.getParentFile();
        if (parentFile == null) {
            throw new PersistenceException("Parent file of "+canonicalTo+" is null");
        }
        parentFile.mkdirs();

        final File savingFile = new File(canonicalTo.getParent(), canonicalTo.getName() + ".saving");
        try(FileWriter writer = new FileWriter(savingFile)) {
            writer.append(data);
            writer.flush();
        } catch (IOException e) {
            throw new PersistenceException("Failed to save initial file", e);
        }

        char[] fileContent = new char[data.length()];
        // Check that saving file has correct content
        try(FileReader reader = new FileReader(savingFile)) {
            int read = reader.read(fileContent);
            if (read != fileContent.length) {
                throw new PersistenceException(".saving file contains invalid amount of characters");
            }

            for (int i = 0; i < read; i++) {
                if (fileContent[i] != data.charAt(i)) {
                    throw new PersistenceException(".saving file differs at position "+i);
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("Exception while checking .saving file content", e);
        }

        //Everything seems fine
        if(canonicalTo.exists() && !canonicalTo.delete()) {
            throw new PersistenceException("Can't delete old save file");
        }

        if(!savingFile.renameTo(canonicalTo)) {
            throw new PersistenceException("Can't rename .saving file");
        }
    }

    public static final class PersistenceException extends Exception {
        public PersistenceException() {
        }

        public PersistenceException(String message) {
            super(message);
        }

        public PersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
