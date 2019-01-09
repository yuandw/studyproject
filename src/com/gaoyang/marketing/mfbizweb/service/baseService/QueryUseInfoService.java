package com.gaoyang.marketing.mfbizweb.service.baseService;/**
 * Created by zhanghui on 2018-9-20.
 */


import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.maccbasecore.facade.AccountBalanceBizServiceFacade;
import com.gaoyang.marketing.maccbasecore.facade.AccountBizServiceFacade;
import com.gaoyang.marketing.maccbasecore.facade.model.ResponseData;
import com.gaoyang.marketing.maccbasecore.facade.model.request.ClearAccountData;
import com.gaoyang.marketing.maccbasecore.facade.model.request.CreateAccountData;
import com.gaoyang.marketing.maccbasecore.facade.model.request.QueryAccountData;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutClearAccountModel;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutCreateAccountModel;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutQueryAccountModel;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.bean.UserInfoBean;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.UserInfoFacade;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author zhanghui
 * @create 2018-9-20
 * @description获取用户信息service
 */
@Service
public class QueryUseInfoService {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource(name = "accountBizServiceFacade")
    AccountBizServiceFacade accountBizServiceFacade;

    @Resource(name = "userInfoFacade")
    UserInfoFacade userInfoFacade;

    @Resource(name = "accountBalanceBizServiceFacade")
    AccountBalanceBizServiceFacade accountBalanceBizServiceFacade;

    @Resource(name="queryUserAuthService")
    QueryUserAuthService queryUserAuthService;

