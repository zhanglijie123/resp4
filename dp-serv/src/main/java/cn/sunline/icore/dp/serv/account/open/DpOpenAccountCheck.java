package cn.sunline.icore.dp.serv.account.open;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpAttrLimitApi;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountType;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpJoinCust;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpOpenAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustSimpleInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_SELFOPTNUMBERIND;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_CUSTOMERTYPE;
import cn.sunline.icore.sys.type.EnumType.E_CUSTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_OWNERLEVEL;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：负债新开账户检查
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年1月6日-下午5:07:42</li>
 *         <li>开户服务相关代码</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpOpenAccountCheck {
	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOpenAccountCheck.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午3:03:06</li>
	 *         <li>功能说明：负债新开账户检查</li>
	 *         </p>
	 * @param cplIn
	 *            开户检查输入接口
	 */
	public static void checkMain(DpOpenAccountIn cplIn) {

		// 客户号不能为空
		BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getAcct_type(), SysDict.A.acct_type.getId(), SysDict.A.acct_type.getLongName());

		// 查询账户类型定义
		DppAccountType acctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getAcct_type());

		// 刷新卡属性、客户属性: 不提交数据库
		DpAttrRefresh.refreshAttrValue(cplIn.getCard_no(), cplIn.getCust_no(), acctType.getCust_type(), E_YESORNO.NO);

		checkMainMethod(cplIn);
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月15日-下午3:03:06</li>
	 *         <li>功能说明：负债新开账户检查主调方法</li>
	 *         </p>
	 * @param cplIn
	 *            开户检查输入接口
	 */
	public static void checkMainMethod(DpOpenAccountIn cplIn) {

		bizlog.method(" DpOpenAccountCheck.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 默认值
		defaultValue(cplIn);

		// 检查输入值
		checkInputData(cplIn);

		// 加载输入数据集
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		// 查询账户类型定义
		DppAccountType acctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getAcct_type());

		// 加载参数数据区
		ApBufferApi.addData(ApConst.PARM_DATA_MART, CommUtil.toMap(acctType));

		// 客户信息检查,加载客户数据集
		checkCustInfo(cplIn.getCust_no(), acctType, cplIn.getAcct_name());

		// 如果存在卡信息,加载卡数据区
		DpaCard cardInfo = DpaCardDao.selectOne_odb1(cplIn.getCard_no(), false);
		if (cardInfo != null) {

			ApBufferApi.addData(ApConst.CARD_DATA_MART, CommUtil.toMap(cardInfo));
		}

		// 账户类型的定义做相关检查
		checkAcctType(cplIn, acctType);

		// 检查联名账户列表信息
		if (cplIn.getJoint_acct_ind() == E_YESORNO.YES) {
			checkJointList(cplIn, acctType.getCust_type());
		}

		// 属性设置合法性检查
		String attrValue = DpAttrLimitApi.checkAttributeSet(cplIn.getList_attribute(), E_OWNERLEVEL.ACCOUNT);

		// 将属性值加载到输入数据区
		Map<String, Object> mapObj = new HashMap<String, Object>();

		mapObj.put("attr_value", attrValue);

		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapObj);

		// 交易控制检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_OPEN_ACCOUNT.getValue());

		bizlog.method(" DpOpenAccountCheck.checkMain end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月31日-上午11:17:38</li>
	 *         <li>功能说明：检查联名账户列表信息</li>
	 *         </p>
	 * @param cplIn
	 *            开户检查输入接口
	 */
	private static void checkJointList(DpOpenAccountIn cplIn, E_CUSTOMERTYPE custType) {

		// 客户set数据集
		Set<String> custNoSet = new HashSet<String>();

		for (DpJoinCust jointCust : cplIn.getList_joint_cust()) {

			// 客户号不能为空
			BizUtil.fieldNotNull(jointCust.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

			// 客户号不能与主客户号相同
			if (CommUtil.equals(jointCust.getCust_no(), cplIn.getCust_no())) {
				throw DpErr.Dp.E0176();
			}

			// 证件号不能为空
			BizUtil.fieldNotNull(jointCust.getDoc_no(), SysDict.A.doc_no.getId(), SysDict.A.doc_no.getLongName());
			// 证件类型不能为空
			BizUtil.fieldNotNull(jointCust.getDoc_type(), SysDict.A.doc_type.getId(), SysDict.A.doc_type.getLongName());
			// 客户名称不能为空
			BizUtil.fieldNotNull(jointCust.getCust_name(), SysDict.A.cust_name.getId(), SysDict.A.cust_name.getLongName());
			// 独立签名指示不能为空
			// BizUtil.fieldNotNull(jointCust.getSignature_ind(),
			// DpBaseDict.A.signature_ind.getId(),
			// DpBaseDict.A.signature_ind.getLongName());

			// 联系人名关系不为空，检查下拉值是否合法
			if (CommUtil.isNotNull(jointCust.getJoint_person_relationship())) {

				ApDropListApi.exists(DpConst.JOINT_PERSON_RELATIONSHIP, jointCust.getJoint_person_relationship());
			}

			// 出现重复客户号，抛出错误信息
			if (custNoSet.contains(jointCust.getCust_no())) {
				throw DpErr.Dp.E0182(jointCust.getCust_no());
			}

			// 获取客户简单信息
			DpCustSimpleInfo simpleCustInfo = DpCustomerIobus.getCustSimpleInfo(jointCust.getCust_no());
			
			// 联名客户类型不一致
			if (simpleCustInfo.getCust_type() != custType) {
				// TODO:
			}

			// 加入客户set数据集
			custNoSet.add(jointCust.getCust_no());
		}
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月14日-下午2:36:41</li>
	 *         <li>功能说明：账户类型的定义做相关检查</li>
	 *         </p>
	 * @param cplIn
	 *            开户检查输入接口
	 * @param acctType
	 *            账户类型
	 */
	private static void checkAcctType(DpOpenAccountIn cplIn, DppAccountType acctType) {

		bizlog.method(" DpOpenAccountCheck.checkAcctType begin >>>>>>>>>>>>>>>>");

		// 对公开户必须录入账户名称
		if (acctType.getCust_type() == E_CUSTOMERTYPE.CORPORATE) {

			BizUtil.fieldNotNull(cplIn.getAcct_name(), SysDict.A.acct_name.getId(), SysDict.A.acct_name.getLongName());
		}

		// 开户许可检查：检查是否满足账户类型参数要求
		DpBaseServiceApi.checkOpenAcctLicense(cplIn);

		bizlog.method(" DpOpenAccountCheck.checkAcctType end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月13日-上午10:56:33</li>
	 *         <li>功能说明：客户信息检查</li>
	 *         </p>
	 * @param custNo
	 *            客户号
	 * @param acctType
	 *            账户类型
	 * @param acctName
	 *            客户名称
	 */
	private static void checkCustInfo(String custNo, DppAccountType acctType, String acctName) {

		bizlog.method(" DpOpenAccountCheck.checkCustInfo begin >>>>>>>>>>>>>>>>");

		// String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(custNo, acctType.getCust_type());

		// 取规则缓存区客户数据
		Object custBuff = ApBufferApi.getBuffer().get(ApConst.CUST_DATA_MART);

		if (CommUtil.isNull(custBuff)) {
			throw DpErr.Dp.E0094();
		}

		// 客户信息
		Map custInfo = CommUtil.toMap(custBuff);

		if (!CommUtil.equals(custInfo.get(SysDict.A.cust_no.getId()).toString(), custNo)) {

			throw DpErr.Dp.E0094();
		}

		// 证件生效日
		String docEffectDate = "";
		// 证件失效日
		String docExpriyDate = "";
		// 客户姓名
		String custName = "";
		// 客户状态默认为正常
		E_CUSTSTATUS custStatus = E_CUSTSTATUS.NORMAL;

		if (custInfo.containsKey(SysDict.A.doc_effe_date.getId()) && CommUtil.isNotNull(custInfo.get(SysDict.A.doc_effe_date.getId()))) {
			docEffectDate = custInfo.get(SysDict.A.doc_effe_date.getId()).toString();
		}

		if (custInfo.containsKey(SysDict.A.doc_expy_date.getId()) && CommUtil.isNotNull(custInfo.get(SysDict.A.doc_expy_date.getId()))) {
			docExpriyDate = custInfo.get(SysDict.A.doc_expy_date.getId()).toString();
		}

		if (custInfo.containsKey(SysDict.A.cust_name.getId()) && CommUtil.isNotNull(custInfo.get(SysDict.A.cust_name.getId()))) {
			custName = custInfo.get(SysDict.A.cust_name.getId()).toString();
		}

		if (custInfo.containsKey(SysDict.A.cust_status.getId()) && CommUtil.isNotNull(custInfo.get(SysDict.A.cust_status.getId()))) {
			custStatus = CommUtil.toEnum(E_CUSTSTATUS.class, custInfo.get(SysDict.A.cust_status.getId()).toString());
		}

		if (custStatus == E_CUSTSTATUS.CANCEL) {
			throw DpErr.Dp.E0041(custNo);
		}

		// 客户状态检查
		if (custStatus != E_CUSTSTATUS.NORMAL) {

			throw DpErr.Dp.E0041(custNo);
		}

		// 客户证件检查
		// if (CommUtil.compare(customerInfo.getDoc_effe_date(), trxnDate) > 0
		// || CommUtil.compare(trxnDate, customerInfo.getDoc_expy_date()) > 0) {
		// 客户证件不在有效期范围内[xxx-xxx],当前交易日期[xxx]
		// DpErr.Dp.E0383(customerInfo.getDoc_effe_date(),
		// customerInfo.getDoc_expy_date(), trxnDate);
		// }

		// 个人开户要求账户户名与客户名一致
		/*
		 * if (E_CUSTOMERTYPE.PERSONAL == acctType.getCust_type() &&
		 * CommUtil.isNotNull(acctName)) {
		 * 
		 * if (!CommUtil.equals(customerInfo.getCust_name(), acctName)) { throw
		 * DpErr.Dp.E0045(acctType.getCust_type()); } }
		 */
		bizlog.method(" DpOpenAccountCheck.checkCustInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月7日-上午11:51:39</li>
	 *         <li>功能说明：校验输入值是否合法</li>
	 *         </p>
	 * @param cplIn
	 *            开户检查输入接口
	 */
	private static void defaultValue(DpOpenAccountIn cplIn) {

		// 联名人标志默认为“否”
		if (cplIn.getJoint_acct_ind() == null) {
			cplIn.setJoint_acct_ind(E_YESORNO.NO);
		}

		// 自选号码标志默认为“不选号”
		if (cplIn.getSelf_opt_number_ind() == null) {
			cplIn.setSelf_opt_number_ind(E_SELFOPTNUMBERIND.NOT_OPTIONAL);
		}

	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月7日-上午11:51:39</li>
	 *         <li>功能说明：校验输入值是否合法</li>
	 *         </p>
	 * @param cplIn
	 *            开户检查输入接口
	 * @param acctType
	 *            账户类型
	 */
	private static void checkInputData(DpOpenAccountIn cplIn) {

		// 账户类型不能为空
		BizUtil.fieldNotNull(cplIn.getAcct_type(), SysDict.A.acct_type.getId(), SysDict.A.acct_type.getLongName());

		// 客户号不能为空
		BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

		// 联名账户标志为YES时，检查客户信息数量
		if (cplIn.getJoint_acct_ind() == E_YESORNO.YES) {

			// 联名客户信息需要输入至少一条联名账户联名信息
			if (CommUtil.isNull(cplIn.getList_joint_cust()) || cplIn.getList_joint_cust().size() < 1) {
				throw DpErr.Dp.E0180();
			}
		}

		if (cplIn.getSelf_opt_number_ind() == E_SELFOPTNUMBERIND.OPTIONAL_SERIAL_NUMBER) {

			// 自选序号不能为空
			BizUtil.fieldNotNull(cplIn.getSelf_opt_number(), DpDict.A.self_opt_number.getId(), DpDict.A.self_opt_number.getLongName());

			// 自选序号最大长度超过序号定义长度
			if (cplIn.getSelf_opt_number().length() > ApBusinessParmApi.getIntValue("OPTIONAL_SERIAL_NUMBER_LENGTH")) {

				throw DpErr.Dp.E0173(Long.valueOf(ApBusinessParmApi.getValue("OPTIONAL_SERIAL_NUMBER_LENGTH")));
			}
		}
	}
}
