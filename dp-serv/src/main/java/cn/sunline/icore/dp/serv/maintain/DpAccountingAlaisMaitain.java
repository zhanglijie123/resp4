package cn.sunline.icore.dp.serv.maintain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAccountApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.type.ComApAccounting.ApAccountingEventIn;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpParmeterMart;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUNTINGSUBJECT;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_BALPROPERTY;
import cn.sunline.icore.sys.type.EnumType.E_CUSTOMERTYPE;
import cn.sunline.icore.sys.type.EnumType.E_REDBLUEWORDIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：核算别名维护
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年8月15日-下午5:31:08</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年8月15日-HongBiao：核算别名维护</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAccountingAlaisMaitain {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAccountingAlaisMaitain.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年8月15日-下午5:32:47</li>
	 *         <li>功能说明：客户信息修改同步修改存款关联信息</li>
	 *         <li>功能说明：修改账户名称、子户核算别名、产品适用条件等</li>
	 *         </p>
	 * @param custNo
	 *            客户号
	 */
	public static void modifyCustomerRelateInfo(String custNo, E_CUSTOMERTYPE custType) {

		bizlog.method(" DpAccountingAlaisMaitain.modifyCustomerRelateInfo begin >>>>>>>>>>>>>>>>");
		bizlog.debug("custNo = [%s]", custNo);

		// 查询客户下账户集
		List<DpaAccount> custAcctList = DpaAccountDao.selectAll_odb2(custNo, false);

		if (CommUtil.isNull(custAcctList) || custAcctList.size() == 0) {
			return;
		}

		// 查询客户信息
		Map<String, Object> custMapInfo = DpPublicCheck.getCustMapInfo(custNo, custType);

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		String custName = custMapInfo.get(SysDict.A.cust_name.getId()).toString();
		String countryCode = custMapInfo.containsKey(SysDict.A.country_code.getId()) ? custMapInfo.get(SysDict.A.country_code.getId()).toString() : "";

		// 加载客户信息集
		ApBufferApi.addData(ApConst.CUST_DATA_MART, custMapInfo);

		for (DpaAccount acctInfo : custAcctList) {

			// 已经销户并且已经过了冲账许可时间，则无需往下处理
			if (acctInfo.getAcct_status() == E_ACCTSTATUS.CLOSE && CommUtil.compare(acctInfo.getClose_acct_date(), trxnDate) < 0) {
				continue;
			}

			// 个人客户有修改户名，或者有修改国别，则维护客户信息
			if ((acctInfo.getCust_type() == E_CUSTOMERTYPE.PERSONAL && !CommUtil.equals(custName, acctInfo.getAcct_name()))
					|| !CommUtil.equals(countryCode, acctInfo.getCountry_code())) {

				// 带锁查询账户
				DpaAccount accountWithLock = DpaAccountDao.selectOneWithLock_odb1(acctInfo.getAcct_no(), true);

				// 个人账户名和客户名保持一致
				if (acctInfo.getCust_type() == E_CUSTOMERTYPE.PERSONAL) {
					accountWithLock.setAcct_name(custName);
				}

				accountWithLock.setCountry_code(countryCode);

				DpaAccountDao.updateOne_odb1(accountWithLock);
			}

			// 查询账户下子户
			List<DpaSubAccount> SubAcctList = DpaSubAccountDao.selectAll_odb3(acctInfo.getAcct_no(), false);

			for (DpaSubAccount subAccount : SubAcctList) {

				// 已销户不处理， 暂不考虑当天销户有当天冲账的可能性
				if (subAccount.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
					continue;
				}

				// 带锁查询子账户
				DpaSubAccount subAccountWithLock = DpaSubAccountDao.selectOneWithLock_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), true);

				if (acctInfo.getCust_type() == E_CUSTOMERTYPE.PERSONAL) {
					subAccountWithLock.setSub_acct_name(custName);
				}

				subAccountWithLock.setCountry_code(countryCode);

				// 修改子户核算别名
				modifyAccountingalias(subAccountWithLock, subAccountWithLock);

				// 再次读最新缓存
				subAccountWithLock = DpaSubAccountDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), true);

				// 业务准入条件刷新
				DpAccountMaintain.modifySubProdBusiCond(subAccountWithLock);
			}
		}

		bizlog.method(" DpAccountingAlaisMaitain.modifyCustomerRelateInfo end <<<<<<<<<<<<<<<<");

	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年8月15日-下午5:32:09</li>
	 *         <li>功能说明：存款子账户核算别名维护</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param beforeSubAcct
	 *            原子户信息
	 */
	public static void modifyAccountingalias(DpaSubAccount subAcct, DpaSubAccount beforeSubAcct) {

		bizlog.method(" DpAccountingAlaisMaitain.modifyAccountingalias begin >>>>>>>>>>>>>>>>");
		bizlog.debug("subAcct = [%s]", subAcct);

		// 存放同业不做核算别名变更
		if (subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET) {
			return;
		}

		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), true);

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(subAcct.getCust_no(), subAcct.getCust_type());

		// 参数数据集
		DpParmeterMart cplParmMart = BizUtil.getInstance(DpParmeterMart.class);

		cplParmMart.setAcct_type(acctInfo.getAcct_type());
		cplParmMart.setCcy_code(subAcct.getCcy_code());
		cplParmMart.setProd_id(subAcct.getProd_id());

		// 加载参数数据集
		DpToolsApi.addDataToParmBuffer(cplParmMart);

		// 获取核算别名
		String sceneCode = ApRuleApi.getFirstResultByScene(DpConst.ACCOUNTING_ALIAS_RULE_SCENE_CODE);

		// 未定位到核算别名报错
		if (CommUtil.isNull(sceneCode)) {

			throw DpBase.E0406();
		}

		// 校验返回的核算别名是否与子户保存的核算别名一致,不一致时修改子户核算别名并调用会计事件登记
		if (!CommUtil.equals(sceneCode, subAcct.getAccounting_alias())) {

			// 备份原渠道码和支付模式
			String trxnChannel = BizUtil.getTrxRunEnvs().getChannel_id();
			String paymentMode = BizUtil.getTrxRunEnvs().getPayment_mode();

			BizUtil.getTrxRunEnvs().setChannel_id(ApConst.SYSTEM_BATCH);
			BizUtil.getTrxRunEnvs().setPayment_mode("RCL");

			// 克隆备份
			DpaSubAccount oldSubAcct = BizUtil.clone(DpaSubAccount.class, subAcct);

			BigDecimal acctBal = subAcct.getAcct_bal();
			BigDecimal acctFloatBal = subAcct.getAcct_float_bal();

			// 账单表里面涉及到产品号, 因此红字账单部分登记子户更新前的信息
			subAcct.setProd_id(beforeSubAcct.getProd_id());
			subAcct.setAcct_bal(CommUtil.compare(subAcct.getAcct_bal(), BigDecimal.ZERO) >= 0 ? BigDecimal.ZERO : subAcct.getAcct_bal());
			subAcct.setAcct_float_bal(BigDecimal.ZERO);

			// 1 原核算别名 - 存款本金 (红字): 账单里面要登记原核算别名，所以要在子账户更新前
			if (!CommUtil.equals(acctBal, BigDecimal.ZERO) || !CommUtil.equals(acctFloatBal, BigDecimal.ZERO)) {
				regAccountingEvent(subAcct, acctBal, acctFloatBal, E_REDBLUEWORDIND.RED);
			}

			// 2.1 重新查询最新的,前面有登记账单，版本序号有变动
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

			subAcct.setAccounting_alias(sceneCode);

			// 还原前面因登记账单需要而修改的子户信息
			subAcct.setProd_id(oldSubAcct.getProd_id());
			subAcct.setAcct_bal(oldSubAcct.getAcct_bal());
			subAcct.setAcct_float_bal(oldSubAcct.getAcct_float_bal());

			// 2.2 修改子户核算别名
			DpaSubAccountDao.updateOne_odb1(subAcct);

			// 3 新核算别名 - 存款本金
			if (!CommUtil.equals(acctBal, BigDecimal.ZERO) || !CommUtil.equals(acctFloatBal, BigDecimal.ZERO)) {
				regAccountingEvent(subAcct, acctBal, acctFloatBal, E_REDBLUEWORDIND.BLUE);
			}

			// 4.登记业务审计日志
			ApDataAuditApi.regLogOnUpdateBusiness(subAcct, oldSubAcct);

			// 还原渠道和支付模式
			BizUtil.getTrxRunEnvs().setChannel_id(trxnChannel);
			BizUtil.getTrxRunEnvs().setPayment_mode(paymentMode);
		}

		bizlog.method(" DpAccountingAlaisMaitain.modifyAccountingalias end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年8月16日-上午10:16:46</li>
	 *         <li>功能说明：登记会计事件</li>
	 *         </p>
	 * @param subAcct
	 *            子户信息
	 * @param acctBal
	 *            账户余额
	 * @param acctFloatBal
	 *            账户浮动余额
	 */
	private static void regAccountingEvent(DpaSubAccount subAcct, BigDecimal acctBal, BigDecimal acctFloatBal, E_REDBLUEWORDIND redOrBlue) {

		// 分析透支本金和正常本金
		BigDecimal overDrawBal = CommUtil.compare(acctBal, BigDecimal.ZERO) < 0 ? acctBal.abs() : BigDecimal.ZERO;
		BigDecimal normalBal = CommUtil.compare(acctBal, BigDecimal.ZERO) < 0 ? acctFloatBal : acctFloatBal.add(acctBal);

		if (CommUtil.compare(normalBal, BigDecimal.ZERO) < 0) {
			normalBal = BigDecimal.ZERO;
		}

		ApAccountingEventIn cplTaEventIn = BizUtil.getInstance(ApAccountingEventIn.class);

		// 会计事件登记公共信息
		cplTaEventIn.setAccounting_alias(subAcct.getAccounting_alias());
		cplTaEventIn.setAccounting_subject(E_ACCOUNTINGSUBJECT.DEPOSIT);
		cplTaEventIn.setAcct_branch(subAcct.getSub_acct_branch());
		cplTaEventIn.setDouble_entry_ind(E_YESORNO.YES);
		cplTaEventIn.setTrxn_ccy(subAcct.getCcy_code());
		cplTaEventIn.setAcct_no(subAcct.getAcct_no());
		cplTaEventIn.setSub_acct_seq(subAcct.getSub_acct_seq());

		cplTaEventIn.setProd_id(subAcct.getProd_id());
		cplTaEventIn.setSummary_code(ApSystemParmApi.getSummaryCode("ACCOUNTING_ALIAS_MODIFY"));
		cplTaEventIn.setSummary_name(ApSummaryApi.getText(ApSystemParmApi.getSummaryCode("ACCOUNTING_ALIAS_MODIFY")));

		cplTaEventIn.setOpp_acct_no(subAcct.getAcct_no());
		// TODO:cplTaEventIn.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());

		// 账单登记公共信息
		DpUpdAccBalIn cplInput = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplInput.setCash_trxn_ind(E_CASHTRXN.TRXN);
		cplInput.setShow_ind(E_YESORNO.NO); // 核算别名变更的账单不要展示给用户看
		cplInput.setAcct_no(subAcct.getAcct_no());
		cplInput.setSub_acct_no(subAcct.getSub_acct_no());
		cplInput.setSummary_code(cplTaEventIn.getSummary_code());
		cplInput.setTally_record_ind(E_YESORNO.YES);
		cplInput.setTrxn_ccy(subAcct.getCcy_code());
		cplInput.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_ACCOUTANALY.DEPOSIT : E_ACCOUTANALY.NOSTRO);
		cplInput.setOpp_acct_no(subAcct.getAcct_no());
		cplInput.setOpp_acct_ccy(subAcct.getCcy_code());
		cplInput.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());
		cplInput.setOpp_acct_name(subAcct.getSub_acct_name());

		// 调正常本金
		if (!CommUtil.equals(normalBal, BigDecimal.ZERO)) {

			// 让版本号加一，给账单登记使用
			DpaSubAccountDao.updateOne_odb1(subAcct);

			// 会计事件登记
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
			cplTaEventIn.setTrxn_amt(redOrBlue == E_REDBLUEWORDIND.RED ? normalBal.negate() : normalBal);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.DEPOSIT.getValue());

			ApAccountApi.regAccountingEvent(cplTaEventIn);

			// 账单登记
			cplInput.setDebit_credit(E_DEBITCREDIT.CREDIT);
			cplInput.setTrxn_amt(redOrBlue == E_REDBLUEWORDIND.RED ? normalBal.negate() : normalBal);
			cplInput.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL);

			DpAccounting.regBill(cplInput, BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 调透支本金
		if (!CommUtil.equals(overDrawBal, BigDecimal.ZERO)) {

			// 让版本号加一，给账单登记使用
			DpaSubAccountDao.updateOne_odb1(subAcct);

			// 会计事件登记
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
			cplTaEventIn.setTrxn_amt(redOrBlue == E_REDBLUEWORDIND.RED ? overDrawBal.negate() : overDrawBal);
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.CAPITAL.getValue());

			ApAccountApi.regAccountingEvent(cplTaEventIn);

			// 账单登记
			cplInput.setDebit_credit(E_DEBITCREDIT.DEBIT);
			cplInput.setTrxn_amt(redOrBlue == E_REDBLUEWORDIND.RED ? overDrawBal.negate() : overDrawBal);
			cplInput.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL);

			DpAccounting.regBill(cplInput, BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 账户利息的对手方式内部户
		cplInput.setOpp_acct_route(E_ACCOUTANALY.INSIDE);
		cplInput.setOpp_acct_no("");
		cplInput.setOpp_sub_acct_seq("");
		cplInput.setOpp_acct_name("");

		// 调存款应付利息
		if (!CommUtil.equals(subAcct.getLast_prov_inst(), BigDecimal.ZERO)) {

			// 让版本号加一，给账单登记使用
			DpaSubAccountDao.updateOne_odb1(subAcct);

			// 会计事件登记
			cplTaEventIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
			cplTaEventIn.setTrxn_amt(redOrBlue == E_REDBLUEWORDIND.RED ? subAcct.getLast_prov_inst().negate() : subAcct.getLast_prov_inst());
			cplTaEventIn.setBal_attributes(E_BALPROPERTY.INTEREST_PAYABLE.getValue());

			ApAccountApi.regAccountingEvent(cplTaEventIn);

			// 账单登记
			cplInput.setDebit_credit(E_DEBITCREDIT.CREDIT);
			cplInput.setTrxn_amt(redOrBlue == E_REDBLUEWORDIND.RED ? subAcct.getLast_prov_inst().negate() : subAcct.getLast_prov_inst());
			cplInput.setTrxn_record_type(E_TRXNRECORDTYPE.INTEREST);
			cplInput.setInst_tax_rate(BigDecimal.ZERO);
			cplInput.setInst_withholding_tax(BigDecimal.ZERO);

			DpAccounting.regBill(cplInput, BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 透支账户调利息收入
		if (subAcct.getOverdraft_allow_ind() == E_YESORNO.YES) {

			// TODO: 暂不实现
		}

	}
}
