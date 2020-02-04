package cn.sunline.icore.dp.serv.common;

import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAttributeApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApChannelApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpTechParmApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpTechPara.DppConditionFeature;
import cn.sunline.icore.dp.base.type.ComDpFrozeBase.DpFrozeObjectLimitStatus;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.iobus.DpInsideAccountIobus;
import cn.sunline.icore.dp.serv.iobus.DpOtherIobus;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustBaseInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ACCTBUSITYPE;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCOUTANALY;
import cn.sunline.icore.sys.type.EnumType.E_ACCTLIMITSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_CUSTOMERTYPE;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_OWNERLEVEL;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：负债业务常用检查方法
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月12日-下午4:42:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpPublicCheck {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPublicCheck.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月8日-下午1:16:29</li>
	 *         <li>功能说明：取款验密</li>
	 *         </p>
	 * @param acctInfo
	 *            账户信息
	 * @param cplIn
	 *            验密条件
	 */
	public static void checkPassWord(DpaAccount acctInfo, DpCheckPassWord cplIn) {

		bizlog.method(" DpPublicCheck.checkPassWord begin >>>>>>>>>>>>>>>>");

		// 第三方校验支取方式，卡账关系直接退出
		if (acctInfo.getCard_relationship_ind() == E_YESORNO.YES) {
			return;
		}

		String drawCond = acctInfo.getWithdrawal_cond(); // 支取条件
		String checkObject = acctInfo.getAcct_no(); // 校验主体对象

		// 获取支取条件定义信息
		DppConditionFeature condInfo = DpTechParmApi.getDrawCondInfo(drawCond);

		if (condInfo.getCheck_password_ind() == E_YESORNO.YES) {

			// 交易密码不能为空
			BizUtil.fieldNotNull(cplIn.getTrxn_password(), SysDict.A.trxn_password.getId(), SysDict.A.trxn_password.getLongName());

			// 校验密码
			DpOtherIobus.verifyPassword(checkObject, cplIn.getTrxn_password());
		}
		else if (condInfo.getCheck_doc_ind() == E_YESORNO.YES) {

			// 证件类型不能为空
			BizUtil.fieldNotNull(cplIn.getDoc_type(), SysDict.A.doc_type.getId(), SysDict.A.doc_type.getLongName());

			// 证件号码不能为空
			BizUtil.fieldNotNull(cplIn.getDoc_no(), SysDict.A.doc_no.getId(), SysDict.A.doc_no.getLongName());

			// 获取客户信息
			DpCustBaseInfo custInfo = DpCustomerIobus.getCustBaseInfo(acctInfo.getCust_no(), acctInfo.getCust_type());

			if (!CommUtil.equals(custInfo.getDoc_type(), cplIn.getDoc_type()) || !CommUtil.equals(custInfo.getDoc_no(), cplIn.getDoc_no())) {
				throw DpErr.Dp.E0257(custInfo.getDoc_type(), cplIn.getDoc_type(), custInfo.getDoc_no(), cplIn.getDoc_no());
			}
		}
		else if (condInfo.getCheck_seal_ind() == E_YESORNO.YES) {
			// TODO: 两种方法：1）核心不验印 2)柜面验印后上送核心验印结果码，核心查询验印码的合法性
		}

		bizlog.method(" DpPublicCheck.checkPassWord end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-下午3:35:57</li>
	 *         <li>功能说明：凭证状态检查</li>
	 *         </p>
	 * @param openVochCheckInd
	 *            凭证状态检标志
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 */
	public static void checkOpenVochStatus(E_YESORNO openVochCheckInd, DpaSubAccount subAccount, DpaAccount account) {

		bizlog.method(" DpPublicCheck.checkOpenVochStatus begin >>>>>>>>>>>>>>>>");

		if (openVochCheckInd == E_YESORNO.NO) {
			return;
		}

		// 检查凭证状态
		DpVoucherIobus.checkVouchersStatus(subAccount, account);

		bizlog.method(" DpPublicCheck.checkOpenVochStatus end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhuw
	 *         <p>
	 *         <li>2018年12月25日</li>
	 *         <li>功能说明：是否可查询员工账户</li>
	 *         </p>
	 * @param acctType
	 *            查询条件账户类型
	 * @param channelId
	 *            当前交易渠道
	 * @param roleCollects
	 *            当前柜员角色集合
	 * @return
	 */
	public static boolean queryEmpAcctFlag(String acctType, String channelId, String roleCollects) {

		// 临时处理hcj
		if (CommUtil.isNull(roleCollects)) {
			return true;
		}

		if (!ApChannelApi.isCounter(channelId)) {
			return true;
		}

		String roles = ApBusinessParmApi.getValue("ROLE_OPR_EMP");// 可操作员工账号的柜员角色，多个以逗号隔开

		if (roles.split(",").length > 1) {
			for (String r : roles.split(",")) {
				if (roleCollects.contains(r)) {
					return true;
				}
			}
		}
		else if (roleCollects.contains(roles)) {
			return true;
		}

		String empAcctType = ApBusinessParmApi.getValue("EMP_ACCT_TYPE");// 员工账户类型

		if (CommUtil.isNotNull(acctType) && acctType.equals(empAcctType)) {
			throw DpErr.Dp.E0461();
		}

		return false;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月30日-下午5:30:31</li>
	 *         <li>功能说明：获取客户信息Map数据</li>
	 *         </p>
	 * @param custNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 * @return Map<String, Object>
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getCustMapInfo(String custNo, E_CUSTOMERTYPE custType) {

		return DpCustomerIobus.getCustBaseInfo(custNo, custType).getCustInfo();
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月22日-下午5:30:31</li>
	 *         <li>功能说明：加载客户信息数据</li>
	 *         </p>
	 * @param custNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 */
	public static void addDataToCustBuffer(String custNo, E_CUSTOMERTYPE custType) {

		// 读取数据缓冲区的客户信息
		Object custBuff = ApBufferApi.getBuffer().get(ApConst.CUST_DATA_MART);

		// 已经存在客户数据集直接退出
		if (CommUtil.isNotNull(custBuff)) {

			if (CommUtil.toMap(custBuff).containsKey(SysDict.A.cust_no.getId()) && CommUtil.equals(CommUtil.toMap(custBuff).get(SysDict.A.cust_no.getId()).toString(), custNo)) {

				return;
			}

		}

		// 获取客户信息Map数据
		Map<String, Object> mapCustData = getCustMapInfo(custNo, custType);

		// 取客户信息中的属性值
		Object oldValue = mapCustData.get(SysDict.A.attr_value.getId());

		String oldAttrValue = CommUtil.isNotNull(oldValue) ? oldValue.toString() : "";

		// 获取客户最新账户属性值
		String newAttrValue = ApAttributeApi.getNewestAttrValue(E_OWNERLEVEL.CUSTOMER, custNo, oldAttrValue);

		mapCustData.put(SysDict.A.attr_value.getId(), newAttrValue);

		// 加载客户数据集
		ApBufferApi.addData(ApConst.CUST_DATA_MART, mapCustData);
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月22日-下午5:30:31</li>
	 *         <li>功能说明：加载客户信息数据</li>
	 *         </p>
	 * @param custNo
	 *            客户号
	 */
	public static void addDataToCustBuffer(String custNo) {
		addDataToCustBuffer(custNo, null);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月14日-下午13:22:21</li>
	 *         <li>功能说明：子账户交易冻结止付限制状态检查</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param eventId
	 *            事件编号
	 * @param frozeId
	 *            冻结编号
	 */
	public static void checkSubAcctTrxnLimit(DpaSubAccount subAcct, E_DEPTTRXNEVENT eventId, String frozeId) {

		bizlog.method(" DpPublicCheck.checkSubAcctTrxnLimit begin >>>>>>>>>>>>>>>>");

		DpFrozeObjectLimitStatus limitStatusInfo = BizUtil.getInstance(DpFrozeObjectLimitStatus.class);

		// 支取类冻结限制状态检查
		if (E_DEPTTRXNEVENT.E_DRAWTYPE.contains(eventId)) {

			limitStatusInfo = DpToolsApi.getRealTimeLimitStatusForDraw(subAcct, frozeId);
		}
		else {

			limitStatusInfo = DpToolsApi.getRealTimeLimitStatusForSave(subAcct);
		}

		// 交易事件和限制状态匹配性检查
		if (CommUtil.in(eventId, E_DEPTTRXNEVENT.DP_SAVE, E_DEPTTRXNEVENT.DP_TIME_SAVE)) {

			if (CommUtil.in(limitStatusInfo.getCust_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0270(subAcct.getCust_no());
			}
			else if (CommUtil.in(limitStatusInfo.getCard_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0032(subAcct.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getAcct_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0032(subAcct.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getSub_acct_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0036(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
			}

		}
		else if (CommUtil.in(eventId, E_DEPTTRXNEVENT.DP_DRAW, E_DEPTTRXNEVENT.DP_TIME_DRAW, E_DEPTTRXNEVENT.PRE_AUTHOR, E_DEPTTRXNEVENT.DP_WAIT_INTEREST_DRAW)) {

			if (CommUtil.in(limitStatusInfo.getCust_limit_status(), E_ACCTLIMITSTATUS.IN, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0271(subAcct.getCust_no());
			}
			else if (CommUtil.in(limitStatusInfo.getCard_limit_status(), E_ACCTLIMITSTATUS.IN, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0030(subAcct.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getAcct_limit_status(), E_ACCTLIMITSTATUS.IN, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0030(subAcct.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getSub_acct_limit_status(), E_ACCTLIMITSTATUS.IN, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0034(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
			}
		}
		else if (CommUtil.in(eventId, E_DEPTTRXNEVENT.DP_OPEN_ACCOUNT, E_DEPTTRXNEVENT.DP_OPEN_SUBACCT)) {
			; // 开户暂不检查冻结限制状态
		}
		else if (CommUtil.in(eventId, E_DEPTTRXNEVENT.DP_CLOSE_ACCOUNT, E_DEPTTRXNEVENT.DP_CLOSE_SUBACCT)) {

			if (CommUtil.in(limitStatusInfo.getCust_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0269(subAcct.getCust_no());
			}
			else if (CommUtil.in(limitStatusInfo.getCard_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0031(subAcct.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getAcct_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0031(subAcct.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getSub_acct_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0035(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
			}
		}

		bizlog.method(" DpPublicCheck.checkSubAcctTrxnLimit end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月14日-下午13:22:21</li>
	 *         <li>功能说明：子账户交易冻结止付限制状态检查</li>
	 *         </p>
	 * @param acctInfo
	 *            子账户信息
	 * @param eventId
	 *            事件编号
	 * @param frozeId
	 *            冻结编号
	 */
	public static void checkAcctTrxnLimit(DpaAccount acctInfo, E_DEPTTRXNEVENT eventId, String frozeId) {

		bizlog.method(" DpPublicCheck.checkAcctTrxnLimit begin >>>>>>>>>>>>>>>>");

		DpFrozeObjectLimitStatus limitStatusInfo = BizUtil.getInstance(DpFrozeObjectLimitStatus.class);

		// 支取类冻结限制状态检查
		if (E_DEPTTRXNEVENT.E_DRAWTYPE.contains(eventId)) {

			limitStatusInfo = DpToolsApi.getRealTimeLimitStatusForDraw(acctInfo, frozeId);
		}
		else {

			limitStatusInfo = DpToolsApi.getRealTimeLimitStatusForSave(acctInfo);
		}

		// 交易事件和限制状态匹配性检查
		if (CommUtil.in(eventId, E_DEPTTRXNEVENT.DP_SAVE, E_DEPTTRXNEVENT.DP_TIME_SAVE)) {

			if (CommUtil.in(limitStatusInfo.getCust_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0270(acctInfo.getCust_no());
			}
			else if (CommUtil.in(limitStatusInfo.getCard_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0032(acctInfo.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getAcct_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0032(acctInfo.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getSub_acct_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0032(acctInfo.getAcct_no());
			}

		}
		else if (CommUtil.in(eventId, E_DEPTTRXNEVENT.DP_DRAW, E_DEPTTRXNEVENT.DP_TIME_DRAW, E_DEPTTRXNEVENT.PRE_AUTHOR, E_DEPTTRXNEVENT.DP_WAIT_INTEREST_DRAW)) {

			if (CommUtil.in(limitStatusInfo.getCust_limit_status(), E_ACCTLIMITSTATUS.IN, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0271(acctInfo.getCust_no());
			}
			else if (CommUtil.in(limitStatusInfo.getCard_limit_status(), E_ACCTLIMITSTATUS.IN, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0030(acctInfo.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getAcct_limit_status(), E_ACCTLIMITSTATUS.IN, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0030(acctInfo.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getSub_acct_limit_status(), E_ACCTLIMITSTATUS.IN, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0030(acctInfo.getAcct_no());
			}
		}
		else if (CommUtil.in(eventId, E_DEPTTRXNEVENT.DP_OPEN_ACCOUNT, E_DEPTTRXNEVENT.DP_OPEN_SUBACCT)) {
			; // 开户暂不检查冻结限制状态
		}
		else if (CommUtil.in(eventId, E_DEPTTRXNEVENT.DP_CLOSE_ACCOUNT, E_DEPTTRXNEVENT.DP_CLOSE_SUBACCT)) {

			if (CommUtil.in(limitStatusInfo.getCust_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0269(acctInfo.getCust_no());
			}
			else if (CommUtil.in(limitStatusInfo.getCard_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0031(acctInfo.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getAcct_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0031(acctInfo.getAcct_no());
			}
			else if (CommUtil.in(limitStatusInfo.getSub_acct_limit_status(), E_ACCTLIMITSTATUS.OUT, E_ACCTLIMITSTATUS.SEAL)) {
				throw DpBase.E0031(acctInfo.getAcct_no());
			}
		}

		bizlog.method(" DpPublicCheck.checkAcctTrxnLimit end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年6月20日-下午4:30:49</li>
	 *         <li>功能说明：收息账户合法性检查</li>
	 *         <li>补充说明：也可以检查本金转入账户</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @param ccyCode
	 *            子账户
	 * @param saveOrDraw
	 *            存入支取标志
	 */
	public static void checkIncomeAcct(String acctNo, String ccyCode, E_SAVEORWITHDRAWALIND saveOrDraw) {

		// 路由类型
		E_ACCOUTANALY routeType = DpInsideAccountIobus.getAccountRouteType(acctNo);

		// 内部户
		if (routeType == E_ACCOUTANALY.INSIDE) {

			// TODO: 内部户不检查，认为内部户是正常的

		}

		// 存款账户、存放同业账户
		else if (CommUtil.in(routeType, E_ACCOUTANALY.DEPOSIT, E_ACCOUTANALY.NOSTRO)) {

			DpAcctAccessIn cplAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			cplAccessIn.setAcct_no(acctNo);
			cplAccessIn.setCcy_code(ccyCode);
			cplAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

			DpAcctAccessOut cplAccessOut = DpToolsApi.locateSingleSubAcct(cplAccessIn);

			// 子账户信息
			DpaSubAccount appointAcct = DpaSubAccountDao.selectOneWithLock_odb1(cplAccessOut.getAcct_no(), cplAccessOut.getSub_acct_no(), true);

			// 检查指定账户存入或支取限制,不传入存入支取类型则不检查冻结限制
			if (saveOrDraw == E_SAVEORWITHDRAWALIND.SAVE) {
				checkSubAcctTrxnLimit(appointAcct, E_DEPTTRXNEVENT.DP_SAVE, null);
			}
			else if (saveOrDraw == E_SAVEORWITHDRAWALIND.WITHDRAWAL) {
				checkSubAcctTrxnLimit(appointAcct, E_DEPTTRXNEVENT.DP_DRAW, null);
			}
		}
		else {
			// 出现了新的情形，代码未开发，先报错
			throw APPUB.E0026(routeType.getLongName(), routeType.getValue());
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年6月20日-下午4:30:49</li>
	 *         <li>功能说明：获取客户默认账户信息</li>
	 *         <li>补充说明：返回值可能为空</li>
	 *         </p>
	 * @param custNo
	 *            客户号
	 * @return 账户信息
	 */
	public static DpaAccountRelate getDefaultAccount(String custNo) {

		// 活期子户，最早开立的默认子户优先
		return DpaAccountRelateDao.selectFirst_odb9(custNo, E_DEMANDORTIME.DEMAND, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT, false);
	}

}
