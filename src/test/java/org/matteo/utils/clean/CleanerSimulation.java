package org.matteo.utils.clean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CleanerSimulation {

    public static void main(final String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println(String.format("Usage: %s <yyyyMMdd> <file>", CleanerSimulation.class.getSimpleName()));
        } else {
            String reference = args[0];
            List<CheckerConfiguration> checkers = Arrays.asList(
                    Checkers.YEARLY.with(1),
                    Checkers.MONTHLY.with(1),
                    Checkers.WEEKLY.with(4),
                    Checkers.DAILY.with(3)
            );
            Collection<String> deleted = Cleaner.clean(new SimulatedEraser(args[1]), stringToDate(reference), checkers, true);
            for (String string : deleted) {
                System.out.println(string);
            }
            System.out.println("Total: " + deleted.size());
        }
    }

    private static Date stringToDate(String date) {
        try {
            return new SimpleDateFormat("yyyyMMdd").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    static class SimulatedEraser implements Eraser<String> {

        private final String file;

        SimulatedEraser(String file) {
            this.file = file;
        }

        @Override
        public void erase(String deletable) {
            System.out.println("Erasing " + deletable);
        }

        @Override
        public Collection<Deletable<String>> getDeletables() {
            List<Deletable<String>> deletables = new ArrayList<>();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String dateString = line;
                    final Date date = stringToDate(line);
                    deletables.add(new Deletable<String>() {
                        @Override
                        public Date getDate() {
                            return date;
                        }

                        @Override
                        public String getObject() {
                            return dateString;
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return deletables;
        }
    }

}
