package cn.sunline.icore.dp.serv.batch;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAccountApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.servicetype.SrvDpOpenAccount;
import cn.sunline.icore.dp.serv.servicetype.SrvDpTimeAccounting;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbTimeFinance;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbTimeFinanceDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbTimeFinanceDetail;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpAddSubAccountOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeSaveOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_TRSFHANDLINGSTATUS;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpTimeFinance {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeFinance.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月16日-下午3:52:00</li>
	 *         <li>功能说明：定期理财单条处理</li>
	 *         </p>
	 * @param timeFinanceInfo
	 *            定期理财信息
	 * @param subAcctInfo
	 *            子账户信息
	 */
	public static void timeFinanceSingleHandle(DpbTimeFinance timeFinanceInfo, DpaSubAccount subAcctInfo) {
		bizlog.method(" DpTimeFinance.timeFinanceSingleHandle begin >>>>>>>>>>>>>>>>");

		// 检查是否满足执行时间
		String execTime = timeFinanceInfo.getExec_time();

		// 为空则取默认执行时间
		if (CommUtil.isNull(execTime)) {

			execTime = ApBusinessParmApi.getValue("AGREEMENT_TRSF_DEFAULT_TIME");
		}

		if (CommUtil.compare(execTime, BizUtil.getTrxRunEnvs().getComputer_time()) > 0) {

			return;
		}

		// 校验累计成功次数 是否超过约定次数
		if (CommUtil.isNotNull(timeFinanceInfo.getAgree_trsf_times()) && timeFinanceInfo.getAgree_trsf_times() != 0) {

			if (CommUtil.compare(timeFinanceInfo.getAgree_trsf_times(), timeFinanceInfo.getTotal_success_times()) < 0) {

				regTimeFinanceDetl(timeFinanceInfo, null, BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE);

				updFiledTimeFinanceInfo(timeFinanceInfo);

				return;
			}
		}

		// 校验累计成功次数 是否超过约定金额
		if (CommUtil.isNotNull(timeFinanceInfo.getAgree_trsf_amt()) && !CommUtil.equals(timeFinanceInfo.getAgree_trsf_amt(), BigDecimal.ZERO)) {

			if (CommUtil.compare(timeFinanceInfo.getAgree_trsf_amt(), timeFinanceInfo.getTotal_success_amt()) < 0) {

				regTimeFinanceDetl(timeFinanceInfo, null, BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE);

				updFiledTimeFinanceInfo(timeFinanceInfo);

				return;
			}
		}

		// 实际交易金额
		BigDecimal actTrxnAmt = BigDecimal.ZERO;

		// 获取实际交易金额
		if (CommUtil.compare(subAcctInfo.getAcct_bal(), timeFinanceInfo.getDemand_remain_bal().add(timeFinanceInfo.getMin_turn_out_amt())) < 0) {

			actTrxnAmt = BigDecimal.ZERO;
		}
		else {

			actTrxnAmt = getTimeFinanceAmt(timeFinanceInfo, subAcctInfo);

		}

		bizlog.debug("actTrxnAmt=[%s]", actTrxnAmt);

		if (CommUtil.equals(actTrxnAmt, BigDecimal.ZERO)) {

			return;
		}

		String summaryCode = "";
		DpTimeSaveOut timeSaveOut = BizUtil.getInstance(DpTimeSaveOut.class);

		try {
			// 1.先开立定期子户

			DpAddSubAccountIn openSubAcctIn = BizUtil.getInstance(DpAddSubAccountIn.class);

			openSubAcctIn.setAcct_no(timeFinanceInfo.getOpp_acct_no()); //
			openSubAcctIn.setProd_id(timeFinanceInfo.getNew_open_acct_prod()); //
			openSubAcctIn.setCash_trxn_ind(E_CASHTRXN.TRXN); //
			openSubAcctIn.setCcy_code(subAcctInfo.getCcy_code()); //
			openSubAcctIn.setTerm_code(timeFinanceInfo.getNew_open_acct_term());
			openSubAcctIn.setTrxn_amt(actTrxnAmt); // transaction amount
			openSubAcctIn.setSub_acct_branch(subAcctInfo.getSub_acct_branch());
			openSubAcctIn.setChannel_remark(""); // channel remark
			openSubAcctIn.setRemark(""); // remark

			DpAddSubAccountOut openAcctOut = BizUtil.getInstance(SrvDpOpenAccount.class).addSubAccount(openSubAcctIn);

			// 2.调用存入服务

			// 2.1取得摘要代码
			summaryCode = ApSystemParmApi.getSummaryCode("DEPT_BATCH_TIME_FINANCE");

			DpTimeSaveIn timeSaveIn = BizUtil.getInstance(DpTimeSaveIn.class);

			timeSaveIn.setAcct_no(openAcctOut.getAcct_no()); // account no
			timeSaveIn.setAcct_type(""); // account type
			timeSaveIn.setAcct_name(openAcctOut.getAcct_name()); //
			timeSaveIn.setCcy_code(openAcctOut.getCcy_code()); //
			timeSaveIn.setSub_acct_seq(openAcctOut.getSub_acct_seq()); //
			timeSaveIn.setAcct_hold_check_Ind(E_YESORNO.YES);
			timeSaveIn.setOpen_voch_check_ind(E_YESORNO.YES);
			timeSaveIn.setBack_value_date(openAcctOut.getBack_value_date()); //
			timeSaveIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // cash or trxn
			timeSaveIn.setOpen_acct_save_ind(null); //
			timeSaveIn.setTrxn_amt(actTrxnAmt); //
			timeSaveIn.setSummary_code(summaryCode); //
			timeSaveIn.setTrxn_remark(""); //
			timeSaveIn.setCustomer_remark(""); // customer remark
			timeSaveIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT); //
			timeSaveIn.setOpp_acct_ccy(subAcctInfo.getCcy_code()); //
			timeSaveIn.setOpp_acct_type(""); //
			timeSaveIn.setOpp_acct_no(subAcctInfo.getAcct_no()); //
			timeSaveIn.setOpp_branch_id(subAcctInfo.getSub_acct_branch()); //
			timeSaveIn.setOpp_sub_acct_seq(subAcctInfo.getSub_acct_seq());
			
			timeSaveOut = BizUtil.getInstance(SrvDpTimeAccounting.class).timeSave(timeSaveIn);

		}
		catch (LttsBusinessException e) {

			DaoUtil.rollbackTransaction();

			bizlog.error(" time save faile=[%s]", e, e.getMessage());

			// 失败登记相关信息
			regTimeFinanceDetl(timeFinanceInfo, null, BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE);
			updTimeFinanceInfo(timeFinanceInfo, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, BigDecimal.ZERO);
			return;
		}

		// 自身账户支取
		DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

		demandDrawIn.setAcct_no(timeFinanceInfo.getAcct_no()); //
		demandDrawIn.setCcy_code(timeFinanceInfo.getCcy_code()); //
		demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // cash
		demandDrawIn.setCheck_password_ind(E_YESORNO.NO); //
		demandDrawIn.setTrxn_amt(actTrxnAmt); //
		demandDrawIn.setSummary_code(summaryCode); //
		demandDrawIn.setTrxn_remark(""); //
		demandDrawIn.setCustomer_remark(""); //
		demandDrawIn.setAcct_hold_check_Ind(E_YESORNO.YES);
		demandDrawIn.setOpen_voch_check_ind(E_YESORNO.YES);
		demandDrawIn.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT); //
		demandDrawIn.setOpp_acct_ccy(timeSaveOut.getCcy_code()); //
		demandDrawIn.setOpp_acct_type(timeSaveOut.getAcct_type()); //
		demandDrawIn.setOpp_acct_no(timeSaveOut.getAcct_no()); //
		demandDrawIn.setOpp_branch_id(timeSaveOut.getSub_acct_branch()); //

		try {

			BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);

			// 登记约定转账明细登记
			regTimeFinanceDetl(timeFinanceInfo, timeSaveOut.getSub_acct_seq(), actTrxnAmt, E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE);

			// 更新约定转账登记
			updTimeFinanceInfo(timeFinanceInfo, E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE, actTrxnAmt);

			ApAccountApi.checkBalance();
		}
		catch (LttsBusinessException e) {

			DaoUtil.rollbackTransaction();

			bizlog.error(" demand draw  faile=[%s]", e, e.getMessage());

			// 失败登记相关信息
			regTimeFinanceDetl(timeFinanceInfo, timeSaveOut.getSub_acct_seq(), BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE);
			updTimeFinanceInfo(timeFinanceInfo, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, BigDecimal.ZERO);

			return;
		}

		bizlog.method(" DpTimeFinance.timeFinanceSingleHandle end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月16日-下午4:07:20</li>
	 *         <li>功能说明：获取实际转出金额</li>
	 *         </p>
	 * @param timeFinanceInfo
	 *            定期理财信息
	 * @param subAcctInfo
	 *            子账户信息
	 * @return 实际转出金额
	 */
	private static BigDecimal getTimeFinanceAmt(DpbTimeFinance timeFinanceInfo, DpaSubAccount subAcctInfo) {
		bizlog.method(" DpTimeFinance.getTimeFinanceAmt begin >>>>>>>>>>>>>>>>");

		BigDecimal actTrxnAmt = BigDecimal.ZERO;

		BigDecimal acctBal = subAcctInfo.getAcct_bal();

		// 最大转出金额,取表中默认值
		BigDecimal maxTrsfAmt = CommUtil.isNotNull(timeFinanceInfo.getMax_turn_out_amt()) ? timeFinanceInfo.getMax_turn_out_amt() : BigDecimal.ZERO;

		// 约定转账金额不为NULL且大于0时,本次最大转出金额不大于:约定转账金额 - 累计成功金额
		if (CommUtil.isNotNull(timeFinanceInfo.getAgree_trsf_amt()) && CommUtil.compare(timeFinanceInfo.getAgree_trsf_amt(), BigDecimal.ZERO) > 0) {

			// 约定转账金额 - 累计成功金额
			BigDecimal nowTrsfAmt = timeFinanceInfo.getAgree_trsf_amt().subtract(timeFinanceInfo.getTotal_success_amt());

			// 与最大转出金额比较,取最小值作为最大转出金额
			if (CommUtil.compare(maxTrsfAmt, BigDecimal.ZERO) > 0 && CommUtil.compare(maxTrsfAmt, nowTrsfAmt) > 0) {

				maxTrsfAmt = nowTrsfAmt;

			}

		}

		// 得出账户可用最大余额
		BigDecimal allowMaxTrsfAmt = acctBal.subtract(timeFinanceInfo.getDemand_remain_bal());

		// 倍增金额不为0,应满足实际转存金额 = 本次起转金额 + n * 倍增金额
		if (CommUtil.isNotNull(timeFinanceInfo.getMultiple_amt()) && CommUtil.compare(timeFinanceInfo.getMultiple_amt(), BigDecimal.ZERO) > 0) {

			// 计算出n,例: 本次起转金额 + n * 倍增金额 <= 账户余额
			int n = (allowMaxTrsfAmt.subtract(timeFinanceInfo.getMin_turn_out_amt())).divide(timeFinanceInfo.getMultiple_amt()).intValue();

			bizlog.debug("Multiple_times=[%s]", n);

			allowMaxTrsfAmt = timeFinanceInfo.getMin_turn_out_amt().add(timeFinanceInfo.getMultiple_amt().multiply(BigDecimal.valueOf(n)));

			allowMaxTrsfAmt = CommUtil.compare(allowMaxTrsfAmt, BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : allowMaxTrsfAmt;
		}

		// 得出实际转出金额
		if (CommUtil.equals(maxTrsfAmt, BigDecimal.ZERO)) {

			actTrxnAmt = allowMaxTrsfAmt;
		}
		else {

			actTrxnAmt = CommUtil.compare(maxTrsfAmt, allowMaxTrsfAmt) < 0 ? maxTrsfAmt : allowMaxTrsfAmt;
		}

		bizlog.debug("actTrxnAmt=[%s]", actTrxnAmt);
		bizlog.method(" DpTimeFinance.getTimeFinanceAmt end <<<<<<<<<<<<<<<<");
		return actTrxnAmt;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月16日-下午4:35:30</li>
	 *         <li>功能说明：更新定期理财协议登记簿</li>
	 *         </p>
	 * @param timeFinanceInfo
	 *            定期协议信息
	 * @param failHandle
	 *            交易处理状态
	 * @param actTrxnAmt
	 *            实际交易金额
	 */
	private static void updTimeFinanceInfo(DpbTimeFinance timeFinanceInfo, E_TRSFHANDLINGSTATUS failHandle, BigDecimal actTrxnAmt) {
		bizlog.method(" DpTimeFinance.updTimeFinanceInfo begin >>>>>>>>>>>>>>>>");

		String nextDate = DpToolsApi.calcDateByReference(timeFinanceInfo.getRef_date(), BizUtil.getTrxRunEnvs().getTrxn_date(), timeFinanceInfo.getAgree_cycle(), timeFinanceInfo.getSign_date());

		bizlog.debug("nextDate=[%s]", nextDate);

		if (failHandle == E_TRSFHANDLINGSTATUS.FAIL_HANDLE) {

			timeFinanceInfo.setNext_exec_date(nextDate);

			DpbTimeFinanceDao.updateOne_odb1(timeFinanceInfo);

		}
		else if (failHandle == E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE) {

			long totalSucesConut = CommUtil.isNull(timeFinanceInfo.getTotal_success_times()) ? 1 : timeFinanceInfo.getTotal_success_times() + 1;

			BigDecimal totalSucesAmt = (CommUtil.isNotNull(timeFinanceInfo.getTotal_success_amt()) ? timeFinanceInfo.getTotal_success_amt() : BigDecimal.ZERO).add(actTrxnAmt);

			timeFinanceInfo.setNext_exec_date(nextDate);
			timeFinanceInfo.setTotal_success_times(totalSucesConut);
			timeFinanceInfo.setTotal_success_amt(totalSucesAmt);

			DpbTimeFinanceDao.updateOne_odb1(timeFinanceInfo);
		}

		bizlog.method(" DpTimeFinance.updTimeFinanceInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月16日-下午3:55:56</li>
	 *         <li>功能说明：超过约定信息失败登记</li>
	 *         </p>
	 * @param timeFinanceInfo
	 *            定期理财信息
	 */
	private static void updFiledTimeFinanceInfo(DpbTimeFinance timeFinanceInfo) {
		bizlog.method(" DpTimeFinance.updFiledTimeFinanceInfo begin >>>>>>>>>>>>>>>>");

		timeFinanceInfo.setNext_exec_date(ApConst.DEFAULT_MAX_DATE);// 超过约定次数默认下一执行日为"20991231"
		// timeFinanceInfo.setFail_times(CommUtil.isNull(timeFinanceInfo.getFail_times())
		// ? 1 : timeFinanceInfo.getFail_times() + 1);

		DpbTimeFinanceDao.updateOne_odb1(timeFinanceInfo);

		bizlog.method(" DpTimeFinance.updFiledTimeFinanceInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月16日-下午3:56:01</li>
	 *         <li>功能说明：定期理财明细登记</li>
	 *         </p>
	 * @param timeFinanceInfo
	 *            定期理财信息
	 * @param subAcctSeq
	 *            子账户序号
	 * @param actTrxnAmt
	 *            实际交易金额
	 * @param failHandle
	 *            处理状态
	 */
	private static void regTimeFinanceDetl(DpbTimeFinance timeFinanceInfo, String subAcctSeq, BigDecimal actTrxnAmt, E_TRSFHANDLINGSTATUS failHandle) {
		bizlog.method(" DpTimeFinance.regTimeFinanceDetl begin >>>>>>>>>>>>>>>>");

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		DpbTimeFinanceDetail timeFinanceDetl = BizUtil.getInstance(DpbTimeFinanceDetail.class);

		timeFinanceDetl.setTrxn_date(runEnvs.getTrxn_date()); //
		timeFinanceDetl.setAgree_no(timeFinanceInfo.getAgree_no()); //
		timeFinanceDetl.setSerial_no(timeFinanceInfo.getSerial_no()); //
		timeFinanceDetl.setAcct_no(timeFinanceInfo.getAcct_no()); // account
		timeFinanceDetl.setCcy_code(timeFinanceInfo.getCcy_code()); //
		timeFinanceDetl.setOpp_acct_no(timeFinanceInfo.getOpp_acct_no()); //
		timeFinanceDetl.setOpp_sub_acct_seq(subAcctSeq); //
		timeFinanceDetl.setTrxn_amt(CommUtil.nvl(actTrxnAmt, BigDecimal.ZERO)); //
		timeFinanceDetl.setTrsf_handling_status(failHandle); //
		timeFinanceDetl.setTrxn_seq(runEnvs.getTrxn_seq()); //

		bizlog.method(" DpTimeFinance.regTimeFinanceDetl end <<<<<<<<<<<<<<<<");
	}

}
