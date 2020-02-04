package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.interest.DpInterestSettlement;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpRegInstBill;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpWaitDrawInterestIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpWaitDrawInterestOut;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 待支取利息支取服务程序
 * </p>
 * 
 * @Author linshiq
 *         <p>
 *         <li>2017年3月13日-上午10:14:43</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月13日-linshiq：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpWaitDrawInterest {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpWaitDrawInterest.class);

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月10日-下午5:09:24</li>
	 *         <li>功能说明：待支取利息处理服务</li>
	 *         </p>
	 * @param cplIn
	 *            待支取利息输入接口
	 * @return 待支取利息支取输出接口
	 */
	public static DpWaitDrawInterestOut doMain(DpWaitDrawInterestIn cplIn) {

		bizlog.method(" DpWaitDrawInterest.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 定位账号，带锁，避免并发解冻或冻结
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 定位子账号
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no()); // 账号
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq()); // 子账户序号
		accessIn.setCcy_code(cplIn.getCcy_code()); // 货币代码
		accessIn.setAcct_type(cplIn.getAcct_type()); // 账户类型
		accessIn.setProd_id(cplIn.getProd_id()); // 产品编号
		accessIn.setDd_td_ind(null); // 定活标志
		accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL); // 存入支取标志

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 子账户查询，带锁，避免并发解冻或冻结
		DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		// 属性到期自动刷新
		DpAttrRefresh.refreshAttrValue(subAcct, acctInfo, cplIn.getAcct_no(), E_YESORNO.YES);

		// 主调检查方法
		DpWaitDrawInterestCheck.checkMainMethod(cplIn, acctInfo, subAcct);

		// 主调处理方法
		DpWaitDrawInterestOut cplOut = doMainMethod(cplIn, acctInfo, subAcct);

		bizlog.debug("<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpWaitDrawInterest.doMain end <<<<<<<<<<<<<<<<");
		return cplOut;

	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月10日-下午4:34:51</li>
	 *         <li>功能说明：待支取利息主检查方法</li>
	 *         </p>
	 * @param cplIn
	 *            待支取利息输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 */
	private static DpWaitDrawInterestOut doMainMethod(DpWaitDrawInterestIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		// 实例化输出接口
		DpWaitDrawInterestOut cplOut = BizUtil.getInstance(DpWaitDrawInterestOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplOut.setAcct_branch(subAcct.getSub_acct_branch()); // 账务机构
		cplOut.setAcct_branch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name()); // 账务机构名称
		cplOut.setAcct_type(acctInfo.getAcct_type()); // 账户类型
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(CommUtil.nvl(cplIn.getSub_acct_seq(), subAcct.getSub_acct_seq())); // 子账户序号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setProd_id(subAcct.getProd_id()); // 产品编号
		cplOut.setProd_name(DpProductFactoryApi.getProdBaseInfo(subAcct.getProd_id()).getProd_name()); // 产品名称
		cplOut.setCust_no(subAcct.getCust_no()); // 客户号
		cplOut.setDd_td_ind(subAcct.getDd_td_ind()); // 定活标志

		// 待支付利息记账
		DpInstAccounting cplInstTax = accountingTally(subAcct);

		// 登记利息过账单: 要放在记账前面登记
		registerBill(subAcct, cplIn, cplInstTax);

		cplOut.setInterest(cplInstTax.getInterest()); // 利息
		cplOut.setInterest_tax(cplInstTax.getInterest_tax()); // 利息税
		cplOut.setTax_after_inst_amt(cplInstTax.getInterest().subtract(cplInstTax.getInterest_tax())); // 税后利息

		return cplOut;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月13日-下午2:47:06</li>
	 *         <li>功能说明：待支付利息记账</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @return 利息及利息税
	 */
	public static DpInstAccounting accountingTally(DpaSubAccount subAcct) {

		bizlog.method(" DpWaitDrawInterest.accountingTally begin >>>>>>>>>>>>>>>>");

		// 1. 利息和利息税记账
		DpInstAccounting cplIn = BizUtil.getInstance(DpInstAccounting.class);

		cplIn.setInterest(subAcct.getInst_payable());
		cplIn.setInterest_tax(subAcct.getInst_tax_payable());
		cplIn.setInst_tax_rate(BigDecimal.ZERO);

		// 利息和利息税记账
		DpInterestSettlement.payInterestAccounting(cplIn, subAcct);

		// 待支付利息信息处理
		DpInterestBasicApi.waitDrawInstInfoDeal(subAcct, E_YESORNO.YES);

		bizlog.method(" DpWaitDrawInterest.accountingTally end <<<<<<<<<<<<<<<<");
		return cplIn;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月16日-下午2:29:05</li>
	 *         <li>功能说明：登记付息账单</li>
	 *         </p>
	 * @param subAcct
	 *            存款子账户表
	 * @param cplIn
	 *            待支取利息支取输入接口
	 * @param cplInstTax
	 *            利息税信息
	 */
	private static void registerBill(DpaSubAccount subAcct, DpWaitDrawInterestIn cplIn, DpInstAccounting cplInstTax) {

		bizlog.method(" DpWaitDrawInterest.registerBill begin >>>>>>>>>>>>>>>>");

		// 登记利息账单接口
		DpRegInstBill instBill = BizUtil.getInstance(DpRegInstBill.class);

		instBill.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAcct.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		instBill.setBack_value_date(null); // 倒起息日
		instBill.setReversal_type(null); // 冲正类型
		instBill.setAcct_no(subAcct.getAcct_no()); // 账号
		instBill.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		instBill.setCcy_code(subAcct.getCcy_code()); // 货币代码
		instBill.setSub_acct_seq(CommUtil.nvl(cplIn.getSub_acct_seq(), subAcct.getSub_acct_seq())); // 子账户序号
		instBill.setInst_withholding_tax(cplInstTax.getInterest_tax());
		instBill.setInterest(cplInstTax.getInterest()); // 利息
		instBill.setInst_tax_rate(cplInstTax.getInst_tax_rate());
		instBill.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
		instBill.setShow_ind(E_YESORNO.YES); // 是否显示标志
		instBill.setSummary_code(cplIn.getSummary_code()); // 摘要代码
		instBill.setOpp_acct_route(cplIn.getOpp_acct_route()); // 对方账户路由
		instBill.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
		instBill.setOpp_acct_type(cplIn.getOpp_acct_type()); // 对方账户类型
		instBill.setOpp_acct_ccy(cplIn.getOpp_acct_ccy()); // 对方账户币种
		instBill.setOpp_branch_id(cplIn.getOpp_branch_id()); // 对方机构号
		instBill.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
		instBill.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
		instBill.setAgent_name(cplIn.getAgent_name()); // 代理人姓名
		instBill.setAgent_doc_type(cplIn.getAgent_doc_type()); // 代理人证件类型
		instBill.setAgent_doc_no(cplIn.getAgent_doc_no()); // 代理人证件号
		instBill.setAgent_country(cplIn.getAgent_country()); // 代理人国籍

		// 登记过息账单
		DpAccounting.registerOtherBill(instBill, subAcct.getAcct_no(), subAcct.getSub_acct_no());

		bizlog.method(" DpWaitDrawInterest.registerBill end <<<<<<<<<<<<<<<<");
	}
}
