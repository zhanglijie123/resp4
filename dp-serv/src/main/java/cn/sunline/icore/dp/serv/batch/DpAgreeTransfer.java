package cn.sunline.icore.dp.serv.batch;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAccountApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.sms.ApSms;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.serv.common.DpDataFormat;
import cn.sunline.icore.dp.serv.common.DpNotice;
import cn.sunline.icore.dp.serv.common.DpTransactions;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTransfers;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTransfersDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTrsfDetail;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTrsfDetailDao;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpTransferIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpTransferOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustBaseInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_AGREETRSFAMOUNTTYPE;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_AGREETRSFTYPE;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_FAILHANDLINGMETHOD;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_TRSFHANDLINGSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_FOREXEXCHOBJECT;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpAgreeTransfer {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAgreeTransfer.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月25日-下午5:27:39</li>
	 *         <li>功能说明：约定转账单条处理</li>
	 *         </p>
	 * @param agreeTrsfInfo
	 *            约定转账信息
	 * @param subAcctInfo
	 *            子账户信息
	 */
	public static void agereeTrsfSingleHandle(DpbAgreeTransfers agreeTrsfInfo, DpaSubAccount subAcctInfo) {
		bizlog.method(" DpAgreeTransfer.agereeTrsfSingleHandle begin >>>>>>>>>>>>>>>>");

		// 检查是否满足执行时间
		String execTime = agreeTrsfInfo.getExec_time();

		bizlog.debug("execTime=[%s] Computer_time=[%s]", execTime, BizUtil.getTrxRunEnvs().getComputer_time());

		// 为空则取默认执行时间
		if (CommUtil.isNull(execTime)) {

			execTime = ApBusinessParmApi.getValue("AGREEMENT_TRSF_DEFAULT_TIME");
			agreeTrsfInfo.setExec_time(execTime);
		}

		if (CommUtil.compare(execTime, BizUtil.getTrxRunEnvs().getComputer_time()) > 0) {

			return;
		}

		// 转账开始
		String summaryCode = "";
		String oppProd = "";

		// 取出摘要代码
		if (agreeTrsfInfo.getAgree_trsf_type() == E_AGREETRSFTYPE.DEMAND_TO_DEMAND) {// 活期转活期

			summaryCode = ApSystemParmApi.getSummaryCode("DEPT_BATCH_AGREE_TRSF_TO_DEMAND");

			DpaAccountRelate oppAcct = DpaAccountRelateDao.selectOne_odb1(agreeTrsfInfo.getOpp_acct_no(), agreeTrsfInfo.getOpp_sub_acct_seq(), true);

			oppProd = oppAcct.getProd_id();
		}
		else if (agreeTrsfInfo.getAgree_trsf_type() == E_AGREETRSFTYPE.DEMAND_TO_INSIDE) {

			summaryCode = ApSystemParmApi.getSummaryCode("DEPT_BATCH_AGREE_TRSF_TO_INSIDE");
		}
		else {
			throw APPUB.E0026(DpDict.A.agree_trsf_type.getLongName(), agreeTrsfInfo.getAgree_trsf_type().getValue());
		}

		// 一对一转账接口
		DpTransferIn transferIn = BizUtil.getInstance(DpTransferIn.class);

		transferIn.setSummary_code(summaryCode);
		transferIn.setTrxn_amt(BigDecimal.ZERO);

		// 约定转账暂停使用
		if (agreeTrsfInfo.getStop_use_ind() == E_YESORNO.YES) {

			regAgreeTrsfDetl(agreeTrsfInfo, BigDecimal.ZERO, BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, null, "Agree stop used");

			updAgreeTrsfInfo(agreeTrsfInfo, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, BigDecimal.ZERO);

			return;
		}

		// 校验累计成功次数 是否超过约定次数
		if (CommUtil.isNotNull(agreeTrsfInfo.getAgree_trsf_times()) && agreeTrsfInfo.getAgree_trsf_times() != 0) {

			if (CommUtil.compare(agreeTrsfInfo.getAgree_trsf_times(), agreeTrsfInfo.getTotal_success_times()) < 0) {

				regAgreeTrsfDetl(agreeTrsfInfo, BigDecimal.ZERO, BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, null, "Beyond agree times limit");

				updFiledTimesAgreeTrsfInfo(agreeTrsfInfo);

				return;
			}
		}

		// 校验累计成功次数 是否超过约定金额
		if (CommUtil.isNotNull(agreeTrsfInfo.getAgree_trsf_amt()) && !CommUtil.equals(agreeTrsfInfo.getAgree_trsf_amt(), BigDecimal.ZERO)) {

			if (CommUtil.compare(agreeTrsfInfo.getAgree_trsf_amt(), agreeTrsfInfo.getTotal_success_amt()) < 0) {

				regAgreeTrsfDetl(agreeTrsfInfo, BigDecimal.ZERO, BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, null, "Beyond agree amount limit");

				updFiledTimesAgreeTrsfInfo(agreeTrsfInfo);

				return;
			}
		}

		// 检测是否为失败重转
		if (CommUtil.isNotNull(agreeTrsfInfo.getFail_renew_trsf_date()) && CommUtil.equals(BizUtil.getTrxRunEnvs().getTrxn_date(), agreeTrsfInfo.getFail_renew_trsf_date())) {

			if (CommUtil.isNotNull(agreeTrsfInfo.getFail_renew_trsf_times()) && CommUtil.compare(agreeTrsfInfo.getFail_renew_trsf_times(), Long.valueOf(0)) != 0) {

				if (CommUtil.compare(agreeTrsfInfo.getFail_renew_trsf_times(), agreeTrsfInfo.getFail_times()) < 0) {

					// 更新重转信息
					agreeTrsfInfo.setFail_times(Long.valueOf(0));
					agreeTrsfInfo.setFail_renew_trsf_date(null);

					DpbAgreeTransfersDao.updateOne_odb1(agreeTrsfInfo);

					return;// 已经超过最大重试次数,直接退出
				}
			}
			else {

				if (ApBusinessParmApi.exists("DP_AGREE_TRSF", "DP_AGREE_TRSF_RERUN_TIMES")) {

					int defTimes = ApBusinessParmApi.getIntValue("DP_AGREE_TRSF", "DP_AGREE_TRSF_RERUN_TIMES");

					if (CommUtil.compare(Long.valueOf(defTimes), agreeTrsfInfo.getFail_times()) < 0) {
						// 更新重转信息
						agreeTrsfInfo.setFail_times(Long.valueOf(0));
						agreeTrsfInfo.setFail_renew_trsf_date(null);

						DpbAgreeTransfersDao.updateOne_odb1(agreeTrsfInfo);

						return;// 已经超过最大重试次数,直接退出
					}
				}
				else {

					return;
				}
			}
		}

		// 实际交易金额
		BigDecimal actTrxnAmt = getActTransferAmt(agreeTrsfInfo, subAcctInfo);

		bizlog.debug("actTrxnAmt=[%s]", actTrxnAmt);

		if (CommUtil.equals(actTrxnAmt, BigDecimal.ZERO)) {

			regAgreeTrsfDetl(agreeTrsfInfo, BigDecimal.ZERO, BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, "2002", "Insufficient account balance");
			registerMailInfo(transferIn, agreeTrsfInfo, subAcctInfo, E_TRSFHANDLINGSTATUS.FAIL_HANDLE);

			return;
		}

		if (CommUtil.isNotNull(agreeTrsfInfo.getExternal_scene_code())) {// 补充外部场景码,涉及限额

			BizUtil.getTrxRunEnvs().setExternal_scene_code(agreeTrsfInfo.getExternal_scene_code());
		}

		if (CommUtil.isNotNull(agreeTrsfInfo.getTrxn_channel())) {// 补充外部场景码,涉及限额

			BizUtil.getTrxRunEnvs().setChannel_id(agreeTrsfInfo.getTrxn_channel());
		}

		// 调用一对一转账
		transferIn.setTrxn_ccy(agreeTrsfInfo.getCcy_code()); // transaction
		transferIn.setTrxn_amt(actTrxnAmt); // transaction amount
		transferIn.setDebit_cash_trxn_ind(E_CASHTRXN.TRXN); //
		transferIn.setDebit_ccy_code(agreeTrsfInfo.getCcy_code()); //
		transferIn.setDebit_acct_no(agreeTrsfInfo.getAcct_no()); // debit
		transferIn.setDebit_acct_name(subAcctInfo.getSub_acct_name()); //
		transferIn.setDebit_prod_id(subAcctInfo.getProd_id()); //
		transferIn.setDebit_acct_branch(subAcctInfo.getSub_acct_branch()); //
		transferIn.setCheck_password_ind(E_YESORNO.NO); //
		transferIn.setTrxn_password(""); // trxn password
		transferIn.setCredit_cash_trxn_ind(E_CASHTRXN.TRXN); //
		transferIn.setCredit_ccy_code(agreeTrsfInfo.getOpp_acct_ccy()); //
		transferIn.setCredit_acct_no(agreeTrsfInfo.getOpp_acct_no()); //
		transferIn.setCredit_prod_id(oppProd);
		transferIn.setCredit_acct_branch(agreeTrsfInfo.getOpp_branch_id()); //
		transferIn.setCountry_code(""); // country code
		transferIn.setSummary_code(summaryCode); // sum
		transferIn.setTrxn_remark(agreeTrsfInfo.getAgree_no());// 默认为协议号
		transferIn.setCustomer_remark(agreeTrsfInfo.getRemark());// 备注字段映射到客户备注字段

		try {
			DpTransferOut transferOut = DpTransactions.singleTransfer(transferIn);

			// 登记约定转账明细登记
			regAgreeTrsfDetl(agreeTrsfInfo, actTrxnAmt, transferOut.getAct_dept_amt(), E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE, null, null);

			// 更新约定转账登记
			updAgreeTrsfInfo(agreeTrsfInfo, E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE, actTrxnAmt);

			ApAccountApi.checkBalance();

			// 登记邮件发送信息
			registerMailInfo(transferIn, agreeTrsfInfo, subAcctInfo, E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE);
		}
		catch (LttsBusinessException e) {

			bizlog.error("trsf faile=[%s]", e, e.getMessage());

			// 抛出了异常先回滚事物
			DaoUtil.rollbackTransaction();

			// 失败登记相关信息
			regAgreeTrsfDetl(agreeTrsfInfo, BigDecimal.ZERO, BigDecimal.ZERO, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, e.getCode(), e.getMessage());
			updAgreeTrsfInfo(agreeTrsfInfo, E_TRSFHANDLINGSTATUS.FAIL_HANDLE, BigDecimal.ZERO);

			// 登记邮件发送信息
			registerMailInfo(transferIn, agreeTrsfInfo, subAcctInfo, E_TRSFHANDLINGSTATUS.FAIL_HANDLE);
			return;
		}

		bizlog.method(" DpAgreeTransfer.agereeTrsfSingleHandle end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月26日-下午4:16:42</li>
	 *         <li>功能说明：结售汇处理</li>
	 *         </p>
	 * @param agreeTrsfInfo
	 *            约定转账信息
	 * @param subAcctInfo
	 *            子账户信息
	 * @param actTrxnAmt
	 *            实际转出金额
	 * @param summaryCode
	 *            摘要代码
	 * @return 结售汇算出后实际转出金额
	 */
	@SuppressWarnings("unused")
	private static BigDecimal forexTrxnMiddleService(DpbAgreeTransfers agreeTrsfInfo, DpaSubAccount subAcctInfo, BigDecimal actTrxnAmt, String summaryCode) {
		bizlog.method(" DpAgreeTransfer.forexTrxnMiddleService begin >>>>>>>>>>>>>>>>");

		// 结售汇中间记账服务输入
		DpExchangeAccountingIn fxExchangeIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

		fxExchangeIn.setBuy_cash_ind(E_CASHTRXN.TRXN);
		fxExchangeIn.setBuy_ccy_code(subAcctInfo.getCcy_code());
		fxExchangeIn.setSell_cash_ind(E_CASHTRXN.TRXN);
		fxExchangeIn.setSell_ccy_code(agreeTrsfInfo.getOpp_acct_ccy());
		fxExchangeIn.setBuy_amt(actTrxnAmt);

		// 客户类型对现钞兑换有用
		fxExchangeIn.setCust_type(subAcctInfo.getCust_type());
		fxExchangeIn.setCountry_code(subAcctInfo.getCountry_code());
		fxExchangeIn.setCustomer_remark("");
		fxExchangeIn.setSummary_code(summaryCode);//
		fxExchangeIn.setTrxn_remark("");

		// 买卖双方账户信息
		fxExchangeIn.setSell_acct_no(agreeTrsfInfo.getOpp_acct_no());
		fxExchangeIn.setSell_sub_acct_seq(agreeTrsfInfo.getOpp_sub_acct_seq());
		fxExchangeIn.setBuy_acct_no(subAcctInfo.getAcct_no());
		fxExchangeIn.setBuy_sub_acct_seq(subAcctInfo.getSub_acct_seq());
		fxExchangeIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.CUSTOMER);

		// 调用结售汇中间记账服务
		DpExchangeAccountingOut fxExchangeOut = DpExchangeIobus.exchangeAccounting(fxExchangeIn);

		bizlog.debug("fxExchangeOut=[%s]", fxExchangeOut);
		bizlog.method(" DpAgreeTransfer.forexTrxnMiddleService end <<<<<<<<<<<<<<<<");
		return fxExchangeOut.getSell_amt();
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月28日-上午9:07:23</li>
	 *         <li>功能说明：超过约定信息失败登记</li>
	 *         </p>
	 * @param agreeTrsfInfo
	 *            约定转账信息
	 */
	private static void updFiledTimesAgreeTrsfInfo(DpbAgreeTransfers agreeTrsfInfo) {

		agreeTrsfInfo.setNext_exec_date(ApConst.DEFAULT_MAX_DATE);// 超过约定次数默认下一执行日为"20991231"
		agreeTrsfInfo.setFail_times(CommUtil.isNull(agreeTrsfInfo.getFail_times()) ? 1 : agreeTrsfInfo.getFail_times() + 1);

		DpbAgreeTransfersDao.updateOne_odb1(agreeTrsfInfo);
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月26日-上午11:40:33</li>
	 *         <li>功能说明：更新约定转账登记簿</li>
	 *         </p>
	 * @param agreeTrsfInfo
	 *            约定转账信息
	 * @param failHandle
	 *            处理结果
	 * @param actTrxnAmt
	 *            实际转出金额
	 */
	private static void updAgreeTrsfInfo(DpbAgreeTransfers agreeTrsfInfo, E_TRSFHANDLINGSTATUS failHandle, BigDecimal actTrxnAmt) {
		bizlog.method(" DpAgreeTransfer.updAgreeTrsfInfo begin >>>>>>>>>>>>>>>>");

		String nextDate = DpDataFormat.calcNextDate(agreeTrsfInfo.getRef_date(), BizUtil.getTrxRunEnvs().getTrxn_date(), agreeTrsfInfo.getAgree_cycle(),
				agreeTrsfInfo.getBrief_date_symbol());

		bizlog.debug("nextDate=[%s]", nextDate);

		if (failHandle == E_TRSFHANDLINGSTATUS.FAIL_HANDLE) {

			// 失败后按周期重转则重计算下一执行日
			if (agreeTrsfInfo.getFail_handling_method() == E_FAILHANDLINGMETHOD.BY_CYClE_AGAIN_TRANSFER) {

				String failNextDate = "";

				if (CommUtil.equals("0D", agreeTrsfInfo.getFail_renew_trsf_cycle())) {
					failNextDate = BizUtil.getTrxRunEnvs().getTrxn_date();
				}
				else {
					failNextDate = DpDataFormat.calcNextDate(agreeTrsfInfo.getRef_date(), BizUtil.getTrxRunEnvs().getTrxn_date(), agreeTrsfInfo.getFail_renew_trsf_cycle(), null);
				}

				agreeTrsfInfo.setFail_renew_trsf_date(failNextDate);
			}
			else {
				// 如果有设置全局重试周期，则默认会按全局参数进行重试
				if (ApBusinessParmApi.exists("DP_AGREE_TRSF", "DP_AGREE_TRSF_RERUN_TIMES")) {

					String defCycle = ApBusinessParmApi.getValue("DP_AGREE_TRSF", "DP_AGREE_TRSF_RERUN_TIMES");

					if (CommUtil.isNotNull(defCycle)) {

						String failNextDate;

						if (CommUtil.equals("0D", agreeTrsfInfo.getFail_renew_trsf_cycle())) {
							failNextDate = BizUtil.getTrxRunEnvs().getTrxn_date();
						}
						else {
							failNextDate = DpDataFormat.calcNextDate(agreeTrsfInfo.getRef_date(), BizUtil.getTrxRunEnvs().getTrxn_date(), defCycle, null);
						}

						agreeTrsfInfo.setFail_renew_trsf_date(failNextDate);
					}
				}
			}

			// 无失败次数,或者失败次数为0,说明为第一次失败,第一次失败仍需计算出下一执行日
			if (CommUtil.isNull(agreeTrsfInfo.getFail_times()) || CommUtil.compare(agreeTrsfInfo.getFail_times(), Long.valueOf(0)) == 0) {

				agreeTrsfInfo.setNext_exec_date(nextDate);
			}

			agreeTrsfInfo.setFail_times(CommUtil.isNull(agreeTrsfInfo.getFail_times()) ? 1 : agreeTrsfInfo.getFail_times() + 1);

			DpbAgreeTransfersDao.updateOne_odb1(agreeTrsfInfo);

		}
		else if (failHandle == E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE) {

			long totalSucesConut = CommUtil.isNull(agreeTrsfInfo.getTotal_success_times()) ? 1 : agreeTrsfInfo.getTotal_success_times() + 1;

			BigDecimal totalSucesAmt = (CommUtil.isNotNull(agreeTrsfInfo.getTotal_success_amt()) ? agreeTrsfInfo.getTotal_success_amt() : BigDecimal.ZERO).add(actTrxnAmt);

			if (CommUtil.isNotNull(agreeTrsfInfo.getFail_renew_trsf_date())) {// 不为空则说明为失败重试,失败重试不需要重新赋值下一执行日

				agreeTrsfInfo.setFail_times((long) 0);
				agreeTrsfInfo.setFail_renew_trsf_date(null);
			}
			else {

				agreeTrsfInfo.setNext_exec_date(nextDate);
			}

			agreeTrsfInfo.setTotal_success_times(totalSucesConut);
			agreeTrsfInfo.setTotal_success_amt(totalSucesAmt);

			DpbAgreeTransfersDao.updateOne_odb1(agreeTrsfInfo);
		}

		bizlog.method(" DpAgreeTransfer.updAgreeTrsfInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月26日-上午10:58:53</li>
	 *         <li>功能说明：登记约定转账明细</li>
	 *         </p>
	 * @param agreeTrsfInfo
	 *            约定转账信息
	 * @param actTrxnAmt
	 *            实际交易金额
	 * @param failHandle
	 *            处理结果
	 */
	private static void regAgreeTrsfDetl(DpbAgreeTransfers agreeTrsfInfo, BigDecimal actTrxnAmt, BigDecimal oppAmt, E_TRSFHANDLINGSTATUS failHandle, String errorCode,
			String errorText) {
		bizlog.method(" DpAgreeTransfer.regAgreeTrsfDetl begin >>>>>>>>>>>>>>>>");

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		DpbAgreeTrsfDetail agreeTrsfDetl = BizUtil.getInstance(DpbAgreeTrsfDetail.class);

		agreeTrsfDetl.setAgree_no(agreeTrsfInfo.getAgree_no()); // 协议号
		agreeTrsfDetl.setTrxn_date(runEnvs.getTrxn_date()); // 交易日期
		agreeTrsfDetl.setTrxn_seq(runEnvs.getBusi_seq()); //
		agreeTrsfDetl.setAgree_trsf_type(agreeTrsfInfo.getAgree_trsf_type()); // 约定转账类型
		agreeTrsfDetl.setAcct_no(agreeTrsfInfo.getAcct_no()); // 账号
		agreeTrsfDetl.setCcy_code(agreeTrsfInfo.getCcy_code()); // 货币代号
		agreeTrsfDetl.setSub_acct_seq(agreeTrsfInfo.getSub_acct_seq());
		agreeTrsfDetl.setTrxn_amt(CommUtil.nvl(actTrxnAmt, BigDecimal.ZERO)); // transaction
		agreeTrsfDetl.setOpp_acct_no(agreeTrsfInfo.getOpp_acct_no()); //
		agreeTrsfDetl.setOpp_acct_ccy(agreeTrsfInfo.getOpp_acct_ccy()); //
		agreeTrsfDetl.setOpp_sub_acct_seq(agreeTrsfInfo.getOpp_sub_acct_seq());
		agreeTrsfDetl.setExch_rate(BigDecimal.valueOf(1));
		agreeTrsfDetl.setTrxn_opp_amt(CommUtil.nvl(oppAmt, BigDecimal.ZERO)); // transaction
		agreeTrsfDetl.setTrsf_handling_status(failHandle); //
		agreeTrsfDetl.setError_code(errorCode);
		agreeTrsfDetl.setError_text(errorText);

		DpbAgreeTrsfDetailDao.insert(agreeTrsfDetl);

		bizlog.method(" DpAgreeTransfer.regAgreeTrsfDetl end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月26日-上午10:04:14</li>
	 *         <li>功能说明：获取实际转出金额</li>
	 *         </p>
	 * @param agreeTrsfInfo
	 *            约定转账信息
	 * @param subAcctInfo
	 *            子账户信息
	 * @return 实际转出金额
	 */
	private static BigDecimal getActTransferAmt(DpbAgreeTransfers agreeTrsfInfo, DpaSubAccount subAcctInfo) {
		bizlog.method(" DpAgreeTransfer.getActTransferAmt begin >>>>>>>>>>>>>>>>");

		if (agreeTrsfInfo.getAgree_trsf_amt_type() == E_AGREETRSFAMOUNTTYPE.FIXED) {

			if (CommUtil.compare(agreeTrsfInfo.getTrxn_amt(), subAcctInfo.getAcct_bal().subtract(agreeTrsfInfo.getDemand_remain_bal())) > 0) {
				return BigDecimal.ZERO;
			}
			else {
				return agreeTrsfInfo.getTrxn_amt();
			}
		}

		// 不够最小转出金额则返回零
		if (CommUtil.compare(subAcctInfo.getAcct_bal(), agreeTrsfInfo.getDemand_remain_bal().add(agreeTrsfInfo.getMin_turn_out_amt())) < 0) {

			return BigDecimal.ZERO;
		}

		BigDecimal actTrxnAmt = BigDecimal.ZERO; // 默认为最小转出金额

		BigDecimal acctBal = subAcctInfo.getAcct_bal();

		// 获取最小转出金额
		BigDecimal minTurnOutAmt = CommUtil.nvl(agreeTrsfInfo.getMin_turn_out_amt(), BigDecimal.ZERO);

		actTrxnAmt = minTurnOutAmt;

		// 最大转出金额,取表中默认值
		BigDecimal maxTrsfAmt = CommUtil.isNotNull(agreeTrsfInfo.getMax_turn_out_amt()) && CommUtil.compare(agreeTrsfInfo.getMax_turn_out_amt(), BigDecimal.ZERO) > 0 ? agreeTrsfInfo
				.getMax_turn_out_amt() : BigDecimal.ZERO;

		// 约定转账金额不为NULL且大于0时,本次最大转出金额不大于:约定转账金额 - 累计成功金额
		if (CommUtil.isNotNull(agreeTrsfInfo.getAgree_trsf_amt()) && CommUtil.compare(agreeTrsfInfo.getAgree_trsf_amt(), BigDecimal.ZERO) > 0) {

			// 约定转账金额 - 累计成功金额
			BigDecimal nowTrsfAmt = agreeTrsfInfo.getAgree_trsf_amt().subtract(agreeTrsfInfo.getTotal_success_amt());

			if (CommUtil.compare(nowTrsfAmt, minTurnOutAmt) < 0) {// 如果最小转出金额比剩余累计转账金额还多,则不转

				return BigDecimal.ZERO;
			}

			// 与最大转出金额比较,取最小值作为最大转出金额
			if (CommUtil.compare(maxTrsfAmt, BigDecimal.ZERO) > 0 && CommUtil.compare(maxTrsfAmt, nowTrsfAmt) > 0) {
				maxTrsfAmt = nowTrsfAmt;
			}
		}

		// 得出账户可用最大余额
		BigDecimal allowMaxTrsfAmt = acctBal.subtract(agreeTrsfInfo.getDemand_remain_bal());

		// 倍增金额不为0,应满足实际转存金额 = 本次起转金额 + n * 倍增金额
		BigDecimal multiplAmt = CommUtil.nvl(agreeTrsfInfo.getMultiple_amt(), BigDecimal.ZERO);

		if (!CommUtil.equals(multiplAmt, BigDecimal.ZERO)) {

			// 计算出n,例: 本次起转金额 + n * 倍增金额 <= 账户余额
			int n = (allowMaxTrsfAmt.subtract(minTurnOutAmt)).divide(multiplAmt).intValue();

			bizlog.debug("Multiple_times=[%s]", n);

			allowMaxTrsfAmt = minTurnOutAmt.add(multiplAmt.multiply(BigDecimal.valueOf(n)));

			allowMaxTrsfAmt = CommUtil.compare(allowMaxTrsfAmt, BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : allowMaxTrsfAmt;

			// 得出实际转出金额
			actTrxnAmt = !CommUtil.equals(maxTrsfAmt, BigDecimal.ZERO) && CommUtil.compare(maxTrsfAmt, allowMaxTrsfAmt) < 0 ? maxTrsfAmt : allowMaxTrsfAmt;

		}

		bizlog.debug("actTrxnAmt=[%s]", actTrxnAmt);
		bizlog.method(" DpAgreeTransfer.getActTransferAmt end <<<<<<<<<<<<<<<<");
		return actTrxnAmt;
	}

	/**
	 * @Author Administrator
	 *         <p>
	 *         <li>2019年1月29日-下午3:15:12</li>
	 *         <li>功能说明：登记邮件发送信息</li>
	 *         </p>
	 * @param transferIn
	 *            转账信息
	 * @param agreeTrsfInfo
	 *            约定转账协议信息
	 * @param subAcctInfo
	 *            转出子户信息
	 * @param handle
	 *            转账状态
	 */
	private static void registerMailInfo(DpTransferIn transferIn, DpbAgreeTransfers agreeTrsfInfo, DpaSubAccount subAcctInfo, E_TRSFHANDLINGSTATUS handle) {
		bizlog.method(" DpAgreeTransfer.registerMailInfo begin >>>>>>>>>>>>>>>>");

		// BAY需求只有转入存钱罐的约定转账需发送邮件
		if (CommUtil.equals("PSS", transferIn.getSummary_code())) {

			// 获取客户基本信息
			DpCustBaseInfo cfCustInfo = DpCustomerIobus.getCustBaseInfo(subAcctInfo.getCust_no(), subAcctInfo.getCust_type());

			Map<String, Object> mailData = new HashMap<>();

			ApBufferApi.addData(ApConst.CUST_DATA_MART, CommUtil.toMap(cfCustInfo));

			mailData.putAll(CommUtil.toMap(cfCustInfo));
			mailData.putAll(CommUtil.toMap(BizUtil.getTrxRunEnvs()));
			mailData.putAll(CommUtil.toMap(agreeTrsfInfo));

			String title = cfCustInfo.getCustInfo().containsKey("title") ? cfCustInfo.getCustInfo().get("title").toString() : "";
			String titleThai = cfCustInfo.getCustInfo().containsKey("title_thai") ? cfCustInfo.getCustInfo().get("title_thai").toString() : "";

			// title处理
			if (ApDropListApi.exists("TITLE", title, false)) {

				mailData.put("title", ApDropListApi.getText("TITLE", title));
			}
			else {
				mailData.put("title", "Mr/Ms");
			}

			if (ApDropListApi.exists("THAI_TITLE", titleThai, false)) {

				mailData.put("title_thai", ApDropListApi.getText("THAI_TITLE", titleThai));
			}
			else {
				mailData.put("title_thai", "นาย/นางสาว");
			}

			mailData.put("sign_time", agreeTrsfInfo.getData_create_time().substring(9, 14));
			mailData.put("trxn_amt", DpDataFormat.messageFormat((handle == E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE ? transferIn.getTrxn_amt() : BigDecimal.ZERO), "amount", "en"));
			mailData.put("trxn_time", BizUtil.getTrxRunEnvs().getComputer_time().substring(0, 5));
			mailData.put(SysDict.A.sign_date.getId(), DpDataFormat.messageFormat(agreeTrsfInfo.getSign_date(), "trxnDate", cfCustInfo.getLanguage()));
			mailData.put(SysDict.A.trxn_date.getId(), DpDataFormat.messageFormat(BizUtil.getTrxRunEnvs().getTrxn_date(), "trxnDate", cfCustInfo.getLanguage()));

			// 登记推送信息
			ApBufferApi.addData(ApConst.INPUT_DATA_MART, mailData);

			// 已验证并且打开推送开关的，发送邮件
			if (cfCustInfo.getEmail_validate_ind() == E_YESORNO.YES && cfCustInfo.getPush_email_ind() == E_YESORNO.YES) {// 邮箱验证成功

				String mailTemplateNo = "";
				// 发送邮件
				try {
					mailTemplateNo = ApBusinessParmApi.getValue(handle == E_TRSFHANDLINGSTATUS.SUCCESS_HANDLE ? "PSS_TRSF_SUCCESS_TEMPLATE" : "PSS_TRSF_FAIL_TEMPLATE",
							CommUtil.isNotNull(cfCustInfo.getLanguage()) ? cfCustInfo.getLanguage() : "en");
				}
				catch (LttsBusinessException e) {
					// 为空则不处理
					bizlog.error("Not find mailTemplateNo, error_code [%s] error_message [%s]", e, e.getCode(), e.getMessage());

				}

				// 发送邮件
				if (CommUtil.isNotNull(mailTemplateNo)) {

					// 登记邮件内容
					DpNotice.registerMailInfoByTemplateNo(mailTemplateNo, mailData, null);
				}
			}

			if (handle == E_TRSFHANDLINGSTATUS.FAIL_HANDLE && CommUtil.isNotNull(cfCustInfo.getPush_sms_ind()) && CommUtil.equals(cfCustInfo.getPush_sms_ind().getValue(), "Y")) {

				ApSms.sendSmsByTemplateNo("SMS0005");
			}
		}

		bizlog.method(" DpAgreeTransfer.registerMailInfo end <<<<<<<<<<<<<<<<");
	}
}
