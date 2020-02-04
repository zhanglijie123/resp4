package cn.sunline.icore.dp.serv.account.open;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountType;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRate;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRateDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfDraw;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfSave;
import cn.sunline.icore.dp.base.type.ComDpProductParmBase.DpOpenProdBaseCheck;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_OPENREFVOCHFLAG;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RENEWSAVEWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.servicetype.SrvDpInstruct;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositSignIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpOpenAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpOpenAcctAndAddSubAcctIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpOpenAcctAndAddSubAcctOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_SELFOPTNUMBERIND;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_DATASTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：负债开户同时开子户
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年1月6日-下午5:07:42</li>
 *         <li>开户同时开子户服务相关代码</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpOpenAcctAndSubAcct {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOpenAcctAndSubAcct.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午19:42:46</li>
	 *         <li>功能说明：负债开户同步开子户</li>
	 *         </p>
	 * @param cplIn
	 *            开户输入接口
	 * @return IoDpOpenAccountOut 开户输出接口
	 */
	public static DpOpenAcctAndAddSubAcctOut doMain(DpOpenAcctAndAddSubAcctIn cplIn) {

		bizlog.method(" DpOpenAcctAndSubAcct.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 检查输入接口必输性
		checkInputElement(cplIn);

		// 查询账户类型定义
		DppAccountType acctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getAcct_type());

		// 刷新卡属性、客户属性
		DpAttrRefresh.refreshAttrValue(cplIn.getCard_no(), cplIn.getCust_no(), acctType.getCust_type(), E_YESORNO.YES);

		// 开户接口
		DpOpenAccountIn cplOpenAcct = switchOpenAcctIntf(cplIn);

		// 检查开户合法性
		DpOpenAccountCheck.checkMainMethod(cplOpenAcct);

		// 检查同步开子户合法性
		sameOpenSubAcctCheck(cplIn, acctType);

		// 获取智能存款产品号
		String smartProd = getAutoSmartDepositProduct(cplIn, acctType);

		// 开账户处理
		DpaAccount acctInfo = DpOpenAccount.doMainMethod(cplOpenAcct, acctType);

		// 加载账户数据区
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 开子户接口
		DpAddSubAccountIn cplOpenSubAcct = switchOpenSubAcctIntf(cplIn);

		// 开子户处理
		cplOpenSubAcct.setAcct_no(acctInfo.getAcct_no());

		DpaSubAccount subAcct = DpAddSubAccount.doMainMethod(cplOpenSubAcct, acctInfo);

		// 签订智能存款协议
		if (CommUtil.isNotNull(smartProd)) {
			smartDepositSign(cplIn, acctInfo, smartProd);
		}

		// 输出
		DpOpenAcctAndAddSubAcctOut cplOut = BizUtil.getInstance(DpOpenAcctAndAddSubAcctOut.class);

		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setAcct_type(acctInfo.getAcct_type()); // 账户类型
		cplOut.setAcct_branch(subAcct.getSub_acct_branch()); // 账务机构
		cplOut.setAcct_date(acctInfo.getAcct_date()); // 账户归档日期
		cplOut.setOpen_acct_branch(acctInfo.getOpen_acct_branch()); // 开户机构
		cplOut.setBranch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name()); // 机构名称
		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
		cplOut.setProd_id(subAcct.getProd_id()); // 产品编号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setDept_type(null); // 存款种类
		cplOut.setBack_value_date(subAcct.getStart_inst_date()); // 倒起息日
		cplOut.setTerm_code(subAcct.getTerm_code()); // 存期
		cplOut.setDue_date(subAcct.getDue_date()); // 到期日

		if (subAcct.getInst_ind() == E_YESORNO.YES) {

			DpaInterestRate interestRate = DpaInterestRateDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, 1l, true);

			cplOut.setEfft_inrt(interestRate.getEfft_inrt()); // 账户执行利率
		}

		bizlog.debug("<<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpOpenAcctAndSubAcct.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午19:42:46</li>
	 *         <li>功能说明：负债开户同步开子户检查服务</li>
	 *         </p>
	 * @param cplIn
	 *            开户输入接口
	 */
	public static void checkMain(DpOpenAcctAndAddSubAcctIn cplIn) {

		// 检查输入接口必输性
		checkInputElement(cplIn);

		// 查询账户类型定义
		DppAccountType acctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getAcct_type());

		// 刷新卡属性、客户属性
		DpAttrRefresh.refreshAttrValue(cplIn.getCard_no(), cplIn.getCust_no(), acctType.getCust_type(), E_YESORNO.NO);

		// 开户接口
		DpOpenAccountIn cplOpenAcct = switchOpenAcctIntf(cplIn);

		// 检查开户合法性
		DpOpenAccountCheck.checkMainMethod(cplOpenAcct);

		// 检查同步开子户合法性
		sameOpenSubAcctCheck(cplIn, acctType);

		// 获取智能存款产品号(含少量检查)
		getAutoSmartDepositProduct(cplIn, acctType);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午1:16:29</li>
	 *         <li>功能说明：检查输入要素
	 *         </p>
	 * @param cplIn
	 *            开账户及开子户服务输入接口
	 */
	private static void checkInputElement(DpOpenAcctAndAddSubAcctIn cplIn) {

		// 客户号不能为空
		BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

		// 账户类型不能为空
		BizUtil.fieldNotNull(cplIn.getAcct_type(), SysDict.A.acct_type.getId(), SysDict.A.acct_type.getLongName());

		// 产品代码不能为空
		BizUtil.fieldNotNull(cplIn.getProd_id(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

		// 币种不能为空
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午1:16:29</li>
	 *         <li>功能说明：同步开子账户检查
	 *         </p>
	 * @param cplIn
	 *            开账户及开子户服务输入接口
	 * @param acctType
	 *            账户类型定义
	 */
	private static void sameOpenSubAcctCheck(DpOpenAcctAndAddSubAcctIn cplIn, DppAccountType acctType) {

		// 检查产品是否适合在此账户类型下开立
		DpAccountTypeParmApi.checkProdAdaptAcctType(cplIn.getProd_id(), cplIn.getAcct_type());

		// 开子户关联凭证检查
		if (acctType.getOpen_acct_voch_ind() != E_OPENREFVOCHFLAG.NONE && acctType.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT) {

			if (acctType.getOpen_acct_voch_ind() == E_OPENREFVOCHFLAG.FORCE) {

				// 凭证类型不能为空
				BizUtil.fieldNotNull(cplIn.getVoch_type(), SysDict.A.voch_type.getId(), SysDict.A.voch_type.getLongName());

				// 凭证号码不能为空
				BizUtil.fieldNotNull(cplIn.getVoch_no(), SysDict.A.voch_no.getId(), SysDict.A.voch_no.getLongName());
			}

			// 凭证类型和凭证号码不为空时要同时录入
			if ((CommUtil.isNotNull(cplIn.getVoch_type()) && CommUtil.isNull(cplIn.getVoch_no()))
					|| (CommUtil.isNull(cplIn.getVoch_type()) && CommUtil.isNotNull(cplIn.getVoch_no()))) {
				throw DpBase.E0159();
			}

			// 检查凭证类型使用的合法性
			if (CommUtil.isNotNull(cplIn.getVoch_type())) {

				DpAccountTypeParmApi.checkAcctVochType(cplIn.getAcct_type(), cplIn.getProd_id(), cplIn.getVoch_type());
			}
		}

		// 产品基础属性
		DpfBase prodBase = DpProductFactoryApi.getProdBaseInfo(cplIn.getProd_id());

		// 产品状态未生效,或日期不在产品有效时间范围内,则报错
		if (prodBase.getProd_status() != E_DATASTATUS.EFFECTIVE
				|| !BizUtil.dateBetween(BizUtil.getTrxRunEnvs().getTrxn_date(), prodBase.getEffect_date(), true, prodBase.getExpiry_date(), false)) {
			throw DpBase.E0400(cplIn.getProd_id());
		}

		// 开定期产品必须先检查账户下是否有活期子户存在的必要
		if (prodBase.getDd_td_ind() != E_DEMANDORTIME.DEMAND) {

			// 账户类型下有挂靠活期产品，且账户下目前没有活跃活期子户， 不能直接开定期，必须先开活期
			boolean existDemandFlag = DpAccountTypeParmApi.existsProductType(cplIn.getAcct_type(), E_DEMANDORTIME.DEMAND);

			if (existDemandFlag) {
				throw DpBase.E0051();
			}
		}

		// 产品基础属性检查
		DpOpenProdBaseCheck prodBaseCheck = BizUtil.getInstance(DpOpenProdBaseCheck.class);

		prodBaseCheck.setDue_date(cplIn.getDue_date());
		prodBaseCheck.setProd_id(cplIn.getProd_id());
		prodBaseCheck.setTerm_code(cplIn.getTerm_code());
		prodBaseCheck.setCcy_code(cplIn.getCcy_code());
		prodBaseCheck.setOpen_acct_amt(cplIn.getTrxn_amt());
		prodBaseCheck.setRenew_prod_id(cplIn.getRenew_prod_id());
		prodBaseCheck.setRenew_save_amt(cplIn.getPrin_adjust_amt());
		prodBaseCheck.setRenew_save_term(cplIn.getRenew_save_term());
		prodBaseCheck.setRenewal_method(cplIn.getRenewal_method());
		prodBaseCheck.setRenewl_pay_inst_cyc(cplIn.getRenewl_pay_inst_cyc());

		// 产品开户检查
		DpProductFactoryApi.checkProdOpenAcct(prodBaseCheck);

		// 续产品和账户类型适配性
		if (CommUtil.in(cplIn.getRenewal_method(), E_RENEWSAVEWAY.ADD_AMOUNT, E_RENEWSAVEWAY.PART_AMOUNT, E_RENEWSAVEWAY.PRIN_INST, E_RENEWSAVEWAY.PRINCIPAL)) {

			DpAccountTypeParmApi.checkProdAdaptAcctType(CommUtil.nvl(cplIn.getRenew_prod_id(), cplIn.getProd_id()), cplIn.getAcct_type());
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午1:16:29</li>
	 *         <li>功能说明：接口转换为开户接口
	 *         </p>
	 * @param cplIn
	 *            开账户及开子户服务输入接口
	 * @return 开户服务接口
	 */
	private static DpOpenAccountIn switchOpenAcctIntf(DpOpenAcctAndAddSubAcctIn cplIn) {

		// 开户接口
		DpOpenAccountIn cplOpenAcct = BizUtil.getInstance(DpOpenAccountIn.class);

		cplOpenAcct.setAcct_branch(cplIn.getAcct_branch());
		cplOpenAcct.setAcct_date(cplIn.getAcct_date());
		cplOpenAcct.setAcct_manager_id(cplIn.getAcct_manager_id());
		cplOpenAcct.setAcct_manager_name(cplIn.getAcct_manager_name());
		cplOpenAcct.setAcct_name(cplIn.getAcct_name());
		cplOpenAcct.setAcct_oth_name(null);
		cplOpenAcct.setAcct_type(cplIn.getAcct_type());
		cplOpenAcct.setAddress_type(null);
		cplOpenAcct.setCard_no(cplIn.getCard_no());
		cplOpenAcct.setCust_no(cplIn.getCust_no());
		cplOpenAcct.setHold_mail(null);
		cplOpenAcct.setJoint_acct_ind(cplIn.getJoint_acct_ind());
		cplOpenAcct.setList_attribute(null); // 账户属性设置接口字段需要另外定义
		cplOpenAcct.setList_joint_cust(cplIn.getList_joint_cust());
		cplOpenAcct.setNostro_acct_no(cplIn.getNostro_acct_no());
		cplOpenAcct.setRemark(cplIn.getRemark());
		cplOpenAcct.setSelf_opt_number(null);
		cplOpenAcct.setSelf_opt_number_ind(E_SELFOPTNUMBERIND.NOT_OPTIONAL);
		cplOpenAcct.setTrxn_password(cplIn.getTrxn_password());
		cplOpenAcct.setVoch_no(cplIn.getVoch_no());
		cplOpenAcct.setVoch_type(cplIn.getVoch_type());
		cplOpenAcct.setWithdrawal_cond(cplIn.getWithdrawal_cond());

		return cplOpenAcct;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午1:16:29</li>
	 *         <li>功能说明：接口转换为开子户接口
	 *         </p>
	 * @param cplIn
	 *            开账户及开子户服务输入接口
	 * @return 开子户服务接口
	 */
	private static DpAddSubAccountIn switchOpenSubAcctIntf(DpOpenAcctAndAddSubAcctIn cplIn) {

		// 开子户接口
		DpAddSubAccountIn cplOpenSubAcct = BizUtil.getInstance(DpAddSubAccountIn.class);

		cplOpenSubAcct.setAcct_manager_id(cplIn.getAcct_manager_id());
		cplOpenSubAcct.setAcct_manager_name(cplIn.getAcct_manager_name());
		cplOpenSubAcct.setAcct_no(null);
		cplOpenSubAcct.setAcct_type(cplIn.getAcct_type());
		cplOpenSubAcct.setAcct_valid_date(cplIn.getAcct_valid_date());
		cplOpenSubAcct.setBack_value_date(cplIn.getBack_value_date());
		cplOpenSubAcct.setCard_no(cplIn.getCard_no());
		cplOpenSubAcct.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		cplOpenSubAcct.setCcy_code(cplIn.getCcy_code());
		cplOpenSubAcct.setChannel_remark(cplIn.getChannel_remark());
		cplOpenSubAcct.setDue_date(cplIn.getDue_date());
		cplOpenSubAcct.setFirst_pay_inst_date(cplIn.getFirst_pay_inst_date());
		cplOpenSubAcct.setIncome_inst_acct(cplIn.getIncome_inst_acct());
		cplOpenSubAcct.setIncome_inst_ccy(cplIn.getIncome_inst_ccy());
		cplOpenSubAcct.setInst_base(cplIn.getInst_base());
		cplOpenSubAcct.setInst_ind(cplIn.getInst_ind());
		cplOpenSubAcct.setList_attribute(cplIn.getList_attribute());
		cplOpenSubAcct.setList_draw_account(null);
		cplOpenSubAcct.setList_float_plan(cplIn.getList_float_plan());
		cplOpenSubAcct.setList_inrt(cplIn.getList_inrt());
		cplOpenSubAcct.setList_inrt_plan(cplIn.getList_inrt_plan());
		cplOpenSubAcct.setPay_inst_cyc(cplIn.getPay_inst_cyc());
		cplOpenSubAcct.setPrin_adjust_amt(cplIn.getPrin_adjust_amt());
		cplOpenSubAcct.setPrin_trsf_acct(cplIn.getPrin_trsf_acct());
		cplOpenSubAcct.setPrin_trsf_acct_ccy(cplIn.getPrin_trsf_acct_ccy());
		cplOpenSubAcct.setProd_id(cplIn.getProd_id());
		cplOpenSubAcct.setRemark(cplIn.getRemark());
		cplOpenSubAcct.setRenew_prod_id(cplIn.getRenew_prod_id());
		cplOpenSubAcct.setRenew_save_term(cplIn.getRenew_save_term());
		cplOpenSubAcct.setRenewal_method(cplIn.getRenewal_method());
		cplOpenSubAcct.setRenewl_pay_inst_cyc(cplIn.getRenewl_pay_inst_cyc());
		cplOpenSubAcct.setScheduled_dept_cycle(cplIn.getScheduled_dept_cycle());
		cplOpenSubAcct.setScheduled_withdrawal_cycle(cplIn.getScheduled_withdrawal_cycle());
		cplOpenSubAcct.setSub_acct_branch(cplIn.getSub_acct_branch());
		cplOpenSubAcct.setSub_acct_name(cplIn.getSub_acct_name());
		cplOpenSubAcct.setTarget_amt(cplIn.getTarget_amt());
		cplOpenSubAcct.setTerm_code(cplIn.getTerm_code());
		cplOpenSubAcct.setTrxn_amt(cplIn.getTrxn_amt());
		cplOpenSubAcct.setTrxn_password(cplIn.getTrxn_password());
		cplOpenSubAcct.setVoch_no(cplIn.getVoch_no());
		cplOpenSubAcct.setVoch_type(cplIn.getVoch_type());
		cplOpenSubAcct.setWithdrawal_cond(cplIn.getWithdrawal_cond());
		cplOpenSubAcct.setIncome_source(cplIn.getIncome_source());
		cplOpenSubAcct.setFund_source(cplIn.getFund_source());
		cplOpenSubAcct.setFund_use_way(cplIn.getFund_use_way());

		return cplOpenSubAcct;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午1:16:29</li>
	 *         <li>功能说明：获取智能定期存款产品号
	 *         </p>
	 * @param cplIn
	 *            开账户及开子户服务输入接口
	 * @param acctType
	 *            账户类型定义
	 * @return 智能定期产品号
	 */
	private static String getAutoSmartDepositProduct(DpOpenAcctAndAddSubAcctIn cplIn, DppAccountType acctType) {

		DpfBase dpfBase = DpProductFactoryApi.getProdBaseInfo(cplIn.getProd_id());

		if (dpfBase.getDd_td_ind() == E_DEMANDORTIME.DEMAND && dpfBase.getAuto_open_smart_ind() == E_YESORNO.YES) {

			String smartProd = DpProductFactoryApi.getSmartTdProdCode(dpfBase.getCust_type(), false);

			if (CommUtil.isNotNull(smartProd)) {

				// 检查产品和账户类型适配性
				DpAccountTypeParmApi.checkProdAdaptAcctType(smartProd, cplIn.getAcct_type());

				// 单子户标志限制、单产品标志限制
				if (acctType.getSigl_sub_acct_ind() == E_YESORNO.YES || acctType.getSigl_prod_acct_ind() == E_YESORNO.YES) {
					return null;
				}
			}

			return smartProd;
		}

		return null;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午1:16:29</li>
	 *         <li>功能说明：智能存款自动开户签约
	 *         </p>
	 * @param cplIn
	 *            开账户及开子户服务输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param smartProd
	 *            智能存款产品
	 */
	private static void smartDepositSign(DpOpenAcctAndAddSubAcctIn cplIn, DpaAccount acctInfo, String smartProd) {

		bizlog.method(" DpOpenAcctAndSubAcct.smartDepositSign begin >>>>>>>>>>>>>>>>");

		// 活期支取参数
		DpfDraw dpfDraw = DpProductFactoryApi.getProdDrawCtrl(cplIn.getProd_id(), cplIn.getCcy_code());

		// 智能存款存入参数
		DpfSave smartSaveInfo = DpProductFactoryApi.getProdSaveCtrl(smartProd, cplIn.getCcy_code());

		// 智能存款签约接口
		DpSmartDepositSignIn smartSignIn = BizUtil.getInstance(DpSmartDepositSignIn.class);

		smartSignIn.setAcct_no(acctInfo.getAcct_no()); // account no
		smartSignIn.setAcct_type(acctInfo.getAcct_type()); // account type
		smartSignIn.setAcct_name(acctInfo.getAcct_name()); // account name
		smartSignIn.setCcy_code(cplIn.getCcy_code()); // currency code
		smartSignIn.setCheck_password_ind(E_YESORNO.NO);
		smartSignIn.setSmart_acct_no(acctInfo.getAcct_no());
		smartSignIn.setSmart_prod_id(smartProd);
		smartSignIn.setMin_turn_out_amt(CommUtil.nvl(cplIn.getMin_turn_out_amt(), smartSaveInfo.getSigl_min_dept_amt()));
		smartSignIn.setMax_turn_out_amt(CommUtil.nvl(cplIn.getMax_turn_out_amt(), smartSaveInfo.getSigl_max_dept_amt()));
		smartSignIn.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), dpfDraw.getMin_remain_bal()));
		smartSignIn.setMultiple_amt(CommUtil.nvl(cplIn.getMultiple_amt(), smartSaveInfo.getDept_step_size()));
		smartSignIn.setAuto_placement_ind(CommUtil.nvl(cplIn.getAuto_placement_ind(), E_YESORNO.YES));
		smartSignIn.setAuto_break_ind(CommUtil.nvl(cplIn.getAuto_break_ind(), E_YESORNO.YES));
		smartSignIn.setBreak_authority_ind(CommUtil.nvl(cplIn.getBreak_authority_ind(), E_YESORNO.NO));

		BizUtil.getInstance(SrvDpInstruct.class).smartDepositSign(smartSignIn);

		bizlog.method(" DpOpenAcctAndSubAcct.smartDepositSign end <<<<<<<<<<");
	}
}