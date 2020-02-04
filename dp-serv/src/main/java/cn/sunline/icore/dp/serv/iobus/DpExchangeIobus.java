package cn.sunline.icore.dp.serv.iobus;

import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeCalcIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeCalcOut;
import cn.sunline.icore.iobus.cm.servicetype.SrvIoCmExchBase;
import cn.sunline.icore.iobus.cm.servicetype.SrvIoFxExchange;
import cn.sunline.icore.iobus.cm.type.ComIoCmExch.IoCmExchAmtCalcIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmExch.IoCmExchAmtCalcOut;
import cn.sunline.icore.iobus.cm.type.ComIoFxExchange.IoFxExchangeIn;
import cn.sunline.icore.iobus.cm.type.ComIoFxExchange.IoFxExchangeOut;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpExchangeIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpExchangeIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：外汇买卖中间服务</li>
	 *         </p>
	 * @param cplIn
	 *            外汇买卖记账输入
	 * @param 外汇买卖记账输出
	 */
	public static DpExchangeAccountingOut exchangeAccounting(DpExchangeAccountingIn cplIn) {

		// 结售汇中间记账服务输入
		IoFxExchangeIn fxExchangeIn = BizUtil.getInstance(IoFxExchangeIn.class);

		fxExchangeIn.setBuy_cash_ind(cplIn.getBuy_cash_ind());
		fxExchangeIn.setBuy_ccy_code(cplIn.getBuy_ccy_code());
		fxExchangeIn.setSell_cash_ind(cplIn.getSell_cash_ind());
		fxExchangeIn.setSell_ccy_code(cplIn.getSell_ccy_code());
		fxExchangeIn.setExch_rate(cplIn.getExch_rate());
		fxExchangeIn.setForex_agree_price_id(cplIn.getForex_agree_price_id());
		fxExchangeIn.setExch_rate_path(cplIn.getExch_rate_path());
		fxExchangeIn.setForex_agree_price_id(cplIn.getForex_agree_price_id());
		fxExchangeIn.setForex_exch_object_type(cplIn.getForex_exch_object_type());
		fxExchangeIn.setBuy_amt(cplIn.getBuy_amt());
		fxExchangeIn.setSell_amt(cplIn.getSell_amt());

		// 客户类型对现钞兑换有用
		fxExchangeIn.setCust_type(cplIn.getCust_type());
		fxExchangeIn.setCountry_code(cplIn.getCountry_code());
		fxExchangeIn.setCustomer_remark(cplIn.getCustomer_remark());
		fxExchangeIn.setSummary_code(cplIn.getSummary_code());
		fxExchangeIn.setTrxn_remark(cplIn.getTrxn_remark());

		// 买卖双方账户信息
		fxExchangeIn.setSell_acct_no(cplIn.getSell_acct_no());
		fxExchangeIn.setSell_sub_acct_seq(null);
		fxExchangeIn.setBuy_acct_no(cplIn.getBuy_acct_no());
		fxExchangeIn.setBuy_sub_acct_seq(null);

		// 代理人信息
		fxExchangeIn.setAgent_country(cplIn.getAgent_country());
		fxExchangeIn.setAgent_doc_no(cplIn.getAgent_doc_no());
		fxExchangeIn.setAgent_doc_type(cplIn.getAgent_doc_type());
		fxExchangeIn.setAgent_name(cplIn.getAgent_name());

		// 调用结售汇中间记账服务
		IoFxExchangeOut fxExchangeOut = SysUtil.getRemoteInstance(SrvIoFxExchange.class).forexTrxnMiddleService(fxExchangeIn);

		DpExchangeAccountingOut cplOut = BizUtil.getInstance(DpExchangeAccountingOut.class);

		cplOut.setBuy_amt(fxExchangeOut.getBuy_amt());
		cplOut.setSell_amt(fxExchangeOut.getSell_amt());
		cplOut.setExch_rate(fxExchangeOut.getExch_rate());
		cplOut.setExch_rate_path(fxExchangeOut.getExch_rate_path());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：外币折算金额计算</li>
	 *         </p>
	 * @param cplIn
	 *            外汇买卖计算输入
	 * @param 外汇买卖计算输出
	 */
	public static DpExchangeCalcOut calcExchangeAmount(DpExchangeCalcIn cplIn) {

		// 外汇买卖双方信息输入: 转账调用的自动结售汇可能输入买方金额求卖方，也可能输入卖方求买方
		IoCmExchAmtCalcIn cplAmtCalcIn = BizUtil.getInstance(IoCmExchAmtCalcIn.class);

		cplAmtCalcIn.setBuy_ccy_code(cplIn.getBuy_ccy_code());
		cplAmtCalcIn.setBuy_amt(cplIn.getBuy_amt());
		cplAmtCalcIn.setExch_rate_type(cplIn.getExch_rate_type());
		cplAmtCalcIn.setExch_rate(cplIn.getExch_rate());
		cplAmtCalcIn.setExch_rate_path(cplIn.getExch_rate_path());
		cplAmtCalcIn.setForex_quot_type(cplIn.getForex_quot_type());
		cplAmtCalcIn.setSell_ccy_code(cplIn.getSell_ccy_code());
		cplAmtCalcIn.setSell_amt(cplIn.getSell_amt());

		IoCmExchAmtCalcOut cplResult = SysUtil.getRemoteInstance(SrvIoCmExchBase.class).calcForexAmount(cplAmtCalcIn);

		DpExchangeCalcOut cplOut = BizUtil.getInstance(DpExchangeCalcOut.class);

		cplOut.setBuy_amt(cplResult.getBuy_amt());
		cplOut.setSell_amt(cplResult.getSell_amt());
		cplOut.setExch_rate(cplResult.getExch_rate());
		cplOut.setExch_rate_path(cplResult.getExch_rate_path());

		return cplOut;

	}
}