package cn.sunline.icore.dp.serv.froze;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApLimitApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpFrozeParmApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.tables.TabDpFrozeParameter.DppFrozeType;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeBaseIn;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeObjectBase;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_FROZESOURCE;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeObjectIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeObjectOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpMultiFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpMultiFrozeOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZEOBJECT;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 冻结止付相关处理
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月10日-下午2:05:59</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月10日-zhoumy：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpMultipleFroze {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpMultipleFroze.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月10日-下午2:06:43</li>
	 *         <li>功能说明：冻结止付主处理服务</li>
	 *         </p>
	 * @param cplIn
	 *            冻结止付服务输入接口
	 * @return DpMultiFrozeOut 冻结止付服务输出接口
	 */
	public static DpMultiFrozeOut doMain(DpMultiFrozeIn cplIn) {

		bizlog.method(" DpMultipleFroze.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 检查主调方法
		DppFrozeType frozeTypeDef = checkMain(cplIn);

		// 取得冻结编号
		String sFrozeNo = DpFrozePublic.getFrozeNo(frozeTypeDef);

		// 输出
		DpMultiFrozeOut cplOut = BizUtil.getInstance(DpMultiFrozeOut.class);

		// 记录中记录数
		long totalCount = 0;

		for (DpFrozeObjectIn frozeObjectIn : cplIn.getList_froze_object()) {

			// 需要再次添加冻结对象数据区，因为检查服务中多条记录循环时，后者会覆盖前者
			DpFrozePublic.addFrozeObjectDataBuffer(frozeObjectIn);

			// 收费试算+扣费
			DpFrozePublic.autoChrg(frozeObjectIn.getAcct_no(), frozeObjectIn.getCcy_code(), frozeObjectIn.getFroze_amt(), E_DEPTTRXNEVENT.DP_FROZE);

			// 接口转换
			DpFrozeBaseIn cplFrozeBaseIn = switchInterface(cplIn, frozeObjectIn);

			// 登记冻结台账信息
			DpbFroze frozeInfo = DpBaseServiceApi.registerFrozeAccount(cplFrozeBaseIn, frozeObjectIn, sFrozeNo);

			// 限额处理
			ApLimitApi.process(E_DEPTTRXNEVENT.DP_FROZE.getValue(), frozeObjectIn.getCcy_code(), frozeObjectIn.getFroze_amt());

			// 账户信息查询
			DpaAccount acctInfo = DpToolsApi.accountInquery(frozeObjectIn.getAcct_no(), null);

			// 准备冻结输出对象信息
			DpFrozeObjectOut frozeObjectOut = BizUtil.getInstance(DpFrozeObjectOut.class);

			frozeObjectOut.setCust_no(frozeInfo.getCust_no()); // 客户号
			frozeObjectOut.setCard_no(frozeInfo.getCard_no()); // 卡号
			frozeObjectOut.setAcct_no(frozeInfo.getAcct_no()); // 账号
			frozeObjectOut.setAcct_type(frozeObjectIn.getAcct_type()); // 账户类型
			frozeObjectOut.setSub_acct_seq(frozeObjectIn.getSub_acct_seq()); // 子账户序号
			frozeObjectOut.setCcy_code(frozeInfo.getCcy_code()); // 货币代码
			frozeObjectOut.setProd_id(frozeObjectIn.getProd_id()); // 产品编号
			frozeObjectOut.setFroze_reason(frozeInfo.getFroze_reason()); // 冻结原因
			frozeObjectOut.setFroze_due_date(frozeInfo.getFroze_due_date()); // 冻结到期日
			frozeObjectOut.setFroze_amt(frozeInfo.getFroze_bal());// 冻结金额
			frozeObjectOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称

			cplOut.getList_froze_object().add(frozeObjectOut);

			totalCount++;
		}

		cplOut.setFroze_no(sFrozeNo); // 冻结编号
		cplOut.setFroze_kind_code(frozeTypeDef.getFroze_kind_code()); // 冻结分类码
		cplOut.setFroze_kind_name(frozeTypeDef.getFroze_kind_name()); // 冻结分类名称
		cplOut.setFroze_source(frozeTypeDef.getFroze_source()); // 冻结来源
		cplOut.setFroze_type(frozeTypeDef.getFroze_type()); // 冻结类型
		cplOut.setOver_limit_froze_ind(frozeTypeDef.getOver_limit_froze_ind()); // 超额冻结许可标志
		cplOut.setPrior_level(frozeTypeDef.getPrior_level()); // 优先级
		cplOut.setExtend_froze_allow_ind(frozeTypeDef.getExtend_froze_allow_ind()); // 续冻许可标志

		BizUtil.getTrxRunEnvs().setTotal_count(totalCount);// 返回总记录数

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method("DpMultipleFroze.doMain end <<<<<<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月10日-下午2:31:35</li>
	 *         <li>功能说明：冻结止付主检查服务</li>
	 *         </p>
	 * @param cplIn
	 *            冻结止付服务输入接口
	 */
	public static DppFrozeType checkMain(DpMultiFrozeIn cplIn) {

		bizlog.method("DpMultipleFroze.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 冻结止付输入接口公用检查
		DppFrozeType frozeTypeDef = checkInputData(cplIn);

		// 加载参数数据区
		ApBufferApi.addData(ApConst.PARM_DATA_MART, CommUtil.toMap(frozeTypeDef));

		// 加载输入数据区
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		// 循环检查冻结对象输入
		for (DpFrozeObjectIn frozeObjectIn : cplIn.getList_froze_object()) {

			// 检查冻结对象信息并获得对象基本信息
			DpFrozeObjectBase cplObjectBase = getFrozeObejctBase(frozeObjectIn, cplIn.getFroze_object_type());

			// 添加冻结对象数据缓存区
			DpFrozePublic.addFrozeObjectDataBuffer(cplObjectBase);

			// 接口转换
			DpFrozeBaseIn cplFrozeBaseIn = switchInterface(cplIn, frozeObjectIn);

			// 冻结许可检查
			DpBaseServiceApi.checkFrozeLicense(cplFrozeBaseIn, cplObjectBase);

			// 交易控制检查: 包括业务规则、属性检查
			ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_FROZE.getValue());
		}

		bizlog.method("DpMultipleFroze.checkMain end <<<<<<<<<<<<<<<<<<<<");

		return frozeTypeDef;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月13日-上午11:24:32</li>
	 *         <li>功能说明：冻结止付输入接口公用检查</li>
	 *         </p>
	 * @param cplIn
	 *            冻结止付服务输入接口
	 */
	private static DppFrozeType checkInputData(DpMultiFrozeIn cplIn) {

		bizlog.method(" DpMultipleFroze.checkInputData begin >>>>>>>>>>>>>>>>");

		// 冻结分类码不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_kind_code(), SysDict.A.froze_kind_code.getId(), SysDict.A.froze_kind_code.getLongName());

		// 冻结对象类型不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_object_type(), DpSysDict.A.froze_object_type.getId(), DpSysDict.A.froze_object_type.getLongName());

		// 冻结对象列表不可为null
		if (cplIn.getList_froze_object() == null || cplIn.getList_froze_object().size() == 0) {

			throw DpErr.Dp.E0102();
		}

		// 循环检查冻结对象输入
		for (DpFrozeObjectIn frozeObjectIn : cplIn.getList_froze_object()) {

			// 冻结原因不能为空
			BizUtil.fieldNotNull(frozeObjectIn.getFroze_reason(), SysDict.A.froze_reason.getId(), SysDict.A.froze_reason.getLongName());

			// 子账户层冻结，账号不能为空
			if (cplIn.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {

				BizUtil.fieldNotNull(frozeObjectIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
			}
			else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.ACCT) {

				BizUtil.fieldNotNull(frozeObjectIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
			}
			else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.CARD) {

				BizUtil.fieldNotNull(frozeObjectIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
			}
			else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.CUST) {

				BizUtil.fieldNotNull(frozeObjectIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());
			}
		}

		// 查询冻结类型定义表 , 获取冻结分类码定义
		DppFrozeType frozeTypeDef = DpFrozeParmApi.getFrozeTypeInfo(cplIn.getFroze_kind_code());

		// 司法冻结检查司法信息
		if (frozeTypeDef.getFroze_source() == E_FROZESOURCE.EXTERNAL) {

			DpFrozePublic.checkLawFroze(cplIn);
		}

		bizlog.method(" DpMultipleFroze.checkInputData end <<<<<<<<<<<<<<<<");
		return frozeTypeDef;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月18日-上午11:24:32</li>
	 *         <li>功能说明：获得冻结对象基础信息</li>
	 *         </p>
	 * @param cplIn
	 *            冻结对象信息
	 * @param frozeObjectType
	 *            冻结对象类型
	 * @return 冻结对象基础信息
	 */
	private static DpFrozeObjectBase getFrozeObejctBase(DpFrozeObjectIn cplIn, E_FROZEOBJECT frozeObjectType) {

		DpFrozeObjectBase cplObjectBase = BizUtil.getInstance(DpFrozeObjectBase.class);

		// 客户冻结
		if (frozeObjectType == E_FROZEOBJECT.SUBACCT) {

			// 初始化定位子账号信息
			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(cplIn.getAcct_no()); // 账号
			accessIn.setSub_acct_seq(cplIn.getSub_acct_seq()); // 子账户序号
			accessIn.setCcy_code(cplIn.getCcy_code()); // 货币代码
			accessIn.setAcct_type(cplIn.getAcct_type()); // 账户类型
			accessIn.setProd_id(cplIn.getProd_id()); // 产品编号
			accessIn.setDd_td_ind(null); // 定活标志
			accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL); // 存入支取标志

			// 定位子账号
			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			cplObjectBase.setSub_acct_no(accessOut.getSub_acct_no());
			cplObjectBase.setAcct_no(accessOut.getAcct_no());
			cplObjectBase.setCard_no(accessOut.getCard_no());
			cplObjectBase.setCust_no(accessOut.getCust_no());
			cplObjectBase.setCust_type(accessOut.getCust_type());
		}
		else if (frozeObjectType == E_FROZEOBJECT.ACCT) {

			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

			cplObjectBase.setSub_acct_no(null);
			cplObjectBase.setAcct_no(acctInfo.getAcct_no());
			cplObjectBase.setCard_no(null);
			cplObjectBase.setCust_no(acctInfo.getCust_no());
			cplObjectBase.setCust_type(acctInfo.getCust_type());
		}
		else if (frozeObjectType == E_FROZEOBJECT.CARD) {

			DpaCard cardInfo = DpToolsApi.getCardInfor(cplIn.getAcct_no(), true);

			cplObjectBase.setSub_acct_no(null);
			cplObjectBase.setAcct_no(cardInfo.getAcct_no());
			cplObjectBase.setCard_no(cardInfo.getCard_no());
			cplObjectBase.setCust_no(cardInfo.getCust_no());
			cplObjectBase.setCust_type(cardInfo.getCust_type());
		}
		else if (frozeObjectType == E_FROZEOBJECT.CUST) {

			E_YESORNO existsFlag = DpCustomerIobus.existsCustomer(cplIn.getCust_no());

			if (existsFlag == E_YESORNO.NO) {

				throw DpBase.E0184(cplIn.getCust_no());
			}

			cplObjectBase.setSub_acct_no(null);
			cplObjectBase.setAcct_no(null);
			cplObjectBase.setCard_no(null);
			cplObjectBase.setCust_no(cplIn.getCust_no());
			cplObjectBase.setCust_type(cplIn.getCust_type());
		}
		else {
			throw APPUB.E0026(DpSysDict.A.froze_object_type.getLongName(), frozeObjectType.getValue());
		}

		// 将冻结对象基本信息反写到子类中，这样可以避免在domain服务中不用第二次调用这些对象查询定位程序，提高代码效率
		cplIn.setAcct_no(cplObjectBase.getAcct_no());
		cplIn.setCard_no(cplObjectBase.getCard_no());
		cplIn.setCust_no(cplObjectBase.getCust_no());
		cplIn.setCust_type(cplObjectBase.getCust_type());
		cplIn.setSub_acct_no(cplObjectBase.getSub_acct_no());

		return cplObjectBase;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月7日-下午3:18:16</li>
	 *         <li>功能说明：转换接口</li>
	 *         </p>
	 * @param cplIn
	 *            多个对象冻结输入
	 * @param cplObjIn
	 *            冻结对象
	 */
	private static DpFrozeBaseIn switchInterface(DpMultiFrozeIn cplIn, DpFrozeObjectIn cplObjIn) {

		DpFrozeBaseIn cplOut = BizUtil.getInstance(DpFrozeBaseIn.class);

		cplOut.setTotal_count((long) cplIn.getList_froze_object().size());
		cplOut.setFroze_due_date(cplObjIn.getFroze_due_date());
		cplOut.setCcy_code(cplObjIn.getCcy_code());
		cplOut.setFroze_amt(cplObjIn.getFroze_amt());
		cplOut.setFroze_before_save_amt(cplObjIn.getFroze_before_save_amt());
		cplOut.setFroze_feature_code(cplObjIn.getFroze_feature_code());
		cplOut.setFroze_kind_code(cplIn.getFroze_kind_code());
		cplOut.setFroze_object_type(cplIn.getFroze_object_type());
		cplOut.setFroze_term(cplObjIn.getFroze_term());
		cplOut.setRemark(cplIn.getRemark());
		cplOut.setFroze_reason(cplObjIn.getFroze_reason());
		cplOut.setEnforced_legal_dept(cplIn.getEnforced_legal_dept());
		cplOut.setEnforced_legal_dept_name(cplIn.getEnforced_legal_dept_name());
		cplOut.setLegal_notice_no(cplIn.getLegal_notice_no());
		cplOut.setLegal_notice_type(cplIn.getLegal_notice_type());
		cplOut.setOfficer2_doc_no(cplIn.getOfficer2_doc_no());
		cplOut.setOfficer2_doc_type(cplIn.getOfficer2_doc_type());
		cplOut.setOfficer2_name(cplIn.getOfficer2_name());
		cplOut.setOfficer2_phone(cplIn.getOfficer2_phone());
		cplOut.setOfficer_doc_no(cplIn.getOfficer_doc_no());
		cplOut.setOfficer_doc_type(cplIn.getOfficer_doc_type());
		cplOut.setOfficer_name(cplIn.getOfficer_name());
		cplOut.setOfficer_phone(cplIn.getOfficer_phone());

		return cplOut;
	}
}
