package cn.sunline.icore.dp.serv.account.draw;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInstAccounting;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_FROZETYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_TRXNRECORDTYPE;
import cn.sunline.icore.dp.serv.account.close.DpCloseSubAccount;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpAccounting;
import cn.sunline.icore.dp.serv.dayend.DpDayEndInterest;
import cn.sunline.icore.dp.serv.errors.DpErr.Dp;
import cn.sunline.icore.dp.serv.froze.DpFrozePublic;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.interest.DpInterestSettlement;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpCommonDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDeposit;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbSmartDepositDao;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpRegInstBill;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpUpdAccBalIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeOut;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawIn;
import cn.sunline.icore.dp.serv.type.ComDpTimeAccounting.DpTimeDrawOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpVoucherChangeIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_DRAWBUSIKIND;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZESTATUS;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：定期支取相关
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年2月9日-下午4:11:48</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTimeDraw {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeDraw.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月9日-下午4:11:58</li>
	 *         <li>功能说明：定期支取主服务</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 * @return 定期支取服务输出接口
	 */
	public static DpTimeDrawOut doMain(DpTimeDrawIn cplIn) {

		bizlog.method(" DpTimeDraw.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 定位账户信息, 带锁避免并发解冻
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 获取子账户信息：带锁
		DpaSubAccount subAcct = DpToolsApi.getTdSubAcct(acctInfo.getAcct_no(), cplIn.getSub_acct_seq(), E_YESORNO.YES);

		// 属性到期自动刷新
		DpAttrRefresh.refreshAttrValue(subAcct, acctInfo, cplIn.getAcct_no(), E_YESORNO.YES);

		// 定期支取主调检查
		DpTimeDrawCheck.checkMainMethod(cplIn, acctInfo, subAcct);

		// 定期支取主调方法
		DpTimeDrawOut cplOut = doMainMethod(cplIn, acctInfo, subAcct);

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpTimeDraw.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月9日-下午4:11:58</li>
	 *         <li>功能说明：定期支取主调方法</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 * @return 定期支取服务输出接口
	 */
	public static DpTimeDrawOut doMainMethod(DpTimeDrawIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		E_FROZESTATUS frozeStutas = null;
		// 1.解除止付处理
		if (CommUtil.isNotNull(cplIn.getFroze_no())) {
			frozeStutas = cancelFroze(cplIn, subAcct.getAcct_no(), subAcct.getSub_acct_no());
		}

		// 2.为支持7*24小时，需要联机处理到期解冻
		boolean existsUnfroze = DpUnFroze.matureAutoUnfrozen(acctInfo.getCust_no());

		// 存在解冻标志，里面可能更新冻结标志状态
		if (existsUnfroze) {
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);
		}

		// 3. 日切后计息前联机处理利息
		if (CommUtil.compare(subAcct.getNext_inst_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {

			DpDayEndInterest.onlineDealInterest(subAcct);

			// 再次读取账户信息
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);

			// 日终结息可能利息入定期账户自身，从而导致账户余额发生变化
			if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.CLOSE) {
				cplIn.setTrxn_amt(subAcct.getAcct_bal());
			}
		}

		// 判断是否销户
		E_YESORNO closeAcctFlag = cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.CLOSE ? E_YESORNO.YES : E_YESORNO.NO;

		// 如果是销户支取需考虑智能存款协议
		if (E_YESORNO.YES == closeAcctFlag) {

			// 智能存款协议解除了则全额支取了才能销智能存款子户
			DpbSmartDeposit smartInfo = DpbSmartDepositDao.selectFirst_odb2(acctInfo.getAcct_no(), cplIn.getSub_acct_seq(), E_STATUS.VALID, false);

			if (CommUtil.isNotNull(smartInfo)) {
				closeAcctFlag = E_YESORNO.NO;
			}
		}

		// 5. 定期支取付息处理，在本金记账之前
		payInterest(cplIn, subAcct.getAcct_no(), subAcct.getSub_acct_no());

		// 6. 凭证更换处理
		if (CommUtil.isNotNull(cplIn.getNew_voch_no())) {
			replaceOpenVoucher(cplIn, acctInfo, subAcct);
		}

		// 7. 定期支取记账: 可能因扣划导致账户余额为零，因此0金额销户也有可能
		if (CommUtil.compare(cplIn.getTrxn_amt(), BigDecimal.ZERO) > 0) {

			tallyAccounting(cplIn, subAcct.getAcct_no(), subAcct.getSub_acct_no());
			
			// 再次读取最新账户信息
			subAcct = DpaSubAccountDao.selectOne_odb1(subAcct.getAcct_no(), subAcct.getSub_acct_no(), true);
		}
		else {
			// 零金额记账发送消息
			pushNotice(cplIn, subAcct);
		}

		// 8. 更新支取计划
		if (CommUtil.isNotNull(subAcct.getScheduled_withdrawal_cycle()) && cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.COMMON) {
			SqlDpCommonDao.updAccountDrawPlan(subAcct.getAcct_no(), subAcct.getSub_acct_no(), E_YESORNO.YES, BizUtil.getTrxRunEnvs().getTrxn_date(), subAcct.getOrg_id());
		}

		// 9. 定期支取销户处理
		if (closeAcctFlag == E_YESORNO.YES) {
			closeAcct(cplIn, subAcct, acctInfo);
		}

		// 输出赋值
		DpTimeDrawOut cplOut = BizUtil.getInstance(DpTimeDrawOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_type(acctInfo.getAcct_type()); // 账户类型
		cplOut.setAcct_name(subAcct.getSub_acct_name()); // 账户名称
		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setProd_id(subAcct.getProd_id()); // 产品编号
		cplOut.setProd_name(DpProductFactoryApi.getProdBaseInfo(subAcct.getProd_id()).getProd_name()); // 产品名称
		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setSub_acct_branch(subAcct.getSub_acct_branch()); // 子账户所属机构
		cplOut.setBranch_name(ApBranchApi.getItem(subAcct.getSub_acct_branch()).getBranch_name()); // 机构名称
		cplOut.setAcct_bal(subAcct.getAcct_bal()); // 交易后账户余额
		cplOut.setFroze_no(cplIn.getFroze_no()); // 冻结编号
		cplOut.setFroze_status(frozeStutas);// 冻结状态
		cplOut.setVoch_type(cplIn.getVoch_type()); // 凭证类型
		cplOut.setVoch_no(cplIn.getVoch_no()); // 凭证号码
		cplOut.setTerm_code(subAcct.getTerm_code()); // 存期
		cplOut.setAct_withdrawal_amt(cplIn.getTrxn_amt());// 实际支取金额
		cplOut.setInterest(cplIn.getInterest()); // 利息
		cplOut.setInterest_tax(cplIn.getInterest_tax()); // 利息税
		cplOut.setTax_after_inst_amt(cplIn.getTax_after_inst_amt()); // 税后利息
		// cplOut.setEfft_inrt(BigDecimal.ZERO); // 账户执行利率
		// cplOut.setOd_interest_rate(BigDecimal.ZERO); // 逾期利率

		// 应付金额 = 实际支取金额 + 税后利息
		cplOut.setPaying_amt(cplOut.getAct_withdrawal_amt().add(cplIn.getTax_after_inst_amt()));

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月18日-上午10:08:17</li>
	 *         <li>功能说明：定期支取销户处理</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入
	 * @param subAcct
	 *            子账户信息
	 */
	private static void closeAcct(DpTimeDrawIn cplIn, DpaSubAccount subAcct, DpaAccount acctInfo) {

		bizlog.method(" DpTimeDraw.closeAcct begin >>>>>>>>>>>>>>>>");

		DpCloseSubAccountIn cplCloseIn = BizUtil.getInstance(DpCloseSubAccountIn.class);

		cplCloseIn.setAcct_name(acctInfo.getAcct_name());

		cplCloseIn.setAcct_no(subAcct.getAcct_no());

		// 卡账关系需要送卡号进行交易
		if (acctInfo.getCard_relationship_ind() == E_YESORNO.YES) {
			String cardNo = DpToolsApi.getCardNoByAcctNo(acctInfo.getAcct_no());
			cplCloseIn.setAcct_no(cardNo);
		}

		cplCloseIn.setAcct_type(acctInfo.getAcct_type());
		cplCloseIn.setCash_trxn_ind(cplIn.getCash_trxn_ind());
		cplCloseIn.setCcy_code(subAcct.getCcy_code());
		cplCloseIn.setCheck_password_ind(E_YESORNO.NO);
		cplCloseIn.setProd_id(subAcct.getProd_id());
		cplCloseIn.setRemark(cplIn.getTrxn_remark());
		cplCloseIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplCloseIn.setProd_id(subAcct.getProd_id()); // product code
		cplCloseIn.setSummary_code(cplIn.getSummary_code()); // summary code

		DpCloseSubAccount.doMain(cplCloseIn);

		bizlog.method(" DpTimeDraw.closeAcct end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月17日-下午6:52:24</li>
	 *         <li>功能说明：解除止付</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 */
	private static E_FROZESTATUS cancelFroze(DpTimeDrawIn cplIn, String acctNo, String subAcctNo) {

		bizlog.method(" DpTimeDraw.cancelFroze begin >>>>>>>>>>>>>>>>");

		// 1.检查是否全额冻结，全额冻结不做解冻处理 , 全额冻结需要自行去解冻交易处理
		DpbFroze frozeInfo = DpFrozePublic.getFirstForzeInfo(cplIn.getFroze_no());

		// 状态冻结在销户时可以解冻，其他支取类型不能解冻
		if (frozeInfo.getFroze_type() != E_FROZETYPE.AMOUNT && cplIn.getWithdrawal_busi_type() != E_DRAWBUSIKIND.CLOSE) {

			return frozeInfo.getFroze_status();
		}

		// 重新读取数据
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		// 解冻服务输入
		DpUnFrozeIn cplUnFrIn = BizUtil.getInstance(DpUnFrozeIn.class);

		cplUnFrIn.setFroze_no(cplIn.getFroze_no());
		cplUnFrIn.setUnfroze_reason(cplIn.getUnfroze_reason());
		cplUnFrIn.setWithdrawal_busi_type(cplIn.getWithdrawal_busi_type());
		cplUnFrIn.setAcct_no(cplIn.getAcct_no());
		cplUnFrIn.setAcct_type(cplIn.getAcct_type());
		cplUnFrIn.setCcy_code(subAcct.getCcy_code());
		cplUnFrIn.setCust_no(subAcct.getCust_no());
		cplUnFrIn.setProd_id(subAcct.getProd_id());
		cplUnFrIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplUnFrIn.setUnfroze_amt(cplIn.getUnfroze_amt());

		// 同客户解冻，调方法，不调服务，这样效率更高
		DpUnFrozeOut unFrozeOut = DpUnFroze.doMain(cplUnFrIn);

		bizlog.method(" DpTimeDraw.cancelFroze end <<<<<<<<<<<<<<<<");

		return unFrozeOut.getFroze_status();
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月18日-上午10:08:17</li>
	 *         <li>功能说明：定期支取付息处理</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入
	 * @param acctNo
	 *            s账号
	 * @param subAcctNo
	 *            子账号
	 */
	private static void payInterest(DpTimeDrawIn cplIn, String acctNo, String subAcctNo) {

		// 定期付息
		DpInstAccounting cplPayed = DpBaseServiceApi.timePayInterest(cplIn, acctNo, subAcctNo);

		// 再次读取缓存中最新子户信息
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		// 利息记账处理，注意可能为负数
		if (CommUtil.compare(cplPayed.getInterest(), BigDecimal.ZERO) != 0) {

			// 利息记账
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
			instBill.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
			instBill.setShow_ind(E_YESORNO.YES); // 是否显示标志
			instBill.setSummary_code(cplIn.getSummary_code()); // 摘要代码
			instBill.setOpp_acct_route(cplIn.getOpp_acct_route()); // 对方账户路由
			instBill.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
			instBill.setOpp_acct_type(cplIn.getOpp_acct_type()); // 对方账户类型
			instBill.setOpp_acct_ccy(cplIn.getOpp_acct_ccy()); // 对方账户币种
			instBill.setOpp_branch_id(cplIn.getOpp_branch_id()); // 对方机构号
			instBill.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
			instBill.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
			instBill.setAgent_name(cplIn.getAgent_name()); // 代理人姓名
			instBill.setAgent_doc_type(cplIn.getAgent_doc_type()); // 代理人证件类型
			instBill.setAgent_doc_no(cplIn.getAgent_doc_no()); // 代理人证件号
			instBill.setAgent_country(cplIn.getAgent_country()); // 代理人国籍

			// 登记过息账单
			DpAccounting.registerOtherBill(instBill, subAcct.getAcct_no(), subAcct.getSub_acct_no());
		}

		// 检查输入利息值和实际计算利息值
		checkDrawInterest(cplIn, cplPayed.getInterest(), cplPayed.getInterest_tax());

		// 返写利息金额信息
		cplIn.setInterest(cplPayed.getInterest());
		cplIn.setInterest_tax(cplPayed.getInterest_tax());
		cplIn.setTax_after_inst_amt(cplIn.getInterest().subtract(cplIn.getInterest_tax()));
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月9日-下午6:52:24</li>
	 *         <li>功能说明：定期支取记账处理</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 * @param acctNo
	 *            账户号
	 * @param subAcctNo
	 *            子账户号
	 */
	public static void tallyAccounting(DpTimeDrawIn cplIn, String acctNo, String subAcctNo) {

		bizlog.method(" DpTimeDraw.tallyAccounting begin >>>>>>>>>>>>>>>>");

		// 读取子户信息
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		DpUpdAccBalIn cplAccBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplAccBalIn.setCard_no(CommUtil.equals(subAcct.getAcct_no(), cplIn.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplAccBalIn.setAcct_no(subAcct.getAcct_no()); // 账号
		cplAccBalIn.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		cplAccBalIn.setBack_value_date(CommUtil.nvl(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date())); // 起息日期
		cplAccBalIn.setTrxn_amt(cplIn.getTrxn_amt()); // 交易金额
		cplAccBalIn.setTrxn_ccy(cplIn.getCcy_code()); // 交易币种
		cplAccBalIn.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
		cplAccBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplAccBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplAccBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
		cplAccBalIn.setChrg_code(cplIn.getChrg_code()); // 费用编号
		cplAccBalIn.setSummary_code(cplIn.getSummary_code()); // 摘要代码
		cplAccBalIn.setVoch_type(cplIn.getVoch_type()); // 凭证类型
		cplAccBalIn.setVoch_no(cplIn.getVoch_no()); // 凭证号码
		cplAccBalIn.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
		cplAccBalIn.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		cplAccBalIn.setOpp_acct_type(cplIn.getOpp_acct_type());
		cplAccBalIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		cplAccBalIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		cplAccBalIn.setOpp_branch_id(cplIn.getOpp_branch_id());
		cplAccBalIn.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
		cplAccBalIn.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
		cplAccBalIn.setAgent_doc_type(cplIn.getAgent_doc_type()); // 代理人证件类型
		cplAccBalIn.setAgent_doc_no(cplIn.getAgent_doc_no()); // 代理人证件号
		cplAccBalIn.setAgent_name(cplIn.getAgent_name()); // 代理人姓名
		cplAccBalIn.setAgent_country(cplIn.getAgent_country()); // 代理人国籍

		// 交易事件
		cplAccBalIn.setTrxn_event_id(E_DEPTTRXNEVENT.DP_TIME_DRAW.getValue());
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.CLOSE) {
			cplAccBalIn.setTrxn_event_id(E_DEPTTRXNEVENT.DP_CLOSE_SUBACCT.getValue());
		}

		// 记账方向
		// 负债账户
		if (subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT) {

			cplAccBalIn.setDebit_credit(E_DEBITCREDIT.DEBIT);
		}
		// 资产账户
		else {

			cplAccBalIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
		}

		// 本金记账
		DpAccounting.online(cplAccBalIn);

		// 卡片记账
		if (DpToolsApi.judgeTimeSlip(subAcct)) {

			DpBaseServiceApi.timeSlipAccounting(cplIn, subAcct);
		}

		bizlog.method(" DpTimeDraw.tallyAccounting end <<<<<<<<<<<<<<<<");

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月9日-下午6:52:24</li>
	 *         <li>功能说明：定期支取存单更换</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入接口
	 * @param acctInfo
	 *            账户信息
	 * @param subAcct
	 *            子账户信息
	 */
	private static void replaceOpenVoucher(DpTimeDrawIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		bizlog.method(" DpTimeDraw.replaceOpenVoucher begin >>>>>>>>>>>>>>>>");

		// 没有输入新凭证号，则直接退出
		if (CommUtil.isNull(cplIn.getNew_voch_no())) {
			bizlog.method(" DpTimeDraw.replaceOpenVoucher end <<<<<<<<<<<<<<<<");
			return;
		}

		DpVoucherChangeIn cplVochIn = BizUtil.getInstance(DpVoucherChangeIn.class);

		cplVochIn.setAcct_no(subAcct.getAcct_no());
		cplVochIn.setCcy_code(subAcct.getCcy_code());
		cplVochIn.setChg_passbook_reason(""); // TODO:
		cplVochIn.setNew_voch_no(cplIn.getNew_voch_no());
		cplVochIn.setNew_voch_type(cplIn.getVoch_type());
		cplVochIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplVochIn.setSummary_code(cplIn.getSummary_code());
		cplVochIn.setRef_voch_level(acctInfo.getRef_voch_level());

		DpVoucherIobus.changeVoucher(cplVochIn);

		bizlog.method(" DpTimeDraw.replaceOpenVoucher end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2015年5月28日-上午10:14:38</li>
	 *         <li>检查定期支取利息</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入
	 * @param instValue
	 *            利息加待支取利息
	 * @param instTax
	 *            利息税加待支取利息税
	 */
	private static void checkDrawInterest(DpTimeDrawIn cplIn, BigDecimal instValue, BigDecimal instTax) {

		// 输入利息和试算利息不相等
		if (CommUtil.isNotNull(cplIn.getInterest()) && !CommUtil.equals(cplIn.getInterest(), instValue)) {
			throw Dp.E0112();
		}

		// 输入利息税和试算利息税不相等
		if (CommUtil.isNotNull(cplIn.getInterest_tax()) && !CommUtil.equals(cplIn.getInterest_tax(), instTax)) {
			throw Dp.E0113();
		}

		// 税后利息
		BigDecimal taxAfterInst = instValue.subtract(instTax);

		// 税后利息比较
		if (CommUtil.isNotNull(cplIn.getTax_after_inst_amt()) && !CommUtil.equals(cplIn.getTax_after_inst_amt(), taxAfterInst)) {
			throw Dp.E0158(cplIn.getTax_after_inst_amt(), taxAfterInst);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2015年5月28日-上午10:14:38</li>
	 *         <li>推送消息</li>
	 *         </p>
	 * @param cplIn
	 *            定期支取服务输入
	 * @param instValue
	 *            利息加待支取利息
	 * @param instTax
	 *            利息税加待支取利息税
	 */
	private static void pushNotice(DpTimeDrawIn cplIn, DpaSubAccount subAcct) {

		DpUpdAccBalIn cplAccBalIn = BizUtil.getInstance(DpUpdAccBalIn.class);

		cplAccBalIn.setCard_no(CommUtil.equals(subAcct.getAcct_no(), cplIn.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		cplAccBalIn.setAcct_no(subAcct.getAcct_no()); // 账号
		cplAccBalIn.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		cplAccBalIn.setBack_value_date(CommUtil.nvl(cplIn.getBack_value_date(), BizUtil.getTrxRunEnvs().getTrxn_date())); // 起息日期
		cplAccBalIn.setTrxn_amt(cplIn.getTrxn_amt()); // 交易金额
		cplAccBalIn.setTrxn_ccy(cplIn.getCcy_code()); // 交易币种
		cplAccBalIn.setCash_trxn_ind(cplIn.getCash_trxn_ind()); // 现转标志
		cplAccBalIn.setTally_record_ind(E_YESORNO.YES); // 是否记账记录标志
		cplAccBalIn.setShow_ind(E_YESORNO.YES); // 是否显示标志
		cplAccBalIn.setTrxn_record_type(E_TRXNRECORDTYPE.NORMAL); // 交易明细类别
		cplAccBalIn.setChrg_code(cplIn.getChrg_code()); // 费用编号
		cplAccBalIn.setSummary_code(cplIn.getSummary_code()); // 摘要代码
		cplAccBalIn.setVoch_type(cplIn.getVoch_type()); // 凭证类型
		cplAccBalIn.setVoch_no(cplIn.getVoch_no()); // 凭证号码
		cplAccBalIn.setOpp_acct_no(cplIn.getOpp_acct_no()); // 对方账号
		cplAccBalIn.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		cplAccBalIn.setOpp_acct_type(cplIn.getOpp_acct_type());
		cplAccBalIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		cplAccBalIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		cplAccBalIn.setOpp_branch_id(cplIn.getOpp_branch_id());
		cplAccBalIn.setTrxn_remark(cplIn.getTrxn_remark()); // 交易备注
		cplAccBalIn.setCustomer_remark(cplIn.getCustomer_remark()); // 客户备注
		cplAccBalIn.setAgent_doc_type(cplIn.getAgent_doc_type()); // 代理人证件类型
		cplAccBalIn.setAgent_doc_no(cplIn.getAgent_doc_no()); // 代理人证件号
		cplAccBalIn.setAgent_name(cplIn.getAgent_name()); // 代理人姓名
		cplAccBalIn.setAgent_country(cplIn.getAgent_country()); // 代理人国籍

		// 交易事件
		cplAccBalIn.setTrxn_event_id(E_DEPTTRXNEVENT.DP_TIME_DRAW.getValue());
		if (cplIn.getWithdrawal_busi_type() == E_DRAWBUSIKIND.CLOSE) {
			cplAccBalIn.setTrxn_event_id(E_DEPTTRXNEVENT.DP_CLOSE_SUBACCT.getValue());
		}

		cplAccBalIn.setDebit_credit(subAcct.getAsst_liab_ind() == E_ASSETORDEBT.DEBT ? E_DEBITCREDIT.DEBIT : E_DEBITCREDIT.CREDIT);

		DpAccounting.updAccBalPushNotice(cplAccBalIn, subAcct, BigDecimal.ZERO);
	}
}
