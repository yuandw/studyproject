package com.gaoyang.marketing.mfbizweb.util.acm;/**
 * Created by zhanghui on 2018-10-24.
 */

import com.alibaba.edas.acm.ConfigService;
import com.alibaba.edas.acm.listener.PropertiesListener;
import com.gaoyang.aliyun.acm.agent.AbstractAcmServer;
import com.gaoyang.aliyun.acm.agent.AcmAgent;
import com.gaoyang.aliyun.acm.agent.AcmConfig;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * @author zhanghui
 * @create 2018-10-24
 * @description 刷新配置service
 */
@Component
public class MfbizWebAcmClient extends AbstractAcmServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MfbizWebAcmClient.class);

    @Autowired
    AcmConfig acmConfig;
    @Value("${alibaba.acm.dataId}")
    String dataId;
    @Value("${alibaba.acm.group}")
    String group;
    @PostConstruct
    @Override
    public void init() {
        AcmAgent.getInstance().init(acmConfig);
        // 配置更新监听
        ConfigService.addListener(dataId, group, new PropertiesListener() {
            @Override
            public void innerReceive(Properties properties) {
                acmProperties = properties;
            }
        });
    }
}
