package cn.sunline.icore.dp.serv.account.open;

import java.util.List;

import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpFrozeParmApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFrozeDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeParameter.DppFrozeType;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_KYCSTSTUS;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_SPECFROZETYPE;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.froze.DpFroze;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.servicetype.SrvDpFroze;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeObjectIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnfrozeObjectIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAcctKycStatusModifyIn;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZEOBJECT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZESTATUS;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明：跟新kyc以及相关处理
 * </p>
 * 
 * @Author maold
 *         <p>
 *         <li>2018年5月8日-下午3:55:19</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2018年5月8日-ThinkPad：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAcctKycStatus {
	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAcctKycStatus.class);

	/**
	 * @Author maold
	 *         <p>
	 *         <li>2018年6月7日-上午9:54:16</li>
	 *         <li>功能说明：对KYC审核操作进行处理</li>
	 *         </p>
	 * @param cplIn
	 *            kyc审核输入
	 */
	public static void updateKycStatus(DpAcctKycStatusModifyIn cplIn) {
		bizlog.method(" DpAcctKycStatus.updateKycStatus begin >>>>>>>>>>>>>>>>");

		// 获取账户信息，带锁防止并发解冻
		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, true);

		// KYC状态不变，无需继续处理
		if (account.getKyc_status() == cplIn.getKyc_status()) {
			bizlog.error("The KYC status of account No.[%s] has not been modified", account.getAcct_no());
			return;
		}

		DpaAccount oldAccount = BizUtil.clone(DpaAccount.class, account);

		// 如果更新KYC状态为不通过
		if (cplIn.getKyc_status() == E_KYCSTSTUS.NO_PASS) {

			frozeByKycNoPass(account, cplIn.getKyc_status());
		}
		else if (cplIn.getKyc_status() == E_KYCSTSTUS.PASS) {

			// 更新KYC状态为通过
			thawByKycPass(account, cplIn.getKyc_status());
		}
		else {
			throw APPUB.E0026(DpBaseDict.A.kyc_status.getLongName(), cplIn.getKyc_status().getValue());
		}

		// 冻结解冻里面有更新账户限制状态，此处需再读缓存
		account = DpaAccountDao.selectOne_odb1(cplIn.getAcct_no(), true);

		account.setKyc_status(cplIn.getKyc_status());
		account.setKyc_status_date(BizUtil.getTrxRunEnvs().getTrxn_date());

		DpaAccountDao.updateOne_odb1(account);

		// 登记审计日志
		ApDataAuditApi.regLogOnUpdateBusiness(oldAccount, account);

		bizlog.method(" DpAcctKycStatus.updateKycStatus end >>>>>>>>>>>>>>>>");
	}

	/**
	 * @Author maold
	 *         <p>
	 *         <li>2018年6月7日-上午9:34:02</li>
	 *         <li>功能说明：KYC审核不通过</li>
	 *         </p>
	 * @param dpaAccount
	 *            账号信息
	 * @param kyc_status
	 *            kyc状态
	 */
	private static void frozeByKycNoPass(DpaAccount dpaAccount, E_KYCSTSTUS kyc_status) {

		bizlog.method(" DpAcctKycStatus.updateKycStatusNoPass begin >>>>>>>>>>>>>>>>");

		// 根据特种冻结类型获取冻结分类吗
		DppFrozeType frozeKindCode = DpFrozeParmApi.getSpecFrozeInfo(E_SPECFROZETYPE.KYCFROZE, null);

		// 账户冻结
		DpFrozeIn frozeIn = BizUtil.getInstance(DpFrozeIn.class);

		frozeIn.setFroze_kind_code(frozeKindCode.getFroze_kind_code());
		frozeIn.setFroze_object_type(E_FROZEOBJECT.ACCT);
		frozeIn.setAcct_no(dpaAccount.getAcct_no());
		frozeIn.setAcct_type(dpaAccount.getAcct_type());
		frozeIn.setAcct_name(dpaAccount.getAcct_name());
		frozeIn.setFroze_reason(ApBusinessParmApi.getValue("DP_FROZE_REASON", "KYC"));

		// 同客户调用方法效率更高
		DpFroze.doMain(frozeIn);

		bizlog.method(" DpAcctKycStatus.updateKycStatusNoPass end >>>>>>>>>>>>>>>>");
	}

	/**
	 * @Author maold
	 *         <p>
	 *         <li>2018年6月7日-上午9:37:29</li>
	 *         <li>功能说明：KYC审核通过解冻处理</li>
	 *         </p>
	 * @param dpaAccount
	 *            账户信息
	 * @param kyc_status
	 *            kyc状态
	 */
	private static void thawByKycPass(DpaAccount dpaAccount, E_KYCSTSTUS kyc_status) {

		bizlog.method(" DpAcctKycStatus.updateKycStatusNoPass begin >>>>>>>>>>>>>>>>");

		// 获取冻结码
		List<DpbFroze> dpbFroze = DpbFrozeDao.selectAll_odb5(dpaAccount.getAcct_no(), E_FROZESTATUS.FROZE, E_SPECFROZETYPE.KYCFROZE, false);

		if (dpbFroze.size() > 1) {

			throw DpErr.Dp.E0434(dpaAccount.getAcct_no(), E_FROZESTATUS.FROZE, E_SPECFROZETYPE.KYCFROZE);
		}

		if (dpbFroze.size() == 0) {

			return;
		}

		// 解冻处理
		DpUnFrozeIn dpUnFrozeIn = BizUtil.getInstance(DpUnFrozeIn.class);

		dpUnFrozeIn.setFroze_no(dpbFroze.get(0).getFroze_no());
		dpUnFrozeIn.setUnfroze_reason(ApBusinessParmApi.getValue("DP_THAW_REASON", "KYC"));
		dpUnFrozeIn.setAcct_no(dpaAccount.getAcct_no());

		// 同客户下调用方法效率更高
		DpUnFroze.doMain(dpUnFrozeIn);

		bizlog.method(" DpAcctKycStatus.updateKycStatusNoPass end >>>>>>>>>>>>>>>>");
	}

}
