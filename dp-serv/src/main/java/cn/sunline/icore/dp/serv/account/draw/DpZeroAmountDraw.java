package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_TRXNSTATUS;
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
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ACCTFORM;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.electronic.DpElectronicAccountBinding;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.maintain.DpAccountFormMove;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBill;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBillDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_REDBLUEWORDIND;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：活期零金额支取处理
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2019年1月10日-下午4:06:32</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpZeroAmountDraw {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpZeroAmountDraw.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月12日-下午2:54:18</li>
	 *         <li>功能说明：零金额支取检查服务</li>
	 *         </p>
	 * @param cplIn
	 *            活期支取服务输入接口
	 * @return 活期支取输出
	 */
	public static DpDemandDrawOut checkMain(DpDemandDrawIn cplIn) {

		bizlog.method(" DpZeroAmountDraw.checkMain end ");

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

		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 属性到期自动刷新：不提交数据库
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.NO);

		// 检查主逻辑
		DpDemandDrawOut cplOut = checkMethod(cplIn, account, subAccount);

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpZeroAmountDraw.checkMain end ");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月15日-下午2:54:18</li>
	 *         <li>功能说明：零金额支取服务</li>
	 *         </p>
	 * @param cplIn
	 *            活期支取服务输入接口
	 * @return 活期支取输出
	 */
	public static DpDemandDrawOut doMain(DpDemandDrawIn cplIn) {

		bizlog.method(" DpZeroAmountDraw.doMain begin ");
		bizlog.debug("cplInput=[%s]", cplIn);

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

		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 属性到期自动刷新：提交数据库
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.YES);

		// 主处理方法
		DpDemandDrawOut cplOut = doMainMethod(cplIn, account, subAccount);

		bizlog.method(" DpZeroAmountDraw.doMain end ");
		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月12日-下午2:54:18</li>
	 *         <li>功能说明：零金额支取服务主处理方法</li>
	 *         </p>
	 * @param cplIn
	 *            活期支取服务输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 * @return 活期支取输出
	 */
	public static DpDemandDrawOut doMainMethod(DpDemandDrawIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		// 到期自动解冻
		boolean existsUnfroze = DpUnFroze.matureAutoUnfrozen(acctInfo.getCust_no());

		// 存在解冻标志，里面可能更新冻结标志状态
		if (existsUnfroze) {
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);
		}

		// 临时处理，子账户自增1
		subAcct.setData_version(subAcct.getData_version() + 1);
		DpaSubAccountDao.updateOne_odb1(subAcct);

		// 交易运行变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 登记零交易金额账单
		DpsBill billInfo = BizUtil.getInstance(DpsBill.class);

		/* 账单关键信息 */
		billInfo.setSub_acct_no(subAcct.getSub_acct_no());
		billInfo.setSerial_no(subAcct.getData_version()); // 子账户表的数据版本号做序号
		billInfo.setBack_value_date(runEnvs.getTrxn_date());
		billInfo.setShow_ind(E_YESORNO.YES);
		billInfo.setTally_record_ind(E_YESORNO.NO);
		billInfo.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL);
		billInfo.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		billInfo.setDebit_credit(E_DEBITCREDIT.DEBIT);
		billInfo.setTrxn_ccy(cplIn.getCcy_code());
		billInfo.setTrxn_amt(BigDecimal.ZERO);
		billInfo.setTrxn_remark(cplIn.getTrxn_remark());
		billInfo.setCustomer_remark(cplIn.getCustomer_remark());
		billInfo.setSummary_code(cplIn.getSummary_code());
		billInfo.setSummary_name(ApSummaryApi.getText(cplIn.getSummary_code()));
		billInfo.setTrxn_status(E_TRXNSTATUS.NORMAL);

		/* 交易主体信息 */
		billInfo.setAcct_no(subAcct.getAcct_no());
		billInfo.setSub_acct_seq(subAcct.getSub_acct_seq());
		billInfo.setProd_id(subAcct.getProd_id());
		billInfo.setDd_td_ind(subAcct.getDd_td_ind());
		billInfo.setAcct_name(subAcct.getSub_acct_name());
		billInfo.setAcct_branch(subAcct.getSub_acct_branch());
		billInfo.setAcct_branch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name());
		billInfo.setAccounting_alias(subAcct.getAccounting_alias());
		billInfo.setCust_no(subAcct.getCust_no());
		billInfo.setCust_type(subAcct.getCust_type());

		// 交易对手方
		billInfo.setOpp_acct_route(cplIn.getOpp_acct_route());
		billInfo.setOpp_acct_no(cplIn.getOpp_acct_no());
		billInfo.setOpp_card_no("");
		billInfo.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		billInfo.setOpp_trxn_amt(cplIn.getOpp_trxn_amt());
		billInfo.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		billInfo.setOpp_branch_id(cplIn.getOpp_branch_id());
		billInfo.setOpp_branch_name("");

		/* 交易环境信息 */
		billInfo.setExternal_scene_code(runEnvs.getExternal_scene_code());
		billInfo.setPayment_mode(runEnvs.getPayment_mode());
		billInfo.setFee_code(cplIn.getChrg_code());
		billInfo.setTrxn_code(runEnvs.getTrxn_code());
		billInfo.setRecon_code(runEnvs.getRecon_code());
		billInfo.setThird_party_date(runEnvs.getInitiator_date());
		billInfo.setTrxn_channel(runEnvs.getChannel_id());
		billInfo.setTrxn_date(CommUtil.nvl(billInfo.getTrxn_date(), runEnvs.getTrxn_date()));
		billInfo.setBusi_seq(runEnvs.getBusi_seq());
		billInfo.setTrxn_seq(runEnvs.getTrxn_seq());
		billInfo.setTrxn_branch(runEnvs.getTrxn_branch());
		billInfo.setTrxn_teller(runEnvs.getTrxn_teller());
		billInfo.setTrxn_time(runEnvs.getComputer_time());
		billInfo.setHost_date(runEnvs.getComputer_date());
		billInfo.setVat_amt(cplIn.getVat_amt());
		billInfo.setVat_rate(cplIn.getVat_rate());

		/* 冲正信息 */
		billInfo.setReversal_ind(BizUtil.getTrxRunEnvs().getRegister_trxn_ind());

		/* 接口上送对手方信息, 通常为系统外账户信息 */
		billInfo.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
		billInfo.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
		billInfo.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
		billInfo.setReal_opp_country(cplIn.getReal_opp_country());
		billInfo.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
		billInfo.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
		billInfo.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
		billInfo.setReal_opp_remark(cplIn.getReal_opp_remark());

		// 平台消费卡券积分信息
		billInfo.setCard_coupon_acct_no(cplIn.getCard_coupon_acct_no());
		billInfo.setCard_coupon_code(cplIn.getCard_coupon_code());
		billInfo.setCard_coupon_source(cplIn.getCard_coupon_source());
		billInfo.setCard_coupon_trxn_amt(cplIn.getCard_coupon_trxn_amt());
		billInfo.setIntegral_acct_no(cplIn.getIntegral_acct_no());
		billInfo.setIntegral_trxn_amt(cplIn.getIntegral_trxn_amt());
		billInfo.setTrxn_integral(cplIn.getTrxn_integral());

		// 电话号码
		billInfo.setContact_phone(cplIn.getContact_phone());

		// 登记账单
		DpsBillDao.insert(billInfo);

		// 更新子账户信息，刷新版本序号
		DpaSubAccountDao.updateOne_odb1(subAcct);

		// 虽是零金额交易， 但它也属于金融交易，也需要动户激活
		if (subAcct.getAcct_form() != E_ACCTFORM.NORMAL) {

			DpAccountFormMove.activationDormantAccount(subAcct);
		}

		// 活期支取服务输出
		DpDemandDrawOut cplOut = BizUtil.getInstance(DpDemandDrawOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_type(acctInfo.getAcct_type()); // 账户类型
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setCust_no(subAcct.getCust_no()); // 客户号
		cplOut.setSub_acct_branch(subAcct.getSub_acct_branch()); // 子账户所属机构
		cplOut.setBranch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name()); // 机构名称
		cplOut.setAcct_bal(subAcct.getAcct_bal()); // 账户余额
		cplOut.setFroze_no(cplIn.getFroze_no());
		cplOut.setAct_withdrawal_amt(cplIn.getTrxn_amt());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月12日-下午2:54:18</li>
	 *         <li>功能说明：零金额支取服务主检查方法</li>
	 *         </p>
	 * @param cplIn
	 *            活期支取服务输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 * @return 活期支取输出
	 */
	private static DpDemandDrawOut checkMethod(DpDemandDrawIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		// 默认值处理
		defaultValue(cplIn);

		// 加载数据缓存区
		addDataToBuffer(cplIn, subAcct, acctInfo);

		// 检查输入有效性
		validInputData(cplIn, subAcct, acctInfo);

		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 交易控制检查: 包括业务规则、属性检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_DRAW.getValue());

		// 账户限制状态检查
		if (cplIn.getAcct_hold_check_Ind() == E_YESORNO.YES) {

			DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_DRAW, cplIn.getFroze_no());
		}

		// 普通支取,进行支取控制检查
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.COMMON && CommUtil.isNull(cplIn.getChrg_code())) {

			DpBaseServiceApi.checkDrawCtrl(cplIn.getTrxn_amt(), subAcct);
		}

		E_YESORNO checkVochInd = cplIn.getOpen_voch_check_ind();
		// 直接扣划,不检查凭证状态
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.DEDUCT) {
			checkVochInd = E_YESORNO.NO;
		}

		// 开户凭证状态检查
		DpPublicCheck.checkOpenVochStatus(checkVochInd, subAcct, acctInfo);

		// 解冻解止检查
		if (CommUtil.isNotNull(cplIn.getFroze_no())) {
			checkUnFroze(cplIn, subAcct);
		}

		// 活期支取服务输出
		DpDemandDrawOut cplOut = BizUtil.getInstance(DpDemandDrawOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_type(acctInfo.getAcct_type()); // 账户类型
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setCust_no(subAcct.getCust_no()); // 客户号
		cplOut.setSub_acct_branch(subAcct.getSub_acct_branch()); // 子账户所属机构
		cplOut.setBranch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name()); // 机构名称
		cplOut.setAcct_bal(subAcct.getAcct_bal()); // 账户余额
		cplOut.setFroze_no(cplIn.getFroze_no());
		cplOut.setAct_withdrawal_amt(cplIn.getTrxn_amt());

		return cplOut;
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
	 *         <li>2017年2月15日-下午2:57:29</li>
	 *         <li>功能说明：验证服务输入接口数据校验</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 * @param subAccount
	 *            子账户信息
	 */
	private static void validInputData(DpDemandDrawIn cplIn, DpaSubAccount subAccount, DpaAccount account) {

		bizlog.method(" DpZeroAmountDraw.validInputData begin >>>>>>>>>>>>>>>>");

		// 子账户状态检查
		if (subAccount.getSub_acct_status() != E_SUBACCTSTATUS.NORMAL) {
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

		bizlog.method(" DpZeroAmountDraw.validInputData end <<<<<<<<<<<<<<<<");
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

		bizlog.method(" DpZeroAmountDraw.checkUnFroze begin >>>>>>>>>>>>>>>>");

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

		// 同客户解冻，调用方法，不调用服务，这样效率更高
		DpUnFroze.checkMain(dpUnFrozeIn);

		bizlog.method(" DpZeroAmountDraw.checkUnFroze end <<<<<<<<<<<<<<<<");
	}
}