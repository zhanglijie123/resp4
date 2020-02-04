package cn.sunline.icore.dp.serv.account.save;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApLimitApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAdjustIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTADJUSTTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.account.open.DpAddSubAccount;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.electronic.DpElectronicAccountBinding;
import cn.sunline.icore.dp.serv.froze.DpFroze;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.iobus.DpChargeIobus;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalOut;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeOut;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAgentInfoRegister;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ADDSUBTRACT;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：活期存入
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年1月10日-下午1:52:27</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月10日-HongBiao：活期存入相关功能</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpDemandSave {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDemandSave.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午1:52:58</li>
	 *         <li>功能说明：活期存入服务</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @return
	 */
	public static DpDemandSaveOut doMain(DpDemandSaveIn cplIn) {

		bizlog.method(" DpDemandSave.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>>cplIn=[%s]", cplIn);

		// 先带锁定位账户表：因为活期存入可能涉及到自动开子户
		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
		acctAccessIn.setProd_id(cplIn.getProd_id());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		acctAccessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.SAVE);

		// 定位存款账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 前检查
		DpDemandSaveCheck.checkMainMethodBefore(cplIn, account, acctAccessOut.getAuto_open_demand_ind());

		DpaSubAccount subAcct = BizUtil.getInstance(DpaSubAccount.class);

		// 子账号不存在, 先自动开子账户
		if (CommUtil.isNotNull(acctAccessOut.getSub_acct_no())) {
			subAcct = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);
		}
		else {
			subAcct = AutoOpenSubAcct(cplIn, account);
		}

		// 刷新属性：提交数据库
		DpAttrRefresh.refreshAttrValue(subAcct, account, cplIn.getAcct_no(), E_YESORNO.YES);

		// 主调检查方法
		DpDemandSaveCheck.checkMainMethodForSubAcct(cplIn, account, subAcct);

		// 主调处理方法
		DpDemandSaveOut cplOut = doMainMethod(cplIn, account, subAcct);

		// 场景收费计算
		DpChargeIobus.calcAutoChrg(subAcct, E_DEPTTRXNEVENT.DP_SAVE, cplIn.getTrxn_amt());

		bizlog.debug("<<<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpDemandSave.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月14日-下午6:46:21</li>
	 *         <li>功能说明：活期存入主调方法</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param account
	 *            账户信息
	 * @param subAcct
	 *            子账户信息输入
	 * @return 活期存入服务输出接口
	 */
	private static DpDemandSaveOut doMainMethod(DpDemandSaveIn cplIn, DpaAccount account, DpaSubAccount subAcct) {

		// 为支持7*24小时，需要联机处理到期解冻
		boolean existsUnfroze = DpUnFroze.matureAutoUnfrozen(account.getCust_no());

		// 存在解冻标志，里面可能更新冻结标志状态
		if (existsUnfroze) {
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);
		}

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		String oppAcctNo = CommUtil.nvl(cplIn.getReal_opp_acct_no(), cplIn.getOpp_acct_no());

		// 电子账户同名户来账激活
		DpElectronicAccountBinding.sameNameActiveBinding(account.getAcct_no(), oppAcctNo, cplIn.getReal_opp_acct_name());

		// 记账处理
		DpUpdAccBalOut accBalOut = dealAccounting(cplIn, subAcct);

		// 倒起息
		if (CommUtil.isNotNull(cplIn.getBack_value_date()) && CommUtil.compare(cplIn.getBack_value_date(), trxnDate) < 0) {

			DpInstAdjustIn cplAdjustIn = BizUtil.getInstance(DpInstAdjustIn.class);

			cplAdjustIn.setEnd_inst_date(BizUtil.dateAdd("day", trxnDate, -1));
			cplAdjustIn.setInit_inst_start_date(subAcct.getStart_inst_date());
			cplAdjustIn.setInst_adjust_aspect(E_ADDSUBTRACT.ADD);
			cplAdjustIn.setInst_adjust_type(E_INSTADJUSTTYPE.BACKVALUE);
			cplAdjustIn.setStart_inst_date(cplIn.getBack_value_date());
			cplAdjustIn.setTrxn_amt(cplIn.getTrxn_amt());

			// 倒起息
			DpInterestBasicApi.adjustInstForBackvalue(subAcct, cplAdjustIn, E_YESORNO.YES);
		}

		// 冻结止付处理
		String frozeNo = "";
		if (CommUtil.isNotNull(cplIn.getFroze_kind_code())) {
			frozeNo = froze(cplIn, subAcct);
		}

		// 限额累计
		ApLimitApi.process(E_DEPTTRXNEVENT.DP_SAVE.toString(), subAcct.getCcy_code(), cplIn.getTrxn_amt());

		// 代理人信息登记
		if (CommUtil.isNotNull(cplIn.getAgent_doc_no()) && CommUtil.isNotNull(cplIn.getAgent_doc_type())) {

			DpAgentInfoRegister agentInfoIn = BizUtil.getInstance(DpAgentInfoRegister.class);

			agentInfoIn.setAgent_doc_no(cplIn.getAgent_doc_no());
			agentInfoIn.setAgent_doc_type(cplIn.getAgent_doc_type());
			agentInfoIn.setAgent_country(cplIn.getAgent_country());
			agentInfoIn.setAgent_name(cplIn.getAgent_name());
			agentInfoIn.setRemark(ApSummaryApi.getText(cplIn.getSummary_code()));

			DpCustomerIobus.registerAgentInfo(agentInfoIn);
		}

		// 返回接口赋值
		DpDemandSaveOut cplOut = BizUtil.getInstance(DpDemandSaveOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(account.getAcct_no());
		cplOut.setAcct_type(account.getAcct_type());
		cplOut.setAcct_name(account.getAcct_name());
		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplOut.setCcy_code(subAcct.getCcy_code());
		cplOut.setCust_no(subAcct.getCust_no());
		cplOut.setProd_id(subAcct.getProd_id());
		cplOut.setSub_acct_branch(subAcct.getSub_acct_branch());
		cplOut.setBranch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name());
		cplOut.setAcct_bal(accBalOut.getAcct_bal()); // 交易后余额
		cplOut.setTrxn_amt(cplIn.getTrxn_amt()); // 实际存入金额
		cplOut.setFroze_no(frozeNo); // 冻结编号
		cplOut.setSub_acct_no(subAcct.getSub_acct_no());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月24日-下午4:26:07</li>
	 *         <li>功能说明：自动开子户方法</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param acctInfo
	 *            账户信息
	 * @return 子账户信息
	 */
	private static DpaSubAccount AutoOpenSubAcct(DpDemandSaveIn cplIn, DpaAccount acctInfo) {

		DpAddSubAccountIn cplAddSubIn = BizUtil.getInstance(DpAddSubAccountIn.class);

		cplAddSubIn.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplAddSubIn.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplAddSubIn.setAcct_type(acctInfo.getAcct_type()); // 账户类型
		cplAddSubIn.setProd_id(cplIn.getProd_id()); // 产品编号
		cplAddSubIn.setSub_acct_name(acctInfo.getAcct_name()); // 子账户名称
		cplAddSubIn.setWithdrawal_cond(null); // 支取条件
		cplAddSubIn.setTrxn_password(null); // 交易密码
		cplAddSubIn.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
		cplAddSubIn.setVoch_type(null); // 凭证类型
		cplAddSubIn.setVoch_no(null); // 凭证号码
		cplAddSubIn.setCcy_code(cplIn.getCcy_code()); // 货币代码
		cplAddSubIn.setTerm_code(null); // 存期
		cplAddSubIn.setDue_date(null); // 到期日
		cplAddSubIn.setAcct_valid_date(null); // 账户有效期
		cplAddSubIn.setAccounting_alias(null); // 核算别名:里面自己定位
		cplAddSubIn.setBack_value_date(cplIn.getBack_value_date()); // 起息日期
		cplAddSubIn.setTrxn_amt(cplIn.getTrxn_amt()); // 交易金额
		cplAddSubIn.setIncome_inst_acct(null); // 收息账号
		cplAddSubIn.setSub_acct_branch(null); // 子账户所属机构
		cplAddSubIn.setChannel_remark(null); // 渠道备注
		cplAddSubIn.setRemark(cplIn.getCustomer_remark()); // 备注
		cplAddSubIn.setPay_inst_cyc(null); // 付息周期
		cplAddSubIn.setFirst_pay_inst_date(null); // 首次派息日
		cplAddSubIn.setRenewal_method(null); // 续存方式
		cplAddSubIn.setRenew_save_term(null); // 续存存期
		cplAddSubIn.setInst_ind(null); // 计息标志
		cplAddSubIn.setInst_base(null); // 计息基础
		cplAddSubIn.setList_inrt(null); // 账户利率设置列表
		cplAddSubIn.setList_inrt_plan(null); // 利率计划列表
		cplAddSubIn.setList_float_plan(null); // 浮动计划列表

		// 调用开子户服务
		DpAddSubAccountOut cplAddOut = DpAddSubAccount.doMain(cplAddSubIn);

		// 查询子账户信息返回
		return DpaSubAccountDao.selectOne_odb1(acctInfo.getAcct_no(), cplAddOut.getSub_acct_no(), true);
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月19日-下午6:46:21</li>
	 *         <li>功能说明：活期存入记账处理</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param subAcct
	 *            子账户信息输入
	 * @param event
	 *            交易事件
	 * @return DpUpdAccBalOut 账户余额更新输出接口
	 */
	private static DpUpdAccBalOut dealAccounting(DpDemandSaveIn cplIn, DpaSubAccount subAcct) {

		bizlog.method(" DpDemandSave.dealAccounting begin >>>>>>>>>>>>>>>>");

		DpUpdAccBalIn cplInput = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplInput.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAcct.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplInput.setAcct_no(subAcct.getAcct_no()); // 账号
		cplInput.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		cplInput.setBack_value_date(CommUtil.nvl(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date())); // 起息日期
		cplInput.setTrxn_amt(cplIn.getTrxn_amt()); // 交易金额
		cplInput.setTrxn_ccy(cplIn.getCcy_code()); // 交易币种
		cplInput.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT); // 记账方向
		cplInput.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
		cplInput.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplInput.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplInput.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
		cplInput.setSummary_code(cplIn.getSummary_code()); // 摘要代码
		cplInput.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
		cplInput.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
		cplInput.setTrxn_event_id(E_DEPTTRXNEVENT.DP_SAVE.getValue());

		// 交易对手
		cplInput.setOpp_acct_route(cplIn.getOpp_acct_route()); // 对方账户路由
		cplInput.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
		cplInput.setOpp_acct_ccy(cplIn.getOpp_acct_ccy()); // 对方币种
		cplInput.setOpp_branch_id(cplIn.getOpp_branch_id()); // 对方机构号
		cplInput.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		cplInput.setOpp_trxn_amt(cplIn.getTrxn_opp_amt());
		cplInput.setOpp_acct_type(cplIn.getOpp_acct_type());

		// 现金交易时，对方账号填当前交易柜员
		if (cplIn.getOpp_acct_route() == E_ACCOUTANALY.CASH) {
			cplInput.setOpp_acct_no(BizUtil.getTrxRunEnvs().getTrxn_teller()); // 对方账号
		}
		else if (cplIn.getOpp_acct_route() == E_ACCOUTANALY.SUSPENSE) {
			cplInput.setOpp_acct_no(cplIn.getDebit_suspense_no());
		}

		// 代理人信息
		cplInput.setAgent_doc_type(cplIn.getAgent_doc_type()); // 代理人证件类型
		cplInput.setAgent_doc_no(cplIn.getAgent_doc_no()); // 代理人证件号
		cplInput.setAgent_name(cplIn.getAgent_name()); // 代理人姓名
		cplInput.setAgent_country(cplIn.getAgent_country()); // 代理人国籍

		// 对账单对手方信息
		cplInput.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
		cplInput.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
		cplInput.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
		cplInput.setReal_opp_country(cplIn.getReal_opp_country());
		cplInput.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
		cplInput.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
		cplInput.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
		cplInput.setReal_opp_remark(cplIn.getReal_opp_remark());

		// 国外刷卡消费信息登记
		cplInput.setTrxn_area(cplIn.getTrxn_area());
		cplInput.setTrxn_area_amt(cplIn.getTrxn_area_amt());
		cplInput.setTrxn_area_ccy(cplIn.getTrxn_area_ccy());
		cplInput.setTrxn_area_exch_rate(cplIn.getTrxn_area_exch_rate());
		cplInput.setConsume_date(cplIn.getConsume_date());
		cplInput.setConsume_time(cplIn.getConsume_time());

		// 利息税和税率
		cplInput.setInst_tax_rate(cplIn.getInst_tax_rate());
		cplInput.setInst_withholding_tax(cplIn.getInst_withholding_tax());

		// 原业务流水: 消费部分退款时需要登记
		cplInput.setOriginal_busi_seq(cplIn.getOriginal_busi_seq());
		cplInput.setOriginal_trxn_seq(cplIn.getOriginal_trxn_seq());

		DpUpdAccBalOut accBalOut = DpAccounting.online(cplInput);

		bizlog.method(" DpDemandSave.dealAccounting end <<<<<<<<<<<<<<<<");
		return accBalOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-下午4:26:07</li>
	 *         <li>功能说明：调用冻结功能.</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param subAcct
	 *            子账户信息
	 * @return 冻结编号
	 */
	private static String froze(DpDemandSaveIn cplIn, DpaSubAccount subAcct) {

		bizlog.method(" DpDemandSave.froze begin >>>>>>>>>>>>>>>>");

		DpFrozeIn dpFrozeIn = BizUtil.getInstance(DpFrozeIn.class);

		dpFrozeIn.setFroze_kind_code(cplIn.getFroze_kind_code());
		dpFrozeIn.setFroze_object_type(cplIn.getFroze_object_type());
		dpFrozeIn.setAcct_no(subAcct.getAcct_no());
		dpFrozeIn.setAcct_type(cplIn.getAcct_type());
		dpFrozeIn.setAcct_name(cplIn.getAcct_name());
		dpFrozeIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		dpFrozeIn.setCcy_code(subAcct.getCcy_code());
		dpFrozeIn.setProd_id(subAcct.getProd_id());
		dpFrozeIn.setFroze_amt(cplIn.getFroze_amt());
		dpFrozeIn.setFroze_reason(cplIn.getFroze_reason());
		dpFrozeIn.setFroze_due_date(cplIn.getFroze_due_date());
		dpFrozeIn.setFroze_before_save_amt(cplIn.getTrxn_amt());
		dpFrozeIn.setFroze_feature_code(cplIn.getFroze_feature_code());

		// 同客户下调用方法效率更高
		DpFrozeOut cplFrozeOut = DpFroze.doMain(dpFrozeIn);

		bizlog.method(" DpDemandSave.froze end <<<<<<<<<<<<<<<<");

		return cplFrozeOut.getFroze_no();
	}
}
