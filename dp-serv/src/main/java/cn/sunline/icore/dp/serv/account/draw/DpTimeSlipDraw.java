package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpTimeInterestApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpTimeSlipBase.DpaTimeSlip;
import cn.sunline.icore.dp.base.tables.TabDpTimeSlipBase.DpaTimeSlipDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpTimeSlipBase.DpTimeSlipGeneralTrxnRegInput;
import cn.sunline.icore.dp.base.type.ComDpTimeSlipBase.DpTimeSlipInstTrxnRegister;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.interest.DpInterestSettlement;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpFicheAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpRegInstBill;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_INCOMEINTERESTOBJECTTYPE;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：定期卡片支取相关
 * </p>
 * 
 * @Author Liubx
 *         <p>
 *         <li>2018年12月24日-下午2:11:58</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTimeSlipDraw {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeSlipDraw.class);

	/**
	 * @Author Liubx
	 *         <p>
	 *         <li>2018年12月24日-下午2:11:58</li>
	 *         <li>功能说明：单张定期卡片提取服务</li>
	 *         </p>
	 * @param cplIn
	 *            记账服务输入接口
	 * @return 对手方记账金额
	 */
	public static BigDecimal ficheAccountingProcess(DpFicheAccountingIn cplIn) {
		bizlog.method(" DpTimeSlipDraw.ficheAccountingProcess begin >>>>>>>>>>>>>>>>");

		// 定位账户信息
		DpAcctAccessIn cplAccsIn = BizUtil.getInstance(DpAcctAccessIn.class);

		cplAccsIn.setAcct_no(cplIn.getAcct_no());
		cplAccsIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplAccsIn.setDd_td_ind(E_DEMANDORTIME.TIME);

		DpAcctAccessOut cplAccsOut = DpToolsApi.locateSingleSubAcct(cplAccsIn);

		// 带锁读取子户信息
		DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(cplAccsOut.getAcct_no(), cplAccsOut.getSub_acct_no(), true);

		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
			throw DpBase.E0017(cplIn.getAcct_no(), cplIn.getSub_acct_seq());
		}

		// 读取卡片信息
		DpaTimeSlip slipInfo = DpaTimeSlipDao.selectOne_odb1(cplAccsOut.getAcct_no(), cplIn.getFiche_no(), true);

		if (slipInfo.getAcct_status() == E_ACCTSTATUS.CLOSE) {
			throw DpBase.E0008(cplIn.getFiche_no());
		}

		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		Map<String, Object> mapObject = new HashMap<String, Object>();
		mapObject.put("trxn_date", BizUtil.getTrxRunEnvs().getTrxn_date());

		// 追加输入数据集
		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObject);

		// 本金
		BigDecimal prcpAmount = slipInfo.getAcct_bal();

		// 先卡片利息处理
		DpInstAccounting cplPayed = slipInterestProcess(cplIn, subAcct);

		// 上面卡片利息处理中卡片信息有修改，在本金账务处理前,利息处理之后需要重新查询一遍
		slipInfo = DpaTimeSlipDao.selectOne_odb1(subAcct.getAcct_no(), cplIn.getFiche_no(), true);

		// 再本金账务处理
		accountingProcess(cplIn, slipInfo, subAcct);

		bizlog.method(" DpTimeSlipDraw.ficheAccountingProcess end <<<<<<<<<<<<<<<<");
		return prcpAmount.add(cplPayed.getInterest()).subtract(cplPayed.getInterest_tax());
	}

	/**
	 * @Author Liubx
	 *         <p>
	 *         <li>2018年12月26日-下午6:51:11</li>
	 *         <li>功能说明：利息处理</li>
	 *         </p>
	 * @param ficheInst
	 * @param subAcct
	 */
	private static void accountingProcess(DpFicheAccountingIn cplIn, DpaTimeSlip slipInfo, DpaSubAccount subAcct) {

		BigDecimal trxnAmount = slipInfo.getAcct_bal();

		// 定期卡片记账处理
		DpTimeSlipGeneralTrxnRegInput cplTrxnIn = BizUtil.getInstance(DpTimeSlipGeneralTrxnRegInput.class);

		cplTrxnIn.setFiche_no(slipInfo.getFiche_no());
		cplTrxnIn.setBack_value_date(null);
		cplTrxnIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
		cplTrxnIn.setTrxn_amt(trxnAmount);

		DpTimeInterestApi.regTrxnDetail(cplTrxnIn);

		// 定期只户层记账
		DpUpdAccBalIn cplAccBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplAccBalIn.setCard_no(""); // 卡号
		cplAccBalIn.setAcct_no(subAcct.getAcct_no()); // 账号
		cplAccBalIn.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		cplAccBalIn.setTrxn_ccy(subAcct.getCcy_code()); // 交易币种
		cplAccBalIn.setTrxn_amt(trxnAmount); // 交易金额
		cplAccBalIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
		cplAccBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplAccBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplAccBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
		cplAccBalIn.setTrxn_event_id(E_DEPTTRXNEVENT.DP_TIME_DRAW.getValue());// 交易事件
		cplAccBalIn.setDebit_credit(E_DEBITCREDIT.DEBIT); // 记账方向
		cplAccBalIn.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_DAYEND_TIEMDRAW"));
		cplAccBalIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		cplAccBalIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		cplAccBalIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		cplAccBalIn.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		cplAccBalIn.setOpp_branch_id(cplIn.getOpp_branch_id());

		// 本金记账
		DpAccounting.online(cplAccBalIn);
	}

	/**
	 * @Author Liubx
	 *         <p>
	 *         <li>2018年12月26日-下午6:51:11</li>
	 *         <li>功能说明：定期卡片利息处理</li>
	 *         </p>
	 * @param ficheInst
	 * @param subAcct
	 */
	private static DpInstAccounting slipInterestProcess(DpFicheAccountingIn cplIn, DpaSubAccount subAcct) {
		bizlog.method(" DpTimeSlipDraw.slipInterestProcess begin >>>>>>>>>>>>>>>>");

		// 计算本次支取利息
		E_YESORNO intoWaitFlag = (cplIn.getInst_into_object_type() == E_INCOMEINTERESTOBJECTTYPE.WAIT) ? E_YESORNO.YES : E_YESORNO.NO;

		DpInstAccounting cplPayed = DpBaseServiceApi.timeSlipMaturePayInterest(subAcct.getAcct_no(), cplIn.getFiche_no(), intoWaitFlag);

		// 利息记账处理，注意可能为负数
		if (CommUtil.compare(cplPayed.getInterest(), BigDecimal.ZERO) != 0) {

			DpInterestSettlement.payInterestAccounting(cplPayed, subAcct);
		}

		// 经上述处理后利息不为零，利息转入对手方，登记利息账单过账信息
		if (CommUtil.compare(cplPayed.getInterest(), BigDecimal.ZERO) != 0) {

			DpRegInstBill instBill = BizUtil.getInstance(DpRegInstBill.class);

			instBill.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAcct.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
			instBill.setBack_value_date(null); // 倒起息日
			instBill.setReversal_type(null); // 冲正类型
			instBill.setAcct_no(subAcct.getAcct_no()); // 账号
			instBill.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
			instBill.setCcy_code(subAcct.getCcy_code()); // 货币代码
			instBill.setSub_acct_seq(CommUtil.nvl(cplIn.getSub_acct_seq(), subAcct.getSub_acct_seq())); // 子账户序号
			instBill.setInst_withholding_tax(cplPayed.getInterest_tax());
			instBill.setInterest(cplPayed.getInterest()); // 利息
			instBill.setCash_trxn_ind(E_CASHTRXN.TRXN); // 现转标志
			instBill.setShow_ind(E_YESORNO.YES); // 是否显示标志
			instBill.setSummary_code(ApSystemParmApi.getSummaryCode("DEPT_DAYEND_TIEMDRAW")); // 摘要代码
			instBill.setOpp_acct_route(cplIn.getOpp_acct_route()); // 对方账户路由
			instBill.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
			instBill.setOpp_acct_ccy(cplIn.getOpp_acct_ccy()); // 对方账户币种
			instBill.setOpp_branch_id(cplIn.getOpp_branch_id()); // 对方机构号
			instBill.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq()); // 对方机构号
			instBill.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注

			// 登记过息账单
			DpAccounting.registerOtherBill(instBill, subAcct.getAcct_no(), subAcct.getSub_acct_no());

			DpTimeSlipInstTrxnRegister cplSlipInstTrxnIn = BizUtil.getInstance(DpTimeSlipInstTrxnRegister.class);

			cplSlipInstTrxnIn.setInst_tax_rate(cplPayed.getInst_tax_rate());
			cplSlipInstTrxnIn.setInst_withholding_tax(cplPayed.getInterest_tax());
			cplSlipInstTrxnIn.setInterest(cplPayed.getInterest());
			cplSlipInstTrxnIn.setFiche_no(cplIn.getFiche_no());

			DpTimeInterestApi.regInterestTrxnDetail(cplSlipInstTrxnIn);
		}

		bizlog.method(" DpTimeSlipDraw.slipInterestProcess end <<<<<<<<<<<<<<<<");
		return cplPayed;
	}
}
