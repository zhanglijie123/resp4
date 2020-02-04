package cn.sunline.icore.dp.serv.interest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DATAOPERATE;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpInterestBasicApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpRateBasicApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.namedsql.SqlDpInterestBasicDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRate;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestRateDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpbFloatPlan;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpbFloatPlanDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpbRatePlan;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpbRatePlanDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfInterest;
import cn.sunline.icore.dp.base.type.ComDpAccountBase.DpInstCoverIn;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpInrtSet;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpLayerInrt;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtCodeDefine;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryIn;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryOut;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpRateFloatPlan;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTADJUSTTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTBASE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTOPERATE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTTERMMETHOD;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RATEMODIFYTYPE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInterestDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpAcctInrtRateIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpAcctInrtRateOut;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpInrtPlanList;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAcountInstPlanMntIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAcountInstPlanMntOut;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAcountInterestMaintainIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAcountInterestMaintainOut;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpFloatPlan;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpMatureRate;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpRatePlan;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_EXECSTATUS;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_INRTDIRECTION;
import cn.sunline.icore.sys.type.EnumType.E_INRTSTAS;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：存款账户利率信息维护
 * </p>
 * 
 * @Author duanhb
 *         <p>
 *         <li>2017年2月15日-下午3:02:34</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：存款账户利率信息维护</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */

public class DpAccountInterestMaintain {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAccountInterestMaintain.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月24日-下午4:29:25</li>
	 *         <li>功能说明：存款账户利率信息维护</li>
	 *         </p>
	 * @param cplIn
	 *            服务输入接口
	 * @return
	 */
	public static DpAcountInterestMaintainOut doMain(DpAcountInterestMaintainIn cplIn) {
		bizlog.method(" DpAccountInterestMaintain.acctInrtMaintain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>>>cplIn=[%s]", cplIn);

		// 检查及子户定位
		DpaSubAccount subAccount = acctRateModifyCheck(cplIn);

		// 维护正常利率
		boolean mdyNormal = modifyNormalRate(cplIn, subAccount);

		// 维护违约及到期利率
		boolean mdyBreach = modifyBreachAndMatureRate(cplIn.getList02(), subAccount);

		// 正常利率、违约利率、逾期利率都没有维护则报错
		if (!mdyNormal && !mdyBreach) {

			throw ApPubErr.APPUB.E0023(OdbFactory.getTable(DpaInterestRate.class).getLongname());
		}

		// 返回输出接口信息
		DpAcountInterestMaintainOut maintainOut = BizUtil.getInstance(DpAcountInterestMaintainOut.class);

		maintainOut.setAcct_no(cplIn.getAcct_no()); // 账号
		maintainOut.setAcct_type(cplIn.getAcct_type()); // 账户类型
		maintainOut.setCcy_code(subAccount.getCcy_code()); // 货币代码
		maintainOut.setSub_acct_seq(subAccount.getSub_acct_seq()); // 子账户序号
		maintainOut.setProd_id(subAccount.getProd_id()); // 产品编号
		maintainOut.setDd_td_ind(subAccount.getDd_td_ind()); // 定活标志
		maintainOut.setInrt_code(cplIn.getInrt_code());//利率编号
		maintainOut.setInrt_float_method(cplIn.getInrt_float_method());//利率浮动方式
		maintainOut.setInrt_float_value(cplIn.getInrt_float_value());//利率浮动值
		maintainOut.setEfft_inrt(cplIn.getEfft_inrt());//账户执行利率
		maintainOut.setList01(cplIn.getList01()); // 账户利率列表
		maintainOut.setList02(cplIn.getList02()); // 账户浮动计划列表

		bizlog.method(" DpAccountInterestMaintain.acctInrtMaintain end <<<<<<<<<<<<<<<<");

		return maintainOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月25日-下午3:02:43</li>
	 *         <li>功能说明：存款账户利率维护检查</li>
	 *         </p>
	 * @param cplIn
	 *            交易输入接口
	 * @param subAccount
	 *            子账户信息
	 */
	private static DpaSubAccount acctRateModifyCheck(DpAcountInterestMaintainIn cplIn) {
		bizlog.method(" DpAccountInterestMaintain.acctRateModifyCheck begin >>>>>>>>>>>>>>>>");

		// 字段必输性检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

		// 子账户定位
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setProd_id(cplIn.getProd_id());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());

		// 定位子账号
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		cplIn.setAcct_type(acctAccessOut.getAcct_type());

		// 子账户信息上锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		if (subAccount.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		// 不计息账户不能维护利率
		if (subAccount.getInst_ind() == E_YESORNO.NO) {

			throw DpErr.Dp.E0396(subAccount.getAcct_no());
		}

		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAccount.getAcct_no(), true);

