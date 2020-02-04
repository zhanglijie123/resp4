package cn.sunline.icore.dp.serv.account.open;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpTimeInterestApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRate;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRateDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBatchFee;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfSave;
import cn.sunline.icore.dp.base.type.ComDpInterestTrxnBasic.DpDepositScheduleTrialIn;
import cn.sunline.icore.dp.base.type.ComDpInterestTrxnBasic.DpDepositScheduleType;
import cn.sunline.icore.dp.base.type.ComDpInterestTrxnBasic.DpWithdrawlScheduleInfo;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.iobus.DpOtherIobus;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.query.DpWithdrawlSchedule;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaAideInfo;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaAideInfoDao;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaDepositPlan;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaDepositPlanDetail;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaDrawPlan;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpbBatchFee;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpbBatchFeeDao;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountOut;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpDrawAcctInfo;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_AMTPERTWAY;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpAddSubAccount {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAddSubAccountCheck.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月19日-下午1:16:29</li>
	 *         <li>功能说明：开子户处理服务</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 * @return cplOut 开子户服务输出接口
	 */
	public static DpAddSubAccountOut doMain(DpAddSubAccountIn cplIn) {

		bizlog.method(" DpAddSubAccount.addSubAccount begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 取账号信息: 带锁
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 属性刷新
		DpAttrRefresh.refreshAttrValue(acctInfo, cplIn.getCard_no(), E_YESORNO.YES);

		// 检查开子户合法性
		DpAddSubAccountCheck.checkMainMethod(cplIn, acctInfo);

		// 调用开子户主调方法
		DpaSubAccount subAcct = doMainMethod(cplIn, acctInfo);

		// 服务输出
		DpAddSubAccountOut cplOut = BizUtil.getInstance(DpAddSubAccountOut.class);

		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_type(acctInfo.getAcct_type());
		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
		cplOut.setAcct_name(subAcct.getSub_acct_name()); // 账户名称
		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setProd_id(subAcct.getProd_id()); // 产品编号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setBack_value_date(cplIn.getBack_value_date()); // 倒起息日
		cplOut.setTerm_code(subAcct.getTerm_code()); // 存期
		cplOut.setDue_date(subAcct.getDue_date()); // 到期日
		cplOut.setRenewal_method(subAcct.getRenewal_method()); // 续存方式
		cplOut.setRenew_save_term(cplIn.getRenew_save_term()); // 续存存期
		cplOut.setDept_type("");
		cplOut.setSpec_dept_type(subAcct.getSpec_dept_type());
		cplOut.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		cplOut.setStart_inst_date(subAcct.getStart_inst_date());

		if (subAcct.getInst_ind() == E_YESORNO.YES) {
			
			DpaInterestRate interestRate = DpaInterestRateDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, 1l, false);
			
			if (interestRate != null) {

				cplOut.setEfft_inrt(interestRate.getEfft_inrt()); // 账户执行利率
			}
		}

		// cplOut.setList_layer_inrt(null); //分层利率列表

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAddSubAccount.addSubAccount end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月19日-下午1:37:19</li>
	 *         <li>功能说明：登记子账户信息</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 * @return 子账户信息
	 */
	public static DpaSubAccount doMainMethod(DpAddSubAccountIn cplIn, DpaAccount acctInfo) {

		// 登记子账户台账信息
		DpaSubAccount subAccount = DpBaseServiceApi.regSubAcct(cplIn, acctInfo);

		// 存入计划登记
		regSavePlan(subAccount, cplIn);

		// 支取计划登记
		regDrawPlan(subAccount, cplIn);

		// 产品批量收费登记
		regBatchFee(subAccount);

		// 子账户管理凭证处理
		referVoch(acctInfo, subAccount, cplIn.getVoch_type(), cplIn.getVoch_no());

		// 登记子账户补充信息
		registerAideInfo(subAccount, cplIn);
		
		// 开户变动推送消息中心
		DpOtherIobus.sendMessageChange(subAccount);

		return subAccount;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月8日-下午3:41:55</li>
	 *         <li>功能说明：子账户关联凭证</li>
	 *         </p>
	 * @param acctInfo
	 *            账户信息
	 * @param subAcctInfo
	 *            子账户信息
	 * @param vochType
	 *            凭证类型
	 * @param vochNo
	 *            凭证号码
	 */
	private static void referVoch(DpaAccount acctInfo, DpaSubAccount subAcctInfo, String vochType, String vochNo) {

		if (acctInfo.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT && subAcctInfo.getCorrelation_voch_ind() == E_YESORNO.YES) {

			DpVoucherIobus.payOutVoucher(acctInfo, subAcctInfo, vochType, vochNo);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月20日-上午11:09:36</li>
	 *         <li>登记存入计划表</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplIn
	 *            开子户输入
	 */
	private static void regSavePlan(DpaSubAccount subAcct, DpAddSubAccountIn cplIn) {

		bizlog.method(" DpAddSubAccount.regSavePlan begin >>>>>>>>>>>>>>>>");

		// 存入周期为空则说明无存入计划, 退出即可
		if (CommUtil.isNull(subAcct.getScheduled_dept_cycle())) {
			return;
		}

		DpfSave prodSaveInfo = DpProductFactoryApi.getProdSaveCtrl(cplIn.getProd_id(), cplIn.getCcy_code());

		// 约定存入间隔周期
		String schedCycle = CommUtil.nvl(cplIn.getScheduled_dept_cycle(), prodSaveInfo.getScheduled_dept_cycle());

		// 目标金额
		BigDecimal targetAmt = cplIn.getTarget_amt();

		List<DpaDepositPlan> listSavePlan = new ArrayList<DpaDepositPlan>();

		// 目标存款存入计划生成
		DpDepositScheduleTrialIn cplScheduleIn = BizUtil.getInstance(DpDepositScheduleTrialIn.class);

		cplScheduleIn.setAcct_no(cplIn.getAcct_no());
		cplScheduleIn.setAcct_type(cplIn.getAcct_type());
		cplScheduleIn.setCcy_code(cplIn.getCcy_code());
		cplScheduleIn.setProd_id(cplIn.getProd_id());
		cplScheduleIn.setScheduled_dept_cycle(schedCycle);
		cplScheduleIn.setTarget_amt(targetAmt);
		cplScheduleIn.setTerm_code(cplIn.getTerm_code());
		cplScheduleIn.setLevy_ind(E_YESORNO.YES);
		cplScheduleIn.setOpen_acct_save_ind(CommUtil.compare(cplIn.getTrxn_amt(), BigDecimal.ZERO) > 0 ? E_YESORNO.YES : E_YESORNO.NO);
		cplScheduleIn.setStart_inst_date(cplIn.getBack_value_date());

		List<DpDepositScheduleType> listSchedule = DpTimeInterestApi.generateDepositSchedule(cplScheduleIn, subAcct.getTax_rate_code());

		for (DpDepositScheduleType cplSchedule : listSchedule) {

			DpaDepositPlan cplSavePlan = BizUtil.getInstance(DpaDepositPlan.class);

			cplSavePlan.setAcct_no(subAcct.getAcct_no());
			cplSavePlan.setSub_acct_no(subAcct.getSub_acct_no());
			cplSavePlan.setPeriod_no(cplSchedule.getPeriod_no());
			cplSavePlan.setPlan_dept_date(cplSchedule.getPlan_dept_date());
			cplSavePlan.setPeriod_prcp(cplSchedule.getPeriod_prcp());
			cplSavePlan.setMaturity_profit(cplSchedule.getMaturity_profit());
			cplSavePlan.setEfft_inrt(cplSchedule.getEfft_inrt());
			cplSavePlan.setExec_ind(E_YESORNO.NO);

			listSavePlan.add(cplSavePlan);
		}

		// 插入账户存入计划
		DaoUtil.insertBatch(DpaDepositPlan.class, listSavePlan);

		// 处理存入计划明细
		if (cplIn.getList_draw_account().size() > 1) {
			BizUtil.listSort(cplIn.getList_draw_account(), true, SysDict.A.serial_no.getId());
		}

		List<DpaDepositPlanDetail> listPlanDetail = new ArrayList<DpaDepositPlanDetail>();

		long sortNo = 1;
		for (DpDrawAcctInfo cplDrawAcct : cplIn.getList_draw_account()) {

			DpaDepositPlanDetail cplPlanDetail = BizUtil.getInstance(DpaDepositPlanDetail.class);

			cplPlanDetail.setAcct_no(subAcct.getAcct_no());
			cplPlanDetail.setSub_acct_no(subAcct.getSub_acct_no());
			cplPlanDetail.setSerial_no(sortNo++);
			cplPlanDetail.setDraw_acct(cplDrawAcct.getDraw_acct());
			cplPlanDetail.setDraw_acct_ccy(cplDrawAcct.getDraw_acct_ccy());
			cplPlanDetail.setAmt_apportion_method(CommUtil.nvl(cplDrawAcct.getAmt_apportion_method(), E_AMTPERTWAY.NO));
			cplPlanDetail.setAmount_ratio(cplPlanDetail.getAmt_apportion_method() == E_AMTPERTWAY.NO ? BigDecimal.ZERO : cplDrawAcct.getAmount_ratio());

			listPlanDetail.add(cplPlanDetail);
		}

		// 插入账户存入计划明细
		DaoUtil.insertBatch(DpaDepositPlanDetail.class, listPlanDetail);

		bizlog.method(" DpAddSubAccount.regSavePlan end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月20日-上午11:09:36</li>
	 *         <li>登记支取计划表</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplIn
	 *            开子户输入
	 */
	private static void regDrawPlan(DpaSubAccount subAcct, DpAddSubAccountIn cplIn) {

		bizlog.method(" DpAddSubAccount.regDrawPlan begin >>>>>>>>>>>>>>>>");

		// 不具有支取计划的退出
		if (CommUtil.isNull(subAcct.getScheduled_withdrawal_cycle())) {
			return;
		}

		List<DpaDrawPlan> listDrawPlan = new ArrayList<DpaDrawPlan>();

		List<DpWithdrawlScheduleInfo> listWithdrawSchedule = DpWithdrawlSchedule.generateWithdrawlPlan(subAcct, cplIn.getTrxn_amt());

		for (DpWithdrawlScheduleInfo timePlan : listWithdrawSchedule) {

			DpaDrawPlan drawPlan = BizUtil.getInstance(DpaDrawPlan.class);

			drawPlan.setAcct_no(subAcct.getAcct_no());
			drawPlan.setSub_acct_no(subAcct.getSub_acct_no());
			drawPlan.setPeriod_no(timePlan.getPeriod_no()); // period no
			drawPlan.setPlan_withdrawl_date(timePlan.getPlan_withdrawl_date());
			drawPlan.setWithdrawl_amt(timePlan.getWithdrawl_amt());
			drawPlan.setExec_ind(E_YESORNO.NO); // Execute indicator

			listDrawPlan.add(drawPlan);
		}

		// 插入账户支取计划
		DaoUtil.insertBatch(DpaDrawPlan.class, listDrawPlan);

		bizlog.method(" DpAddSubAccount.regDrawPlan end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月21日-下午4:33:43</li>
	 *         <li>功能说明：登记子账户批量收费信息</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 * @param subAcctInfo
	 *            子账户信息
	 */
	public static void regBatchFee(DpaSubAccount subAcctInfo) {

		bizlog.method(" DpAddSubAccount.regBatchFee begin >>>>>>>>>>>>>>>>");

		List<DpfBatchFee> listBatchFee = DpProductFactoryApi.getProdBatchFee(subAcctInfo.getProd_id(), subAcctInfo.getCcy_code());

		for (DpfBatchFee batchFee : listBatchFee) {

			regSingleBatchFee(subAcctInfo, batchFee);
		}

		bizlog.method(" DpAddSubAccount.regBatchFee end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月21日-下午4:33:43</li>
	 *         <li>功能说明：登记子账户单条批量收费信息</li>
	 *         </p>
	 * @param batchFee
	 *            单条收费信息
	 * @param subAcctInfo
	 *            子账户信息
	 */
	public static void regSingleBatchFee(DpaSubAccount subAcctInfo, DpfBatchFee batchFee) {

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		DpbBatchFee acctFee = BizUtil.getInstance(DpbBatchFee.class);

		acctFee.setSub_acct_no(subAcctInfo.getSub_acct_no());
		acctFee.setAcct_no(subAcctInfo.getAcct_no());
		acctFee.setChrg_code(batchFee.getChrg_code());
		acctFee.setChrg_cycle(batchFee.getChrg_cycle());
		acctFee.setFee_ref_date(batchFee.getFee_ref_date());
		acctFee.setLast_fee_date(trxnDate);
		acctFee.setBusi_cond(batchFee.getBusi_cond());
		acctFee.setEffect_date(batchFee.getEffect_date());
		acctFee.setExpiry_date(batchFee.getExpiry_date());
		acctFee.setPrior_level(batchFee.getPrior_level());

		String nextFeeDate = "";

		if (CommUtil.equals(batchFee.getFee_ref_date(), DpConst.CASE_DATE)) {

			nextFeeDate = BizUtil.calcDateByReference(subAcctInfo.getOpen_acct_date(), trxnDate, batchFee.getChrg_cycle());
		}
		else {
			nextFeeDate = BizUtil.calcDateByReference(batchFee.getFee_ref_date(), trxnDate, batchFee.getChrg_cycle());
		}

		acctFee.setNext_fee_date(nextFeeDate);

		DpbBatchFeeDao.insert(acctFee);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月21日-下午4:33:43</li>
	 *         <li>功能说明：登记子账户辅助信息</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplIn
	 *            子账户开户信息
	 */
	private static void registerAideInfo(DpaSubAccount subAcct, DpAddSubAccountIn cplIn) {

		bizlog.method(" DpAddSubAccount.registerAideInfo begin ");

		// 存款资金来源不为空则检查合法性
		if (CommUtil.isNotNull(cplIn.getFund_source())) {

			ApDropListApi.exists(DpConst.FUND_SOURCE, cplIn.getFund_source());
		}
		// 开户目的不为空则检查合法性
		if (CommUtil.isNotNull(cplIn.getFund_use_way())) {

			ApDropListApi.exists(DpConst.FUND_USE_WAY, cplIn.getFund_use_way());
		}

		DpaAideInfo aideInfo = BizUtil.getInstance(DpaAideInfo.class);

		aideInfo.setSub_acct_no(subAcct.getSub_acct_no());
		aideInfo.setAcct_no(subAcct.getAcct_no());
		aideInfo.setText_short_remark_1(cplIn.getFund_source());
		aideInfo.setText_short_remark_2(cplIn.getFund_use_way());
		aideInfo.setText_short_remark_3(cplIn.getIncome_source());

		DpaAideInfoDao.insert(aideInfo);

		bizlog.method(" DpAddSubAccount.registerAideInfo end ");
	}
}
