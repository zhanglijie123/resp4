package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApLimitApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAdjustIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_DRAWTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_FROZETYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTADJUSTTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.froze.DpFrozePublic;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.fundpool.DpPool;
import cn.sunline.icore.dp.serv.fundpool.DpWithdrawlProtect;
import cn.sunline.icore.dp.serv.instruct.DpPiggyBank;
import cn.sunline.icore.dp.serv.instruct.DpReservationWithdraw;
import cn.sunline.icore.dp.serv.instruct.DpSmartDepositAmt;
import cn.sunline.icore.dp.serv.iobus.DpChargeIobus;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.settle.DpSettleVoucherTrxn;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDeposit;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDepositDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtect;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtectDao;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpAacctTriggerFinaCheckIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalOut;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeOut;
import cn.sunline.icore.dp.serv.type.ComDpFundPool.DpWithdrawlProtectIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositProtectQueryIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositProtectQueryOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.LargeReserWithdrawMainIn;
import cn.sunline.icore.dp.serv.type.ComDpSettleVoucher.DpSettleVochCancel;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAgentInfoRegister;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZESTATUS;
import cn.sunline.icore.sys.type.EnumType.E_ADDSUBTRACT;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.logging.LogConfigManager.SystemType;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：活期支取处理
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
public class DpDemandDraw {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDemandDraw.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午4:06:39</li>
	 *         <li>功能说明：活期支取服务：含检查</li>
	 *         </p>
	 * @param cplIn
	 *            活期支取服务输入接口
	 * @return 活期支取输出接口
	 */
	public static DpDemandDrawOut doMain(DpDemandDrawIn cplIn) {

		bizlog.method(" DpDemandDraw.doMain begin >>>>>>>>>>>>>>>>");
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

		// 属性到期自动刷新：提交数据库
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.YES);

		// 活期支取检查
		DpDemandDrawCheck.checkMainMethod(cplIn, account, subAccount);

		// 场景收费
		if (E_DRAWBUSIKIND.COMMON == cplIn.getWithdrawal_busi_type()) {

			DpChargeIobus.calcAutoChrg(subAccount, E_DEPTTRXNEVENT.DP_DRAW, cplIn.getTrxn_amt());
		}

		// BAY需求：输出智能存款支取保护信息
		DpDemandDrawOut protectOut = getWithdrawlProtectInfo(cplIn, account, subAccount);

		// 活期支取处理
		DpDemandDrawOut cplOut = doMainMethod(cplIn, account, subAccount);

