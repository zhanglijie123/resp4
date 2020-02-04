package cn.sunline.icore.dp.serv.maintain;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DATAOPERATE;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpbJointAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpbJointAccountDao;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_SIGNATURE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpJointAcctInfoMntIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpJointAcctInfoMntOut;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpJointAcctInfoMntSubIn;
import cn.sunline.icore.dp.serv.type.ComDpMaintainAcct.DpJointAcctInfoMntSubOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustBaseInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustSimpleInfo;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 联名账户联名信息维护
 * </p>
 * 
 * @Author linshiq
 *         <p>
 *         <li>2017年3月23日-下午3:46:26</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月23日-linshiq：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpJointAccountMaintain {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpJointAccountMaintain.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年3月23日-下午3:47:16</li>
	 *         <li>功能说明： 联名账户联名信息维护主入口</li>
	 *         </p>
	 * @param cplIn
	 *            联名账户联名信息维护输入
	 * @return 联名账户联名信息维护输出
	 */
	public static DpJointAcctInfoMntOut jointAcctInfoMnt(DpJointAcctInfoMntIn cplIn) {
		bizlog.method(" DpJointAccountMaintain.jointAcctInfoMnt begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 输入数据检查(并返回需要操作的账户)
		DpaAccount acctInfo = checkJointAcctInfoMntInput(cplIn);

		// 初始化输出接口
		DpJointAcctInfoMntOut cplOut = BizUtil.getInstance(DpJointAcctInfoMntOut.class);

		// 循环维护
		for (DpJointAcctInfoMntSubIn info : cplIn.getList01()) {

			// 检查子列表信息(并返回待处理客户号)
			DpCustBaseInfo custInfo = checkJointAcctInfoMntListInput(info);

			// 初始化子输出接口
			DpJointAcctInfoMntSubOut listCplOut = BizUtil.getInstance(DpJointAcctInfoMntSubOut.class);

			if (info.getOperater_ind() == E_DATAOPERATE.ADD) {// 新增

				addJointAcctInfo(info, custInfo, acctInfo);

				// 相关输出
				listCplOut.setCust_no(custInfo.getCust_no()); // 客户号
				listCplOut.setCust_name(custInfo.getCust_name()); // 客户名称
				listCplOut.setDoc_type(custInfo.getDoc_type()); // 证件类型
				listCplOut.setDoc_no(custInfo.getDoc_no()); // 证件号码
				listCplOut.setSignature_ind(info.getSignature_ind()); // 独立签名指示
				listCplOut.setJoint_person_relationship(info.getJoint_person_relationship()); // 联系人名关系
				listCplOut.setData_version(1l); // TODO:数据版本号
			}
			else if (info.getOperater_ind() == E_DATAOPERATE.MODIFY) {// 维护

				// 数据版本号不可为NULL
				BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

				// 校验数据版本号
				if (CommUtil.compare(acctInfo.getData_version(), cplIn.getData_version()) != 0) {

					throw ApPubErr.APPUB.E0018(DpaAccount.class.getName());
				}

				DpbJointAccount jointAcctInfo = modifyJointAcctInfo(info, custInfo, acctInfo);

				// 相关输出
				listCplOut.setCust_no(jointAcctInfo.getCust_no()); // 客户号
				listCplOut.setCust_name(custInfo.getCust_name()); // 客户名称
				listCplOut.setDoc_type(custInfo.getDoc_type()); // 证件类型
				listCplOut.setDoc_no(custInfo.getDoc_no()); // 证件号码
				listCplOut.setSignature_ind(jointAcctInfo.getSignature_ind()); // 独立签名指示
				listCplOut.setJoint_person_relationship(jointAcctInfo.getJoint_person_relationship()); // 联系人名关系
				listCplOut.setData_version(jointAcctInfo.getData_version()); // 数据版本号
			}
			else if (info.getOperater_ind() == E_DATAOPERATE.DELETE) {// 删除

				// 数据版本号不可为NULL
				BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

				// 校验数据版本号
				if (CommUtil.compare(acctInfo.getData_version(), cplIn.getData_version()) != 0) {

					throw ApPubErr.APPUB.E0018(DpaAccount.class.getName());
				}

				DpbJointAccount jointAcctInfo = deleteJointAcctInfo(info, custInfo, acctInfo);

				// 相关输出
				listCplOut.setCust_no(jointAcctInfo.getCust_no()); // 客户号
				listCplOut.setCust_name(custInfo.getCust_name()); // 客户名称
				listCplOut.setDoc_type(custInfo.getDoc_type()); // 证件类型
				listCplOut.setDoc_no(custInfo.getDoc_no()); // 证件号码
				listCplOut.setSignature_ind(jointAcctInfo.getSignature_ind()); // 独立签名指示
				listCplOut.setJoint_person_relationship(jointAcctInfo.getJoint_person_relationship()); // 联系人名关系
				listCplOut.setData_version(jointAcctInfo.getData_version()); // 数据版本号
			}
			else {
				throw APPUB.E0026(SysDict.A.operater_ind.getLongName(), info.getOperater_ind().getValue());
			}

			cplOut.getList01().add(listCplOut);
		}

		cplOut.setAcct_no(acctInfo.getAcct_no());// 账号
		cplOut.setAcct_name(acctInfo.getAcct_name());// 账号名

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpJointAccountMaintain.jointAcctInfoMnt end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年3月24日-下午1:39:36</li>
	 *         <li>功能说明：检查子列表信息(并返回待处理客户号)</li>
	 *         </p>
	 * @param info
	 *            联名账户联名信息维护子输入
	 * @return 客户基本信息查询输出
	 */
	private static DpCustBaseInfo checkJointAcctInfoMntListInput(DpJointAcctInfoMntSubIn info) {
		bizlog.method(" DpJointAccountMaintain.checkJointAcctInfoMntListInput begin >>>>>>>>>>>>>>>>");

		// 操作标志不可为NULL
		BizUtil.fieldNotNull(info.getOperater_ind(), SysDict.A.operater_ind.getId(), SysDict.A.operater_ind.getLongName());

		// 客户号不可为NULL
		BizUtil.fieldNotNull(info.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

		// 联名人关系不为NULL校验下拉字典合法性
		if (CommUtil.isNotNull(info.getJoint_person_relationship())) {

			ApDropListApi.exists(DpConst.JOINT_PERSON_RELATIONSHIP, info.getJoint_person_relationship());
		}

		DpCustBaseInfo custInfo = DpCustomerIobus.getCustBaseInfo(info.getCust_no());

		bizlog.method(" DpJointAccountMaintain.checkJointAcctInfoMntListInput end <<<<<<<<<<<<<<<<");
		return custInfo;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年3月24日-上午10:12:27</li>
	 *         <li>功能说明：联名账户联名信息新增</li>
	 *         </p>
	 * @param info
	 *            联名账户联名信息维护子输入
	 * @param custInfo
	 *            客户基本信息查询输出
	 * @param acctInfo
	 *            存款账户表
	 */
	private static void addJointAcctInfo(DpJointAcctInfoMntSubIn info, DpCustBaseInfo custInfo, DpaAccount acctInfo) {
		bizlog.method(" DpJointAccountMaintain.addJointAcctInfo begin >>>>>>>>>>>>>>>>");

		// 独立签名指示不可为NULL
		// BizUtil.fieldNotNull(info.getSignature_ind(),
		// DpBaseDict.A.signature_ind.getId(),
		// DpBaseDict.A.signature_ind.getLongName());

		// 检查主信息是否已经插入(数据库中只允许存在一条)
		DpbJointAccount jointAcctInfo = DpbJointAccountDao.selectOne_odb1(acctInfo.getAcct_no(), acctInfo.getCust_no(), false);

		// 为空则进行插入
		if (jointAcctInfo == null) {

			DpCustSimpleInfo custMainInfo = DpCustomerIobus.getCustSimpleInfo(acctInfo.getCust_no(), acctInfo.getCust_type());

			// 初始化主信息
			jointAcctInfo = BizUtil.getInstance(DpbJointAccount.class);

			jointAcctInfo.setAcct_no(acctInfo.getAcct_no()); // 账号
			jointAcctInfo.setCust_no(custMainInfo.getCust_no()); // 客户号
			jointAcctInfo.setJoint_primary_cust_ind(E_YESORNO.YES); // 联名主客户标志

			jointAcctInfo.setSignature_ind(CommUtil.nvl(info.getSignature_ind(), E_SIGNATURE.SOLELY)); // 独立签名指示
			jointAcctInfo.setJoint_person_relationship(info.getJoint_person_relationship()); // 联系人名关系

			// 插入
			DpbJointAccountDao.insert(jointAcctInfo);
		}
		else if (CommUtil.equals(jointAcctInfo.getCust_no(), custInfo.getCust_no())) {

			// 检查是否为主客户号,主客户号只允许维护不可进行新增(如果已经存在一条)
			throw DpErr.Dp.E0176();
		}

		// 新增客户联名信息(已经存在的联名信息不可再次插入,所以需要捕捉异常)
		try {

			// 初始化客信息
			jointAcctInfo = BizUtil.getInstance(DpbJointAccount.class);

			jointAcctInfo.setAcct_no(acctInfo.getAcct_no()); // 账号
			jointAcctInfo.setCust_no(custInfo.getCust_no()); // 客户号
			jointAcctInfo.setJoint_primary_cust_ind(E_YESORNO.NO); // 联名主客户标志
			jointAcctInfo.setSignature_ind(CommUtil.nvl(info.getSignature_ind(), E_SIGNATURE.SOLELY)); // 独立签名指示
			jointAcctInfo.setJoint_person_relationship(info.getJoint_person_relationship()); // 联系人名关系

			// 插入
			DpbJointAccountDao.insert(jointAcctInfo);
		}
		catch (Exception e) {

			bizlog.error("addJointAcctInfo-Exception=[%s]", e, e.getMessage());

			throw APPUB.E0032(OdbFactory.getTable(DpbJointAccount.class).getLongname(), acctInfo.getAcct_no(), custInfo.getCust_no());
		}

		bizlog.method(" DpJointAccountMaintain.addJointAcctInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年3月24日-上午10:58:29</li>
	 *         <li>功能说明：联名账户联名信息更新</li>
	 *         </p>
	 * @param info
	 *            联名账户联名信息维护子输入
	 * @param custInfo
	 *            客户基本信息查询输出
	 * @param acctInfo
	 *            存款账户表
	 * @return 存款联名账户登记簿
	 */
	private static DpbJointAccount modifyJointAcctInfo(DpJointAcctInfoMntSubIn info, DpCustBaseInfo custInfo, DpaAccount acctInfo) {
		bizlog.method(" DpJointAccountMaintain.modifyJointAcctInfo begin >>>>>>>>>>>>>>>>");

		// 数据版本号不可为NULL
		BizUtil.fieldNotNull(info.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

		// 查询需维护的信息
		DpbJointAccount jointAcctInfo = DpbJointAccountDao.selectOne_odb1(acctInfo.getAcct_no(), custInfo.getCust_no(), false);

		if (jointAcctInfo == null) {

			throw APPUB.E0024(OdbFactory.getTable(DpbJointAccount.class).getLongname(), SysDict.A.acct_no.getId(), acctInfo.getAcct_no(), SysDict.A.cust_no.getId(),
					custInfo.getCust_no());
		}

		// 校验数据版本号
		if (CommUtil.compare(jointAcctInfo.getData_version(), info.getData_version()) != 0) {

			throw ApPubErr.APPUB.E0018(DpbJointAccount.class.getName());
		}

		// 复制旧数据
		DpbJointAccount oldJointAcctInfo = BizUtil.clone(DpbJointAccount.class, jointAcctInfo);

		// 检查是否是维护主客户号信息(主客户号信息只允许维护独立签名指示)
		if (CommUtil.equals(jointAcctInfo.getCust_no(), acctInfo.getCust_no())) {

			// 联名人关系不为空,则检验
			if (CommUtil.isNotNull(info.getJoint_person_relationship()) && !CommUtil.equals(info.getJoint_person_relationship(), jointAcctInfo.getJoint_person_relationship())) {

				throw DpErr.Dp.E0179(info.getJoint_person_relationship(), jointAcctInfo.getJoint_person_relationship());
			}

			// 独立签名指示不可为NULL
			BizUtil.fieldNotNull(info.getSignature_ind(), DpBaseDict.A.signature_ind.getId(), DpBaseDict.A.signature_ind.getLongName());

			jointAcctInfo.setSignature_ind(info.getSignature_ind());// 独立签名指示

		}
		else {// 非主客户号维护

			jointAcctInfo.setCust_no(custInfo.getCust_no()); // 客户号
			jointAcctInfo.setSignature_ind(CommUtil.nvl(info.getSignature_ind(), oldJointAcctInfo.getSignature_ind())); // 独立签名指示
			jointAcctInfo.setJoint_person_relationship(CommUtil.nvl(info.getJoint_person_relationship(), oldJointAcctInfo.getJoint_person_relationship())); // 联系人名关系
		}

		// 登记审计
		if (0 != ApDataAuditApi.regLogOnUpdateParameter(oldJointAcctInfo, jointAcctInfo)) {
			DpbJointAccountDao.updateOne_odb1(jointAcctInfo);
		}

		// 更新
		bizlog.method(" DpJointAccountMaintain.modifyJointAcctInfo end <<<<<<<<<<<<<<<<");
		return jointAcctInfo;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年3月24日-上午11:01:38</li>
	 *         <li>功能说明：联名账户联名信息删除</li>
	 *         </p>
	 * @param info
	 *            联名账户联名信息维护子输入
	 * @param custInfo
	 *            客户基本信息查询输出
	 * @param acctInfo
	 *            存款账户表
	 * @return 存款联名账户登记簿
	 */
	private static DpbJointAccount deleteJointAcctInfo(DpJointAcctInfoMntSubIn info, DpCustBaseInfo custInfo, DpaAccount acctInfo) {
		bizlog.method(" DpJointAccountMaintain.deleteJointAcctInfo begin >>>>>>>>>>>>>>>>");

		// 数据版本号不可为NULL
		BizUtil.fieldNotNull(info.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

		// 查询需维护的信息
		DpbJointAccount jointAcctInfo = DpbJointAccountDao.selectOne_odb1(acctInfo.getAcct_no(), custInfo.getCust_no(), false);

		if (jointAcctInfo == null) {

			throw APPUB.E0024(OdbFactory.getTable(DpbJointAccount.class).getLongname(), SysDict.A.acct_no.getId(), acctInfo.getAcct_no(), SysDict.A.cust_no.getId(),
					custInfo.getCust_no());
		}

		// 校验数据版本号
		if (CommUtil.compare(jointAcctInfo.getData_version(), info.getData_version()) != 0) {

			throw ApPubErr.APPUB.E0018(DpbJointAccount.class.getName());
		}

		// 检查是否是维护主客户号信息(主客户号信息不允许删除)
		if (CommUtil.equals(jointAcctInfo.getCust_no(), acctInfo.getCust_no())) {

			throw DpErr.Dp.E0177();
		}

		// 签名指示不为空,则检验
		if (CommUtil.isNotNull(info.getSignature_ind()) && info.getSignature_ind() != jointAcctInfo.getSignature_ind()) {

			throw DpErr.Dp.E0178(info.getSignature_ind(), jointAcctInfo.getSignature_ind());
		}

		// 联名人关系不为空,则检验
		if (CommUtil.isNotNull(info.getJoint_person_relationship()) && !CommUtil.equals(info.getJoint_person_relationship(), jointAcctInfo.getJoint_person_relationship())) {

			throw DpErr.Dp.E0179(info.getJoint_person_relationship(), jointAcctInfo.getJoint_person_relationship());
		}

		// 复制旧数据
		DpbJointAccount oldJointAcctInfo = BizUtil.clone(DpbJointAccount.class, jointAcctInfo);

		// 登记审计
		ApDataAuditApi.regLogOnDeleteParameter(jointAcctInfo);

		// 删除
		DpbJointAccountDao.deleteOne_odb1(jointAcctInfo.getAcct_no(), jointAcctInfo.getCust_no());

		bizlog.method(" DpJointAccountMaintain.deleteJointAcctInfo end <<<<<<<<<<<<<<<<");
		return oldJointAcctInfo;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年3月23日-下午7:28:20</li>
	 *         <li>功能说明：联名账户联名信息维护输入数据检查</li>
	 *         </p>
	 * @param cplIn
	 *            联名账户联名信息维护输入
	 * @return 存款账户表
	 */
	private static DpaAccount checkJointAcctInfoMntInput(DpJointAcctInfoMntIn cplIn) {
		bizlog.method(" DpJointAccountMaintain.checkJointAcctInfoMntInput begin >>>>>>>>>>>>>>>>");

		// 账号不可为NULL
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 验密标志不可为NULL
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		// 定位客户账户
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 验证密码 ：验密标志 = Y 时，则需要校验账户的支取方式
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());

			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 验证账户名称：账户名有值时验证其与数据库记录一致性
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(acctInfo.getAcct_name(), cplIn.getAcct_name())) {

			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctInfo.getAcct_name());
		}

		// 检查账号是否允许联名
		if (acctInfo.getJoint_acct_ind() == E_YESORNO.NO) {

			throw DpBase.E0018(DpSysDict.A.joint_acct_ind.getLongName());
		}

		// 联名人信息不可为null
		if (cplIn.getList01() == null || cplIn.getList01().size() <= 0) {

			throw DpErr.Dp.E0180();
		}

		bizlog.method(" DpJointAccountMaintain.checkJointAcctInfoMntInput end <<<<<<<<<<<<<<<<");
		;
		return acctInfo;
	}
}
