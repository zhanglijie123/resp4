package cn.sunline.icore.dp.serv.maintain;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DATAOPERATE;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAccountApi;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApSummaryApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.type.ComApAccounting.ApAccountingEventIn;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpbJointAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpbJointAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterest;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DpaInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfInterest;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfInterestDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfOpen;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_PAYINSTWAY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RENEWSAVEWAY;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpInterestRateIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpAcctQueryDao;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbAccountSignature;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbAccountSignatureDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAccountBranchChangeIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAccountBranchChangeOut;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAcctSignInfoMaintainIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAcountMaintainIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpAcountMaintainOut;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ASSETORDEBT;
import cn.sunline.icore.iobus.dp.type.ComIoDpOpenAccount.IoDpRegAccountMappingIn;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUNTINGSUBJECT;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_BALPROPERTY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：存款账户信息维护
 * </p>
 * 
 * @Author yangdl
 *         <p>
 *         <li>2017年2月15日-下午3:02:34</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：存款账户信息维护</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */

public class DpAccountMaintain {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAccountMaintain.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年2月17日-上午10:34:29</li>
	 *         <li>功能说明：存款账户信息维护</li>
	 *         </p>
	 * @param cplIn
	 *            账户维护信息输入
	 * @return cplOut
	 */

