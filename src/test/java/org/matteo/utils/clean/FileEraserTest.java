package org.matteo.utils.clean;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 12/02/13
 */
class FileEraserTest {

    private final String TODAY = "20121212";

    @Test
    void test() throws Exception {
        FileEraser eraser = new FileEraser();
        Date date = eraser.getDate("20121212");
        assertTrue(checkDeletable(eraser, new File("log_" + TODAY), date));
        assertTrue(checkDeletable(eraser, new File(TODAY + ".log"), date));
        assertTrue(checkDeletable(eraser, new File(TODAY), date));
        assertFalse(checkDeletable(eraser, new File("log"), date));
        assertFalse(checkDeletable(eraser, new File("20130239"), date));
        assertFalse(checkDeletable(eraser, new File("20130229"), date));
    }

    @Test
    void testDeleteFolder() throws Exception {
        File file = new File(TODAY +"_folder");
        if (!file.isDirectory() && !file.mkdir()) {
            throw new Exception("Unable to create folder " + file.getAbsolutePath());
        }
        assertTrue(file.exists());
        FileEraser eraser = new FileEraser();
        eraser.erase(file);
        assertFalse(file.exists());
    }

    @Test
    void testDeleteFile() throws Exception {
        File file = new File(TODAY +"_file");
        if (!file.isFile() && !file.createNewFile()) {
            throw new Exception("Unable to create folder " + file.getAbsolutePath());
        }
        assertTrue(file.exists());
        FileEraser eraser = new FileEraser();
        eraser.erase(file);
        assertFalse(file.exists());
    }

    private boolean checkDeletable(FileEraser eraser, File file, Date date) {
        Deletable<File> deletableFile = eraser.getDeletable(file);
        if (deletableFile != null) {
            assertEquals(date, deletableFile.getDate());
            return true;
        }
        return false;
    }
}