		// 补充输出
		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAccount.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(acctAccessOut.getAcct_no());
		cplOut.setAcct_type(acctAccessOut.getAcct_type());
		cplOut.setAcct_name(acctAccessOut.getAcct_name());
		cplOut.setSub_acct_seq(acctAccessOut.getSub_acct_seq());
		cplOut.setCcy_code(acctAccessOut.getCcy_code());
		cplOut.setCust_no(subAccount.getCust_no());
		cplOut.setProd_id(subAccount.getProd_id());
		cplOut.setSub_acct_branch(subAccount.getSub_acct_branch());
		cplOut.setBranch_name(ApBranchApi.getItem(subAccount.getSub_acct_branch()).getBranch_name());
		cplOut.setFroze_no(cplIn.getFroze_no());
		cplOut.setAct_withdrawal_amt(cplIn.getTrxn_amt());
		cplOut.setCheque_no(cplIn.getCheque_no());
		cplOut.setSettle_voch_type(cplIn.getSettle_voch_type());
		cplOut.setTigger_smart_protect_ind(protectOut.getTigger_smart_protect_ind());
		cplOut.setBreak_authority_ind(protectOut.getBreak_authority_ind());
		cplOut.setProtect_fiche_count(protectOut.getProtect_fiche_count());
		cplOut.setProtect_amt(protectOut.getProtect_amt());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpDemandDraw.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author Administrator
	 *         <p>
	 *         <li>2019年1月2日-上午11:08:39</li>
	 *         <li>功能说明：输出智能存款支取保护信息</li>
	 *         </p>
	 * @param cplIn
	 *            输入信息
	 * @param account
	 *            账户信息
	 * @param subAccount
	 *            子户信息
	 * @param cplOut
	 *            输出信息
	 */
	private static DpDemandDrawOut getWithdrawlProtectInfo(DpDemandDrawIn cplIn, DpaAccount account, DpaSubAccount subAccount) {

		DpDemandDrawOut cplOut = BizUtil.getInstance(DpDemandDrawOut.class);

		if (SysUtil.getCurrentSystemType() == SystemType.batch) {
			return cplOut;
		}

		// 账户保护余额
		BigDecimal sumProtctAmt = DpPool.getProtectAmount(subAccount);

		cplOut.setTigger_smart_protect_ind(E_YESORNO.NO);
		cplOut.setProtect_amt(BigDecimal.ZERO);

		if (CommUtil.equals(sumProtctAmt, BigDecimal.ZERO)) {

			return cplOut;
		}

		// 账户余额
		DpBalanceCalculateOut demandBanlance = DpToolsApi.getBalance(subAccount.getSub_acct_no(), cplIn.getAcct_no(), E_DRAWTYPE.COMMON);

		// 可用余额减去保护金额、保留透支等其他资金池余额
		BigDecimal usableBal = demandBanlance.getUsable_bal().subtract(sumProtctAmt);

		// 交易金额-可用余额=本次需提供保护金额
		BigDecimal protectAmt = cplIn.getTrxn_amt().subtract(usableBal);
		protectAmt = CommUtil.compare(BigDecimal.ZERO, protectAmt) > 0 ? BigDecimal.ZERO : protectAmt;

		DpSmartDepositProtectQueryIn protectQryIn = BizUtil.getInstance(DpSmartDepositProtectQueryIn.class);
		protectQryIn.setAcct_no(account.getAcct_no());
		protectQryIn.setAcct_type(account.getAcct_type());
		protectQryIn.setCcy_code(subAccount.getCcy_code());
		protectQryIn.setProtect_amt(protectAmt);
		DpSmartDepositProtectQueryOut smartDepostPro = DpSmartDepositAmt.qrySmartDepositProtect(protectQryIn);
		
		//如果没有智能存款签约信息则直接退出
		if (CommUtil.isNull(smartDepostPro)){
			return cplOut;
		}

		// 保护卡片数量大于1,表示有触发支取保护
		if (CommUtil.compare(smartDepostPro.getProtect_fiche_count(), 0l) != 0) {

			cplOut.setTigger_smart_protect_ind(E_YESORNO.YES);
			DpbSmartDeposit smartDeposit = DpbSmartDepositDao.selectFirst_odb3(account.getAcct_no(), subAccount.getCcy_code(), E_STATUS.VALID, true);
			cplOut.setBreak_authority_ind(smartDeposit.getBreak_authority_ind());
			cplOut.setProtect_fiche_count(smartDepostPro.getProtect_fiche_count());
			cplOut.setProtect_amt(smartDepostPro.getProtect_amt());
		}

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午4:06:39</li>
	 *         <li>功能说明：活期支取主处理程序</li>
	 *         </p>
	 * @param cplIn
	 *            支取服务输入接口
	 * @param account
	 *            账户信息
	 * @param subAccount
	 *            子账户信息
	 * @return DpDemandDrawOut 活期支取输出接口
	 */
	public static DpDemandDrawOut doMainMethod(DpDemandDrawIn cplIn, DpaAccount account, DpaSubAccount subAccount) {

		// 返回接口赋值
		DpDemandDrawOut cplOut = BizUtil.getInstance(DpDemandDrawOut.class);

		// 强制扣划，且交易金额为零，直接返回账户余额
		if (CommUtil.in(cplIn.getWithdrawal_busi_type(), E_DRAWBUSIKIND.CLOSE, E_DRAWBUSIKIND.DEDUCT) && CommUtil.equals(cplIn.getTrxn_amt(), BigDecimal.ZERO)) {

			BigDecimal frozeBal = BigDecimal.ZERO;
			E_FROZESTATUS frozeStatus = null;

			// 0金额销户若需要解冻执行解冻
			if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.CLOSE && CommUtil.isNotNull(cplIn.getFroze_no())) {

				DpUnFrozeOut unFrozeOut = cancleFroze(cplIn, subAccount);

				frozeBal = unFrozeOut.getFroze_bal();
				frozeStatus = unFrozeOut.getFroze_status();
			}

			// 为支持7*24小时，需要联机处理到期解冻
			DpUnFroze.matureAutoUnfrozen(account.getCust_no());

			// 0金额交易
			cplOut = DpZeroAmountDraw.doMainMethod(cplIn, account, subAccount);

			// 补充输出
			cplOut.setAcct_bal(subAccount.getAcct_bal());
			cplOut.setAct_withdrawal_amt(cplIn.getTrxn_amt());
			cplOut.setFroze_bal(frozeBal);
			cplOut.setFroze_status(frozeStatus);

			return cplOut;
		}

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 结算凭证调用
		if (CommUtil.isNotNull(cplIn.getCheque_no())) {

			DpSettleVochCancel sttleIn = BizUtil.getInstance(DpSettleVochCancel.class);

			sttleIn.setAcct_no(cplIn.getAcct_no());
			sttleIn.setCcy_code(cplIn.getCcy_code());
			sttleIn.setSettle_voch_type(cplIn.getSettle_voch_type());
			sttleIn.setCheque_no(cplIn.getCheque_no());
			sttleIn.setFroze_no(cplIn.getFroze_no());
			sttleIn.setTrxn_amt(cplIn.getTrxn_amt());

			DpSettleVoucherTrxn.settleVochVerifyCancellation(sttleIn);
		}

		// 解冻处理
		if (CommUtil.isNotNull(cplIn.getFroze_no())) {

			DpUnFrozeOut unFrozeOut = cancleFroze(cplIn, subAccount);

			cplOut.setFroze_bal(unFrozeOut.getFroze_bal());
			cplOut.setFroze_status(unFrozeOut.getFroze_status());
		}

		// 为支持7*24小时，需要联机处理到期解冻
		boolean existsUnfroze = DpUnFroze.matureAutoUnfrozen(account.getCust_no());

		// 存在解冻标志，里面可能更新冻结标志状态
		if (existsUnfroze) {
			subAccount = DpaSubAccountDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), true);
		}

		// 法院强制扣划交易
		E_YESORNO lawyerDeduct = E_YESORNO.NO;
		if (CommUtil.isNotNull(BizUtil.getTrxRunEnvs().getFlow_trxn_id())
				&& ApSystemParmApi.getValue("COURT_FORCE_DEDUCT_FLOWTRAN").contains(BizUtil.getTrxRunEnvs().getFlow_trxn_id())) {
			lawyerDeduct = E_YESORNO.YES;
		}

		// 限额累计: 收费不登记限额
		if (CommUtil.isNull(cplIn.getChrg_code()) && lawyerDeduct == E_YESORNO.NO) {
			ApLimitApi.process(E_DEPTTRXNEVENT.DP_DRAW.getValue(), cplIn.getCcy_code(), cplIn.getTrxn_amt());
		}

		// 支取服务里面的倒起息处理，要放在记账处理前面， 因为透支时倒起息依赖记账前的可以额度
		if (CommUtil.isNotNull(cplIn.getBack_value_date()) && CommUtil.compare(cplIn.getBack_value_date(), trxnDate) < 0) {

			DpInstAdjustIn cplAdjustIn = BizUtil.getInstance(DpInstAdjustIn.class);

			cplAdjustIn.setEnd_inst_date(BizUtil.dateAdd("day", trxnDate, -1));
			cplAdjustIn.setInit_inst_start_date(subAccount.getStart_inst_date());
			cplAdjustIn.setInst_adjust_aspect(E_ADDSUBTRACT.SUBTRACT);
			cplAdjustIn.setInst_adjust_type(E_INSTADJUSTTYPE.BACKVALUE);
			cplAdjustIn.setStart_inst_date(cplIn.getBack_value_date());
			cplAdjustIn.setTrxn_amt(cplIn.getTrxn_amt());

			DpInterestBasicApi.adjustInstForBackvalue(subAccount, cplAdjustIn, E_YESORNO.YES);
		}

		// 记账处理
		DpUpdAccBalOut tallyAfterInfo = accountingTally(cplIn, subAccount.getAcct_no(), subAccount.getSub_acct_no());

		// 存钱罐协议检查触发理财
		DpAacctTriggerFinaCheckIn checkIn = BizUtil.getInstance(DpAacctTriggerFinaCheckIn.class);

		checkIn.setAcct_no(account.getAcct_no());
		checkIn.setCcy_code(subAccount.getCcy_code());
		checkIn.setCust_no(account.getCust_no());
		checkIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		checkIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		checkIn.setTrxn_amt(cplIn.getTrxn_amt());

		DpPiggyBank.acctConsTriggerPiggyFinaCheck(checkIn);

		// 代理人信息登记
		if (CommUtil.isNotNull(cplIn.getAgent_doc_no()) && CommUtil.isNotNull(cplIn.getAgent_doc_type())) {

			DpAgentInfoRegister agentInfoIn = BizUtil.getInstance(DpAgentInfoRegister.class);

			agentInfoIn.setAgent_doc_no(cplIn.getAgent_doc_no());
			agentInfoIn.setAgent_doc_type(cplIn.getAgent_doc_type());
			agentInfoIn.setAgent_country(cplIn.getAgent_country());
			agentInfoIn.setAgent_name(cplIn.getAgent_name());
			agentInfoIn.setRemark(ApSummaryApi.getText(cplIn.getSummary_code()));

			DpCustomerIobus.registerAgentInfo(agentInfoIn);
		}

		// 修改预约登記簿
		if (CommUtil.isNotNull(cplIn.getReservation_no())) {

			LargeReserWithdrawMainIn largeReserWithdrawMainIn = BizUtil.getInstance(LargeReserWithdrawMainIn.class);

			largeReserWithdrawMainIn.setReservation_no(cplIn.getReservation_no());
			largeReserWithdrawMainIn.setWithdraw_date(BizUtil.getTrxRunEnvs().getTrxn_date());
			largeReserWithdrawMainIn.setReservation_status(E_STATUS.INVALID);

			DpReservationWithdraw.updateReservationInfo(largeReserWithdrawMainIn);
		}

		// 输出
		cplOut.setAcct_bal(tallyAfterInfo.getAcct_bal()); // 交易后余额
		cplOut.setFroze_no(cplIn.getFroze_no());
		cplOut.setAct_withdrawal_amt(cplIn.getTrxn_amt());
		cplOut.setCheque_no(cplIn.getCheque_no());

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月14日-上午10:14:32</li>
	 *         <li>功能说明：会计记账</li>
	 *         </p>
	 * @param cplIn
	 *            活期支取服务输入接口
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 * @return
	 */
	private static DpUpdAccBalOut accountingTally(DpDemandDrawIn cplIn, String acctNo, String subAcctNo) {

		bizlog.method(" DpDemandDraw.accountingTally begin >>>>>>>>>>>>>>>>");

		// 查询子户信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		// 获取子账户余额信息
		E_DRAWTYPE drawType = cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.DEDUCT ? E_DRAWTYPE.DEDUCT : E_DRAWTYPE.COMMON;

		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.CLOSE) {
			drawType = E_DRAWTYPE.CLOSE;
		}

		// 余额信息查询
		DpBalanceCalculateOut balance = DpToolsApi.getBalance(subAccount.getSub_acct_no(), cplIn.getAcct_no(), drawType, cplIn.getFroze_no(), cplIn.getUnfroze_amt());

		// 自身可用余额比交易金额少且存在有效的支取保护协议则会触发关联保护
		if (CommUtil.compare(balance.getSelf_usable_bal(), cplIn.getTrxn_amt()) < 0) {

			// 查询子账户是否存在有效的支取保护协议
			List<DpbWithdrawlProtect> protectList = DpbWithdrawlProtectDao.selectAll_odb4(subAccount.getAcct_no(), subAccount.getCcy_code(), E_STATUS.VALID, false);

			if (CommUtil.isNotNull(protectList) && protectList.size() > 0) {

				DpWithdrawlProtectIn protectIn = BizUtil.getInstance(DpWithdrawlProtectIn.class);

				protectIn.setAcct_no(cplIn.getAcct_no());
				protectIn.setCcy_code(cplIn.getCcy_code());
				protectIn.setTrxn_amt(cplIn.getTrxn_amt().subtract(balance.getSelf_usable_bal()));
				protectIn.setOpp_acct_route(cplIn.getOpp_acct_route());
				protectIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
				protectIn.setOpp_acct_type(cplIn.getOpp_acct_type());
				protectIn.setOpp_acct_no(cplIn.getOpp_acct_no());
				protectIn.setOpp_branch_id(cplIn.getOpp_branch_id());

				protectIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
				protectIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
				protectIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
				protectIn.setReal_opp_country(cplIn.getReal_opp_country());
				protectIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
				protectIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
				protectIn.setReal_opp_branch_name(cplIn.getReal_opp_bank_name());
				protectIn.setReal_opp_remark(cplIn.getReal_opp_remark());

				DpWithdrawlProtect.withdrawlProtect(protectIn);
			}
		}

		// 本金记账输入
		DpUpdAccBalIn cplInput = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplInput.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAccount.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplInput.setAcct_no(subAccount.getAcct_no());
		cplInput.setSub_acct_no(subAccount.getSub_acct_no()); // 子账号
		cplInput.setBack_value_date(CommUtil.nvl(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date())); // 起息日期
		cplInput.setTrxn_amt(cplIn.getTrxn_amt()); // 交易金额
		cplInput.setTrxn_ccy(cplIn.getCcy_code()); // 交易币种
		cplInput.setDebit_credit(subAccount.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT); // 记账方向
		cplInput.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
		cplInput.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplInput.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplInput.setTrxn_record_type(CommUtil.isNull(cplIn.getTrxn_record_type()) ? E_TRXNRECORDTYPE.NORMAL : cplIn.getTrxn_record_type()); // 交易明细类别
		cplInput.setChrg_code(cplIn.getChrg_code()); // 费用编号
		cplInput.setSummary_code(cplIn.getSummary_code()); // 摘要代码
		cplInput.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
		cplInput.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注

		// 交易事件
		cplInput.setTrxn_event_id(E_DEPTTRXNEVENT.DP_DRAW.getValue());
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.CLOSE) {
			cplInput.setTrxn_event_id(E_DEPTTRXNEVENT.DP_CLOSE_SUBACCT.getValue());
		}

		// 真实对手方信息
		cplInput.setOpp_acct_route(cplIn.getOpp_acct_route()); // 对方账户路由
		cplInput.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
		cplInput.setOpp_branch_id(cplIn.getOpp_branch_id()); // 对方机构号
		cplInput.setOpp_acct_ccy(cplIn.getOpp_acct_ccy()); // 对方币种
		cplInput.setOpp_acct_route(cplIn.getOpp_acct_route()); // 对方账户路由
		cplInput.setOpp_acct_type(cplIn.getOpp_acct_type());
		cplInput.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		cplInput.setOpp_trxn_amt(cplIn.getOpp_trxn_amt());

		// 代理人信息
		cplInput.setAgent_doc_type(cplIn.getAgent_doc_type()); // 代理人证件类型
		cplInput.setAgent_doc_no(cplIn.getAgent_doc_no()); // 代理人证件号
		cplInput.setAgent_name(cplIn.getAgent_name()); // 代理人姓名
		cplInput.setAgent_country(cplIn.getAgent_country()); // 代理人国籍

		// 对账单对手方信息
		cplInput.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
		cplInput.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
		cplInput.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
		cplInput.setReal_opp_country(cplIn.getReal_opp_country());
		cplInput.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
		cplInput.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
		cplInput.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
		cplInput.setReal_opp_remark(cplIn.getReal_opp_remark());

		// 国外刷卡消费信息登记
		cplInput.setTrxn_area(cplIn.getTrxn_area());
		cplInput.setTrxn_area_amt(cplIn.getTrxn_area_amt());
		cplInput.setTrxn_area_ccy(cplIn.getTrxn_area_ccy());
		cplInput.setTrxn_area_exch_rate(cplIn.getTrxn_area_exch_rate());
		cplInput.setConsume_date(cplIn.getConsume_date());
		cplInput.setConsume_time(cplIn.getConsume_time());

		// 增值税和增值税率
		cplInput.setVat_amt(cplIn.getVat_amt());
		cplInput.setVat_rate(cplIn.getVat_rate());

		// 平台消费卡券积分信息
		cplInput.setCard_coupon_acct_no(cplIn.getCard_coupon_acct_no());// 卡券核算编号
		cplInput.setCard_coupon_code(cplIn.getCard_coupon_code());// 卡券编号
		cplInput.setCard_coupon_source(cplIn.getCard_coupon_source());// 卡券来源
		cplInput.setCard_coupon_trxn_amt(cplIn.getCard_coupon_trxn_amt());// 卡券金额
		cplInput.setIntegral_acct_no(cplIn.getIntegral_acct_no());// 积分核算账号
		cplInput.setIntegral_trxn_amt(cplIn.getIntegral_trxn_amt());// 积分金额
		cplInput.setTrxn_integral(cplIn.getTrxn_integral());// 交易积分

		// 电话号码
		cplInput.setContact_phone(cplIn.getContact_phone());

		// 本金记账
		DpUpdAccBalOut cplOut = DpAccounting.online(cplInput);

		bizlog.method(" DpDemandDraw.accountingTally end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午2:54:18</li>
	 *         <li>功能说明：解冻处理</li>
	 *         </p>
	 * @param cplIn
	 *            活期支取服务输入接口 (输入冻结编号为空时,不做处理返回null)
	 * @param subAccount
	 *            子账户信息
	 * @return BigDecimal 冻结余额
	 */
	private static DpUnFrozeOut cancleFroze(DpDemandDrawIn cplIn, DpaSubAccount subAccount) {

		bizlog.method(" DpDemandDraw.cancleFroze begin >>>>>>>>>>>>>>>>");

		// 检查是否全额冻结，全额冻结在只在销户时才做解冻处理 , 若需要解冻，全额冻结需要自行去解冻交易处理
		DpbFroze frozeInfo = DpFrozePublic.getFirstForzeInfo(cplIn.getFroze_no());

		// 状态冻结在销户时可以解冻，其他支取类型不能解冻
		if (frozeInfo.getFroze_type() != E_FROZETYPE.AMOUNT && cplIn.getWithdrawal_busi_type() != E_DRAWBUSIKIND.CLOSE) {

			// 非金额冻结不做解冻处理,所以冻结状态应为原冻结状态
			DpUnFrozeOut unfrozeOut = BizUtil.getInstance(DpUnFrozeOut.class);

			unfrozeOut.setFroze_status(frozeInfo.getFroze_status());
			unfrozeOut.setFroze_bal(frozeInfo.getFroze_bal());
			unfrozeOut.setFroze_due_date(frozeInfo.getFroze_due_date());

			return unfrozeOut;
		}

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
		dpUnFrozeIn.setFroze_feature_code(cplIn.getFroze_feature_code());
		dpUnFrozeIn.setSub_acct_seq(subAccount.getSub_acct_seq());
		dpUnFrozeIn.setCust_no(subAccount.getCust_no());

		// 同一客户下解冻，调方法不调服务
		DpUnFrozeOut unFrozeOut = DpUnFroze.doMain(dpUnFrozeIn);

		bizlog.method(" DpDemandDraw.cancleFroze end <<<<<<<<<<<<<<<<");

		return unFrozeOut;
	}
}
