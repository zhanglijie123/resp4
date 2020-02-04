package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpTaxApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpTaxBase.DpIntTaxInfo;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpWaitDrawInterestIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpWaitDrawInterestOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 待支取利息支取检查服务程序
 * </p>
 * 
 * @Author linshiq
 *         <p>
 *         <li>2017年3月13日-上午10:15:05</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月13日-linshiq：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpWaitDrawInterestCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpWaitDrawInterestCheck.class);

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月10日-下午4:34:51</li>
	 *         <li>功能说明：待支取利息检查服务</li>
	 *         </p>
	 * @param cplIn
	 *            待支取利息输入接口
	 * @return 待支取利息支取输出接口
	 */
	public static DpWaitDrawInterestOut checkMain(DpWaitDrawInterestIn cplIn) {

		bizlog.method(" DpWaitDrawInterestCheck.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>>cplIn=[%s]", cplIn);

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

		// 属性到期自动刷新: 不提交数据库
		DpAttrRefresh.refreshAttrValue(subAcct, acctInfo, cplIn.getAcct_no(), E_YESORNO.NO);

		// 主调检查
		checkMainMethod(cplIn, acctInfo, subAcct);

		// 输出
		DpWaitDrawInterestOut cplOut = BizUtil.getInstance(DpWaitDrawInterestOut.class);

		cplOut.setCard_no(accessOut.getCard_no()); // 卡号
		cplOut.setAcct_branch(accessOut.getAcct_branch()); // 账务机构
		cplOut.setAcct_branch_name(ApBranchApi.getItem(accessOut.getAcct_branch()).getBranch_name());// 账务机构名称
		cplOut.setAcct_type(accessOut.getAcct_type()); // 账户类型
		cplOut.setAcct_no(subAcct.getAcct_no()); // 账号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(accessOut.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setProd_id(subAcct.getProd_id()); // 产品编号
		cplOut.setCust_no(subAcct.getCust_no()); // 客户号
		cplOut.setInterest(subAcct.getInst_payable()); // 利息
		cplOut.setInterest_tax(subAcct.getInst_tax_payable()); // 利息税
		cplOut.setTax_after_inst_amt(subAcct.getInst_payable().subtract(subAcct.getInst_tax_payable())); // 税后利息

		bizlog.debug("<<<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpWaitDrawInterestCheck.checkMain end <<<<<<<<<<<<<<<<");
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
	public static void checkMainMethod(DpWaitDrawInterestIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		// 货币代号和现转标志不能为空
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getCash_trxn_ind(), SysDict.A.cash_trxn_ind.getId(), SysDict.A.cash_trxn_ind.getLongName());

		// 验密标志默认为“是”
		if (cplIn.getCheck_password_ind() == null) {
			cplIn.setCheck_password_ind(E_YESORNO.YES);
		}

		// 检查输入信息
		checkInputInfo(cplIn, acctInfo, subAcct);

		// 加载数据区
		addDataToBuffer(cplIn, acctInfo, subAcct);

		// 检查交易密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 交易控制检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_WAIT_INTEREST_DRAW.getValue());

		// 待支取利息支取步用检查冻结等限制状态
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年11月20日-下午12:12:38</li>
	 *         <li>功能说明：添加数据到缓存区</li>
	 *         </p>
	 * @param cplIn
	 *            待支取利息输入
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 */
	private static void addDataToBuffer(DpWaitDrawInterestIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		// 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 加载账户数据区
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 加载币种数据区
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 加载客户数据区
		DpPublicCheck.addDataToCustBuffer(acctInfo.getCust_no(), acctInfo.getCust_type());

	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月10日-下午4:40:36</li>
	 *         <li>功能说明：检查输入信息</li>
	 *         </p>
	 * @param cplIn
	 *            待支取利息输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 */
	private static void checkInputInfo(DpWaitDrawInterestIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		bizlog.method(" DpWaitDrawInterestCheck.checkInputInfo begin >>>>>>>>>>>>>>>>");

		// 子户已经销户,需要抛出异常
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(cplIn.getAcct_no(), subAcct.getSub_acct_seq());
		}

		// 子户没有待支付利息,需要抛出异常
		if (CommUtil.compare(subAcct.getInst_payable(), BigDecimal.ZERO) <= 0) {

			throw DpErr.Dp.E0093(cplIn.getAcct_no(), subAcct.getSub_acct_seq());
		}

		// 账号名称与传入的账号名称不一致,需要抛出异常
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(cplIn.getAcct_name(), acctInfo.getAcct_name())) {

			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctInfo.getAcct_name());
		}

		// 利息不为空则检查与待支付利息匹配性
		if (CommUtil.isNotNull(cplIn.getInst_payable()) && !CommUtil.equals(subAcct.getInst_payable(), cplIn.getInst_payable())) {
			throw DpErr.Dp.E0112();
		}

		// 利息税不为空则检查与待支付利息税匹配性
		if (CommUtil.isNotNull(cplIn.getInst_tax_payable())) {

			DpIntTaxInfo taxInfo = DpTaxApi.calcWithholdingTax(subAcct.getAcct_no(), subAcct.getSub_acct_no(), subAcct.getInst_payable());

			if (!CommUtil.equals(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), taxInfo.getAccrual_inst_tax()), cplIn.getInst_tax_payable())) {
				throw DpErr.Dp.E0113();
			}
		}

		bizlog.method(" DpWaitDrawInterestCheck.checkInputInfo end <<<<<<<<<<<<<<<<");
	}

}