    /**
     * @param  uid,userId,gyMemberId
     * @return
     * @desc 调用用户接口查询用户信息
     */
    public HttpResponseBean getUserInfo(String uid,String userId,String gyMemberId,String moblie) {
        String traceId= TraceIdUtil.getTraceId();
        HttpResponseBean httpResponseBean = new HttpResponseBean();
        UserInfoBean userInfoBean = new UserInfoBean();
        try {
            //判断是否有账户，有账户查询账户信息，没有账户先创建账户
            //调用帐号数据平台，查询账户信息
            ResponseData<com.gaoyang.marketing.maccbasecore.facade.model.response.OutQueryAccountModel> responseData = queryAccountInfo(userId);
            if(PublicUtil.isEmpty(responseData)){
                httpResponseBean.setCode(ResponseEnums.TYPE_USERINFO_EXCEPYION.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_USERINFO_EXCEPYION.getDesc());
                httpResponseBean.setData(userInfoBean);
                logger.warn("调用账户数据平台接口queryAccountInfo返回responseData对象为空 traceId={},userId={}",traceId,userId);
                return httpResponseBean;
            }
            userInfoBean.setPhone(moblie);
            userInfoBean.setMciGyId(gyMemberId);
            String code=responseData.getCode();
            //账户存在并查询成功
            if(ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)){
                httpResponseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
                OutQueryAccountModel outQueryAccountModel=responseData.getObj();
                //查询成功先判断是否月初需要清零
                String currMonth=outQueryAccountModel.getCurrMonth();
                if (currMonth == null) {
                    logger.warn("调用账户数据平台接口queryAccountInfo返回curr_month为null，请关注！traceId={}, userId={}", traceId, userId);
                    return null;
                }
                // 获取当前真实月份
                SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
                String realCurrMonthStr =format.format(new Date(System.currentTimeMillis()));
                if (logger.isInfoEnabled()) {
                    logger.info("判断是否需要清零操作开始, traceId={},userId={},记录的月份={},当前实际月份={}",
                            traceId, userId, currMonth, realCurrMonthStr);
                }
                // 转换为月份进行比较
                int dbCurrMonthNum = Integer.parseInt(currMonth.substring(0, 4)) * 12 +
                        Integer.parseInt(currMonth.substring(4, 6));
                int realCurrMonthNum = Integer.parseInt(realCurrMonthStr.substring(0, 4)) * 12 +
                        Integer.parseInt(realCurrMonthStr.substring(4, 6));
                /**
                 *  根据【差值 = 实际月数-记录月数】进行判断是否进行清零操作
                 *  差值大于0 则需要清零操作
                 */
                int monthDiff = realCurrMonthNum - dbCurrMonthNum;
                if(monthDiff>0){
                    if (logger.isInfoEnabled()) {
                        logger.info("计算当前月份年份差值不为0,需要进行清零操作，traceId={}, userId={},记录的月份={},当前实际月份={}", traceId, userId, currMonth, realCurrMonthStr);
                    }
                    OutClearAccountModel outClearAccountModel=clearAccount(userId);
                    //判断调用清零操作是否成功
                    if(PublicUtil.isEmpty(outClearAccountModel)){
                        httpResponseBean.setCode(ResponseEnums.TYPE_USERINFO_EXCEPYION.getCode());
                        httpResponseBean.setMsg(ResponseEnums.TYPE_USERINFO_EXCEPYION.getDesc());
                        httpResponseBean.setData(userInfoBean);
                        logger.warn("调用账户数据平台接口clearAccount清零操作失败 traceId={},userId={}",traceId,userId);
                        return httpResponseBean;
                    }
                    //账户余额
                    if(!PublicUtil.isEmpty(outClearAccountModel.getBalance())){
                        userInfoBean.setBalance(outClearAccountModel.getBalance().toPlainString());
                    }else{
                        userInfoBean.setBalance("0");
                    }
                    if(!PublicUtil.isEmpty(outClearAccountModel.getTradeTimesCur())){
                        userInfoBean.setTradeTimesCur(outClearAccountModel.getTradeTimesCur().toString());
                    }else{
                        userInfoBean.setTradeTimesCur("0");
                    }
                    if(!PublicUtil.isEmpty(outClearAccountModel.getBalanceExchanged())){
                        userInfoBean.setTradeAmountCur(outClearAccountModel.getBalanceExchanged().toPlainString());
                    }else{
                        userInfoBean.setTradeAmountCur("0");
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("调用账户数据平台接口clearAccount清零成功 traceId={},userId={},balance={},tradeTimesCur={},balanceExchanged={}",
                                traceId, userId, outClearAccountModel.getBalance(), outClearAccountModel.getTradeTimesCur(), outClearAccountModel.getBalanceExchanged());
                    }
                }else{
                    if(!PublicUtil.isEmpty(outQueryAccountModel.getBalance())){
                        userInfoBean.setBalance(outQueryAccountModel.getBalance().toPlainString());
                    }else{
                        userInfoBean.setBalance("0");
                    }
                    if(!PublicUtil.isEmpty(outQueryAccountModel.getTradeTimesCur())){
                        userInfoBean.setTradeTimesCur(outQueryAccountModel.getTradeTimesCur().toString());
                    }else{
                        userInfoBean.setTradeTimesCur("0");
                    }
                    if(!PublicUtil.isEmpty(outQueryAccountModel.getBalanceExchanged())){
                        userInfoBean.setTradeAmountCur(outQueryAccountModel.getBalanceExchanged().toPlainString());
                    }else{
                        userInfoBean.setTradeAmountCur("0");
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("调用账户数据平台接口queryAccountInfo成功 traceId={},userId={},balance={},tradeTimesCur={},balanceExchanged={}",
                                traceId, userId, outQueryAccountModel.getBalance(), outQueryAccountModel.getTradeTimesCur(), outQueryAccountModel.getBalanceExchanged());
                    }
                }
            }else if(ResponseEnums.RPC_TYPE_QUERYACCOUNTINFO_FAIL.getCode().equals(code)){
                //查询账户信息返回40007，账户不存在需要创建账户。
                if (logger.isInfoEnabled()) {
                    logger.info("调用账户数据平台接口queryAccountInfo查询帐户为空，需要创建账户,traceId={},userId={},code={}", traceId, userId, code);
                }
              OutCreateAccountModel outCreateAccountModel=createAccountInfo(userId);
              if(PublicUtil.isEmpty(outCreateAccountModel)){
                  httpResponseBean.setCode(ResponseEnums.TYPE_USERINFO_EXCEPYION.getCode());
                  httpResponseBean.setMsg(ResponseEnums.TYPE_USERINFO_EXCEPYION.getDesc());
                  httpResponseBean.setData(userInfoBean);
                  logger.warn("调用账户数据平台接口createAccountInfo返回对象为空 traceId={},userId={}",traceId,userId);
                  return httpResponseBean;
              }
             if(!PublicUtil.isEmpty(outCreateAccountModel.getBalance())){
                  userInfoBean.setBalance(outCreateAccountModel.getBalance().toPlainString());
              }
                //创建帐户后将账户信息返回给前端 ，不需要再次查询账户信息
              if(!PublicUtil.isEmpty(outCreateAccountModel.getTradeTimesCur())){
                  userInfoBean.setTradeTimesCur(outCreateAccountModel.getTradeTimesCur().toString());
              }
              if(!PublicUtil.isEmpty(outCreateAccountModel.getBalanceExchanged())){
                  userInfoBean.setTradeAmountCur(outCreateAccountModel.getBalanceExchanged().toPlainString());
              }
                if (logger.isInfoEnabled()) {
                    logger.info("调用账户数据平台接口queryAccountInfo成功 traceId={},userId={},balance={},tradeTimesCur={},balanceExchanged={}",
                            traceId, userId, outCreateAccountModel.getBalance(), outCreateAccountModel.getTradeTimesCur(), outCreateAccountModel.getBalanceExchanged());
                }
          }else{
              httpResponseBean.setCode(ResponseEnums.TYPE_USERINFO_EXCEPYION.getCode());
              httpResponseBean.setMsg(ResponseEnums.TYPE_USERINFO_EXCEPYION.getDesc());
              httpResponseBean.setData(userInfoBean);
              logger.warn("调用账户数据平台接口queryAccountInfo查询失败 traceId={},userId={},code={}",traceId,userId,code);
              return httpResponseBean;
          }
            userInfoBean.setTradeTimesTotal("10");
            userInfoBean.setTradeAmountTotal("10G");
            httpResponseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_SUCCESS.getDesc());
            httpResponseBean.setData(userInfoBean);
            if (logger.isInfoEnabled()) {
                logger.info("用户信息查询接口service,traceId={},userId={}", traceId, userId);
            }
            return httpResponseBean;

        } catch (Exception e) {
            httpResponseBean.setData(ResponseEnums.TYPE_USERINFO_EXCEPYION.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_USERINFO_EXCEPYION.getDesc());
            logger.error("traceId={},userId={},用户信息查询接口异常e={}",
                    traceId,userId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
            return httpResponseBean;
        }

    }

    /**
     * @param userId
     * @return
     * @desc 调用帐户数据平台接口  查询账户信息
     */
    public ResponseData queryAccountInfo(String userId) {
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用账户数据平台接口queryAccountInfo,traceId={},userId={}", traceId, userId);
        }
        try {
            QueryAccountData queryAccountData = new QueryAccountData();
            queryAccountData.setUserId(userId);
            ResponseData<com.gaoyang.marketing.maccbasecore.facade.model.response.OutQueryAccountModel> responseData = accountBizServiceFacade.queryAccountInfoByUserId(queryAccountData);
            if (logger.isInfoEnabled()) {
                logger.info("调用账户数据平台接口queryAccountInfo,traceId={},userId={},responseData={}", traceId, userId, JSONObject.toJSONString(responseData));
            }
            return responseData;
        } catch (Exception e) {
            logger.error("traceId={},userId={},调用账户数据平台接口queryAccountInfo异常e={}",
                    traceId,userId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
        }
            return null;
    }
    public OutCreateAccountModel createAccountInfo(String userId) {
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用账户数据平台接口createAccountInfo,traceId={},userId={}", traceId, userId);
        }
        try {
            CreateAccountData createAccountData = new CreateAccountData();
            createAccountData.setUserId(userId);
            ResponseData<com.gaoyang.marketing.maccbasecore.facade.model.response.OutCreateAccountModel> responseData = accountBizServiceFacade.createAccountInfo(createAccountData);
            if(!PublicUtil.isEmpty(responseData)) {
                String code = responseData.getCode();
                //判断接口返回code
                if (!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)) {
                    logger.warn("调用用户数据平台接口createAccountInfo查询失败,traceId={},userId={},code={}", traceId, userId, code);
                    return null;
                }
                OutCreateAccountModel outCreateAccountModel = responseData.getObj();
                if (logger.isInfoEnabled()) {
                    logger.info("调用账户数据平台接口createAccountInfo查询成功,traceId={},userId={},responseData={}", traceId, userId, JSONObject.toJSONString(responseData));
                }
                return outCreateAccountModel;
            }
        } catch (Exception e) {
            logger.error("traceId={},userId={},调用账户数据平台接口createAccountInfo异常e={}",
                    traceId,userId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @desc  清零操作
     * @param userId
     * @return
     */
    public OutClearAccountModel clearAccount(String userId) {
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用账户数据平台接口createAccountInfo,traceId={},userId={}", traceId, userId);
        }
        try {
            ClearAccountData clearAccountData = new ClearAccountData();
            clearAccountData.setUserId(userId);
            ResponseData<OutClearAccountModel> responseData=accountBizServiceFacade.clearAccount(clearAccountData);
            if(!PublicUtil.isEmpty(responseData)) {
                String code = responseData.getCode();
                //判断接口返回code
                if (!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)) {
                    logger.warn("调用用户数据平台接口clearAccount清零失败,traceId={},userId={},code={}", traceId, userId, code);
                    return null;
                }
                OutClearAccountModel outClearAccountModel = responseData.getObj();
                if (logger.isInfoEnabled()) {
                    logger.info("调用账户数据平台接口clearAccount清零成功,traceId={},userId={},responseData={}", traceId, userId, JSONObject.toJSONString(responseData));
                }
                return outClearAccountModel;
            }
        } catch (Exception e) {
            logger.error("traceId={},userId={},调用账户数据平台接口clearAccount清零异常e={}",
                    traceId,userId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
        }
        return null;
    }

}