			if (acctInfo.getCard_relationship_ind() != E_YESORNO.YES) {

				// 验证密码
				DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

				checkIn.setTrxn_password(cplIn.getTrxn_password());

				DpPublicCheck.checkPassWord(acctInfo, checkIn);
			}
		}

		// 检查数据版本号
		if (CommUtil.compare(cplIn.getData_version(), subAccount.getData_version()) != 0) {
			throw ApPubErr.APPUB.E0018(DpaInterestRate.class.getName());
		}

		// 利率编号可选范围
		List<String> listRateCode = new ArrayList<String>();

		if (subAccount.getAllow_hand_rate_ind() == E_YESORNO.YES) {

			Options<DpInrtCodeDefine> InrtCodeList = DpRateBasicApi.getContractRateCode(subAccount.getDd_td_ind() == E_DEMANDORTIME.TIME ? E_INRTDIRECTION.SIMPLE_CONT : null);

			for (DpInrtCodeDefine cplRateCode : InrtCodeList) {
				listRateCode.add(cplRateCode.getInrt_code());
			}
		}

		// 其他正常利率信息有输入时，利率编号必输
		if (CommUtil.isNotNull(cplIn.getInrt_float_method()) || CommUtil.isNotNull(cplIn.getInrt_float_value()) || CommUtil.isNotNull(cplIn.getEfft_inrt())
				|| (CommUtil.isNotNull(cplIn.getList01()) && cplIn.getList01().size() > 0)) {

			BizUtil.fieldNotNull(cplIn.getInrt_code(), SysDict.A.inrt_code.getId(), SysDict.A.inrt_code.getLongName());
		}

		// 正常利率修改只能是活期账户
		if (CommUtil.isNotNull(cplIn.getInrt_code())) {

			if (subAccount.getDd_td_ind() != E_DEMANDORTIME.DEMAND) {
				throw DpErr.Dp.E0487(cplIn.getAcct_no());
			}

			DpfInterest prodInst = DpProductFactoryApi.getProdInterestDefine(subAccount.getProd_id(), subAccount.getCcy_code(), E_INSTKEYTYPE.NORMAL, true);

			// 利率编号超出产品约束范围
			if (!CommUtil.equals(prodInst.getInrt_code(), cplIn.getInrt_code()) && !listRateCode.contains(cplIn.getInrt_code())) {
				throw DpErr.Dp.E0397(cplIn.getInrt_code());
			}

			// 合法性检查
			DpInrtCodeDefine cmInterestRate = DpRateBasicApi.getInrtCodeDefine(cplIn.getInrt_code());

			if (cmInterestRate.getInrt_code_status() == E_INRTSTAS.CANCEL) {

				throw DpBase.E0219(cplIn.getInrt_code());
			}
		}

		// 违约及逾期利率维护
		if (CommUtil.isNotNull(cplIn.getList02()) && cplIn.getList02().size() > 0) {

			// 活期户不能维护违约利率和到期利率
			if (subAccount.getDd_td_ind() != E_DEMANDORTIME.TIME) {

				throw DpBase.E0280(subAccount.getAcct_no());
			}

			// 账户已到期不能维护提前支取和逾期利率
			if (CommUtil.isNull(subAccount.getDue_date()) || CommUtil.compare(subAccount.getDue_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {
				throw DpErr.Dp.E0371(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
			}

			for (DpMatureRate modifyRate : cplIn.getList02()) {

				BizUtil.fieldNotNull(modifyRate.getInst_key_type(), DpBaseDict.A.inst_key_type.getId(), DpBaseDict.A.inst_key_type.getLongName());
				BizUtil.fieldNotNull(modifyRate.getInrt_code(), SysDict.A.inrt_code.getId(), SysDict.A.inrt_code.getLongName());

				// 获取计息定义信息
				DpfInterest prodInst = DpProductFactoryApi.getProdInterestDefine(subAccount.getProd_id(), subAccount.getCcy_code(), modifyRate.getInst_key_type(), true);

				String prodRateCode = prodInst.getInrt_code();

				// 利率编号超出产品约束范围
				if (!CommUtil.equals(prodRateCode, modifyRate.getInrt_code()) && !listRateCode.contains(modifyRate.getInrt_code())) {
					throw DpErr.Dp.E0397(modifyRate.getInrt_code());
				}

				DpInrtCodeDefine cmInterestRate = DpRateBasicApi.getInrtCodeDefine(modifyRate.getInrt_code());

				if (cmInterestRate.getInrt_code_status() == E_INRTSTAS.CANCEL) {

					throw DpBase.E0219(modifyRate.getInrt_code());
				}
			}
		}

		bizlog.method(" DpAccountInterestMaintain.acctRateModifyCheck end <<<<<<<<<<<<<<<<");
		return subAccount;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月12日-下午7:13:42</li>
	 *         <li>功能说明：账户正常利率信息维护</li>
	 *         </p>
	 * @param cplIn
	 *            账户利率维护输入
	 * @param subAccount
	 *            子账号
	 * @return
	 */
	private static boolean modifyNormalRate(DpAcountInterestMaintainIn cplIn, DpaSubAccount subAccount) {

		bizlog.method(" DpAccountInterestMaintain.modifyNormalRate begin >>>>>>>>>>>>>>>>");

		if (CommUtil.isNull(cplIn.getInrt_code())) {
			return false;
		}

		// 正常计息定义
		DpaInterest nomalInterest = DpaInterestDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, true);

		// 获取账户利率信息
		List<DpaInterestRate> listAcctInrt = DpaInterestRateDao.selectAll_odb2(subAccount.getAcct_no(), subAccount.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, true);

		// 利率查询输入
		DpInrtQryIn cplInrtQryIn = BizUtil.getInstance(DpInrtQryIn.class);

		cplInrtQryIn.setInrt_code(cplIn.getInrt_code()); // 利率编号
		cplInrtQryIn.setCcy_code(subAccount.getCcy_code()); // 货币代码
		cplInrtQryIn.setTerm_code(nomalInterest.getInst_term_method() == E_INSTTERMMETHOD.ACCTTRERM ? subAccount.getTerm_code() : nomalInterest.getAppo_inrt_term()); // 存期
		cplInrtQryIn.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date()); // 交易日期
		cplInrtQryIn.setTrxn_amt(subAccount.getAcct_bal()); // 交易金额
		cplInrtQryIn.setStart_inst_date(subAccount.getStart_inst_date()); // 起息日期
		cplInrtQryIn.setEnd_date(""); // 截止日期
		cplInrtQryIn.setInst_rate_file_way(nomalInterest.getInst_rate_file_way());
		cplInrtQryIn.setInrt_float_method(cplIn.getInrt_float_method()); // 利率浮动方式
		cplInrtQryIn.setInrt_float_value(cplIn.getInrt_float_value()); // 利率浮动值
		cplInrtQryIn.setInrt_reset_method(nomalInterest.getInrt_reset_method());
		cplInrtQryIn.setContract_inrt(cplIn.getEfft_inrt());
		cplInrtQryIn.setList_contract_inrt(cplIn.getList01());

		// 利率查询
		DpInrtQryOut cplInrtQryOut = DpRateBasicApi.getInstRateInfo(cplInrtQryIn);

		// 获取新的账户利率
		List<DpaInterestRate> listNewAcctInrt = DpInterestBasicApi.getNewAcctInrt(listAcctInrt, cplInrtQryOut);

		boolean changedFlag = false;
		int oldLayerNum = listAcctInrt.size();
		int newLayerNum = listNewAcctInrt.size();
		int bigerValue = (listAcctInrt.size() >= listNewAcctInrt.size()) ? listAcctInrt.size() : listNewAcctInrt.size();

		for (int k = 0; k < bigerValue; k++) {

			DpaInterestRate oldAcctRate = (oldLayerNum > k) ? listAcctInrt.get(k) : null;
			DpaInterestRate newAcctRate = (newLayerNum > k) ? listNewAcctInrt.get(k) : null;

			// 登记利率变更审计
			if (ApDataAuditApi.regLogOnUpdateBusiness(oldAcctRate, newAcctRate) > 0) {
				changedFlag = true;
			}
		}

		// 数据有修改标志
		if (changedFlag) {

			// 登记历史利率信息
			DpInterestBasicApi.regHistInterestRate(listAcctInrt, nomalInterest.getLast_inrt_renew_date(), subAccount.getStart_inst_date(), E_RATEMODIFYTYPE.HAND_MODIFY);

			// 更新利率表
			DpInterestBasicApi.modifyAcctInrt(listAcctInrt, cplInrtQryOut);

			// 克隆计息定义
			DpaInterest oldNomalInst = BizUtil.clone(DpaInterest.class, nomalInterest);

			nomalInterest.setInrt_code(cplInrtQryOut.getInrt_code());
			nomalInterest.setInrt_code_direction(cplInrtQryOut.getInrt_code_direction());
			nomalInterest.setLast_inst_oper_type(E_INSTOPERATE.RENEW);
			nomalInterest.setLast_inrt_renew_date(BizUtil.getTrxRunEnvs().getTrxn_date());

			// 利率变更调整利息
			DpInterestBasicApi.adjustInstForInrtRenew(nomalInterest, listAcctInrt, E_INSTADJUSTTYPE.RENEW);

			// 更新计息定义表
			DpaInterestDao.updateOne_odb1(nomalInterest);

			// 登记计息定义审计
			ApDataAuditApi.regLogOnUpdateBusiness(oldNomalInst, nomalInterest);
		}

		bizlog.method(" DpAccountInterestMaintain.modifyNormalRate end <<<<<<<<<<<<<<<<");
		return changedFlag;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月11日-上午11:05:40</li>
	 *         <li>功能说明：账户违约及逾期利率维护</li>
	 *         </p>
	 * @param cplIn
	 *            违约，到期利率信息
	 * @param subAccount
	 *            子账户信息
	 * @return
	 */
	private static boolean modifyBreachAndMatureRate(List<DpMatureRate> listModifyInrt, DpaSubAccount subAccount) {

		bizlog.method(" DpAccountInterestMaintain.modifyBreachAndMatureRate begin >>>>>>>>>>>>>>>>");

		if (CommUtil.isNull(listModifyInrt) || listModifyInrt.size() == 0) {
			return false;
		}

		// 有修改标志
		boolean changedFlag = false;

		for (DpMatureRate modifyRate : listModifyInrt) {

			// 账户计息定义
			DpaInterest acctInst = DpaInterestDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), modifyRate.getInst_key_type(), false);

			// 账户利率定义
			DpaInterestRate acctRate = DpaInterestRateDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), modifyRate.getInst_key_type(),
					DpConst.START_SORT_VALUE, false);

			if (acctInst == null) {

				changedFlag = true;

				DpInrtSet inrtSet = BizUtil.getInstance(DpInrtSet.class);

				inrtSet.setContract_inrt(modifyRate.getEfft_inrt());
				inrtSet.setInrt_code(modifyRate.getInrt_code());
				inrtSet.setInrt_float_method(modifyRate.getInrt_float_method());
				inrtSet.setInrt_float_value(modifyRate.getInrt_float_value());
				inrtSet.setInrt_key_type(modifyRate.getInst_key_type());

				Options<DpInrtSet> listInrtSet = new DefaultOptions<DpInrtSet>();

				listInrtSet.add(inrtSet);

				DpInstCoverIn cplCoverIn = BizUtil.getInstance(DpInstCoverIn.class);

				cplCoverIn.setProd_id(subAccount.getProd_id());
				cplCoverIn.setCcy_code(subAccount.getCcy_code());
				cplCoverIn.setOpen_acct_date(subAccount.getOpen_acct_date());
				cplCoverIn.setBack_value_date(subAccount.getStart_inst_date());
				cplCoverIn.setInst_key_type(modifyRate.getInst_key_type());
				cplCoverIn.setInst_ind(subAccount.getInst_ind());
				cplCoverIn.setList_inrt(listInrtSet);
				cplCoverIn.setInst_base(E_INSTBASE.ACTUAL);

				/* 产品计息定义信息继承 */
				if (modifyRate.getInst_key_type() == E_INSTKEYTYPE.MATURE) {

					DpfInterest prodInst = DpProductFactoryApi.getProdInterestDefine(subAccount.getProd_id(), subAccount.getCcy_code(), modifyRate.getInst_key_type(), true);

					cplCoverIn.setInst_base(prodInst.getInst_base());
					cplCoverIn.setFirst_pay_inst_date(null);
					cplCoverIn.setPay_inst_cyc(null);
				}

				DpInterestBasicApi.inheritInterest(cplCoverIn, subAccount);

				// 再次查询利率、利息定义
				acctInst = DpaInterestDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), modifyRate.getInst_key_type(), true);

				acctRate = DpaInterestRateDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), modifyRate.getInst_key_type(), DpConst.START_SORT_VALUE, true);

				// TODO
				// 登记审计日志
				// ApDataAuditApi.regLogOnUpdateBusiness(null, acctRate);
				//
				// ApDataAuditApi.regLogOnUpdateBusiness(null, acctInst);
			}
			else {

				// 利率查询输入
				DpInrtQryIn cplInrtQryIn = BizUtil.getInstance(DpInrtQryIn.class);

				cplInrtQryIn.setInrt_code(modifyRate.getInrt_code()); // 利率编号
				cplInrtQryIn.setCcy_code(subAccount.getCcy_code()); // 货币代码
				cplInrtQryIn.setTerm_code(acctInst.getInst_term_method() == E_INSTTERMMETHOD.ACCTTRERM ? subAccount.getTerm_code() : acctInst.getAppo_inrt_term()); // 存期
				cplInrtQryIn.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date()); // 交易日期
				cplInrtQryIn.setTrxn_amt(subAccount.getAcct_bal()); // 交易金额
				cplInrtQryIn.setStart_inst_date(acctInst.getStart_inst_date()); // 起息日期
				cplInrtQryIn.setEnd_date(""); // 截止日期
				cplInrtQryIn.setInst_rate_file_way(acctInst.getInst_rate_file_way());
				cplInrtQryIn.setInrt_float_method(modifyRate.getInrt_float_method()); // 利率浮动方式
				cplInrtQryIn.setInrt_float_value(modifyRate.getInrt_float_value()); // 利率浮动值
				cplInrtQryIn.setInrt_reset_method(acctInst.getInrt_reset_method());
				cplInrtQryIn.setContract_inrt(modifyRate.getEfft_inrt());

				// 利率查询
				DpInrtQryOut cplInrtQryOut = DpRateBasicApi.getInstRateInfo(cplInrtQryIn);

				List<DpaInterestRate> listAcctInrt = new ArrayList<DpaInterestRate>();

				listAcctInrt.add(acctRate);

				// 获取新的账户利率
				List<DpaInterestRate> listNewAcctInrt = DpInterestBasicApi.getNewAcctInrt(listAcctInrt, cplInrtQryOut);

				// 先登记审计，返回数据修改结果
				int i = ApDataAuditApi.regLogOnUpdateBusiness(acctRate, listNewAcctInrt.get(0));

				if (i > 0) {

					// 有变更
					changedFlag = true;

					DpInterestBasicApi.regHistInterestRate(listAcctInrt, acctInst.getLast_inrt_renew_date(), subAccount.getStart_inst_date(), E_RATEMODIFYTYPE.HAND_MODIFY);

					// 更新利率表
					DpInterestBasicApi.modifyAcctInrt(listAcctInrt, cplInrtQryOut);

					// 更新计息定义表
					DpaInterest oldAcctInst = BizUtil.clone(DpaInterest.class, acctInst);

					acctInst.setInrt_code(modifyRate.getInrt_code());
					acctInst.setInrt_code_direction(cplInrtQryOut.getInrt_code_direction());
					acctInst.setLast_inrt_renew_date(BizUtil.getTrxRunEnvs().getTrxn_date());

					DpaInterestDao.updateOne_odb1(acctInst);

					ApDataAuditApi.regLogOnUpdateBusiness(oldAcctInst, acctInst);

				}
			}
		}
		bizlog.method(" DpAccountInterestMaintain.modifyBreachAndMatureRate end <<<<<<<<<<<<<<<<");
		return changedFlag;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月21日-下午3:41:26</li>
	 *         <li>功能说明：账户利率计划维护</li>
	 *         </p>
	 * @param cplIn
	 *            账户利率计划维护输入
	 * @return DpAcountInstPlanMntOut 账户利率计划维护输出
	 */
	public static DpAcountInstPlanMntOut acountInstPlanMntIn(DpAcountInstPlanMntIn cplIn) {
		bizlog.method(" DpAccountInterestMaintain.acountInstPlanMntIn begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 账号必须输入
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 验证密码标志必须输入
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			// 交易密码必须输入
			BizUtil.fieldNotNull(cplIn.getTrxn_password(), SysDict.A.trxn_password.getId(), SysDict.A.trxn_password.getLongName());

		}

		// 封装子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);
		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setProd_id(cplIn.getProd_id());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());

		// 定位子账号
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 子账户信息上锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), false);

		if (subAccount.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			// 验证密码
			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAccount.getAcct_no(), true);

			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 账户利率计划处理
		int flagPlan = instPlanMnt(cplIn.getList01(), subAccount);

		// 账户浮动计划处理
		int flagPloat = instFloatMnt(cplIn.getList02(), subAccount);

		// 判断如果都没有处理则报错
		if (flagPlan == 0 && flagPloat == 0) {

			throw ApPubErr.APPUB.E0023(OdbFactory.getTable(DpaInterestRate.class).getLongname());
		}

		E_INSTKEYTYPE instKey = DpInterestBasicApi.getCainInstKey(subAccount, BizUtil.getTrxRunEnvs().getTrxn_date());

		// 读取账户计息信息
		DpaInterest acctInst = DpaInterestDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), instKey, true);

		// 查询未完成的利率计划数和浮动计划数：包括未执行和执行中
		long planNum = SqlDpInterestBasicDao.selUnfinishedInrtPlanNum(subAccount.getAcct_no(), subAccount.getSub_acct_no(), BizUtil.getTrxRunEnvs().getTrxn_date(),
				acctInst.getOrg_id(), false);
		long floatNum = SqlDpInterestBasicDao.selUnfinishedFloatPlanNum(subAccount.getAcct_no(), subAccount.getSub_acct_no(), BizUtil.getTrxRunEnvs().getTrxn_date(),
				acctInst.getOrg_id(), false);

		// 有未完成的利率计划 或 浮动计划,设置 interest rate plan flag = Y
		if (planNum > 0 || floatNum > 0) {
			acctInst.setSet_inrt_plan_ind(E_YESORNO.YES);
		}

		// 查询当天是否有待执行利率计划
		List<DpRateFloatPlan> listInrtPlan = DpInterestBasicApi.getInrtPlan(acctInst);

		// 执行利率计划
		if (CommUtil.isNotNull(listInrtPlan) && listInrtPlan.size() > 0) {

			DpInterestBasicApi.doInterestRatePlan(acctInst, subAccount, listInrtPlan);
		}

		// 简便安全起见：更新计息定义表
		DpaInterestDao.updateOne_odb1(acctInst);

		// 返回输出接口信息
		DpAcountInstPlanMntOut maintainOut = BizUtil.getInstance(DpAcountInstPlanMntOut.class);

		maintainOut.setEfft_inrt(BigDecimal.ZERO);

		// BAY需求,IG需要获取指定的原始利率,做消息推送
		DpInrtQryIn cplInrtQryIn = BizUtil.getInstance(DpInrtQryIn.class);

		cplInrtQryIn.setInrt_code(cplIn.getList01().get(0).getInrt_code()); // 利率编号
		cplInrtQryIn.setCcy_code(subAccount.getCcy_code()); // 货币代码
		cplInrtQryIn.setTerm_code(acctInst.getInst_term_method() == E_INSTTERMMETHOD.ACCTTRERM ? subAccount.getTerm_code() : acctInst.getAppo_inrt_term()); // 存期
		cplInrtQryIn.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date()); // 交易日期
		cplInrtQryIn.setTrxn_amt(subAccount.getAcct_bal()); // 交易金额
		cplInrtQryIn.setStart_inst_date(subAccount.getStart_inst_date()); // 起息日期
		cplInrtQryIn.setInst_rate_file_way(acctInst.getInst_rate_file_way());
		cplInrtQryIn.setInrt_reset_method(acctInst.getInrt_reset_method());

		// 利率查询
		DpInrtQryOut cplInrtQryOut = DpRateBasicApi.getInstRateInfo(cplInrtQryIn);

		if (cplInrtQryOut.getSingle_layer_ind() == E_YESORNO.YES) {
			maintainOut.setEfft_inrt(cplInrtQryOut.getEfft_inrt());
		}
		else {

			BizUtil.listSort(cplInrtQryOut.getListLayerInrt(), false, DpBaseDict.A.layer_no.getId());

			for (DpLayerInrt inrtInfo : cplInrtQryOut.getListLayerInrt()) {

				if (CommUtil.compare(CommUtil.nvl(inrtInfo.getBand_amount(), BigDecimal.ZERO), subAccount.getAcct_bal()) <= 0) {
					maintainOut.setEfft_inrt(inrtInfo.getEfft_inrt());
					break;
				}
			}
		}

		maintainOut.setAcct_no(subAccount.getAcct_no()); // 账号
		maintainOut.setAcct_name(acctAccessOut.getAcct_name()); //账号名字
		maintainOut.setAcct_type(acctAccessOut.getAcct_type()); // 账户类型
		maintainOut.setCcy_code(subAccount.getCcy_code()); // 货币代码
		maintainOut.setSub_acct_seq(subAccount.getSub_acct_seq()); // 子账户序号
		maintainOut.setProd_id(subAccount.getProd_id()); // 产品编号
		maintainOut.setDd_td_ind(subAccount.getDd_td_ind()); // 定活标志
		maintainOut.setList01(cplIn.getList01()); // 账户利率计划列表
		maintainOut.setList02(cplIn.getList02()); // 账户浮动计划列表

		bizlog.method(" DpAccountInterestMaintain.acountInstPlanMntIn end <<<<<<<<<<<<<<<<");
		return maintainOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月21日-下午4:01:00</li>
	 *         <li>功能说明：利率计划处理</li>
	 *         </p>
	 * @param list01
	 *            利率计划列表
	 * @param subAccount
	 *            子账号
	 * @return
	 */
	private static int instPlanMnt(Options<DpRatePlan> ratePlanList, DpaSubAccount subAccount) {
		bizlog.method(" DpAccountInterestMaintain.instPlanMnt begin >>>>>>>>>>>>>>>>");
		int flag = 0;
		for (DpRatePlan ratePlan : ratePlanList) {

			DpInrtCodeDefine cmInterestRate = DpRateBasicApi.getInrtCodeDefine(ratePlan.getInrt_code());

			if (cmInterestRate.getInrt_code_direction() == E_INRTDIRECTION.SIMPLE_CONT && CommUtil.isNull(ratePlan.getContract_inrt())) {

				// 利率编号对应利率指向是合同利率时必须有值
				throw APPUB.E0001(SysDict.A.inrt_code.getId(), SysDict.A.inrt_code.getLongName());
			}

			if (cmInterestRate.getInrt_code_direction() == E_INRTDIRECTION.LAYER_CONT && CommUtil.isNull(ratePlan.getContract_inrt())) {

				// 利率编号对应利率指向是合同利率时必须有值
				throw DpErr.Dp.E0081(ratePlan.getInrt_code());
			}

			DpbRatePlan tblRatePlan = DpbRatePlanDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), ratePlan.getPlan_start_date(), ratePlan.getLayer_no(),
					false);

			if (ratePlan.getOperater_ind() == E_DATAOPERATE.ADD) {

				if (tblRatePlan != null) {
					// 账户利率计划已存在
					StringBuilder keyValue = new StringBuilder();
					keyValue.append(SysDict.A.acct_no.getLongName()).append("[").append(subAccount.getAcct_no()).append("]");
					keyValue.append(",");
					keyValue.append(SysDict.A.sub_acct_seq.getLongName()).append("[").append(subAccount.getSub_acct_seq()).append("]");
					keyValue.append(",");
					keyValue.append(DpBaseDict.A.layer_no.getLongName()).append("[").append(ratePlan.getLayer_no()).append("]");
					keyValue.append(",");
					keyValue.append(DpBaseDict.A.plan_start_date.getLongName()).append("[").append(ratePlan.getPlan_start_date()).append("]");

					throw ApPubErr.APPUB.E0019(OdbFactory.getTable(DpbRatePlan.class).getLongname(), keyValue.toString());

				}

				List<DpRatePlan> planList = SqlDpInterestDao.selRatePlanRecord(subAccount.getAcct_no(), subAccount.getOrg_id(), subAccount.getSub_acct_no(), E_EXECSTATUS.ALL,
						null, false);

				if (!planList.isEmpty()) {

					BizUtil.listSort(planList, true, DpBaseDict.A.plan_start_date.getId());

					DpRatePlan lastRatePlan = planList.get(planList.size() - 1);

					// 最后一笔优惠计划
					if (CommUtil.compare(ratePlan.getPlan_start_date(), lastRatePlan.getPlan_start_date()) == 0
							&& CommUtil.compare(ratePlan.getPlan_end_date(), lastRatePlan.getPlan_end_date()) == 0) {

						throw DpErr.Dp.E0051(lastRatePlan.getPlan_start_date(), lastRatePlan.getPlan_end_date());
					}
					// 判断新增计划在最后一笔优惠计划起始日期范围内或者起始日期小于等于最后一笔优惠计划起始日期,则抛出异常
					if (CommUtil.compare(ratePlan.getPlan_start_date(), lastRatePlan.getPlan_start_date()) < 0
							|| CommUtil.Between(ratePlan.getPlan_start_date(), lastRatePlan.getPlan_start_date(), lastRatePlan.getPlan_end_date())) {

						throw DpErr.Dp.E0050(ratePlan.getPlan_start_date(), lastRatePlan.getPlan_start_date(), lastRatePlan.getPlan_end_date());
					}
				}

				//计划开始日期大于结束时，则抛出异常
				if (CommUtil.compare(ratePlan.getPlan_start_date(), ratePlan.getPlan_end_date()) > 0){
					
					throw DpErr.Dp.E0133(ratePlan.getPlan_start_date(), ratePlan.getPlan_end_date());
				}
				
				tblRatePlan = BizUtil.getInstance(DpbRatePlan.class);

				tblRatePlan.setSub_acct_no(subAccount.getSub_acct_no()); // 子账号
				tblRatePlan.setAcct_no(subAccount.getAcct_no());
				tblRatePlan.setLayer_no(ratePlan.getLayer_no()); // 层次序号
				tblRatePlan.setBand_amount(ratePlan.getBand_amount());// 分层金额
				tblRatePlan.setPlan_start_date(ratePlan.getPlan_start_date()); // 计划起始日
				tblRatePlan.setPlan_end_date(ratePlan.getPlan_end_date()); // 计划终止日
				tblRatePlan.setInrt_code_direction(cmInterestRate.getInrt_code_direction()); // 利率编号指向
				tblRatePlan.setContract_inrt(ratePlan.getContract_inrt()); // 合同利率
				tblRatePlan.setInrt_code(ratePlan.getInrt_code()); // 利率编号
				tblRatePlan.setBand_term(subAccount.getTerm_code()); //

				DpbRatePlanDao.insert(tblRatePlan);

				flag = 1;
			}
			else if (ratePlan.getOperater_ind() == E_DATAOPERATE.MODIFY) {

				if (tblRatePlan == null) {
					// 账户利率计划不存在
					throw ApPubErr.APPUB.E0025(OdbFactory.getTable(DpbFloatPlan.class).getLongname(), SysDict.A.sub_acct_seq.getLongName(), subAccount.getSub_acct_seq(),
							DpBaseDict.A.layer_no.getLongName(), ratePlan.getLayer_no().toString(), DpBaseDict.A.plan_start_date.getLongName(), ratePlan.getPlan_start_date());

				}
				
				//计划开始日期大于结束时，则抛出异常
                if (CommUtil.compare(ratePlan.getPlan_start_date(), ratePlan.getPlan_end_date()) > 0){
					
					throw DpErr.Dp.E0133(ratePlan.getPlan_start_date(), ratePlan.getPlan_end_date());
				}
                
				DpbRatePlan oldRatePlan = BizUtil.clone(DpbRatePlan.class, tblRatePlan);

				tblRatePlan.setLayer_no(ratePlan.getLayer_no()); // 层次序号
				tblRatePlan.setBand_amount(ratePlan.getBand_amount());// 分层金额

				tblRatePlan.setPlan_start_date(ratePlan.getPlan_start_date()); // 计划起始日
				tblRatePlan.setPlan_end_date(ratePlan.getPlan_end_date()); // 计划终止日
				tblRatePlan.setInrt_code_direction(cmInterestRate.getInrt_code_direction()); // 利率编号指向
				tblRatePlan.setContract_inrt(ratePlan.getContract_inrt()); // 合同利率
				tblRatePlan.setInrt_code(ratePlan.getInrt_code()); // 利率编号Rate.getInrt
				tblRatePlan.setBand_term(""); // TODO 分层存期
				tblRatePlan.setData_version(ratePlan.getData_version());
				// 先登记审计，返回数据修改结果
				int i = ApDataAuditApi.regLogOnUpdateBusiness(oldRatePlan, tblRatePlan);

				if (i > 0) {
					DpbRatePlanDao.updateOne_odb1(tblRatePlan);
					flag = 1;
				}

			}
			else if (ratePlan.getOperater_ind() == E_DATAOPERATE.DELETE) {

				if (tblRatePlan == null) {
					// 账户利率计划不存在
					throw ApPubErr.APPUB.E0025(OdbFactory.getTable(DpbFloatPlan.class).getLongname(), SysDict.A.sub_acct_seq.getLongName(), subAccount.getSub_acct_seq(),
							DpBaseDict.A.layer_no.getLongName(), ratePlan.getLayer_no().toString(), DpBaseDict.A.plan_start_date.getLongName(), ratePlan.getPlan_start_date());

				}

				DpbRatePlanDao.deleteOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), ratePlan.getPlan_start_date(), ratePlan.getLayer_no());

				flag = 1;
			}

		}

		bizlog.method(" DpAccountInterestMaintain.instPlanMnt end <<<<<<<<<<<<<<<<");

		return flag;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年9月21日-下午3:56:32</li>
	 *         <li>功能说明：利率浮动计划处理</li>
	 *         </p>
	 * @param floatPlanList
	 *            利率浮动列表
	 * @param subAccount
	 *            子账号
	 */
	private static int instFloatMnt(Options<DpFloatPlan> floatPlanList, DpaSubAccount subAccount) {
		bizlog.method(" DpAccountInterestMaintain.instFloatMnt begin >>>>>>>>>>>>>>>>");

		int flag = 0;
		for (DpFloatPlan floatPlan : floatPlanList) {

			DpbFloatPlan tblFloatPlan = DpbFloatPlanDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), floatPlan.getFloat_start_date(), false);

			if (floatPlan.getOperater_ind() == E_DATAOPERATE.ADD) {

				if (tblFloatPlan != null) {
					// 账户浮动计划已存在
					StringBuilder keyValue = new StringBuilder();
					keyValue.append(SysDict.A.acct_no.getLongName()).append("[").append(subAccount.getAcct_no()).append("]");
					keyValue.append(",");
					keyValue.append(SysDict.A.sub_acct_seq.getLongName()).append("[").append(subAccount.getSub_acct_seq()).append("]");
					keyValue.append(",");
					keyValue.append(DpBaseDict.A.float_start_date.getLongName()).append("[").append(floatPlan.getFloat_start_date()).append("]");

					throw ApPubErr.APPUB.E0019(OdbFactory.getTable(DpbFloatPlan.class).getLongname(), keyValue.toString());

				}
				
				//浮动计划开始日期大于结束时，则抛出异常
				if (CommUtil.compare(floatPlan.getFloat_start_date(), floatPlan.getFloat_end_date()) > 0){
					
					throw DpErr.Dp.E0133(floatPlan.getFloat_start_date(), floatPlan.getFloat_end_date());
				}
				
				tblFloatPlan = BizUtil.getInstance(DpbFloatPlan.class);

				tblFloatPlan.setSub_acct_no(subAccount.getSub_acct_no()); // 子账号
				tblFloatPlan.setAcct_no(subAccount.getAcct_no()); // 账号
				tblFloatPlan.setFloat_start_date(floatPlan.getFloat_start_date()); // 浮动起始日
				tblFloatPlan.setFloat_end_date(floatPlan.getFloat_end_date()); // 浮动终止日
				tblFloatPlan.setInrt_float_method(floatPlan.getInrt_float_method()); // 利率浮动方式
				tblFloatPlan.setInrt_float_value(floatPlan.getInrt_float_value()); // 利率浮动值

				DpbFloatPlanDao.insert(tblFloatPlan);

				flag = 1;
			}
			else if (floatPlan.getOperater_ind() == E_DATAOPERATE.MODIFY) {

				if (tblFloatPlan == null) {
					// 账户浮动计划不存在
					throw ApPubErr.APPUB.E0025(OdbFactory.getTable(DpbFloatPlan.class).getLongname(), SysDict.A.sub_acct_seq.getLongName(), subAccount.getSub_acct_seq(),
							DpBaseDict.A.float_end_date.getLongName(), floatPlan.getFloat_end_date(), DpBaseDict.A.float_start_date.getLongName(), floatPlan.getFloat_start_date());
				}

				//浮动计划开始日期大于结束时，则抛出异常
				if (CommUtil.compare(floatPlan.getFloat_start_date(), floatPlan.getFloat_end_date()) > 0){
					
					throw DpErr.Dp.E0133(floatPlan.getFloat_start_date(), floatPlan.getFloat_end_date());
				}
				
				DpbFloatPlan oldFloatPlan = BizUtil.clone(DpbFloatPlan.class, tblFloatPlan);

				tblFloatPlan.setFloat_end_date(floatPlan.getFloat_end_date()); // 浮动终止日
				tblFloatPlan.setInrt_float_method(floatPlan.getInrt_float_method()); // 利率浮动方式
				tblFloatPlan.setInrt_float_value(floatPlan.getInrt_float_value()); // 利率浮动值
				tblFloatPlan.setData_version(floatPlan.getData_version());
				// 先登记审计，返回数据修改结果
				int i = ApDataAuditApi.regLogOnUpdateBusiness(oldFloatPlan, tblFloatPlan);

				if (i > 0) {
					flag = 1;
					DpbFloatPlanDao.updateOne_odb1(tblFloatPlan);
				}

			}
			else if (floatPlan.getOperater_ind() == E_DATAOPERATE.DELETE) {

				if (tblFloatPlan == null) {
					// 账户浮动计划不存在
					throw ApPubErr.APPUB.E0025(OdbFactory.getTable(DpbFloatPlan.class).getLongname(), SysDict.A.sub_acct_seq.getLongName(), subAccount.getSub_acct_seq(),
							DpBaseDict.A.float_end_date.getLongName(), floatPlan.getFloat_end_date(), DpBaseDict.A.float_start_date.getLongName(), floatPlan.getFloat_start_date());
				}

				DpbFloatPlanDao.deleteOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), floatPlan.getFloat_start_date());

				flag = 1;
			}

		}

		bizlog.method(" DpAccountInterestMaintain.instFloatMnt end <<<<<<<<<<<<<<<<");
		return flag;

	}

	/**
	 * @author szj
	 *         <p>
	 *         <li>2019年5月15日-上午11:21:53</li>
	 *         <li>功能说明：账户利率查询</li>
	 *         </p>
	 * @param airIn
	 */
	public static DpAcctInrtRateOut DpAcctInrtRateOut(DpAcctInrtRateIn airIn) {
		bizlog.method(" DpAccountInterestMaintain.DpAcctInrtRateOut begin >>>>>>>>>>>>>>>>");

		// 非空字段检查
		BizUtil.fieldNotNull(airIn.getAcct_no(), SysDict.A.card_no.getId(), SysDict.A.card_no.getLongName());

		// 封装子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(airIn.getAcct_no());
		acctAccessIn.setAcct_type(airIn.getAcct_type());
		acctAccessIn.setCcy_code(airIn.getCcy_code());
		acctAccessIn.setProd_id(airIn.getProd_id());
		acctAccessIn.setSub_acct_seq(airIn.getSub_acct_seq());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.subAcctInquery(acctAccessIn);

		// 查询子账户信息：带锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 动态sql查询账单信息
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String orgId = runEnvs.getBusi_org_id();

		Page<DpInrtPlanList> page = SqlDpInterestDao.selInrtRatePlan(orgId, subAccount.getAcct_no(), subAccount.getSub_acct_no(), runEnvs.getPage_start(), runEnvs.getPage_size(),
				runEnvs.getTotal_count(), false);

		DpAcctInrtRateOut dpAcctInrtRateOut = BizUtil.getInstance(DpAcctInrtRateOut.class);

		dpAcctInrtRateOut.setAcct_no(subAccount.getAcct_no());
		dpAcctInrtRateOut.setAcct_type(acctAccessOut.getAcct_type());
		dpAcctInrtRateOut.setCcy_code(subAccount.getCcy_code());
		dpAcctInrtRateOut.setSub_acct_seq(subAccount.getSub_acct_seq());
		dpAcctInrtRateOut.setProd_id(subAccount.getProd_id());
		dpAcctInrtRateOut.setDd_td_ind(subAccount.getDd_td_ind());
		dpAcctInrtRateOut.setInst_ind(subAccount.getInst_ind());

		dpAcctInrtRateOut.getList01().addAll(page.getRecords());
		runEnvs.setTotal_count(page.getPageCount());

		bizlog.method(" DpAccountInterestMaintain.DpAcctInrtRateOut end <<<<<<<<<<<<<<<<");
		return dpAcctInrtRateOut;
	}
}
