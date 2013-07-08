package se.sitic.megatron.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;


/**
 * Static utility-methods for file handling.
 */
public abstract class FileUtil {
    private static final Logger log = Logger.getLogger(FileUtil.class);


    /**
     * As ensureDir(File), but takes a dir as string.
     */
    public static boolean ensureDir(String dirname) throws IOException {
        if (dirname == null) {
            throw new IOException("Cannot check directory. Parameter is null.");
        }
        return ensureDir(new File(dirname));
    }


    /**
     * Ensure that specified directory exist. If dir is missing, it's created.
     *
     * @param dir directory to check.
     * @return true if directory was created, false if it existed.
     * @exception IOException if directory could not be created.
     */
    public static boolean ensureDir(File dir) throws IOException {
        if (dir == null) {
            throw new IOException("Cannot check directory. Parameter is null.");
        }

        boolean result = false;
        // exists dir?
        if (dir.isDirectory()) {
            result = false;
        } else {
            if (dir.exists()) {
                throw new IOException("File exist, but is not a directory: " + dir);
            }
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create directory: " + dir);
            }
            result = true;
        }

        return result;
    }

    
    /**
     * Deletes specified directory and its sub-directories.
     * 
     * @return true if directory have been deleted, or is missing.
     */
    public static boolean removeDirectory(File directory) {
        if ((directory == null) || !directory.isDirectory()) {
            return false;
        }
        if (!directory.exists()) {
            return true;
        }

        String[] dirList = directory.list();
        if (dirList != null) {
            for (int i = 0; i < dirList.length; i++) {
                File entry = new File(directory, dirList[i]);
                if (entry.isDirectory()) {
                    if (!removeDirectory(entry)) {
                        return false;
                    }
                } else {
                    // log.debug("Deleting file: " + entry.getAbsolutePath());
                    if (!entry.delete()) {
                        return false;
                    }
                }
            }
        }

        log.debug("Deleting directory: " + directory.getAbsolutePath());
        return directory.delete();
    }

    
    /**
     * Concatenates specified path with name, and makes sure that only one
     * path-separator is added. Examples: "/path1/" + "/name1" -->
     * "/path1/name1", "/path2" + "name2" --> "path1/name1".
     */
    public static String concatPath(String pathName, String name) {
        // param-check
        if ((pathName == null) || (pathName.length() == 0)) {
            return name;
        }
        if ((name == null) || (name.length() == 0)) {
            return pathName;
        }

        StringBuffer result = new StringBuffer(160);
        if (pathName.endsWith("/")) {
            if (name.startsWith("/")) {
                // remove "/"
                result.append(pathName.substring(0, pathName.length() - 1));
            } else {
                result.append(pathName);
            }
        } else {
            result.append(pathName);
            if (!name.startsWith("/")) {
                // add "/"
                result.append("/");
            }
        }
        result.append(name);

        return result.toString();
    }
    
    
    /**
     * Reads specified file and returns content as a string. 
     */
    public static String readFile(File file, String charSet) throws FileNotFoundException, IOException {
        StringBuilder result = new StringBuilder(1024);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), charSet));
            String line = null;
            while ((line = in.readLine()) != null) {
                result.append(line).append(Constants.LINE_BREAK);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        
        return result.toString();
    }

    
    /**
     * Writes content to specified file and character-set. 
     */
    public static void writeFile(File file, String content, String charSet) throws IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charSet));
            out.write(content);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    
    /**
     * Writes content to specified file in UTF-8. 
     */
    public static void writeFile(File file, String content) throws IOException {
        writeFile(file, content, Constants.UTF8);
    }

    
    /**
     * Returns the contents of the file in a byte array.
     */
    public static byte[] getBytesFromFile(File file) throws FileNotFoundException, IOException {
        byte[] result = null;
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            long fileLength = file.length();
            if (fileLength > Integer.MAX_VALUE) {
                throw new IOException("File too large: " + file.getAbsolutePath());
            }
            result = new byte[(int)fileLength];

            int offset = 0;
            int numRead = 0;
            while ((offset < result.length) && (numRead = in.read(result, offset, result.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < result.length) {
                throw new IOException("Could not completely read file:" + file.getAbsolutePath());
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return result;
    }

    
    /**
     * Count number of lines in specified file.
     */
    public static long countLines(File file, String charSet) throws IOException {
        long result = 0L;
        
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), charSet));
            while (in.readLine() != null) {
                ++result;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    
        return result;
    }

    
    /**
     * Returns hash value for specified file, e.g. using the MD5 algorithm.
     *
     * @return hex-encoded hash value. 
     */
    public static String hashFile(File file) throws IOException {
        String result = null;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(Constants.DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot initialize digest algorithm (this should never happen).", e);
            return "";
        }
        byte[] buffer = new byte[16*1024];
        int read = 0;
        InputStream in = new FileInputStream(file);
        try {
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] hashValue = digest.digest();
            result = StringUtil.encode(hashValue);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return result;
    }
    
    
    /**
     * Copy source file to target.
     * 
     * @param overwrite overwrite target? If target exists and overwrite==false an exception
     *      will be thrown.
     */
    public static void copyFile(File srcFile, File targetFile, boolean overwrite) throws IOException {
        log.debug("Copy file '" + srcFile.getAbsolutePath() + "' to '" + targetFile.getAbsolutePath() + "'.");
        if (!overwrite && targetFile.exists()) {
            throw new IOException("Target file exists and cannot be overwritten: " + targetFile.getAbsolutePath());
        }
        
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(targetFile);
            byte[] buf = new byte[4*1024];
            long bytesCopied = 0L;
            int len = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                bytesCopied += len;
            }
            log.debug("File copied. No. of bytes: " + bytesCopied);
            if (bytesCopied != srcFile.length()) {
                throw new IOException("Size of source- and target file differ: " + bytesCopied + " != " + srcFile.length());
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();    
            }
        }
    }

}
