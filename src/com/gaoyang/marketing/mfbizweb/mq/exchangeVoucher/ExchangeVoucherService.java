package com.gaoyang.marketing.mfbizweb.mq.exchangeVoucher;/**
 * Created by zhanghui on 2018-12-5.
 */

import org.springframework.stereotype.Component;

/**
 * @author zhanghui
 * @create 2018-12-5
 * @description 特殊记账  发送事务消息卡券核销
 */
@Component
public class ExchangeVoucherService {
    /*@Autowired
    TransactionProducerAgent transactionProducerAgent;

    @Resource(name = "exchangeVoucherChecker")
    ExchangeVoucherChecker exchangeVoucherChecker;

    @Value("${rocketmq.env}")
    String msgQueueEnv;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeVoucherService.class);
    @PostConstruct
    public void init() {
        // 初始化领取会员卡赠送流量提供者
        transactionProducerAgent.init(
                exchangeVoucherChecker,
                MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.EXCHANGE_VOUCHER_PROTOCOL).getProducerId()).start();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[用户特殊记账并核销卡券服务]-卡券核销事务消息[生产者]specialChargeAccProducerAgent初始化完成,当前环境为-{}", msgQueueEnv);
        }

    }*/
}
