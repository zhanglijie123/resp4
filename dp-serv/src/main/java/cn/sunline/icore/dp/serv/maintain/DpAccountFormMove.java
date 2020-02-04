package cn.sunline.icore.dp.serv.maintain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApCurrencyApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.sms.ApSms;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfFormMove;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ACCTFORM;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_AUTOCLOSE;
import cn.sunline.icore.dp.serv.account.close.DpCloseSubAccountCheck;
import cn.sunline.icore.dp.serv.common.DpNotice;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbFormMove;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbFormMoveDao;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseSubAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpUnclaimedAcctToNormalIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpUnclaimedAcctToNormalOut;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.exception.LTTSException;
import cn.sunline.ltts.core.api.exception.LttsBusinessException;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：账户形态转移
 * </p>
 * 
 * @Author yangdl
 *         <p>
 *         <li>2017年6月26日-下午1:50:23</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年6月26日-yangdl：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAccountFormMove {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAccountFormMove.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年6月27日-下午1:27:12</li>
	 *         <li>功能说明：账户形态转移(不适用不动户激活)</li>
	 *         </p>
	 * @param subAcct
	 *            子账户
	 * @param acctForm
	 *            需要转换的形态
	 * @reurn 形态转移成功失败标志
	 */
	public static boolean acctountFormMove(DpaSubAccount subAcct, E_ACCTFORM acctForm) {

		// 1.1 销户的或者形态相同直接退出， 要报错在调用此方法的外层服务处理
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE || subAcct.getAcct_form() == acctForm) {
			bizlog.debug("Sub-account status[%s],acct form [%s]", subAcct.getSub_acct_status(), acctForm);
			return false;
		}

		// 1.2 不动户激活不适用此方法
		if (acctForm == E_ACCTFORM.NORMAL) {
			bizlog.error("Invoking an inappropriate method");
			return false;
		}

		// 不能失联户转不动户
		if (acctForm == E_ACCTFORM.DORMANT && subAcct.getAcct_form() == E_ACCTFORM.SUSPENSION) {
			return false;
		}

		// 不能失联户或不动户转非活跃户
		if (acctForm == E_ACCTFORM.INACTIVE && CommUtil.in(subAcct.getAcct_form(), E_ACCTFORM.DORMANT, E_ACCTFORM.SUSPENSION)) {
			return false;
		}

		// 1.3 形态转移参数
		DpfFormMove formDefine = DpProductFactoryApi.getProdFormMove(subAcct.getProd_id(), subAcct.getCcy_code(), false);

		// 无配置或不启用则直接退出
		if (formDefine == null || (formDefine.getMonths_to_inactive() == 0 && formDefine.getMonths_to_dormant() == 0 && formDefine.getMonths_to_unclaimed() == 0)) {
			return false;
		}

		// 1.3 添加规则数据缓存区
		addBuffer(subAcct);

		// 1.4 账户形态还是正常户， 说明本轮还没有执行过形态转移规则检查
		if (subAcct.getAcct_form() == E_ACCTFORM.NORMAL && CommUtil.isNotNull(formDefine.getBusi_cond())) {

			if (!ApRuleApi.mapping(formDefine.getBusi_cond())) {
				bizlog.info("Sub-Account Number[%s] does not meet the condition form transfer condition", subAcct.getSub_acct_no());
				return false;
			}
		}

		// 2. 登记账户形态转移登记薄
		regFormMoveInfo(subAcct, acctForm);

		// 3. 更新账户形态
		subAcct.setAcct_form(acctForm);
		subAcct.setForm_update_date(BizUtil.getTrxRunEnvs().getTrxn_date());

		DpaSubAccountDao.updateOne_odb1(subAcct);

		// 4. 形态变更后附加处理
		// 4.1 转为非活跃户，无需特殊处理，触发消息通知在方法外面处理
		if (acctForm == E_ACCTFORM.INACTIVE) {
			;
		}
		// 4.2 转为不动户，也无需特殊处理，触发消息通知在方法外面处理
		else if (acctForm == E_ACCTFORM.DORMANT) {
			;
		}
		// 4.3 转为失联户, 可能需要变更核算别名，触发消息通知在方法外面处理
		else if (acctForm == E_ACCTFORM.SUSPENSION) {

			DpaSubAccount beforeSubAcct = BizUtil.clone(DpaSubAccount.class, subAcct);

			// 方法里面自己有判断核算别名是否有变更
			DpAccountingAlaisMaitain.modifyAccountingalias(subAcct, beforeSubAcct);
		}
		else {
			// 还未支持的新形态则报错
			throw APPUB.E0026(DpBaseDict.A.acct_form.getLongName(), acctForm.getValue());
		}

		// 最后返回成功
		return true;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年5月31日-下午1:27:12</li>
	 *         <li>功能说明：不动户激活</li>
	 *         </p>
	 * @param subAcct
	 *            子账户
	 */
	public static void activationDormantAccount(DpaSubAccount subAcct) {

		bizlog.method(" DpAccountFormMove.activationDormantAccount begin ");

		// 已销户或正常户直接退出, 报错在调用此方法的外层服务处理
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE || subAcct.getAcct_form() == E_ACCTFORM.NORMAL) {
			return;
		}

		// 原账户形态
		E_ACCTFORM oldAcctForm = subAcct.getAcct_form();

		// 1. 登记账户形态转移登记薄
		regFormMoveInfo(subAcct, E_ACCTFORM.NORMAL);

		// 2. 更新账户形态
		subAcct.setAcct_form(E_ACCTFORM.NORMAL);
		subAcct.setForm_update_date(BizUtil.getTrxRunEnvs().getTrxn_date());

		DpaSubAccountDao.updateOne_odb1(subAcct);

		// 3. 如果原状态是失联户，则可能需要形态变更
		if (oldAcctForm == E_ACCTFORM.SUSPENSION) {

			DpaSubAccount beforeSubAcct = BizUtil.clone(DpaSubAccount.class, subAcct);

			// 方法里面自己有判断核算别名是否有变更
			DpAccountingAlaisMaitain.modifyAccountingalias(subAcct, beforeSubAcct);
		}

		bizlog.method(" DpAccountFormMove.activationDormantAccount end ");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年5月29日-下午3:31:51</li>
	 *         <li>功能说明：日终批量自动销户</li>
	 *         </p>
	 * @param subAcct
	 *            子户信息
	 * @return 成功失败标志
	 */
	public static boolean autoClose(DpaSubAccount subAcct) {

		// 已经销户则直接退出
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {
			return false;
		}

		// 形态转移参数
		DpfFormMove formDefine = DpProductFactoryApi.getProdFormMove(subAcct.getProd_id(), subAcct.getCcy_code(), false);

		// 无配置或不自动销户则直接退出
		if (formDefine == null || formDefine.getAuto_close_ind() == E_AUTOCLOSE.NO) {
			return false;
		}

		if (formDefine.getAuto_close_ind() == E_AUTOCLOSE.ZERO && !CommUtil.equals(subAcct.getAcct_bal(), BigDecimal.ZERO)) {
			return false;
		}

		// 自动销户最小月份数，以上次金额交易日期推算
		String autoCloseDate = BizUtil.dateAdd("mm", subAcct.getPrev_financial_trxn_date(), formDefine.getMonths_to_close().intValue());

		// 未达到自动销户日期则退出
		if (CommUtil.compare(autoCloseDate, BizUtil.getTrxRunEnvs().getTrxn_date()) > 0) {
			return false;
		}

		// 存在透支则直接跳出
		if (CommUtil.compare(subAcct.getAcct_bal(), BigDecimal.ZERO) < 0) {
			return false;
		}

		// 摘要代码及业务编码
		String summaryCode = ApSystemParmApi.getSummaryCode("CLOSE_SUB_ACCOUNT");
		String busiCode = ApSystemParmApi.getValue("DEPOSIT_FORM_AUTO_CLOSE");

		// 获取账户信息
		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), true);

		// 销户合法性检查接口
		DpCloseSubAccountIn cplCloseIn = BizUtil.getInstance(DpCloseSubAccountIn.class);

		cplCloseIn.setAcct_no(subAcct.getAcct_no());
		cplCloseIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
		cplCloseIn.setCcy_code(subAcct.getCcy_code());
		cplCloseIn.setProd_id(subAcct.getProd_id());
		cplCloseIn.setCheck_password_ind(E_YESORNO.NO);
		cplCloseIn.setClose_acct_reason(""); // TODO
		cplCloseIn.setOpp_acct_ccy(subAcct.getCcy_code());
		cplCloseIn.setOpp_acct_no("");
		cplCloseIn.setOpp_acct_route(E_ACCOUTANALY.BUSINESE);
		cplCloseIn.setSame_close_card_ind(E_YESORNO.NO);
		cplCloseIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		cplCloseIn.setSummary_code(summaryCode);

		try {

			// 销子户合法性检查
			DpCloseSubAccountCheck.checkMainMethod(cplCloseIn, acctInfo, subAcct);

			// 账户形态还是正常户， 说明本轮还没有执行过形态转移规则检查
			if (subAcct.getAcct_form() == E_ACCTFORM.NORMAL && CommUtil.isNotNull(formDefine.getBusi_cond())) {

				// 不符合形态转移条件, 前面销户check里面已经加载过规则数据缓存
				if (!ApRuleApi.mapping(formDefine.getBusi_cond())) {
					bizlog.error("Sub-Account Number[%s] does not meet the condition form transfer condition", subAcct.getSub_acct_no());
					return false;
				}
			}
		}
		catch (LttsBusinessException e) {

			bizlog.error("Sub-Account Number[%s] auto close business exception, error info: errorCode:[%s],errorMessage:[%s]", subAcct.getSub_acct_no(),
					((LttsBusinessException) e).getCode(), ((LttsBusinessException) e).getMessage());
			return false;
		}
		catch (Exception e) {

			bizlog.error("Sub-Account Number[%s] auto close other exception.", subAcct.getSub_acct_no());
			// 系统异常需要抛出错误
			throw e;
		}

		// 批量调联机: 智能存款也可能配置形态转移
		Map<String, Object> tranInput = new HashMap<String, Object>();

		tranInput.put(SysDict.A.acct_no.getId(), subAcct.getAcct_no());
		tranInput.put(SysDict.A.check_password_ind.getId(), E_YESORNO.NO);
		tranInput.put(SysDict.A.credit_acct_no.getId(), busiCode);
		tranInput.put(SysDict.A.credit_acct_branch.getId(), subAcct.getSub_acct_branch());
		tranInput.put(SysDict.A.credit_ccy_code.getId(), subAcct.getCcy_code());
		tranInput.put(SysDict.A.summary_code.getId(), summaryCode);

		try {

			if (subAcct.getDd_td_ind() == E_DEMANDORTIME.DEMAND) {

				tranInput.put(SysDict.A.ccy_code.getId(), subAcct.getCcy_code());
				tranInput.put(SysDict.A.prod_id.getId(), subAcct.getProd_id());
				tranInput.put(DpBaseDict.A.close_acct_reason.getId(), ""); // TODO
				tranInput.put(DpBaseDict.A.same_close_card_ind.getId(), E_YESORNO.NO);

				SysUtil.callFlowTran("4806", tranInput);
			}
			else {

				tranInput.put(SysDict.A.sub_acct_seq.getId(), subAcct.getSub_acct_seq());
				tranInput.put(SysDict.A.trxn_ccy.getId(), subAcct.getCcy_code());
				tranInput.put(SysDict.A.trxn_amt.getId(), subAcct.getAcct_bal());

				SysUtil.callFlowTran("4804", tranInput);
			}
		}
		catch (LttsBusinessException e) {

			// 抛出了异常先回滚事物
			DaoUtil.rollbackTransaction();

			bizlog.error("Sub-Account Number[%s] auto close business exception 2, error info: errorCode:[%s],errorMessage:[%s]", subAcct.getSub_acct_no(),
					((LttsBusinessException) e).getCode(), ((LttsBusinessException) e).getMessage());

			// 日终业务不报错，返回销户失败标志
			return false;
		}
		catch (Exception e) {

			// 抛出了异常先回滚事物
			DaoUtil.rollbackTransaction();

			bizlog.error("Sub-Account Number[%s] auto close other exception 2.", subAcct.getSub_acct_no());
			// 系统异常需要抛出错误
			throw (LTTSException) e;
		}

		// 最后返回成功标志
		return true;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年6月26日-下午4:44:45</li>
	 *         <li>功能说明：登记账户形态转移登记薄</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void regFormMoveInfo(DpaSubAccount subAcct, E_ACCTFORM acctForm) {

		DpbFormMove formMoveInfo = BizUtil.getInstance(DpbFormMove.class);

		formMoveInfo.setSub_acct_no(subAcct.getSub_acct_no()); // 子账号
		formMoveInfo.setOld_acct_form(subAcct.getAcct_form());
		formMoveInfo.setNew_acct_form(acctForm);
		formMoveInfo.setAcct_no(subAcct.getAcct_no()); // 账号
		formMoveInfo.setAcct_name(subAcct.getSub_acct_name()); // 账户名称
		formMoveInfo.setCust_no(subAcct.getCust_no()); // 客户号
		formMoveInfo.setCcy_code(subAcct.getCcy_code()); // 币种
		formMoveInfo.setProd_id(subAcct.getProd_id()); // 产品编号
		formMoveInfo.setPrev_financial_trxn_date(subAcct.getPrev_financial_trxn_date()); // 上次金融交易日期
		formMoveInfo.setAcct_bal(subAcct.getAcct_bal()); // 账户余额
		formMoveInfo.setTrxn_branch(BizUtil.getTrxRunEnvs().getTrxn_branch()); // 机构号
		formMoveInfo.setTrxn_teller(BizUtil.getTrxRunEnvs().getTrxn_teller()); // 柜员号
		formMoveInfo.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date()); // 交易日期
		formMoveInfo.setTrxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq()); // 交易流水
		formMoveInfo.setBusi_seq(BizUtil.getTrxRunEnvs().getBusi_seq()); // 业务流水

		DpbFormMoveDao.insert(formMoveInfo);
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年12月5日-上午11:15:06</li>
	 *         <li>功能说明：久悬户或不动户手工激活</li>
	 *         </p>
	 * @param cplIn
	 *            不动户手工激活输入
	 * @return 不动户手工激活输出
	 */
	public static DpUnclaimedAcctToNormalOut acctUnclaimedToNormal(DpUnclaimedAcctToNormalIn cplIn) {
		bizlog.method(" DpAccountFormMove.acctUnclaimedToNormal begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 定位相关子账号信息
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no());// 账号
		accessIn.setAcct_type(cplIn.getAcct_type());// 账号类型
		accessIn.setCcy_code(cplIn.getCcy_code());// 货币代码
		accessIn.setProd_id(cplIn.getProd_id());// 产品编号
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq()); // 特殊定期也可能有形态管理

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 带锁查询
		DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOneWithLock_odb1(accessOut.getAcct_no(), accessOut.getSub_acct_no(), true);

		// 已销户报错
		if (subAcctInfo.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_no());
		}

		// 激活处理
		activationDormantAccount(subAcctInfo);

		// 输出
		DpUnclaimedAcctToNormalOut cplOut = BizUtil.getInstance(DpUnclaimedAcctToNormalOut.class);

		cplOut.setAcct_name(accessOut.getAcct_name());
		cplOut.setAcct_no(accessOut.getAcct_no());
		cplOut.setAcct_type(accessOut.getAcct_type());
		cplOut.setSub_acct_seq(accessOut.getSub_acct_seq());
		cplOut.setCcy_code(accessOut.getCcy_code());
		cplOut.setProd_id(accessOut.getProd_id());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAccountFormMove.acctUnclaimedToNormal end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2018年8月30日-下午8:52:41</li>
	 *         <li>功能说明：账户形态变更通知</li>
	 *         </p>
	 * @param subAcct
	 *            形态变更后账户信息
	 */
	public static void notice(DpaSubAccount subAcct) {
		bizlog.method(" DpAccountFormMove.notice begin >>>>>>>>>>>>>>>>");

		// 形态没有变更或不动户激活无需触发通知
		if (subAcct.getAcct_form() == E_ACCTFORM.NORMAL) {
			return;
		}

		// 当晚形态转移触发的自动销户准许进入通知流程，其他情况下已销户账户不再消息通知
		if (subAcct.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE && CommUtil.compare(subAcct.getClose_acct_seq(), BizUtil.getTrxRunEnvs().getTrxn_seq()) != 0) {
			return;
		}

		// 产品形态参数
		DpfFormMove formMove = DpProductFactoryApi.getProdFormMove(subAcct.getProd_id(), subAcct.getCcy_code(), false);

		// 未定义形态参数，则对应子户不做形态转移处理
		if (formMove == null) {
			return;
		}

		// 配置的形态转移参数都不启用，则也不需触发通知
		if (CommUtil.compare(formMove.getMonths_to_inactive(), 0L) == 0 && CommUtil.compare(formMove.getMonths_to_dormant(), 0L) == 0
				&& CommUtil.compare(formMove.getMonths_to_unclaimed(), 0L) == 0 && formMove.getAuto_close_ind() == E_AUTOCLOSE.NO) {
			return;
		}

		// 首次触发通知日期
		String firstNoticeDate = "";
		// 交易日期
		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 非活跃户月份数有设置，则以非活跃户日为首次触发通知日
		if (CommUtil.compare(formMove.getMonths_to_inactive(), 0L) > 0) {
			firstNoticeDate = BizUtil.dateAdd("mm", subAcct.getPrev_financial_trxn_date(), formMove.getMonths_to_inactive().intValue());
		}
		// 其次以转不动户为首次触发通知日
		else if (CommUtil.compare(formMove.getMonths_to_dormant(), 0L) > 0) {
			firstNoticeDate = BizUtil.dateAdd("mm", subAcct.getPrev_financial_trxn_date(), formMove.getMonths_to_dormant().intValue());
		}
		// 其次以转失联户为首次触发通知日
		else if (CommUtil.compare(formMove.getMonths_to_unclaimed(), 0L) > 0) {
			firstNoticeDate = BizUtil.dateAdd("mm", subAcct.getPrev_financial_trxn_date(), formMove.getMonths_to_unclaimed().intValue());
		}
		// 最后以触发销户日为首次触发通知日
		else if (formMove.getAuto_close_ind() != E_AUTOCLOSE.NO && CommUtil.compare(formMove.getMonths_to_close(), 0L) > 0) {
			firstNoticeDate = BizUtil.dateAdd("mm", subAcct.getPrev_financial_trxn_date(), formMove.getMonths_to_close().intValue());
		}

		// 未到首次触发通知日
		if (CommUtil.isNull(firstNoticeDate) || CommUtil.compare(firstNoticeDate, trxnDate) > 0) {
			return;
		}

		// TODO: 产品工厂暂时没有明确字段表示是否需要发送通知， 如果无告警周期和次数则认为不需要发送通知
		if (CommUtil.isNull(formMove.getWarning_cycle()) && (CommUtil.isNull(formMove.getWarning_times()) || CommUtil.compare(formMove.getWarning_times(), 0L) == 0)) {
			return;
		}

		// 通知日
		String noticeDate = firstNoticeDate;

		// 今天不是首次通知日, 并且告警次数不止1次，则计算下次告警日
		if (!CommUtil.equals(firstNoticeDate, trxnDate) && CommUtil.compare(formMove.getWarning_times(), 1L) != 0) {

			int diffTimes = BizUtil.dateDiffByCycle(formMove.getWarning_cycle(), firstNoticeDate, trxnDate) + 1;

			// 已超过告警次数，不再触发告警： 0表示无穷大
			if (CommUtil.compare(formMove.getWarning_times(), 0L) != 0 && diffTimes > formMove.getWarning_times().intValue()) {
				return;
			}

			noticeDate = BizUtil.calcDateByReference(firstNoticeDate, BizUtil.getTrxRunEnvs().getLast_date(), formMove.getWarning_cycle());
		}

		// 加载缓存，用于组织通知信息
		addBuffer(subAcct);

		// 今天是通知日则触发通知
		if (CommUtil.equals(noticeDate, trxnDate)) {

			// 短信通知
			smsNotice(subAcct);

			// 邮件通知
			mailNotice(subAcct);
		}

		bizlog.method(" DpAccountFormMove.notice end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年5月31日-上午9:42:19</li>
	 *         <li>功能说明：形态管理短信通知</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void smsNotice(DpaSubAccount subAcct) {

		// 短信模板号
		String smsTemplateNo = "";

		if (subAcct.getSub_acct_status() != E_SUBACCTSTATUS.CLOSE) {

			if (ApBusinessParmApi.exists("DP_INACTIVE_SMS_TEMPLATE_NO")) {
				smsTemplateNo = ApBusinessParmApi.getValue("DP_INACTIVE_SMS_TEMPLATE_NO");
			}
		}
		else {
			if (ApBusinessParmApi.exists("DP_AUTOCLOSE_SMS_TEMPLATE_NO")) {
				smsTemplateNo = ApBusinessParmApi.getValue("DP_AUTOCLOSE_SMS_TEMPLATE_NO");
			}
		}

		// 短信模板号不为空则发送短信
		if (CommUtil.isNotNull(smsTemplateNo)) {

			ApSms.sendSmsByTemplateNo(smsTemplateNo);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年5月31日-上午9:42:34</li>
	 *         <li>功能说明：形态管理邮件通知</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	private static void mailNotice(DpaSubAccount subAcct) {

		// 邮件模板号
		String mailTemplateNo = "";

		if (subAcct.getSub_acct_status() != E_SUBACCTSTATUS.CLOSE) {

			if (ApBusinessParmApi.exists("DP_INACTIVE_MAIL_TEMPLATE_NO")) {
				mailTemplateNo = ApBusinessParmApi.getValue("DP_INACTIVE_MAIL_TEMPLATE_NO");
			}
		}
		else {
			if (ApBusinessParmApi.exists("DP_AUTOCLOSE_MAIL_TEMPLATE_NO")) {
				mailTemplateNo = ApBusinessParmApi.getValue("DP_AUTOCLOSE_MAIL_TEMPLATE_NO");
			}
		}

		// 发送邮件
		if (CommUtil.isNotNull(mailTemplateNo)) {

			Map<String, Object> mailData = addMailData(subAcct);

			DpNotice.registerMailInfoByTemplateNo(mailTemplateNo, mailData, null);
		}
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2018年8月31日-上午9:43:19</li>
	 *         <li>功能说明：邮件数据</li>
	 *         </p>
	 * @param subAcct
	 */
	private static Map<String, Object> addMailData(DpaSubAccount subAccount) {
		bizlog.method(" DpAccountFormMove.addMailData begin >>>>>>>>>>>>>>>>");
		bizlog.method(" subAccount>>>>>[%s]", subAccount);
		// 声明邮件数据集
		Map<String, Object> mailData = new HashMap<String, Object>();

		// 子账户信息加入邮件数据集
		mailData.putAll(CommUtil.toMap(subAccount));

		// 客户信息加入邮件数据集
		mailData.putAll(CommUtil.toMap(ApBufferApi.getBuffer().get(ApConst.CUST_DATA_MART)));

		// 账户信息加入邮件数据集
		mailData.putAll(CommUtil.toMap(ApBufferApi.getBuffer().get(ApConst.ACCOUNT_DATA_MART)));

		// 获取交易卡号
		String cardNo = DpToolsApi.getCardNoByAcctNo(subAccount.getAcct_no());

		// 补充特殊字段
		mailData.put(SysDict.A.card_no.getId(), cardNo); // 卡号

		mailData.putAll(CommUtil.toMap(BizUtil.getTrxRunEnvs()));

		bizlog.method(" subAccount>>>>>[%s]", mailData);
		bizlog.method(" DpAccountFormMove.addMailData end <<<<<<<<<<<<<<<<");

		return mailData;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2018年8月30日-下午8:58:50</li>
	 *         <li>功能说明：加载缓存</li>
	 *         </p>
	 * @param subAcct
	 */
	private static void addBuffer(DpaSubAccount subAcct) {
		bizlog.method(" DpAccountFormMove.addBuffer begin >>>>>>>>>>>>>>>>");

		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), true);

		// 添加账户数据集
		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));

		// 币种数据集
		ApBufferApi.addData(ApConst.CURRENCY_DATA_MART, CommUtil.toMap(ApCurrencyApi.getItem(subAcct.getCcy_code())));

		// 添加客户数据集
		DpPublicCheck.addDataToCustBuffer(subAcct.getCust_no(), subAcct.getCust_type());

		// 加载子账户数据区
		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 加载卡数据区
		if (acctInfo.getCard_relationship_ind() == E_YESORNO.YES) {

			DpaCard cardInfo = DpaCardDao.selectFirst_odb2(subAcct.getAcct_no(), false);

			if (cardInfo != null) {
				ApBufferApi.addData(ApConst.CARD_DATA_MART, CommUtil.toMap(cardInfo));
			}
		}

		bizlog.method(" DpAccountFormMove.addBuffer end <<<<<<<<<<<<<<<<");
	}

}
