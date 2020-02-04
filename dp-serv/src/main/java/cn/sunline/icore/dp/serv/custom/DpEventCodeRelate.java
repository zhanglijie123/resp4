package cn.sunline.icore.dp.serv.custom;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpPayModeAndChargeCode;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：Event code 相关(CIMB 对接总账使用)
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2018年3月8日-上午9:49:44</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpEventCodeRelate {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpEventCodeRelate.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年12月5日-下午3:05:50</li>
	 *         <li>功能说明：获取支付模式</li>
	 *         </p>
	 * @param oppAcctRoute
	 *            对手方账户路由
	 * @param oppAcctNo
	 *            对手账号、挂账编号、业务编码
	 * @param feeCode
	 *            费用代码
	 * @return 支付模式和费用代码
	 */
	public static DpPayModeAndChargeCode getPayModeAndChargeCode(E_ACCOUTANALY oppAcctRoute, String oppAcctNo, String feeCode, E_DEBITCREDIT drOrCr) {

		bizlog.method(" DpEventCodeRelate.getPayModeAndChargeCode begin >>>>>>>>>>>>>>");
		bizlog.debug("oppAcctRoute=[%s],oppAcctNo=[%s]", oppAcctRoute, oppAcctNo);

		DpPayModeAndChargeCode cplPayModeAndChargeCode = BizUtil.getInstance(DpPayModeAndChargeCode.class);

		// 默认支付模式: PTF
		String paymentCode = CommUtil.nvl(BizUtil.getTrxRunEnvs().getPayment_mode(), ApSystemParmApi.getValue("DEFAULT_PAYMENT_MODE"));

		cplPayModeAndChargeCode.setChrg_code(feeCode);
		cplPayModeAndChargeCode.setPayment_mode(paymentCode);

		// 费用代码不为空时，支付模式直接返回PTF
		if (CommUtil.isNotNull(feeCode)) {

			cplPayModeAndChargeCode.setChrg_code(feeCode);
			cplPayModeAndChargeCode.setPayment_mode(ApSystemParmApi.getValue("DEFAULT_PAYMENT_MODE"));

			return cplPayModeAndChargeCode;
		}

		// 如果对手方是存款账户、挂销账，则支付模式为PTF。如果是内部户， 则去查询业务编码对应的支付模式
		if (E_ACCOUTANALY.INSIDE == oppAcctRoute && CommUtil.isNotNull(oppAcctNo)) {

			// TODO: CIMB客户化代码，屏蔽
			/* IoTaQueGlCodeOutput cplOut = BizUtil.getInstance(SrvIoTaAccounting.class).qryGlCodeInfo(sAcctNo);

			 String chrgCode = BizUtil.getInstance(SrvIoCmChrg.class).qryChrgCodeByAcctAlias(cplOut.getGl_ref_code());
			*/
			
			String chrgCode = "";
			
			// 业务编码为收费业务编码,支付模式固定为PTF
			if (CommUtil.isNotNull(chrgCode)) {

				cplPayModeAndChargeCode.setChrg_code(chrgCode);
				cplPayModeAndChargeCode.setPayment_mode(ApSystemParmApi.getValue("DEFAULT_PAYMENT_MODE"));
			}
			else {
				
				// TODO: CIMB客户化代码，屏蔽
				// paymentCode = CommUtil.nvl(cplOut.getPayment_mode(), paymentCode);

				// 接口传入了非PTF的支付模式，则以接口传入为准
				if (CommUtil.isNotNull(BizUtil.getTrxRunEnvs().getPayment_mode())
						&& !CommUtil.equals(BizUtil.getTrxRunEnvs().getPayment_mode(), ApSystemParmApi.getValue("DEFAULT_PAYMENT_MODE"))) {

					cplPayModeAndChargeCode.setPayment_mode(BizUtil.getTrxRunEnvs().getPayment_mode());
				}
				else {
					// 业务编码指定了支付模式，则不用PTF
					cplPayModeAndChargeCode.setPayment_mode(paymentCode);
				}

				cplPayModeAndChargeCode.setChrg_code(null);
			}
		}

		bizlog.method(" DpEventCodeRelate.getPayModeAndChargeCode end <<<<<<<<<<<<<<<<");
		return cplPayModeAndChargeCode;
	}

}