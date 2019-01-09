package com.util;

public class DataDisSensitivity {

    private static final int SIZE = 16;//最多多少个*
    private static final String STR_STAR = "*";


    /**
     * 字符串总长度不变
     * 一个字，直接返回*
     * 两个字，替换第一个字为*
     * 【3,size+1】只留首尾，中间全替换成*
     * [size+2,*)前面留(len-size)/2,中间size个*，后面(len-size)/2个字
     * @param value
     * @return
     */
    public static String toDisSensite(String value) {
        if (null == value || "".equals(value)) {//空字符串直接返回
            return value;
        }
        int len = value.length();
        int str_one = len / 2;
        int str_two = str_one - 1;
        int str_three = len % 2;
        StringBuilder stringBuilder = new StringBuilder();
        if (len <= 2) {//一个字或者两个字
            if (str_three == 1) {//一个字，直接返回*
                return STR_STAR;
            }
            stringBuilder.append(STR_STAR);
            stringBuilder.append(value.charAt(len - 1));//两个字，替换第一个字为*
        } else {//两个字以上
            if (str_two <= 0) {//3或者4个字，直流首尾，中间全替换成*
                stringBuilder.append(value.substring(0, 1));
                stringBuilder.append(STR_STAR);
                stringBuilder.append(value.substring(len - 1, len));
            } else if (str_two >= SIZE / 2 && SIZE + 1 != len) {//[size+2,*)，中间size个*
                int pamafive = (len - SIZE) / 2;//*号前面字的数目
                stringBuilder.append(value.substring(0, pamafive));
                for (int i = 0; i < SIZE; i++) {//加size个*
                    stringBuilder.append(STR_STAR);
                }
                if ((str_three == 0 && SIZE / 2 == 0) || (str_three != 0 && SIZE % 2 != 0)) {//总字数,*奇偶数不同，后面显示字数不同，保证字符串总长度不变
                    stringBuilder.append(value.substring(len - pamafive, len));
                } else {
                    stringBuilder.append(value.substring(len - (pamafive + 1), len));
                }
            } else {//【5,size+1】
                int str_four = len - 2;
                stringBuilder.append(value.substring(0, 1));
                for (int i = 0; i < str_four; i++) {
                    stringBuilder.append(STR_STAR);
                }
                stringBuilder.append(value.substring(len - 1, len));
            }
        }
        return stringBuilder.toString();
    }

    public static void main(String[] args) {
        System.out.println(DataDisSensitivity.toDisSensite("袁大为袁大为大为为"));
    }
}
