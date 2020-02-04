package cn.sunline.icore.dp.serv.dayend;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAccountApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.ap.util.DBUtil;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.serv.account.save.DpTimeSaveCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpCommonDao;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.servicetype.SrvDpTimeAccounting;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaDepositPlan;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaDepositPlanDao;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaDepositPlanDetail;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpaDepositPlanDetailDao;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeSaveIn;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_AMTPERTWAY;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：定期账户自动交易
 * </p>
 * 
 * @Author shenxy
 *         <p>
 *         <li>2017年8月27日-下午12:58:32</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年8月27日-shenxy：定期账户自动交易</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTdAcctAutoTrxn {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTdAcctAutoTrxn.class);

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年8月28日-下午4:11:28</li>
	 *         <li>功能说明：定期账户自动支取</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 * @throws Exception
	 */
	public static void tdAcctAutoDraw(String acctNo, String subAcctNo) throws Exception {

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String trxnDate = runEnvs.getTrxn_date();
		String orgId = runEnvs.getBusi_org_id();

		// 带锁查询子账户
		DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(acctNo, subAcctNo, true);

		// 销户的直接退出
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
			return;
		}

		// 获得支取金额: 因为部分扣划，可能导致支取计划表执行标志更新不准确
		BigDecimal totalAmount = SqlDpCommonDao.getTimeDrawPlanTotalAmount(acctNo, subAcct.getSub_acct_no(), trxnDate, orgId, false);

		if (CommUtil.isNull(totalAmount) || CommUtil.equals(totalAmount, BigDecimal.ZERO)) {
			return;
		}

		// 计算可支取金额
		BigDecimal trxnAmt = totalAmount.subtract(subAcct.getAccm_withdrawal_amt());

		if (CommUtil.compare(trxnAmt, subAcct.getAcct_bal()) > 0) {

			trxnAmt = subAcct.getAcct_bal();
		}

		// 贷方账户及币种信息
		String creditAcct = subAcct.getPrin_trsf_acct();
		String creditCcy = subAcct.getPrin_trsf_acct_ccy();

		if (CommUtil.isNull(creditAcct)) {
			creditAcct = subAcct.getIncome_inst_acct();
			creditCcy = subAcct.getIncome_inst_ccy();
		}

		// 批量调定期支取联机接口
		Map<String, Object> tranInput = new HashMap<String, Object>();

		tranInput.put(DpDict.A.check_same_cust_ind.toString(), E_YESORNO.NO);
		tranInput.put(SysDict.A.cash_trxn_ind.toString(), E_CASHTRXN.TRXN);
		tranInput.put(SysDict.A.check_password_ind.toString(), E_YESORNO.NO);
		tranInput.put(SysDict.A.acct_no.toString(), subAcct.getAcct_no());
		tranInput.put(SysDict.A.trxn_ccy.toString(), subAcct.getCcy_code());
		tranInput.put(SysDict.A.trxn_amt.toString(), trxnAmt);
		tranInput.put(SysDict.A.sub_acct_seq.toString(), subAcct.getSub_acct_seq());
		tranInput.put(SysDict.A.credit_acct_no.toString(), creditAcct);
		tranInput.put(SysDict.A.credit_ccy_code.toString(), creditCcy);
		tranInput.put(SysDict.A.summary_code.toString(), ApSystemParmApi.getSummaryCode("DEPT_DAYEND_TIEMDRAW"));
		tranInput.put(SysDict.A.trxn_remark.toString(), "");

		if (!CommUtil.equals(subAcct.getCcy_code(), creditCcy)) {
			tranInput.put(SysDict.A.exch_rate_path.toString(), subAcct.getCcy_code().concat("/").concat(creditCcy));
		}

		// 调用定期支取接口， 只捕捉业务异常，系统异常让它仍然抛错
		try {
			SysUtil.callFlowTran("4041", tranInput);
		}
		catch (LttsBusinessException e) {
			DBUtil.rollBack(); // 回滚事物

			// 定期账户自动支取处理异常
			bizlog.error("Sub-account[%s] automatic withdrawl business exception, errorCode:[%s],errorMessage:[%s]", subAcctNo, ((LttsBusinessException) e).getCode(),
					((LttsBusinessException) e).getMessage());

		}
		catch (Exception e) {
			DBUtil.rollBack(); // 回滚事物

			// 定期账户自动支取处理系统异常
			bizlog.error("Sub-account[%s] automatic withdrawl others exception", subAcctNo);

			throw e;
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月20日-下午4:11:54</li>
	 *         <li>功能说明：定期账户自动存入</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 */
	public static void tdAcctAutoDeposit(String acctNo, String subAcctNo) {

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String trxnDate = runEnvs.getTrxn_date();

		// 带锁查询子账户
		DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(acctNo, subAcctNo, true);

		// 销户的直接退出
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
			return;
		}

		// 支取计划账户明细
		List<DpaDepositPlanDetail> listDrawAcct = DpaDepositPlanDetailDao.selectAll_odb2(subAcct.getAcct_no(), subAcct.getSub_acct_no(), false);

		if (listDrawAcct.isEmpty() || listDrawAcct.size() == 0) {
			bizlog.error("Sub-account[%s] no found withdrawal account record", subAcctNo);
			return;
		}

		// 当期成功标志： 默认为成功， 目的是为了让系统能自动补漏存
		boolean curTermSuccess = true;

		// (一) 定期存入计划: 当期; 计划状态更新在定期存入服务中处理
		DpaDepositPlan deptPlan = DpaDepositPlanDao.selectOne_odb3(subAcct.getAcct_no(), subAcct.getSub_acct_no(), trxnDate, false);

		if (CommUtil.isNotNull(deptPlan) && deptPlan.getExec_ind() != E_YESORNO.YES) {

			curTermSuccess = tdAcctSingleAutoDeposit(subAcct, listDrawAcct, deptPlan.getPeriod_prcp(), "normal processing");

			if (!curTermSuccess) {
				return;
			}
		}

		// (二) 定期存入计划: 尝试补往期漏存; 漏存的也可以手工去补足
		if (ApBusinessParmApi.exists("TIME_SAVE_PLAN_AUTO_ADD_OMIT") && CommUtil.equals(ApBusinessParmApi.getValue("TIME_SAVE_PLAN_AUTO_ADD_OMIT"), E_YESORNO.YES.getValue())) {

			// 漏存金额必须一次性补齐
			BigDecimal omitAmount = DpTimeSaveCheck.getTimeOmitAmout(subAcct);

			if (CommUtil.compare(omitAmount, BigDecimal.ZERO) > 0) {

				tdAcctSingleAutoDeposit(subAcct, listDrawAcct, omitAmount, "Leakage processing");
			}
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月20日-下午4:11:54</li>
	 *         <li>功能说明：定期账户自动存入</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param listDrawAcct
	 *            支取账户列表
	 * @param prcpAmount
	 *            需存入本金
	 * @return 成功或失败
	 */
	private static boolean tdAcctSingleAutoDeposit(DpaSubAccount subAcct, List<DpaDepositPlanDetail> listDrawAcct, BigDecimal prcpAmount, String trxnRemark) {

		// 剩余本金
		BigDecimal laveAmount = prcpAmount;
		// 摘要代码
		String summaryCode = ApSystemParmApi.getSummaryCode("DEPT_DAYEND_TIEMDEPOSIT");

		// 循环处理转出金额: 只支持同币种
		int maxSort = listDrawAcct.get(listDrawAcct.size()).getSerial_no().intValue();
		for (DpaDepositPlanDetail drawAcct : listDrawAcct) {

			// 没有剩余金额则跳出处理
			if (CommUtil.compare(laveAmount, BigDecimal.ZERO) <= 0) {
				break;
			}

			// 单条支取账户应该支取金额
			BigDecimal trxnAmt = getSingleDrawAmt(drawAcct, prcpAmount, laveAmount, maxSort);

			// 调用活期支取
			DpDemandDrawIn demandDraw = BizUtil.getInstance(DpDemandDrawIn.class);

			demandDraw.setCash_trxn_ind(E_CASHTRXN.TRXN);// 现转标志
			demandDraw.setOpen_voch_check_ind(E_YESORNO.NO);
			demandDraw.setAcct_no(drawAcct.getDraw_acct());// 账号
			demandDraw.setCcy_code(drawAcct.getDraw_acct_ccy());// 币种
			demandDraw.setTrxn_amt(trxnAmt);// 交易金额
			demandDraw.setCheck_password_ind(E_YESORNO.NO);
			demandDraw.setForce_draw_ind(drawAcct.getAmt_apportion_method() == E_AMTPERTWAY.NO ? E_YESORNO.YES : E_YESORNO.NO);
			demandDraw.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);
			demandDraw.setOpp_acct_ccy(subAcct.getCcy_code());
			demandDraw.setOpp_acct_no(subAcct.getAcct_no());
			demandDraw.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());
			demandDraw.setSummary_code(summaryCode);
			demandDraw.setTrxn_remark(trxnRemark);

			try {
				DpDemandDrawOut cplDrawOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDraw);

				// 更新剩余金额
				laveAmount = laveAmount.subtract(cplDrawOut.getAct_withdrawal_amt());
			}
			catch (LttsBusinessException e) {

				DBUtil.rollBack(); // 只要其中一个循环服务报错，全线回滚

				// 定期账户自动存入处理异常
				bizlog.error("Sub-account[%s] automatic deposit business exception[dot 1], errorCode:[%s],errorMessage:[%s]", subAcct.getSub_acct_no(),
						((LttsBusinessException) e).getCode(), ((LttsBusinessException) e).getMessage());

				return false;
			}
			catch (Exception e) {
				DBUtil.rollBack(); // 只要其中一个循环服务报错，全线回滚

				// 定期账户自动存入处理系统异常
				bizlog.error("Sub-account[%s] automatic deposit others exception[dot 2]", subAcct.getSub_acct_no());

				throw e;
			}
		}

		// 未处理完，说明支取方余额不足，则本次自动存入失败，全部回滚掉
		if (CommUtil.compare(laveAmount, BigDecimal.ZERO) > 0) {

			DBUtil.rollBack();
			bizlog.error("Sub-account[%s] auto deposit failure, withdrawer account balance is insufficient", subAcct.getSub_acct_no());

			return false;
		}

		try {
			// 调用定期存入
			DpTimeSaveIn timeSave = BizUtil.getInstance(DpTimeSaveIn.class);

			timeSave.setTrxn_amt(prcpAmount);// 交易金额
			timeSave.setCcy_code(subAcct.getCcy_code());// 币种
			timeSave.setAcct_no(subAcct.getAcct_no());// 账号
			timeSave.setSub_acct_seq(subAcct.getSub_acct_seq());// 子账户序号
			timeSave.setCash_trxn_ind(E_CASHTRXN.TRXN);// 现转标志
			timeSave.setOpp_acct_route(E_ACCOUTANALY.DEPOSIT);// 对方账户路由
			timeSave.setOpp_acct_no(listDrawAcct.get(0).getDraw_acct());
			timeSave.setOpp_acct_ccy(listDrawAcct.get(0).getDraw_acct_ccy());
			timeSave.setSummary_code(summaryCode);
			timeSave.setTrxn_remark(trxnRemark);

			BizUtil.getInstance(SrvDpTimeAccounting.class).timeSave(timeSave);

			// 检查账务平衡
			ApAccountApi.checkBalance();
		}
		catch (LttsBusinessException e) {

			DBUtil.rollBack(); // 回滚

			// 定期账户自动存入处理异常
			bizlog.error("Sub-account[%s] automatic deposit business exception[dot 3], errorCode:[%s],errorMessage:[%s]", subAcct.getSub_acct_no(),
					((LttsBusinessException) e).getCode(), ((LttsBusinessException) e).getMessage());

			return false;
		}
		catch (Exception e) {
			DBUtil.rollBack(); // 回滚

			// 定期账户自动存入处理系统异常
			bizlog.error("Sub-account[%s] automatic deposit others exception[dot 4]", subAcct.getSub_acct_no());

			throw e;
		}

		// 处理成功提交事务
		DBUtil.commit();
		return true;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年6月20日-下午4:11:54</li>
	 *         <li>功能说明：获取单条支取金额</li>
	 *         </p>
	 * @param drawAcct
	 *            支取账户信息
	 * @param totalTrxnAmt
	 *            本期总交易金额
	 * @param laveAmount
	 *            剩余交易金额
	 * @param maxSort
	 *            最大次序
	 * @return 本条支取账户应交易金额
	 */
	private static BigDecimal getSingleDrawAmt(DpaDepositPlanDetail drawAcct, BigDecimal totalTrxnAmt, BigDecimal laveAmount, int maxSort) {

		BigDecimal trxnAmt = BigDecimal.ZERO;

		if (drawAcct.getAmt_apportion_method() == E_AMTPERTWAY.NO) {
			trxnAmt = laveAmount;
		}
		else if (drawAcct.getAmt_apportion_method() == E_AMTPERTWAY.AMOUNT) {
			trxnAmt = ApCurrencyApi.roundAmount(drawAcct.getDraw_acct_ccy(), drawAcct.getAmount_ratio());
		}
		else {

			// 最后一个账户， 不按比例处理，因为按比例计算可能分摊不干净
			if (maxSort == drawAcct.getSerial_no().intValue()) {
				trxnAmt = laveAmount;
			}
			else {
				trxnAmt = totalTrxnAmt.multiply(drawAcct.getAmount_ratio()).divide(BigDecimal.valueOf(100), 7, BigDecimal.ROUND_HALF_UP);

				trxnAmt = ApCurrencyApi.roundAmount(drawAcct.getDraw_acct_ccy(), trxnAmt);
			}
		}

		return trxnAmt;
	}
}
