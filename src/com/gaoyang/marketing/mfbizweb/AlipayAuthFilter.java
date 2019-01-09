package com.gaoyang.marketing.mfbizweb;/**
 * Created by zhanghui on 2018-9-25.
 */

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.proxy.DummyTraceIdUtil;
import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.service.baseService.QueryUserAuthService;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

/**
 * @author gongzelin
 * @create 2018-9-25
 * @description
 */
@WebFilter(urlPatterns = "/*", filterName = "userInfoFilter")
public class AlipayAuthFilter implements Filter {

    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");

    private FilterConfig config;

    private QueryUserAuthService queryUserAuthService;

    public AlipayAuthFilter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        DummyTraceIdUtil.createTraceId();
        String traceId = TraceIdUtil.getTraceId();
        try {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            HttpSession session = request.getSession();

            String sessionId = session.getId();
            Cookie[] cookieId = request.getCookies();
            String currentAccessUrl = request.getRequestURI();
            String requestIp = request.getRemoteAddr();
            if (logger.isInfoEnabled()) {
                logger.info("调用filter,sessionId={},requestIp={},requestUrl={},traceId={}", sessionId, requestIp, currentAccessUrl, traceId);
            }

            if ("/bookCreateGyMemeber".equals(currentAccessUrl)||"/healthz".equals(currentAccessUrl)) {

                filterChain.doFilter(request, response);
                return;
            }
            String authCode = null;
            String sourceId = null;
            //前端传入字段，用于区分是否切换用户
            String token = "";

            if (!StringUtils.isBlank(request.getParameter("authcode"))) {
                authCode = request.getParameter("authcode");
            }
            if (!StringUtils.isBlank(request.getParameter("sourceId"))) {
                sourceId = request.getParameter("sourceId");
            }
            //新增token 作为切换用户的标识
            if (!StringUtils.isBlank(request.getParameter("token"))) {
                token = request.getParameter("token");
            }
            Map<String, String> userInfo = (Map<String, String>) request.getSession().getAttribute("userInfo");
            /*
            * 1、缓存信息为空时，刷新缓存
            *2、缓存不为空 ，判断前端传入token标识，与缓存中token是否相等
            *       2.1相等时，
            *               2.1.1 非领卡请求，return
            *               2.1.2  领卡请求  ，刷authcode  , return
            *
            *      2.2 不等时（切换用户）
            *              是否领卡都刷authcode ,return
            *
            * */

            //领卡接口标识
            boolean isCreateMember = false;
            if ("/receive_member_card".equals(currentAccessUrl)||"/laXinCreateMemeber".equals(currentAccessUrl)) {
                isCreateMember = true;
            }
            //缓存为空
            if (null == userInfo || userInfo.isEmpty()) {

                Map map = refreshUserInfo(authCode, sourceId, traceId, isCreateMember);
                //兼容不传token情况
                if (!PublicUtil.isEmpty(map)&&!StringUtils.isBlank(request.getParameter("token"))) {
                    map.put("userIdentity", token);
                }
                if (logger.isInfoEnabled()) {
                    logger.info("调用filter,缓存为空,刷新authcode获取用户信息,sessionId={},traceId={},刷新用户信息={}", sessionId, traceId,JSON.toJSONString(map));
                }
                request.getSession().setAttribute("userInfo", map);
                //设置有效期
                session.setMaxInactiveInterval(300);
                filterChain.doFilter(request, response);
                return;
            } else {
                //缓存中信息非空，需要判断是否切换用户即前端传入token是否相等。
                if ((token.equals(userInfo.get("userIdentity")) && isCreateMember) || !token.equals(userInfo.get("userIdentity"))) {
                    Map map = refreshUserInfo(authCode, sourceId, traceId, isCreateMember);
                    if (!PublicUtil.isEmpty(map)&&!StringUtils.isBlank(request.getParameter("token"))) {
                        map.put("userIdentity", token);
                    }
                        request.getSession().setAttribute("userInfo", map);
                        //设置有效期
                        session.setMaxInactiveInterval(300);
                    if (logger.isInfoEnabled()) {
                        logger.info("调用filter,缓存非空,刷新authcode,获取用户信息,sessionId={},traceId={},前端传入userIdentity={},历史记录缓存中信息userInfo={},刷新用户信息={}",
                                sessionId, traceId, token, JSON.toJSONString(userInfo), JSON.toJSONString(map));
                    }
                        filterChain.doFilter(request, response);
                        return;


                }
                if (logger.isInfoEnabled()) {
                    logger.info("调用filter,同一个会话缓存非空,用户标识一致且非领卡接口,traceId={},前端传入userIdentity={},历史记录缓存中信息userInfo={}",
                            traceId, token, JSON.toJSONString(userInfo));
                }
                filterChain.doFilter(request, response);
                return;

            }

        } catch (Exception e) {
            logger.error("filter获取用户信息异常,exception={},traceId={}", LogExceptionWapper.getStackTrace(e), traceId);
            filterChain.doFilter(servletRequest, servletResponse);
            e.printStackTrace();
            return;
        }

    }

    /**
     * @desc 根据authcode刷新用户信息
     */
    private Map refreshUserInfo(String authCode, String sourceId, String traceId, boolean isCreateMember) {
        Map map = queryUserAuthService.getUserAuth(authCode, isCreateMember, sourceId);
        if (!PublicUtil.isEmpty(map)) {
            return map;

        } else if (isCreateMember && PublicUtil.isEmpty(map)) {
            //创建会员卡 获取用户信息时失败，打印异常日志
            if (isCreateMember) {
                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("muserbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("领卡");
                busiMonitorLoggerBean.setError_desc("领卡异常");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);
            }
            logger.warn("调用filter，缓存为空，领取会员卡刷新authcode，获取用户信息失败,traceId={},authCode={}", traceId,authCode);
            return null;
        } else {
            //其他情况刷新authcode失败
            logger.warn("调用filter,缓存为空,获取用户信息失败,traceId={},authCode={}", traceId,authCode);
            return null;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub
        logger.info("init filter");
        //queryUserAuthService注入
        ServletContext sc = filterConfig.getServletContext();
        WebApplicationContext cxt = WebApplicationContextUtils.getWebApplicationContext(sc);
        if (cxt != null && cxt.getBean("queryUserAuthService") != null && queryUserAuthService == null) {
            queryUserAuthService = (QueryUserAuthService) cxt.getBean("queryUserAuthService");
        }
    }


}
