package cn.sunline.icore.dp.serv.fundpool;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTHANDLINGMETHOD;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.servicetype.SrvDpTimeAccounting;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtect;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtectDao;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpFundPool.DpWithdrawlProtectIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeCalcIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeCalcOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_PROTECTTYPE;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_EXCHRATETYPE;
import cn.sunline.icore.sys.type.EnumType.E_FOREXEXCHOBJECT;
import cn.sunline.icore.sys.type.EnumType.E_FOREXQUOTTYPE;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：关联保护处理
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年7月4日-下午2:16:21</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年7月4日-HongBiao：关联保护处理</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpWithdrawlProtect {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpWithdrawlProtect.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年7月5日-下午3:36:09</li>
	 *         <li>功能说明：支取保护处理程序</li>
	 *         </p>
	 * @param cplIn
	 *            支取保护输入接口
	 * @return DpWithdrawlProtectOut 支取保护输出数据
	 */
	public static void withdrawlProtect(DpWithdrawlProtectIn cplIn) {

		bizlog.method(" DpWithdrawlProtect.withdrawlProtect begin >>>>>>>>>>>>>>>>");
		bizlog.debug("DpWithdrawlProtectIn=[%s]", cplIn);

		// 验证输入数据合法性
		validataInput(cplIn);

		// 1.定位被保护账户信息
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no());
		accessIn.setCcy_code(cplIn.getCcy_code());
		accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
		accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.SAVE);

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), false);

		// 2. 获取被保护账户协议信息
		List<DpbWithdrawlProtect> withdrawlList = DpbWithdrawlProtectDao.selectAll_odb4(subAcct.getAcct_no(), subAcct.getCcy_code(), E_STATUS.VALID, false);

		if (withdrawlList.isEmpty()) {

			ApPubErr.APPUB.E0005(OdbFactory.getTable(DpbWithdrawlProtect.class).getLongname(), SysDict.A.acct_no.getLongName(), cplIn.getAcct_no());
		}

		BigDecimal txrnAmt = cplIn.getTrxn_amt();

		// 3.保护账户余额处理
		for (DpbWithdrawlProtect withdrawlPro : withdrawlList) {

			// 交易金额为0 退出保护处理
			if (CommUtil.equals(txrnAmt, BigDecimal.ZERO)) {
				bizlog.debug("withdrawlPro trxn amt [%s]", txrnAmt);
				continue;
			}

			// 协议失效不做处理
			if (CommUtil.compare(BizUtil.getTrxRunEnvs().getTrxn_date(), withdrawlPro.getExpiry_date()) > 0) {
				bizlog.debug("withdrawlPro expiry[%s]", withdrawlPro.getExpiry_date());
				continue;
			}

			// 协议还未生效
			if (CommUtil.compare(BizUtil.getTrxRunEnvs().getTrxn_date(), withdrawlPro.getEffect_date()) < 0) {
				bizlog.debug("withdrawlPro effect date[%s]", withdrawlPro.getEffect_date());
				continue;
			}

			// 协议暂时停用
			if (withdrawlPro.getStop_use_ind() == E_YESORNO.YES) {
				bizlog.debug("withdrawl Protect Agree Stop Use[%s]", withdrawlPro.getAgree_no());
				continue;
			}

			BigDecimal actualWithdrawalAmount = txrnAmt;

			// 3.1 定位保护账户信息
			accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
			accessIn.setAcct_no(withdrawlPro.getProtect_acct_no());
			accessIn.setSub_acct_seq(withdrawlPro.getProtect_sub_acct_seq());
			accessIn.setCcy_code(withdrawlPro.getProtect_ccy());
			accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);

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

			DpaSubAccount protectSubAcct = DpaSubAccountDao.selectOne_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), false);

			// 3.2 获取保护子账户可用余额
			DpBalanceCalculateOut protectSubAcctBalance = DpToolsApi.getBalance(protectSubAcct.getSub_acct_no(), protectSubAcct.getAcct_no(), null);

			// 被保护账户与提供保护账户币种不一致,做货币试算计算实际支取金额,并结售汇处理
			if (!CommUtil.equals(withdrawlPro.getProtect_ccy(), cplIn.getCcy_code())) {

				DpExchangeCalcIn sellIn = BizUtil.getInstance(DpExchangeCalcIn.class);

				sellIn.setBuy_amt(protectSubAcctBalance.getUsable_bal());
				sellIn.setBuy_ccy_code(withdrawlPro.getCcy_code());
				sellIn.setForex_quot_type(E_FOREXQUOTTYPE.MIDDLE);
				sellIn.setSell_ccy_code(subAcct.getCcy_code());
				sellIn.setExch_rate_type(E_EXCHRATETYPE.EXCHANGE);

				// 货币对试算
				DpExchangeCalcOut sellOut = DpExchangeIobus.calcExchangeAmount(sellIn);

				// 保护子户可用余额小于实际支取金额。
				if (CommUtil.compare(sellOut.getSell_amt().subtract(actualWithdrawalAmount), BigDecimal.ZERO) < 0) {

					// 实际支取金额设置为账户余额，进行支取
					actualWithdrawalAmount = sellOut.getSell_amt();
				}
				else {
					// 保护子户可用余额大于实际支取金额。结售汇只支取实际交易金额
					sellOut.setSell_amt(actualWithdrawalAmount);
				}

				// 结售汇处理
				forexTrxnMiddleService(withdrawlPro, cplIn, sellOut);
			}

			else {// 币种一致,检查保护子户余额可取金额

				// 保护子户可用余额小于实际支取金额。
				if (CommUtil.compare(protectSubAcctBalance.getUsable_bal().subtract(actualWithdrawalAmount), BigDecimal.ZERO) < 0) {

					// 实际支取金额设置为账户余额，进行支取
					actualWithdrawalAmount = protectSubAcctBalance.getUsable_bal();
				}
			}

			// 4. 支取保护子账户金额
			if (withdrawlPro.getProtect_type() == E_PROTECTTYPE.INTELLIGENT) {

				// 智能存款关联保护一借一贷不登记冲账事件
				// TODO:
				// BizUtil.getTrxRunEnvs().setReg_reversal_event_ind(E_YESORNO.NO);

				// 中途执行失败也要把登记冲账事件的开关还原
				try {

					protTimeDraw(cplIn, withdrawlPro, actualWithdrawalAmount, protectSubAcct);

					protectedSava(cplIn, withdrawlPro, actualWithdrawalAmount, subAcct);
				}
				finally {
					// 还原
					// TODO:
					// BizUtil.getTrxRunEnvs().setReg_reversal_event_ind(E_YESORNO.YES);
				}

			}
			else {

				protDemandDraw(cplIn, withdrawlPro, actualWithdrawalAmount, protectSubAcct);

				protectedSava(cplIn, withdrawlPro, actualWithdrawalAmount, subAcct);
			}

			// 更新交易金额
			txrnAmt = txrnAmt.subtract(actualWithdrawalAmount);

			// 5.更新交易金额,下一序号继续进行支取
			// cplIn.setTrxn_amt(cplIn.getTrxn_amt().subtract(actualWithdrawalAmount));

			bizlog.debug("Protect_acct_no=[%s],Ccy_code[%s],draw_amt[%s]", protectSubAcct.getAcct_no(), protectSubAcct.getCcy_code(), actualWithdrawalAmount);
		}

		/*
		 * DpWithdrawlProtectOut cplOut =
		 * BizUtil.getInstance(DpWithdrawlProtectOut.class);
		 * 
		 * //bizlog.debug("DpWithdrawlProtectOut=[%s]", cplOut);
		 * bizlog.method(" DpWithdrawlProtect.withdrawlProtect end <<<<<<<<<<<<<<<<"
		 * );
		 * 
		 * return cplOut;
		 */
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月24日-上午11:40:14</li>
	 *         <li>功能说明：被保护账号存入服务</li>
	 *         </p>
	 * @param cplIn
	 *            服务输入信息
	 * @param withdrawlPro
	 *            服务输入信息
	 * @param actualWithdrawalAmount
	 *            实际支取金额
	 * @param subAcct
	 *            保护子账户信息
	 */
	private static void protectedSava(DpWithdrawlProtectIn cplIn, DpbWithdrawlProtect withdrawlPro, BigDecimal actualWithdrawalAmount, DpaSubAccount subAcct) {
		bizlog.method(" DpWithdrawlProtect.protectedSava begin >>>>>>>>>>>>>>>>");

		// 活期存入记账输入
		DpDemandSaveIn demandSaveIn = BizUtil.getInstance(DpDemandSaveIn.class);

		demandSaveIn.setAcct_no(withdrawlPro.getAcct_no()); //
		demandSaveIn.setAcct_name(subAcct.getSub_acct_name()); //
		demandSaveIn.setProd_id(subAcct.getProd_id()); //
		demandSaveIn.setCcy_code(subAcct.getCcy_code()); //
		demandSaveIn.setCash_trxn_ind(E_CASHTRXN.TRXN); //
		demandSaveIn.setTrxn_amt(actualWithdrawalAmount); //
		demandSaveIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_WITHDRAW_PROTECT")); //
		demandSaveIn.setTrxn_remark(""); // transaction remark
		demandSaveIn.setCustomer_remark(""); // customer remark

		// 对手方
		demandSaveIn.setOpp_acct_ccy(withdrawlPro.getProtect_ccy());
		demandSaveIn.setOpp_acct_no(withdrawlPro.getProtect_acct_no());
		demandSaveIn.setOpp_sub_acct_seq(withdrawlPro.getProtect_sub_acct_seq());
		demandSaveIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
		demandSaveIn.setOpp_acct_type("");
		demandSaveIn.setOpp_branch_id("");

		// 对账单对手方
		demandSaveIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no()); // real
																		// opponent
																		// account
																		// no
		demandSaveIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name()); // real
																			// opponent
																			// account
																			// name
		demandSaveIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias()); // real
																				// opponent
																				// account
																				// name
		demandSaveIn.setReal_opp_country(cplIn.getReal_opp_country()); // real
																		// opponent
																		// country
		demandSaveIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id()); // real
																		// opponent
																		// bank
																		// id
		demandSaveIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name()); // real
																			// opponent
																			// bank
																			// name
		demandSaveIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name()); // real
																				// opponent
																				// branch
																				// name
		demandSaveIn.setReal_opp_remark(cplIn.getReal_opp_remark()); // real
																		// opponent
																		// remark

		// 调用活期存款支取服务
		BizUtil.getInstance(SrvDpDemandAccounting.class).demandSave(demandSaveIn);

		bizlog.method(" DpWithdrawlProtect.protectedSava end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年7月7日-下午4:01:32</li>
	 *         <li>功能说明：验证输入数据合法性</li>
	 *         </p>
	 * @param cplIn
	 *            输入数据
	 */
	private static void validataInput(DpWithdrawlProtectIn cplIn) {
		bizlog.method(" DpWithdrawlProtect.validataInput begin >>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

		bizlog.method(" DpWithdrawlProtect.validataInput end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年7月7日-下午3:57:12</li>
	 *         <li>功能说明：调用定期支取服务</li>
	 *         </p>
	 * @param cplIn
	 *            服务输入信息
	 * @param withdrawlPro
	 *            保护协议信息
	 * @param actualWithdrawalAmount
	 *            实际支取金额
	 * @param protectSubAcct
	 *            保护子账户信息
	 */
	private static void protTimeDraw(DpWithdrawlProtectIn cplIn, DpbWithdrawlProtect withdrawlPro, BigDecimal actualWithdrawalAmount, DpaSubAccount protectSubAcct) {
		// 定期支取记账输入
		DpTimeDrawIn timeDrawIn = BizUtil.getInstance(DpTimeDrawIn.class);

		timeDrawIn.setAcct_no(withdrawlPro.getProtect_acct_no());
		timeDrawIn.setAcct_name(protectSubAcct.getSub_acct_name());
		timeDrawIn.setSub_acct_seq(protectSubAcct.getSub_acct_seq());
		timeDrawIn.setCheck_password_ind(E_YESORNO.NO);
		timeDrawIn.setCcy_code(protectSubAcct.getCcy_code());
		timeDrawIn.setTrxn_amt(actualWithdrawalAmount); // 使用实际支取金额
		timeDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.DEDUCT); // 普通支取
		timeDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 交易级现转标志
		timeDrawIn.setInst_handling_method(E_INSTHANDLINGMETHOD.PRODUCT_DEFINE);
		timeDrawIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_WITHDRAW_PROTECT"));

		// 对手方
		timeDrawIn.setOpp_acct_ccy(withdrawlPro.getCcy_code());
		timeDrawIn.setOpp_acct_no(withdrawlPro.getAcct_no());
		timeDrawIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
		timeDrawIn.setOpp_sub_acct_seq("");
		timeDrawIn.setOpp_acct_type("");
		timeDrawIn.setOpp_branch_id("");

		// 调用定期存款支取服务
		BizUtil.getInstance(SrvDpTimeAccounting.class).timeDraw(timeDrawIn);
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年7月7日-下午3:49:22</li>
	 *         <li>功能说明：调用活期支取服务</li>
	 *         </p>
	 * @param cplIn
	 *            服务输入信息
	 * @param withdrawlPro
	 *            保护协议信息
	 * @param actualWithdrawalAmount
	 *            实际支取金额
	 * @param protectSubAcct
	 *            保护子账户信息
	 */
	private static void protDemandDraw(DpWithdrawlProtectIn cplIn, DpbWithdrawlProtect withdrawlPro, BigDecimal actualWithdrawalAmount, DpaSubAccount protectSubAcct) {
		// 活期支取记账输入
		DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

		demandDrawIn.setAcct_no(withdrawlPro.getProtect_acct_no());
		demandDrawIn.setAcct_name(protectSubAcct.getSub_acct_name());
		demandDrawIn.setProd_id(protectSubAcct.getProd_id());
		demandDrawIn.setCheck_password_ind(E_YESORNO.NO);
		demandDrawIn.setCcy_code(protectSubAcct.getCcy_code());
		demandDrawIn.setTrxn_amt(actualWithdrawalAmount); // 使用实际支取金额
		demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.DEDUCT); // 扣划支取:TODO:避免触发收费
		demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 交易级现转标志
		demandDrawIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_WITHDRAW_PROTECT"));

		// 对手方
		demandDrawIn.setOpp_acct_ccy(withdrawlPro.getCcy_code());
		demandDrawIn.setOpp_acct_no(withdrawlPro.getAcct_no());
		demandDrawIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
		demandDrawIn.setOpp_sub_acct_seq("");
		demandDrawIn.setOpp_acct_type("");
		demandDrawIn.setOpp_branch_id("");

		// 对账单对手方
		demandDrawIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no()); // real
																		// opponent
																		// account
																		// no
		demandDrawIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name()); // real
																			// opponent
																			// account
																			// name
		demandDrawIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias()); // real
																				// opponent
																				// account
																				// name
		demandDrawIn.setReal_opp_country(cplIn.getReal_opp_country()); // real
																		// opponent
																		// country
		demandDrawIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id()); // real
																		// opponent
																		// bank
																		// id
		demandDrawIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name()); // real
																			// opponent
																			// bank
																			// name
		demandDrawIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name()); // real
																				// opponent
																				// branch
																				// name
		demandDrawIn.setReal_opp_remark(cplIn.getReal_opp_remark()); // real
																		// opponent
																		// remark

		// 调用活期存款支取服务
		BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年7月7日-下午2:08:16</li>
	 *         <li>功能说明：外汇交易中间服务</li>
	 *         </p>
	 * @param withdrawlPro
	 *            支取保护协议
	 * @param cplIn
	 *            服务输入信息
	 * @param actualWithdrawalAmount
	 *            实际支取金额
	 * @return
	 */
	private static DpExchangeAccountingOut forexTrxnMiddleService(DpbWithdrawlProtect withdrawlPro, DpWithdrawlProtectIn cplIn, DpExchangeCalcOut sellOut) {

		// 结售汇中间记账服务输入
		DpExchangeAccountingIn fxExchangeIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

		fxExchangeIn.setExch_rate(sellOut.getExch_rate());

		fxExchangeIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_WITHDRAW_PROTECT"));

		// 买卖双方账户信息
		fxExchangeIn.setSell_acct_no(cplIn.getOpp_acct_no());
		fxExchangeIn.setSell_ccy_code(cplIn.getOpp_acct_ccy());
		fxExchangeIn.setSell_cash_ind(E_CASHTRXN.TRXN);

		fxExchangeIn.setBuy_acct_no(withdrawlPro.getProtect_acct_no());
		fxExchangeIn.setBuy_cash_ind(E_CASHTRXN.TRXN);
		fxExchangeIn.setBuy_ccy_code(withdrawlPro.getProtect_ccy());
		fxExchangeIn.setBuy_amt(sellOut.getSell_amt());
		fxExchangeIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.CUSTOMER);

		// 调用结售汇中间记账服务
		return DpExchangeIobus.exchangeAccounting(fxExchangeIn);
	}
}
