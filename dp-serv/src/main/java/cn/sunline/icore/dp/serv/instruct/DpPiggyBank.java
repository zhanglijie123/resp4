package cn.sunline.icore.dp.serv.instruct;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApChannelApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.sms.ApSms;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfSave;
import cn.sunline.icore.dp.base.type.ComDpFrozeBase.DpFrozeObjectLimitStatus;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_DRAWTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RENEWSAVEWAY;
import cn.sunline.icore.dp.serv.account.close.DpCurrentInterestClear;
import cn.sunline.icore.dp.serv.account.open.DpAddSubAccount;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInstructDao;
import cn.sunline.icore.dp.serv.servicetype.SrvDpCloseAccount;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTransfers;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTransfersDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbPiggyBank;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbPiggyBankDao;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpAacctTriggerFinaCheckIn;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpAgreeTransfersSignInfos;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpCreditCardConsTriggerPiggyFinaIn;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpPiggyBankProtocolMaintainIn;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpPiggyBankProtocolMaintainOut;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpPiggyBankProtocolSignIn;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpPiggyBankProtocolSignOut;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpQryPiggyBankProtocolIn;
import cn.sunline.icore.dp.serv.type.ComDpAgreeProductManagement.DpQryPiggyBankProtocolOut;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpSubAcctInstClearIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpSubAcctInstClearOut;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersMntIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersSignIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersSignOut;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustSimpleInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_AGREETRSFAMOUNTTYPE;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_AGREETRSFTYPE;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_FAILHANDLINGMETHOD;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_PIGGYFINANCEMETHOD;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZESTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTLIMITSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.base.util.RunnableWithReturn;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

