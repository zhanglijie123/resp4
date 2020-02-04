package cn.sunline.icore.dp.serv.fundpool;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApReversalApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpOverDraftApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlipDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraft;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlipDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInterestCalcOut;
import cn.sunline.icore.dp.base.type.ComDpOverdraftTrxnBasic.DpOverDraftSettleAmount;
import cn.sunline.icore.dp.base.type.ComDpReversalBase.DpOverDraftAgreeClearReversalIn;
import cn.sunline.icore.dp.base.type.ComDpReversalBase.DpOverDraftSlipClear;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTOPERATE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_OVERDRAFTINSTTYPE;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.interest.DpInterestSettlement;
import cn.sunline.icore.dp.serv.iobus.DpCashIobus;
import cn.sunline.icore.dp.serv.iobus.DpCreditLimitIobus;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftPayInstIn;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftPayInstOut;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpSingleOverdraftPayInstIn;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpSingleOverdraftPayInstOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAccountRouteInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCashAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpSuspenseAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpSuspenseAccountingOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_FOREXEXCHOBJECT;
import cn.sunline.icore.sys.type.EnumType.E_ROUNDRULE;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.base.util.RunnableWithReturn;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：透支结清处理
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年11月24日-上午11:44:32</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */

public class DpOverdraftSettlement {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOverdraftSettlement.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月24日-下午6:50:26</li>
	 *         <li>功能说明：获取透支利息</li>
	 *         <li>使用说明：透支协议号为空，表示查询账户下总计欠息</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 * @param OdAgreeNo
	 *            透支协议号: 可以为空
	 */
	public static BigDecimal getOdInterest(final DpaSubAccount subAccount, final String OdAgreeNo) {

		bizlog.method(" DpOverDraftSettle.wholeAcctSettle begin >>>>>>>>>>>>>>>>");

		if (subAccount.getOverdraft_allow_ind() == E_YESORNO.NO) {
			bizlog.method(" DpOverDraftSettle.getOdInterest end <<<<<<<<<<<<<<<<");
			return BigDecimal.ZERO;
		}

		BigDecimal OdInterest = DaoUtil.executeInNewTransation(new RunnableWithReturn<BigDecimal>() {

			public BigDecimal execute() {

				// 当天已经计提过的不再处理
				if (CommUtil.compare(subAccount.getNext_inst_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) > 0) {

					DpOverDraftApi.accruedOdInterest(subAccount);
				}

				// 透支利息
				BigDecimal overDraftInst = BigDecimal.ZERO;

				if (CommUtil.isNotNull(OdAgreeNo)) {

					DpbOverdraft OdAgree = DpbOverdraftDao.selectOne_odb1(subAccount.getAcct_no(), OdAgreeNo, false);

					if (CommUtil.isNull(OdAgree)) {
						throw APPUB.E0005(OdbFactory.getTable(DpbOverdraft.class).getLongname(), SysDict.A.agree_no.getLongName(), SysDict.A.agree_no.getId());
					}

					List<DpbOverdraftSlip> listOdFiche = DpbOverdraftSlipDao.selectAll_odb3(subAccount.getAcct_no(), OdAgreeNo, false);

					for (DpbOverdraftSlip OdFiche : listOdFiche) {

						DpaSlip ficheInst = DpaSlipDao.selectOne_odb1(subAccount.getAcct_no(), OdFiche.getFiche_no(), true);

						// 透支卡片利息 + 罚息
						overDraftInst = overDraftInst.add(ficheInst.getAccrual_inst()).add(OdFiche.getOverdue_interest());
					}

				}
				else {

					// 透支卡片信息
					List<DpaSlip> listFicheInst = DpaSlipDao.selectAll_odb2(subAccount.getAcct_no(), subAccount.getSub_acct_no(), E_ACCTSTATUS.NORMAL, false);

					for (DpaSlip ficheInst : listFicheInst) {

						// 透支卡片表
						DpbOverdraftSlip OdFiche = DpbOverdraftSlipDao.selectOne_odb2(subAccount.getAcct_no(), ficheInst.getFiche_no(), true);

						// 透支卡片利息 + 罚息
						overDraftInst = overDraftInst.add(ficheInst.getAccrual_inst()).add(OdFiche.getOverdue_interest());
					}
				}

				return overDraftInst;
			}
		});

		bizlog.method(" DpOverDraftSettle.wholeAcctSettle end <<<<<<<<<<<<<<<<");
		return OdInterest;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年10月21日-下午2:46:00</li>
	 *         <li>功能说明：透支账户透支结清</li>
	 *         </p>
	 * @param cplIn
	 *            透支结清输入
	 * @return
	 */
	public static DpOverdraftPayInstOut overdraftAgreePayInst(DpOverdraftPayInstIn cplIn) {

		bizlog.method(" DpOverdraftAgree.overdraftAgreePayInst begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 必输校验
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getCash_trxn_ind(), SysDict.A.cash_trxn_ind.getId(), SysDict.A.cash_trxn_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getDebit_ccy_code(), SysDict.A.debit_ccy_code.getId(), SysDict.A.debit_ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

		// 定位账号信息
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no()); // 账号
		accessIn.setCcy_code(cplIn.getCcy_code()); // 货币代码
		accessIn.setProd_id(cplIn.getProd_id());// 产品编号
		accessIn.setAcct_type(cplIn.getAcct_type()); // 账户类型
		accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND); // 定活标志
		accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL); // 存入支取标志

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 子账户查询，带锁，避免并发
		DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		// 查询账户下所有生效协议
		List<DpbOverdraft> listOdAgree = DpbOverdraftDao.selectAll_odb2(subAcct.getAcct_no(), E_STATUS.VALID, false);

