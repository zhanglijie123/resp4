package cn.sunline.icore.dp.serv.interest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAccountApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApReversalApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.type.ComApAccounting.ApAccountingEventIn;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpTaxApi;
import cn.sunline.icore.dp.base.api.DpTimeInterestApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.namedsql.SqlDpTimeSlipBasicDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpTimeSlipBase.DpaTimeSlip;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpTaxBase.DpIntTaxInfo;
import cn.sunline.icore.dp.base.type.ComDpTimeSlipBase.DpTimeSlipInstTrxnRegister;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INCOMEACCTPRIOR;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTOPERATE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_PAYINSTWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RENEWSAVEWAY;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.type.ComDpReversal.DpPayedInstReversalIn;
import cn.sunline.icore.dp.serv.type.ComDpReversal.DpRecievedInstReversalIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_INCOMEINTERESTOBJECTTYPE;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUNTINGSUBJECT;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_BALPROPERTY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_ROUNDRULE;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.base.logging.LogConfigManager.SystemType;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：结息相关
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年3月24日-下午4:42:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpInterestSettlement {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpInterestSettlement.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月17日-下午3:21:29</li>
	 *         <li>功能说明：存款结息处理</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号: 外面已经带锁
	 */
	public static void settleInterest(String acctNo, String subAcctNo) {

		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		if (subAcct.getInst_ind() == E_YESORNO.NO) {
			return;
		}

		boolean updateInstDefineFlag = false; // 更新计息定义表标志
		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 获取计提利息索引类型
		E_INSTKEYTYPE instKey = DpInterestBasicApi.getCainInstKey(subAcct, trxnDate);

		// 读取账户计息信息
		DpaInterest instAcct = DpaInterestDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), instKey, true);

		// 1. 周期付息，且到期日不是周期结息日
		if (instAcct.getPay_inst_method() != E_PAYINSTWAY.NO_CYCLE && CommUtil.equals(instAcct.getNext_pay_inst_date(), trxnDate)
				&& !CommUtil.equals(trxnDate, subAcct.getDue_date())) {

			dayEndPayInterest(subAcct, instAcct, E_YESORNO.YES, E_YESORNO.NO);

			updateInstDefineFlag = true;
		}
		// 2.定期到期付息: 全部记到待支付利息，如果设有到期指令则到期指令步骤再处理待支付
		else if (CommUtil.equals(trxnDate, subAcct.getDue_date())) {

			// 添加本金续存、减少本金续存、本息续存，到期日利息强制入待支取利息
			if (CommUtil.in(subAcct.getRenewal_method(), E_RENEWSAVEWAY.ADD_AMOUNT, E_RENEWSAVEWAY.PART_AMOUNT, E_RENEWSAVEWAY.PRIN_INST)) {
				dayEndPayInterest(subAcct, instAcct, E_YESORNO.NO, E_YESORNO.YES);
			}
			else if (CommUtil.in(subAcct.getRenewal_method(), E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT, E_RENEWSAVEWAY.PRINCIPAL)) {
				dayEndPayInterest(subAcct, instAcct, E_YESORNO.NO, E_YESORNO.NO);
			}
			else {
				// 无指示也入待支取利息
				dayEndPayInterest(subAcct, instAcct, E_YESORNO.NO, E_YESORNO.YES);
			}

			updateInstDefineFlag = true;
		}

		// 不规则付息变更付息周期
		if (instAcct.getPay_inst_method() == E_PAYINSTWAY.CHANGE_CYCLE) {

			DpTimeInterestApi.modifyPayCycle(instAcct, subAcct, trxnDate);

			updateInstDefineFlag = true;
		}

		// 更新计息定义表
		if (updateInstDefineFlag) {
			DpaInterestDao.updateOne_odb1(instAcct);
		}

		// 今天为周期结息日，重置净存入余额字段
		if (instAcct.getPay_inst_method() != E_PAYINSTWAY.NO_CYCLE && CommUtil.equals(instAcct.getLast_pay_inst_date(), trxnDate)) {

			// 结息里面会更新子户信息，安全起见先重新读取缓存
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

			subAcct.setPrev_net_dept_bal(BigDecimal.ZERO);
			subAcct.setNet_dept_amt(subAcct.getAcct_bal().subtract(subAcct.getPrev_date_bal()));

			DpaSubAccountDao.updateOne_odb1(subAcct);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年3月22日-上午10:25:39</li>
	 *         <li>功能说明：日终存款结息</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param instAcct
	 *            账户计息定义
	 * @param cycPayFlag
	 *            周期结息标志
	 * @param forceIntoWait
	 *            强制入待支取利息标志
	 */
	private static void dayEndPayInterest(DpaSubAccount subAcct, DpaInterest instAcct, E_YESORNO cycPayFlag, E_YESORNO forceIntoWait) {

		bizlog.parm("Start interest payment processing, sub account = [%s]", subAcct.getSub_acct_no());

		// 不计息的也要清空积数
		if (subAcct.getInst_ind() == E_YESORNO.NO) {
			instAcct.setAccrual_sum_bal(BigDecimal.ZERO);
			instAcct.setCur_term_inst_sum_bal(BigDecimal.ZERO);

			// 计算下次付息日
			bizlog.debug("Sub-account[%s] is not interest-bearing, no interest", subAcct.getSub_acct_no());
			return;
		}

		// 流水重置(只有日终批量渠道有待结转利息才申请新流水，其他渠道触发的结息不会重置流水)
		if (CommUtil.compare(instAcct.getAccrual_inst(), BigDecimal.ZERO) > 0 && CommUtil.equals(BizUtil.getTrxRunEnvs().getChannel_id(), ApConst.SYSTEM_BATCH)) {
			BizUtil.resetTrxnSequence();
		}

		// 默认为四舍五入
		E_ROUNDRULE roundRule = E_ROUNDRULE.ROUND;

		// 活期利率低，周期性结息是否截位处理利息
		if (subAcct.getDd_td_ind() == E_DEMANDORTIME.DEMAND && cycPayFlag == E_YESORNO.YES) {
			// roundRule = E_ROUNDRULE.DOWN;
		}

		// 得到待结清利息信息
		DpInstAccounting cplWaitSettleInst = getInstSettleInfo(subAcct, instAcct, roundRule);

		// 利息转入方类型
		E_INCOMEINTERESTOBJECTTYPE intoObjectType = (forceIntoWait == E_YESORNO.YES) ? E_INCOMEINTERESTOBJECTTYPE.WAIT : E_INCOMEINTERESTOBJECTTYPE.SELF;

		// 不是强制入待支取利息则要分析利息转入方类型
		if (forceIntoWait != E_YESORNO.YES) {
			intoObjectType = analysisIncomeInterestObject(instAcct, subAcct);
		}

		// 利息记账金额不为零且不是入待支取利息则进行账务处理
		if (CommUtil.compare(cplWaitSettleInst.getInterest(), BigDecimal.ZERO) != 0 && intoObjectType != E_INCOMEINTERESTOBJECTTYPE.WAIT) {

			cplWaitSettleInst.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_PAY_INST_SELF"));

			// （一）借：应付利息，贷：代扣利息税
			DpInterestSettlement.payInterestAccounting(cplWaitSettleInst, subAcct);

			// （二）贷：收息账号: 自身、收息账户、待支付利息等
			if (intoObjectType == E_INCOMEINTERESTOBJECTTYPE.SELF) {
				DpInterestAccounting.instIntoSelf(cplWaitSettleInst, subAcct);
			}
			else if (intoObjectType == E_INCOMEINTERESTOBJECTTYPE.OTHER) {

				String inComeCcy = CommUtil.nvl(subAcct.getIncome_inst_ccy(), subAcct.getCcy_code());

				DpInterestAccounting.instIntoAppointAcct(cplWaitSettleInst, subAcct, subAcct.getIncome_inst_acct(), inComeCcy);
			}
			else {
				throw APPUB.E0026("income interest object type", intoObjectType.getValue());
			}
		}

		// 计息定义和利息明细记录处理
		processInterestRecord(instAcct, roundRule, intoObjectType, cycPayFlag);

		// 更新账户层信息
		if (intoObjectType != E_INCOMEINTERESTOBJECTTYPE.WAIT) {

			afreshAcctInfoForInterest(subAcct.getAcct_no(), subAcct.getSub_acct_no(), cplWaitSettleInst, cycPayFlag);
		}

		// TODO:处理结息短信
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年3月25日-下午4:34:52</li>
	 *         <li>付息记账处理：不含利息接收方分录</li>
	 *         <li>需考虑利息税的值不能大于利息</li>
	 *         </p>
	 *         <p>
	 * @param cplInput
	 *            付息利息信息
	 * @param subAcct
	 *            子账户信息
	 */
	public static void payInterestAccounting(DpInstAccounting cplIn, DpaSubAccount subAcct) {

		// 冲账会是红字，所以判断不等于零即可
		if (CommUtil.equals(cplIn.getInterest(), BigDecimal.ZERO) && CommUtil.equals(cplIn.getInterest_tax(), BigDecimal.ZERO)) {
			return;
		}

		bizlog.method(" DpInterestAccounting.payInterestAccounting begin >>>>>>>>>>>>>>>>");

		String summaryCode = CommUtil.nvl(cplIn.getSummary_code(), ApSystemParmApi.getSummaryCode("DEPT_PAY_INST"));

		// 登记会计记账事件
		ApAccountingEventIn cplTaEventIn = BizUtil.getInstance(ApAccountingEventIn.class);

		cplTaEventIn.setAccounting_alias(subAcct.getAccounting_alias());
		cplTaEventIn.setAccounting_subject(E_ACCOUNTINGSUBJECT.DEPOSIT);
		cplTaEventIn.setAcct_branch(subAcct.getSub_acct_branch());
		cplTaEventIn.setDouble_entry_ind(E_YESORNO.YES);
		cplTaEventIn.setTrxn_ccy(subAcct.getCcy_code());
		cplTaEventIn.setAcct_no(subAcct.getAcct_no());
		cplTaEventIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplTaEventIn.setProd_id(subAcct.getProd_id());
		cplTaEventIn.setSummary_code(summaryCode);
		cplTaEventIn.setSummary_name(ApSummaryApi.getText(summaryCode));

		/* 会计入账接口信息: 利息处理, 用不等于符号，是为了便于红字冲账时调用 */
		if (CommUtil.compare(cplIn.getInterest(), BigDecimal.ZERO) != 0) {

			cplTaEventIn.setTrxn_amt(cplIn.getInterest());

			if (subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET) {
				// 资产类：贷 应收利息
				cplTaEventIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
				cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_RECEIVABLE.getValue());
			}
			else {
				// 负债类：借 应付利息
				cplTaEventIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
				cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_PAYABLE.getValue());
			}

			ApAccountApi.regAccountingEvent(cplTaEventIn);
		}

		/* 负债类账户应付利息类才有利息税，存放同业特殊需求也有利息税 */
		if (CommUtil.compare(cplIn.getInterest_tax(), BigDecimal.ZERO) != 0) {

			cplTaEventIn.setTrxn_amt(cplIn.getInterest_tax());
			cplTaEventIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_TAX.getValue());
			cplTaEventIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_WITH_HOLD_TAX"));

			ApAccountApi.regAccountingEvent(cplTaEventIn);
		}

		// 登记利息冲账事件
		DpPayedInstReversalIn reversalIn = BizUtil.getInstance(DpPayedInstReversalIn.class);

		reversalIn.setSub_acct_no(subAcct.getSub_acct_no());
		reversalIn.setAcct_no(subAcct.getAcct_no());
		reversalIn.setInst_paid(cplIn.getInterest()); // interest paid
		reversalIn.setInterest_tax(cplIn.getInterest_tax()); // interest tax
		reversalIn.setOriginal_trxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		reversalIn.setOriginal_trxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

		ApReversalApi.register("deptPaidInst", reversalIn);

		bizlog.method(" DpInterestAccounting.payInterestAccounting end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年7月13日-下午4:34:52</li>
	 *         <li>透支利息记账处理：不含对方账务</li>
	 *         <li>补充说明：调用此方法前需在外面判断是否为透支计息账户</li>
	 *         </p>
	 *         <p>
	 * @param cplInput
	 *            利息信息
	 * @param subAcct
	 *            子账户信息
	 */
	public static void receivableInterestAccounting(DpInstAccounting cplIn, DpaSubAccount subAcct) {

		// 冲账会是红字，所以判断不等于零即可， 透支利息没有利息税
		if (CommUtil.equals(cplIn.getInterest(), BigDecimal.ZERO)) {
			return;
		}

		bizlog.method(" DpInterestAccounting.receivableInterestAccounting begin >>>>>>>>>>>>>>>>");

		String summaryCode = CommUtil.nvl(cplIn.getSummary_code(), ApSystemParmApi.getSummaryCode("DEPT_PAY_INST"));

		// 登记会计记账事件
		ApAccountingEventIn cplTaEventIn = BizUtil.getInstance(ApAccountingEventIn.class);

		cplTaEventIn.setAccounting_alias(subAcct.getAccounting_alias());
		cplTaEventIn.setAccounting_subject(E_ACCOUNTINGSUBJECT.DEPOSIT);
		cplTaEventIn.setAcct_branch(subAcct.getSub_acct_branch());
		cplTaEventIn.setDouble_entry_ind(E_YESORNO.YES);
		cplTaEventIn.setTrxn_ccy(subAcct.getCcy_code());
		cplTaEventIn.setAcct_no(subAcct.getAcct_no());
		cplTaEventIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplTaEventIn.setProd_id(subAcct.getProd_id());
		cplTaEventIn.setSummary_code(summaryCode);
		cplTaEventIn.setSummary_name(ApSummaryApi.getText(summaryCode));
		cplTaEventIn.setTrxn_amt(cplIn.getInterest());
		cplTaEventIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
		cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_RECEIVABLE.getValue());

		ApAccountApi.regAccountingEvent(cplTaEventIn);

		// 登记透支利息冲账事件
		DpRecievedInstReversalIn reversalIn = BizUtil.getInstance(DpRecievedInstReversalIn.class);

		reversalIn.setSub_acct_no(subAcct.getSub_acct_no());
		reversalIn.setAcct_no(subAcct.getAcct_no());
		reversalIn.setInst_paid(cplIn.getInterest());
		reversalIn.setOriginal_trxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		reversalIn.setOriginal_trxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

		ApReversalApi.register("deptRecievedInst", reversalIn);

		bizlog.method(" DpInterestAccounting.receivableInterestAccounting end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年3月25日-下午4:21:53</li>
	 *         <li>功能说明：日终周期结息账户收息账务处理：先收息账户、再自身账户、最终挂待支付利息</li>
	 *         </p>
	 * @param cplIn
	 *            利息信息
	 * @param subAcct
	 *            子账户信息
	 */
	public static E_INCOMEINTERESTOBJECTTYPE analysisIncomeInterestObject(DpaInterest instAcct, DpaSubAccount subAcct) {

		E_INCOMEINTERESTOBJECTTYPE objectType = E_INCOMEINTERESTOBJECTTYPE.SELF;

		// 收息账户优先且收息账户不为空：先判断收息账户
		if (instAcct.getIncome_acct_prior() == E_INCOMEACCTPRIOR.INCOME_ACCT && CommUtil.isNotNull(subAcct.getIncome_inst_acct())) {

			// 收息币种
			String incomeCcy = CommUtil.nvl(subAcct.getIncome_inst_ccy(), subAcct.getCcy_code());

			try {
				// 存入支取标志，默认为空表示不检查冻结状态
				E_SAVEORWITHDRAWALIND saveDrawFlag = null;

				// 收息账户和待结息账户是同一主户，不检查收息账户冻结信息, 不相同则检查转入冻结限制
				if (!CommUtil.equals(subAcct.getAcct_no(), subAcct.getIncome_inst_acct())) {
					saveDrawFlag = E_SAVEORWITHDRAWALIND.SAVE;
				}

				DpPublicCheck.checkIncomeAcct(subAcct.getIncome_inst_acct(), incomeCcy, saveDrawFlag);
			}
			catch (Exception e) {

				// 系统异常抛错
				if (!(e instanceof LttsBusinessException)) {
					throw e;
				}

				// 定位收息账户出错，不报错且尝试入自身
				objectType = E_INCOMEINTERESTOBJECTTYPE.SELF;
			}

			// 入收息账户
			objectType = E_INCOMEINTERESTOBJECTTYPE.OTHER;
		}

		// 利息入自身处理
		if (objectType == E_INCOMEINTERESTOBJECTTYPE.SELF) {

			// 定期入待支付利息: 要求入自身的定期账户除外
			if (subAcct.getDd_td_ind() == E_DEMANDORTIME.TIME && instAcct.getIncome_acct_prior() != E_INCOMEACCTPRIOR.SELF) {
				objectType = E_INCOMEINTERESTOBJECTTYPE.WAIT;
			}

			// 定期联机交易利息不能入自身，免得定期支取时分析的支取类型因余额变动变得不准确
			if (subAcct.getDd_td_ind() == E_DEMANDORTIME.TIME && SysUtil.getCurrentSystemType() == SystemType.onl) {
				objectType = E_INCOMEINTERESTOBJECTTYPE.WAIT;
			}

			// 检查账户自身是否有转入冻结限制
			if (objectType != E_INCOMEINTERESTOBJECTTYPE.WAIT) {

				try {
					DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_SAVE, null);
				}
				catch (Exception e) {

					// 系统异常抛错
					if (!(e instanceof LttsBusinessException)) {
						throw e;
					}

					// 受限制出错，不报错则挂待支付利息
					objectType = E_INCOMEINTERESTOBJECTTYPE.WAIT;
				}
			}
		}

		return objectType;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年12月19日-上午10:04:54</li>
	 *         <li>功能说明：获得利息待结算信息</li>
	 *         <li>补充说明：适用活期账户和定期账户日终周期性结息</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param instAcct
	 *            账户计息信息
	 * @param roundRule
	 *            计息舍入规则
	 * @return 付息利息信息
	 */
	private static DpInstAccounting getInstSettleInfo(DpaSubAccount subAcct, DpaInterest instAcct, E_ROUNDRULE roundRule) {

		// 实例化利息和利息税输出结果
		DpInstAccounting cplOut = BizUtil.getInstance(DpInstAccounting.class);

		cplOut.setInterest(BigDecimal.ZERO);
		cplOut.setInterest_tax(BigDecimal.ZERO);
		cplOut.setInst_tax_rate(BigDecimal.ZERO);

		// 按舍入规则处理后的应计利息
		BigDecimal instValue = ApCurrencyApi.roundAmount(instAcct.getCcy_code(), instAcct.getAccrual_inst(), roundRule);

		// 待支取利息加应计利息大于零才有付息意义
		if (CommUtil.compare(subAcct.getInst_payable().add(instValue), BigDecimal.ZERO) > 0) {

			cplOut.setInterest(subAcct.getInst_payable().add(instValue));

			// 计算代扣利息税金额
			DpIntTaxInfo taxInfo = DpTaxApi.calcWithholdingTax(subAcct.getAcct_no(), subAcct.getSub_acct_no(), instValue);

			cplOut.setInterest_tax(subAcct.getInst_tax_payable().add(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), taxInfo.getAccrual_inst_tax())));
			cplOut.setInst_tax_rate(taxInfo.getInst_tax_rate());
		}

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年12月19日-上午10:04:54</li>
	 *         <li>功能说明：刷新账户层利息信息(入息方是待支取利息不能调用此方法)</li>
	 *         <li>补充说明：适用活期账户和定期账户日终周期性结息</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 * @param cplWaitSettleInst
	 *            本次结转利息
	 * @param cycPayFlag
	 *            周期付息标志
	 */
	private static void afreshAcctInfoForInterest(String acctNo, String subAcctNo, DpInstAccounting cplWaitSettleInst, E_YESORNO cycPayFlag) {

		// 结息里面会更新子户信息，安全起见先重新读取缓存
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		// 子账户信息是否需要更新
		boolean updateFlag = false;

		// 账户此处有待支取利息结转则需要清零
		if (CommUtil.compare(subAcct.getInst_payable(), BigDecimal.ZERO) != 0) {

			subAcct.setInst_payable(BigDecimal.ZERO);
			subAcct.setInst_tax_payable(BigDecimal.ZERO);
			subAcct.setCyc_inst_payable(BigDecimal.ZERO);
			subAcct.setCyc_inst_tax_payable(BigDecimal.ZERO);

			updateFlag = true;
		}

		// 周期性付息，登记周期支付利息，周期结息定期户提前支取冲账只能冲这一部分利息
		if (CommUtil.compare(cplWaitSettleInst.getInterest(), BigDecimal.ZERO) != 0 && cycPayFlag == E_YESORNO.YES) {

			subAcct.setCyc_inst_paid(subAcct.getCyc_inst_paid().add(cplWaitSettleInst.getInterest()));
			subAcct.setCyc_inst_withholding_tax(subAcct.getCyc_inst_withholding_tax().add(cplWaitSettleInst.getInterest_tax()));

			updateFlag = true;
		}

		// 更新子账户信息
		if (updateFlag) {
			DpaSubAccountDao.updateOne_odb1(subAcct);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2014年12月17日-上午10:04:54</li>
	 *         <li>功能说明：处理账户计息定义和利息明细记录</li>
	 *         <li>补充说明：适用活期账户和定期账户日终周期性结息</li>
	 *         </p>
	 * @param instAcct
	 *            账户计息信息
	 * @param roundRule
	 *            计息舍入规则
	 * @param intoObjectType
	 *            利息转入方类型
	 * @param cycPayFlag
	 *            周期性付息标志
	 */
	private static void processInterestRecord(DpaInterest instAcct, E_ROUNDRULE roundRule, E_INCOMEINTERESTOBJECTTYPE intoObjectType, E_YESORNO cycPayFlag) {

		bizlog.method(" DpInterestSettlement.processInterestRecord begin >>>>>>>>>>>>>>>>");

		// 结息里面会更新子户信息，安全起见先重新读取缓存
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(instAcct.getAcct_no(), instAcct.getSub_acct_no(), true);

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		boolean slipFlag = DpTimeInterestApi.judgeSlipCain(subAcct, trxnDate);

		// 日终结息当前积数、利息是必然要清零的
		instAcct.setCur_term_inst(BigDecimal.ZERO);
		instAcct.setCur_term_inst_tax(BigDecimal.ZERO);
		instAcct.setCur_term_inst_sum_bal(BigDecimal.ZERO);
		instAcct.setInst_day(0L);
		instAcct.setLast_inst_oper_type(E_INSTOPERATE.PAY);

		List<DpaTimeSlip> listSlip = new ArrayList<DpaTimeSlip>();

		if (slipFlag) {

			listSlip = SqlDpTimeSlipBasicDao.selNoClearTimeSlipList(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_ACCTSTATUS.NORMAL, subAcct.getOrg_id(), false);

			for (DpaTimeSlip ficheInst : listSlip) {

				// 日终结息当前积数、利息是必然要清零的
				ficheInst.setCur_term_inst(BigDecimal.ZERO);
				ficheInst.setCur_term_inst_tax(BigDecimal.ZERO);
				ficheInst.setCur_term_inst_sum_bal(BigDecimal.ZERO);
				ficheInst.setLast_inst_oper_type(E_INSTOPERATE.PAY);
			}
		}

		// 周期性结息， 更新上次结息日和下次结息日
		if (cycPayFlag == E_YESORNO.YES && CommUtil.isNotNull(instAcct.getPay_inst_cyc())) {

			// 下次付息日
			String nextPayInstDate = DpToolsApi.calcDateByReference(instAcct.getPay_inst_ref_date(), trxnDate, instAcct.getPay_inst_cyc(), instAcct.getStart_inst_date());

			instAcct.setNext_pay_inst_date(nextPayInstDate);
			instAcct.setLast_pay_inst_date(trxnDate);
		}

		// 账户应计利息明细处理: 待支取利息或应计利息字段任意不为零就有利息明细要处理
		if (!CommUtil.equals(subAcct.getInst_payable(), BigDecimal.ZERO) || !CommUtil.equals(instAcct.getAccrual_inst(), BigDecimal.ZERO)) {

			// 利息不是入待支取利息
			if (intoObjectType != E_INCOMEINTERESTOBJECTTYPE.WAIT) {
				DpInterestBasicApi.accruedIntoPayed(subAcct, instAcct);
			}
			else {
				DpInterestBasicApi.accruedSwitchWait(subAcct, cycPayFlag);
			}

			// 各层卡片利息明细处理及更新卡片信息后提交数据库
			for (DpaTimeSlip ficheInst : listSlip) {

				if (intoObjectType != E_INCOMEINTERESTOBJECTTYPE.WAIT) {

					DpTimeInterestApi.accruedIntoPayed(ficheInst);

					ficheInst.setAccrual_inst(BigDecimal.ZERO);
					ficheInst.setAccrual_inst_tax(BigDecimal.ZERO);
					ficheInst.setAccrual_sum_bal(BigDecimal.ZERO);

					regFichePayInterestTrxnRecord(ficheInst, roundRule, intoObjectType);
				}
				else {

					DpTimeInterestApi.accruedSwitchWait(ficheInst);
				}
			}
		}

		// 按照舍入方式处理账户计息定义表, 应计利息恰好为零也需要清空积数，因为零利率计息也会累计积数
		if (E_ROUNDRULE.ROUND == roundRule || CommUtil.equals(instAcct.getAccrual_inst(), BigDecimal.ZERO)) {

			// 应计利息字段清零
			instAcct.setAccrual_inst(BigDecimal.ZERO);
			instAcct.setAccrual_inst_tax(BigDecimal.ZERO);
			instAcct.setAccrual_sum_bal(BigDecimal.ZERO);
		}
		else if (E_ROUNDRULE.DOWN == roundRule) {

			// 按舍入规则处理后的应计利息和利息税
			BigDecimal instValue = ApCurrencyApi.roundAmount(instAcct.getCcy_code(), instAcct.getAccrual_inst(), roundRule);
			BigDecimal taxValue = ApCurrencyApi.roundAmount(instAcct.getCcy_code(), instAcct.getAccrual_inst_tax(), roundRule);
			BigDecimal radio = (instAcct.getAccrual_inst().subtract(instValue)).divide(instAcct.getAccrual_inst(), 12, RoundingMode.HALF_UP);

			// 截位处理总应计利息信息
			instAcct.setAccrual_inst(instAcct.getAccrual_inst().subtract(instValue));
			instAcct.setAccrual_inst_tax(instAcct.getAccrual_inst_tax().subtract(taxValue));
			instAcct.setAccrual_sum_bal(ApCurrencyApi.roundAmount(instAcct.getCcy_code(), instAcct.getAccrual_sum_bal().multiply(radio)));
			// TODO: 是否登记截位后的零头利息明细需要考虑性能
		}

		bizlog.method(" DpInterestSettlement.processInterestRecord end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年12月19日-上午10:04:54</li>
	 *         <li>功能说明：登记卡片付息交易记录</li>
	 *         </p>
	 * @param ficheInst
	 *            卡片信息
	 * @param roundRule
	 *            计息舍入规则
	 * @param intoObjectType
	 *            利息转入方类型
	 */
	private static void regFichePayInterestTrxnRecord(DpaTimeSlip timeSlip, E_ROUNDRULE roundRule, E_INCOMEINTERESTOBJECTTYPE intoObjectType) {

		// 前面已经处理待支取利息
		if (intoObjectType == E_INCOMEINTERESTOBJECTTYPE.WAIT) {
			return;
		}

		// 应计利息字段精度处理
		BigDecimal instValue = ApCurrencyApi.roundInterest(timeSlip.getCcy_code(), timeSlip.getAccrual_inst(), roundRule);
		BigDecimal taxValue = ApCurrencyApi.roundInterest(timeSlip.getCcy_code(), timeSlip.getAccrual_inst_tax(), roundRule);

		DpTimeSlipInstTrxnRegister cplInstIn = BizUtil.getInstance(DpTimeSlipInstTrxnRegister.class);

		cplInstIn.setInst_tax_rate(timeSlip.getLast_prov_inst_tax_rate());
		cplInstIn.setInst_withholding_tax(timeSlip.getInst_tax_payable().add(taxValue));
		cplInstIn.setInterest(timeSlip.getInst_payable().add(instValue));

		// 先清零待支取利息字段
		timeSlip.setInst_payable(BigDecimal.ZERO);
		timeSlip.setInst_tax_payable(BigDecimal.ZERO);

		timeSlip.setAccrual_inst(BigDecimal.ZERO);
		timeSlip.setAccrual_inst_tax(BigDecimal.ZERO);
		timeSlip.setAccrual_sum_bal(BigDecimal.ZERO);
		timeSlip.setCur_term_inst(BigDecimal.ZERO);
		timeSlip.setCur_term_inst_sum_bal(BigDecimal.ZERO);
		timeSlip.setCur_term_inst_tax(BigDecimal.ZERO);
		timeSlip.setLast_inst_oper_type(E_INSTOPERATE.PAY);

		// TODO: 如何合理布局

		// 登记卡片利息交易明细并更新卡片账信息
		DpTimeInterestApi.regInterestTrxnDetail(cplInstIn);
	}
}