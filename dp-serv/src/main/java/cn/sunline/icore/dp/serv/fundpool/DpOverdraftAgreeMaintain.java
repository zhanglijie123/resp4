package cn.sunline.icore.dp.serv.fundpool;

import java.math.BigDecimal;
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
import cn.sunline.icore.dp.base.type.ComDpOverdraftTrxnBasic.DpOverdraftAgreeModifyIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_OVERDRAFTINSTTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_OVERDRAFTRATESOURCE;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftMntIn;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftMntOut;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_FLOATWAY;
import cn.sunline.icore.sys.type.EnumType.E_INRTDIRECTION;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.odb.OdbFactory;
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

public class DpOverdraftAgreeMaintain {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOverdraftAgreeMaintain.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年7月8日-上午9:59:19</li>
	 *         <li>功能说明：透支协议维护</li>
	 *         </p>
	 * @param DpOverdraftMntIn
	 */
	public static DpOverdraftMntOut overdraftAgreeMnt(DpOverdraftMntIn cplIn) {

		bizlog.method("DpOverdraftAgreeMaintain.overdraftAgreeMnt begin");

		// 非空要素检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getAgree_no(), SysDict.A.agree_no.getId(), SysDict.A.agree_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		// 获取透支协议信息
		DpbOverdraft OdAgree = DpbOverdraftDao.selectOne_odb1(acctInfo.getAcct_no(), cplIn.getAgree_no(), false);

		if (CommUtil.isNull(OdAgree)) {

			throw ApPubErr.APPUB.E0024(OdbFactory.getTable(DpbOverdraft.class).getLongname(), SysDict.A.acct_no.getLongName(), acctInfo.getAcct_no(),
					SysDict.A.agree_no.getLongName(), cplIn.getAgree_no());
		}

		// 子户定位
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(OdAgree.getAcct_no());
		acctAccessIn.setAcct_type(null);
		acctAccessIn.setCcy_code(OdAgree.getCcy_code());

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 锁住子账户信息
		DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 透支协议维护检查
		checkOverdraftMnt(cplIn, OdAgree, acctInfo);

		// 透支协议信息维护
		DpOverdraftAgreeModifyIn cplAgreeModifyIn = BizUtil.getInstance(DpOverdraftAgreeModifyIn.class);

		cplAgreeModifyIn.setEffect_date(cplIn.getEffect_date());
		cplAgreeModifyIn.setExpiry_date(cplIn.getExpiry_date());
		cplAgreeModifyIn.setOverdraft_type(OdAgree.getOverdraft_type());
		cplAgreeModifyIn.setOverdraft_rate_source(cplIn.getOverdraft_rate_source());
		cplAgreeModifyIn.setFixed_acct_no(cplIn.getFixed_acct_no());
		cplAgreeModifyIn.setFixed_sub_acct_seq(cplIn.getFixed_sub_acct_seq());
		cplAgreeModifyIn.setInrt_code(cplIn.getInrt_code());
		cplAgreeModifyIn.setContract_inrt(cplIn.getContract_inrt());
		cplAgreeModifyIn.setOverdraft_inrt_float_method(cplIn.getOverdraft_inrt_float_method());
		cplAgreeModifyIn.setOverdraft_inrt_float_value(cplIn.getOverdraft_inrt_float_value());
		cplAgreeModifyIn.setOverdue_inrt_float_method(cplIn.getOverdue_inrt_float_method());
		cplAgreeModifyIn.setOverdue_inrt_float_value(cplIn.getOverdue_inrt_float_value());
		cplAgreeModifyIn.setOver_quota_inrt_float_method(cplIn.getOver_quota_inrt_float_method());
		cplAgreeModifyIn.setOver_quota_inrt_float_value(cplIn.getOver_quota_inrt_float_value());

		boolean mdyInrtSuccess = DpOverDraftApi.modifyAgreeOverdraftInfo(cplAgreeModifyIn, OdAgree);

		// 读最新缓存信息
		OdAgree = DpbOverdraftDao.selectOne_odb1(acctInfo.getAcct_no(), cplIn.getAgree_no(), false);

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 额度重新分布
		if (mdyInrtSuccess || (CommUtil.equals(OdAgree.getEffect_date(), trxnDate) && !CommUtil.equals(OdAgree.getEffect_date(), OdAgree.getEffect_date()))) {

			DpOverDraftApi.mdyODInrtAfreshLimitLayout(OdAgree, subAcct);
		}

		// 输出
		DpOverdraftMntOut cplOut = BizUtil.getInstance(DpOverdraftMntOut.class);

		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_type(acctInfo.getAcct_type());// 账号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setCcy_code(OdAgree.getCcy_code()); // 币种
		cplOut.setAgree_no(OdAgree.getAgree_no()); // 协议号
		cplOut.setEffect_date(OdAgree.getEffect_date()); // 生效日期
		cplOut.setExpiry_date(OdAgree.getExpiry_date()); // 失效日期
		cplOut.setAgree_status(OdAgree.getAgree_status()); // 协议状态
		cplOut.setOverdraft_rate_source(cplIn.getOverdraft_rate_source());// 透支利率来源
		cplOut.setFixed_acct_no(cplIn.getFixed_acct_no());// 定期账号
		cplOut.setFixed_sub_acct_seq(cplIn.getFixed_sub_acct_seq());// 定期子账户序号
		cplOut.setInrt_code(cplIn.getInrt_code());// 利率编号
		cplOut.setOverdraft_inrt_float_method(cplIn.getOverdraft_inrt_float_method());// 透支利率浮动方式
		cplOut.setOverdraft_inrt_float_value(cplIn.getOverdraft_inrt_float_value()); // 透支利率浮动值
		cplOut.setOver_quota_inrt_float_method(cplIn.getOver_quota_inrt_float_method());// 超额利率浮动方式
		cplOut.setOver_quota_inrt_float_value(cplIn.getOver_quota_inrt_float_value());// 超额利率浮动值
		cplOut.setOverdue_inrt_float_method(cplIn.getOverdue_inrt_float_method());// 逾期利率浮动方式
		cplOut.setOverdue_inrt_float_value(cplIn.getOverdue_inrt_float_value());// 逾期利率浮动值

		// 查询利率
		List<DpbOverdraftSlip> listOdFiche = DpbOverdraftSlipDao.selectAll_odb3(OdAgree.getAcct_no(), OdAgree.getAgree_no(), false);

		for (DpbOverdraftSlip OdFiche : listOdFiche) {

			DpaSlip ficheInfo = DpaSlipDao.selectOne_odb1(OdAgree.getAcct_no(), OdFiche.getFiche_no(), true);

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

		bizlog.method("DpOverdraftAgreeMaintain.overdraftAgreeMnt end");

		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年10月21日-上午9:55:43</li>
	 *         <li>功能说明：透支协议维护检查</li>
	 *         </p>
	 * @param cplIn
	 *            维护输入
	 * @param OdAgree
	 *            透支协议
	 * @param acctInfo
	 *            账户信息
	 */
	private static void checkOverdraftMnt(DpOverdraftMntIn cplIn, DpbOverdraft OdAgree, DpaAccount acctInfo) {

		bizlog.method("DpOverdraftAgree.checkOverdraftMnt begin >>>>>>>>>>>>>");

		// 协议状态不正常
		if (OdAgree.getAgree_status() != E_STATUS.VALID) {

			throw DpBase.E0296(OdAgree.getAgree_no());
		}

		// 判断数据版本号是否是最新
		if (CommUtil.compare(cplIn.getData_version(), OdAgree.getData_version()) != 0) {

			throw ApPubErr.APPUB.E0018(DpbOverdraft.class.getName());
		}

		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			// TODO: DpCheckPassWord checkIn =
			// BizUtil.getInstance(DpCheckPassWord.class);

			// TODO: checkIn.setTrxn_password(cplIn.getTrxn_password());

			// TODO: DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 失效日期必须大于交易日期
		if (CommUtil.isNotNull(cplIn.getExpiry_date()) && CommUtil.compare(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) <= 0) {

			throw DpBase.E0294(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
		}

		// 生效日期必须小于失效日期
		if (CommUtil.isNotNull(cplIn.getEffect_date()) && CommUtil.isNotNull(cplIn.getExpiry_date())) {

			BizUtil.checkEffectDate(cplIn.getEffect_date(), cplIn.getExpiry_date());
		}

		// 查询透支参数类型
		DppOverdraftType overdraftType = DpOverdraftParmApi.getOverDraftTypeInfo(OdAgree.getOverdraft_type());

		// 利率来源为手工设置
		if (cplIn.getOverdraft_rate_source() == E_OVERDRAFTRATESOURCE.MANUAL_SET) {

			// 由关联定期利率变更为手工设置
			if (OdAgree.getOverdraft_rate_source() == E_OVERDRAFTRATESOURCE.RELATION_RECEIPT) {

				BizUtil.fieldNotNull(cplIn.getInrt_code(), SysDict.A.inrt_code.getId(), SysDict.A.inrt_code.getLongName());
			}

			// 透支利率编号若有修改
			if (CommUtil.isNotNull(cplIn.getInrt_code())) {

				if (!CommUtil.equals(cplIn.getInrt_code(), overdraftType.getInrt_code()) && overdraftType.getAllow_hand_rate_ind() == E_YESORNO.YES) {

					Options<DpInrtCodeDefine> InrtCodeList = DpRateBasicApi.getContractRateCode(E_INRTDIRECTION.SIMPLE_CONT);

					// 输入的利率编号不在许可范围内
					if (SysUtil.serialize(InrtCodeList).contains(cplIn.getInrt_code())) {

						// throw DpErr.Dp.E0402(cplIn.getInrt_code());
					}
				}

				// 合法性检查
				DpInrtCodeDefine cmInterestRate = DpRateBasicApi.getInrtCodeDefine(cplIn.getInrt_code());

				// 利率编号不合法
				// if (CommUtil.in(cmInterestRate.getInrt_code_direction(),
				// E_INRTDIRECTION.LAYER_CONT, E_INRTDIRECTION.LAYER,
				// E_INRTDIRECTION.REFERENCE)) {
				//
				// throw DpErr.Dp.E0254();
				// }

				// 为合同利率时
				if (cmInterestRate.getInrt_code_direction() == E_INRTDIRECTION.SIMPLE_CONT) {

					// 合同利率必输
					BizUtil.fieldNotNull(cplIn.getContract_inrt(), DpBaseDict.A.contract_inrt.getId(), DpBaseDict.A.contract_inrt.getLongName());

					// 透支合同利率不能浮动
					if (cplIn.getOverdraft_inrt_float_method() != null && cplIn.getOverdraft_inrt_float_method() != E_FLOATWAY.NONE) {

						throw DpBase.E0403();
					}
				}
			}
		}
		else {

			if (OdAgree.getOverdraft_rate_source() == E_OVERDRAFTRATESOURCE.MANUAL_SET) {

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
		}

		bizlog.method("DpOverdraftAgree.checkOverdraftMnt end <<<<<<<<<<<<<");

	}
}
