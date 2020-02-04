package cn.sunline.icore.dp.serv.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_REVERSALTYPE;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_TRXNSTATUS;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApChannelApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApReversalApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.sms.ApSms;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseConst;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpAccountBase.DpUpdAccBalBaseOut;
import cn.sunline.icore.dp.base.type.ComDpTaxBase.DpSettleInstRegister;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ACCTFORM;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.custom.DpEventCodeRelate;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.iobus.DpOtherIobus;
import cn.sunline.icore.dp.serv.maintain.DpAccountFormMove;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBill;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBillDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpPayModeAndChargeCode;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpRegInstBill;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalOut;
import cn.sunline.icore.dp.serv.type.ComDpReversal.DpOtherBillReversalIn;
import cn.sunline.icore.dp.serv.type.ComDpReversal.DpTrxnReversalIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：负债记账及账单登记方法
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月16日-下午4:30:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAccounting {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAccounting.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月17日-下午3:21:29</li>
	 *         <li>功能说明：联机记账处理（非热点账户）</li>
	 *         </p>
	 * @param cplInput
	 *            记账信息
	 * @return
	 */
	public static DpUpdAccBalOut online(DpUpdAccBalIn cplInput) {

		// 非存款利息记录记账
		if (cplInput.getTrxn_record_type() != E_TRXNRECORDTYPE.INTEREST) {
			return prcMain(cplInput, true);
		}

		DpUpdAccBalOut cplOutput = BizUtil.getInstance(DpUpdAccBalOut.class);

		// 接下来处理存款利息记账记录, 按利息和利息税是合并登记还是分开登记两种模式处理
		if (!ApSystemParmApi.exists(DpBaseConst.INT_AND_TAX_BILL_MERGE)
				|| !CommUtil.equals(ApSystemParmApi.getValue(DpBaseConst.INT_AND_TAX_BILL_MERGE), E_YESORNO.YES.getValue())) {

			// 先备份
			DpUpdAccBalIn cplInputNew = BizUtil.clone(DpUpdAccBalIn.class, cplInput);

			cplInputNew.setInst_tax_rate(BigDecimal.ZERO);
			cplInputNew.setInst_withholding_tax(BigDecimal.ZERO);
			cplInputNew.setTrxn_amt(cplInput.getTrxn_amt().add(cplInput.getInst_withholding_tax()));

			if (CommUtil.isNotNull(cplInput.getOpp_trxn_amt())) {

				BigDecimal oppTrxnAmt = cplInput.getOpp_trxn_amt().multiply(cplInputNew.getTrxn_amt()).divide(cplInput.getTrxn_amt(), DpConst.iScale_inst_calc,
						RoundingMode.HALF_UP);

				cplInputNew.setOpp_trxn_amt(ApCurrencyApi.roundAmount(CommUtil.nvl(cplInput.getOpp_acct_ccy(), cplInput.getTrxn_ccy()), oppTrxnAmt));
			}

			cplOutput = prcMain(cplInputNew, true);

			BigDecimal beforeBalance = cplOutput.getTally_before_bal();

			// 利息税有值,再记一遍利息税的账
			if (!CommUtil.equals(cplInput.getInst_withholding_tax(), BigDecimal.ZERO)) {

				cplInputNew.setTrxn_record_type(E_TRXNRECORDTYPE.TAX);
				cplInputNew.setDebit_credit(cplInput.getDebit_credit() == E_DEBITCREDIT.CREDIT ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT);
				cplInputNew.setTrxn_amt(cplInput.getInst_withholding_tax());
				cplInputNew.setInst_tax_rate(cplInput.getInst_tax_rate());
				cplInputNew.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_WITH_HOLD_TAX"));

				if (CommUtil.isNotNull(cplInput.getOpp_trxn_amt())) {

					BigDecimal oppTrxnAmt = cplInput.getOpp_trxn_amt().multiply(cplInputNew.getTrxn_amt()).divide(cplInput.getTrxn_amt(), DpConst.iScale_inst_calc,
							RoundingMode.HALF_UP);

					cplInputNew.setOpp_trxn_amt(ApCurrencyApi.roundAmount(CommUtil.nvl(cplInput.getOpp_acct_ccy(), cplInput.getTrxn_ccy()), oppTrxnAmt));
				}

				cplOutput = prcMain(cplInputNew, true);
			}

			cplOutput.setTally_before_bal(beforeBalance);
			cplOutput.setTrxn_amt(cplInput.getTrxn_amt());
		}
		else {

			cplOutput = prcMain(cplInput, true);
		}

		/* 登记利息税结算信息 */
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(cplInput.getAcct_no(), cplInput.getSub_acct_no(), true);

		DpSettleInstRegister settleInstReg = BizUtil.getInstance(DpSettleInstRegister.class);

		settleInstReg.setInst_tax_rate(cplInput.getInst_tax_rate());
		settleInstReg.setInterest(cplInput.getTrxn_amt().add(cplInput.getInst_withholding_tax()));
		settleInstReg.setInterest_tax(cplInput.getInst_withholding_tax());
		settleInstReg.setTax_rate_code(subAcct.getTax_rate_code());

		DpInterestBasicApi.regInterestSettled(subAcct, settleInstReg);

		// 登记邮件信息: 放到外面来避免结息发两封邮件
		updAccBalPushNotice(cplInput, subAcct, cplOutput.getAcct_bal());

		return cplOutput;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月16日-下午16:27:16</li>
	 *         <li>负债本金记账处理</li>
	 *         </p>
	 * @param cplInput
	 *            记账信息
	 * @param onlineFlag
	 *            联机记账标志 ture 联机 false 异步
	 * @return
	 */
	private static DpUpdAccBalOut prcMain(DpUpdAccBalIn cplInput, boolean onlineFlag) {

		bizlog.method(" DpAccounting.prcMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplInput=[%s]", cplInput);

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		boolean strikeFlag = false; // 冲账交易标志

		// 判断是否冲正交易ID
		if (runEnvs.getReversal_ind() == E_YESORNO.YES)
			strikeFlag = true;

		// 客户交易明细类别，默认为一般收付款
		if (cplInput.getTrxn_record_type() == null)
			cplInput.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL);

		// 余额更新及记账
		DpUpdAccBalBaseOut cplBaseOut = DpBaseServiceApi.tallyProcess(cplInput, onlineFlag);

		// 获取余额更新后的账户信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(cplInput.getAcct_no(), cplInput.getSub_acct_no(), true);

		// 结息和收费不更改上次业务日期： 客户主动交易收费支取部分会更新上次业务日期，日终批量管理费不应更新上次业务日期
		if (subAccount.getAcct_form() != E_ACCTFORM.NORMAL && CommUtil.isNull(cplInput.getChrg_code())
				&& !CommUtil.in(cplInput.getTrxn_record_type(), E_TRXNRECORDTYPE.INTEREST, E_TRXNRECORDTYPE.OD_INTEREST, E_TRXNRECORDTYPE.TAX)) {

			// 动户账户形态变更: 除结息、扣费不触发形态转移外除外， 柜面手工调账也要排除
			if (!ApChannelApi.isCounter(BizUtil.getTrxRunEnvs().getChannel_id()) || !ApSystemParmApi.exists("COUNTER_ADJUST_ACCOUNTING_TRXN")
					|| !ApSystemParmApi.getValue("COUNTER_ADJUST_ACCOUNTING_TRXN").contains(BizUtil.getTrxRunEnvs().getRecon_code())) {

				DpAccountFormMove.acctountFormMove(subAccount, E_ACCTFORM.NORMAL);
			}
		}

		/* 交易流水 */
		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		String offsetInstDate = null;

		// 联机存入时的特殊倒起息处理
		if (onlineFlag && !strikeFlag && E_DEPTTRXNEVENT.DP_SAVE.getValue().equals(cplInput.getTrxn_event_id())) {
			// 实际入账日期大于交易日期，则补这天的利息
			if (CommUtil.compare(subAccount.getBal_update_date(), trxnDate) > 0) {

				if (CommUtil.isNull(cplInput.getBack_value_date()))
					offsetInstDate = trxnDate; // T日
			}
		}

		/* 登记账单表 */
		DpsBill billInfo = regBill(cplInput, offsetInstDate);

		/* 冲正事件登记 */
		if (!strikeFlag) {

			DpTrxnReversalIn reversalIn = BizUtil.getInstance(DpTrxnReversalIn.class);

			reversalIn.setSub_acct_no(cplInput.getSub_acct_no());
			reversalIn.setAcct_no(cplInput.getAcct_no());
			reversalIn.setSerial_no(billInfo.getSerial_no());
			reversalIn.setDebit_credit(cplInput.getDebit_credit());
			reversalIn.setTrxn_amt(cplInput.getTrxn_amt());
			reversalIn.setCard_no(cplInput.getCard_no());
			reversalIn.setRoll_no(subAccount.getRoll_no());
			reversalIn.setOriginal_trxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
			reversalIn.setOriginal_trxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

			ApReversalApi.register("deptTrxnMoney", reversalIn);
		}

		DpUpdAccBalOut cplOut = BizUtil.getInstance(DpUpdAccBalOut.class);

		cplOut.setOffset_inst_date(offsetInstDate); // 补计息日期
		cplOut.setSerial_no(billInfo.getSerial_no()); // 本次记账的明细序号
		cplOut.setAcct_bal(cplBaseOut.getAcct_bal());
		cplOut.setFact_posting_date(cplBaseOut.getFact_posting_date());
		cplOut.setTally_before_bal(cplBaseOut.getTally_before_bal());
		cplOut.setTrxn_amt(cplBaseOut.getTrxn_amt());
		cplOut.setSub_acct_no(subAccount.getSub_acct_no());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAccounting.prcMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2018年1月23日-上午10:54:04</li>
	 *         <li>功能说明：动账消息推送</li>
	 *         </p>
	 * @param cplInput
	 * @param subAccount
	 *            子账户
	 * @param afterBl
	 *            变动后余额
	 */
	public static void updAccBalPushNotice(DpUpdAccBalIn cplInput, DpaSubAccount subAccount, BigDecimal afterBl) {
		bizlog.method(" DpAccounting.updAccBalPushNotice begin >>>>>>>>>>>>>>>>");

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 声明邮件数据集
		Map<String, Object> mailData = new HashMap<String, Object>();

		// 将公共运行区数据加入邮件数据
		mailData.putAll(CommUtil.toMap(runEnvs));

		// 读取数据缓冲区的客户信息
		Object custBuff = ApBufferApi.getBuffer().get(ApConst.CUST_DATA_MART);

		// 客户数据加入邮件内容数据集
		if (CommUtil.isNotNull(custBuff)) {
			mailData.putAll(CommUtil.toMap(custBuff));
		}
		else {
			Map<String, Object> custMap = DpPublicCheck.getCustMapInfo(subAccount.getCust_no(), subAccount.getCust_type());

			mailData.putAll(custMap);

			ApBufferApi.addData(ApConst.CUST_DATA_MART, custMap);
		}

		// 邮件、消息推送、站内信开关未打开,不做下一步处理
		boolean pushEmailInd = CommUtil.equals(mailData.get(SysDict.A.push_email_ind.getId()) + "", E_YESORNO.YES.getValue());
		boolean pushMsglInd = CommUtil.equals(mailData.get(SysDict.A.push_msg_ind.getId()) + "", E_YESORNO.YES.getValue());
		// boolean pushNotificationInd =
		// CommUtil.equals(mailData.get(CfDict.A.push_notification_ind.getId())
		// + "", E_YESORNO.YES.getValue());
		// if (!pushEmailInd && !pushMsglInd && !pushNotificationInd) {
		// return;
		// }
		if (!pushEmailInd && !pushMsglInd) {
			return;
		}

		// 输入数据加入邮件内容数据集
		Object inputBuff = ApBufferApi.getBuffer().get(ApConst.INPUT_DATA_MART);

		if (CommUtil.isNotNull(inputBuff)) {
			mailData.putAll(CommUtil.toMap(inputBuff));
		}

		// 账户数据加入邮件内容数据集
		Object acctBuff = ApBufferApi.getBuffer().get(ApConst.ACCOUNT_DATA_MART);

		if (CommUtil.isNotNull(acctBuff)) {
			mailData.putAll(CommUtil.toMap(acctBuff));
		}
		else {
			DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAccount.getAcct_no(), true);

			mailData.putAll(CommUtil.toMap(acctInfo));

			ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));
		}

		// 子账户信息加入邮件数据集
		mailData.putAll(CommUtil.toMap(subAccount));

		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAccount));

		// 把卡数据集加入邮件数据
		Object cardBuff = ApBufferApi.getBuffer().get(ApConst.CARD_DATA_MART);

		if (CommUtil.isNotNull(cardBuff)) {
			mailData.putAll(CommUtil.toMap(cardBuff));
		}

		// 把参数数据集则加入邮件数据
		Object parmBuff = ApBufferApi.getBuffer().get(ApConst.PARM_DATA_MART);

		if (CommUtil.isNotNull(parmBuff)) {
			mailData.putAll(CommUtil.toMap(parmBuff));
		}

		// 添加动账输入域数据
		mailData.putAll(CommUtil.toMap(cplInput));

		// 获取交易卡号
		String cardNo = CommUtil.nvl(cplInput.getCard_no(), DpToolsApi.getCardNoByAcctNo(subAccount.getAcct_no()));

		// 对外账号： 优先卡号、其次客户账号
		String externalAcctNo = CommUtil.nvl(cardNo, subAccount.getAcct_no());

		// 账号后四位尾数
		String acctTailNo = externalAcctNo.length() >= 4 ? externalAcctNo.substring(externalAcctNo.length() - 4) : "";

		// 邮件验证,并且发送邮件标志已打开时才发送邮件
		if (CommUtil.equals(mailData.get(SysDict.A.email_validate_ind.getId()) + "", E_YESORNO.YES.getValue()) && pushEmailInd) {

			// 补充特殊字段
			mailData.put(DpDict.A.external_acct_no.getId(), externalAcctNo);
			mailData.put(SysDict.A.card_no.getId(), cardNo); // 卡号：有可能是旧卡号
			mailData.put("acct_tail_no", acctTailNo); // 对外账号后四位
			mailData.put("fee_and_trxn_amt", cplInput.getTrxn_amt().add(CommUtil.nvl(runEnvs.getDeduct_chrg_amt(), BigDecimal.ZERO)));
			mailData.put("debit_credit_desc", cplInput.getDebit_credit().getLongName());

			// 登记邮件信息
			DpNotice.registerMailInfoByTrxnEventId(cplInput.getTrxn_event_id(), mailData, null);
		}

		// if (pushMsglInd || pushNotificationInd) {
		if (pushMsglInd) {

			// 有配置短信发送,调用平台方法
			if (ApBusinessParmApi.exists("SMS_TEMPLATE_CODE", cplInput.getTrxn_event_id())) {

				ApSms.sendSmsByTrxnEventId(cplInput.getTrxn_event_id());
			}
		}

		bizlog.method(" DpAccounting.updAccBalPushNotice end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月17日-上午11:07:37</li>
	 *         <li>功能说明：登记负债账单</li>
	 *         </p>
	 * @param cplInput
	 *            余额更新输入接口信息
	 * @param offsetInstDate
	 *            补计息日期
	 */
	public static DpsBill regBill(DpUpdAccBalIn cplInput, String offsetInstDate) {

		bizlog.method(" DpAccounting.regBill begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplInput=[%s]", cplInput);

		// 交易运行变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 读取子账户缓存信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(cplInput.getAcct_no(), cplInput.getSub_acct_no(), true);

		DpsBill billInfo = BizUtil.getInstance(DpsBill.class);

		/* 账单关键信息 */
		billInfo.setSub_acct_no(cplInput.getSub_acct_no());
		billInfo.setSerial_no(subAccount.getData_version()); // 子账户表的数据版本号做序号
		billInfo.setBack_value_date(CommUtil.nvl(CommUtil.nvl(offsetInstDate, cplInput.getBack_value_date()), runEnvs.getTrxn_date()));
		billInfo.setTrxn_event_id(cplInput.getTrxn_event_id());
		billInfo.setShow_ind(cplInput.getShow_ind());
		billInfo.setTally_record_ind(cplInput.getTally_record_ind());
		billInfo.setTrxn_record_type(cplInput.getTrxn_record_type());
		billInfo.setCash_trxn_ind(cplInput.getCash_trxn_ind());
		billInfo.setDebit_credit(cplInput.getDebit_credit());
		billInfo.setTrxn_ccy(CommUtil.nvl(cplInput.getTrxn_ccy(), subAccount.getCcy_code()));
		billInfo.setTrxn_amt(cplInput.getTrxn_amt());
		billInfo.setBal_after_trxn(null);
		billInfo.setTrxn_remark(cplInput.getTrxn_remark());
		billInfo.setCustomer_remark(cplInput.getCustomer_remark());
		billInfo.setSummary_code(cplInput.getSummary_code());
		billInfo.setSummary_name(ApSummaryApi.getText(cplInput.getSummary_code()));
		billInfo.setTrxn_status(E_TRXNSTATUS.NORMAL);

		if (cplInput.getTally_record_ind() == E_YESORNO.YES) {
			billInfo.setBal_after_trxn(subAccount.getAcct_bal().add(subAccount.getAcct_float_bal()));
			billInfo.setTrxn_date(subAccount.getBal_update_date()); // 为了保证连续不能用交易日期
		}

		/* 交易主体信息 */
		billInfo.setCard_no(cplInput.getCard_no());
		billInfo.setAcct_no(subAccount.getAcct_no());
		billInfo.setSub_acct_seq(subAccount.getSub_acct_seq());
		billInfo.setProd_id(subAccount.getProd_id());
		billInfo.setDd_td_ind(subAccount.getDd_td_ind());
		billInfo.setAcct_name(subAccount.getSub_acct_name());
		billInfo.setAcct_branch(subAccount.getSub_acct_branch());
		billInfo.setAcct_branch_name(ApBranchApi.getItem(subAccount.getSub_acct_branch()).getBranch_name());
		billInfo.setAccounting_alias(subAccount.getAccounting_alias());
		billInfo.setCust_no(subAccount.getCust_no());
		billInfo.setCust_type(subAccount.getCust_type());
		billInfo.setVoch_type(cplInput.getVoch_type());
		billInfo.setVoch_no(cplInput.getVoch_no());

		// 交易对手方
		billInfo.setOpp_acct_route(cplInput.getOpp_acct_route());
		billInfo.setOpp_acct_no(cplInput.getOpp_acct_no());
		billInfo.setOpp_card_no("");
		billInfo.setOpp_acct_name(cplInput.getOpp_acct_name());
		billInfo.setOpp_acct_ccy(cplInput.getOpp_acct_ccy());
		billInfo.setOpp_trxn_amt(cplInput.getOpp_trxn_amt());
		billInfo.setOpp_sub_acct_seq(cplInput.getOpp_sub_acct_seq());
		billInfo.setOpp_branch_id(cplInput.getOpp_branch_id());
		billInfo.setOpp_branch_name("");

		/* 交易环境信息 */
		billInfo.setExternal_scene_code(runEnvs.getExternal_scene_code());
		billInfo.setPayment_mode(runEnvs.getPayment_mode());
		billInfo.setFee_code(cplInput.getChrg_code());
		billInfo.setTrxn_code(runEnvs.getTrxn_code());
		billInfo.setRecon_code(runEnvs.getRecon_code());
		billInfo.setThird_party_date(runEnvs.getInitiator_date());
		billInfo.setTrxn_channel(runEnvs.getChannel_id());
		billInfo.setTrxn_date(CommUtil.nvl(billInfo.getTrxn_date(), runEnvs.getTrxn_date()));
		billInfo.setBusi_seq(runEnvs.getBusi_seq());
		billInfo.setTrxn_seq(runEnvs.getTrxn_seq());
		billInfo.setTrxn_branch(runEnvs.getTrxn_branch());
		billInfo.setTrxn_teller(runEnvs.getTrxn_teller());
		billInfo.setTrxn_time(runEnvs.getComputer_time());
		billInfo.setHost_date(runEnvs.getComputer_date());
		billInfo.setInterest_tax(cplInput.getInst_withholding_tax());
		billInfo.setInst_tax_rate(cplInput.getInst_tax_rate());
		billInfo.setVat_amt(cplInput.getVat_amt());
		billInfo.setVat_rate(cplInput.getVat_rate());

		/* 冲正信息 */
		billInfo.setReversal_ind(BizUtil.getTrxRunEnvs().getReversal_ind());
		E_REVERSALTYPE reversalType = null;
		if (CommUtil.isNotNull(((Map) BizUtil.getTrxInput()).get(SysDict.A.reversal_type.getId()))) {
			reversalType = CommUtil.toEnum(E_REVERSALTYPE.class, ((Map) BizUtil.getTrxInput()).get(SysDict.A.reversal_type.getId()));
		}
		billInfo.setReversal_type(reversalType);

		if (CommUtil.isNotNull(cplInput.getReversal_type())) {
			billInfo.setClear_accounts_ind(CommUtil.nvl(cplInput.getClear_accounts_ind(), E_YESORNO.NO));
			billInfo.setOriginal_trxn_date(cplInput.getOriginal_trxn_date());
			billInfo.setOriginal_busi_seq(cplInput.getOriginal_busi_seq());
			billInfo.setOriginal_trxn_seq(cplInput.getOriginal_trxn_seq());
		}

		// 部分退款交易需要登记原交易流水
		if (CommUtil.isNotNull(cplInput.getOriginal_busi_seq()) && CommUtil.isNotNull(cplInput.getOriginal_trxn_seq())) {
			billInfo.setOriginal_busi_seq(cplInput.getOriginal_busi_seq());
			billInfo.setOriginal_trxn_seq(cplInput.getOriginal_trxn_seq());
		}

		/* 代理人或经办人信息 */
		billInfo.setAgent_doc_type(cplInput.getAgent_doc_type());
		billInfo.setAgent_doc_no(cplInput.getAgent_doc_no());
		billInfo.setAgent_name(cplInput.getAgent_name());
		billInfo.setAgent_country(cplInput.getAgent_country());

		/* 接口上送对手方信息, 通常为系统外账户信息 */
		billInfo.setReal_opp_acct_no(cplInput.getReal_opp_acct_no());
		billInfo.setReal_opp_acct_name(cplInput.getReal_opp_acct_name());
		billInfo.setReal_opp_acct_alias(cplInput.getReal_opp_acct_alias());
		billInfo.setReal_opp_country(cplInput.getReal_opp_country());
		billInfo.setReal_opp_bank_id(cplInput.getReal_opp_bank_id());
		billInfo.setReal_opp_bank_name(cplInput.getReal_opp_bank_name());
		billInfo.setReal_opp_branch_name(cplInput.getReal_opp_branch_name());
		billInfo.setReal_opp_remark(cplInput.getReal_opp_remark());

		// 国外刷卡消费信息登记
		billInfo.setTrxn_area(cplInput.getTrxn_area());
		billInfo.setTrxn_area_amt(cplInput.getTrxn_area_amt());
		billInfo.setTrxn_area_ccy(cplInput.getTrxn_area_ccy());
		billInfo.setTrxn_area_exch_rate(cplInput.getTrxn_area_exch_rate());
		billInfo.setConsume_date(cplInput.getConsume_date());
		billInfo.setConsume_time(cplInput.getConsume_time());
		
		//平台消费卡券积分信息
		billInfo.setCard_coupon_acct_no(cplInput.getCard_coupon_acct_no());
		billInfo.setCard_coupon_code(cplInput.getCard_coupon_code());
		billInfo.setCard_coupon_source(cplInput.getCard_coupon_source());
		billInfo.setCard_coupon_trxn_amt(cplInput.getCard_coupon_trxn_amt());
		billInfo.setIntegral_acct_no(cplInput.getIntegral_acct_no());
		billInfo.setIntegral_trxn_amt(cplInput.getIntegral_trxn_amt());
		billInfo.setTrxn_integral(cplInput.getTrxn_integral());
		
		//电话号码
		billInfo.setContact_phone(cplInput.getContact_phone());

		// 对手方信息加工
		trxnCounterparty(billInfo);

		// 其他特殊处理: CIMB
		// trxnDetailSpecialProcess(billInfo);

		// 登记账单: 技术冲正不登记账单， 原账单也会删除
		if (cplInput.getReversal_type() != E_REVERSALTYPE.TECHNOLOGY) {
			DpsBillDao.insert(billInfo);
		}

		// 本金变动信息推送决策中心
		DpOtherIobus.princChangeNotice(billInfo, subAccount);

		// 本金变动推送消息中心
		DpOtherIobus.sendMessageChange(billInfo, subAccount);

		bizlog.method(" DpAccounting.regBill end <<<<<<<<<<<<<<<");

		return billInfo;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月11日-下午3:05:50</li>
	 *         <li>功能说明：登记过息账单</li>
	 *         <li>补充说明：里面有对子账户信息表对象做修改，但是未更新子账户表数据库，需要在外面更新</li>
	 *         </p>
	 * @param cplIn
	 *            登记利息账单接口
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 */
	public static void registerOtherBill(DpRegInstBill cplIn, String acctNo, String subAcctNo) {

		bizlog.method(" DpAccounting.registerOtherBill begin >>>>>>>>>>>>>>>>");

		// 利息、利息税二者都为零直接返回
		if (CommUtil.compare(cplIn.getInst_withholding_tax(), BigDecimal.ZERO) == 0 && CommUtil.compare(cplIn.getInterest(), BigDecimal.ZERO) == 0) {

			return;
		}

		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		long SerialNo = subAcct.getData_version();

		/* 交易环境信息 */
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 取得公共运行区变量

		// 初始化存款账单表
		DpsBill billInfo = BizUtil.getInstance(DpsBill.class);

		/* 账单关键信息 */
		billInfo.setSub_acct_no(cplIn.getSub_acct_no()); // 子账号
		billInfo.setBack_value_date(CommUtil.nvl(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date())); // 起息日期
		billInfo.setShow_ind(cplIn.getShow_ind());// 是否显示标志
		billInfo.setTally_record_ind(E_YESORNO.NO); // 是否记账记录标志
		billInfo.setTrxn_ccy(subAcct.getCcy_code()); // 交易币种
		billInfo.setFee_code(null); // 费用代码
		billInfo.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
		billInfo.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
		billInfo.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
		billInfo.setSummary_code(cplIn.getSummary_code()); // 摘要代码
		billInfo.setSummary_name(ApSummaryApi.getText(cplIn.getSummary_code())); // 摘要描述
		billInfo.setTrxn_status(E_TRXNSTATUS.NORMAL); // TODO: 交易状态
		billInfo.setBal_after_trxn(null); // 交易后余额, 不用填交易后余额
		billInfo.setReceipt_seq(null); // 回单序号，不用填回单序号
		billInfo.setDd_td_ind(subAcct.getDd_td_ind());// 定活标志

		/* 交易主体信息 */
		billInfo.setCard_no(cplIn.getCard_no()); // 卡号
		billInfo.setAcct_no(cplIn.getAcct_no()); // 账号
		billInfo.setSub_acct_seq(cplIn.getSub_acct_seq()); // 子账户序号
		billInfo.setProd_id(subAcct.getProd_id()); // 产品编号
		billInfo.setAcct_name(subAcct.getSub_acct_name()); // 账户名称
		billInfo.setAcct_branch(subAcct.getSub_acct_branch()); // 账务机构
		billInfo.setAcct_branch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name()); // 账务机构名称
		billInfo.setCust_no(subAcct.getCust_no()); // 客户号
		billInfo.setCust_type(subAcct.getCust_type());// 客户类型
		billInfo.setVoch_type(null); // 凭证类型
		billInfo.setVoch_no(null); // 凭证号码

		/* 系统内实际交易对方信息 */
		billInfo.setOpp_acct_route(cplIn.getOpp_acct_route());
		billInfo.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		billInfo.setOpp_acct_no(cplIn.getOpp_acct_no());
		billInfo.setOpp_card_no("");
		billInfo.setOpp_branch_id(cplIn.getOpp_branch_id());
		billInfo.setOpp_trxn_amt(null);
		billInfo.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());

		// 登记公共交易字段
		billInfo.setExternal_scene_code(runEnvs.getExternal_scene_code());
		billInfo.setPayment_mode(runEnvs.getPayment_mode());
		billInfo.setTrxn_code(runEnvs.getTrxn_code()); // 交易码
		billInfo.setRecon_code(runEnvs.getRecon_code()); // 对账代码
		billInfo.setThird_party_date(runEnvs.getInitiator_date());
		billInfo.setTrxn_channel(runEnvs.getChannel_id()); // 交易渠道
		billInfo.setTrxn_date(runEnvs.getTrxn_date()); // 交易日期
		billInfo.setBusi_seq(runEnvs.getBusi_seq()); // 业务流水
		billInfo.setTrxn_seq(runEnvs.getTrxn_seq()); // 交易流水
		billInfo.setTrxn_branch(runEnvs.getTrxn_branch()); // 交易机构
		billInfo.setTrxn_teller(runEnvs.getTrxn_teller()); // 交易柜员
		billInfo.setTrxn_time(runEnvs.getComputer_time()); // 交易时间
		billInfo.setHost_date(runEnvs.getComputer_date()); // 主机日期

		/* 冲正信息 */
		billInfo.setReversal_ind(runEnvs.getReversal_ind());

		if (CommUtil.isNotNull(cplIn.getReversal_type())) {
			billInfo.setClear_accounts_ind(CommUtil.nvl(cplIn.getClear_accounts_ind(), E_YESORNO.NO));
			billInfo.setOriginal_trxn_date(cplIn.getOriginal_trxn_date());
			billInfo.setOriginal_busi_seq(cplIn.getOriginal_busi_seq());
			billInfo.setOriginal_trxn_seq(cplIn.getOriginal_trxn_seq());
		}

		/* 代理人或经办人信息 */
		billInfo.setAgent_doc_type(cplIn.getAgent_doc_type()); // 代理人证件类型
		billInfo.setAgent_doc_no(cplIn.getAgent_doc_no()); // 代理人证件号
		billInfo.setAgent_name(cplIn.getAgent_name()); // 代理人姓名
		billInfo.setAgent_country(cplIn.getAgent_country()); // 代理人国籍

		if (subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET) {
			billInfo.setDebit_credit(E_DEBITCREDIT.DEBIT); // 记账方向
		}
		else {
			billInfo.setDebit_credit(E_DEBITCREDIT.CREDIT); // 记账方向
		}

		billInfo.setTrxn_record_type(E_TRXNRECORDTYPE.INTEREST); // 交易明细类别
		billInfo.setSerial_no(++SerialNo); // 序号
		billInfo.setTrxn_amt(cplIn.getInterest().subtract(cplIn.getInst_withholding_tax())); // 净利息
		billInfo.setBal_after_trxn(null); // 交易后余额
		billInfo.setReceipt_seq(null); // 回单序号
		billInfo.setInterest_tax(cplIn.getInst_withholding_tax());
		billInfo.setInst_tax_rate(cplIn.getInst_tax_rate());
		billInfo.setVat_amt(BigDecimal.ZERO);
		billInfo.setVat_rate(BigDecimal.ZERO);
		billInfo.setAccounting_alias(subAcct.getAccounting_alias());

		// 对手方信息加工
		// trxnCounterparty(billInfo);

		// 交易记录特殊处理: CIMB
		// trxnDetailSpecialProcess(billInfo);

		// 登记账单
		if (!ApSystemParmApi.exists(DpBaseConst.INT_AND_TAX_BILL_MERGE)
				|| !CommUtil.equals(ApSystemParmApi.getValue(DpBaseConst.INT_AND_TAX_BILL_MERGE), E_YESORNO.YES.getValue())) {

			billInfo.setTrxn_amt(cplIn.getInterest()); // 利息
			billInfo.setBal_after_trxn(subAcct.getAcct_bal().add(subAcct.getAcct_float_bal()).add(cplIn.getInterest()));
			billInfo.setInterest_tax(BigDecimal.ZERO);
			billInfo.setInst_tax_rate(BigDecimal.ZERO);

			// 对手方
			billInfo.setOpp_acct_route(E_ACCOUTANALY.BUSINESE);
			billInfo.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
			billInfo.setOpp_acct_no("");
			billInfo.setOpp_card_no("");
			billInfo.setOpp_branch_id(cplIn.getOpp_branch_id());
			billInfo.setOpp_trxn_amt(cplIn.getInterest());
			billInfo.setOpp_sub_acct_seq("");

			DpsBillDao.insert(billInfo);

			if (!CommUtil.equals(cplIn.getInst_withholding_tax(), BigDecimal.ZERO)) {

				billInfo.setTrxn_record_type(E_TRXNRECORDTYPE.TAX);
				billInfo.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT);
				billInfo.setTrxn_amt(cplIn.getInst_withholding_tax()); // 利息税
				billInfo.setBal_after_trxn(subAcct.getAcct_bal().add(subAcct.getAcct_float_bal()).add(cplIn.getInterest()).subtract(cplIn.getInst_withholding_tax()));
				billInfo.setInst_tax_rate(cplIn.getInst_tax_rate());
				billInfo.setSerial_no(++SerialNo); // 序号

				DpsBillDao.insert(billInfo);
			}

			// 净利息不为零
			if (!CommUtil.equals(cplIn.getInterest().subtract(cplIn.getInst_withholding_tax()), BigDecimal.ZERO)) {

				billInfo.setTrxn_record_type(E_TRXNRECORDTYPE.INTEREST);
				billInfo.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT);
				billInfo.setTrxn_amt(cplIn.getInterest().subtract(cplIn.getInst_withholding_tax())); // 净利息
				billInfo.setBal_after_trxn(subAcct.getAcct_bal().add(subAcct.getAcct_float_bal())); // 交易后余额
				billInfo.setInst_tax_rate(BigDecimal.ZERO);
				billInfo.setSerial_no(++SerialNo); // 序号

				// 对手方
				billInfo.setOpp_acct_route(cplIn.getOpp_acct_route());
				billInfo.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
				billInfo.setOpp_acct_no(cplIn.getOpp_acct_no());
				billInfo.setOpp_card_no("");
				billInfo.setOpp_branch_id(cplIn.getOpp_branch_id());
				billInfo.setOpp_trxn_amt(null);
				billInfo.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());

				DpsBillDao.insert(billInfo);
			}
		}
		else {

			billInfo.setBal_after_trxn(subAcct.getAcct_bal().add(subAcct.getAcct_float_bal()).add(billInfo.getTrxn_amt())); // 交易后余额

			// 对手方为内部户
			billInfo.setOpp_acct_route(E_ACCOUTANALY.BUSINESE);
			billInfo.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
			billInfo.setOpp_acct_no("");
			billInfo.setOpp_card_no("");
			billInfo.setOpp_branch_id(cplIn.getOpp_branch_id());
			billInfo.setOpp_trxn_amt(cplIn.getInterest());
			billInfo.setOpp_sub_acct_seq("");

			DpsBillDao.insert(billInfo);

			// 记账方向反过来
			billInfo.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT);
			billInfo.setSerial_no(++SerialNo); // 序号
			billInfo.setBal_after_trxn(subAcct.getAcct_bal().add(subAcct.getAcct_float_bal())); // 交易后余额

			// 对手方可能为客户账
			billInfo.setOpp_acct_route(cplIn.getOpp_acct_route());
			billInfo.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
			billInfo.setOpp_acct_no(cplIn.getOpp_acct_no());
			billInfo.setOpp_card_no("");
			billInfo.setOpp_branch_id(cplIn.getOpp_branch_id());
			billInfo.setOpp_trxn_amt(null);
			billInfo.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());

			DpsBillDao.insert(billInfo);
		}

		// 已派利息和已扣利息税更新
		subAcct.setInst_paid(subAcct.getInst_paid().add(cplIn.getInterest()));
		subAcct.setInst_withholding_tax(subAcct.getInst_withholding_tax().add(cplIn.getInst_withholding_tax()));
		subAcct.setData_version(SerialNo);

		DpaSubAccountDao.updateOne_odb1(subAcct);

		/* 登记利息税结算信息 */
		DpSettleInstRegister settleInstReg = BizUtil.getInstance(DpSettleInstRegister.class);

		settleInstReg.setInst_tax_rate(cplIn.getInst_tax_rate());
		settleInstReg.setInterest(cplIn.getInterest());
		settleInstReg.setInterest_tax(cplIn.getInst_withholding_tax());
		settleInstReg.setTax_rate_code(subAcct.getTax_rate_code());

		DpInterestBasicApi.regInterestSettled(subAcct, settleInstReg);

		// 过息账单冲账
		DpOtherBillReversalIn reversalIn = BizUtil.getInstance(DpOtherBillReversalIn.class);

		reversalIn.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		reversalIn.setAcct_no(cplIn.getAcct_no());
		reversalIn.setSerial_no(SerialNo); // serial no
		reversalIn.setInterest(cplIn.getInterest());
		reversalIn.setInterest_tax(cplIn.getInst_withholding_tax());
		reversalIn.setOriginal_trxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		reversalIn.setOriginal_trxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

		ApReversalApi.register("deptOtherBill", reversalIn);

		bizlog.method(" DpAccounting.registerOtherBill end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年8月16日-下午3:05:50</li>
	 *         <li>功能说明：交易对手方信息加工</li>
	 *         </p>
	 * @param billInfo
	 *            账单信息
	 */
	public static void trxnCounterparty(DpsBill billInfo) {

		// 利息及税相关的记账记录
		// 金融若是入自身则它的对手方就登记为内部户,直接退出; 若是利息过账单则还是需要登记真实的收息账户
		if (billInfo.getTally_record_ind() == E_YESORNO.YES
				&& CommUtil.in(billInfo.getTrxn_record_type(), E_TRXNRECORDTYPE.INTEREST, E_TRXNRECORDTYPE.TAX, E_TRXNRECORDTYPE.STAMP_TAX)) {

			billInfo.setOpp_acct_route(E_ACCOUTANALY.INSIDE);
			billInfo.setOpp_acct_no("");
			billInfo.setOpp_acct_name("");
			billInfo.setOpp_acct_ccy(billInfo.getTrxn_ccy());
			billInfo.setOpp_trxn_amt(billInfo.getTrxn_amt());
			billInfo.setOpp_branch_id("");
			billInfo.setOpp_branch_name("");
			billInfo.setOpp_card_no("");
			billInfo.setOpp_sub_acct_no("");
			billInfo.setOpp_sub_acct_seq("");

			return;
		}

		// 没有传入对手方账户路由类型且现转标志为现金，则认为对手方为现金; 如果传了路由类型要以传入的为准
		if (billInfo.getCash_trxn_ind() == E_CASHTRXN.CASH && CommUtil.isNull(billInfo.getOpp_acct_route())) {
			billInfo.setOpp_acct_route(E_ACCOUTANALY.CASH);
		}

		// 现金的尾箱台账就是柜员号
		if (billInfo.getOpp_acct_route() == E_ACCOUTANALY.CASH) {

			billInfo.setOpp_acct_no(BizUtil.getTrxRunEnvs().getTrxn_teller());
			billInfo.setOpp_acct_ccy(CommUtil.nvl(billInfo.getOpp_acct_ccy(), billInfo.getTrxn_ccy()));
		}

		// 对手方为存款账户且对手方账号与对手方子账户序号都不为空时读取子账号填入
		if (billInfo.getOpp_acct_route() == E_ACCOUTANALY.DEPOSIT && CommUtil.isNotNull(billInfo.getOpp_acct_no()) && CommUtil.isNotNull(billInfo.getOpp_sub_acct_seq())) {

			DpaAccountRelate acctRelate = DpaAccountRelateDao.selectOne_odb1(billInfo.getOpp_acct_no(), billInfo.getOpp_sub_acct_seq(), false);

			if (CommUtil.isNotNull(acctRelate)) {
				billInfo.setOpp_sub_acct_no(acctRelate.getSub_acct_no());
			}
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>20187年8月17日-下午3:05:50</li>
	 *         <li>功能说明：交易明细特殊处理</li>
	 *         </p>
	 * @param billInfo
	 *            账单信息
	 */
	private static void trxnDetailSpecialProcess(DpsBill billInfo) {

		// 支付模式和费用代码确认
		DpPayModeAndChargeCode cplPayMode = DpEventCodeRelate.getPayModeAndChargeCode(billInfo.getOpp_acct_route(), billInfo.getOpp_acct_no(), billInfo.getFee_code(),
				billInfo.getDebit_credit());

		// 更新到账单信息中
		billInfo.setPayment_mode(cplPayMode.getPayment_mode());
		billInfo.setFee_code(cplPayMode.getChrg_code());

		// 检查生成的 event code 合不合法
		// DpEventCodeRelate.getAccountingEventCode(billInfo);
	}

}