package cn.sunline.icore.dp.serv.account.save;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TARGETAMOUNTTYPE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.errors.DpErr.Dp;
import cn.sunline.icore.dp.serv.froze.DpFroze;
import cn.sunline.icore.dp.serv.query.DpAcctQuery;
import cn.sunline.icore.dp.serv.query.DpDepositSchedule;
import cn.sunline.icore.dp.serv.type.ComDpAccessSchedule.DpTargetDepositQryIn;
import cn.sunline.icore.dp.serv.type.ComDpAccessSchedule.DpTargetDepositQryOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpQueryAcct.DpMainAcctBalInfo;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeSaveOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.base.logging.LogConfigManager.SystemType;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：定期存款
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年1月10日-下午3:35:52</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月10日-HongBiao：定期存入</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTimeSaveCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeSaveCheck.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午3:36:18</li>
	 *         <li>功能说明：定期存入</li>
	 *         </p>
	 * @param cplIn
	 *            定期存入输入接口
	 * @return
	 */
	public static DpTimeSaveOut checkMain(DpTimeSaveIn cplIn) {

		bizlog.method(" DpTimeSaveCheck.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>>cplIn[%s]", cplIn);

		// 带锁定位账户表
		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.TIME);
		acctAccessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.SAVE);
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 存款子账户信息, 带锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 属性到期自动刷新: 不提交数据库
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.NO);

		// 检查主调方法
		DpTimeSaveOut cplOut = checkMainMethod(cplIn, subAccount, account);

		bizlog.debug("<<<<<<cplOut[%s]", cplOut);
		bizlog.method(" DpTimeSaveCheck.checkMain end <<<<<<<<<<<<<<<<");

		return cplOut;

	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月13日-下午3:23:28</li>
	 *         <li>功能说明：定期存入检查主程序,并返回输出信息</li>
	 *         </p>
	 * @param cplIn
	 *            定期存入输入接口
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 * @return DpTimeSaveOut 返回输出信息
	 */
	public static DpTimeSaveOut checkMainMethod(DpTimeSaveIn cplIn, DpaSubAccount subAccount, DpaAccount account) {

		// 1. 默认值
		defaultValue(cplIn);

		// 2. 输入字段检查
		checkInputData(cplIn);

		// 3.加载数据集
		addBuffer(cplIn, subAccount, account);

		// 4.账户信息检查
		checkAllAccount(cplIn, account, subAccount);

		// 5.存入控制操作
		DpBaseServiceApi.checkSaveCtrl(cplIn.getTrxn_amt(), subAccount);

		// 6. 账户限制状态检查
		if (cplIn.getAcct_hold_check_Ind() == E_YESORNO.YES) {
			DpPublicCheck.checkSubAcctTrxnLimit(subAccount, E_DEPTTRXNEVENT.DP_SAVE, null);
		}

		// 7.非开户存入检查凭证状态
		if (cplIn.getOpen_acct_save_ind() == E_YESORNO.NO) {
			// 7.1 开户凭证状态检查
			DpPublicCheck.checkOpenVochStatus(E_YESORNO.YES, subAccount, account);
		}

		// 8.冻结止付检查
		if (CommUtil.isNotNull(cplIn.getFroze_kind_code())) {
			checkFroze(cplIn, subAccount);
		}

		// 9.交易控制检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_TIME_SAVE.getValue());

		// 输出
		DpTimeSaveOut cplOut = BizUtil.getInstance(DpTimeSaveOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplOut.setAcct_no(account.getAcct_no()); // 账号
		cplOut.setAcct_type(account.getAcct_type()); // 账户类型
		cplOut.setAcct_name(account.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(subAccount.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(subAccount.getCcy_code()); // 货币代码
		cplOut.setCust_no(subAccount.getCust_no()); // 客户号
		cplOut.setSub_acct_branch(subAccount.getSub_acct_branch()); // 子账户所属机构
		cplOut.setBranch_name(ApBranchApi.getItem(subAccount.getSub_acct_branch()).getBranch_name()); // 机构名称

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年7月12日-上午11:12:34</li>
	 *         <li>功能说明：加载数据集</li>
	 *         </p>
	 * @param cplIn
	 *            服务输入
	 * @param subAccount
	 *            子户信息
	 * @param account
	 *            账户信息
	 */
	private static void addBuffer(DpTimeSaveIn cplIn, DpaSubAccount subAccount, DpaAccount account) {
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		Map<String, Object> mapObj = new HashMap<String, Object>();

		mapObj.put("same_cust_ind", E_YESORNO.NO);
		mapObj.put("same_acct_ind", E_YESORNO.NO);

		// 对手方账号不为空且为存款类账户
		if (cplIn.getOpp_acct_route() == E_ACCOUTANALY.DEPOSIT && CommUtil.isNotNull(cplIn.getOpp_acct_no())) {

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
			accessIn.setAcct_no(cplIn.getOpp_acct_no());
			accessIn.setAcct_type(cplIn.getOpp_acct_type());
			accessIn.setCcy_code(cplIn.getOpp_acct_ccy());
			accessIn.setSub_acct_seq(cplIn.getOpp_sub_acct_seq());

			// 查询对方子户产品
			DpAcctAccessOut locateSubAcct = DpToolsApi.subAcctInquery(accessIn);

			// 查询对方账户
			DpaAccount oppAcct = DpaAccountDao.selectOne_odb1(locateSubAcct.getAcct_no(), false);

			// 将对手方的主账号和服务的主账号做比较，若一致则将same_acct_ind 置为 Y-YES.
			if (CommUtil.equals(cplIn.getOpp_acct_no(), cplIn.getAcct_no())) {

				mapObj.put("same_acct_ind", E_YESORNO.YES);
			}

			DpaAccount dpaAccount = DpaAccountDao.selectOne_odb1(cplIn.getAcct_no(), false);

			// 将对手方的客户号和服务的主账号对应的客户号做比较，若一致则将same_cust_ind 置为 Y-YES
			if (CommUtil.equals(oppAcct.getCust_no(), dpaAccount.getCust_no())) {

				mapObj.put("same_cust_ind", E_YESORNO.YES);
			}
		}

		// 追加输入数据集
		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObj);

		// 3.2加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAccount));

		// 3.3加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(account));

		DpMainAcctBalInfo mainAcctBalance = DpAcctQuery.getMainAcctBalance(account.getAcct_no(), null, null);

		Map<String, Object> mapObject = new HashMap<String, Object>();
		// 账户余额
		mapObject.put("total_amt", mainAcctBalance.getTotal_amt());

		// 追加账户数据集
		ApBufferApi.appendData(ApConst.ACCOUNT_DATA_MART, mapObject);

		// 3.4货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAccount.getCcy_code())));

		// 3.5加载客户数据集
		DpPublicCheck.addDataToCustBuffer(account.getCust_no(), account.getCust_type());
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月6日-下午2:09:35</li>
	 *         <li>功能说明：检查账户信息</li>
	 *         </p>
	 * @param cplIn
	 *            定期存入输入接口
	 * @param account
	 *            账户信息
	 * @param subAccount
	 *            子账户信息
	 */
	private static void checkAllAccount(DpTimeSaveIn cplIn, DpaAccount account, DpaSubAccount subAccount) {

		bizlog.method(" DpTimeSaveCheck.checkSubAccount begin >>>>>>>>>>>>>>>>");

		// 账户状态检查
		if (account.getAcct_status() != E_ACCTSTATUS.NORMAL) {
			throw DpBase.E0008(account.getAcct_no());
		}

		// 子账户状态检查
		if (subAccount.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		/*
		 * // 客户账户名称一致性检查 if (CommUtil.isNotNull(cplIn.getAcct_name()) &&
		 * !CommUtil.equals(cplIn.getAcct_name(), account.getAcct_name())) {
		 * throw DpErr.Dp.E0058(cplIn.getAcct_name(), account.getAcct_name()); }
		 */

		// 检查输入币种与子账户币种是否一致
		if (CommUtil.compare(subAccount.getCcy_code(), cplIn.getCcy_code()) != 0) {

			throw DpErr.Dp.E0023(cplIn.getCcy_code(), subAccount.getCcy_code());
		}

		// 定期账户到期日之后不能再存入
		if (CommUtil.isNotNull(subAccount.getDue_date()) && CommUtil.compare(BizUtil.getTrxRunEnvs().getTrxn_date(), subAccount.getDue_date()) > 0) {

			throw DpErr.Dp.E0213();
		}

		// 有存入计划的存入金额相关检查
		if (CommUtil.isNotNull(subAccount.getScheduled_dept_cycle())) {

			BigDecimal omitDeptAmt = getTimeOmitAmout(subAccount);

			// 联机时漏存金额手工补存必须一次性补齐，因为补存一部分后面更新存入计划标志就不准确
			if (CommUtil.compare(omitDeptAmt, BigDecimal.ZERO) > 0 && CommUtil.compare(omitDeptAmt, cplIn.getTrxn_amt()) != 0) {

				if (SystemType.onl == SysUtil.getCurrentSystemType()) {
					throw Dp.E0372(omitDeptAmt);
				}
			}
		}

		bizlog.method(" DpTimeSaveCheck.checkSubAccount end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午3:31:53</li>
	 *         <li>功能说明：检查输入数据合法性</li>
	 *         </p>
	 * @param cplIn
	 *            定期存入输入接口
	 */
	private static void checkInputData(DpTimeSaveIn cplIn) {
		bizlog.method(" DpTimeSaveCheck.checkInputData begin >>>>>>>>>>>>>>>>");

		// 交易金額必须输入
		BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

		// 现转标志必须输入
		BizUtil.fieldNotNull(cplIn.getCash_trxn_ind(), SysDict.A.cash_trxn_ind.getId(), SysDict.A.cash_trxn_ind.getLongName());

		// 交易币种必须输入
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 摘要代码必须输入
		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

		// 金额精度检检查，不合法抛出异常
		ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getTrxn_amt());

		// 检查摘要代码
		ApSummaryApi.exists(cplIn.getSummary_code());

		bizlog.method(" DpTimeSaveCheck.checkInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-下午4:26:07</li>
	 *         <li>功能说明：调用冻结检查,未传入冻结分类码，直接返回不做检查</li>
	 *         </p>
	 * @param cplIn
	 *            定期存入输入接口
	 * @param subAcct
	 *            子账户信息
	 */
	private static void checkFroze(DpTimeSaveIn cplIn, DpaSubAccount subAcct) {

		bizlog.method(" DpTimeSaveCheck.checkFroze begin >>>>>>>>>>>>>>>>");

		DpFrozeIn dpFrozeIn = BizUtil.getInstance(DpFrozeIn.class);

		dpFrozeIn.setFroze_kind_code(cplIn.getFroze_kind_code());
		dpFrozeIn.setFroze_object_type(cplIn.getFroze_object_type());
		dpFrozeIn.setAcct_no(subAcct.getAcct_no());
		dpFrozeIn.setAcct_type(cplIn.getAcct_type());
		dpFrozeIn.setAcct_name(cplIn.getAcct_name());
		dpFrozeIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		dpFrozeIn.setCcy_code(subAcct.getCcy_code());
		dpFrozeIn.setProd_id(subAcct.getProd_id());
		dpFrozeIn.setFroze_amt(cplIn.getFroze_amt());
		dpFrozeIn.setFroze_reason(cplIn.getFroze_reason());
		dpFrozeIn.setFroze_due_date(cplIn.getFroze_due_date());
		dpFrozeIn.setFroze_before_save_amt(cplIn.getTrxn_amt());

		// 同客户冻结调用方法效率更高
		DpFroze.checkMain(dpFrozeIn);

		bizlog.method(" DpTimeSaveCheck.checkFroze end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-下午3:41:06</li>
	 *         <li>功能说明：输入为空默认值</li>
	 *         </p>
	 * @param cplIn
	 *            定期存入服务输入接口
	 */
	private static void defaultValue(DpTimeSaveIn cplIn) {

		// 账户限制检查默认为“是”
		if (cplIn.getAcct_hold_check_Ind() == null) {
			cplIn.setAcct_hold_check_Ind(E_YESORNO.YES);
		}

		// 开户凭证检查标志默认为“是”
		if (cplIn.getOpen_voch_check_ind() == null) {
			cplIn.setOpen_voch_check_ind(E_YESORNO.YES);
		}

		// 是否开户存入标志默认为“否”
		if (cplIn.getOpen_acct_save_ind() == null) {
			cplIn.setOpen_acct_save_ind(E_YESORNO.NO);
		}

		// 倒起息日期默认为系统日期
		if (CommUtil.isNull(cplIn.getBack_value_date())) {
			cplIn.setBack_value_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年8月26日-下午4:30:49</li>
	 *         <li>功能说明：零存整取和目标存款类漏存金额计算</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 * @return 漏存金额
	 */
	public static BigDecimal getTimeOmitAmout(DpaSubAccount subAccount) {

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		if (subAccount.getTarget_amt_type() == E_TARGETAMOUNTTYPE.PRINCIPAL) {

			int termCounts = BizUtil.dateDiffByCycle(subAccount.getScheduled_dept_cycle(), subAccount.getStart_inst_date(), trxnDate);

			BigDecimal needDeptAmount = subAccount.getFirst_deposit_amt().multiply(BigDecimal.valueOf(termCounts + 1L));

			return needDeptAmount.subtract(subAccount.getAccm_dept_amt());
		}
		else if (subAccount.getTarget_amt_type() == E_TARGETAMOUNTTYPE.PRINCIPAL_INTEREST) {

			DpTargetDepositQryIn cplQryIn = BizUtil.getInstance(DpTargetDepositQryIn.class);

			cplQryIn.setAcct_no(subAccount.getAcct_no());
			cplQryIn.setAcct_type(null);
			cplQryIn.setSub_acct_seq(subAccount.getSub_acct_seq());

			DpTargetDepositQryOut cplOut = DpDepositSchedule.getTargetDepositInfo(cplQryIn);

			return cplOut.getNeed_reple_dept_amt();
		}
		else {
			return BigDecimal.ZERO;
		}

	}
}
