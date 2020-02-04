package cn.sunline.icore.dp.serv.common;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_TRXNSTATUS;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApReversalApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpAccountBase.DpUpdFloatAmtBaseOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ACCTFORM;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.maintain.DpAccountFormMove;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBill;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBillDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdFloatAmtIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdFloatAmtOut;
import cn.sunline.icore.dp.serv.type.ComDpReversal.DpAcctFloatAmtReversalIn;
import cn.sunline.icore.dp.serv.type.ComDpSettleVoucher.DpFloatAmountServiceIn;
import cn.sunline.icore.dp.serv.type.ComDpSettleVoucher.DpFloatAmountServiceOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpAccountingFloatAmt {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAccountingFloatAmt.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月17日-下午1:49:31</li>
	 *         <li>功能说明：浮动金额记账服务</li>
	 *         </p>
	 * @param cplIn
	 *            结算凭证浮动金额记账服务输入
	 * @retrun cplOut 结算凭证浮动金额记账服务输出
	 */
	public static DpFloatAmountServiceOut floatAmountSerive(DpFloatAmountServiceIn cplIn) {

		bizlog.method(" DpAccountingFloatAmt.floatAmountSerive begin >>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(cplIn.getFloat_amt(), SysDict.A.float_amt.getId(), SysDict.A.float_amt.getLongName());
		BizUtil.fieldNotNull(cplIn.getDebit_credit(), SysDict.A.debit_credit.getId(), SysDict.A.debit_credit.getLongName());
		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setProd_id(cplIn.getProd_id());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 子账号信息: 外围要确保已经Lock
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		if (subAccount.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		// 账户限制状态检查 与 账户存入支取控制
		if (cplIn.getDebit_credit() == E_DEBITCREDIT.CREDIT) {

			DpPublicCheck.checkSubAcctTrxnLimit(subAccount, E_DEPTTRXNEVENT.DP_SAVE, null);
		}
		else {

			DpPublicCheck.checkSubAcctTrxnLimit(subAccount, E_DEPTTRXNEVENT.DP_DRAW, null);
		}

		DpUpdFloatAmtIn cplUpdBalIn = BizUtil.getInstance(DpUpdFloatAmtIn.class);

		cplUpdBalIn.setCard_no(CommUtil.equals(acctAccessOut.getAcct_no(), cplIn.getAcct_no()) ? null : cplIn.getAcct_no());
		cplUpdBalIn.setAcct_no(subAccount.getAcct_no());
		cplUpdBalIn.setSub_acct_no(subAccount.getSub_acct_no());
		cplUpdBalIn.setTrxn_event_id(cplIn.getTrxn_event_id());
		cplUpdBalIn.setTrxn_ccy(subAccount.getCcy_code());
		cplUpdBalIn.setFloat_amt(cplIn.getFloat_amt());
		cplUpdBalIn.setDebit_credit(cplIn.getDebit_credit()); // debit credit
		cplUpdBalIn.setSummary_code(cplIn.getSummary_code()); // summary code
		cplUpdBalIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		cplUpdBalIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		cplUpdBalIn.setOpp_acct_type(cplIn.getOpp_acct_type());
		cplUpdBalIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		cplUpdBalIn.setOpp_acct_name(cplIn.getOpp_acct_name());
		cplUpdBalIn.setOpp_branch_id(cplIn.getOpp_branch_id());
		cplUpdBalIn.setTrxn_remark(cplIn.getTrxn_remark());
		cplUpdBalIn.setCustomer_remark(cplIn.getCustomer_remark());
		cplUpdBalIn.setAgent_doc_type(cplIn.getAgent_doc_type());
		cplUpdBalIn.setAgent_doc_no(cplIn.getAgent_doc_no());
		cplUpdBalIn.setAgent_name(cplIn.getAgent_name()); // agent name
		cplUpdBalIn.setAgent_country(cplIn.getAgent_country()); // agent country
		cplUpdBalIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
		cplUpdBalIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
		cplUpdBalIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
		cplUpdBalIn.setReal_opp_country(cplIn.getReal_opp_country());
		cplUpdBalIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
		cplUpdBalIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
		cplUpdBalIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
		cplUpdBalIn.setReal_opp_remark(cplIn.getReal_opp_remark());

		// 浮动余额记账
		DpUpdFloatAmtOut cplUpdBalOut = updAccFloatBal(cplUpdBalIn);

		// 输出
		DpFloatAmountServiceOut cplOut = BizUtil.getInstance(DpFloatAmountServiceOut.class);

		cplOut.setAcct_bal(cplUpdBalOut.getAcct_bal());
		cplOut.setAcct_float_bal(cplUpdBalOut.getAcct_float_bal());
		cplOut.setAcct_name(acctAccessOut.getAcct_name());
		cplOut.setAcct_no(acctAccessOut.getAcct_no());
		cplOut.setCcy_code(acctAccessOut.getCcy_code());
		cplOut.setFact_posting_date(cplUpdBalOut.getFact_posting_date());
		cplOut.setFloat_amt(cplUpdBalOut.getFloat_amt());
		cplOut.setSub_acct_seq(acctAccessOut.getSub_acct_seq());

		bizlog.method(" DpAccountingFloatAmt.floatAmountSerive end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月17日-下午1:49:31</li>
	 *         <li>功能说明：更新账户浮动余额： 外面子账户信息表已带锁</li>
	 *         </p>
	 * @param cplIn
	 *            结算凭证浮动额更新输入接口
	 * @retrun cplOut 结算凭证浮动额更新输入接口
	 */
	public static DpUpdFloatAmtOut updAccFloatBal(DpUpdFloatAmtIn cplIn) {

		bizlog.method(" DpAccountingFloatAmt.updAccFloatBal begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		DpUpdFloatAmtBaseOut cplBaseOut = DpBaseServiceApi.floatBalTally(cplIn);

		// 记账后再读一遍子账号信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(cplIn.getAcct_no(), cplIn.getSub_acct_no(), true);

		// 动户账户形态变更
		if (subAccount.getAcct_form() != E_ACCTFORM.NORMAL) {

			DpAccountFormMove.acctountFormMove(subAccount, E_ACCTFORM.NORMAL);
		}

		/* 登记浮动额账单信息 */
		DpsBill billInfo = regSettleVocherBill(cplIn);

		/* 登记冲正事件 */
		DpAcctFloatAmtReversalIn reversalIn = BizUtil.getInstance(DpAcctFloatAmtReversalIn.class);

		reversalIn.setSub_acct_no(subAccount.getSub_acct_no());
		reversalIn.setAcct_no(subAccount.getAcct_no());
		reversalIn.setFloat_amt(cplIn.getFloat_amt());
		reversalIn.setDebit_credit(cplIn.getDebit_credit());
		reversalIn.setSerial_no(billInfo.getSerial_no());
		reversalIn.setOriginal_trxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());

		ApReversalApi.register("deptAcctFloatAmount", reversalIn);

		// 输出
		DpUpdFloatAmtOut cplOut = BizUtil.getInstance(DpUpdFloatAmtOut.class);

		cplOut.setSub_acct_no(subAccount.getSub_acct_no());
		cplOut.setFloat_amt(cplIn.getFloat_amt());
		cplOut.setAcct_bal(subAccount.getAcct_bal());// 记账后账户余额
		cplOut.setAcct_float_bal(subAccount.getAcct_float_bal());
		cplOut.setFact_posting_date(cplBaseOut.getFact_posting_date());// 应入账日期

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAccountingFloatAmt.updAccFloatBal end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月17日-下午1:55:26</li>
	 *         <li>功能说明：浮动额账单登记</li>
	 *         </p>
	 * @param cplInput
	 *            结算凭证浮动额更新输入接口
	 * @return 账单信息
	 */
	private static DpsBill regSettleVocherBill(DpUpdFloatAmtIn cplInput) {

		bizlog.method(" DpAccountingFloatAmt.regSettleVocherBill begin >>>>>>>>>>>>>>>>");

		// 交易运行变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 读取子账户缓存信息: 获取新的数据版本号
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(cplInput.getAcct_no(), cplInput.getSub_acct_no(), true);

		DpsBill billInfo = BizUtil.getInstance(DpsBill.class);

		/* 账单关键信息 */
		billInfo.setSub_acct_no(subAccount.getSub_acct_no());
		billInfo.setSerial_no(subAccount.getData_version()); // 子账户表的数据版本号做序号
		billInfo.setBack_value_date(null);
		billInfo.setShow_ind(E_YESORNO.YES);
		billInfo.setTally_record_ind(E_YESORNO.YES);
		billInfo.setTrxn_record_type(E_TRXNRECORDTYPE.FLOAT);
		billInfo.setCash_trxn_ind(E_CASHTRXN.TRXN);
		billInfo.setDebit_credit(cplInput.getDebit_credit());
		billInfo.setTrxn_ccy(cplInput.getTrxn_ccy());
		billInfo.setTrxn_amt(cplInput.getFloat_amt());
		billInfo.setBal_after_trxn(subAccount.getAcct_float_bal().add(subAccount.getAcct_bal()));
		billInfo.setTrxn_date(subAccount.getBal_update_date()); // 为了保证连续不能用交易日期
		billInfo.setFee_code(null); // 费用编号
		billInfo.setTrxn_remark(cplInput.getTrxn_remark());
		billInfo.setCustomer_remark(cplInput.getCustomer_remark());
		billInfo.setSummary_code(cplInput.getSummary_code());
		billInfo.setSummary_name(ApSummaryApi.getText(cplInput.getSummary_code()));
		billInfo.setTrxn_status(E_TRXNSTATUS.NORMAL);

		/* 交易主体信息 */
		billInfo.setCard_no(cplInput.getCard_no());
		billInfo.setAcct_no(subAccount.getAcct_no());
		billInfo.setSub_acct_seq(subAccount.getSub_acct_seq());
		billInfo.setProd_id(subAccount.getProd_id());
		billInfo.setAcct_name(subAccount.getSub_acct_name());
		billInfo.setAcct_branch(subAccount.getSub_acct_branch());
		billInfo.setAcct_branch_name(ApBranchApi.getItem(subAccount.getSub_acct_branch()).getBranch_name());
		billInfo.setCust_no(subAccount.getCust_no());
		billInfo.setCust_type(subAccount.getCust_type());

		/* 对手方信息 */
		billInfo.setOpp_acct_route(cplInput.getOpp_acct_route());
		billInfo.setOpp_acct_no(cplInput.getOpp_acct_no());
		billInfo.setOpp_acct_name(cplInput.getOpp_acct_name());
		billInfo.setOpp_branch_id(cplInput.getOpp_branch_id());
		billInfo.setOpp_acct_ccy(cplInput.getOpp_acct_ccy());
		billInfo.setOpp_sub_acct_seq("");
		billInfo.setOpp_trxn_amt(null);

		/* 交易环境信息 */
		billInfo.setTrxn_code(runEnvs.getTrxn_code());
		billInfo.setRecon_code(runEnvs.getRecon_code());
		billInfo.setThird_party_date(runEnvs.getInitiator_date());
		billInfo.setTrxn_channel(runEnvs.getChannel_id());
		billInfo.setTrxn_date(CommUtil.nvl(billInfo.getTrxn_date(), runEnvs.getTrxn_date()));
		billInfo.setBusi_seq(runEnvs.getBusi_seq());
		billInfo.setTrxn_seq(runEnvs.getTrxn_seq());
		billInfo.setTrxn_branch(runEnvs.getTrxn_branch());
		billInfo.setTrxn_teller(runEnvs.getTrxn_teller());
		billInfo.setTrxn_time(runEnvs.getComputer_time());
		billInfo.setHost_date(runEnvs.getComputer_date());

		/* 冲正信息 */
		billInfo.setReversal_ind(runEnvs.getReversal_ind());

		if (CommUtil.isNotNull(cplInput.getReversal_type())) {
			billInfo.setClear_accounts_ind(CommUtil.nvl(cplInput.getClear_accounts_ind(), E_YESORNO.NO));
			billInfo.setOriginal_trxn_date(cplInput.getOriginal_trxn_date());
			billInfo.setOriginal_busi_seq(cplInput.getOriginal_busi_seq());
			billInfo.setOriginal_trxn_seq(cplInput.getOriginal_trxn_seq());
		}

		/* 代理人或经办人信息 */
		billInfo.setAgent_doc_type(cplInput.getAgent_doc_type());
		billInfo.setAgent_doc_no(cplInput.getAgent_doc_no());
		billInfo.setAgent_name(cplInput.getAgent_name());
		billInfo.setAgent_country(cplInput.getAgent_country());

		/* 接口上送对手方信息, 通常为系统外账户信息 */
		billInfo.setReal_opp_acct_no(cplInput.getReal_opp_acct_no()); // real
																		// opponent
																		// account
																		// no
		billInfo.setReal_opp_acct_name(cplInput.getReal_opp_acct_name()); // real
																			// opponent
																			// account
																			// name
		billInfo.setReal_opp_acct_alias(cplInput.getReal_opp_acct_alias()); // real
																			// opponent
																			// account
																			// name
		billInfo.setReal_opp_country(cplInput.getReal_opp_country()); // real
																		// opponent
																		// country
		billInfo.setReal_opp_bank_id(cplInput.getReal_opp_bank_id()); // real
																		// opponent
																		// bank
																		// id
		billInfo.setReal_opp_bank_name(cplInput.getReal_opp_bank_name()); // real
																			// opponent
																			// bank
																			// name
		billInfo.setReal_opp_branch_name(cplInput.getReal_opp_branch_name()); // real
																				// opponent
																				// branch
																				// name
		billInfo.setReal_opp_remark(cplInput.getReal_opp_remark()); // real
																	// opponent
																	// remark
		billInfo.setVat_amt(BigDecimal.ZERO);
		billInfo.setVat_rate(BigDecimal.ZERO);
		billInfo.setAccounting_alias(subAccount.getAccounting_alias());

		// 交易对手方处理
		DpAccounting.trxnCounterparty(billInfo);

		// 登记账单
		DpsBillDao.insert(billInfo);

		bizlog.method(" DpAccountingFloatAmt.regSettleVocherBill end <<<<<<<<<<<<<<<<");

		return billInfo;
	}
}
