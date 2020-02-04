package cn.sunline.icore.dp.serv.account.close;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpCloseAccountCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCloseAccountCheck.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月11日-下午3:57:21</li>
	 *         <li>功能说明：销户服务主检查程序</li>
	 *         </p>
	 * @param cplIn
	 *            销户输入接口
	 */
	public static void checkMain(DpCloseAccountIn cplIn) {

		bizlog.method(" DpCloseAccountCheck.closeAccount begin >>>>>>>>>>>>>>>>");
		bizlog.debug("closeAccountIn=[%s]", cplIn);

		// 定位账户信息
		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 属性到期自动刷新: 不提交数据库
		DpAttrRefresh.refreshAttrValue(account, cplIn.getAcct_no(), E_YESORNO.NO);

		// 检查客户账户信息
		checkMainMethod(cplIn, account);

		bizlog.method(" DpCloseAccountCheck.closeAccount end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月17日-下午2:05:15</li>
	 *         <li>功能说明：检查客户账户信息</li>
	 *         </p>
	 * @param cplIn
	 *            销户输入接口
	 * @param account
	 *            账户信息
	 */
	public static void checkMainMethod(DpCloseAccountIn cplIn, DpaAccount account) {
		bizlog.method(" DpCloseAccountCheck.checkDpaAccount begin >>>>>>>>>>>>>>>>");

		// 检查账户信息
		checkAcctInfo(cplIn, account);

		// 检查账户开户凭证信息,子户不在此处查询，传空对象
		DpPublicCheck.checkOpenVochStatus(E_YESORNO.YES, BizUtil.getInstance(DpaSubAccount.class), account);

		// 客户账户签约检查
		checkSignAgreement(account);

		bizlog.method(" DpCloseAccountCheck.checkDpaAccount end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午4:33:45</li>
	 *         <li>功能说明：检查账户信息</li>
	 *         </p>
	 * @param cplIn
	 *            销户输入接口
	 * @param account
	 *            账户信息
	 */
	private static void checkAcctInfo(DpCloseAccountIn cplIn, DpaAccount account) {

		// 校验客户账户名称
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(cplIn.getAcct_name(), account.getAcct_name())) {
			throw DpErr.Dp.E0058(account.getAcct_no(), account.getAcct_name());
		}

		// 验证密码标志为 yes,需验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(account, checkIn);
		}

		// 销账户许可检查
		DpBaseServiceApi.checkCloseAcctLicense(account);

		// 账户限制检查
		DpPublicCheck.checkAcctTrxnLimit(account, E_DEPTTRXNEVENT.DP_CLOSE_ACCOUNT, null);
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午4:30:27</li>
	 *         <li>功能说明：账户统一签约信息检查</li>
	 *         </p>
	 * @param account
	 *            账户信息
	 */
	public static void checkSignAgreement(DpaAccount account) {

		// TODO 调用公共方法检查统一签约信息，查看是否可以销户
	}
}
