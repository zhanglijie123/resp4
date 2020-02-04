package cn.sunline.icore.dp.serv.common;

import java.math.BigDecimal;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.common.util.DateUtil;
import cn.sunline.icore.ap.api.ApChannelApi;
import cn.sunline.icore.ap.api.ApMailApi;
import cn.sunline.icore.ap.type.ComApMail.ApAssembleEmailInfo;
import cn.sunline.icore.ap.type.ComApMail.ApMailAttachement;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明：
 * </p>
 * 
 * @Author shenxy
 *         <p>
 *         <li>2017年12月7日-下午1:23:39</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：登记邮件信息</li>
 *         <li>2017年12月7日-shenxy：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpNotice {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpNotice.class);
	
	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年12月13日-上午9:44:45</li>
	 *         <li>功能说明：登记邮件信息</li>
	 *         <li>使用说明：根据邮件模版编号登记邮件信息</li>
	 *         </p>
	 * @param mailTemplateNo
	 *            邮件模版编号
	 * @param mailData
	 *            邮件内容Map数据
	 * @param attachListInfo
	 *            附件，可不传
	 */
	public static void registerMailInfoByTemplateNo(String mailTemplateNo, Map<String, Object> mailData, Options<ApMailAttachement> attachListInfo) {
		
		bizlog.method(" DpRegisterMailInfo.registerMailInfoByTemplateNo begin <<<<<<<<<<<<<<<<");
		
		if (CommUtil.isNull(mailTemplateNo)) {

			bizlog.error("Template No is empty, trxn sequence No. [%s]", BizUtil.getTrxRunEnvs().getTrxn_seq());
			bizlog.method(" DpRegisterMailInfo.registerMailInfoByTemplateNo end <<<<<<<<<<<<<<<<");
			return;
		}
		
		// 加载数据集
		E_YESORNO regFlag = initMailData(mailData);
		
		if(regFlag == E_YESORNO.NO){
			bizlog.method(" DpRegisterMailInfo.registerMailInfoByTemplateNo end <<<<<<<<<<<<<<<<");
			return;
		}

		// 登记邮件
		ApAssembleEmailInfo mailInfo = BizUtil.getInstance(ApAssembleEmailInfo.class);

		mailInfo.setAttachListInfo(attachListInfo);
		mailInfo.setMail_template_no(mailTemplateNo);
		mailInfo.setE_mail((String) mailData.get(SysDict.A.e_mail.getId()));
		mailInfo.setCust_no((String) mailData.get(SysDict.A.cust_no.getId()));
		mailInfo.setMailData(mailData); 

		ApMailApi.registerMailInfo(mailInfo);

		bizlog.method(" DpRegisterMailInfo.registerMailInfoByTemplateNo end <<<<<<<<<<<<<<<<");
	
	}
	
	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年12月13日-上午9:44:45</li>
	 *         <li>功能说明：登记邮件信息</li>
	 *         <li>使用说明：根据交易事件编号登记邮件信息</li>
	 *         </p>
	 * @param trxnEventId
	 *            交易事件编号
	 * @param mailData
	 *            邮件内容Map数据
	 * @param attachListInfo
	 *            附件，可不传
	 */
	public static void registerMailInfoByTrxnEventId(String trxnEventId, Map<String, Object> mailData, Options<ApMailAttachement> attachListInfo) {

		bizlog.method(" DpRegisterMailInfo.registerMailInfotrxnEventId begin <<<<<<<<<<<<<<<<");

		initMailData(mailData);

		// 登记邮件
		ApAssembleEmailInfo mailInfo = BizUtil.getInstance(ApAssembleEmailInfo.class);

		mailInfo.setAttachListInfo(attachListInfo);
		mailInfo.setTrxn_event_id(trxnEventId);
		mailInfo.setE_mail((String) mailData.get(SysDict.A.e_mail.getId()));
		mailInfo.setCust_no((String) mailData.get(SysDict.A.cust_no.getId()));
		mailInfo.setMailData(mailData); 

		ApMailApi.registerMailInfo(mailInfo);

		bizlog.method(" DpRegisterMailInfo.registerMailInfotrxnEventId end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年12月13日-上午9:44:45</li>
	 *         <li>功能说明：登记邮件信息初始处理</li>
	 *         </p>
	 * @param mailData
	 *            邮件内容Map数据
	 * @return 登记邮件信息标志   
	 */
	private static E_YESORNO initMailData(Map<String, Object> mailData) {
		
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 邮件内容数据区
		if (CommUtil.isNull(mailData)) {

			bizlog.error("Mail data is empty, trxn sequence No. [%s]", runEnvs.getTrxn_seq());
			return E_YESORNO.NO;
		}

		// 没找到邮件地址key
		if (!mailData.containsKey(SysDict.A.e_mail.getId())) {

			bizlog.error("Missing email address field, trxn sequence No. [%s]", runEnvs.getTrxn_seq());
			return E_YESORNO.NO;
		}

		// 没找到客户号key
		if (!mailData.containsKey(SysDict.A.cust_no.getId())) {

			bizlog.error("Missing customer id field, trxn sequence No. [%s]", runEnvs.getTrxn_seq());
			return E_YESORNO.NO;
		}

		// 客户没有注册邮件地址
		if (CommUtil.isNull(mailData.get(SysDict.A.e_mail.getId()))) {

			bizlog.error("Customers[%s] have no e_mail number", (String) mailData.get(SysDict.A.cust_no.getId()));
			return E_YESORNO.NO;
		}

		if (CommUtil.isNull(runEnvs.getDeduct_chrg_amt())) {
			runEnvs.setDeduct_chrg_amt(BigDecimal.ZERO);
		}

		// 将公共运行区数据加入邮件数据
		mailData.putAll(CommUtil.toMap(runEnvs));

		// 增加日期时间
		mailData.put("date_time", DateUtil.formatDate(DateUtil.parseDate(BizUtil.getComputerDateTime(), "yyyyMMdd HH:mm"), "dd-MM-yyyy HH:mm"));

		// 交易渠道描述
		mailData.put(SysDict.A.channel_desc.getId(), ApChannelApi.getChannel(runEnvs.getChannel_id()).getChannel_desc());
	
		return E_YESORNO.YES;
	}
}
