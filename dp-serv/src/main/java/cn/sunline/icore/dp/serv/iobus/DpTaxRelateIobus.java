package cn.sunline.icore.dp.serv.iobus;

import java.math.BigDecimal;

import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpStampTaxInfo;
import cn.sunline.icore.iobus.cm.servicetype.SrvIoCmTax;
import cn.sunline.icore.iobus.cm.type.ComIoCmTax.IoCmStampTaxRateInfoQryIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmTax.IoCmStampTaxRateInfoQryOut;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpTaxRelateIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTaxRelateIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：印花税率信息查询</li>
	 *         </p>
	 * @param taxRateCode
	 *            税率代码
	 * @param ccyCode
	 *            币种
	 * @param trxnAmt
	 *            交易金额
	 * @param termCode
	 *            存期
	 * @return 印花税信息
	 */
	public static DpStampTaxInfo getStampTaxInfo(String taxRateCode, String ccyCode, BigDecimal trxnAmt, String termCode) {

		// 印花税信息查询输入
		IoCmStampTaxRateInfoQryIn stampTaxInfoQryIn = BizUtil.getInstance(IoCmStampTaxRateInfoQryIn.class);

		stampTaxInfoQryIn.setTax_rate_code(taxRateCode);
		stampTaxInfoQryIn.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		stampTaxInfoQryIn.setCcy_code(ccyCode);
		stampTaxInfoQryIn.setTrxn_amt(trxnAmt);
		stampTaxInfoQryIn.setTerm_days((long) BizUtil.calcSaveDaysByCycle(termCode));

		// 印花税信息查询服务
		IoCmStampTaxRateInfoQryOut stampTaxInfoQryOut = SysUtil.getRemoteInstance(SrvIoCmTax.class).qryStampTaxRate(stampTaxInfoQryIn);

		DpStampTaxInfo cplOut = BizUtil.getInstance(DpStampTaxInfo.class);

		cplOut.setStamp_tax_amt(stampTaxInfoQryOut.getStamp_tax_amt());
		cplOut.setBank_assume_ind(stampTaxInfoQryOut.getBank_assume_ind());
		cplOut.setTax_accounting_alias(stampTaxInfoQryOut.getTax_accounting_alias());

		return cplOut;
	}

}