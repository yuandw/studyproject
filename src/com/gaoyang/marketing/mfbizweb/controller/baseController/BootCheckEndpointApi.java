package com.gaoyang.marketing.mfbizweb.controller.baseController;/**
 * Created by zhanghui on 2018-12-12.
 */

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhanghui
 * @version 1.0
 * @date 2018/11/26 10:19
 * @className BootCheckEndpointApi
 * @desc 启动检查API
 */
@RestController
public class BootCheckEndpointApi {

    @RequestMapping("/healthz")
    public String bootCheckApi() {
        return "OK";
    }
}