package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ACCTBUSITYPE;
import cn.sunline.icore.iobus.dp.dict.IoDpDict;
import cn.sunline.icore.iobus.dp.type.ComIoDpDemandAccounting.IoDpDeductInOrderInput;
import cn.sunline.icore.iobus.dp.type.ComIoDpDemandAccounting.IoDpDeductInOrderOutput;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：指定次序扣款
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2019年1月10日-下午4:06:32</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpDeductInOrder {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDeductInOrder.class);
	private static final int maxSubCounts = 2; // 最大一次性扣款子户数
	private static final String DefaultOrder = "A"; // 默认子户扣款
	private static final String AppointOrder = "B"; // 指定子户扣款
	private static final String DefaultToAppointOrder = "C"; // 先默认子户后指定子户扣款
	private static final String OpenTimeOrder = "D"; // 按开户先后顺序扣款

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年8月8日-下午4:06:39</li>
	 *         <li>功能说明：指定次序扣款：含检查</li>
	 *         </p>
	 * @param cplIn
	 *            指定次序扣款输入接口
	 * @return 指定次序扣款输出接口
	 */
	public static IoDpDeductInOrderOutput doMain(IoDpDeductInOrderInput cplIn) {

		bizlog.method(" DpDeductInOrder.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 输入接口必输检查
		inputDataCheck(cplIn);

		DpaAccount account = BizUtil.getInstance(DpaAccount.class);

		// 获取账户信息，带锁防止并发解冻
		if (CommUtil.isNotNull(cplIn.getAcct_no())) {

			account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, true);
		}
		else {

			DpaAccountRelate acctRelte = DpPublicCheck.getDefaultAccount(cplIn.getCust_no());

			if (acctRelte == null) {
				throw DpErr.Dp.E0006(cplIn.getCust_no());
			}

			account = DpaAccountDao.selectOneWithLock_odb1(acctRelte.getAcct_no(), true);
		}

		// 获取扣款子户信息列表
		List<DpaAccountRelate> listAcctRelate = getDeductAcctList(cplIn, account);

		// 扣款子账户数量大于1，那么必然是有多少扣多少才行
		if (listAcctRelate.size() > 1) {
			cplIn.setForce_draw_ind(E_YESORNO.YES);
		}

		// 剩余扣款金额
		BigDecimal remainDeductAmt = cplIn.getTrxn_amt();
		// 实际扣款金额
		BigDecimal actualDeductAmt = BigDecimal.ZERO;

		// 依次扣款处理： 不考虑扣款时还收手续费
		for (DpaAccountRelate acctRelate : listAcctRelate) {

			// 子账户信息，上锁
			DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctRelate.getAcct_no(), acctRelate.getSub_acct_no(), true);

			// 活期支取服务输入接口
			DpDemandDrawIn cplDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			cplDrawIn.setAcct_no(acctRelate.getAcct_no());
			cplDrawIn.setAcct_name(cplIn.getAcct_name());
			cplDrawIn.setAcct_type(acctRelate.getAcct_type());
			cplDrawIn.setBack_value_date(cplIn.getBack_value_date());
			cplDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			cplDrawIn.setCcy_code(acctRelate.getCcy_code());
			cplDrawIn.setCheck_password_ind(E_YESORNO.NO);
			cplDrawIn.setChrg_code(cplIn.getChrg_code());
			cplDrawIn.setCustomer_remark(cplIn.getCustomer_remark());
			cplDrawIn.setForce_draw_ind(cplIn.getForce_draw_ind());
			cplDrawIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
			cplDrawIn.setOpp_acct_no(cplIn.getOpp_acct_no());
			cplDrawIn.setOpp_acct_route(cplIn.getOpp_acct_route());
			cplDrawIn.setOpp_branch_id(cplIn.getOpp_branch_id());
			cplDrawIn.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
			cplDrawIn.setOpp_trxn_amt(cplIn.getOpp_trxn_amt());
			cplDrawIn.setProd_id(acctRelate.getProd_id());
			cplDrawIn.setSummary_code(cplIn.getSummary_code());
			cplDrawIn.setTrxn_amt(remainDeductAmt);
			cplDrawIn.setTrxn_remark(cplIn.getTrxn_remark());
			cplDrawIn.setVat_amt(cplIn.getVat_amt());
			cplDrawIn.setVat_rate(cplIn.getVat_rate());
			cplDrawIn.setWithdrawal_busi_type(cplIn.getWithdrawal_busi_type());
			cplDrawIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
			cplDrawIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
			cplDrawIn.setReal_opp_country(cplIn.getReal_opp_country());
			cplDrawIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
			cplDrawIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
			cplDrawIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
			cplDrawIn.setReal_opp_remark(cplIn.getReal_opp_remark());

			// 属性到期自动刷新：提交数据库
			DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.YES);

			// 活期支取检查
			DpDemandDrawCheck.checkMainMethod(cplDrawIn, account, subAccount);

			// 活期支取处理
			DpDemandDrawOut cplDrawOut = DpDemandDraw.doMainMethod(cplDrawIn, account, subAccount);

			// 更新剩余扣款金额
			remainDeductAmt = remainDeductAmt.subtract(cplDrawOut.getAct_withdrawal_amt());

			// 更新实际扣款金额
			actualDeductAmt = actualDeductAmt.add(cplDrawOut.getAct_withdrawal_amt());

			// 扣款提前完成则退出
			if (CommUtil.compare(remainDeductAmt, BigDecimal.ZERO) <= 0) {
				break;
			}
		}

		// 补充输出
		IoDpDeductInOrderOutput cplOut = BizUtil.getInstance(IoDpDeductInOrderOutput.class);

		cplOut.setAcct_no(cplIn.getAcct_no());
		cplOut.setAcct_name(account.getAcct_name());
		cplOut.setTrxn_ccy(cplIn.getCcy_code());
		cplOut.setTrxn_amt(actualDeductAmt);
		cplOut.setAcct_branch(account.getAcct_branch());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpDeductInOrder.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年8月8日-下午4:06:39</li>
	 *         <li>功能说明：输入数据检查</li>
	 *         </p>
	 * @param cplIn
	 *            指定次序扣款输入接口
	 */
	private static void inputDataCheck(IoDpDeductInOrderInput cplIn) {

		// 客户号和账号不能同时为空
		if (CommUtil.isNull(cplIn.getCust_no()) && CommUtil.isNull(cplIn.getAcct_no())) {
			throw DpErr.Dp.E0008();
		}

		BizUtil.fieldNotNull(cplIn.getDeduct_order(), IoDpDict.A.deduct_order.getId(), IoDpDict.A.deduct_order.getLongName());

		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年8月8日-下午4:06:39</li>
	 *         <li>功能说明：获取扣款子户列表</li>
	 *         </p>
	 * @param cplIn
	 *            指定次序扣款输入接口
	 * @param acctInfo
	 *            账户信息
	 * @return 扣款子账户列表
	 */
	private static List<DpaAccountRelate> getDeductAcctList(IoDpDeductInOrderInput cplIn, DpaAccount acctInfo) {

		List<DpaAccountRelate> listAcctRelate = new ArrayList<DpaAccountRelate>();

		// 不用枚举是为了方便扩展，发布在Iobus上的服务， 如果发版会影响到多个子系统
		if (CommUtil.equals(cplIn.getDeduct_order(), DefaultOrder)) {

			DpaAccountRelate acctRelate = getDefaultAcct(cplIn, acctInfo);

			if (acctRelate != null) {
				listAcctRelate.add(acctRelate);
			}
		}
		else if (CommUtil.equals(cplIn.getDeduct_order(), AppointOrder)) {

			listAcctRelate.add(getAppointAcct(cplIn, acctInfo));
		}
		else if (CommUtil.equals(cplIn.getDeduct_order(), DefaultToAppointOrder)) {

			DpaAccountRelate acctRelate = getDefaultAcct(cplIn, acctInfo);

			if (acctRelate != null) {
				listAcctRelate.add(acctRelate);
			}

			listAcctRelate.add(getAppointAcct(cplIn, acctInfo));
		}
		else if (CommUtil.equals(cplIn.getDeduct_order(), OpenTimeOrder)) {

			listAcctRelate.addAll(getOpenTimeAcctList(cplIn, acctInfo));
		}
		else {

			throw APPUB.E0026("deduction order", cplIn.getDeduct_order());
		}

		return listAcctRelate;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年8月8日-下午4:06:39</li>
	 *         <li>功能说明：获取默认扣款子户</li>
	 *         </p>
	 * @param cplIn
	 *            指定次序扣款输入接口
	 * @param acctInfo
	 *            账户信息
	 * @return 扣款子账户
	 */
	private static DpaAccountRelate getDefaultAcct(IoDpDeductInOrderInput cplIn, DpaAccount acctInfo) {

		// 查询状态正常的默认子户
		List<DpaAccountRelate> listAcctRelate = DpaAccountRelateDao.selectAll_odb8(acctInfo.getAcct_no(), E_YESORNO.YES, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT, false);

		for (DpaAccountRelate acctRelate : listAcctRelate) {

			if (acctRelate.getDd_td_ind() == E_DEMANDORTIME.DEMAND && CommUtil.equals(acctRelate.getCcy_code(), cplIn.getCcy_code())) {
				return acctRelate;
			}
		}

		return null;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年8月8日-下午4:06:39</li>
	 *         <li>功能说明：获取指定扣款子户</li>
	 *         </p>
	 * @param cplIn
	 *            指定次序扣款输入接口
	 * @param acctInfo
	 *            账户信息
	 * @return 扣款子账户
	 */
	private static DpaAccountRelate getAppointAcct(IoDpDeductInOrderInput cplIn, DpaAccount acctInfo) {

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(acctInfo.getAcct_no());
		acctAccessIn.setAcct_type(acctInfo.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
		acctAccessIn.setProd_id(cplIn.getProd_id());
		acctAccessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);
		acctAccessIn.setSub_acct_seq(null);

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 返回指定子户信息
		DpaAccountRelate acctRelate = BizUtil.getInstance(DpaAccountRelate.class);

		acctRelate.setAcct_no(acctInfo.getAcct_no());
		acctRelate.setAcct_busi_type(E_ACCTBUSITYPE.DEPOSIT);
		acctRelate.setCcy_code(acctAccessOut.getCcy_code());
		acctRelate.setAcct_type(acctInfo.getAcct_type());
		acctRelate.setDd_td_ind(E_DEMANDORTIME.DEMAND);
		acctRelate.setDefault_ind(E_YESORNO.NO);
		acctRelate.setProd_id(acctAccessOut.getProd_id());
		acctRelate.setSub_acct_no(acctAccessOut.getSub_acct_no());
		acctRelate.setSub_acct_seq(acctAccessOut.getSub_acct_seq());
		acctRelate.setAcct_status(acctAccessOut.getAcct_status());

		return acctRelate;

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年8月8日-下午4:06:39</li>
	 *         <li>功能说明：获取扣款子户列表</li>
	 *         </p>
	 * @param cplIn
	 *            指定次序扣款输入接口
	 * @param acctInfo
	 *            账户信息
	 * @return 扣款子账户列表
	 */
	private static List<DpaAccountRelate> getOpenTimeAcctList(IoDpDeductInOrderInput cplIn, DpaAccount acctInfo) {

		List<DpaAccountRelate> listAcctOut = new ArrayList<DpaAccountRelate>();

		// 查询状态正常的活期子户
		List<DpaAccountRelate> listAcctRelate = DpaAccountRelateDao.selectAll_odb6(acctInfo.getAcct_no(), cplIn.getCcy_code(), E_DEMANDORTIME.DEMAND, E_ACCTSTATUS.NORMAL,
				E_ACCTBUSITYPE.DEPOSIT, false);

		// 按开户先后顺序排序
		BizUtil.listSort(listAcctRelate, true, SysDict.A.sub_acct_seq.getId());

		int count = 0;
		for (DpaAccountRelate acctRalate : listAcctRelate) {

			// 读取产品信息
			DpfBase baseProdInfo = DpProductFactoryApi.getProdBaseInfo(acctRalate.getProd_id());

			// 协议产品不参与按次序还款，比如亲情钱包子户
			if (baseProdInfo.getAgree_prod_ind() == E_YESORNO.YES) {
				continue;
			}

			count++;

			listAcctOut.add(acctRalate);

			// 最多只支持两条子户扣款
			if (count >= maxSubCounts) {
				break;
			}
		}

		return listAcctOut;
	}

}