		// 协议信息为空报错
		if (CommUtil.isNull(listOdAgree) || listOdAgree.size() == 0) {

			throw DpBase.E0401(accessOut.getAcct_no());
		}

		Options<DpOverDraftSettleAmount> listOdSettleAmount = new DefaultOptions<DpOverDraftSettleAmount>();

		BigDecimal trxnAmount = BigDecimal.ZERO; // 本金
		BigDecimal stillInst = BigDecimal.ZERO; // 利息

		for (DpbOverdraft agree : listOdAgree) {

			// 获取透支协议下利息、透支余额信息
			DpOverDraftSettleAmount cplOdAmount = getSingleOdAgreeSettleInfo(subAcct, agree);

			listOdSettleAmount.add(cplOdAmount);

			// 应还本金
			trxnAmount = trxnAmount.add(cplOdAmount.getAcct_bal());

			// 应收利息之和
			stillInst = stillInst.add(cplOdAmount.getStill_inst());
		}

		// 输出实例化
		DpOverdraftPayInstOut cplOut = BizUtil.getInstance(DpOverdraftPayInstOut.class);

		// 账务处理
		if (CommUtil.compare(trxnAmount.add(stillInst), BigDecimal.ZERO) > 0) {
			cplOut = accounting(cplIn, subAcct, trxnAmount, stillInst);
		}

		// 清零卡片信息和关闭协议
		for (DpOverDraftSettleAmount cplOdAmount : listOdSettleAmount) {

			clearSingleOdAgree(subAcct.getAcct_no(), cplOdAmount.getAgree_no(), cplIn.getCancle_agree_ind());
		}

		// 补充输出
		cplOut.setAcct_no(accessOut.getAcct_no());
		cplOut.setAcct_type(accessOut.getAcct_type());
		cplOut.setAcct_name(accessOut.getAcct_name());
		cplOut.setCcy_code(subAcct.getCcy_code());
		cplOut.setInterest(stillInst);
		cplOut.setCash_trxn_ind(E_CASHTRXN.TRXN);
		cplOut.setDebit_ccy_code(cplIn.getDebit_ccy_code());
		cplOut.setDebit_suspense_no(cplIn.getDebit_suspense_no());
		cplOut.setDebit_acct_no(cplIn.getDebit_acct_no());
		cplOut.setDebit_acct_name(cplIn.getDebit_acct_name());
		cplOut.setList01(listOdSettleAmount);

		bizlog.debug("<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpOverdraftAgree.overdraftAgreePayInst end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年10月21日-下午2:46:00</li>
	 *         <li>功能说明：透支利息结清</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	private static DpOverDraftSettleAmount getSingleOdAgreeSettleInfo(DpaSubAccount subAcct, DpbOverdraft agreeInfo) {

		DpbOverdraft ODAgree = DpbOverdraftDao.selectOne_odb1(subAcct.getAcct_no(), agreeInfo.getAgree_no(), false);

		if (CommUtil.isNull(ODAgree)) {
			throw APPUB.E0005(OdbFactory.getTable(DpbOverdraft.class).getLongname(), SysDict.A.agree_no.getLongName(), SysDict.A.agree_no.getId());
		}

		if (ODAgree.getAgree_status() == E_STATUS.INVALID) {
			throw DpBase.E0296(agreeInfo.getAgree_no());
		}

		if (CommUtil.compare(agreeInfo.getData_version(), ODAgree.getData_version()) != 0) {

			throw ApPubErr.APPUB.E0018(DpbOverdraft.class.getName());
		}

		// 当天已经计提过的不再处理
		if (CommUtil.compare(subAcct.getNext_inst_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) > 0) {

			// 透支利息计提
			DpOverDraftApi.accruedOdInterest(subAcct);

			// 存款利息计提
			DpInterestBasicApi.accruedInterest(subAcct.getAcct_no(), subAcct.getSub_acct_no());
		}

		BigDecimal instValue = BigDecimal.ZERO;
		BigDecimal occupiedAmt = BigDecimal.ZERO;
		BigDecimal ficheBal = BigDecimal.ZERO;

		// 透支卡片对照
		List<DpbOverdraftSlip> listODFiche = DpbOverdraftSlipDao.selectAll_odb3(ODAgree.getAcct_no(), ODAgree.getAgree_no(), true);

		for (DpbOverdraftSlip ODFiche : listODFiche) {

			DpaSlip ficheInst = DpaSlipDao.selectOne_odb1(ODAgree.getAcct_no(), ODFiche.getFiche_no(), true);

			if (ficheInst.getAcct_status() == E_ACCTSTATUS.CLOSE) {
				continue;
			}

			BigDecimal accrInst = ApCurrencyApi.roundAmount(agreeInfo.getCcy_code(), ficheInst.getAccrual_inst(), E_ROUNDRULE.ROUND);

			BigDecimal overdueInst = ApCurrencyApi.roundAmount(agreeInfo.getCcy_code(), ODFiche.getOverdue_interest(), E_ROUNDRULE.ROUND);

			// 累计利息信息: 透支利息 + 罚息
			instValue = instValue.add(accrInst.add(overdueInst));
			// 占用金额
			occupiedAmt = occupiedAmt.add(ODFiche.getOccupied_amt());
			// 卡片余额
			ficheBal = ficheBal.add(ficheInst.getAcct_bal());
		}

		// 透支金额信息
		DpOverDraftSettleAmount cplAmtInfo = BizUtil.getInstance(DpOverDraftSettleAmount.class);

		cplAmtInfo.setAgree_no(agreeInfo.getAgree_no());
		cplAmtInfo.setAcct_bal(ficheBal);
		cplAmtInfo.setOccupied_amt(occupiedAmt);
		cplAmtInfo.setStill_inst(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), instValue, E_ROUNDRULE.ROUND));

