package cn.sunline.icore.dp.serv.iobus;

import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.core.parameter.MsOrg;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ACCOUNTBUSINESSSOURCE;
import cn.sunline.icore.dp.serv.account.draw.DpDemandDraw;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBill;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustBaseInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.iobus.ds.servicetype.SrvIoDsFund;
import cn.sunline.icore.iobus.ds.type.ComIoDsFund.IoDsPricalChangeProcessInput;
import cn.sunline.icore.iobus.servicetype.ap.SrvIoApPushMessage;
import cn.sunline.icore.iobus.type.ap.ComIoApPushMessage.ApMessageDefineContents;
import cn.sunline.icore.iobus.type.ap.ComIoApPushMessage.ApMessageParm;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.us.iobus.servicetype.SrvIoUsUser;
import cn.sunline.icore.us.iobus.type.ComIoUsUser;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpOtherIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOtherIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：密码设置</li>
	 *         </p>
	 * @param sAcctNo
	 *            账号
	 * @param passWord
	 *            密码
	 */
	public static void setPassword(String sAcctNo, String sPassWord) {

		// 设置密码输入复合类型实例化
		// CmSetPasswordIn cmPwdIn = BizUtil.getInstance(CmSetPasswordIn.class);

		// cmPwdIn.setPassword(sPassWord);// 交易密码
		// cmPwdIn.setPwd_owner(sAcctNo);// 密码属主
		// cmPwdIn.setPwd_owner_type(E_PWDOWNERTYPE.ACCT);// 密码属主类型

		// 调用公共设置密码接口
		// SysUtil.getRemoteInstance(SrvIoCmPassword.class).setPassword(cmPwdIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：密码校验</li>
	 *         </p>
	 * @param sAcctNo
	 *            账号
	 * @param passWord
	 *            密码
	 */
	public static void verifyPassword(String sAcctNo, String sPassWord) {

		// 验密输入复合类型实例化
		// CmVerifyPasswordIn cmPwdIn =
		// BizUtil.getInstance(CmVerifyPasswordIn.class);

		// cmPwdIn.setPassword(sPassWord); // 交易密码
		// cmPwdIn.setPwd_owner(sAcctNo);// 密码属主
		// cmPwdIn.setPwd_owner_type(E_PWDOWNERTYPE.ACCT);// 密码属主类型

		// 调用公共验密服务或调用密码平台验密
		// SysUtil.getRemoteInstance(SrvIoCmPassword.class).verifyPassword(cmPwdIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：归还贷款</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	public static void loanRepay(DpaSubAccount subAcct) {

		try {
			// 还款处理
			// LnLoanRepayByAccountIn loanRepayByAccountIn =
			// BizUtil.getInstance(LnLoanRepayByAccountIn.class);

			// loanRepayByAccountIn.setRpym_account(subAcct.getAcct_no());
			// loanRepayByAccountIn.setRpym_account_ccy(subAcct.getCcy_code());

			// SysUtil.getRemoteInstance(SrvIoLnLoanRepayment.class).lnRepayByAccount(loanRepayByAccountIn);

		}
		catch (Exception e) {
			// 还款失败，回滚事务
			// DBUtil.rollBack();
		}

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：归还贷款</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	public static boolean isLoanRepayAcct(DpaSubAccount subAcct) {

		// 检查是否还款账号
		// IoLnCheckAccountIn checkAccountIn =
		// BizUtil.getInstance(IoLnCheckAccountIn.class);

		// checkAccountIn.setAcct_no(subAcct.getAcct_no());
		// checkAccountIn.setCcy_code(subAcct.getCcy_code());
		// checkAccountIn.setDd_td_ind(subAcct.getDd_td_ind());
		// checkAccountIn.setSub_acct_seq(subAcct.getSub_acct_seq());

		// return
		// SysUtil.getRemoteInstance(SrvIoLnLoanQuery.class).queryCheckAccount(checkAccountIn);

		return false;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年9月3日-下午3:56:50</li>
	 *         <li>功能说明：本金变动通知决策中心</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	public static void princChangeNotice(DpsBill billInfo, DpaSubAccount subAcct) {

		// 余额变动发送决策中心开关
		if (!ApSystemParmApi.getOFF_ON("DP_BAL_CHANGE_PUSH_DS_SWITCH")) {
			return;
		}

		// 不是会计记账记录，不通知决策中心
		if (billInfo.getTally_record_ind() == E_YESORNO.NO) {
			return;
		}

		// 产品信息
		DpfBase prodBase = DpProductFactoryApi.getProdBaseInfo(billInfo.getProd_id());

		// 暂时只同步存款数据
		if (prodBase.getAcct_busi_source() != E_ACCOUNTBUSINESSSOURCE.DEPOSIT) {
			return;
		}

		IoDsPricalChangeProcessInput cplDsInput = BizUtil.getInstance(IoDsPricalChangeProcessInput.class);

		cplDsInput.setAcct_bal(billInfo.getBal_after_trxn());
		cplDsInput.setAcct_no(billInfo.getAcct_no());
		cplDsInput.setCcy_code(billInfo.getTrxn_ccy());
		cplDsInput.setChannel_id(billInfo.getTrxn_channel());
		cplDsInput.setCust_no(billInfo.getCust_no());
		cplDsInput.setDebit_credit(billInfo.getDebit_credit());
		cplDsInput.setExternal_scene_code(billInfo.getExternal_scene_code());
		cplDsInput.setInfo_version(billInfo.getSerial_no());
		cplDsInput.setPrinciple_change_amt(billInfo.getTrxn_amt().abs());
		cplDsInput.setProd_id(billInfo.getProd_id());
		cplDsInput.setReversal_ind(CommUtil.nvl(billInfo.getReversal_ind(), E_YESORNO.NO));
		cplDsInput.setSub_acct_no(billInfo.getSub_acct_no());
		cplDsInput.setSummary_code(billInfo.getSummary_code());
		cplDsInput.setTrxn_event_id(billInfo.getTrxn_event_id());
		cplDsInput.setTrxn_date(billInfo.getTrxn_date());
		cplDsInput.setTrxn_seq(billInfo.getTrxn_seq());
		cplDsInput.setOpen_acct_date(subAcct.getOpen_acct_date());
		if (CommUtil.isNull(BizUtil.getTrxRunEnvs().getReversal_ind())) {
			cplDsInput.setReversal_ind(E_YESORNO.NO);

		}
		else {
			cplDsInput.setReversal_ind(BizUtil.getTrxRunEnvs().getReversal_ind());

		}
		if (CommUtil.isNull(billInfo.getTrxn_event_id())) {

			if (subAcct.getDd_td_ind() == E_DEMANDORTIME.DEMAND) {
				cplDsInput.setTrxn_event_id(billInfo.getDebit_credit() == E_DEBITCREDIT.CREDIT ? E_DEPTTRXNEVENT.DP_SAVE.getValue() : E_DEPTTRXNEVENT.DP_DRAW.getValue());
			}
			else {
				cplDsInput.setTrxn_event_id(billInfo.getDebit_credit() == E_DEBITCREDIT.CREDIT ? E_DEPTTRXNEVENT.DP_TIME_SAVE.getValue() : E_DEPTTRXNEVENT.DP_TIME_DRAW.getValue());
			}
		}
		// 捕捉异常： 推送决策中心的服务要配置成异步服务确认模式
		try {
			SysUtil.getRemoteInstance(SrvIoDsFund.class).noticeDpPrincChange(cplDsInput);
		}
		catch (Exception e) {
			;
		}

	}

	/**
	 * @Author huangchunjiang
	 *         <p>
	 *         <li>2019年9月9日-下午2:21:50</li>
	 *         <li>功能说明：本金变动推送消息中心</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 */
	public static void sendMessageChange(DpsBill billInfo, DpaSubAccount subAcct) {

		// 本金变动推送消息开关
		if (!ApSystemParmApi.getOFF_ON("DP_BAL_CHANGE_PUSH_MESSAGE_SWITCH")) {
			return;
		}

		// 消息推送,消息中心没给模板,提供后在具体修改
		String orgId = MsOrg.getReferenceOrgId(DpDemandDraw.class);
		String noteId = "";
		String category = "";
		String filePath = "";
		String fileFormatId = "";
		String startDateTime = "";
		String channel = "64";// 等模板修改
		String language = "2";// 等模板修改
		String tmplId = "account_money_in";// 等模板修改
		String priority = "";
		String sync = "";

		ApMessageDefineContents template = BizUtil.getInstance(ApMessageDefineContents.class);

		// params
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("money", billInfo.getTrxn_amt().toString());
		map.put("transaction_time", BizUtil.getTrxRunEnvs().getTimestamp());
		map.put("account_no", subAcct.getAcct_no());
		map.put("summary_code", billInfo.getSummary_code());
		map.put("summary_name", billInfo.getSummary_name());

		ApMessageParm ApMessageParm = BizUtil.getInstance(ApMessageParm.class);

		ApMessageParm.setData(map);

		String custNo = subAcct.getCust_no();
		boolean existsCustInfo = false;

		// 读取数据缓冲区的客户信息
		Object custBuff = ApBufferApi.getBuffer().get(ApConst.CUST_DATA_MART);

		// 已经存在客户数据集直接退出
		if (CommUtil.isNotNull(custBuff)) {

			Map<String, Object> custMap = CommUtil.toMap(custBuff);

			if (custMap.containsKey(SysDict.A.cust_no.getId()) && CommUtil.equals(custMap.get(SysDict.A.cust_no.getId()).toString(), custNo)) {

				existsCustInfo = true;
			}
		}

		ComIoUsUser.qryPushDemandInfoIn qryPushDemandInfoIn = SysUtil.getInstance(ComIoUsUser.qryPushDemandInfoIn.class);
		qryPushDemandInfoIn.setCust_no(custNo);
		ComIoUsUser.qryPushDemandInfoOut qryPushDemandInfoOut = SysUtil.getRemoteInstance(SrvIoUsUser.class).qryPushDemandInfo(qryPushDemandInfoIn);

		String userId = qryPushDemandInfoOut.getUser_no();
		String to = "";

		if (existsCustInfo) {

			Map<String, Object> custMap = CommUtil.toMap(custBuff);

			to = CommUtil.isNotNull(custMap.get(SysDict.A.e_mail.getId())) ? custMap.get(SysDict.A.e_mail.getId()).toString() : "";
		}
		else {

			DpCustBaseInfo custInfo = DpCustomerIobus.getCustBaseInfo(subAcct.getCust_no(), subAcct.getCust_type());

			to = CommUtil.isNotNull(custInfo.getCustInfo().get(SysDict.A.e_mail.getId())) ? custInfo.getCustInfo().get(SysDict.A.e_mail.getId()).toString() : "";
		}

		SysUtil.getRemoteInstance(SrvIoApPushMessage.class).sendMessage(noteId, category, orgId, userId, custNo, to, filePath, fileFormatId, startDateTime, channel, language,
				tmplId, ApMessageParm, priority, sync, template);

	}

	/**
	 * 开户消息推送
	 * 
	 * @param subAcct
	 */
	public static void sendMessageChange(DpaSubAccount subAcct) {

		// 本金变动推送消息开关
		if (!ApSystemParmApi.getOFF_ON("DP_OPEND_ACCOUNT_PUSH_MESSAGE_SWITCH")) {
			return;
		}

		// TODO: 客户化开发
	}

}