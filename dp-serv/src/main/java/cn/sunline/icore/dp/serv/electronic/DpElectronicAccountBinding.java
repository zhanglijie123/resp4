package cn.sunline.icore.dp.serv.electronic;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ACCTLEVEL;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpElectronicAccountDao;
import cn.sunline.icore.dp.serv.tables.TabDpTwoThreeTypeAccount.DpbAccountBinding;
import cn.sunline.icore.dp.serv.tables.TabDpTwoThreeTypeAccount.DpbAccountBindingDao;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpAccountBindingInfo;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpAccountBindingInfoQueryIn;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpAccountBindingInput;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpAccountCancelBindingInput;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：电子账户绑卡与解绑
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2019年11月28日-下午13:42:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpElectronicAccountBinding {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpElectronicAccountBinding.class);

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月26日-上午11:23:51</li>
	 *         <li>功能说明：电子账户绑定结算户</li>
	 *         </p>
	 * @param cplIn
	 *            绑定结算账户输入
	 */
	public static void bindingSettleAccount(DpAccountBindingInput cplIn) {
		bizlog.method(" DpElectronicAccountBinding.bindingSettleAccount begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 输入要素合法性检查
		DpaAccount acctInfo = checkBinding(cplIn);

		// 登记结算户绑定信息
		registerBindingInfo(cplIn, acctInfo);

		// 账户级别需要更新
		if (cplIn.getAcct_level() != null && cplIn.getAcct_level() != acctInfo.getAcct_level()) {

			DpaAccount oldAcctInfo = BizUtil.clone(DpaAccount.class, acctInfo);

			acctInfo.setAcct_level(cplIn.getAcct_level());

			DpaAccountDao.updateOne_odb1(acctInfo);

			// 登记审计日志
			ApDataAuditApi.regLogOnUpdateBusiness(oldAcctInfo, acctInfo);
		}

		bizlog.method(" DpElectronicAccountBinding.bindingSettleAccount end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月26日-上午11:23:51</li>
	 *         <li>功能说明：电子账户绑定结算户检查</li>
	 *         </p>
	 * @param cplIn
	 *            绑定结算账户输入
	 */
	private static DpaAccount checkBinding(DpAccountBindingInput cplIn) {

		// 检查必输项
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号
		BizUtil.fieldNotNull(cplIn.getOpp_acct_no(), DpDict.A.opp_acct_no.getId(), DpDict.A.opp_acct_no.getLongName());// 对方账号
		BizUtil.fieldNotNull(cplIn.getBinding_acct_kind(), DpDict.A.binding_acct_kind.getId(), DpDict.A.binding_acct_kind.getLongName());// 绑定账户性质
		BizUtil.fieldNotNull(cplIn.getOwn_bank_ind(), DpDict.A.own_bank_ind.getId(), DpDict.A.own_bank_ind.getLongName());// 本行标志

		// 不是本行结算账户要提供他行信息
		if (cplIn.getOwn_bank_ind() == E_YESORNO.NO) {
			BizUtil.fieldNotNull(cplIn.getOpp_bank_id(), DpDict.A.opp_bank_id.getId(), DpDict.A.opp_bank_id.getLongName());
		}

		// 带锁定位账户信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, true);

		if (E_ACCTSTATUS.CLOSE == acctInfo.getAcct_status()) {
			throw DpBase.E0008(cplIn.getAcct_no());
		}

		// 升级全功能账户不能在绑定结算账户时操作
		if (cplIn.getAcct_level() == E_ACCTLEVEL.FULL_FUNCTION && cplIn.getAcct_level() != acctInfo.getAcct_level()) {
			throw DpErr.Dp.E0505();
		}

		return acctInfo;
	}

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月26日-上午11:23:51</li>
	 *         <li>功能说明：登记电子账户绑定信息</li>
	 *         </p>
	 * @param cplIn
	 *            绑定结算账户输入
	 * @param acctInfo
	 *            账户信息
	 */
	private static void registerBindingInfo(DpAccountBindingInput cplIn, DpaAccount acctInfo) {

		// 先查询是否已经存在绑定信息
		DpbAccountBinding accountBinding = DpbAccountBindingDao.selectOne_odb1(acctInfo.getAcct_no(), cplIn.getOpp_acct_no(), false);

		// 已经存在有效绑定关系，不要重复操作
		if (accountBinding != null && accountBinding.getBusi_status() == E_STATUS.VALID) {
			throw DpErr.Dp.E0506();
		}

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 全新绑定
		if (accountBinding == null) {

			accountBinding = BizUtil.getInstance(DpbAccountBinding.class);

			accountBinding.setAcct_no(acctInfo.getAcct_no());
			accountBinding.setOpp_acct_no(cplIn.getOpp_acct_no());
			accountBinding.setOpp_acct_name(cplIn.getOpp_acct_name());
			accountBinding.setBinding_acct_kind(cplIn.getBinding_acct_kind());
			accountBinding.setOwn_bank_ind(cplIn.getOwn_bank_ind());
			accountBinding.setOpp_bank_id(cplIn.getOpp_bank_id());
			accountBinding.setOpp_bank_name(cplIn.getOpp_bank_name());
			accountBinding.setAcct_level(cplIn.getAcct_level());
			accountBinding.setBusi_status(E_STATUS.VALID);
			accountBinding.setActive_ind(cplIn.getActive_ind() == null ? E_YESORNO.YES : cplIn.getActive_ind());
			accountBinding.setSign_date(runEnvs.getTrxn_date());
			accountBinding.setSign_seq(runEnvs.getTrxn_seq());
			accountBinding.setSign_branch(runEnvs.getTrxn_branch());
			accountBinding.setSign_channel(runEnvs.getChannel_id());

			DpbAccountBindingDao.insert(accountBinding);
		}
		// 旧绑定重新启用
		else {

			// 待绑定的结算户性质前后不一致
			if (accountBinding.getBinding_acct_kind() != cplIn.getBinding_acct_kind()) {
				throw DpErr.Dp.E0507();
			}

			// 待绑定的结算户名称前后不一致
			if (CommUtil.isNotNull(accountBinding.getOpp_acct_name()) && CommUtil.isNotNull(cplIn.getOpp_acct_name())
					&& !CommUtil.equals(cplIn.getOpp_acct_name(), accountBinding.getOpp_acct_name())) {
				throw DpErr.Dp.E0508();
			}

			// 银行可能发生合并或改名，所以同一结算账号归属银行可能变化，有再次传入以传入为准
			if (cplIn.getOwn_bank_ind() == E_YESORNO.NO) {
				accountBinding.setOpp_bank_id(CommUtil.nvl(cplIn.getOpp_bank_id(), accountBinding.getOpp_bank_id()));
				accountBinding.setOpp_bank_name(CommUtil.nvl(cplIn.getOpp_bank_name(), accountBinding.getOpp_bank_name()));
			}

			accountBinding.setOpp_acct_name(CommUtil.nvl(cplIn.getOpp_acct_name(), accountBinding.getOpp_acct_name()));
			accountBinding.setAcct_level(cplIn.getAcct_level() == null ? accountBinding.getAcct_level() : cplIn.getAcct_level());
			accountBinding.setBusi_status(E_STATUS.VALID);
			accountBinding.setSign_date(runEnvs.getTrxn_date());
			accountBinding.setSign_seq(runEnvs.getTrxn_seq());
			accountBinding.setSign_branch(runEnvs.getTrxn_branch());
			accountBinding.setSign_channel(runEnvs.getChannel_id());
			accountBinding.setCancel_branch(null);
			accountBinding.setCancel_channel(null);
			accountBinding.setCancel_date(null);
			accountBinding.setCancel_seq(null);

			DpbAccountBindingDao.updateOne_odb1(accountBinding);
		}
	}

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月26日-上午11:24:31</li>
	 *         <li>功能说明：电子账户解绑结算户</li>
	 *         </p>
	 * @param cplIn
	 *            解绑结算户输入
	 */
	public static void cancelBindingSettleAccount(DpAccountCancelBindingInput cplIn) {
		bizlog.method(" DpElectronicAccountBinding.cancelBindingSettleAccount begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 检查必输项
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号
		BizUtil.fieldNotNull(cplIn.getOpp_acct_no(), DpDict.A.opp_acct_no.getId(), DpDict.A.opp_acct_no.getLongName());// 对方账号

		// 定位账户信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 查询绑定信息
		DpbAccountBinding accountBinding = DpbAccountBindingDao.selectOne_odb1(acctInfo.getAcct_no(), cplIn.getOpp_acct_no(), false);

		if (accountBinding == null) {
			throw APPUB.E0024(OdbFactory.getTable(DpbAccountBinding.class).getLongname(), SysDict.A.acct_no.getLongName(), acctInfo.getAcct_no(),
					SysDict.A.opp_acct_no.getLongName(), cplIn.getOpp_acct_no());
		}

		// 检查不用重复操作解绑
		if (accountBinding.getBusi_status() == E_STATUS.INVALID) {
			throw DpErr.Dp.E0509();
		}

		// 更新绑定信息
		accountBinding.setBusi_status(E_STATUS.INVALID);
		accountBinding.setCancel_date(runEnvs.getTrxn_date());
		accountBinding.setCancel_seq(runEnvs.getTrxn_seq());
		accountBinding.setCancel_branch(runEnvs.getTrxn_branch());
		accountBinding.setCancel_channel(runEnvs.getChannel_id());

		DpbAccountBindingDao.updateOne_odb1(accountBinding);

		bizlog.method(" DpElectronicAccountBinding.cancelBindingSettleAccount end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月26日-上午11:24:31</li>
	 *         <li>功能说明：电子账户绑定记录查询</li>
	 *         </p>
	 * @param cplIn
	 *            电子账户绑定记录查询输入
	 * @return 电子账户绑定记录查询输出
	 */
	public static Options<DpAccountBindingInfo> bindingQuery(DpAccountBindingInfoQueryIn cplIn) {
		bizlog.method(" DpElectronicAccountBinding.bindingQuery begin >>>>>>>>>>>>>>>>");

		// 检查必输项
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号

		// 账号信息查询
		DpaAccount acctInfo = DpToolsApi.accountInquery(cplIn.getAcct_no(), null);

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		Page<DpAccountBindingInfo> page = SqlDpElectronicAccountDao.selElectronicAccountBindingList(acctInfo.getAcct_no(), cplIn.getBinding_acct_kind(), cplIn.getOwn_bank_ind(),
				cplIn.getOpp_acct_no(), runEnvs.getBusi_org_id(), runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(page.getRecordCount());

		// 输出对象集合实例化
		Options<DpAccountBindingInfo> listOut = new DefaultOptions<DpAccountBindingInfo>();

		// 添加对象到集合
		listOut.addAll(page.getRecords());

		bizlog.method(" DpElectronicAccountBinding.bindingQuery end <<<<<<<<<<<<<<<<");

		// 返回集合对象
		return listOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月25日-上午11:24:31</li>
	 *         <li>功能说明：判断对手方是否绑定结算户</li>
	 *         <li>补充说明：跨行或跨系统转账时对方账号填真实他行账户</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param saveOrDraw
	 *            存入支取标志
	 * @param oppAcctNo
	 *            对方客户账
	 * @param oppAcctName
	 *            对方客户账户名
	 * @return 是否绑定结算户标志
	 */
	public static E_YESORNO isBindingSettleAccount(String acctNo, E_SAVEORWITHDRAWALIND saveOrDraw, String oppAcctNo, String oppAcctName) {
		bizlog.method(" DpElectronicAccountBinding.isBindingSettleAccount begin >>>>>>>>>>>>>>>>");

		// 查询绑卡信息
		DpbAccountBinding bindingInfo = DpbAccountBindingDao.selectOne_odb1(acctNo, oppAcctNo, false);

		// 如果为空或者已失效，则返回否
		if (bindingInfo == null || bindingInfo.getBusi_status() == E_STATUS.INVALID) {

			return E_YESORNO.NO;
		}

		// 如果是支取，则没有激活的视同未绑卡，要拒绝
		if (E_SAVEORWITHDRAWALIND.WITHDRAWAL == saveOrDraw) {

			return bindingInfo.getActive_ind();
		}

		// 接下来处理存入绑卡判断，默认为通过验证; 同名来账激活，因此不要考虑激活标志，只需考虑是否同名
		E_YESORNO checkResult = E_YESORNO.YES;

		// 账户名称不一致，认为未绑定
		if (CommUtil.isNotNull(oppAcctName) && CommUtil.isNotNull(bindingInfo.getOpp_acct_name()) && !CommUtil.equals(oppAcctName, bindingInfo.getOpp_acct_name())) {

			checkResult = E_YESORNO.NO;
		}

		String oppName = CommUtil.nvl(bindingInfo.getOpp_acct_name(), oppAcctName);

		// 有对方户名，则还检查是否与交易账号的户名是否一致
		if (checkResult == E_YESORNO.YES && CommUtil.isNotNull(oppName)) {

			DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(acctNo, true);

			if (!CommUtil.equals(acctInfo.getAcct_name(), oppName)) {

				checkResult = E_YESORNO.NO;
			}
		}

		// 返回判断结果
		return checkResult;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月25日-上午11:24:31</li>
	 *         <li>功能说明：同名户来账激活绑定关系</li>
	 *         <li>补充说明：只适用于同名来账激活，之前应通过绑卡判断</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param oppAcctNo
	 *            对方客户账
	 * @param oppAcctName
	 *            对方客户账户名
	 */
	public static void sameNameActiveBinding(String acctNo, String oppAcctNo, String oppAcctName) {
		bizlog.method(" DpElectronicAccountBinding.sameNameActiveBinding begin >>>>>>>>>>>>>>>>");

		// 查询绑卡信息
		DpbAccountBinding bindingInfo = DpbAccountBindingDao.selectOne_odb1(acctNo, oppAcctNo, false);

		// 如果为空或者已失效，则直接返回
		if (bindingInfo == null || bindingInfo.getBusi_status() == E_STATUS.INVALID) {
			return;
		}

		// 如果已经激活，也直接返回
		if (bindingInfo.getActive_ind() == E_YESORNO.YES) {
			return;
		}

		// 前面已经做过同名认证检查了，此处不再检查
		// 同名来账激活处理
		bindingInfo.setActive_ind(E_YESORNO.YES);

		// 有传入户名且原户名为空，则更新
		if (CommUtil.isNotNull(oppAcctName) && CommUtil.isNull(bindingInfo.getOpp_acct_name())) {
			bindingInfo.setOpp_acct_name(oppAcctName);
		}

		DpbAccountBindingDao.updateOne_odb1(bindingInfo);

		bizlog.method(" DpElectronicAccountBinding.sameNameActiveBinding end <<<<<<<<<<<<<<<<");
	}
}