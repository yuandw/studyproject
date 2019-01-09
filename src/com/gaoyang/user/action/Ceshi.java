package com.gaoyang.user.action;

import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;

/**
 * @Auther: yuandw
 * @Date: 2018-12-28 09:12
 * @Description:
 */
public class Ceshi {
    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;


    String poioutId=mfbizWebAcmClient.getPorpertiesValue("poiId_"+poiId);
}
