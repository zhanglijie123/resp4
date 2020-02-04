package cn.sunline.icore.dp.serv.account.close;

import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.common.util.DateUtil;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpTimeInterestApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ALLOW;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TIMEDRAWDATESCENE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpOtherIobus;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpCloseSubAccountCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCloseSubAccountCheck.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月11日-下午3:58:15</li>
	 *         <li>功能说明：销子户服务主处理程序</li>
	 *         </p>
	 * @param cplIn
	 *            销子户输入接口
	 */
	public static void checkMain(DpCloseSubAccountIn cplIn) {
		bizlog.method(" DpCloseSubAccountCheck.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 获取账户信息
		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no());
		accessIn.setAcct_type(cplIn.getAcct_type());
		accessIn.setCcy_code(cplIn.getCcy_code());
		accessIn.setProd_id(cplIn.getProd_id());
		accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		// 定位账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 获取子账户信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), false);

		// 属性到期自动刷新: 不提交数据库
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.NO);

		// 子账户销户检查
		checkMainMethod(cplIn, account, subAccount);

		bizlog.method(" DpCloseSubAccountCheck.checkMain end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月13日-下午2:50:29</li>
	 *         <li>功能说明：子账户销户检查主程序</li>
	 *         </p>
	 * @param cplIn
	 *            销子户输入接口
	 * @param account
	 *            账户信息
	 * @param subAccount
	 *            子账户信息
	 */
	public static void checkMainMethod(DpCloseSubAccountIn cplIn, DpaAccount account, DpaSubAccount subAccount) {

		// 非空数据检查
		validInputData(cplIn);

		// 加载数据区
		addDataToBuffer(cplIn, account, subAccount);

		// 验证密码标志为 yes,需验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(account, checkIn);
		}

		// 交易控制检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_CLOSE_SUBACCT.getValue());

		// 子账户销户许可
		DpBaseServiceApi.checkCloseSubAcctLicense(subAccount);

		// 账户限制检查
		DpPublicCheck.checkSubAcctTrxnLimit(subAccount, E_DEPTTRXNEVENT.DP_CLOSE_SUBACCT, null);

		// 外部约束等检查
		checkExternalConstraint(subAccount);

		// 检查开户凭证信息
		DpPublicCheck.checkOpenVochStatus(E_YESORNO.YES, subAccount, account);

		// TODO: 如果同销账户的话进行销户检查
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月19日-上午10:25:54</li>
	 *         <li>功能说明：检查子账户收息还息性质等外部约束</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 */
	public static void checkExternalConstraint(DpaSubAccount subAccount) {

		bizlog.method(" DpCloseSubAccountCheck.checkExternalConstraint begin >>>>>>>>>>>>>>>>");

		// 检查是否还款账号,还款账号不能销户
		if (DpOtherIobus.isLoanRepayAcct(subAccount)) {

			throw DpErr.Dp.E0316(subAccount.getAcct_no());
		}

		// 子户允许使用支票,检查支票相关
		if (subAccount.getUse_cheque_allow_ind() == E_ALLOW.ALLOW) {

			DpVoucherIobus.checkChequeBeforeClose(subAccount);
		}

		// 检查销户账户是否为收息账户
		checkIsInstIncomeAcct(subAccount);

		// TODO: 签约及欠费检查

		bizlog.method(" DpCloseSubAccountCheck.checkExternalConstraint end <<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年12月18日-上午9:36:40</li>
	 *         <li>功能说明：检查是否为收息账户</li>
	 *         </p>
	 * @param subAccount
	 */
	private static void checkIsInstIncomeAcct(DpaSubAccount subAccount) {

		if (subAccount.getDd_td_ind() == E_DEMANDORTIME.TIME) {// 定期不可能是收息账号

			return;
		}

		DpaSubAccount incomeAcct = DpaSubAccountDao.selectFirst_odb5(subAccount.getAcct_no(), subAccount.getCcy_code(), E_SUBACCTSTATUS.NORMAL, false);

		if (incomeAcct != null) {

			throw DpErr.Dp.E0417();
		}

	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月19日-上午9:59:16</li>
	 *         <li>功能说明：非空数据检查</li>
	 *         </p>
	 * @param cplIn
	 *            销子户输入接口
	 */
	private static void validInputData(DpCloseSubAccountIn cplIn) {
		bizlog.method(" DpCloseSubAccountCheck.validInputData begin >>>>>>>>>>>>>>>>");

		// 现转标志 不可为空
		BizUtil.fieldNotNull(cplIn.getCash_trxn_ind(), SysDict.A.cash_trxn_ind.getId(), SysDict.A.cash_trxn_ind.getLongName());

		bizlog.method(" DpCloseSubAccountCheck.validInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月23日-上午11:08:33</li>
	 *         <li>功能说明：添加数据到缓存区</li>
	 *         </p>
	 * @param cplIn
	 *            输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 */
	private static void addDataToBuffer(DpCloseSubAccountIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		// 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		Map<String, Object> mapObj = new HashMap<String, Object>();

		// 计算销户月份
		int openMethods = DateUtil.getMonthCount(DateUtil.parseDate(subAcct.getOpen_acct_date(), "yyyyMMdd"),
				DateUtil.parseDate(BizUtil.getTrxRunEnvs().getTrxn_date(), "yyyyMMdd"));

		mapObj.put("open_months", openMethods);

		// 定期账户按提前支取收费
		if (subAcct.getDd_td_ind() == E_DEMANDORTIME.TIME) {

			// 定期支取日期场景
			E_TIMEDRAWDATESCENE drawDateScene = DpTimeInterestApi.getDrawDateScene(subAcct, BizUtil.getTrxRunEnvs().getTrxn_date());

			mapObj.put("time_draw_date_scene", drawDateScene);
		}

		// 加载输入数据集
		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObj);

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 加载账户数据区
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 加载币种数据区
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 加载客户数据区
		DpPublicCheck.addDataToCustBuffer(acctInfo.getCust_no(), acctInfo.getCust_type());

		// 加载卡数据区
		if (acctInfo.getCard_relationship_ind() == E_YESORNO.YES && !CommUtil.equals(acctInfo.getAcct_no(), cplIn.getAcct_no())) {
			DpToolsApi.addDataToCardBuffer(cplIn.getAcct_no());
		}
	}
}
