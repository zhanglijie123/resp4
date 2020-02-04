package cn.sunline.icore.dp.serv.froze;

import java.math.BigDecimal;
import java.util.HashMap;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApLimitApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpFrozeParmApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFrozeDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeParameter.DppFrozeType;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeBaseIn;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeObjectBase;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpUnfrozeBaseIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_DRAWTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_FROZESOURCE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_SPECFROZETYPE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpChargeIobus;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZEOBJECT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZESTATUS;
import cn.sunline.icore.iobus.dp.type.ComIoDpPreAuthoriation.DpPreAuthorCompletedIn;
import cn.sunline.icore.iobus.dp.type.ComIoDpPreAuthoriation.DpPreAuthorCompletedOut;
import cn.sunline.icore.iobus.dp.type.ComIoDpPreAuthoriation.DpPreAuthorIn;
import cn.sunline.icore.iobus.dp.type.ComIoDpPreAuthoriation.DpPreAuthorOut;
import cn.sunline.icore.iobus.dp.type.ComIoDpPreAuthoriation.DpPreAuthorRevokeIn;
import cn.sunline.icore.iobus.dp.type.ComIoDpPreAuthoriation.DpPreAuthorRevokeOut;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：预授权相关处理
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2018年10月30日-下午2:05:59</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpPreAuthorization {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPreAuthorization.class);

	/**
	 * @Author huyw
	 *         <p>
	 *         <li>2018年10月30日-下午2:06:43</li>
	 *         <li>功能说明：预授权</li>
	 *         </p>
	 * @param cplIn
	 *            预授权服务输入接口
	 * @return 预授权服务输出接口
	 */
	public static DpPreAuthorOut preAuthor(DpPreAuthorIn cplIn) {

		bizlog.method(" DpPreAuthorization.preAuthor begin >>>>>>>>>>>>>>>>");

		// 输入接口必输性检查
		BizUtil.fieldNotNull(cplIn.getCard_no(), SysDict.A.card_no.getId(), SysDict.A.card_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getFroze_amt(), SysDict.A.froze_amt.getId(), SysDict.A.froze_amt.getLongName());

		// 定位活期子户
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getCard_no());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
		acctAccessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 子账户信息，上锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 账户信息查询
		DpaAccount account = DpaAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), true);

		// 基本约束检查
		commonCheck(cplIn, account);

		// 活期支取检查
		DpBaseServiceApi.checkDrawCtrl(cplIn.getFroze_amt(), subAccount);

		// 获取预授权冻结参数
		DppFrozeType frozeType = DpFrozeParmApi.getSpecFrozeInfo(E_SPECFROZETYPE.PRE_AUTHOR, null);

		// 加载数据集
		addDataToBuffer(cplIn, subAccount, account, frozeType);

		// 属性到期自动刷新
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getCard_no(), E_YESORNO.YES);

		// 交易控制检查: 包括业务规则、属性检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.PRE_AUTHOR.getValue());

		// 余额信息查询
		DpBalanceCalculateOut balanceInfo = DpToolsApi.getBalance(subAccount.getSub_acct_no(), cplIn.getCard_no(), E_DRAWTYPE.COMMON);

		// 可用余额不足检查
		if (CommUtil.compare(cplIn.getFroze_amt(), balanceInfo.getUsable_bal()) > 0) {

			throw DpBase.E0118(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		// 限额累计及检查
		ApLimitApi.process(E_DEPTTRXNEVENT.PRE_AUTHOR.getValue(), cplIn.getCcy_code(), cplIn.getFroze_amt());

		// 费用试算
		DpChargeIobus.calcAutoChrg(subAccount, E_DEPTTRXNEVENT.PRE_AUTHOR, cplIn.getFroze_amt());

		// 登记冻结信息
		DpbFroze frozeInfo = regFrozeInfo(cplIn, subAccount, frozeType);

		// 输出
		DpPreAuthorOut cplOut = BizUtil.getInstance(DpPreAuthorOut.class);

		cplOut.setCard_no(frozeInfo.getCard_no());
		cplOut.setCcy_code(frozeInfo.getCcy_code());
		cplOut.setFroze_amt(cplIn.getFroze_amt());
		cplOut.setFroze_due_date(frozeInfo.getFroze_due_date());
		cplOut.setFroze_no(frozeInfo.getFroze_no());
		cplOut.setAcct_name(account.getAcct_name());

		bizlog.method(" DpPreAuthorization.preAuthor end <<<<<<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年10月30日-下午4:58:22</li>
	 *         <li>功能说明：添加交易数据到缓存区</li>
	 *         </p>
	 * @param cplIn
	 *            预授权交易输入接口
	 * @param subAcct
	 *            子账户信息
	 * @param account
	 *            账户信息
	 * @param parm
	 *            冻结参数信息
	 */
	private static void addDataToBuffer(DpPreAuthorIn cplIn, DpaSubAccount subAcct, DpaAccount account, DppFrozeType parm) {

		// 1 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		HashMap<String, Object> mapSource = new HashMap<String, Object>();

		mapSource.put(DpBaseDict.A.froze_source.getId(), parm.getFroze_source() == E_FROZESOURCE.INTERIOR ? "2" : "1");

		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapSource);

		// 2 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 3 加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(account));

		// 4. 货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 5. 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(account.getCust_no(), account.getCust_type());

		// 6. 加载卡数据集
		DpToolsApi.addDataToCardBuffer(cplIn.getCard_no());

		// 7.加载参数数据区
		ApBufferApi.addData(ApConst.PARM_DATA_MART, CommUtil.toMap(parm));
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年10月30日-下午4:58:22</li>
	 *         <li>功能说明：基本约束检查</li>
	 *         </p>
	 * @param cplIn
	 *            预授权交易输入接口
	 * @param account
	 *            账户信息
	 */
	private static void commonCheck(DpPreAuthorIn cplIn, DpaAccount account) {

		if (account.getCard_relationship_ind() == E_YESORNO.NO) {
			throw DpErr.Dp.E0455();
		}

		if (CommUtil.compare(cplIn.getFroze_amt(), BigDecimal.ZERO) <= 0) {
			throw DpBase.E0016(cplIn.getFroze_amt());
		}

		ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getFroze_amt());
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年10月30日-下午4:58:22</li>
	 *         <li>功能说明：登记冻结信息</li>
	 *         </p>
	 * @param cplIn
	 *            预授权交易输入接口
	 * @param subAcct
	 *            子账户信息
	 * @param parm
	 *            冻结参数信息
	 * @return 冻结登记簿
	 */
	private static DpbFroze regFrozeInfo(DpPreAuthorIn cplIn, DpaSubAccount subAcct, DppFrozeType parm) {

		bizlog.method(" DpPreAuthorization.regFrozeInfo begin >>>>>>>>>>>>>>>>");

		String frozeNo = ApSeqApi.genSeq(DpConst.FROZE_NO);
		String frozeReason = ApBusinessParmApi.getValue(DpConst.FROZE_REASON, "PREAUTHOR");

		// 登记冻结台账
		DpFrozeBaseIn cplFrozeIn = BizUtil.getInstance(DpFrozeBaseIn.class);

		cplFrozeIn.setFroze_kind_code(parm.getFroze_kind_code());
		cplFrozeIn.setFroze_object_type(E_FROZEOBJECT.SUBACCT);
		cplFrozeIn.setRemark(cplIn.getRemark());
		cplFrozeIn.setCcy_code(subAcct.getCcy_code());
		cplFrozeIn.setFroze_amt(cplIn.getFroze_amt());
		cplFrozeIn.setFroze_before_save_amt(BigDecimal.ZERO);
		cplFrozeIn.setFroze_due_date(null);
		cplFrozeIn.setFroze_feature_code(null);
		cplFrozeIn.setFroze_reason(frozeReason);
		cplFrozeIn.setFroze_term(cplIn.getFroze_term());

		DpFrozeObjectBase frozeObjIn = BizUtil.getInstance(DpFrozeObjectBase.class);

		frozeObjIn.setCard_no(cplIn.getCard_no());
		frozeObjIn.setAcct_no(subAcct.getAcct_no());
		frozeObjIn.setCust_no(subAcct.getCust_no());
		frozeObjIn.setCust_type(subAcct.getCust_type());
		frozeObjIn.setSub_acct_no(subAcct.getSub_acct_no());

		DpbFroze frozeInfo = DpBaseServiceApi.registerFrozeAccount(cplFrozeIn, frozeObjIn, frozeNo);

		bizlog.method(" DpPreAuthorization.regFrozeInfo end <<<<<<<<<<<<<<<<<<<<");

		return frozeInfo;
	}

	/**
	 * @Author huyw
	 *         <p>
	 *         <li>2018年10月30日-下午2:06:43</li>
	 *         <li>功能说明：预授权撤销</li>
	 *         </p>
	 * @param cplIn
	 *            预授权撤销服务输入接口
	 * @return 预授权撤销服务输出接口
	 */
	public static DpPreAuthorRevokeOut preAuthorRevoke(DpPreAuthorRevokeIn cplIn) {

		bizlog.method(" DpPreAuthorization.preAuthorRevoke begin >>>>>>>>>>>>>>>>");

		// 获取预授权记录
		DpbFroze frozeInfo = getPreAuthorRecord(cplIn.getOriginal_trxn_seq(), cplIn.getCard_no());

		// 解冻服务
		DpUnfrozeBaseIn cplUnFrozeIn = BizUtil.getInstance(DpUnfrozeBaseIn.class);

		cplUnFrozeIn.setFroze_no(frozeInfo.getFroze_no());
		cplUnFrozeIn.setUnfroze_reason(ApBusinessParmApi.getValue(DpConst.UNFROZE_REASON, "PREAUTHOR_CANCEL"));
		cplUnFrozeIn.setRemark(null);
		cplUnFrozeIn.setUnfroze_amt(frozeInfo.getFroze_bal());
		
		DpBaseServiceApi.cancelFroze(frozeInfo, cplUnFrozeIn);

		// 查询账户信息
		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(frozeInfo.getAcct_no(), true);

		// 输出
		DpPreAuthorRevokeOut cplOut = BizUtil.getInstance(DpPreAuthorRevokeOut.class);

		cplOut.setCard_no(frozeInfo.getCard_no());
		cplOut.setCcy_code(frozeInfo.getCcy_code());
		cplOut.setFroze_no(frozeInfo.getFroze_no());
		cplOut.setUnfroze_amt(frozeInfo.getFroze_bal());
		cplOut.setFroze_trxn_seq(frozeInfo.getFroze_trxn_seq());
		cplOut.setAcct_name(acctInfo.getAcct_name());

		bizlog.method(" DpPreAuthorization.preAuthorRevoke end <<<<<<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author huyw
	 *         <p>
	 *         <li>2018年10月30日-下午2:06:43</li>
	 *         <li>功能说明：预授权完成</li>
	 *         </p>
	 * @param cplIn
	 *            预授权完成服务输入接口
	 * @return 预授权完成服务输出接口
	 */
	public static DpPreAuthorCompletedOut preAuthorComplete(DpPreAuthorCompletedIn cplIn) {

		bizlog.method(" DpPreAuthorization.preAuthorComplete begin >>>>>>>>>>>>>>>>");

		// 获取预授权记录
		DpbFroze frozeInfo = getPreAuthorRecord(cplIn.getOriginal_trxn_seq(), cplIn.getCard_no());

		String summaryCode = ApSummaryApi.getSummaryClass("PREAUTHOR_COMPLETE");
		String glRefNo = ApBusinessParmApi.getValue(DpConst.BUSINESS_CODE, "PREAUTHOR_COMPLETE");

		// 活期支取记账输入
		DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

		demandDrawIn.setAcct_no(CommUtil.nvl(frozeInfo.getCard_no(), frozeInfo.getAcct_no()));
		demandDrawIn.setAcct_type(null);
		demandDrawIn.setCheck_password_ind(E_YESORNO.NO);
		demandDrawIn.setForce_draw_ind(E_YESORNO.NO);
		demandDrawIn.setCcy_code(frozeInfo.getCcy_code());
		demandDrawIn.setTrxn_amt(frozeInfo.getFroze_bal());
		demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.COMMON);
		demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
		demandDrawIn.setSummary_code(summaryCode);
		demandDrawIn.setTrxn_remark(cplIn.getTrxn_remark());
		demandDrawIn.setCustomer_remark(cplIn.getCustomer_remark());

		// 解冻信息
		demandDrawIn.setFroze_no(frozeInfo.getFroze_no());
		demandDrawIn.setUnfroze_amt(frozeInfo.getFroze_bal());
		demandDrawIn.setUnfroze_reason(ApBusinessParmApi.getValue(DpConst.UNFROZE_REASON, "PREAUTHOR_COMPLETE"));

		// 对手方
		demandDrawIn.setOpp_acct_ccy(frozeInfo.getCcy_code());
		demandDrawIn.setOpp_acct_no(glRefNo);
		demandDrawIn.setOpp_acct_route(E_ACCOUTANALY.INSIDE);
		demandDrawIn.setOpp_branch_id(BizUtil.getTrxRunEnvs().getTrxn_branch());
		demandDrawIn.setOpp_trxn_amt(frozeInfo.getFroze_bal());

		// 境外消费交易地信息登记
		demandDrawIn.setTrxn_area(cplIn.getTrxn_area());
		demandDrawIn.setTrxn_area_amt(cplIn.getTrxn_area_amt());
		demandDrawIn.setTrxn_area_ccy(cplIn.getTrxn_area_ccy());
		demandDrawIn.setTrxn_area_exch_rate(cplIn.getTrxn_area_exch_rate());
		demandDrawIn.setConsume_date(cplIn.getConsume_date());
		demandDrawIn.setConsume_time(cplIn.getConsume_time());

		// 调用活期存款支取服务
		DpDemandDrawOut demandDrawOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);

		// 贷：VISA清算内部户
		DpInsideAccountingIn bookAccoutingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

		bookAccoutingIn.setAcct_branch(BizUtil.getTrxRunEnvs().getTrxn_branch());
		bookAccoutingIn.setAcct_no("");
		bookAccoutingIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
		bookAccoutingIn.setCcy_code(frozeInfo.getCcy_code());
		bookAccoutingIn.setTrxn_amt(frozeInfo.getFroze_bal());
		bookAccoutingIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
		bookAccoutingIn.setGl_ref_code(glRefNo);
		bookAccoutingIn.setSummary_code(summaryCode);
		bookAccoutingIn.setTrxn_remark(cplIn.getTrxn_remark());
		bookAccoutingIn.setCustomer_remark(cplIn.getCustomer_remark());

		// 真实对手方信息
		bookAccoutingIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
		bookAccoutingIn.setOpp_acct_no(CommUtil.nvl(frozeInfo.getAcct_no(), frozeInfo.getCard_no()));
		bookAccoutingIn.setOpp_acct_ccy(frozeInfo.getCcy_code());

		// 调用内部户记账服务
		DpInsideAccountingOut bookAccoutingOut = DpInsideAccountIobus.insideAccounting(bookAccoutingIn);

		// 输出
		DpPreAuthorCompletedOut cplOut = BizUtil.getInstance(DpPreAuthorCompletedOut.class);

		cplOut.setCard_no(frozeInfo.getCard_no());
		cplOut.setCcy_code(frozeInfo.getCcy_code());
		cplOut.setFroze_no(frozeInfo.getFroze_no());
		cplOut.setTrxn_amt(frozeInfo.getFroze_bal());
		cplOut.setAcct_name(demandDrawOut.getAcct_name());
		cplOut.setFroze_trxn_seq(frozeInfo.getFroze_trxn_seq());
		cplOut.setOpp_acct_name(bookAccoutingOut.getAcct_name());
		cplOut.setOpp_acct_no(bookAccoutingOut.getAcct_no());

		bizlog.method(" DpPreAuthorization.preAuthorComplete end <<<<<<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author huyw
	 *         <p>
	 *         <li>2018年10月30日-下午2:06:43</li>
	 *         <li>功能说明：获取预授权记录</li>
	 *         </p>
	 * @param orgTrxSeq
	 *            原交易流水或冻结编号
	 * @param cardNo
	 *            卡号
	 * @return 预授权记录
	 */
	private static DpbFroze getPreAuthorRecord(String orgTrxSeq, String cardNo) {

		// 原交易流水或冻结编号不输入，则要求输入原交易流水或编号
		if (CommUtil.isNull(orgTrxSeq)) {
			throw ApPubErr.APPUB.E0001(SysDict.A.original_trxn_seq.getId(), SysDict.A.original_trxn_seq.getLongName());
		}

		// 查询冻结登记簿信息
		DpbFroze frozeInfo = DpbFrozeDao.selectFirst_odb6(orgTrxSeq, false);

		if (CommUtil.isNull(frozeInfo)) {
			frozeInfo = DpbFrozeDao.selectFirst_odb3(orgTrxSeq, false);
		}

		// 输入条件未定位到冻结信息
		if (CommUtil.isNull(frozeInfo)) {
			throw DpBase.E0127(orgTrxSeq);
		}

		if (frozeInfo.getSpec_froze_type() != E_SPECFROZETYPE.PRE_AUTHOR) {
			throw DpErr.Dp.E0456();
		}

		if (frozeInfo.getFroze_status() == E_FROZESTATUS.CLOSE) {
			throw DpBase.E0106(frozeInfo.getFroze_no(), frozeInfo.getCard_no());
		}

		if (CommUtil.isNotNull(cardNo) && !CommUtil.equals(cardNo, frozeInfo.getCard_no())) {
			throw DpErr.Dp.E0457();
		}

		return frozeInfo;
	}
}