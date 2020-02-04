package cn.sunline.icore.dp.serv.froze;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ACCOUNTBUSINESSSOURCE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.servicetype.SrvDpTimeAccounting;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbForceDeduct;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbForceDeductDao;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpLawDeductAcctIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpLawDeductAcctOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpMultiForceDeductIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpMultiForceDeductOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEDUCTWAY;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_EXCHRATETYPE;
import cn.sunline.icore.sys.type.EnumType.E_FOREXEXCHOBJECT;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 多对象法院扣划支取
 * </p>
 * 
 * @Author linshiq
 *         <p>
 *         <li>2017年1月18日-下午1:40:54</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月18日-linshiq：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpMultiForceDeduct {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpMultiForceDeduct.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月17日-下午1:56:03</li>
	 *         <li>功能说明：法院扣划支取主程序入口</li>
	 *         </p>
	 * @param cplIn
	 *            法院扣划支取输入接口
	 * @return 法院扣划支取输出接口
	 */
	public static DpMultiForceDeductOut doMain(DpMultiForceDeductIn cplIn) {

		bizlog.method(" DpForceDeduct.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(" cplIn=[%s]", cplIn);

		// 先调用公用检查方法
		checkInputData(cplIn);

		// 初始化输出接口
		DpMultiForceDeductOut cplOut = BizUtil.getInstance(DpMultiForceDeductOut.class);

		BigDecimal factSaveAmt = BigDecimal.ZERO;// 初始化实际存入金额

		// 记录中记录数
		long totalCount = 0;

		for (DpLawDeductAcctIn cplAcctIn : cplIn.getList_lawdeduct_acct()) {

			// 账户扣划
			DpLawDeductAcctOut cplLawDeductOut = acctDeduct(cplAcctIn, cplIn);

			// 实际存入金额计算
			if (CommUtil.equals(cplLawDeductOut.getCcy_code(), cplIn.getOpp_acct_ccy())) {
				factSaveAmt = factSaveAmt.add(cplLawDeductOut.getAct_withdrawal_amt());
			}
			else {

				factSaveAmt = autoExchange(cplLawDeductOut, cplIn).add(factSaveAmt);
			}

			// 扣划转出登记
			registerDeduct(cplIn, cplAcctIn, cplLawDeductOut.getSub_acct_no());

			// 法院扣划输出信息列表
			cplOut.getList_lawdeduct_acct().add(cplLawDeductOut);

			totalCount++;
		}

		cplOut.setForce_deduct_method(cplIn.getForce_deduct_method()); // 扣划方式
		cplOut.setAct_dept_amt(factSaveAmt); // 实际存入金额

		BizUtil.getTrxRunEnvs().setTotal_count(totalCount);// 返回总记录数

		bizlog.debug(" cplOut=[%s]", cplOut);
		bizlog.method(" DpForceDeduct.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月7日-下午7:33:09</li>
	 *         <li>功能说明：账户支取</li>
	 *         </p>
	 * @param cplAcctIn
	 *            法院扣划账户输入
	 * @param cplIn
	 *            扣划传入接口
	 * @return 账户扣划支取输出接口
	 */
	private static DpLawDeductAcctOut acctDeduct(DpLawDeductAcctIn cplAcctIn, DpMultiForceDeductIn cplIn) {

		bizlog.method(" DpForceDeduct.acctDraw begin >>>>>>>>>>>>>>>>");

		if (CommUtil.equals(cplAcctIn.getAcct_no(), cplIn.getOpp_acct_no())) {

			throw DpErr.Dp.E0329(cplAcctIn.getAcct_no(), cplIn.getOpp_acct_no());
		}

		// 定位相关子账号信息
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplAcctIn.getAcct_no());// 账号
		accessIn.setAcct_type(cplAcctIn.getAcct_type());// 账号类型
		accessIn.setCcy_code(cplAcctIn.getCcy_code());// 货币代码
		accessIn.setSub_acct_seq(cplAcctIn.getSub_acct_seq());// 子账户序号
		accessIn.setProd_id(cplAcctIn.getProd_id());// 产品编号

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 查询账户信息
		DpaAccount AcctInfo = DpaAccountDao.selectOne_odb1(accessOut.getAcct_no(), true);

		if (AcctInfo.getAcct_busi_source() != E_ACCOUNTBUSINESSSOURCE.DEPOSIT) {

			throw DpErr.Dp.E0243();
		}

		// 司法扣划解冻原因
		String unfrozeReason = ApBusinessParmApi.getValue(DpConst.UNFROZE_REASON, "LAW_DEDUCT");

		// 初始化子输出接口
		DpLawDeductAcctOut cplAcctOut = BizUtil.getInstance(DpLawDeductAcctOut.class);

		/* 账户支取操作 */
		if (accessOut.getDd_td_ind() == E_DEMANDORTIME.DEMAND) {// 活期子户

			// 活期支取
			DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			// 活期子户支取输入赋值
			demandDrawIn.setAcct_no(cplAcctIn.getAcct_no()); // 账号
			demandDrawIn.setAcct_type(cplAcctIn.getAcct_type()); // 账户类型
			demandDrawIn.setAcct_name(cplAcctIn.getAcct_name()); // 账户名称
			demandDrawIn.setProd_id(accessOut.getProd_id()); // 产品编号
			demandDrawIn.setCcy_code(cplAcctIn.getCcy_code()); // 交易币种
			demandDrawIn.setSub_acct_seq(cplAcctIn.getSub_acct_seq());
			demandDrawIn.setBack_value_date(cplAcctIn.getBack_value_date()); // 起息日期
			demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
			demandDrawIn.setRed_blue_word_ind(null); // 红蓝字记账标志
			demandDrawIn.setForce_draw_ind(cplAcctIn.getForce_draw_ind()); // 是否强制借记
			demandDrawIn.setCheck_password_ind(E_YESORNO.NO); // 验密标志
			demandDrawIn.setTrxn_password(null); // 交易密码
			demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.DEDUCT); // 支取业务种类
			demandDrawIn.setTrxn_amt(cplAcctIn.getTrxn_amt()); // 交易金额
			demandDrawIn.setFroze_no(cplIn.getFroze_no()); // 冻结编号
			demandDrawIn.setUnfroze_amt(cplAcctIn.getUnfroze_amt()); // 解冻金额
			demandDrawIn.setUnfroze_reason(unfrozeReason); // 解冻原因
			demandDrawIn.setSummary_code(cplIn.getSummary_code()); // 摘要代码
			demandDrawIn.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
			demandDrawIn.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
			demandDrawIn.setOpen_voch_check_ind(null); // 开户凭证检查标志
			demandDrawIn.setAcct_hold_check_Ind(E_YESORNO.NO); // 账户限制检查标志
			demandDrawIn.setReal_opp_acct_no(cplIn.getReal_opp_acct_no());
			demandDrawIn.setReal_opp_acct_name(cplIn.getReal_opp_acct_name());
			demandDrawIn.setReal_opp_acct_alias(cplIn.getReal_opp_acct_alias());
			demandDrawIn.setReal_opp_country(cplIn.getReal_opp_country());
			demandDrawIn.setReal_opp_bank_id(cplIn.getReal_opp_bank_id());
			demandDrawIn.setReal_opp_bank_name(cplIn.getReal_opp_bank_name());
			demandDrawIn.setReal_opp_branch_name(cplIn.getReal_opp_branch_name());
			demandDrawIn.setReal_opp_remark(cplIn.getReal_opp_remark());
			demandDrawIn.setOpp_acct_route(cplIn.getOpp_acct_route());
			demandDrawIn.setOpp_acct_no(cplIn.getOpp_acct_no());
			demandDrawIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
			demandDrawIn.setOpp_acct_type(cplIn.getOpp_acct_type());
			demandDrawIn.setOpp_branch_id(cplIn.getOpp_branch_id());

			// 活期支取
			DpDemandDrawOut cplDrawOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);

			// 相关数据输出
			cplAcctOut.setAcct_no(cplDrawOut.getAcct_no()); // 账号
			cplAcctOut.setAcct_type(cplDrawOut.getAcct_type()); // 账户类型
			cplAcctOut.setAcct_name(cplDrawOut.getAcct_name()); // 账户名称
			cplAcctOut.setSub_acct_seq(cplDrawOut.getSub_acct_seq()); // 子账户序号
			cplAcctOut.setCcy_code(cplDrawOut.getCcy_code()); // 货币代码
			cplAcctOut.setAct_withdrawal_amt(cplDrawOut.getAct_withdrawal_amt()); // 交易金额
			cplAcctOut.setProd_id(accessOut.getProd_id()); // 产品编号
			cplAcctOut.setUnfroze_amt(cplAcctIn.getUnfroze_amt()); // 解冻金额
			cplAcctOut.setForce_draw_ind(cplAcctIn.getForce_draw_ind() == null ? E_YESORNO.NO : cplAcctIn.getForce_draw_ind()); // 是否强制借记
			cplAcctOut.setFroze_status(cplDrawOut.getFroze_status());// 冻结状态
			cplAcctOut.setUnfroze_reason(unfrozeReason); // 解冻原因
			cplAcctOut.setSub_acct_no(accessOut.getSub_acct_no());
		}
		else {// 定期子户

			DpTimeDrawIn timeDrawIn = BizUtil.getInstance(DpTimeDrawIn.class);

			timeDrawIn.setCard_no(null); // 卡号
			timeDrawIn.setAcct_no(cplAcctIn.getAcct_no()); // 账号
			timeDrawIn.setAcct_type(cplAcctIn.getAcct_type()); // 账户类型
			timeDrawIn.setAcct_name(cplAcctIn.getAcct_name()); // 账户名称
			timeDrawIn.setSub_acct_seq(cplAcctIn.getSub_acct_seq()); // 子账户序号
			timeDrawIn.setCcy_code(cplAcctIn.getCcy_code()); // 货币代码
			timeDrawIn.setBack_value_date(cplAcctIn.getBack_value_date()); // 起息日期
			timeDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
			timeDrawIn.setCheck_password_ind(null); // 验密标志
			timeDrawIn.setTrxn_password(null); // 交易密码
			timeDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.DEDUCT); // 支取业务种类
			timeDrawIn.setTrxn_amt(cplAcctIn.getTrxn_amt()); // 交易金额
			timeDrawIn.setFroze_no(cplIn.getFroze_no()); // 冻结编号
			timeDrawIn.setUnfroze_amt(cplAcctIn.getUnfroze_amt()); // 解冻金额
			timeDrawIn.setUnfroze_reason(unfrozeReason); // 解冻原因
			timeDrawIn.setSummary_code(cplIn.getSummary_code()); // 摘要代码
			timeDrawIn.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
			timeDrawIn.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
			timeDrawIn.setOpen_voch_check_ind(null); // 开户凭证检查标志
			timeDrawIn.setAcct_hold_check_Ind(E_YESORNO.NO); // 账户限制检查标志
			timeDrawIn.setForce_draw_ind(cplAcctIn.getForce_draw_ind()); // 是否强制借记
			timeDrawIn.setVoch_type(null); // 凭证类型
			timeDrawIn.setVoch_no(null); // 凭证号码
			timeDrawIn.setOpp_acct_route(cplIn.getOpp_acct_route()); // 对方账户路由
			timeDrawIn.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
			timeDrawIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
			timeDrawIn.setOpp_acct_type(cplIn.getOpp_acct_type());
			timeDrawIn.setOpp_branch_id(cplIn.getOpp_branch_id());

			// 定期支取
			DpTimeDrawOut cplDrawOut = BizUtil.getInstance(SrvDpTimeAccounting.class).timeDraw(timeDrawIn);

			cplAcctOut.setAcct_no(cplDrawOut.getAcct_no()); // 账号
			cplAcctOut.setAcct_type(cplDrawOut.getAcct_type()); // 账户类型
			cplAcctOut.setAcct_name(cplDrawOut.getAcct_name()); // 账户名称
			cplAcctOut.setSub_acct_seq(cplDrawOut.getSub_acct_seq()); // 子账户序号
			cplAcctOut.setCcy_code(cplDrawOut.getCcy_code()); // 货币代码
			cplAcctOut.setAct_withdrawal_amt(cplDrawOut.getPaying_amt()); // 交易金额
			cplAcctOut.setProd_id(accessOut.getProd_id()); // 产品编号
			cplAcctOut.setUnfroze_amt(cplAcctIn.getUnfroze_amt()); // 解冻金额
			cplAcctOut.setForce_draw_ind(cplAcctIn.getForce_draw_ind() == null ? E_YESORNO.NO : cplAcctIn.getForce_draw_ind()); // 是否强制借记
			cplAcctOut.setFroze_status(cplDrawOut.getFroze_status());// 冻结状态
			cplAcctOut.setUnfroze_reason(unfrozeReason); // 解冻原因
			cplAcctOut.setSub_acct_no(accessOut.getSub_acct_no());
		}

		bizlog.method(" DpForceDeduct.acctDraw end <<<<<<<<<<<<<<<<");
		return cplAcctOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月7日-下午7:33:09</li>
	 *         <li>功能说明：自动结售汇</li>
	 *         </p>
	 * @param cplAcctIn
	 *            法院扣划账户输出
	 * @param cplIn
	 *            扣划传入接口
	 * @return 账户扣划支取输出接口
	 */
	private static BigDecimal autoExchange(DpLawDeductAcctOut cplAcctIn, DpMultiForceDeductIn cplIn) {

		DpExchangeAccountingIn cplExchIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

		cplExchIn.setBuy_acct_no(cplAcctIn.getAcct_no());
		cplExchIn.setBuy_amt(cplAcctIn.getAct_withdrawal_amt());
		cplExchIn.setBuy_cash_ind(E_CASHTRXN.TRXN);
		cplExchIn.setBuy_ccy_code(cplAcctIn.getCcy_code());
		cplExchIn.setBuy_sub_acct_seq(cplAcctIn.getSub_acct_seq());
		cplExchIn.setCountry_code("");
		cplExchIn.setCust_type(null);
		cplExchIn.setExch_rate_path(cplAcctIn.getCcy_code().concat("/").concat(cplIn.getOpp_acct_ccy()));
		cplExchIn.setExch_rate(cplAcctIn.getExch_rate());
		cplExchIn.setExch_rate_type(cplIn.getOpp_acct_route() == E_ACCOUTANALY.CASH ? E_EXCHRATETYPE.CASH : E_EXCHRATETYPE.EXCHANGE);
		cplExchIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.CUSTOMER);
		cplExchIn.setSell_acct_no(cplIn.getOpp_acct_no());
		cplExchIn.setSell_amt(null);
		cplExchIn.setSell_cash_ind(cplIn.getOpp_acct_route() == E_ACCOUTANALY.CASH ? E_CASHTRXN.CASH : E_CASHTRXN.TRXN);
		cplExchIn.setSell_ccy_code(cplIn.getOpp_acct_ccy());
		cplExchIn.setSell_sub_acct_seq(null);
		cplExchIn.setSummary_code(cplIn.getSummary_code());
		cplExchIn.setTrxn_remark(cplIn.getTrxn_remark());

		DpExchangeAccountingOut cplExchOut = DpExchangeIobus.exchangeAccounting(cplExchIn);

		return cplExchOut.getSell_amt();
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月17日-下午3:57:03</li>
	 *         <li>功能说明：扣划登记</li>
	 *         </p>
	 * @param cplIn
	 *            法院扣划支取输入接口
	 * @param cplAcctIn
	 *            法院扣划账户输入
	 * @param accessOut
	 *            账户访问输出接口
	 */
	private static void registerDeduct(DpMultiForceDeductIn cplIn, DpLawDeductAcctIn cplAcctIn, String subAcctNo) {

		bizlog.method(" DpForceDeduct.registerDeduct begin >>>>>>>>>>>>>>>>");

		// 生成扣划编号
		String forceDeductId = ApSeqApi.genSeq("FORCE_DEDUCT_NO");

		// 初始化强制扣划登记簿
		DpbForceDeduct forceDeductInfo = BizUtil.getInstance(DpbForceDeduct.class);

		// 获取公共运行区变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		forceDeductInfo.setForce_deduct_id(forceDeductId);// 扣划编号
		forceDeductInfo.setTrxn_date(runEnvs.getTrxn_date());// 交易日期
		forceDeductInfo.setSub_acct_no(subAcctNo);// 子账号
		forceDeductInfo.setForce_deduct_method(cplIn.getForce_deduct_method());// 扣划方式
		forceDeductInfo.setFroze_no(cplIn.getFroze_no());// 冻结编号
		forceDeductInfo.setAcct_no(cplAcctIn.getAcct_no());// 账号
		forceDeductInfo.setTrxn_amt(cplAcctIn.getTrxn_amt());// 交易金额
		forceDeductInfo.setOpp_acct_no(cplIn.getOpp_acct_no());// 对方账号
		forceDeductInfo.setOpp_sub_acct_seq(null);// 对方子账号序号
		forceDeductInfo.setFailure_reason(null);// 失败原因
		forceDeductInfo.setRemark(cplIn.getCustomer_remark());// 备注
		forceDeductInfo.setForce_deduct_reason(cplAcctIn.getForce_deduct_reason());// 扣划原因
		forceDeductInfo.setLegal_notice_no(cplIn.getLegal_notice_no());// 法律文书编号
		forceDeductInfo.setEnforced_legal_dept(cplIn.getEnforced_legal_dept());// 执法部门
		forceDeductInfo.setEnforced_legal_dept_name(cplIn.getEnforced_legal_dept_name());// 执法部门名称
		forceDeductInfo.setOfficer_doc_type(cplIn.getOfficer_doc_type());// 执法人员证件种类
		forceDeductInfo.setOfficer_doc_no(cplIn.getOfficer_doc_no());// 执法人员证件号码
		forceDeductInfo.setOfficer_name(cplIn.getOfficer_name());// 执法人员姓名
		forceDeductInfo.setOfficer_phone(cplIn.getOfficer_phone());// 执法人员联系方式
		forceDeductInfo.setOfficer2_doc_type(cplIn.getOfficer2_doc_type());// 执法人员二种类
		forceDeductInfo.setOfficer2_doc_no(cplIn.getOfficer2_doc_no());// 执法人员二证件号码
		forceDeductInfo.setOfficer2_name(cplIn.getOfficer2_name());// 执法人员二姓名
		forceDeductInfo.setOfficer2_phone(cplIn.getOfficer2_phone());// 执法人员二联系方式
		forceDeductInfo.setSource_channel(runEnvs.getChannel_id()); // 源渠道,暂时由交易渠道代替
		forceDeductInfo.setTrxn_channel(runEnvs.getChannel_id());// 交易渠道
		forceDeductInfo.setTrxn_branch(runEnvs.getTrxn_branch());// 交易机构
		forceDeductInfo.setTrxn_teller(runEnvs.getTrxn_teller());// 交易柜员
		forceDeductInfo.setThird_party_date(runEnvs.getInitiator_date());// 第三方日期
		forceDeductInfo.setBusi_seq(runEnvs.getBusi_seq());// 业务流水
		forceDeductInfo.setTrxn_seq(runEnvs.getTrxn_seq());// 交易流水
		forceDeductInfo.setTrxn_code(runEnvs.getTrxn_code());// 交易码
		forceDeductInfo.setSummary_code(cplIn.getSummary_code());
		forceDeductInfo.setSummary_name(ApSummaryApi.getText(cplIn.getSummary_code()));

		// 登记强制扣划登记簿
		DpbForceDeductDao.insert(forceDeductInfo);

		bizlog.method(" DpForceDeduct.registerDeduct end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月17日-下午1:52:16</li>
	 *         <li>功能说明：法院扣划支取服务输入检查入口</li>
	 *         </p>
	 * @param cplIn
	 *            法院扣划支取输入接口
	 */
	public static void checkMain(DpMultiForceDeductIn cplIn) {

		bizlog.method("DpForceDeductCheck.checkMain begin <<<<<<<<<<<<<<<<<<<<");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 检查扣划公用数据
		checkInputData(cplIn);

		// 检查扣划对象的合法性
		for (DpLawDeductAcctIn cplAcctIn : cplIn.getList_lawdeduct_acct()) {

			checkDeductAcct(cplAcctIn, cplIn);
		}

		bizlog.method("DpForceDeductCheck.checkMain end <<<<<<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月16日-下午7:27:35</li>
	 *         <li>功能说明：扣划公用数据检查</li>
	 *         </p>
	 * @param cplIn
	 *            法院扣划支取输入接口
	 */
	private static void checkInputData(DpMultiForceDeductIn cplIn) {
		bizlog.method(" DpForceDeductCheck.checkInputData begin >>>>>>>>>>>>>>>>");

		// 扣划方式不可为空
		BizUtil.fieldNotNull(cplIn.getForce_deduct_method(), DpDict.A.force_deduct_method.getId(), DpDict.A.force_deduct_method.getLongName());

		// 摘要代码
		BizUtil.fieldNotNull(cplIn.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());

		// 对方币种不能为空
		BizUtil.fieldNotNull(cplIn.getOpp_acct_ccy(), SysDict.A.opp_acct_ccy.getId(), SysDict.A.opp_acct_ccy.getLongName());

		// 解冻扣划冻结编号不能为NULL
		if (cplIn.getForce_deduct_method() == E_DEDUCTWAY.UNFROZE && CommUtil.isNull(cplIn.getFroze_no())) {
			throw APPUB.E0001(SysDict.A.froze_no.getId(), SysDict.A.froze_no.getLongName());
		}

		// 扣划对象不可为null
		if (cplIn.getList_lawdeduct_acct() == null || cplIn.getList_lawdeduct_acct().size() <= 0) {

			throw DpErr.Dp.E0104();
		}

		for (DpLawDeductAcctIn cplAcctIn : cplIn.getList_lawdeduct_acct()) {

			// 扣划原因不能为空
			BizUtil.fieldNotNull(cplAcctIn.getForce_deduct_reason(), DpDict.A.force_deduct_reason.getId(), DpDict.A.force_deduct_reason.getLongName());

			// 交易金额不能为空
			BizUtil.fieldNotNull(cplAcctIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

			// 货币代码不能为空
			BizUtil.fieldNotNull(cplAcctIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

			// 检查金额合法性
			ApCurrencyApi.chkAmountByCcy(cplAcctIn.getCcy_code(), cplAcctIn.getTrxn_amt());

			// TODO: 扣划原因下拉字典
		}

		bizlog.method(" DpForceDeductCheck.checkInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月7日-下午3:35:46</li>
	 *         <li>功能说明：检查扣划账户的合法性</li>
	 *         </p>
	 * @param cplAcctIn
	 *            法院扣划账户输入
	 * @param cplIn
	 *            法院扣划输入
	 */
	private static void checkDeductAcct(DpLawDeductAcctIn cplAcctIn, DpMultiForceDeductIn cplIn) {

		bizlog.method(" DpForceDeductCheck.checkSubAcctInfo begin >>>>>>>>>>>>>>>>");

		if (CommUtil.equals(cplAcctIn.getAcct_no(), cplIn.getOpp_acct_no())) {

			throw DpErr.Dp.E0329(cplAcctIn.getAcct_no(), cplIn.getOpp_acct_no());
		}

		// 定位子账号
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplAcctIn.getAcct_no());// 账号
		accessIn.setAcct_type(cplAcctIn.getAcct_type());// 账号类型
		accessIn.setCcy_code(cplAcctIn.getCcy_code());// 货币代码
		accessIn.setSub_acct_seq(cplAcctIn.getSub_acct_seq());// 子账户序号
		accessIn.setProd_id(cplAcctIn.getProd_id());// 产品编号

		// 定位子账号
		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 查询账户信息
		DpaAccount AcctInfo = DpaAccountDao.selectOne_odb1(accessOut.getAcct_no(), true);

		if (AcctInfo.getAcct_busi_source() != E_ACCOUNTBUSINESSSOURCE.DEPOSIT) {

			throw DpErr.Dp.E0243();
		}

		// 司法扣划解冻原因
		String unfrozeReason = ApBusinessParmApi.getValue(DpConst.UNFROZE_REASON, "LAW_DEDUCT");

		// 法院冻结不会录入冻结特征码， 所以扣划时不用考虑冻结特征码
		// 活期子户
		if (accessOut.getDd_td_ind() == E_DEMANDORTIME.DEMAND) {

			// 初始化活期支取检查
			DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

			demandDrawIn.setSub_acct_seq(cplAcctIn.getSub_acct_seq());// 子户序号
			demandDrawIn.setAcct_no(cplAcctIn.getAcct_no());// 账号
			demandDrawIn.setAcct_type(cplAcctIn.getAcct_type());// 账号类型
			demandDrawIn.setAcct_name(cplAcctIn.getAcct_name());// 账号名称
			demandDrawIn.setProd_id(cplAcctIn.getProd_id());// 产品编号
			demandDrawIn.setCcy_code(cplAcctIn.getCcy_code());// 货币代号
			demandDrawIn.setBack_value_date(cplAcctIn.getBack_value_date());
			demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			demandDrawIn.setRed_blue_word_ind(null);//
			demandDrawIn.setForce_draw_ind(cplAcctIn.getForce_draw_ind());// 是否强制标志
			demandDrawIn.setCheck_password_ind(E_YESORNO.NO);// 验密标志
			demandDrawIn.setTrxn_password(null);// 交易密码
			demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.DEDUCT);// 支取类型
			demandDrawIn.setTrxn_amt(cplAcctIn.getTrxn_amt());// 交易金额
			demandDrawIn.setFroze_no(cplIn.getFroze_no());// 冻结编号
			demandDrawIn.setUnfroze_amt(cplAcctIn.getUnfroze_amt());// 解冻金额
			demandDrawIn.setUnfroze_reason(unfrozeReason);// 解冻原因
			demandDrawIn.setSummary_code(cplIn.getSummary_code());// 摘要代码
			demandDrawIn.setTrxn_remark(cplIn.getTrxn_remark());// 交易备注
			demandDrawIn.setCustomer_remark(cplIn.getCustomer_remark());// 客户备注
			demandDrawIn.setOpen_voch_check_ind(null);// 开户凭证检查
			demandDrawIn.setAcct_hold_check_Ind(E_YESORNO.NO);// 账户限制检查标志

			// 活期支取检查
			BizUtil.getInstance(SrvDpDemandAccounting.class).demandDrawCheck(demandDrawIn);
		}
		else {// 定期子户

			// 初始化定期支取检查
			DpTimeDrawIn timeDrawIn = BizUtil.getInstance(DpTimeDrawIn.class);

			timeDrawIn.setCard_no(cplAcctIn.getAcct_no());// 卡号
			timeDrawIn.setAcct_no(cplAcctIn.getAcct_no());// 账号
			timeDrawIn.setAcct_type(cplAcctIn.getAcct_type());// 账号类型
			timeDrawIn.setAcct_name(cplAcctIn.getAcct_name());// 账号名称
			timeDrawIn.setSub_acct_seq(cplAcctIn.getSub_acct_seq());// 子账号序号
			timeDrawIn.setCcy_code(cplAcctIn.getCcy_code());// 货币代号
			timeDrawIn.setBack_value_date(cplAcctIn.getBack_value_date());// 起息日期
			timeDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN);// 现转标志
			timeDrawIn.setCheck_password_ind(null);// 验密标志
			timeDrawIn.setTrxn_password(null);// 交易密码
			timeDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.DEDUCT);// 支取业务种类
			timeDrawIn.setTrxn_amt(cplAcctIn.getTrxn_amt());// 交易金额
			timeDrawIn.setFroze_no(cplIn.getFroze_no());// 冻结编号
			timeDrawIn.setUnfroze_amt(cplAcctIn.getUnfroze_amt());// 解冻金额
			timeDrawIn.setUnfroze_reason(unfrozeReason);// 解冻原因
			timeDrawIn.setSummary_code(cplIn.getSummary_code());// 摘要代码
			timeDrawIn.setTrxn_remark(cplIn.getTrxn_remark());// 交易备注
			timeDrawIn.setCustomer_remark(cplIn.getCustomer_remark());// 客户备注
			timeDrawIn.setOpen_voch_check_ind(null);// 开户凭证检查
			timeDrawIn.setAcct_hold_check_Ind(null);// 账户限制检查标志
			timeDrawIn.setForce_draw_ind(cplAcctIn.getForce_draw_ind());// 是否强制标志
			timeDrawIn.setVoch_type(null);// 凭证种类
			timeDrawIn.setVoch_no(null);// 凭证号码
			timeDrawIn.setNew_voch_no(null);// 新凭证号码
			timeDrawIn.setInst_handling_method(null);// 指定利率标志
			timeDrawIn.setInrt_code(null);// 利率编号
			timeDrawIn.setEfft_inrt(null);// 账户执行利率
			timeDrawIn.setInrt_float_method(null);// 利率浮动方式
			timeDrawIn.setInrt_float_value(null);// 利率浮动值

			BizUtil.getInstance(SrvDpTimeAccounting.class).timeDrawCheck(timeDrawIn);
		}

		bizlog.method(" DpForceDeductCheck.checkSubAcctInfo end <<<<<<<<<<<<<<<<");
	}
}
