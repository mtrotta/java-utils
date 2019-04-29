package org.matteo.utils.clean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 21/09/12
 */
class CleanerTest {

    @Test
    void testDelete() throws Exception {

        final List<Fake> deletables = new ArrayList<>();

        List<CheckerConfiguration> checkers = new ArrayList<>();
        checkers.add(Checkers.YEARLY.with(1));
        checkers.add(Checkers.QUARTERLY.with(2));
        checkers.add(Checkers.MONTHLY.with(3));
        checkers.add(Checkers.DAILY.with(4));

        Fake daily = new Fake("20130125");
        Fake daily2 = new Fake("20130124");
        Fake daily3 = new Fake("20130123");
        Fake daily4 = new Fake("20130122");
        Fake daily5 = new Fake("20130121");
        Fake monthly = new Fake("20121130");
        Fake monthly2 = new Fake("20121031");
        Fake monthly3 = new Fake("20120831");
        Fake quarterly = new Fake("20121231");
        Fake quarterly1 = new Fake("20120928");
        Fake quarterly2 = new Fake("20120629");
        Fake quarterly3 = new Fake("20110930");
        Fake yearly = new Fake("20121231");
        Fake yearly2 = new Fake("20111230");

        deletables.add(daily);
        deletables.add(daily2);
        deletables.add(daily3);
        deletables.add(daily4);
        deletables.add(daily5);
        deletables.add(monthly);
        deletables.add(monthly2);
        deletables.add(monthly3);
        deletables.add(quarterly);
        deletables.add(quarterly1);
        deletables.add(quarterly2);
        deletables.add(quarterly3);
        deletables.add(yearly);
        deletables.add(yearly2);

        Eraser<Fake> eraser = new Eraser<Fake>() {
            @Override
            public void erase(Fake fakeDeletable) {
                fakeDeletable.setDeleted();
            }

            @Override
            public Collection<Deletable<Fake>> getDeletables() {
                List<Deletable<Fake>> list = new ArrayList<>();
                for (Fake fake : deletables) {
                    list.add(new Deletable<Fake>() {
                        @Override
                        public Date getDate() {
                            return fake.date;
                        }

                        @Override
                        public Fake getObject() {
                            return fake;
                        }
                    });
                }
                return list;
            }
        };

        Collection<Fake> cleaned = Cleaner.clean(eraser, stringToDate("20130126"), checkers, false);

        Assertions.assertEquals(5, cleaned.size());

        Assertions.assertFalse(daily.deleted);
        Assertions.assertFalse(daily2.deleted);
        Assertions.assertFalse(daily3.deleted);
        Assertions.assertFalse(daily4.deleted);
        Assertions.assertTrue(daily5.deleted);
        Assertions.assertFalse(monthly.deleted);
        Assertions.assertFalse(monthly2.deleted);
        Assertions.assertTrue(monthly3.deleted);
        Assertions.assertFalse(quarterly.deleted);
        Assertions.assertFalse(quarterly1.deleted);
        Assertions.assertTrue(quarterly2.deleted);
        Assertions.assertTrue(quarterly3.deleted);
        Assertions.assertFalse(yearly.deleted);
        Assertions.assertTrue(yearly2.deleted);
    }

    public static class Fake {

        private final Date date;
        private boolean deleted;

        Fake(String string) {
            this.date = stringToDate(string);
        }

        void setDeleted() {
            this.deleted = true;
        }

    }

    private static Date stringToDate(String date) {
        try {
            return new SimpleDateFormat("yyyyMMdd").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
