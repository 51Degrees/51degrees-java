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

package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.Date;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The published date is the start of its day in the local time zone, read
 * in the morning or the afternoon, so the data update service can compare
 * the date of a new file with the date of the data loaded.
 */
public class PublishedDateTests {

    private static final int YEAR = 2026;

    /**
     * The month as the native engine gives it, where January is 1.
     */
    private static final int MONTH = 4;

    private static final int DAY = 1;

    private static final int MORNING_HOUR = 2;

    private static final int AFTERNOON_HOUR = 14;

    /**
     * The smallest offset from UTC, in hours, that a time zone has.
     */
    private static final int MIN_OFFSET_HOURS = -12;

    private static final int HOURS_PER_DAY = 24;

    @Test
    public void PublishedDate_StartOfDay_MorningAndAfternoon()
        throws Exception {
        EngineOverMockNativeCode mock = new EngineOverMockNativeCode();
        Date published = mock(Date.class);
        when(published.getYear()).thenReturn(YEAR);
        when(published.getMonth()).thenReturn(MONTH);
        when(published.getDay()).thenReturn(DAY);
        when(mock.nativeEngine.getPublishedTime()).thenReturn(published);

        TimeZone original = TimeZone.getDefault();
        try {
            for (int hour : new int[]{MORNING_HOUR, AFTERNOON_HOUR}) {
                TimeZone.setDefault(zoneWhereItIs(hour));
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(mock.engine.getDataFilePublishedDate(null));
                String when = "Read at " + hour + ":00 local time";
                assertEquals(when, YEAR, calendar.get(Calendar.YEAR));
                assertEquals(when, MONTH - 1, calendar.get(Calendar.MONTH));
                assertEquals(when, DAY, calendar.get(Calendar.DAY_OF_MONTH));
                assertEquals(when, 0, calendar.get(Calendar.HOUR_OF_DAY));
                assertEquals(when, 0, calendar.get(Calendar.MINUTE));
                assertEquals(when, 0, calendar.get(Calendar.SECOND));
                assertEquals(when, 0, calendar.get(Calendar.MILLISECOND));
            }
        }
        finally {
            TimeZone.setDefault(original);
        }
    }

    /**
     * A time zone where the local time now is within the hour given.
     */
    private static TimeZone zoneWhereItIs(int localHour) {
        int offset = localHour - ZonedDateTime.now(ZoneOffset.UTC).getHour();
        if (offset < MIN_OFFSET_HOURS) {
            offset += HOURS_PER_DAY;
        }
        return TimeZone.getTimeZone(ZoneOffset.ofHours(offset));
    }
}
