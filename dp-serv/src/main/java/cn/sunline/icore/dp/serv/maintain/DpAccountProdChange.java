package cn.sunline.icore.dp.serv.maintain;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountCondition;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountConditionDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountType;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfDraw;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfSave;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_CARDACCTMDY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_MATURESUREWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNAMTCTRLWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNTIMESCTRLWAY;
import cn.sunline.icore.dp.serv.account.open.DpAddSubAccount;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpbBatchFeeDao;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctInfo;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctQueryIn;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctQueryOut;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAccountProdChangeIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAccountProdChangeOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_CARDACCTQUERYWAY;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.type.EnumType.E_DATASTATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：存款账户产品转换
 * </p>
 * 
 * @Author duanhb
 *         <p>
 *         <li>2018年3月05日-下午3:02:34</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：存款账户产品转换</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAccountProdChange {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAccountProdChange.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2018年3月5日-下午2:14:13</li>
	 *         <li>功能说明：账户产品转换</li>
	 *         </p>
	 * @param cplIn
	 *            输入接口信息
	 * @return DpAccountProdChangeOut 输出信息
	 */
	public static DpAccountProdChangeOut accountProdChange(DpAccountProdChangeIn cplIn) {

		bizlog.method(" DpAccountProdChange.accountProdChange begin >>>>>>>>>>>>>>>>");
		bizlog.method("DpAccountProdChangeIn >>>>===[%s]", cplIn);

		// 1.验证输入数据
		valiedateInput(cplIn);

		// 2.定位账户信息
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
		accessIn.setAcct_no(cplIn.getAcct_no());
		accessIn.setAcct_type(cplIn.getAcct_type());
		accessIn.setCcy_code(cplIn.getCcy_code());
		accessIn.setProd_id(cplIn.getProd_id());
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 3.修改账户信息
		changeAcctType(cplIn);

		// 4.修改子户信息
		changeSubAcctProd(cplIn, accessOut);

		// 5.核销凭证信息
		if (cplIn.getCancle_original_voch_ind() == E_YESORNO.YES) {

			DpaAccount account = DpaAccountDao.selectOne_odb1(accessOut.getAcct_no(), true);

			DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

			DpVoucherIobus.modifyCustVoucherStatus(account, subAccount, "");
		}

		DpAccountProdChangeOut changeOut = BizUtil.getInstance(DpAccountProdChangeOut.class);

		changeOut.setAcct_no(cplIn.getAcct_no());
		changeOut.setCard_no(CommUtil.nvl(accessOut.getCard_no(), cplIn.getNew_card_no()));
		changeOut.setSub_acct_seq(cplIn.getSub_acct_seq());
		changeOut.setAcct_type(cplIn.getNew_acct_type());
		changeOut.setAcct_type_name(DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getNew_acct_type()).getAcct_type_name());
		changeOut.setAcct_name(accessOut.getAcct_name());
		changeOut.setCcy_code(accessOut.getCcy_code());
		changeOut.setProd_id(cplIn.getNew_prod_id());
		changeOut.setProd_name(DpProductFactoryApi.getProdBaseInfo(cplIn.getNew_prod_id()).getProd_name());

		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		changeOut.setTerm_code(subAccount.getTerm_code()); // term code
		changeOut.setAcct_bal(subAccount.getAcct_bal()); // account balance
		changeOut.setStart_inst_date(subAccount.getStart_inst_date());
		changeOut.setDue_date(subAccount.getDue_date()); // due date

		bizlog.method("DpAccountProdChangeIn >>>>===[%s]", changeOut);
		bizlog.method(" DpAccountProdChange.accountProdChange end <<<<<<<<<<<<<<<<");

		return changeOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2018年3月5日-下午3:31:32</li>
	 *         <li>功能说明：修改子户信息</li>
	 *         </p>
	 * @param cplIn
	 *            交易接口输入信息
	 * @param accessOut
	 *            子账户定位输出信息
	 */
	private static void changeSubAcctProd(DpAccountProdChangeIn cplIn, DpAcctAccessOut accessOut) {
		bizlog.method(" DpAccountProdChange.changeSubAcctType begin >>>>>>>>>>>>>>>>");

		// 产品相同不修改产品信息
		if (CommUtil.isNull(cplIn.getNew_prod_id()) || CommUtil.equals(cplIn.getProd_id(), cplIn.getNew_prod_id())) {
			return;
		}

		// 获取子账户信息
		DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		// 获取产品存入控制信息
		DpfSave prodSaveInfo = DpProductFactoryApi.getProdSaveCtrl(cplIn.getNew_prod_id(), cplIn.getCcy_code());

		// 获取产品支取控制信息
		DpfDraw prodDrawInfo = DpProductFactoryApi.getProdDrawCtrl(cplIn.getNew_prod_id(), cplIn.getCcy_code());

		// 1.修改子账户表：
		subAcctInfo = updateSubAccount(cplIn, subAcctInfo, prodSaveInfo, prodDrawInfo);

		// 2.修改存入控制信息
		updateSubAccountSave(subAcctInfo, prodSaveInfo);

		// 3.修改支取控制信息
		updateSubAccountDraw(subAcctInfo, prodDrawInfo);

		// 4.修改账户子户对照信息
		DpaAccountRelate relateInfo = DpaAccountRelateDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_seq(), true);

		DpaAccountRelate oldRelateInfo = BizUtil.clone(DpaAccountRelate.class, relateInfo);

		relateInfo.setAcct_type(cplIn.getNew_acct_type());
		relateInfo.setProd_id(cplIn.getNew_prod_id());

		int i = ApDataAuditApi.regLogOnUpdateBusiness(oldRelateInfo, relateInfo);

		if (i > 0) {

			DpaAccountRelateDao.updateOne_odb1(relateInfo);
		}

		// 5.批量收费信息修改
		DpbBatchFeeDao.delete_odb2(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no());

		DpAddSubAccount.regBatchFee(subAcctInfo);

		bizlog.method(" DpAccountProdChange.changeSubAcctType end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2018年3月6日-下午3:35:01</li>
	 *         <li>功能说明：修改子户信息</li>
	 *         </p>
	 * @param cplIn
	 *            输入接口信息
	 * @param subAcctInfo
	 *            子户信息
	 * @param prodSaveInfo
	 *            产品存入信息
	 * @param prodDrawInfo
	 *            产品支取信息
	 * @return
	 */
	private static DpaSubAccount updateSubAccount(DpAccountProdChangeIn cplIn, DpaSubAccount subAcctInfo, DpfSave prodSaveInfo, DpfDraw prodDrawInfo) {

		DpaSubAccount beforeSubAcct = BizUtil.clone(DpaSubAccount.class, subAcctInfo);

		// 获取产品基础信息
		DpfBase prodBaseInfo = DpProductFactoryApi.getProdBaseInfo(cplIn.getNew_prod_id());

		// 1.修改子户产品之后,修改子户核算别名
		subAcctInfo.setProd_id(cplIn.getNew_prod_id());
		// 1.1修改子户核算别名
		DpAccountingAlaisMaitain.modifyAccountingalias(subAcctInfo, beforeSubAcct);

		// 2.重新查询子户信息
		subAcctInfo = DpaSubAccountDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), true);

		DpaSubAccount oldSubAcctInfo = BizUtil.clone(DpaSubAccount.class, subAcctInfo);

		// 2.1子账户信息产品相关数据修改
		subAcctInfo.setMin_remain_bal(prodDrawInfo == null ? BigDecimal.ZERO : prodDrawInfo.getMin_remain_bal()); // 账户最小留存余额
		subAcctInfo.setCorrelation_voch_ind((subAcctInfo.getCorrelation_voch_ind() == E_YESORNO.YES && cplIn.getCancle_original_voch_ind() == E_YESORNO.NO) ? E_YESORNO.YES
				: E_YESORNO.NO); // 关联凭证标志

		subAcctInfo.setInst_ind(prodBaseInfo.getInst_ind()); // 计息标志
		subAcctInfo.setAllow_hand_rate_ind(prodBaseInfo.getAllow_hand_rate_ind());
		subAcctInfo.setUse_cheque_allow_ind(prodBaseInfo.getUse_cheque_allow_ind());
		subAcctInfo.setRenewal_method(prodBaseInfo.getDue_date_confirm_method() == E_MATURESUREWAY.NONE ? null : subAcctInfo.getRenewal_method()); // 续存方式
		subAcctInfo.setSpec_dept_type(prodBaseInfo.getSpec_dept_type());
		subAcctInfo.setOverdraft_allow_ind(prodBaseInfo.getOverdraft_allow_ind()); // 准许透支标志

		int i = ApDataAuditApi.regLogOnUpdateBusiness(oldSubAcctInfo, subAcctInfo);
		if (i > 0) {

			DpaSubAccountDao.updateOne_odb1(subAcctInfo);
		}

		return subAcctInfo;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2018年3月6日-下午3:33:40</li>
	 *         <li>功能说明：修改支取控制信息</li>
	 *         </p>
	 * @param subAcctInfo
	 *            子户信息
	 * @param prodDrawInfo
	 *            产品支取控制信息
	 */
	private static void updateSubAccountDraw(DpaSubAccount subAcctInfo, DpfDraw prodDrawInfo) {

		if (prodDrawInfo == null) {
			prodDrawInfo = BizUtil.getInstance(DpfDraw.class);
		}

		DpaSubAccount oldSubAcctInfo = BizUtil.clone(DpaSubAccount.class, subAcctInfo);

		// 修改支取控制信息 ,已有控制不做处理
		BigDecimal siglMinDrawAmt = CommUtil.nvl(prodDrawInfo.getSigl_min_withdrawal_amt(), BigDecimal.ZERO);
		BigDecimal siglMaxDrawAmt = CommUtil.nvl(prodDrawInfo.getSigl_max_withdrawal_amt(), BigDecimal.ZERO); // 0表示无穷大
		long MaxDrawTimes = CommUtil.nvl(prodDrawInfo.getMax_withdrawal_count(), 0L); // 0表示不限制次数

		subAcctInfo.setSigl_min_withdrawal_amt(siglMinDrawAmt); // 单次支取最小金额
		subAcctInfo.setSigl_max_withdrawal_amt(siglMaxDrawAmt); // 单次支取最大金额
		subAcctInfo.setMax_withdrawal_count(MaxDrawTimes); // 最大支取次数
		subAcctInfo.setMin_remain_bal(prodDrawInfo.getMin_remain_bal()); // 账户留存最小余额
		subAcctInfo.setAccm_withdrawal_amt(BigDecimal.ZERO); // 实际支取金额
		subAcctInfo.setAccm_withdrawal_count(0L); // 实际支取次数

		// 最小值最大值都为零，表示不控制
		if (CommUtil.equals(siglMinDrawAmt, BigDecimal.ZERO) && CommUtil.equals(siglMaxDrawAmt, BigDecimal.ZERO)) {
			subAcctInfo.setWithdrawal_amt_control_method(E_TRXNAMTCTRLWAY.NONE); // 存入金额控制方式
		}
		else if (CommUtil.equals(siglMinDrawAmt, BigDecimal.ZERO) && !CommUtil.equals(siglMaxDrawAmt, BigDecimal.ZERO)) {
			subAcctInfo.setWithdrawal_amt_control_method(E_TRXNAMTCTRLWAY.MOST); // 存入金额控制方式
		}
		else if (!CommUtil.equals(siglMinDrawAmt, BigDecimal.ZERO) && CommUtil.equals(siglMaxDrawAmt, BigDecimal.ZERO)) {
			subAcctInfo.setWithdrawal_amt_control_method(E_TRXNAMTCTRLWAY.LEAST); // 存入金额控制方式
		}
		else {
			subAcctInfo.setWithdrawal_amt_control_method(E_TRXNAMTCTRLWAY.RANGE); // 存入金额控制方式
		}

		// 存入次数控制方式
		if (MaxDrawTimes == 0) {
			subAcctInfo.setWithdrawal_count_ctrl_method(E_TRXNTIMESCTRLWAY.NONE); // 存入次数控制方式
		}
		else {
			subAcctInfo.setWithdrawal_count_ctrl_method(E_TRXNTIMESCTRLWAY.MOST); // 存入次数控制方式
		}

		int i = ApDataAuditApi.regLogOnUpdateBusiness(oldSubAcctInfo, subAcctInfo);
		if (i > 0) {

			DpaSubAccountDao.updateOne_odb1(subAcctInfo);
		}
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2018年3月6日-下午3:32:52</li>
	 *         <li>功能说明：修改存入控制信息</li>
	 *         </p>
	 * @param subAcctInfo
	 *            子户信息
	 * @param prodSaveInfo
	 *            产品存入信息
	 */
	private static void updateSubAccountSave(DpaSubAccount subAcctInfo, DpfSave prodSaveInfo) {

		// 修改存入控制信息,已有控制不做处理
		BigDecimal siglMinSaveAmt = CommUtil.nvl(prodSaveInfo.getSigl_min_dept_amt(), BigDecimal.ZERO);
		BigDecimal siglMaxSaveAmt = CommUtil.nvl(prodSaveInfo.getSigl_max_dept_amt(), BigDecimal.ZERO); // 0表示无穷大
		long MaxSaveTimes = CommUtil.nvl(prodSaveInfo.getMax_dept_count(), 0L); // 0表示不限制次数

		// 账户存入控制实例化
		subAcctInfo = DpaSubAccountDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), true);

		DpaSubAccount oldSubAcctInfo = BizUtil.clone(DpaSubAccount.class, subAcctInfo);

		subAcctInfo.setSub_acct_no(subAcctInfo.getSub_acct_no()); // 子账号
		subAcctInfo.setSigl_min_dept_amt(siglMinSaveAmt); // 单次存入最小金额
		subAcctInfo.setSigl_max_dept_amt(siglMaxSaveAmt); // 单次存入最大金额
		subAcctInfo.setMax_dept_count(MaxSaveTimes); // 最大存入次数
		subAcctInfo.setMax_remain_bal(prodSaveInfo.getMax_remain_bal()); // 账户留存最大余额
		subAcctInfo.setAccm_dept_amt(BigDecimal.ZERO); // 实际存入金额
		subAcctInfo.setAccm_dept_count(0L); // 实际存入次数

		// 最小值最大值都为零，表示不控制
		if (CommUtil.equals(siglMinSaveAmt, BigDecimal.ZERO) && CommUtil.equals(siglMaxSaveAmt, BigDecimal.ZERO)) {
			subAcctInfo.setDept_amt_ctrl_method(E_TRXNAMTCTRLWAY.NONE); // 存入金额控制方式
		}
		else if (CommUtil.equals(siglMinSaveAmt, BigDecimal.ZERO) && !CommUtil.equals(siglMaxSaveAmt, BigDecimal.ZERO)) {
			subAcctInfo.setDept_amt_ctrl_method(E_TRXNAMTCTRLWAY.MOST); // 存入金额控制方式
		}
		else if (!CommUtil.equals(siglMinSaveAmt, BigDecimal.ZERO) && CommUtil.equals(siglMaxSaveAmt, BigDecimal.ZERO)) {
			subAcctInfo.setDept_amt_ctrl_method(E_TRXNAMTCTRLWAY.LEAST); // 存入金额控制方式
		}
		else {
			subAcctInfo.setDept_amt_ctrl_method(E_TRXNAMTCTRLWAY.RANGE); // 存入金额控制方式
		}

		// 存入次数控制方式
		if (MaxSaveTimes == 0) {
			subAcctInfo.setDept_count_control_method(E_TRXNTIMESCTRLWAY.NONE); // 存入次数控制方式
		}
		else {
			subAcctInfo.setDept_count_control_method(E_TRXNTIMESCTRLWAY.MOST); // 存入次数控制方式
		}

		int i = ApDataAuditApi.regLogOnUpdateBusiness(oldSubAcctInfo, subAcctInfo);

		if (i > 0) {

			DpaSubAccountDao.updateOne_odb1(subAcctInfo);
		}
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2018年3月5日-下午3:29:12</li>
	 *         <li>功能说明：修改账户信息</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void changeAcctType(DpAccountProdChangeIn cplIn) {

		bizlog.method(" DpAccountProdChange.changeAcctType begin >>>>>>>>>>>>>>>>");

		// 账户类型相同不修改账户信息
		if (CommUtil.isNull(cplIn.getNew_acct_type()) || CommUtil.equals(cplIn.getNew_acct_type(), cplIn.getAcct_type())) {
			return;
		}

		// 1.修改账户类型相关信息
		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		DpaAccount oldAccount = BizUtil.clone(DpaAccount.class, account);

		// 有输入账户名称,校验账户名称
		if (CommUtil.isNotNull(cplIn.getAcct_name())) {

			if (!CommUtil.equals(account.getAcct_name(), cplIn.getAcct_name())) {

				throw DpErr.Dp.E0058(account.getAcct_name(), cplIn.getAcct_name());
			}
		}

		// 2.验证密码标志为 yes,需验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(account, checkIn);
		}

		DppAccountType newAcctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getNew_acct_type());

		if (newAcctType.getCard_relationship_ind() == E_YESORNO.NO) {

			List<DppAccountCondition> conditionList = DppAccountConditionDao.selectAll_odb2(newAcctType.getAcct_type(), false);

			if (!conditionList.isEmpty() && conditionList.size() == 1 && CommUtil.isNull(account.getWithdrawal_cond())) {

				account.setWithdrawal_cond(conditionList.get(0).getWithdrawal_cond());
			}
		}

		account.setAcct_type(newAcctType.getAcct_type());
		account.setCard_relationship_ind(newAcctType.getCard_relationship_ind()); // 卡账关系标志
		account.setSigl_sub_acct_ind(newAcctType.getSigl_sub_acct_ind()); // 单子户账户标志
		account.setSigl_ccy_acct_ind(newAcctType.getSigl_ccy_acct_ind()); // 单币种账户标志
		account.setSigl_prod_acct_ind(newAcctType.getSigl_prod_acct_ind()); // 单产品账户标志

		// 凭证相关
		account.setOpen_acct_voch_ind(newAcctType.getOpen_acct_voch_ind());
		account.setRef_voch_level(newAcctType.getRef_voch_level());
		account.setCorrelation_voch_ind(account.getCorrelation_voch_ind() == E_YESORNO.YES && cplIn.getCancle_original_voch_ind() == E_YESORNO.NO ? E_YESORNO.YES : E_YESORNO.NO); // 关联凭证标志

		int i = ApDataAuditApi.regLogOnUpdateBusiness(oldAccount, account);
		if (i > 0) {

			DpaAccountDao.updateOne_odb1(account);
		}

		// 3.卡账关系变更
		DppAccountType oldAcctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getAcct_type());

		if (oldAcctType.getCard_relationship_ind() != newAcctType.getCard_relationship_ind()) {

			// 非卡类变成卡类时要传入卡号要登记卡账关系
			if (newAcctType.getCard_relationship_ind() == E_YESORNO.YES) {

				DpBaseServiceApi.registerCardInfo(account, cplIn.getNew_card_no(), null);
			}
			// 卡类变成非卡类时去掉卡账关系。
			else if (oldAcctType.getCard_relationship_ind() == E_YESORNO.YES) {

				// 根据账户查询出卡账关系
				DpCardAcctQueryIn cardAcctQryIn = BizUtil.getInstance(DpCardAcctQueryIn.class);

				cardAcctQryIn.setAcct_no(account.getAcct_no());
				cardAcctQryIn.setAcct_type(account.getAcct_type());
				cardAcctQryIn.setCard_acct_query_way(E_CARDACCTQUERYWAY.ACCT);

				DpCardAcctQueryOut cardAcctList = DpCardAccountRelate.cardAccountQuery(cardAcctQryIn);

				for (DpCardAcctInfo cardAcct : cardAcctList.getList01()) {

					// 取消卡账关系变更登记簿
					DpBaseServiceApi.registerCardChangeBook(cardAcct.getCard_no(), account.getAcct_no(), E_CARDACCTMDY.CLOSE);
				}
			}
		}

		bizlog.method(" DpAccountProdChange.enclosing_method end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2018年3月5日-下午2:13:50</li>
	 *         <li>功能说明：检查输入信息合法性</li>
	 *         </p>
	 * @param cplIn
	 *            输入接口
	 */
	private static void valiedateInput(DpAccountProdChangeIn cplIn) {
		bizlog.method(" DpAccountProdChange.valiedateInput begin >>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号

		// 新产品代码和新账户类型不能同时为空,也不能同时与账户层原本的值相同
		if (CommUtil.isNull(cplIn.getNew_acct_type()) && CommUtil.isNull(cplIn.getNew_prod_id())) {

			BizUtil.fieldNotNull(cplIn.getNew_acct_type(), DpBaseDict.A.new_acct_type.getId(), DpBaseDict.A.new_acct_type.getLongName());
		}
		else {

			if (CommUtil.equals(cplIn.getNew_acct_type(), cplIn.getAcct_type()) && CommUtil.equals(cplIn.getNew_prod_id(), cplIn.getProd_id())) {

				StringBuffer buffStr = new StringBuffer();

				buffStr.append(DpBaseDict.A.new_acct_type.getLongName()).append(":").append(cplIn.getNew_acct_type());
				buffStr.append("-");
				buffStr.append(SysDict.A.acct_type.getLongName()).append(":").append(cplIn.getAcct_type());
				buffStr.append(",");
				buffStr.append(DpBaseDict.A.new_prod_id.getLongName()).append(":").append(cplIn.getNew_prod_id());
				buffStr.append("-");
				buffStr.append(SysDict.A.prod_id.getLongName()).append(":").append(cplIn.getProd_id());

				ApPubErr.APPUB.E0023(buffStr.toString());
			}
		}

		if (CommUtil.isNotNull(cplIn.getNew_acct_type())) {

			if (!CommUtil.equals(cplIn.getNew_acct_type(), cplIn.getAcct_type())) {

				DppAccountType newAcctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getNew_acct_type());
				DppAccountType acctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getAcct_type());

				if (newAcctType.getCard_relationship_ind() == E_YESORNO.YES) {
					// 当新账户类型不等于旧账户类型且新账户类型是卡时此字段必输
					BizUtil.fieldNotNull(cplIn.getNew_card_no(), DpBaseDict.A.new_card_no.getId(), DpBaseDict.A.new_card_no.getLongName());
				}

				// 默认为Y-YES，当账户类型有修改时传值有效
				if (CommUtil.isNull(cplIn.getCancle_original_voch_ind())) {

					cplIn.setCancle_original_voch_ind(E_YESORNO.YES);
				}

				// 客户类型、资产负债标志必须一致才允许转换账户类型
				if (newAcctType.getCust_type() != acctType.getCust_type() || newAcctType.getAcct_busi_source() != acctType.getAcct_busi_source()) {
					throw DpErr.Dp.E0429();
				}
			}
			else {
				// 账户类型无修改传值无效
				cplIn.setCancle_original_voch_ind(E_YESORNO.NO);
			}
		}
		if (CommUtil.isNotNull(cplIn.getNew_prod_id())) {

			if (!CommUtil.equals(cplIn.getNew_prod_id(), cplIn.getProd_id())) {

				DpfBase newProd = DpProductFactoryApi.getProdBaseInfo(cplIn.getNew_prod_id());
				DpfBase oldProd = DpProductFactoryApi.getProdBaseInfo(cplIn.getProd_id());

				// 产品状态未生效,或日期不在产品有效时间范围内,则报错
				if (newProd.getProd_status() != E_DATASTATUS.EFFECTIVE
						|| !BizUtil.dateBetween(BizUtil.getTrxRunEnvs().getTrxn_date(), newProd.getEffect_date(), true, newProd.getExpiry_date(), false)) {
					throw DpBase.E0400(cplIn.getProd_id());
				}

				// 活期产品与定期产品不能互相转换
				if (newProd.getDd_td_ind() != oldProd.getDd_td_ind()) {
					throw DpErr.Dp.E0430();
				}

				// 特定存款产品不能转换
				if (CommUtil.isNotNull(newProd.getSpec_dept_type()) || CommUtil.isNotNull(oldProd.getSpec_dept_type())) {
					throw DpErr.Dp.E0431();
				}
			}
		}

		// 为空默认不验密
		if (CommUtil.isNull(cplIn.getCheck_password_ind())) {

			cplIn.setCheck_password_ind(E_YESORNO.NO);
		}

		bizlog.method(" DpAccountProdChange.valiedateInput end <<<<<<<<<<<<<<<<");
	}

}
