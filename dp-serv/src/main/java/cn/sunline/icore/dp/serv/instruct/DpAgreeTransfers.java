package cn.sunline.icore.dp.serv.instruct;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.common.DpDataFormat;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInstructDao;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBill;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBillDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTransfers;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTransfersDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbAgreeTrsfDetail;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersDetail;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersDetailQryIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersDetailQryOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersInfo;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersMntIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersQryIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersQryOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersSignIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpAgreeTransfersSignOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAccountRouteInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_AGREETRSFAMOUNTTYPE;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_AGREETRSFTYPE;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_FAILHANDLINGMETHOD;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApBaseErr;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.base.util.DateTimeUtil_;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：
 * </p>
 * 
 * @Author shenxy
 *         <p>
 *         <li>2017年7月25日-下午4:15:14</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年7月25日-shenxy：约定转账相关处理</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAgreeTransfers {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAgreeTransfers.class);

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月25日-下午3:55:39</li>
	 *         <li>功能说明：约定转账签约</li>
	 *         </p>
	 * @param cplIn
	 *            约定转账签约输入
	 * @return DpAgreeTransfersSignOut 约定转账签约输出
	 */
	public static DpAgreeTransfersSignOut agreeTransfersSign(DpAgreeTransfersSignIn cplIn) {
		bizlog.method(" DpAgreeTransfers.agreeTransfersSign begin >>>>>>>>>>>>>>>>");
		bizlog.debug("DpAgreeTransfers.cplIn = [%s]", cplIn);

		// 检查输入数据
		checkSignInputData(cplIn);

		// 转出账户定位
		DpAcctAccessIn cplAcctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		cplAcctAccessIn.setAcct_no(cplIn.getAcct_no());
		cplAcctAccessIn.setAcct_type(cplIn.getAcct_type());
		cplAcctAccessIn.setProd_id(cplIn.getProd_id());
		cplAcctAccessIn.setCcy_code(cplIn.getCcy_code());
		cplAcctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplAcctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

		DpAcctAccessOut cplAcctAccessOut = DpToolsApi.locateSingleSubAcct(cplAcctAccessIn);

		cplIn.setSub_acct_seq(cplAcctAccessOut.getSub_acct_seq());

		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(cplAcctAccessOut.getAcct_no(), cplAcctAccessOut.getSub_acct_no(), false);

		// 签约时检查限制状态, 很多银行要求签约时要检查
		DpPublicCheck.checkSubAcctTrxnLimit(subAccount, E_DEPTTRXNEVENT.DP_DRAW, null);

		// 检查交易密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(cplAcctAccessOut.getAcct_no(), true);

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}
		//验证机构号是否存在
		if(!cplIn.getOpp_branch_id().isEmpty()&&!ApBranchApi.exists(cplIn.getOpp_branch_id())){
					throw ApBaseErr.ApBase.E0014(cplIn.getOpp_branch_id());
			}
		
		if (cplIn.getAgree_trsf_type() == E_AGREETRSFTYPE.DEMAND_TO_DEMAND) {

			DpAcctAccessIn cplOppAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			cplOppAccessIn.setAcct_no(cplIn.getOpp_acct_no());
			cplOppAccessIn.setAcct_type(cplIn.getOpp_acct_type());
			cplOppAccessIn.setCcy_code(CommUtil.nvl(cplIn.getOpp_acct_ccy(), subAccount.getCcy_code()));
			cplOppAccessIn.setProd_id(cplIn.getOpp_prod_id());
			cplOppAccessIn.setSub_acct_seq(cplIn.getOpp_sub_acct_seq());

			DpAcctAccessOut cplOppAccessOut = DpToolsApi.locateSingleSubAcct(cplOppAccessIn);
			
			if (CommUtil.equals(cplOppAccessOut.getSub_acct_no(), subAccount.getSub_acct_no())) {
				throw DpErr.Dp.E0100();
			}

			cplIn.setOpp_sub_acct_seq(cplOppAccessOut.getSub_acct_seq());

			DpaSubAccount oppSubAccount = DpaSubAccountDao.selectOne_odb1(cplOppAccessOut.getAcct_no(), cplOppAccessOut.getSub_acct_no(), true);

			// 很多银行签约时检查限制状态
			DpPublicCheck.checkSubAcctTrxnLimit(oppSubAccount, E_DEPTTRXNEVENT.DP_SAVE, null);
		}
		
		if (cplIn.getAgree_trsf_type() == E_AGREETRSFTYPE.DEMAND_TO_INSIDE) {
			//对方账务机构可以为空， 当对方为业务编码时， 账务机构为空时默认为活期账户的账务机构
			if(cplIn.getOpp_branch_id().isEmpty()){
				cplIn.setOpp_branch_id(cplAcctAccessOut.getAcct_branch());
			}
			//检查业务编码是否合法
			DpInsideAccountIobus.checkBusiCode(cplIn);
		}
		
		// 登记签约转账协议表
		String agreeNo = insertAgreeTransfers(cplIn, subAccount);

		// 初始化输出接口
		DpAgreeTransfersSignOut cplOut = BizUtil.getInstance(DpAgreeTransfersSignOut.class);

		cplOut.setAgree_no(agreeNo);// 协议号
		cplOut.setCust_no(subAccount.getCust_no());// 客户号
		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAccount.getAcct_no()) ? null : cplIn.getAcct_no());// 卡号
		cplOut.setAcct_no(cplAcctAccessOut.getAcct_no());// 账号
		cplOut.setAcct_name(cplAcctAccessOut.getAcct_name());// 账户名称
		cplOut.setSub_acct_seq(cplAcctAccessOut.getSub_acct_seq());// 子账户序号
		cplOut.setCcy_code(cplAcctAccessOut.getCcy_code());// 货币代号
		cplOut.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), BigDecimal.ZERO));

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAgreeTransfers.agreeTransfersSign end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年12月22日-上午10:00:28</li>
	 *         <li>功能说明：计算下一执行日</li>
	 *         </p>
	 * @param cycle
	 *            周期
	 * @param refDate
	 *            参考日期
	 * @param brief
	 *            简明日期字符
	 * @param effectDate
	 *            生效日期
	 * @return 下一执行日
	 */
	private static String getNextExecDate(String cycle, String refDate, String brief, String effectDate) {
		bizlog.method(" DpAgreeTransfers.getNextExecDate begin >>>>>>>>>>>>>>>>");

		// 约定周期不为空
		BizUtil.fieldNotNull(cycle, DpDict.A.agree_cycle.getId(), DpDict.A.agree_cycle.getLongName());

		// BizUtil.fieldNotNull(singleInfo.getRef_date(),
		// SysDict.A.ref_date.getId(), SysDict.A.ref_date.getLongName());

		if (!BizUtil.isCycleString(cycle)) {
			throw APPUB.E0012(cycle);
		}

		String nextExecDate = "";

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		refDate = CommUtil.nvl(refDate, trxnDate);

		if (CommUtil.isNull(brief)) {

			if (CommUtil.compare(trxnDate, refDate) < 0) {

				nextExecDate = refDate;
			}
			else if (CommUtil.compare(trxnDate, refDate) > 0) {

				nextExecDate = BizUtil.calcDateByReference(refDate, trxnDate, cycle);

			}
			else {

				nextExecDate = BizUtil.calcDateByRefernce(refDate, cycle);
			}
		}
		else {

			// refDate = getTrialRefDate(refDate, singleInfo.getAgree_cycle(),
			// singleInfo.getBrief_date_symbol());
			// nextExecDate = CommUtil.compare(refDate, trxnDate) <= 0 ?
			// DpPublic.calcDateByReference(refDate, trxnDate,
			// singleInfo.getAgree_cycle()) : refDate;

			nextExecDate = DpDataFormat.calcNextDate(refDate, BizUtil.calcDateByCycle(trxnDate, "-1D"), cycle, brief);

		}

		bizlog.method(" DpAgreeTransfers.getNextExecDate end <<<<<<<<<<<<<<<<");
		return nextExecDate;
	}

	/**
	 * @param subAccount
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月25日-下午5:13:25</li>
	 *         <li>功能说明：登记约定转账登记簿</li>
	 *         </p>
	 * @param cplIn
	 * @param nextExecDate
	 * @param agreeTrsf
	 * @return
	 */
	private static String insertAgreeTransfers(DpAgreeTransfersSignIn cplIn, DpaSubAccount subAccount) {
		bizlog.method(" DpAgreeTransfers.checkOppAcctValid begin >>>>>>>>>>>>>>>>");

		// 生成协议号
		String agreeNo = ApSeqApi.genSeq("AGREE_NO");

		String nextExecDate = getNextExecDate(cplIn.getAgree_cycle(), cplIn.getRef_date(), cplIn.getBrief_date_symbol(), cplIn.getEffect_date());

		if (CommUtil.compare(cplIn.getExpiry_date(), nextExecDate) < 0) {
			throw DpErr.Dp.E0411(cplIn.getExpiry_date(), nextExecDate);
		}

		// 初始化约定转账协议表
		DpbAgreeTransfers agreeTransfers = BizUtil.getInstance(DpbAgreeTransfers.class);

		agreeTransfers.setAgree_no(agreeNo);// 协议号
		agreeTransfers.setAgree_trsf_type(cplIn.getAgree_trsf_type());// 约定转账类型
		agreeTransfers.setAcct_no(subAccount.getAcct_no());// 账号
		agreeTransfers.setCcy_code(subAccount.getCcy_code());// 货币代号
		agreeTransfers.setSub_acct_seq(subAccount.getSub_acct_seq());
		agreeTransfers.setEffect_date(CommUtil.nvl(cplIn.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date()));// 生效日期
		agreeTransfers.setExpiry_date(CommUtil.nvl(cplIn.getExpiry_date(), ApConst.DEFAULT_MAX_DATE));// 失效日期
		agreeTransfers.setOpp_acct_no(cplIn.getOpp_acct_no());// 对方账户
		agreeTransfers.setOpp_acct_ccy(CommUtil.nvl(cplIn.getOpp_acct_ccy(), subAccount.getCcy_code()));// 对方账户币种
		agreeTransfers.setOpp_sub_acct_seq(cplIn.getOpp_sub_acct_seq());
		agreeTransfers.setOpp_branch_id(cplIn.getOpp_branch_id());// 对方机构号
		agreeTransfers.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), BigDecimal.ZERO));// 活期留存余额
		agreeTransfers.setAgree_trsf_amt_type(cplIn.getAgree_trsf_amt_type());
		agreeTransfers.setTrxn_amt(cplIn.getTrxn_amt());
		agreeTransfers.setMin_turn_out_amt(CommUtil.nvl(cplIn.getMin_turn_out_amt(), BigDecimal.ZERO));// 最小转出金额
		agreeTransfers.setMultiple_amt(CommUtil.nvl(cplIn.getMultiple_amt(), BigDecimal.ZERO));// 倍增金额
		agreeTransfers.setMax_turn_out_amt(CommUtil.nvl(cplIn.getMax_turn_out_amt(), BigDecimal.ZERO));// 最大转出金额
		agreeTransfers.setAgree_trsf_times(cplIn.getAgree_trsf_times());// 约定转账次数
		agreeTransfers.setAgree_trsf_amt(CommUtil.nvl(cplIn.getAgree_trsf_amt(), BigDecimal.ZERO));// 约定转账金额
		agreeTransfers.setAgree_cycle(cplIn.getAgree_cycle());// 约定周期
		agreeTransfers.setRef_date(CommUtil.nvl(cplIn.getRef_date(), BizUtil.getTrxRunEnvs().getTrxn_date()));// 参考日期
		agreeTransfers.setBrief_date_symbol(cplIn.getBrief_date_symbol());
		agreeTransfers.setExec_time(CommUtil.nvl(cplIn.getExec_time(), ApBusinessParmApi.getValue("AGREEMENT_TRSF_DEFAULT_TIME")));
		agreeTransfers.setLast_exec_date("");
		agreeTransfers.setNext_exec_date(nextExecDate);// 下次执行日
		agreeTransfers.setTotal_success_times(0L);// 累计成功次数
		agreeTransfers.setTotal_success_amt(BigDecimal.ZERO);// 累计成功金额
		agreeTransfers.setFail_handling_method(CommUtil.nvl(cplIn.getFail_handling_method(), E_FAILHANDLINGMETHOD.NOT_HANDLE));// 失败后处理方式
		agreeTransfers.setHash_value(BizUtil.getGroupHashValue(ApConst.WILDCARD, subAccount.getCust_no()));
		agreeTransfers.setFail_times(0L);
		agreeTransfers.setExternal_scene_code(BizUtil.getTrxRunEnvs().getExternal_scene_code());
		agreeTransfers.setTrxn_channel(BizUtil.getTrxRunEnvs().getChannel_id());
		agreeTransfers.setRemark(cplIn.getRemark());

		// 失败后处理相关设置
		if (cplIn.getFail_handling_method() == E_FAILHANDLINGMETHOD.BY_CYClE_AGAIN_TRANSFER) {

			// 设置值
			agreeTransfers.setFail_renew_trsf_cycle(cplIn.getFail_renew_trsf_cycle());// 失败重转周期
			agreeTransfers.setFail_renew_trsf_times(cplIn.getFail_renew_trsf_times());// 失败重转次数
			agreeTransfers.setFail_renew_trsf_date("");// 下次重转日期
		}

		agreeTransfers.setPrior_level(CommUtil.nvl(cplIn.getPrior_level(), 1L));
		agreeTransfers.setStop_use_ind(E_YESORNO.NO);
		agreeTransfers.setAgree_status(E_STATUS.VALID);// 协议状态
		agreeTransfers.setSign_date(BizUtil.getTrxRunEnvs().getTrxn_date());// 签约日期
		agreeTransfers.setSign_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());// 签约流水

		DpbAgreeTransfersDao.insert(agreeTransfers);

		bizlog.method(" DpAgreeTransfers.checkOppAcctValid end <<<<<<<<<<<<<<<<");
		return agreeNo;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月25日-下午4:03:10</li>
	 *         <li>功能说明：约定转账输入检查</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void checkSignInputData(DpAgreeTransfersSignIn cplIn) {
		bizlog.method(" DpAgreeTransfers.checkInputData begin >>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getAgree_trsf_type(), DpDict.A.agree_trsf_type.getId(), DpDict.A.agree_trsf_type.getLongName());
		BizUtil.fieldNotNull(cplIn.getAgree_trsf_amt_type(), DpDict.A.agree_trsf_amt_type.getId(), DpDict.A.agree_trsf_amt_type.getLongName());
		BizUtil.fieldNotNull(cplIn.getAgree_cycle(), DpDict.A.agree_cycle.getId(), DpDict.A.agree_cycle.getLongName());
		BizUtil.fieldNotNull(cplIn.getOpp_acct_no(), SysDict.A.opp_acct_no.getId(), SysDict.A.opp_acct_no.getLongName());

		if (cplIn.getAgree_trsf_amt_type() == E_AGREETRSFAMOUNTTYPE.FIXED) {

			BizUtil.fieldNotNull(cplIn.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

			if (CommUtil.compare(cplIn.getTrxn_amt(), BigDecimal.ZERO) <= 0) {
				throw APPUB.E0020(cplIn.getTrxn_amt().toString(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());
			}

			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getTrxn_amt());
		}
		else {

			BizUtil.fieldNotNull(cplIn.getMin_turn_out_amt(), DpDict.A.min_turn_out_amt.getId(), DpDict.A.min_turn_out_amt.getLongName());

			if (CommUtil.compare(cplIn.getMin_turn_out_amt(), BigDecimal.ZERO) <= 0) {
				throw APPUB.E0020(cplIn.getMin_turn_out_amt().toString(), DpDict.A.min_turn_out_amt.getId(), DpDict.A.min_turn_out_amt.getLongName());
			}

			ApCurrencyApi.chkAmountByCcy(cplIn.getCcy_code(), cplIn.getMin_turn_out_amt());

			// 最大转出金额要大于或等于最小转出金额
			if (CommUtil.isNotNull(cplIn.getMax_turn_out_amt()) && CommUtil.compare(BigDecimal.ZERO, cplIn.getMax_turn_out_amt()) != 0) {

				if (CommUtil.compare(cplIn.getMax_turn_out_amt(), cplIn.getMin_turn_out_amt()) < 0) {
					throw DpErr.Dp.E0328();
				}
			}
		}

		// 验密标志不为空
		if (CommUtil.isNull(cplIn.getCheck_password_ind())) {
			cplIn.setCheck_password_ind(E_YESORNO.NO);
		}

		if (CommUtil.isNull(cplIn.getEffect_date())) {
			cplIn.setEffect_date(BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		if (CommUtil.isNull(cplIn.getExpiry_date())) {
			cplIn.setExpiry_date(ApConst.DEFAULT_MAX_DATE);
		}

		BizUtil.checkEffectDate(cplIn.getEffect_date(), cplIn.getExpiry_date());

		checkValidityAndExpiryDateValidity(cplIn.getAgree_cycle(), cplIn.getEffect_date(), cplIn.getExpiry_date());

		// 失败后处理相关设置
		if (cplIn.getFail_handling_method() == E_FAILHANDLINGMETHOD.BY_CYClE_AGAIN_TRANSFER) {

			// 不为空校验
			BizUtil.fieldNotNull(cplIn.getFail_renew_trsf_cycle(), DpDict.A.fail_renew_trsf_cycle.getId(), DpDict.A.fail_renew_trsf_cycle.getLongName());
			BizUtil.fieldNotNull(cplIn.getFail_renew_trsf_times(), DpDict.A.fail_renew_trsf_times.getId(), DpDict.A.fail_renew_trsf_times.getLongName());

			// 格式校验
			if (!BizUtil.isCycleString(cplIn.getFail_renew_trsf_cycle())) {
				throw APPUB.E0012(cplIn.getFail_renew_trsf_cycle());
			}

			if (CommUtil.compare(cplIn.getFail_renew_trsf_times(), Long.valueOf(0)) <= 0) {
				throw DpErr.Dp.E0412();
			}
		}

		// 查询对方账户路由
		DpAccountRouteInfo analyOut = DpInsideAccountIobus.getAccountRouteInfo(cplIn.getOpp_acct_no(), E_CASHTRXN.TRXN);

		if (cplIn.getAgree_trsf_type() == E_AGREETRSFTYPE.DEMAND_TO_DEMAND) {

			if (analyOut.getAcct_analy() != E_ACCOUTANALY.DEPOSIT) {
				throw DpErr.Dp.E0356();
			}
		}
		else if (cplIn.getAgree_trsf_type() == E_AGREETRSFTYPE.DEMAND_TO_INSIDE) {

			if (analyOut.getAcct_analy() != E_ACCOUTANALY.INSIDE) {
				throw DpErr.Dp.E0356();
			}
		}
		else {
			throw DpErr.Dp.E0356();
		}
		
		//判断活期留存余额格式，为负数会抛出异常
		if (CommUtil.isNotNull(cplIn.getDemand_remain_bal()) && CommUtil.compare(cplIn.getDemand_remain_bal(), BigDecimal.ZERO) < 0){
			throw ApPubErr.APPUB.E0040(DpDict.A.demand_remain_bal.getLongName());
		}

		bizlog.method(" DpAgreeTransfers.checkInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月27日-上午10:27:50</li>
	 *         <li>功能说明：约定转账协议查询</li>
	 *         </p>
	 * @param cplIn
	 *            约定转账查询输入
	 * @return DpAgreeTransfersQryOut 约定转账查询输出
	 */
	public static DpAgreeTransfersQryOut agreeTransfersQry(DpAgreeTransfersQryIn cplIn) {
		bizlog.method(" DpAgreeTransfers.agreeTransfersQry begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		if (CommUtil.isNull(cplIn.getAgree_no()) && CommUtil.isNull(cplIn.getAcct_no())) {
			throw DpErr.Dp.E0297();
		}

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 获取公共变量

		String orgId = runEnvs.getBusi_org_id();// 取得法人代码

		// 动态查询约定转账表
		Page<DpbAgreeTransfers> page = SqlDpInstructDao.selAgreeTransfersInfo(cplIn.getAgree_no(), cplIn.getAcct_no(), cplIn.getAgree_status(), orgId, runEnvs.getPage_start(),
				runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(page.getRecordCount());// 返回总记录数

		// 初始化输出接口
		DpAgreeTransfersQryOut cplOut = BizUtil.getInstance(DpAgreeTransfersQryOut.class);

		if (CommUtil.isNull(page.getRecords()) && page.getRecords().size() == 0) {
			return cplOut;
		}

		// 获取账户信息
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(page.getRecords().get(0).getAcct_no());
		accessIn.setCcy_code(page.getRecords().get(0).getCcy_code());
		accessIn.setSub_acct_seq(page.getRecords().get(0).getSub_acct_seq());
		accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		Options<DpAgreeTransfersInfo> cplInfoList = cplOut.getList01();

		cplOut.setAcct_no(accessOut.getAcct_no());// 账号
		cplOut.setAcct_name(accessOut.getAcct_name());// 账户名称
		cplOut.setCcy_code(accessOut.getCcy_code());// 货币代号
		cplOut.setSub_acct_seq(accessOut.getSub_acct_seq());
		cplOut.setProd_id(accessOut.getProd_id());

		// 循环赋值
		for (DpbAgreeTransfers agreeTransfers : page.getRecords()) {

			DpAgreeTransfersInfo inrtInfo = BizUtil.getInstance(DpAgreeTransfersInfo.class);

			inrtInfo.setDemand_remain_bal(page.getRecords().get(0).getDemand_remain_bal());// 活期留存余额
			inrtInfo.setAgree_no(agreeTransfers.getAgree_no());// 协议号
			inrtInfo.setAgree_trsf_type(agreeTransfers.getAgree_trsf_type());// 约定转账类型
			inrtInfo.setEffect_date((agreeTransfers.getEffect_date()));// 生效日期
			inrtInfo.setExpiry_date(agreeTransfers.getExpiry_date());// 失效日期
			inrtInfo.setOpp_acct_no(agreeTransfers.getOpp_acct_no());// 对方账号
			inrtInfo.setOpp_acct_ccy(agreeTransfers.getOpp_acct_ccy());// 对方账户币种
			inrtInfo.setOpp_branch_id(agreeTransfers.getOpp_branch_id());// 对方机构号
			inrtInfo.setOpp_sub_acct_seq(agreeTransfers.getOpp_sub_acct_seq());
			inrtInfo.setAgree_trsf_amt_type(agreeTransfers.getAgree_trsf_amt_type());
			inrtInfo.setTrxn_amt(agreeTransfers.getTrxn_amt());
			inrtInfo.setMin_turn_out_amt(agreeTransfers.getMin_turn_out_amt());// 最小转出金额
			inrtInfo.setMultiple_amt(agreeTransfers.getMultiple_amt());// 倍增金额
			inrtInfo.setMax_turn_out_amt(agreeTransfers.getMax_turn_out_amt());// 最大转出金额
			inrtInfo.setAgree_trsf_times(agreeTransfers.getAgree_trsf_times());// 约定转账次数
			inrtInfo.setAgree_trsf_amt(agreeTransfers.getAgree_trsf_amt());// 约定转账金额
			inrtInfo.setAgree_cycle(agreeTransfers.getAgree_cycle());// 约定周期
			inrtInfo.setRef_date(agreeTransfers.getRef_date());// 参考周期
			inrtInfo.setLast_exec_date(agreeTransfers.getLast_exec_date());
			inrtInfo.setNext_exec_date(agreeTransfers.getNext_exec_date());// 下次执行日期
			inrtInfo.setTotal_success_times(agreeTransfers.getTotal_success_times());// 累计成功次数
			inrtInfo.setTotal_success_amt(agreeTransfers.getTotal_success_amt());// 累计成功金额
			inrtInfo.setFail_handling_method(agreeTransfers.getFail_handling_method());// 失败后处理方式
			inrtInfo.setFail_renew_trsf_cycle(agreeTransfers.getFail_renew_trsf_cycle());// 失败重转周期
			inrtInfo.setFail_renew_trsf_times(agreeTransfers.getFail_renew_trsf_times());// 失败重转次数
			inrtInfo.setFail_renew_trsf_date(agreeTransfers.getFail_renew_trsf_date());// 失败重日期
			inrtInfo.setFail_times(agreeTransfers.getFail_times());// 失败次数
			inrtInfo.setAgree_status(agreeTransfers.getAgree_status());// 协议状态

			// 如果协议为有效，并且到期日小于系统日期，协议状态显示为无效
			if (agreeTransfers.getAgree_status() == E_STATUS.VALID && CommUtil.isNotNull(agreeTransfers.getExpiry_date())) {

				if (CommUtil.compare(agreeTransfers.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {

					inrtInfo.setAgree_status(E_STATUS.INVALID);// 协议状态

				}
			}
			inrtInfo.setSign_date(agreeTransfers.getSign_date());// 签约日期
			inrtInfo.setSign_seq(agreeTransfers.getSign_seq());// 签约流水
			inrtInfo.setCancel_date(agreeTransfers.getCancel_date());// 解约日期
			inrtInfo.setCancel_seq(agreeTransfers.getCancel_seq());// 解约流水
			inrtInfo.setExec_time(agreeTransfers.getExec_time());
			inrtInfo.setBrief_date_symbol(agreeTransfers.getBrief_date_symbol());
			inrtInfo.setData_version(agreeTransfers.getData_version());// 数据版本号
			inrtInfo.setStop_use_ind(agreeTransfers.getStop_use_ind());
			inrtInfo.setData_create_time(agreeTransfers.getData_create_time());
			inrtInfo.setData_update_time(agreeTransfers.getData_update_time());

			cplInfoList.add(inrtInfo);
		}

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAgreeTransfers.agreeTransfersQry end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月27日-下午2:16:20</li>
	 *         <li>功能说明：约定转账协议维护</li>
	 *         </p>
	 * @param cplIn
	 *            约定转账协议维护输入
	 * @return DpAgreeTransfersSignOut 约定转账协议维护输出
	 */
	public static DpAgreeTransfersSignOut agreeTransfersMnt(DpAgreeTransfersMntIn cplIn) {
		bizlog.method(" DpAgreeTransfers.agreeTransfersMnt begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 协议号、是否解约标志、版本号不为空
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getAgree_no(), SysDict.A.agree_no.getId(), SysDict.A.agree_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCancle_agree_ind(), DpBaseDict.A.cancle_agree_ind.getId(), DpBaseDict.A.cancle_agree_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		// 查询约定转账协议表
		DpbAgreeTransfers agreeInfo = DpbAgreeTransfersDao.selectOneWithLock_odb1(acctInfo.getAcct_no(), cplIn.getAgree_no(), false);

		if (CommUtil.isNull(agreeInfo)) {
			throw APPUB.E0024(OdbFactory.getTable(DpbAgreeTransfers.class).getLongname(), SysDict.A.acct_no.getLongName(), acctInfo.getAcct_no(), SysDict.A.agree_no.getLongName(),
					cplIn.getAgree_no());
		}

		if (agreeInfo.getAgree_status() == E_STATUS.INVALID) {
			throw DpBase.E0296(cplIn.getAgree_no());
		}

		// 校验数据版本号
		if (CommUtil.compare(agreeInfo.getData_version(), cplIn.getData_version()) != 0) {
			throw ApPubErr.APPUB.E0018(DpbAgreeTransfers.class.getName());
		}

		// 检查交易密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		if (cplIn.getCancle_agree_ind() == E_YESORNO.YES) {// 解约
			cancleAgreeAgreeTransfers(cplIn, agreeInfo);
		}
		else {
			updateAgreeTransfers(cplIn, agreeInfo);
		}

		// 初始化输出接口
		DpAgreeTransfersSignOut cplOut = BizUtil.getInstance(DpAgreeTransfersSignOut.class);

		cplOut.setAgree_no(cplIn.getAgree_no());// 协议号
		cplOut.setCust_no(acctInfo.getCust_no());// 客户号
		cplOut.setCard_no(acctInfo.getCard_relationship_ind() == E_YESORNO.YES ? DpToolsApi.getCardNoByAcctNo(acctInfo.getAcct_no()) : null); // 卡号
		cplOut.setAcct_no(acctInfo.getAcct_no());// 账号
		cplOut.setAcct_name(acctInfo.getAcct_name());// 账户名称
		cplOut.setSub_acct_seq(agreeInfo.getSub_acct_seq());// 子账户序号
		cplOut.setCcy_code(agreeInfo.getCcy_code());// 货币代号
		cplOut.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), agreeInfo.getDemand_remain_bal()));// 活期留存金额

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAgreeTransfers.agreeTransfersMnt end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月27日-下午3:54:57</li>
	 *         <li>功能说明：约定转账解约</li>
	 *         </p>
	 * @param cplIn
	 *            约定转账维护输入
	 * @param agreeInfo
	 *            约定转账协议信息
	 */
	private static void cancleAgreeAgreeTransfers(DpAgreeTransfersMntIn cplIn, DpbAgreeTransfers agreeInfo) {
		bizlog.method(" DpAgreeTransfers.cancleAgreeAgreeTransfers begin >>>>>>>>>>>>>>>>");

		// 复制一份数据
		DpbAgreeTransfers copyAgreeTransfers = BizUtil.clone(DpbAgreeTransfers.class, agreeInfo);

		agreeInfo.setAgree_status(E_STATUS.INVALID);// 协议状态
		agreeInfo.setCancel_date(BizUtil.getTrxRunEnvs().getTrxn_date());// 解约日期
		agreeInfo.setCancel_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());// 解约流水
		// agreeInfo.setCancel_call_seq(BizUtil.getTrxRunEnvs().getCall_seq());

		// 登记审计
		ApDataAuditApi.regLogOnUpdateBusiness(copyAgreeTransfers, agreeInfo);

		DpbAgreeTransfersDao.updateOne_odb1(agreeInfo);

		bizlog.method(" DpAgreeTransfers.cancleAgreeAgreeTransfers end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月27日-下午3:59:45</li>
	 *         <li>功能说明：更新签约转账协议</li>
	 *         </p>
	 * @param cplIn
	 *            约定转账维护输入
	 * @param agreeInfo
	 *            约定转账协议信息
	 */
	private static void updateAgreeTransfers(DpAgreeTransfersMntIn cplIn, DpbAgreeTransfers agreeInfo) {
		bizlog.method(" DpAgreeTransfers.updateAgreeTransfers begin >>>>>>>>>>>>>>>>");

		if (CommUtil.isNotNull(cplIn.getAgree_cycle()) && !BizUtil.isCycleString(cplIn.getAgree_cycle())) {
			throw APPUB.E0012(cplIn.getAgree_cycle());
		}

		String cycle = CommUtil.nvl(cplIn.getAgree_cycle(), agreeInfo.getAgree_cycle());
		String refDate = CommUtil.nvl(cplIn.getRef_date(), agreeInfo.getRef_date());
		String brief = CommUtil.nvl(cplIn.getBrief_date_symbol(), agreeInfo.getBrief_date_symbol());
		String effectDate = CommUtil.nvl(cplIn.getEffect_date(), agreeInfo.getEffect_date());
		String expiryDate = CommUtil.nvl(cplIn.getExpiry_date(), agreeInfo.getExpiry_date());

		BizUtil.checkEffectDate(effectDate, expiryDate);

		checkValidityAndExpiryDateValidity(cycle, effectDate, expiryDate);

		String nextDate = getNextExecDate(cycle, refDate, brief, effectDate);

		if (CommUtil.compare(expiryDate, BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {
			throw DpBase.E0294(expiryDate, BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		if (CommUtil.compare(expiryDate, nextDate) < 0) {
			throw DpErr.Dp.E0411(expiryDate, nextDate);
		}

		// 复制一份数据
		DpbAgreeTransfers copyAgreeTransfers = BizUtil.clone(DpbAgreeTransfers.class, agreeInfo);

		agreeInfo.setEffect_date(effectDate);// 生效日期
		agreeInfo.setExpiry_date(expiryDate);// 失效日期
		agreeInfo.setAgree_trsf_amt_type(CommUtil.nvl(cplIn.getAgree_trsf_amt_type(), agreeInfo.getAgree_trsf_amt_type()));
		agreeInfo.setTrxn_amt(CommUtil.nvl(cplIn.getTrxn_amt(), agreeInfo.getTrxn_amt()));
		agreeInfo.setMin_turn_out_amt(CommUtil.nvl(cplIn.getMin_turn_out_amt(), agreeInfo.getMin_turn_out_amt()));// 最小转出金额
		agreeInfo.setMultiple_amt(CommUtil.nvl(cplIn.getMultiple_amt(), agreeInfo.getMultiple_amt()));// 倍增金额
		agreeInfo.setMax_turn_out_amt(CommUtil.nvl(cplIn.getMax_turn_out_amt(), agreeInfo.getMax_turn_out_amt()));// 最大转出金额
		agreeInfo.setAgree_trsf_times(CommUtil.nvl(cplIn.getAgree_trsf_times(), agreeInfo.getAgree_trsf_times()));// 约定转账次数
		agreeInfo.setAgree_trsf_amt(CommUtil.nvl(cplIn.getAgree_trsf_amt(), agreeInfo.getAgree_trsf_amt()));// 约定转账金额
		agreeInfo.setAgree_cycle(cycle);// 约定周期
		agreeInfo.setRef_date(refDate);// 参考日期
		agreeInfo.setBrief_date_symbol(brief);// 参考日期
		agreeInfo.setFail_handling_method(CommUtil.nvl(cplIn.getFail_handling_method(), agreeInfo.getFail_handling_method()));// 失败后处理方式
		agreeInfo.setNext_exec_date(CommUtil.nvl(nextDate, agreeInfo.getNext_exec_date()));
		agreeInfo.setStop_use_ind(CommUtil.nvl(cplIn.getStop_use_ind(), agreeInfo.getStop_use_ind()));
		agreeInfo.setRemark(CommUtil.nvl(cplIn.getRemark(), agreeInfo.getRemark()));
		agreeInfo.setPrior_level(CommUtil.nvl(cplIn.getPrior_level(), agreeInfo.getPrior_level()));
		agreeInfo.setDemand_remain_bal(CommUtil.nvl(cplIn.getDemand_remain_bal(), agreeInfo.getDemand_remain_bal()));
		agreeInfo.setRemark(CommUtil.nvl(cplIn.getRemark(), agreeInfo.getRemark()));

		if (cplIn.getCancle_agree_ind() == E_YESORNO.YES) {// 解约
			agreeInfo.setAgree_status(E_STATUS.INVALID);// 协议状态
		}
		else {
			agreeInfo.setAgree_status(E_STATUS.VALID);// 协议状态
		}

		// 失败后处理相关设置
		if (cplIn.getFail_handling_method() == E_FAILHANDLINGMETHOD.BY_CYClE_AGAIN_TRANSFER && cplIn.getFail_handling_method() != copyAgreeTransfers.getFail_handling_method()) {

			// 不为空校验
			BizUtil.fieldNotNull(cplIn.getFail_renew_trsf_cycle(), DpDict.A.fail_renew_trsf_cycle.getId(), DpDict.A.fail_renew_trsf_cycle.getLongName());
			BizUtil.fieldNotNull(cplIn.getFail_renew_trsf_times(), DpDict.A.fail_renew_trsf_times.getId(), DpDict.A.fail_renew_trsf_times.getLongName());

			// 格式校验
			if (!BizUtil.isCycleString(cplIn.getFail_renew_trsf_cycle())) {
				throw APPUB.E0012(cplIn.getFail_renew_trsf_cycle());
			}

			// 设置值
			agreeInfo.setFail_renew_trsf_cycle(cplIn.getFail_renew_trsf_cycle());// 失败重转周期
			agreeInfo.setFail_renew_trsf_times(cplIn.getFail_renew_trsf_times());// 失败重转次数
		}

		if (agreeInfo.getAgree_trsf_amt_type() == E_AGREETRSFAMOUNTTYPE.FIXED) {

			BizUtil.fieldNotNull(agreeInfo.getTrxn_amt(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());

			if (CommUtil.compare(agreeInfo.getTrxn_amt(), BigDecimal.ZERO) <= 0) {
				throw APPUB.E0020(agreeInfo.getTrxn_amt().toString(), SysDict.A.trxn_amt.getId(), SysDict.A.trxn_amt.getLongName());
			}

			ApCurrencyApi.chkAmountByCcy(agreeInfo.getCcy_code(), agreeInfo.getTrxn_amt());
		}
		else {

			// 最小转出金额必须大于0
			if (CommUtil.compare(agreeInfo.getMin_turn_out_amt(), BigDecimal.ZERO) <= 0) {
				throw APPUB.E0020(agreeInfo.getMin_turn_out_amt().toString(), DpDict.A.min_turn_out_amt.getId(), DpDict.A.min_turn_out_amt.getLongName());
			}

			// 最大转出金额要大于最小转出金额
			if (CommUtil.isNotNull(agreeInfo.getMax_turn_out_amt()) && CommUtil.compare(BigDecimal.ZERO, agreeInfo.getMax_turn_out_amt()) != 0) {

				if (CommUtil.compare(agreeInfo.getMax_turn_out_amt(), agreeInfo.getMin_turn_out_amt()) < 0) {
					throw DpErr.Dp.E0328();
				}
			}
		}

		// 登记审计
		if (ApDataAuditApi.regLogOnUpdateBusiness(copyAgreeTransfers, agreeInfo) != 0) {
			DpbAgreeTransfersDao.updateOne_odb1(agreeInfo);
		}

		bizlog.method(" DpAgreeTransfers.updateAgreeTransfers end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2018年1月16日-下午2:50:17</li>
	 *         <li>功能说明：校验生效日期与失效日期是否符合周期范围</li>
	 *         </p>
	 * @param agreeCycle
	 *            周期
	 * @param effectDate
	 *            生效日期
	 * @param expiryDate
	 *            失效日期
	 */
	private static void checkValidityAndExpiryDateValidity(String agreeCycle, String effectDate, String expiryDate) {

		if (CommUtil.isNull(agreeCycle)) {

			return;
		}

		String cycUnit = agreeCycle.substring(agreeCycle.length() - 1);

		if (CommUtil.equals(cycUnit, "M")) {

			String effectFirstDay = DateTimeUtil_.firstDay(effectDate, "M");// 获取本月初日期
			String expiryFirstDay = DateTimeUtil_.firstDay(expiryDate, "M");// 获取本月初日期

			if (CommUtil.equals(effectFirstDay, expiryFirstDay)) {

				throw DpErr.Dp.E0423(cycUnit, effectDate, expiryDate);
			}
		}
		else if (CommUtil.equals(cycUnit, "Q")) {

			String effectFirstDay = DateTimeUtil_.firstDay(effectDate, "M");// 获取本月初日期
			String expiryFirstDay = DateTimeUtil_.firstDay(expiryDate, "M");// 获取本月初日期

			if (CommUtil.equals(effectFirstDay, expiryFirstDay)) {

				throw DpErr.Dp.E0423(cycUnit, effectDate, expiryDate);
			}

			if (CommUtil.equals(effectFirstDay, BizUtil.calcDateByCycle(expiryFirstDay, "1M"))) {

			}
		}
		else if (CommUtil.equals(cycUnit, "Y")) {

			String effectFirstDay = DateTimeUtil_.firstDay(effectDate, "Y");// 获取本年初日期
			String expiryFirstDay = DateTimeUtil_.firstDay(expiryDate, "Y");// 获取本年初日期

			if (CommUtil.equals(effectFirstDay, expiryFirstDay)) {

				throw DpErr.Dp.E0423(cycUnit, effectDate, expiryDate);
			}
		}
	}

	/**
	 * @Author duanhb
	 *         <p>
	 *         <li>2018年1月16日-下午2:50:17</li>
	 *         <li>功能说明：约定转账明细查询</li>
	 *         </p>
	 * @param cplIn
	 *            约定转账明细查询输入
	 * @return 约定转账明细查询输出
	 */
	public static DpAgreeTransfersDetailQryOut agreeTransfersDetailQry(DpAgreeTransfersDetailQryIn cplIn) {
		bizlog.method(" DpAgreeTransfers.agreeTransfersDetailQry begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		DpAgreeTransfersDetailQryOut cplOut = BizUtil.getInstance(DpAgreeTransfersDetailQryOut.class);

		if (CommUtil.isNull(cplIn.getCust_no()) && CommUtil.isNull(cplIn.getAcct_no())) {
			throw DpErr.Dp.E0479();
		}

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 获取公共变量
		String acctNo = null;

		if (CommUtil.isNotNull(cplIn.getAcct_no())) {
			// 取账号信息
			acctNo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false).getAcct_no();
		}
		else if (CommUtil.isNotNull(cplIn.getCust_no())) {

			DpbAgreeTrsfDetail agreeTrsfDetail = SqlDpInstructDao.selAcctnoByCust(runEnvs.getBusi_org_id(), cplIn.getCust_no(), false);

			if (CommUtil.isNull(agreeTrsfDetail)) {
				runEnvs.setTotal_count(0L);// 返回总记录数
				return cplOut;
			}
			acctNo = agreeTrsfDetail.getAcct_no();
		}

		Page<DpbAgreeTrsfDetail> page = SqlDpInstructDao.selAgreeTransfersDetailInfo(acctNo, cplIn.getTrxn_date(), cplIn.getTrsf_handling_status(), runEnvs.getBusi_org_id(),
				runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		Options<DpAgreeTransfersDetail> list01 = new DefaultOptions<DpAgreeTransfersDetail>();

		if (page.getRecordCount() > 0) {

			for (DpbAgreeTrsfDetail tabAgreeTransfersDetail : page.getRecords()) {

				DpAgreeTransfersDetail dpAgreeTransfersDetail = getAgreeTransDetailObj(tabAgreeTransfersDetail);

				list01.add(dpAgreeTransfersDetail);
			}
		}

		runEnvs.setTotal_count(page.getRecordCount());

		cplOut.setList01(list01);

		bizlog.method(" DpAgreeTransfers.agreeTransfersDetailQry end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author duanhb
	 *         <p>
	 *         <li>2018年1月16日-下午2:50:17</li>
	 *         <li>功能说明：约定转账明细对象信息加工</li>
	 *         </p>
	 * @param tabAgreeTransfersDetail
	 *            约定转账明信息
	 * @return 约定转账明细信息加工后输出
	 */
	private static DpAgreeTransfersDetail getAgreeTransDetailObj(DpbAgreeTrsfDetail tabAgreeTransfersDetail) {

		DpAgreeTransfersDetail dpAgreeTransfersDetail = BizUtil.getInstance(DpAgreeTransfersDetail.class);

		dpAgreeTransfersDetail.setAgree_no(tabAgreeTransfersDetail.getAgree_no());
		dpAgreeTransfersDetail.setTrxn_date(tabAgreeTransfersDetail.getTrxn_date());
		dpAgreeTransfersDetail.setTrxn_time(tabAgreeTransfersDetail.getData_create_time());
		dpAgreeTransfersDetail.setTrxn_seq(tabAgreeTransfersDetail.getTrxn_seq());
		dpAgreeTransfersDetail.setAgree_trsf_type(tabAgreeTransfersDetail.getAgree_trsf_type());
		dpAgreeTransfersDetail.setAcct_no(tabAgreeTransfersDetail.getAcct_no());
		dpAgreeTransfersDetail.setSub_acct_seq(tabAgreeTransfersDetail.getSub_acct_seq());
		dpAgreeTransfersDetail.setCcy_code(tabAgreeTransfersDetail.getCcy_code());
		dpAgreeTransfersDetail.setTrxn_amt(tabAgreeTransfersDetail.getTrxn_amt());
		dpAgreeTransfersDetail.setOpp_acct_ccy(tabAgreeTransfersDetail.getOpp_acct_ccy());
		dpAgreeTransfersDetail.setExch_rate(tabAgreeTransfersDetail.getExch_rate());
		dpAgreeTransfersDetail.setTrxn_opp_amt(tabAgreeTransfersDetail.getTrxn_opp_amt());
		dpAgreeTransfersDetail.setTrsf_handling_status(tabAgreeTransfersDetail.getTrsf_handling_status());
		dpAgreeTransfersDetail.setError_code(tabAgreeTransfersDetail.getError_code());
		dpAgreeTransfersDetail.setError_text(tabAgreeTransfersDetail.getError_text());

		String oppSubAcctNo = "";
		String oppAcctName = "";
		String oppAcctBank = "";

		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb4(tabAgreeTransfersDetail.getAcct_no(), tabAgreeTransfersDetail.getSub_acct_seq(), true);

		DpsBill dpsBill = DpsBillDao.selectFirst_odb2(subAccount.getAcct_no(), subAccount.getSub_acct_no(), tabAgreeTransfersDetail.getTrxn_seq(), false);

		if (CommUtil.isNotNull(dpsBill)) {

			if (CommUtil.isNull(dpsBill.getReal_opp_bank_id())) {
				oppSubAcctNo = dpsBill.getOpp_sub_acct_no();
				oppAcctName = dpsBill.getOpp_acct_name();
				oppAcctBank = tabAgreeTransfersDetail.getOrg_id();
			}
			else {
				oppSubAcctNo = dpsBill.getReal_opp_acct_no();
				oppAcctName = dpsBill.getReal_opp_acct_name();
				oppAcctBank = dpsBill.getReal_opp_bank_id();
			}

			dpAgreeTransfersDetail.setOpp_bank_name(ApDropListApi.getText("BANK_CODE", oppAcctBank, false));
			dpAgreeTransfersDetail.setOpp_acct_no(oppSubAcctNo);
			dpAgreeTransfersDetail.setOpp_acct_name(oppAcctName);
		}

		return dpAgreeTransfersDetail;
	}
}
