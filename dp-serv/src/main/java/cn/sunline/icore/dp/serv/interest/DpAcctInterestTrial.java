package cn.sunline.icore.dp.serv.interest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpTaxApi;
import cn.sunline.icore.dp.base.api.DpTimeInterestApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRate;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRateDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfTdDrawInterest;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstDetailRegister;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInterestResult;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpTimeDrawInterest;
import cn.sunline.icore.dp.base.type.ComDpTaxBase.DpIntTaxInfo;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_DRAWTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TIMEDRAWDATESCENE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.dayend.DpDayEndInterest;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.fundpool.DpOverdraftSettlement;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpDemandCloseInstInfo;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpDemandCloseInstTrialIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpDemandCloseInstTrialOut;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpDemandSubAcct;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpInstIndexTypeInfo;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpTimeDrawInterestTrialIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpTimeDrawInterestTrialOut;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.base.util.RunnableWithReturn;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 账户利息试算:包含活期 定期
 * </p>
 * 
 * @Author linshiq
 *         <p>
 *         <li>2017年3月16日-上午10:11:18</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月16日-linshiq：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAcctInterestTrial {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAcctInterestTrial.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年3月16日-上午11:18:07</li>
	 *         <li>功能说明：活期销户利息试算主程序</li>
	 *         </p>
	 * @param cplIn
	 *            活期销户利息试算输入
	 * @return 活期销户利息试算输出
	 */
	public static DpDemandCloseInstTrialOut deamdCloseInstTrialMain(DpDemandCloseInstTrialIn cplIn) {

		bizlog.method(" DpAcctInterestTrial.deamdCloseInstTrialMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 账号不可为空
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 子账户列表不可为空
		if (cplIn.getList01() == null || cplIn.getList01().size() == 0) {
			throw DpErr.Dp.E0128();
		}

		// 定位账号信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		// 初始化输出接口
		DpDemandCloseInstTrialOut cplOut = BizUtil.getInstance(DpDemandCloseInstTrialOut.class);

		for (DpDemandSubAcct demandSubAcct : cplIn.getList01()) {

			// 货币代号不可为空
			BizUtil.fieldNotNull(demandSubAcct.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

			// 定位子账号信息
			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(acctInfo.getAcct_no()); // 账号
			accessIn.setCcy_code(demandSubAcct.getCcy_code()); // 货币代码
			accessIn.setProd_id(demandSubAcct.getProd_id()); // 产品编号
			accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND); // 定活标志

			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			// 查询存款子账户表
			DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOne_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

			if (subAcctInfo.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

				throw DpBase.E0017(acctInfo.getAcct_no(), subAcctInfo.getSub_acct_seq());
			}

			// 获取活期销户利息试算结果
			DpDemandCloseInstInfo demandCloseInst = deamdCloseInstTrial(subAcctInfo);

			cplOut.getList01().add(demandCloseInst);
		}

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_type(acctInfo.getAcct_type()); // 账户类型
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAcctInterestTrial.deamdCloseInstTrialMain end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年3月16日-下午4:03:33</li>
	 *         <li>功能说明：活期子户销户利息试算</li>
	 *         </p>
	 * @param subAcctInfo
	 *            存款子账户表
	 * @return 单个活期销户利息试算结果
	 */
	public static DpDemandCloseInstInfo deamdCloseInstTrial(final DpaSubAccount subAcctInfo) {

		bizlog.method(" DpAcctInterestTrial.deamdCloseInstTrial begin >>>>>>>>>>>>>>>>");

		// 使用独立事物，好回滚DML语句
		DpDemandCloseInstInfo instResult = DaoUtil.executeInNewTransation(new RunnableWithReturn<DpDemandCloseInstInfo>() {

			public DpDemandCloseInstInfo execute() {

				DpDemandCloseInstInfo demandInstInfo = BizUtil.getInstance(DpDemandCloseInstInfo.class);

				demandInstInfo.setAcct_bal(BigDecimal.ZERO);
				demandInstInfo.setInterest(BigDecimal.ZERO);
				demandInstInfo.setInterest_tax(BigDecimal.ZERO);
				demandInstInfo.setTax_after_inst_amt(BigDecimal.ZERO);
				demandInstInfo.setStill_inst(BigDecimal.ZERO);

				// 另外定义一个子账户信息对象
				DpaSubAccount subAcctNew = DpaSubAccountDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), true);

				// 日切后计息前调此接口需补算日终利息处理逻辑
				if (CommUtil.compare(subAcctNew.getNext_inst_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {

					// 计息、结息、重定价、透支计息、透支结息、透支重定价
					DpDayEndInterest.onlineDealInterest(subAcctNew);

					// 读取最新账户信息
					subAcctNew = DpaSubAccountDao.selectOne_odb1(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no(), true);

					// 日终结息利息、利息税
					demandInstInfo.setInterest(subAcctNew.getInst_paid().subtract(subAcctInfo.getInst_paid()));
					demandInstInfo.setInterest_tax(subAcctNew.getInst_withholding_tax().subtract(subAcctInfo.getInst_withholding_tax()));
				}

				// 活期销户直接付息处理
				DpInterestResult cplInstOut = DpInterestBasicApi.currentInstSettleClear(subAcctNew);

				// 因为已派利息字段的更新在账单登记逻辑里面，所以直接付息里面没有更新这个字段，在此处更新下
				demandInstInfo.setInterest(demandInstInfo.getInterest().add(ApCurrencyApi.roundAmount(subAcctInfo.getCcy_code(), cplInstOut.getAccrual_inst())));
				demandInstInfo.setInterest_tax(demandInstInfo.getInterest_tax().add((ApCurrencyApi.roundAmount(subAcctInfo.getCcy_code(), cplInstOut.getAccrual_inst_tax()))));

				// 税后利息 = 利息 - 利息税
				demandInstInfo.setTax_after_inst_amt(demandInstInfo.getInterest().subtract(demandInstInfo.getInterest_tax()));

				// 应付金额 = 账户余额 + 税后利息 - 透支欠息
				BigDecimal payAmt = subAcctNew.getAcct_bal().add(demandInstInfo.getTax_after_inst_amt());

				// 补充其他输出信息
				demandInstInfo.setSub_acct_seq(subAcctNew.getSub_acct_seq());
				demandInstInfo.setProd_id(subAcctNew.getProd_id());
				demandInstInfo.setCcy_code(subAcctNew.getCcy_code());
				demandInstInfo.setAcct_bal(subAcctNew.getAcct_bal());
				demandInstInfo.setPaying_amt(payAmt);

				// 查询交易一定要回滚事物
				DaoUtil.rollbackTransaction();

				return demandInstInfo;
			}
		});

		// 透支欠息
		instResult.setStill_inst(DpOverdraftSettlement.getOdInterest(subAcctInfo, null));

		instResult.setPaying_amt(instResult.getPaying_amt().subtract(instResult.getStill_inst()));

		bizlog.method(" DpAcctInterestTrial.deamdCloseInstTrial end <<<<<<<<<<<<<<<<");
		return instResult;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年5月10日-上午11:18:07</li>
	 *         <li>功能说明：定期支取利息试算主程序</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取利息试算输入
	 * @return 定期支取利息试算输出
	 */
	public static DpTimeDrawInterestTrialOut timeDrawInstTrial(DpTimeDrawInterestTrialIn cplIn) {

		bizlog.method(" DpAcctInterestTrial.timeDrawInstTrial begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());
		BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

		// 子账号定位
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no());
		accessIn.setAcct_type(cplIn.getAcct_type());
		accessIn.setCcy_code(cplIn.getCcy_code());
		accessIn.setDd_td_ind(E_DEMANDORTIME.TIME);
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		final DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		// 支取金额合规性检查
		ApCurrencyApi.chkAmountByCcy(subAcct.getCcy_code(), cplIn.getTrxn_amt());

		// 利息试算输入
		final DpTimeDrawInterest cplCalcIn = BizUtil.getInstance(DpTimeDrawInterest.class);

		cplCalcIn.setDraw_date(CommUtil.nvl(cplIn.getDraw_date(), BizUtil.getTrxRunEnvs().getTrxn_date()));
		cplCalcIn.setTrxn_amt(cplIn.getTrxn_amt());

		if (CommUtil.equals(cplIn.getTrxn_amt(), subAcct.getAcct_bal())) {
			cplCalcIn.setWithdrawal_type(E_DRAWTYPE.CLOSE);
		}
		else {
			cplCalcIn.setWithdrawal_type(E_DRAWTYPE.COMMON);
		}

		cplCalcIn.setInst_handling_method(cplIn.getInst_handling_method());
		cplCalcIn.setEfft_inrt(cplIn.getContract_inrt());
		cplCalcIn.setInrt_code(cplIn.getInrt_code());
		cplCalcIn.setInrt_float_method(cplIn.getInrt_float_method());
		cplCalcIn.setInrt_float_value(cplIn.getInrt_float_value());
		cplCalcIn.setAppo_inrt_term(cplIn.getAppo_inrt_term());

		// 使用独立事物，好回滚DML语句
		DpInterestResult cplInstResult = DaoUtil.executeInNewTransation(new RunnableWithReturn<DpInterestResult>() {

			public DpInterestResult execute() {

				// 日切后计息前调此接口需补算日终利息处理逻辑
				if (CommUtil.compare(subAcct.getNext_inst_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {

					DpDayEndInterest.onlineDealInterest(subAcct);

					// 读取最新账户信息更新原账户信息对象
					DpaSubAccount subAcctNew = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

					// 日终结息期间利息可能入定期户自身导致账户余额发生变动
					if (cplCalcIn.getWithdrawal_type() == E_DRAWTYPE.CLOSE) {
						cplCalcIn.setTrxn_amt(subAcctNew.getAcct_bal());
					}

					subAcct.setAcct_bal(subAcctNew.getAcct_bal());
					subAcct.setInst_paid(subAcctNew.getInst_paid());
					subAcct.setInst_withholding_tax(subAcctNew.getInst_withholding_tax());
					subAcct.setCyc_inst_paid(subAcctNew.getCyc_inst_paid());
					subAcct.setCyc_inst_withholding_tax(subAcctNew.getCyc_inst_withholding_tax());
					subAcct.setInst_payable(subAcctNew.getInst_payable());
					subAcct.setInst_tax_payable(subAcctNew.getInst_tax_payable());
					subAcct.setCyc_inst_payable(subAcctNew.getCyc_inst_payable());
					subAcct.setCyc_inst_tax_payable(subAcctNew.getCyc_inst_tax_payable());
				}

				DpInterestResult cplInstOut = DpTimeInterestApi.CalcTimeDrawInst(cplCalcIn, subAcct);

				// 查询交易一定要回滚事物
				DaoUtil.rollbackTransaction();

				return cplInstOut;
			}
		});

		// 获取定期支取场景: 默认为一般支取，比如定活两遍
		E_TIMEDRAWDATESCENE drawScene = E_TIMEDRAWDATESCENE.GENERAL;

		if (CommUtil.isNotNull(subAcct.getDue_date()) && !CommUtil.equals(subAcct.getDue_date(), cplIn.getDraw_date())) {

			drawScene = DpTimeInterestApi.getDrawDateScene(subAcct, cplIn.getDraw_date());

			DpfTdDrawInterest tdDrawInst = DpProductFactoryApi.getProdTdDrawInstInfo(subAcct.getProd_id(), subAcct.getCcy_code(), false);

			// 节假日非销户结息，重定义支取场景
			if (drawScene == E_TIMEDRAWDATESCENE.HOLIDAY_BEFORE && cplCalcIn.getWithdrawal_type() != E_DRAWTYPE.CLOSE && tdDrawInst.getHoliday_delayed_ind() == E_YESORNO.YES) {

				drawScene = E_TIMEDRAWDATESCENE.BEFORE;
			}
			else if (drawScene == E_TIMEDRAWDATESCENE.HOLIDAY_AFTER && cplCalcIn.getWithdrawal_type() != E_DRAWTYPE.CLOSE && tdDrawInst.getHoliday_delayed_ind() == E_YESORNO.YES) {

				drawScene = E_TIMEDRAWDATESCENE.AFTER;
			}
		}

		// 定期试算输出
		DpTimeDrawInterestTrialOut cplOut = BizUtil.getInstance(DpTimeDrawInterestTrialOut.class);

		cplOut.setCard_no(accessOut.getCard_no()); // 卡号
		cplOut.setAcct_no(accessOut.getAcct_no()); // 账号
		cplOut.setAcct_type(accessOut.getAcct_type()); // 账户类型
		cplOut.setAcct_name(accessOut.getAcct_name()); // 账户名称
		cplOut.setCcy_code(accessOut.getCcy_code()); // 货币代码
		cplOut.setSub_acct_seq(accessOut.getSub_acct_seq()); // 子账户序号
		cplOut.setAcct_bal(subAcct.getAcct_bal().subtract(cplIn.getTrxn_amt())); // 账户余额
		cplOut.setTerm_code(subAcct.getTerm_code()); // 存期
		cplOut.setDue_date(subAcct.getDue_date()); // 到期日
		cplOut.setInterest(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplInstResult.getAccrual_inst())); // 利息
		cplOut.setInterest_tax(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplInstResult.getAccrual_inst_tax())); // 利息税
		cplOut.setTrxn_amt(cplIn.getTrxn_amt()); // 交易金额
		cplOut.setTime_withdrawal_scene(drawScene);

		// 税后利息
		BigDecimal instAfterTax = cplOut.getInterest().subtract(cplOut.getInterest_tax());

		cplOut.setTax_after_inst_amt(instAfterTax);
		cplOut.setPaying_amt(cplIn.getTrxn_amt().add(instAfterTax)); // 应付金额
		cplOut.getList01().addAll(initInstIndexInfo(subAcct)); // 初始化各类利息索引

		// 各利息索引信息
		if (CommUtil.isNotNull(cplInstResult.getList_inst_detail()) && cplInstResult.getList_inst_detail().size() > 0) {

			// 按利息索引类型排序
			BizUtil.listSort(cplInstResult.getList_inst_detail(), true, DpBaseDict.A.inst_key_type.getId());

			// 利息明细归类汇总
			for (DpInstDetailRegister cplInstDetail : cplInstResult.getList_inst_detail()) {

				for (int index = 0; index < cplOut.getList01().size(); index++) {

					if (cplInstDetail.getInst_key_type() == cplOut.getList01().get(index).getInst_key_type()) {

						cplOut.getList01().get(index).setInst_key_type(cplInstDetail.getInst_key_type());
						cplOut.getList01().get(index).setEfft_inrt(cplInstDetail.getEfft_inrt());
						cplOut.getList01().get(index).setInterest(cplOut.getList01().get(index).getInterest().add(cplInstDetail.getSeg_inst()));
						cplOut.getList01().get(index).setInterest_tax(cplOut.getList01().get(index).getInterest_tax().add(cplInstDetail.getSeg_inst_tax()));
					}
				}
			}
		}

		// 输出列表按币种精度处理
		for (int index = 0; index < cplOut.getList01().size(); index++) {

			cplOut.getList01().get(index).setInterest(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplOut.getList01().get(index).getInterest()));
			cplOut.getList01().get(index).setInterest_tax(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), cplOut.getList01().get(index).getInterest_tax()));
		}

		bizlog.method(" DpAcctInterestTrial.timeDrawInstTrial end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年5月10日-上午11:18:07</li>
	 *         <li>功能说明：定期各类利息索引利息记录初始化</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param drawDate
	 *            支取日期
	 * @return 定期支取利息索引信息列表
	 */
	private static List<DpInstIndexTypeInfo> initInstIndexInfo(DpaSubAccount subAcct) {

		List<DpInstIndexTypeInfo> cplOut = new ArrayList<DpInstIndexTypeInfo>();

		// 正常利率信息
		DpaInterestRate normalInstRate = DpaInterestRateDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, DpConst.START_SORT_VALUE, true);

		// 1. 正常利息索引
		DpInstIndexTypeInfo cplInfo = BizUtil.getInstance(DpInstIndexTypeInfo.class);

		cplInfo.setInst_key_type(E_INSTKEYTYPE.NORMAL);
		cplInfo.setBank_base_inrt(normalInstRate.getBank_base_inrt());
		cplInfo.setEfft_inrt(normalInstRate.getEfft_inrt());
		cplInfo.setInterest(BigDecimal.ZERO);
		cplInfo.setInterest_tax(BigDecimal.ZERO);

		cplOut.add(cplInfo);

		// 2. 零头天数利息索引
		if (CommUtil.isNotNull(subAcct.getRemnant_day_start_date())) {

			// 零头天数利率
			DpaInterestRate remnantInstRate = DpaInterestRateDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.REMNANT, DpConst.START_SORT_VALUE,
					true);

			cplInfo = BizUtil.getInstance(DpInstIndexTypeInfo.class);

			cplInfo.setInst_key_type(E_INSTKEYTYPE.REMNANT);
			cplInfo.setBank_base_inrt(remnantInstRate.getBank_base_inrt());
			cplInfo.setEfft_inrt(remnantInstRate.getEfft_inrt());
			cplInfo.setInterest(BigDecimal.ZERO);
			cplInfo.setInterest_tax(BigDecimal.ZERO);

			cplOut.add(cplInfo);
		}

		// 3. 提前支取利息索引: 利率不急着登记准确，后面再更新
		cplInfo = BizUtil.getInstance(DpInstIndexTypeInfo.class);

		cplInfo.setInst_key_type(E_INSTKEYTYPE.BREACH);
		cplInfo.setBank_base_inrt(BigDecimal.ZERO);
		cplInfo.setEfft_inrt(BigDecimal.ZERO);
		cplInfo.setInterest(BigDecimal.ZERO);
		cplInfo.setInterest_tax(BigDecimal.ZERO);

		cplOut.add(cplInfo);

		// 4. 逾期支取利息索引: 利率不急着登记准确，后面再更新
		cplInfo = BizUtil.getInstance(DpInstIndexTypeInfo.class);

		cplInfo.setInst_key_type(E_INSTKEYTYPE.MATURE);
		cplInfo.setBank_base_inrt(BigDecimal.ZERO);
		cplInfo.setEfft_inrt(BigDecimal.ZERO);
		cplInfo.setInterest(BigDecimal.ZERO);
		cplInfo.setInterest_tax(BigDecimal.ZERO);

		cplOut.add(cplInfo);

		// 返回各类型利息索引
		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年4月9日-上午11:18:07</li>
	 *         <li>功能说明：账户到期收益试算</li>
	 *         <li>补充说明：主要给定期账户信息查询使用</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取利息试算输入
	 * @return 定期支取利息试算输出
	 */
	public static DpInstAccounting acctMatureProfitTrial(final DpaSubAccount subAcct) {

		bizlog.method(" DpAcctInterestTrial.acctMatureProfitTrial begin >>>>>>>>>>>>>>>>");

		final String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		final String nextInstDate = subAcct.getNext_inst_date();

		// 输出结果实例化
		DpInstAccounting cplOut = BizUtil.getInstance(DpInstAccounting.class);

		cplOut.setInterest(BigDecimal.ZERO);
		cplOut.setInterest_tax(BigDecimal.ZERO);
		cplOut.setInst_tax_rate(BigDecimal.ZERO);

		if (subAcct.getInst_ind() == E_YESORNO.NO || subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
			bizlog.method(" DpAcctInterestTrial.acctMatureProfitTrial end <<<<<<<<<<<<<<<<");
			return cplOut;
		}

		// 总应计利息
		BigDecimal totalAccrualInst = subAcct.getInst_payable();

		// 读取账户计息定义数据
		List<DpaInterest> interestList = DpaInterestDao.selectAll_odb2(subAcct.getAcct_no(), subAcct.getSub_acct_no(), false);

		for (DpaInterest DpaInterest : interestList) {

			totalAccrualInst = totalAccrualInst.add(DpaInterest.getAccrual_inst());
		}

		// 未到期或者当天还未日终计息, 需要调用计提代码试算，为避免操作数据库使用独立事物
		if (CommUtil.compare(subAcct.getDue_date(), trxnDate) > 0 || CommUtil.compare(subAcct.getNext_inst_date(), trxnDate) > 0) {

			BigDecimal addInst = DaoUtil.executeInNewTransation(new RunnableWithReturn<BigDecimal>() {

				public BigDecimal execute() {

					// 获取计提利息索引类型
					E_INSTKEYTYPE instKey = DpInterestBasicApi.getCainInstKey(subAcct, trxnDate);

					// 读取计息定义表
					DpaInterest instAcct = DpaInterestDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), instKey, true);

					// 刷新交易日期，以便利息索引内的天数一次性计提完毕
					if (instKey == E_INSTKEYTYPE.REMNANT) {
						BizUtil.getTrxRunEnvs().setTrxn_date(subAcct.getDue_date());
					}
					else if (instKey == E_INSTKEYTYPE.NORMAL && subAcct.getDd_td_ind() == E_DEMANDORTIME.TIME) {
						BizUtil.getTrxRunEnvs().setTrxn_date(CommUtil.nvl(subAcct.getRemnant_day_start_date(), subAcct.getDue_date()));
					}

					// 计提试算
					BigDecimal addValue = DpInterestBasicApi.getAccruedInterest(subAcct, instAcct).getAccrual_inst();

					// 当前是正常利息索引，还要考虑零头天数利息索引
					if (instKey == E_INSTKEYTYPE.NORMAL && CommUtil.isNotNull(subAcct.getRemnant_day_start_date())) {

						subAcct.setNext_inst_date(BizUtil.dateAdd("day", subAcct.getRemnant_day_start_date(), 1));

						BizUtil.getTrxRunEnvs().setTrxn_date(subAcct.getDue_date());

						instAcct = DpaInterestDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_INSTKEYTYPE.REMNANT, true);

						// 零头天数计提试算
						BigDecimal rtAddValue = DpInterestBasicApi.getAccruedInterest(subAcct, instAcct).getAccrual_inst();

						addValue = addValue.add(rtAddValue);
					}

					// 查询交易一定要回滚事物
					DaoUtil.rollbackTransaction();

					// 交易日期、下次计息日期数据还原
					BizUtil.getTrxRunEnvs().setTrxn_date(trxnDate);
					subAcct.setNext_inst_date(nextInstDate);

					return addValue;
				}
			});

			totalAccrualInst = totalAccrualInst.add(addInst);
		}

		// 计算代扣利息税
		DpIntTaxInfo taxInfo = DpTaxApi.calcWithholdingTax(subAcct.getAcct_no(), subAcct.getSub_acct_no(), totalAccrualInst);

		cplOut.setInterest(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), totalAccrualInst));
		cplOut.setInterest_tax(ApCurrencyApi.roundAmount(subAcct.getCcy_code(), taxInfo.getAccrual_inst_tax()));
		cplOut.setInst_tax_rate(taxInfo.getInst_tax_rate());

		bizlog.method(" DpAcctInterestTrial.acctMatureProfitTrial end <<<<<<<<<<<<<<<<");
		return cplOut;
	}
}
