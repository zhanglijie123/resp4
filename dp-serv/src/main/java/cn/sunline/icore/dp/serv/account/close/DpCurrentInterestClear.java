package cn.sunline.icore.dp.serv.account.close;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFrozeDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlipDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraft;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlipDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInterestResult;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.account.draw.DpDemandDraw;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dayend.DpDayEndInterest;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.fundpool.DpOverdraftSettlement;
import cn.sunline.icore.dp.serv.instruct.DpSmartDepositAmt;
import cn.sunline.icore.dp.serv.interest.DpInterestAccounting;
import cn.sunline.icore.dp.serv.interest.DpInterestSettlement;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDeposit;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDepositDao;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpAcctClearDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpAcctInstClearIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpAcctInstClearOut;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpInstClearFrozeCancleIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpInstClearIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpInstClearInfo;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpSubAcctInstClearIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpSubAcctInstClearOut;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ACCTBUSITYPE;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZEOBJECT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZESTATUS;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpCurrentInterestClear {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCurrentInterestClear.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月25日-下午4:21:13</li>
	 *         <li>功能说明：活期子户销户前利息结清</li>
	 *         </p>
	 * @param cplIn
	 *            活期子户利息结清输入
	 * @return 活期子户利息结清输出
	 */
	public static DpSubAcctInstClearOut subAccountClear(DpSubAcctInstClearIn cplIn) {

		bizlog.method(" DpCurrentInterestClear.subAccountClear begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 定位子账号信息
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no()); // 账号
		accessIn.setAcct_type(cplIn.getAcct_type());
		accessIn.setCcy_code(cplIn.getCcy_code()); // 货币代码
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq());// 子账户序号
		accessIn.setProd_id(cplIn.getProd_id()); // 产品编号
		accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND); // 定活标志

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 查询存款子账户表
		DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOneWithLock_odb1(cplIn.getAcct_no(), accessOut.getSub_acct_no(), true);

		// 子账户结清检查
		subAccountClearCheck(subAcctInfo);

		// 解冻处理赋值
		DpInstClearFrozeCancleIn cplCancelIn = BizUtil.getInstance(DpInstClearFrozeCancleIn.class);

		cplCancelIn.setAcct_no(subAcctInfo.getAcct_no());
		cplCancelIn.setCust_no(subAcctInfo.getCust_no());
		cplCancelIn.setFroze_feature_code(cplIn.getFroze_feature_code());
		cplCancelIn.setFroze_no(cplIn.getFroze_no());
		cplCancelIn.setSub_acct_seq(subAcctInfo.getSub_acct_seq());
		cplCancelIn.setUnfroze_reason(cplIn.getUnfroze_reason());

		// 解冻处理: 含冻结状况检查
		cancleFrozeProcess(cplCancelIn);

		// 协议处理
		agreeProcess(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no());

		// 交易前余额
		BigDecimal tallyBeforeBalance = subAcctInfo.getAcct_bal();

		// 存款利息结清
		DpInstAccounting depositInstInfo = depositInterestClear(cplIn, subAcctInfo);

		// 透支利息结清
		BigDecimal overDraftInst = overDraftInterestClear(cplIn, subAcctInfo);

		// 利息结清后账户余额仍小于零，不能销户
		if (CommUtil.compare(subAcctInfo.getAcct_bal(), BigDecimal.ZERO) < 0) {
			throw DpBase.E0118(cplIn.getAcct_no(), subAcctInfo.getSub_acct_seq());
		}

		// 输出
		DpSubAcctInstClearOut cplOut = BizUtil.getInstance(DpSubAcctInstClearOut.class);

		cplOut.setAcct_no(cplIn.getAcct_no());
		cplOut.setAcct_type(accessOut.getAcct_type());
		cplOut.setSub_acct_seq(subAcctInfo.getSub_acct_seq());
		cplOut.setProd_id(subAcctInfo.getProd_id());
		cplOut.setCcy_code(subAcctInfo.getCcy_code());
		cplOut.setTally_before_bal(tallyBeforeBalance);
		cplOut.setPaying_amt(subAcctInfo.getAcct_bal());
		cplOut.setInterest(depositInstInfo.getInterest());
		cplOut.setInterest_tax(depositInstInfo.getInterest_tax());
		cplOut.setOverdraft_interest(overDraftInst);

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpCurrentInterestClear.subAccountClear end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月25日-下午4:21:13</li>
	 *         <li>功能说明：活期账户结清</li>
	 *         </p>
	 * @param cplIn
	 *            账户结清输入
	 * @return 账户结清输出
	 */
	public static DpAcctInstClearOut accountClear(DpAcctInstClearIn cplIn) {

		bizlog.method(" DpCurrentInterestClear.accountClear begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 输出
		DpAcctInstClearOut cplOut = BizUtil.getInstance(DpAcctInstClearOut.class);

		// 账户定位
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, true);

		// 账户结清检查
		List<DpaSubAccount> listSubAcct = accountClearCheck(cplIn, acctInfo);

		// 解冻处理赋值
		DpInstClearFrozeCancleIn cplCancelIn = BizUtil.getInstance(DpInstClearFrozeCancleIn.class);

		cplCancelIn.setAcct_no(acctInfo.getAcct_no());
		cplCancelIn.setCust_no(acctInfo.getCust_no());

		// 解冻处理: 含冻结状况检查
		cancleFrozeProcess(cplCancelIn);

		// 上次币种
		String lastCurrency = listSubAcct.get(0).getCcy_code();
		// 利息结清结果
		DpInstClearInfo cplInstClear = BizUtil.getInstance(DpInstClearInfo.class);

		cplInstClear.setBal_after_trxn(BigDecimal.ZERO);
		cplInstClear.setInterest(BigDecimal.ZERO);
		cplInstClear.setInterest_tax(BigDecimal.ZERO);
		cplInstClear.setOverdraft_interest(BigDecimal.ZERO);
		cplInstClear.setTally_before_bal(BigDecimal.ZERO);

		// 循环结清各活期子户
		for (DpaSubAccount subAcct : listSubAcct) {

			DpbFroze subAcctFroze = DpbFrozeDao.selectFirst_odb2(subAcct.getSub_acct_no(), false);

			// 子账户存在冻结，不能结清
			if (subAcctFroze != null && subAcctFroze.getFroze_status() == E_FROZESTATUS.FROZE) {
				throw DpBase.E0035(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
			}

			// 交易前余额
			BigDecimal tallyBeforeBalance = subAcct.getAcct_bal();

			// 协议自动关闭处理
			agreeProcess(subAcct.getAcct_no(), subAcct.getSub_acct_no());

			// 存款利息结清
			DpInstAccounting depositInstInfo = depositInterestClear(cplIn, subAcct);

			// 透支利息结清
			BigDecimal overDraftInst = overDraftInterestClear(cplIn, subAcct);

			// 利息结清后账户余额仍小于零，不能销户
			if (CommUtil.compare(subAcct.getAcct_bal(), BigDecimal.ZERO) < 0) {
				throw DpBase.E0118(cplIn.getAcct_no(), subAcct.getSub_acct_seq());
			}

			// 按币种维度汇总利息结清结果
			if (!CommUtil.equals(lastCurrency, subAcct.getCcy_code())) {

				cplOut.getList01().add(cplInstClear);

				// 再次初始赋值
				cplInstClear = BizUtil.getInstance(DpInstClearInfo.class);

				cplInstClear.setBal_after_trxn(subAcct.getAcct_bal());
				cplInstClear.setInterest(depositInstInfo.getInterest());
				cplInstClear.setInterest_tax(depositInstInfo.getInterest_tax());
				cplInstClear.setOverdraft_interest(overDraftInst);
				cplInstClear.setTally_before_bal(tallyBeforeBalance);
				cplInstClear.setPaying_amt(cplInstClear.getBal_after_trxn());
				cplInstClear.setCcy_code(subAcct.getCcy_code());
			}
			else {

				cplInstClear.setBal_after_trxn(cplInstClear.getBal_after_trxn().add(subAcct.getAcct_bal()));
				cplInstClear.setInterest(cplInstClear.getInterest().add(depositInstInfo.getInterest()));
				cplInstClear.setInterest_tax(cplInstClear.getInterest_tax().add(depositInstInfo.getInterest_tax()));
				cplInstClear.setOverdraft_interest(cplInstClear.getOverdraft_interest().add(overDraftInst));
				cplInstClear.setTally_before_bal(cplInstClear.getTally_before_bal().add(tallyBeforeBalance));
				cplInstClear.setPaying_amt(cplInstClear.getBal_after_trxn());
				cplInstClear.setCcy_code(subAcct.getCcy_code());
			}
		}

		cplOut.getList01().add(cplInstClear);
		cplOut.setAcct_no(acctInfo.getAcct_no());
		cplOut.setAcct_name(acctInfo.getAcct_name());
		cplOut.setCust_no(acctInfo.getCust_no());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpCurrentInterestClear.accountClear end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月25日-下午4:21:13</li>
	 *         <li>功能说明：活期账户结清后全额支取</li>
	 *         </p>
	 * @param cplIn
	 *            账户结清支取输入
	 */
	public static void accountClearDraw(DpAcctClearDrawIn cplIn) {

		bizlog.method(" DpCurrentInterestClear.accountClearDraw begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 账户定位
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, true);

		// 活期子户集
		List<DpaAccountRelate> listAcctRelate = new ArrayList<DpaAccountRelate>();

		if (CommUtil.isNull(cplIn.getCcy_code())) {

			listAcctRelate = DpaAccountRelateDao.selectAll_odb7(acctInfo.getAcct_no(), E_DEMANDORTIME.DEMAND, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT, false);
		}
		else {

			listAcctRelate = DpaAccountRelateDao.selectAll_odb6(acctInfo.getAcct_no(), cplIn.getCcy_code(), E_DEMANDORTIME.DEMAND, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT,
					false);
		}

		// 循环结清支取各活期子户
		for (DpaAccountRelate cplAcctRelate : listAcctRelate) {

			// 剔除产品不符合要求的活期子户
			if (CommUtil.isNotNull(cplIn.getProd_id()) && !CommUtil.equals(cplIn.getProd_id(), cplAcctRelate.getProd_id())) {
				continue;
			}

			// 子账户信息带锁查询
			DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(cplAcctRelate.getAcct_no(), cplAcctRelate.getSub_acct_no(), true);

			// 账户未结清利息
			if (subAcct.getSub_acct_status() != E_SUBACCTSTATUS.PRECLOSE) {
				throw DpBase.E0408(cplAcctRelate.getAcct_no(), cplAcctRelate.getSub_acct_seq());
			}

			DpDemandDrawIn cplDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			cplDrawIn.setAcct_no(subAcct.getAcct_no());
			cplDrawIn.setCash_trxn_ind(cplIn.getCash_trxn_ind());
			cplDrawIn.setCcy_code(subAcct.getCcy_code());
			cplDrawIn.setTrxn_amt(subAcct.getAcct_bal());
			cplDrawIn.setCheck_password_ind(E_YESORNO.NO);
			cplDrawIn.setCustomer_remark(cplIn.getCustomer_remark());
			cplDrawIn.setSummary_code(cplIn.getSummary_code());
			cplDrawIn.setOpp_acct_route(cplIn.getOpp_acct_route());
			cplDrawIn.setOpp_acct_no(cplIn.getOpp_acct_no());
			cplDrawIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
			cplDrawIn.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
			cplDrawIn.setOpp_trxn_amt(cplIn.getOpp_trxn_amt());
			cplDrawIn.setReal_opp_acct_no(cplDrawIn.getReal_opp_acct_no());
			cplDrawIn.setReal_opp_acct_name(cplDrawIn.getReal_opp_acct_name());
			cplDrawIn.setReal_opp_bank_id(cplDrawIn.getReal_opp_bank_id());
			cplDrawIn.setReal_opp_bank_name(cplDrawIn.getReal_opp_bank_name());
			cplDrawIn.setReal_opp_branch_name(cplDrawIn.getReal_opp_branch_name());
			cplDrawIn.setReal_opp_remark(cplDrawIn.getReal_opp_remark());

			//DpDemandDraw.doMainMethod(cplDrawIn, acctInfo, subAcct);
			
			
			
			DpDemandDraw.doMain(cplDrawIn);
		}

		bizlog.method(" DpCurrentInterestClear.accountClearDraw end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月25日-下午4:21:13</li>
	 *         <li>功能说明：子账户结清检查</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void subAccountClearCheck(DpaSubAccount subAcct) {

		// 不是活期账户
		if (subAcct.getDd_td_ind() != E_DEMANDORTIME.DEMAND) {

			throw DpErr.Dp.E0487(subAcct.getAcct_no());
		}

		// 子账户状态检查
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE || subAcct.getSub_acct_status() == E_SUBACCTSTATUS.PRECLOSE) {

			throw DpBase.E0017(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
		}

		// 账户下子户有浮动额，不能销户
		if (CommUtil.compare(subAcct.getAcct_float_bal(), BigDecimal.ZERO) > 0) {
			throw DpBase.E0197(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
		}

		// 调用收息账户、还息账户、协议等销户外部约束检查
		DpCloseSubAccountCheck.checkExternalConstraint(subAcct);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月25日-下午4:21:13</li>
	 *         <li>功能说明：账户结清检查</li>
	 *         </p>
	 * @param cplIn
	 *            账户结清输入
	 * @param acctInfo
	 *            账户信息
	 * @return 活期子户集
	 */
	private static List<DpaSubAccount> accountClearCheck(DpAcctInstClearIn cplIn, DpaAccount acctInfo) {

		// 账户状态检查
		if (acctInfo.getAcct_status() == E_ACCTSTATUS.CLOSE) {

			throw DpBase.E0008(acctInfo.getAcct_no());
		}

		// 活期子户集
		List<DpaAccountRelate> listAcctRelate = new ArrayList<DpaAccountRelate>();

		if (CommUtil.isNull(cplIn.getCcy_code())) {

			// 判断是否存在定期子户
			DpaAccountRelate acctRelate = DpaAccountRelateDao.selectFirst_odb7(acctInfo.getAcct_no(), E_DEMANDORTIME.TIME, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT, false);

			if (acctRelate != null) {

				throw DpBase.E0413(acctRelate.getAcct_no());
			}

			// 查询所有活期子户
			listAcctRelate = DpaAccountRelateDao.selectAll_odb7(acctInfo.getAcct_no(), E_DEMANDORTIME.DEMAND, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT, false);
		}
		else {

			// 判断是否存在指定币种定期子户
			DpaAccountRelate acctRelate = DpaAccountRelateDao.selectFirst_odb6(acctInfo.getAcct_no(), cplIn.getCcy_code(), E_DEMANDORTIME.TIME, E_ACCTSTATUS.NORMAL,
					E_ACCTBUSITYPE.DEPOSIT, false);

			if (acctRelate != null) {

				throw DpBase.E0413(acctRelate.getAcct_no());
			}

			// 查询指定币种活期子户
			listAcctRelate = DpaAccountRelateDao.selectAll_odb6(acctInfo.getAcct_no(), cplIn.getCcy_code(), E_DEMANDORTIME.DEMAND, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT,
					false);

			// TODO: 如果是结清默认子户，那么是否还有其他币种活期子户未关闭，以保证默认子户是最后关闭的
			if (listAcctRelate.get(0).getDefault_ind() == E_YESORNO.YES 
					&& CommUtil.equals(listAcctRelate.get(0).getProd_id(), cplIn.getProd_id())
					&& listAcctRelate.size() > 1) {
				// TODO:
				throw DpBase.E0413(acctInfo.getAcct_no());
			}
		}

		// 子账户信息列表
		List<DpaSubAccount> listSubAcct = new ArrayList<DpaSubAccount>();

		// 获得子户信息
		for (DpaAccountRelate relateInfo : listAcctRelate) {

			// 剔除产品不符合要求的活期子户
			if (CommUtil.isNotNull(cplIn.getProd_id()) && !CommUtil.equals(cplIn.getProd_id(), relateInfo.getProd_id())) {
				continue;
			}

			// 子账户信息带锁查询
			DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(relateInfo.getAcct_no(), relateInfo.getSub_acct_no(), true);

			// 非结清活期子户需要检查是否收息或还息账户；是否有协议限制不能销户
			if (subAcct.getSub_acct_status() != E_SUBACCTSTATUS.PRECLOSE) {

				// 账户下子户有浮动额，不能销户
				if (CommUtil.compare(subAcct.getAcct_float_bal(), BigDecimal.ZERO) > 0) {
					throw DpBase.E0197(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
				}

				// 调用收息账户、还息账户、协议等销户外部约束检查
				DpCloseSubAccountCheck.checkExternalConstraint(subAcct);
			}

			// 账户下子户有浮动额，不能销户
			if (CommUtil.compare(subAcct.getAcct_float_bal(), BigDecimal.ZERO) > 0) {
				throw DpBase.E0197(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
			}

			listSubAcct.add(subAcct);
		}

		// 账户下没有活期子户
		if (listSubAcct.isEmpty() || listSubAcct.size() == 0) {
			throw DpErr.Dp.E0020(acctInfo.getAcct_no());
		}

		// 按币种排序
		BizUtil.listSort(listSubAcct, true, SysDict.A.ccy_code.getId());

		// 输出子账户列表
		return listSubAcct;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月25日-下午4:21:13</li>
	 *         <li>功能说明：协议相关处理</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 * @return 账户结清输出
	 */
	private static void agreeProcess(String acctNo, String subAcctNo) {

		if (CommUtil.isNotNull(subAcctNo)) {

			DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

			// 查询智能存款协议, 存在有效协议则自动关闭
			DpbSmartDeposit smartDeptAgree = DpbSmartDepositDao.selectFirst_odb3(subAcctInfo.getAcct_no(), subAcctInfo.getCcy_code(), E_STATUS.VALID, false);

			if (smartDeptAgree != null) {

				DpSmartDepositIn dpSmartDepositIn = BizUtil.getInstance(DpSmartDepositIn.class);

				dpSmartDepositIn.setAgree_no(smartDeptAgree.getAgree_no());
				dpSmartDepositIn.setCheck_password_ind(E_YESORNO.NO);
				dpSmartDepositIn.setCancle_agree_ind(E_YESORNO.YES);
				dpSmartDepositIn.setData_version(smartDeptAgree.getData_version());

				DpSmartDepositAmt.maintenSmartDepositAnt(dpSmartDepositIn);

				// 关闭智能存款协议有账务处理，需重新再读一次
				subAcctInfo = DpaSubAccountDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), true);
			}
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月25日-下午4:21:13</li>
	 *         <li>功能说明： 存款利息结清</li>
	 *         </p>
	 * @param cplIn
	 *            利息结清输入
	 * @param subAcct
	 *            子账户信息
	 * @return 存款利息信息
	 */
	private static DpInstAccounting depositInterestClear(DpInstClearIn cplIn, DpaSubAccount subAcct) {

		// 已经结清的账户无需二次结清利息
		if (subAcct.getSub_acct_status() != E_SUBACCTSTATUS.NORMAL) {

			DpInstAccounting cplInstIn = BizUtil.getInstance(DpInstAccounting.class);

			cplInstIn.setInst_tax_rate(BigDecimal.ZERO);
			cplInstIn.setInterest(BigDecimal.ZERO);
			cplInstIn.setInterest_tax(BigDecimal.ZERO);
			cplInstIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_PAY_INST_SELF"));

			return cplInstIn;
		}

		// 日期后计息期间补结利息值
		BigDecimal eodInstPaid = BigDecimal.ZERO;
		BigDecimal eodInstTaxPaid = BigDecimal.ZERO;

		// 日切后计息前补充利息处理
		if (CommUtil.compare(subAcct.getNext_inst_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {

			BigDecimal instPaid = subAcct.getInst_paid();
			BigDecimal InstTaxPaid = subAcct.getInst_withholding_tax();

			DpDayEndInterest.onlineDealInterest(subAcct);

			// 重新再读一次
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

			// 日终期间结转的周期利息
			eodInstPaid = subAcct.getInst_paid().subtract(instPaid);
			eodInstTaxPaid = subAcct.getInst_withholding_tax().subtract(InstTaxPaid);
		}

		// 活期销户直接付息处理
		DpInterestResult cplInstOut = DpInterestBasicApi.currentInstSettleClear(subAcct);

		// 登记付息明细
		if (CommUtil.isNotNull(cplInstOut.getList_inst_detail()) && cplInstOut.getList_inst_detail().size() > 0) {

			DpInterestBasicApi.regPayedInstDetl(cplInstOut.getList_inst_detail(), subAcct);
		}

		// 利息及利息税记账信息
		DpInstAccounting cplInstIn = BizUtil.getInstance(DpInstAccounting.class);

		cplInstIn.setInst_tax_rate(cplInstOut.getInst_tax_rate());
		cplInstIn.setInterest(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplInstOut.getAccrual_inst()));
		cplInstIn.setInterest_tax(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplInstOut.getAccrual_inst_tax()));
		cplInstIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_PAY_INST_SELF"));

		// 借：应付利息，贷：代扣利息税
		DpInterestSettlement.payInterestAccounting(cplInstIn, subAcct);

		// 存款利息入账，即 贷：税后利息
		depositInstAccounting(subAcct, cplInstIn);

		// 再读取一次
		subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

		// 更新为预销户状态
		subAcct.setSub_acct_status(E_SUBACCTSTATUS.PRECLOSE);

		DpaSubAccountDao.updateOne_odb1(subAcct);

		// 补充日终期间结息金额
		cplInstIn.setInterest(cplInstIn.getInterest().add(eodInstPaid));
		cplInstIn.setInterest_tax(cplInstIn.getInterest_tax().add(eodInstTaxPaid));

		return cplInstIn;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月25日-下午4:21:13</li>
	 *         <li>功能说明： 存款利息结清入息记账</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplInstIn
	 *            利息记账数据
	 */
	private static void depositInstAccounting(DpaSubAccount subAcct, DpInstAccounting cplInstIn) {

		// 税后利息
		BigDecimal instAfterTax = cplInstIn.getInterest().subtract(cplInstIn.getInterest_tax());

		// 无净利息则退出
		if (CommUtil.compare(instAfterTax, BigDecimal.ZERO) == 0) {

			return;
		}

		// 利息小于零，结清时需要扣回利息，前面已经做过账户冻结检查
		if (CommUtil.compare(instAfterTax, BigDecimal.ZERO) < 0) {

			// 记账方法接口
			DpUpdAccBalIn cplUpdBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

			cplUpdBalIn.setAcct_no(subAcct.getAcct_no()); // 账号
			cplUpdBalIn.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
			cplUpdBalIn.setTrxn_event_id(""); // 交易事件ID
			cplUpdBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.INTEREST);
			cplUpdBalIn.setTrxn_ccy(subAcct.getCcy_code()); // 交易币种
			cplUpdBalIn.setTrxn_amt(instAfterTax.abs()); // 税后利息
			cplUpdBalIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT);
			cplUpdBalIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
			cplUpdBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
			cplUpdBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
			cplUpdBalIn.setOpp_acct_no("");// 对手方账户登记自身账户
			cplUpdBalIn.setOpp_acct_ccy(subAcct.getCcy_code());// 对手方币种
			cplUpdBalIn.setOpp_trxn_amt(instAfterTax);
			cplUpdBalIn.setOpp_acct_route(E_ACCOUTANALY.INSIDE);// 对方账户路由
			cplUpdBalIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_PAY_INST_SELF")); // 摘要代码
			cplUpdBalIn.setInst_tax_rate(cplInstIn.getInst_tax_rate());
			cplUpdBalIn.setInst_withholding_tax(cplInstIn.getInterest_tax());

			DpAccounting.online(cplUpdBalIn);

			return;
		}

		// 接下来处理税后利息大于零的情况
		// 贷：收息账号或自身
		E_YESORNO intoSelfFlag = E_YESORNO.YES;

		// 先判断收息账户
		if (CommUtil.isNotNull(subAcct.getIncome_inst_acct())) {

			intoSelfFlag = E_YESORNO.NO;

			// 收息币种
			String incomeCcy = CommUtil.nvl(subAcct.getIncome_inst_ccy(), subAcct.getCcy_code());

			try {
				// 检查收息账户
				DpPublicCheck.checkIncomeAcct(subAcct.getIncome_inst_acct(), incomeCcy, E_SAVEORWITHDRAWALIND.SAVE);
			}
			catch (Exception e) {

				// 系统异常抛错
				if (!(e instanceof LttsBusinessException)) {
					throw e;
				}

				intoSelfFlag = E_YESORNO.YES; // 定位收息账户出错，不报错且尝试入自身
			}

			// 入收息账户
			if (intoSelfFlag == E_YESORNO.NO) {

				DpInterestAccounting.instIntoAppointAcct(cplInstIn, subAcct, subAcct.getIncome_inst_acct(), incomeCcy);
			}
		}

		// 利息入自身
		if (intoSelfFlag == E_YESORNO.YES) {

			DpInterestAccounting.instIntoSelf(cplInstIn, subAcct);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月29日-下午4:21:13</li>
	 *         <li>功能说明： 透支利息结清</li>
	 *         </p>
	 * @param cplIn
	 *            利息结清输入
	 * @param subAcct
	 *            子账户信息
	 * @return 透支利息
	 */
	private static BigDecimal overDraftInterestClear(DpInstClearIn cplIn, DpaSubAccount subAcct) {

		// 不是透支账户
		if (subAcct.getOverdraft_allow_ind() != E_YESORNO.YES) {

			return BigDecimal.ZERO;
		}

		// 已经结清的账户无需二次结清利息
		if (subAcct.getSub_acct_status() != E_SUBACCTSTATUS.NORMAL) {

			return BigDecimal.ZERO;
		}

		BigDecimal overDraftInst = BigDecimal.ZERO;

		List<DpbOverdraft> listOdAgree = DpbOverdraftDao.selectAll_odb5(subAcct.getAcct_no(), subAcct.getCcy_code(), false);

		for (DpbOverdraft OdAgree : listOdAgree) {

			if (OdAgree.getAgree_status() == E_STATUS.INVALID) {
				continue;
			}

			List<DpbOverdraftSlip> listOdFiche = DpbOverdraftSlipDao.selectAll_odb3(subAcct.getAcct_no(), OdAgree.getAgree_no(), false);

			for (DpbOverdraftSlip OdFiche : listOdFiche) {

				DpaSlip ficheInst = DpaSlipDao.selectOne_odb1(subAcct.getAcct_no(), OdFiche.getFiche_no(), true);

				// 透支卡片利息 + 罚息
				overDraftInst = overDraftInst.add(ficheInst.getAccrual_inst()).add(OdFiche.getOverdue_interest());
			}

			// 单个透支协议关闭及利息数据清理
			DpOverdraftSettlement.clearSingleOdAgree(OdAgree.getAcct_no(), OdAgree.getAgree_no(), E_YESORNO.YES);
		}

		// 无透支利息则返回零
		if (CommUtil.equals(overDraftInst, BigDecimal.ZERO)) {
			return BigDecimal.ZERO;
		}

		if (CommUtil.compare(overDraftInst, BigDecimal.ZERO) > 0) {

			DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			demandDrawIn.setAcct_no(subAcct.getAcct_no());
			demandDrawIn.setAcct_type("");
			demandDrawIn.setCcy_code(subAcct.getCcy_code());
			demandDrawIn.setProd_id(subAcct.getProd_id());
			demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			demandDrawIn.setTrxn_record_type(E_TRXNRECORDTYPE.OD_INTEREST);
			demandDrawIn.setOpp_acct_route(E_ACCOUTANALY.INSIDE);
			demandDrawIn.setOpp_acct_no(null);
			demandDrawIn.setOpp_acct_ccy(subAcct.getCcy_code());
			demandDrawIn.setOpp_trxn_amt(overDraftInst);
			demandDrawIn.setOpp_acct_type(null);
			demandDrawIn.setTrxn_amt(overDraftInst);
			demandDrawIn.setSummary_code(cplIn.getSummary_code());
			demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.CLOSE);

			DpDemandDraw.doMain(demandDrawIn);
		}
		else {

			// 存入冻结检查
			DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_SAVE, null);

			// 存入
			DpUpdAccBalIn cplInput = BizUtil.getInstance(DpUpdAccBalIn.class);

			cplInput.setAcct_no(subAcct.getAcct_no()); // 账号
			cplInput.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
			cplInput.setBack_value_date(BizUtil.getTrxRunEnvs().getTrxn_date()); // 起息日期
			cplInput.setTrxn_amt(overDraftInst.abs()); // 交易金额
			cplInput.setTrxn_ccy(subAcct.getCcy_code()); // 交易币种
			cplInput.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT); // 记账方向
			cplInput.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
			cplInput.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
			cplInput.setShow_ind(E_YESORNO.YES); // 是否显示标志
			cplInput.setTrxn_record_type(E_TRXNRECORDTYPE.OD_INTEREST); // 交易明细类别
			cplInput.setSummary_code(cplIn.getSummary_code()); // 摘要代码
			cplInput.setTrxn_remark(""); // 交易备注
			cplInput.setCustomer_remark(""); // 客户备注
			cplInput.setOpp_acct_route(E_ACCOUTANALY.INSIDE); // 对方账户路由
			cplInput.setOpp_acct_no(""); // 对方账号
			cplInput.setOpp_acct_ccy(subAcct.getCcy_code()); // 对方币种
			cplInput.setOpp_branch_id(subAcct.getSub_acct_branch()); // 对方机构号
			cplInput.setOpp_trxn_amt(overDraftInst.abs());

			DpAccounting.online(cplInput);
		}

		// 读取子账户信息
		subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

		// 贷： 应收利息
		DpInstAccounting cplInstIn = BizUtil.getInstance(DpInstAccounting.class);

		cplInstIn.setInterest(overDraftInst);
		cplInstIn.setInterest_tax(BigDecimal.ZERO);

		DpInterestSettlement.receivableInterestAccounting(cplInstIn, subAcct);

		return overDraftInst;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月29日-下午4:21:13</li>
	 *         <li>功能说明： 解冻检查及处理</li>
	 *         </p>
	 * @param cplIn
	 *            利息结清解冻输入
	 */
	private static void cancleFrozeProcess(DpInstClearFrozeCancleIn cplIn) {

		// 对于全额冻结和金额冻结，销户结清都可以解冻， 如果仅仅是支取是不能解除全额冻结的
		if (CommUtil.isNotNull(cplIn.getFroze_no())) {

			DpUnFrozeIn dpUnFrozeIn = BizUtil.getInstance(DpUnFrozeIn.class);

			dpUnFrozeIn.setFroze_no(cplIn.getFroze_no());
			dpUnFrozeIn.setUnfroze_reason(cplIn.getUnfroze_reason());
			dpUnFrozeIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.CLOSE);
			dpUnFrozeIn.setAcct_no(cplIn.getAcct_no());
			dpUnFrozeIn.setFroze_feature_code(cplIn.getFroze_feature_code());
			dpUnFrozeIn.setSub_acct_seq(cplIn.getSub_acct_seq());
			dpUnFrozeIn.setCust_no(cplIn.getCust_no());

			// 同一客户下解冻，调用方法，不用调服务降低效率
			DpUnFroze.doMain(dpUnFrozeIn);
		}

		// 再处理自助解冻问题
		if (ApBusinessParmApi.getValue("IS_SELF_FROZEN_THAW") == E_YESORNO.YES.getValue()) {

			// 解冻渠道
			String frozeChannel = ApBusinessParmApi.getValue("SELF_FROZEN_THAW_CHANNEL");

			String cardNo = DpToolsApi.getCardNoByAcctNo(cplIn.getAcct_no());

			// 调用自助解冻
			DpUnFroze.selfUnFrozen(cardNo, E_FROZEOBJECT.CARD, frozeChannel);
		}

		// 为支持7*24小时，需要联机处理到期解冻
		DpUnFroze.matureAutoUnfrozen(cplIn.getCust_no());

		// 经过上述处理，最后检查，客户、卡、账户是否还有冻结记录，若存在则结清没有意义，后续仍然无法立即销户
		DpbFroze custFroze = DpbFrozeDao.selectFirst_odb2(cplIn.getCust_no(), false);

		// 客户存在冻结，不能结清
		if (custFroze != null && custFroze.getFroze_status() == E_FROZESTATUS.FROZE) {
			throw DpBase.E0271(cplIn.getCust_no());
		}

		DpbFroze acctFroze = DpbFrozeDao.selectFirst_odb2(cplIn.getAcct_no(), false);

		// 账户存在冻结，不能结清
		if (acctFroze != null && acctFroze.getFroze_status() == E_FROZESTATUS.FROZE) {
			throw DpBase.E0030(cplIn.getAcct_no());
		}

		if (CommUtil.isNotNull(cplIn.getSub_acct_seq())) {

			DpaAccountRelate acctRelate = DpaAccountRelateDao.selectOne_odb1(cplIn.getAcct_no(), cplIn.getSub_acct_seq(), true);

			DpbFroze subAcctFroze = DpbFrozeDao.selectFirst_odb2(acctRelate.getSub_acct_no(), false);

			// 子账户存在冻结，不能结清
			if (subAcctFroze != null && subAcctFroze.getFroze_status() == E_FROZESTATUS.FROZE) {
				throw DpBase.E0035(acctRelate.getAcct_no(), acctRelate.getSub_acct_seq());
			}
		}
	}
}
