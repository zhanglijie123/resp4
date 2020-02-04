package cn.sunline.icore.dp.serv.fundpool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpOverDraftApi;
import cn.sunline.icore.dp.base.api.DpOverdraftParmApi;
import cn.sunline.icore.dp.base.api.DpRateBasicApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpaSlipDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraft;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlip;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftSlipDao;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DppOverdraftType;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtCodeDefine;
import cn.sunline.icore.dp.base.type.ComDpOverdraftTrxnBasic.DpOverdraftAgreeRegisterIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ALLOW;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_OVERDRAFTINSTTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_OVERDRAFTRATESOURCE;
import cn.sunline.icore.dp.serv.iobus.DpCreditLimitIobus;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftSignIn;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftSignOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitInfo;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_EXCLUSIVESHARE;
import cn.sunline.icore.sys.type.EnumType.E_FLOATWAY;
import cn.sunline.icore.sys.type.EnumType.E_INRTDIRECTION;
import cn.sunline.icore.sys.type.EnumType.E_LIMITCUTIND;
import cn.sunline.icore.sys.type.EnumType.E_LIMITSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_LIMITTYPE;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明：透支协议相关
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

public class DpOverdraftAgreeSign {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOverdraftAgreeSign.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年7月3日-下午8:12:05</li>
	 *         <li>功能说明：透支协议签约</li>
	 *         </p>
	 * @param cplIn
	 */
	public static DpOverdraftSignOut overdraftSign(DpOverdraftSignIn cplIn) {

		bizlog.method("DpOverdraftAgreeSign.overdraftSign begin");
		bizlog.debug("DpOverdraftSignIn.cplIn = [%s]", cplIn);

		// 透支签约检查
		DpaSubAccount subAcct = checkOverDraftSign(cplIn);

		// 登记账户协议透支信息
		DpOverdraftAgreeRegisterIn cplAgreeRegIn = BizUtil.getInstance(DpOverdraftAgreeRegisterIn.class);

		cplAgreeRegIn.setAcct_no(subAcct.getAcct_no());
		cplAgreeRegIn.setCcy_code(subAcct.getCcy_code());
		cplAgreeRegIn.setEffect_date(cplIn.getEffect_date());
		cplAgreeRegIn.setExpiry_date(cplIn.getExpiry_date());
		cplAgreeRegIn.setLimit_code(cplIn.getLimit_code());
		cplAgreeRegIn.setOverdraft_type(cplIn.getOverdraft_type());
		cplAgreeRegIn.setOverdraft_rate_source(cplIn.getOverdraft_rate_source());
		cplAgreeRegIn.setInrt_code(cplIn.getInrt_code());
		cplAgreeRegIn.setContract_inrt(cplIn.getContract_inrt());
		cplAgreeRegIn.setFixed_acct_no(cplIn.getFixed_acct_no());
		cplAgreeRegIn.setFixed_sub_acct_seq(cplIn.getFixed_sub_acct_seq());
		cplAgreeRegIn.setOverdraft_inrt_float_method(cplIn.getOverdraft_inrt_float_method());
		cplAgreeRegIn.setOverdraft_inrt_float_value(cplIn.getOverdraft_inrt_float_value());
		cplAgreeRegIn.setOverdue_inrt_float_method(cplIn.getOverdue_inrt_float_method());
		cplAgreeRegIn.setOverdue_inrt_float_value(cplIn.getOverdue_inrt_float_value());
		cplAgreeRegIn.setOver_quota_inrt_float_method(cplIn.getOver_quota_inrt_float_method());
		cplAgreeRegIn.setOver_quota_inrt_float_value(cplIn.getOver_quota_inrt_float_value());

		DpbOverdraft OdAgree = DpOverDraftApi.regAgreeOverdraftInfo(cplAgreeRegIn, subAcct);

		// 当天生效则额度重新分布,否则日终处理
		if (CommUtil.equals(OdAgree.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date())) {

			List<DpbOverdraft> listNewODAgree = new ArrayList<>();

			listNewODAgree.add(OdAgree);

			DpOverDraftApi.ODLimitAfreshLayout(listNewODAgree, subAcct);
		}

		// 输出
		DpOverdraftSignOut cplOut = BizUtil.getInstance(DpOverdraftSignOut.class);

		cplOut.setAcct_no(subAcct.getAcct_no());
		cplOut.setAcct_type(cplIn.getAcct_type());
		cplOut.setAcct_name(subAcct.getSub_acct_name());
		cplOut.setCcy_code(subAcct.getCcy_code());
		cplOut.setAgree_no(OdAgree.getAgree_no());
		cplOut.setEffect_date(OdAgree.getEffect_date());
		cplOut.setExpiry_date(OdAgree.getExpiry_date());
		cplOut.setLimit_code(cplIn.getLimit_code());
		cplOut.setOverdraft_type(OdAgree.getOverdraft_type());
		// cplOut.setOverdraft_type_name(overdraftType.getOverdraft_type_name());
		cplOut.setPay_inst_cyc(OdAgree.getPay_inst_cyc());
		cplOut.setFirst_pay_inst_date(OdAgree.getNext_pay_inst_date());
		cplOut.setOverdraft_rate_source(OdAgree.getOverdraft_rate_source());
		cplOut.setFixed_acct_no(cplIn.getFixed_acct_no());
		cplOut.setFixed_sub_acct_seq(cplIn.getFixed_sub_acct_seq());
		cplOut.setInrt_code(cplIn.getInrt_code());
		cplOut.setOverdraft_inrt_float_method(cplIn.getOverdraft_inrt_float_method());
		cplOut.setOverdraft_inrt_float_value(cplIn.getOverdraft_inrt_float_value());
		cplOut.setOver_quota_inrt_float_method(cplIn.getOver_quota_inrt_float_method());
		cplOut.setOver_quota_inrt_float_value(cplIn.getOver_quota_inrt_float_value());
		cplOut.setOverdue_inrt_float_method(OdAgree.getOverdue_inrt_float_method());

		if (CommUtil.compare(OdAgree.getOverdue_inrt_float_value(), BigDecimal.ZERO) != 0) {
			cplOut.setOverdue_inrt_float_value(OdAgree.getOverdue_inrt_float_value());
		}

		// 查询利率
		List<DpbOverdraftSlip> listOdFiche = DpbOverdraftSlipDao.selectAll_odb3(subAcct.getAcct_no(), OdAgree.getAgree_no(), false);

		for (DpbOverdraftSlip OdFiche : listOdFiche) {

			DpaSlip ficheInfo = DpaSlipDao.selectOne_odb1(subAcct.getAcct_no(), OdFiche.getFiche_no(), true);

			if (OdFiche.getOverdraft_inst_type() == E_OVERDRAFTINSTTYPE.NORMAL) {

				BigDecimal overdraftInrt = ficheInfo.getEfft_inrt();
				cplOut.setOverdraft_inrt(overdraftInrt);

				E_FLOATWAY floatMethod = OdAgree.getOverdue_inrt_float_method();

				if (CommUtil.isNull(floatMethod) || floatMethod == E_FLOATWAY.NONE) {
					cplOut.setOverdue_inrt(overdraftInrt);
				}
				else if (floatMethod == E_FLOATWAY.PERCENT) {

					BigDecimal addValue = overdraftInrt.multiply(cplOut.getOverdue_inrt_float_value()).divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);
					cplOut.setOverdue_inrt(overdraftInrt.add(addValue));
				}
				else if (floatMethod == E_FLOATWAY.VALUE) {
					cplOut.setOverdue_inrt(overdraftInrt.add(cplOut.getOverdue_inrt_float_value()));
				}
			}
			else if (OdFiche.getOverdraft_inst_type() == E_OVERDRAFTINSTTYPE.EXCESS) {
				cplOut.setOver_quota_inrt(ficheInfo.getEfft_inrt());
			}
		}

