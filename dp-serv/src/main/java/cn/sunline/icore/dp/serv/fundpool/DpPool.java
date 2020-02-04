package cn.sunline.icore.dp.serv.fundpool;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpOverDraftApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtect;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtectDao;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeCalcIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeCalcOut;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_EXCHRATETYPE;
import cn.sunline.icore.sys.type.EnumType.E_FOREXQUOTTYPE;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpPool {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPool.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年7月29日-下午4:56:32</li>
	 *         <li>功能说明：获取资金池余额</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @return 资金池余额
	 */
	public static BigDecimal getBanlance(DpaSubAccount subAcct) {

		bizlog.debug("DpPool.getBanlance begin >>>>>>>>>>");

		// 资金池总余额
		BigDecimal totalAmt = BigDecimal.ZERO;

		// 透支可用额度
		if (subAcct.getOverdraft_allow_ind() == E_YESORNO.YES) {

			BigDecimal sumOverDrawAmt = DpOverDraftApi.getLimtInfo(subAcct);

			totalAmt = totalAmt.add(sumOverDrawAmt);
		}

		// 账户保护余额
		BigDecimal sumProtctAmt = getProtectAmount(subAcct);

		totalAmt = totalAmt.add(sumProtctAmt);

		bizlog.debug("totalAmt=[%s]", totalAmt);
		bizlog.debug("DpPool.getBanlance end  <<<<<<<<<<<<<");

		return totalAmt;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年8月24日-上午10:17:10</li>
	 *         <li>功能说明：获取支取保护金额</li>
	 *         </p>
	 * @param subAcct
	 * @return
	 */
	public static BigDecimal getProtectAmount(DpaSubAccount subAcct) {

		bizlog.debug("DpWithdrawlProtect.getProtectAmount begin >>>>>>>>>>>>");

		BigDecimal sumProtectAmt = BigDecimal.ZERO;

		// 获取被保护账户协议信息
		List<DpbWithdrawlProtect> withdrawlList = DpbWithdrawlProtectDao.selectAll_odb4(subAcct.getAcct_no(), subAcct.getCcy_code(), E_STATUS.VALID, false);

		for (DpbWithdrawlProtect withdrawlPro : withdrawlList) {

			if (withdrawlPro.getStop_use_ind() == E_YESORNO.YES) {

				bizlog.debug("The withdrawal protection agreement has been discontinued [%s]", withdrawlPro.getAgree_no());
				continue;
			}

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			// 定位保护账户信息
			accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
			accessIn.setAcct_no(withdrawlPro.getProtect_acct_no());
			accessIn.setSub_acct_seq(withdrawlPro.getProtect_sub_acct_seq());
			accessIn.setCcy_code(withdrawlPro.getProtect_ccy());
			accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);

			DpAcctAccessOut accessOut = null;

			// 提供保护账户异常,不报错,继续下一个账户进行处理
			try {
				accessOut = DpToolsApi.locateSingleSubAcct(accessIn);
				if (accessOut.getAcct_status() == E_ACCTSTATUS.CLOSE) {
					bizlog.debug("SubAcct is closed[%s]", accessOut.getSub_acct_no());
					continue;
				}
			}
			catch (LttsBusinessException e) {
				bizlog.error("protectSubAcct-Exception=[%s]", e, e.getMessage());
				continue;
			}
			catch (Exception e) {
				throw e;
			}

			DpaSubAccount protectSubAcct = DpaSubAccountDao.selectOne_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), false);

			// 获取保护子账户可用余额
			DpBalanceCalculateOut protectSubAcctBalance = DpToolsApi.getBalance(protectSubAcct.getSub_acct_no(), protectSubAcct.getAcct_no(), null);

			if (!CommUtil.equals(subAcct.getCcy_code(), protectSubAcct.getCcy_code())) {

				DpExchangeCalcIn sellIn = BizUtil.getInstance(DpExchangeCalcIn.class);

				sellIn.setBuy_amt(protectSubAcctBalance.getUsable_bal());
				sellIn.setBuy_ccy_code(withdrawlPro.getCcy_code());
				sellIn.setForex_quot_type(E_FOREXQUOTTYPE.MIDDLE);
				sellIn.setSell_ccy_code(subAcct.getCcy_code());
				sellIn.setExch_rate_type(E_EXCHRATETYPE.EXCHANGE);

				// 货币对试算
				DpExchangeCalcOut sellOut = DpExchangeIobus.calcExchangeAmount(sellIn);

				sumProtectAmt = sumProtectAmt.add(sellOut.getSell_amt());
			}
			else {

				sumProtectAmt = sumProtectAmt.add(protectSubAcctBalance.getUsable_bal());
			}

		}

		bizlog.debug("sumProtectAmt=[%s]", sumProtectAmt);
		bizlog.debug("DpWithdrawlProtect.getProtectAmount end >>>>>>>>>>>>");

		return sumProtectAmt;
	}
}
