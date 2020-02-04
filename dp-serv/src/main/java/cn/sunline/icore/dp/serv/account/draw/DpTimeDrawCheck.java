package cn.sunline.icore.dp.serv.account.draw;

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
import cn.sunline.icore.dp.base.api.DpTimeInterestApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTHANDLINGMETHOD;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TIMEDRAWDATESCENE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.errors.DpErr.Dp;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpCommonDao;
import cn.sunline.icore.dp.serv.query.DpAcctQuery;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDeposit;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDepositDao;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpQueryAcct.DpMainAcctBalInfo;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpVoucherParmInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_VOCHPROP;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：定期本金支取(销户)相关
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月10日-下午4:11:48</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月10日-HongBiao：定期支取</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTimeDrawCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeDrawCheck.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月10日-下午4:11:58</li>
	 *         <li>功能说明：定期支取检查服务</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpTimeDrawOut checkMain(DpTimeDrawIn cplIn) {

		bizlog.method(" DpTimeDrawCheck.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 定位账户信息, 带锁避免并发解冻
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 获取子账户信息：带锁
		DpaSubAccount subAcct = DpToolsApi.getTdSubAcct(acctInfo.getAcct_no(), cplIn.getSub_acct_seq(), E_YESORNO.YES);

		// 属性到期自动刷新: 不提交数据库
		DpAttrRefresh.refreshAttrValue(subAcct, acctInfo, cplIn.getAcct_no(), E_YESORNO.NO);

		// 定期支取检查主调方法
		checkMainMethod(cplIn, acctInfo, subAcct);

		// 输出
		DpTimeDrawOut cplOut = BizUtil.getInstance(DpTimeDrawOut.class);

		cplOut.setCust_no(acctInfo.getCust_no());
		cplOut.setAcct_no(acctInfo.getAcct_no());
		cplOut.setAcct_type(acctInfo.getAcct_type());
		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplOut.setSub_acct_branch(subAcct.getSub_acct_branch());
		cplOut.setBranch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name());
		cplOut.setAct_withdrawal_amt(cplIn.getTrxn_amt());

		bizlog.method("cplOut=[%s]", cplOut);
		bizlog.method(" DpTimeDrawCheck.demandDraw end <<<<<<<<<<<<<<<<");

		return cplOut;
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
	private static void addDataToBuffer(DpTimeDrawIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		Map<String, Object> mapObj = CommUtil.toMap(cplIn);

		// 定期支取日期场景
		E_TIMEDRAWDATESCENE drawDateScene = DpTimeInterestApi.getDrawDateScene(subAcct, BizUtil.getTrxRunEnvs().getTrxn_date());

		mapObj.put("time_draw_date_scene", drawDateScene);

		mapObj.put("same_acct_ind", E_YESORNO.NO);
		mapObj.put("same_cust_ind", E_YESORNO.NO);

		// 如果对手方为存款类或存放同业类， 并且对手方账号不为空，那么再把对手方账号客户类型、对手方账户类型、对手方产品加到输入区
		if (CommUtil.in(cplIn.getOpp_acct_route(), E_ACCOUTANALY.NOSTRO, E_ACCOUTANALY.DEPOSIT) && CommUtil.isNotNull(cplIn.getOpp_acct_no())) {

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
			accessIn.setAcct_no(cplIn.getOpp_acct_no());
			accessIn.setAcct_type(cplIn.getOpp_acct_type());
			accessIn.setCcy_code(cplIn.getOpp_acct_ccy());

			// 查询对方子户产品
			DpAcctAccessOut locateSubAcct = DpToolsApi.subAcctInquery(accessIn);

			mapObj.put("opp_prod_id", locateSubAcct.getProd_id());

			// 查询对方账号客户类型 账户类型
			DpaAccount oppAcct = DpaAccountDao.selectOne_odb1(cplIn.getOpp_acct_no(), false);

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

		// 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, mapObj);

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 加载账户数据区
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		DpMainAcctBalInfo mainAcctBalance = DpAcctQuery.getMainAcctBalance(acctInfo.getAcct_no(), null, null);

		Map<String, Object> mapObject = new HashMap<String, Object>();
		// 账户余额
		mapObject.put("total_amt", mainAcctBalance.getTotal_amt());

		// 追加账户数据集
		ApBufferApi.appendData(ApConst.ACCOUNT_DATA_MART, mapObject);

		// 加载币种数据区
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 加载客户数据区
		DpPublicCheck.addDataToCustBuffer(acctInfo.getCust_no(), acctInfo.getCust_type());

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月10日-下午4:11:58</li>
	 *         <li>功能说明：定期支取主检查方法</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 */
	public static void checkMainMethod(DpTimeDrawIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		// 输入空值默认处理
		defaultValue(cplIn, subAcct);

		// 加载数据区
		addDataToBuffer(cplIn, acctInfo, subAcct);

		// 1. 输入要素合法性检查
		checkElementVaild(cplIn, acctInfo, subAcct);

		// 2. 检查支取控制表
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.COMMON) {

			DpBaseServiceApi.checkDrawCtrl(cplIn.getTrxn_amt(), subAcct);

			// 按支取计划支取金额的要么销户，要么只能支取到计划时的支取金额
			if (CommUtil.isNotNull(subAcct.getScheduled_withdrawal_cycle()) && !CommUtil.equals(cplIn.getTrxn_amt(), subAcct.getAcct_bal())) {

				BigDecimal drawableAmt = CommUtil.nvl(SqlDpCommonDao.getTimeDrawPlanTotalAmount(subAcct.getAcct_no(), subAcct.getSub_acct_no(),
						BizUtil.getTrxRunEnvs().getTrxn_date(), subAcct.getOrg_id(), false), BigDecimal.ZERO);

				// 支取计划剩余可支取金额比交易金额小
				if (CommUtil.compare(drawableAmt.subtract(subAcct.getAccm_withdrawal_amt()), cplIn.getTrxn_amt()) < 0) {
					throw DpBase.E0118(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
				}
			}
		}

		E_YESORNO checkVochInd = cplIn.getOpen_voch_check_ind();
		// 直接扣划,不检查凭证状态
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.DEDUCT && CommUtil.isNull(cplIn.getFroze_no())) {
			checkVochInd = E_YESORNO.NO;
		}
		// 3. 开户凭证状态检查
		DpPublicCheck.checkOpenVochStatus(checkVochInd, subAcct, acctInfo);

		// 4. 解冻合法性检查
		if (CommUtil.isNotNull(cplIn.getFroze_no())) {
			checkUnfroze(cplIn, subAcct);
		}

		// 5. 冻结止付限制检查
		if (cplIn.getAcct_hold_check_Ind() != E_YESORNO.NO) {

			E_DEPTTRXNEVENT eventId = getTimeEventId(cplIn, subAcct);

			DpPublicCheck.checkSubAcctTrxnLimit(subAcct, eventId, cplIn.getFroze_no());
		}

		// 6. 可支取金额检查
		DpBaseServiceApi.checkUsableBalance(cplIn, subAcct);

		// 7. 新凭证使用合法性检查
		if (CommUtil.isNotNull(cplIn.getNew_voch_no())) {
			checkNewVoucher(cplIn, acctInfo);
		}

		// 8. 交易控制检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_TIME_DRAW.getValue());
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月14日-上午9:54:30</li>
	 *         <li>功能说明：输入为空默认赋值</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 * @param subAcct
	 */
	private static void defaultValue(DpTimeDrawIn cplIn, DpaSubAccount subAcct) {

		// 强制借记标志默认为“否”
		if (cplIn.getForce_draw_ind() == null) {
			cplIn.setForce_draw_ind(E_YESORNO.NO);
		}

		// 账户限制检查默认为“是”
		if (cplIn.getAcct_hold_check_Ind() == null) {
			cplIn.setAcct_hold_check_Ind(E_YESORNO.YES);
		}

		// 支取业务类型默认为普通支取
		if (cplIn.getWithdrawal_busi_type() == null) {

			cplIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.COMMON);
		}

		// 交易金额为空或为0则视为全额支取
		if (CommUtil.isNull(cplIn.getTrxn_amt()) || CommUtil.compare(cplIn.getTrxn_amt(), BigDecimal.ZERO) == 0) {

			cplIn.setTrxn_amt(subAcct.getAcct_bal());
		}

		// 普通支取金额等于账户余额,则认为是销户支取
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.COMMON && CommUtil.equals(subAcct.getAcct_bal(), cplIn.getTrxn_amt())) {

			cplIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.CLOSE);
		}

		// 倒起息日期默认为交易日期
		if (CommUtil.isNull(cplIn.getBack_value_date())) {
			cplIn.setBack_value_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 指定利率标志默认为“否”
		if (cplIn.getInst_handling_method() == null) {
			cplIn.setInst_handling_method(E_INSTHANDLINGMETHOD.PRODUCT_DEFINE);
		}

		// 开户凭证检查标志默认为“是”
		if (cplIn.getOpen_voch_check_ind() == null) {
			cplIn.setOpen_voch_check_ind(E_YESORNO.YES);
		}

		// 验密标志默认为“是”
		if (cplIn.getCheck_password_ind() == null) {
			cplIn.setCheck_password_ind(E_YESORNO.YES);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月8日-上午9:11:12</li>
	 *         <li>功能说明：获取定期支取事件ID</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 * @param subAcct
	 *            子账户信息
	 * @return 负债交易事件
	 */
	public static E_DEPTTRXNEVENT getTimeEventId(DpTimeDrawIn cplIn, DpaSubAccount subAcct) {

		E_DEPTTRXNEVENT eventId = E_DEPTTRXNEVENT.DP_DRAW; // 默认为支取事件

		// 普通支取，支取金额等于账户余额则认为是销户
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.COMMON && CommUtil.equals(cplIn.getTrxn_amt(), subAcct.getAcct_bal())) {
			eventId = E_DEPTTRXNEVENT.DP_CLOSE_SUBACCT; // 销户事件
		}

		return eventId;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月8日-上午9:11:12</li>
	 *         <li>功能说明：定期支取基本输入要素合法性检查</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 */
	private static void checkElementVaild(DpTimeDrawIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		bizlog.method(" DpTimeDrawCheck.checkElementVaild begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>>>>cplIn=[%s]", cplIn);

		// 必输性检查
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());
		BizUtil.fieldNotNull(cplIn.getWithdrawal_busi_type(), DpSysDict.A.withdrawal_busi_type.getId(), DpSysDict.A.withdrawal_busi_type.getLongName());
		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

		// 子账户状态检查
		if (subAcct.getSub_acct_status() != E_SUBACCTSTATUS.NORMAL) {
			throw DpBase.E0017(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
		}

		// 定期支取不支持倒起息
		if (CommUtil.isNotNull(cplIn.getBack_value_date()) && CommUtil.compare(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {
			throw DpErr.Dp.E0150();
		}

		// 交易金额不能小于零
		if (CommUtil.isNotNull(cplIn.getTrxn_amt()) && CommUtil.compare(cplIn.getTrxn_amt(), BigDecimal.ZERO) < 0) {
			throw APPUB.E0022(cplIn.getTrxn_amt().toString(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());
		}

		// 账户有余额时，支取金额必须大于零
		if (!CommUtil.equals(subAcct.getAcct_bal(), BigDecimal.ZERO) && CommUtil.compare(cplIn.getTrxn_amt(), BigDecimal.ZERO) <= 0) {
			throw APPUB.E0020(cplIn.getTrxn_amt().toString(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());
		}

		// 金额精度检查：包括了币种合法性检查
		ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getTrxn_amt());

		// 摘要代码检查
		ApSummaryApi.exists(cplIn.getSummary_code(), true);

		// 户名一致性检查
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(cplIn.getAcct_name(), acctInfo.getAcct_name())) {
			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctInfo.getAcct_name());
		}

		// 智能存款定期子户在协议没有解除时不能主动支取。
		DpbSmartDeposit smartDeposit = DpbSmartDepositDao.selectFirst_odb2(cplIn.getAcct_no(), cplIn.getSub_acct_seq(), E_STATUS.VALID, false);
		if (smartDeposit != null && cplIn.getWithdrawal_busi_type() != E_DRAWBUSIKIND.DEDUCT) {
			throw DpErr.Dp.E0438(cplIn.getAcct_no(), cplIn.getSub_acct_seq());
		}

		bizlog.method(" DpTimeDrawCheck.checkElementVaild end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月8日-上午9:11:12</li>
	 *         <li>功能说明：定期支取解冻检查</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 */
	private static void checkUnfroze(DpTimeDrawIn cplIn, DpaSubAccount subAcct) {

		// 解冻服务输入
		DpUnFrozeIn cplUnFrIn = BizUtil.getInstance(DpUnFrozeIn.class);

		cplUnFrIn.setFroze_no(cplIn.getFroze_no());
		cplUnFrIn.setUnfroze_reason(cplIn.getUnfroze_reason());
		cplUnFrIn.setWithdrawal_busi_type(cplIn.getWithdrawal_busi_type());
		cplUnFrIn.setAcct_no(cplIn.getAcct_no());
		cplUnFrIn.setAcct_type(cplIn.getAcct_type());
		cplUnFrIn.setCcy_code(subAcct.getCcy_code());
		cplUnFrIn.setCust_no(subAcct.getCust_no());
		cplUnFrIn.setProd_id(subAcct.getProd_id());
		cplUnFrIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplUnFrIn.setUnfroze_amt(cplIn.getUnfroze_amt());

		// 同客户解冻，调用方法，不调用服务，这样效率更高
		DpUnFroze.checkMain(cplUnFrIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月8日-上午9:11:12</li>
	 *         <li>功能说明：检查新凭证合法性</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 */
	private static void checkNewVoucher(DpTimeDrawIn cplIn, DpaAccount acctInfo) {

		bizlog.method(" DpTimeDrawCheck.checkNewVoucher begin >>>>>>>>>>>>>>>>");

		// 没有输入新凭证号，则直接退出
		if (CommUtil.isNull(cplIn.getNew_voch_no())) {
			bizlog.method(" DpTimeDrawCheck.checkNewVoucher end <<<<<<<<<<<<<<<<");
			return;
		}

		// 凭证参数信息
		DpVoucherParmInfo vochParm = DpVoucherIobus.getVoucherParmInfo(cplIn.getVoch_type());

		// 不是存单类凭证，支取时无需凭证更换
		if (vochParm.getVoch_prop() != E_VOCHPROP.CERT_DP) {
			throw Dp.E0153(acctInfo.getAcct_no(), cplIn.getSub_acct_seq());
		}

		// 可使用凭证查询验证
		DpVoucherIobus.existsUsableVouchers(cplIn.getVoch_type(), cplIn.getNew_voch_no());

		bizlog.method(" DpTimeDrawCheck.checkNewVoucher end <<<<<<<<<<<<<<<<");
	}
}