		return cplAmtInfo;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年10月23日-下午2:19:47</li>
	 *         <li>功能说明：透支结清账务处理</li>
	 *         </p>
	 * @param cplIn
	 *            透支结清输入
	 * @param subAcct
	 *            子账户信息
	 * @param trxnAmt
	 *            本金
	 * @param instAmt
	 *            利息
	 */
	private static DpOverdraftPayInstOut accounting(DpOverdraftPayInstIn cplIn, DpaSubAccount subAcct, BigDecimal trxnAmt, BigDecimal instAmt) {

		bizlog.method(" DpOverDraftSettle.accounting begin >>>>>>>>>>>>>>>>");

		// 资金记账分析
		DpAccountRouteInfo RouteAnalyValue = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getAcct_no(), cplIn.getCash_trxn_ind(), cplIn.getDebit_suspense_no());

		BigDecimal factTrxnAmt = instAmt.add(trxnAmt);

		// 跨币种调结售汇中间服务
		if (!CommUtil.equals(cplIn.getCcy_code(), cplIn.getDebit_ccy_code())) {

			// 结售汇中间记账服务输入
			DpExchangeAccountingIn fxExchangeIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

			fxExchangeIn.setBuy_cash_ind(cplIn.getCash_trxn_ind());
			fxExchangeIn.setBuy_ccy_code(cplIn.getDebit_ccy_code());
			fxExchangeIn.setSell_cash_ind(E_CASHTRXN.TRXN);
			fxExchangeIn.setSell_ccy_code(cplIn.getCcy_code());
			fxExchangeIn.setSell_amt(trxnAmt.add(instAmt));
			fxExchangeIn.setExch_rate(cplIn.getExch_rate());
			fxExchangeIn.setForex_agree_price_id(cplIn.getForex_agree_price_id());
			fxExchangeIn.setExch_rate_path(cplIn.getExch_rate_path());
			fxExchangeIn.setForex_exch_object_type(
					CommUtil.in(RouteAnalyValue.getAcct_analy(), E_ACCOUTANALY.CASH, E_ACCOUTANALY.DEPOSIT) ? E_FOREXEXCHOBJECT.CUSTOMER : E_FOREXEXCHOBJECT.SELF);
			// 客户类型对现钞兑换有用
			fxExchangeIn.setCust_type(subAcct.getCust_type());
			fxExchangeIn.setCountry_code(subAcct.getCountry_code());
			fxExchangeIn.setCustomer_remark(cplIn.getCustomer_remark());
			fxExchangeIn.setSummary_code(cplIn.getSummary_code());
			fxExchangeIn.setTrxn_remark(cplIn.getTrxn_remark());

			// 买卖双方账户信息
			fxExchangeIn.setSell_acct_no(cplIn.getAcct_no());
			fxExchangeIn.setSell_sub_acct_seq(null);
			fxExchangeIn.setBuy_acct_no(cplIn.getDebit_acct_no());
			fxExchangeIn.setBuy_sub_acct_seq(null);

			// 调用结售汇中间记账服务
			DpExchangeAccountingOut fxExchangeOut = DpExchangeIobus.exchangeAccounting(fxExchangeIn);

			// 结售汇生成借方金额
			factTrxnAmt = fxExchangeOut.getBuy_amt();
		}

