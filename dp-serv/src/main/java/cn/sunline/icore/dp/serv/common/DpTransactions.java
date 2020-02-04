package cn.sunline.icore.dp.serv.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpCashIobus;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpTransferProperty;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveOut;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpTransferIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpTransferOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAccountRouteInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCashAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustBaseInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeCalcIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeCalcOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpSuspenseAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpSuspenseAccountingOut;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApBaseErr.ApBase;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_CUSTOMERTYPE;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_EXCHRATETYPE;
import cn.sunline.icore.sys.type.EnumType.E_FOREXEXCHOBJECT;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpTransactions {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTransactions.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月25日-下午3:25:13</li>
	 *         <li>功能说明：多个一对一转账</li>
	 *         </p>
	 * @param dpTransferInList
	 *            多个一对一输入列表
	 * @return List<DpTransferOut 多个一对一转账输出列表
	 */
	public static List<DpTransferOut> multiTransfer(List<DpTransferIn> dpTransferInList) {

		bizlog.method(" DpTransactions.multiTransfer begin >>>>>>>>>>>>>>>>");

		List<DpTransferOut> tranStosOutList = new ArrayList<DpTransferOut>();

		for (DpTransferIn dpTransferIn : dpTransferInList) {

			DpTransferOut tranStosOut = BizUtil.getInstance(DpTransferOut.class);

			// 表外记账判断
			boolean fineFlag = offPosting(dpTransferIn, tranStosOut);

			// 非表外记账，调用单个一对一转账
			if (fineFlag == false) {
				tranStosOut = DpTransactions.singleTransfer(dpTransferIn);
			}

			tranStosOutList.add(tranStosOut);
		}

		bizlog.method(" DpTransactions.multiTransfer end <<<<<<<<<<<<<<<<");

		return tranStosOutList;

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月21日-上午10:28:23</li>
	 *         <li>功能说明：单条一对一活期转账处理</li>
	 *         <li>补充说明：不包括授权检查、交易流水登记等接口独特性部分</li>
	 *         </p>
	 * @param cplIn
	 *            输入接口
	 * @return cplOut 输出接口
	 */
	public static DpTransferOut singleTransfer(DpTransferIn cplIn) {

		bizlog.method(" DpTransactions.singleTransfer begin >>>>>>>>>>>>>>>>");
		bizlog.debug(" DpTransactions.singleTransfer cplIn = [%s]", cplIn);
		bizlog.debug("singleTransfer.Trxn_seq = [%s]", BizUtil.getTrxRunEnvs().getTrxn_seq());

		// 交易预检查
		trxnAdvanceCheck(cplIn);

		// 借方账户路由分析
		DpAccountRouteInfo debitRoute = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getDebit_acct_no(), cplIn.getDebit_cash_trxn_ind(), cplIn.getDebit_suspense_no());

		// 贷方账户路由分析
		DpAccountRouteInfo creditRoute = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getCredit_acct_no(), cplIn.getCredit_cash_trxn_ind(), cplIn.getCredit_suspense_no());

		// 借贷方路由信息暂存运行中间数据集
		DpTransferProperty cplProperty = BizUtil.getInstance(DpTransferProperty.class);

		cplProperty.setDebit_acct_analy(debitRoute.getAcct_analy());
		cplProperty.setDebit_busi_code(debitRoute.getGl_ref_code());
		cplProperty.setCredit_acct_analy(creditRoute.getAcct_analy());
		cplProperty.setCredit_busi_code(creditRoute.getGl_ref_code());
		cplProperty.setCash_trxn_ind((cplIn.getCredit_cash_trxn_ind() == E_CASHTRXN.CASH || cplIn.getDebit_cash_trxn_ind() == E_CASHTRXN.CASH) ? E_CASHTRXN.CASH : E_CASHTRXN.TRXN);

		// 交易中间检查
		trxnMiddleCheck(cplIn, cplProperty);

		// 输出对象实例化
		DpTransferOut cplOut = BizUtil.getInstance(DpTransferOut.class);

		// 交易币种和借方币种一样
		if (CommUtil.equals(cplIn.getTrxn_ccy(), cplIn.getDebit_ccy_code())) {

			// 实际支取金额初始化为交易金额，若是强制扣划，借方记账会更新此值
			cplProperty.setAct_withdrawal_amt(cplIn.getTrxn_amt());

			if (CommUtil.compare(cplIn.getDebit_ccy_code(), cplIn.getCredit_ccy_code()) == 0) {

				// 先默认为交易金额，强制借记后面在借方交易之后再刷新
				cplProperty.setAct_dept_amt(cplIn.getTrxn_amt());
			}
			else {

				if (CommUtil.isNotNull(cplIn.getTrxn_opp_amt()) && !CommUtil.equals(cplIn.getTrxn_opp_amt(), BigDecimal.ZERO)) {

					cplProperty.setAct_dept_amt(cplIn.getTrxn_opp_amt());
				}
				else {

					// 对手方金额很很重要，尽量登记准确
					if (cplIn.getForce_draw_ind() != E_YESORNO.YES) {

						// TODO: E_EXCHRATETYPE exchPrice =
						// FxExchange.getCashExchPriceFlag(IoFxQueryCashExchPrice
						// cplIn)
						E_EXCHRATETYPE exchType = (cplIn.getDebit_cash_trxn_ind() == E_CASHTRXN.CASH || cplIn.getCredit_cash_trxn_ind() == E_CASHTRXN.CASH) ? E_EXCHRATETYPE.CASH
								: E_EXCHRATETYPE.EXCHANGE;

						// 外汇买卖双方信息输入: 转账调用的自动结售汇可能输入买方金额求卖方，也可能输入卖方求买方
						DpExchangeCalcIn cplAmtCalcIn = BizUtil.getInstance(DpExchangeCalcIn.class);

						cplAmtCalcIn.setBuy_ccy_code(cplIn.getDebit_ccy_code());
						cplAmtCalcIn.setBuy_amt(cplIn.getTrxn_amt());
						cplAmtCalcIn.setExch_rate_type(exchType);
						cplAmtCalcIn.setExch_rate(cplIn.getExch_rate());
						cplAmtCalcIn.setExch_rate_path(cplIn.getExch_rate_path());
						cplAmtCalcIn.setSell_ccy_code(cplIn.getCredit_ccy_code());
						cplAmtCalcIn.setSell_amt(null);

						// 获得结售汇计算结果
						DpExchangeCalcOut cplAmtCalcOut = DpExchangeIobus.calcExchangeAmount(cplAmtCalcIn);

						cplProperty.setAct_dept_amt(cplAmtCalcOut.getSell_amt());
					}
					else {
						throw ApBase.E0068("Can not be forced debit in different currencies");
					}
				}
			}

			// 借方记账
			debitAccounting(cplIn, cplProperty, cplOut);

			// 跨币种调用结售汇中间服务
			if (CommUtil.compare(cplIn.getDebit_ccy_code(), cplIn.getCredit_ccy_code()) != 0) {

				forexTrxnMiddleService(cplIn, cplProperty, cplOut);
			}
			else {
				// 同币种交易时，实际存入金额等于实际支取金额
				cplProperty.setAct_dept_amt(cplProperty.getAct_withdrawal_amt());
			}

			// 贷方记账
			creditAccounting(cplIn, cplProperty, cplOut);
		}
		else {
			/* 交易币种跟贷方一样，一定是跨币种的 */

			// 先结售汇计算通过贷方金额得到实际支取金额
			forexTrxnMiddleService(cplIn, cplProperty, cplOut);

			// 实际支取金额初始化为交易金额，若是强制扣划，借方记账会更新此值
			cplProperty.setAct_withdrawal_amt(cplOut.getBuy_amt());
			cplProperty.setAct_dept_amt(cplOut.getSell_amt());

			// 借方记账
			debitAccounting(cplIn, cplProperty, cplOut);

			// 贷方记账
			creditAccounting(cplIn, cplProperty, cplOut);
		}

		// 存款同一账户检查
		checkSameAcct(cplIn, cplProperty, cplOut);

		// 补充输出
		cplOut.setDebit_acct_analy(debitRoute.getAcct_analy());
		cplOut.setCredit_acct_analy(creditRoute.getAcct_analy());
		cplOut.setTrxn_amt(cplIn.getTrxn_amt());
		cplOut.setTrxn_ccy(cplIn.getTrxn_ccy());
		cplOut.setBack_value_date(cplIn.getBack_value_date());
		cplOut.setSummary_code(cplIn.getSummary_code());
		cplOut.setSummary_name(ApSummaryApi.getText(cplIn.getSummary_code()));
		cplOut.setCash_trxn_ind(cplProperty.getCash_trxn_ind());
		cplOut.setAct_dept_amt(cplProperty.getAct_dept_amt());
		cplOut.setAct_withdrawal_amt(cplProperty.getAct_withdrawal_amt());

		if (CommUtil.equals(cplIn.getTrxn_ccy(), cplIn.getDebit_ccy_code())) {
			cplOut.setTrxn_amt(cplProperty.getAct_withdrawal_amt());
		}

		bizlog.debug("DpTransactions.singleTransfer cplOut= [%s]", cplOut);
		bizlog.method(" DpTransactions.singleTransfer end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月20日-上午11:09:33</li>
	 *         <li>功能说明：借方记账处理</li>
	 *         </p>
	 * @param cplIn
	 * @param cplProperty
	 * @param cplOut
	 */
	private static void debitAccounting(DpTransferIn cplIn, DpTransferProperty cplProperty, DpTransferOut cplOut) {

		// 现金
		if (cplProperty.getDebit_acct_analy() == E_ACCOUTANALY.CASH) {

			// 记账输入数据
			DpCashAccountingIn cashAccountingIn = BizUtil.getInstance(DpCashAccountingIn.class);

			cashAccountingIn.setCcy_code(cplIn.getDebit_ccy_code());
			cashAccountingIn.setTrxn_amt(cplProperty.getAct_withdrawal_amt()); // 使用实际支取金额
			cashAccountingIn.setDebit_credit(E_DEBITCREDIT.DEBIT); // 借贷方向
			cashAccountingIn.setSummary_code(cplIn.getSummary_code());
			cashAccountingIn.setTrxn_remark(cplIn.getTrxn_remark());

			// 真实对手方
			cashAccountingIn.setOpp_acct_ccy(cplIn.getCredit_ccy_code());
			cashAccountingIn.setOpp_acct_no(cplIn.getCredit_acct_no());
			cashAccountingIn.setOpp_acct_route(cplProperty.getCredit_acct_analy());
			cashAccountingIn.setOpp_branch_id(cplIn.getCredit_acct_branch());

			DpCashIobus.cashAccounting(cashAccountingIn);
		}
		else if (cplProperty.getDebit_acct_analy() == E_ACCOUTANALY.DEPOSIT) {

			// 活期支取记账输入
			DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			demandDrawIn.setAcct_no(cplIn.getDebit_acct_no());
			demandDrawIn.setAcct_type(cplIn.getDebit_acct_type());
			demandDrawIn.setAcct_name(cplIn.getDebit_acct_name());
			demandDrawIn.setProd_id(cplIn.getDebit_prod_id());
			demandDrawIn.setCheck_password_ind(cplIn.getCheck_password_ind());
			demandDrawIn.setTrxn_password(cplIn.getTrxn_password());
			demandDrawIn.setForce_draw_ind(cplIn.getForce_draw_ind());
			demandDrawIn.setCcy_code(cplIn.getDebit_ccy_code());
			demandDrawIn.setTrxn_amt(cplProperty.getAct_withdrawal_amt()); // 使用实际支取金额
			demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.COMMON); // 普通支取
			demandDrawIn.setBack_value_date(cplIn.getBack_value_date());
			demandDrawIn.setCash_trxn_ind(cplProperty.getCash_trxn_ind()); // 交易级现转标志
			demandDrawIn.setChrg_code(null); // 费用编号，不是费用模块调用传空值
			demandDrawIn.setOpen_voch_check_ind(null);
			demandDrawIn.setRed_blue_word_ind(null); // 红蓝字记账
			demandDrawIn.setAcct_hold_check_Ind(null); // 账户限制检查标志
			demandDrawIn.setSummary_code(cplIn.getSummary_code());
			demandDrawIn.setTrxn_remark(cplIn.getTrxn_remark());
			demandDrawIn.setCustomer_remark(cplIn.getCustomer_remark());
			demandDrawIn.setCheque_no(cplIn.getCheque_no()); // 支票
			demandDrawIn.setSettle_voch_type(cplIn.getSettle_voch_type());

			// 解冻信息
			demandDrawIn.setFroze_no(cplIn.getFroze_no());
			demandDrawIn.setUnfroze_amt(cplIn.getUnfroze_amt());
			demandDrawIn.setUnfroze_reason(cplIn.getUnfroze_reason());

			// 对手方
			demandDrawIn.setOpp_acct_ccy(cplIn.getCredit_ccy_code());
			demandDrawIn.setOpp_acct_no(cplIn.getCredit_acct_no());
			demandDrawIn.setOpp_acct_route(cplProperty.getCredit_acct_analy());
			demandDrawIn.setOpp_acct_type(cplIn.getCredit_acct_type());
			demandDrawIn.setOpp_branch_id(cplIn.getCredit_acct_branch());
			demandDrawIn.setOpp_trxn_amt(cplProperty.getAct_dept_amt());

			// 对账单对手方
			demandDrawIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
			demandDrawIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
			demandDrawIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
			demandDrawIn.setReal_opp_country(cplIn.getReal_opp_country());
			demandDrawIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
			demandDrawIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
			demandDrawIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
			demandDrawIn.setReal_opp_remark(cplIn.getReal_opp_remark());

			// 代理人信息
			demandDrawIn.setAgent_country(cplIn.getAgent_country());
			demandDrawIn.setAgent_doc_no(cplIn.getAgent_doc_no());
			demandDrawIn.setAgent_doc_type(cplIn.getAgent_doc_type());
			demandDrawIn.setAgent_name(cplIn.getAgent_name());

			// 境外消费交易地信息登记
			demandDrawIn.setTrxn_area(cplIn.getTrxn_area());
			demandDrawIn.setTrxn_area_amt(cplIn.getTrxn_area_amt());
			demandDrawIn.setTrxn_area_ccy(cplIn.getTrxn_area_ccy());
			demandDrawIn.setTrxn_area_exch_rate(cplIn.getTrxn_area_exch_rate());
			demandDrawIn.setConsume_date(cplIn.getConsume_date());
			demandDrawIn.setConsume_time(cplIn.getConsume_time());

			demandDrawIn.setReservation_no(cplIn.getReservation_no());// 预约编号

			// 调用活期存款支取服务
			DpDemandDrawOut demandDrawOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);

			// 更新实际支取金额
			cplProperty.setAct_withdrawal_amt(demandDrawOut.getAct_withdrawal_amt());
			// 交易输出
			cplOut.setDebit_acct_no(demandDrawOut.getAcct_no());
			cplOut.setDebit_acct_type(demandDrawOut.getAcct_type());
			cplOut.setDebit_acct_name(demandDrawOut.getAcct_name());
			cplOut.setDebit_prod_id(demandDrawOut.getProd_id());
			cplOut.setDebit_ccy_code(demandDrawOut.getCcy_code());
			cplOut.setDebit_froze_bal(demandDrawOut.getFroze_bal());
			cplOut.setDebit_acct_bal(demandDrawOut.getAcct_bal());
			cplOut.setCheque_no(demandDrawOut.getCheque_no());
			cplOut.setSettle_voch_type(demandDrawOut.getSettle_voch_type());
			cplOut.setTigger_smart_protect_ind(demandDrawOut.getTigger_smart_protect_ind());
			cplOut.setBreak_authority_ind(demandDrawOut.getBreak_authority_ind());
			cplOut.setProtect_fiche_count(demandDrawOut.getProtect_fiche_count());
			cplOut.setProtect_amt(demandDrawOut.getProtect_amt());
		}
		else if (cplProperty.getDebit_acct_analy() == E_ACCOUTANALY.SUSPENSE) {

			// 挂销账记账输入
			DpSuspenseAccountingIn suspenseIn = BizUtil.getInstance(DpSuspenseAccountingIn.class);

			suspenseIn.setAcct_branch(cplIn.getDebit_acct_branch());
			suspenseIn.setCash_trxn_ind(cplProperty.getCash_trxn_ind());
			suspenseIn.setCcy_code(cplIn.getDebit_ccy_code());
			suspenseIn.setTrxn_amt(cplProperty.getAct_withdrawal_amt());
			suspenseIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
			suspenseIn.setGl_ref_code(cplProperty.getDebit_busi_code());
			suspenseIn.setMulti_deposit_ind(cplIn.getDebit_multi_deposit_ind());
			suspenseIn.setMulti_draw_ind(cplIn.getDebit_multi_draw_ind());

			// 补充信息
			suspenseIn.setSummary_code(cplIn.getSummary_code());
			suspenseIn.setSuspense_no(cplIn.getDebit_suspense_no());
			suspenseIn.setTrxn_remark(cplIn.getTrxn_remark());
			suspenseIn.setCustomer_remark(cplIn.getCustomer_remark());

			// 真实对手方信息
			suspenseIn.setOpp_acct_route(cplProperty.getCredit_acct_analy());
			suspenseIn.setOpp_acct_no(cplIn.getCredit_acct_no());
			suspenseIn.setOpp_acct_ccy(cplIn.getCredit_ccy_code());

			// 调用挂销账记账服务
			DpSuspenseAccountingOut suspenseOut = DpInsideAccountIobus.suspenseAccounting(suspenseIn);

			cplOut.setDebit_acct_no(suspenseOut.getAcct_no());
			cplOut.setDebit_acct_name(suspenseOut.getAcct_name());
			cplOut.setDebit_ccy_code(suspenseOut.getCcy_code());
			cplOut.setDebit_suspense_no(suspenseOut.getSuspense_no());
			cplOut.setDebit_acct_bal(suspenseOut.getAcct_bal());
		}
		else if (cplProperty.getDebit_acct_analy() == E_ACCOUTANALY.INSIDE) {

			// 内部户记账服务输入
			DpInsideAccountingIn bookAccoutingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

			bookAccoutingIn.setAcct_branch(cplIn.getDebit_acct_branch());
			bookAccoutingIn.setAcct_no(CommUtil.equals(cplIn.getDebit_acct_no(), cplProperty.getDebit_busi_code()) ? null : cplIn.getDebit_acct_no());
			bookAccoutingIn.setCash_trxn_ind(cplProperty.getCash_trxn_ind());
			bookAccoutingIn.setCcy_code(cplIn.getDebit_ccy_code());
			bookAccoutingIn.setTrxn_amt(cplProperty.getAct_withdrawal_amt()); // 使用实际支取金额
			bookAccoutingIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
			bookAccoutingIn.setGl_ref_code(cplProperty.getDebit_busi_code());
			bookAccoutingIn.setSummary_code(cplIn.getSummary_code());
			bookAccoutingIn.setTrxn_remark(cplIn.getTrxn_remark());
			bookAccoutingIn.setCustomer_remark(cplIn.getCustomer_remark());

			// 真实对手方信息
			bookAccoutingIn.setOpp_acct_route(cplProperty.getCredit_acct_analy());
			bookAccoutingIn.setOpp_acct_no(cplIn.getCredit_acct_no());
			bookAccoutingIn.setOpp_acct_ccy(cplIn.getCredit_ccy_code());
			bookAccoutingIn.setOpp_branch_id(cplIn.getCredit_acct_branch());

			// 调用内部户记账服务
			DpInsideAccountingOut bookAccountingOut = DpInsideAccountIobus.insideAccounting(bookAccoutingIn);

			cplOut.setDebit_acct_no(bookAccountingOut.getAcct_no());
			cplOut.setDebit_acct_name(bookAccountingOut.getAcct_name());
			cplOut.setDebit_ccy_code(bookAccountingOut.getCcy_code());
			cplOut.setDebit_acct_bal(bookAccountingOut.getAcct_bal());
		}
		// 存放同业
		else if (cplProperty.getDebit_acct_analy() == E_ACCOUTANALY.NOSTRO) {

			DpDemandSaveIn demandSaveIn = BizUtil.getInstance(DpDemandSaveIn.class);

			demandSaveIn.setAcct_no(cplIn.getDebit_acct_no());
			demandSaveIn.setAcct_type(cplIn.getDebit_acct_type());
			demandSaveIn.setAcct_name(cplIn.getDebit_acct_name());
			demandSaveIn.setProd_id(cplIn.getDebit_prod_id());
			demandSaveIn.setCcy_code(cplIn.getDebit_ccy_code());
			demandSaveIn.setBack_value_date(cplIn.getBack_value_date());
			demandSaveIn.setCash_trxn_ind(cplProperty.getCash_trxn_ind()); // 交易级现转标志
			demandSaveIn.setTrxn_amt(cplProperty.getAct_withdrawal_amt()); // 实际存入金额
			demandSaveIn.setSummary_code(cplIn.getSummary_code());
			demandSaveIn.setTrxn_remark(cplIn.getTrxn_remark());
			demandSaveIn.setCustomer_remark(cplIn.getCustomer_remark());
			demandSaveIn.setOpen_voch_check_ind(null);
			demandSaveIn.setAcct_hold_check_Ind(null); // 账户限制检查标志
			demandSaveIn.setRed_blue_word_ind(null); // 红蓝字记账
			demandSaveIn.setOpen_acct_save_ind(null);

			// 冻结信息
			demandSaveIn.setExtend_froze_no(cplIn.getExtend_froze_no()); // 续冻编号
			demandSaveIn.setFroze_kind_code(cplIn.getFroze_kind_code());
			demandSaveIn.setFroze_object_type(cplIn.getFroze_object_type());
			demandSaveIn.setFroze_amt(cplIn.getFroze_amt());
			demandSaveIn.setFroze_reason(cplIn.getFroze_reason());
			demandSaveIn.setFroze_due_date(cplIn.getFroze_due_date());

			// 真实对手方信息
			demandSaveIn.setOpp_acct_route(cplProperty.getCredit_acct_analy());
			demandSaveIn.setOpp_acct_ccy(cplIn.getCredit_ccy_code());
			demandSaveIn.setOpp_acct_type(cplIn.getCredit_acct_type());
			demandSaveIn.setOpp_acct_no(cplIn.getCredit_acct_no());
			demandSaveIn.setOpp_branch_id(cplIn.getCredit_acct_branch());

			// 对账单对手方信息
			demandSaveIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no()); 
			demandSaveIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name()); 
			demandSaveIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias()); 
			demandSaveIn.setReal_opp_country(cplIn.getReal_opp_country()); 
			demandSaveIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id()); 
			demandSaveIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name()); 
			demandSaveIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name()); 
			demandSaveIn.setReal_opp_remark(cplIn.getReal_opp_remark()); 

			// 代理人信息
			demandSaveIn.setAgent_country(cplIn.getAgent_country());
			demandSaveIn.setAgent_doc_no(cplIn.getAgent_doc_no());
			demandSaveIn.setAgent_doc_type(cplIn.getAgent_doc_type());
			demandSaveIn.setAgent_name(cplIn.getAgent_name());

			// 调用存款存入服务
			DpDemandSaveOut demandSaveOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandSave(demandSaveIn);

			// 交易输出
			cplOut.setCredit_acct_no(demandSaveOut.getAcct_no());
			cplOut.setCredit_acct_type(demandSaveOut.getAcct_type());
			cplOut.setCredit_acct_name(demandSaveOut.getAcct_name());
			cplOut.setCredit_prod_id(demandSaveOut.getProd_id());
			cplOut.setCredit_acct_bal(demandSaveOut.getAcct_bal());
			cplOut.setNew_froze_no(demandSaveOut.getFroze_no());

		}
		else {
			throw APPUB.E0026(DpDict.A.debit_acct_analy.getLongName(), cplProperty.getDebit_acct_analy().getValue());
		}
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月20日-上午11:09:33</li>
	 *         <li>功能说明：贷方记账处理</li>
	 *         </p>
	 * @param cplIn
	 * @param cplProperty
	 * @param cplOut
	 */
	private static void creditAccounting(DpTransferIn cplIn, DpTransferProperty cplProperty, DpTransferOut cplOut) {

		// 现金
		if (cplProperty.getCredit_acct_analy() == E_ACCOUTANALY.CASH) {

			// 现金服务输入
			DpCashAccountingIn cashAccountingIn = BizUtil.getInstance(DpCashAccountingIn.class);

			cashAccountingIn.setCcy_code(cplIn.getCredit_ccy_code());
			cashAccountingIn.setTrxn_amt(cplProperty.getAct_dept_amt());
			cashAccountingIn.setDebit_credit(E_DEBITCREDIT.CREDIT); // 借贷方向
			cashAccountingIn.setSummary_code(cplIn.getSummary_code());
			cashAccountingIn.setTrxn_remark(cplIn.getTrxn_remark());

			// 真实对手方信息
			cashAccountingIn.setOpp_acct_route(cplProperty.getDebit_acct_analy());
			cashAccountingIn.setOpp_branch_id(cplIn.getDebit_acct_branch());
			cashAccountingIn.setOpp_acct_ccy(cplIn.getDebit_ccy_code());
			cashAccountingIn.setOpp_acct_no(cplIn.getDebit_acct_no());

			// 调用公共现金记账服务
			DpCashIobus.cashAccounting(cashAccountingIn);
		}
		// 存款账户
		else if (cplProperty.getCredit_acct_analy() == E_ACCOUTANALY.DEPOSIT) {

			DpDemandSaveIn demandSaveIn = BizUtil.getInstance(DpDemandSaveIn.class);

			demandSaveIn.setAcct_no(cplIn.getCredit_acct_no());
			demandSaveIn.setAcct_type(cplIn.getCredit_acct_type());
			demandSaveIn.setAcct_name(cplIn.getCredit_acct_name());
			demandSaveIn.setProd_id(cplIn.getCredit_prod_id());
			demandSaveIn.setCcy_code(cplIn.getCredit_ccy_code());
			demandSaveIn.setBack_value_date(cplIn.getBack_value_date());
			demandSaveIn.setCash_trxn_ind(cplProperty.getCash_trxn_ind()); // 交易级现转标志
			demandSaveIn.setTrxn_amt(cplProperty.getAct_dept_amt()); // 实际存入金额
			demandSaveIn.setSummary_code(cplIn.getSummary_code());
			demandSaveIn.setTrxn_remark(cplIn.getTrxn_remark());
			demandSaveIn.setCustomer_remark(cplIn.getCustomer_remark());
			demandSaveIn.setOpen_voch_check_ind(null);
			demandSaveIn.setAcct_hold_check_Ind(null); // 账户限制检查标志
			demandSaveIn.setRed_blue_word_ind(null); // 红蓝字记账
			demandSaveIn.setOpen_acct_save_ind(null);

			// 冻结信息
			demandSaveIn.setExtend_froze_no(cplIn.getExtend_froze_no()); // 续冻编号
			demandSaveIn.setFroze_kind_code(cplIn.getFroze_kind_code());
			demandSaveIn.setFroze_object_type(cplIn.getFroze_object_type());
			demandSaveIn.setFroze_amt(cplIn.getFroze_amt());
			demandSaveIn.setFroze_reason(cplIn.getFroze_reason());
			demandSaveIn.setFroze_due_date(cplIn.getFroze_due_date());

			// 真实对手方信息
			demandSaveIn.setOpp_acct_route(cplProperty.getDebit_acct_analy());
			demandSaveIn.setOpp_acct_ccy(cplOut.getDebit_ccy_code());
			demandSaveIn.setOpp_acct_type(cplOut.getDebit_acct_type());
			demandSaveIn.setOpp_acct_no(cplOut.getDebit_acct_no());
			demandSaveIn.setOpp_branch_id(cplIn.getDebit_acct_branch());
			demandSaveIn.setTrxn_opp_amt(cplProperty.getAct_withdrawal_amt());
			demandSaveIn.setDebit_suspense_no(cplOut.getDebit_suspense_no());

			// 对账单对手方信息
			demandSaveIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
			demandSaveIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
			demandSaveIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
			demandSaveIn.setReal_opp_country(cplIn.getReal_opp_country());
			demandSaveIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
			demandSaveIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
			demandSaveIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
			demandSaveIn.setReal_opp_remark(cplIn.getReal_opp_remark());

			// 代理人信息
			demandSaveIn.setAgent_country(cplIn.getAgent_country());
			demandSaveIn.setAgent_doc_no(cplIn.getAgent_doc_no());
			demandSaveIn.setAgent_doc_type(cplIn.getAgent_doc_type());
			demandSaveIn.setAgent_name(cplIn.getAgent_name());

			// 境外消费交易地信息登记
			demandSaveIn.setTrxn_area(cplIn.getTrxn_area());
			demandSaveIn.setTrxn_area_amt(cplIn.getTrxn_area_amt());
			demandSaveIn.setTrxn_area_ccy(cplIn.getTrxn_area_ccy());
			demandSaveIn.setTrxn_area_exch_rate(cplIn.getTrxn_area_exch_rate());
			demandSaveIn.setConsume_date(cplIn.getConsume_date());

			// 调用存款存入服务
			DpDemandSaveOut demandSaveOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandSave(demandSaveIn);

			// 交易输出
			cplOut.setCredit_acct_no(demandSaveOut.getAcct_no());
			cplOut.setCredit_acct_type(demandSaveOut.getAcct_type());
			cplOut.setCredit_acct_name(demandSaveOut.getAcct_name());
			cplOut.setCredit_ccy_code(demandSaveOut.getCcy_code());
			cplOut.setCredit_prod_id(demandSaveOut.getProd_id());
			cplOut.setCredit_acct_bal(demandSaveOut.getAcct_bal());
			cplOut.setNew_froze_no(demandSaveOut.getFroze_no());
		}
		else if (cplProperty.getCredit_acct_analy() == E_ACCOUTANALY.SUSPENSE) {

			DpSuspenseAccountingIn suspenseIn = BizUtil.getInstance(DpSuspenseAccountingIn.class);

			suspenseIn.setSuspense_no(cplIn.getCredit_suspense_no());
			suspenseIn.setAcct_branch(cplIn.getCredit_acct_branch());
			suspenseIn.setCash_trxn_ind(cplProperty.getCash_trxn_ind()); // 交易级现转标志
			suspenseIn.setCcy_code(cplIn.getCredit_ccy_code());
			suspenseIn.setTrxn_amt(cplProperty.getAct_dept_amt());
			suspenseIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
			suspenseIn.setGl_ref_code(cplProperty.getCredit_busi_code());
			suspenseIn.setMulti_deposit_ind(cplIn.getCredit_multi_deposit_ind());
			suspenseIn.setMulti_draw_ind(cplIn.getCredit_multi_draw_ind());

			// 补充信息
			suspenseIn.setSummary_code(cplIn.getSummary_code());
			suspenseIn.setSummary_code(cplIn.getSummary_code());
			suspenseIn.setTrxn_remark(cplIn.getTrxn_remark());
			suspenseIn.setCustomer_remark(cplIn.getCustomer_remark());

			// 对手方信息
			suspenseIn.setOpp_acct_route(cplProperty.getDebit_acct_analy());
			suspenseIn.setOpp_acct_no(cplIn.getDebit_acct_no());
			suspenseIn.setOpp_acct_ccy(cplIn.getDebit_ccy_code());

			// 调用挂销账记账服务
			DpSuspenseAccountingOut suspenseOut = DpInsideAccountIobus.suspenseAccounting(suspenseIn);

			cplOut.setCredit_acct_no(suspenseOut.getAcct_no());
			cplOut.setCredit_ccy_code(suspenseOut.getCcy_code());
			cplOut.setCredit_acct_name(suspenseOut.getAcct_name());
			cplOut.setCredit_suspense_no(suspenseOut.getSuspense_no());
			cplOut.setCredit_acct_bal(suspenseOut.getAcct_bal());
		}
		else if (cplProperty.getCredit_acct_analy() == E_ACCOUTANALY.INSIDE) {

			DpInsideAccountingIn bookAccoutingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

			bookAccoutingIn.setAcct_branch(cplIn.getCredit_acct_branch());
			bookAccoutingIn.setAcct_no(CommUtil.equals(cplIn.getCredit_acct_no(), cplProperty.getCredit_busi_code()) ? null : cplIn.getCredit_acct_no());
			bookAccoutingIn.setCash_trxn_ind(cplProperty.getCash_trxn_ind()); // 交易级现转标志
			bookAccoutingIn.setCcy_code(cplIn.getCredit_ccy_code());
			bookAccoutingIn.setTrxn_amt(cplProperty.getAct_dept_amt());
			bookAccoutingIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
			bookAccoutingIn.setGl_ref_code(cplProperty.getCredit_busi_code());
			bookAccoutingIn.setSummary_code(cplIn.getSummary_code());
			bookAccoutingIn.setTrxn_remark(cplIn.getTrxn_remark());
			bookAccoutingIn.setCustomer_remark(cplIn.getCustomer_remark());

			// 真实对手方信息
			bookAccoutingIn.setOpp_acct_route(cplProperty.getDebit_acct_analy());
			bookAccoutingIn.setOpp_acct_no(cplIn.getDebit_acct_no());
			bookAccoutingIn.setOpp_acct_ccy(cplIn.getDebit_ccy_code());
			bookAccoutingIn.setOpp_branch_id(cplIn.getDebit_acct_branch());

			// 调用内部户记账服务
			DpInsideAccountingOut bookAccountingOut = DpInsideAccountIobus.insideAccounting(bookAccoutingIn);

			cplOut.setCredit_acct_no(bookAccountingOut.getAcct_no());
			cplOut.setCredit_ccy_code(bookAccountingOut.getCcy_code());
			cplOut.setCredit_acct_name(bookAccountingOut.getAcct_name());
			cplOut.setCredit_acct_bal(bookAccountingOut.getAcct_bal());
		}
		// 存放同业
		else if (cplProperty.getCredit_acct_analy() == E_ACCOUTANALY.NOSTRO) {
			// 活期支取记账输入
			DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			demandDrawIn.setAcct_no(cplIn.getCredit_acct_no());
			demandDrawIn.setAcct_type(cplIn.getCredit_acct_type());
			demandDrawIn.setAcct_name(cplIn.getCredit_acct_name());
			demandDrawIn.setProd_id(cplIn.getCredit_prod_id());
			demandDrawIn.setCheck_password_ind(cplIn.getCheck_password_ind());
			demandDrawIn.setTrxn_password(cplIn.getTrxn_password());
			demandDrawIn.setForce_draw_ind(cplIn.getForce_draw_ind());
			demandDrawIn.setCcy_code(cplIn.getCredit_ccy_code());
			demandDrawIn.setTrxn_amt(cplProperty.getAct_dept_amt()); // 使用实际支取金额
			demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.COMMON); // 普通支取
			demandDrawIn.setBack_value_date(cplIn.getBack_value_date());
			demandDrawIn.setCash_trxn_ind(cplProperty.getCash_trxn_ind()); // 交易级现转标志
			demandDrawIn.setChrg_code(null); // 费用编号，不是费用模块调用传空值
			demandDrawIn.setOpen_voch_check_ind(null);
			demandDrawIn.setRed_blue_word_ind(null); // 红蓝字记账
			demandDrawIn.setAcct_hold_check_Ind(null); // 账户限制检查标志
			demandDrawIn.setSummary_code(cplIn.getSummary_code());
			demandDrawIn.setTrxn_remark(cplIn.getTrxn_remark());
			demandDrawIn.setCustomer_remark(cplIn.getCustomer_remark());

			// 解冻信息
			demandDrawIn.setFroze_no(cplIn.getFroze_no());
			demandDrawIn.setUnfroze_amt(cplIn.getUnfroze_amt());
			demandDrawIn.setUnfroze_reason(cplIn.getUnfroze_reason());

			// 对手方
			demandDrawIn.setOpp_acct_ccy(cplOut.getDebit_ccy_code());
			demandDrawIn.setOpp_acct_no(cplOut.getDebit_acct_no());
			demandDrawIn.setOpp_acct_route(cplProperty.getDebit_acct_analy());
			demandDrawIn.setOpp_acct_type(cplOut.getDebit_acct_type());
			demandDrawIn.setOpp_branch_id(cplIn.getDebit_acct_branch());

			// 对账单对手方
			demandDrawIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
			demandDrawIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
			demandDrawIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
			demandDrawIn.setReal_opp_country(cplIn.getReal_opp_country());
			demandDrawIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
			demandDrawIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
			demandDrawIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
			demandDrawIn.setReal_opp_remark(cplIn.getReal_opp_remark());

			// 代理人信息
			demandDrawIn.setAgent_country(cplIn.getAgent_country());
			demandDrawIn.setAgent_doc_no(cplIn.getAgent_doc_no());
			demandDrawIn.setAgent_doc_type(cplIn.getAgent_doc_type());
			demandDrawIn.setAgent_name(cplIn.getAgent_name());

			// 调用活期存款支取服务
			DpDemandDrawOut demandDrawOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);

			// 更新实际支取金额
			cplProperty.setAct_withdrawal_amt(demandDrawOut.getAct_withdrawal_amt());

			// 交易输出
			cplOut.setDebit_acct_no(demandDrawOut.getAcct_no());
			cplOut.setDebit_acct_type(demandDrawOut.getAcct_type());
			cplOut.setDebit_acct_name(demandDrawOut.getAcct_name());
			cplOut.setDebit_prod_id(demandDrawOut.getProd_id());
			cplOut.setDebit_ccy_code(demandDrawOut.getCcy_code());
			cplOut.setDebit_froze_bal(demandDrawOut.getFroze_bal());
			cplOut.setDebit_acct_bal(demandDrawOut.getAcct_bal());
		}
		else {
			throw APPUB.E0026(DpDict.A.credit_acct_analy.getLongName(), cplProperty.getCredit_acct_analy().getValue());
		}

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月21日-上午10:32:01</li>
	 *         <li>功能说明：外汇交易中间服务</li>
	 *         </p>
	 * @param cplIn
	 * @param cplProperty
	 * @param cplOut
	 */
	private static void forexTrxnMiddleService(DpTransferIn cplIn, DpTransferProperty cplProperty, DpTransferOut cplOut) {

		// 结售汇中间记账服务输入
		DpExchangeAccountingIn fxExchangeIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

		fxExchangeIn.setBuy_cash_ind(cplIn.getDebit_cash_trxn_ind());
		fxExchangeIn.setBuy_ccy_code(cplIn.getDebit_ccy_code());
		fxExchangeIn.setSell_cash_ind(cplIn.getCredit_cash_trxn_ind());
		fxExchangeIn.setSell_ccy_code(cplIn.getCredit_ccy_code());
		fxExchangeIn.setExch_rate(cplIn.getExch_rate());
		fxExchangeIn.setForex_agree_price_id(cplIn.getForex_agree_price_id());
		fxExchangeIn.setExch_rate_path(cplIn.getExch_rate_path());
		fxExchangeIn.setForex_agree_price_id(cplIn.getForex_agree_price_id());

		E_FOREXEXCHOBJECT subjectType = E_FOREXEXCHOBJECT.SELF;
		if (CommUtil.in(cplProperty.getDebit_acct_analy(), E_ACCOUTANALY.CASH, E_ACCOUTANALY.DEPOSIT)) {
			subjectType = E_FOREXEXCHOBJECT.CUSTOMER;
		}
		fxExchangeIn.setForex_exch_object_type(subjectType);

		// 交易币种等于借方币种
		if (CommUtil.equals(cplIn.getTrxn_ccy(), cplIn.getDebit_ccy_code())) {
			// 属性区取实际支取金额, 因为借方可能强制借记
			fxExchangeIn.setBuy_amt(cplProperty.getAct_withdrawal_amt());
			fxExchangeIn.setSell_amt(cplIn.getTrxn_opp_amt());
		}
		else {

			fxExchangeIn.setBuy_amt(cplIn.getTrxn_opp_amt());
			fxExchangeIn.setSell_amt(cplIn.getTrxn_amt());
		}

		// 客户类型对现钞兑换有用
		fxExchangeIn.setCust_type(cplIn.getCust_type());
		fxExchangeIn.setCountry_code(cplIn.getCountry_code());
		fxExchangeIn.setCustomer_remark(cplIn.getCustomer_remark());
		fxExchangeIn.setSummary_code(cplIn.getSummary_code());
		fxExchangeIn.setTrxn_remark(cplIn.getTrxn_remark());

		// 买卖双方账户信息
		fxExchangeIn.setSell_acct_no(cplIn.getCredit_acct_no());
		fxExchangeIn.setSell_sub_acct_seq(null);
		fxExchangeIn.setBuy_acct_no(cplIn.getDebit_acct_no());
		fxExchangeIn.setBuy_sub_acct_seq(null);

		// 代理人信息
		fxExchangeIn.setAgent_country(cplIn.getAgent_country());
		fxExchangeIn.setAgent_doc_no(cplIn.getAgent_doc_no());
		fxExchangeIn.setAgent_doc_type(cplIn.getAgent_doc_type());
		fxExchangeIn.setAgent_name(cplIn.getAgent_name());

		// 调用结售汇中间记账服务
		DpExchangeAccountingOut fxExchangeOut = DpExchangeIobus.exchangeAccounting(fxExchangeIn);
		
		// 跨币种转账实际存入金额等于卖出金额
		cplProperty.setAct_dept_amt(fxExchangeOut.getSell_amt());

		// 输出
		cplOut.setBuy_acct_no(fxExchangeIn.getBuy_acct_no());
		cplOut.setBuy_amt(fxExchangeOut.getBuy_amt());
		cplOut.setBuy_ccy_code(fxExchangeIn.getBuy_ccy_code());
		cplOut.setExch_rate(fxExchangeOut.getExch_rate());
		cplOut.setSell_acct_no(fxExchangeIn.getSell_acct_no());
		cplOut.setSell_amt(fxExchangeOut.getSell_amt());
		cplOut.setSell_ccy_code(fxExchangeIn.getSell_ccy_code());
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月21日-上午10:32:01</li>
	 *         <li>功能说明：交易预检查</li>
	 *         </p>
	 * @param cplIn
	 *            输入接口
	 */
	private static void trxnAdvanceCheck(DpTransferIn cplIn) {

		// 必输性检查
		BizUtil.fieldNotNull(cplIn.getDebit_cash_trxn_ind(), DpDict.A.debit_cash_trxn_ind.getId(), DpDict.A.debit_cash_trxn_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getCredit_cash_trxn_ind(), DpDict.A.credit_cash_trxn_ind.getId(), DpDict.A.credit_cash_trxn_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getTrxn_ccy(), SysDict.A.trxn_ccy.getId(), SysDict.A.trxn_ccy.getLongName());
		BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

		// 借方币种、贷方币种未传入则认为是交易币种
		if (CommUtil.isNull(cplIn.getDebit_ccy_code())) {
			cplIn.setDebit_ccy_code(cplIn.getTrxn_ccy());
		}

		if (CommUtil.isNull(cplIn.getCredit_ccy_code())) {
			cplIn.setCredit_ccy_code(cplIn.getTrxn_ccy());
		}

		// 借记贷记均为现金时， 借贷双方币种必须不一致，即外币现金兑换
		if (cplIn.getDebit_cash_trxn_ind() == E_CASHTRXN.CASH && cplIn.getCredit_cash_trxn_ind() == E_CASHTRXN.CASH) {

			if (CommUtil.equals(cplIn.getDebit_ccy_code(), cplIn.getCredit_ccy_code())) {
				throw DpErr.Dp.E0099();
			}
		}

		// 交易币种必须是借方币种或贷方币种
		if (!CommUtil.in(cplIn.getTrxn_ccy(), cplIn.getDebit_ccy_code(), cplIn.getCredit_ccy_code())) {
			throw DpErr.Dp.E0015();
		}

		// 客户一致性检查
		if (cplIn.getCheck_same_cust_ind() == E_YESORNO.YES) {

			// 验证同一客户，转出方账户名称与转入方账户名称需都输入
			if (CommUtil.isNull(cplIn.getDebit_acct_name()) || CommUtil.isNull(cplIn.getCredit_acct_name())) {
				throw DpErr.Dp.E0096();
			}

			// 验证同一客户，转出方账户名称与转入方账户名称必需一致
			if (!CommUtil.equals(cplIn.getDebit_acct_name(), cplIn.getCredit_acct_name())) {
				throw DpErr.Dp.E0097();
			}
		}

		// 交易币种跟贷方币种一致，不能强制借记
		if (CommUtil.equals(cplIn.getTrxn_ccy(), cplIn.getCredit_ccy_code())) {

			if (cplIn.getForce_draw_ind() == E_YESORNO.YES) {
				throw DpErr.Dp.E0037();
			}
		}

		// 同币种交易
		if (CommUtil.equals(cplIn.getDebit_ccy_code(), cplIn.getCredit_ccy_code())) {

			// 同币种交易，交易对手方金额不为空时须与交易金额相等
			if (CommUtil.isNotNull(cplIn.getTrxn_opp_amt()) && !CommUtil.equals(cplIn.getTrxn_opp_amt(), BigDecimal.ZERO)) {

				if (!CommUtil.equals(cplIn.getTrxn_opp_amt(), cplIn.getTrxn_amt())) {
					throw DpErr.Dp.E0039();
				}
			}
		}
	}

	private static void trxnMiddleCheck(DpTransferIn cplIn, DpTransferProperty cplProperty) {

		// 内部户交易标志为是,借贷双方必须都是内部账户
		if (cplIn.getOnly_inac_trxn_ind() == E_YESORNO.YES) {

			if (cplProperty.getDebit_acct_analy() != E_ACCOUTANALY.INSIDE) {

				throw DpErr.Dp.E0275(cplIn.getDebit_acct_no(), E_ACCOUTANALY.INSIDE);
			}
			if (cplProperty.getCredit_acct_analy() != E_ACCOUTANALY.INSIDE) {

				throw DpErr.Dp.E0275(cplIn.getCredit_acct_no(), E_ACCOUTANALY.INSIDE);
			}
		}

		// 不予许负债账户与存放同业互转
		if ((cplProperty.getDebit_acct_analy() == E_ACCOUTANALY.DEPOSIT && cplProperty.getCredit_acct_analy() == E_ACCOUTANALY.NOSTRO)
				|| (cplProperty.getDebit_acct_analy() == E_ACCOUTANALY.NOSTRO && cplProperty.getCredit_acct_analy() == E_ACCOUTANALY.DEPOSIT)) {

			throw DpErr.Dp.E0098();
		}

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月21日-上午10:32:01</li>
	 *         <li>功能说明：同账户转账检查</li>
	 *         </p>
	 * @param cplIn
	 *            输入接口
	 */
	private static void checkSameAcct(DpTransferIn cplIn, DpTransferProperty cplProperty, DpTransferOut cplOut) {

		bizlog.method(" DpTransactions.checkSameAcct begin >>>>>>>>>>>>>>>>");
		bizlog.debug("DpTransferIn =[%s], DpTransferProperty =[%s], DpTransferOut =[%s]", cplIn, cplProperty, cplOut);

		// 不准许存款账户同币种之间互转，同账户跨币种互转可以，即为结售汇
		if (cplProperty.getDebit_acct_analy() == E_ACCOUTANALY.DEPOSIT && cplProperty.getCredit_acct_analy() == E_ACCOUTANALY.DEPOSIT) {

			if (CommUtil.equals(cplOut.getDebit_acct_no(), cplOut.getCredit_acct_no()) && CommUtil.equals(cplIn.getTrxn_ccy(), cplIn.getCredit_ccy_code())
					&& CommUtil.equals(cplOut.getDebit_prod_id(), cplOut.getCredit_prod_id())) {

				throw DpErr.Dp.E0100();
			}
		}

		bizlog.method(" DpTransactions.checkSameAcct end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-上午10:16:30</li>
	 *         <li>功能说明：表外记账</li>
	 *         </p>
	 * @param cplIn
	 *            表外记账输入接口
	 * @param cplOut
	 *            表外记账输出接口
	 * @return 是否有发生表外记账 true: 发生 false: 未发生
	 */
	private static boolean offPosting(DpTransferIn cplIn, DpTransferOut cplOut) {

		// 借方现转标志、贷方现转标志都为空，则报错
		if (cplIn.getDebit_cash_trxn_ind() == null && cplIn.getCredit_cash_trxn_ind() == null) {
			throw DpErr.Dp.E0040();
		}

		// 有一方未录入现转标志，则认为是记表外账
		if (cplIn.getDebit_cash_trxn_ind() == null || cplIn.getCredit_cash_trxn_ind() == null) {

			// 内部户记账服务输入
			DpInsideAccountingIn bookAccoutingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

			if (cplIn.getDebit_cash_trxn_ind() != null) {

				DpAccountRouteInfo cplAnaly = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getDebit_acct_no(), cplIn.getDebit_cash_trxn_ind(), cplIn.getDebit_suspense_no());

				bookAccoutingIn.setAcct_branch(cplIn.getDebit_acct_branch());
				bookAccoutingIn.setAcct_no(cplAnaly.getAcct_no());
				bookAccoutingIn.setCash_trxn_ind(cplIn.getDebit_cash_trxn_ind());
				bookAccoutingIn.setGl_ref_code(cplAnaly.getGl_ref_code());
				bookAccoutingIn.setDebit_credit(E_DEBITCREDIT.DEBIT);

			}
			else {

				DpAccountRouteInfo cplAnaly = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getCredit_acct_no(), cplIn.getCredit_cash_trxn_ind(), cplIn.getCredit_suspense_no());

				bookAccoutingIn.setAcct_branch(cplIn.getCredit_acct_branch());
				bookAccoutingIn.setAcct_no(cplAnaly.getAcct_no());
				bookAccoutingIn.setCash_trxn_ind(cplIn.getCredit_cash_trxn_ind());
				bookAccoutingIn.setGl_ref_code(cplAnaly.getGl_ref_code());
				bookAccoutingIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
			}

			bookAccoutingIn.setCcy_code(cplIn.getTrxn_ccy());
			bookAccoutingIn.setTrxn_amt(cplIn.getTrxn_amt());
			bookAccoutingIn.setSummary_code(cplIn.getSummary_code());
			bookAccoutingIn.setTrxn_remark(cplIn.getTrxn_remark());
			bookAccoutingIn.setCustomer_remark(cplIn.getCustomer_remark());

			// 调用内部户记账服务记表外账
			DpInsideAccountingOut bookAccountingOut = DpInsideAccountIobus.insideAccounting(bookAccoutingIn);
			
			if (cplIn.getDebit_cash_trxn_ind() != null) {

				cplOut.setDebit_acct_no(bookAccountingOut.getAcct_no());
				cplOut.setDebit_acct_name(bookAccountingOut.getAcct_name());
				cplOut.setDebit_ccy_code(bookAccountingOut.getCcy_code());
				cplOut.setDebit_acct_bal(bookAccountingOut.getAcct_bal());
			}
			else {

				cplOut.setCredit_acct_no(bookAccountingOut.getAcct_no());
				cplOut.setCredit_acct_name(bookAccountingOut.getAcct_name());
				cplOut.setCredit_acct_bal(bookAccountingOut.getAcct_bal());
			}

			// 发生表外记账
			return true;
		}

		// 没有发生表外记账
		return false;
	}

	/**
	 * @Author qiuhan
	 *         <p>
	 *         <li>2017年8月8日-下午16:14:33</li>
	 *         <li>功能说明：添加收费信息集</li>
	 *         </p>
	 * @param StChequeBookApplyIn
	 */
	public static void addChrgDataMart(DpTransferIn cplIn) {

		String custNo = "";
		E_CUSTOMERTYPE custType = null;

		/* 添加输入信息集 */
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		/* 添加账户信息集 */
		if (CommUtil.isNotNull(cplIn.getCredit_acct_no())) {
			
			DpaAccount acctInfo = DpToolsApi.accountInquery(cplIn.getCredit_acct_no(), null);

			ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));
			
			if(CommUtil.isNotNull(cplIn.getCredit_ccy_code())){
				
				DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
				
				accessIn.setAcct_no(acctInfo.getAcct_no());
				accessIn.setCcy_code(cplIn.getCredit_ccy_code());
				accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
				
				DpAcctAccessOut accessOut = DpToolsApi.subAcctInquery(accessIn);
				
				DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctInfo.getAcct_no(), accessOut.getSub_acct_no(), true);
			
				ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));
			}

			custNo = acctInfo.getCust_no();
			custType = acctInfo.getCust_type();
		}

		/* 添加客户信息集 */
		if (CommUtil.isNotNull(custNo)) {

			// 查询客户信息
			DpCustBaseInfo custInfo = DpCustomerIobus.getCustBaseInfo(custNo, custType);

			ApBufferApi.addData(ApConst.CUST_DATA_MART, custInfo.getCustInfo());
		}
	}
}
