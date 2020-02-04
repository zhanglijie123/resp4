package cn.sunline.icore.dp.serv.iobus;

import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitTrialInfo;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpCreditLimitIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCreditLimitIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：额度信息查询</li>
	 *         </p>
	 * @param quotaCode
	 *            额度代码
	 * @return 额度信息
	 */
	public static DpCreditLimitInfo getCreditLimitInfo(String quotaCode) {

		// IoClAccountInfo cplLimitInfo = SysUtil.getRemoteInstance(SrvIoClLimitQry.class).queryLimitInfo(quotaCode);

		DpCreditLimitInfo cplOut = BizUtil.getInstance(DpCreditLimitInfo.class);

		// cplOut.setDue_date(cplLimitInfo.getDue_date());
		// cplOut.setStatus(cplLimitInfo.getStatus());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取额度试算信息</li>
	 *         </p>
	 * @param quotaCode
	 *            额度代码
	 * @return 额度信息
	 */
	public static DpCreditLimitTrialInfo getCreditLimitTrialInfo(String quotaCode, String ccyCode) {

	    //IoClLimitTrialIn cplLimitQryIn = BizUtil.getInstance(IoClLimitTrialIn.class);
		  
		//cplLimitQryIn.setCcy_code(ccyCode);
		//cplLimitQryIn.setLimit_code(quotaCode);
		
		// 调用额度试算(带锁)服务 
		//IoClLimitTrialOut  cplLimitQryOut = SysUtil.getRemoteInstance(SrvIoClLimitMgt.class).limitBalTrial(cplLimitQryIn); 

		DpCreditLimitTrialInfo cplOut = BizUtil.getInstance(DpCreditLimitTrialInfo.class);

		// cplOut.setLimit_bal(cplLimitQryOut.getLimit_bal());
		// cplOut.setAvailable_overdraw_amount(cplLimitQryOut.getAvailable_overdraw_amount());

		return cplOut;
	}
}