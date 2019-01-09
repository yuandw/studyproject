package com.gaoyang.marketing.mfbizweb.controller.baseController;/**
 * Created by zhanghui on 2018-9-24.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.log.util.LogUtil;
import com.gaoyang.marketing.log.util.MethodCallResultEnum;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.bean.UserInfoBean;
import com.gaoyang.marketing.mfbizweb.service.baseService.CreateGyMemeberService;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author zhanghui
 * @create 2018-9-24
 * @description 领取会员卡
 */
@Controller
public class CreateGyMemeberController {
    private static final String CONTROLLER_NAME="[创建会员卡controller]";
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    CreateGyMemeberService createGyMemeberService;
    @Autowired
    private LogUtil logUtil;
    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;
    @RequestMapping(value="/receive_member_card",method= RequestMethod.GET)
    @ResponseBody
    public String createGYMemeberInfo(@RequestParam("authcode") String authCode,
                                      @RequestParam(value = "sourceId", required = false, defaultValue = "default") String sourceId,
                                            HttpServletRequest request,
                                            HttpServletResponse response){
        long startTime = System.currentTimeMillis();
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "traceId={},authCode={}", traceId, authCode);
        }
        HttpResponseBean responseBean=new HttpResponseBean();
        Map<String,String> userInfo=(Map<String, String>)request.getSession().getAttribute("userInfo");
        //对session中的用户信息进行校验
        if(PublicUtil.isEmpty(userInfo)){
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            String returnStr= JSON.toJSONString(responseBean);
            logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.FAIL, "创建会员卡controller获取session为空", startTime);
            logger.warn(CONTROLLER_NAME+"traceId={},authCode={},returnStr={}",traceId,authCode,returnStr);
            return returnStr;
        }
        String uid=userInfo.get("uid");
        String userId=userInfo.get("userId");
        String accessToken=userInfo.get("accessTokenForCreateMember");
        String bookFlag=userInfo.get("bookFlag");
        String bookDate=userInfo.get("bookDate");

        //对session中的用户信息进行校验
        if(PublicUtil.isEmpty(uid)||PublicUtil.isEmpty(userId)||PublicUtil.isEmpty(accessToken)){
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            String returnStr= JSON.toJSONString(responseBean);
            logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.FAIL, "创建会员卡controller获取session中uid、userid、token信息为空为空", startTime);
            logger.warn(CONTROLLER_NAME+"traceId={},authCode={},returnStr={}",traceId,authCode,returnStr);
            return returnStr;
        }
        //sourceId为空时  默认取acm配置
        if(StringUtils.isBlank(sourceId)){
            sourceId=mfbizWebAcmClient.getPorpertiesValue("sourceId");
            if(StringUtils.isBlank(sourceId)||"null".equals(sourceId)){
                //默认开通服务
                sourceId="default";
            }
        }
        responseBean=createGyMemeberService.createGYMemeber(userId, accessToken, uid,bookFlag,bookDate,sourceId);
        String returnStr = JSONObject.toJSONString(responseBean);
        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "向前端返回，traceId={},uid={},userId={},returnStr={}，", traceId, uid,userId,returnStr);
        }
        if(ResponseEnums.TYPE_SUCCESS.getCode().equals(responseBean.getCode())){
            UserInfoBean userInfoBean=(UserInfoBean)responseBean.getData();
            userInfo.put("gyMemberId",userInfoBean.getMciGyId());
            userInfo.put("openDate",userInfoBean.getOpenDate());
            userInfo.put("iphone",userInfoBean.getPhone());
            userInfo.put("realIphone",userInfoBean.getRealIphone());
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "将手机号，会员卡号计入缓存,traceId={},userinfo={},创建会员卡成功向前端返回 res={}"
                        ,traceId, JSONObject.toJSONString(userInfo),returnStr);
            }
            request.getSession().setAttribute("userInfo",userInfo);
            //设置有效期
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(300);
            logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.SUCCESS, "创建会员卡成功", startTime);

        }else{
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "创建会员卡失败", startTime);
        }


        return returnStr;
    }



}
