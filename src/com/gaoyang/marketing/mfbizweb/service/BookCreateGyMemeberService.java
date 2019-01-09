package com.gaoyang.marketing.mfbizweb.service;/**
 * Created by zhanghui on 2018-11-24.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONArray;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.UserReserveFacade;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.model.ResponseData;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author zhanghui
 * @create 2018-11-24
 * @description
 */
@Service
public class BookCreateGyMemeberService {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource(name = "userReserveFacade")
    UserReserveFacade userReserveFacade;

    public HttpResponseBean bookCreateGyMemeber(String authcode) {
        String traceId = TraceIdUtil.getTraceId();
        ResponseData<Boolean> responseData = appointmentBooking(authcode);
        HttpResponseBean httpResponseBean=new HttpResponseBean();
        //调用rpc异常 返回0999 系统异常
        if (PublicUtil.isEmpty(authcode)) {
            logger.warn("调用用户平台接口authcode为空,traceId={},authCode={}", traceId, authcode);
            httpResponseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
            return httpResponseBean ;
        }

        //调用rpc异常 返回0999 系统异常
        if (PublicUtil.isEmpty(responseData)) {
            logger.warn("调用用户平台接口appointmentBooking预约返回responseData为空,traceId={},authCode={}", traceId, authcode);
            httpResponseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
            return httpResponseBean ;
        }
        if (ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(responseData.getCode())){
            //预约成功
            if (logger.isInfoEnabled()) {
                logger.info("调用用户平台接口appointmentBooking预约成功,traceId={},authCode={}", traceId, authcode);
            }
            httpResponseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_SUCCESS.getDesc());
            return httpResponseBean ;

        }
        if (ResponseEnums.RPC_TYPE_GET_UID_ILLEGAL.getCode().equals(responseData.getCode())){
            //获取uid失败
            if (logger.isInfoEnabled()) {
                logger.info("调用用户平台接口appointmentBooking预约获取uid失败,traceId={},authCode={}", traceId, authcode);
            }

            httpResponseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
            return httpResponseBean ;
        }
        if (ResponseEnums.RPC_TYPE_IS_MEMBER.getCode().equals(responseData.getCode())){
            //已经是高阳会员
            if (logger.isInfoEnabled()) {
                logger.info("调用用户平台接口appointmentBooking预约已经是高阳会员,traceId={},authCode={}", traceId, authcode);
            }
            httpResponseBean.setCode(ResponseEnums.TYPE_IS_MEMBER_FAIL.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_IS_MEMBER_FAIL.getDesc());
            return httpResponseBean;

        }
        if (ResponseEnums.RPC_TYPE_APPOINTMENT_MORE.getCode().equals(responseData.getCode())){
            //重复预约
            if (logger.isInfoEnabled()) {
                logger.info("调用用户平台接口appointmentBooking预约.重复预约,traceId={},authCode={}", traceId, authcode);
            }
            httpResponseBean.setCode(ResponseEnums.TYPE_APPOINTMENT_MORE_FAIL.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_APPOINTMENT_MORE_FAIL.getDesc());
            return httpResponseBean;

        }
        httpResponseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
        httpResponseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
        return httpResponseBean;

    }

    /**
     * @param authCode
     * @return
     * @desc 调用用户平台预约创建会员卡
     */
    public ResponseData<Boolean> appointmentBooking(String authCode) {
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用用户平台接口appointmentBooking,traceId={},authCode={}", traceId, authCode);
        }
        try {
            ResponseData<Boolean> responseData = userReserveFacade.appointmentBooking(authCode);

            //判断接口返回code
            if (PublicUtil.isEmpty(responseData)) {
                logger.warn("调用用户平台接口appointmentBooking预约失败，返回responseData为空,traceId={},authCode={}", traceId, authCode);
                return null;
            }
            if (logger.isInfoEnabled()) {
                logger.info("调用用户平台接口appointmentBooking预约,traceId={},responseData={}", traceId, JSONArray.toJSONString(responseData));
            }
            return responseData;
        } catch (Exception e) {

            logger.error("traceId={},authCode={},调用用户授权平台接口getAlipayAuthToken异常e={}",
                    traceId, authCode, LogExceptionWapper.getStackTrace(e));
            return null;
        }
    }

}
