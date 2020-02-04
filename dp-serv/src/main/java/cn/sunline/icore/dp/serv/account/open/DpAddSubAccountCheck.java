package cn.sunline.icore.dp.serv.account.open;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAttrLimitApi;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpRateBasicApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfDraw;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfInterest;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfSave;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpInrtSet;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtCodeDefine;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpParmeterMart;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_SPECPRODTYPE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.errors.DpErr.Dp;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.iobus.DpInterestRateIobus;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountOut;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpDrawAcctInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApBaseErr;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_AMTPERTWAY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_INRTDIRECTION;
import cn.sunline.icore.sys.type.EnumType.E_OWNERLEVEL;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpAddSubAccountCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAddSubAccountCheck.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月18日-下午1:16:29</li>
	 *         <li>功能说明：开子户主检查服务</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 * @return cplOut 开子户服务输出接口
	 */
	public static DpAddSubAccountOut checkMain(DpAddSubAccountIn cplIn) {

		bizlog.method(" DpAddSubAccountCheck.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 带锁定位账户信息: 里面包含账户状态检查
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 属性刷新：不提交数据库
		DpAttrRefresh.refreshAttrValue(acctInfo, cplIn.getCard_no(), E_YESORNO.NO);

		// 主调检查方法
		checkMainMethod(cplIn, acctInfo);

		// 输出
		DpAddSubAccountOut cplOut = BizUtil.getInstance(DpAddSubAccountOut.class);

		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_type(acctInfo.getAcct_type());
		cplOut.setSub_acct_seq(null); // 子账户序号
		cplOut.setAcct_name(CommUtil.nvl(cplIn.getSub_acct_name(), acctInfo.getAcct_name())); // 账户名称
		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setProd_id(cplIn.getProd_id()); // 产品编号
		cplOut.setCcy_code(cplIn.getCcy_code()); // 货币代码
		cplOut.setDept_type(null); // 存款种类
		cplOut.setBack_value_date(cplIn.getBack_value_date()); // 起息日期
		cplOut.setTerm_code(cplIn.getTerm_code()); // 存期
		cplOut.setDue_date(cplIn.getDue_date()); // 到期日
		cplOut.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
		cplOut.setRenew_save_term(cplIn.getRenew_save_term()); // 续存存期
		cplOut.setEfft_inrt(null); // 账户执行利率
		cplOut.setList_layer_inrt(null); // 分层利率列表

		bizlog.debug("<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpAddSubAccountCheck.checkMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月18日-下午1:16:29</li>
	 *         <li>功能说明：开子户主调检查方法</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 * @param acctInfo
	 *            账户信息
	 */
	public static void checkMainMethod(DpAddSubAccountIn cplIn, DpaAccount acctInfo) {

		// 输入接口默认值处理
		defaultValue(cplIn, acctInfo);

		// 检查输入要素, 不检查产品适配性
		checkAddSubElement(cplIn, acctInfo);

		// 加载缓存区
		addBuffer(cplIn, acctInfo);

		// 开子户属性设置合法性检查
		String attrValue = DpAttrLimitApi.checkAttributeSet(cplIn.getList_attribute(), E_OWNERLEVEL.SUB_ACCTOUNT);

		// 将属性值加载到输入数据区
		Map<String, Object> mapObj = new HashMap<String, Object>();

		mapObj.put("attr_value", attrValue);

		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObj);

		// 子账户开户许可检查
		DpBaseServiceApi.checkOpenSubAcctLicense(cplIn, acctInfo);

		// 交易控制检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_OPEN_SUBACCT.getValue());

		// 利率信息检查: 包括利率编码、合同利率、利率计划、浮动计划等
		checkInstRate(cplIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月18日-下午1:16:29</li>
	 *         <li>功能说明：检查开子户输入要素的合法性: 不含跟产品紧密相关的检查,
	 *         开账户同开子账时做检查时账号还未生成，因此不传入账户信息</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 */
	private static void checkAddSubElement(DpAddSubAccountIn cplIn, DpaAccount acctInfo) {

		bizlog.method(" DpAddSubAccountCheck.checkAddSubElement begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		RunEnvs runEnvsInfo = BizUtil.getTrxRunEnvs();

		// 产品编号不能为空
		BizUtil.fieldNotNull(cplIn.getProd_id(), SysDict.A.prod_id.getId(), SysDict.A.prod_id.getLongName());

		// 货币代码不能为空
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 个人客户子户名需与账户名一致
		/*
		 * if (CommUtil.isNotNull(cplIn.getSub_acct_name()) &&
		 * acctTypeInfo.getCust_type() == E_CUSTOMERTYPE.PERSONAL &&
		 * !CommUtil.equals(cplIn.getSub_acct_name(), acctInfo.getAcct_name()))
		 * { throw Dp.E0084(cplIn.getSub_acct_name(), acctInfo.getAcct_name());
		 * }
		 */

		// 币种非法
		if (!ApCurrencyApi.exists(cplIn.getCcy_code())) {
			throw ApBaseErr.ApBase.E0128(cplIn.getCcy_code());
		}

		// 不支持晚起息
		if (CommUtil.compare(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) > 0) {
			throw DpBase.E0258(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 付息周期不为空，则首次付息日也不能为空
		if (CommUtil.isNotNull(cplIn.getPay_inst_cyc()) && CommUtil.isNull(cplIn.getFirst_pay_inst_date())) {
			BizUtil.fieldNotNull(cplIn.getFirst_pay_inst_date(), DpBaseDict.A.first_pay_inst_date.getId(), DpBaseDict.A.first_pay_inst_date.getLongName());
		}

		// 账户有效日期不为空则不能小于当前交易日期
		if (CommUtil.isNotNull(cplIn.getAcct_valid_date()) && CommUtil.compare(cplIn.getAcct_valid_date(), runEnvsInfo.getTrxn_date()) < 0) {
			throw Dp.E0083(cplIn.getAcct_valid_date(), runEnvsInfo.getTrxn_date());
		}
		
		// 账户有效日期不为空则要符合日期格式
		if (CommUtil.isNotNull(cplIn.getAcct_valid_date()) && !BizUtil.isDateString(cplIn.getAcct_valid_date())){
			throw ApPubErr.APPUB.E0011(cplIn.getAcct_valid_date());
		}

		// 机构检查
		if (CommUtil.isNotNull(cplIn.getSub_acct_branch())) {

			ApBranchApi.exists(cplIn.getSub_acct_branch(), true);
		}

		// 产品基础属性
		DpfBase prodBase = DpProductFactoryApi.getProdBaseInfo(cplIn.getProd_id());

		if (prodBase.getDd_td_ind() == E_DEMANDORTIME.TIME) {

			DpfSave prodSave = DpProductFactoryApi.getProdSaveCtrl(cplIn.getProd_id(), cplIn.getCcy_code());

			// 存入周期不为空， 说明有存入计划
			if (CommUtil.isNotNull(prodSave.getScheduled_dept_cycle())) {
				checkDepositPlan(cplIn);
			}

			DpfDraw prodDraw = DpProductFactoryApi.getProdDrawCtrl(cplIn.getProd_id(), cplIn.getCcy_code());

			// 支取周期不为空， 说明有支取计划, 本金转入和利息转入账号不能为空
			if (CommUtil.isNotNull(prodDraw.getScheduled_withdrawal_cycle())) {

				BizUtil.fieldNotNull(cplIn.getPrin_trsf_acct(), DpBaseDict.A.prin_trsf_acct.getId(), DpBaseDict.A.prin_trsf_acct.getLongName());

				BizUtil.fieldNotNull(cplIn.getIncome_inst_acct(), DpBaseDict.A.income_inst_acct.getId(), DpBaseDict.A.income_inst_acct.getLongName());
			}
		}

		// 定期产品可以也存入多次，智能定期存款不限存入间隔，目标存款有存入间隔，因此目标存款有存入计划
		if (prodBase.getSpec_dept_type() == E_SPECPRODTYPE.TARGET_DEPOSIT) {

			// 目标存款必须录入目标金额
			BizUtil.fieldNotNull(cplIn.getTarget_amt(), DpBaseDict.A.target_amt.getId(), DpBaseDict.A.target_amt.getLongName());
		}

		// 检查收息账户的合法性
		if (CommUtil.isNotNull(cplIn.getIncome_inst_acct())) {
			DpPublicCheck.checkIncomeAcct(cplIn.getIncome_inst_acct(), cplIn.getIncome_inst_ccy(), E_SAVEORWITHDRAWALIND.SAVE);
		}

		// 检查本息账户的合法性
		if (CommUtil.isNotNull(cplIn.getPrin_trsf_acct())) {

			boolean sameAcct = CommUtil.equals(cplIn.getPrin_trsf_acct(), cplIn.getIncome_inst_acct());
			boolean sameCCy = CommUtil.equals(cplIn.getPrin_trsf_acct_ccy(), cplIn.getIncome_inst_ccy());

			// 若收息账户和本金转入账户是一样的， 则没有必要重复检查
			if (!sameAcct || !sameCCy) {
				DpPublicCheck.checkIncomeAcct(cplIn.getPrin_trsf_acct(), cplIn.getPrin_trsf_acct_ccy(), E_SAVEORWITHDRAWALIND.SAVE);
			}
		}

		bizlog.method(" DpAddSubAccountCheck.checkAddSubElement end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月18日-下午1:16:29</li>
	 *         <li>功能说明：存入计划检查</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 */
	private static void checkDepositPlan(DpAddSubAccountIn cplIn) {

		// 按计划存入需输入扣款账户
		if (CommUtil.isNull(cplIn.getList_draw_account()) || cplIn.getList_draw_account().size() == 0) {
			// TODO: throw 计划转出账户不能为空
		}

		for (DpDrawAcctInfo cplDrawAcct : cplIn.getList_draw_account()) {

			BizUtil.fieldNotNull(cplDrawAcct.getDraw_acct(), DpDict.A.draw_acct.getId(), DpDict.A.draw_acct.getLongName());
			BizUtil.fieldNotNull(cplDrawAcct.getDraw_acct_ccy(), DpDict.A.draw_acct_ccy.getId(), DpDict.A.draw_acct_ccy.getLongName());

			// 多笔转出方时下列要素需要传
			if (cplIn.getList_draw_account().size() > 1) {

				BizUtil.fieldNotNull(cplDrawAcct.getSerial_no(), SysDict.A.serial_no.getId(), SysDict.A.serial_no.getLongName());
				BizUtil.fieldNotNull(cplDrawAcct.getAmt_apportion_method(), DpDict.A.amt_apportion_method.getId(), DpDict.A.amt_apportion_method.getLongName());

				// 如果是不控制，则依次单条处理转出方，有多少转多少，只要汇总起来余额足够
				if (cplDrawAcct.getAmt_apportion_method() != E_AMTPERTWAY.NO) {

					BizUtil.fieldNotNull(cplDrawAcct.getAmount_ratio(), DpDict.A.amount_ratio.getId(), DpDict.A.amount_ratio.getLongName());

					// 检查比例是否在0-100之间
					if (cplDrawAcct.getAmt_apportion_method() == E_AMTPERTWAY.PERCENT) {

						if (CommUtil.compare(cplDrawAcct.getAmount_ratio(), BigDecimal.ZERO) <= 0 || CommUtil.compare(cplDrawAcct.getAmount_ratio(), new BigDecimal(100)) > 0) {
							throw DpErr.Dp.E0488(cplDrawAcct.getAmount_ratio());
						}
					}
					else if (cplDrawAcct.getAmt_apportion_method() == E_AMTPERTWAY.AMOUNT) {

						if (CommUtil.compare(cplDrawAcct.getAmount_ratio(), BigDecimal.ZERO) <= 0) {
							throw DpErr.Dp.E0488(cplDrawAcct.getAmount_ratio());
						}

						ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplDrawAcct.getAmount_ratio());
					}
				}
			}

			// 获取账户路由
			E_ACCOUTANALY routeType = DpInsideAccountIobus.getAccountRouteType(cplDrawAcct.getDraw_acct());

			// 存入计划扣款账号必须是存款账号，收息账户检查方法里再判断是不是活期
			if (routeType != E_ACCOUTANALY.DEPOSIT) {
				// TODO: throw
			}

			DpPublicCheck.checkIncomeAcct(cplDrawAcct.getDraw_acct(), cplDrawAcct.getDraw_acct_ccy(), E_SAVEORWITHDRAWALIND.WITHDRAWAL);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月18日-下午1:16:29</li>
	 *         <li>功能说明：检查开子户输入要素的合法性: 不含跟产品紧密相关的检查,
	 *         开账户同开子账时做检查时账号还未生成，因此不传入账户信息</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 */
	private static void defaultValue(DpAddSubAccountIn cplIn, DpaAccount acctInfo) {

		// 交易金额为空默认为零
		cplIn.setTrxn_amt(CommUtil.nvl(cplIn.getTrxn_amt(), BigDecimal.ZERO));

		// 默认不早起息开户
		cplIn.setBack_value_date(CommUtil.nvl(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date()));

		// 账户类型
		cplIn.setAcct_type(acctInfo.getAcct_type());

		// 有设置收息账户，没有设置收息币种，则默认收息币种为开户币种
		if (CommUtil.isNotNull(cplIn.getIncome_inst_acct()) && CommUtil.isNull(cplIn.getIncome_inst_ccy())) {
			cplIn.setIncome_inst_ccy(cplIn.getCcy_code());
		}

		// 有设置本金转入账户，没有设置本金转入币种，则默认本金转入币种为开户币种
		if (CommUtil.isNotNull(cplIn.getPrin_trsf_acct()) && CommUtil.isNull(cplIn.getPrin_trsf_acct_ccy())) {
			cplIn.setPrin_trsf_acct_ccy(cplIn.getCcy_code());
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月18日-下午1:16:29</li>
	 *         <li>功能说明：数据区加载</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 */
	private static void addBuffer(DpAddSubAccountIn cplIn, DpaAccount acctInfo) {

		// 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		// 加载账户数据区
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 币种数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(cplIn.getCcy_code())));

		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(acctInfo.getCust_no(), acctInfo.getCust_type());

		// 参数数据集
		DpParmeterMart cplParmMart = BizUtil.getInstance(DpParmeterMart.class);

		cplParmMart.setAcct_type(acctInfo.getAcct_type());
		cplParmMart.setCcy_code(cplIn.getCcy_code());
		cplParmMart.setProd_id(cplIn.getProd_id());

		// 加载参数数据集
		DpToolsApi.addDataToParmBuffer(cplParmMart);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月18日-下午1:16:29</li>
	 *         <li>功能说明：开户利率信息检查: 包块利率编号、利率计划、浮动计划等</li>
	 *         </p>
	 * @param cplIn
	 *            开子户服务输入接口
	 */
	private static void checkInstRate(DpAddSubAccountIn cplIn) {

		/* 不计息也可以设置利率信息、利率计划等， 因为后面某个时机可以改为计息 */

		// 利率设置检查
		if (!cplIn.getList_inrt().isEmpty()) {

			DpInrtCodeDefine cplInrtDef = BizUtil.getInstance(DpInrtCodeDefine.class);

			for (DpInrtSet cplInrt : cplIn.getList_inrt()) {

				// 利息索引类型必须有值
				if (CommUtil.isNull(cplInrt.getInrt_key_type())) {
					throw APPUB.E0001(DpBaseDict.A.inrt_key_type.getId(), DpBaseDict.A.inrt_key_type.getLongName());
				}

				// 产品计息定义信息
				DpfInterest instInfo = DpProductFactoryApi.getProdInterestDefine(cplIn.getProd_id(), cplIn.getCcy_code(), cplInrt.getInrt_key_type(), true);

				String inrtCode = CommUtil.nvl(cplInrt.getInrt_code(), instInfo.getInrt_code());

				// 验证续存存期是否合法
				if (CommUtil.isNotNull(cplIn.getRenew_save_term()) && cplInrt.getInrt_key_type() == E_INSTKEYTYPE.NORMAL) {

					DpInterestRateIobus.checkTermRateMatch(inrtCode, cplIn.getCcy_code(), cplIn.getRenew_save_term(), instInfo.getInst_rate_file_way());
				}

				if (CommUtil.isNotNull(inrtCode)) {

					cplInrtDef = DpRateBasicApi.getInrtCodeDefine(inrtCode);

					// 如果利率代码为合同利率则合同利率字段不能为空，否则合同利率字段不能有值
					if (cplInrtDef.getInrt_code_direction() == E_INRTDIRECTION.SIMPLE_CONT && CommUtil.isNull(cplInrt.getContract_inrt())) {
						throw APPUB.E0001(DpBaseDict.A.contract_inrt.getId(), DpBaseDict.A.contract_inrt.getLongName());
					}

					if (cplInrtDef.getInrt_code_direction() != E_INRTDIRECTION.SIMPLE_CONT && CommUtil.isNotNull(cplInrt.getContract_inrt())) {
						throw Dp.E0080(inrtCode);
					}

					// 如果利率代码为分层合同利率，则分层利率信息不能为空，否则必须为空
					if (cplInrtDef.getInrt_code_direction() == E_INRTDIRECTION.LAYER_CONT && CommUtil.isNull(cplInrt.getList_contract_inrt())) {
						throw Dp.E0081(inrtCode);
					}

					if (cplInrtDef.getInrt_code_direction() != E_INRTDIRECTION.LAYER_CONT
							&& (CommUtil.isNotNull(cplInrt.getList_contract_inrt()) && cplInrt.getList_contract_inrt().size() > 0)) {
						throw Dp.E0082(inrtCode);
					}

					// 使用手输分层利率，则检查手输分层利率组成的合法性
					if (CommUtil.isNotNull(cplInrt.getList_contract_inrt())) {
						DpRateBasicApi.chkCustLevelInrt(cplInrt.getList_contract_inrt());
					}
				}
			}
		}

		// 利率计划数据检查
		if (CommUtil.isNotNull(cplIn.getList_inrt_plan()) && cplIn.getList_inrt_plan().size() > 0) {
			DpRateBasicApi.checkInrtPlan(cplIn.getList_inrt_plan());
		}

		// 浮动计划数据检查
		if (CommUtil.isNotNull(cplIn.getList_float_plan()) && cplIn.getList_float_plan().size() > 0) {
			DpRateBasicApi.checkFloatPlan(cplIn.getList_float_plan());
		}
	}
}
