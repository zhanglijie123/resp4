package cn.sunline.icore.dp.serv.account.close;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.common.util.DateUtil;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountType;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseSubAccountOut;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ACCTBUSITYPE;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：子账户销户功能
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年1月23日-下午4:51:25</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月23日-HongBiao：子账户销户功能</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpCloseSubAccount {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCloseSubAccountCheck.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月23日-下午4:51:44</li>
	 *         <li>功能说明：子账户销户主处理程序</li>
	 *         </p>
	 * @param cplIn
	 *            销子户输入接口
	 * @return DpCloseSubAccountOut 销子户输出接口
	 */
	public static DpCloseSubAccountOut doMain(DpCloseSubAccountIn cplIn) {

		bizlog.method(" DpCloseSubAccount.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>>cplIn=[%s]", cplIn);

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

		// 子账户序号
		cplIn.setSub_acct_seq(CommUtil.nvl(cplIn.getSub_acct_seq(), acctAccessOut.getSub_acct_seq()));

		// 获取子账户信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), false);

		// 2.属性到期自动刷新
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.YES);

		// 3.销子户主调检查
		DpCloseSubAccountCheck.checkMainMethod(cplIn, account, subAccount);

		// 4.销子户主调方法
		DpCloseSubAccountOut cplOut = doMethod(cplIn, subAccount, account);

		bizlog.debug("<<<<<<cplOut=[%S]", cplOut);
		bizlog.method(" DpCloseSubAccount.doMain end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月23日-下午4:51:44</li>
	 *         <li>功能说明：子账户销户主处理程序</li>
	 *         </p>
	 * @param cplIn
	 *            销子户输入接口
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 */
	public static DpCloseSubAccountOut doMethod(DpCloseSubAccountIn cplIn, DpaSubAccount subAccount, DpaAccount account) {

		// 输出接口赋值
		DpCloseSubAccountOut cplOut = BizUtil.getInstance(DpCloseSubAccountOut.class);

		// 账户余额大于零不能销户: 要放在这里检查
		if (CommUtil.compare(subAccount.getAcct_bal(), BigDecimal.ZERO) > 0) {
			throw DpBase.E0080(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		// 关闭子账户台账
		DpBaseServiceApi.closeSubAcct(subAccount, cplIn);

		// 3. 有关联开户凭证的话,核销开户凭证;支票类凭证暂不支持自动核销，需专门交易核销或回收
		if (account.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT && subAccount.getCorrelation_voch_ind() == E_YESORNO.YES) {

			DpVoucherIobus.modifyCustVoucherStatus(account, subAccount, cplIn.getRemark());

		}

		// 4.销客户账户
		E_YESORNO closeAcctFlag = closeAcct(cplIn, account);

		// 输出接口赋值
		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(account.getAcct_no());
		cplOut.setAcct_type(account.getAcct_type());
		cplOut.setAcct_name(account.getAcct_name());
		cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplOut.setCcy_code(subAccount.getCcy_code());
		cplOut.setProd_id(subAccount.getProd_id());
		cplOut.setSub_acct_status(subAccount.getSub_acct_status());
		cplOut.setCust_no(subAccount.getCust_no());
		cplOut.setSub_acct_branch(subAccount.getSub_acct_branch());
		cplOut.setBranch_name(ApBranchApi.getItem(subAccount.getSub_acct_branch()).getBranch_name());
		cplOut.setClose_acct_ind(closeAcctFlag);

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午3:17:09</li>
	 *         <li>功能说明：销账户处理</li>
	 *         </p>
	 * @param cplIn
	 * @param account
	 *            账户信息
	 * @return E_YESORNO 是否已销账户
	 */
	private static E_YESORNO closeAcct(DpCloseSubAccountIn cplIn, DpaAccount account) {

		bizlog.method(" DpCloseSubAccount.closeAcct begin >>>>>>>>>>>>>>>>");

		// 根据账户类型的【销完子户同销账户标志】属性确定是否 【销客户账户标志】
		DppAccountType dppAcctType = DpAccountTypeParmApi.getAcctTypeInfo(account.getAcct_type());

		if (dppAcctType.getClose_sub_acct_and_acct_ind() == E_YESORNO.NO) {
			bizlog.method(" DpCloseSubAccount.closeAcct end <<<<<<<<<<<<<<<<");
			return E_YESORNO.NO;
		}

		// 查询客户是否还有未销户子账户
		DpaAccountRelate accountRelate = DpaAccountRelateDao.selectFirst_odb3(account.getAcct_no(), E_ACCTBUSITYPE.DEPOSIT, false);

		// 未关联子账户 或者 已关联子账户,但子账户状态是非正常的,同销账户处理.
		if (accountRelate == null || accountRelate.getAcct_status() != E_ACCTSTATUS.NORMAL) {

			// 销账户处理
			DpCloseAccountIn cplAcctIn = BizUtil.getInstance(DpCloseAccountIn.class);

			cplAcctIn.setAcct_no(cplIn.getAcct_no()); // 账号
			cplAcctIn.setAcct_type(account.getAcct_type()); // 账户类型
			cplAcctIn.setAcct_name(account.getAcct_name()); // 账户名称
			cplAcctIn.setSame_close_card_ind(cplIn.getSame_close_card_ind());
			cplAcctIn.setCheck_password_ind(E_YESORNO.NO); // 验密标志

			// 调用销账户处理程序
			DpCloseAccount.doMethod(cplAcctIn, account);
			bizlog.method(" DpCloseSubAccount.closeAcct end <<<<<<<<<<<<<<<<");
			return E_YESORNO.YES;
		}

		bizlog.method(" DpCloseSubAccount.closeAcct end <<<<<<<<<<<<<<<<");

		// 是否已销客户
		return E_YESORNO.NO;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月22日-下午4:58:22</li>
	 *         <li>功能说明：添加交易数据到缓存区</li>
	 *         </p>
	 * @param cplIn
	 *            交易输入接口
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 */
	private static void addDataToBuffer(DpCloseSubAccountIn cplIn, DpaSubAccount subAccount, DpaAccount account) {

		// 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		Map<String, Object> mapObj = new HashMap<String, Object>();

		// 计算销户月份
		int openMethods = DateUtil.getMonthCount(DateUtil.parseDate(subAccount.getOpen_acct_date(), "yyyyMMdd"),
				DateUtil.parseDate(BizUtil.getTrxRunEnvs().getTrxn_date(), "yyyyMMdd"));

		mapObj.put("open_months", openMethods);

		// 追加输入数据集
		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObj);

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAccount));

		// 加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(account));

		// 货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAccount.getCcy_code())));

		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(account.getCust_no(), account.getCust_type());

	}

}
