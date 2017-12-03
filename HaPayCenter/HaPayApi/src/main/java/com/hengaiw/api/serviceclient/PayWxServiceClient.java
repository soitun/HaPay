package com.hengaiw.api.serviceclient;

import com.hengaiw.pub.utils.HaBase64;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class PayWxServiceClient {

    @Autowired
    RestTemplate restTemplate;
    /**
     * 处理微信支付
     * @param jsonParam
     * @return
     */
    @HystrixCommand(fallbackMethod = "doWxUnifiedOrderReqFallback")
    public String doWxUnifiedOrderReq(String jsonParam) {
        return restTemplate.getForEntity("http://HAPAYWXSERVICE/pay/wx/unifiedOrder?jsonParam=" + HaBase64.encode(jsonParam.getBytes()), String.class).getBody();
    }

    public String doWxUnifiedOrderReqFallback(String jsonParam) {
        return "error";
    }
    /**
     * 处理微信退款
     * @param jsonParam
     * @return
     */
    @HystrixCommand(fallbackMethod = "doWxRefundOrderReqFallback")
    public String doWxRefundOrderReq(String jsonParam) {
        return restTemplate.getForEntity("http://HAPAYWXSERVICE/pay/wx/refundOrder?jsonParam=" + HaBase64.encode(jsonParam.getBytes()), String.class).getBody();
    }

    public String doWxRefundOrderReqFallback(String jsonParam) {
        return "error";
    }

    /**
     * 获取对帐单
     * @param jsonParam
     * @return
     */
    @HystrixCommand(fallbackMethod = "doWxBillReqFallback")
    public String doWxBillReq(String jsonParam) {
        return restTemplate.getForEntity("http://HAPAYWXSERVICE/pay/wx/bill?jsonParam=" + HaBase64.encode(jsonParam.getBytes()), String.class).getBody();
    }

    public String doWxBillReqFallback(String jsonParam) {
        return "error";
    }
}