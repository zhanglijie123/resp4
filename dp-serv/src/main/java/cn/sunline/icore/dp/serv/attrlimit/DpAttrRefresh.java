package cn.sunline.icore.dp.serv.attrlimit;

import java.util.HashMap;
import java.util.Map;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAttributeApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpMdyCustSimpleIn;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_CUSTOMERTYPE;
import cn.sunline.icore.sys.type.EnumType.E_OWNERLEVEL;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：业务表属性刷新方法
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月12日-下午4:42:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAttrRefresh {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAttrRefresh.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年10月20日-下午4:58:22</li>
	 *         <li>功能说明：获取各数据域最新属性值</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 * @param inputAcctNo
	 *            输入账号域: 从输入接口直接取的账号域值
	 * @param custNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 * @return 最新属性值
	 */
	@SuppressWarnings("unchecked")
	private static Map<E_OWNERLEVEL, String> getNewestAttrValue(DpaSubAccount subAccount, DpaAccount account, String inputAcctNo, String custNo, E_CUSTOMERTYPE custType) {

		Map<E_OWNERLEVEL, String> result = new HashMap<E_OWNERLEVEL, String>();

		// 1 获取最新子账户属性值
		if (CommUtil.isNotNull(subAccount) && CommUtil.isNotNull(subAccount.getSub_acct_no())) {

			String newSubAcctAttrValue = ApAttributeApi.getNewestAttrValue(E_OWNERLEVEL.SUB_ACCTOUNT, subAccount.getSub_acct_no(), subAccount.getAttr_value());

			result.put(E_OWNERLEVEL.SUB_ACCTOUNT, newSubAcctAttrValue);
		}
		else {
			result.put(E_OWNERLEVEL.SUB_ACCTOUNT, null);
		}

		// 2 获取最新账户属性值
		if (CommUtil.isNotNull(account) && CommUtil.isNotNull(account.getAcct_no())) {

			String newAcctAttrValue = ApAttributeApi.getNewestAttrValue(E_OWNERLEVEL.ACCOUNT, account.getAcct_no(), account.getAttr_value());

			result.put(E_OWNERLEVEL.ACCOUNT, newAcctAttrValue);
		}
		else {
			result.put(E_OWNERLEVEL.ACCOUNT, null);
		}

		// 3.1 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(custNo, custType);

		Map<String, Object> custDataMart = (Map<String, Object>) ApBufferApi.getBuffer().get(ApConst.CUST_DATA_MART);

		// 取客户信息中的属性值
		Object oldValue = custDataMart.get(SysDict.A.attr_value.getId());

		String oldAttrValue = CommUtil.isNotNull(oldValue) ? oldValue.toString() : "";

		// 3.2 获取客户最新账户属性值
		String newCustAttrValue = ApAttributeApi.getNewestAttrValue(E_OWNERLEVEL.CUSTOMER, custNo, oldAttrValue);

		result.put(E_OWNERLEVEL.CUSTOMER, newCustAttrValue);

		// 4. 获取最新卡属性值
		if (CommUtil.isNull(inputAcctNo)) {

			result.put(E_OWNERLEVEL.CARD, null);
		}
		else {

			String cardNo = "";

			if (CommUtil.isNotNull(account) && CommUtil.isNotNull(account.getAcct_no())) {

				if (account.getCard_relationship_ind() == E_YESORNO.YES && !CommUtil.equals(inputAcctNo, account.getAcct_no())) {
					cardNo = inputAcctNo;
				}
			}
			else {

				DpaAccount cardAcct = DpaAccountDao.selectOne_odb1(inputAcctNo, false);

				// 不是账号则就是卡号
				if (CommUtil.isNull(cardAcct)) {
					cardNo = inputAcctNo;
				}
			}

			// 卡号不为空加载卡数据
			if (CommUtil.isNotNull(cardNo)) {

				DpToolsApi.addDataToCardBuffer(cardNo);

				// 存折卡数据才做卡属性处理，因为卡号可能是第一次来核心开账户使用
				if (CommUtil.isNotNull(ApBufferApi.getBuffer().get(ApConst.CARD_DATA_MART))) {

					Map<String, Object> cardDataMart = (Map<String, Object>) ApBufferApi.getBuffer().get(ApConst.CARD_DATA_MART);

					// 前面可能是旧卡，此处获取新卡
					cardNo = cardDataMart.get(SysDict.A.card_no.getId()).toString();

					String oldCardAttrValue = CommUtil.isNotNull(cardDataMart.get(SysDict.A.attr_value.getId())) ? cardDataMart.get(SysDict.A.attr_value.getId()).toString() : "";

					String newCardAttrValue = ApAttributeApi.getNewestAttrValue(E_OWNERLEVEL.CARD, cardNo, oldCardAttrValue);

					result.put(E_OWNERLEVEL.CARD, newCardAttrValue);
				}
				else {
					result.put(E_OWNERLEVEL.CARD, null);
				}
			}
			else {
				result.put(E_OWNERLEVEL.CARD, null);
			}
		}

		return result;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年10月20日-下午4:58:22</li>
	 *         <li>功能说明：更新各数据域最新属性值</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 * @param inputAcctNo
	 *            输入账号域: 从输入接口直接取的账号域值
	 * @param commitDB
	 *            提交数据库标志
	 */
	public static void refreshAttrValue(DpaSubAccount subAccount, DpaAccount account, String inputAcctNo, E_YESORNO commitDB) {

		refreshAttrValue(subAccount, account, inputAcctNo, account.getCust_no(), account.getCust_type(), commitDB);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年10月20日-下午4:58:22</li>
	 *         <li>功能说明：更新各数据域最新属性值</li>
	 *         </p>
	 * @param account
	 *            账户信息
	 * @param inputAcctNo
	 *            输入账号域: 从输入接口直接取的账号域值
	 * @param commitDB
	 *            提交数据库标志
	 */
	public static void refreshAttrValue(DpaAccount account, String inputAcctNo, E_YESORNO commitDB) {

		refreshAttrValue(null, account, inputAcctNo, account.getCust_no(), account.getCust_type(), commitDB);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年10月20日-下午4:58:22</li>
	 *         <li>功能说明：更新各数据域最新属性值</li>
	 *         </p>
	 * @param cardNo
	 *            卡号: 一定要确定是卡号才传入，否则传空值
	 * @param custNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 * @param commitDB
	 *            提交数据库标志
	 */
	public static void refreshAttrValue(String cardNo, String custNo, E_CUSTOMERTYPE custType, E_YESORNO commitDB) {

		refreshAttrValue(null, null, cardNo, custNo, custType, commitDB);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年10月20日-下午4:58:22</li>
	 *         <li>功能说明：更新各数据域最新属性值</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 * @param inputAcctNo
	 *            输入账号域: 从输入接口直接取的账号域值,为了取卡号
	 * @param custNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 * @param commitDB
	 *            提交数据库标志
	 */
	@SuppressWarnings("unchecked")
	private static void refreshAttrValue(DpaSubAccount subAccount, DpaAccount account, String inputAcctNo, String custNo, E_CUSTOMERTYPE custType, E_YESORNO commitDB) {

		bizlog.method(" DpBuffer.refreshAttrValue begin >>>>>>>>>>>>>>>>");

		Map<E_OWNERLEVEL, String> dataMartAttrValue = getNewestAttrValue(subAccount, account, inputAcctNo, custNo, custType);

		// 1. 子账户属性有更新
		if (CommUtil.isNotNull(subAccount) && CommUtil.isNotNull(subAccount.getSub_acct_no())) {

			if (!CommUtil.equals(dataMartAttrValue.get(E_OWNERLEVEL.SUB_ACCTOUNT), subAccount.getAttr_value())) {

				subAccount.setAttr_value(dataMartAttrValue.get(E_OWNERLEVEL.SUB_ACCTOUNT));

				if (commitDB == E_YESORNO.YES) {
					DpaSubAccountDao.updateOne_odb1(subAccount);
				}
			}
		}

		// 2. 账户属性有更新
		if (CommUtil.isNotNull(account) && CommUtil.isNotNull(account.getAcct_no())) {

			if (!CommUtil.equals(dataMartAttrValue.get(E_OWNERLEVEL.ACCOUNT), account.getAttr_value())) {

				account.setAttr_value(dataMartAttrValue.get(E_OWNERLEVEL.ACCOUNT));

				if (commitDB == E_YESORNO.YES) {
					DpaAccountDao.updateOne_odb1(account);
				}
			}
		}

		// 客户数据集
		Map<String, Object> custDataMart = (Map<String, Object>) ApBufferApi.getBuffer().get(ApConst.CUST_DATA_MART);

		String oldCustAttrValue = CommUtil.isNull(custDataMart.get(SysDict.A.attr_value.getId())) ? null : custDataMart.get(SysDict.A.attr_value.getId()).toString();

		// 3. 客户属性有更新
		if (!CommUtil.equals(dataMartAttrValue.get(E_OWNERLEVEL.CUSTOMER), oldCustAttrValue)) {

			custDataMart.put(SysDict.A.attr_value.getId(), dataMartAttrValue.get(E_OWNERLEVEL.CUSTOMER));

			ApBufferApi.addData(ApConst.CUST_DATA_MART, custDataMart);

			if (commitDB == E_YESORNO.YES) {

				DpMdyCustSimpleIn cplMdyCustIn = BizUtil.getInstance(DpMdyCustSimpleIn.class);

				cplMdyCustIn.setCust_no(custNo);
				cplMdyCustIn.setCust_type(custType);
				cplMdyCustIn.setAttr_value((String) custDataMart.get(SysDict.A.attr_value.getId()));
				cplMdyCustIn.setData_version(CommUtil.isNotNull(custDataMart.get(SysDict.A.data_version.getId())) ? (long) custDataMart.get(SysDict.A.data_version.getId()) : null);
				
				DpCustomerIobus.modifyCustSimpleInfo(cplMdyCustIn);
			}

		}

		// 4. 卡属性值，卡模块处理
		if (CommUtil.isNotNull(dataMartAttrValue.get(E_OWNERLEVEL.CARD)) && CommUtil.isNotNull(ApBufferApi.getBuffer().get(ApConst.CARD_DATA_MART))) {

			Map<String, Object> cardDataMart = (Map<String, Object>) ApBufferApi.getBuffer().get(ApConst.CARD_DATA_MART);

			String oldCardAttrValue = CommUtil.isNull(cardDataMart.get(SysDict.A.attr_value.getId())) ? null : cardDataMart.get(SysDict.A.attr_value.getId()).toString();

			if (!CommUtil.equals(dataMartAttrValue.get(E_OWNERLEVEL.CARD), oldCardAttrValue)) {

				cardDataMart.put(SysDict.A.attr_value.getId(), dataMartAttrValue.get(E_OWNERLEVEL.CARD));

				ApBufferApi.addData(ApConst.CARD_DATA_MART, cardDataMart);

				if (commitDB == E_YESORNO.YES) {
					DpaCardDao.updateOne_odb1((DpaCard) cardDataMart);
				}
			}
		}

		bizlog.method(" DpBuffer.refreshAttrValue end <<<<<<<<<<<<<<<<");
	}
}
