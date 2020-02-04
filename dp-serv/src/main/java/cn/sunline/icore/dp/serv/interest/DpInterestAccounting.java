package cn.sunline.icore.dp.serv.interest;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpRegInstBill;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingIn;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_EXCHRATETYPE;
import cn.sunline.icore.sys.type.EnumType.E_FOREXEXCHOBJECT;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：利息账务处理相关
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
public class DpInterestAccounting {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpInterestAccounting.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年3月25日-下午4:21:53</li>
	 *         <li>功能说明：利息入自身账户</li>
	 *         <li>补充说明：是否符合入自身的条件在外面检查控制; 外层方法后面要继续使用子账户信息需要重新读取</li>
	 *         </p>
	 * @param cplIn
	 *            付息结果
	 * @param subAcct
	 *            子账户信息：计息账户本身账户信息，外面带锁传入
	 */
	public static void instIntoSelf(DpInstAccounting cplIn, DpaSubAccount subAcct) {

		bizlog.method(" DpInterestAccounting.instIntoSelf begin >>>>>>>>>>>>>>>>");

		// 利息和利息税都为零，则直接退出: 红字冲账时利息值可能为负数
		if (CommUtil.equals(cplIn.getInterest(), BigDecimal.ZERO) && CommUtil.equals(cplIn.getInterest_tax(), BigDecimal.ZERO)) {
			return;
		}

		// 税后利息
		BigDecimal instAfterTax = cplIn.getInterest().subtract(cplIn.getInterest_tax());

		// 不管定期还是活期，都做入自身处理，能否入自身的逻辑判断在外层处理
		DpUpdAccBalIn cplUpdBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplUpdBalIn.setCard_no(""); // 卡号
		cplUpdBalIn.setAcct_no(subAcct.getAcct_no()); // 账号
		cplUpdBalIn.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		cplUpdBalIn.setTrxn_event_id(""); // 交易事件ID
		cplUpdBalIn.setTrxn_ccy(subAcct.getCcy_code()); // 交易币种
		cplUpdBalIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
		cplUpdBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplUpdBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplUpdBalIn.setOpp_acct_no("");// 对手方账户登记自身账户
		cplUpdBalIn.setOpp_acct_ccy(subAcct.getCcy_code());// 对手方币种
		cplUpdBalIn.setOpp_trxn_amt(instAfterTax);
		cplUpdBalIn.setOpp_acct_route(E_ACCOUTANALY.INSIDE);// 对方账户路由
		cplUpdBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.INTEREST); // 交易明细类别=利息
		cplUpdBalIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT); // 记账方向
		cplUpdBalIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_PAY_INST")); // 摘要代码

		// 利息税相关信息
		cplUpdBalIn.setTrxn_amt(instAfterTax); // 税后利息做交易金额
		cplUpdBalIn.setInst_withholding_tax(cplIn.getInterest_tax()); // 实扣利息税
		cplUpdBalIn.setInst_tax_rate(cplIn.getInst_tax_rate()); // 税率

		DpAccounting.online(cplUpdBalIn);

		bizlog.method(" DpInterestAccounting.instIntoSelf end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月22日-下午4:21:53</li>
	 *         <li>功能说明：利息入指定账户：比如收息账户、其他指定账户</li>
	 *         <li>补充说明：是否符合入指定账户的条件在外面检查控制</li>
	 *         </p>
	 * @param cplIn
	 *            付息结果
	 * @param subAcct
	 *            子账户信息：计息账户本身账户信息，外面带锁传入
	 * @param appointAcct
	 *            指定账户信息： 外面带锁查询传入
	 */
	public static void instIntoAppointAcct(final DpInstAccounting cplIn, DpaSubAccount subAcct, String appointAcct, String appointCcy) {

		bizlog.method(" DpInterestAccounting.instIntoAppointAcct begin >>>>>>>>>>>>>>>>");

		// 利息和利息税都为零，则直接退出: 红字冲账时利息值可能为负数
		if (CommUtil.equals(cplIn.getInterest(), BigDecimal.ZERO) && CommUtil.equals(cplIn.getInterest_tax(), BigDecimal.ZERO)) {
			bizlog.method(" DpInterestAccounting.instIntoAppointAcct end <<<<<<<<<<<<<<<<");
			return;
		}

		String summaryCode = CommUtil.nvl(cplIn.getSummary_code(), ApSystemParmApi.getSummaryCode("DEPT_PAY_INST"));

		// 税后利息
		BigDecimal instAfterTax = cplIn.getInterest().subtract(cplIn.getInterest_tax());

		// 等价税后利息， 当跨币种时此值为税后利息的折算值
		BigDecimal equalInstAfterTax = instAfterTax;

		boolean regOtherBillFlag = true; // 登记利息过账单标志
		String oppSubAcctSeq = "";

		// 计息账户本身币种与指定收息账户币种不一致，做结售汇处理
		if (!CommUtil.equals(subAcct.getCcy_code(), appointCcy)) {

			DpExchangeAccountingIn cplFxIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

			cplFxIn.setBuy_cash_ind(E_CASHTRXN.TRXN); // 买入现金标志
			cplFxIn.setBuy_ccy_code(subAcct.getCcy_code()); // 买入币种
			cplFxIn.setBuy_amt(instAfterTax); // 买入金额: 净利息
			cplFxIn.setSell_cash_ind(E_CASHTRXN.TRXN); // 卖出现金标志
			cplFxIn.setSell_ccy_code(appointCcy); // 卖出币种
			cplFxIn.setSell_amt(null); // 卖出金额
			cplFxIn.setSummary_code(summaryCode); // 摘要代码
			cplFxIn.setCustomer_remark(""); // 客户备注
			cplFxIn.setTrxn_remark(""); // 交易备注
			cplFxIn.setBuy_acct_no(subAcct.getAcct_no());
			cplFxIn.setBuy_sub_acct_seq(subAcct.getSub_acct_seq());
			cplFxIn.setSell_acct_no(appointAcct);
			cplFxIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.CUSTOMER);

			// 外汇买卖中间服务
			DpExchangeAccountingOut cplFxOut = DpExchangeIobus.exchangeAccounting(cplFxIn);

			// 得到转换后的净利息
			equalInstAfterTax = cplFxOut.getSell_amt();
		}

		// 路由类型
		E_ACCOUTANALY routeType = DpInsideAccountIobus.getAccountRouteType(appointAcct);

		// 收息账户为内部户
		if (routeType == E_ACCOUTANALY.INSIDE || routeType == E_ACCOUTANALY.BUSINESE) {

			DpInsideAccountingIn cplTaAccountingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

			cplTaAccountingIn.setAcct_no(appointAcct);
			cplTaAccountingIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			cplTaAccountingIn.setCcy_code(appointCcy);
			cplTaAccountingIn.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_ACCOUTANALY.DEPOSIT : E_ACCOUTANALY.NOSTRO);
			cplTaAccountingIn.setOpp_acct_ccy(subAcct.getCcy_code());
			cplTaAccountingIn.setOpp_acct_no(subAcct.getAcct_no());
			cplTaAccountingIn.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());

			// 用不等于符号，是为了便于红字冲账时调用
			if (CommUtil.compare(equalInstAfterTax, BigDecimal.ZERO) != 0) {

				cplTaAccountingIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
				cplTaAccountingIn.setTrxn_amt(equalInstAfterTax);
				cplTaAccountingIn.setSummary_code(summaryCode);

				DpInsideAccountIobus.insideAccounting(cplTaAccountingIn);
			}
		}
		// 收息账户为存款账户
		else {

			DpAcctAccessIn cplAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			cplAccessIn.setAcct_no(appointAcct);
			cplAccessIn.setCcy_code(appointCcy);
			cplAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

			DpAcctAccessOut cplAccessOut = DpToolsApi.locateSingleSubAcct(cplAccessIn);

			oppSubAcctSeq = cplAccessOut.getSub_acct_seq();

			// 要带锁查询，后面直接记账了
			DpaSubAccount appointSubAcct = DpaSubAccountDao.selectOneWithLock_odb1(cplAccessOut.getAcct_no(), cplAccessOut.getSub_acct_no(), true);

			// 指定账户就是自身则不登记利息过账单
			if (CommUtil.equals(appointSubAcct.getSub_acct_no(), subAcct.getSub_acct_no())) {
				regOtherBillFlag = false;
			}

			// 本金记账
			DpUpdAccBalIn cplUpdBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

			cplUpdBalIn.setCard_no(cplAccessOut.getCard_no()); // 卡号
			cplUpdBalIn.setAcct_no(appointSubAcct.getAcct_no()); // 账号
			cplUpdBalIn.setSub_acct_no(appointSubAcct.getSub_acct_no()); // 子账号
			cplUpdBalIn.setTrxn_event_id(""); // 交易事件ID
			cplUpdBalIn.setTrxn_ccy(appointCcy); // 交易币种
			cplUpdBalIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
			cplUpdBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
			cplUpdBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
			cplUpdBalIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
			cplUpdBalIn.setOpp_acct_no(subAcct.getAcct_no());
			cplUpdBalIn.setOpp_acct_ccy(subAcct.getCcy_code());
			cplUpdBalIn.setOpp_trxn_amt(instAfterTax);
			cplUpdBalIn.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());
			cplUpdBalIn.setOpp_acct_name(subAcct.getSub_acct_name());
			cplUpdBalIn.setOpp_acct_type(null);
			cplUpdBalIn.setOpp_branch_id(subAcct.getSub_acct_branch());
			cplUpdBalIn.setSummary_code(summaryCode); // 摘要代码
			cplUpdBalIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT); // 记账方向
			cplUpdBalIn.setTrxn_amt(equalInstAfterTax); // 净利息
			cplUpdBalIn.setTrxn_record_type(regOtherBillFlag ? E_TRXNRECORDTYPE.NORMAL : E_TRXNRECORDTYPE.INTEREST); // 交易明细类别

			// 利息税相关信息
			if (regOtherBillFlag == false) {

				cplUpdBalIn.setInst_withholding_tax(cplIn.getInterest_tax()); // 利息税
				cplUpdBalIn.setInst_tax_rate(cplIn.getInst_tax_rate()); // 税率
			}

			DpAccounting.online(cplUpdBalIn);

		}

		// 登记利息账单接口
		if (regOtherBillFlag) {

			DpRegInstBill instBill = BizUtil.getInstance(DpRegInstBill.class);

			instBill.setAcct_no(subAcct.getAcct_no()); // 账号
			instBill.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
			instBill.setCcy_code(subAcct.getCcy_code()); // 货币代码
			instBill.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
			instBill.setInst_withholding_tax(cplIn.getInterest_tax());
			instBill.setInst_tax_rate(cplIn.getInst_tax_rate());
			instBill.setInterest(cplIn.getInterest()); // 利息
			instBill.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
			instBill.setSummary_code(summaryCode); // 摘要代码
			instBill.setShow_ind(E_YESORNO.YES); // 是否显示标志
			instBill.setOpp_acct_no(appointAcct);// 收息账户
			instBill.setOpp_acct_ccy(appointCcy);// 收息账户币种
			instBill.setOpp_acct_route(routeType);
			instBill.setOpp_sub_acct_seq(oppSubAcctSeq);
			instBill.setTrxn_remark(cplIn.getTrxn_remark());
			
			// 登记过息账单
			DpAccounting.registerOtherBill(instBill, subAcct.getAcct_no(), subAcct.getSub_acct_no());
		}

		bizlog.method(" DpInterestAccounting.instIntoAppointAcct end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年3月25日-下午4:21:53</li>
	 *         <li>功能说明：利息挂记待支取利息：外面要锁住子账户信息</li>
	 *         </p>
	 * @param cplIn
	 *            利息结果
	 * @param subAcct
	 *            子账户信息：计息账户本身账户信息，外面带锁传入
	 * @param cycPayInstFlag
	 *            周期付息标志：Yes -是周期付息 No - 不是周期付息
	 */
	public static void instIntoWaitDraw(final DpInstAccounting cplIn, DpaSubAccount subAcct, E_YESORNO cycPayInstFlag) {

		bizlog.method(" DpInterestAccounting.instIntoWaitDraw begin >>>>>>>>>>>>>>>>");

		// 利息和利息税都为零，则直接退出: 红字冲账时利息值可能为负数
		if (CommUtil.equals(cplIn.getInterest(), BigDecimal.ZERO) && CommUtil.equals(cplIn.getInterest_tax(), BigDecimal.ZERO)) {
			bizlog.method(" DpInterestAccounting.instIntoWaitDraw end <<<<<<<<<<<<<<<<");
			return;
		}

		// 待支付利息信息
		subAcct.setInst_payable(subAcct.getInst_payable().add(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplIn.getInterest())));
		subAcct.setInst_tax_payable(subAcct.getInst_tax_payable().add(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplIn.getInterest_tax())));

		// 周期性付息，登记周期待支付利息，周期结息定期户提前支取冲账只能冲这一部分利息
		if (cycPayInstFlag == E_YESORNO.YES) {

			subAcct.setCyc_inst_payable(subAcct.getCyc_inst_payable().add(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplIn.getInterest())));
			subAcct.setCyc_inst_tax_payable(subAcct.getCyc_inst_tax_payable().add(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplIn.getInterest_tax())));
		}

		// 更新子账户信息
		DpaSubAccountDao.updateOne_odb1(subAcct);

		bizlog.method(" DpInterestAccounting.instIntoWaitDraw end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月22日-下午4:21:53</li>
	 *         <li>功能说明：利息入现金</li>
	 *         </p>
	 * @param cplIn
	 *            付息结果
	 * @param subAcct
	 *            子账户信息：计息账户本身账户信息，外面带锁传入
	 * @param cashCcy
	 *            取现币种
	 */
	public static void instIntoCash(final DpInstAccounting cplIn, DpaSubAccount subAcct, String cashCcy, String custRemark, String trxnRemark) {

		bizlog.method(" DpInterestAccounting.instIntoCash begin >>>>>>>>>>>>>>>>");

		// 利息和利息税都为零，则直接退出: 红字冲账时利息值可能为负数
		if (CommUtil.equals(cplIn.getInterest(), BigDecimal.ZERO) && CommUtil.equals(cplIn.getInterest_tax(), BigDecimal.ZERO)) {
			bizlog.method(" DpInterestAccounting.instIntoCash end <<<<<<<<<<<<<<<<");
			return;
		}

		String summaryCode = CommUtil.nvl(cplIn.getSummary_code(), ApSystemParmApi.getSummaryCode("DEPT_PAY_INST"));

		// 税后利息
		BigDecimal instAfterTax = cplIn.getInterest().subtract(cplIn.getInterest_tax());

		// 等价税后利息， 当跨币种时此值为税后利息的折算值
		BigDecimal equalInstAfterTax = instAfterTax;

		// 登记利息账单接口
		DpRegInstBill instBill = BizUtil.getInstance(DpRegInstBill.class);

		instBill.setAcct_no(subAcct.getAcct_no()); // 账号
		instBill.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		instBill.setCcy_code(subAcct.getCcy_code()); // 货币代码
		instBill.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
		instBill.setInst_tax_rate(cplIn.getInst_tax_rate());
		instBill.setInst_withholding_tax(cplIn.getInterest_tax());
		instBill.setInterest(cplIn.getInterest()); // 利息
		instBill.setCash_trxn_ind(E_CASHTRXN.CASH); // 现转标志
		instBill.setShow_ind(E_YESORNO.YES); // 是否显示标志
		instBill.setOpp_acct_ccy(subAcct.getCcy_code());
		instBill.setOpp_acct_route(E_ACCOUTANALY.CASH);
		instBill.setSummary_code(summaryCode); // 摘要代码
		instBill.setTrxn_remark(trxnRemark);
		instBill.setCustomer_remark(custRemark);

		// 登记过息账单
		DpAccounting.registerOtherBill(instBill, subAcct.getAcct_no(), subAcct.getSub_acct_no());

		// 计息账户本身币种与指定收息账户币种不一致，做结售汇处理
		if (!CommUtil.equals(subAcct.getCcy_code(), cashCcy)) {

			DpExchangeAccountingIn cplFxIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

			cplFxIn.setBuy_cash_ind(E_CASHTRXN.TRXN); // 买入现金标志
			cplFxIn.setBuy_ccy_code(subAcct.getCcy_code()); // 买入币种
			cplFxIn.setBuy_amt(instAfterTax); // 买入金额: 净利息
			cplFxIn.setSell_cash_ind(E_CASHTRXN.CASH); // 卖出现金标志
			cplFxIn.setSell_ccy_code(cashCcy); // 卖出币种
			cplFxIn.setSell_amt(null); // 卖出金额
			cplFxIn.setSummary_code(summaryCode); // 摘要代码
			cplFxIn.setCustomer_remark(custRemark); // 客户备注
			cplFxIn.setTrxn_remark(trxnRemark); // 交易备注
			cplFxIn.setBuy_acct_no(subAcct.getAcct_no());
			cplFxIn.setBuy_sub_acct_seq(subAcct.getSub_acct_seq());
			cplFxIn.setSell_acct_no(null);
			cplFxIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.CUSTOMER);
			cplFxIn.setExch_rate_path(subAcct.getCcy_code().concat("/").concat(cashCcy));
			cplFxIn.setExch_rate_type(E_EXCHRATETYPE.EXCHANGE);

			// 外汇买卖中间服务
			DpExchangeAccountingOut cplFxOut = DpExchangeIobus.exchangeAccounting(cplFxIn);

			// 得到转换后的利息
			equalInstAfterTax = cplFxOut.getSell_amt();
		}

		DpInsideAccountingIn cplTaAccountingIn = BizUtil.getInstance(DpInsideAccountingIn.class);

		cplTaAccountingIn.setAcct_no(null);
		cplTaAccountingIn.setCash_trxn_ind(E_CASHTRXN.CASH);
		cplTaAccountingIn.setCcy_code(cashCcy);
		cplTaAccountingIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
		cplTaAccountingIn.setTrxn_amt(equalInstAfterTax);
		cplTaAccountingIn.setSummary_code(summaryCode);
		cplTaAccountingIn.setOpp_acct_ccy(subAcct.getCcy_code());
		cplTaAccountingIn.setOpp_acct_no(subAcct.getAcct_no());
		cplTaAccountingIn.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_ACCOUTANALY.DEPOSIT : E_ACCOUTANALY.NOSTRO);
		cplTaAccountingIn.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());

		DpInsideAccountIobus.insideAccounting(cplTaAccountingIn);

		bizlog.method(" DpInterestAccounting.instIntoCash end <<<<<<<<<<<<<<<<");
	}
}