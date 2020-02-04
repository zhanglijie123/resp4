package cn.sunline.icore.dp.serv.dayend;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpRateBasicApi;
import cn.sunline.icore.dp.base.api.DpTimeInterestApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRate;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRateDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpbPayInterestPlanDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfDraw;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfInterest;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfTdDrawInterest;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryIn;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryOut;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpProductParmBase.DpOpenProdBaseCheck;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpParmeterMart;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_DRAWTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTOPERATE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTTERMMETHOD;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_PAYINSTWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RATERESETWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RENEWSAVEWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.account.draw.DpTimeDraw;
import cn.sunline.icore.dp.serv.account.draw.DpWaitDrawInterest;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.interest.DpInterestAccounting;
import cn.sunline.icore.dp.serv.iobus.DpExchangeIobus;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.maintain.DpAccountingAlaisMaitain;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbRolloverBook;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbRolloverBookDao;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpRenewSaveTallyAideInfo;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAccountRouteInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpExchangeAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpInsideAccountingIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_ROLLTYPE;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_FOREXEXCHOBJECT;
import cn.sunline.icore.sys.type.EnumType.E_INRTDIRECTION;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：负债日终定期自动续存处理
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年4月20日-下午4:30:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTdRenewSave {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTdRenewSave.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：定期到期自动续存处理</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            定期子账号
	 */
	public static void tdAutoRenewSave(String acctNo, String subAcctNo) {

		bizlog.method(" DpTdRenewSave.tdAutoRenewSave begin >>>>>>>>>>>>>>>>");

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 外面已带锁查询，此处读缓存
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		DpaSubAccount oldSubAcct = BizUtil.clone(DpaSubAccount.class, subAcct);

		// 无到期日或还未到期的直接退出
		if (CommUtil.isNull(subAcct.getDue_date()) || CommUtil.compare(subAcct.getDue_date(), trxnDate) > 0) {
			bizlog.info("Sub-account no[%s] expiration date or not yet expired, can not rollover", subAcct.getSub_acct_no());
			return;
		}

		// 销户的直接退出
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
			bizlog.info("Sub-account[%s] has been closed, can not rollover", subAcct.getSub_acct_no());
			return;
		}

		// 续存方式为“不续存”直接退出
		if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.NONE) {
			bizlog.info("Sub account[%s] is not set to rollover", subAcct.getSub_acct_no());
			return;
		}

		// 获取定期产补充属性
		DpfTdDrawInterest tdProdInfo = DpProductFactoryApi.getProdTdDrawInstInfo(subAcct.getProd_id(), subAcct.getCcy_code(), true);

		// 不是到期销户，如果超过转存次数，则不做续存处理
		if (subAcct.getRenewal_method() != E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT && CommUtil.compare(tdProdInfo.getMax_roll_count(), 0L) > 0
				&& CommUtil.compare(subAcct.getRoll_no(), tdProdInfo.getMax_roll_count()) >= 0) {
			bizlog.info("Sub account[%s] beyond roll times limit", subAcct.getSub_acct_no());
			return;
		}

		// 加载数据区
		addBuffData(subAcct);

		// 获取续存金额
		BigDecimal renewSaveAmt = getRenewSaveAmount(subAcct);

		// 将交易机构设置为当前账户机构
		BizUtil.getTrxRunEnvs().setTrxn_branch(subAcct.getSub_acct_branch());

		// 流水重置
		BizUtil.resetTrxnSequence();

		// 检查续存合法性
		if (subAcct.getRenewal_method() != E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {
			checkRenewVaild(subAcct, renewSaveAmt, subAcct.getRenew_prod_id(), subAcct.getRenew_save_term());
		}

		// 转账处理
		principalPosting(subAcct);

		// 获取原账户利率
		DpaInterestRate oldRate = DpaInterestRateDao.selectOne_odb1(oldSubAcct.getAcct_no(), oldSubAcct.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, DpConst.START_SORT_VALUE, false);

		// 转入其他账户后定期户已销户，不用再重置账户信息
		if (subAcct.getRenewal_method() != E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {

			renewSaveReset(subAcct, BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 登记转续存登记簿
		resetRolloverBook(oldSubAcct, trxnDate, renewSaveAmt, subAcct.getRenewal_method(), oldRate.getEfft_inrt());

		bizlog.method(" DpTdRenewSave.tdAutoRenewSave end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年12月19日-下午17:32:15</li>
	 *         <li>功能说明：续存重置账户信息</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param vaildDate
	 *            生效日期
	 */
	public static void renewSaveReset(DpaSubAccount subAcct, String vaildDate) {

		String oldProd = subAcct.getProd_id();

		// 重置子账户信息
		resetSubAcctInfo(subAcct, vaildDate);

		// 重置子账户存入支取控制
		resetSaveDrawCtrl(subAcct, vaildDate);

		// 重置账户计息信息
		resetInterest(subAcct, oldProd, vaildDate);

		// 重置账户子户关系表
		resetAccountRelate(subAcct);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年12月19日-下午17:32:15</li>
	 *         <li>功能说明：获取续存金额</li>
	 *         </p>
	 * @param subAcct
	 * @return 续存金额
	 */
	public static BigDecimal getRenewSaveAmount(DpaSubAccount subAcct) {

		// 续存金额
		BigDecimal renewSaveAmt = BigDecimal.ZERO;

		if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.PRINCIPAL) {

			// 本金转存：利息转入收息账户
			renewSaveAmt = subAcct.getAcct_bal();
		}
		else if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.PRIN_INST) {

			// 利息税
			BigDecimal instTax = subAcct.getInst_tax_payable();

			// 续存后本金金额
			renewSaveAmt = subAcct.getAcct_bal().add(subAcct.getInst_payable()).subtract(instTax);

			// 检查定期户存入限制，不符合报错
			if (CommUtil.compare(renewSaveAmt, subAcct.getAcct_bal()) > 0) {
				DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_SAVE, null);
			}
		}
		else if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.PART_AMOUNT) {

			// 利息税
			BigDecimal instTax = subAcct.getInst_tax_payable();

			// 减少金额续存
			renewSaveAmt = subAcct.getAcct_bal().add(subAcct.getInst_payable()).subtract(instTax).subtract(subAcct.getRenew_save_amt());

			// 有利息需要入本金，检查定期户存入限制，不符合报错
			if (CommUtil.compare(subAcct.getInst_payable(), instTax) > 0) {
				DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_SAVE, null);
			}

			// 检查定期户自身账户支取限制，不符合报错
			if (CommUtil.compare(renewSaveAmt, subAcct.getAcct_bal()) < 0) {

				DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_DRAW, null);

				// 读取账户余额信息
				DpBalanceCalculateOut balInfo = DpToolsApi.getBalance(subAcct.getSub_acct_no(), subAcct.getAcct_no(), E_DRAWTYPE.COMMON);

				// 定期户自身可用余额不够扣减应截取金额
				if (CommUtil.compare(balInfo.getSelf_usable_bal().add(subAcct.getInst_payable()).subtract(instTax), subAcct.getAcct_bal().subtract(renewSaveAmt)) < 0) {
					throw DpBase.E0118(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
				}
			}
		}
		else if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.ADD_AMOUNT) {

			// 利息税
			BigDecimal instTax = subAcct.getInst_tax_payable();

			// 增加金额续存
			renewSaveAmt = subAcct.getAcct_bal().add(subAcct.getInst_payable()).subtract(instTax).add(subAcct.getRenew_save_amt());

			// 检查定期户自身存入限制, 本金账户支出在后面转账时检查
			DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_SAVE, null);
		}
		// 转入其他账户
		else if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {

			DpPublicCheck.checkSubAcctTrxnLimit(subAcct, E_DEPTTRXNEVENT.DP_DRAW, null);

			// 读取账户余额信息: 使用销户模式
			if (CommUtil.compare(subAcct.getAcct_bal(), BigDecimal.ZERO) > 0) {

				DpBalanceCalculateOut balInfo = DpToolsApi.getBalance(subAcct.getSub_acct_no(), subAcct.getAcct_no(), E_DRAWTYPE.CLOSE);

				// 定期户有金额冻结
				if (CommUtil.compare(balInfo.getSelf_usable_bal(), subAcct.getAcct_bal()) < 0) {
					bizlog.info("The sub-account [%s] is frozen and can not be dumped", subAcct.getSub_acct_no());
					throw DpBase.E0118(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
				}
			}

		}
		else {
			// 出现了新的情形，代码未开发，先报错
			throw APPUB.E0026(subAcct.getRenewal_method().getLongName(), subAcct.getRenewal_method().getValue());
		}

		// 返回续存金额
		return renewSaveAmt;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年12月11日-下午1:32:15</li>
	 *         <li>功能说明：重置账户子户关系表</li>
	 *         </p>
	 * @param subAcct
	 */
	private static void resetAccountRelate(DpaSubAccount subAcct) {
		bizlog.method(" DpTdRenewSave.resetAccountRelate begin >>>>>>>>>>>>>>>>");

		DpaAccountRelate accountRelate = DpaAccountRelateDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_seq(), false);

		subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), false);

		// 账户子户关系表产品与子账户产品不一致是重置
		if (!CommUtil.equals(subAcct.getProd_id(), accountRelate.getProd_id())) {

			accountRelate.setProd_id(subAcct.getProd_id());

			DpaAccountRelateDao.updateOne_odb1(accountRelate);
		}

		bizlog.method(" DpTdRenewSave.resetAccountRelate end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年9月12日-下午3:48:24</li>
	 *         <li>功能说明：登记转续存登记簿</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param trxnDate
	 *            交易日期
	 * @param renewSaveAmt
	 *            转存金额
	 * @param renewalMethod
	 * @param rate
	 */
	public static void resetRolloverBook(DpaSubAccount subAcct, String trxnDate, BigDecimal renewSaveAmt, E_RENEWSAVEWAY renewalMethod, BigDecimal rate) {
		bizlog.method(" DpTdRenewSave.resetRolloverBook begin >>>>>>>>>>>>>>>>");

		DpbRolloverBook rolloverBook = BizUtil.getInstance(DpbRolloverBook.class);

		E_ROLLTYPE rollType = null;

		if (renewalMethod == E_RENEWSAVEWAY.PRINCIPAL) {

			rollType = E_ROLLTYPE.C2;
		}
		else if (renewalMethod == E_RENEWSAVEWAY.PRIN_INST) {

			rollType = E_ROLLTYPE.C1;
		}
		else if (renewalMethod == E_RENEWSAVEWAY.PART_AMOUNT) {

			rollType = E_ROLLTYPE.C3;
		}
		else if (renewalMethod == E_RENEWSAVEWAY.ADD_AMOUNT) {

			rollType = E_ROLLTYPE.C4;
		}
		else if (renewalMethod == E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {

			rollType = E_ROLLTYPE.C5;
		}
		else {
			// 出现了新的情形，代码未开发，先报错
			throw APPUB.E0026(subAcct.getRenewal_method().getLongName(), subAcct.getRenewal_method().getValue());
		}

		rolloverBook.setSub_acct_no(subAcct.getSub_acct_no()); // sub account
																// number
		rolloverBook.setRoll_type(rollType); // rollover type
		rolloverBook.setSerial_no(subAcct.getData_version()); // serial no
		rolloverBook.setTrxn_date(trxnDate); // transaction date
		rolloverBook.setRoll_no(subAcct.getRoll_no()); // roll number
		rolloverBook.setCard_no(""); // card no
		rolloverBook.setAcct_no(subAcct.getAcct_no()); // account no
		rolloverBook.setSub_acct_seq(subAcct.getSub_acct_seq());
		rolloverBook.setCcy_code(subAcct.getCcy_code()); // currency code
		rolloverBook.setRoll_renew_amount(renewSaveAmt); // roll or renew amount
		rolloverBook.setInterest_tax(subAcct.getInst_tax_payable());
		rolloverBook.setIn_acct_no(subAcct.getPrin_trsf_acct()); // into account
		rolloverBook.setIn_ccy_code(subAcct.getPrin_trsf_acct_ccy());
		rolloverBook.setIn_sub_acct_seq(null); // into subaccount serial no.
		rolloverBook.setTrxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());

		rolloverBook.setAcct_bal(subAcct.getAcct_bal());// 账户余额
		rolloverBook.setStart_inst_date(subAcct.getStart_inst_date());// 起息日
		rolloverBook.setDue_date(subAcct.getDue_date());// 到期日
		rolloverBook.setInst_payable(subAcct.getInst_payable());// 待支付利息
		rolloverBook.setEfft_inrt(rate);// 账户利率
		rolloverBook.setTerm_code(subAcct.getTerm_code());// 存期
		rolloverBook.setRenew_save_term(subAcct.getRenew_save_term());// 续存存期

		DpbRolloverBookDao.insert(rolloverBook);
		bizlog.method(" DpTdRenewSave.resetRolloverBook end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月18日-下午1:16:29</li>
	 *         <li>功能说明：续存合法性检查</li>
	 *         </p>
	 * @param subAcct
	 *            子账号信息
	 * @param renewSaveAmt
	 *            续存金额
	 * @param renewProd
	 *            续存产品
	 * @param renewTerm
	 *            续存存期
	 */
	public static void checkRenewVaild(DpaSubAccount subAcct, BigDecimal renewSaveAmt, String renewProd, String renewTerm) {

		bizlog.method(" DpTdRenewSave.checkRenewVaild begin >>>>>>>>>>>>>>>>");

		if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {
			return;
		}
		// 获取子户产品基础信息
		DpfBase oldProdBaseInfo = DpProductFactoryApi.getProdBaseInfo(subAcct.getProd_id());

		// 检查协议产品标志
		if (oldProdBaseInfo.getAgree_prod_ind() == E_YESORNO.YES) {

			throw DpErr.Dp.E0381(subAcct.getProd_id());
		}

		// 产品开户控制检查：起存金额、存期、准入条件
		DpOpenProdBaseCheck cplOpenCheckIn = BizUtil.getInstance(DpOpenProdBaseCheck.class);

		cplOpenCheckIn.setCcy_code(subAcct.getCcy_code());
		cplOpenCheckIn.setDue_date(subAcct.getDue_date());
		cplOpenCheckIn.setOpen_acct_amt(subAcct.getAcct_bal());
		cplOpenCheckIn.setProd_id(subAcct.getProd_id());
		cplOpenCheckIn.setRenew_prod_id(renewProd);
		cplOpenCheckIn.setRenew_save_amt(renewSaveAmt);
		cplOpenCheckIn.setRenew_save_term(renewTerm);
		cplOpenCheckIn.setRenewal_method(subAcct.getRenewal_method());
		cplOpenCheckIn.setRenewl_pay_inst_cyc(subAcct.getRenewl_pay_inst_cyc());
		cplOpenCheckIn.setTerm_code(subAcct.getTerm_code());

		DpProductFactoryApi.checkProdOpenAcct(cplOpenCheckIn);

		bizlog.method(" DpTdRenewSave.checkRenewVaild end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：重置子账户信息</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void resetSubAcctInfo(DpaSubAccount subAcct, String trxnDate) {

		// 获取余额更新后的账户信息
		subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

		DpaSubAccount beforeSubAcct = BizUtil.clone(DpaSubAccount.class, subAcct);

		boolean changeProd = false;

		// 续存产品不为空,并且与子户产品不一致时,更换新产品
		if (CommUtil.isNotNull(subAcct.getRenew_prod_id()) && !CommUtil.equals(subAcct.getRenew_prod_id(), subAcct.getProd_id())) {

			DpfBase prodBaseInfo = DpProductFactoryApi.getProdBaseInfo(subAcct.getRenew_prod_id());

			DpfDraw prodDrawInfo = DpProductFactoryApi.getProdDrawCtrl(subAcct.getRenew_prod_id(), subAcct.getCcy_code());

			subAcct.setProd_id(subAcct.getRenew_prod_id());
			subAcct.setSpec_dept_type(prodBaseInfo.getSpec_dept_type());
			subAcct.setOverdraft_allow_ind(prodBaseInfo.getOverdraft_allow_ind()); // 准许透支标志
			subAcct.setAllow_hand_rate_ind(prodBaseInfo.getAllow_hand_rate_ind());
			subAcct.setUse_cheque_allow_ind(prodBaseInfo.getUse_cheque_allow_ind());
			subAcct.setMin_remain_bal(prodDrawInfo == null ? BigDecimal.ZERO : prodDrawInfo.getMin_remain_bal()); // 账户最小留存余额

			changeProd = true;
		}

		// 续存时改变存期
		if (CommUtil.isNotNull(subAcct.getRenew_save_term()) && !CommUtil.equals(subAcct.getRenew_save_term(), subAcct.getTerm_code())) {

			subAcct.setTerm_code(subAcct.getRenew_save_term());

			changeProd = true;
		}

		// 存期
		String term = subAcct.getTerm_code();
		// 存期单位
		String termUnit = term.substring(term.length() - 1);

		String value = ApBusinessParmApi.getValue("RENEWAL_DUE_REFER_INIT_DATE");

		String dueDate = null;

		if (!CommUtil.equals(termUnit, "D") && !CommUtil.equals(termUnit, "W") && CommUtil.equals(value, "Y")) {

			dueDate = BizUtil.calcDateByReference(subAcct.getStart_inst_date(), trxnDate, term);
		}
		else {

			dueDate = BizUtil.calcDateByCycle(trxnDate, term);
		}

		// 可能需要顺延
		dueDate = DpToolsApi.calcMatureDate(subAcct.getProd_id(), subAcct.getCcy_code(), dueDate, subAcct.getSub_acct_branch(), subAcct.getRenewal_method());

		subAcct.setStart_inst_date(trxnDate);
		subAcct.setDue_date(dueDate);

		Long rollNo = subAcct.getRoll_no();

		if (subAcct.getRenewal_method() != E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {

			rollNo++;
		}

		subAcct.setRoll_no(rollNo); // 更新转存序号

		// TODO:零头天数处理

		// 定位利息税代码
		String instTaxCode = ApRuleApi.getFirstResultByScene(DpConst.INTEREST_TAX_CODE_RULE_SCENE_CODE);

		subAcct.setTax_rate_code(instTaxCode);

		// 清空已派利息: 新的周期可能发生冲账，因此需要清空
		subAcct.setInst_paid(BigDecimal.ZERO);
		subAcct.setInst_withholding_tax(BigDecimal.ZERO);
		subAcct.setLast_prov_inst(BigDecimal.ZERO);
		subAcct.setNet_dept_amt(BigDecimal.ZERO);

		DpaSubAccountDao.updateOne_odb1(subAcct);

		// 修改子户之后,检查是否修改产品,有修改需要重新定位核算别名
		if (changeProd) {

			// 改变存期了，可能影响到核算别名，重新定位核算别名
			DpAccountingAlaisMaitain.modifyAccountingalias(subAcct, beforeSubAcct);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：重置子账户存入支取控制</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void resetSaveDrawCtrl(DpaSubAccount subAcct, String trxnDate) {

		// 获取余额更新后的账户信息
		subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

		// 支取控制
		subAcct.setAccm_withdrawal_count(0L); // 实际支取次数
		subAcct.setAccm_withdrawal_amt(BigDecimal.ZERO); // 实际支取次数

		// 存入控制
		subAcct.setAccm_dept_count(1L);
		subAcct.setAccm_dept_amt(subAcct.getAcct_bal());

		DpaSubAccountDao.updateOne_odb1(subAcct);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：重置计息信息</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param oldProdId
	 *            原产品号
	 */
	private static void resetInterest(DpaSubAccount subAcct, String oldProdId, String trxnDate) {

		// 获取余额更新后的账户信息
		subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

		// 正常利息信息: 定期账户是都需要计息的
		DpaInterest tblNormal = DpaInterestDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, true);

		// 重定价方式为下期重定价，不是合同利率，则需要做重定价处理
		if (tblNormal.getInrt_reset_method() == E_RATERESETWAY.NEXT && !CommUtil.in(tblNormal.getInrt_code_direction(), E_INRTDIRECTION.SIMPLE_CONT, E_INRTDIRECTION.LAYER_CONT)) {

			// 获取账户正常利率
			List<DpaInterestRate> listAcctRate = DpaInterestRateDao.selectAll_odb2(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, true);

			// 获取最新利率
			DpInrtQryIn cplInrtQryIn = BizUtil.getInstance(DpInrtQryIn.class);

			DpfInterest prodInst = DpProductFactoryApi.getProdInterestDefine(subAcct.getProd_id(), subAcct.getCcy_code(), E_INSTKEYTYPE.NORMAL, true);

			cplInrtQryIn.setInrt_code(prodInst.getInrt_code()); // 利率编号
			cplInrtQryIn.setCcy_code(subAcct.getCcy_code()); // 货币代码
			cplInrtQryIn.setTerm_code(prodInst.getInst_term_method() == E_INSTTERMMETHOD.ACCTTRERM ? subAcct.getTerm_code() : prodInst.getAppo_inrt_term()); // 存期
			cplInrtQryIn.setTrxn_date(trxnDate); // 交易日期
			cplInrtQryIn.setTrxn_amt(subAcct.getAcct_bal()); // 交易金额
			cplInrtQryIn.setStart_inst_date(subAcct.getStart_inst_date()); // 起息日期
			cplInrtQryIn.setEnd_date(trxnDate); // 截止日期
			cplInrtQryIn.setInst_rate_file_way(tblNormal.getInst_rate_file_way()); // 利率索引类型
			cplInrtQryIn.setInrt_float_method(listAcctRate.get(0).getInrt_float_method()); // 利率浮动方式
			cplInrtQryIn.setInrt_float_value(listAcctRate.get(0).getInrt_float_value()); // 利率浮动值
			cplInrtQryIn.setInrt_reset_method(tblNormal.getInrt_reset_method());

			// 利率查询
			DpInrtQryOut cplInrtQryOut = DpRateBasicApi.getInstRateInfo(cplInrtQryIn);

			// 更新账户利率
			DpInterestBasicApi.modifyAcctInrt(listAcctRate, cplInrtQryOut);

			tblNormal.setLast_inrt_renew_date(trxnDate); // 利率上次更新日
		}

		tblNormal.setStart_inst_date(trxnDate);
		tblNormal.setEnd_inst_date(CommUtil.isNull(subAcct.getRemnant_day_start_date()) ? subAcct.getDue_date() : BizUtil.dateAdd("day", subAcct.getRemnant_day_start_date(), -1));
		tblNormal.setAccrual_inst(BigDecimal.ZERO);
		tblNormal.setAccrual_inst_tax(BigDecimal.ZERO);
		tblNormal.setAccrual_sum_bal(BigDecimal.ZERO);
		tblNormal.setCur_term_inst(BigDecimal.ZERO);
		tblNormal.setCur_term_inst_tax(BigDecimal.ZERO);
		tblNormal.setCur_term_inst_sum_bal(BigDecimal.ZERO);
		tblNormal.setLast_inst_oper_type(E_INSTOPERATE.PAY); // 上次利息操作
		tblNormal.setInst_day(0L); // 计息天数
		tblNormal.setWait_deal_inst(BigDecimal.ZERO); // 应加应减利息
		tblNormal.setWait_deal_sum_bal(BigDecimal.ZERO);// 应加应减积数
		tblNormal.setNext_inrt_renew_date(null); // 利率下次更新日
		tblNormal.setLast_pay_inst_date(null); // 上次结息日

		// 续存产品不为空,并且与子户产品不一致时,更换新产品,需更新相应计息信息
		if (CommUtil.isNotNull(subAcct.getRenew_prod_id()) && !CommUtil.equals(oldProdId, subAcct.getProd_id())) {

			// 获取产品计息定义
			DpfInterest prodInst = DpProductFactoryApi.getProdInterestDefine(subAcct.getProd_id(), subAcct.getCcy_code(), E_INSTKEYTYPE.NORMAL, true);

			tblNormal.setInst_rate_file_way(prodInst.getInst_rate_file_way());
			tblNormal.setInrt_code(prodInst.getInrt_code());
			tblNormal.setInst_base(prodInst.getInst_base());
			tblNormal.setPay_inst_method(prodInst.getPay_inst_method());
			tblNormal.setPay_inst_cyc(prodInst.getPay_inst_cyc());
			tblNormal.setPay_inst_ref_date(prodInst.getPay_inst_ref_date());
			tblNormal.setIncome_acct_prior(prodInst.getIncome_acct_prior());
			tblNormal.setMin_inst_bal(prodInst.getMin_inst_bal());
			tblNormal.setInrt_reset_method(prodInst.getInrt_reset_method());
			tblNormal.setInst_seg_ind(prodInst.getInst_seg_ind());
		}

		if (tblNormal.getPay_inst_method() == E_PAYINSTWAY.FIX_CYCLE) {

			if (CommUtil.equals(tblNormal.getPay_inst_ref_date(), DpConst.CASE_DATE)) {
				tblNormal.setNext_pay_inst_date(BizUtil.calcDateByReference(tblNormal.getStart_inst_date(), trxnDate, tblNormal.getPay_inst_cyc()));// 下次结息日
			}
			else {
				tblNormal.setNext_pay_inst_date(BizUtil.calcDateByReference(tblNormal.getPay_inst_ref_date(), trxnDate, tblNormal.getPay_inst_cyc()));// 下次结息日
			}
		}
		else if (tblNormal.getPay_inst_method() == E_PAYINSTWAY.CHANGE_CYCLE) {

			// 先删除旧的付息周期计划
			DpbPayInterestPlanDao.delete_odb2(subAcct.getAcct_no(), subAcct.getSub_acct_no());

			// 重新继承付息周期计划
			DpInterestBasicApi.regPayInstCyclePlan(subAcct);

			// 更新付息计划
			DpTimeInterestApi.modifyPayCycle(tblNormal, subAcct, trxnDate);
		}

		// 更新计息定义表
		DpaInterestDao.updateOne_odb1(tblNormal);

		// 若有零头天数，还要处理零头天数
		if (CommUtil.isNotNull(subAcct.getRemnant_day_start_date())) {

			// 正常利息信息: 定期账户是都需要计息的
			DpaInterest tblRemant = DpaInterestDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.REMNANT, true);

			// 正常利率需要重定价，而且零头天数利率不是合同利率，则零头天数也需要做重定价处理
			if (tblNormal.getInrt_reset_method() == E_RATERESETWAY.NEXT
					&& !CommUtil.in(tblRemant.getInrt_code_direction(), E_INRTDIRECTION.SIMPLE_CONT, E_INRTDIRECTION.LAYER_CONT)) {

				// 获取账户零头天数利率
				List<DpaInterestRate> listAcctRate = DpaInterestRateDao.selectAll_odb2(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.REMNANT, true);

				// 获取最新利率
				DpInrtQryIn cplInrtQryIn = BizUtil.getInstance(DpInrtQryIn.class);

				cplInrtQryIn.setInrt_code(tblRemant.getInrt_code()); // 利率编号
				cplInrtQryIn.setCcy_code(subAcct.getCcy_code()); // 货币代码
				cplInrtQryIn.setTerm_code(tblRemant.getInst_term_method() == E_INSTTERMMETHOD.ACCTTRERM ? subAcct.getTerm_code() : tblRemant.getAppo_inrt_term()); // 存期
				cplInrtQryIn.setTrxn_date(trxnDate); // 交易日期
				cplInrtQryIn.setTrxn_amt(subAcct.getAcct_bal()); // 交易金额
				cplInrtQryIn.setStart_inst_date(subAcct.getStart_inst_date()); // 起息日期
				cplInrtQryIn.setEnd_date(trxnDate); // 截止日期
				cplInrtQryIn.setInst_rate_file_way(tblRemant.getInst_rate_file_way()); // 利率索引类型
				cplInrtQryIn.setInrt_float_method(listAcctRate.get(0).getInrt_float_method()); // 利率浮动方式
				cplInrtQryIn.setInrt_float_value(listAcctRate.get(0).getInrt_float_value()); // 利率浮动值
				cplInrtQryIn.setInrt_reset_method(tblRemant.getInrt_reset_method());

				// 利率查询
				DpInrtQryOut cplInrtQryOut = DpRateBasicApi.getInstRateInfo(cplInrtQryIn);

				// 更新账户利率
				DpInterestBasicApi.modifyAcctInrt(listAcctRate, cplInrtQryOut);

				tblRemant.setLast_inrt_renew_date(trxnDate); // 利率上次更新日
			}

			tblRemant.setStart_inst_date(subAcct.getRemnant_day_start_date());
			tblRemant.setEnd_inst_date(subAcct.getDue_date());
			tblRemant.setAccrual_inst(BigDecimal.ZERO);
			tblRemant.setAccrual_inst_tax(BigDecimal.ZERO);
			tblRemant.setAccrual_sum_bal(BigDecimal.ZERO);
			tblRemant.setCur_term_inst(BigDecimal.ZERO);
			tblRemant.setCur_term_inst_tax(BigDecimal.ZERO);
			tblRemant.setCur_term_inst_sum_bal(BigDecimal.ZERO);
			tblRemant.setLast_inst_oper_type(E_INSTOPERATE.PAY); // 上次利息操作
			tblRemant.setInst_day(0L); // 计息天数
			tblRemant.setWait_deal_inst(BigDecimal.ZERO); // 应加应减利息
			tblRemant.setWait_deal_sum_bal(BigDecimal.ZERO);// 应加应减积数
			tblRemant.setNext_inrt_renew_date(null); // 利率下次更新日
			tblRemant.setLast_pay_inst_date(null); // 上次结息日
			tblRemant.setNext_pay_inst_date(null); // 零头天数下次付息日为空

			DpaInterestDao.updateOne_odb1(tblRemant);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：加载数据缓存区</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void addBuffData(DpaSubAccount subAcct) {

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 查询账户信息
		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), true);

		// 加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(subAcct.getCust_no(), subAcct.getCust_type());

		DpParmeterMart cplIn = BizUtil.getInstance(DpParmeterMart.class);

		cplIn.setAcct_type(acctInfo.getAcct_type());
		cplIn.setCcy_code(subAcct.getCcy_code());
		cplIn.setProd_id(CommUtil.nvl(subAcct.getRenew_prod_id(), subAcct.getProd_id()));

		// 加载参数数据集
		DpToolsApi.addDataToParmBuffer(cplIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：续存本金记账</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void principalPosting(DpaSubAccount subAcct) {

		DpRenewSaveTallyAideInfo cplAideInfo = BizUtil.getInstance(DpRenewSaveTallyAideInfo.class);

		cplAideInfo.setCash_trxn_ind(E_CASHTRXN.TRXN);
		cplAideInfo.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_TIME_ROLLOVER"));

		principalPosting(subAcct, cplAideInfo);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：续存本金记账</li>
	 *         <li>补充说明：不做冻结等检查，检查逻辑在前面处理</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplAideInfo
	 *            续存记账辅助信息
	 */
	public static void principalPosting(DpaSubAccount subAcct, DpRenewSaveTallyAideInfo cplAideInfo) {

		// 本金转存
		if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.PRINCIPAL) {

			// 有待支取利息和收息账户，则尝试将待支取利息转入收息账户
			if (CommUtil.compare(subAcct.getInst_payable(), BigDecimal.ZERO) != 0) {

				if (E_CASHTRXN.CASH == cplAideInfo.getCash_trxn_ind()) {

					// 待支取利息支取
					DpInstAccounting cplInstInfo = DpWaitDrawInterest.accountingTally(subAcct);

					// 利息入现金
					DpInterestAccounting.instIntoCash(cplInstInfo, subAcct, cplAideInfo.getOpp_acct_ccy(), cplAideInfo.getCustomer_remark(), cplAideInfo.getTrxn_remark());
				}
				else if (CommUtil.isNotNull(subAcct.getIncome_inst_acct())) {

					// 待支取利息支取
					DpInstAccounting cplInstInfo = DpWaitDrawInterest.accountingTally(subAcct);

					// 转入指定账户
					DpInterestAccounting.instIntoAppointAcct(cplInstInfo, subAcct, subAcct.getIncome_inst_acct(), subAcct.getIncome_inst_ccy());
				}
			}
		}
		// 本息转存
		else if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.PRIN_INST) {

			if (CommUtil.compare(subAcct.getInst_payable(), BigDecimal.ZERO) != 0) {

				// 待支取利息支取
				DpInstAccounting cplInstInfo = DpWaitDrawInterest.accountingTally(subAcct);

				// 存入定期账户自身
				DpInterestAccounting.instIntoSelf(cplInstInfo, subAcct);
			}
		}
		// 部分本金转存
		else if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.PART_AMOUNT) {

			partPrcpRenew(subAcct, cplAideInfo);
		}
		// 添加本金转存
		else if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.ADD_AMOUNT) {

			addPrcpRenew(subAcct, cplAideInfo);
		}
		// 转入其他账户
		else if (subAcct.getRenewal_method() == E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {

			matureCloseAcct(subAcct, cplAideInfo);
		}
		else {
			// 出现了新的情形，代码未开发，先报错
			throw APPUB.E0026(subAcct.getRenewal_method().getLongName(), subAcct.getRenewal_method().getValue());
		}

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：部分本金续存记账</li>
	 *         <li>补充说明：不含检查，检查在前面处理</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplAideInfo
	 *            续存记账辅助信息
	 */
	private static void partPrcpRenew(DpaSubAccount subAcct, DpRenewSaveTallyAideInfo cplAideInfo) {

		// 先处理利息
		if (CommUtil.compare(subAcct.getInst_payable(), BigDecimal.ZERO) != 0) {

			// 待支取利息支取
			DpInstAccounting cplInstInfo = DpWaitDrawInterest.accountingTally(subAcct);

			// 存入定期账户自身
			DpInterestAccounting.instIntoSelf(cplInstInfo, subAcct);

			// 读取一下最新缓存
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);
		}

		// 多余本金金额从定期账户取出
		BigDecimal drawAmt = subAcct.getRenew_save_amt();
		BigDecimal prinTrstAmt = drawAmt;
		String prinTrsfCcy = CommUtil.nvl(subAcct.getPrin_trsf_acct_ccy(), subAcct.getCcy_code());

		if (CommUtil.compare(drawAmt, BigDecimal.ZERO) == 0 || CommUtil.compare(subAcct.getAcct_bal(), drawAmt) < 0) {
			return;
		}

		// 分析路由
		DpAccountRouteInfo oppRouteType = DpInsideAccountIobus.getAccountRouteInfo(subAcct.getPrin_trsf_acct(), E_CASHTRXN.TRXN);

		DpTimeDrawIn cplTimeDrawIn = BizUtil.getInstance(DpTimeDrawIn.class);

		cplTimeDrawIn.setAcct_no(subAcct.getAcct_no());
		cplTimeDrawIn.setCard_no(null);
		cplTimeDrawIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind());
		cplTimeDrawIn.setCcy_code(subAcct.getCcy_code());
		cplTimeDrawIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplTimeDrawIn.setOpp_acct_ccy(prinTrsfCcy);
		cplTimeDrawIn.setOpp_acct_no(subAcct.getPrin_trsf_acct());
		cplTimeDrawIn.setOpp_acct_route(oppRouteType.getAcct_analy());
		cplTimeDrawIn.setSummary_code(cplAideInfo.getSummary_code());
		cplTimeDrawIn.setTrxn_amt(drawAmt);
		cplTimeDrawIn.setTrxn_remark(cplAideInfo.getTrxn_remark());
		cplTimeDrawIn.setCustomer_remark(cplAideInfo.getCustomer_remark());

		// 定期支取记账
		DpTimeDraw.tallyAccounting(cplTimeDrawIn, subAcct.getAcct_no(), subAcct.getSub_acct_no());

		// 跨币种结售汇
		if (!CommUtil.equals(prinTrsfCcy, subAcct.getCcy_code())) {

			DpExchangeAccountingIn cplFxIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

			cplFxIn.setBuy_cash_ind(E_CASHTRXN.TRXN); // 买入现金标志
			cplFxIn.setBuy_ccy_code(subAcct.getCcy_code()); // 买入币种
			cplFxIn.setBuy_amt(drawAmt); // 买入金额
			cplFxIn.setSell_cash_ind(cplAideInfo.getCash_trxn_ind()); // 卖出现金标志
			cplFxIn.setSell_ccy_code(prinTrsfCcy); // 卖出币种
			cplFxIn.setSell_amt(null); // 卖出金额
			cplFxIn.setSummary_code(cplAideInfo.getSummary_code()); // 摘要代码
			cplFxIn.setCustomer_remark(cplAideInfo.getCustomer_remark()); // 客户备注
			cplFxIn.setTrxn_remark(cplAideInfo.getTrxn_remark()); // 交易备注
			cplFxIn.setBuy_acct_no(subAcct.getAcct_no());
			cplFxIn.setBuy_sub_acct_seq(subAcct.getSub_acct_seq());
			cplFxIn.setSell_acct_no(subAcct.getPrin_trsf_acct());
			cplFxIn.setForex_agree_price_id(cplAideInfo.getForex_agree_price_id());
			cplFxIn.setExch_rate(cplAideInfo.getExch_rate());
			cplFxIn.setExch_rate_path(cplAideInfo.getExch_rate_path());
			cplFxIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.CUSTOMER);

			// 外汇买卖中间服务
			DpExchangeAccountingOut cplFxOut = DpExchangeIobus.exchangeAccounting(cplFxIn);

			// 得到转换后的金额
			prinTrstAmt = cplFxOut.getSell_amt();
		}

		// 本金转入处理
		if (oppRouteType.getAcct_analy() == E_ACCOUTANALY.DEPOSIT || oppRouteType.getAcct_analy() == E_ACCOUTANALY.NOSTRO) {

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(subAcct.getPrin_trsf_acct());
			accessIn.setCcy_code(prinTrsfCcy);
			accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			// 带锁读本金转入账户
			DpaSubAccount prcpAcct = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

			DpUpdAccBalIn cplUpdBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

			cplUpdBalIn.setCard_no(""); // 卡号
			cplUpdBalIn.setAcct_no(prcpAcct.getAcct_no()); // 账号
			cplUpdBalIn.setSub_acct_no(prcpAcct.getSub_acct_no()); // 子账号
			cplUpdBalIn.setTrxn_event_id(""); // 交易事件ID
			cplUpdBalIn.setTrxn_ccy(prcpAcct.getCcy_code()); // 交易币种
			cplUpdBalIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind()); // 现转标志
			cplUpdBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
			cplUpdBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
			cplUpdBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
			cplUpdBalIn.setTrxn_amt(prinTrstAmt);
			cplUpdBalIn.setDebit_credit(prcpAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT); // 记账方向
			cplUpdBalIn.setSummary_code(cplAideInfo.getSummary_code()); // 摘要代码
			cplUpdBalIn.setTrxn_remark(cplAideInfo.getTrxn_remark());
			cplUpdBalIn.setCustomer_remark(cplAideInfo.getCustomer_remark());
			cplUpdBalIn.setOpp_acct_ccy(subAcct.getCcy_code());// 对方账户币种
			cplUpdBalIn.setOpp_acct_no(subAcct.getAcct_no());// 对方账户
			cplUpdBalIn.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_ACCOUTANALY.NOSTRO : E_ACCOUTANALY.DEPOSIT);// 对方账户路由

			// 本金账户存入
			DpAccounting.online(cplUpdBalIn);
		}
		else {

			DpInsideAccountingIn cplTaBookIn = BizUtil.getInstance(DpInsideAccountingIn.class);

			cplTaBookIn.setAcct_branch(BizUtil.getTrxRunEnvs().getTrxn_branch());
			cplTaBookIn.setAcct_no(subAcct.getPrin_trsf_acct());
			cplTaBookIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind());
			cplTaBookIn.setCcy_code(prinTrsfCcy);
			cplTaBookIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT);
			cplTaBookIn.setOpp_acct_ccy(subAcct.getCcy_code());
			cplTaBookIn.setOpp_acct_no(subAcct.getAcct_no());
			// TODO: cplTaBookIn.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());
			cplTaBookIn.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_ACCOUTANALY.NOSTRO : E_ACCOUTANALY.DEPOSIT);
			cplTaBookIn.setTrxn_amt(prinTrstAmt);
			cplTaBookIn.setSummary_code(cplAideInfo.getSummary_code());
			cplTaBookIn.setTrxn_remark(null);

			DpInsideAccountIobus.insideAccounting(cplTaBookIn);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：添加本金续存记账</li>
	 *         <li>补充说明：不含检查，检查在前面处理</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplAideInfo
	 *            续存记账辅助信息
	 */
	private static void addPrcpRenew(DpaSubAccount subAcct, DpRenewSaveTallyAideInfo cplAideInfo) {

		if (CommUtil.compare(subAcct.getInst_payable(), BigDecimal.ZERO) != 0) {

			// 待支取利息支取
			DpInstAccounting cplInstInfo = DpWaitDrawInterest.accountingTally(subAcct);

			// 存入定期账户自身
			DpInterestAccounting.instIntoSelf(cplInstInfo, subAcct);

			// 读取一下最新缓存
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);
		}

		BigDecimal addAmt = subAcct.getRenew_save_amt();
		String prinTrsfCcy = CommUtil.nvl(subAcct.getPrin_trsf_acct_ccy(), subAcct.getCcy_code());
		BigDecimal prinTrstAmt = addAmt;

		// 定位本金转入金额
		if (CommUtil.compare(addAmt, BigDecimal.ZERO) <= 0) {
			return;
		}

		// 跨币种结售汇
		if (!CommUtil.equals(prinTrsfCcy, subAcct.getCcy_code())) {

			DpExchangeAccountingIn cplFxIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

			cplFxIn.setBuy_cash_ind(cplAideInfo.getCash_trxn_ind()); // 买入现金标志
			cplFxIn.setBuy_ccy_code(prinTrsfCcy); // 买入币种
			cplFxIn.setBuy_amt(null); // 买入金额
			cplFxIn.setSell_cash_ind(E_CASHTRXN.TRXN); // 卖出现金标志
			cplFxIn.setSell_ccy_code(subAcct.getCcy_code()); // 卖出币种
			cplFxIn.setSell_amt(addAmt); // 卖出金额
			cplFxIn.setSummary_code(cplAideInfo.getSummary_code()); // 摘要代码
			cplFxIn.setCustomer_remark(cplAideInfo.getCustomer_remark()); // 客户备注
			cplFxIn.setTrxn_remark(cplAideInfo.getTrxn_remark()); // 交易备注
			cplFxIn.setBuy_acct_no(subAcct.getPrin_trsf_acct());
			cplFxIn.setSell_acct_no(subAcct.getAcct_no());
			cplFxIn.setSell_sub_acct_seq(subAcct.getSub_acct_seq());
			cplFxIn.setForex_agree_price_id(cplAideInfo.getForex_agree_price_id());
			cplFxIn.setExch_rate(cplAideInfo.getExch_rate());
			cplFxIn.setExch_rate_path(cplAideInfo.getExch_rate_path());
			cplFxIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.CUSTOMER);

			// 外汇买卖中间服务
			DpExchangeAccountingOut cplFxOut = DpExchangeIobus.exchangeAccounting(cplFxIn);

			// 得到转换后的金额
			prinTrstAmt = cplFxOut.getBuy_amt();
		}

		// 分析路由
		DpAccountRouteInfo oppRouteType = DpInsideAccountIobus.getAccountRouteInfo(subAcct.getPrin_trsf_acct(), E_CASHTRXN.TRXN);

		// 本金转出处理
		if (oppRouteType.getAcct_analy() == E_ACCOUTANALY.DEPOSIT) {

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(subAcct.getPrin_trsf_acct());
			accessIn.setCcy_code(prinTrsfCcy);
			accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			// 带锁读本金转入账户
			DpaSubAccount prcpAcct = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

			DpUpdAccBalIn cplUpdBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

			cplUpdBalIn.setCard_no(""); // 卡号
			cplUpdBalIn.setAcct_no(prcpAcct.getAcct_no()); // 账号
			cplUpdBalIn.setSub_acct_no(prcpAcct.getSub_acct_no()); // 子账号
			cplUpdBalIn.setTrxn_event_id(""); // 交易事件ID
			cplUpdBalIn.setTrxn_ccy(prcpAcct.getCcy_code()); // 交易币种
			cplUpdBalIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind()); // 现转标志
			cplUpdBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
			cplUpdBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
			cplUpdBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
			cplUpdBalIn.setTrxn_amt(prinTrstAmt);
			cplUpdBalIn.setDebit_credit(prcpAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT); // 记账方向
			cplUpdBalIn.setSummary_code(cplAideInfo.getSummary_code()); // 摘要代码
			cplUpdBalIn.setTrxn_remark(cplAideInfo.getTrxn_remark());
			cplUpdBalIn.setCustomer_remark(cplAideInfo.getCustomer_remark());
			cplUpdBalIn.setOpp_acct_ccy(subAcct.getCcy_code());// 对方账户币种
			cplUpdBalIn.setOpp_acct_no(subAcct.getAcct_no());// 对方账户
			cplUpdBalIn.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_ACCOUTANALY.NOSTRO : E_ACCOUTANALY.DEPOSIT);// 对方账户路由

			// 本金账户存入
			DpAccounting.online(cplUpdBalIn);
		}
		else {

			DpInsideAccountingIn cplTaBookIn = BizUtil.getInstance(DpInsideAccountingIn.class);

			cplTaBookIn.setAcct_branch(BizUtil.getTrxRunEnvs().getTrxn_branch());
			cplTaBookIn.setAcct_no(subAcct.getPrin_trsf_acct());
			cplTaBookIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind());
			cplTaBookIn.setCcy_code(prinTrsfCcy);
			cplTaBookIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT);
			cplTaBookIn.setOpp_acct_ccy(subAcct.getCcy_code());
			cplTaBookIn.setOpp_acct_no(subAcct.getAcct_no());
			// TODO: cplTaBookIn.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());
			cplTaBookIn.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_ACCOUTANALY.NOSTRO : E_ACCOUTANALY.DEPOSIT);
			cplTaBookIn.setTrxn_amt(prinTrstAmt);
			cplTaBookIn.setSummary_code(cplAideInfo.getSummary_code());
			cplTaBookIn.setTrxn_remark(null);

			DpInsideAccountIobus.insideAccounting(cplTaBookIn);
		}

		// 定期增加本金存入
		DpUpdAccBalIn cplUpdBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplUpdBalIn.setCard_no(""); // 卡号
		cplUpdBalIn.setAcct_no(subAcct.getAcct_no()); // 账号
		cplUpdBalIn.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		cplUpdBalIn.setTrxn_event_id(""); // 交易事件ID
		cplUpdBalIn.setTrxn_ccy(subAcct.getCcy_code()); // 交易币种
		cplUpdBalIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind()); // 现转标志
		cplUpdBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplUpdBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplUpdBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
		cplUpdBalIn.setTrxn_amt(addAmt); // 定期户追加本金
		cplUpdBalIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT); // 记账方向
		cplUpdBalIn.setSummary_code(cplAideInfo.getSummary_code()); // 摘要代码
		cplUpdBalIn.setTrxn_remark(cplAideInfo.getTrxn_remark());
		cplUpdBalIn.setCustomer_remark(cplAideInfo.getCustomer_remark());
		cplUpdBalIn.setOpp_acct_ccy(prinTrsfCcy);// 对方账户币种
		cplUpdBalIn.setOpp_acct_no(subAcct.getPrin_trsf_acct());// 对方账户
		cplUpdBalIn.setOpp_acct_route(oppRouteType.getAcct_analy());// 对方账户路由

		// 定期账户存入
		DpAccounting.online(cplUpdBalIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年4月20日-下午3:21:29</li>
	 *         <li>功能说明：到期销户记账</li>
	 *         <li>补充说明：不含检查，检查在前面处理</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplAideInfo
	 *            续存记账辅助信息
	 */
	private static void matureCloseAcct(DpaSubAccount subAcct, DpRenewSaveTallyAideInfo cplAideInfo) {

		// 待支取利息支取：在销户前处理
		if (CommUtil.compare(subAcct.getInst_payable(), BigDecimal.ZERO) > 0) {

			// 待支取利息账务
			DpInstAccounting cplInstInfo = DpWaitDrawInterest.accountingTally(subAcct);

			// 收息币种
			String incomeCcy = CommUtil.nvl(subAcct.getIncome_inst_ccy(), subAcct.getCcy_code());

			// 入收息账户
			DpInterestAccounting.instIntoAppointAcct(cplInstInfo, subAcct, subAcct.getIncome_inst_acct(), incomeCcy);

			// 读取一下最新缓存
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);
		}

		BigDecimal trxnAmt = subAcct.getAcct_bal();
		String prinTrsfCcy = CommUtil.nvl(subAcct.getPrin_trsf_acct_ccy(), subAcct.getCcy_code());

		// 分析路由
		DpAccountRouteInfo oppRouteType = DpInsideAccountIobus.getAccountRouteInfo(subAcct.getPrin_trsf_acct(), E_CASHTRXN.TRXN);

		DpTimeDrawIn cplTimeDrawIn = BizUtil.getInstance(DpTimeDrawIn.class);

		cplTimeDrawIn.setAcct_no(subAcct.getAcct_no());
		cplTimeDrawIn.setCard_no(null);
		cplTimeDrawIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind());
		cplTimeDrawIn.setCcy_code(subAcct.getCcy_code());
		cplTimeDrawIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplTimeDrawIn.setOpp_acct_ccy(prinTrsfCcy);
		cplTimeDrawIn.setOpp_acct_no(subAcct.getPrin_trsf_acct());
		cplTimeDrawIn.setOpp_acct_route(oppRouteType.getAcct_analy());
		cplTimeDrawIn.setSummary_code(cplAideInfo.getSummary_code());
		cplTimeDrawIn.setTrxn_amt(trxnAmt);
		cplTimeDrawIn.setTrxn_remark(cplAideInfo.getTrxn_remark());
		cplTimeDrawIn.setCustomer_remark(cplAideInfo.getCustomer_remark());

		// 定期支取记账
		DpTimeDraw.tallyAccounting(cplTimeDrawIn, subAcct.getAcct_no(), subAcct.getSub_acct_no());

		subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

		// 关闭定期子户
		DpCloseSubAccountIn closeSubAcctIn = BizUtil.getInstance(DpCloseSubAccountIn.class);

		closeSubAcctIn.setClose_acct_reason(ApSummaryApi.getText(cplAideInfo.getSummary_code()));
		closeSubAcctIn.setSub_acct_seq(subAcct.getSub_acct_seq());

		DpBaseServiceApi.closeSubAcct(subAcct, closeSubAcctIn);

		// 不是0金额支取销户则有对手方账务处理
		if (CommUtil.compare(trxnAmt, BigDecimal.ZERO) > 0) {

			// 跨币种则先结售汇
			if (!CommUtil.equals(prinTrsfCcy, subAcct.getCcy_code())) {

				DpExchangeAccountingIn cplFxIn = BizUtil.getInstance(DpExchangeAccountingIn.class);

				cplFxIn.setBuy_cash_ind(E_CASHTRXN.TRXN); // 买入现金标志
				cplFxIn.setBuy_ccy_code(subAcct.getCcy_code()); // 买入币种
				cplFxIn.setBuy_amt(trxnAmt); // 买入金额
				cplFxIn.setSell_cash_ind(cplAideInfo.getCash_trxn_ind()); // 卖出现金标志
				cplFxIn.setSell_ccy_code(prinTrsfCcy); // 卖出币种
				cplFxIn.setSell_amt(null); // 卖出金额
				cplFxIn.setSummary_code(cplAideInfo.getSummary_code()); // 摘要代码
				cplFxIn.setCustomer_remark(""); // 客户备注
				cplFxIn.setTrxn_remark(""); // 交易备注
				cplFxIn.setBuy_acct_no(subAcct.getAcct_no());
				cplFxIn.setBuy_sub_acct_seq(subAcct.getSub_acct_seq());
				cplFxIn.setSell_acct_no(subAcct.getPrin_trsf_acct());
				cplFxIn.setExch_rate_path(cplAideInfo.getExch_rate_path());
				cplFxIn.setForex_exch_object_type(E_FOREXEXCHOBJECT.CUSTOMER);

				DpExchangeAccountingOut cplFxOut = DpExchangeIobus.exchangeAccounting(cplFxIn);

				// 得到转换后的金额
				trxnAmt = cplFxOut.getSell_amt();
			}

			// 本金转入处理
			if (oppRouteType.getAcct_analy() == E_ACCOUTANALY.DEPOSIT) {

				DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

				accessIn.setAcct_no(subAcct.getPrin_trsf_acct());
				accessIn.setCcy_code(prinTrsfCcy);
				accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

				DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

				// 带锁读本金转入账户
				DpaSubAccount prcpAcct = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

				DpUpdAccBalIn cplUpdBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

				cplUpdBalIn.setCard_no(""); // 卡号
				cplUpdBalIn.setAcct_no(prcpAcct.getAcct_no()); // 账号
				cplUpdBalIn.setSub_acct_no(prcpAcct.getSub_acct_no()); // 子账号
				cplUpdBalIn.setTrxn_event_id(""); // 交易事件ID
				cplUpdBalIn.setTrxn_ccy(prcpAcct.getCcy_code()); // 交易币种
				cplUpdBalIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind()); // 现转标志
				cplUpdBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
				cplUpdBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
				cplUpdBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
				cplUpdBalIn.setTrxn_amt(trxnAmt);
				cplUpdBalIn.setDebit_credit(prcpAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT); // 记账方向
				cplUpdBalIn.setSummary_code(cplAideInfo.getSummary_code()); // 摘要代码
				cplUpdBalIn.setTrxn_remark(cplAideInfo.getTrxn_remark());
				cplUpdBalIn.setCustomer_remark(cplAideInfo.getCustomer_remark());
				cplUpdBalIn.setOpp_acct_ccy(subAcct.getCcy_code());// 对方账户币种
				cplUpdBalIn.setOpp_acct_no(subAcct.getAcct_no());// 对方账户
				cplUpdBalIn.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_ACCOUTANALY.NOSTRO : E_ACCOUTANALY.DEPOSIT);// 对方账户路由

				// 本金账户存入
				DpAccounting.online(cplUpdBalIn);
			}
			else {

				DpInsideAccountingIn cplTaBookIn = BizUtil.getInstance(DpInsideAccountingIn.class);

				cplTaBookIn.setAcct_branch(BizUtil.getTrxRunEnvs().getTrxn_branch());
				cplTaBookIn.setAcct_no(subAcct.getPrin_trsf_acct());
				cplTaBookIn.setCash_trxn_ind(cplAideInfo.getCash_trxn_ind());
				cplTaBookIn.setCcy_code(prinTrsfCcy);
				cplTaBookIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT);
				cplTaBookIn.setOpp_acct_ccy(subAcct.getCcy_code());
				cplTaBookIn.setOpp_acct_no(subAcct.getAcct_no());
				// TODO:
				// cplTaBookIn.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());
				cplTaBookIn.setOpp_acct_route(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET ? E_ACCOUTANALY.NOSTRO : E_ACCOUTANALY.DEPOSIT);
				cplTaBookIn.setTrxn_amt(trxnAmt);
				cplTaBookIn.setSummary_code(cplAideInfo.getSummary_code());
				cplTaBookIn.setTrxn_remark(null);

				DpInsideAccountIobus.insideAccounting(cplTaBookIn);
			}
		}
	}
}