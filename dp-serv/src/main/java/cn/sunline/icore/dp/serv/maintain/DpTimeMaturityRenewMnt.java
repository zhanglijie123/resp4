package cn.sunline.icore.dp.serv.maintain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApReversalApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpTimeInterestApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRate;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRateDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAdjustIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTADJUSTTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RENEWSAVEWAY;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dayend.DpDayEndInterest;
import cn.sunline.icore.dp.serv.dayend.DpTdRenewSave;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.settle.DpSettleVoucherTrxn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpRenewSaveTallyAideInfo;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpTdAfterDueRenewMntIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpTdAfterDueRenewMntOut;
import cn.sunline.icore.dp.serv.type.ComDpReversal.DpAfterMatureHandRenewReversalIn;
import cn.sunline.icore.dp.serv.type.ComDpReversal.DpTdMatureHandRenewInterest;
import cn.sunline.icore.dp.serv.type.ComDpSettleVoucher.DpSettleVochCancel;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_ADDSUBTRACT;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpTimeMaturityRenewMnt {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeMaturityRenewMnt.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月12日-下午4:52:23</li>
	 *         <li>功能说明：定期到期后手工转存</li>
	 *         </p>
	 * @param cplIn
	 *            定期到期后手工转存输入
	 * @return 定期到期后手工转存 输出
	 */
	public static DpTdAfterDueRenewMntOut timeMaturityAfterRenewInstructMnt(DpTdAfterDueRenewMntIn cplIn) {
		bizlog.method(" DpTimeMaturityRenewMnt.timeMaturityAfterRenewInstructMnt begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 1.1 检查输入信息
		DpaSubAccount subAcctInfo = checkInput(cplIn);

		// 当天若还未计提过的先做计提处理
		if (CommUtil.compare(subAcctInfo.getNext_inst_date(), trxnDate) <= 0) {

			DpDayEndInterest.onlineDealInterest(subAcctInfo);

			subAcctInfo = DpaSubAccountDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), true);
		}

		DpaSubAccount oldSubAcct = BizUtil.clone(DpaSubAccount.class, subAcctInfo);

		// 获取原账户利率
		DpaInterestRate oldRate = DpaInterestRateDao.selectOne_odb1(oldSubAcct.getAcct_no(), oldSubAcct.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, DpConst.START_SORT_VALUE, false);

		// 1.2 登记定期到期手工续存冲账事件
		registeredReversal(oldSubAcct);

		// 1.3 加载数据缓存区
		addBuffData(subAcctInfo);

		// 1.4 定期子户收息账户、本金转账账户设置
		modifyAcctRenewInfo(cplIn, subAcctInfo);

		// 2.1 续存前结息
		renewBeforeInterestDeal(cplIn, subAcctInfo);

		// 2.2 结息里面有变更子户信息，再次查询子户信息
		subAcctInfo = DpaSubAccountDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), true);

		// 3. 获取续存金额
		BigDecimal renewSaveAmt = DpTdRenewSave.getRenewSaveAmount(subAcctInfo);

		// 4. 续存合法性检查
		DpTdRenewSave.checkRenewVaild(subAcctInfo, renewSaveAmt, cplIn.getRenew_prod_id(), cplIn.getRenew_save_term());

		// 5. 账务处理
		DpRenewSaveTallyAideInfo cplAideInfo = BizUtil.getInstance(DpRenewSaveTallyAideInfo.class);

		cplAideInfo.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		cplAideInfo.setSummary_code(cplIn.getSummary_code());
		cplAideInfo.setCustomer_remark(cplIn.getCustomer_remark());
		cplAideInfo.setExch_rate(cplIn.getExch_rate());
		cplAideInfo.setExch_rate_path(cplIn.getExch_rate_path());
		cplAideInfo.setForex_agree_price_id(cplIn.getForex_agree_price_id());
		cplAideInfo.setOpp_acct_ccy(CommUtil.nvl(cplIn.getOpp_ccy_code(), subAcctInfo.getCcy_code()));
		cplAideInfo.setTrxn_remark(cplIn.getTrxn_remark());

		DpTdRenewSave.principalPosting(subAcctInfo, cplAideInfo);

		// 6.续存信息重置
		DpTdRenewSave.renewSaveReset(subAcctInfo, CommUtil.nvl(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date()));

		// 7. 新的周期正常利息倒起息
		if (CommUtil.isNotNull(cplIn.getBack_value_date()) && CommUtil.compare(cplIn.getBack_value_date(), trxnDate) < 0) {

			DpInstAdjustIn cplAdjustIn = BizUtil.getInstance(DpInstAdjustIn.class);

			cplAdjustIn.setEnd_inst_date(trxnDate);
			cplAdjustIn.setInit_inst_start_date(subAcctInfo.getStart_inst_date());
			cplAdjustIn.setInst_adjust_aspect(E_ADDSUBTRACT.ADD); // 调增
			cplAdjustIn.setInst_adjust_type(E_INSTADJUSTTYPE.BACKVALUE);
			cplAdjustIn.setStart_inst_date(cplIn.getBack_value_date());
			cplAdjustIn.setTrxn_amt(subAcctInfo.getAcct_bal());

			DpInterestBasicApi.adjustInstForBackvalue(subAcctInfo, cplAdjustIn, E_YESORNO.NO);
		}

		// 8.登记转续存登记簿
		DpTdRenewSave.resetRolloverBook(oldSubAcct, BizUtil.getTrxRunEnvs().getTrxn_date(), renewSaveAmt, cplIn.getRenewal_method(), oldRate.getEfft_inrt());

		// 9.获取输出信息
		DpTdAfterDueRenewMntOut cplOut = getOutPutInfo(cplIn, subAcctInfo);

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpTimeMaturityRenewMnt.timeMaturityAfterRenewInstructMnt end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月14日-上午9:36:57</li>
	 *         <li>功能说明：检查输入接口信息</li>
	 *         </p>
	 * @param cplIn
	 *            定期到期后手工转存输入
	 * @return 子账户信息
	 */
	private static DpaSubAccount checkInput(DpTdAfterDueRenewMntIn cplIn) {

		bizlog.method(" DpTimeMaturityRenewMnt.checkInput begin >>>>>>>>>>>>>>>>");

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 1.非空项校验
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getRenewal_method(), DpBaseDict.A.renewal_method.getId(), DpBaseDict.A.renewal_method.getLongName());
		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

		// 2. 续存方式不合法
		if (CommUtil.in(cplIn.getRenewal_method(), E_RENEWSAVEWAY.NONE, E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT)) {
			// TODO:
			throw APPUB.E0026(cplIn.getRenewal_method().getLongName(), cplIn.getRenewal_method().getValue());
		}

		// 2.1 现转标志和对手方账号
		if (CommUtil.in(cplIn.getRenewal_method(), E_RENEWSAVEWAY.PRINCIPAL, E_RENEWSAVEWAY.PART_AMOUNT, E_RENEWSAVEWAY.ADD_AMOUNT)) {

			BizUtil.fieldNotNull(cplIn.getCash_trxn_ind(), SysDict.A.cash_trxn_ind.getId(), SysDict.A.cash_trxn_ind.getLongName());

			if (cplIn.getCash_trxn_ind() == E_CASHTRXN.TRXN) {

				BizUtil.fieldNotNull(cplIn.getOpp_acct_no(), SysDict.A.opp_acct_no.getId(), SysDict.A.opp_acct_no.getLongName());
			}

			// 加本金或减本金续存时本金调节字段必输
			if (CommUtil.in(cplIn.getRenewal_method(), E_RENEWSAVEWAY.PART_AMOUNT, E_RENEWSAVEWAY.ADD_AMOUNT)) {

				BizUtil.fieldNotNull(cplIn.getPrin_adjust_amt(), DpBaseDict.A.prin_adjust_amt.getId(), DpBaseDict.A.prin_adjust_amt.getLongName());
			}
		}

		// 定位定期子户
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no());// 账号
		accessIn.setAcct_type(cplIn.getAcct_type());// 账号类型
		accessIn.setCcy_code(cplIn.getCcy_code());// 货币代号
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq());// 子账户序号
		accessIn.setDd_td_ind(E_DEMANDORTIME.TIME);// 定活标志

		// 定位子账户
		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 防并发、带锁查询
		DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		// 账户信息
		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(accessOut.getAcct_no(), true);

		// 账户状态
		if (subAcctInfo.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_seq());
		}

		// 验证密码 ：验密标志 = Y 时，则需要校验账户的支取方式
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 未到期账户不允许手工到期续存
		if (CommUtil.compare(BizUtil.getTrxRunEnvs().getTrxn_date(), subAcctInfo.getDue_date()) < 0) {

			throw DpErr.Dp.E0330(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_seq());
		}

		// 倒起息日不为空
		if (CommUtil.isNotNull(cplIn.getBack_value_date())) {

			BizUtil.dateBetween(cplIn.getBack_value_date(), subAcctInfo.getDue_date(), true, trxnDate, true);
		}

		bizlog.method(" DpTimeMaturityRenewMnt.checkInput end <<<<<<<<<<<<<<<<");

		return subAcctInfo;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年12月29日-下午2:19:58</li>
	 *         <li>功能说明：登记定期到期手工续存冲账事件</li>
	 *         </p>
	 * @param subAcctInfo
	 */
	private static void registeredReversal(DpaSubAccount subAcctInfo) {

		// 提炼计息相关信息
		List<DpTdMatureHandRenewInterest> listHandRenewInst = new ArrayList<DpTdMatureHandRenewInterest>();
		List<E_INSTKEYTYPE> listInstKeyType = new ArrayList<E_INSTKEYTYPE>();

		listInstKeyType.add(E_INSTKEYTYPE.NORMAL);

		if (CommUtil.isNotNull(subAcctInfo.getRemnant_day_start_date())) {
			listInstKeyType.add(E_INSTKEYTYPE.REMNANT);
		}

		listInstKeyType.add(E_INSTKEYTYPE.MATURE);

		for (E_INSTKEYTYPE instKeyType : listInstKeyType) {

			DpaInterest acctInst = DpaInterestDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), instKeyType, true);

			DpaInterestRate acctRate = DpaInterestRateDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), instKeyType, DpConst.START_SORT_VALUE, true);

			DpTdMatureHandRenewInterest cplHandRenewInst = BizUtil.getInstance(DpTdMatureHandRenewInterest.class);

			cplHandRenewInst.setAccrual_inst(acctInst.getAccrual_inst());
			cplHandRenewInst.setAccrual_inst_tax(acctInst.getAccrual_inst_tax());
			cplHandRenewInst.setAccrual_sum_bal(acctInst.getAccrual_sum_bal());
			cplHandRenewInst.setBank_base_inrt(acctRate.getBank_base_inrt());
			cplHandRenewInst.setCur_term_inst(acctInst.getCur_term_inst());
			cplHandRenewInst.setCur_term_inst_sum_bal(acctInst.getCur_term_inst_sum_bal());
			cplHandRenewInst.setCur_term_inst_tax(acctInst.getCur_term_inst_tax());
			cplHandRenewInst.setCur_year_sum_bal(acctInst.getCur_year_sum_bal());
			cplHandRenewInst.setEfft_inrt(acctRate.getEfft_inrt());
			cplHandRenewInst.setEnd_inst_date(acctInst.getEnd_inst_date());
			cplHandRenewInst.setInrt_code(acctInst.getInrt_code());
			cplHandRenewInst.setInrt_float_method(acctRate.getInrt_float_method());
			cplHandRenewInst.setInrt_float_value(acctRate.getInrt_float_value());
			cplHandRenewInst.setInst_base(acctInst.getInst_base());
			cplHandRenewInst.setInst_day(acctInst.getInst_day());
			cplHandRenewInst.setInst_key_type(acctInst.getInst_key_type());
			cplHandRenewInst.setLast_inrt_renew_date(acctInst.getLast_inrt_renew_date());
			cplHandRenewInst.setLast_pay_inst_date(acctInst.getLast_pay_inst_date());
			cplHandRenewInst.setLast_year_sum_bal(acctInst.getLast_year_sum_bal());
			cplHandRenewInst.setLatest_inrt_plan_date(acctInst.getLatest_inrt_plan_date());
			cplHandRenewInst.setNext_inrt_renew_date(acctInst.getNext_inrt_renew_date());
			cplHandRenewInst.setNext_pay_inst_date(acctInst.getNext_pay_inst_date());
			cplHandRenewInst.setPay_inst_cyc(acctInst.getPay_inst_cyc());
			cplHandRenewInst.setPay_inst_method(acctInst.getPay_inst_method());
			cplHandRenewInst.setSet_inrt_plan_ind(acctInst.getSet_inrt_plan_ind());
			cplHandRenewInst.setStart_inst_date(acctInst.getStart_inst_date());
			cplHandRenewInst.setWait_deal_inst(acctInst.getWait_deal_inst());
			cplHandRenewInst.setWait_deal_sum_bal(acctInst.getWait_deal_sum_bal());

			listHandRenewInst.add(cplHandRenewInst);
		}

		// 登记定期到期手工续存冲账事件
		DpAfterMatureHandRenewReversalIn reversalIn = BizUtil.getInstance(DpAfterMatureHandRenewReversalIn.class);

		reversalIn.setSub_acct_no(subAcctInfo.getSub_acct_no());
		reversalIn.setAcct_no(subAcctInfo.getAcct_no());
		reversalIn.setProd_id(subAcctInfo.getProd_id());
		reversalIn.setStart_inst_date(subAcctInfo.getStart_inst_date());
		reversalIn.setTerm_code(subAcctInfo.getTerm_code());
		reversalIn.setDue_date(subAcctInfo.getDue_date());
		reversalIn.setRoll_no(subAcctInfo.getRoll_no());
		reversalIn.setRemnant_day_start_date(subAcctInfo.getRemnant_day_start_date());
		reversalIn.setRenewal_method(subAcctInfo.getRenewal_method());
		reversalIn.setRenew_save_term(subAcctInfo.getRenew_save_term());
		reversalIn.setRenew_save_amt(subAcctInfo.getRenew_save_amt());
		reversalIn.setRenew_prod_id(subAcctInfo.getRenew_prod_id());
		reversalIn.setScheduled_dept_cycle(subAcctInfo.getScheduled_dept_cycle());
		reversalIn.setAccm_dept_amt(subAcctInfo.getAccm_dept_amt());
		reversalIn.setAccm_dept_count(subAcctInfo.getAccm_dept_count());
		reversalIn.setTarget_amt_type(subAcctInfo.getTarget_amt_type());
		reversalIn.setTarget_amt(subAcctInfo.getTarget_amt());
		reversalIn.setAccm_withdrawal_amt(subAcctInfo.getAccm_withdrawal_amt());
		reversalIn.setAccm_withdrawal_count(subAcctInfo.getAccm_withdrawal_count());
		reversalIn.setScheduled_withdrawal_cycle(subAcctInfo.getScheduled_withdrawal_cycle());
		reversalIn.setIncome_inst_acct(subAcctInfo.getIncome_inst_acct());
		reversalIn.setIncome_inst_ccy(subAcctInfo.getIncome_inst_ccy());
		reversalIn.setPrin_trsf_acct(subAcctInfo.getPrin_trsf_acct());
		reversalIn.setPrin_trsf_acct_ccy(subAcctInfo.getPrin_trsf_acct_ccy());
		reversalIn.setAcct_inst_info(SysUtil.serialize(listHandRenewInst));
		reversalIn.setOriginal_trxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		reversalIn.setOriginal_trxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

		ApReversalApi.register("deptAfterMatureHandRenew", reversalIn);
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月14日-上午9:36:57</li>
	 *         <li>功能说明：设置账户层续存信息</li>
	 *         </p>
	 * @param cplIn
	 *            定期到期后手工转存输入
	 * @param subAcctInfo
	 *            子账户信息
	 */
	private static void modifyAcctRenewInfo(DpTdAfterDueRenewMntIn cplIn, DpaSubAccount subAcctInfo) {

		bizlog.method(" DpTimeMaturityRenewMnt.modifyAcctRenewInfo begin >>>>>>>>>>>>>>>>");

		DpaSubAccount oldSubAcctInfo = BizUtil.clone(DpaSubAccount.class, subAcctInfo);

		// 更新续存方式
		subAcctInfo.setRenewal_method(cplIn.getRenewal_method());
		subAcctInfo.setRenew_prod_id(CommUtil.nvl(cplIn.getRenew_prod_id(), subAcctInfo.getProd_id()));
		subAcctInfo.setRenew_save_term(CommUtil.nvl(cplIn.getRenew_save_term(), subAcctInfo.getTerm_code()));

		// 登记本金及收息账户
		if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.PRIN_INST || cplIn.getCash_trxn_ind() != E_CASHTRXN.TRXN) {

			int i = ApDataAuditApi.regLogOnUpdateBusiness(oldSubAcctInfo, subAcctInfo);
			if (i > 0) {

				DpaSubAccountDao.updateOne_odb1(subAcctInfo);
			}
			return;
		}

		// 对手方账户路由分析
		E_ACCOUTANALY acctRouteType = DpInsideAccountIobus.getAccountRouteType(cplIn.getOpp_acct_no());

		if (CommUtil.in(acctRouteType, E_ACCOUTANALY.DEPOSIT, E_ACCOUTANALY.NOSTRO)) {

			// 定位对手方子户
			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(cplIn.getOpp_acct_no());// 账号
			accessIn.setAcct_type(cplIn.getOpp_acct_type());// 账号类型
			accessIn.setCcy_code(CommUtil.nvl(cplIn.getOpp_ccy_code(), subAcctInfo.getCcy_code()));// 货币代号
			accessIn.setProd_id(cplIn.getOpp_prod_id());
			accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);// 定活标志

			// 定位子账户
			DpAcctAccessOut accessOppOut = DpToolsApi.locateSingleSubAcct(accessIn);

			if (accessOppOut.getAcct_status() == E_ACCTSTATUS.CLOSE) {

				throw DpBase.E0017(accessOppOut.getAcct_no(), accessOppOut.getSub_acct_seq());
			}

			String acctNo = CommUtil.equals(cplIn.getOpp_acct_no(), accessOppOut.getAcct_no()) ? accessOppOut.getAcct_no() : cplIn.getOpp_acct_no();

			if (CommUtil.in(cplIn.getRenewal_method(), E_RENEWSAVEWAY.PART_AMOUNT, E_RENEWSAVEWAY.ADD_AMOUNT, E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT)) {

				subAcctInfo.setRenew_save_amt(cplIn.getPrin_adjust_amt());
				subAcctInfo.setPrin_trsf_acct(acctNo);
				subAcctInfo.setPrin_trsf_acct_ccy(accessOppOut.getCcy_code());
				subAcctInfo.setIncome_inst_acct(acctNo);
				subAcctInfo.setIncome_inst_ccy(accessOppOut.getCcy_code());
			}
			else {

				subAcctInfo.setRenew_save_amt(null);
				subAcctInfo.setPrin_trsf_acct(null);
				subAcctInfo.setPrin_trsf_acct_ccy(null);
				subAcctInfo.setIncome_inst_acct(acctNo);
				subAcctInfo.setIncome_inst_ccy(accessOppOut.getCcy_code());
			}

			// 核销结算凭证
			if (CommUtil.isNotNull(cplIn.getSettle_voch_type()) && CommUtil.isNotNull(cplIn.getCheque_no())) {

				DpSettleVochCancel sttleIn = BizUtil.getInstance(DpSettleVochCancel.class);

				sttleIn.setAcct_no(accessOppOut.getAcct_no());
				sttleIn.setCcy_code(accessOppOut.getCcy_code());
				sttleIn.setSettle_voch_type(cplIn.getSettle_voch_type());
				sttleIn.setCheque_no(cplIn.getCheque_no());
				sttleIn.setFroze_no(null);
				sttleIn.setTrxn_amt(null);

				DpSettleVoucherTrxn.settleVochVerifyCancellation(sttleIn);
			}
		}
		else if (acctRouteType == E_ACCOUTANALY.INSIDE || acctRouteType == E_ACCOUTANALY.BUSINESE) {

			if (CommUtil.in(cplIn.getRenewal_method(), E_RENEWSAVEWAY.PART_AMOUNT, E_RENEWSAVEWAY.ADD_AMOUNT)) {

				subAcctInfo.setPrin_trsf_acct(cplIn.getOpp_acct_no());
				subAcctInfo.setPrin_trsf_acct_ccy(CommUtil.nvl(cplIn.getOpp_ccy_code(), subAcctInfo.getCcy_code()));
			}
			else {
				subAcctInfo.setIncome_inst_acct(cplIn.getOpp_acct_no());
				subAcctInfo.setIncome_inst_ccy(CommUtil.nvl(cplIn.getOpp_ccy_code(), subAcctInfo.getCcy_code()));
			}
		}
		int n = ApDataAuditApi.regLogOnUpdateBusiness(oldSubAcctInfo, subAcctInfo);
		if (n > 0) {

			DpaSubAccountDao.updateOne_odb1(subAcctInfo);
		}

		bizlog.method(" DpTimeMaturityRenewMnt.modifyAcctRenewInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月14日-下午1:13:54</li>
	 *         <li>功能说明：续存前利息处理</li>
	 *         </p>
	 * @param cplIn
	 *            定期到期后手工转存输入
	 * @param subAcctInfo
	 *            子账户信息
	 */
	private static void renewBeforeInterestDeal(DpTdAfterDueRenewMntIn cplIn, DpaSubAccount subAcctInfo) {

		bizlog.method(" DpTimeMaturityRenewMnt.renewBeforeInterestDeal begin >>>>>>>>>>>>>>>>");

		// 获取交易日期
		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 原逾期利息倒起息
		if (CommUtil.isNotNull(cplIn.getBack_value_date()) && CommUtil.compare(cplIn.getBack_value_date(), trxnDate) < 0) {

			DpInstAdjustIn cplAdjustIn = BizUtil.getInstance(DpInstAdjustIn.class);

			cplAdjustIn.setEnd_inst_date(trxnDate);
			cplAdjustIn.setInit_inst_start_date(subAcctInfo.getStart_inst_date());
			cplAdjustIn.setInst_adjust_aspect(E_ADDSUBTRACT.SUBTRACT);
			cplAdjustIn.setInst_adjust_type(E_INSTADJUSTTYPE.BACKVALUE);
			cplAdjustIn.setStart_inst_date(cplIn.getBack_value_date());
			cplAdjustIn.setTrxn_amt(subAcctInfo.getAcct_bal());

			DpInterestBasicApi.adjustInstForBackvalue(subAcctInfo, cplAdjustIn, E_YESORNO.NO);
		}

		// 将利息明细转入待支取利息
		DpTimeInterestApi.timeDirectPayInterest(subAcctInfo, BizUtil.getTrxRunEnvs().getTrxn_date(), E_YESORNO.YES);

		bizlog.method(" DpTimeMaturityRenewMnt.renewBeforeInterestDeal end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月14日-下午1:13:54</li>
	 *         <li>功能说明：输出信息处理</li>
	 *         </p>
	 * @param cplIn
	 *            定期到期后手工转存输入
	 * @param subAcctInfo
	 *            子账户信息
	 */
	private static DpTdAfterDueRenewMntOut getOutPutInfo(DpTdAfterDueRenewMntIn cplIn, DpaSubAccount subAcctInfo) {

		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAcctInfo.getAcct_no(), true);

		// 输出接口
		DpTdAfterDueRenewMntOut cplOut = BizUtil.getInstance(DpTdAfterDueRenewMntOut.class);

		cplOut.setAcct_bal(subAcctInfo.getAcct_bal());
		cplOut.setAcct_name(acctInfo.getAcct_name());
		cplOut.setAcct_no(cplIn.getAcct_no());
		cplOut.setCcy_code(subAcctInfo.getCcy_code());
		cplOut.setDue_date(subAcctInfo.getDue_date());
		cplOut.setExch_rate(cplIn.getExch_rate());
		cplOut.setIncome_inst_acct(subAcctInfo.getIncome_inst_acct());
		cplOut.setIncome_inst_acct_name(null);
		cplOut.setIncome_inst_ccy(subAcctInfo.getIncome_inst_ccy());
		cplOut.setPrin_adjust_amt(cplIn.getPrin_adjust_amt());
		cplOut.setPrin_trsf_acct(subAcctInfo.getPrin_trsf_acct());
		cplOut.setPrin_trsf_acct_ccy(subAcctInfo.getPrin_trsf_acct_ccy());
		cplOut.setPrin_trsf_acct_name(null);
		cplOut.setProd_name(null);
		cplOut.setProd_id(subAcctInfo.getProd_id());
		cplOut.setRenewal_method(subAcctInfo.getRenewal_method());
		cplOut.setStart_inst_date(subAcctInfo.getStart_inst_date());
		cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplOut.setTerm_code(subAcctInfo.getTerm_code());
		cplOut.setRenew_save_amt(subAcctInfo.getRenew_save_amt());

		// 账户利率
		DpaInterestRate acctRate = DpaInterestRateDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, DpConst.START_SORT_VALUE, true);

		cplOut.setEfft_inrt(acctRate.getEfft_inrt());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：加载数据缓存区</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void addBuffData(DpaSubAccount subAcct) {

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 查询账户信息
		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), true);

		// 加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(subAcct.getCust_no(), subAcct.getCust_type());
	}

}