	public static DpAcountMaintainOut acountMaintain(DpAcountMaintainIn cplIn) {

		bizlog.method("DpAccountMaintain.accountMaintain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("accountMaintain=[%s]", cplIn);

		// 必须要素检查
		checkAcctMaintainNull(cplIn);

		// 定位客户账户: 带锁查询
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 封装子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(cplIn.getDd_td_ind());
		acctAccessIn.setProd_id(cplIn.getProd_id());
		// acctAccessIn.setSave_or_draw_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL);
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 查询子账户信息：带锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

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

		// 修改后子账户信息
		DpaSubAccount oldSubAccount = BizUtil.clone(DpaSubAccount.class, subAccount);
        //在这里检查一下账户时间的格式问题
	    if(!BizUtil.isDateString(cplIn.getAcct_valid_date())) {
		   throw ApPubErr.APPUB.E0002(cplIn.getAcct_valid_date(), SysDict.A.create_date.getId(), SysDict.A.create_date.getLongName());
	    }
		subAccount.setAcct_valid_date(CommUtil.nvl(cplIn.getAcct_valid_date(), subAccount.getAcct_valid_date())); // 账户有效期
		subAccount.setChannel_remark(CommUtil.nvl(cplIn.getChannel_remark(), subAccount.getChannel_remark())); // 渠道备注
		subAccount.setInst_ind(CommUtil.nvl(cplIn.getInst_ind(), subAccount.getInst_ind())); // 计息标志
		subAccount.setTax_rate_code(CommUtil.nvl(cplIn.getTax_rate_code(), subAccount.getTax_rate_code()));// 税率编号
		subAccount.setAcct_manager_id(CommUtil.nvl(cplIn.getAcct_manager_id(), subAccount.getAcct_manager_id())); // 账户经理号
		subAccount.setAcct_manager_name(CommUtil.nvl(cplIn.getAcct_manager_name(), subAccount.getAcct_manager_name())); // 账户经理名称

		subAccount.setRenewal_method(CommUtil.nvl(cplIn.getRenewal_method(), subAccount.getRenewal_method())); // 续存方式
		subAccount.setRenew_prod_id(CommUtil.nvl(cplIn.getRenew_prod_id(), subAccount.getRenew_prod_id())); // 续存产品
		subAccount.setRenew_save_term(CommUtil.nvl(cplIn.getRenew_save_term(), subAccount.getRenew_save_term())); // 续存存期
		subAccount.setRenew_save_amt(CommUtil.nvl(cplIn.getRenew_save_amt(), subAccount.getRenew_save_amt())); // 续存金额
		subAccount.setPrin_trsf_acct(CommUtil.nvl(cplIn.getPrin_trsf_acct(), subAccount.getPrin_trsf_acct())); // 本金账号
		subAccount.setPrin_trsf_acct_ccy(CommUtil.nvl(cplIn.getPrin_trsf_acct_ccy(), subAccount.getPrin_trsf_acct_ccy()));// 本金账号币种
		subAccount.setIncome_inst_acct(CommUtil.nvl(CommUtil.nvl(cplIn.getIncome_inst_acct(), subAccount.getIncome_inst_acct()), subAccount.getPrin_trsf_acct())); // 收息账号
		subAccount.setIncome_inst_ccy(CommUtil.nvl(CommUtil.nvl(cplIn.getIncome_inst_ccy(), subAccount.getIncome_inst_ccy()), subAccount.getPrin_trsf_acct_ccy())); // 收息账号币种

		subAccount.setAcct_manager_id(CommUtil.nvl(cplIn.getAcct_manager_id(), subAccount.getAcct_manager_id())); // 账户经理编号
		subAccount.setAcct_manager_name(CommUtil.nvl(cplIn.getAcct_manager_name(), subAccount.getAcct_manager_name())); // 账户经理名称
		subAccount.setMandatory_tax_deduct_ind(CommUtil.nvl(cplIn.getMandatory_tax_deduct_ind(), subAccount.getMandatory_tax_deduct_ind())); // 强制扣税标志

		if (CommUtil.isNotNull(cplIn.getStart_inst_date())) { // 起息日期不为空,则维护起息日期

			subAccount.setInit_inst_start_date(cplIn.getStart_inst_date()); // 起息日期

			// 由于维护了起息日期,需要更新计息定义表
			DpaInterest instInfo = DpaInterestDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, false);

			if (instInfo != null) {

				DpaInterest oldInstInfo = BizUtil.clone(DpaInterest.class, instInfo);

				instInfo.setStart_inst_date(cplIn.getStart_inst_date());
				DpaInterestDao.updateOne_odb1(instInfo);

				ApDataAuditApi.regLogOnUpdateBusiness(oldInstInfo, instInfo);
			}

		}

		// TODO:账户到期日期：账户存期不变，如果账户起息日期变了，账户到期日是否需要修改？需要银行确认。

		// 续存信息调整
		renewalAdjustmentInfo(cplIn, acctInfo, oldSubAccount, subAccount);

		int i = 0;
		// 登记审计信息
		i = ApDataAuditApi.regLogOnUpdateBusiness(oldSubAccount, subAccount);

		// 维护子账户信息
		if (i > 0) {
			DpaSubAccountDao.updateOne_odb1(subAccount);
		}

		// 修改前账户信息
		DpaAccount oldAccountInfo = BizUtil.clone(DpaAccount.class, acctInfo);

		acctInfo.setAcct_name(CommUtil.nvl(cplIn.getAcct_name(), acctInfo.getAcct_name()));
		acctInfo.setAcct_oth_name(CommUtil.nvl(cplIn.getAcct_oth_name(), acctInfo.getAcct_oth_name())); // 账户别名
		acctInfo.setAddress_type(CommUtil.nvl(cplIn.getAddress_type(), acctInfo.getAddress_type())); // 邮寄地址
		acctInfo.setHold_mail(CommUtil.nvl(cplIn.getHold_mail(), acctInfo.getHold_mail())); // 是否邮寄标志

		// 登记审计信息
		int j = ApDataAuditApi.regLogOnUpdateBusiness(oldAccountInfo, acctInfo);

		// 维护账户信息
		if (i == 0 && j == 0) {
			throw ApPubErr.APPUB.E0023(OdbFactory.getTable(DpaAccount.class).getLongname());
		}
		else if (j > 0) {
			DpaAccountDao.updateOne_odb1(acctInfo);
		}

		// 输出
		DpAcountMaintainOut cplOut = BizUtil.getInstance(DpAcountMaintainOut.class);

		cplOut.setAcct_no(acctInfo.getAcct_no());
		cplOut.setAcct_name(acctInfo.getAcct_name());
		cplOut.setAcct_oth_name(acctInfo.getAcct_oth_name());
		cplOut.setSub_acct_seq(subAccount.getSub_acct_seq());
		cplOut.setAcct_type(acctInfo.getAcct_type());
		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), acctInfo.getAcct_no()) ? null : cplIn.getAcct_no());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAccountMaintain.accountMaintain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年9月7日-上午10:05:38</li>
	 *         <li>功能说明：续存信息调整</li>
	 *         </p>
	 * @param cplIn
	 * @param acctInfo
	 * @param subAccount
	 * @param newSubAccount
	 */
	private static void renewalAdjustmentInfo(DpAcountMaintainIn cplIn, DpaAccount acctInfo, DpaSubAccount subAccount, DpaSubAccount newSubAccount) {
		bizlog.method(" DpAccountMaintain.renewalAdjustmentInfo begin >>>>>>>>>>>>>>>>");

		// 非续存直接退出
		if (CommUtil.isNull(cplIn.getRenewal_method())) {

			return;
		}

		// 续存信息调整检查
		checkrRenewalAdjustmentInfo(cplIn, acctInfo, subAccount);

		// 续存信息调整
		if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.NONE) {

			newSubAccount.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
			newSubAccount.setRenew_prod_id(null); // 续存产品
			newSubAccount.setRenew_save_term(null); // 续存存期
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
			newSubAccount.setRenew_save_amt(cplIn.getRenew_save_amt()); // 续存金额
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
			newSubAccount.setRenew_save_amt(cplIn.getRenew_save_amt()); // 续存金额
			newSubAccount.setPrin_trsf_acct(cplIn.getPrin_trsf_acct()); // 本金账号
			newSubAccount.setPrin_trsf_acct_ccy(cplIn.getPrin_trsf_acct_ccy());// 本金账号币种
			newSubAccount.setIncome_inst_acct(CommUtil.nvl(cplIn.getIncome_inst_acct(), cplIn.getPrin_trsf_acct())); // 收息账号
			newSubAccount.setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), cplIn.getPrin_trsf_acct_ccy())); // 收息账号币种

		}
		else if (cplIn.getRenewal_method() == E_RENEWSAVEWAY.MATURE_TO_OTHER_ACCT) {

			newSubAccount.setRenewal_method(cplIn.getRenewal_method()); // 续存方式
			newSubAccount.setRenew_prod_id(null); // 续存产品
			newSubAccount.setRenew_save_term(null); // 续存存期
			newSubAccount.setRenew_save_amt(null); // 续存金额
			newSubAccount.setPrin_trsf_acct(cplIn.getPrin_trsf_acct()); // 本金账号
			newSubAccount.setPrin_trsf_acct_ccy(cplIn.getPrin_trsf_acct_ccy());// 本金账号币种
			newSubAccount.setIncome_inst_acct(CommUtil.nvl(cplIn.getIncome_inst_acct(), cplIn.getPrin_trsf_acct())); // 收息账号
			newSubAccount.setIncome_inst_ccy(CommUtil.nvl(cplIn.getIncome_inst_ccy(), cplIn.getPrin_trsf_acct_ccy())); // 收息账号币种

		}

		bizlog.method(" DpAccountMaintain.renewalAdjustmentInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年9月7日-上午10:04:48</li>
	 *         <li>功能说明：续存信息检查</li>
	 *         </p>
	 * @param cplIn
	 * @param acctInfo
	 * @param subAccount
	 */
	private static void checkrRenewalAdjustmentInfo(DpAcountMaintainIn cplIn, DpaAccount acctInfo, DpaSubAccount subAccount) {
		bizlog.method(" DpAccountMaintain.checkrRenewalAdjustmentInfo begin >>>>>>>>>>>>>>>>");

		// 检查是否为定期账户
		if (subAccount.getDd_td_ind() == E_DEMANDORTIME.DEMAND) {

			throw DpBase.E0280(subAccount.getAcct_no());
		}

		if (CommUtil.isNull(subAccount.getDue_date())) {

			throw DpErr.Dp.E0281(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		if (CommUtil.isNull(subAccount.getTerm_code())) {

			throw DpErr.Dp.E0282(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		if (CommUtil.compare(BizUtil.getTrxRunEnvs().getTrxn_date(), subAccount.getDue_date()) > 0) {

			throw DpErr.Dp.E0371(subAccount.getAcct_no(), subAccount.getSub_acct_seq());
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
			BizUtil.fieldNotNull(cplIn.getRenew_save_term(), DpBaseDict.A.renew_save_term.getId(), DpBaseDict.A.renew_save_term.getLongName());
			BizUtil.fieldNotNull(cplIn.getRenew_prod_id(), DpBaseDict.A.renew_prod_id.getId(), DpBaseDict.A.renew_prod_id.getLongName());
			BizUtil.fieldNotNull(cplIn.getPrin_trsf_acct_ccy(), DpBaseDict.A.prin_trsf_acct_ccy.getId(), DpBaseDict.A.prin_trsf_acct_ccy.getLongName());
			BizUtil.fieldNotNull(cplIn.getRenew_save_amt(), DpBaseDict.A.prin_adjust_amt.getId(), DpBaseDict.A.prin_adjust_amt.getLongName());

			// 检查金额精度合法性
			ApCurrencyApi.chkAmountByCcy(subAccount.getCcy_code(), cplIn.getRenew_save_amt());

			DpPublicCheck.checkIncomeAcct(cplIn.getPrin_trsf_acct(), cplIn.getPrin_trsf_acct_ccy(), E_SAVEORWITHDRAWALIND.SAVE);

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

		// 续存存期的适配性检查
		if (CommUtil.isNotNull(cplIn.getRenew_save_term())) {

			DpfInterest inrtInfo = DpfInterestDao.selectOne_odb1(CommUtil.nvl(cplIn.getRenew_prod_id(), subAccount.getProd_id()), subAccount.getCcy_code(), E_INSTKEYTYPE.NORMAL,
					true);

			DpInterestRateIobus.checkTermRateMatch(instInfo.getInrt_code(), subAccount.getCcy_code(), cplIn.getRenew_save_term(), inrtInfo.getInst_rate_file_way());
		}

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

			}

			DpfOpen openCtrl = DpProductFactoryApi.getProdOpenCtrl(cplIn.getRenew_prod_id(), subAccount.getCcy_code());

			// 如果产品设置了存期范围，但续存存期又不在产品范围之内，则报错
			if (CommUtil.isNotNull(openCtrl.getTerm_scope()) && !openCtrl.getTerm_scope().contains(cplIn.getRenew_save_term())) {

				throw DpBase.E0244();
			}

			DpAccountTypeParmApi.checkProdAdaptAcctType(cplIn.getRenew_prod_id(), acctInfo.getAcct_type());

		}

		if (CommUtil.isNotNull(cplIn.getIncome_inst_acct())) {

			DpPublicCheck.checkIncomeAcct(cplIn.getIncome_inst_acct(), cplIn.getIncome_inst_ccy(), E_SAVEORWITHDRAWALIND.SAVE);
		}

		bizlog.method(" DpAccountMaintain.checkrRenewalAdjustmentInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年2月17日-上午9:46:31</li>
	 *         <li>功能说明：账户信息必须字段检查</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void checkAcctMaintainNull(DpAcountMaintainIn cplIn) {

		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号
		BizUtil.fieldNotNull(cplIn.getDd_td_ind(), SysDict.A.dd_td_ind.getId(), SysDict.A.dd_td_ind.getLongName());// 定活标志
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());// 验密标志
		BizUtil.fieldNotNull(cplIn.getAcct_name(), SysDict.A.acct_name.getId(), SysDict.A.acct_name.getLongName());// 验密标志
    }

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年12月6日-下午3:04:08</li>
	 *         <li>功能说明：账户维护IOBUS入口</li>
	 *         </p>
	 * @param acountMaintainIn
	 * @return
	 */
	public static DpAcountMaintainOut ioAcountMaintain(DpAcountMaintainIn acountMaintainIn) {
		bizlog.method(" DpAccountMaintain.ioAcountMaintain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("acountMaintainIn=[%s]", acountMaintainIn);

		DpAcountMaintainIn acctMainIn = BizUtil.getInstance(DpAcountMaintainIn.class);

		acctMainIn.setAcct_no(acountMaintainIn.getAcct_no());
		acctMainIn.setAcct_type(acountMaintainIn.getAcct_type());
		acctMainIn.setAcct_name(acountMaintainIn.getAcct_name());
		acctMainIn.setAcct_oth_name(acountMaintainIn.getAcct_oth_name());
		acctMainIn.setDd_td_ind(acountMaintainIn.getDd_td_ind());
		acctMainIn.setSub_acct_seq(acountMaintainIn.getSub_acct_seq());
		acctMainIn.setProd_id(acountMaintainIn.getProd_id()); // product code
		acctMainIn.setCcy_code(acountMaintainIn.getCcy_code()); // currency code
		acctMainIn.setCheck_password_ind(acountMaintainIn.getCheck_password_ind());
		acctMainIn.setTrxn_password(acountMaintainIn.getTrxn_password());
		acctMainIn.setAcct_valid_date(acountMaintainIn.getAcct_valid_date());
		acctMainIn.setIncome_inst_acct(acountMaintainIn.getIncome_inst_acct());
		acctMainIn.setIncome_inst_ccy(acountMaintainIn.getIncome_inst_ccy());
		acctMainIn.setChannel_remark(acountMaintainIn.getChannel_remark());
		acctMainIn.setInst_ind(acountMaintainIn.getInst_ind());
		acctMainIn.setTax_rate_code(acountMaintainIn.getTax_rate_code());
		acctMainIn.setAcct_manager_id(acountMaintainIn.getAcct_manager_id());
		acctMainIn.setAcct_manager_name(acountMaintainIn.getAcct_manager_name());
		acctMainIn.setData_version(acountMaintainIn.getData_version());
		acctMainIn.setRenewal_method(acountMaintainIn.getRenewal_method());
		acctMainIn.setRenew_prod_id(acountMaintainIn.getRenew_prod_id());
		acctMainIn.setRenew_save_term(acountMaintainIn.getRenew_save_term());
		acctMainIn.setRenew_save_amt(acountMaintainIn.getRenew_save_amt());
		acctMainIn.setPrin_trsf_acct(acountMaintainIn.getPrin_trsf_acct());
		acctMainIn.setPrin_trsf_acct_ccy(acountMaintainIn.getPrin_trsf_acct_ccy());
		acctMainIn.setAddress_type(acountMaintainIn.getAddress_type());
		acctMainIn.setHold_mail(acountMaintainIn.getHold_mail()); // hold mail
		acctMainIn.setStart_inst_date(acountMaintainIn.getStart_inst_date());

		DpAcountMaintainOut acctMainOut = acountMaintain(acctMainIn);

		DpAcountMaintainOut cplOut = BizUtil.getInstance(DpAcountMaintainOut.class);

		cplOut.setCard_no(acctMainOut.getCard_no()); // card no
		cplOut.setAcct_no(acctMainOut.getAcct_no()); // acctount no
		cplOut.setAcct_type(acctMainOut.getAcct_type()); // account type
		cplOut.setAcct_name(acctMainOut.getAcct_name()); // account name
		cplOut.setAcct_oth_name(acctMainOut.getAcct_oth_name());
		cplOut.setSub_acct_seq(acctMainOut.getSub_acct_seq());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAccountMaintain.ioAcountMaintain end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Liubx
	 *         <p>
	 *         <li>2018年3月5日-下午4:42:23</li>
	 *         <li>功能说明：账户机构变更IOBUS入口</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpAccountBranchChangeOut ioAccountBrchChange(DpAccountBranchChangeIn cplIn) {
		bizlog.method(" DpAccountMaintain.ioAccountBrchChange begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 账号不能为空
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号

		// 验密标志不能为空
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());// 验密标志

		// 定位客户账户: 带锁查询
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 封装子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setProd_id(cplIn.getProd_id());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 查询子账户信息：带锁
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 变更子账户机构
		modifyAccountBranch(subAccount, cplIn.getSub_acct_branch());

		// DpaSubAccount newSubAccount =
		// DpaSubAccountDao.selectOne_odb1(acctAccessOut.getSub_acct_no(),
		// true);

		// 输出
		DpAccountBranchChangeOut cplOut = BizUtil.getInstance(DpAccountBranchChangeOut.class);

		cplOut.setAcct_no(acctInfo.getAcct_no());
		cplOut.setAcct_name(acctInfo.getAcct_name());
		cplOut.setSub_acct_branch(subAccount.getSub_acct_branch());
		cplOut.setProd_id(subAccount.getProd_id());
		cplOut.setProd_name(DpProductFactoryApi.getProdBaseInfo(subAccount.getProd_id()).getProd_name());
		cplOut.setSub_acct_seq(subAccount.getSub_acct_seq());
		cplOut.setCcy_code(subAccount.getCcy_code());
		cplOut.setAcct_bal(subAccount.getAcct_bal());

		DpaInterest instInfo = DpaInterestDao.selectOne_odb1(subAccount.getAcct_no(), subAccount.getSub_acct_no(), E_INSTKEYTYPE.NORMAL, false);

		if (instInfo != null) {

			cplOut.setStart_inst_date(instInfo.getStart_inst_date());
		}

		cplOut.setDue_date(subAccount.getDue_date());
		cplOut.setBranch_name(ApBranchApi.getItem(BizUtil.getTrxRunEnvs().getTrxn_branch()).getBranch_name());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAccountMaintain.ioAccountBrchChange end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author Liubx
	 *         <p>
	 *         <li>2018年3月6日-上午11:18:51</li>
	 *         <li>功能说明：存款子账户机构维护</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 * @param subAccountBranch
	 *            子账户机构
	 */
	private static void modifyAccountBranch(DpaSubAccount subAccount, String subAccountBranch) {
		bizlog.method(" DpAccountMaintain.modifyAccountBranch begin >>>>>>>>>>>>>>>>");

		// 校验子账户机构是否与子户保存的子账户机构一致,不一致时修改子账户机构并调用会计事件登记
		if (!CommUtil.equals(subAccountBranch, subAccount.getSub_acct_branch())) {

			DpaSubAccount oldSubAccount = BizUtil.clone(DpaSubAccount.class, subAccount);
			subAccount.setSub_acct_branch(subAccountBranch);

			int i = 0;
			// 登记审计信息
			i = ApDataAuditApi.regLogOnUpdateBusiness(oldSubAccount, subAccount);

			// 维护子账户信息
			if (i > 0) {
				// 1.修改子账户机构
				DpaSubAccountDao.updateOne_odb1(subAccount);
			}

			if (CommUtil.compare(oldSubAccount.getAcct_bal(), BigDecimal.ZERO) != 0) {

				// 2.调用会计事件登记子账户机构
				// 2.1原子账户机构 - 存款本金 (红字)
				BigDecimal trxn_amt = oldSubAccount.getAcct_bal().negate();
				regAccountingEvent(oldSubAccount, trxn_amt);

				// 2.2新子账户机构 - 存款本金
				regAccountingEvent(subAccount, subAccount.getAcct_bal());
			}
		}

		bizlog.method(" DpAccountMaintain.modifyAccountBranch end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年8月16日-上午10:16:46</li>
	 *         <li>功能说明：登记会计事件</li>
	 *         </p>
	 * @param subAcct
	 *            子户信息
	 * @param acctBal
	 *            交易金额
	 */
	private static void regAccountingEvent(DpaSubAccount subAcct, BigDecimal trxn_amt) {

		ApAccountingEventIn cplTaEventIn = BizUtil.getInstance(ApAccountingEventIn.class);

		cplTaEventIn.setAccounting_alias(subAcct.getAccounting_alias());
		cplTaEventIn.setAccounting_subject(E_ACCOUNTINGSUBJECT.DEPOSIT);
		cplTaEventIn.setAcct_branch(subAcct.getSub_acct_branch());
		cplTaEventIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
		cplTaEventIn.setDouble_entry_ind(E_YESORNO.YES);
		cplTaEventIn.setTrxn_ccy(subAcct.getCcy_code());
		cplTaEventIn.setAcct_no(subAcct.getAcct_no());
		cplTaEventIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplTaEventIn.setTrxn_amt(trxn_amt);
		cplTaEventIn.setBal_attributes(E_BALPROPERTY.DEPOSIT.getValue());
		cplTaEventIn.setOpp_acct_no(subAcct.getAcct_no());
		// TODO: cplTaEventIn.setOpp_sub_acct_seq(subAcct.getSub_acct_seq());

		cplTaEventIn.setProd_id(subAcct.getProd_id());
		cplTaEventIn.setSummary_code(ApSystemParmApi.getSummaryCode("ACCOUNTING_ALIAS_MODIFY"));
		cplTaEventIn.setSummary_name(ApSummaryApi.getText(ApSystemParmApi.getSummaryCode("ACCOUNTING_ALIAS_MODIFY")));

		ApAccountApi.regAccountingEvent(cplTaEventIn);
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年2月17日-上午9:46:31</li>
	 *         <li>功能说明：账户信息必须字段检查</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static String IoDpAcountSubMaintain(String subAcctNo, String acctNo, String subAcctSeq, String startInstDate, String termCode) {

		String subAcNo = subAcctNo;
		if (CommUtil.isNull(subAcctNo)) {

			DpaAccountRelate cplDpaAccountRelate = DpaAccountRelateDao.selectOne_odb1(acctNo, subAcctSeq, true);

			subAcNo = cplDpaAccountRelate.getSub_acct_no();
		}

		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctNo, subAcNo, true);

		if (CommUtil.isNull(termCode)) {
			termCode = subAccount.getTerm_code();
		}

		String dueDate = BizUtil.calcDateByCycle(startInstDate, termCode);

		// 可能需要遇节假日延期
		dueDate = DpToolsApi.calcMatureDate(subAccount.getProd_id(), subAccount.getCcy_code(), dueDate, subAccount.getSub_acct_branch(), subAccount.getRenewal_method());

		subAccount.setInit_inst_start_date(startInstDate);
		subAccount.setInit_due_date(dueDate);
		subAccount.setDue_date(dueDate);
		subAccount.setNext_inst_date(BizUtil.dateAdd("day", startInstDate, 1));

		DpaSubAccountDao.updateOne_odb1(subAccount);

		DpaInterest dpainterest = DpaInterestDao.selectOne_odb1(acctNo, subAcNo, E_INSTKEYTYPE.NORMAL, false);

		if (CommUtil.isNotNull(dpainterest)) {
			dpainterest.setStart_inst_date(startInstDate);
			dpainterest.setEnd_inst_date(dueDate);
			DpaInterestDao.updateOne_odb1(dpainterest);
		}

		bizlog.debug("1111111111dueDate=[%s],sub_acct_no=%s,acct_no=%s,sub_acct_seq=%s,start_inst_date=%s,term_code=%s", dueDate, subAcNo, acctNo, subAcctSeq, startInstDate,
				termCode);
		return subAccount.getDue_date();

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2018年4月16日-下午1:30:04</li>
	 *         <li>功能说明：更新产品准入条件</li>
	 *         </p>
	 * @param subAcct
	 */
	public static void modifySubProdBusiCond(DpaSubAccount subAcct) {

		// 存放同业不做核算别名变更
		if (subAcct.getAsst_liab_ind() == E_ASSETORDEBT.ASSET) {
			return;
		}

		// 产品开户控制
		DpfOpen openInfo = DpProductFactoryApi.getProdOpenCtrl(subAcct.getProd_id(), subAcct.getCcy_code());

		if (subAcct.getProd_allow_use_ind() == E_YESORNO.YES) {
			return;
		}

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 加载账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), true)));

		// 货币数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(subAcct.getCust_no(), subAcct.getCust_type());

		// 是否匹配产品准入条件标志
		// TODO: boolean isMatchProdUseBusi =
		// CommUtil.isNull(openInfo.getBusi_cond()) ? true :
		// ApRuleApi.mapping(openInfo.getBusi_cond());
		boolean isMatchProdUseBusi = true;

		// 更新账户层标志就可以了， 不要去做账务处理，账务处理容易导致维护超时，如有账务处理的必要可以开定时任务处理
		if (isMatchProdUseBusi) {

			if (subAcct.getProd_allow_use_ind() == E_YESORNO.NO) {

				DpaSubAccount oldSubAcct = BizUtil.clone(DpaSubAccount.class, subAcct);

				subAcct.setProd_busicond_update_date(BizUtil.getTrxRunEnvs().getTrxn_date());
				subAcct.setProd_allow_use_ind(E_YESORNO.YES);

				DpaSubAccountDao.updateOne_odb1(subAcct);

				ApDataAuditApi.regLogOnUpdateBusiness(oldSubAcct, subAcct);
			}
		}
		else {

			if (subAcct.getProd_allow_use_ind() == E_YESORNO.YES) {

				DpaSubAccount oldSubAcct = BizUtil.clone(DpaSubAccount.class, subAcct);

				subAcct.setProd_busicond_update_date(BizUtil.getTrxRunEnvs().getTrxn_date());
				subAcct.setProd_allow_use_ind(E_YESORNO.NO);

				DpaSubAccountDao.updateOne_odb1(subAcct);

				ApDataAuditApi.regLogOnUpdateBusiness(oldSubAcct, subAcct);
			}
		}

	}

	/**
	 * @Author hongbiao
	 *         <p>
	 *         <li>2018年7月10日-下午2:12:57</li>
	 *         <li>功能说明：登记账户子户对照表</li>
	 *         </p>
	 * @param cplIn
	 *            登记账户子户对照表输入接口
	 */
	public static void regAccountMapping(IoDpRegAccountMappingIn cplIn) {
		bizlog.method(" DpAccountMaintain.regAccountMapping begin >>>>>>>>>>>>>>>>");

		// 查询客户号下第一个账户信息
		DpaAccountRelate accountRelate = DpPublicCheck.getDefaultAccount(cplIn.getCust_no());

		if (accountRelate == null) {
			return;
		}

		// 客户号不能为空
		BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

		// 产品编号不能为空
		BizUtil.fieldNotNull(cplIn.getProd_id(), SysDict.A.prod_id.getId(), SysDict.A.prod_id.getLongName());

		// 货币代码不能为空
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 子账号不能为空
		BizUtil.fieldNotNull(cplIn.getSub_acct_no(), DpSysDict.A.sub_acct_no.getId(), DpSysDict.A.sub_acct_no.getLongName());

		// 账户业务类型不能为空
		BizUtil.fieldNotNull(cplIn.getAcct_busi_type(), DpSysDict.A.acct_busi_type.getId(), DpSysDict.A.acct_busi_type.getLongName());

		// 带锁再读一遍，防止并发
		DpaAccount account = DpaAccountDao.selectOneWithLock_odb1(accountRelate.getAcct_no(), true);

		// 实例化账户子户对照表
		DpaAccountRelate relateInfo = BizUtil.getInstance(DpaAccountRelate.class);

		relateInfo.setAcct_no(account.getAcct_no());
		relateInfo.setSub_acct_seq(account.getUnused_sub_acct_seq());
		relateInfo.setAcct_type(account.getAcct_type());
		relateInfo.setCust_no(cplIn.getCust_no());
		relateInfo.setSub_acct_no(cplIn.getSub_acct_no());
		relateInfo.setCcy_code(cplIn.getCcy_code());
		relateInfo.setProd_id(cplIn.getProd_id());
		relateInfo.setDd_td_ind(E_DEMANDORTIME.TIME); // 默认为定期，避免活期子户定位出问题
		relateInfo.setAcct_status(E_ACCTSTATUS.NORMAL);
		relateInfo.setDefault_ind(E_YESORNO.NO);
		relateInfo.setAcct_busi_type(cplIn.getAcct_busi_type());

		// 登记账户子户对照信息
		DpaAccountRelateDao.insert(relateInfo);

		String nextSubSeq = String.valueOf(Integer.parseInt(account.getUnused_sub_acct_seq()) + 1);

		// 小于原来的长度则在左边补零
		if (nextSubSeq.length() < account.getUnused_sub_acct_seq().length()) {
			nextSubSeq = CommUtil.lpad(nextSubSeq, account.getUnused_sub_acct_seq().length(), "0");
		}

		account.setUnused_sub_acct_seq(nextSubSeq);

		// 更新账户信息
		DpaAccountDao.updateOne_odb1(account);

		bizlog.method(" DpAccountMaintain.regAccountMapping end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author DXY
	 *         <p>
	 *         <li>2019年5月7日-下午2:12:57</li>
	 *         <li>功能说明：账号签名信息的维护（新增、修改、删除）</li>
	 *         </p>
	 * @param
	 */
	public static void acctSignMain(DpAcctSignInfoMaintainIn cplIn) {
		bizlog.method(" SrvDpAcctMaintain.accountSignMaintain begin >>>>>>>>>>>>>>>>");

		// 操作标志不能为空
		BizUtil.fieldNotNull(cplIn.getOperater_ind(), SysDict.A.operater_ind.getId(), SysDict.A.operater_ind.getLongName());

		// 账号不能为空
		BizUtil.fieldNotNull(cplIn.getList01().getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		if (cplIn.getOperater_ind() == E_DATAOPERATE.ADD) {
			// 检查账号
			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getList01().getAcct_no(), null, false);

			if (CommUtil.isNull(acctInfo)) {
				throw DpErr.Dp.E0004(cplIn.getList01().getAcct_no());
			}

			// 检查客户号
			if (!CommUtil.equals(cplIn.getList01().getCust_no(), acctInfo.getCust_no())) {

				DpbJointAccount jointAcct = DpbJointAccountDao.selectOne_odb1(cplIn.getList01().getAcct_no(), cplIn.getList01().getCust_no(), false);

				if (CommUtil.isNull(jointAcct)) {
					throw APPUB.E0024(OdbFactory.getTable(DpbJointAccount.class).getLongname(), SysDict.A.acct_no.getLongName(), cplIn.getList01().getAcct_no(),
							SysDict.A.cust_no.getLongName(), cplIn.getList01().getCust_no());
				}
			}

			long serial_no = SqlDpAcctQueryDao.selAcctSignMaxSerno(cplIn.getList01().getAcct_no(), true);
			DpbAccountSignature dpbAcctSign = BizUtil.getInstance(DpbAccountSignature.class);

			dpbAcctSign.setAcct_no(cplIn.getList01().getAcct_no());
			serial_no++;
			dpbAcctSign.setSerial_no(serial_no);
			dpbAcctSign.setCust_no(cplIn.getList01().getCust_no());
			dpbAcctSign.setSign_desc(cplIn.getList01().getSign_desc());
			dpbAcctSign.setSign_info(cplIn.getList01().getSign_info());

			DpbAccountSignatureDao.insert(dpbAcctSign);
		}

		if (cplIn.getOperater_ind() == E_DATAOPERATE.MODIFY) {

			DpbAccountSignature dpbAcctSignOld = DpbAccountSignatureDao.selectOne_odb1(cplIn.getList01().getAcct_no(), cplIn.getList01().getSerial_no(), true);
			dpbAcctSignOld.setCust_no(cplIn.getList01().getCust_no());
			dpbAcctSignOld.setSign_desc(cplIn.getList01().getSign_desc());
			dpbAcctSignOld.setSign_info(cplIn.getList01().getSign_info());

			DpbAccountSignatureDao.updateOne_odb1(dpbAcctSignOld);

		}
		if (cplIn.getOperater_ind() == E_DATAOPERATE.DELETE) {

			DpbAccountSignatureDao.deleteOne_odb1(cplIn.getList01().getAcct_no(), cplIn.getList01().getSerial_no());

		}

		bizlog.method(" DpAccountMaintain.accountSignMaintain end >>>>>>>>>>>>>>>>");

	}
}
