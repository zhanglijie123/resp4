package cn.sunline.icore.dp.serv.account.save;

import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.electronic.DpElectronicAccountBinding;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.froze.DpFroze;
import cn.sunline.icore.dp.serv.query.DpAcctQuery;
import cn.sunline.icore.dp.serv.servicetype.SrvDpOpenAccount;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpQueryAcct.DpMainAcctBalInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ACCTBUSITYPE;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_REDBLUEWORDIND;
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
public class DpDemandSaveCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDemandSaveCheck.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午1:52:58</li>
	 *         <li>功能说明：活期存入检查服务</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpDemandSaveOut checkMain(DpDemandSaveIn cplIn) {

		bizlog.method(" DpDemandSaveCheck.demandSave begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

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
		checkMainMethodBefore(cplIn, account, acctAccessOut.getAuto_open_demand_ind());

		// 活期存入主调检查
		checkMainMethod(cplIn, account, acctAccessOut);

		// 输出
		DpDemandSaveOut cplOut = BizUtil.getInstance(DpDemandSaveOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplOut.setAcct_no(account.getAcct_no()); // 账号
		cplOut.setAcct_type(account.getAcct_type()); // 账户类型
		cplOut.setAcct_name(account.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(acctAccessOut.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(acctAccessOut.getCcy_code()); // 货币代码
		cplOut.setCust_no(account.getCust_no()); // 客户号
		cplOut.setSub_acct_branch(acctAccessOut.getAcct_branch()); // 子账户所属机构

		if (CommUtil.isNotNull(acctAccessOut.getAcct_branch())) {
			cplOut.setBranch_name(ApBranchApi.getItem(acctAccessOut.getAcct_branch()).getBranch_name()); // 机构名称
		}

		bizlog.debug("<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpDemandSaveCheck.demandSave end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午1:52:58</li>
	 *         <li>功能说明：活期存入检查主调方法：给Do服务调用</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param account
	 *            账户信息
	 * @param acctAccessOut
	 *            子户定位信息
	 * @return 活期存入输出接口
	 */
	private static void checkMainMethod(DpDemandSaveIn cplIn, DpaAccount account, DpAcctAccessOut acctAccessOut) {

		// 定位到子户
		if (CommUtil.isNotNull(acctAccessOut.getSub_acct_no())) {

			// 带锁查询子户信息
			DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(account.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

			// 刷新属性：不提交数据库
			DpAttrRefresh.refreshAttrValue(subAcct, account, cplIn.getAcct_no(), E_YESORNO.NO);

			checkMainMethodForSubAcct(cplIn, account, subAcct);
		}

		// 自动开子户存入检查
		else {
			checkMainMethodForAutoOpen(cplIn, account);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月14日-下午1:52:58</li>
	 *         <li>功能说明：活期存入检查主调方法前处理</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param account
	 *            账户信息
	 */
	public static void checkMainMethodBefore(DpDemandSaveIn cplIn, DpaAccount acctInfo, E_YESORNO autoOpenAcct) {

		// 默认赋值
		defaultValue(cplIn);

		// 验证输入字段合法性
		checkInputData(cplIn, acctInfo);

		// 自动开户时，产品为空
		if (autoOpenAcct == E_YESORNO.YES && CommUtil.isNull(cplIn.getProd_id())) {

			if (acctInfo.getSigl_prod_acct_ind() == E_YESORNO.YES) {

				DpaAccountRelate acctRelate = DpaAccountRelateDao.selectFirst_odb3(acctInfo.getAcct_no(), E_ACCTBUSITYPE.DEPOSIT, true);

				cplIn.setProd_id(acctRelate.getProd_id());
			}
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月14日-下午1:52:58</li>
	 *         <li>功能说明：活期存入自动开户检查主调方法</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param account
	 *            账户信息
	 */
	private static void checkMainMethodForAutoOpen(DpDemandSaveIn cplIn, DpaAccount acctInfo) {

		// 开子户检查, 除此之外还需再加存入检查
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

		// 调用开子户检查服务，开辟新的规则缓存区
		BizUtil.getInstance(SrvDpOpenAccount.class).addSubAccountCheck(cplAddSubIn);
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午1:52:58</li>
	 *         <li>功能说明：存在子户时活期存入检查主调方法</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param account
	 *            账户信息
	 * @param subAccount
	 *            子账户信息
	 */
	public static void checkMainMethodForSubAcct(DpDemandSaveIn cplIn, DpaAccount account, DpaSubAccount subAcct) {

		// 1.1 子账户状态检查
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
			throw DpBase.E0017(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
		}

		// 1.2 倒起息日期检查
		if (CommUtil.isNotNull(cplIn.getBack_value_date()) && CommUtil.compare(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) > 0) {
			throw DpBase.E0258(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 2. 数据集加载
		addDataToBuffer(cplIn, subAcct, account);

		// 3. 交易控制检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_SAVE.getValue());

		// 4. 账户限制检查
		if (cplIn.getAcct_hold_check_Ind() != E_YESORNO.NO) {
			DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_SAVE, null);
		}

		// 5. 存入控制检查
		DpBaseServiceApi.checkSaveCtrl(cplIn.getTrxn_amt(), subAcct);

		// 6. 开户凭证状态检查
		if (E_YESORNO.YES.getValue().equals(ApBusinessParmApi.getValue("DEPOSIT_CHECK_VOUCHER"))) {

			DpPublicCheck.checkOpenVochStatus(cplIn.getOpen_voch_check_ind(), subAcct, account);
		}

		// 7. 冻结检查
		if (CommUtil.isNotNull(cplIn.getFroze_kind_code())) {
			checkFroze(cplIn, subAcct);
		}
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月22日-下午4:58:22</li>
	 *         <li>功能说明：添加交易数据到缓存区</li>
	 *         </p>
	 * @param cplIn
	 *            交易输入接口
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 */
	private static void addDataToBuffer(DpDemandSaveIn cplIn, DpaSubAccount subAcct, DpaAccount account) {

		// 2.1加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		Map<String, Object> mapObj = new HashMap<String, Object>();

		// 借贷标志
		mapObj.put("debit_credit", subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT);

		// 判断对方账户为电子账户绑定结算户
		String oppAcctNo = CommUtil.nvl(cplIn.getReal_opp_acct_no(), cplIn.getOpp_acct_no());

		E_YESORNO bindingInd = DpElectronicAccountBinding.isBindingSettleAccount(account.getAcct_no(), E_SAVEORWITHDRAWALIND.SAVE, oppAcctNo, cplIn.getReal_opp_acct_name());

		mapObj.put("band_acct_ind", bindingInd.getValue());

		mapObj.put("same_cust_ind", E_YESORNO.NO);
		mapObj.put("same_acct_ind", E_YESORNO.NO);

		// 对手方账号不为空且为存款类账户
		if (cplIn.getOpp_acct_route() == E_ACCOUTANALY.DEPOSIT && CommUtil.isNotNull(cplIn.getOpp_acct_no())) {

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
			accessIn.setAcct_no(cplIn.getOpp_acct_no());
			accessIn.setAcct_type(cplIn.getOpp_acct_type());
			accessIn.setCcy_code(cplIn.getOpp_acct_ccy());
			accessIn.setSub_acct_seq(cplIn.getOpp_sub_acct_seq());

			// 查询对方子户产品
			DpAcctAccessOut locateSubAcct = DpToolsApi.subAcctInquery(accessIn);

			// 查询对方账户
			DpaAccount oppAcct = DpaAccountDao.selectOne_odb1(locateSubAcct.getAcct_no(), false);

			// 将对手方的主账号和服务的主账号做比较，若一致则将same_acct_ind 置为 Y-YES.
			if (CommUtil.equals(cplIn.getOpp_acct_no(), cplIn.getAcct_no())) {

				mapObj.put("same_acct_ind", E_YESORNO.YES);
			}

			DpaAccount dpaAccount = DpaAccountDao.selectOne_odb1(cplIn.getAcct_no(), false);

			// 将对手方的客户号和服务的主账号对应的客户号做比较，若一致则将same_cust_ind 置为 Y-YES
			if (CommUtil.equals(oppAcct.getCust_no(), dpaAccount.getCust_no())) {

				mapObj.put("same_cust_ind", E_YESORNO.YES);
			}
		}

		// 追加输入数据集
		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObj);

		// 2.2加载子账户数据区
		// DpaAideInfo aideInfo =
		// DpaAideInfoDao.selectOne_odb1(subAcct.getSub_acct_no(), false);
		Map<String, Object> subAcctMar = CommUtil.toMap(subAcct);
		// subAcctMar.put("text_large_remark_3",aideInfo == null ? null :
		// aideInfo.getText_large_remark_3());
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, subAcctMar);

		// 2.3加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(account));

		DpMainAcctBalInfo mainAcctBalance = DpAcctQuery.getMainAcctBalance(account.getAcct_no(), null, null);

		Map<String, Object> mapObject = new HashMap<String, Object>();
		// 账户余额
		mapObject.put("total_amt", mainAcctBalance.getTotal_amt());

		// 追加账户数据集
		ApBufferApi.appendData(ApConst.ACCOUNT_DATA_MART, mapObject);

		// 2.4货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 2.5加载客户数据集
		DpPublicCheck.addDataToCustBuffer(account.getCust_no(), account.getCust_type());
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-下午4:26:07</li>
	 *         <li>功能说明：活期存入服务调用冻结止付检查</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param subAcct
	 *            子账户信息
	 */
	private static void checkFroze(DpDemandSaveIn cplIn, DpaSubAccount subAcct) {

		bizlog.method(" DpDemandSaveCheck.checkFroze begin >>>>>>>>>>>>>>>>");

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

		// 同客户下冻结调方法效率更高
		DpFroze.checkMain(dpFrozeIn);

		bizlog.method(" DpDemandSaveCheck.checkFroze end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-下午3:41:06</li>
	 *         <li>功能说明：活期存入服务输入接口检查</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 */
	private static void checkInputData(DpDemandSaveIn cplIn, DpaAccount acctInfo) {

		bizlog.method(" DpDemandSaveCheck.checkInputData begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 现转标志必须输入
		BizUtil.fieldNotNull(cplIn.getCash_trxn_ind(), SysDict.A.cash_trxn_ind.getId(), SysDict.A.cash_trxn_ind.getLongName());

		// 交易币种必须输入
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 交易金额必须输入
		BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

		// 摘要代码必须输入
		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

		// 金额精度检检查，不合法抛出异常
		ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getTrxn_amt());

		// 检查摘要代码
		ApSummaryApi.exists(cplIn.getSummary_code());

		// 检查红蓝字交易金额合法性
		DpToolsApi.checkRedBlueIndTrxnAmt(cplIn.getRed_blue_word_ind(), cplIn.getTrxn_amt());

		// 客户账户名称一致性检查
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(cplIn.getAcct_name(), acctInfo.getAcct_name())) {
			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctInfo.getAcct_name());
		}

		bizlog.method(" DpDemandSaveCheck.checkInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-下午3:41:06</li>
	 *         <li>功能说明：输入为空默认值</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 */
	private static void defaultValue(DpDemandSaveIn cplIn) {

		// 账户限制检查默认为“是”
		if (cplIn.getAcct_hold_check_Ind() == null) {
			cplIn.setAcct_hold_check_Ind(E_YESORNO.YES);
		}

		// 红蓝字记账默认为“蓝字”
		if (cplIn.getRed_blue_word_ind() == null) {
			cplIn.setRed_blue_word_ind(E_REDBLUEWORDIND.BLUE);
		}

		// 开户凭证检查标志默认为“是”
		if (cplIn.getOpen_voch_check_ind() == null) {
			cplIn.setOpen_voch_check_ind(E_YESORNO.YES);
		}

		// 是否开户存入标志默认为“否”
		if (cplIn.getOpen_acct_save_ind() == null) {
			cplIn.setOpen_acct_save_ind(E_YESORNO.NO);
		}
	}
}
