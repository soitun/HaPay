package com.hengaiw.service.channel.payeco.client;

import java.net.URLEncoder;

import com.hengaiw.service.channel.payeco.tools.Base64;
import com.hengaiw.service.channel.payeco.tools.Log;
import com.hengaiw.service.channel.payeco.tools.PayEcoTools;
import com.hengaiw.service.channel.payeco.tools.Xml;
import com.hengaiw.service.channel.payeco.tools.http.HttpClient;
import com.hengaiw.service.channel.payeco.tools.rsa.Signatory;
/**
 * 【短信+H5页面版本】商户对接下单接口封装
 * 易联服务器交易接口调用API封装，分别对以下接口调用进行了封装；
 * 接口封装了参数的转码（中文base64转码）、签名和验证签名、通讯和通讯报文处理
 * 1、【短信+H5页面版本】的商户订单下单接口
 * 2、【短信+H5页面版本】的重新发送支付短信接口
 */
public class TransactionClientSmsH5 {
    /**
     * 商户订单下单接口(短信+H5页面版本)，本接口比SDK的下单接口增加了【returnUrl】参数
     * @param merchantId        商户代码
     * @param merchOrderId      商户订单号
     * @param amount            商户订单金额，单位为元，格式： nnnnnn.nn
     * @param orderDesc         商户订单描述    字符最大128，中文最多40个；参与签名：采用UTF-8编码
     * @param tradeTime         商户订单提交时间，格式：yyyyMMddHHmmss，超过订单超时时间未支付，订单作废；不提交该参数，采用系统的默认时间（从接收订单后超时时间为30分钟）
     * @param expTime           交易超时时间，格式：yyyyMMddHHmmss， 超过订单超时时间未支付，订单作废；不提交该参数，采用系统的默认时间（从接收订单后超时时间为30分钟）
     * @param notifyUrl         异步通知URL
     * @param returnUrl         同步通知URL
     * @param extData           商户保留信息； 通知结果时，原样返回给商户；字符最大128，中文最多40个；参与签名：采用UTF-8编码
     * @param miscData          订单扩展信息   根据不同的行业，传送的信息不一样；参与签名：采用UTF-8编码
     * @param notifyFlag        订单通知标志    0：成功才通知，1：全部通知（成功或失败）  不填默认为“1：全部通知”
     * @param mercPriKey        商户签名的私钥
     * @param payecoPubKey      易联签名验证公钥
     * @param payecoUrl         易联服务器URL地址，只需要填写域名部分
     * @param retXml            通讯返回数据；当不是通讯错误时，该对象返回数据
     * @return  处理状态码： 0000 : 处理成功， 其他： 处理失败
     * @throws Exception        E101:通讯失败； E102：签名验证失败；  E103：签名失败；
     */
    public static String MerchantOrderSmsH5Pay(String merchantId, String merchOrderId,
            String amount, String orderDesc, String tradeTime, String expTime,
            String notifyUrl, String returnUrl, String extData,
            String miscData, String notifyFlag, String mercPriKey,
            String payecoPubKey, String payecoUrl, Xml retXml) throws Exception {
		//交易参数
		String tradeCode = "SmsPayOrder";
		String version = ConstantsClient.COMM_INTF_VERSION;
		
		//进行数据签名  
		String signData = "Version="+version+"&MerchantId=" + merchantId + "&MerchOrderId=" + merchOrderId 
				+ "&Amount=" + amount + "&OrderDesc=" + orderDesc + "&TradeTime=" + tradeTime + "&ExpTime="
				+ expTime + "&NotifyUrl=" + notifyUrl + "&ReturnUrl=" + returnUrl + "&ExtData=" + extData
				+ "&MiscData=" + miscData + "&NotifyFlag=" + notifyFlag;
		
		// 私钥签名
		Log.println("PrivateKey = " + mercPriKey);
		Log.println("SignData = " + signData);
		String sign = Signatory.sign(mercPriKey, signData, ConstantsClient.PAYECO_DATA_ENCODE);
		if(PayEcoTools.isStrEmpty(sign)){
			throw new Exception("E103");
		}
		Log.println("sign=" + sign);

		//提交参数包含中文的需要做base64转码
		String orderDesc64 = Base64.encodeBytes(orderDesc.getBytes(ConstantsClient.PAYECO_DATA_ENCODE));
		String extData64 = Base64.encodeBytes(extData.getBytes(ConstantsClient.PAYECO_DATA_ENCODE));
		String miscData64 = Base64.encodeBytes(miscData.getBytes(ConstantsClient.PAYECO_DATA_ENCODE));
		//通知地址做URLEncoder处理
		String notifyUrlEn = URLEncoder.encode(notifyUrl, ConstantsClient.PAYECO_DATA_ENCODE);
		String returnUrlEn = URLEncoder.encode(returnUrl, ConstantsClient.PAYECO_DATA_ENCODE);
		
		String data64 = "Version="+version+"&MerchantId=" + merchantId + "&MerchOrderId=" + merchOrderId 
                    + "&Amount=" + amount + "&OrderDesc=" + orderDesc64 + "&TradeTime=" + tradeTime + "&ExpTime="
                    + expTime + "&NotifyUrl=" + notifyUrlEn + "&ReturnUrl=" + returnUrlEn + "&ExtData=" + extData64
                    + "&MiscData=" + miscData64 + "&NotifyFlag=" + notifyFlag;

		//通讯报文
		String url= payecoUrl + "/ppi/merchant/itf.do"; //下订单URL
		data64 = "TradeCode=" + tradeCode + "&" + data64 + "&Sign=" + sign;
		HttpClient httpClient = new HttpClient();
		Log.println("url = " + url + "?" + data64);
		String retStr = httpClient.send(url, data64, ConstantsClient.PAYECO_DATA_ENCODE, ConstantsClient.PAYECO_DATA_ENCODE,
				ConstantsClient.CONNECT_TIME_OUT, ConstantsClient.RESPONSE_TIME_OUT);
		Log.println("retStr="+retStr);
		if(PayEcoTools.isStrEmpty(retStr)){
			throw new Exception("E101");
		}

		//返回数据的返回码判断
		retXml.setXmlData(retStr);
		String retCode = PayEcoTools.getXMLValue(retStr, "retCode");
		retXml.setRetCode(retCode);
		retXml.setRetMsg(PayEcoTools.getXMLValue(retStr, "retMsg"));
		if(!"0000".equals(retCode)){
			return retCode;
		}
		//获取返回数据
		String retVer = PayEcoTools.getXMLValue(retStr, "Version");
		String retMerchantId = PayEcoTools.getXMLValue(retStr, "MerchantId");
		String retMerchOrderId = PayEcoTools.getXMLValue(retStr, "MerchOrderId");
		String retAmount = PayEcoTools.getXMLValue(retStr, "Amount");
		String retTradeTime = PayEcoTools.getXMLValue(retStr, "TradeTime");
		String retOrderId = PayEcoTools.getXMLValue(retStr, "OrderId");
		String retSign = PayEcoTools.getXMLValue(retStr, "Sign");
		
		//设置返回数据
		retXml.setTradeCode(tradeCode);
		retXml.setVersion(retVer);
		retXml.setMerchantId(retMerchantId);
		retXml.setMerchOrderId(retMerchOrderId);
		retXml.setAmount(retAmount);
		retXml.setTradeTime(tradeTime);
		retXml.setOrderId(retOrderId);
		retXml.setSign(retSign);
		
		//验证签名的字符串
		String backSign = "Version="+retVer+"&MerchantId=" + retMerchantId + "&MerchOrderId=" + retMerchOrderId 
				+ "&Amount=" + retAmount + "&TradeTime=" + retTradeTime + "&OrderId=" + retOrderId;
		//验证签名
		retSign = retSign.replaceAll(" ", "+");
		boolean b = Signatory.verify(payecoPubKey, backSign, retSign, ConstantsClient.PAYECO_DATA_ENCODE);
		Log.println("PublicKey=" + payecoPubKey);
		Log.println("data=" + backSign);
		Log.println("Sign=" + retSign);
		Log.println("验证结果=" + b);
		if(!b){
			throw new Exception("E102");
		}
		return retCode;
	}

    
	/**
	 * 重新发送支付短信接口
	 * @param merchantId:		商户代码
	 * @param merchOrderId	:	商户订单号
	 * @param tradeTime		:	商户订单提交时间
	 * @param priKey		:	商户签名的私钥
	 * @param pubKey        :   易联签名验证公钥
	 * @param payecoUrl		：	易联服务器URL地址，只需要填写域名部分
	 * @param retXml        :   通讯返回数据；当不是通讯错误时，该对象返回数据
	 * @return 				: 处理状态码： 0000 : 处理成功， 其他： 处理失败
	 * @throws Exception    :  E101:通讯失败； E102：签名验证失败；  E103：签名失败；
	 */
	public static String OrderSmsSendAgain(String merchantId, String merchOrderId, String tradeTime, 
			String priKey, String pubKey, String payecoUrl, Xml retXml) 
			throws Exception{
		//交易参数
		String tradeCode = "SmsSendAgain";
		String version = ConstantsClient.COMM_INTF_VERSION;
		
	    //进行数据签名
	    String signData = "Version="+version+"&MerchantId=" + merchantId + "&MerchOrderId=" + merchOrderId 
	             + "&TradeTime=" + tradeTime;
	    
	    // 私钥签名
		Log.println("PrivateKey=" + priKey);
		Log.println("data=" + signData);
	    String sign = Signatory.sign(priKey, signData, ConstantsClient.PAYECO_DATA_ENCODE);
		if(PayEcoTools.isStrEmpty(sign)){
			throw new Exception("E103");
		}
		Log.println("sign=" + sign);

		//通讯报文
	    String url= payecoUrl + "/ppi/merchant/itf.do?TradeCode="+tradeCode; //查询订单URL
	    signData = signData + "&Sign=" + sign;
	    HttpClient httpClient = new HttpClient();
	    Log.println("url="+url+"&"+signData);
		String retStr = httpClient.send(url, signData, ConstantsClient.PAYECO_DATA_ENCODE, ConstantsClient.PAYECO_DATA_ENCODE,
				ConstantsClient.CONNECT_TIME_OUT, ConstantsClient.RESPONSE_TIME_OUT);
		Log.println("retStr="+retStr);
		if(PayEcoTools.isStrEmpty(retStr)){
			throw new Exception("E101");
		}

		//返回数据的返回码判断
		retXml.setXmlData(retStr);
		String retCode = PayEcoTools.getXMLValue(retStr, "retCode");
		retXml.setRetCode(retCode);
		retXml.setRetMsg(PayEcoTools.getXMLValue(retStr, "retMsg"));
		if(!"0000".equals(retCode)){
			return retCode;
		}
		//获取返回数据
		String retVer = PayEcoTools.getXMLValue(retStr, "Version");
		String retMerchantId = PayEcoTools.getXMLValue(retStr, "MerchantId");
		String retMerchOrderId = PayEcoTools.getXMLValue(retStr, "MerchOrderId");
		String retSendNum = PayEcoTools.getXMLValue(retStr, "SendNum");
		String retTradeTime = PayEcoTools.getXMLValue(retStr, "TradeTime");
		String retSign = PayEcoTools.getXMLValue(retStr, "Sign");
		//设置返回数据
		retXml.setTradeCode(tradeCode);
		retXml.setVersion(retVer);
		retXml.setMerchantId(retMerchantId);
		retXml.setMerchOrderId(retMerchOrderId);
		retXml.setSendNum(retSendNum);
		retXml.setTradeTime(retTradeTime);
		retXml.setSign(retSign);
		  
		//验证签名的字符串
		String backSign = "Version="+retVer+"&MerchantId=" + retMerchantId + "&MerchOrderId=" + retMerchOrderId 
		  + "&SendNum=" + retSendNum + "&TradeTime="+retTradeTime;

		//验证签名
		retSign = retSign.replaceAll(" ", "+");
		boolean b = Signatory.verify(pubKey, backSign, retSign, ConstantsClient.PAYECO_DATA_ENCODE);
		Log.println("PublicKey=" + pubKey);
		Log.println("data=" + backSign);
		Log.println("Sign=" + retSign);
		Log.println("验证结果=" + b);
		if(!b){
			throw new Exception("E102");
		}
		return retCode;
	}
	

}
