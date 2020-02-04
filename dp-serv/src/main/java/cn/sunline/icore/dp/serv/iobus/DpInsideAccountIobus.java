package cn.sunline.icore.dp.serv.iobus;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.icore.ap.type.ComApBasic.ApAccountRouteInfo;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersSignIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAccountRouteInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpSuspenseAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpSuspenseAccountingOut;
import cn.sunline.icore.iobus.cm.servicetype.SrvIoCmTaAccounting;
import cn.sunline.icore.iobus.cm.type.ComIoCmTaAccounting.IoTaBookAccountingIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmTaAccounting.IoTaBookAccountingOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmTaAccounting.IoTaBookSuspenseIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmTaAccounting.IoTaBookSuspenseOut;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpInsideAccountIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpInsideAccountIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：暂收暂付记账</li>
	 *         </p>
	 * @param cplIn
	 *            内部户记账服务输入
	 * @return 内部户记账服务输出
	 */
	public static DpInsideAccountingOut insideAccounting(DpInsideAccountingIn cplIn) {

		IoTaBookAccountingIn bookAccoutingIn = BizUtil.getInstance(IoTaBookAccountingIn.class);

		bookAccoutingIn.setAcct_branch(cplIn.getAcct_branch());
		bookAccoutingIn.setAcct_no(cplIn.getAcct_no());
		bookAccoutingIn.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		bookAccoutingIn.setCcy_code(cplIn.getCcy_code());
		bookAccoutingIn.setTrxn_amt(cplIn.getTrxn_amt());
		bookAccoutingIn.setDebit_credit(cplIn.getDebit_credit());
		bookAccoutingIn.setGl_ref_code(cplIn.getGl_ref_code());
		bookAccoutingIn.setSummary_code(cplIn.getSummary_code());
		bookAccoutingIn.setTrxn_remark(cplIn.getTrxn_remark());
		bookAccoutingIn.setCustomer_remark(cplIn.getCustomer_remark());

		// 真实对手方信息
		bookAccoutingIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		bookAccoutingIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		bookAccoutingIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		bookAccoutingIn.setOpp_acct_name(cplIn.getOpp_acct_name());

		IoTaBookAccountingOut bookAccoutingOut = SysUtil.getRemoteInstance(SrvIoCmTaAccounting.class).bookAccountingWithCheck(bookAccoutingIn);

		// 输出
		DpInsideAccountingOut cplOut = BizUtil.getInstance(DpInsideAccountingOut.class);

		cplOut.setAcct_name(bookAccoutingOut.getAcct_name());
		cplOut.setAcct_no(bookAccoutingOut.getAcct_no());
		cplOut.setGl_ref_code(bookAccoutingOut.getGl_ref_code());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：挂销账记账</li>
	 *         </p>
	 * @param cplIn
	 *            挂销账记账服务输入
	 * @return 挂销账记账服务输出
	 */
	public static DpSuspenseAccountingOut suspenseAccounting(DpSuspenseAccountingIn cplIn) {

		// 挂销账记账输入
		IoTaBookSuspenseIn suspenseIn = BizUtil.getInstance(IoTaBookSuspenseIn.class);

		suspenseIn.setAcct_branch(cplIn.getAcct_branch());
		suspenseIn.setSuspense_no(cplIn.getSuspense_no());
		suspenseIn.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		suspenseIn.setCcy_code(cplIn.getCcy_code());
		suspenseIn.setTrxn_amt(cplIn.getTrxn_amt());
		suspenseIn.setDebit_credit(cplIn.getDebit_credit());
		suspenseIn.setGl_ref_code(cplIn.getGl_ref_code());
		suspenseIn.setSummary_code(cplIn.getSummary_code());
		suspenseIn.setTrxn_remark(cplIn.getTrxn_remark());
		suspenseIn.setCustomer_remark(cplIn.getCustomer_remark());

		// 挂账方
		if (CommUtil.isNull(cplIn.getSuspense_no())) {

			suspenseIn.setSusp_acct_no(cplIn.getOpp_acct_no());
			suspenseIn.setSusp_cust_name(cplIn.getOpp_acct_name());
			suspenseIn.setSusp_cust_no(cplIn.getOpp_cust_no());
		}

		// 真实对手方信息
		suspenseIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		suspenseIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		suspenseIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		suspenseIn.setOpp_acct_name(cplIn.getOpp_acct_name());

		IoTaBookSuspenseOut suspenseOut = SysUtil.getRemoteInstance(SrvIoCmTaAccounting.class).bookSuspenseWithCheck(suspenseIn);

		// 输出
		DpSuspenseAccountingOut cplOut = BizUtil.getInstance(DpSuspenseAccountingOut.class);

		cplOut.setAcct_name(suspenseOut.getAcct_name());
		cplOut.setAcct_no(suspenseOut.getAcct_no());
		cplOut.setGl_ref_code(suspenseOut.getGl_ref_code());
		cplOut.setSuspense_no(suspenseOut.getSuspense_no());

		return cplOut;
	}
	
	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取账户路由类型</li>
	 *         </p>
	 * @param sAcctNo
	 *            账号
	 * @return 账户路由类型
	 */
	public static E_ACCOUTANALY getAccountRouteType(String sAcctNo) {

		return getAccountRouteInfo(sAcctNo, E_CASHTRXN.TRXN, null).getAcct_analy();
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取账户路由信息</li>
	 *         </p>
	 * @param sAcctNo
	 *            账号
	 * @param cashTrxnInd
	 *            现转标志
	 * @return 账户路由信息
	 */
	public static DpAccountRouteInfo getAccountRouteInfo(String sAcctNo, E_CASHTRXN cashTrxnInd) {

		return getAccountRouteInfo(sAcctNo, cashTrxnInd, null);
	}
	

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取账户路由信息</li>
	 *         </p>
	 * @param sAcctNo
	 *            账号
	 * @param cashTrxnInd
	 *            现转标志
	 * @param suspenseNo
	 *            挂账编号
	 * @return 账户路由信息
	 */
	public static DpAccountRouteInfo getAccountRouteInfo(String sAcctNo, E_CASHTRXN cashTrxnInd, String suspenseNo) {

		DpAccountRouteInfo cplOut = BizUtil.getInstance(DpAccountRouteInfo.class);

		if (cashTrxnInd == E_CASHTRXN.CASH) {

			cplOut.setAcct_analy(E_ACCOUTANALY.CASH);

			return cplOut;
		}

		ApAccountRouteInfo cplRoutInfo = BizUtil.getAccountRouteInfo(sAcctNo, cashTrxnInd, suspenseNo);

		cplOut.setAcct_analy(cplRoutInfo.getAcct_analy());
		cplOut.setAcct_no(cplRoutInfo.getAcct_no());
		cplOut.setGl_ref_code(cplRoutInfo.getGl_ref_code());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：暂收暂付记账检查</li>
	 *         </p>
	 * @param cplIn
	 *            内部户记账服务输入
	 * @return 内部户记账服务输出
	 */
	public static void checkInsideAccounting(DpInsideAccountingIn cplIn) {

		IoTaBookAccountingIn bookAccoutingIn = BizUtil.getInstance(IoTaBookAccountingIn.class);

		bookAccoutingIn.setAcct_branch(cplIn.getAcct_branch());
		bookAccoutingIn.setAcct_no(cplIn.getAcct_no());
		bookAccoutingIn.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		bookAccoutingIn.setCcy_code(cplIn.getCcy_code());
		bookAccoutingIn.setTrxn_amt(cplIn.getTrxn_amt());
		bookAccoutingIn.setDebit_credit(cplIn.getDebit_credit());
		bookAccoutingIn.setGl_ref_code(cplIn.getGl_ref_code());
		bookAccoutingIn.setSummary_code(cplIn.getSummary_code());
		bookAccoutingIn.setTrxn_remark(cplIn.getTrxn_remark());
		bookAccoutingIn.setCustomer_remark(cplIn.getCustomer_remark());

		// 真实对手方信息
		bookAccoutingIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		bookAccoutingIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		bookAccoutingIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		bookAccoutingIn.setOpp_acct_name(cplIn.getOpp_acct_name());

		// 调用内部户记账检查服务
		SysUtil.getRemoteInstance(SrvIoCmTaAccounting.class).checkBookAccounting(bookAccoutingIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：挂销账记账检查</li>
	 *         </p>
	 * @param cplIn
	 *            挂销账记账服务输入
	 * @return 挂销账记账服务输出
	 */
	public static void checkSuspenseAccounting(DpSuspenseAccountingIn cplIn) {

		// 挂销账记账输入
		IoTaBookSuspenseIn suspenseIn = BizUtil.getInstance(IoTaBookSuspenseIn.class);

		suspenseIn.setAcct_branch(cplIn.getAcct_branch());
		suspenseIn.setSuspense_no(cplIn.getSuspense_no());
		suspenseIn.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		suspenseIn.setCcy_code(cplIn.getCcy_code());
		suspenseIn.setTrxn_amt(cplIn.getTrxn_amt());
		suspenseIn.setDebit_credit(cplIn.getDebit_credit());
		suspenseIn.setGl_ref_code(cplIn.getGl_ref_code());
		suspenseIn.setSummary_code(cplIn.getSummary_code());
		suspenseIn.setTrxn_remark(cplIn.getTrxn_remark());
		suspenseIn.setCustomer_remark(cplIn.getCustomer_remark());

		// 挂账方
		if (CommUtil.isNull(cplIn.getSuspense_no())) {

			suspenseIn.setSusp_acct_no(cplIn.getOpp_acct_no());
			suspenseIn.setSusp_cust_name(cplIn.getOpp_acct_name());
			suspenseIn.setSusp_cust_no(cplIn.getOpp_cust_no());
		}

		// 真实对手方信息
		suspenseIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		suspenseIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		suspenseIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		suspenseIn.setOpp_acct_name(cplIn.getOpp_acct_name());

		// 调用挂销账记账检查服务
		SysUtil.getRemoteInstance(SrvIoCmTaAccounting.class).checkBookSuspense(suspenseIn);
	}
	/**
	 * @Author lfl
	 *         <p>
	 *         <li>2020年1月8日-上午9:27:08</li>
	 *         <li>功能说明：检查业务编码是否存在</li>
	 *         </p>
	 */
	public static void checkBusiCode(DpAgreeTransfersSignIn cplIn){
		
	}
}