public class DpPiggyBank {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPiggyBank.class);

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年10月25日-上午10:00:42</li>
	 *         <li>功能说明：签订存钱罐协议</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpPiggyBankProtocolSignOut piggyBankSign(DpPiggyBankProtocolSignIn cplIn) {
		bizlog.method(" DpPiggyBank.piggyBankSign begin >>>>>>>>>>>>>>>>");

		// 为空检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getPiggy_finance_method(), DpDict.A.piggy_finance_method.getId(), DpDict.A.piggy_finance_method.getLongName());
		BizUtil.fieldNotNull(cplIn.getAmount_ratio(), DpDict.A.amount_ratio.getId(), DpDict.A.amount_ratio.getLongName());

		if (CommUtil.isNull(cplIn.getPiggy_acct())) {
			cplIn.setPiggy_acct(cplIn.getAcct_no());
		}

		if (CommUtil.isNull(cplIn.getEffect_date())) {
			cplIn.setEffect_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		if (CommUtil.isNull(cplIn.getExpiry_date())) {
			cplIn.setExpiry_date(ApConst.DEFAULT_MAX_DATE);
		}

		if (cplIn.getPiggy_finance_method() == E_PIGGYFINANCEMETHOD.RATIO) {

			if (CommUtil.compare(cplIn.getAmount_ratio(), BigDecimal.ZERO) < 1 || CommUtil.compare(cplIn.getAmount_ratio(), new BigDecimal(100)) > 0) {
				throw DpErr.Dp.E0488(cplIn.getAmount_ratio());
			}
		}
		else if (cplIn.getPiggy_finance_method() == E_PIGGYFINANCEMETHOD.AMOUNT) {

			if (CommUtil.compare(cplIn.getAmount_ratio(), BigDecimal.ZERO) < 1) {
				throw DpErr.Dp.E0488(cplIn.getAmount_ratio());
			}

			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getAmount_ratio());
		}

		if (CommUtil.compare(cplIn.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {
			throw APPUB.E0045(cplIn.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		if (CommUtil.compare(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {
			throw DpBase.E0294(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		BizUtil.checkEffectDate(cplIn.getEffect_date(), cplIn.getExpiry_date());

		// 定位活期子户,没有活期子户签不了约
		DpAcctAccessIn cplAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		cplAccessIn.setAcct_no(cplIn.getAcct_no());
		cplAccessIn.setAcct_type(cplIn.getAcct_type());
		cplAccessIn.setCcy_code(cplIn.getCcy_code());
		cplAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

		DpAcctAccessOut cplAccessOut = DpToolsApi.locateSingleSubAcct(cplAccessIn);

		DpaAccount acctInfo = DpaAccountDao.selectOneWithLock_odb1(cplAccessOut.getAcct_no(), true);
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(cplAccessOut.getAcct_no(), cplAccessOut.getSub_acct_no(), true);

		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 已签约，不允许再签
		DpbPiggyBank piggyBank = DpbPiggyBankDao.selectFirst_odb2(acctInfo.getAcct_no(), cplIn.getCcy_code(), E_STATUS.VALID, false);

		if (CommUtil.isNotNull(piggyBank)) {
			throw DpErr.Dp.E0463(cplIn.getAcct_no());
		}

		DpfSave prodSave = DpProductFactoryApi.getProdSaveCtrl(ApBusinessParmApi.getValue("NEW_PIGGY_ACCOUNT", "PROD_ID"), cplIn.getCcy_code());

		// 存钱罐客户化设置的最大留存余额不能超过产品许可上限
		if (CommUtil.isNotNull(cplIn.getMax_remain_bal()) && !CommUtil.equals(prodSave.getMax_remain_bal(), BigDecimal.ZERO)
				&& CommUtil.compare(cplIn.getMax_remain_bal(), prodSave.getMax_remain_bal()) > 0) {
			throw DpErr.Dp.E0464(cplIn.getMax_remain_bal(), prodSave.getMax_remain_bal());
		}

		// 存钱罐客户化设置的每次最大存入金额不能超过产品许可上限
		if (CommUtil.isNotNull(cplIn.getSigl_max_dept_amt()) && !CommUtil.equals(prodSave.getSigl_max_dept_amt(), BigDecimal.ZERO)
				&& CommUtil.compare(cplIn.getSigl_max_dept_amt(), prodSave.getSigl_max_dept_amt()) > 0) {
			throw DpErr.Dp.E0465(cplIn.getSigl_max_dept_amt(), prodSave.getSigl_max_dept_amt());
		}

		BigDecimal maxSiglAmout = prodSave.getSigl_max_dept_amt();
		BigDecimal minSiglAmout = prodSave.getSigl_min_dept_amt();

		if (CommUtil.isNotNull(cplIn.getSigl_max_dept_amt()) && !CommUtil.equals(cplIn.getSigl_max_dept_amt(), BigDecimal.ZERO)) {
			maxSiglAmout = cplIn.getSigl_max_dept_amt();
		}

		// 存钱罐客户化设置的每次固定理财金额不能超过产品许可上限
		if (cplIn.getPiggy_finance_method() == E_PIGGYFINANCEMETHOD.AMOUNT && !CommUtil.equals(maxSiglAmout, BigDecimal.ZERO)
				&& CommUtil.compare(cplIn.getAmount_ratio(), maxSiglAmout) > 0) {
			throw DpErr.Dp.E0490(cplIn.getAmount_ratio(), maxSiglAmout);
		}

		// 存钱罐客户化设置的每次比例理财金额不能小于产品许可下限
		if (cplIn.getPiggy_finance_method() == E_PIGGYFINANCEMETHOD.AMOUNT && CommUtil.compare(cplIn.getAmount_ratio(), minSiglAmout) < 0) {
			throw DpErr.Dp.E0492(cplIn.getAmount_ratio(), minSiglAmout);
		}

		// 活期留存余额
		if (CommUtil.isNotNull(cplIn.getDemand_remain_bal()) && CommUtil.compare(cplIn.getDemand_remain_bal(), subAccount.getMin_remain_bal()) < 0) {
			throw DpErr.Dp.E0494(cplIn.getDemand_remain_bal(), subAccount.getSub_acct_no(), subAccount.getMin_remain_bal());
		}
		else {
			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getDemand_remain_bal());
		}

		// 开存钱罐子户
		DpAddSubAccountOut openSubAccount = openSubAccount(cplIn, acctInfo);

		String relation_agree_no = null;

		if (cplIn.getAgree_trsf_ind() == E_YESORNO.YES) {

			BizUtil.fieldNotNull(cplIn.getAgree_trxn_amt(), DpDict.A.agree_trxn_amt.getId(), DpDict.A.agree_trxn_amt.getLongName());

			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getAgree_trxn_amt());

			// 存钱罐客户化设置的约定转账金额不能超过每次最大存入金额
			if (!CommUtil.equals(maxSiglAmout, BigDecimal.ZERO) && CommUtil.compare(cplIn.getAgree_trxn_amt(), maxSiglAmout) > 0) {
				throw DpErr.Dp.E0491(cplIn.getAgree_trxn_amt(), maxSiglAmout);
			}

			// 存钱罐客户化设置的约定转账金额不能小于每次最小存入金额
			if (CommUtil.compare(cplIn.getAgree_trxn_amt(), minSiglAmout) < 0) {
				throw DpErr.Dp.E0493(cplIn.getAgree_trxn_amt(), minSiglAmout);
			}

			/******** 约定转账签约 *********/
			DpAgreeTransfersSignInfos agreeTrans = BizUtil.getInstance(DpAgreeTransfersSignInfos.class);

			agreeTrans.setAcct_no(acctInfo.getAcct_no());
			agreeTrans.setAcct_type(acctInfo.getAcct_type());
			agreeTrans.setCcy_code(cplIn.getCcy_code());
			agreeTrans.setCheck_password_ind(cplIn.getCheck_password_ind());
			agreeTrans.setTrxn_password(cplIn.getTrxn_password());
			agreeTrans.setEffect_date(cplIn.getEffect_date());
			agreeTrans.setExpiry_date(cplIn.getExpiry_date());
			agreeTrans.setOpp_acct_no(cplIn.getAcct_no());
			agreeTrans.setOpp_branch_id(acctInfo.getAcct_branch());
			agreeTrans.setDemand_remain_bal(cplIn.getDemand_remain_bal());
			agreeTrans.setAgree_cycle(cplIn.getAgree_cycle());
			agreeTrans.setAgree_trxn_amt(cplIn.getAgree_trxn_amt());
			agreeTrans.setBrief_date_symbol(cplIn.getBrief_date_symbol());

			DpAgreeTransfersSignOut agreeTransfersSignOut = agreeTransfersSign(agreeTrans);

			relation_agree_no = agreeTransfersSignOut.getAgree_no();
			/*****************************/
		}

		// 生成存钱罐协议号
		String agreeNo = ApSeqApi.genSeq("AGREE_NO");

		DpbPiggyBank tabPiggyBank = BizUtil.getInstance(DpbPiggyBank.class);

		tabPiggyBank.setAgree_no(agreeNo);
		tabPiggyBank.setAcct_no(acctInfo.getAcct_no());
		tabPiggyBank.setSub_acct_seq(cplAccessOut.getSub_acct_seq());
		tabPiggyBank.setCcy_code(cplIn.getCcy_code());
		tabPiggyBank.setEffect_date(cplIn.getEffect_date());
		tabPiggyBank.setExpiry_date(cplIn.getExpiry_date());
		tabPiggyBank.setPiggy_acct(cplIn.getPiggy_acct());
		tabPiggyBank.setPiggy_sub_acct_seq(openSubAccount.getSub_acct_seq());
		tabPiggyBank.setPiggy_product(openSubAccount.getProd_id());
		tabPiggyBank.setMax_remain_bal(CommUtil.nvl(cplIn.getMax_remain_bal(), prodSave.getMax_remain_bal()));
		tabPiggyBank.setPiggy_finance_method(cplIn.getPiggy_finance_method());
		tabPiggyBank.setAmount_ratio(cplIn.getAmount_ratio());
		tabPiggyBank.setSigl_max_dept_amt(CommUtil.nvl(cplIn.getSigl_max_dept_amt(), prodSave.getSigl_max_dept_amt()));
		tabPiggyBank.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), subAccount.getMin_remain_bal()));
		tabPiggyBank.setAgree_trsf_ind(CommUtil.isNotNull(cplIn.getAgree_trsf_ind()) ? cplIn.getAgree_trsf_ind() : E_YESORNO.NO);
		tabPiggyBank.setRelation_agree_no(relation_agree_no);
		tabPiggyBank.setCredit_card_finance_ind(CommUtil.isNotNull(cplIn.getCredit_card_finance_ind()) ? cplIn.getCredit_card_finance_ind() : E_YESORNO.NO);
		tabPiggyBank.setAuto_grap_ind(E_YESORNO.YES);
		tabPiggyBank.setAgree_status(E_STATUS.VALID);
		tabPiggyBank.setRemark(cplIn.getRemark());
		tabPiggyBank.setSign_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		tabPiggyBank.setSign_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());
		tabPiggyBank.setSign_call_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());
		tabPiggyBank.setCancel_date("");
		tabPiggyBank.setCancel_seq("");
		tabPiggyBank.setCancel_call_seq("");

		DpbPiggyBankDao.insert(tabPiggyBank);

		// 输出
		DpPiggyBankProtocolSignOut cplOut = BizUtil.getInstance(DpPiggyBankProtocolSignOut.class);

		cplOut.setAgree_no(agreeNo);
		cplOut.setCust_no(acctInfo.getCust_no());
		cplOut.setCard_no(acctInfo.getCard_relationship_ind() == E_YESORNO.YES ? DpToolsApi.getCardNoByAcctNo(acctInfo.getAcct_no()) : null);
		cplOut.setAcct_no(acctInfo.getAcct_no());
		cplOut.setAcct_name(acctInfo.getAcct_name());
		cplOut.setCcy_code(cplIn.getCcy_code());
		cplOut.setPiggy_acct(cplIn.getPiggy_acct());
		cplOut.setPiggy_product(openSubAccount.getProd_id());
		cplOut.setPiggy_sub_acct_seq(openSubAccount.getSub_acct_seq());

		bizlog.method(" DpPiggyBank.piggyBankSign end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年10月25日-下午5:28:57</li>
	 *         <li>功能说明：存钱罐协议维护</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpPiggyBankProtocolMaintainOut piggyBankMaintain(DpPiggyBankProtocolMaintainIn cplIn) {
		bizlog.method(" DpPiggyBank.piggyBankMaintain begin >>>>>>>>>>>>>>>>");

		// 为空检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getAgree_no(), SysDict.A.agree_no.getId(), SysDict.A.agree_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCancle_agree_ind(), DpBaseDict.A.cancle_agree_ind.getId(), DpBaseDict.A.cancle_agree_ind.getLongName());

		if (cplIn.getCancle_agree_ind() == E_YESORNO.NO) {
			BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());
		}

		DpaAccount tabAccount = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		DpbPiggyBank tabPiggyBank = DpbPiggyBankDao.selectOneWithLock_odb1(tabAccount.getAcct_no(), cplIn.getAgree_no(), false);

		if (CommUtil.isNull(tabPiggyBank)) {
			throw APPUB.E0024(OdbFactory.getTable(DpbPiggyBank.class).getLongname(), SysDict.A.acct_no.getLongName(), tabAccount.getAcct_no(), SysDict.A.agree_no.getLongName(),
					cplIn.getAgree_no());
		}

		if (tabPiggyBank.getAgree_status() != E_STATUS.VALID) {
			throw DpBase.E0296(cplIn.getAgree_no());
		}

		// 校验数据版本号
		if (cplIn.getCancle_agree_ind() == E_YESORNO.NO) {

			if (CommUtil.compare(tabPiggyBank.getData_version(), cplIn.getData_version()) != 0) {

				throw ApPubErr.APPUB.E0018(DpbPiggyBank.class.getName());
			}
		}

		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(tabAccount, checkIn);
		}

		// 存钱罐子户定位
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(tabPiggyBank.getPiggy_acct());
		accessIn.setProd_id(tabPiggyBank.getPiggy_product());
		accessIn.setCcy_code(tabPiggyBank.getCcy_code());
		accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		DpaSubAccount piggySubAcct = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		String relation_agree_no = null;

		/**
		 * 1、解约，解除约定转账协议 2、维护 2.1、约定转账标志yes to yes,维护约定转账协议 2.2、约定转账标志 yes to
		 * no,解除约定转账协议 2.3、约定转账标志 no to yes,签约约定转账协议
		 */
		if (cplIn.getCancle_agree_ind() == E_YESORNO.YES && CommUtil.isNull(cplIn.getAgree_trsf_ind())) {

			// 复制一份,做审计用
			DpbPiggyBank oldPiggyBank = BizUtil.clone(DpbPiggyBank.class, tabPiggyBank);

			if (cplIn.getCredit_card_finance_ind() != E_YESORNO.NO && tabPiggyBank.getCredit_card_finance_ind() == E_YESORNO.YES) {
				throw DpErr.Dp.E0471();
			}

			if (CommUtil.isNotNull(tabPiggyBank.getRelation_agree_no())) {
				// 约定转账解约
				cplIn.setAgree_trsf_ind(E_YESORNO.NO);
				modifyAgreeAgreeTransfers(cplIn, tabPiggyBank);
			}

			// 关闭存钱罐子户并且本金和利息转入活期户
			closeSubAcctAndInrtClear(tabPiggyBank);

			tabPiggyBank.setAgree_status(E_STATUS.INVALID);
			tabPiggyBank.setCancel_date(BizUtil.getTrxRunEnvs().getTrxn_date());
			tabPiggyBank.setCancel_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());
			tabPiggyBank.setCancel_call_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

			// 登记审计
			ApDataAuditApi.regLogOnUpdateBusiness(oldPiggyBank, tabPiggyBank);

			DpbPiggyBankDao.updateOne_odb1(tabPiggyBank);
		}
		else {

			// 复制一份,做审计用
			DpbPiggyBank oldPiggyBank = BizUtil.clone(DpbPiggyBank.class, tabPiggyBank);

			// 生效日期修改检查
			if (CommUtil.isNotNull(cplIn.getEffect_date())) {

				// 记录已经生效不能再修改生效日期
				if (CommUtil.compare(tabPiggyBank.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0
						&& CommUtil.compare(tabPiggyBank.getEffect_date(), cplIn.getEffect_date()) != 0) {

					throw DpErr.Dp.E0052(tabPiggyBank.getPiggy_acct(), cplIn.getEffect_date());
				}

				// 不能把生效日期修改的比交易日期还早
				if (CommUtil.compare(cplIn.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {
					throw APPUB.E0045(cplIn.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
				}
			}

			if (CommUtil.isNotNull(cplIn.getExpiry_date())) {

				// 不能把失效日期修改的比系统日期小
				if (CommUtil.compare(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {
					throw DpBase.E0294(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
				}
			}

			tabPiggyBank.setEffect_date(CommUtil.nvl(cplIn.getEffect_date(), tabPiggyBank.getEffect_date()));
			tabPiggyBank.setExpiry_date(CommUtil.nvl(cplIn.getExpiry_date(), tabPiggyBank.getExpiry_date()));

			BizUtil.checkEffectDate(tabPiggyBank.getEffect_date(), tabPiggyBank.getExpiry_date());

			// 定位活期子户
			DpAcctAccessIn cplAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			cplAccessIn.setAcct_no(tabPiggyBank.getAcct_no());
			cplAccessIn.setAcct_type(accessOut.getAcct_type());
			cplAccessIn.setCcy_code(tabPiggyBank.getCcy_code());
			cplAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

			DpAcctAccessOut cplAccessOut = DpToolsApi.locateSingleSubAcct(cplAccessIn);

			DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(cplAccessOut.getAcct_no(), cplAccessOut.getSub_acct_no(), true);

			// 活期留存余额
			if (CommUtil.isNotNull(cplIn.getDemand_remain_bal()) && CommUtil.compare(cplIn.getDemand_remain_bal(), subAccount.getMin_remain_bal()) < 0) {
				throw DpErr.Dp.E0494(cplIn.getDemand_remain_bal(), subAccount.getSub_acct_no(), subAccount.getMin_remain_bal());
			}
			else {
				ApCurrencyApi.chkAmountByCcy(tabPiggyBank.getCcy_code(), cplIn.getDemand_remain_bal());
			}

			// 产品存入控制
			DpfSave prodSave = DpProductFactoryApi.getProdSaveCtrl(tabPiggyBank.getPiggy_product(), tabPiggyBank.getCcy_code());

			// 存钱罐客户化设置最大账户余额不能超过产品设置上限
			if (CommUtil.isNotNull(cplIn.getMax_remain_bal())) {

				if (!CommUtil.equals(prodSave.getMax_remain_bal(), BigDecimal.ZERO) && CommUtil.compare(cplIn.getMax_remain_bal(), prodSave.getMax_remain_bal()) > 0) {
					throw DpErr.Dp.E0464(cplIn.getMax_remain_bal(), prodSave.getMax_remain_bal());
				}

				tabPiggyBank.setMax_remain_bal(CommUtil.equals(cplIn.getMax_remain_bal(), BigDecimal.ZERO) ? prodSave.getMax_remain_bal() : cplIn.getMax_remain_bal());
				piggySubAcct.setMax_remain_bal(CommUtil.equals(cplIn.getMax_remain_bal(), BigDecimal.ZERO) ? prodSave.getMax_remain_bal() : cplIn.getMax_remain_bal());
			}

			// 存钱罐客户化设置最大单笔存入余额不能超过产品上限
			if (CommUtil.isNotNull(cplIn.getSigl_max_dept_amt())) {

				if (!CommUtil.equals(prodSave.getSigl_max_dept_amt(), BigDecimal.ZERO) && CommUtil.compare(cplIn.getSigl_max_dept_amt(), prodSave.getSigl_max_dept_amt()) > 0) {
					throw DpErr.Dp.E0465(cplIn.getSigl_max_dept_amt(), prodSave.getSigl_max_dept_amt());
				}

				tabPiggyBank.setSigl_max_dept_amt(CommUtil.equals(cplIn.getSigl_max_dept_amt(), BigDecimal.ZERO) ? prodSave.getSigl_max_dept_amt() : cplIn.getSigl_max_dept_amt());
				piggySubAcct.setSigl_max_dept_amt(CommUtil.equals(cplIn.getSigl_max_dept_amt(), BigDecimal.ZERO) ? prodSave.getSigl_max_dept_amt() : cplIn.getSigl_max_dept_amt());
			}

			// 更新子户信息
			DpaSubAccountDao.updateOne_odb1(piggySubAcct);

			BigDecimal maxSiglAmout = prodSave.getSigl_max_dept_amt();
			BigDecimal minSiglAmout = piggySubAcct.getSigl_min_dept_amt();

			if (CommUtil.isNotNull(cplIn.getSigl_max_dept_amt()) && !CommUtil.equals(cplIn.getSigl_max_dept_amt(), BigDecimal.ZERO)) {
				maxSiglAmout = cplIn.getSigl_max_dept_amt();
			}

			if (cplIn.getPiggy_finance_method() == E_PIGGYFINANCEMETHOD.RATIO) {

				if (CommUtil.compare(cplIn.getAmount_ratio(), BigDecimal.ZERO) < 1 || CommUtil.compare(cplIn.getAmount_ratio(), new BigDecimal(100)) > 0) {
					throw DpErr.Dp.E0488(cplIn.getAmount_ratio());
				}
			}
			else if (cplIn.getPiggy_finance_method() == E_PIGGYFINANCEMETHOD.AMOUNT) {

				if (CommUtil.compare(cplIn.getAmount_ratio(), BigDecimal.ZERO) < 1) {
					throw DpErr.Dp.E0488(cplIn.getAmount_ratio());
				}

				ApCurrencyApi.chkAmountByCcy(tabPiggyBank.getCcy_code(), cplIn.getAmount_ratio());
			}

			// 存钱罐客户化设置的每次比例理财金额不能超过产品许可上限
			if (cplIn.getPiggy_finance_method() == E_PIGGYFINANCEMETHOD.AMOUNT && !CommUtil.equals(maxSiglAmout, BigDecimal.ZERO)
					&& CommUtil.compare(cplIn.getAmount_ratio(), maxSiglAmout) > 0) {
				throw DpErr.Dp.E0490(cplIn.getAmount_ratio(), maxSiglAmout);
			}

			// 存钱罐客户化设置的每次比例理财金额不能小于产品许可下限
			if (cplIn.getPiggy_finance_method() == E_PIGGYFINANCEMETHOD.AMOUNT && CommUtil.compare(cplIn.getAmount_ratio(), minSiglAmout) < 0) {
				throw DpErr.Dp.E0492(cplIn.getAmount_ratio(), minSiglAmout);
			}

			// 原存钱罐协议从没有关联过约定转账协议，签订约定转账协议
			if (CommUtil.isNull(tabPiggyBank.getRelation_agree_no()) && cplIn.getAgree_trsf_ind() == E_YESORNO.YES) {

				BizUtil.fieldNotNull(cplIn.getAgree_trxn_amt(), DpDict.A.agree_trxn_amt.getId(), DpDict.A.agree_trxn_amt.getLongName());

				ApCurrencyApi.chkAmountByCcy(tabPiggyBank.getCcy_code(), cplIn.getAgree_trxn_amt());

				// 存钱罐客户化设置的约定转账金额不能超过每次最大存入金额
				if (!CommUtil.equals(maxSiglAmout, BigDecimal.ZERO) && CommUtil.compare(cplIn.getAgree_trxn_amt(), maxSiglAmout) > 0) {
					throw DpErr.Dp.E0491(cplIn.getAgree_trxn_amt(), maxSiglAmout);
				}

				// 存钱罐客户化设置的约定转账金额不能小于每次最小存入金额
				if (CommUtil.compare(cplIn.getAgree_trxn_amt(), minSiglAmout) < 0) {
					throw DpErr.Dp.E0493(cplIn.getAgree_trxn_amt(), minSiglAmout);
				}

				/******** 约定转账签约 *********/
				DpAgreeTransfersSignInfos agreeTrans = BizUtil.getInstance(DpAgreeTransfersSignInfos.class);

				agreeTrans.setAcct_no(tabPiggyBank.getAcct_no());
				agreeTrans.setCcy_code(tabPiggyBank.getCcy_code());
				agreeTrans.setCheck_password_ind(E_YESORNO.NO);
				agreeTrans.setEffect_date(tabPiggyBank.getEffect_date());
				agreeTrans.setExpiry_date(tabPiggyBank.getExpiry_date());
				agreeTrans.setOpp_acct_no(tabPiggyBank.getPiggy_acct());
				agreeTrans.setOpp_acct_ccy(tabPiggyBank.getCcy_code());
				agreeTrans.setMax_turn_out_amt(tabPiggyBank.getSigl_max_dept_amt());
				agreeTrans.setAgree_cycle(cplIn.getAgree_cycle());
				agreeTrans.setAgree_trxn_amt(cplIn.getAgree_trxn_amt());
				agreeTrans.setBrief_date_symbol(cplIn.getBrief_date_symbol());
				agreeTrans.setOpp_branch_id(cplIn.getOpp_branch_id());

				DpAgreeTransfersSignOut agreeTransfersSignOut = agreeTransfersSign(agreeTrans);

				relation_agree_no = agreeTransfersSignOut.getAgree_no();
				/*****************************/

				tabPiggyBank.setRelation_agree_no(CommUtil.nvl(relation_agree_no, tabPiggyBank.getRelation_agree_no()));
			}
			// 协议号为空,约定转账标志 = N, 做解约操作
			else if (cplIn.getCancle_agree_ind() == E_YESORNO.YES && cplIn.getAgree_trsf_ind() == E_YESORNO.NO) {

				modifyAgreeAgreeTransfers(cplIn, tabPiggyBank);
				tabPiggyBank.setRelation_agree_no(null);
			}
			// 解约标志为N,只做停用/启用操作
			else if (CommUtil.isNotNull(tabPiggyBank.getRelation_agree_no())) {
				if (cplIn.getCancle_agree_ind() == E_YESORNO.NO) {

					if (cplIn.getAgree_trsf_ind() == E_YESORNO.NO) {

						// 约定转账协议维护
						cplIn.setStop_use_ind(E_YESORNO.YES);
					}
					else {

						// 约定转账协议维护
						cplIn.setStop_use_ind(E_YESORNO.NO);
					}
					cplIn.setAgree_trsf_ind(E_YESORNO.YES);
					modifyAgreeAgreeTransfers(cplIn, tabPiggyBank);
				}
			}

			tabPiggyBank.setPiggy_finance_method(CommUtil.nvl(cplIn.getPiggy_finance_method(), tabPiggyBank.getPiggy_finance_method()));
			tabPiggyBank.setAmount_ratio(CommUtil.nvl(cplIn.getAmount_ratio(), tabPiggyBank.getAmount_ratio()));
			tabPiggyBank.setAgree_trsf_ind(CommUtil.isNotNull(cplIn.getAgree_trsf_ind()) ? cplIn.getAgree_trsf_ind() : tabPiggyBank.getAgree_trsf_ind());
			tabPiggyBank.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), tabPiggyBank.getDemand_remain_bal()));
			tabPiggyBank.setAuto_grap_ind(CommUtil.isNotNull(cplIn.getAuto_grap_ind()) ? cplIn.getAuto_grap_ind() : tabPiggyBank.getAuto_grap_ind());
			tabPiggyBank.setCredit_card_finance_ind(cplIn.getCredit_card_finance_ind() != null ? cplIn.getCredit_card_finance_ind() : tabPiggyBank.getCredit_card_finance_ind());
			tabPiggyBank.setRemark(CommUtil.nvl(cplIn.getRemark(), tabPiggyBank.getRemark()));

			// 登记审计
			if (ApDataAuditApi.regLogOnUpdateBusiness(oldPiggyBank, tabPiggyBank) != 0) {
				DpbPiggyBankDao.updateOne_odb1(tabPiggyBank);
			}
		}

		// 输出
		DpPiggyBankProtocolMaintainOut cplOut = BizUtil.getInstance(DpPiggyBankProtocolMaintainOut.class);

		cplOut.setAgree_no(cplIn.getAgree_no());
		cplOut.setCust_no(tabAccount.getCust_no());
		cplOut.setCard_no(tabAccount.getCard_relationship_ind() == E_YESORNO.YES ? DpToolsApi.getCardNoByAcctNo(tabAccount.getAcct_no()) : null);
		cplOut.setAcct_no(tabPiggyBank.getAcct_no());
		cplOut.setAcct_name(tabAccount.getAcct_name());
		cplOut.setCcy_code(tabPiggyBank.getCcy_code());
		cplOut.setPiggy_acct(tabPiggyBank.getPiggy_acct());
		cplOut.setPiggy_product(tabPiggyBank.getPiggy_product());
		cplOut.setAgree_trsf_ind(tabPiggyBank.getAgree_trsf_ind());
		cplOut.setCredit_card_finance_ind(tabPiggyBank.getCredit_card_finance_ind());

		bizlog.method(" DpPiggyBank.piggyBankMaintain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年10月29日-下午3:32:47</li>
	 *         <li>功能说明：约定转账签约</li>
	 *         </p>
	 * @param cplIn
	 */
	private static DpAgreeTransfersSignOut agreeTransfersSign(DpAgreeTransfersSignInfos input) {
		bizlog.method(" DpPiggyBank.agreeTransfersSign begin >>>>>>>>>>>>>>>>");

		DpAgreeTransfersSignIn cplIn = BizUtil.getInstance(DpAgreeTransfersSignIn.class);

		cplIn.setAcct_no(input.getAcct_no());
		cplIn.setCcy_code(input.getCcy_code());
		cplIn.setAcct_type(input.getAcct_type());
		cplIn.setProd_id(input.getProd_id());
		cplIn.setCheck_password_ind(E_YESORNO.NO);
		cplIn.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), BigDecimal.ZERO));
		cplIn.setAgree_trsf_type(E_AGREETRSFTYPE.DEMAND_TO_DEMAND);
		cplIn.setEffect_date(input.getEffect_date());
		cplIn.setExpiry_date(input.getExpiry_date());
		cplIn.setOpp_acct_no(input.getOpp_acct_no());
		cplIn.setOpp_acct_ccy(input.getOpp_acct_ccy());
		cplIn.setOpp_branch_id(input.getOpp_branch_id());
		cplIn.setOpp_prod_id(ApBusinessParmApi.getValue("NEW_PIGGY_ACCOUNT", "PROD_ID"));
		cplIn.setAgree_trsf_amt_type(E_AGREETRSFAMOUNTTYPE.FIXED);
		cplIn.setTrxn_amt(input.getAgree_trxn_amt());
		cplIn.setAgree_cycle(input.getAgree_cycle());
		cplIn.setRef_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		cplIn.setBrief_date_symbol(input.getBrief_date_symbol());
		cplIn.setFail_handling_method(E_FAILHANDLINGMETHOD.NOT_HANDLE);

		bizlog.method(" DpPiggyBank.agreeTransfersSign end <<<<<<<<<<<<<<<<");

		/* 约定转账签约 */
		return DpAgreeTransfers.agreeTransfersSign(cplIn);

	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年10月25日-上午11:00:42</li>
	 *         <li>功能说明：开存钱罐子户</li>
	 *         </p>
	 * @param cplIn
	 * @param acctInfo
	 */
	private static DpAddSubAccountOut openSubAccount(DpPiggyBankProtocolSignIn cplIn, DpaAccount acctInfo) {
		bizlog.method(" DpPiggyBank.openSubAccount begin >>>>>>>>>>>>>>>>");

		// 1.开子户输入接口赋值
		DpAddSubAccountIn addSubAccountIn = BizUtil.getInstance(DpAddSubAccountIn.class);

		addSubAccountIn.setAcct_no(cplIn.getPiggy_acct()); // 账号
		addSubAccountIn.setAcct_type(null); // 账户类型
		addSubAccountIn.setTrxn_password(cplIn.getTrxn_password()); // 交易密码
		addSubAccountIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
		addSubAccountIn.setCcy_code(cplIn.getCcy_code()); // 货币代码
		addSubAccountIn.setProd_id(ApBusinessParmApi.getValue("NEW_PIGGY_ACCOUNT", "PROD_ID")); // 产品编号
		addSubAccountIn.setTrxn_amt(BigDecimal.ZERO); // 交易金额
		addSubAccountIn.setSub_acct_branch(BizUtil.getTrxRunEnvs().getTrxn_branch()); // 子账户所属机构
		addSubAccountIn.setChannel_remark(""); // 渠道备注
		addSubAccountIn.setRemark(cplIn.getRemark()); // 备注
		addSubAccountIn.setRenewal_method(E_RENEWSAVEWAY.NONE); // 续存方式
		addSubAccountIn.setInst_ind(E_YESORNO.YES); // 计息标志

		// 2.开子户处理
		DpAddSubAccountOut addSubAcctOut = DpAddSubAccount.doMain(addSubAccountIn);

		// 协议层覆盖产品层
		if (CommUtil.isNotNull(cplIn.getSigl_max_dept_amt()) || CommUtil.isNotNull(cplIn.getMax_remain_bal())) {

			DpaSubAccount tabSubAccount = DpaSubAccountDao.selectOne_odb1(addSubAcctOut.getAcct_no(), addSubAcctOut.getSub_acct_no(), true);

			if (CommUtil.isNotNull(cplIn.getSigl_max_dept_amt()) && !CommUtil.equals(cplIn.getSigl_max_dept_amt(), BigDecimal.ZERO)) {
				tabSubAccount.setSigl_max_dept_amt(cplIn.getSigl_max_dept_amt());
			}

			if (CommUtil.isNotNull(cplIn.getMax_remain_bal()) && !CommUtil.equals(cplIn.getMax_remain_bal(), BigDecimal.ZERO)) {
				tabSubAccount.setMax_remain_bal(cplIn.getMax_remain_bal());
			}

			DpaSubAccountDao.updateOne_odb1(tabSubAccount);
		}

		bizlog.method(" DpPiggyBank.openSubAccount end <<<<<<<<<<<<<<<<");

		return addSubAcctOut;
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年10月30日-下午3:01:26</li>
	 *         <li>功能说明：关闭存钱罐子户并且本金和利息转入活期户</li>
	 *         </p>
	 * @param tabPiggyBank
	 *            存钱罐协议
	 */
	private static void closeSubAcctAndInrtClear(DpbPiggyBank tabPiggyBank) {
		bizlog.method(" DpPiggyBank.closeSubAcctAndInrtClear begin >>>>>>>>>>>>>>>>");

		// 结清存钱罐子户
		DpSubAcctInstClearIn demandDrawIn = BizUtil.getInstance(DpSubAcctInstClearIn.class);

		demandDrawIn.setAcct_no(tabPiggyBank.getPiggy_acct());
		demandDrawIn.setCcy_code(tabPiggyBank.getCcy_code());
		demandDrawIn.setProd_id(tabPiggyBank.getPiggy_product());
		demandDrawIn.setSummary_code(ApSystemParmApi.getSummaryCode("CLOSE_SUB_ACCOUNT"));

		DpSubAcctInstClearOut cplPiggyClearOut = DpCurrentInterestClear.subAccountClear(demandDrawIn);

		// 存钱罐子户支取
		DpDemandDrawIn demandDraw = BizUtil.getInstance(DpDemandDrawIn.class);

		demandDraw.setAcct_no(tabPiggyBank.getPiggy_acct());// 账号
		demandDraw.setCcy_code(tabPiggyBank.getCcy_code());// 币种
		demandDraw.setProd_id(tabPiggyBank.getPiggy_product());
		demandDraw.setOpp_acct_no(tabPiggyBank.getAcct_no());
		demandDraw.setOpp_acct_ccy(tabPiggyBank.getCcy_code());
		demandDraw.setOpp_sub_acct_seq(tabPiggyBank.getSub_acct_seq());
		demandDraw.setTrxn_amt(cplPiggyClearOut.getPaying_amt());
		demandDraw.setCash_trxn_ind(E_CASHTRXN.TRXN);// 现转标志
		demandDraw.setWithdrawal_busi_type(E_DRAWBUSIKIND.CLOSE);// 支取类型
		demandDraw.setSummary_code(ApSystemParmApi.getSummaryCode("CLOSE_SUB_ACCOUNT"));

		BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDraw);

		// 结算子户存入
		DpDemandSaveIn demandSaveIn = BizUtil.getInstance(DpDemandSaveIn.class);

		demandSaveIn.setAcct_no(tabPiggyBank.getAcct_no());// 账号
		demandSaveIn.setCcy_code(tabPiggyBank.getCcy_code());// 币种
		demandSaveIn.setOpp_acct_no(tabPiggyBank.getPiggy_acct());
		demandSaveIn.setOpp_acct_ccy(tabPiggyBank.getCcy_code());
		demandSaveIn.setOpp_sub_acct_seq(cplPiggyClearOut.getSub_acct_seq());
		demandSaveIn.setTrxn_amt(cplPiggyClearOut.getPaying_amt());// 交易金额
		demandSaveIn.setCash_trxn_ind(E_CASHTRXN.TRXN);// 现转标志
		demandSaveIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);// 对方账户路由
		demandSaveIn.setSummary_code(ApSystemParmApi.getSummaryCode("CLOSE_SUB_ACCOUNT"));

		BizUtil.getInstance(SrvDpDemandAccounting.class).demandSave(demandSaveIn);

		// 销存钱罐子户处理
		DpCloseSubAccountIn cplIn = BizUtil.getInstance(DpCloseSubAccountIn.class);

		cplIn.setAcct_no(tabPiggyBank.getPiggy_acct());
		cplIn.setCcy_code(tabPiggyBank.getCcy_code());
		cplIn.setProd_id(tabPiggyBank.getPiggy_product());
		cplIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
		cplIn.setCheck_password_ind(E_YESORNO.NO);
		cplIn.setRemark(tabPiggyBank.getRemark());

		BizUtil.getInstance(SrvDpCloseAccount.class).closeSubAccount(cplIn);

		bizlog.method(" DpPiggyBank.closeSubAcctAndInrtClear end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年10月29日-下午5:05:58</li>
	 *         <li>功能说明：约定转账维护</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void modifyAgreeAgreeTransfers(DpPiggyBankProtocolMaintainIn cplIn, DpbPiggyBank tabPiggyBank) {
		bizlog.method(" DpPiggyBank.modifyAgreeAgreeTransfers begin >>>>>>>>>>>>>>>>");

		if (CommUtil.isNull(tabPiggyBank.getRelation_agree_no())) {
			return;
		}

		// 获取约定转账信息
		DpbAgreeTransfers tabAgreeTransfers = DpbAgreeTransfersDao.selectOne_odb1(tabPiggyBank.getAcct_no(), tabPiggyBank.getRelation_agree_no(), true);

		DpAgreeTransfersMntIn mntIn = BizUtil.getInstance(DpAgreeTransfersMntIn.class);

		mntIn.setEffect_date(CommUtil.nvl(cplIn.getEffect_date(), tabAgreeTransfers.getEffect_date()));
		mntIn.setExpiry_date(CommUtil.nvl(cplIn.getExpiry_date(), tabAgreeTransfers.getExpiry_date()));
		mntIn.setTrxn_amt(CommUtil.nvl(cplIn.getAgree_trxn_amt(), tabAgreeTransfers.getTrxn_amt()));
		mntIn.setAgree_cycle(CommUtil.nvl(cplIn.getAgree_cycle(), tabAgreeTransfers.getAgree_cycle()));
		mntIn.setRef_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		mntIn.setBrief_date_symbol(cplIn.getBrief_date_symbol());
		mntIn.setData_version(tabAgreeTransfers.getData_version());
		mntIn.setAgree_no(tabAgreeTransfers.getAgree_no());
		mntIn.setAcct_no(tabAgreeTransfers.getAcct_no());
		mntIn.setCancle_agree_ind(CommUtil.nvl(cplIn.getCancle_agree_ind(), E_YESORNO.NO));
		mntIn.setCheck_password_ind(E_YESORNO.NO);
		mntIn.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), BigDecimal.ZERO));
		mntIn.setStop_use_ind(CommUtil.nvl(cplIn.getStop_use_ind(), tabAgreeTransfers.getStop_use_ind()));

		// 约定转账启用停用修改
		if (cplIn.getAgree_trsf_ind() != null && cplIn.getStop_use_ind() == null) {
			mntIn.setStop_use_ind(cplIn.getAgree_trsf_ind() == E_YESORNO.NO ? E_YESORNO.YES : E_YESORNO.NO);
		}

		// 不取消约定转账协议并且失效日期早于系统日期时,不做修改
		if (mntIn.getCancle_agree_ind() == E_YESORNO.NO && CommUtil.compare(BizUtil.getTrxRunEnvs().getTrxn_date(), mntIn.getExpiry_date()) >= 0) {
			return;
		}

		/* 约定转账协议维护 */
		DpAgreeTransfers.agreeTransfersMnt(mntIn);

		bizlog.method(" DpPiggyBank.modifyAgreeAgreeTransfers end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年10月30日-下午7:46:55</li>
	 *         <li>功能说明：存钱罐协议查询</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static Options<DpQryPiggyBankProtocolOut> qryPiggyBankProtocol(DpQryPiggyBankProtocolIn cplIn) {
		bizlog.method(" DpPiggyBank.qryPiggyBankProtocol begin >>>>>>>>>>>>>>>>");

		// 存钱罐协议记录输出
		Options<DpQryPiggyBankProtocolOut> qryPiggyBankOutput = new DefaultOptions<DpQryPiggyBankProtocolOut>();

		// 获取公共运行变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		String acctNo = null;

		if (CommUtil.isNotNull(cplIn.getAcct_no())) {
			// 取账号信息
			acctNo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false).getAcct_no();
		}
		else if (CommUtil.isNotNull(cplIn.getCust_no())) {

			DpbPiggyBank piggyBank = SqlDpInstructDao.selPiggyBankByCust(runEnvs.getBusi_org_id(), cplIn.getCust_no(), false);

			if (CommUtil.isNotNull(piggyBank)) {
				acctNo = piggyBank.getAcct_no();
			}
		}

		Page<DpQryPiggyBankProtocolOut> PiggyBankProtocolList = SqlDpInstructDao.selPiggyBankProtocol(runEnvs.getBusi_org_id(), acctNo, cplIn.getCcy_code(),
				cplIn.getAgree_status(), runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		// 从约定转账协议中获取对应的约定周期、简明日期字符
		for (DpQryPiggyBankProtocolOut out : PiggyBankProtocolList.getRecords()) {

			if (out.getAgree_trsf_ind() == E_YESORNO.YES) {

				DpbAgreeTransfers tabAgreeTransfers = DpbAgreeTransfersDao.selectOne_odb1(out.getAcct_no(), out.getRelation_agree_no(), true);

				out.setAgree_cycle(tabAgreeTransfers.getAgree_cycle());
				out.setAgree_trxn_amt(tabAgreeTransfers.getTrxn_amt());
				out.setBrief_date_symbol(tabAgreeTransfers.getBrief_date_symbol());
			}

			// 存钱罐子户
			DpaSubAccount piggyAcct = DpaSubAccountDao.selectOne_odb4(out.getAcct_no(), out.getPiggy_sub_acct_seq(), false);

			// 获取限制状态
			DpFrozeObjectLimitStatus limitStatusInfo = DpToolsApi.getRealTimeLimitStatusForDraw(piggyAcct, null);

			out.setSigl_min_dept_amt(piggyAcct.getSigl_min_dept_amt());
			out.setFroze_status(E_FROZESTATUS.CLOSE);

			if (limitStatusInfo.getAcct_limit_status() != E_ACCTLIMITSTATUS.NONE || limitStatusInfo.getCust_limit_status() != E_ACCTLIMITSTATUS.NONE
					|| limitStatusInfo.getSub_acct_limit_status() != E_ACCTLIMITSTATUS.NONE) {
				out.setFroze_status(E_FROZESTATUS.FROZE);
			}
		}

		runEnvs.setTotal_count(PiggyBankProtocolList.getRecordCount());

		qryPiggyBankOutput.setValues(PiggyBankProtocolList.getRecords());

		bizlog.method(" DpPiggyBank.qryPiggyBankProtocol end <<<<<<<<<<<<<<<<");

		return qryPiggyBankOutput;
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年11月9日-下午2:11:03</li>
	 *         <li>功能说明：存钱罐理财</li>
	 *         </p>
	 * @param tabPiggyBank
	 *            存期罐理财协议
	 * @param trxnAmt
	 *            交易金额
	 */
	private static void acctConsTriggerPiggyFina(final DpbPiggyBank tabPiggyBank, BigDecimal trxnAmt, final String remark) {
		bizlog.method(" DpPiggyBank.acctConsTriggerPiggyFina start <<<<<<<<<<<<<<<<");

		if (tabPiggyBank.getAgree_status() == E_STATUS.INVALID) {
			return;
		}

		if (tabPiggyBank.getAuto_grap_ind() == E_YESORNO.NO) {
			return;
		}

		BigDecimal finaAmt = BigDecimal.ZERO; // 理财金额
		final E_PIGGYFINANCEMETHOD financeMethod = tabPiggyBank.getPiggy_finance_method();
		switch (financeMethod) {
		case AMOUNT:
			finaAmt = tabPiggyBank.getAmount_ratio();
			break;
		case RATIO:
			finaAmt = ApCurrencyApi.roundAmount(tabPiggyBank.getCcy_code(), trxnAmt.multiply(tabPiggyBank.getAmount_ratio()).divide(new BigDecimal(100)));
			break;
		case REDOUBLE:
			int intValue = trxnAmt.setScale(2, BigDecimal.ROUND_HALF_UP).divide(tabPiggyBank.getAmount_ratio()).intValue() + 1;
			finaAmt = tabPiggyBank.getAmount_ratio().multiply(new BigDecimal(intValue)).subtract(trxnAmt);
			if (CommUtil.compare(tabPiggyBank.getAmount_ratio(), finaAmt) == 0) {
				finaAmt = BigDecimal.ZERO;
			}
			break;
		}

		// 单次理财金额不能超过上限
		if (!CommUtil.equals(tabPiggyBank.getSigl_max_dept_amt(), BigDecimal.ZERO) && CommUtil.compare(finaAmt, tabPiggyBank.getSigl_max_dept_amt()) > 0) {
			finaAmt = tabPiggyBank.getSigl_max_dept_amt();
		}

		// 定位存钱罐子户
		DpAcctAccessIn cplPiggyAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		cplPiggyAccessIn.setAcct_no(tabPiggyBank.getPiggy_acct());
		cplPiggyAccessIn.setCcy_code(tabPiggyBank.getCcy_code());
		cplPiggyAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
		cplPiggyAccessIn.setProd_id(tabPiggyBank.getPiggy_product());

		DpAcctAccessOut cplPiggyAccessOut = DpToolsApi.locateSingleSubAcct(cplPiggyAccessIn);

		final DpaSubAccount piggySubAccount = DpaSubAccountDao.selectOne_odb1(cplPiggyAccessOut.getAcct_no(), cplPiggyAccessOut.getSub_acct_no(), false);

		// 正常触发理财标志
		boolean autoGrabCorrect = true;

		// 不超过存钱罐最大留存金额
		if (!CommUtil.equals(tabPiggyBank.getMax_remain_bal(), BigDecimal.ZERO)
				&& CommUtil.compare(piggySubAccount.getAcct_bal().add(finaAmt), tabPiggyBank.getMax_remain_bal()) > 0) {
			autoGrabCorrect = false;
		}

		// 单次理财金额不能小于下限
		if (CommUtil.compare(finaAmt, piggySubAccount.getSigl_min_dept_amt()) < 0) {
			finaAmt = BigDecimal.ZERO;
		}

		// 记账金额为零，中断处理
		if (CommUtil.equals(finaAmt, BigDecimal.ZERO)) {
			autoGrabCorrect = false;
		}

		// 结算子户定位
		DpAcctAccessIn cplAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		cplAccessIn.setAcct_no(tabPiggyBank.getAcct_no());
		cplAccessIn.setCcy_code(tabPiggyBank.getCcy_code());
		cplAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);
		cplAccessIn.setSub_acct_seq(tabPiggyBank.getSub_acct_seq());

		DpAcctAccessOut cplAccessOut = DpToolsApi.locateSingleSubAcct(cplAccessIn);

		final DpaSubAccount settleSubAccount = DpaSubAccountDao.selectOne_odb1(cplAccessOut.getAcct_no(), cplAccessOut.getSub_acct_no(), false);

		// 扣除理财金额后，活期账户余额低于协议指定的留存金额则不做理财处理
		if (!CommUtil.equals(tabPiggyBank.getDemand_remain_bal(), BigDecimal.ZERO)
				&& CommUtil.compare(settleSubAccount.getAcct_bal().subtract(finaAmt), tabPiggyBank.getDemand_remain_bal()) < 0) {
			autoGrabCorrect = false;
		}

		// 计算结算子户可用余额
		DpBalanceCalculateOut cplBalInfo = DpToolsApi.getBalance(settleSubAccount.getSub_acct_no(), settleSubAccount.getAcct_no(), E_DRAWTYPE.COMMON);

		// 若消费后结算子户自身可用余额不足理财金额则放弃本次理财，不能从智能存款关联保护里面取出资金
		if (CommUtil.compare(finaAmt, cplBalInfo.getSelf_usable_bal()) > 0) {
			autoGrabCorrect = false;
		}

		// 不能正常触发理财，推送站内消息后退出
		if (!autoGrabCorrect) {

			autoGrabPushInApp("SMS0003", piggySubAccount, settleSubAccount, finaAmt, financeMethod, remark);

			return;
		}

		final String settleSubAcctSeq = cplAccessOut.getSub_acct_seq();
		final String piggySubAcctSeq = cplPiggyAccessOut.getSub_acct_seq();
		final BigDecimal piggyFinaAmt = finaAmt;

		// 使用独立事物，存钱罐理财失败不影响主交易成功 Auto grab
		DaoUtil.executeInSubTransation("Auto_grab", new RunnableWithReturn<Boolean>() {
			@Override
			public Boolean execute() {
				bizlog.method(" Auto grab execute begin >>>>>>>>>>>>>>>>");

				System.err.println("Auto grab execute begin >>>>>>>>>>>>>>>");
				try {

					// 活期子户余额支取
					DpDemandDrawIn demandDraw = BizUtil.getInstance(DpDemandDrawIn.class);

					demandDraw.setAcct_no(tabPiggyBank.getAcct_no());// 账号
					demandDraw.setCcy_code(tabPiggyBank.getCcy_code());// 币种
					demandDraw.setOpp_acct_no(tabPiggyBank.getPiggy_acct());
					demandDraw.setOpp_acct_ccy(tabPiggyBank.getCcy_code());
					demandDraw.setOpp_sub_acct_seq(piggySubAcctSeq);
					demandDraw.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
					demandDraw.setOpp_trxn_amt(piggyFinaAmt);
					demandDraw.setTrxn_amt(piggyFinaAmt);// 交易金额
					demandDraw.setCash_trxn_ind(E_CASHTRXN.TRXN);// 现转标志
					demandDraw.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_AUTO_GRAB_TO_PIGGY"));
					demandDraw.setCustomer_remark(remark);
					demandDraw.setTrxn_remark(tabPiggyBank.getPiggy_finance_method().getLongName().concat("/" + piggyFinaAmt));

					BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDraw);

					// 存钱罐子户存入
					DpDemandSaveIn demandSaveIn = BizUtil.getInstance(DpDemandSaveIn.class);

					demandSaveIn.setAcct_no(tabPiggyBank.getPiggy_acct());// 账号
					demandSaveIn.setCcy_code(tabPiggyBank.getCcy_code());// 币种
					demandSaveIn.setProd_id(tabPiggyBank.getPiggy_product());
					demandSaveIn.setOpp_acct_no(tabPiggyBank.getAcct_no());
					demandSaveIn.setOpp_acct_ccy(tabPiggyBank.getCcy_code());
					demandSaveIn.setOpp_sub_acct_seq(settleSubAcctSeq);
					demandSaveIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
					demandSaveIn.setTrxn_opp_amt(piggyFinaAmt);
					demandSaveIn.setTrxn_amt(piggyFinaAmt);// 交易金额
					demandSaveIn.setCash_trxn_ind(E_CASHTRXN.TRXN);// 现转标志
					demandSaveIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_AUTO_GRAB_TO_PIGGY"));
					demandSaveIn.setCustomer_remark(remark);
					demandSaveIn.setTrxn_remark(tabPiggyBank.getPiggy_finance_method().getLongName().concat("/" + piggyFinaAmt));

					/** 活期存入 */
					DpDemandSaveOut saveOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandSave(demandSaveIn);
					piggySubAccount.setAcct_bal(saveOut.getAcct_bal());

					// 推送站内信
					autoGrabPushInApp("SMS0002", piggySubAccount, settleSubAccount, piggyFinaAmt, financeMethod, remark);

					bizlog.method(" Auto grab execute end <<<<<<<<<<<<<<<<");
				}
				catch (Exception e) {

					DaoUtil.rollbackTransaction();

					// 推送站内信
					autoGrabPushInApp("SMS0003", piggySubAccount, settleSubAccount, piggyFinaAmt, financeMethod, remark);
					bizlog.error(" Auto grab execute end :[%s]", e, e.getMessage());
				}

				return null;
			}
		});

		bizlog.method(" DpPiggyBank.acctConsTriggerPiggyFina end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Administrator
	 *         <p>
	 *         <li>2019年1月8日-下午3:12:33</li>
	 *         <li>功能说明：存钱罐转账推送站内信</li>
	 *         </p>
	 * @param templateNo
	 *            站内信模版编号
	 * @param piggySubAccount
	 *            存钱罐子户
	 * @param settleSubAccount
	 *            结算子户
	 * @param piggyFinaAmt
	 *            理财金额
	 * @param financeMethod
	 *            理财方式
	 * @param remark
	 *            交易备注
	 */
	private static void autoGrabPushInApp(String templateNo, DpaSubAccount piggySubAccount, DpaSubAccount settleSubAccount, BigDecimal piggyFinaAmt,
			E_PIGGYFINANCEMETHOD financeMethod, String remark) {

		Map<String, Object> mapObj = new HashMap<String, Object>();

		mapObj.put("piggy_prod_name", DpProductFactoryApi.getProdBaseInfo(piggySubAccount.getProd_id()).getProd_name());
		mapObj.put("saving_prod_name", DpProductFactoryApi.getProdBaseInfo(settleSubAccount.getProd_id()).getProd_name());
		mapObj.put("auto_grap_amt", piggyFinaAmt);
		mapObj.put("piggy_finance_method", financeMethod.getLongName());
		mapObj.put("piggy_acct_balance", piggySubAccount.getAcct_bal());
		mapObj.put("customer_remark", remark);

		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObj);

		ApSms.sendSmsByTemplateNo(templateNo);
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年11月9日-下午3:34:27</li>
	 *         <li>功能说明：信用卡消费触发存钱罐理财</li>
	 *         </p>
	 * @param cplIn
	 */
	public static void creditCardConsTriggerPiggyFina(DpCreditCardConsTriggerPiggyFinaIn cplIn) {
		bizlog.method(" DpPiggyBank.creditCardConsTriggerPiggyFina begin >>>>>>>>>>>>>>>>");
		// 为空检查
		BizUtil.fieldNotNull(cplIn.getTrxn_ccy(), SysDict.A.trxn_ccy.getId(), SysDict.A.trxn_ccy.getLongName());
		BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

		DpbPiggyBank tabPiggyBank = null;

		if (CommUtil.isNull(cplIn.getCust_no()) && CommUtil.isNull(cplIn.getDoc_no())) {
			throw DpErr.Dp.E0462();
		}

		if (CommUtil.isNotNull(cplIn.getCust_no())) {

			tabPiggyBank = SqlDpInstructDao.selPiggyBankByCust(BizUtil.getTrxRunEnvs().getBusi_org_id(), cplIn.getCust_no(), false);
		}
		else {

			DpCustSimpleInfo cfCustInfo = DpCustomerIobus.getCustSimpleInfo(cplIn.getDoc_type(), cplIn.getDoc_no());

			if (CommUtil.isNull(cfCustInfo)) {
				throw DpErr.Dp.E0094();
			}

			tabPiggyBank = SqlDpInstructDao.selPiggyBankByCust(BizUtil.getTrxRunEnvs().getBusi_org_id(), cfCustInfo.getCust_no(), false);
		}

		if (CommUtil.isNull(tabPiggyBank)) {
			return;
		}

		// 结算户消费触发存钱罐理财
		acctConsTriggerPiggyFina(tabPiggyBank, cplIn.getTrxn_amt(), cplIn.getCustomer_remark());

		bizlog.method(" DpPiggyBank.creditCardConsTriggerPiggyFina end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2018年12月19日-上午11:33:24</li>
	 *         <li>功能说明：结算户触发存钱罐理财检查</li>
	 *         </p>
	 * @param checkIn
	 *            输入信息
	 */
	public static void acctConsTriggerPiggyFinaCheck(DpAacctTriggerFinaCheckIn checkIn) {
		bizlog.method(" DpPiggyBank.acctConsTriggerPiggyFinaCheck begin >>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(checkIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(checkIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(checkIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

		if (CommUtil.equals(checkIn.getTrxn_amt(), BigDecimal.ZERO)) {
			return;
		}

		// 日终渠道的交易不处理存钱罐理财，文件批量还是要处理的
		if (CommUtil.equals(BizUtil.getTrxRunEnvs().getChannel_id(), ApConst.SYSTEM_BATCH) || ApChannelApi.isCounter(BizUtil.getTrxRunEnvs().getChannel_id())) {
			return;
		}

		DpbPiggyBank tabPiggyBank = DpbPiggyBankDao.selectFirst_odb2(checkIn.getAcct_no(), checkIn.getCcy_code(), E_STATUS.VALID, false);

		if (CommUtil.isNull(tabPiggyBank)) {
			return;
		}

		// 先确定对手方账户类型
		if (checkIn.getOpp_acct_route() == null && CommUtil.isNotNull(checkIn.getOpp_acct_no())) {

			checkIn.setOpp_acct_route(DpInsideAccountIobus.getAccountRouteType(checkIn.getOpp_acct_no()));
		}

		if (checkIn.getOpp_acct_route() == E_ACCOUTANALY.DEPOSIT && CommUtil.isNotNull(checkIn.getOpp_acct_no())) {

			DpaAccount piggyAcct = DpToolsApi.locateSingleAccount(checkIn.getOpp_acct_no(), null, false);

			// 同客户转账不触发存钱罐理财
			if (CommUtil.equals(piggyAcct.getCust_no(), checkIn.getCust_no())) {
				return;
			}
		}

		acctConsTriggerPiggyFina(tabPiggyBank, checkIn.getTrxn_amt(), checkIn.getCustomer_remark());

		bizlog.method(" DpPiggyBank.acctConsTriggerPiggyFinaCheck end <<<<<<<<<<<<<<<<");
	}

}