		// 借方账户余额检查，结清欠息不能使用透支额度，可以使用关联保护
		if (RouteAnalyValue.getAcct_analy() == E_ACCOUTANALY.DEPOSIT) {

			// 定位账号信息
			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(cplIn.getDebit_acct_no()); // 账号
			accessIn.setCcy_code(cplIn.getDebit_ccy_code()); // 货币代码
			accessIn.setAcct_type(cplIn.getDebit_acct_type()); // 账户类型
			accessIn.setProd_id(cplIn.getDebit_prod_id()); // 产品代码
			accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND); // 定活标志
			accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL); // 存入支取标志

			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			DpaSubAccount subAcctDr = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

			// 计算可用余额
			DpBalanceCalculateOut cplBalInfo = DpToolsApi.getBalance(accessOut.getSub_acct_no(), cplIn.getDebit_acct_no(), cplIn.getFroze_no(), cplIn.getUnfroze_amt());

			// 计算可用额度
			BigDecimal limitUsable = DpOverDraftApi.getLimtInfo(subAcctDr);

			// 若为单个协议结清则可使用该账户其余协议
			if (CommUtil.equals(BizUtil.getTrxRunEnvs().getTrxn_code(), "4239")) {

				limitUsable = BigDecimal.ZERO;
			}

			if (CommUtil.compare(cplBalInfo.getUsable_bal().subtract(limitUsable), factTrxnAmt) < 0) {
				if (CommUtil.compare(cplBalInfo.getFroze_amt(), BigDecimal.valueOf(0)) > 0 || CommUtil.compare(cplBalInfo.getFact_froze_amt(), BigDecimal.valueOf(0)) > 0) {
					throw DpBase.E0395();
				}
				else {
					throw DpBase.E0118(cplIn.getDebit_acct_no(), cplIn.getDebit_ccy_code());
				}

			}
		}

		// 输出信息
		DpOverdraftPayInstOut cplOut = BizUtil.getInstance(DpOverdraftPayInstOut.class);

		cplOut.setAct_withdrawal_amt(factTrxnAmt);

		// 现金
		if (RouteAnalyValue.getAcct_analy() == E_ACCOUTANALY.CASH) {

			// 记账输入数据
			DpCashAccountingIn cashAccountingIn = BizUtil.getInstance(DpCashAccountingIn.class);

			cashAccountingIn.setCcy_code(cplIn.getDebit_ccy_code());
			cashAccountingIn.setTrxn_amt(factTrxnAmt);
			cashAccountingIn.setDebit_credit(E_DEBITCREDIT.DEBIT); // 借贷方向
			cashAccountingIn.setSummary_code(cplIn.getSummary_code());
			cashAccountingIn.setTrxn_remark(cplIn.getTrxn_remark());
			cashAccountingIn.setOpp_acct_ccy(cplIn.getCcy_code());
			cashAccountingIn.setOpp_acct_no(cplIn.getAcct_no());
			cashAccountingIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
			cashAccountingIn.setOpp_branch_id(null);

			// 调用公共现金记账服务
			DpCashIobus.cashAccounting(cashAccountingIn);
		}
		else if (RouteAnalyValue.getAcct_analy() == E_ACCOUTANALY.DEPOSIT) {

			DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			demandDrawIn.setAcct_no(cplIn.getDebit_acct_no());
			demandDrawIn.setAcct_type(cplIn.getDebit_acct_type());
			demandDrawIn.setAcct_name(cplIn.getDebit_acct_name());
			demandDrawIn.setProd_id(cplIn.getDebit_prod_id());
			demandDrawIn.setCheck_password_ind(cplIn.getCheck_password_ind());
			demandDrawIn.setTrxn_password(cplIn.getTrxn_password());
			demandDrawIn.setCcy_code(cplIn.getDebit_ccy_code());
			demandDrawIn.setTrxn_amt(factTrxnAmt);
			demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.COMMON); // 普通支取
			demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 交易级现转标志
			demandDrawIn.setSummary_code(cplIn.getSummary_code());
			demandDrawIn.setTrxn_remark(cplIn.getTrxn_remark());
			demandDrawIn.setCustomer_remark(cplIn.getCustomer_remark());
			demandDrawIn.setCheque_no(cplIn.getCheque_no()); // 支票

			// 解冻信息
			demandDrawIn.setFroze_no(cplIn.getFroze_no());
			demandDrawIn.setUnfroze_amt(cplIn.getUnfroze_amt());
			demandDrawIn.setUnfroze_reason(cplIn.getUnfroze_reason());

			// 对手方信息
			demandDrawIn.setOpp_acct_ccy(cplIn.getCcy_code());
			demandDrawIn.setOpp_acct_no(cplIn.getAcct_no());
			demandDrawIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
			demandDrawIn.setOpp_acct_type(cplIn.getAcct_type());

			// 调用活期存款支取服务
			DpDemandDrawOut demandDrawOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);

			cplOut.setDebit_acct_no(demandDrawOut.getAcct_no());
			cplOut.setDebit_acct_name(demandDrawOut.getAcct_name());
			cplOut.setDebit_ccy_code(demandDrawOut.getCcy_code());
			cplOut.setDebit_acct_bal(demandDrawOut.getAcct_bal());
		}
		else if (RouteAnalyValue.getAcct_analy() == E_ACCOUTANALY.SUSPENSE) {

			// 挂销账记账输入
			DpSuspenseAccountingIn suspenseIn = BizUtil.getInstance(DpSuspenseAccountingIn.class);

			suspenseIn.setAcct_branch(cplIn.getDebit_acct_branch());
			suspenseIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			suspenseIn.setCcy_code(cplIn.getDebit_ccy_code());
			suspenseIn.setTrxn_amt(factTrxnAmt);
			suspenseIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
			suspenseIn.setGl_ref_code(RouteAnalyValue.getGl_ref_code());

			// 补充信息
			suspenseIn.setSummary_code(cplIn.getSummary_code());
			suspenseIn.setSuspense_no(cplIn.getDebit_suspense_no());
			suspenseIn.setTrxn_remark(cplIn.getTrxn_remark());
			suspenseIn.setCustomer_remark(cplIn.getCustomer_remark());
			suspenseIn.setOpp_acct_ccy(cplIn.getCcy_code());
			suspenseIn.setOpp_acct_no(cplIn.getAcct_no());
			suspenseIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);

			// 调用挂销账记账服务
			DpSuspenseAccountingOut suspenseOut = DpInsideAccountIobus.suspenseAccounting(suspenseIn);

			cplOut.setDebit_acct_no(suspenseOut.getAcct_no());
			cplOut.setDebit_acct_name(suspenseOut.getAcct_name());
			cplOut.setDebit_ccy_code(suspenseOut.getCcy_code());
			cplOut.setDebit_acct_bal(suspenseOut.getAcct_bal());
		}
		else if (RouteAnalyValue.getAcct_analy() == E_ACCOUTANALY.INSIDE) {

			// 内部户记账服务输入
			DpInsideAccountingIn bookAccoutingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

			bookAccoutingIn.setAcct_branch(cplIn.getDebit_acct_branch());
			bookAccoutingIn.setAcct_no(CommUtil.equals(cplIn.getDebit_acct_no(), RouteAnalyValue.getGl_ref_code()) ? null : cplIn.getDebit_acct_no());
			bookAccoutingIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			bookAccoutingIn.setCcy_code(cplIn.getDebit_ccy_code());
			bookAccoutingIn.setTrxn_amt(factTrxnAmt); // 使用实际支取金额
			bookAccoutingIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
			bookAccoutingIn.setGl_ref_code(RouteAnalyValue.getGl_ref_code());
			bookAccoutingIn.setSummary_code(cplIn.getSummary_code());
			bookAccoutingIn.setTrxn_remark(cplIn.getTrxn_remark());
			bookAccoutingIn.setCustomer_remark(cplIn.getCustomer_remark());

			// 真实对手方信息
			bookAccoutingIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
			bookAccoutingIn.setOpp_acct_no(cplIn.getAcct_no());
			bookAccoutingIn.setOpp_acct_ccy(cplIn.getCcy_code());

			// 调用内部户记账服务
			DpInsideAccountingOut bookAccountingOut = DpInsideAccountIobus.insideAccounting(bookAccoutingIn);

			cplOut.setDebit_acct_no(bookAccountingOut.getAcct_no());
			cplOut.setDebit_acct_name(bookAccountingOut.getAcct_name());
			cplOut.setDebit_ccy_code(bookAccountingOut.getCcy_code());
			cplOut.setDebit_acct_bal(bookAccountingOut.getAcct_bal());
		}
		else {
			throw APPUB.E0026(DpDict.A.debit_acct_analy.getLongName(), RouteAnalyValue.getAcct_analy().getValue());
		}

		// 贷记本金
		if (CommUtil.compare(trxnAmt, BigDecimal.ZERO) > 0) {

			DpDemandSaveIn demandSaveIn = BizUtil.getInstance(DpDemandSaveIn.class);

			demandSaveIn.setAcct_no(cplIn.getAcct_no());
			demandSaveIn.setAcct_type(cplIn.getAcct_type());
			demandSaveIn.setProd_id(null);
			demandSaveIn.setCcy_code(cplIn.getCcy_code());
			demandSaveIn.setCash_trxn_ind(cplIn.getCash_trxn_ind());
			demandSaveIn.setTrxn_amt(trxnAmt);
			demandSaveIn.setSummary_code(cplIn.getSummary_code());
			demandSaveIn.setTrxn_remark(cplIn.getTrxn_remark());
			demandSaveIn.setCustomer_remark(cplIn.getCustomer_remark());
			demandSaveIn.setOpp_acct_route(RouteAnalyValue.getAcct_analy());
			demandSaveIn.setOpp_acct_ccy(cplIn.getDebit_ccy_code());
			demandSaveIn.setOpp_acct_type(cplIn.getDebit_acct_type());
			demandSaveIn.setOpp_acct_no(cplIn.getDebit_acct_no());
			demandSaveIn.setOpp_branch_id(cplIn.getDebit_acct_branch());

			// 调用存款存入服务
			BizUtil.getInstance(SrvDpDemandAccounting.class).demandSave(demandSaveIn);
		}

		// 贷记应收收息
		if (CommUtil.compare(instAmt, BigDecimal.ZERO) > 0) {

			DpInstAccounting cplInstInfo = BizUtil.getInstance(DpInstAccounting.class);

			cplInstInfo.setInterest(instAmt);
			cplInstInfo.setInterest_tax(BigDecimal.ZERO);

			DpInterestSettlement.receivableInterestAccounting(cplInstInfo, subAcct);
		}

		bizlog.method(" DpOverDraftSettle.accounting end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月27日-下午2:46:00</li>
	 *         <li>功能说明：透支卡片清零及协议信息关闭</li>
	 *         </p>
	 */
	public static void clearSingleOdAgree(String acctNo, String agreeNo, E_YESORNO closeAgreeFlag) {

		// 登记透支协议冲账事件
		DpOverDraftAgreeClearReversalIn cplReversalIn = BizUtil.getInstance(DpOverDraftAgreeClearReversalIn.class);

		cplReversalIn.setAgree_no(agreeNo);
		cplReversalIn.setOriginal_busi_seq(BizUtil.getTrxRunEnvs().getBusi_seq());
		cplReversalIn.setOriginal_trxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		cplReversalIn.setOriginal_trxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

		if (closeAgreeFlag == E_YESORNO.YES) {

			DpbOverdraft overdraftInfo = DpbOverdraftDao.selectOne_odb1(acctNo, agreeNo, true);

			overdraftInfo.setAgree_status(E_STATUS.INVALID);
			overdraftInfo.setCancel_date(BizUtil.getTrxRunEnvs().getTrxn_date());
			overdraftInfo.setCancel_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

			DpbOverdraftDao.updateOne_odb1(overdraftInfo);
		}

		// 查询卡片信息
		List<DpbOverdraftSlip> listODFiche = DpbOverdraftSlipDao.selectAll_odb3(acctNo, agreeNo, true);

		for (DpbOverdraftSlip ODFiche : listODFiche) {

			DpaSlip ficheInst = DpaSlipDao.selectOne_odb1(acctNo, ODFiche.getFiche_no(), true);

			// 登记透支卡片冲账信息
			DpOverDraftSlipClear cplFicheReversalIn = BizUtil.getInstance(DpOverDraftSlipClear.class);

			cplFicheReversalIn.setFiche_no(ficheInst.getFiche_no());
			cplFicheReversalIn.setAccrual_inst(ficheInst.getAccrual_inst());
			cplFicheReversalIn.setAccrual_sum_bal(ficheInst.getAccrual_sum_bal());
			cplFicheReversalIn.setCur_term_inst(ficheInst.getCur_term_inst());
			cplFicheReversalIn.setCur_term_inst_sum_bal(ficheInst.getCur_term_inst_sum_bal());
			cplFicheReversalIn.setOverdue_interest(ODFiche.getOverdue_interest());

			cplReversalIn.getList01().add(cplFicheReversalIn);

			// 卡片计息定义、计息明细清零
			dealFicheInterestForDayEnd(ficheInst, E_ROUNDRULE.DOWN);

			// 更新卡片信息
			DpaSlipDao.updateOne_odb1(ficheInst);

			if (CommUtil.compare(ODFiche.getOverdue_interest(), BigDecimal.ZERO) > 0) {

				ODFiche.setOverdue_interest(BigDecimal.ZERO);

				DpbOverdraftSlipDao.updateOne_odb1(ODFiche);
			}
		}

		// 登记冲账事件
		ApReversalApi.register("overdraftAgreeClear", cplReversalIn);
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2018年2月9日-下午4:27:16</li>
	 *         <li>功能说明：单个透支协议结清</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpSingleOverdraftPayInstOut singleOverdraftAgreePayInst(DpSingleOverdraftPayInstIn cplIn) {

		bizlog.method(" DpOverDraftSettle.singleOverdraftAgreePayInst begin >>>>>>>>>>>>>>>>");

		// 必输校验
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getAgree_no(), SysDict.A.agree_no.getId(), SysDict.A.agree_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCash_trxn_ind(), SysDict.A.cash_trxn_ind.getId(), SysDict.A.cash_trxn_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getDebit_ccy_code(), SysDict.A.debit_ccy_code.getId(), SysDict.A.debit_ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		// 查询协议信息
		DpbOverdraft overdraftAgree = DpbOverdraftDao.selectOne_odb1(acctInfo.getAcct_no(), cplIn.getAgree_no(), true);

		DpbOverdraftSlip overdraftFiche = DpbOverdraftSlipDao.selectOne_odb1(overdraftAgree.getAcct_no(), cplIn.getAgree_no(), E_OVERDRAFTINSTTYPE.EXCESS, false);

		BigDecimal excessAmt = BigDecimal.ZERO;
		if (CommUtil.isNotNull(overdraftFiche)) {

			excessAmt = overdraftFiche.getOccupied_amt();
		}

		// 定位账号信息
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(overdraftAgree.getAcct_no()); // 账号
		accessIn.setCcy_code(overdraftAgree.getCcy_code()); // 货币代码
		accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND); // 定活标志
		accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL); // 存入支取标志

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 子账户查询，带锁，避免并发
		DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		// 加载客户数据区
		DpPublicCheck.addDataToCustBuffer(accessOut.getCust_no(), null);

		// 查询可用余额
		DpBalanceCalculateOut balance = DpToolsApi.getBalance(subAcct.getSub_acct_no(), subAcct.getAcct_no(), null);

		Options<DpOverDraftSettleAmount> listOdSettleAmount = new DefaultOptions<DpOverDraftSettleAmount>();

		BigDecimal trxnAmount = BigDecimal.ZERO; // 本金
		BigDecimal stillInst = BigDecimal.ZERO; // 利息

		// 获取透支协议下利息、透支余额信息
		DpOverDraftSettleAmount cplOdAmount = getSingleOdAgreeSettleInfo(subAcct, overdraftAgree);

		listOdSettleAmount.add(cplOdAmount);

		// 调用额度查询
		DpCreditLimitInfo clAccountInfo = DpCreditLimitIobus.getCreditLimitInfo(overdraftAgree.getLimit_code());

		BigDecimal sumAmt = BigDecimal.ZERO;

		if (CommUtil.compare(clAccountInfo.getDue_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) > 0) {

			sumAmt = clAccountInfo.getInit_limit_amt().add(clAccountInfo.getAdjust_amt()).add(excessAmt);
		}

		// 应还本金
		trxnAmount = trxnAmount.add(cplOdAmount.getAcct_bal());

		// 冻结处理
		if (CommUtil.compare(balance.getUsable_bal().add(trxnAmount), BigDecimal.ZERO) < 0) {

			trxnAmount = trxnAmount.add(balance.getUsable_bal().add(trxnAmount).negate());
		}

		// 应收利息之和
		stillInst = stillInst.add(cplOdAmount.getStill_inst());

		// 输出实例化
		DpSingleOverdraftPayInstOut cplOut = BizUtil.getInstance(DpSingleOverdraftPayInstOut.class);

		// 检查还款账号限制情况
		// 子账户定位输入接口
		if (CommUtil.isNotNull(cplIn.getDebit_acct_no())) {

			DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			acctAccessIn.setAcct_no(cplIn.getDebit_acct_no());
			acctAccessIn.setAcct_type(cplIn.getDebit_acct_type());
			acctAccessIn.setCcy_code(cplIn.getDebit_ccy_code());
			acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
			acctAccessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);
			acctAccessIn.setSub_acct_seq(null);

			DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

			DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

			DpPublicCheck.checkSubAcctTrxnLimit(subAccount, E_DEPTTRXNEVENT.DP_DRAW, null);

		}

		// 账务处理
		if (CommUtil.compare(trxnAmount.add(stillInst), BigDecimal.ZERO) > 0) {

			// 将本透支协议号压入公共运行区的临时数据中，后面占用额度时会用到
			String tempData = BizUtil.getTrxRunEnvs().getTemp_data();

			if (CommUtil.isNull(tempData)) {

				BizUtil.getTrxRunEnvs().setTemp_data("OD_Agree_No=".concat(cplIn.getAgree_no()));
			}
			else {

				BizUtil.getTrxRunEnvs().setTemp_data(tempData.concat("OD_Agree_No=").concat(cplIn.getAgree_no()));
			}

			// 复合类型转换
			DpOverdraftPayInstIn payInstIn = initInputData(cplIn);

			payInstIn.setAcct_no(subAcct.getAcct_no());
			payInstIn.setAcct_type(accessOut.getAcct_type());
			payInstIn.setCcy_code(subAcct.getCcy_code());
			payInstIn.setProd_id(subAcct.getProd_id());

			DpOverdraftPayInstOut payInstOut = accounting(payInstIn, subAcct, trxnAmount, stillInst);

			// 公共运行区临时数据恢复原样，，避免影响接下来的业务
			BizUtil.getTrxRunEnvs().setTemp_data(tempData);

			cplOut.setDebit_acct_bal(payInstOut.getDebit_acct_bal());
			cplOut.setAct_withdrawal_amt(payInstOut.getAct_withdrawal_amt());

		}
		else {

			ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_PAY_INTEREST.getValue());
		}

		// 清零卡片信息和关闭协议
		clearSingleOdAgree(subAcct.getAcct_no(), cplOdAmount.getAgree_no(), cplIn.getCancle_agree_ind());

		// 补充输出
		cplOut.setAcct_no(accessOut.getAcct_no());
		cplOut.setAcct_type(accessOut.getAcct_type());
		cplOut.setAcct_name(accessOut.getAcct_name());
		cplOut.setCcy_code(subAcct.getCcy_code());
		cplOut.setAgree_no(cplIn.getAgree_no());
		cplOut.setOverdraft_amt(cplOdAmount.getAcct_bal());
		cplOut.setOverdraft_interest(stillInst);
		cplOut.setOccupied_amt(cplOdAmount.getOccupied_amt());
		cplOut.setAgree_status(overdraftAgree.getAgree_status());
		cplOut.setCash_trxn_ind(E_CASHTRXN.TRXN);
		cplOut.setDebit_ccy_code(cplIn.getDebit_ccy_code());
		cplOut.setDebit_suspense_no(cplIn.getDebit_suspense_no());
		cplOut.setDebit_acct_no(cplIn.getDebit_acct_no());
		cplOut.setDebit_acct_name(cplIn.getDebit_acct_name());
		cplOut.setExch_rate(cplIn.getExch_rate());

		bizlog.method(" DpOverDraftSettle.singleOverdraftAgreePayInst end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2018年2月11日-上午10:04:53</li>
	 *         <li>功能说明：单个协议结清输入转换</li>
	 *         </p>
	 * @param cplIn
	 */
	private static DpOverdraftPayInstIn initInputData(DpSingleOverdraftPayInstIn cplIn) {
		bizlog.method(" DpOverDraftSettle.initInputData begin >>>>>>>>>>>>>>>>");

		DpOverdraftPayInstIn payInstIn = BizUtil.getInstance(DpOverdraftPayInstIn.class);

		payInstIn.setCheck_password_ind(cplIn.getCheck_password_ind()); // password
																		// form
																		// indication
		payInstIn.setTrxn_password(cplIn.getTrxn_password()); // trxn password
		payInstIn.setCancle_agree_ind(cplIn.getCancle_agree_ind()); // cancle
																	// agree ind
		payInstIn.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // cash or trxn
		payInstIn.setDebit_ccy_code(cplIn.getDebit_ccy_code()); // debit
																// currency code
		payInstIn.setDebit_suspense_no(cplIn.getDebit_suspense_no()); // the
																		// suspense
																		// number
																		// in
																		// debit
		payInstIn.setDebit_acct_no(cplIn.getDebit_acct_no()); // debit account
																// number
		payInstIn.setDebit_acct_name(cplIn.getDebit_acct_name()); // debit
																	// account
																	// name
		payInstIn.setDebit_acct_type(cplIn.getDebit_acct_type()); // debit
																	// account
																	// type
		payInstIn.setDebit_prod_id(cplIn.getDebit_prod_id()); // debit product
																// id
		payInstIn.setDebit_acct_branch(cplIn.getDebit_acct_branch()); // debit
																		// account
																		// branch
		payInstIn.setSettle_voch_type(cplIn.getSettle_voch_type()); // settle
																	// voucher
																	// type
		payInstIn.setCheque_no(cplIn.getCheque_no()); // cheque number
		payInstIn.setFroze_no(cplIn.getFroze_no()); // freeze number
		payInstIn.setUnfroze_amt(cplIn.getUnfroze_amt()); // unfroze amount
		payInstIn.setUnfroze_reason(cplIn.getUnfroze_reason()); // unfroze
																// reason
		payInstIn.setForex_agree_price_id(cplIn.getForex_agree_price_id()); // forex
																			// agree
																			// price
																			// id
		payInstIn.setBase_exch_rate(cplIn.getBase_exch_rate()); // base exch
																// rate
		payInstIn.setExch_rate(cplIn.getExch_rate()); // exch rate
		payInstIn.setExch_rate_path(cplIn.getExch_rate_path());
		payInstIn.setSummary_code(cplIn.getSummary_code()); // summary code
		payInstIn.setTrxn_remark(cplIn.getTrxn_remark()); // transaction remark
		payInstIn.setCustomer_remark(cplIn.getCustomer_remark()); // customer
																	// remark

		bizlog.method(" DpOverDraftSettle.initInputData end <<<<<<<<<<<<<<<<");

		return payInstIn;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2014年12月17日-上午10:04:54</li>
	 *         <li>功能说明：处理卡片周期性利息</li>
	 *         <li>补充说明：适用周期性结息的卡片</li>
	 *         </p>
	 * @param ficheInst
	 *            卡片计息信息
	 * @param roundRule
	 *            计息舍入规则
	 * @return 付息利息信息
	 */
	public static DpInterestCalcOut dealFicheInterestForDayEnd(DpaSlip ficheInst, E_ROUNDRULE roundRule) {

		// 实例化利息和利息税输出结果
		DpInterestCalcOut cplOut = BizUtil.getInstance(DpInterestCalcOut.class);

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 下次付息日
		String nextPayInstDate = ficheInst.getNext_pay_inst_date();

		if (CommUtil.isNotNull(ficheInst.getPay_inst_cyc())) {
			nextPayInstDate = BizUtil.calcDateByReference(ficheInst.getPay_inst_ref_date(), trxnDate, ficheInst.getPay_inst_cyc());
		}

		// 利息为负数并且是周期性结息，则不处理利息相关表的金额数据(因为倒起息对以前结息周期的利息进行调整)
		if (CommUtil.compare(ficheInst.getAccrual_inst(), BigDecimal.ZERO) <= 0) {

			ficheInst.setNext_pay_inst_date(nextPayInstDate);

			// 输出
			cplOut.setAccrual_inst(BigDecimal.ZERO);
			cplOut.setSum_bal(BigDecimal.ZERO);

			// 应计利息为零需要清空积数，因为零利率计息会累计积数
			if (CommUtil.equals(ficheInst.getAccrual_inst(), BigDecimal.ZERO)) {

				ficheInst.setAccrual_sum_bal(BigDecimal.ZERO);
			}

			// 当前积数清空
			ficheInst.setCur_term_inst(BigDecimal.ZERO);
			ficheInst.setCur_term_inst_sum_bal(BigDecimal.ZERO);
			ficheInst.setLast_inst_oper_type(E_INSTOPERATE.PAY);

			return cplOut;
		}

		// 应付利息
		BigDecimal payInstAmt = ApCurrencyApi.roundAmount(ficheInst.getCcy_code(), ficheInst.getAccrual_inst(), roundRule);

		// 输出值: 按币种取精度，是否四舍五入由交易场景决定
		cplOut.setAccrual_inst(payInstAmt);
		cplOut.setSum_bal(ficheInst.getAccrual_sum_bal());

		// 截位处理利息，且应付利息明细金额为零不处理付息明细, 否则迁移利息明细到付息明细,案例：活期周期结息
		if ((E_ROUNDRULE.DOWN == roundRule && CommUtil.equals(payInstAmt, BigDecimal.ZERO)) == false) {
			DpOverDraftApi.accruedIntoPayed(ficheInst); // 利息明细迁移到付息明细中
		}

		// 截位处理的，将尾数部分登记到应加应减利息上去
		if (E_ROUNDRULE.DOWN == roundRule) {
			// TODO:
		}

		// 计息定义表里面的相关数据清零,更新下次结息日
		clearFicheInstDefine(ficheInst, nextPayInstDate);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年7月12日-上午10:04:54</li>
	 *         <li>功能说明：结息后卡片计息定义表数据清零处理</li>
	 *         <li>补充说明：卡片支取结息、周期性结息都会使用</li>
	 *         </p>
	 * @param ficheInst
	 *            卡片计息信息
	 * @param nextPayInstDate
	 *            下次付息日
	 */
	private static void clearFicheInstDefine(DpaSlip ficheInst, String nextPayInstDate) {

		// 计息定义表里面的相关数据清零
		ficheInst.setAccrual_inst(BigDecimal.ZERO);
		ficheInst.setAccrual_sum_bal(BigDecimal.ZERO);
		ficheInst.setCur_term_inst(BigDecimal.ZERO);
		ficheInst.setCur_term_inst_sum_bal(BigDecimal.ZERO);

		// 更新计息表其他信息
		ficheInst.setLast_inst_oper_type(E_INSTOPERATE.PAY);
		ficheInst.setLast_pay_inst_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		ficheInst.setNext_pay_inst_date(nextPayInstDate);
	}
}
