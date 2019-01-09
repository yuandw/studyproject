package com.gaoyang.marketing.mfbizweb.util;

import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author snowalker
 * @date 2018/9/20
 * @desc 日期工具
 */
public class DateUtil {

    private DateUtil() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);

    /**
     * 线程安全方式
     */
    private static final ThreadLocal<DateFormat> DATE_FORMAT_THREAD_LOCAL
            = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMddHHmmss"));

    private static final ThreadLocal<DateFormat> DATE_FORMATY_YYYMMDD
            = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    private static final ThreadLocal<DateFormat> DATE_FORMAT_YYYYMM
            = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMM"));

    private static final ThreadLocal<DateFormat> DATE_FORMAT_NORMAL_THREAD_LOCAL
            = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    /**
     * 格式化时间为时间戳：yyyyMMddHHmmss
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        return DATE_FORMAT_THREAD_LOCAL.get().format(date);
    }

    /**
     * 解析字符串形式的时间戳为java.util.Date
     * @param timeStampStr
     * @return
     */
    public static Date parseDateFromStr(String timeStampStr) {
        try {
            return DATE_FORMAT_THREAD_LOCAL.get().parse(timeStampStr);
        } catch (ParseException e) {
            LOGGER.error("解析'yyyyMMddHHmmss'形式时间戳为java.util.Date失败,e={}", e);
        }
        return null;
    }

    public static void remove() {
        DATE_FORMAT_THREAD_LOCAL.remove();
    }


    /**
     * 格式化时间为时间戳：yyyyMMdd
     * @param date
     * @return
     */
    public static String formatDateYyyyMMdd(Date date) {
        return DATE_FORMATY_YYYMMDD.get().format(date);
    }

    /**
     * 解析字符串形式的时间戳为java.util.Date
     * @param timeStampStr
     * @return
     */
    public static Date parseDateFromStrYyyyMMdd(String timeStampStr) {
        try {
            return DATE_FORMATY_YYYMMDD.get().parse(timeStampStr);
        } catch (ParseException e) {
            LOGGER.error("解析'yyyyMMdd'形式时间戳为java.util.Date失败,e={}", e);
        }
        return null;
    }

    public static void removeYyyyMMdd() {
        DATE_FORMATY_YYYMMDD.remove();
    }

    /**
     * 格式化时间为时间戳：yyyyMM
     * @param date
     * @return
     */
    public static String formatNormalDate(Date date) {
        return DATE_FORMAT_NORMAL_THREAD_LOCAL.get().format(date);
    }

    /**
     * 解析字符串形式的时间戳为java.util.Date
     * @param timeStampStr
     * @return
     */
    public static Date parseNormalDateFromStr(String timeStampStr) {
        try {
            return DATE_FORMAT_NORMAL_THREAD_LOCAL.get().parse(timeStampStr);
        } catch (ParseException e) {
            LOGGER.error("解析'yyyy-MM-dd HH:mm:ss'形式时间戳为java.util.Date失败,e={}", e);
        }
        return null;
    }

    public static void removeNormal() {
        DATE_FORMAT_NORMAL_THREAD_LOCAL.remove();
    }

    /**
     * 格式化时间为时间戳：yyyy-MM-dd HH:mm:ss
     * @param date
     * @return
     */
    public static String formatDateYyyyMM(Date date) {
        return DATE_FORMAT_YYYYMM.get().format(date);
    }

    /**
     * 解析字符串形式的时间戳为java.util.Date  yyyy-MM-dd HH:mm:ss
     * @param timeStampStr
     * @return
     */
    public static Date parseDateFromStrYyyyMM(String timeStampStr) {
        try {
            return DATE_FORMAT_YYYYMM.get().parse(timeStampStr);
        } catch (ParseException e) {
            LOGGER.error("解析'yyyyMM'形式时间戳为java.util.Date失败,e={}", e);
        }
        return null;
    }

    public static void removeYyyyMM() {
        DATE_FORMAT_YYYYMM.remove();
    }



}


