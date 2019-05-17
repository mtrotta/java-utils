package org.matteo.utils.clean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileEraser implements Eraser<File> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileEraser.class);

    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final String DATE_PATTERN = "(\\\\B|\\\\b)*20[0-9]{2}[01][0-9][0-3][0-9](\\\\B|\\\\b)*";

    private final SimpleDateFormat dateFormat;
    private final Pattern datePattern;

    private final Collection<String> paths;

    public FileEraser(Collection<String> paths) {
        this.paths = paths;
        this.dateFormat = new SimpleDateFormat(DATE_FORMAT);
        this.dateFormat.setLenient(false);
        this.datePattern = Pattern.compile(DATE_PATTERN);
    }

    public FileEraser() {
       this(new ArrayList<>());
    }

    @Override
    public void erase(File file) throws Exception {
        LOGGER.info("Deleting {}", file.getPath());
        delete(file);
    }

    private static void delete(File path) throws IOException {
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            delete(file);
                        } else {
                            Files.delete(file.toPath());
                        }
                    }
                }
            }
            Files.delete(path.toPath());
        }
    }

    @Override
    public Collection<Deletable<File>> getDeletables() {
        Collection<Deletable<File>> deletables = new ArrayList<>();
        for (String path : paths) {
            File folder = new File(path);
            if (folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null && files.length > 0) {
                    LOGGER.info("Analysing folder {} ({} elements)", folder.getPath(), files.length);
                    for (final File file : files) {
                        Deletable<File> deletable = getDeletable(file);
                        if (deletable != null) {
                            deletables.add(deletable);
                        }
                    }
                }
            } else {
                LOGGER.info("Ignoring path {} since it's a file or it doesn't exist", path);
            }
        }

        return deletables;
    }

    Deletable<File> getDeletable(File file) {
        Matcher matcher = datePattern.matcher(file.getName());
        if (matcher.find()) {
            String group = matcher.group();
            try {
                final Date date = getDate(group);
                return new Deletable<File>() {
                    @Override
                    public Date getDate() {
                        return date;
                    }

                    @Override
                    public File getObject() {
                        return file;
                    }
                };
            } catch (ParseException e) {
                LOGGER.warn("Ignoring file '{}' since it has an invalid date: {}", file, e.getMessage());
            }
        }
        return null;
    }

    Date getDate(String group) throws ParseException {
        return dateFormat.parse(group);
    }

}
