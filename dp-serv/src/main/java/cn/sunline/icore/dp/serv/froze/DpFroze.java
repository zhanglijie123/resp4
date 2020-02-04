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
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.tables.TabDpFrozeParameter.DppFrozeType;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeBaseIn;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeObjectBase;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_FROZESOURCE;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpFrozeOut;
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
public class DpFroze {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpFroze.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月10日-下午2:06:43</li>
	 *         <li>功能说明：冻结止付主处理服务</li>
	 *         </p>
	 * @param cplIn
	 *            冻结止付服务输入接口
	 * @return DpFrozeOut 冻结止付服务输出接口
	 */
	public static DpFrozeOut doMain(DpFrozeIn cplIn) {

		bizlog.method(" DpFroze.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);
		
		// 检查主调方法
		DpFrozeObjectBase cplObjectBase = checkMain(cplIn);

		// 冻结分类码参数
		DppFrozeType frozeTypeDef = DpFrozeParmApi.getFrozeTypeInfo(cplIn.getFroze_kind_code());

		// 取得冻结编号
		String sFrozeNo = DpFrozePublic.getFrozeNo(frozeTypeDef);

		// 收费试算+扣费
		DpFrozePublic.autoChrg(cplIn.getAcct_no(), cplIn.getCcy_code(), cplIn.getFroze_amt(), E_DEPTTRXNEVENT.DP_FROZE);

		// 接口转换
		DpFrozeBaseIn cplFrozeBase = switchInterface(cplIn);

		// 登记冻结台账信息
		DpbFroze frozeInfo = DpBaseServiceApi.registerFrozeAccount(cplFrozeBase, cplObjectBase, sFrozeNo);

		// 限额处理
		ApLimitApi.process(E_DEPTTRXNEVENT.DP_FROZE.getValue(), cplIn.getCcy_code(), cplIn.getFroze_amt());

		// 输出
		DpFrozeOut cplOut = BizUtil.getInstance(DpFrozeOut.class);

		cplOut.setFroze_no(sFrozeNo);
		cplOut.setAcct_name(cplIn.getAcct_name());
		cplOut.setCust_no(frozeInfo.getCust_no());
		cplOut.setCust_type(frozeInfo.getCust_type());
		cplOut.setFroze_bal(frozeInfo.getFroze_bal());
		cplOut.setFroze_due_date(frozeInfo.getFroze_due_date());
		cplOut.setFroze_kind_code(frozeInfo.getFroze_kind_code());
		cplOut.setFroze_kind_name(frozeTypeDef.getFroze_kind_name());
		cplOut.setFroze_type(frozeInfo.getFroze_type());
		cplOut.setOver_limit_froze_ind(frozeTypeDef.getOver_limit_froze_ind());
		cplOut.setPrior_level(frozeInfo.getPrior_level());
		cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method("DpFroze.doMain end <<<<<<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年1月10日-下午2:31:35</li>
	 *         <li>功能说明：冻结止付主检查服务</li>
	 *         </p>
	 * @param cplIn
	 *            冻结止付服务输入接口
	 */
	public static DpFrozeObjectBase checkMain(DpFrozeIn cplIn) {

		bizlog.method("DpFroze.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 冻结止付输入接口公用检查
		checkInputData(cplIn);

		// 检查冻结对象合法性并获得冻结对象基础信息
		DpFrozeObjectBase cplObjectBase = getFrozeObejctBase(cplIn);

		// 添加规则数据缓存
		addDataBuffer(cplIn, cplObjectBase);

		// 接口转换
		DpFrozeBaseIn cplFrozeBase = switchInterface(cplIn);

		// 冻结许可检查
		DpBaseServiceApi.checkFrozeLicense(cplFrozeBase, cplObjectBase);

		// 交易控制检查: 包括业务规则、属性检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_FROZE.getValue());

		bizlog.method("DpFroze.checkMain end <<<<<<<<<<<<<<<<<<<<");

		return cplObjectBase;
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
	private static void checkInputData(DpFrozeIn cplIn) {

		bizlog.method(" DpFroze.checkInputData begin >>>>>>>>>>>>>>>>");

		// 冻结分类码不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_kind_code(), SysDict.A.froze_kind_code.getId(), SysDict.A.froze_kind_code.getLongName());

		// 冻结对象类型不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_object_type(), DpSysDict.A.froze_object_type.getId(), DpSysDict.A.froze_object_type.getLongName());

		// 冻结原因不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_reason(), SysDict.A.froze_reason.getId(), SysDict.A.froze_reason.getLongName());

		// 子账户层冻结，账号不能为空
		if (cplIn.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {

			BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		}
		else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.ACCT) {

			BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		}
		else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.CARD) {

			BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		}
		else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.CUST) {

			BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());
		}

		// 查询冻结类型定义表 , 获取冻结分类码定义
		DppFrozeType frozeTypeDef = DpFrozeParmApi.getFrozeTypeInfo(cplIn.getFroze_kind_code());

		// 检查法院续冻必输补充要素是否输入
		if (frozeTypeDef.getFroze_source() == E_FROZESOURCE.EXTERNAL) {

			DpFrozePublic.checkLawFroze(cplIn);
		}

		bizlog.method(" DpFroze.checkInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月18日-上午11:24:32</li>
	 *         <li>功能说明：获得冻结对象基础信息</li>
	 *         </p>
	 * @param cplIn
	 *            冻结对象信息
	 * @return 冻结对象基础信息
	 */
	private static DpFrozeObjectBase getFrozeObejctBase(DpFrozeIn cplIn) {

		bizlog.method(" DpFroze.getFrozeObejctBase begin >>>>>>>>>>>>>>>>");

		DpFrozeObjectBase cplObjectBase = BizUtil.getInstance(DpFrozeObjectBase.class);

		// 客户冻结
		if (cplIn.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {

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
		else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.ACCT) {

			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

			cplObjectBase.setSub_acct_no(null);
			cplObjectBase.setAcct_no(acctInfo.getAcct_no());
			cplObjectBase.setCard_no(null);
			cplObjectBase.setCust_no(acctInfo.getCust_no());
			cplObjectBase.setCust_type(acctInfo.getCust_type());
		}
		else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.CARD) {

			DpaCard cardInfo = DpToolsApi.getCardInfor(cplIn.getAcct_no(), true);

			cplObjectBase.setSub_acct_no(null);
			cplObjectBase.setAcct_no(cardInfo.getAcct_no());
			cplObjectBase.setCard_no(cardInfo.getCard_no());
			cplObjectBase.setCust_no(cardInfo.getCust_no());
			cplObjectBase.setCust_type(cardInfo.getCust_type());
		}
		else if (cplIn.getFroze_object_type() == E_FROZEOBJECT.CUST) {

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
			throw APPUB.E0026(DpSysDict.A.froze_object_type.getLongName(), cplIn.getFroze_object_type().getValue());
		}

		bizlog.method(" DpFroze.getFrozeObejctBase end <<<<<<<<<<<<<<<<");
		return cplObjectBase;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月13日-上午11:24:32</li>
	 *         <li>功能说明：添加规则数据缓存</li>
	 *         </p>
	 * @param cplIn
	 *            冻结止付服务输入接口
	 * @param cplObjectBase
	 *            冻结对象基础信息
	 */
	private static void addDataBuffer(DpFrozeIn cplIn, DpFrozeObjectBase cplObjectBase) {

		// 查询冻结类型定义表 , 获取冻结分类码定义
		DppFrozeType frozeTypeDef = DpFrozeParmApi.getFrozeTypeInfo(cplIn.getFroze_kind_code());

		// 加载参数数据区
		ApBufferApi.addData(ApConst.PARM_DATA_MART, CommUtil.toMap(frozeTypeDef));

		// 加载输入数据区
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		// 添加冻结对象数据缓存区
		DpFrozePublic.addFrozeObjectDataBuffer(cplObjectBase);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月27日-下午3:18:16</li>
	 *         <li>功能说明：转换接口</li>
	 *         </p>
	 * @param cplIn
	 *            续冻输入
	 */
	private static DpFrozeBaseIn switchInterface(DpFrozeIn cplIn) {

		DpFrozeBaseIn cplOut = BizUtil.getInstance(DpFrozeBaseIn.class);

		cplOut.setTotal_count(1L); // 单笔冻结
		cplOut.setFroze_amt(cplIn.getFroze_amt());
		cplOut.setFroze_due_date(cplIn.getFroze_due_date());
		cplOut.setFroze_before_save_amt(cplIn.getFroze_before_save_amt());
		cplOut.setCcy_code(cplIn.getCcy_code());
		cplOut.setFroze_feature_code(cplIn.getFroze_feature_code());
		cplOut.setFroze_kind_code(cplIn.getFroze_kind_code());
		cplOut.setFroze_object_type(cplIn.getFroze_object_type());
		cplOut.setFroze_term(cplIn.getFroze_term());
		cplOut.setFroze_reason(cplIn.getFroze_reason());
		cplOut.setRemark(cplIn.getRemark());
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
