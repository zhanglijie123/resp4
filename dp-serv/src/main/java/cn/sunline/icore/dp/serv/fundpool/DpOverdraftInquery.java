package cn.sunline.icore.dp.serv.fundpool;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.namedsql.SqlDpOverdraftBasicDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlipDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraft;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlipDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpBalanceCalculateOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ALLOW;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_OVERDRAFTINSTTYPE;
import cn.sunline.icore.dp.serv.iobus.DpCreditLimitIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInstructDao;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpAcctOverdraftInfo;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftAgreeInfo;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftQryIn;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftQryOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitTrialInfo;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_FLOATWAY;
import cn.sunline.icore.sys.type.EnumType.E_ROUNDRULE;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明：透支相关查询
 * </p>
 * 
 * @Author yangdl
 *         <p>
 *         <li>2017年7月8日-上午11:23:06</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年7月8日-yangdl：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */

public class DpOverdraftInquery {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOverdraftInquery.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年7月11日-下午3:22:37</li>
	 *         <li>功能说明：透支协议查询</li>
	 *         </p>
	 * @param DpOverdraftQryIn
	 * @param DpOverdraftQryOut
	 */
	public static DpOverdraftQryOut overdraftAgreeQry(DpOverdraftQryIn cplIn) {

		bizlog.method("DpOverdraftInquery.overdraftAgreeQry begin >>>>>>>>>>>>>");

		// 获取公共运行变量
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		String acctNo = "";

		if (CommUtil.isNotNull(cplIn.getAcct_no())) {

			// 定位客户账户
			DpaAccount acctInfo = DpToolsApi.accountInquery(cplIn.getAcct_no(), cplIn.getAcct_type());

			acctNo = acctInfo.getAcct_no();
		}

		// 查询透支协议信息
		Page<DpbOverdraft> page = SqlDpInstructDao.selOverdraftDetails(cplIn.getAgree_no(), acctNo, cplIn.getCcy_code(), cplIn.getLimit_code(), cplIn.getAgree_status(),
				runEnvs.getBusi_org_id(), cplIn.getSign_date(), cplIn.getOverdraft_type(), runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(page.getRecordCount());

		List<DpbOverdraft> overdraftList = page.getRecords();

		DpOverdraftQryOut cplOut = BizUtil.getInstance(DpOverdraftQryOut.class);

		Options<DpOverdraftAgreeInfo> overdraftAgreeInfoListy = cplOut.getList01();

		for (DpbOverdraft overdraft : overdraftList) {

			DpOverdraftAgreeInfo overdraftInfo = BizUtil.getInstance(DpOverdraftAgreeInfo.class);

			// 定位账户
			DpaAccount dpaAccount = DpToolsApi.locateSingleAccount(overdraft.getAcct_no(), cplIn.getAcct_type(), true);

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(dpaAccount.getAcct_no());
			accessIn.setCcy_code(overdraft.getCcy_code());
			accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

			// 查询可用余额
			DpBalanceCalculateOut balance = DpToolsApi.getBalance(subAcct.getSub_acct_no(), subAcct.getAcct_no(), null);

			overdraftInfo.setAgree_no(overdraft.getAgree_no()); // 协议号
			overdraftInfo.setAcct_no(overdraft.getAcct_no());
			overdraftInfo.setAcct_type(dpaAccount.getAcct_type());
			overdraftInfo.setAcct_name(dpaAccount.getAcct_name());
			overdraftInfo.setCcy_code(overdraft.getCcy_code());
			overdraftInfo.setEffect_date(overdraft.getEffect_date());
			overdraftInfo.setExpiry_date(overdraft.getExpiry_date());
			overdraftInfo.setOverdraft_type(overdraft.getOverdraft_type());
			overdraftInfo.setLimit_code(overdraft.getLimit_code());
			overdraftInfo.setPay_inst_cyc(overdraft.getPay_inst_cyc());// 付息周期
			overdraftInfo.setLast_pay_inst_date(overdraft.getLast_pay_inst_date());// 上次付息日
			overdraftInfo.setNext_pay_inst_date(overdraft.getNext_pay_inst_date());// 下次付息日
			overdraftInfo.setOverdraft_rate_source(overdraft.getOverdraft_rate_source());// 透支利率来源
			overdraftInfo.setFixed_acct_no(overdraft.getFixed_acct_no());// 定期账户
			overdraftInfo.setFixed_sub_acct_seq(overdraft.getFixed_sub_acct_seq());// 定期子帐户序号
			overdraftInfo.setOver_quota_allow_ind(overdraft.getOver_quota_allow_ind());// 超额透支许可标志
			overdraftInfo.setAgree_status(overdraft.getAgree_status());
			overdraftInfo.setSign_date(overdraft.getSign_date());
			overdraftInfo.setSign_seq(overdraft.getSign_seq());
			overdraftInfo.setCancel_date(overdraft.getCancel_date());
			overdraftInfo.setCancel_seq(overdraft.getCancel_seq());
			overdraftInfo.setData_version(overdraft.getData_version());

			// 额度相关信息输入
			// 调用额度查询
			DpCreditLimitInfo clAccountInfo = DpCreditLimitIobus.getCreditLimitInfo(overdraft.getLimit_code());

			overdraftInfo.setDue_date(clAccountInfo.getDue_date());// 额度到期日
			overdraftInfo.setAvailable_bal(clAccountInfo.getAvailable_bal());// 实际可用透支金额

			// 透支利率相关
			// 查询透支卡片
			DpbOverdraftSlip overdraftFiche = DpbOverdraftSlipDao.selectOne_odb1(overdraft.getAcct_no(), overdraft.getAgree_no(), E_OVERDRAFTINSTTYPE.NORMAL, false);

			// 查询透支计息
			DpaSlip ficheInterest = DpaSlipDao.selectOne_odb1(overdraft.getAcct_no(), overdraftFiche.getFiche_no(), false);

			overdraftInfo.setOverdraft_amt(ficheInterest.getAcct_bal());

			// 利率定义
			DpaSlip ficheInrt = DpaSlipDao.selectOne_odb1(overdraft.getAcct_no(), overdraftFiche.getFiche_no(), false);

			BigDecimal baseInrt = ficheInrt.getBank_base_inrt();

			// 透支利率
			overdraftInfo.setOverdraft_inrt(ficheInrt.getEfft_inrt());
			overdraftInfo.setBank_base_inrt(baseInrt);
			overdraftInfo.setInrt_code(ficheInrt.getInrt_code());// 利率编号
			overdraftInfo.setInrt_code_direction(ficheInrt.getInrt_code_direction());// 利率编号指向

			overdraftInfo.setOverdraft_interest(ApCurrencyApi.roundAmount(overdraft.getCcy_code(), ficheInterest.getAccrual_inst(), E_ROUNDRULE.ROUND));// 透支利息

			overdraftInfo.setOverdraft_inrt_float_method(ficheInrt.getInrt_float_method());// 透支利率浮动方式
			overdraftInfo.setOverdraft_inrt_float_value(ficheInrt.getInrt_float_value());// 透支利率浮动值

			BigDecimal inst = ficheInterest.getAccrual_inst();// 总利息

			BigDecimal overInsts = overdraftFiche.getOverdue_interest();// 逾期罚息

			// 加上逾期罚息
			inst = inst.add(overInsts);

			// 初始化超额相关
			overdraftInfo.setOver_quota_inrt(BigDecimal.ZERO);// 超额利率
			overdraftInfo.setExcess_limit_amt(BigDecimal.ZERO);// 超限金额
			overdraftInfo.setOver_quota_interest(BigDecimal.ZERO);// 超额利息

			// 超额利率
			if (overdraftInfo.getOver_quota_allow_ind() == E_ALLOW.ALLOW) {

				DpbOverdraftSlip overdraftFicheOver = DpbOverdraftSlipDao.selectOne_odb1(overdraft.getAcct_no(), overdraft.getAgree_no(), E_OVERDRAFTINSTTYPE.EXCESS, false);

				// 超额计息
				DpaSlip ficheInterestOver = DpaSlipDao.selectOne_odb1(overdraft.getAcct_no(), overdraftFicheOver.getFiche_no(), false);

				overdraftInfo.setOver_quota_inrt(ficheInterestOver.getEfft_inrt());// 超额利率
				overdraftInfo.setOver_quota_inrt_float_method(ficheInterestOver.getInrt_float_method());// 超额利率浮动方式
				overdraftInfo.setOver_quota_inrt_float_value(ficheInterestOver.getInrt_float_value());// 超额利率浮动值
				overdraftInfo.setExcess_limit_amt(ficheInterestOver.getAcct_bal());// 超限金额

				overdraftInfo.setOver_quota_interest(ApCurrencyApi.roundAmount(overdraft.getCcy_code(), ficheInterestOver.getAccrual_inst(), E_ROUNDRULE.ROUND));// 超额利息

				inst = inst.add(ficheInterestOver.getAccrual_inst());

				overInsts = overInsts.add(overdraftFicheOver.getOverdue_interest());
			}

			overdraftInfo.setOverdue_inrt_float_method(overdraft.getOverdue_inrt_float_method());// 逾期利率浮动方式
			overdraftInfo.setOverdue_inrt_float_value(overdraft.getOverdue_inrt_float_value());// 逾期利率浮动值
			// 逾期利率
			if (overdraft.getOverdue_inrt_float_method() == E_FLOATWAY.PERCENT) {

				BigDecimal addValue = overdraftInfo.getOverdraft_inrt().multiply(overdraft.getOverdue_inrt_float_value())
						.divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);

				overdraftInfo.setOverdue_inrt(overdraftInfo.getOverdraft_inrt().add(addValue));// 逾期利率

			}
			else if (overdraft.getOverdue_inrt_float_method() == E_FLOATWAY.VALUE) {

				overdraftInfo.setOverdue_inrt(overdraftInfo.getOverdraft_inrt().add(overdraft.getOverdue_inrt_float_value()));
			}
			else {

				overdraftInfo.setOverdue_inrt(overdraftInfo.getOverdraft_inrt());
			}

			overdraftInfo.setOverdue_interest(ApCurrencyApi.roundAmount(overdraft.getCcy_code(), overInsts, E_ROUNDRULE.ROUND));// 逾期利息

			// 总利息取三种利息四舍五入后的值
			overdraftInfo.setInterest(overdraftInfo.getOverdue_interest().add(overdraftInfo.getOverdraft_interest()).add(overdraftInfo.getOver_quota_interest()));

			overdraftInfo.setTotal_overdraft_amt(overdraftInfo.getOverdraft_amt().add(overdraftInfo.getExcess_limit_amt()));// 总透支额度

			// 解约应还金额计算
			BigDecimal trxnAmount = BigDecimal.ZERO; // 本金

			// 应还本金
			trxnAmount = trxnAmount.add(overdraftInfo.getTotal_overdraft_amt());

			if (CommUtil.compare(balance.getUsable_bal().add(trxnAmount), BigDecimal.ZERO) < 0) {

				trxnAmount = trxnAmount.add(balance.getUsable_bal().add(trxnAmount).negate());
			}

			overdraftInfo.setRpym_amt(trxnAmount.add(overdraftInfo.getInterest()));

			overdraftAgreeInfoListy.add(overdraftInfo);

		}

		cplOut.setList01(overdraftAgreeInfoListy);

		bizlog.method("DpOverdraftInquery.overdraftAgreeQry end <<<<<<<<<<<<<<");

		return cplOut;

	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2018年5月17日-下午3:20:47</li>
	 *         <li>功能说明：获取账户透支信息</li>
	 *         </p>
	 * @param subAcct
	 * @return
	 */
	public static DpAcctOverdraftInfo getAcctOverdraftInfo(DpaSubAccount subAcct) {

		DpAcctOverdraftInfo acctOverdraftInfo = BizUtil.getInstance(DpAcctOverdraftInfo.class);
		BigDecimal sumLimtAmt = BigDecimal.ZERO; // 可以透支金额
		BigDecimal totalAmt = BigDecimal.ZERO; // 已用透支额度
		BigDecimal interest = BigDecimal.ZERO; // 透支利息
		BigDecimal overQuotaInterest = BigDecimal.ZERO; // 超额透支利息
		BigDecimal overdueInterest = BigDecimal.ZERO; // 逾期利息

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();

		List<DpbOverdraft> listODAgree = SqlDpOverdraftBasicDao.selOverdraftAgreeByAcct(subAcct.getAcct_no(), subAcct.getCcy_code(), E_STATUS.VALID, trxnDate, orgId, false);

		for (DpbOverdraft ODAgree : listODAgree) {
			// 不是生效的不处理，是否生效记录在外面筛选号，本处只是防止协议被联机关闭
			if (ODAgree.getAgree_status() != E_STATUS.VALID) {
				continue;
			}

			// 调用额度查询
			DpCreditLimitInfo clAccountInfo = DpCreditLimitIobus.getCreditLimitInfo(ODAgree.getLimit_code());
			;

			clAccountInfo.getInit_limit_amt();

			// 调用额度试算(带锁)服务
			DpCreditLimitTrialInfo cplLimitQryOut = DpCreditLimitIobus.getCreditLimitTrialInfo(ODAgree.getLimit_code(), subAcct.getCcy_code());

			// 协议卡片关系
			List<DpbOverdraftSlip> listODFiche = DpbOverdraftSlipDao.selectAll_odb3(ODAgree.getAcct_no(), ODAgree.getAgree_no(), true);

			for (DpbOverdraftSlip ODFiche : listODFiche) {

				DpaSlip ficheInst = DpaSlipDao.selectOne_odb1(ODAgree.getAcct_no(), ODFiche.getFiche_no(), true);

				totalAmt = totalAmt.add(ficheInst.getAcct_bal());

				if (ODFiche.getOverdraft_inst_type() == E_OVERDRAFTINSTTYPE.NORMAL) {
					// 正常透支剩余额度
					sumLimtAmt = sumLimtAmt.add(cplLimitQryOut.getLimit_bal());
					interest = interest.add(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), ficheInst.getAccrual_inst(), E_ROUNDRULE.ROUND));
				}
				else if (ODFiche.getOverdraft_inst_type() == E_OVERDRAFTINSTTYPE.EXCESS) {
					// 超额透支剩余额度
					sumLimtAmt = sumLimtAmt.add(cplLimitQryOut.getAvailable_overdraw_amount());
					overQuotaInterest = overQuotaInterest.add(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), ficheInst.getAccrual_inst(), E_ROUNDRULE.ROUND));
				}
				else {
					throw APPUB.E0026(DpBaseDict.A.overdraft_inst_type.getLongName(), ODFiche.getOverdraft_inst_type().getValue());
				}

				overdueInterest = overdueInterest.add(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), ODFiche.getOverdue_interest(), E_ROUNDRULE.ROUND));
			}
		}

		acctOverdraftInfo.setAvailable_overdraw_amount(sumLimtAmt);
		acctOverdraftInfo.setTotal_overdraft_amt(totalAmt);
		acctOverdraftInfo.setOverdraft_interest(interest);
		acctOverdraftInfo.setOver_quota_interest(overQuotaInterest);
		acctOverdraftInfo.setOverdraft_total_interest(interest.add(overdueInterest).add(overQuotaInterest));

		return acctOverdraftInfo;
	}
}
