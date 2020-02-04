package cn.sunline.icore.dp.serv.account.save;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAdjustIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTADJUSTTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.froze.DpFroze;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpCommonDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeObjectIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeSaveIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeSaveOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.sys.type.EnumType.E_ADDSUBTRACT;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明：定期存款
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年1月10日-下午3:35:52</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月10日-HongBiao：定期存入</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTimeSave {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeSave.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午3:36:18</li>
	 *         <li>功能说明：定期存入</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpTimeSaveOut doMain(DpTimeSaveIn cplIn) {

		bizlog.method(" DpTimeSave.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 带锁定位账户表
		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.TIME);
		acctAccessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.SAVE);
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 存款子账户信息, 带锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 属性到期自动刷新
		DpAttrRefresh.refreshAttrValue(subAccount, account, cplIn.getAcct_no(), E_YESORNO.YES);

		// 检查主调方法
		DpTimeSaveCheck.checkMainMethod(cplIn, subAccount, account);

		// 主调处理方法
		DpTimeSaveOut cplOut = doMainMethod(cplIn, account, subAccount);

		bizlog.method(" DpTimeSave.doMain end <<<<<<<<<<<<<<<<");
		bizlog.debug("<<<<<cplOut=[%s]", cplOut);

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月10日-下午3:36:18</li>
	 *         <li>功能说明：定期存入</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpTimeSaveOut doMainMethod(DpTimeSaveIn cplIn, DpaAccount account, DpaSubAccount subAccount) {

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 存入计划更新
		if (subAccount.getDd_td_ind() == E_DEMANDORTIME.TIME && CommUtil.isNotNull(subAccount.getScheduled_dept_cycle())) {

			SqlDpCommonDao.updAccountDepositPlan(subAccount.getAcct_no(), subAccount.getSub_acct_no(), E_YESORNO.YES, trxnDate, subAccount.getOrg_id());
		}

		// 记账处理
		DpUpdAccBalOut accBalOut = dealAccounting(cplIn, subAccount);

		// 多次存入的定期，每次存入生成一笔卡片账: 含倒起息处理
		if (DpToolsApi.judgeTimeSlip(subAccount)) {

			DpBaseServiceApi.regTimeSlip(cplIn, subAccount);
		}
		else {

			// 倒起息
			if (CommUtil.isNotNull(cplIn.getBack_value_date()) && CommUtil.compare(cplIn.getBack_value_date(), trxnDate) < 0) {

				DpInstAdjustIn cplAdjustIn = BizUtil.getInstance(DpInstAdjustIn.class);

				cplAdjustIn.setEnd_inst_date(BizUtil.dateAdd("day", trxnDate, -1));
				cplAdjustIn.setInit_inst_start_date(subAccount.getStart_inst_date());
				cplAdjustIn.setInst_adjust_aspect(E_ADDSUBTRACT.ADD);
				cplAdjustIn.setInst_adjust_type(E_INSTADJUSTTYPE.BACKVALUE);
				cplAdjustIn.setStart_inst_date(cplIn.getBack_value_date());
				cplAdjustIn.setTrxn_amt(cplIn.getTrxn_amt());

				DpInterestBasicApi.adjustInstForBackvalue(subAccount, cplAdjustIn, E_YESORNO.YES);
			}
		}

		// 冻结止付处理
		DpFrozeOut cplFroze = BizUtil.getInstance(DpFrozeOut.class);

		if (CommUtil.isNotNull(cplIn.getFroze_kind_code())) {
			cplFroze = frozeDeal(cplIn, subAccount);
		}

		// 输出
		DpTimeSaveOut cplOut = BizUtil.getInstance(DpTimeSaveOut.class);
		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(account.getAcct_no());
		cplOut.setAcct_type(cplIn.getAcct_type());
		cplOut.setAcct_name(cplIn.getAcct_name());
		cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplOut.setCcy_code(cplIn.getCcy_code());
		cplOut.setCust_no(account.getCust_no());
		cplOut.setSub_acct_branch(subAccount.getSub_acct_branch());
		cplOut.setBranch_name(ApBranchApi.getItem(subAccount.getSub_acct_branch()).getBranch_name());
		cplOut.setAcct_bal(accBalOut.getAcct_bal()); // 交易后余额
		cplOut.setTrxn_amt(cplIn.getTrxn_amt());
		cplOut.setFroze_no(cplFroze.getFroze_no());

		bizlog.method(" DpTimeSave.timeSave end <<<<<<<<<<<<<<<<");
		bizlog.debug("cplOut=[%s]", cplOut);

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-下午4:26:07</li>
	 *         <li>功能说明：调用冻结功能.</li>
	 *         </p>
	 * @param cplIn
	 *            活期存入服务输入接口
	 * @param subAcct
	 *            子账户信息
	 * @return 冻结编号
	 */
	private static DpFrozeOut frozeDeal(DpTimeSaveIn cplIn, DpaSubAccount subAcct) {

		DpFrozeIn dpFrozeIn = BizUtil.getInstance(DpFrozeIn.class);

		dpFrozeIn.setFroze_kind_code(cplIn.getFroze_kind_code());
		dpFrozeIn.setFroze_object_type(cplIn.getFroze_object_type());
		dpFrozeIn.setAcct_no(subAcct.getAcct_no());
		dpFrozeIn.setAcct_type(cplIn.getAcct_type());
		dpFrozeIn.setAcct_name(cplIn.getAcct_name());
		dpFrozeIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		dpFrozeIn.setCcy_code(subAcct.getCcy_code());
		dpFrozeIn.setProd_id(subAcct.getProd_id());
		dpFrozeIn.setFroze_amt(cplIn.getFroze_amt());
		dpFrozeIn.setFroze_reason(cplIn.getFroze_reason());
		dpFrozeIn.setFroze_due_date(cplIn.getFroze_due_date());
		dpFrozeIn.setFroze_before_save_amt(cplIn.getTrxn_amt());

		// 同客户调用方法效率更高
		return DpFroze.doMain(dpFrozeIn);
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月19日-下午6:46:21</li>
	 *         <li>功能说明：定期存入记账处理</li>
	 *         </p>
	 * @param cplIn
	 *            定期存入服务输入接口
	 * @param subAcct
	 *            子账户信息输入
	 * @param event
	 *            交易事件
	 * @return DpUpdAccBalOut 账户余额更新输出接口
	 */
	private static DpUpdAccBalOut dealAccounting(DpTimeSaveIn cplIn, DpaSubAccount subAcct) {

		bizlog.method(" DpTimeSave.dealAccounting begin >>>>>>>>>>>>>>>>");

		DpUpdAccBalIn cplInput = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplInput.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAcct.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplInput.setAcct_no(subAcct.getAcct_no()); // 账号
		cplInput.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		cplInput.setBack_value_date(CommUtil.nvl(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date())); // 起息日期
		cplInput.setTrxn_amt(cplIn.getTrxn_amt()); // 交易金额
		cplInput.setTrxn_ccy(cplIn.getCcy_code()); // 交易币种
		cplInput.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_DEBITCREDIT.CREDIT : E_DEBITCREDIT.DEBIT); // 记账方向
		cplInput.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
		cplInput.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplInput.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplInput.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
		cplInput.setSummary_code(cplIn.getSummary_code()); // 摘要代码
		cplInput.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
		cplInput.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
		cplInput.setTrxn_event_id(E_DEPTTRXNEVENT.DP_TIME_SAVE.getValue());

		// 交易对手
		cplInput.setOpp_acct_route(cplIn.getOpp_acct_route()); // 对方账户路由
		cplInput.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
		cplInput.setOpp_acct_ccy(cplIn.getOpp_acct_ccy()); // 对方币种
		cplInput.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		cplInput.setOpp_branch_id(cplIn.getOpp_branch_id()); // 对方机构号

		// 代理人信息
		cplInput.setAgent_doc_type(cplIn.getAgent_doc_type()); // 代理人证件类型
		cplInput.setAgent_doc_no(cplIn.getAgent_doc_no()); // 代理人证件号
		cplInput.setAgent_name(cplIn.getAgent_name()); // 代理人姓名
		cplInput.setAgent_country(cplIn.getAgent_country()); // 代理人国籍

		DpUpdAccBalOut accBalOut = DpAccounting.online(cplInput);

		bizlog.method(" DpTimeSave.dealAccounting end <<<<<<<<<<<<<<<<");
		return accBalOut;
	}

}
