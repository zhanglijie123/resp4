package cn.sunline.icore.dp.serv.maintain;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfInterest;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfOpen;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_PAYINSTWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RENEWSAVEWAY;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpInterestRateIobus;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpTdBeforceDueRenewMntIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpTdBeforceDueRenewMntOut;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpTimeBeforeRenewMnt {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpTimeBeforeRenewMnt.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年12月15日-下午2:23:48</li>
	 *         <li>功能说明：定期到期续存前维护入口</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpTdBeforceDueRenewMntOut timeBeforceDueRenewMnt(DpTdBeforceDueRenewMntIn cplIn) {
		bizlog.method(" DpTimeBeforeRenewMnt.timeBeforceDueRenewMnt begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 1. 对相关信息进行检查
		DpaSubAccount subAccount = checkTdBeforceDueRenewInfo(cplIn);

		// 2.修改前子账户信息
		DpaSubAccount oldSubAccount = BizUtil.clone(DpaSubAccount.class, subAccount);

		// 3.续存信息调整
		renewAdjust(cplIn, oldSubAccount, subAccount);

		int i = 0;
		// 登记审计信息
		i = ApDataAuditApi.regLogOnUpdateBusiness(oldSubAccount, subAccount);

		// 维护子账户信息
		if (i > 0) {
			DpaSubAccountDao.updateOne_odb1(subAccount);
		}

		DpTdBeforceDueRenewMntOut cplOut = BizUtil.getInstance(DpTdBeforceDueRenewMntOut.class);

		cplOut.setAcct_no(cplIn.getAcct_no());
		cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplOut.setAcct_name(cplIn.getAcct_name());
		cplOut.setCcy_code(cplIn.getCcy_code());
		cplOut.setProd_id(subAccount.getProd_id());
		cplOut.setProd_name(DpProductFactoryApi.getProdBaseInfo(subAccount.getProd_id()).getProd_name());
		cplOut.setTerm_code(subAccount.getTerm_code());
		cplOut.setAcct_bal(subAccount.getAcct_bal());
		cplOut.setStart_inst_date(subAccount.getStart_inst_date());
		cplOut.setDue_date(subAccount.getDue_date());
		cplOut.setRenewal_method(subAccount.getRenewal_method());
		cplOut.setPrin_trsf_acct(subAccount.getPrin_trsf_acct());
		cplOut.setPrin_trsf_acct_name(CommUtil.isNotNull(subAccount.getPrin_trsf_acct()) ? DpToolsApi.locateSingleAccount(subAccount.getPrin_trsf_acct(), null, false)
				.getAcct_name() : null);
		cplOut.setPrin_trsf_acct_ccy(subAccount.getPrin_trsf_acct_ccy());
		cplOut.setIncome_inst_acct(subAccount.getIncome_inst_acct());
		cplOut.setIncome_inst_acct_name(CommUtil.isNotNull(subAccount.getIncome_inst_acct()) ? DpToolsApi.locateSingleAccount(subAccount.getIncome_inst_acct(), null, false)
				.getAcct_name() : null);
		cplOut.setIncome_inst_ccy(subAccount.getIncome_inst_ccy());
		cplOut.setPrin_adjust_amt(subAccount.getRenew_save_amt());
		cplOut.setRenew_save_term(subAccount.getRenew_save_term());
		cplOut.setRenew_prod_id(subAccount.getRenew_prod_id());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpTimeBeforeRenewMnt.timeBeforceDueRenewMnt end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年12月18日-下午1:30:21</li>
	 *         <li>功能说明：续存信息调整</li>
	 *         </p>
	 * @param cplIn
	 * @param subAccount
	 *            旧子户信息
	 * @param newSubAccount
	 *            新子户信息
	 */
	private static void renewAdjust(DpTdBeforceDueRenewMntIn cplIn, DpaSubAccount subAccount, DpaSubAccount newSubAccount) {
		bizlog.method(" DpTimeBeforeRenewMnt.renewAdjust begin >>>>>>>>>>>>>>>>");

		if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.NONE) {

			newSubAccount.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
			newSubAccount.setRenew_prod_id(null); // 续存产品
			newSubAccount.setRenew_save_term(null); // 续存存期
			newSubAccount.setRenewl_pay_inst_cyc(null);
			newSubAccount.setRenew_save_amt(null); // 续存金额
			newSubAccount.setPrin_trsf_acct(null); // 本金账号
			newSubAccount.setPrin_trsf_acct_ccy(null);// 本金账号币种
			newSubAccount.setIncome_inst_acct(cplIn.getIncome_inst_acct()); // 收息账号
			newSubAccount.setIncome_inst_ccy(cplIn.getIncome_inst_ccy()); // 收息账号币种

		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.PRINCIPAL) {

			newSubAccount.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
			newSubAccount.setRenew_prod_id(CommUtil.nvl(cplIn.getRenew_prod_id(), subAccount.getProd_id())); // 续存产品
			newSubAccount.setRenew_save_term(CommUtil.nvl(cplIn.getRenew_save_term(), subAccount.getTerm_code())); // 续存存期
			newSubAccount.setRenewl_pay_inst_cyc(cplIn.getRenewl_pay_inst_cyc());
			newSubAccount.setRenew_save_amt(null); // 续存金额
			newSubAccount.setPrin_trsf_acct(null); // 本金账号
			newSubAccount.setPrin_trsf_acct_ccy(null);// 本金账号币种
			newSubAccount.setIncome_inst_acct(CommUtil.nvl(cplIn.getIncome_inst_acct(), cplIn.getPrin_trsf_acct())); // 收息账号
			newSubAccount.setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), cplIn.getPrin_trsf_acct_ccy())); // 收息账号币种

		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.PRIN_INST) {

			newSubAccount.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
			newSubAccount.setRenew_prod_id(CommUtil.nvl(cplIn.getRenew_prod_id(), subAccount.getProd_id())); // 续存产品
			newSubAccount.setRenew_save_term(CommUtil.nvl(cplIn.getRenew_save_term(), subAccount.getTerm_code())); // 续存存期
			newSubAccount.setRenewl_pay_inst_cyc(null);
			newSubAccount.setRenew_save_amt(null); // 续存金额
			newSubAccount.setPrin_trsf_acct(null); // 本金账号
			newSubAccount.setPrin_trsf_acct_ccy(null);// 本金账号币种
			newSubAccount.setIncome_inst_acct(CommUtil.nvl(cplIn.getIncome_inst_acct(), cplIn.getPrin_trsf_acct())); // 收息账号
			newSubAccount.setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), cplIn.getPrin_trsf_acct_ccy())); // 收息账号币种

		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.PART_AMOUNT) {

			newSubAccount.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
			newSubAccount.setRenew_prod_id(CommUtil.nvl(cplIn.getRenew_prod_id(), subAccount.getProd_id())); // 续存产品
			newSubAccount.setRenew_save_term(CommUtil.nvl(cplIn.getRenew_save_term(), subAccount.getTerm_code())); // 续存存期
			newSubAccount.setRenewl_pay_inst_cyc(null);
			newSubAccount.setRenew_save_amt(cplIn.getPrin_adjust_amt()); // 续存金额
			newSubAccount.setPrin_trsf_acct(cplIn.getPrin_trsf_acct()); // 本金账号
			newSubAccount.setPrin_trsf_acct_ccy(cplIn.getPrin_trsf_acct_ccy());// 本金账号币种
			newSubAccount.setIncome_inst_acct(CommUtil.nvl(cplIn.getIncome_inst_acct(), cplIn.getPrin_trsf_acct())); // 收息账号
			newSubAccount.setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), cplIn.getPrin_trsf_acct_ccy())); // 收息账号币种

		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.ADD_AMOUNT) {

			newSubAccount.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
			newSubAccount.setRenew_prod_id(CommUtil.nvl(cplIn.getRenew_prod_id(), subAccount.getProd_id())); // 续存产品
			newSubAccount.setRenew_save_term(CommUtil.nvl(cplIn.getRenew_save_term(), subAccount.getTerm_code())); // 续存存期
			newSubAccount.setRenewl_pay_inst_cyc(null);
			newSubAccount.setRenew_save_amt(cplIn.getPrin_adjust_amt()); // 续存金额
			newSubAccount.setPrin_trsf_acct(cplIn.getPrin_trsf_acct()); // 本金账号
			newSubAccount.setPrin_trsf_acct_ccy(cplIn.getPrin_trsf_acct_ccy());// 本金账号币种
			newSubAccount.setIncome_inst_acct(CommUtil.nvl(cplIn.getIncome_inst_acct(), cplIn.getPrin_trsf_acct())); // 收息账号
			newSubAccount.setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), cplIn.getPrin_trsf_acct_ccy())); // 收息账号币种

		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {

			newSubAccount.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
			newSubAccount.setRenew_prod_id(null); // 续存产品
			newSubAccount.setRenew_save_term(null); // 续存存期
			newSubAccount.setRenewl_pay_inst_cyc(null);
			newSubAccount.setRenew_save_amt(null); // 续存金额
			newSubAccount.setPrin_trsf_acct(cplIn.getPrin_trsf_acct()); // 本金账号
			newSubAccount.setPrin_trsf_acct_ccy(cplIn.getPrin_trsf_acct_ccy());// 本金账号币种
			newSubAccount.setIncome_inst_acct(CommUtil.nvl(cplIn.getIncome_inst_acct(), cplIn.getPrin_trsf_acct())); // 收息账号
			newSubAccount.setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), cplIn.getPrin_trsf_acct_ccy())); // 收息账号币种

		}

		bizlog.method(" DpTimeBeforeRenewMnt.renewAdjust end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年12月15日-下午2:27:52</li>
	 *         <li>功能说明：定期到期续存前维护数据校验</li>
	 *         </p>
	 * @param cplIn
	 */
	private static DpaSubAccount checkTdBeforceDueRenewInfo(DpTdBeforceDueRenewMntIn cplIn) {
		bizlog.method(" DpTimeBeforeRenewMnt.checkTdBeforceDueRenewInfo begin >>>>>>>>>>>>>>>>");

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());//
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());//
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());// 验密标志
		BizUtil.fieldNotNull(cplIn.getRenewal_method(), DpBaseDict.A.renewal_method.getId(), DpBaseDict.A.renewal_method.getLongName());//
		BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());//

		// 定位客户账户: 带锁查询
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		// 封装子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.TIME);
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 查询子账户信息：带锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 判断数据版本号是否是最新
		if (CommUtil.compare(cplIn.getData_version(), subAccount.getData_version()) != 0) {
			throw ApPubErr.APPUB.E0018(DpaSubAccount.class.getName());
		}

		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		if (CommUtil.isNull(subAccount.getDue_date())) {// 无到期日无需处理

			throw DpErr.Dp.E0281(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		if (CommUtil.isNull(subAccount.getTerm_code())) {// 无存期不需处理

			throw DpErr.Dp.E0282(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		// 账户已经到期
		if (CommUtil.compare(BizUtil.getTrxRunEnvs().getTrxn_date(), subAccount.getDue_date()) > 0) {

			throw DpErr.Dp.E0371(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(cplIn.getAcct_name(), acctAccessOut.getAcct_name())) {

			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctAccessOut.getAcct_name());
		}

		// 获取子户产品基础信息
		DpfBase subProdInfo = DpProductFactoryApi.getProdBaseInfo(subAccount.getProd_id());

		// 检查协议产品标志
		if (subProdInfo.getAgree_prod_ind() == E_YESORNO.YES) {

			throw DpErr.Dp.E0381(subAccount.getProd_id());
		}

		// 各类续存信息分类检查
		if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.NONE) {

			// TODO:
		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.PART_AMOUNT || cplIn.getRenewal_method() == E_RENEWSAVEWAY.ADD_AMOUNT) {

			BizUtil.fieldNotNull(cplIn.getPrin_trsf_acct(), DpBaseDict.A.prin_trsf_acct.getId(), DpBaseDict.A.prin_trsf_acct.getLongName());
			BizUtil.fieldNotNull(cplIn.getPrin_trsf_acct_ccy(), DpBaseDict.A.prin_trsf_acct_ccy.getId(), DpBaseDict.A.prin_trsf_acct_ccy.getLongName());
			BizUtil.fieldNotNull(cplIn.getPrin_adjust_amt(), DpBaseDict.A.prin_adjust_amt.getId(), DpBaseDict.A.prin_adjust_amt.getLongName());

			// 检查金额精度合法性
			ApCurrencyApi.chkAmountByCcy(subAccount.getCcy_code(), cplIn.getPrin_adjust_amt());

			if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.PART_AMOUNT) {

				DpPublicCheck.checkIncomeAcct(cplIn.getPrin_trsf_acct(), cplIn.getPrin_trsf_acct_ccy(), E_SAVEORWITHDRAWALIND.WITHDRAWAL);
			}
			else {

				DpPublicCheck.checkIncomeAcct(cplIn.getPrin_trsf_acct(), cplIn.getPrin_trsf_acct_ccy(), E_SAVEORWITHDRAWALIND.SAVE);
			}

			if (CommUtil.isNotNull(cplIn.getRenewl_pay_inst_cyc())) {

				throw DpErr.Dp.E0414(cplIn.getRenewal_method().getLongName());
			}
		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.PRINCIPAL) {

			BizUtil.fieldNotNull(cplIn.getIncome_inst_acct(), DpBaseDict.A.income_inst_acct.getId(), DpBaseDict.A.income_inst_acct.getLongName());
			BizUtil.fieldNotNull(cplIn.getIncome_inst_ccy(), DpBaseDict.A.income_inst_ccy.getId(), DpBaseDict.A.income_inst_ccy.getLongName());

			DpPublicCheck.checkIncomeAcct(cplIn.getIncome_inst_acct(), cplIn.getIncome_inst_ccy(), E_SAVEORWITHDRAWALIND.SAVE);
		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.PRIN_INST) {

			if (CommUtil.isNotNull(cplIn.getRenewl_pay_inst_cyc())) {

				throw DpErr.Dp.E0414(cplIn.getRenewal_method().getLongName());
			}

		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {

			BizUtil.fieldNotNull(cplIn.getPrin_trsf_acct(), DpBaseDict.A.prin_trsf_acct.getId(), DpBaseDict.A.prin_trsf_acct.getLongName());
			BizUtil.fieldNotNull(cplIn.getPrin_trsf_acct_ccy(), DpBaseDict.A.prin_trsf_acct_ccy.getId(), DpBaseDict.A.prin_trsf_acct_ccy.getLongName());
			BizUtil.fieldNotNull(cplIn.getIncome_inst_acct(), DpBaseDict.A.income_inst_acct.getId(), DpBaseDict.A.income_inst_acct.getLongName());
			BizUtil.fieldNotNull(cplIn.getIncome_inst_ccy(), DpBaseDict.A.income_inst_ccy.getId(), DpBaseDict.A.income_inst_ccy.getLongName());

			DpPublicCheck.checkIncomeAcct(cplIn.getIncome_inst_acct(), cplIn.getIncome_inst_ccy(), E_SAVEORWITHDRAWALIND.SAVE);
		}
		else {

			throw APPUB.E0026(DpBaseDict.A.renewal_method.getLongName(), cplIn.getRenewal_method().getValue());
		}

		DpaInterest instInfo = DpaInterestDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, true);

		// 周期性结息账户不能为本息转存
		if (instInfo.getPay_inst_method() == E_PAYINSTWAY.FIX_CYCLE && CommUtil.in(cplIn.getRenewal_method(), E_RENEWSAVEWAY.PRIN_INST)) {

			throw DpErr.Dp.E0415(cplIn.getRenewal_method().getLongName());
		}

		String termCode = cplIn.getRenew_save_term();

		// 续存产品检查
		if (CommUtil.isNotNull(cplIn.getRenew_prod_id())) {

			// 获取续存产品基础信息
			DpfBase prodBaseInfo = DpProductFactoryApi.getProdBaseInfo(cplIn.getRenew_prod_id());

			// 检查是否为定期产品
			if (prodBaseInfo.getDd_td_ind() == E_DEMANDORTIME.DEMAND) {

				throw DpBase.E0122(prodBaseInfo.getProd_id());
			}

			if (prodBaseInfo.getAgree_prod_ind() != subProdInfo.getAgree_prod_ind()) {

				// 协议产品标志不一致,产品[]不允许续存[]产品
				throw DpBase.E0380(subAccount.getProd_id(), cplIn.getRenew_prod_id());
			}

			if (prodBaseInfo.getAgree_prod_ind() == E_YESORNO.YES && cplIn.getRenewal_method() != subAccount.getRenewal_method()) {

				throw DpErr.Dp.E0381(subAccount.getProd_id());
			}

			DpfInterest prodInstInfo = DpProductFactoryApi.getProdInterestDefine(cplIn.getRenew_prod_id(), subAccount.getCcy_code(), E_INSTKEYTYPE.NORMAL, false);

			if (prodInstInfo != null && prodInstInfo.getPay_inst_method() == E_PAYINSTWAY.FIX_CYCLE) {

				BizUtil.fieldNotNull(cplIn.getRenewl_pay_inst_cyc(), DpBaseDict.A.renewl_pay_inst_cyc.getId(), DpBaseDict.A.renewl_pay_inst_cyc.getLongName());
				if (!BizUtil.isCycleString(cplIn.getRenewl_pay_inst_cyc())) {
					throw APPUB.E0012(cplIn.getRenewl_pay_inst_cyc());
				}
			}

			DpfOpen openCtrl = DpProductFactoryApi.getProdOpenCtrl(cplIn.getRenew_prod_id(), subAccount.getCcy_code());

			// 如果产品设置了存期范围，但续存存期又不在产品范围之内，则报错
			if (CommUtil.isNotNull(openCtrl.getTerm_scope()) && !openCtrl.getTerm_scope().contains(termCode)) {

				throw DpBase.E0244();
			}

			DpAccountTypeParmApi.checkProdAdaptAcctType(cplIn.getRenew_prod_id(), acctInfo.getAcct_type());

		}

		// 续存存期的适配性检查
		if (CommUtil.isNotNull(cplIn.getRenew_save_term())) {

			// 账户计息定义信息
			DpaInterest inrtInfo = DpaInterestDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, true);

			DpInterestRateIobus.checkTermRateMatch(inrtInfo.getInrt_code(), subAccount.getCcy_code(), cplIn.getRenew_save_term(), inrtInfo.getInst_rate_file_way());
		}

		bizlog.method(" DpTimeBeforeRenewMnt.checkTdBeforceDueRenewInfo end <<<<<<<<<<<<<<<<");
		return subAccount;
	}
}
