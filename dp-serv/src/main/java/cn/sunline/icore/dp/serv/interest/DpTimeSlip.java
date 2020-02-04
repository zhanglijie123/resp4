package cn.sunline.icore.dp.serv.interest;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.icore.ap.api.ApChannelApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfSave;
import cn.sunline.icore.dp.base.tables.TabDpTimeSlipBase.DpaTimeSlip;
import cn.sunline.icore.dp.base.tables.TabDpTimeSlipBase.DpaTimeSlipDao;
import cn.sunline.icore.dp.base.type.ComDpInterestTrxnBasic.DpProdInterestTrialIn;
import cn.sunline.icore.dp.base.type.ComDpInterestTrxnBasic.DpProdInterestTrialOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpTimeSlipDao;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpFicheTrxnDetail;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpFicheTrxnListIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpFicheTrxnListOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmartDepositFicheDetailIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmartDepositFicheDetailOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmartDepositFicheListIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmartDepositFicheListOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmartDepositFichePayInst;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmartDepositFichePayInstIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmartDepositFichePayInstList;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmartDepositFichePayInstOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmtFicheAccruedDtlOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmtFichePayedIntDtlIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeSlip.DpSmtFichePayedIntDtlOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_FICHESTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：定期卡片相关
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2019年6月4日-下午1:30:55</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTimeSlip {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeSlip.class);

	/**
	 * @Author Liubx
	 *         <p>
	 *         <li>2019年1月2日-下午6:57:30</li>
	 *         <li>功能说明：智能存款卡片列表查询</li>
	 *         </p>
	 * @param cplIn
	 *            卡片列表查询输入信息
	 * @return
	 */
	public static Options<DpSmartDepositFicheListOut> qrySmartDepositFicheList(DpSmartDepositFicheListIn cplIn) {
		bizlog.method(" DpSmartDepositAmt.qrySmartDepositFicheList begin >>>>>>>>>>>>>>>>");
		bizlog.debug("DpSmartDepositFicheListIn=[%s]", cplIn);

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());

		// 非柜面渠道只显示未关闭以及有利息的卡片
		if (!ApChannelApi.isCounter(BizUtil.getTrxRunEnvs().getChannel_id())) {
			cplIn.setAcct_status(E_ACCTSTATUS.NORMAL);
		}

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.TIME);

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.subAcctInquery(acctAccessIn);

		DpaInterest acctInst = DpaInterestDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, true);

		// 获取公共运行期变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String orgId = runEnvs.getBusi_org_id();// 取得法人代码
		Page<DpaTimeSlip> fichePage = SqlDpTimeSlipDao.selFixedFicheList(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), cplIn.getAcct_status(), orgId,
				runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);
		runEnvs.setTotal_count(fichePage.getRecordCount());

		// 返回页账户数据
		List<DpaTimeSlip> ficheList = fichePage.getRecords();

		// 智能卡片列表输出
		Options<DpSmartDepositFicheListOut> cplOut = new DefaultOptions<DpSmartDepositFicheListOut>();

		// 组织输出信息
		for (DpaTimeSlip fiche : ficheList) {

			DpSmartDepositFicheListOut ficheOut = BizUtil.getInstance(DpSmartDepositFicheListOut.class);

			ficheOut.setFiche_no(fiche.getFiche_no());
			ficheOut.setAcct_bal(fiche.getAcct_bal());
			ficheOut.setStart_inst_date(fiche.getStart_inst_date());
			ficheOut.setAcct_valid_date(fiche.getAcct_valid_date());
			ficheOut.setBal_update_date(fiche.getBal_update_date());
			ficheOut.setEfft_inrt(fiche.getEfft_inrt());
			ficheOut.setMin_remain_bal(fiche.getMin_remain_bal());
			ficheOut.setFirst_deposit_amt(fiche.getFirst_deposit_amt());
			ficheOut.setAcct_status(fiche.getAcct_status());
			ficheOut.setLast_pay_inst_date(acctInst.getLast_pay_inst_date());
			ficheOut.setNext_pay_inst_date(acctInst.getNext_pay_inst_date());

			if (CommUtil.equals(fiche.getAcct_valid_date(), fiche.getBal_update_date())) {

				ficheOut.setFiche_status(E_FICHESTATUS.MATURED);
			}
			else if (fiche.getAcct_status() == E_ACCTSTATUS.CLOSE) {

				ficheOut.setFiche_status(E_FICHESTATUS.CLOSE);
			}
			else {

				ficheOut.setFiche_status(E_FICHESTATUS.NORMAL);
			}

			cplOut.add(ficheOut);
		}

		bizlog.method(" DpSmartDepositAmt.qrySmartDepositFicheList end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Liubx
	 *         <p>
	 *         <li>2019年1月3日-上午9:47:03</li>
	 *         <li>功能说明：智能存款卡片详情查询</li>
	 *         </p>
	 * @param cplIn
	 *            卡片详情查询输入信息
	 * @return
	 */
	public static DpSmartDepositFicheDetailOut qrySmartDepositFicheDetail(DpSmartDepositFicheDetailIn cplIn) {
		bizlog.method(" DpSmartDepositAmt.qrySmartDepositFicheDetail begin >>>>>>>>>>>>>>>>");
		bizlog.debug("DpSmartDepositFicheDetailIn = [%s]", cplIn);

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getFiche_no(), DpBaseDict.A.fiche_no.getId(), DpBaseDict.A.fiche_no.getLongName());

		// 定位账号
		DpaAccount acctInfo = DpToolsApi.accountInquery(cplIn.getAcct_no(), cplIn.getAcct_type());

		// 查询定期卡片信息
		DpaTimeSlip fiche = DpaTimeSlipDao.selectOne_odb1(acctInfo.getAcct_no(), cplIn.getFiche_no(), false);
		if(fiche==null){
			throw DpErr.Dp.E0004(cplIn.getFiche_no());
		}
		// 子账户信息
		DpaSubAccount tdSubAcct = DpaSubAccountDao.selectOne_odb1(fiche.getAcct_no(), fiche.getSub_acct_no(), true);

		// 查询计息定义信息
		DpaInterest acctInst = DpaInterestDao.selectOne_odb1(acctInfo.getAcct_no(), fiche.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, true);

		BigDecimal pendInst = BigDecimal.ZERO;

		if (!CommUtil.equals(fiche.getAcct_bal(), BigDecimal.ZERO)) {

			// 卡片预期利息试算
			pendInst = ficheInstTrial(fiche, tdSubAcct);

			pendInst = pendInst.subtract(fiche.getInst_paid()).subtract(fiche.getAccrual_inst());
		}

		// 卡片详情输出
		DpSmartDepositFicheDetailOut cplOut = BizUtil.getInstance(DpSmartDepositFicheDetailOut.class);

		cplOut.setStart_inst_date(fiche.getStart_inst_date());
		cplOut.setAcct_bal(fiche.getAcct_bal());
		cplOut.setEfft_inrt(fiche.getEfft_inrt());
		cplOut.setMin_remain_bal(fiche.getMin_remain_bal());
		cplOut.setFirst_deposit_amt(fiche.getFirst_deposit_amt());
		cplOut.setAcct_status(fiche.getAcct_status());
		cplOut.setInrt_reset_method(fiche.getInrt_reset_method());
		cplOut.setInst_seg_ind(fiche.getInst_seg_ind());
		cplOut.setPay_inst_cyc(acctInst.getPay_inst_cyc());
		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		cplOut.setLast_pay_inst_date(acctInst.getLast_pay_inst_date());
		cplOut.setNext_pay_inst_date(acctInst.getNext_pay_inst_date());
		cplOut.setAccrual_inst(fiche.getAccrual_inst());
		cplOut.setAccrual_inst_rund(ApCurrencyApi.roundAmount(fiche.getCcy_code(), fiche.getAccrual_inst()));// 应付利息
																												// ksmir-560

		if (fiche.getAcct_status() == E_ACCTSTATUS.CLOSE && CommUtil.compare(fiche.getInst_payable(), BigDecimal.ZERO) != 0) {

			cplOut.setNext_pay_inst_date(CommUtil.nvl(acctInst.getNext_pay_inst_date(),
					BizUtil.calcDateByReference(acctInst.getPay_inst_ref_date(), trxnDate, acctInst.getPay_inst_cyc())));
			cplOut.setAccrual_inst(fiche.getInst_payable());
		}

		cplOut.setAccrual_sum_bal(fiche.getAccrual_sum_bal());
		cplOut.setAccrual_inst_tax(fiche.getAccrual_inst_tax());
		cplOut.setAccrual_inst_tax_rund(ApCurrencyApi.roundAmount(fiche.getCcy_code(), fiche.getAccrual_inst_tax()));// 应付利息
																														// ksmir-560
		cplOut.setInst_paid(fiche.getInst_paid());
		cplOut.setPending_accrual_interest(pendInst);

		bizlog.debug(" DpSmartDepositFicheDetailOut cplOut:[%s]", cplOut);
		bizlog.method(" DpSmartDepositAmt.qrySmartDepositFicheDetail end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Administrator
	 *         <p>
	 *         <li>2019年1月21日-下午4:06:37</li>
	 *         <li>功能说明：卡片利息试算</li>
	 *         </p>
	 * @param fiche
	 * @param tdSubAcct
	 * @return
	 */
	private static BigDecimal ficheInstTrial(DpaTimeSlip fiche, DpaSubAccount tdSubAcct) {

		BigDecimal pendInst = BigDecimal.ZERO;

		DpfSave prodSave = DpProductFactoryApi.getProdSaveCtrl(tdSubAcct.getProd_id(), tdSubAcct.getCcy_code());

		// 产品利息试算输入
		DpProdInterestTrialIn trialIn = BizUtil.getInstance(DpProdInterestTrialIn.class);

		trialIn.setProd_id(tdSubAcct.getProd_id());
		trialIn.setCcy_code(fiche.getCcy_code());
		trialIn.setInrt_code(fiche.getInrt_code());
		trialIn.setInrt_float_method(fiche.getInrt_float_method());
		trialIn.setInrt_float_value(fiche.getInrt_float_value());
		trialIn.setFirst_deposit_amt(fiche.getAcct_bal());
		trialIn.setEfft_inrt(fiche.getEfft_inrt());
		trialIn.setScheduled_dept_cycle(prodSave.getScheduled_dept_cycle());
		trialIn.setStart_inst_date(fiche.getStart_inst_date());
		trialIn.setDue_date(fiche.getAcct_valid_date());
		trialIn.setDraw_date(fiche.getAcct_valid_date());
		trialIn.setTerm_code(tdSubAcct.getTerm_code());
		// 存款产品利息试算
		DpProdInterestTrialOut trialOut = DpInterestBasicApi.prodInstTrialMain(trialIn);

		// 分计划的每一期就是一次卡片
		if (CommUtil.isNotNull(prodSave.getScheduled_dept_cycle())) {
			pendInst = trialOut.getList01().get(0).getInterest();
		}
		else {
			pendInst = trialOut.getInterest();
		}

		return pendInst;
	}

	public static void main(String[] args) {
		int[] years = new int[3];
		years[0] = 2019;
		years[1] = 2020;
		years[2] = 2021;
		// 闰年366、平年365天
		for (int i = 0; i < years.length; i++) {
			int year = years[i];
			if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
				System.out.println(years[i] + "=366");
			}
			else {
				System.out.println(years[i] + "=365");
			}
		}

		System.out.println(BizUtil.dateDiff("D", "20190219", BizUtil.calcDateByCycle("20190219", "2M")));
		System.out.println(BizUtil.dateDiff("D", "20190419", BizUtil.calcDateByCycle("20190419", "2M")));
		System.out.println(BizUtil.dateDiff("D", "20190619", BizUtil.calcDateByCycle("20190619", "2M")));
		System.out.println(BizUtil.dateDiff("D", "20190819", "20210219"));
	}

	/**
	 * @Author XJW
	 *         <p>
	 *         <li>2019年3月20日-上午11:23:30</li>
	 *         <li>功能说明：智能存款卡片账单列表查询</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpFicheTrxnListOut qryFicheTrxnList(DpFicheTrxnListIn cplIn) {
		bizlog.method(" DpSmartDepositAmt.qryFicheTrxnList begin >>>>>>>>>>>>>>>>");

		// 智能存款卡片账单列表输出
		DpFicheTrxnListOut cplOut = BizUtil.getInstance(DpFicheTrxnListOut.class);
		Options<DpFicheTrxnDetail> ficheTrxnDetail = new DefaultOptions<DpFicheTrxnDetail>();

		BigDecimal totalDebitAmt = BigDecimal.ZERO;
		BigDecimal totalCreditAmt = BigDecimal.ZERO;

		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		// 获取公共运行变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		Page<DpFicheTrxnDetail> ficheTrxnList = SqlDpTimeSlipDao.selFicheTrxnList(acctInfo.getAcct_no(), cplIn.getFiche_no(), cplIn.getStart_date(), cplIn.getEnd_date(),
				cplIn.getTrxn_status(), cplIn.getTrxn_seq(), cplIn.getMin_amt(), cplIn.getMax_amt(), cplIn.getDebit_credit(), runEnvs.getBusi_org_id(), runEnvs.getPage_start(),
				runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		if (CommUtil.isNotNull(ficheTrxnList)) {

			for (DpFicheTrxnDetail out : ficheTrxnList.getRecords()) {

				// 贷方金额
				if (out.getSave_or_withdrawal_ind() == E_SAVEORWITHDRAWALIND.SAVE) {
					totalCreditAmt = totalCreditAmt.add(out.getTrxn_amt());
				}
				else {
					totalDebitAmt = totalDebitAmt.add(out.getTrxn_amt());
				}
			}
		}

		runEnvs.setTotal_count(ficheTrxnList.getRecordCount());

		ficheTrxnDetail.setValues(ficheTrxnList.getRecords());

		cplOut.setTotal_credit_amt(totalCreditAmt);
		cplOut.setTotal_debit_amt(totalDebitAmt);
		cplOut.setList01(ficheTrxnDetail);

		bizlog.method(" DpSmartDepositAmt.qryFicheTrxnList end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Jss
	 *         <p>
	 *         <li>2019年3月20日-下午4:00:00</li>
	 *         <li>功能说明：智能存款卡片已付利息明细查询</li>
	 *         </p>
	 * @param cplIn
	 *            查询输入信息
	 * @return
	 */
	public static Options<DpSmtFichePayedIntDtlOut> qrySmtFichePayedIntDtl(DpSmtFichePayedIntDtlIn cplIn) {

		bizlog.method(" DpSmartDepositAmt.qrySmtFichePayedIntDtl begin >>>>>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 获取公共运行期变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String orgId = runEnvs.getBusi_org_id();// 取得法人代码

		Page<DpSmtFichePayedIntDtlOut> payedInt = SqlDpTimeSlipDao.selSmtFichePayedIntList(acctAccessOut.getAcct_no(), cplIn.getFiche_no(), acctAccessOut.getSub_acct_no(),
				cplIn.getInst_seq(), cplIn.getAcct_status(), cplIn.getRecord_status(), cplIn.getStart_date(), cplIn.getEnd_date(), orgId, runEnvs.getPage_start(),
				runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(payedInt.getRecordCount());

		// 返回页账户数据
		List<DpSmtFichePayedIntDtlOut> payedIntList = payedInt.getRecords();

		// 智能卡片列表输出
		Options<DpSmtFichePayedIntDtlOut> cplOut = new DefaultOptions<DpSmtFichePayedIntDtlOut>();

		cplOut.setValues(payedIntList);

		bizlog.method(" DpSmartDepositAmt.qrySmtFichePayedIntDtl end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author Jss
	 *         <p>
	 *         <li>2019年3月20日-下午5:00:00</li>
	 *         <li>功能说明：智能存款卡片应计利息列表查询</li>
	 *         </p>
	 * @param cplIn
	 *            查询输入信息
	 * @return
	 */
	public static Options<DpSmtFicheAccruedDtlOut> qrySmtFicheAccruedDtl(DpSmtFichePayedIntDtlIn cplIn) {

		bizlog.method(" DpSmartDepositAmt.qrySmtFicheAccruedDtl begin >>>>>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 获取公共运行期变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String orgId = runEnvs.getBusi_org_id();// 取得法人代码

		Page<DpSmtFicheAccruedDtlOut> accruedInt = SqlDpTimeSlipDao.selSmtFicheAccruedList(acctAccessOut.getAcct_no(), cplIn.getFiche_no(), orgId, 
				cplIn.getInst_seq(),  cplIn.getRecord_status(), cplIn.getStart_date(), cplIn.getEnd_date(),  runEnvs.getPage_start(),
				runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(accruedInt.getRecordCount());

		// 返回页账户数据
		List<DpSmtFicheAccruedDtlOut> accruedIntList = accruedInt.getRecords();

		// 智能卡片列表输出
		Options<DpSmtFicheAccruedDtlOut> cplOut = new DefaultOptions<DpSmtFicheAccruedDtlOut>();

		cplOut.setValues(accruedIntList);

		bizlog.method(" DpSmartDepositAmt.qrySmtFicheAccruedDtl end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * 
	 * @Author Lfl
	 *         <p>
	 *         <li>2019年12月26日-下午4:52:13</li>
	 *         <li>功能说明：定期卡片付息明细查询</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpSmartDepositFichePayInstOut qryPayInstInfoList(DpSmartDepositFichePayInstIn cplIn) {
		bizlog.method(" DpSmartDepositAmt.qryPayInstInfoList begin >>>>>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);
		
		//非空校验
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getFiche_no(), DpBaseDict.A.fiche_no.getId(), DpBaseDict.A.fiche_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getPay_inst_date(), DpBaseDict.A.pay_inst_date.getId(), DpBaseDict.A.pay_inst_date.getLongName());
		
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		DpaTimeSlip fiche = DpaTimeSlipDao.selectOne_odb1(acctInfo.getAcct_no(), cplIn.getFiche_no(), false);
		
		DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOne_odb1(fiche.getAcct_no(), fiche.getSub_acct_no(), true);
		
		// 获取公共运行期变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String orgId = runEnvs.getBusi_org_id();// 取得法人代码
		
		DpSmartDepositFichePayInst cplSubOut = SqlDpTimeSlipDao.selSmtFichePayInstInfo( subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(),orgId, cplIn.getPay_inst_date(),
				 false);
		
		// 智能卡片列表输出
		DpSmartDepositFichePayInstOut cplOut = BizUtil.getInstance(DpSmartDepositFichePayInstOut.class);
		if(cplSubOut!=null){
		cplOut.setInterest(cplSubOut.getInterest()); // 利息
		cplOut.setInterest_tax(cplSubOut.getInterest_tax()); // 利息税
		Page<DpSmartDepositFichePayInstList> payInst = SqlDpTimeSlipDao.selSmtFichePayInstList(fiche.getAcct_no(),fiche.getSub_acct_no(),orgId,cplIn.getPay_inst_date(), runEnvs.getPage_start(),
				runEnvs.getPage_size(), runEnvs.getTotal_count(), false);
			
		runEnvs.setTotal_count(payInst.getRecordCount());// 返回总记录数
		
		cplOut.getList01().addAll(payInst.getRecords());// 付息详细信息赋值
		}
		
		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpSmartDepositAmt.qryPayInstInfoList end <<<<<<<<<<<<<<<<");
		return cplOut;
	}
}
