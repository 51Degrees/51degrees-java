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

package fiftyone.pipeline.developerexamples.clientsideelement.data;

import java.util.Calendar;

//! [class]
public class StarSign {
    private final Calendar end;
    private final Calendar start;
    private final String name;

    public StarSign(String name, String start, String end) {
        this.name = name;
        this.start = dayOfYear(start);
        this.end = dayOfYear(end);
    }

    /**
     * The day and month of a "dd/mm" or "dd/mm/yyyy" date, in year 0 at
     * midnight, so that two dates compare by day and month alone.
     * @param date the date
     * @return the day and month
     */
    public static Calendar dayOfYear(String date) {
        String[] sections = date.split("/");
        Calendar calendar = Calendar.getInstance();
        // Clear the time of day the calendar was made with, which would
        // otherwise decide the comparison of two dates on the same day.
        calendar.clear();
        calendar.set(
            0,
            Integer.parseInt(sections[1]) - 1,
            Integer.parseInt(sections[0]));
        return calendar;
    }

    /**
     * True if the day and month fall within this sign, including its first
     * and last days. A sign that runs over the end of the year, as
     * Capricorn does, holds the days from its start to the end of December
     * and from the start of January to its end.
     * @param day a day and month from {@link #dayOfYear(String)}
     * @return true if the day falls within this sign
     */
    public boolean contains(Calendar day) {
        if (start.compareTo(end) <= 0) {
            return day.compareTo(start) >= 0 && day.compareTo(end) <= 0;
        }
        return day.compareTo(start) >= 0 || day.compareTo(end) <= 0;
    }

    public String getName() {
        return name;
    }

    public Calendar getStart() {
        return start;
    }

    public Calendar getEnd() {
        return end;
    }
}
//! [class]
