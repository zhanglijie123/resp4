package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpParmeterMart;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.iobus.DpTaxRelateIobus;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbStampTax;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbStampTaxDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpTax.DpStampTaxIn;
import cn.sunline.icore.dp.serv.type.ComDpTax.DpStampTaxOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAccountRouteInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpStampTaxInfo;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：印花税相关处理类
 * </p>
 * 
 * @Author maold
 *         <p>
 *         <li>2018年5月7日-下午3:38:16</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2018年5月7日-maold：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpStampTax {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpStampTax.class);

	/**
	 * @Author maold
	 *         <p>
	 *         <li>2018年5月30日-下午2:11:49</li>
	 *         <li>功能说明：印花税服务</li>
	 *         </p>
	 * @param stampTaxAccountIn
	 *            印花税服务输入
	 * @return 印花税服务输出
	 */
	public static DpStampTaxOut stampTaxAccounting(DpStampTaxIn stampTaxAccountIn) {
		bizlog.method(" DpStampTax.stampTaxAccounting begin >>>>>>>>>>>>>>>>");
		bizlog.debug("stampTaxAccountIn=[%s]", stampTaxAccountIn);

		// 输出对象
		DpStampTaxOut stampTaxOut = BizUtil.getInstance(DpStampTaxOut.class);

		stampTaxOut.setStamp_tax_amt(BigDecimal.ZERO);

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(stampTaxAccountIn.getAcct_no());
		acctAccessIn.setAcct_type(stampTaxAccountIn.getAcct_type());
		acctAccessIn.setCcy_code(stampTaxAccountIn.getCcy_code());
		acctAccessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);
		acctAccessIn.setSub_acct_seq(stampTaxAccountIn.getSub_acct_seq());

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 带锁获取子账户信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 加载数据区
		addBuffer(stampTaxAccountIn, subAccount);

		// 定位印花税税代码
		String stampTaxCode = ApRuleApi.getFirstResultByScene(DpConst.STAMP_TAX_CODE_RULE_SCENE_CODE);

		// 没有定位到印花税代码说明不需要收税
		if (CommUtil.isNull(stampTaxCode)) {
			return stampTaxOut;
		}

		// 获取印花税信息
		DpStampTaxInfo stampTaxInfo = DpTaxRelateIobus.getStampTaxInfo(stampTaxCode, subAccount.getCcy_code(), stampTaxAccountIn.getTrxn_amt(), subAccount.getTerm_code());

		// 印花税不为零 记账
		if (CommUtil.compare(stampTaxInfo.getStamp_tax_amt(), BigDecimal.ZERO) == 0) {
			return stampTaxOut;
		}

		if (stampTaxInfo.getBank_assume_ind() == E_YESORNO.YES) {
			// 银行垫付印花税
			bankAssumeStampTax(subAccount, stampTaxAccountIn, stampTaxInfo);

		}
		else {
			// 自身承担印花税
			selfAssumeStampTax(subAccount, stampTaxAccountIn, stampTaxInfo);
		}

		// 代扣印花税入税务账户
		DpInsideAccountingIn bookAccountingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

		bookAccountingIn.setGl_ref_code(stampTaxInfo.getTax_accounting_alias());
		bookAccountingIn.setCcy_code(stampTaxAccountIn.getCcy_code());
		bookAccountingIn.setTrxn_amt(stampTaxInfo.getStamp_tax_amt());
		bookAccountingIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
		bookAccountingIn.setSummary_code(ApSystemParmApi.getValue(ApConst.CORE_SUMMARY_CODE, "DEPT_STAMP_TAX"));
		bookAccountingIn.setAcct_no("");
		bookAccountingIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
		bookAccountingIn.setOpp_acct_ccy(subAccount.getCcy_code());
		bookAccountingIn.setOpp_acct_no(subAccount.getAcct_no());
		// TODO: bookAccountingIn.setOpp_sub_act_seq();

		DpInsideAccountIobus.insideAccounting(bookAccountingIn);

		// 登记印花税扣收登记簿
		registerStampBook(stampTaxInfo, stampTaxAccountIn, subAccount, stampTaxCode);

		// 准备输出信息
		stampTaxOut.setStamp_tax_amt(stampTaxInfo.getStamp_tax_amt());

		bizlog.method(" DpStampTax.stampTaxAccounting end <<<<<<<<<<<<<<<<");

		return stampTaxOut;
	}

	/**
	 * @Author maold
	 *         <p>
	 *         <li>2018年5月30日-上午10:44:10</li>
	 *         <li>功能说明：客户自身承担印花税</li>
	 *         </p>
	 * @param subAcctInfo
	 *            子户信息
	 * @param cplIn
	 *            印花税服务输入信息
	 * @param taxRateInfo
	 *            印花税率信息
	 */
	private static void selfAssumeStampTax(DpaSubAccount subAcctInfo, DpStampTaxIn cplIn, DpStampTaxInfo taxRateInfo) {
		bizlog.method(" DpStampTax.selfAssumeStampTax begin >>>>>>>>>>>>>>>>");
		bizlog.debug("stampTaxInfoQryOut=[%s]", taxRateInfo);

		// 扣款账户必输
		BizUtil.fieldNotNull(cplIn.getDraw_acct(), DpDict.A.draw_acct.getId(), DpDict.A.draw_acct.getLongName());

		// 账户路由分析
		DpAccountRouteInfo analyOut = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getDraw_acct(), E_CASHTRXN.TRXN);

		// 活期支取
		if (analyOut.getAcct_analy() == E_ACCOUTANALY.DEPOSIT) {

			DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			demandDrawIn.setAcct_no(cplIn.getDraw_acct());
			demandDrawIn.setCcy_code(CommUtil.nvl(cplIn.getAcct_type(), subAcctInfo.getCcy_code()));
			demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			demandDrawIn.setTrxn_amt(taxRateInfo.getStamp_tax_amt());
			demandDrawIn.setSummary_code(ApSystemParmApi.getValue(ApConst.CORE_SUMMARY_CODE, "DEPT_STAMP_TAX"));
			demandDrawIn.setOpp_acct_route(E_ACCOUTANALY.INSIDE);
			demandDrawIn.setOpp_branch_id(BizUtil.getTrxRunEnvs().getTrxn_branch());
			demandDrawIn.setOpp_acct_ccy(subAcctInfo.getCcy_code());
			demandDrawIn.setOpp_acct_no(taxRateInfo.getTax_accounting_alias());
			demandDrawIn.setTrxn_record_type(E_TRXNRECORDTYPE.STAMP_TAX);

			BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);
		}
		else {
			// 其余账户种类暂不支持印花税扣收
			throw APPUB.E0026(SysDict.A.acct_analy.getLongName(), analyOut.getAcct_analy().getValue());
		}

		bizlog.method(" DpStampTax.selfAssumeStampTax end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author maold
	 *         <p>
	 *         <li>2018年5月30日-下午2:14:03</li>
	 *         <li>功能说明：银行垫付印花税</li>
	 *         </p>
	 * @param subAcctInfo
	 *            子户信息
	 * @param cplIn
	 *            印花税服务输入信息
	 * @param taxRateInfo
	 *            印花税率信息
	 */

	private static void bankAssumeStampTax(DpaSubAccount subAcctInfo, DpStampTaxIn cplIn, DpStampTaxInfo taxRateInfo) {
		bizlog.method(" DpStampTax.bankAssumeStamp begin >>>>>>>>>>>>>>>>");
		bizlog.debug("subAcctInfo=[%s],stampTaxInfoQryOut=[%s]", subAcctInfo, taxRateInfo);

		DpInsideAccountingIn bookAccoutingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

		// 银行垫付核算别名记账
		bookAccoutingIn.setAcct_branch(BizUtil.getTrxRunEnvs().getTrxn_branch());
		bookAccoutingIn.setAcct_no(taxRateInfo.getAssume_accounting_alias());
		bookAccoutingIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 交易级现转标志
		bookAccoutingIn.setCcy_code(subAcctInfo.getCcy_code());
		bookAccoutingIn.setTrxn_amt(taxRateInfo.getStamp_tax_amt());
		bookAccoutingIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
		bookAccoutingIn.setSummary_code(ApSystemParmApi.getValue(ApConst.CORE_SUMMARY_CODE, "DEPT_STAMP_TAX"));
		bookAccoutingIn.setOpp_acct_route(E_ACCOUTANALY.INSIDE);
		bookAccoutingIn.setOpp_acct_ccy(subAcctInfo.getCcy_code());
		bookAccoutingIn.setOpp_acct_no(taxRateInfo.getTax_accounting_alias());

		DpInsideAccountIobus.insideAccounting(bookAccoutingIn);

		// 登记印花税账单
		DpUpdAccBalIn instBill = BizUtil.getInstance(DpUpdAccBalIn.class);

		instBill.setCard_no(subAcctInfo.getAcct_no());
		instBill.setAcct_no(subAcctInfo.getAcct_no());
		instBill.setSub_acct_no(subAcctInfo.getSub_acct_no());
		instBill.setTrxn_event_id(cplIn.getTrxn_event_id());
		instBill.setBack_value_date("");
		instBill.setTrxn_amt(taxRateInfo.getStamp_tax_amt());
		instBill.setTrxn_ccy(subAcctInfo.getCcy_code());
		instBill.setDebit_credit(E_DEBITCREDIT.DEBIT);
		instBill.setCash_trxn_ind(E_CASHTRXN.TRXN);
		instBill.setTally_record_ind(E_YESORNO.NO);
		instBill.setShow_ind(E_YESORNO.NO);
		instBill.setTrxn_record_type(E_TRXNRECORDTYPE.STAMP_TAX);
		instBill.setChrg_code("");
		instBill.setSummary_code(ApSystemParmApi.getValue(ApConst.CORE_SUMMARY_CODE, "DEPT_STAMP_TAX"));
		instBill.setVoch_type("");
		instBill.setVoch_no("");
		instBill.setOpp_acct_route(E_ACCOUTANALY.INSIDE);
		instBill.setOpp_acct_no(taxRateInfo.getTax_accounting_alias());
		instBill.setOpp_acct_type("");
		instBill.setOpp_acct_ccy(subAcctInfo.getCcy_code());
		instBill.setOpp_sub_acct_seq("");
		instBill.setOpp_trxn_amt(taxRateInfo.getStamp_tax_amt());
		instBill.setOpp_acct_name("");
		instBill.setOpp_branch_id(BizUtil.getTrxRunEnvs().getTrxn_branch());

		DpAccounting.regBill(instBill, BizUtil.getTrxRunEnvs().getTrxn_date());

		bizlog.method(" DpStampTax.bankAssumeStamp end <<<<<<<<<<<<<<<<");

	}

	/**
	 * @Author maold
	 *         <p>
	 *         <li>2018年5月30日-下午2:17:42</li>
	 *         <li>功能说明：登记印花税代收登记簿</li>
	 *         </p>
	 * @param stampTaxInfoQryOut
	 *            印花税信息查询服务输出信息
	 * @param stampTaxAccountIn
	 *            印花税信息查询输入信息
	 * @param subAccount
	 *            子账户信息
	 * @param stampTaxCode
	 *            印花税代码
	 */
	private static void registerStampBook(DpStampTaxInfo stampTaxInfoQryOut, DpStampTaxIn stampTaxAccountIn, DpaSubAccount subAccount, String stampTaxCode) {
		bizlog.method(" DpStampTax.registerStampBook begin >>>>>>>>>>>>>>>>");
		bizlog.debug("stampTaxInfoQryOut=[%s],stampTaxAccountIn=[%s]", stampTaxInfoQryOut, stampTaxAccountIn);

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 数据库最新记录
		DpbStampTax stampRecode = DpbStampTaxDao.selectFirst_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), trxnDate, false);

		// 登记存款印花税扣收登记簿
		DpbStampTax instStampTax = BizUtil.getInstance(DpbStampTax.class);

		instStampTax.setAcct_no(subAccount.getAcct_no());
		instStampTax.setSub_acct_no(subAccount.getSub_acct_no());
		instStampTax.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		instStampTax.setData_sort(CommUtil.isNotNull(stampRecode) ? (stampRecode.getData_sort() + 1) : 1L);
		instStampTax.setTrxn_event_id(stampTaxAccountIn.getTrxn_event_id());
		instStampTax.setCcy_code(subAccount.getCcy_code());
		instStampTax.setTrxn_amt(stampTaxInfoQryOut.getStamp_tax_amt());
		instStampTax.setCust_no(subAccount.getCust_no());
		instStampTax.setTax_rate_code(stampTaxCode);
		instStampTax.setTax_accounting_alias(stampTaxInfoQryOut.getTax_accounting_alias());
		instStampTax.setTax_min_amt(stampTaxInfoQryOut.getTax_min_amt());
		instStampTax.setStamp_tax_rate(stampTaxInfoQryOut.getStamp_tax_rate());
		instStampTax.setStamp_tax_amt(stampTaxInfoQryOut.getStamp_tax_amt());
		instStampTax.setBank_assume_ind(stampTaxInfoQryOut.getBank_assume_ind());
		instStampTax.setAssume_accounting_alias(stampTaxInfoQryOut.getAssume_accounting_alias());
		instStampTax.setDraw_acct(stampTaxAccountIn.getDraw_acct());
		instStampTax.setDraw_acct_ccy(CommUtil.nvl(stampTaxAccountIn.getDraw_acct_ccy(), subAccount.getCcy_code()));
		instStampTax.setWithdrawl_amt(stampTaxInfoQryOut.getStamp_tax_amt());
		instStampTax.setTrxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());
		instStampTax.setTrxn_code(BizUtil.getTrxRunEnvs().getTrxn_code());
		instStampTax.setSummary_code(ApSystemParmApi.getValue(ApConst.CORE_SUMMARY_CODE, "DEPT_STAMP_TAX"));

		// 登记印花税登记簿
		DpbStampTaxDao.insert(instStampTax);

		bizlog.method(" DpStampTax.registerStampBook end <<<<<<<<<<<<<<<<");

	}

	/**
	 * @Author maold
	 *         <p>
	 *         <li>2018年6月3日-下午9:19:39</li>
	 *         <li>功能说明：加载数据缓存区</li>
	 *         </p>
	 * @param stampTaxAccountIn
	 *            印花税服务输入
	 * @param subAcct
	 *            子账户信息
	 */
	private static void addBuffer(DpStampTaxIn stampTaxAccountIn, DpaSubAccount subAcct) {
		bizlog.method(" DpStampTax.addBuffer begin >>>>>>>>>>>>>>>>");

		// 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(stampTaxAccountIn));

		// 加载子账户信息集
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 加载账户信息集
		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), false);

		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 加载客户信息集
		DpPublicCheck.addDataToCustBuffer(acctInfo.getCust_no(), acctInfo.getCust_type());

		// 加载币种数据区
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 加载参数信息
		DpParmeterMart cplParmIn = BizUtil.getInstance(DpParmeterMart.class);

		cplParmIn.setAcct_type(acctInfo.getAcct_type());
		cplParmIn.setCcy_code(subAcct.getCcy_code());
		cplParmIn.setProd_id(subAcct.getProd_id());

		DpToolsApi.addDataToParmBuffer(cplParmIn);

		bizlog.method(" DpStampTax.addBuffer begin >>>>>>>>>>>>>>>>");
	}

}
