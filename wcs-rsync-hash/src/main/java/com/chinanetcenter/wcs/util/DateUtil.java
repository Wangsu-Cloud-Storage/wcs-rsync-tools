package com.chinanetcenter.wcs.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {

    private static final int ONE = 1;
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATE_PATTERN_SIMPLE = "yyyyMMdd";
    public static final String LOG_DATE_PATTERN = "dd/MMM/yyyy";
    public static final String HOUR_PATTERN = "yyyy-MM-dd-HH";
    public static final String MIN_PATTERN = "yyyy-MM-dd-HH-mm";
    public static final String MINUTE_PATTERN = "yyyy-MM-dd HH:mm";
    public static final String SECOND_PATTERN = "yyyy-MM-dd-HH-mm-ss";
    public static final String SIMPLE_SECOND_PATTERN = "yyyyMMddHHmmss";
    public static final String COMMON_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String MILLISECOND_PATTERN = "HH:mm:ss.SSS";
    public static final String COMMON_PATTERN_MILLISECOND = "yyyy-MM-dd HH:mm:ss.SSS";
    /**
     * format for RFC 1123 date string -- "Sun, 06 Nov 1994 08:49:37 GMT"
     */
    public final static String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String TIME_MIN_PATTERN = "HH:mm";
    public static final Locale US = Locale.US;
    public static final String CHART_DATE_PATTERN = "%Y-%m-%d";
    public static final String CHART_HOUR_PATTERN = "%Y-%m-%d-%H";

    public static Date now() {
        return new Date();
    }

    public static long yesterdayBeginTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static Date dayBeforeBeginTime(int num) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -num);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date dayBeforeEndTime(int num) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -num);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    public static Date yesterday() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Date(calendar.getTimeInMillis());
    }

    public static long todayBeginTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static Date hourEndTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date hourStartTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static boolean isSameDay(Long start, Long end) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(start);
        int date = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.setTimeInMillis(end);
        return date == calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static boolean isSameDay(Date start, Date end) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        int date = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTime(end);
        return date == calendar.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isSameByDatePattern(Date start, Date end) {
        String formatDate = formatDate(start, DATE_PATTERN);
        String endDate = formatDate(end, DATE_PATTERN);
        return formatDate.equals(endDate);
    }

    public static boolean isSameWeek(Date start, Date end) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        int date = calendar.get(Calendar.WEEK_OF_YEAR);
        calendar.setTime(end);
        return date == calendar.get(Calendar.WEEK_OF_YEAR);
    }

    public static boolean isSameMonth(Date start, Date end) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        int date = calendar.get(Calendar.MONTH);
        calendar.setTime(end);
        return date == calendar.get(Calendar.MONTH);
    }

    public static long lastHour() {
        return lastHours(1);
    }

    public static long last2Hours() {
        return lastHours(2);
    }

    public static long lastHours(int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, (-1) * n);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long nextDays(int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, n);
        return calendar.getTimeInMillis();
    }

    public static Date nextHours(int n, Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, n);
        return calendar.getTime();
    }

    public static Date nextMinute(int n, Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, n);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    public static Date nextSecond(int n, Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, n);
        return calendar.getTime();
    }

    public static Date nextDate(int n, Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, n);
        return calendar.getTime();
    }

    /**
     * 获得某天的开始时间
     *
     * @param n 负数代表过去, 正数代表将来
     * @return
     */
    public static Date getNextDateOfStart(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, n);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 获取某天的结束时间
     *
     * @param n n 负数代表过去, 正数代表将来
     * @return
     */
    public static Date getNextDateOfEnd(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, n);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }


    /**
     * 获得昨天某个小时的开始时间
     *
     * @param n 负数代表过去, 正数代表将来
     * @return
     */
    public static Date getNextHourOfStart(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        c.add(Calendar.HOUR_OF_DAY, n);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 获取昨天某个小时的结束时间
     *
     * @param n n 负数代表过去, 正数代表将来
     * @return
     */
    public static Date getNextHourOfEnd(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        c.add(Calendar.HOUR_OF_DAY, n);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    /**
     * 获得某分钟的开始时间
     *
     * @param n 负数代表过去, 正数代表将来
     * @return
     */
    public static Date getNextMinuteOfStart(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, n);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 获取某分钟的结束时间
     *
     * @param n n 负数代表过去, 正数代表将来
     * @return
     */
    public static Date getNextMinuteOfEnd(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, n);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    public static void main(String[] args) {
        System.out.println(DateUtil.getNextHourOfStart(-1));
        System.out.println(DateUtil.getNextHourOfEnd(-1));
        System.out.println(DateUtil.getNextMinuteOfStart(-5));
        System.out.println(DateUtil.getNextMinuteOfEnd(-1));
    }

    public static long lastHalfHours() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static Date lastMonth(int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -n);
        return new Date(calendar.getTimeInMillis());
    }

    public static Date lastMonthDay(Date date, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, -n);
        return new Date(calendar.getTimeInMillis());
    }

    public static String formatDate(Date time, String pattern) {
        if (time == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(time);
    }

    public static String formatDate(Date time, String pattern, Locale locale) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        return format.format(time);
    }

    public static String dayPattern(long time) {
        return DateFormatUtils.format(time, DATE_PATTERN);
    }

    public static String logDayPattern(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(LOG_DATE_PATTERN, US);
        return format.format(date);
    }

    public static String hourPattern(long time) {
        SimpleDateFormat format = new SimpleDateFormat(HOUR_PATTERN);
        return format.format(new Date(time));
    }

    public static String minutePattern(long time) {
        return DateFormatUtils.format(time, MIN_PATTERN);
    }

    public static String simpleSecondPattern(long time) {
        return DateFormatUtils.format(time, SECOND_PATTERN);
    }

    public static String fiveMinutePattern(long time) {
        time = (time / (300000)) * 300000;
        return DateFormatUtils.format(time, MIN_PATTERN);
    }

    public static String halfMinutePattern(long time) {
        time = (time / (30000)) * 30000;
        return DateFormatUtils.format(time, SECOND_PATTERN);
    }

    public static Date parseDate(String time, String pattern) {
        if (StringUtils.isEmpty(time)) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        try {
            return format.parse(time);
        } catch (ParseException e) {
        }
        return null;
    }

    public static Date parseDate(String time, String pattern, Locale local) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, local);
        try {
            return format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] last2HourPattern() {
        return new String[]{hourPattern(System.currentTimeMillis()), hourPattern(lastHour()), hourPattern(last2Hours())};
    }

    public static String[] last1HourPattern() {
        return new String[]{hourPattern(System.currentTimeMillis()), hourPattern(lastHour())};
    }

    public static Date minutesBefore(int num) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -num);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Date(calendar.getTimeInMillis());
    }

    /**
     * 取指定日期的开始日期时间
     *
     * @param date 待处理的日期
     * @return 传入日期的开始时间
     */
    @SuppressWarnings("deprecation")
    public static Date getStartDate(Date date) {
        try {
            date.setHours(0);
            date.setMinutes(0);
            date.setSeconds(0);
            return date;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Date getFirstDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return calendar.getTime();
    }

    public static Date getFirstDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_MONTH, ONE);
        return calendar.getTime();
    }

    public static Date getLastDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    public static long dateDiff(Date startTime, Date endTime, int type) {
        long result = 0;
        long nd = 1000 * 24 * 60 * 60;// 一天的毫秒数
        long nh = 1000 * 60 * 60;// 一小时的毫秒数
        long nm = 1000 * 60;// 一分钟的毫秒数
        long ns = 1000;// 一秒钟的毫秒数
        long diff;
        try {
            // 获得两个时间的毫秒时间差异
            diff = endTime.getTime() - startTime.getTime();
            switch (type) {
                case Calendar.DATE:
                    result = diff / nd;// 计算差多少天
                    break;
                case Calendar.HOUR:
                    result = diff / nh;// 计算差多少小时
                    break;
                case Calendar.MINUTE:
                    result = diff / nm;// 计算差多少分钟
                    break;
                case Calendar.SECOND:
                    result = diff / ns;// 计算差多少秒
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取更改时区后的日期
     *
     * @param date    日期
     * @param oldZone 旧时区对象
     * @param newZone 新时区对象
     * @return 日期
     */
    public static Date changeTimeZone(Date date, TimeZone oldZone, TimeZone newZone) {
        Date dateTmp = null;
        if (date != null) {
            int timeOffset = oldZone.getRawOffset() - newZone.getRawOffset();
            dateTmp = new Date(date.getTime() - timeOffset);
        }
        return dateTmp;
    }

    /**
     * 负数为几个小时之前，正数为几个小时之后
     *
     * @param n
     * @return
     */
    public static Date getNextHourNow(int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, n);
        return calendar.getTime();
    }
}
