package cn.sunline.icore.dp.serv.interest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpRateBasicApi;
import cn.sunline.icore.dp.base.api.DpTaxApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRate;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRateDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpsPayedInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpsPayedInterestDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryIn;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryOut;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstDetailRegister;
import cn.sunline.icore.dp.base.type.ComDpTaxBase.DpIntTaxInfo;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTADJUSTTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTOPERATE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpCashIobus;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpInterestAdjustmentIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpInterestAdjustmentOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAccountRouteInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCashAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpSuspenseAccountingIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_ADJUSTWAY;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ADDSUBTRACT;
import cn.sunline.icore.sys.type.EnumType.E_FOREXEXCHOBJECT;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpInterestHandAdjust {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpInterestHandAdjust.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年9月14日-下午3:53:52</li>
	 *         <li>功能说明：存款账户利息调整</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpInterestAdjustmentOut depositInterestAdjustment(DpInterestAdjustmentIn cplIn) {
		bizlog.method(" DpInterestHandAdjust.DepositInterestAdjustment begin >>>>>>>>>>>>>>>>");
		bizlog.debug(" cplIn >>>>>>>>>>>>>>>>[%s]", cplIn);

		// 检查输入数据合法性
		validateInputData(cplIn);
		// 取账号信息: 带锁
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);
		// 加载账户数据区
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));
		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(acctInfo.getCust_no(), acctInfo.getCust_type());

		// 检查账户相关信息
		DpaSubAccount subAcct = checkAcctData(cplIn);

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 利息信息结果
		DpInstAccounting cplWaitDealInst = BizUtil.getInstance(DpInstAccounting.class);

		// 交易控制检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_SAVE.getValue());

		// 直接付息
		if (cplIn.getInst_adjust_way() == E_ADJUSTWAY.PAY) {

			// 付息账务处理
			cplWaitDealInst = directPayInterest(cplIn, subAcct);
		}
		else {
			// 调应计利息
			cplWaitDealInst = adjustAccruedInterest(cplIn, subAcct);
		}

		// 3. 返回输出信息
		DpInterestAdjustmentOut cplOut = BizUtil.getInstance(DpInterestAdjustmentOut.class);

		cplOut.setAcct_no(subAcct.getAcct_no());
		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplOut.setAcct_name(subAcct.getSub_acct_name());
		cplOut.setCcy_code(subAcct.getCcy_code());
		cplOut.setProd_id(subAcct.getProd_id());
		cplOut.setProd_name(DpProductFactoryApi.getProdBaseInfo(subAcct.getProd_id()).getProd_name());
		cplOut.setInst_adjust_aspect(cplIn.getInst_adjust_aspect());
		cplOut.setInst_adjust_way(cplIn.getInst_adjust_way());
		cplOut.setAccrual_inst(cplWaitDealInst.getInterest().abs());
		cplOut.setInterest_tax(cplWaitDealInst.getInterest_tax().abs());
		cplOut.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		cplOut.setOpp_acct_no(cplIn.getOpp_acct_no());
		cplOut.setOpp_acct_name(cplIn.getOpp_acct_name());
		cplOut.setOpp_ccy_code(cplIn.getOpp_ccy_code());
		cplOut.setAct_dept_amt(cplIn.getAct_dept_amt());

		bizlog.debug(" cplOut >>>>>>>>>>>>>>>>[%s]", cplOut);
		bizlog.method(" DpInterestHandAdjust.DepositInterestAdjustment end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年10月11日-下午2:19:03</li>
	 *         <li>功能说明：校验账户相关信息</li>
	 *         </p>
	 * @param cplIn
	 * @return 子账户信息
	 */
	private static DpaSubAccount checkAcctData(DpInterestAdjustmentIn cplIn) {
		bizlog.method(" DpInterestHandAdjust.checkAcctData begin >>>>>>>>>>>>>>>>");

		// 1. 定位账户
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no());
		accessIn.setAcct_type(cplIn.getAcct_type());
		accessIn.setCcy_code(cplIn.getCcy_code());
		accessIn.setProd_id(cplIn.getProd_id());
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 带锁
		DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 1.1 有输入账户名称,需要校验名称是否一致
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(acctAccessOut.getAcct_name(), cplIn.getAcct_name())) {

			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctAccessOut.getAcct_name());
		}

		// 不计息直接报错
		if (subAcct.getInst_ind() == E_YESORNO.NO) {

			throw DpBase.E0253();
		}

		// 定期卡片账不在此调整利息, 因为这类账户涉及到多张卡片，无法区分哪张卡片该处理多少金额
		if (DpToolsApi.judgeTimeSlip(subAcct)) {
			throw DpErr.Dp.E0419();
		}

		// 2. 直接付息对手方合法性检查：存放同业对手方需为同业活期或内部户、存款账户对手方需为现金或存款账户或挂账
		if (cplIn.getInst_adjust_way() == E_ADJUSTWAY.PAY) {

			DpAccountRouteInfo acctAanlyOut = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getOpp_acct_no(), cplIn.getCash_trxn_ind());

			if (subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT) {

				// 对手方账户性质不符合要求
				if (!CommUtil.in(acctAanlyOut.getAcct_analy(), E_ACCOUTANALY.CASH, E_ACCOUTANALY.DEPOSIT, E_ACCOUTANALY.SUSPENSE)) {
					throw DpErr.Dp.E0019(cplIn.getOpp_acct_no());
				}
			}
			else {

				// 对手方账户性质不符合要求
				if (!CommUtil.in(acctAanlyOut.getAcct_analy(), E_ACCOUTANALY.NOSTRO, E_ACCOUTANALY.BUSINESE)) {
					throw DpErr.Dp.E0019(cplIn.getOpp_acct_no());
				}
			}
		}

		bizlog.method(" DpInterestHandAdjust.checkAcctData end <<<<<<<<<<<<<<<<");
		return subAcct;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年9月14日-下午5:06:58</li>
	 *         <li>功能说明：验证输入接口数据合法性</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void validateInputData(DpInterestAdjustmentIn cplIn) {
		bizlog.method(" DpInterestHandAdjust.validateInputData begin >>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getInst_adjust_aspect(), DpBaseDict.A.inst_adjust_aspect.getId(), DpBaseDict.A.inst_adjust_aspect.getLongName());
		BizUtil.fieldNotNull(cplIn.getInst_adjust_way(), DpDict.A.inst_adjust_way.getId(), DpDict.A.inst_adjust_way.getLongName());
		BizUtil.fieldNotNull(cplIn.getAccrual_inst(), SysDict.A.interest.getId(), SysDict.A.interest.getLongName());

		if (cplIn.getInst_adjust_way() == E_ADJUSTWAY.PAY) {

			BizUtil.fieldNotNull(cplIn.getCash_trxn_ind(), SysDict.A.cash_trxn_ind.getId(), SysDict.A.cash_trxn_ind.getLongName());

			if (cplIn.getCash_trxn_ind() == E_CASHTRXN.TRXN) {

				BizUtil.fieldNotNull(cplIn.getOpp_acct_no(), SysDict.A.opp_acct_no.getId(), SysDict.A.opp_acct_no.getLongName());
			}
		}
		else if (cplIn.getInst_adjust_way() == E_ADJUSTWAY.ADJUST) {

			BizUtil.fieldNotNull(cplIn.getBack_value_date(), DpSysDict.A.back_value_date.getId(), DpSysDict.A.back_value_date.getLongName());
		}

		bizlog.method(" DpInterestHandAdjust.validateInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年12月13日-下午5:06:58</li>
	 *         <li>功能说明：直接付息处理</li>
	 *         </p>
	 * @param cplIn
	 */
	private static DpInstAccounting directPayInterest(DpInterestAdjustmentIn cplIn, DpaSubAccount subAcct) {

		bizlog.method(" DpInterestHandAdjust.payInstAccounting begin >>>>>>>>>>>>>>>>");

		// 记账金额检查精度合法性
		ApCurrencyApi.chkAmountByCcy(subAcct.getCcy_code(), cplIn.getAccrual_inst());

		// 利息
		BigDecimal instValue = (cplIn.getInst_adjust_aspect() == E_ADDSUBTRACT.ADD) ? cplIn.getAccrual_inst() : cplIn.getAccrual_inst().negate();

		// 计算代扣利息税
		DpIntTaxInfo taxInfo = DpTaxApi.calcWithholdingTax(subAcct.getAcct_no(), subAcct.getSub_acct_no(), instValue);

		DpInstAccounting cplWaitDealInst = BizUtil.getInstance(DpInstAccounting.class);

		cplWaitDealInst.setInterest(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), instValue));
		cplWaitDealInst.setInterest_tax(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), taxInfo.getAccrual_inst_tax()));
		cplWaitDealInst.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_INST_ADJUST"));
		cplWaitDealInst.setTrxn_remark(cplIn.getTrxn_remark());
		
		// 对手方账户路由分析
		DpAccountRouteInfo acctAanlyOut = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getOpp_acct_no(), cplIn.getCash_trxn_ind());

		// （一）借：应付利息，贷：代扣利息税
		DpInterestSettlement.payInterestAccounting(cplWaitDealInst, subAcct);

		// （二）贷： 利息入指定账户
		if (CommUtil.in(acctAanlyOut.getAcct_analy(), E_ACCOUTANALY.BUSINESE, E_ACCOUTANALY.DEPOSIT, E_ACCOUTANALY.NOSTRO)) {

			if (acctAanlyOut.getAcct_analy() == E_ACCOUTANALY.BUSINESE) {

				DpInterestAccounting.instIntoAppointAcct(cplWaitDealInst, subAcct, cplIn.getOpp_acct_no(), cplIn.getOpp_ccy_code());
			}
			else {

				DpaAccount oppAcctInfo = DpToolsApi.locateSingleAccount(cplIn.getOpp_acct_no(), cplIn.getOpp_acct_type(), false);
				DpPublicCheck.checkIncomeAcct(cplIn.getOpp_acct_no(), cplIn.getOpp_ccy_code(), E_SAVEORWITHDRAWALIND.SAVE);
				DpInterestAccounting.instIntoAppointAcct(cplWaitDealInst, subAcct, oppAcctInfo.getAcct_no(), cplIn.getOpp_ccy_code());
			}
		}
		else if (acctAanlyOut.getAcct_analy() == E_ACCOUTANALY.CASH) {

			BigDecimal trxnAmt = cplWaitDealInst.getInterest().subtract(cplWaitDealInst.getInterest_tax());

			if (!CommUtil.equals(cplIn.getCcy_code(), cplIn.getOpp_ccy_code())) {

				// 结售汇中间记账服务输入
				DpExchangeAccountingIn fxExchangeIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

				if (cplIn.getInst_adjust_aspect() == E_ADDSUBTRACT.ADD) {

					fxExchangeIn.setBuy_cash_ind(E_CASHTRXN.TRXN);
					fxExchangeIn.setBuy_ccy_code(cplIn.getCcy_code());
					fxExchangeIn.setBuy_amt(trxnAmt.abs());
					fxExchangeIn.setBuy_acct_no(cplIn.getAcct_no());
					fxExchangeIn.setBuy_sub_acct_seq(cplIn.getSub_acct_seq());

					fxExchangeIn.setSell_cash_ind(E_CASHTRXN.CASH);
					fxExchangeIn.setSell_ccy_code(cplIn.getOpp_ccy_code());

				}
				else {

					fxExchangeIn.setBuy_cash_ind(E_CASHTRXN.CASH);
					fxExchangeIn.setBuy_ccy_code(cplIn.getOpp_ccy_code());

					fxExchangeIn.setSell_cash_ind(E_CASHTRXN.TRXN);
					fxExchangeIn.setSell_ccy_code(cplIn.getCcy_code());
					fxExchangeIn.setSell_acct_no(cplIn.getAcct_no());
					fxExchangeIn.setSell_sub_acct_seq(cplIn.getSub_acct_seq());
					fxExchangeIn.setSell_amt(trxnAmt.abs());

				}

				fxExchangeIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.SELF);

				// 客户类型对现钞兑换有用
				fxExchangeIn.setCust_type(subAcct.getCust_type());
				fxExchangeIn.setCustomer_remark(cplIn.getCustomer_remark());
				fxExchangeIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_INST_ADJUST"));
				fxExchangeIn.setTrxn_remark(cplIn.getTrxn_remark());

				// 调用结售汇中间记账服务
				DpExchangeAccountingOut fxExchangeOut = DpExchangeIobus.exchangeAccounting(fxExchangeIn);

				trxnAmt = cplIn.getInst_adjust_aspect() == E_ADDSUBTRACT.ADD ? fxExchangeOut.getSell_amt() : fxExchangeOut.getBuy_amt();
			}

			DpCashAccountingIn cashAccountingIn = BizUtil.getInstance(DpCashAccountingIn.class);

			cashAccountingIn.setCcy_code(cplIn.getOpp_ccy_code());
			cashAccountingIn.setTrxn_amt(trxnAmt.abs());
			cashAccountingIn.setDebit_credit((cplIn.getInst_adjust_aspect() == E_ADDSUBTRACT.ADD) ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT);
			cashAccountingIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
			cashAccountingIn.setOpp_acct_no(cplIn.getAcct_no());
			cashAccountingIn.setOpp_acct_ccy(cplIn.getCcy_code());
			cashAccountingIn.setOpp_branch_id(BizUtil.getTrxRunEnvs().getTrxn_branch());
			cashAccountingIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_INST_ADJUST"));
			cashAccountingIn.setTrxn_remark(cplIn.getTrxn_remark()); // transaction
																		// remark
			DpCashIobus.cashAccounting(cashAccountingIn);
		}
		else if (acctAanlyOut.getAcct_analy() == E_ACCOUTANALY.SUSPENSE) {

			// 挂账,不能结售汇
			if (!CommUtil.equals(cplIn.getCcy_code(), cplIn.getOpp_ccy_code())) {
				DpErr.Dp.E0318(cplIn.getOpp_ccy_code(), cplIn.getCcy_code());
			}

			BigDecimal trxnAmt = cplWaitDealInst.getInterest().subtract(cplWaitDealInst.getInterest_tax());

			DpSuspenseAccountingIn suspenseIn = BizUtil.getInstance(DpSuspenseAccountingIn.class);

			suspenseIn.setAcct_branch(BizUtil.getTrxRunEnvs().getTrxn_branch());
			suspenseIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			suspenseIn.setCcy_code(cplIn.getOpp_ccy_code());
			suspenseIn.setTrxn_amt(trxnAmt.abs());
			suspenseIn.setDebit_credit((cplIn.getInst_adjust_aspect() == E_ADDSUBTRACT.ADD) ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT);
			suspenseIn.setGl_ref_code(acctAanlyOut.getGl_ref_code());

			// 补充信息
			suspenseIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_INST_ADJUST"));
			suspenseIn.setMulti_deposit_ind(E_YESORNO.YES);
			suspenseIn.setMulti_draw_ind(E_YESORNO.YES);

			suspenseIn.setTrxn_remark(cplIn.getTrxn_remark());
			suspenseIn.setCustomer_remark(cplIn.getCustomer_remark());

			// 对手方信息
			suspenseIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
			suspenseIn.setOpp_acct_no(cplIn.getAcct_no());
			suspenseIn.setOpp_acct_ccy(cplIn.getCcy_code());

			// 调用挂销账记账服务
			DpInsideAccountIobus.suspenseAccounting(suspenseIn);

		}

		// 登记付息明细
		regPayInterestDetail(cplWaitDealInst, subAcct, taxInfo.getInst_tax_rate(), cplIn.getBack_value_date());

		bizlog.method(" DpInterestHandAdjust.payInstAccounting end <<<<<<<<<<<<<<<<");
		return cplWaitDealInst;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年12月13日-下午5:06:58</li>
	 *         <li>功能说明：登记付息明细</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void regPayInterestDetail(DpInstAccounting cplIn, DpaSubAccount subAcct, BigDecimal taxRate, String backDate) {

		bizlog.method(" DpInterestHandAdjust.regPayInterestDetail begin >>>>>>>>>>>>>>>>");

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 获取计息类型
		E_INSTKEYTYPE instKeyType = DpInterestBasicApi.getCainInstKey(subAcct, BizUtil.getTrxRunEnvs().getTrxn_date());

		// 计息定义
		DpaInterest instAcct = DpaInterestDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), instKeyType, true);

		// 利率定义
		DpaInterestRate acctRate = DpaInterestRateDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), instKeyType, DpConst.START_SORT_VALUE, true);

		// 登记付息明细
		DpsPayedInterest payDetl = BizUtil.getInstance(DpsPayedInterest.class);

		payDetl.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		payDetl.setAcct_no(subAcct.getAcct_no());
		payDetl.setInst_key_type(instKeyType); // 利息索引类型
		payDetl.setLayer_no(DpConst.START_SORT_VALUE); // 层次序号
		payDetl.setSerial_no(instAcct.getLast_payed_inst_seq() + 1); // 序号
		payDetl.setInst_oper_type(E_INSTOPERATE.PAY); // 利息操作类型
		payDetl.setInst_adjust_type(E_INSTADJUSTTYPE.BACKVALUE); // 利息调整类型
		payDetl.setSeg_inst_start_date(trxnDate); // 分段起息日
		payDetl.setSeg_inst_end_date(trxnDate); // 分段止息日

		DpInrtQryOut interestRate = getBackDateInterestRate(backDate, subAcct, instAcct);

		int yearInstDays = DpInterestBasicApi.getYearInterestDays(instAcct.getAccrual_base_day(), BizUtil.getTrxRunEnvs().getTrxn_date());
		BigDecimal sumBal = BigDecimal.ZERO;
		BigDecimal effectRate = (interestRate.getSingle_layer_ind() == E_YESORNO.YES) ? interestRate.getEfft_inrt() : interestRate.getListLayerInrt().get(0).getEfft_inrt();

		if (!CommUtil.equals(effectRate, BigDecimal.ZERO)) {

			sumBal = cplIn.getInterest().divide(effectRate.divide(BigDecimal.valueOf(100 * yearInstDays), DpConst.iScale_inst_calc, RoundingMode.HALF_UP), 2, RoundingMode.HALF_UP);
		}

		payDetl.setSeg_sum_bal(sumBal); // 分段积数
		payDetl.setInst_day((long) 0); // 计息天数
		payDetl.setInrt_code(instAcct.getInrt_code()); // 利率编号
		payDetl.setInrt_code_direction(instAcct.getInrt_code_direction()); // 利率编号指向
		payDetl.setInst_bal(BigDecimal.ZERO); // 计息余额
		payDetl.setLayer_inst_bal(BigDecimal.ZERO); // 层次计息余额
		payDetl.setBank_base_inrt(acctRate.getBank_base_inrt()); // 行内基准利率
		payDetl.setEfft_inrt(interestRate.getEfft_inrt()); // 账户执行利率
		payDetl.setInst_base(instAcct.getInst_base()); // 计息基础
		payDetl.setInst_tax_rate(taxRate); // 应计利息税率
		payDetl.setSeg_inst(cplIn.getInterest()); // 分段利息
		payDetl.setSeg_inst_tax(cplIn.getInterest_tax()); // 分段利息税
		payDetl.setInst_date(trxnDate); // 计息日期
		payDetl.setInst_seq(BizUtil.getTrxRunEnvs().getTrxn_seq()); // 计息流水
		payDetl.setTrxn_code(BizUtil.getTrxRunEnvs().getTrxn_code()); // 交易码
		payDetl.setPay_inst_date(trxnDate); // 付息日期
		payDetl.setPay_inst_seq(BizUtil.getTrxRunEnvs().getTrxn_seq()); // 付息流水
		payDetl.setPay_inst_trxn_code(BizUtil.getTrxRunEnvs().getTrxn_code()); // 付息交易码
		payDetl.setSource_wait_inst_ind(E_YESORNO.NO);
		payDetl.setRecord_status(E_STATUS.VALID); // 记录状态

		DpsPayedInterestDao.insert(payDetl);

		// 计息定义表更新
		instAcct.setLast_payed_inst_seq(instAcct.getLast_payed_inst_seq() + 1);
		instAcct.setLast_inst_oper_type(E_INSTOPERATE.PAY);

		DpaInterestDao.updateOne_odb1(instAcct);

		bizlog.method(" DpInterestHandAdjust.regPayInterestDetail end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年12月13日-下午5:06:58</li>
	 *         <li>功能说明：调应计利息</li>
	 *         </p>
	 * @param cplIn
	 */
	private static DpInstAccounting adjustAccruedInterest(DpInterestAdjustmentIn cplIn, DpaSubAccount subAcct) {

		bizlog.method(" DpInterestHandAdjust.adjustAccruedInterest begin >>>>>>>>>>>>>>>>");

		// 按币种计息精度四舍五入
		cplIn.setAccrual_inst(ApCurrencyApi.roundInterest(subAcct.getCcy_code(), cplIn.getAccrual_inst()));

		// 获取计息类型
		E_INSTKEYTYPE instKeyType = DpInterestBasicApi.getCainInstKey(subAcct, cplIn.getBack_value_date());

		// 计息定义
		DpaInterest instAcct = DpaInterestDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), instKeyType, true);

		// 利率定义
		DpaInterestRate acctRate = DpaInterestRateDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), instKeyType, DpConst.START_SORT_VALUE, true);

		// 利息
		BigDecimal instValue = (cplIn.getInst_adjust_aspect() == E_ADDSUBTRACT.ADD) ? cplIn.getAccrual_inst() : cplIn.getAccrual_inst().negate();

		// 计算利息税
		DpIntTaxInfo taxInfo = DpTaxApi.calcAccruedTax(subAcct.getAcct_no(), subAcct.getSub_acct_no(), BizUtil.getTrxRunEnvs().getTrxn_date(), instValue);
		// 计息明细
		DpInstDetailRegister instDetl = BizUtil.getInstance(DpInstDetailRegister.class);

		instDetl.setInst_key_type(instKeyType); // 利息索引类型
		instDetl.setLayer_no(DpConst.START_SORT_VALUE); // 层次序号
		instDetl.setTotal_layer_num(DpConst.START_SORT_VALUE);
		instDetl.setInst_oper_type(E_INSTOPERATE.ADJUST); // 利息操作类型
		instDetl.setInst_adjust_type(E_INSTADJUSTTYPE.BACKVALUE); // 利息调整类型
		instDetl.setSeg_inst_start_date(cplIn.getBack_value_date()); // 分段起息日
		instDetl.setSeg_inst_end_date(cplIn.getBack_value_date()); // 分段止息日

		DpInrtQryOut interestRate = getBackDateInterestRate(cplIn.getBack_value_date(), subAcct, instAcct);

		int yearInstDays = DpInterestBasicApi.getYearInterestDays(instAcct.getAccrual_base_day(), cplIn.getBack_value_date());
		BigDecimal sumBal = BigDecimal.ZERO;
		BigDecimal effectRate = (interestRate.getSingle_layer_ind() == E_YESORNO.YES) ? interestRate.getEfft_inrt() : interestRate.getListLayerInrt().get(0).getEfft_inrt();

		if (!CommUtil.equals(effectRate, BigDecimal.ZERO)) {

			sumBal = instValue.divide(effectRate.divide(BigDecimal.valueOf(100 * yearInstDays), DpConst.iScale_inst_calc, RoundingMode.HALF_UP), 2, RoundingMode.HALF_UP);
		}

		instDetl.setSeg_sum_bal(sumBal); // 分段积数
		instDetl.setInst_day((long) 0); // 计息天数
		instDetl.setTier_method(acctRate.getTier_method()); // 金额分层方式
		instDetl.setBand_amount(null); // 分层金额
		instDetl.setBand_term(""); // 分层存期
		instDetl.setInst_bal(BigDecimal.ZERO); // 计息余额
		instDetl.setLayer_inst_bal(BigDecimal.ZERO); // 层次计息余额
		instDetl.setEfft_inrt(interestRate.getEfft_inrt()); // 账户执行利率
		instDetl.setBank_base_inrt(acctRate.getBank_base_inrt()); // 行内基准利率
		instDetl.setSeg_inst(instValue); // 分段利息
		instDetl.setSeg_inst_tax(taxInfo.getAccrual_inst_tax()); // 分段利息税
		instDetl.setInst_tax_rate(taxInfo.getInst_tax_rate()); // 应计利息税率
		instDetl.setInst_base(instAcct.getInst_base()); // 负债计息规则
		instDetl.setInrt_code(instAcct.getInrt_code()); // 利率编号
		instDetl.setInrt_code_direction(instAcct.getInrt_code_direction()); // 利率编号指向

		// 登记利息明细
		DpInterestBasicApi.regAccrualInstDetl(instDetl, instAcct);

		// 计息定义表利息值更新
		instAcct.setLast_inst_oper_type(E_INSTOPERATE.ADJUST); // 利息操作类型
		instAcct.setAccrual_inst(instAcct.getAccrual_inst().add(instValue));
		instAcct.setAccrual_sum_bal(instAcct.getAccrual_sum_bal().add(sumBal));
		instAcct.setAccrual_inst_tax(instAcct.getAccrual_inst_tax().add(taxInfo.getAccrual_inst_tax()));

		boolean CurTermFlag = true; // 当前标志，默认为是

		if (CommUtil.isNotNull(instAcct.getPay_inst_cyc()) && CommUtil.compare(cplIn.getBack_value_date(), instAcct.getLast_pay_inst_date()) < 0) {
			CurTermFlag = false;
		}

		if (instAcct.getInst_seg_ind() == E_YESORNO.YES && CommUtil.compare(cplIn.getBack_value_date(), instAcct.getLast_inrt_renew_date()) < 0) {
			CurTermFlag = false;
		}

		// 本期应计利息
		if (CurTermFlag) {

			instAcct.setCur_term_inst(instAcct.getCur_term_inst().add(instValue));
			instAcct.setCur_term_inst_sum_bal(instAcct.getCur_term_inst_sum_bal().add(sumBal));
			instAcct.setCur_term_inst_tax(instAcct.getCur_term_inst_tax().add(taxInfo.getAccrual_inst_tax()));
		}

		DpaInterestDao.updateOne_odb1(instAcct);

		DpInstAccounting cplWaitDealInst = BizUtil.getInstance(DpInstAccounting.class);

		cplWaitDealInst.setInterest(instValue);
		cplWaitDealInst.setInterest_tax(taxInfo.getAccrual_inst_tax());

		bizlog.method(" DpInterestHandAdjust.adjustAccruedInterest end <<<<<<<<<<<<<<<<");
		return cplWaitDealInst;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2018年1月29日-下午5:20:18</li>
	 *         <li>功能说明：查询倒起息日期有效利率</li>
	 *         </p>
	 * @param backDate
	 *            倒起息日期
	 * @param subAcct
	 *            子户信息
	 * @param instAcct
	 *            子户利息定义
	 * @return
	 */
	private static DpInrtQryOut getBackDateInterestRate(String backDate, DpaSubAccount subAcct, DpaInterest instAcct) {
		// 查询倒起息日期有效利率
		DpInrtQryIn qryRateInfo = BizUtil.getInstance(DpInrtQryIn.class);

		qryRateInfo.setInrt_code(instAcct.getInrt_code());
		qryRateInfo.setCcy_code(subAcct.getCcy_code());
		qryRateInfo.setTerm_code(subAcct.getTerm_code());
		qryRateInfo.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		qryRateInfo.setTrxn_amt(subAcct.getAcct_bal());
		qryRateInfo.setStart_inst_date(backDate);
		qryRateInfo.setInst_rate_file_way(instAcct.getInst_rate_file_way());

		DpInrtQryOut interestRate = DpRateBasicApi.getInstRateInfo(qryRateInfo);
		return interestRate;
	}
}
