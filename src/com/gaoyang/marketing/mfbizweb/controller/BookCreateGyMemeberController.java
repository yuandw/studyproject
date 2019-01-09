package com.gaoyang.marketing.mfbizweb.controller;/**
 * Created by zhanghui on 2018-11-22.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.log.util.LogUtil;
import com.gaoyang.marketing.log.util.MethodCallResultEnum;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.service.BookCreateGyMemeberService;
import com.gaoyang.marketing.mfbizweb.util.DateUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author zhanghui
 * @create 2018-11-22
 * @description 预约领卡入口
 */

@RestController
public class BookCreateGyMemeberController {
    private static final String CONTROLLER_NAME = "[预约创建会员卡controller]";
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private LogUtil logUtil;

    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;

    @Autowired
    BookCreateGyMemeberService bookCreateGyMemeberService;

    /**
     * @return
     * @desc
     */
    @RequestMapping(value = "/bookCreateGyMemeber")
    public void bookCreateGyMemeber(@RequestParam("authcode") String authcode, HttpServletRequest request,
                                    HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("测试Origin------" + request.getHeader("Origin"));
        }
        //解决跨域问题  设置信任凤蝶域名来源

        String allowOrigin = mfbizWebAcmClient.getPorpertiesValue("allowOrigin");
        if (StringUtils.isBlank(allowOrigin) || "null".equals(allowOrigin)) {
            allowOrigin = "*";
        }

        response.setHeader("Access-Control-Allow-Origin", allowOrigin);
        response.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS,DELETE");

        try {
            //判断是否在预约时间有效期
            boolean isOpenBook = true;
            String res = "";
            String bookDateJson = mfbizWebAcmClient.getPorpertiesValue("bookMemeberDate");
            if (!StringUtils.isBlank(bookDateJson) && !"null".equals(bookDateJson)) {
                //在acm中出预约有效时间json串
                JSONArray jsonArray = JSONArray.parseArray(bookDateJson);

                //循环配置的jsonarray
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    String startBookTime = jsonObject.getString("start_time");
                    String endBookTime = jsonObject.getString("end_time");

                    //当前时间
                    Long cruTime = System.currentTimeMillis();
                    Long startTimeMillis = DateUtil.parseNormalDateFromStr(startBookTime).getTime();
                    Long endTimeMillis = DateUtil.parseNormalDateFromStr(endBookTime).getTime();
                    //判断当前时间是否在（开始时间 结束时间）区间
                    if (startTimeMillis > cruTime || endTimeMillis < cruTime) {
                        isOpenBook = false;
                        break;
                    }
                }
            }
            HttpResponseBean responseBean = new HttpResponseBean();
            if (!isOpenBook) {
                //超出预约有效时间 21610
                res = ResponseEnums.TYPE_APPOINTMENT_OUT_TIME.getCode();

            } else {
                responseBean = bookCreateGyMemeberService.bookCreateGyMemeber(authcode);
                res = responseBean.getCode();
            }

            // 0999 系统繁忙   10000 预约成功   21608 已预约  21609 已经是会员 21610 超出预约有效时间
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "预约开卡返回信息,traceId={},authCode={},res={}", traceId, authcode, res);
            }
            if (ResponseEnums.TYPE_SUCCESS.getCode().equals(responseBean.getCode())) {
                logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.SUCCESS, "用户预约成功", startTime);
            } else {
                logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "用户预约失败", startTime);
            }

            response.getWriter().write(res);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }
}
