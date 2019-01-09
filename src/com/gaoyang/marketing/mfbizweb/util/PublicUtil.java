package com.gaoyang.marketing.mfbizweb.util;/**
 * Created by zhanghui on 2018-9-21.
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/**
 * @author zhanghui
 * @create 2018-9-21
 * @description
 */
public class PublicUtil {
    /**
     * @desc 判断对象是否为空
     * @param obj
     * @return
     */
    public static boolean isEmpty(Object obj)
    {
        if (obj == null)
        {
            return true;
        }
        if ((obj instanceof List))
        {
            return ((List) obj).size() == 0;
        }
        if ((obj instanceof String))
        {
            return ("").equals(((String) obj).trim());
        }
        return false;
    }


    public  static String getSercetiphone(String iphone){
        String res =iphone.substring(0,iphone.length()-(iphone.substring(3)).length())+"****"+iphone.substring(7);
        return  res;

    }

    /**
     * @desc  在acm配置中获取流量
     * createMemeberSendFlowRule=[{"start_time" : "2018-10-30 12:00:00","end_time" : "2038-11-15 12:00:00","send_flow" : "1000"}]

     * @param json
     * @return
     */
    public static String getSendFlow(String json) {
        String sendFlow = "0";
        JSONArray jsonArray = JSONArray.parseArray(json);

        //循环配置的jsonarray
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String startTime = jsonObject.getString("start_time");
            String endTime = jsonObject.getString("end_time");

            //当前时间
            Long cruTime = System.currentTimeMillis();
            Long startTimeMillis = DateUtil.parseNormalDateFromStr(startTime).getTime();
            Long endTimeMillis = DateUtil.parseNormalDateFromStr(endTime).getTime();

            //判断当前时间是否在（开始时间 结束时间）区间
            if (startTimeMillis <= cruTime && cruTime <= endTimeMillis) {
                sendFlow = jsonObject.getString("send_flow");
                break;
            }
        }
        return sendFlow;
    }
    public static boolean isMember(String memberId){
        if (StringUtils.isBlank(memberId) || "-1".equals(memberId)) {
            return false;
        }
        return  true;
    }

    public static String getTag(String paramsContent){
        String paramsDecode= null;
        String tag="";
        try {
            paramsDecode = URLDecoder.decode(paramsContent,"UTF-8");
            JSONObject response = JSON.parseObject(paramsDecode, Feature.OrderedField);
            tag= response.getString("tag");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return tag;
    }

}
