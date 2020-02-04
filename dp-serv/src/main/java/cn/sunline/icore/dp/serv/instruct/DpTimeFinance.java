package cn.sunline.icore.dp.serv.instruct;

import java.math.BigDecimal;
import java.util.HashSet;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfOpen;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfSave;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInstructDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbTimeFinance;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbTimeFinanceDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpTimeFinanceInfo;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpTimeFinanceQueryIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpTimeFinanceQueryOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpTimeFinanceSignIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpTimeFinanceSignOppInfo;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpTimeFinanceSignOut;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明： 定期理财相关
 * </p>
 * 
 * @Author shenxy
 *         <p>
 *         <li>2017年9月4日-下午9:55:08</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年9月4日-shenxy：定期理财相关</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpTimeFinance {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeFinance.class);

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月4日-下午9:55:30</li>
	 *         <li>功能说明：定期理财签约</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpTimeFinanceSignOut timeFinanceSign(DpTimeFinanceSignIn cplIn) {
		bizlog.method(" DpTimeFinance.timeFinanceSign begin >>>>>>>>>>>>>>>>");

		// 检查输入数据
		checkInputData(cplIn);

		// 定位账号
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		// 子账户定位
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), false);

		// 检查交易密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 生成协议号
		String agreeNo = ApSeqApi.genSeq("AGREE_NO");

		// 初始化序号
		// long serialNo = DpConst.START_SORT_VALUE;

		HashSet<Long> hashSet = new HashSet<>();

		// 判断序号是否重复
		for (DpTimeFinanceSignOppInfo singleInfo : cplIn.getList01()) {

			hashSet.add(singleInfo.getSerial_no());
		}

		if (CommUtil.compare(hashSet.size(), cplIn.getList01().size()) != 0) {

			throw DpErr.Dp.E0378();
		}

		// 循环处理约定转账
		for (DpTimeFinanceSignOppInfo singleInfo : cplIn.getList01()) {

			// 对方信息校验
			checkSubInoputDate(singleInfo, cplIn);

			// 登记签约转账协议表
			insertAgreeTransfers(subAccount, cplIn, singleInfo, agreeNo);

			// serialNo++;//序号自增
		}

		// 初始化输出接口
		DpTimeFinanceSignOut cplOut = BizUtil.getInstance(DpTimeFinanceSignOut.class);
		cplOut.setAgree_no(agreeNo);// 协议号
		cplOut.setCust_no(acctInfo.getCust_no());// 客户号
		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no());// 卡号
		cplOut.setAcct_no(acctInfo.getAcct_no());// 账号
		cplOut.setAcct_name(acctInfo.getAcct_name());// 账户名称
		cplOut.setSub_acct_seq(acctAccessOut.getSub_acct_seq());// 子账户序号
		cplOut.setCcy_code(cplIn.getCcy_code());// 货币代号
		cplOut.setDemand_remain_bal(cplIn.getDemand_remain_bal());// 活期留存金额

		bizlog.debug("cplOut=[%s]", cplOut);

		bizlog.method(" DpTimeFinance.timeFinanceSign end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月4日-下午9:57:02</li>
	 *         <li>功能说明：定期理财输入检查</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void checkInputData(DpTimeFinanceSignIn cplIn) {
		bizlog.method(" DpTimeFinanceSignIn.checkInputData begin >>>>>>>>>>>>>>>>");

		// 账号不为空
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 货币代号不为空
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 验密标志不为空
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		// 活期留存余额不为空
		BizUtil.fieldNotNull(cplIn.getDemand_remain_bal(), DpDict.A.demand_remain_bal.getId(), DpDict.A.demand_remain_bal.getLongName());

		// 对方信息不能为空
		Options<DpTimeFinanceSignOppInfo> oppInfo = cplIn.getList01();

		if (CommUtil.isNull(oppInfo)) {
			throw DpErr.Dp.E0301();
		}

		bizlog.method(" DpTimeFinanceSignIn.checkInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月4日-下午10:03:25</li>
	 *         <li>功能说明：定期理财对方信息校验</li>
	 *         </p>
	 * @param opp
	 * @param cplIn
	 */
	private static void checkSubInoputDate(DpTimeFinanceSignOppInfo opp, DpTimeFinanceSignIn cplIn) {

		// 对方账号不能为空
		BizUtil.fieldNotNull(opp.getOpp_acct_no(), SysDict.A.opp_acct_no.getId(), SysDict.A.opp_acct_no.getLongName());

		// 新开账户产品号
		BizUtil.fieldNotNull(opp.getNew_open_acct_prod(), DpDict.A.new_open_acct_prod.getId(), DpDict.A.new_open_acct_prod.getLongName());

		// 新开账户存期
		BizUtil.fieldNotNull(opp.getNew_open_acct_term(), DpDict.A.new_open_acct_term.getId(), DpDict.A.new_open_acct_term.getLongName());

		// 最小转出金额不能为空
		BizUtil.fieldNotNull(opp.getMin_turn_out_amt(), DpDict.A.min_turn_out_amt.getId(), DpDict.A.min_turn_out_amt.getLongName());

		// 约定周期不为空
		BizUtil.fieldNotNull(opp.getAgree_cycle(), DpDict.A.agree_cycle.getId(), DpDict.A.agree_cycle.getLongName());

		// 下次执行日期不为空
		BizUtil.fieldNotNull(opp.getNext_exec_date(), DpDict.A.next_exec_date.getId(), DpDict.A.next_exec_date.getLongName());

		// 查询产品存入控制
		DpfSave dpfSave = DpProductFactoryApi.getProdSaveCtrl(opp.getNew_open_acct_prod(), cplIn.getCcy_code());

		// 新开账户产品号判断此产品必须是一次性存入开户的产品
		if (dpfSave.getMax_dept_count() != 1) {

			throw DpErr.Dp.E0326();
		}

		// 查询产品开户控制
		DpfOpen dpfOpen = DpProductFactoryApi.getProdOpenCtrl(opp.getNew_open_acct_prod(), cplIn.getCcy_code());

		// 最小转出金额大于产品起存金额
		if (CommUtil.compare(opp.getMin_turn_out_amt(), dpfOpen.getMin_save_amt()) < 0) {

			throw DpErr.Dp.E0327();
		}

		// 检查生效日期与失效日期合法性
		if (CommUtil.isNotNull(opp.getEffect_date()) || CommUtil.isNotNull(opp.getExpiry_date())) {

			if (CommUtil.isNotNull(opp.getEffect_date()) && CommUtil.isNotNull(opp.getExpiry_date())) {

				BizUtil.checkEffectDate(opp.getEffect_date(), opp.getExpiry_date());

			}

			if (CommUtil.isNotNull(opp.getExpiry_date())) {

				if (CommUtil.compare(opp.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {

					throw DpBase.E0294(opp.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
				}

				if (CommUtil.compare(opp.getExpiry_date(), opp.getNext_exec_date()) < 0) {

					throw DpErr.Dp.E0411(opp.getExpiry_date(), opp.getNext_exec_date());
				}
			}

		}

		// 最小转出金额必须大于0
		if (CommUtil.compare(opp.getMin_turn_out_amt(), BigDecimal.ZERO) <= 0) {

			throw APPUB.E0020(opp.getMin_turn_out_amt().toString(), DpDict.A.min_turn_out_amt.getId(), DpDict.A.min_turn_out_amt.getLongName());
		}

		// 最大转出金额要大于最小转出金额
		if (CommUtil.isNotNull(opp.getMax_turn_out_amt()) && CommUtil.compare(BigDecimal.ZERO, opp.getMax_turn_out_amt()) != 0) {

			if (CommUtil.compare(opp.getMax_turn_out_amt(), opp.getMin_turn_out_amt()) < 0) {

				throw DpErr.Dp.E0328();
			}
		}

		// 下次执行日格式校验
		BizUtil.isDateString(opp.getNext_exec_date());

		// 约定周期校验
		BizUtil.isCycleString(opp.getAgree_cycle());

	}

	/**
	 * @param subAccount
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月4日-下午10:11:21</li>
	 *         <li>功能说明：插入数据</li>
	 *         </p>
	 * @param cplIn
	 * @param singleInfo
	 * @param agreeNo
	 */
	private static void insertAgreeTransfers(DpaSubAccount subAccount, DpTimeFinanceSignIn cplIn, DpTimeFinanceSignOppInfo singleInfo, String agreeNo) {
		bizlog.method(" DpAgreeTransfers.checkOppAcctValid begin >>>>>>>>>>>>>>>>");

		// 初始化约定转账协议表
		DpbTimeFinance tiemFinance = BizUtil.getInstance(DpbTimeFinance.class);

		tiemFinance.setAgree_no(agreeNo);// 协议号
		tiemFinance.setSerial_no(singleInfo.getSerial_no());// 序号
		tiemFinance.setAcct_no(cplIn.getAcct_no());// 账号
		tiemFinance.setCcy_code(cplIn.getCcy_code());// 货币代号
		tiemFinance.setEffect_date(CommUtil.nvl(singleInfo.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date()));// 生效日期
		tiemFinance.setExpiry_date(CommUtil.nvl(singleInfo.getExpiry_date(), ApConst.DEFAULT_MAX_DATE));// 失效日期
		tiemFinance.setOpp_acct_no(singleInfo.getOpp_acct_no());// 对方账户
		tiemFinance.setDemand_remain_bal(cplIn.getDemand_remain_bal());// 活期留存余额
		tiemFinance.setNew_open_acct_prod(singleInfo.getNew_open_acct_prod());// 新开产品号
		tiemFinance.setNew_open_acct_term(singleInfo.getNew_open_acct_term());// 新开账户存期
		tiemFinance.setMin_turn_out_amt(singleInfo.getMin_turn_out_amt());// 最小转出金额
		tiemFinance.setMultiple_amt(singleInfo.getMultiple_amt());// 倍增金额
		tiemFinance.setMax_turn_out_amt(singleInfo.getMax_turn_out_amt());// 最大转出金额
		tiemFinance.setAgree_trsf_times(singleInfo.getAgree_trsf_times());// 约定转账次数
		tiemFinance.setAgree_trsf_amt(singleInfo.getAgree_trsf_amt());// 约定转账金额
		tiemFinance.setAgree_cycle(singleInfo.getAgree_cycle());// 约定周期
		tiemFinance.setRef_date(singleInfo.getNext_exec_date());// 参考日期
		tiemFinance.setExec_time(CommUtil.nvl(singleInfo.getExec_time(), ApBusinessParmApi.getValue("AGREEMENT_TRSF_DEFAULT_TIME")));
		tiemFinance.setNext_exec_date(singleInfo.getNext_exec_date());// 下次执行日
		tiemFinance.setTotal_success_times(0L);// 累计成功次数
		// agreeTransfers.set//累计成功金额
		tiemFinance.setTotal_success_amt(BigDecimal.ZERO);
		tiemFinance.setHash_value(subAccount.getHash_value());

		tiemFinance.setAgree_status(E_STATUS.VALID);// 协议状态
		tiemFinance.setSign_date(BizUtil.getTrxRunEnvs().getTrxn_date());// 签约日期
		tiemFinance.setSign_seq(BizUtil.getTrxRunEnvs().getTrxn_seq());// 签约流水
		// agreeTransfers.set //解约日期
		// agreeTransfers.set //解约流水

		DpbTimeFinanceDao.insert(tiemFinance);

		bizlog.method(" DpAgreeTransfers.checkOppAcctValid end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月5日-下午12:19:46</li>
	 *         <li>功能说明：定期理财查询</li>
	 *         </p>
	 * @param cplIn
	 *            定期理财查询输入
	 * @return DpTimeFinanceQueryOut 定期理财查询输出
	 */
	public static DpTimeFinanceQueryOut timeFinanceQry(DpTimeFinanceQueryIn cplIn) {
		bizlog.method(" DpTimeFinance.timeFinanceQry begin >>>>>>>>>>>>>>>>");

		if (CommUtil.isNull(cplIn.getAgree_no()) && CommUtil.isNull(cplIn.getAcct_no())) {
			throw DpErr.Dp.E0297();
		}

		String acctNo = null;
		String subAcctSeq = null;
		if (CommUtil.isNotNull(cplIn.getAcct_no())) {

			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);
			accessIn.setAcct_no(cplIn.getAcct_no());
			accessIn.setAcct_type(cplIn.getAcct_type());
			accessIn.setCcy_code(cplIn.getCcy_code());
			accessIn.setProd_id(cplIn.getProd_id());

			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			// 定位账号
			acctNo = accessOut.getAcct_no();
			subAcctSeq = accessOut.getSub_acct_seq();
		}

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 获取公共变量

		String orgId = runEnvs.getBusi_org_id();// 取得法人代码

		// 动态查询约定转账表
		Page<DpbTimeFinance> page = SqlDpInstructDao.selTimeFinanceInfo(cplIn.getAgree_no(), acctNo, cplIn.getAgree_status(), orgId, runEnvs.getPage_start(),
				runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(page.getRecordCount());// 返回总记录数

		if (CommUtil.isNull(page.getRecords()) && page.getRecords().size() == 0) {
			return null;
		}
		// 获取账户信息
		DpaAccount acctInfo2 = DpToolsApi.locateSingleAccount(page.getRecords().get(0).getAcct_no(), cplIn.getAcct_type(), false);

		// 初始化输出接口
		DpTimeFinanceQueryOut cplOut = BizUtil.getInstance(DpTimeFinanceQueryOut.class);

		Options<DpTimeFinanceInfo> cplInfoList = cplOut.getList01();

		cplOut.setAcct_no(acctInfo2.getAcct_no());// 账号
		cplOut.setAcct_name(acctInfo2.getAcct_name());// 账户名称
		cplOut.setCcy_code(cplIn.getCcy_code());// 货币代号
		cplOut.setSub_acct_seq(subAcctSeq);// 子账户序号

		// 循环赋值
		for (DpbTimeFinance timeFinaces : page.getRecords()) {

			DpTimeFinanceInfo timeFinance = BizUtil.getInstance(DpTimeFinanceInfo.class);

			timeFinance.setDemand_remain_bal(page.getRecords().get(0).getDemand_remain_bal());// 活期留存余额
			timeFinance.setAgree_no(timeFinaces.getAgree_no());// 协议号
			timeFinance.setSerial_no(timeFinaces.getSerial_no());// 序号
			timeFinance.setEffect_date((timeFinaces.getEffect_date()));// 生效日期
			timeFinance.setExpiry_date(timeFinaces.getExpiry_date());// 失效日期
			timeFinance.setOpp_acct_no(timeFinaces.getOpp_acct_no());// 对方账号
			timeFinance.setMin_turn_out_amt(timeFinaces.getMin_turn_out_amt());// 最小转出金额
			timeFinance.setMultiple_amt(timeFinaces.getMultiple_amt());// 倍增金额
			timeFinance.setMax_turn_out_amt(timeFinaces.getMax_turn_out_amt());// 最大转出金额
			timeFinance.setAgree_trsf_times(timeFinaces.getAgree_trsf_times());// 约定转账次数
			timeFinance.setAgree_trsf_amt(timeFinaces.getAgree_trsf_amt());// 约定转账金额
			timeFinance.setAgree_cycle(timeFinaces.getAgree_cycle());// 约定周期
			timeFinance.setRef_date(timeFinaces.getRef_date());// 参考周期
			timeFinance.setNext_exec_date(timeFinaces.getNext_exec_date());// 下次执行日期
			timeFinance.setTotal_success_times(timeFinaces.getTotal_success_times());// 累计成功次数
			timeFinance.setTotal_success_amt(timeFinaces.getTotal_success_amt());// 累计成功金额
			timeFinance.setAgree_status(timeFinaces.getAgree_status());// 协议状态
			timeFinance.setSign_date(timeFinaces.getSign_date());// 签约日期
			timeFinance.setSign_seq(timeFinaces.getSign_seq());// 签约流水
			timeFinance.setCancel_date(timeFinaces.getCancel_date());// 解约日期
			timeFinance.setCancel_seq(timeFinaces.getCancel_seq());// 解约流水
			timeFinance.setExec_time(timeFinaces.getExec_time());// 执行时间
			timeFinance.setData_version(timeFinaces.getData_version());// 数据版本号

			cplInfoList.add(timeFinance);
		}

		bizlog.method(" DpTimeFinance.timeFinanceQry end <<<<<<<<<<<<<<<<");
		return cplOut;
	}
}
