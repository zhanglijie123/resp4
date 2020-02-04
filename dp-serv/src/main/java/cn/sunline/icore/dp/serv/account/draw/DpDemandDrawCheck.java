package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
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
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_DRAWTYPE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.electronic.DpElectronicAccountBinding;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.query.DpAcctQuery;
import cn.sunline.icore.dp.serv.settle.DpSettleVoucherTrxn;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbReservationBook;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbReservationBookDao;
import cn.sunline.icore.dp.serv.tables.TabDpSettleVoucher.DpbSettleVoucher;
import cn.sunline.icore.dp.serv.tables.TabDpSettleVoucher.DpbSettleVoucherDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpQueryAcct.DpMainAcctBalInfo;
import cn.sunline.icore.dp.serv.type.ComDpSettleVoucher.DpSettleVochCancel;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_REDBLUEWORDIND;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：活期支取
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年1月10日-下午4:06:32</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月10日-HongBiao：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpDemandDrawCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDemandDrawCheck.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午4:06:39</li>
	 *         <li>功能说明：活期支取主检查程序</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 * @return
	 */
	public static DpDemandDrawOut checkMain(DpDemandDrawIn cplIn) {

		bizlog.method(" DpDemandDrawCheck.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 获取账户信息，带锁防止并发解冻
		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
		acctAccessIn.setProd_id(cplIn.getProd_id());
		acctAccessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 子账户信息，上锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 属性到期自动刷新: 不提交数据库
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.NO);

		// 活期支取主调检查
		DpDemandDrawOut cplOut = checkMainMethod(cplIn, account, subAccount);

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpDemandDrawCheck.checkMain end <<<<<<<<<<<<<<<<");

		return cplOut;
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
	private static void addDataToBuffer(DpDemandDrawIn cplIn, DpaSubAccount subAccount, DpaAccount account) {

		// 1.1 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		Map<String, Object> mapObj = new HashMap<String, Object>();

		// 借贷标志
		mapObj.put("debit_credit", subAccount.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT);

		// 判断对方账户为电子账户绑定结算户
		String oppAcctNo = CommUtil.nvl(cplIn.getReal_opp_acct_no(), cplIn.getOpp_acct_no());

		E_YESORNO bindingInd = DpElectronicAccountBinding.isBindingSettleAccount(account.getAcct_no(), E_SAVEORWITHDRAWALIND.WITHDRAWAL, oppAcctNo, cplIn.getReal_opp_acct_name());

		mapObj.put("band_acct_ind", bindingInd.getValue());

		mapObj.put("same_cust_ind", E_YESORNO.NO);
		mapObj.put("same_acct_ind", E_YESORNO.NO);

		// 如果对手方为存款类或存放同业类， 并且对手方账号不为空，那么再把对手方账号客户类型、对手方账户类型、对手方产品加到输入区
		if (CommUtil.in(cplIn.getOpp_acct_route(), E_ACCOUTANALY.NOSTRO, E_ACCOUTANALY.DEPOSIT) && CommUtil.isNotNull(cplIn.getOpp_acct_no())) {

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
			accessIn.setAcct_no(cplIn.getOpp_acct_no());
			accessIn.setAcct_type(cplIn.getOpp_acct_type());
			accessIn.setCcy_code(cplIn.getOpp_acct_ccy());
			accessIn.setSub_acct_seq(cplIn.getOpp_sub_acct_seq());

			// 查询对方子户产品
			DpAcctAccessOut locateSubAcct = DpToolsApi.subAcctInquery(accessIn);

			mapObj.put("opp_prod_id", locateSubAcct.getProd_id());

			// 查询对方账号客户类型 账户类型
			DpaAccount oppAcct = DpaAccountDao.selectOne_odb1(locateSubAcct.getAcct_no(), false);

			mapObj.put("opp_cust_type", oppAcct.getCust_type());
			mapObj.put("opp_acct_type", oppAcct.getAcct_type());

			// 对手方账号不为空且为存款类账户
			if (cplIn.getOpp_acct_route() == E_ACCOUTANALY.DEPOSIT) {

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
		}

		// 1.2 追加输入数据集
		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObj);

		// 2 加载子账户数据区
		// DpaAideInfo aideInfo =
		// DpaAideInfoDao.selectOne_odb1(subAccount.getSub_acct_no(), false);
		Map<String, Object> subAcctMar = CommUtil.toMap(subAccount);
		// subAcctMar.put("text_large_remark_3",aideInfo == null ? null :
		// aideInfo.getText_large_remark_3());
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, subAcctMar);

		// 3 加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(account));

		DpMainAcctBalInfo mainAcctBalance = DpAcctQuery.getMainAcctBalance(account.getAcct_no(), null, null);

		Map<String, Object> mapObject = new HashMap<String, Object>();
		// 账户余额
		mapObject.put("total_amt", mainAcctBalance.getTotal_amt());

		// 追加账户数据集
		ApBufferApi.appendData(ApConst.ACCOUNT_DATA_MART, mapObject);

		// 4. 货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAccount.getCcy_code())));

		// 5. 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(account.getCust_no(), account.getCust_type());

		// 6. 加载卡数据集
		if (account.getCard_relationship_ind() == E_YESORNO.YES && !CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no())) {
			DpToolsApi.addDataToCardBuffer(cplIn.getAcct_no());
		}
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月13日-下午2:26:41</li>
	 *         <li>功能说明：活期支取主调方法,并返回输出信息</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 * @param account
	 *            账户信息
	 * @param subAccount
	 *            子账户信息
	 * @return DpDemandDrawOut 服务输出接口
	 */
	public static DpDemandDrawOut checkMainMethod(DpDemandDrawIn cplIn, DpaAccount account, DpaSubAccount subAccount) {

		// 输入空值默认赋值
		defaultValue(cplIn);

		// 添加数据到规则数据缓存区
		addDataToBuffer(cplIn, subAccount, account);

		// 验证输入字段合法性
		validInputData(cplIn, subAccount, account);

		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(account, checkIn);
		}

		// 交易控制检查: 包括业务规则、属性检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_DRAW.getValue());

		// 预约信息检查
		checkResevationAmount(cplIn);

		// 账户限制状态检查
		if (cplIn.getAcct_hold_check_Ind() == E_YESORNO.YES) {

			DpPublicCheck.checkSubAcctTrxnLimit(subAccount, E_DEPTTRXNEVENT.DP_DRAW, cplIn.getFroze_no());
		}

		// 获取冻结编号
		String frozeNo = cplIn.getFroze_no();

		// 结算凭证检查
		if (CommUtil.isNotNull(cplIn.getCheque_no())) {

			DpSettleVochCancel sttleIn = BizUtil.getInstance(DpSettleVochCancel.class);

			sttleIn.setAcct_no(cplIn.getAcct_no()); // account no
			sttleIn.setCcy_code(cplIn.getCcy_code()); // currency code
			sttleIn.setSettle_voch_type(cplIn.getSettle_voch_type()); //
			sttleIn.setCheque_no(cplIn.getCheque_no()); // cheque number
			sttleIn.setFroze_no(cplIn.getFroze_no()); // freeze number
			sttleIn.setTrxn_amt(cplIn.getTrxn_amt());

			DpSettleVoucherTrxn.checkSettleVochVerifyCancellation(sttleIn);

			DpbSettleVoucher settleVochInfo = DpbSettleVoucherDao.selectOneWithLock_odb1(cplIn.getAcct_no(), cplIn.getCcy_code(), cplIn.getSettle_voch_type(), cplIn.getCheque_no(),
					true);

			frozeNo = settleVochInfo.getFroze_no();
		}

		// 可用余额检查: 里面可能会修改交易金额, 销户会跳过
		if (!CommUtil.equals(cplIn.getTrxn_amt(), BigDecimal.ZERO)) {
			checkBalance(cplIn, subAccount, frozeNo);
		}

		// 普通支取,进行支取控制检查
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.COMMON && CommUtil.isNull(cplIn.getChrg_code())) {

			DpBaseServiceApi.checkDrawCtrl(cplIn.getTrxn_amt(), subAccount);
		}
		else if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.DEDUCT) {// 强制扣划,检查扣划金额不可超过账户余额

			if (CommUtil.compare(cplIn.getTrxn_amt(), subAccount.getAcct_bal()) > 0) {

				throw DpErr.Dp.E0355(subAccount.getAcct_bal(), cplIn.getTrxn_amt());
			}
		}

		E_YESORNO checkVochInd = cplIn.getOpen_voch_check_ind();
		// 直接扣划,不检查凭证状态
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.DEDUCT) {
			checkVochInd = E_YESORNO.NO;
		}

		// 开户凭证状态检查
		DpPublicCheck.checkOpenVochStatus(checkVochInd, subAccount, account);

		// 解冻解止检查
		if (CommUtil.isNotNull(cplIn.getFroze_no())) {
			checkUnFroze(cplIn, subAccount);
		}

		// 活期支取服务输出
		DpDemandDrawOut cplOut = BizUtil.getInstance(DpDemandDrawOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(account.getAcct_no()); // 账号
		cplOut.setAcct_type(account.getAcct_type()); // 账户类型
		cplOut.setAcct_name(account.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(subAccount.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(subAccount.getCcy_code()); // 货币代码
		cplOut.setCust_no(subAccount.getCust_no()); // 客户号
		cplOut.setSub_acct_branch(subAccount.getSub_acct_branch()); // 子账户所属机构
		cplOut.setBranch_name(ApBranchApi.getItem(subAccount.getSub_acct_branch()).getBranch_name()); // 机构名称
		cplOut.setAcct_bal(subAccount.getAcct_bal()); // 账户余额
		cplOut.setFroze_no(cplIn.getFroze_no());
		cplOut.setAct_withdrawal_amt(cplIn.getTrxn_amt());

		return cplOut;
	}

	/**
	 * 检查预约金额
	 * 
	 * @param cplIn
	 */
	private static void checkResevationAmount(DpDemandDrawIn cplIn) {

		// 如果预约金额不为空，则预约金额与交易金额需要一样
		if (CommUtil.isNotNull(cplIn.getReservation_no())) {

			DpbReservationBook reservationBook = DpbReservationBookDao.selectFirst_od2(cplIn.getReservation_no(), E_STATUS.VALID, true);

			if (!CommUtil.equals(reservationBook.getReservation_amt(), cplIn.getTrxn_amt()) || !CommUtil.equals(cplIn.getAcct_no(), reservationBook.getAcct_no())) {

				throw DpErr.Dp.E0460(cplIn.getReservation_no());
			}
		}

	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月14日-上午9:54:30</li>
	 *         <li>功能说明：输入为空默认赋值</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 */
	private static void defaultValue(DpDemandDrawIn cplIn) {

		// 红篮字记账标示：默认为蓝字
		if (cplIn.getRed_blue_word_ind() == null) {
			cplIn.setRed_blue_word_ind(E_REDBLUEWORDIND.BLUE);
		}

		// 强制借记标志默认为否
		if (cplIn.getForce_draw_ind() == null) {
			cplIn.setForce_draw_ind(E_YESORNO.NO);
		}

		// 账户限制检查默认为是
		if (cplIn.getAcct_hold_check_Ind() == null) {
			cplIn.setAcct_hold_check_Ind(E_YESORNO.YES);
		}

		// 支取业务类型默认为普通支取
		if (cplIn.getWithdrawal_busi_type() == null) {
			cplIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.COMMON);
		}

		/*
		 * // 销户支取,未录入余额默认为账户余额 if (cplIn.getWithdrawal_busi_type() ==
		 * E_DRAWBUSIKIND.CLOSE) {
		 * 
		 * cplIn.setTrxn_amt(CommUtil.nvl(cplIn.getTrxn_amt(),
		 * subAccount.getAcct_bal())); }
		 */

	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月14日-上午9:54:30</li>
	 *         <li>功能说明： 解冻解止检查</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 * @param subAccount
	 *            子账户信息
	 */
	private static void checkUnFroze(DpDemandDrawIn cplIn, DpaSubAccount subAccount) {

		bizlog.method(" DpDemandDrawCheck.checkUnFroze begin >>>>>>>>>>>>>>>>");

		DpUnFrozeIn dpUnFrozeIn = BizUtil.getInstance(DpUnFrozeIn.class);

		dpUnFrozeIn.setFroze_no(cplIn.getFroze_no());
		dpUnFrozeIn.setUnfroze_reason(cplIn.getUnfroze_reason());
		dpUnFrozeIn.setWithdrawal_busi_type(cplIn.getWithdrawal_busi_type());
		dpUnFrozeIn.setAcct_no(cplIn.getAcct_no());
		dpUnFrozeIn.setUnfroze_amt(cplIn.getUnfroze_amt());
		dpUnFrozeIn.setAcct_name(cplIn.getAcct_name());
		dpUnFrozeIn.setAcct_type(cplIn.getAcct_type());
		dpUnFrozeIn.setCcy_code(cplIn.getCcy_code());
		dpUnFrozeIn.setProd_id(cplIn.getProd_id());
		dpUnFrozeIn.setSub_acct_seq(subAccount.getSub_acct_seq());
		dpUnFrozeIn.setFroze_feature_code(cplIn.getFroze_feature_code());
		dpUnFrozeIn.setCust_no(subAccount.getCust_no());

		// 同一客户下的解冻， 不用另外开辟规则区，调方法，不调服务
		DpUnFroze.checkMain(dpUnFrozeIn);

		bizlog.method(" DpDemandDrawCheck.checkUnFroze end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午2:57:29</li>
	 *         <li>功能说明：验证服务输入接口数据校验</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 * @param subAccount
	 *            子账户信息
	 */
	private static void validInputData(DpDemandDrawIn cplIn, DpaSubAccount subAccount, DpaAccount account) {
		bizlog.method(" DpDemandDrawCheck.validInputData begin >>>>>>>>>>>>>>>>");

		// 子账户状态检查
		if (subAccount.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
			throw DpBase.E0017(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

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

		// 检查红蓝字交易金额合法性
		DpToolsApi.checkRedBlueIndTrxnAmt(cplIn.getRed_blue_word_ind(), cplIn.getTrxn_amt());

		// 客户账户名称一致性检查
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(cplIn.getAcct_name(), account.getAcct_name())) {
			throw DpErr.Dp.E0058(cplIn.getAcct_name(), subAccount.getSub_acct_name());
		}

		// 倒起息日期检查
		if (CommUtil.isNotNull(cplIn.getBack_value_date()) && CommUtil.compare(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) > 0) {

			throw DpBase.E0258(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		bizlog.method(" DpDemandDrawCheck.validInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午2:57:29</li>
	 *         <li>功能说明：支取服务余额检查</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 * @param subAccount
	 *            子账户信息
	 */
	private static void checkBalance(DpDemandDrawIn cplIn, DpaSubAccount subAccount, String frozeNo) {

		bizlog.method(" DpDemandDrawCheck.checkBalance begin >>>>>>>>>>>>>>>>");

		// 获取子账户余额信息
		E_DRAWTYPE drawType = cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.DEDUCT ? E_DRAWTYPE.DEDUCT : E_DRAWTYPE.COMMON;

		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.CLOSE) {
			drawType = E_DRAWTYPE.CLOSE;
		}

		// 余额信息查询
		DpBalanceCalculateOut balance = DpToolsApi.getBalance(subAccount.getSub_acct_no(), cplIn.getAcct_no(), drawType, frozeNo, cplIn.getUnfroze_amt());

		// 账户控制检查标志为不检查则检查宽限余额， 宽限余额 = 账户余额 + 资金池额度 - 最小流程金额
		if (cplIn.getAcct_hold_check_Ind() == E_YESORNO.NO) {

			if (CommUtil.compare(cplIn.getTrxn_amt(), balance.getGrace_bal()) > 0) {

				// 不是强制借记则直接报余额不足错误
				if (cplIn.getForce_draw_ind() != E_YESORNO.YES) {
					throw DpBase.E0118(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
				}
				else {

					if (CommUtil.compare(balance.getGrace_bal(), BigDecimal.ZERO) <= 0) {
						throw DpBase.E0118(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
					}

					cplIn.setTrxn_amt(balance.getGrace_bal());

					if (CommUtil.equals(cplIn.getCcy_code(), cplIn.getOpp_acct_ccy())) {
						cplIn.setOpp_trxn_amt(cplIn.getTrxn_amt());
					}
				}
			}
		}
		else {

			// 可用余额不足检查
			if (CommUtil.compare(cplIn.getTrxn_amt(), balance.getUsable_bal()) > 0) {

				// 日终透支结息不检查可用余额是否足够
				if (!CommUtil.equals(ApSystemParmApi.getValue(DpConst.EOD_OD_SETTLE_INST_FLOW_TRXN), BizUtil.getTrxRunEnvs().getTrxn_code())) {

					// 不是强制借记则直接报余额不足错误
					if (cplIn.getForce_draw_ind() != E_YESORNO.YES) {
						if (CommUtil.compare(balance.getFroze_amt(), BigDecimal.valueOf(0)) > 0 || CommUtil.compare(balance.getFact_froze_amt(), BigDecimal.valueOf(0)) > 0) {
							throw DpErr.Dp.E0497(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
						}
						else {
							throw DpBase.E0118(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
						}
					}
					else {

						if (CommUtil.compare(balance.getUsable_bal(), BigDecimal.ZERO) <= 0) {
							throw DpBase.E0118(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
						}

						cplIn.setTrxn_amt(balance.getUsable_bal());

						if (CommUtil.equals(cplIn.getCcy_code(), cplIn.getOpp_acct_ccy())) {
							cplIn.setOpp_trxn_amt(cplIn.getTrxn_amt());
						}
					}
				}
			}

			bizlog.method(" DpDemandDrawCheck.checkBalance end <<<<<<<<<<<<<<<<");
		}
	}
}