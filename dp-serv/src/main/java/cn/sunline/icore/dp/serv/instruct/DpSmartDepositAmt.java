package cn.sunline.icore.dp.serv.instruct;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
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
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfSave;
import cn.sunline.icore.dp.base.tables.TabDpTimeSlipBase.DpaTimeSlip;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_SPECPRODTYPE;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.fundpool.DpDrawProtectAgree;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInstructDao;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInterestDao;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.servicetype.SrvDpOpenAccount;
import cn.sunline.icore.dp.serv.servicetype.SrvDpTimeAccounting;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDeposit;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDepositDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtect;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtectDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpMntWithdrawProtectAgreeIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositAgmIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositAgmOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositFechiInfo;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositProtectQueryIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositProtectQueryOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositSignIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpSmartDepositSignOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpWithdrawProtectAgreeIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpWithdrawProtectAgreeOut;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_PROTECTTYPE;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

public class DpSmartDepositAmt {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpSmartDepositAmt.class);

	/**
	 * @Author cxing
	 *         <p>
	 *         <li>2017年7月6日-上午10:12:54</li>
	 *         <li>功能说明：智能存款协议维护主程序</li>
	 *         </p>
	 * @param dpSmartDepositIn
	 */
	public static DpSmartDepositOut maintenSmartDepositAnt(DpSmartDepositIn dpSmartDepositIn) {

		bizlog.method("DpSmartDepositAnt.maintenSmartDepositAnt begin >>>>>>>>>>>>>");
		bizlog.debug("dpSmartDepositIn=[%s]", dpSmartDepositIn);

		// 验证输入字段合法性
	    validInputData(dpSmartDepositIn);
				
		// 定位账号
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(dpSmartDepositIn.getAcct_no(), null, false);

		// 根据协议号获取账号
		DpbSmartDeposit smartAgree = DpbSmartDepositDao.selectOne_odb1(acctInfo.getAcct_no(), dpSmartDepositIn.getAgree_no(), false);

		if (smartAgree == null) {
			throw ApPubErr.APPUB.E0024(OdbFactory.getTable(DpbSmartDeposit.class).getLongname(), SysDict.A.acct_no.getLongName(), acctInfo.getAcct_no(),
					SysDict.A.agree_no.getLongName(), dpSmartDepositIn.getAgree_no());
		}

		if (smartAgree.getAgree_status() == E_STATUS.INVALID) {
			throw DpBase.E0296(dpSmartDepositIn.getAgree_no());
		}

		// 数据版本号不一致， 抛出异常
		if (CommUtil.compare(smartAgree.getData_version(), dpSmartDepositIn.getData_version()) != 0) {
			throw ApPubErr.APPUB.E0018(DpbSmartDeposit.class.getName());
		}

		// 检查交易密码
		if (dpSmartDepositIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(dpSmartDepositIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 检查解约或维护
		if (E_YESORNO.YES == dpSmartDepositIn.getCancle_agree_ind()) {
			// 解约
			cancelSmartDepositAgree(dpSmartDepositIn, smartAgree);
		}
		else {
			// 更新
			modifyDepositAgreeTerm(dpSmartDepositIn, smartAgree);
		}

		// 初始化输出
		DpSmartDepositOut cplOut = BizUtil.getInstance(DpSmartDepositOut.class);

		cplOut.setAgree_no(smartAgree.getAgree_no());// 协议号
		cplOut.setAcct_no(smartAgree.getAcct_no());// 账号
		cplOut.setAcct_type(acctInfo.getAcct_type());// 账号类型
		cplOut.setAcct_name(acctInfo.getAcct_name());// 账户名称
		cplOut.setCcy_code(smartAgree.getCcy_code());// 货币代号
		cplOut.setSmart_acct_no(smartAgree.getSmart_acct_no());// 智能存款账号
		cplOut.setSmart_sub_acct_seq(smartAgree.getSmart_sub_acct_seq());// 智能存款子账号
		cplOut.setEffect_date(smartAgree.getEffect_date());
		cplOut.setExpiry_date(smartAgree.getExpiry_date());// 失效日期
		cplOut.setMin_turn_out_amt(smartAgree.getMin_turn_out_amt());// 最小转出金额
		cplOut.setDemand_remain_bal(smartAgree.getDemand_remain_bal());// 活期留存余额
		cplOut.setMultiple_amt(smartAgree.getMultiple_amt());// 倍增金额
		cplOut.setBreak_authority_ind(smartAgree.getBreak_authority_ind());
		cplOut.setAuto_placement_ind(smartAgree.getAuto_placement_ind());
		cplOut.setAuto_break_ind(smartAgree.getAuto_break_ind());

		smartAgree = DpbSmartDepositDao.selectOne_odb1(acctInfo.getAcct_no(), dpSmartDepositIn.getAgree_no(), false);

		cplOut.setData_version(smartAgree.getData_version());

		bizlog.debug("cplOut=[%s]", cplOut);

		bizlog.method("DpSmartDepositAnt.maintenSmartDepositAnt end <<<<<<<<<<<<");
		return cplOut;
	}
	
	/**
	 * @author lijiawei
	 *         <p>
	 *         <li>2019年12月31日-下午15:43:54</li>
	 *         <li>功能说明：智能存款协议输入参数合法性检查</li>
	 *         </p>
	 * @param  dpSmartDepositIn
	 * 
	 * 
	 */
	private static void validInputData(DpSmartDepositIn dpSmartDepositIn){
		bizlog.method("DpSmartDepositAnt.validInputData begin >>>>>>>>>>>>>");

		// 检查传入数据
		BizUtil.fieldNotNull(dpSmartDepositIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(dpSmartDepositIn.getAgree_no(), SysDict.A.agree_no.getId(), SysDict.A.agree_no.getLongName());
		BizUtil.fieldNotNull(dpSmartDepositIn.getCancle_agree_ind(), DpBaseDict.A.cancle_agree_ind.getId(), DpBaseDict.A.cancle_agree_ind.getLongName());
		BizUtil.fieldNotNull(dpSmartDepositIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());
		
		//判断最小转出金额格式，为负数会抛出异常
		if (CommUtil.isNotNull(dpSmartDepositIn.getMin_turn_out_amt()) && CommUtil.compare(dpSmartDepositIn.getMin_turn_out_amt(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.min_turn_out_amt.getLongName());
		}
		
		//判断活期留存余额格式，为负数会抛出异常
		if (CommUtil.isNotNull(dpSmartDepositIn.getDemand_remain_bal()) && CommUtil.compare(dpSmartDepositIn.getDemand_remain_bal(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.demand_remain_bal.getLongName());
		}
		
		//判断倍增金额格式，为负数会抛出异常
		if (CommUtil.isNotNull(dpSmartDepositIn.getMultiple_amt()) && CommUtil.compare(dpSmartDepositIn.getMultiple_amt(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.multiple_amt.getLongName());
		}
		
		//判断最大转出金额格式，为负数会抛出异常
		if (CommUtil.isNotNull(dpSmartDepositIn.getMax_turn_out_amt()) && CommUtil.compare(dpSmartDepositIn.getMax_turn_out_amt(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.max_turn_out_amt.getLongName());
		}
		
		//判断生效日期是否小于失效日期，否则抛出异常
		if (CommUtil.compare(dpSmartDepositIn.getEffect_date(),dpSmartDepositIn.getExpiry_date()) > 0){
			throw ApPubErr.APPUB.E0021(dpSmartDepositIn.getEffect_date(),dpSmartDepositIn.getExpiry_date());
		}
		
		// 校验失效日期
		if (CommUtil.compare(dpSmartDepositIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {
			throw DpBase.E0294(dpSmartDepositIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}
		
		//检验日期格式	
		if (CommUtil.isNotNull(dpSmartDepositIn.getExpiry_date()) && !BizUtil.isDateString(dpSmartDepositIn.getExpiry_date())){
			throw ApPubErr.APPUB.E0011(dpSmartDepositIn.getExpiry_date());
		}
				
		if (CommUtil.isNotNull(dpSmartDepositIn.getEffect_date()) && !BizUtil.isDateString(dpSmartDepositIn.getEffect_date())){
			throw ApPubErr.APPUB.E0011(dpSmartDepositIn.getEffect_date());
		}
		
		bizlog.method("DpSmartDepositAnt.validInputData end <<<<<<<<<<<<");
	}

	/**
	 * @Author cxing
	 *         <p>
	 *         <li>2017年7月6日-上午10:12:54</li>
	 *         <li>功能说明：智能存款协议解约</li>
	 *         </p>
	 * @param dpSmartDepositIn
	 *            智能存款协议解约输入接口
	 * @param depositEntity
	 *            智能存款协议表
	 */
	private static void cancelSmartDepositAgree(DpSmartDepositIn cplIn, DpbSmartDeposit smartAgree) {
		bizlog.method("DpSmartDepositAnt.cancelSmartDepositAgree begin >>>>>>>>>>>>>");

		// 复制一份,做审计用
		DpbSmartDeposit oldAgreeInfo = BizUtil.clone(DpbSmartDeposit.class, smartAgree);

		// 数据版本号不一致， 抛出异常
		if (CommUtil.compare(smartAgree.getData_version(), cplIn.getData_version()) != 0) {
			throw ApPubErr.APPUB.E0018(DpbSmartDeposit.class.getName());
		}

		// 1.关闭协议
		smartAgree.setAgree_status(E_STATUS.INVALID);// 协议状态
		smartAgree.setCancel_date(BizUtil.getTrxRunEnvs().getTrxn_date());// 解约日期
		smartAgree.setCancel_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());// 解约流水

		DpbSmartDepositDao.updateOne_odb1(smartAgree);

		// 登记审计
		ApDataAuditApi.regLogOnUpdateBusiness(oldAgreeInfo, smartAgree);

		// 2.销定期子户
		DpTimeDrawIn timeDrawIn = BizUtil.getInstance(DpTimeDrawIn.class);

		timeDrawIn.setCard_no(""); // card no
		timeDrawIn.setAcct_no(smartAgree.getSmart_acct_no());
		timeDrawIn.setSub_acct_seq(smartAgree.getSmart_sub_acct_seq());
		timeDrawIn.setCcy_code(smartAgree.getCcy_code());
		timeDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
		timeDrawIn.setCheck_password_ind(E_YESORNO.NO);
		timeDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.CLOSE);
		timeDrawIn.setSummary_code(ApSystemParmApi.getSummaryCode("CLOSE_SUB_ACCOUNT"));
		timeDrawIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
		timeDrawIn.setOpp_acct_no(smartAgree.getAcct_no());
		timeDrawIn.setOpp_acct_ccy(smartAgree.getCcy_code());
		timeDrawIn.setOpp_sub_acct_seq(smartAgree.getSub_acct_seq());
		timeDrawIn.setCustomer_remark(cplIn.getRemark());

		DpTimeDrawOut timeAcctingOut = BizUtil.getInstance(SrvDpTimeAccounting.class).timeDraw(timeDrawIn);

		// 3.资金转入活期户
		DpDemandSaveIn demandSaveIn = BizUtil.getInstance(DpDemandSaveIn.class);

		demandSaveIn.setAcct_no(smartAgree.getAcct_no());
		demandSaveIn.setCcy_code(smartAgree.getCcy_code());
		demandSaveIn.setBack_value_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		demandSaveIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
		demandSaveIn.setOpen_acct_save_ind(E_YESORNO.NO);
		demandSaveIn.setTrxn_amt(timeAcctingOut.getPaying_amt());
		demandSaveIn.setSummary_code(ApSystemParmApi.getSummaryCode("CLOSE_SUB_ACCOUNT"));
		demandSaveIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
		demandSaveIn.setOpp_acct_ccy(smartAgree.getCcy_code());
		demandSaveIn.setOpp_acct_no(smartAgree.getSmart_acct_no());
		demandSaveIn.setOpp_sub_acct_seq(smartAgree.getSmart_sub_acct_seq());
		demandSaveIn.setCustomer_remark(cplIn.getRemark());

		BizUtil.getInstance(SrvDpDemandAccounting.class).demandSave(demandSaveIn);

		// 4.支取保护解约
		if (CommUtil.isNotNull(smartAgree.getRelation_agree_no())) {

			DpbWithdrawlProtect protectInfo = DpbWithdrawlProtectDao.selectOne_odb1(smartAgree.getAcct_no(), smartAgree.getRelation_agree_no(), true);

			DpMntWithdrawProtectAgreeIn drawProtAgreeMntIn = BizUtil.getInstance(DpMntWithdrawProtectAgreeIn.class);

			drawProtAgreeMntIn.setAcct_no(smartAgree.getAcct_no());
			drawProtAgreeMntIn.setAgree_no(smartAgree.getRelation_agree_no());
			drawProtAgreeMntIn.setCheck_password_ind(E_YESORNO.NO);
			drawProtAgreeMntIn.setCancle_agree_ind(E_YESORNO.YES);
			drawProtAgreeMntIn.setData_version(protectInfo.getData_version());

			DpDrawProtectAgree.withdrawProtectAgreeMnt(drawProtAgreeMntIn);
		}

		bizlog.method("DpSmartDepositAnt.cancelSmartDepositAgree end <<<<<<<<<<<<");
	}

	/**
	 * @Author cxing
	 *         <p>
	 *         <li>2017年7月6日-上午10:12:54</li>
	 *         <li>功能说明：智能存款协议更新</li>
	 *         </p>
	 * @param dpSmartDepositIn
	 *            智能存款协议解约输入接口
	 * @param depositEntity
	 *            智能存款协议表
	 */
	private static void modifyDepositAgreeTerm(DpSmartDepositIn cplIn, DpbSmartDeposit smartAgree) {
		bizlog.method("DpSmartDepositAnt.modifyDepositAgreeTerm begin >>>>>>>>>>>>>");

		// 复制一份,做审计用
		DpbSmartDeposit oldAgreeInfo = BizUtil.clone(DpbSmartDeposit.class, smartAgree);

		// 数据版本号不一致， 抛出异常
		if (CommUtil.compare(smartAgree.getData_version(), cplIn.getData_version()) != 0) {
			throw ApPubErr.APPUB.E0018(DpbSmartDeposit.class.getName());
		}

		// 更新智能存款协议数据
		if (CommUtil.isNotNull(cplIn.getEffect_date())) {
			smartAgree.setEffect_date(cplIn.getEffect_date()); // 生效日期
		}
		if (CommUtil.isNotNull(cplIn.getExpiry_date())) {
			smartAgree.setExpiry_date(cplIn.getExpiry_date());// 失效日期
		}
		if (CommUtil.isNotNull(cplIn.getMin_turn_out_amt())) {
			smartAgree.setMin_turn_out_amt(cplIn.getMin_turn_out_amt());// 最小转出金额
		}
		if (CommUtil.isNotNull(cplIn.getDemand_remain_bal())) {
			smartAgree.setDemand_remain_bal(cplIn.getDemand_remain_bal());// 活期留存余额
		}
		if (CommUtil.isNotNull(cplIn.getMultiple_amt())) {
			smartAgree.setMultiple_amt(cplIn.getMultiple_amt());// 倍增金额
		}
		if (CommUtil.isNotNull(cplIn.getMax_turn_out_amt())) {
			smartAgree.setMax_turn_out_amt(cplIn.getMax_turn_out_amt()); // 最大转出金额
		}
		if (CommUtil.isNotNull(cplIn.getAuto_placement_ind())) {
			smartAgree.setAuto_placement_ind(cplIn.getAuto_placement_ind()); // 自动建卡启用标识
		}
		if (CommUtil.isNotNull(cplIn.getAuto_break_ind())) {
			smartAgree.setAuto_break_ind(cplIn.getAuto_break_ind()); // 自动保护启用标识
		}
		if (CommUtil.isNotNull(cplIn.getBreak_authority_ind())) {
			smartAgree.setBreak_authority_ind(cplIn.getBreak_authority_ind()); // 保护授权标识
		}

		if (cplIn.getAuto_backfill_ind() != null) {
			smartAgree.setAuto_backfill_ind(cplIn.getAuto_backfill_ind());
		}

		// 登记审计
		int iCount = ApDataAuditApi.regLogOnUpdateParameter(oldAgreeInfo, smartAgree);

		// 数据未发生变化，则报错
		if (iCount == 0) {

			throw DpErr.Dp.E0274(OdbFactory.getTable(DpbSmartDeposit.class).getLongname(), smartAgree.getExpiry_date(), smartAgree.getMin_turn_out_amt(),
					smartAgree.getDemand_remain_bal(), smartAgree.getMultiple_amt());
		}

		// 需补签关联保护协议
		if (cplIn.getAuto_break_ind() == E_YESORNO.YES && CommUtil.isNull(smartAgree.getRelation_agree_no())) {

			DpWithdrawProtectAgreeIn drawProtAgreeIn = BizUtil.getInstance(DpWithdrawProtectAgreeIn.class);

			drawProtAgreeIn.setAcct_no(smartAgree.getAcct_no());
			drawProtAgreeIn.setCcy_code(smartAgree.getCcy_code());
			drawProtAgreeIn.setCheck_password_ind(E_YESORNO.NO);
			drawProtAgreeIn.setProtect_type(E_PROTECTTYPE.INTELLIGENT);
			drawProtAgreeIn.setProtect_acct_no(smartAgree.getSmart_acct_no());
			drawProtAgreeIn.setProtect_ccy(smartAgree.getCcy_code());
			drawProtAgreeIn.setProtect_sub_acct_seq(smartAgree.getSmart_sub_acct_seq());
			drawProtAgreeIn.setExpiry_date(smartAgree.getExpiry_date());
			drawProtAgreeIn.setEffect_date(smartAgree.getEffect_date());

			DpWithdrawProtectAgreeOut drawProtAgreeOut = DpDrawProtectAgree.withdrawProtectAgree(drawProtAgreeIn);

			smartAgree.setRelation_agree_no(drawProtAgreeOut.getAgree_no());
		}
		else if (CommUtil.isNotNull(smartAgree.getRelation_agree_no()) && smartAgree.getAuto_break_ind() != oldAgreeInfo.getAuto_break_ind()) {

			DpbWithdrawlProtect protectInfo = DpbWithdrawlProtectDao.selectOne_odb1(smartAgree.getAcct_no(), smartAgree.getRelation_agree_no(), true);

			DpMntWithdrawProtectAgreeIn drawProtAgreeMntIn = BizUtil.getInstance(DpMntWithdrawProtectAgreeIn.class);

			drawProtAgreeMntIn.setAcct_no(smartAgree.getAcct_no());
			drawProtAgreeMntIn.setAgree_no(smartAgree.getRelation_agree_no());
			drawProtAgreeMntIn.setCheck_password_ind(E_YESORNO.NO);
			drawProtAgreeMntIn.setCancle_agree_ind(E_YESORNO.NO);
			drawProtAgreeMntIn.setStop_use_ind(smartAgree.getAuto_break_ind() == E_YESORNO.NO ? E_YESORNO.YES : E_YESORNO.NO);
			drawProtAgreeMntIn.setData_version(protectInfo.getData_version());

			DpDrawProtectAgree.withdrawProtectAgreeMnt(drawProtAgreeMntIn);
		}

		DpbSmartDepositDao.updateOne_odb1(smartAgree);

		bizlog.method("DpSmartDepositAnt.modifyDepositAgreeTerm end <<<<<<<<<<<<");
	}

	/**
	 * @Author shenhao
	 *         <p>
	 *         <li>2017年7月11日-上午10:12:54</li>
	 *         <li>功能说明：智能存款协议查询</li>
	 *         </p>
	 * @param DpSmartDepositAgmIn
	 *            智能存款协议查询参数入口
	 * @param DpSmartDepositAgmOut
	 *            智能存款协议查询出口
	 */
	public static Options<DpSmartDepositAgmOut> queryListSmartDepositAgm(DpSmartDepositAgmIn dpSmartDepositAgmIn) {
		bizlog.method(" DpSmartDepositAnt.queryListSmartDepositAgm begin >>>>>>>>>>>>>>>>");
		bizlog.debug("qsdIn=[%s]", dpSmartDepositAgmIn);

		BizUtil.fieldNotNull(dpSmartDepositAgmIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(dpSmartDepositAgmIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 初始化输出类型
		Options<DpSmartDepositAgmOut> options = new DefaultOptions<DpSmartDepositAgmOut>();
		// 获取公共运行期变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String orgId = runEnvs.getBusi_org_id();// 取得法人代码
		List<DpSmartDepositAgmOut> temp = SqlDpInstructDao.selSmartDeposit(dpSmartDepositAgmIn.getAcct_no(), dpSmartDepositAgmIn.getAcct_type(), dpSmartDepositAgmIn.getCcy_code(),
				dpSmartDepositAgmIn.getAgree_status(), orgId, dpSmartDepositAgmIn.getSub_acct_seq(), false);

		options.setValues(temp);
		bizlog.method(" DpSmartDepositAnt.queryListSmartDepositAgm end <<<<<<<<<<<<<<<<");
		return options;

	}

	/**
	 * @Author LIAOJC
	 *         <p>
	 *         <li>2017年7月3日-下午4:00:35</li>
	 *         <li>功能说明：智能存款签约</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpSmartDepositSignOut smartDepositSign(DpSmartDepositSignIn cplIn) {

		bizlog.method(" DpSmartDepositAmt.smartDepositSign begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 1.校验相关输入
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getSmart_prod_id(), DpDict.A.smart_prod_id.getId(), DpDict.A.smart_prod_id.getLongName());

		// 2.主要逻辑处理
		// 2.1 封装子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 2.2 查询账户信息
		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), true);

		// 2.3 查询子账户信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 2.4 检查签约要素
		checkSmartDepositSignData(cplIn, acctInfo, subAccount);

		// 3.开立子账户
		DpAddSubAccountIn addSubAccountIn = BizUtil.getInstance(DpAddSubAccountIn.class);

		addSubAccountIn.setAcct_no(CommUtil.nvl(cplIn.getSmart_acct_no(), cplIn.getAcct_no()));
		addSubAccountIn.setProd_id(cplIn.getSmart_prod_id());
		addSubAccountIn.setTrxn_password(cplIn.getTrxn_password());
		addSubAccountIn.setCcy_code(cplIn.getCcy_code());
		addSubAccountIn.setTrxn_amt(BigDecimal.ZERO);
		addSubAccountIn.setIncome_inst_acct(CommUtil.nvl(cplIn.getSmart_acct_no(), subAccount.getAcct_no()));
		addSubAccountIn.setIncome_inst_ccy(CommUtil.nvl(cplIn.getCcy_code(), subAccount.getCcy_code()));

		DpAddSubAccountOut openSubOut = BizUtil.getInstance(SrvDpOpenAccount.class).addSubAccount(addSubAccountIn);

		// 4.登记相关信息
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		String relateAgreeNo = ""; // 关联保护协议编号

		// 4.1 支取保护协议签订
		if (cplIn.getAuto_break_ind() != E_YESORNO.NO) {

			DpWithdrawProtectAgreeIn drawProtAgreeIn = BizUtil.getInstance(DpWithdrawProtectAgreeIn.class);

			drawProtAgreeIn.setAcct_no(acctAccessOut.getAcct_no());
			drawProtAgreeIn.setCcy_code(acctAccessOut.getCcy_code());
			drawProtAgreeIn.setCheck_password_ind(E_YESORNO.NO);
			drawProtAgreeIn.setProtect_type(E_PROTECTTYPE.INTELLIGENT);
			drawProtAgreeIn.setProtect_acct_no(openSubOut.getAcct_no());
			drawProtAgreeIn.setProtect_ccy(cplIn.getCcy_code());
			drawProtAgreeIn.setProtect_sub_acct_seq(openSubOut.getSub_acct_seq());
			drawProtAgreeIn.setExpiry_date(cplIn.getExpiry_date());
			drawProtAgreeIn.setEffect_date(cplIn.getEffect_date());

			DpWithdrawProtectAgreeOut drawProtAgreeOut = DpDrawProtectAgree.withdrawProtectAgree(drawProtAgreeIn);

			relateAgreeNo = drawProtAgreeOut.getAgree_no();
		}

		// 智能存款卡片创建参考日期, 参考日期可以是四位日期， 也可以是END这种特殊字符串
		String refDate = ApBusinessParmApi.exists("SMART_SLIP_CREATE_REF_DATE") ? ApBusinessParmApi.getValue("SMART_SLIP_CREATE_REF_DATE") : BizUtil.getTrxRunEnvs().getTrxn_date();
		// 智能存款卡片创建周期，格式如1M, 3M, 1W等
		String createCycle = ApBusinessParmApi.getValue("SMART_SLIP_CREATE_CYCLE");

		// 4.2 登记智能存款协议
		DpbSmartDeposit smartInfo = BizUtil.getInstance(DpbSmartDeposit.class);

		smartInfo.setAgree_no(ApSeqApi.genSeq("AGREE_NO"));
		smartInfo.setCust_no(openSubOut.getCust_no());
		smartInfo.setAcct_no(subAccount.getAcct_no());
		smartInfo.setCcy_code(subAccount.getCcy_code());
		smartInfo.setSub_acct_seq(acctAccessOut.getSub_acct_seq());
		smartInfo.setSmart_acct_no(openSubOut.getAcct_no());
		smartInfo.setSmart_sub_acct_seq(openSubOut.getSub_acct_seq());
		smartInfo.setMin_turn_out_amt(CommUtil.nvl(cplIn.getMin_turn_out_amt(), BigDecimal.ZERO));
		smartInfo.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), BigDecimal.ZERO));
		smartInfo.setMultiple_amt(CommUtil.nvl(cplIn.getMultiple_amt(), BigDecimal.ZERO));
		smartInfo.setMax_turn_out_amt(CommUtil.nvl(cplIn.getMax_turn_out_amt(), BigDecimal.ZERO));
		smartInfo.setAuto_placement_ind(CommUtil.nvl(cplIn.getAuto_placement_ind(), E_YESORNO.YES));
		smartInfo.setAuto_break_ind(CommUtil.nvl(cplIn.getAuto_break_ind(), E_YESORNO.YES));
		smartInfo.setRelation_agree_no(relateAgreeNo);
		smartInfo.setBreak_authority_ind(CommUtil.nvl(cplIn.getBreak_authority_ind(), E_YESORNO.NO));
		smartInfo.setAuto_backfill_ind(CommUtil.nvl(cplIn.getAuto_backfill_ind(), E_YESORNO.NO));
		smartInfo.setNext_exec_date(BizUtil.calcDateByRefernce(refDate, createCycle));
		smartInfo.setAgree_status(E_STATUS.VALID); // agree status
		smartInfo.setSign_date(runEnvs.getTrxn_date()); // sign date
		smartInfo.setSign_seq(runEnvs.getTrxn_seq()); // sign seq
		smartInfo.setEffect_date(cplIn.getEffect_date());
		smartInfo.setExpiry_date(cplIn.getExpiry_date()); // expiry
		smartInfo.setHash_value(subAccount.getHash_value());

		DpbSmartDepositDao.insert(smartInfo);

		// 5.初始化输出
		DpSmartDepositSignOut cplOut = BizUtil.getInstance(DpSmartDepositSignOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAccount.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(smartInfo.getAcct_no());
		cplOut.setAcct_type(cplIn.getAcct_type());
		cplOut.setAcct_name(subAccount.getSub_acct_name());
		cplOut.setCcy_code(smartInfo.getCcy_code());
		cplOut.setSmart_sub_acct_seq(smartInfo.getSmart_sub_acct_seq());
		cplOut.setSmart_acct_no(cplIn.getSmart_acct_no());
		cplOut.setSmart_prod_id(cplIn.getSmart_prod_id());
		cplOut.setAgree_no(smartInfo.getAgree_no());
		cplOut.setEffect_date(smartInfo.getEffect_date());
		cplOut.setExpiry_date(smartInfo.getExpiry_date());
		cplOut.setMin_turn_out_amt(smartInfo.getMin_turn_out_amt());
		cplOut.setDemand_remain_bal(smartInfo.getDemand_remain_bal());
		cplOut.setMultiple_amt(smartInfo.getMultiple_amt());
		cplOut.setBreak_authority_ind(smartInfo.getBreak_authority_ind());
		cplOut.setAuto_placement_ind(smartInfo.getAuto_placement_ind());
		cplOut.setAuto_break_ind(smartInfo.getAuto_break_ind());

		bizlog.debug("cplIn=[%s]", cplOut);
		bizlog.method(" DpSmartDepositAmt.smartDepositSign end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月3日-上午9:15:33</li>
	 *         <li>功能说明：智能存款签约相关数据检查</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void checkSmartDepositSignData(DpSmartDepositSignIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {
		bizlog.method(" DpSmartDepositAmt.checkSmartDepositSignData begin >>>>>>>>>>>>>>>>");

		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 同名检查
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(acctInfo.getAcct_name(), cplIn.getAcct_name())) {

			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctInfo.getAcct_name());
		}

		// 校验智能存款产品
		DpfBase smartProdInfo = DpProductFactoryApi.getProdBaseInfo(cplIn.getSmart_prod_id());

		DpfSave smartSaveInfo = DpProductFactoryApi.getProdSaveCtrl(cplIn.getSmart_prod_id(), cplIn.getCcy_code());

		if (smartProdInfo.getSpec_dept_type() != E_SPECPRODTYPE.SMART_TIME) {
			throw DpErr.Dp.E0348();
		}

		// 最小转出金额合法性检查
		if (CommUtil.isNotNull(cplIn.getMin_turn_out_amt()) && CommUtil.compare(cplIn.getMin_turn_out_amt(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.min_turn_out_amt.getLongName());
		}
		if (CommUtil.isNotNull(cplIn.getMin_turn_out_amt()) && !CommUtil.equals(cplIn.getMin_turn_out_amt(), BigDecimal.ZERO)) {
			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getMin_turn_out_amt());
		}
		else {
			cplIn.setMin_turn_out_amt(smartSaveInfo.getSigl_min_dept_amt());
		}

		// 不能低于产品最低限制
		if (!CommUtil.equals(smartSaveInfo.getSigl_min_dept_amt(), BigDecimal.ZERO) && CommUtil.compare(cplIn.getMin_turn_out_amt(), smartSaveInfo.getSigl_min_dept_amt()) < 0) {
			throw DpErr.Dp.E0495(cplIn.getMin_turn_out_amt(), smartSaveInfo.getSigl_min_dept_amt());
		}

		// 最大转出金额合法性检查
		if (CommUtil.isNotNull(cplIn.getMax_turn_out_amt()) && CommUtil.compare(cplIn.getMax_turn_out_amt(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.max_turn_out_amt.getLongName());
		}
		if (CommUtil.isNotNull(cplIn.getMax_turn_out_amt()) && !CommUtil.equals(cplIn.getMax_turn_out_amt(), BigDecimal.ZERO)) {
			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getMax_turn_out_amt());
		}
		else {
			cplIn.setMax_turn_out_amt(smartSaveInfo.getSigl_max_dept_amt());
		}

		// 不能超过产品最高限制
		if (!CommUtil.equals(smartSaveInfo.getSigl_max_dept_amt(), BigDecimal.ZERO) && CommUtil.compare(cplIn.getMax_turn_out_amt(), smartSaveInfo.getSigl_max_dept_amt()) > 0) {
			throw DpErr.Dp.E0468(cplIn.getMax_turn_out_amt(), smartSaveInfo.getSigl_max_dept_amt());
		}
		
		//最小转出金额不能大于最大转出金额，否则抛出异常
		if (CommUtil.compare(cplIn.getMin_turn_out_amt(), cplIn.getMax_turn_out_amt()) > 0){
			throw DpErr.Dp.E0328();
		}

		// 倍增金额合法性检查
		if (CommUtil.isNotNull(cplIn.getMultiple_amt()) && CommUtil.compare(cplIn.getMultiple_amt(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.multiple_amt.getLongName());
		}
		if (CommUtil.isNotNull(cplIn.getMultiple_amt())) {
			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getMultiple_amt());
		}
		else {
			cplIn.setMultiple_amt(BigDecimal.ZERO);
		}

		// 活期留存余额
		if (CommUtil.isNotNull(cplIn.getDemand_remain_bal()) && CommUtil.compare(cplIn.getDemand_remain_bal(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.demand_remain_bal.getLongName());
		}
		if (CommUtil.compare(cplIn.getDemand_remain_bal(), BigDecimal.ZERO) > 0) {
			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getDemand_remain_bal());
		}
		else {
			cplIn.setDemand_remain_bal(subAcct.getMin_remain_bal());
		}

		// 校验智能存款账号
		if (CommUtil.isNotNull(cplIn.getSmart_acct_no())) {

			DpaAccount samrtAcctInfo = DpToolsApi.locateSingleAccount(cplIn.getSmart_acct_no(), null, false);

			if (!CommUtil.equals(samrtAcctInfo.getCust_no(), acctInfo.getCust_no())) {

				throw DpErr.Dp.E0346(samrtAcctInfo.getCust_no(), acctInfo.getCust_no());
			}

			if (samrtAcctInfo.getCust_type() != acctInfo.getCust_type()) {

				throw DpErr.Dp.E0347(samrtAcctInfo.getCust_type(), acctInfo.getCust_type());
			}
		}

		if (CommUtil.isNull(cplIn.getEffect_date())) {
			cplIn.setEffect_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		if (CommUtil.isNull(cplIn.getExpiry_date())) {
			cplIn.setExpiry_date(ApConst.DEFAULT_MAX_DATE);
		}

		// 校验失效日期
		if (CommUtil.compare(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {
			throw DpBase.E0294(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}
		
		//检验日期格式
		if (CommUtil.isNotNull(cplIn.getExpiry_date()) && !BizUtil.isDateString(cplIn.getExpiry_date())){
			throw ApPubErr.APPUB.E0011(cplIn.getExpiry_date());
		}
		
		if (CommUtil.isNotNull(cplIn.getEffect_date()) && !BizUtil.isDateString(cplIn.getEffect_date())){
			throw ApPubErr.APPUB.E0011(cplIn.getEffect_date());
		}

		BizUtil.checkEffectDate(cplIn.getEffect_date(), cplIn.getExpiry_date());

		bizlog.debug("check after cplIn = [%s]", cplIn);
		bizlog.method(" DpSmartDepositAmt.checkSmartDepositSignData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author duanhb
	 *         <p>
	 *         <li>2018年12月15日-上午11:28:26</li>
	 *         <li>功能说明：智能存款保护明细查询</li>
	 *         </p>
	 * @param cplIn
	 *            服务输入信息
	 * @return
	 */
	public static DpSmartDepositProtectQueryOut qrySmartDepositProtect(DpSmartDepositProtectQueryIn cplIn) {
		bizlog.method(" DpSmartDepositAmt.qrySmartDepositProtect begin >>>>>>>>>>>>>>>>");
		bizlog.debug(" DpSmartDepositProtectQueryIn cplIn:[%s]", cplIn);

		// 账号必须输入
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 币种必须输入
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 保护金额必须输入
		BizUtil.fieldNotNull(cplIn.getProtect_amt(), DpDict.A.protect_amt.getId(), DpDict.A.protect_amt.getLongName());

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 获取有效的智能存款签约信息
		DpbSmartDeposit smartDeposit = DpbSmartDepositDao.selectFirst_odb3(cplIn.getAcct_no(), cplIn.getCcy_code(), E_STATUS.VALID, false);

		//判断智能存款签约信息是不是为空
		if (CommUtil.isNull(smartDeposit)){
			return null;
		}
		
		// 查询签约的智能存款定期子户信息
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(smartDeposit.getSmart_acct_no());
		acctAccessIn.setSub_acct_seq(smartDeposit.getSmart_sub_acct_seq());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.TIME);

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 获取智能存款卡片信息
		Page<DpaTimeSlip> ficheList = SqlDpInterestDao.selFicheInfoList(runEnvs.getBusi_org_id(), acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), E_ACCTSTATUS.NORMAL,
				runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(ficheList.getRecordCount());

		// 组织输出信息
		DpSmartDepositProtectQueryOut protectQueryOut = BizUtil.getInstance(DpSmartDepositProtectQueryOut.class);
		BigDecimal protectAmt = cplIn.getProtect_amt();
		protectQueryOut.setSmart_acct_no(smartDeposit.getSmart_acct_no());
		protectQueryOut.setSmart_sub_acct_seq(smartDeposit.getSmart_sub_acct_seq());
		protectQueryOut.setProtect_ccy(acctAccessOut.getCcy_code());
		protectQueryOut.setProtect_amt(protectAmt);
		protectQueryOut.setProtect_fiche_count(0l);

		// 非第一页开始查询,需去除前面页已提供保护的金额
		if (CommUtil.compare(runEnvs.getPage_start(), 0l) != 0) {

			Page<DpaTimeSlip> skipList = SqlDpInterestDao.selFicheInfoList(runEnvs.getBusi_org_id(), acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(),
					E_ACCTSTATUS.NORMAL, 0, runEnvs.getPage_start(), runEnvs.getTotal_count(), false);

			for (DpaTimeSlip DpaSlip : skipList.getRecords()) {

				if (CommUtil.compare(protectAmt, BigDecimal.ZERO) > 0) {

					protectAmt = protectAmt.subtract(DpaSlip.getAcct_bal());

					protectQueryOut.setProtect_fiche_count(protectQueryOut.getProtect_fiche_count() + 1);
				}
			}
		}

		// 处理当前页数据
		for (DpaTimeSlip DpaSlip : ficheList.getRecords()) {

			DpSmartDepositFechiInfo fechiInfo = BizUtil.getInstance(DpSmartDepositFechiInfo.class);

			fechiInfo.setFiche_no(DpaSlip.getFiche_no());
			fechiInfo.setStart_inst_date(DpaSlip.getStart_inst_date());
			fechiInfo.setEfft_inrt(DpaSlip.getEfft_inrt());
			fechiInfo.setAcct_bal(DpaSlip.getAcct_bal());
			fechiInfo.setNext_inrt_renew_date(DpaSlip.getNext_inrt_renew_date());
			fechiInfo.setProtect_amt(BigDecimal.ZERO);

			if (CommUtil.compare(protectAmt, BigDecimal.ZERO) > 0) {

				fechiInfo.setProtect_amt(CommUtil.compare(protectAmt, DpaSlip.getAcct_bal()) > 0 ? DpaSlip.getAcct_bal() : protectAmt);

				protectAmt = protectAmt.subtract(DpaSlip.getAcct_bal());
				protectQueryOut.setProtect_fiche_count(protectQueryOut.getProtect_fiche_count() + 1);
			}

			protectQueryOut.getList01().add(fechiInfo);
		}

		bizlog.debug(" DpSmartDepositProtectQueryOut cplOut:[%s]", protectQueryOut);
		bizlog.method(" DpSmartDepositAmt.qrySmartDepositProtect end <<<<<<<<<<<<<<<<");

		return protectQueryOut;
	}

}
