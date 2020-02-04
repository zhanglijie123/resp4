package cn.sunline.icore.dp.serv.iobus;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.cf.iobus.servicetype.SrvIoCfCustomerInfo;
import cn.sunline.icore.cf.iobus.type.ComIoCfCustomer.IoCfAgentInfoIn;
import cn.sunline.icore.cf.iobus.type.ComIoCfCustomer.IoCfCustBriefInfoMntIn;
import cn.sunline.icore.cf.iobus.type.ComIoCfCustomerInfo.IoCfCustQryIn;
import cn.sunline.icore.cf.iobus.type.ComIoCfCustomerInfo.IoCfCustSimpleInfo;
import cn.sunline.icore.cf.iobus.type.ComIoCfCustomerInfo.IoCfCustomerInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpAgentInfoRegister;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustBaseInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustSimpleInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpMdyCustSimpleIn;
import cn.sunline.icore.sys.type.EnumType.E_CUSTOMERTYPE;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpCustomerIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCustomerIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：登记代理人信息</li>
	 *         </p>
	 * @param cplIn
	 *            登记代理人信息输入
	 */
	public static void registerAgentInfo(DpAgentInfoRegister cplIn) {

		if (CommUtil.isNull(cplIn.getAgent_doc_no()) || CommUtil.isNull(cplIn.getAgent_doc_type())) {
			return;
		}

		IoCfAgentInfoIn agentInfoIn = BizUtil.getInstance(IoCfAgentInfoIn.class);

		agentInfoIn.setAgent_doc_no(cplIn.getAgent_doc_no());
		agentInfoIn.setAgent_doc_type(cplIn.getAgent_doc_type());
		agentInfoIn.setAgent_country(cplIn.getAgent_country());
		agentInfoIn.setAgent_name(cplIn.getAgent_name());
		agentInfoIn.setRemark(cplIn.getRemark());

		SysUtil.getRemoteInstance(SrvIoCfCustomerInfo.class).regAgentInfo(agentInfoIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取客户简单信息</li>
	 *         </p>
	 * @param sCustNo
	 *            客户号：必输
	 */
	public static DpCustSimpleInfo getCustSimpleInfo(String sCustNo) {

		return getCustSimpleInfo(sCustNo, null, null, null);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取客户简单信息</li>
	 *         </p>
	 * @param sCustNo
	 *            客户号：必输
	 * @param custType
	 *            客户类型
	 */
	public static DpCustSimpleInfo getCustSimpleInfo(String sCustNo, E_CUSTOMERTYPE custType) {

		return getCustSimpleInfo(sCustNo, custType, null, null);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取客户简单信息</li>
	 *         </p>
	 * @param sDocType
	 *            证件类型
	 * @param sDocNo
	 *            证件号码
	 */
	public static DpCustSimpleInfo getCustSimpleInfo(String sDocType, String sDocNo) {

		return getCustSimpleInfo(null, null, sDocType, sDocNo);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取客户简单信息</li>
	 *         </p>
	 * @param sCustNo
	 *            客户号：必输
	 * @param custType
	 *            客户类型
	 * @param sDocType
	 *            证件类型
	 * @param sDocNo
	 *            证件号码
	 */
	public static DpCustSimpleInfo getCustSimpleInfo(String sCustNo, E_CUSTOMERTYPE custType, String sDocType, String sDocNo) {

		IoCfCustQryIn cplCustQryIn = BizUtil.getInstance(IoCfCustQryIn.class);

		cplCustQryIn.setCust_no(sCustNo);
		cplCustQryIn.setCust_type(custType);
		cplCustQryIn.setDoc_no(sDocNo);
		cplCustQryIn.setDoc_type(sDocType);

		IoCfCustSimpleInfo cplCustQryOut = SysUtil.getRemoteInstance(SrvIoCfCustomerInfo.class).qryCustSimpleInfo(cplCustQryIn);

		// 简单客户信息输出
		DpCustSimpleInfo simpleInfo = BizUtil.getInstance(DpCustSimpleInfo.class);

		simpleInfo.setAttr_value(cplCustQryOut.getAttr_value());
		simpleInfo.setCust_exist_ind(E_YESORNO.YES);
		simpleInfo.setCust_foreign_name(cplCustQryOut.getCust_foreign_name());
		simpleInfo.setCust_limit_status(cplCustQryOut.getCust_limit_status());
		simpleInfo.setCust_name(cplCustQryOut.getCust_name());
		simpleInfo.setCust_no(cplCustQryOut.getCust_no());
		simpleInfo.setCust_type(cplCustQryOut.getCust_type());
		simpleInfo.setData_version(cplCustQryOut.getData_version());
		simpleInfo.setStaff_ind(cplCustQryOut.getStaff_ind());
		simpleInfo.setTitle(cplCustQryOut.getTitle());
		simpleInfo.setGender(cplCustQryOut.getGender());
		simpleInfo.setCust_status(cplCustQryOut.getCust_status());
		simpleInfo.setBirth_date(cplCustQryOut.getBirth_date());
		simpleInfo.setAge(cplCustQryOut.getAge());
		simpleInfo.setNationality(cplCustQryOut.getNationality());
		simpleInfo.setResident_country(cplCustQryOut.getResident_country());

		return simpleInfo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：维护客户简单信息</li>
	 *         </p>
	 * @param cplIn
	 *            客户简单信息维护输入
	 */
	public static void modifyCustSimpleInfo(DpMdyCustSimpleIn cplIn) {

		IoCfCustBriefInfoMntIn cplMntCustIn = BizUtil.getInstance(IoCfCustBriefInfoMntIn.class);

		cplMntCustIn.setCust_no(cplIn.getCust_no());
		cplMntCustIn.setCust_type(cplIn.getCust_type());
		cplMntCustIn.setCust_limit_status(cplIn.getCust_limit_status());
		cplMntCustIn.setAttr_value(cplIn.getAttr_value());
		cplMntCustIn.setData_version(cplIn.getData_version());

		SysUtil.getRemoteInstance(SrvIoCfCustomerInfo.class).mntCustBriefInfo(cplMntCustIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取客户基本信息</li>
	 *         </p>
	 * @param sCustNo
	 *            客户号：必输
	 * @return 客户基础信息
	 */
	public static DpCustBaseInfo getCustBaseInfo(String sCustNo) {

		return getCustBaseInfo(sCustNo, null, "", "");
	}
	
	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取客户基本信息</li>
	 *         </p>
	 * @param sCustNo
	 *            客户号：必输
	 * @param custType
	 *            客户类型:必输
	 * @return 客户基础信息
	 */
	public static DpCustBaseInfo getCustBaseInfo(String sCustNo, E_CUSTOMERTYPE custType) {

		return getCustBaseInfo(sCustNo, custType, "", "");
	}
	
	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取客户基本信息</li>
	 *         </p>
	 * @param docType
	 *            证件类型
	 * @param docNo
	 *            证件号
	 * @return 客户基础信息
	 */
	public static DpCustBaseInfo getCustBaseInfo(String docType, String docNo) {

		return getCustBaseInfo("", null, docType, docNo);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取客户基本信息</li>
	 *         </p>
	 * @param sCustNo
	 *            客户号
	 * @param custType
	 *            客户类型
	 * @param docType
	 *            证件类型
	 * @param docNo
	 *            证件号
	 * @return 客户基础信息
	 */
	public static DpCustBaseInfo getCustBaseInfo(String sCustNo, E_CUSTOMERTYPE custType, String docType, String docNo) {

		IoCfCustQryIn cplCustIn = BizUtil.getInstance(IoCfCustQryIn.class);

		cplCustIn.setCust_no(sCustNo);
		cplCustIn.setCust_type(custType);
		cplCustIn.setDoc_no(docType);
		cplCustIn.setDoc_type(docNo);

		IoCfCustomerInfo cplCustOut = SysUtil.getRemoteInstance(SrvIoCfCustomerInfo.class).qryCustInfo(cplCustIn);

		// 输出
		DpCustBaseInfo custInfo = BizUtil.getInstance(DpCustBaseInfo.class);

		custInfo.setCust_exist_ind(E_YESORNO.YES);
		custInfo.setCustInfo(CommUtil.toMap(cplCustOut));
		custInfo.setEmail_validate_ind(cplCustOut.getEmail_validate_ind());
		custInfo.setLanguage(cplCustOut.getLanguage());
		custInfo.setPush_email_ind(cplCustOut.getPush_email_ind());
		custInfo.setPush_msg_ind(cplCustOut.getPush_msg_ind());
		custInfo.setPush_sms_ind(cplCustOut.getPush_sms_ind());
		custInfo.setCust_foreign_name(cplCustOut.getCust_foreign_name());
		custInfo.setCust_limit_status(cplCustOut.getCust_limit_status());
		custInfo.setCust_name(cplCustOut.getCust_name());
		custInfo.setCust_no(cplCustOut.getCust_no());
		custInfo.setCust_type(cplCustOut.getCust_type());
		custInfo.setData_version(cplCustOut.getData_version());
		custInfo.setDoc_no(cplCustOut.getDoc_no());
		custInfo.setDoc_type(cplCustOut.getDoc_type());
		custInfo.setStaff_ind(cplCustOut.getStaff_ind());
		custInfo.setTitle(cplCustOut.getTitle());
		custInfo.setGender(cplCustOut.getGender());
		custInfo.setCust_status(cplCustOut.getCust_status());
		custInfo.setBirth_date(cplCustOut.getBirth_date());
		custInfo.setAge(cplCustOut.getAge());
		custInfo.setNationality(cplCustOut.getNationality());
		custInfo.setResident_country(cplCustOut.getResident_country());

		return custInfo;
	}
	
	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：判断客户是否存在</li>
	 *         </p>
	 * @param sCustNo
	 *            客户号
	 * @return 是否存在
	 */
	public static E_YESORNO existsCustomer(String sCustNo) {
	
		return SysUtil.getRemoteInstance(SrvIoCfCustomerInfo.class).checkCustExist(sCustNo);
	}
}