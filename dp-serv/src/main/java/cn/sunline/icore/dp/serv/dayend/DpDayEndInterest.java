package cn.sunline.icore.dp.serv.dayend;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpOverDraftApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlipDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraft;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlipDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtChangeInfo;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_DRAWTYPE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.fundpool.DpOverdraftSettlement;
import cn.sunline.icore.dp.serv.interest.DpInterestSettlement;
import cn.sunline.icore.dp.serv.iobus.DpCreditLimitIobus;
import cn.sunline.icore.dp.serv.iobus.DpInterestRateIobus;
import cn.sunline.icore.dp.serv.servicetype.SrvDpDemandAccounting;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpDemandAccounting.DpDemandDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitInfo;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_LIMITSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_ROUNDRULE;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明：负债日终利息利率相关处理主文件
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月16日-下午4:30:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpDayEndInterest {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDayEndInterest.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月17日-下午3:21:29</li>
	 *         <li>功能说明：存款利息利率处理: 计提、结息、利率重定价</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息: 外面已经带锁
	 */
	public static void DealInterest(DpaSubAccount subAcct, List<DpInrtChangeInfo> listRateChange) {

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 当天已经计提过的不再处理
		if (CommUtil.compare(subAcct.getNext_inst_date(), trxnDate) > 0) {
			return;
		}

		// (一) 透支账户计息
		if (subAcct.getOverdraft_allow_ind() == E_YESORNO.YES) {

			DpOverDraftApi.accruedOdInterest(subAcct);
		}

		// (二) 存款利息计息
		DpInterestBasicApi.accruedInterest(subAcct.getAcct_no(), subAcct.getSub_acct_no());

		// (三) 存款结息
		DpInterestSettlement.settleInterest(subAcct.getAcct_no(), subAcct.getSub_acct_no());

		// (四)存款利率重定价
		DpInterestBasicApi.afreshInterestRate(subAcct.getAcct_no(), subAcct.getSub_acct_no(), listRateChange);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月8日-下午2:01:29</li>
	 *         <li>功能说明：日切期间补计提利息</li>
	 *         </p>
	 * @param subAcct
	 *            子账户
	 */
	public static void supplementAccruedInterest(DpaSubAccount subAcct) {

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 若当天未计提则补计提
		if (CommUtil.compare(subAcct.getNext_inst_date(), trxnDate) <= 0) {

			if (subAcct.getOverdraft_allow_ind() == E_YESORNO.YES) {

				DpOverDraftApi.accruedOdInterest(subAcct);
			}

			DpInterestBasicApi.accruedInterest(subAcct.getAcct_no(), subAcct.getSub_acct_no());
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年7月27日-下午2:01:29</li>
	 *         <li>功能说明：联机处理利息</li>
	 *         <li>使用说明：适用于日切后计息前账户联机交易涉及到利息时调用：活期销户、定期支取</li>
	 *         </p>
	 * @param subAcct
	 *            子账户:处理之后在外面要再次读取
	 */
	public static void onlineDealInterest(DpaSubAccount subAcct) {

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		String channelId = BizUtil.getTrxRunEnvs().getChannel_id();

		// 当天已经计提过的不再处理
		if (CommUtil.compare(subAcct.getNext_inst_date(), trxnDate) > 0) {
			return;
		}

		// 先将渠道暂时设为日终批量渠道，这样可以避免里面登记冲账事件
		BizUtil.getTrxRunEnvs().setChannel_id(ApConst.SYSTEM_BATCH);

		Options<DpInrtChangeInfo> listInrtChange = DpInterestRateIobus.getInrtCodeChangeList(trxnDate);

		// 处理利息计提、存款结息、存款利率重定价： 联机处理定期账户的利息不能入自身，避免因余额变动导致开始分析的支取类型不对
		DealInterest(subAcct, listInrtChange);

		// 重新再读一遍
		subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

		// 透支結息
		settleOdInterest(subAcct);

		// 透支利率重定价
		DpOverDraftApi.afreshOverDraftRate(subAcct, listInrtChange);

		// 渠道还原
		BizUtil.getTrxRunEnvs().setChannel_id(channelId);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年8月7日-下午3:21:29</li>
	 *         <li>功能说明：透支利息利率处理: 计提、结息、利率重定价</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息: 外面已经带锁
	 */
	public static void settleOdInterest(DpaSubAccount subAcct) {

		if (subAcct.getOverdraft_allow_ind() != E_YESORNO.YES) {
			return;
		}

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 查询账户下透支协议
		List<DpbOverdraft> listODAgree = DpbOverdraftDao.selectAll_odb5(subAcct.getAcct_no(), subAcct.getCcy_code(), false);

		if (CommUtil.isNull(listODAgree) || listODAgree.size() == 0) {
			bizlog.debug("Account[%s] without overdraft information record", subAcct.getSub_acct_no());
			return;
		}

		bizlog.method(" DpDayEndInterest.settlementOdInterest begin >>>>>>>>>>>>>>>>");

		// 循环处理账户下透支协议
		for (DpbOverdraft ODAgree : listODAgree) {

			if (ODAgree.getAgree_status() == E_STATUS.INVALID || CommUtil.compare(ODAgree.getNext_pay_inst_date(), trxnDate) > 0) {
				continue;
			}

			List<DpbOverdraftSlip> listODFiche = DpbOverdraftSlipDao.selectAll_odb3(ODAgree.getAcct_no(), ODAgree.getAgree_no(), true);

			// 单个透支协议结息: 一定是周期性结息
			overDraftSettlementInst(subAcct, ODAgree, listODFiche);
		}

		bizlog.method(" DpDayEndInterest.settlementOdInterest end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年8月7日-下午4:21:53</li>
	 *         <li>功能说明：透支账户还息账务处理</li>
	 *         </p>
	 * @param listFiche
	 *            卡片计息列表信息
	 * @param subAcct
	 *            子账户信息
	 */
	private static void overDraftSettlementInst(DpaSubAccount subAcct, DpbOverdraft ODAgree, List<DpbOverdraftSlip> listODFiche) {

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		if (CommUtil.compare(ODAgree.getNext_pay_inst_date(), trxnDate) > 0) {
			return;
		}

		// 更新下次付息日
		String nextPayInstDate = "";

		if (CommUtil.equals(ODAgree.getPay_inst_ref_date(), DpConst.CASE_DATE)) {
			nextPayInstDate = BizUtil.calcDateByReference(ODAgree.getSign_date(), trxnDate, ODAgree.getPay_inst_cyc());
		}
		else {
			nextPayInstDate = BizUtil.calcDateByReference(ODAgree.getPay_inst_ref_date(), trxnDate, ODAgree.getPay_inst_cyc());
		}

		ODAgree.setNext_pay_inst_date(nextPayInstDate);
		ODAgree.setLast_pay_inst_date(trxnDate);

		DpbOverdraftDao.updateOne_odb1(ODAgree);

		E_ROUNDRULE roundRule = E_ROUNDRULE.ROUND;
		BigDecimal instValue = BigDecimal.ZERO; // 利息

		for (DpbOverdraftSlip ODFiche : listODFiche) {

			DpaSlip ficheInst = DpaSlipDao.selectOne_odb1(ODAgree.getAcct_no(), ODFiche.getFiche_no(), true);

			if (ficheInst.getAcct_status() == E_ACCTSTATUS.CLOSE) {
				continue;
			}

			// 累计利息信息: 透支利息 + 罚息
			instValue = instValue.add(ApCurrencyApi.roundAmount(ficheInst.getCcy_code(), ficheInst.getAccrual_inst().add(ODFiche.getOverdue_interest()), roundRule));
		}

		// 无透支利息则退出
		if (CommUtil.compare(instValue, BigDecimal.ZERO) <= 0) {
			return;
		}

		DpInstAccounting cplWaitDealInst = BizUtil.getInstance(DpInstAccounting.class);

		cplWaitDealInst.setInterest(instValue);
		cplWaitDealInst.setInterest_tax(BigDecimal.ZERO);

		// 透支利息收息处理
		DpInstAccounting cplODInstInfo = incomeODInst(subAcct, cplWaitDealInst, ODAgree);

		if (CommUtil.isNull(cplODInstInfo) || CommUtil.isNull(cplODInstInfo.getInterest()) || CommUtil.equals(cplODInstInfo.getInterest(), BigDecimal.ZERO)) {
			return;
		}

		// TODO:暂不用考虑余额不足部分结息的情况
		for (DpbOverdraftSlip ODFiche : listODFiche) {

			DpaSlip ficheInst = DpaSlipDao.selectOne_odb1(ODAgree.getAcct_no(), ODFiche.getFiche_no(), true);

			if (ficheInst.getAcct_status() == E_ACCTSTATUS.CLOSE) {
				continue;
			}

			DpOverdraftSettlement.dealFicheInterestForDayEnd(ficheInst, roundRule);

			// 更新卡片信息
			DpaSlipDao.updateOne_odb1(ficheInst);

			if (CommUtil.compare(ODFiche.getOverdue_interest(), BigDecimal.ZERO) > 0) {

				ODFiche.setOverdue_interest(BigDecimal.ZERO);

				DpbOverdraftSlipDao.updateOne_odb1(ODFiche);
			}
		}

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年11月19日-下午2:01:29</li>
	 *         <li>功能说明：单个协议透支利息收息处理</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param cplWaitDealInst
	 *            透支利息信息
	 * @param ODAgree
	 *            透支协议
	 * @return 实际收取利息
	 */
	private static DpInstAccounting incomeODInst(DpaSubAccount subAcct, DpInstAccounting cplWaitDealInst, DpbOverdraft ODAgree) {

		// 应收利息小于等于零直接退出
		if (CommUtil.compare(cplWaitDealInst.getInterest(), BigDecimal.ZERO) <= 0) {
			return null;
		}

		bizlog.method(" DpOverDraftDayEnd.incomeODInst begin >>>>>>>>>>>>>>>>");

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 查询额度信息
		DpCreditLimitInfo cplLimitInfo = DpCreditLimitIobus.getCreditLimitInfo(ODAgree.getLimit_code());

		// 额度已到期，账户本身余额足够则结息，不使用其他透支协议的额度来结息，关联保护可用用
		// 新需求，可使用其余正生效协议结清
		if (cplLimitInfo.getStatus() == E_LIMITSTATUS.EXPIRED || (CommUtil.isNotNull(cplLimitInfo.getDue_date()) && CommUtil.compare(cplLimitInfo.getDue_date(), trxnDate) <= 0)) {

			// 查询余额信息
			DpBalanceCalculateOut cplBalInfo = DpToolsApi.getBalance(subAcct.getSub_acct_no(), subAcct.getAcct_no(), E_DRAWTYPE.COMMON);

			// 余额不足，不结息
			if (CommUtil.compare(cplWaitDealInst.getInterest(), cplBalInfo.getUsable_bal()) > 0) {
				bizlog.info("The quota[%s] has expired, you can only use the account balance to repay interest", ODAgree.getLimit_code());
				return null;
			}
		}

		// 将本透支协议号压入公共运行区的临时数据中，后面占用额度时会用到
		final String tempData = BizUtil.getTrxRunEnvs().getTemp_data();

		if (CommUtil.isNull(tempData)) {

			BizUtil.getTrxRunEnvs().setTemp_data("OD_Agree_No=".concat(ODAgree.getAgree_no()));
		}
		else {

			BizUtil.getTrxRunEnvs().setTemp_data(tempData.concat("OD_Agree_No=").concat(ODAgree.getAgree_no()));
		}

		// 透支结息先使用额度，额度用完后结息不发起占额
		final DpDemandDrawIn demandDrawIn = BizUtil.getInstance(DpDemandDrawIn.class);

		demandDrawIn.setAcct_no(subAcct.getAcct_no());
		demandDrawIn.setAcct_name(null);
		demandDrawIn.setProd_id(subAcct.getProd_id());
		demandDrawIn.setCheck_password_ind(E_YESORNO.NO);
		demandDrawIn.setCcy_code(subAcct.getCcy_code());
		demandDrawIn.setTrxn_amt(cplWaitDealInst.getInterest());
		demandDrawIn.setWithdrawal_busi_type(E_DRAWBUSIKIND.COMMON); // 普通支取
		demandDrawIn.setCash_trxn_ind(E_CASHTRXN.TRXN); // 交易级现转标志
		demandDrawIn.setSummary_code(ApSystemParmApi.getValue(ApConst.CORE_SUMMARY_CODE, "OD_PAY_INST"));
		demandDrawIn.setTrxn_remark(ODAgree.getAgree_no());

		DpDemandDrawOut demandDrawOut = BizUtil.getInstance(DpDemandDrawOut.class);

		// 活期支取检查，无数据库操作，不能回滚
		try {
			BizUtil.getInstance(SrvDpDemandAccounting.class).demandDrawCheck(demandDrawIn);
		}
		catch (Exception e) {

			// 公共运行区临时数据恢复原样，，避免影响接下来的业务
			BizUtil.getTrxRunEnvs().setTemp_data(tempData);

			// 系统异常抛错
			if (!(e instanceof LttsBusinessException)) {
				throw e;
			}

			return null;
		}

		// 借： 活期存款账户
		demandDrawOut = BizUtil.getInstance(SrvDpDemandAccounting.class).demandDraw(demandDrawIn);

		// 克隆，不要修改输入接口
		DpInstAccounting cplActInstInfo = BizUtil.clone(DpInstAccounting.class, cplWaitDealInst);

		// 利息值刷新为实际支取金额
		cplActInstInfo.setInterest(demandDrawOut.getAct_withdrawal_amt());

		// 贷： 应收利息
		DpInterestSettlement.receivableInterestAccounting(cplActInstInfo, subAcct);

		// 公共运行区临时数据恢复原样，，避免影响接下来的业务
		BizUtil.getTrxRunEnvs().setTemp_data(tempData);

		// 返回本次实际收取利息
		bizlog.method(" DpOverDraftDayEnd.incomeODInst end <<<<<<<<<<<<<<<<");
		return cplActInstInfo;
	}
}