package cn.sunline.icore.dp.serv.callback;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAccountApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.type.ComApAccounting.ApAccountingEventIn;
import cn.sunline.icore.ap.type.ComApAccounting.ApRecordAccure;
import cn.sunline.icore.ap.type.ComApAccounting.ApRegLedgerBal;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.plugin.DpAccountingPlugin;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.type.ComDpAccountBase.DpAccountingEventAidIn;
import cn.sunline.icore.dp.base.type.ComDpInterestTrxnBasic.DpAccrualExtractData;
import cn.sunline.icore.dp.base.type.ComDpInterestTrxnBasic.DpLastBalanceData;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUNTINGSUBJECT;
import cn.sunline.icore.sys.type.EnumType.E_ACCRUETYPE;
import cn.sunline.icore.sys.type.EnumType.E_BALPROPERTY;
import cn.sunline.ltts.biz.global.CommUtil;

/**
 * <p>
 * 文件功能说明：会计登记IOBUS服务底层调用扩展点
 * </p>
 * 
 * @Author 周明易
 *         <p>
 *         <li>2019年3月29日-下午14:35:50</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>2019年3月29日-周明易：存款模块透支额度相关扩展点</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAccountingPluginImpl implements DpAccountingPlugin {

	/**
	 * 分户账余额汇总登记
	 * 
	 * @param cplData
	 *            上日余额数据
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void ledgerBalRegister(DpLastBalanceData cplData) {

		ApRegLedgerBal cplBalRecord = BizUtil.getInstance(ApRegLedgerBal.class);

		cplBalRecord.setAccounting_alias(cplData.getAccounting_alias());
		cplBalRecord.setAcct_branch(cplData.getSub_acct_branch());
		cplBalRecord.setCcy_code(cplData.getCcy_code());
		cplBalRecord.setAccounting_subject(E_ACCOUNTINGSUBJECT.DEPOSIT);

		// B - 负债类
		if (cplData.getAsst_liab_ind() == E_ASSETORDEBT.DEBT) {

			cplBalRecord.setBal_attributes(E_BALPROPERTY.DEPOSIT.getValue()); // 负债本金
			cplBalRecord.setAcct_bal(cplData.getCredit_acct_bal().add(cplData.getAcct_float_bal()));
			cplBalRecord.setBal_type(E_DEBITCREDIT.CREDIT); // 余额方向为贷方

			// 登记负债本金数据
			ApAccountApi.regLedgerBal(cplBalRecord);

			// 资产本金: 大部分账户都是无资产本金的，只有透支导致余额为负数才登记
			if (CommUtil.compare(cplData.getDebit_acct_bal(), BigDecimal.ZERO) < 0) {

				cplBalRecord.setBal_attributes(E_BALPROPERTY.CAPITAL.getValue()); // 资产本金
				cplBalRecord.setAcct_bal(cplData.getDebit_acct_bal().abs());
				cplBalRecord.setBal_type(E_DEBITCREDIT.DEBIT); // 余额方向为借方

				// 登记透支资产本金数据
				ApAccountApi.regLedgerBal(cplBalRecord);
			}
		}
		// A - 资产类
		else {

			cplBalRecord.setBal_attributes(E_BALPROPERTY.CAPITAL.getValue()); // 资产本金
			cplBalRecord.setAcct_bal(cplData.getCredit_acct_bal());
			cplBalRecord.setBal_type(E_DEBITCREDIT.DEBIT); // 余额方向为借方

			// 登记资产本金数据
			ApAccountApi.regLedgerBal(cplBalRecord);

			// 负债本金: 目前存放同业是不能透支的，所以资产类不可能出现透支的情况，此处列出来是为了逻辑完整性
			if (CommUtil.compare(cplData.getDebit_acct_bal(), BigDecimal.ZERO) < 0) {

				cplBalRecord.setBal_attributes(E_BALPROPERTY.DEPOSIT.getValue()); // 负债本金
				cplBalRecord.setAcct_bal(cplData.getDebit_acct_bal().abs());
				cplBalRecord.setBal_type(E_DEBITCREDIT.CREDIT); // 余额方向为贷方

				// 登记透支负债本金数据
				ApAccountApi.regLedgerBal(cplBalRecord);
			}
		}
	}

	/**
	 * 应计利息汇总登记
	 * 
	 * @param cplData
	 *            应计利息数据
	 * @param accrueType
	 *            应计利息类型
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void accrueExtractRegister(DpAccrualExtractData cplData, E_ACCRUETYPE accrueType) {

		ApRecordAccure cplAccuralRecord = BizUtil.getInstance(ApRecordAccure.class);

		cplAccuralRecord.setAcct_branch(cplData.getSub_acct_branch());
		cplAccuralRecord.setAccounting_alias(cplData.getAccounting_alias());
		cplAccuralRecord.setCcy_code(cplData.getCcy_code());
		cplAccuralRecord.setBudget_inst_amt(cplData.getLast_prov_inst());

		if (cplData.getAsst_liab_ind() == E_ASSETORDEBT.DEBT) {

			cplAccuralRecord.setAccrue_type(accrueType); // 应计利息类型

			// 应付利息
			if (accrueType == E_ACCRUETYPE.DEPOSIT_INTEREST_PAYBLE) {
				cplAccuralRecord.setBal_attributes(E_BALPROPERTY.INTEREST_PAYABLE.getValue()); // 应付利息
			}
			// 应收利息
			else if (accrueType == E_ACCRUETYPE.LOAN_INTEREST_RECEIVABLE) {
				cplAccuralRecord.setBal_attributes(E_BALPROPERTY.INTEREST_RECEIVABLE.getValue());
			}
			// 逾期利息
			else if (accrueType == E_ACCRUETYPE.LOAN_LOST_PROVISION) {
				cplAccuralRecord.setBal_attributes(E_BALPROPERTY.BAD_INTEREST_RECEIVABLE.getValue());
			}
		}
		else {
			// 应收利息
			cplAccuralRecord.setAccrue_type(E_ACCRUETYPE.LOAN_INTEREST_RECEIVABLE); // 应收利息计提
			cplAccuralRecord.setBal_attributes(E_BALPROPERTY.INTEREST_RECEIVABLE.getValue()); // 应收利息
		}

		// 登记计提会计数据
		ApAccountApi.regAccure(cplAccuralRecord);
	}

	/**
	 * 本金会计事件登记
	 * 
	 * @param subAcct
	 *            子账户信息
	 * @param cplIn
	 *            会计登记辅助信息
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void accountingEventRegister(DpaSubAccount subAcct, DpAccountingEventAidIn cplIn) {

		// 登记会计记账事件
		ApAccountingEventIn cplTaEventIn = BizUtil.getInstance(ApAccountingEventIn.class);

		cplTaEventIn.setAccounting_alias(subAcct.getAccounting_alias());
		cplTaEventIn.setAccounting_subject(E_ACCOUNTINGSUBJECT.DEPOSIT);
		cplTaEventIn.setAcct_branch(subAcct.getSub_acct_branch());
		cplTaEventIn.setDebit_credit(cplIn.getDebit_credit());
		cplTaEventIn.setDouble_entry_ind(E_YESORNO.YES);
		cplTaEventIn.setTrxn_ccy(subAcct.getCcy_code());
		cplTaEventIn.setTrxn_amt(cplIn.getTrxn_amt());
		cplTaEventIn.setAcct_no(subAcct.getAcct_no());
		cplTaEventIn.setProd_id(subAcct.getProd_id());
		cplTaEventIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplTaEventIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		// TODO:
		// cplTaEventIn.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		cplTaEventIn.setSummary_code(cplIn.getSummary_code());
		cplTaEventIn.setSummary_name(ApSummaryApi.getText(cplIn.getSummary_code()));

		if (cplIn.getAsst_liab_ind() == E_ASSETORDEBT.DEBT) {
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.DEPOSIT.getValue());
		}
		else {
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.CAPITAL.getValue());

		}

		ApAccountApi.regAccountingEvent(cplTaEventIn);
	}

	/**
	 * 单账户计提会计事件登记
	 * 
	 * @param subAcct
	 *            子账户信息
	 * @param dailyAppendInst
	 *            每日增量利息值
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void singleAccrueRegister(DpaSubAccount subAcct, BigDecimal dailyAppendInst) {

		String summaryCode = ApSystemParmApi.getSummaryCode("DEPT_PAY_INST");

		// 登记会计记账事件
		ApAccountingEventIn cplTaEventIn = BizUtil.getInstance(ApAccountingEventIn.class);

		cplTaEventIn.setAccounting_alias(subAcct.getAccounting_alias());
		cplTaEventIn.setAccounting_subject(E_ACCOUNTINGSUBJECT.DEPOSIT);
		cplTaEventIn.setAcct_branch(subAcct.getSub_acct_branch());
		cplTaEventIn.setDouble_entry_ind(E_YESORNO.YES);
		cplTaEventIn.setTrxn_ccy(subAcct.getCcy_code());
		cplTaEventIn.setTrxn_amt(dailyAppendInst);
		cplTaEventIn.setAcct_no(subAcct.getAcct_no());
		cplTaEventIn.setProd_id(subAcct.getProd_id());
		cplTaEventIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplTaEventIn.setTrxn_date(BizUtil.getTrxRunEnvs().getLast_date());
		cplTaEventIn.setSummary_code(summaryCode);
		cplTaEventIn.setSummary_name(ApSummaryApi.getText(summaryCode));

		// 对于负债类
		if (subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT) {

			cplTaEventIn.setAccrue_type(E_ACCRUETYPE.DEPOSIT_INTEREST_PAYBLE);

			// 借：利息支出
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_PAY.getValue());

			ApAccountApi.regAccountingEvent(cplTaEventIn);

			// 贷：应付利息
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_PAYABLE.getValue());

			ApAccountApi.regAccountingEvent(cplTaEventIn);
		}
		// 对于资产类
		else {

			cplTaEventIn.setAccrue_type(E_ACCRUETYPE.LOAN_INTEREST_RECEIVABLE);

			// 借：应收利息
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_RECEIVABLE.getValue()); 

			ApAccountApi.regAccountingEvent(cplTaEventIn);

			// 贷：利息收入
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_INCOME.getValue());

			ApAccountApi.regAccountingEvent(cplTaEventIn);
		}
	}
}
