package cn.sunline.icore.dp.serv.interest;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAccountApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.type.ComApAccounting.ApAccountingEventIn;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseConst;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.api.DpXpGoldApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpExperienceGold.DpaXpGold;
import cn.sunline.icore.dp.base.tables.TabDpExperienceGold.DpaXpGoldDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpXpGoldBase.DpXpGoldInfoInqueryIn;
import cn.sunline.icore.dp.base.type.ComDpXpGoldBase.DpXpGoldInfoInqueryOut;
import cn.sunline.icore.dp.base.type.ComDpXpGoldBase.DpXpGoldInterestSettleIn;
import cn.sunline.icore.dp.base.type.ComDpXpGoldBase.DpXpGoldInterestSettleOut;
import cn.sunline.icore.dp.base.type.ComDpXpGoldBase.DpXpGoldLayerInfo;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_PROFITPAYMETHOD;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_XPGOLDSTATUS;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInterestDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpXpGoldInfo;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpXpGoldProfitInfo;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUNTINGSUBJECT;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_BALPROPERTY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 体验金相关
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2019年6月25日-下午1:30:55</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpXpGold {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpXpGold.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月25日-下午6:57:30</li>
	 *         <li>功能说明：体验金自动派息</li>
	 *         </p>
	 * @param xpGoldNo
	 *            体验金编号
	 */
	public static void autoPaymentInterest(String xpGoldNo) {
		bizlog.method(" DpXpGold.autoPaymentInterest begin ");

		// 带锁读体验金台账
		DpaXpGold xpGold = DpaXpGoldDao.selectOneWithLock_odb1(xpGoldNo, true);

		if (xpGold.getXp_gold_status() != E_XPGOLDSTATUS.USING) {
			return;
		}

		if (xpGold.getProfit_pay_method() != E_PROFITPAYMETHOD.AUTO) {
			return;
		}

		if (xpGold.getProfit_payable_ind() != E_YESORNO.YES) {
			return;
		}

		if (CommUtil.isNull(xpGold.getIncome_inst_acct())) {
			return;
		}

		BizUtil.resetTrxnSequence();

		// 利息信息
		DpInstAccounting cplInstInfo = DpXpGoldApi.xpGoldSettlement(xpGold);

		// 利息记账金额不为零且不是入待支取利息则进行账务处理
		if (CommUtil.compare(cplInstInfo.getInterest(), BigDecimal.ZERO) != 0) {

			// （一）借：应付利息，贷：代扣利息税
			payInterestAccounting(cplInstInfo, xpGold);

			// （二）贷：收息账号
			instIntoIncomeAcct(cplInstInfo, xpGold);
		}

		bizlog.method(" DpXpGold.autoPaymentInterest end ");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月25日-下午6:57:30</li>
	 *         <li>功能说明：体验金手工领取利息</li>
	 *         </p>
	 * @param cplIn
	 *            体验金手工派息输入
	 */
	public static DpXpGoldInterestSettleOut xpGoldHandSttlement(DpXpGoldInterestSettleIn cplIn) {

		BizUtil.fieldNotNull(cplIn.getIncome_inst_acct(), DpBaseDict.A.income_inst_acct.getId(),  DpBaseDict.A.income_inst_acct.getLongName());
		
		// 输出
		DpXpGoldInterestSettleOut cplOut = BizUtil.getInstance(DpXpGoldInterestSettleOut.class);

		// 利息记账信息
		DpInstAccounting cplInstInfo = BizUtil.getInstance(DpInstAccounting.class);

		cplInstInfo.setInterest(BigDecimal.ZERO);
		cplInstInfo.setInterest_tax(BigDecimal.ZERO);

		if (CommUtil.isNotNull(cplIn.getXp_gold_no())) {

			// 带锁读体验金台账
			DpaXpGold xpGold = DpaXpGoldDao.selectOneWithLock_odb1(cplIn.getXp_gold_no(), false);

			if (xpGold == null) {
				throw DpBaseErr.DpBase.E0082(cplIn.getXp_gold_no());
			}

			if (xpGold.getXp_gold_status() != E_XPGOLDSTATUS.USING) {
				throw DpBaseErr.DpBase.E0083(cplIn.getXp_gold_no());
			}

			if (xpGold.getProfit_payable_ind() != E_YESORNO.YES) {
				throw DpBaseErr.DpBase.E0092(cplIn.getXp_gold_no());
			}

			// 利息信息
			cplInstInfo = DpXpGoldApi.xpGoldSettlement(xpGold);

			// 利息记账金额不为零且不是入待支取利息则进行账务处理
			if (CommUtil.compare(cplInstInfo.getInterest(), BigDecimal.ZERO) != 0) {

				xpGold.setIncome_inst_acct(cplIn.getIncome_inst_acct());
				xpGold.setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), xpGold.getCcy_code()));

				// （一）借：应付利息，贷：代扣利息税
				payInterestAccounting(cplInstInfo, xpGold);

				// （二）贷：收息账号
				instIntoIncomeAcct(cplInstInfo, xpGold);
			}

			// 输出
			cplOut.setCcy_code(xpGold.getCcy_code());
			cplOut.setInterest(cplInstInfo.getInterest());
			cplOut.setInterest_tax(cplInstInfo.getInterest_tax());
			cplOut.setProfit(cplInstInfo.getInterest().subtract(cplInstInfo.getInterest_tax()));
		}
		else {

			List<DpaXpGold> listGold = DpaXpGoldDao.selectAll_odb3(cplIn.getCust_no(), E_XPGOLDSTATUS.USING, E_YESORNO.YES, false);

			if (CommUtil.isNull(listGold) || listGold.size() == 0) {
				throw DpBaseErr.DpBase.E0093();
			}

			for (DpaXpGold xpGold : listGold) {

				xpGold = DpaXpGoldDao.selectOneWithLock_odb1(cplIn.getXp_gold_no(), true);

				if (xpGold.getXp_gold_status() != E_XPGOLDSTATUS.USING) {
					break;
				}

				if (xpGold.getProfit_payable_ind() != E_YESORNO.YES) {
					break;
				}

				// 利息信息
				DpInstAccounting cplSingleInstInfo = DpXpGoldApi.xpGoldSettlement(xpGold);

				cplInstInfo.setInst_tax_rate(cplSingleInstInfo.getInst_tax_rate());
				cplInstInfo.setInterest(cplInstInfo.getInterest().add(cplSingleInstInfo.getInterest()));
				cplInstInfo.setInterest_tax(cplInstInfo.getInterest_tax().add(cplSingleInstInfo.getInterest_tax()));
				cplInstInfo.setSummary_code(cplSingleInstInfo.getSummary_code());

				// 利息记账金额不为零且不是入待支取利息则进行账务处理
				if (CommUtil.compare(cplSingleInstInfo.getInterest(), BigDecimal.ZERO) != 0) {

					// （一）借：应付利息，贷：代扣利息税
					payInterestAccounting(cplSingleInstInfo, xpGold);
				}
			}

			// （二）贷：收息账号
			if (CommUtil.compare(cplInstInfo.getInterest(), BigDecimal.ZERO) != 0) {

				listGold.get(0).setIncome_inst_acct(cplIn.getIncome_inst_acct());
				listGold.get(0).setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), listGold.get(0).getCcy_code()));

				instIntoIncomeAcct(cplInstInfo, listGold.get(0));
			}

			cplOut.setCcy_code(listGold.get(0).getCcy_code());
			cplOut.setInterest(cplInstInfo.getInterest());
			cplOut.setInterest_tax(cplInstInfo.getInterest_tax());
			cplOut.setProfit(cplInstInfo.getInterest().subtract(cplInstInfo.getInterest_tax()));
		}

		return cplOut;
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
	 * @param xpGold
	 *            体验金信息
	 */
	private static void payInterestAccounting(DpInstAccounting cplInstInfo, DpaXpGold xpGold) {

		// 冲账会是红字，所以判断不等于零即可
		if (CommUtil.equals(cplInstInfo.getInterest(), BigDecimal.ZERO) && CommUtil.equals(cplInstInfo.getInterest_tax(), BigDecimal.ZERO)) {
			return;
		}

		bizlog.method(" DpXpGold.payInterestAccounting begin >>>>>>>>>>>>>>>>");

		String summaryCode = ApSystemParmApi.getSummaryCode("DEPT_PAY_INST");

		// 登记会计记账事件
		ApAccountingEventIn cplTaEventIn = BizUtil.getInstance(ApAccountingEventIn.class);

		cplTaEventIn.setAccounting_alias(xpGold.getAccounting_alias());
		cplTaEventIn.setAccounting_subject(E_ACCOUNTINGSUBJECT.DEPOSIT);
		cplTaEventIn.setAcct_branch(xpGold.getSub_acct_branch());
		cplTaEventIn.setDouble_entry_ind(E_YESORNO.YES);
		cplTaEventIn.setTrxn_ccy(xpGold.getCcy_code());
		cplTaEventIn.setAcct_no(xpGold.getXp_gold_no());
		cplTaEventIn.setSub_acct_seq(ApBusinessParmApi.getValue(DpBaseConst.MIN_SUB_ACCT_SEQ));
		cplTaEventIn.setProd_id(xpGold.getProd_id());
		cplTaEventIn.setSummary_code(summaryCode);
		cplTaEventIn.setSummary_name(ApSummaryApi.getText(summaryCode));

		/* 会计入账接口信息: 利息处理, 用不等于符号，是为了便于红字冲账时调用 */
		if (CommUtil.compare(cplInstInfo.getInterest(), BigDecimal.ZERO) != 0) {

			cplTaEventIn.setTrxn_amt(cplInstInfo.getInterest());
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_PAYABLE.getValue());

			ApAccountApi.regAccountingEvent(cplTaEventIn);
		}

		// 负债类账户利息税
		if (CommUtil.compare(cplInstInfo.getInterest_tax(), BigDecimal.ZERO) != 0) {

			cplTaEventIn.setTrxn_amt(cplInstInfo.getInterest_tax());
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_TAX.getValue());
			cplTaEventIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_WITH_HOLD_TAX"));

			ApAccountApi.regAccountingEvent(cplTaEventIn);
		}

		bizlog.method(" DpXpGold.payInterestAccounting end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月25日-下午4:21:53</li>
	 *         <li>功能说明：利息入指定账户</li>
	 *         </p>
	 * @param cplIn
	 *            付息结果
	 * @param xpGold
	 *            体验金信息
	 */
	private static void instIntoIncomeAcct(DpInstAccounting cplIn, DpaXpGold xpGold) {

		bizlog.method(" DpXpGold.instIntoIncomeAcct begin >>>>>>>>>>>>>>>>");

		// 利息和利息税都为零，则直接退出: 红字冲账时利息值可能为负数
		if (CommUtil.equals(cplIn.getInterest(), BigDecimal.ZERO) && CommUtil.equals(cplIn.getInterest_tax(), BigDecimal.ZERO)) {
			bizlog.method(" DpXpGold.instIntoIncomeAcct end <<<<<<<<<<<<<<<<");
			return;
		}

		String summaryCode = ApSystemParmApi.getSummaryCode("DEPT_PAY_INST");

		// 税后利息
		BigDecimal instAfterTax = cplIn.getInterest().subtract(cplIn.getInterest_tax());

		DpAcctAccessIn cplAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		cplAccessIn.setAcct_no(xpGold.getIncome_inst_acct());
		cplAccessIn.setCcy_code(xpGold.getIncome_inst_ccy());
		cplAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

		DpAcctAccessOut cplAccessOut = DpToolsApi.locateSingleSubAcct(cplAccessIn);

		// 要带锁查询，后面直接记账了
		DpaSubAccount appointSubAcct = DpaSubAccountDao.selectOneWithLock_odb1(cplAccessOut.getAcct_no(), cplAccessOut.getSub_acct_no(), true);

		// 本金记账
		DpUpdAccBalIn cplUpdBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplUpdBalIn.setCard_no(cplAccessOut.getCard_no()); // 卡号
		cplUpdBalIn.setAcct_no(appointSubAcct.getAcct_no()); // 账号
		cplUpdBalIn.setSub_acct_no(appointSubAcct.getSub_acct_no()); // 子账号
		cplUpdBalIn.setTrxn_event_id(""); // 交易事件ID
		cplUpdBalIn.setTrxn_ccy(xpGold.getIncome_inst_ccy()); // 交易币种
		cplUpdBalIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
		cplUpdBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplUpdBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplUpdBalIn.setOpp_acct_route(E_ACCOUTANALY.INSIDE);
		cplUpdBalIn.setOpp_acct_ccy(xpGold.getCcy_code());
		cplUpdBalIn.setOpp_trxn_amt(instAfterTax);
		cplUpdBalIn.setOpp_branch_id(xpGold.getSub_acct_branch());
		cplUpdBalIn.setSummary_code(summaryCode); // 摘要代码
		cplUpdBalIn.setDebit_credit(E_DEBITCREDIT.CREDIT); // 记账方向
		cplUpdBalIn.setTrxn_amt(instAfterTax); // 净利息
		cplUpdBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别

		DpAccounting.online(cplUpdBalIn);

		bizlog.method(" DpXpGold.instIntoIncomeAcct <<<<<<<<<<<<<<<<");
	}
	
	/**
	 * @author lijiawei
	 *         <p>
	 *         <li>2019年10月28日-下午4:01:29</li>
	 *         <li>功能说明：体验金信息查询</li>
	 *         </p>
	 * @param cplIn 体验金信息查询输入
	 * 
	 * @return 体验金详细信息
	 * 
	 */
	public static DpXpGoldInfoInqueryOut xpGoldInfoQuery(DpXpGoldInfoInqueryIn cplIn){
		
		bizlog.method(" DpXpGoldInquery.xpGoldInfoQuery begin");
		
		BizUtil.fieldNotNull(cplIn.getXp_gold_no(), DpBaseDict.A.xp_gold_no.getId(), DpBaseDict.A.xp_gold_no.getLongName());
		
		String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();
		
		//输出体验金详细信息
		DpXpGoldInfoInqueryOut cplOut = BizUtil.getInstance(DpXpGoldInfoInqueryOut.class);
		
		//查询体验金基本详情
		DpXpGoldInfo cplInfo = SqlDpInterestDao.selXpGoldInfo(orgId, cplIn.getXp_gold_no(), false);
		
		if(cplInfo != null){
			cplOut.setCcy_code(cplInfo.getCcy_code());
			cplOut.setCust_no(cplInfo.getCust_no());
			cplOut.setEnd_inst_date(cplInfo.getEnd_inst_date());
			cplOut.setXp_gold(cplInfo.getXp_gold());
			cplOut.setExpiry_date(cplInfo.getExpiry_date());
			cplOut.setTerm_code(cplInfo.getTerm_code());
			cplOut.setProd_id(cplInfo.getProd_id());
			cplOut.setStart_inst_date(cplInfo.getStart_inst_date());
			cplOut.setProfit_payable_ind(cplInfo.getProfit_payable_ind());
			cplOut.setXp_gold_status(cplInfo.getXp_gold_status());
			cplOut.setProfit_pay_method(cplInfo.getProfit_pay_method());
			cplOut.setIncome_inst_acct(cplInfo.getIncome_inst_acct());
			cplOut.setIncome_inst_ccy(cplInfo.getIncome_inst_ccy());
			cplOut.setRemark(cplInfo.getRemark());
			cplOut.setInst_paid(cplInfo.getInst_paid());
			cplOut.setInst_tax_rate(cplInfo.getInst_tax_rate());
			cplOut.setInst_withholding_tax(cplInfo.getInst_withholding_tax());
		}
		else{
			throw DpBaseErr.DpBase.E0082(cplIn.getXp_gold_no());
		}
		
		//查询体验金已产生和可提取收益
		DpXpGoldProfitInfo cplProfitInfo = SqlDpInterestDao.selXpGoldProfitInfo(orgId, cplIn.getXp_gold_no(), false);
		
		// 有查询到体验金信息赋值统计信息
	    if (cplProfitInfo != null) {
	    	
	    	cplOut.setAlready_calc_profit(cplProfitInfo.getAlready_calc_profit());
			cplOut.setProfit_payable(cplProfitInfo.getProfit_payable());
					
		}
		else {		
			cplOut.setAlready_calc_profit(BigDecimal.ZERO);	
			cplOut.setProfit_payable(BigDecimal.ZERO);	
		}
	    
	    //查询分层利率信息
	    DpXpGoldLayerInfo cplLayerInfo = SqlDpInterestDao.selXpGoldLayerInfo(orgId, cplIn.getXp_gold_no(), false);
	    
	    if(cplLayerInfo != null){
	    	
	    	//计算体验金收益
		    BigDecimal profitPaid = BigDecimal.ZERO;
			BigDecimal profitWaitPay = BigDecimal.ZERO;
				
			profitPaid = cplInfo.getInst_paid().subtract(cplInfo.getInst_withholding_tax());
			profitWaitPay = ApCurrencyApi.roundAmount(cplInfo.getCcy_code(), cplInfo.getAccrual_inst()).subtract(
					ApCurrencyApi.roundAmount(cplInfo.getCcy_code(), cplInfo.getAccrual_inst_tax()));
		 
			cplLayerInfo.setProfit(profitPaid.add(profitWaitPay));
			
			//输出分层利率信息
			cplOut.getList_info().add(cplLayerInfo);
			
			return cplOut;
	    }
        DpXpGoldLayerInfo cplLayerInfoOut = BizUtil.getInstance(DpXpGoldLayerInfo.class);
	    
	    cplLayerInfoOut.setBand_amount(BigDecimal.ZERO);
	    cplLayerInfoOut.setEfft_inrt(BigDecimal.ZERO);
	    cplLayerInfoOut.setLayer_inst_bal(BigDecimal.ZERO);
	    cplLayerInfoOut.setLayer_no(0L);
	    cplLayerInfoOut.setProfit(BigDecimal.ZERO);
	    cplLayerInfoOut.setSeg_inst(BigDecimal.ZERO);
	    cplLayerInfoOut.setSeg_inst_tax(BigDecimal.ZERO);
	    	
	    cplOut.getList_info().add(cplLayerInfoOut);
		
	    bizlog.method(" DpXpGoldInquery.xpGoldInfoQuery end");
		return cplOut;
	}
}
