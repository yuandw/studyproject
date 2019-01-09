package com.gaoyang.marketing.mfbizweb.util;

/**
 * @Auther: yuandw
 * @Date: 2018-12-20 14:21
 * @Description:昵称脱敏
 */
public class NameDisSensitivity {
    private static final String STR_STAR = "*";
    private static final int STAR_NO = 2;

    /**
     * 昵称脱敏展示规则：
     * 1、昵称大于等于4个字（字符），显示昵称的第一个字和最后一个字，中间的用2个*代替
     * 2、昵称小于4个字（字符），显示最后一个字，前面的用*代替，其中当昵称为1个字（字符）时，在昵称前面加一个*
     * @param value
     * @return
     */
    public static String toDisSensite(String value) {
        if ((null == value) || ("".equals(value)))
            return value;
        int len = value.length();
        StringBuilder stringBuilder = new StringBuilder();
        if (len < 4 && len > 0) {
            stringBuilder.append("*");
            stringBuilder.append(value.charAt(len - 1));
        } else {
            stringBuilder.append(value.substring(0, 1));
            for (int i = 0; i < 2; ++i)
                stringBuilder.append("*");
            stringBuilder.append(value.substring(len - 1, len));
        }
        return stringBuilder.toString();
    }
}
