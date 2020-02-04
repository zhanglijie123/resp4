package cn.sunline.icore.dp.serv.callback;

import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.plugin.DpBusinessPlugin;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustSimpleInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpMdyCustSimpleIn;
import cn.sunline.icore.sys.type.EnumType.E_ACCTLIMITSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_CUSTOMERTYPE;

/**
 * <p>
 * 文件功能说明：业务层处理供底层调用扩展点
 * </p>
 * 
 * @Author 周明易
 *         <p>
 *         <li>2019年3月29日-下午14:35:50</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>2019年3月29日-周明易：存款模块透支额度相关扩展点</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpBusinessPluginImpl implements DpBusinessPlugin {

	/**
	 * 销子账户特殊检查: 欠费、签约等
	 * 
	 * @param subAcct
	 *            子账户信息
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void closeSubAcctSpecialCheck(DpaSubAccount subAcct) {
		// TODO
	}

	/**
	 * 添加客户数据到规则缓存区
	 * 
	 * @param custNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void addCustDataToBuffer(String custNo, E_CUSTOMERTYPE custType) {
		DpPublicCheck.addDataToCustBuffer(custNo, custType);
	}

	/**
	 * 维护客户基础信息
	 * 
	 * @param custNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 * @param limitStatus
	 *            客户限制状态
	 * @param attrValue
	 *            客户属性值
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void modifyCustomerBaseInfo(String custNo, E_CUSTOMERTYPE custType, E_ACCTLIMITSTATUS limitStatus, String attrValue) {

		DpMdyCustSimpleIn cplMntCustIn = BizUtil.getInstance(DpMdyCustSimpleIn.class);

		cplMntCustIn.setCust_no(custNo);
		cplMntCustIn.setCust_type(custType);
		cplMntCustIn.setCust_limit_status(limitStatus);
		cplMntCustIn.setAttr_value(attrValue);

		DpCustomerIobus.modifyCustSimpleInfo(cplMntCustIn);
	}

	/**
	 * 维护客户基础信息
	 * 
	 * @param custNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 * @return attrValue 客户属性值
	 */
	@Override
	@SuppressWarnings("unchecked")
	public String getCustomerAttrValue(String custNo, E_CUSTOMERTYPE custType) {

		// 查询客户简要信息
		DpCustSimpleInfo cplCustInfo = DpCustomerIobus.getCustSimpleInfo(custNo, custType);

		return cplCustInfo.getAttr_value();
	}
}