		bizlog.method("DpOverdraftAgreeSign.overdraftSign end");

		return cplOut;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年7月3日-下午8:31:12</li>
	 *         <li>功能说明：透支签约检查</li>
	 *         </p>
	 * @param cplIn
	 *            透支签约输入
	 */
	private static DpaSubAccount checkOverDraftSign(DpOverdraftSignIn cplIn) {

		bizlog.method("DpOverdraftAgree.checkOverDraftSign begin");

		// 1. 必输字段非空检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getLimit_code(), SysDict.A.limit_code.getId(), SysDict.A.limit_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getOverdraft_type(), DpBaseDict.A.overdraft_type.getId(), DpBaseDict.A.overdraft_type.getLongName());
		BizUtil.fieldNotNull(cplIn.getOverdraft_rate_source(), DpBaseDict.A.overdraft_rate_source.getId(), DpBaseDict.A.overdraft_rate_source.getLongName());
		BizUtil.fieldNotNull(cplIn.getOverdraft_inrt_float_method(), DpBaseDict.A.overdraft_inrt_float_method.getId(), DpBaseDict.A.overdraft_inrt_float_method.getLongName());

		// 查询透支参数类型
		DppOverdraftType overdraftType = DpOverdraftParmApi.getOverDraftTypeInfo(cplIn.getOverdraft_type());

		// 失效日期必须大于交易日期
		if (CommUtil.isNotNull(cplIn.getExpiry_date()) && CommUtil.compare(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {

			throw DpBase.E0294(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 生效日期必须小于失效日期
		if (CommUtil.isNotNull(cplIn.getEffect_date()) && CommUtil.isNotNull(cplIn.getExpiry_date())) {

			BizUtil.checkEffectDate(cplIn.getEffect_date(), cplIn.getExpiry_date());
		}

		// 利率来源为手工设置
		if (cplIn.getOverdraft_rate_source() == E_OVERDRAFTRATESOURCE.MANUAL_SET) {

			// 利率编号必输
			BizUtil.fieldNotNull(cplIn.getInrt_code(), SysDict.A.inrt_code.getId(), SysDict.A.inrt_code.getLongName());

			if (!CommUtil.equals(cplIn.getInrt_code(), overdraftType.getInrt_code())) {

				if (overdraftType.getAllow_hand_rate_ind() == E_YESORNO.YES) {

					Options<DpInrtCodeDefine> InrtCodeList = DpRateBasicApi.getContractRateCode(E_INRTDIRECTION.SIMPLE_CONT);

					// 输入的利率编号不在许可范围内
					if (!SysUtil.serialize(InrtCodeList).contains(cplIn.getInrt_code())) {

						// throw DpErr.Dp.E0402(cplIn.getInrt_code());
					}
				}
			}

			// 合法性检查
			DpInrtCodeDefine cmInterestRate = DpRateBasicApi.getInrtCodeDefine(cplIn.getInrt_code());

			// 为合同利率时
			if (cmInterestRate.getInrt_code_direction() == E_INRTDIRECTION.SIMPLE_CONT) {

				// 合同利率必输
				BizUtil.fieldNotNull(cplIn.getContract_inrt(), DpBaseDict.A.contract_inrt.getId(), DpBaseDict.A.contract_inrt.getLongName());

				// 透支合同利率不能浮动
				if (cplIn.getOverdraft_inrt_float_method() != E_FLOATWAY.NONE) {

					throw DpBase.E0403();
				}
			}
		}
		else {

			// 利率来源为关联定期存单，定期账户信息必输
			BizUtil.fieldNotNull(cplIn.getFixed_acct_no(), DpBaseDict.A.fixed_acct_no.getId(), DpBaseDict.A.fixed_acct_no.getLongName());
			BizUtil.fieldNotNull(cplIn.getFixed_sub_acct_seq(), DpBaseDict.A.fixed_sub_acct_seq.getId(), DpBaseDict.A.fixed_sub_acct_seq.getLongName());

			// 定期子账户定位
			DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			acctAccessIn.setAcct_no(cplIn.getFixed_acct_no());
			acctAccessIn.setDd_td_ind(E_DEMANDORTIME.TIME);
			acctAccessIn.setSub_acct_seq(cplIn.getFixed_sub_acct_seq());

			// 获取定期子账户信息，看是否存在活跃定期子户
			DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

			DpaSubAccount subAcctTime = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

			if (subAcctTime.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
				throw DpBase.E0017(cplIn.getFixed_acct_no(), cplIn.getFixed_sub_acct_seq());
			}
		}

		// 子账户定位
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 查询存款账户信息
		DpaAccount account = DpaAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), true);

		// 带锁，免得签约额的时候销户了
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 已销户不能签约：在定位之后查询的瞬间可能被销户了
		if (subAccount.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		// 产品透支许可标志检查
		if (subAccount.getOverdraft_allow_ind() != E_YESORNO.YES) {

			throw DpBase.E0293(subAccount.getProd_id());
		}

		// 检查额度编号
		DpbOverdraft overdraftInfo = DpbOverdraftDao.selectFirst_odb4(cplIn.getLimit_code(), E_STATUS.VALID, false);

		// 检查额度编号是否正在被签约账户、币种使用， 避免重复签约
		if (CommUtil.isNotNull(overdraftInfo)) {

			throw DpBase.E0295(overdraftInfo.getLimit_code());
		}

		// 调用额度查询
		DpCreditLimitInfo clAccountInfo = DpCreditLimitIobus.getCreditLimitInfo(cplIn.getLimit_code());

		if (clAccountInfo.getStatus() != E_LIMITSTATUS.NORMAL) {
			// 额度编号不正常
			throw DpBase.E9998(cplIn.getLimit_code(), clAccountInfo.getStatus().getLongName());
		}

		if (clAccountInfo.getExc_shr_flag() == E_EXCLUSIVESHARE.EXCLUSIVE && CommUtil.compare(clAccountInfo.getOccupied_amt(), BigDecimal.ZERO) > 0) {
			// 额度编号被占用
			throw DpBase.E0295(cplIn.getLimit_code());
		}

		if (clAccountInfo.getLeaf_node_ind() == E_LIMITCUTIND.MUST_CUT) {

			// 额度编号必须是末节节点
			// throw DpErr.Dp.E0433(cplIn.getLimit_code());
		}

		// 客户号与创建额度客户号不一致
		if (!CommUtil.equals(subAccount.getCust_no(), clAccountInfo.getCust_no())) {

			throw DpBase.E0332(subAccount.getCust_no(), clAccountInfo.getCust_no());
		}

		if (CommUtil.isNotNull(clAccountInfo.getProd_id()) && !CommUtil.equals(clAccountInfo.getProd_id(), overdraftType.getProd_id())) {
			throw DpBase.E0333(clAccountInfo.getProd_id(), overdraftType.getProd_id());
		}

		// 初始化客户号校验输入
		// IoClCheckCustOnUseLimit checkCustOnUseLimit =
		// BizUtil.getInstance(IoClCheckCustOnUseLimit.class);

		// 客户号校验
		// TODO 需要额度模块同步
		// if (account.getJoint_acct_ind() == E_YESORNO.NO) {
		//
		// checkCustOnUseLimit.setCust_no(subAccount.getCust_no());// 客户号
		//
		// }
		// else {
		//
		// // 查询联名客户信息
		// List<DpbJointAccount> jointAcctList =
		// DpbJointAccountDao.selectAll_odb2(account.getAcct_no(), false);
		//
		// DefaultOptions<IoClJointCustomer> defaultOptions = new
		// DefaultOptions<>();
		//
		// for (DpbJointAccount joinAcct : jointAcctList) {
		//
		// IoClJointCustomer jointCustomer =
		// BizUtil.getInstance(IoClJointCustomer.class);
		//
		// jointCustomer.setCust_no(joinAcct.getCust_no());
		// defaultOptions.add(jointCustomer);
		// }
		// checkCustOnUseLimit.setJoint_custs(defaultOptions);
		// }
		//
		// checkCustOnUseLimit.setLimit_code(cplIn.getLimit_code());
		//
		// limitQry.queryCustomerOnUseLimit(checkCustOnUseLimit);

		if (clAccountInfo.getLimit_type() == E_LIMITTYPE.JOINT && account.getJoint_acct_ind() != E_YESORNO.YES) {

			// throw DpErr.Dp.E0430(cplIn.getLimit_code());
		}

		if (clAccountInfo.getLimit_type() != E_LIMITTYPE.JOINT && account.getJoint_acct_ind() == E_YESORNO.YES) {

			// throw DpErr.Dp.E0431(account.getAcct_no());
		}

		// 账户币种与额度币种不一致
		if (overdraftType.getAllow_cross_ccy_ind() == E_ALLOW.BAN && !CommUtil.equals(subAccount.getCcy_code(), clAccountInfo.getCcy_code())) {

			throw DpBase.E0331(subAccount.getCcy_code(), clAccountInfo.getCcy_code());
		}

		bizlog.method("DpOverdraftAgreeSign.checkOverDraftSign end");

		// 返回子账户信息
		return subAccount;
	}
}
