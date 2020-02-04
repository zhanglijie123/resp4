package cn.sunline.icore.dp.serv.froze;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.api.ApLimitApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpFrozeParmApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardNew;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardNewDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFrozeDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeParameter.DppFrozeType;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeObjectBase;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpUnfrozeBaseIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_FROZESOURCE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpMultiUnFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpMultiUnFrozeOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnfrozeObjectIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnfrozeObjectOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZEOBJECT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZESTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明： 解冻解止
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月10日-上午8:55:16</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月10日-zhoumy：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpMultipleUnFroze {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpMultipleUnFroze.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月10日-上午8:58:45</li>
	 *         <li>功能说明：解冻解止主代码</li>
	 *         </p>
	 * @param forceDeductIn
	 *            解冻解止输入接口
	 * @return dpForceDeductOut 解冻解止输出接口
	 */
	public static DpMultiUnFrozeOut doMain(DpMultiUnFrozeIn cplIn) {

		bizlog.method("DpUnFroze.doMain begin >>>>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 解冻前检查
		E_FROZEOBJECT frozeObjectType = checkMain(cplIn);

		// 初始化输出接口
		DpMultiUnFrozeOut cplOut = BizUtil.getInstance(DpMultiUnFrozeOut.class);

		// 记录中记录数
		long totalCount = 0;

		// 循环解冻处理
		for (DpUnfrozeObjectIn cplUnfrObject : cplIn.getList01()) {

			// 获取冻结对象对应单条冻结记录: 带锁查询
			DpbFroze frozeInfo = getForzeInfo(cplUnfrObject, cplIn.getFroze_no(), frozeObjectType);

			// 添加数据缓存区
			addDataBuffer(cplIn, frozeInfo);

			// 接口转换
			DpUnfrozeBaseIn cplUnfrozeBaseIn = switchInterface(cplIn, cplUnfrObject);

			// 解冻维护冻结台账
			BigDecimal unfrozeAmt = DpBaseServiceApi.cancelFroze(frozeInfo, cplUnfrozeBaseIn);

			// 限额处理
			ApLimitApi.process(E_DEPTTRXNEVENT.DP_UNFROZE.getValue(), cplUnfrObject.getCcy_code(), unfrozeAmt);

			// 初始化解冻对象输出
			DpUnfrozeObjectOut unfrozeObjOut = BizUtil.getInstance(DpUnfrozeObjectOut.class);

			unfrozeObjOut.setCust_no(frozeInfo.getCust_no()); // 客户号
			unfrozeObjOut.setCard_no(frozeInfo.getCard_no()); // 卡号
			unfrozeObjOut.setAcct_no(frozeInfo.getAcct_no()); // 账号
			unfrozeObjOut.setCcy_code(frozeInfo.getCcy_code()); // 货币代码
			unfrozeObjOut.setUnfroze_amt(unfrozeAmt); // 解冻金额
			unfrozeObjOut.setUnfroze_reason(frozeInfo.getUnfroze_reason()); // 解冻原因
			unfrozeObjOut.setFroze_bal(frozeInfo.getFroze_bal()); // 冻结余额
			unfrozeObjOut.setFroze_status(frozeInfo.getFroze_status());// 冻结状态
			unfrozeObjOut.setSub_acct_seq(cplUnfrObject.getSub_acct_seq());
			unfrozeObjOut.setProd_id(cplUnfrObject.getProd_id());

			cplOut.getList_unfroze_object().add(unfrozeObjOut);

			totalCount++;

			// 准备收费试算
			DpFrozePublic.autoChrg(cplUnfrObject.getAcct_no(), cplUnfrObject.getCcy_code(), cplUnfrObject.getUnfroze_amt(), E_DEPTTRXNEVENT.DP_UNFROZE);
		}

		cplOut.setFroze_no(cplIn.getFroze_no());// 冻结编号

		BizUtil.getTrxRunEnvs().setTotal_count(totalCount);// 返回总记录数

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpUnFroze.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月10日-上午9:25:22</li>
	 *         <li>功能说明：校验输入值是否合法</li>
	 *         </p>
	 * @param unFrozeIn
	 *            解冻解止输入接口
	 */
	public static E_FROZEOBJECT checkMain(DpMultiUnFrozeIn cplIn) {

		bizlog.method("DpUnFrozeCheck.checkMain begin <<<<<<<<<<<<<<<<<<<<");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 检查冻结公用输入数据
		DpbFroze firstFroze = checkInputData(cplIn);

		// 循环解冻检查
		for (DpUnfrozeObjectIn cplUnfrObject : cplIn.getList01()) {

			// 获取冻结对象对应单条冻结记录: 带锁查询
			DpbFroze frozeInfo = getForzeInfo(cplUnfrObject, firstFroze.getFroze_no(), firstFroze.getFroze_object_type());

			// 添加数据缓存区
			addDataBuffer(cplIn, frozeInfo);

			// 接口转换
			DpUnfrozeBaseIn cplUnfrozeBaseIn = switchInterface(cplIn, cplUnfrObject);

			// 解冻许可检查
			DpBaseServiceApi.checkUnfrozeLicense(frozeInfo, cplUnfrozeBaseIn);

			// 交易控制检查: 包括业务规则、属性检查
			ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_UNFROZE.getValue());
		}

		bizlog.method("DpUnFrozeCheck.checkMain end <<<<<<<<<<<<<<<<<<<<");
		return firstFroze.getFroze_object_type();
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月13日-下午1:56:35</li>
	 *         <li>功能说明：检查解冻公用输入数据</li>
	 *         </p>
	 * @param cplIn
	 *            解冻解止输入接口
	 */
	private static DpbFroze checkInputData(DpMultiUnFrozeIn cplIn) {

		bizlog.method(" DpUnFrozeCheck.checkInputData begin >>>>>>>>>>>>>>>>");

		// 冻结编号不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_no(), SysDict.A.froze_no.getId(), SysDict.A.froze_no.getLongName());

		// 解冻原因不能为空
		BizUtil.fieldNotNull(cplIn.getUnfroze_reason(), SysDict.A.unfroze_reason.getId(), SysDict.A.unfroze_reason.getLongName());

		// 检验冻结原因合法性(下拉字典)
		ApDropListApi.exists(DpConst.UNFROZE_REASON, cplIn.getUnfroze_reason());

		// 获取首条冻结记录
		DpbFroze firstFroze = DpFrozePublic.getFirstForzeInfo(cplIn.getFroze_no());

		// 检查法院解冻必输补充要素是否输入
		if (firstFroze.getFroze_source() == E_FROZESOURCE.EXTERNAL) {

			DpFrozePublic.checkLawFroze(cplIn);
		}

		// 解冻对象列表有值，则对解冻列表进行深加工，以便后面的逻辑简单，且Do服务不用反复去查询定位对象信息，以提高效率
		if (cplIn.getList01() != null && cplIn.getList01().size() > 0) {

			unforzeObjectWorking(cplIn, firstFroze.getFroze_object_type());
		}
		// 解冻对象为空表示全部解冻
		else {

			genUnfrozeObject(cplIn);
		}

		bizlog.method(" DpUnFrozeCheck.checkInputData end <<<<<<<<<<<<<<<<");
		return firstFroze;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月13日-下午1:56:35</li>
	 *         <li>功能说明：检查解冻公用输入数据</li>
	 *         </p>
	 * @param cplIn
	 *            解冻解止输入接口
	 */
	private static void unforzeObjectWorking(DpMultiUnFrozeIn cplIn, E_FROZEOBJECT frozeObjectType) {

		bizlog.method(" DpUnFrozeCheck.unforzeObjectWorking begin >>>>>>>>>>>>>>>>");

		// 解冻对象列表有值，则对解冻列表进行深加工，以便后面的逻辑简单，且Do服务不用反复去查询定位对象信息，以提高效率

		for (DpUnfrozeObjectIn cplObjectIn : cplIn.getList01()) {

			if (frozeObjectType == E_FROZEOBJECT.SUBACCT) {// 指定子账号

				// 初始化定位子账号信息
				DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

				accessIn.setAcct_no(cplObjectIn.getAcct_no()); // 账号
				accessIn.setSub_acct_seq(cplObjectIn.getSub_acct_seq()); // 子账户序号
				accessIn.setCcy_code(cplObjectIn.getCcy_code()); // 货币代码
				accessIn.setAcct_type(cplObjectIn.getAcct_type()); // 账户类型
				accessIn.setProd_id(cplObjectIn.getProd_id()); // 产品编号
				accessIn.setDd_td_ind(null); // 定活标志
				accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.WITHDRAWAL); // 存入支取标志

				DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

				cplObjectIn.setCard_no(accessOut.getCard_no());
				cplObjectIn.setCust_no(accessOut.getCust_no());
				cplObjectIn.setCust_type(accessOut.getCust_type());
				cplObjectIn.setAcct_no(accessOut.getAcct_no());
				cplObjectIn.setSub_acct_no(accessOut.getSub_acct_no());
				cplObjectIn.setSub_acct_seq(accessOut.getSub_acct_seq());
				cplObjectIn.setCcy_code(accessOut.getCcy_code());
				cplObjectIn.setProd_id(accessOut.getProd_id());
			}
			else if (frozeObjectType == E_FROZEOBJECT.ACCT) {// 指定账号

				DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplObjectIn.getAcct_no(), cplObjectIn.getAcct_type(), false);

				cplObjectIn.setCust_no(acctInfo.getCust_no());
				cplObjectIn.setCust_type(acctInfo.getCust_type());
				cplObjectIn.setAcct_no(acctInfo.getAcct_no());
			}
			else if (frozeObjectType == E_FROZEOBJECT.CARD) {// 指定卡号

				// 卡号不能为空
				BizUtil.fieldNotNull(cplObjectIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

				DpaCard cardInfo = DpToolsApi.getCardInfor(cplObjectIn.getAcct_no(), true);

				cplObjectIn.setCard_no(cardInfo.getCard_no());
				cplObjectIn.setCust_no(cardInfo.getCust_no());
				cplObjectIn.setCust_type(cardInfo.getCust_type());
				cplObjectIn.setAcct_no(cardInfo.getAcct_no());
			}
			else if (frozeObjectType == E_FROZEOBJECT.CUST) {

				// 客户号不能为空
				BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

			}
		}

		bizlog.method(" DpUnFrozeCheck.unforzeObjectWorking begin >>>>>>>>>>>>>>>>");
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月13日-下午1:56:35</li>
	 *         <li>功能说明：生成解冻对象集合</li>
	 *         </p>
	 * @param cplIn
	 *            解冻解止输入接口
	 */
	private static void genUnfrozeObject(DpMultiUnFrozeIn cplIn) {

		if (cplIn.getList01() != null && cplIn.getList01().size() > 0) {
			return;
		}

		bizlog.method(" DpUnFrozeCheck.getUnfrozeObject begin >>>>>>>>>>>>>>>>");

		// 获取全部冻结信息
		List<DpbFroze> listFrozeInfo = DpbFrozeDao.selectAll_odb4(cplIn.getFroze_no(), E_FROZESTATUS.FROZE, false);

		// 不存在未解冻冻结信息则需要抛出异常
		if (listFrozeInfo == null || listFrozeInfo.size() == 0) {
			throw DpBase.E0127(cplIn.getFroze_no());
		}

		Options<DpUnfrozeObjectIn> listFrozeObject = new DefaultOptions<DpUnfrozeObjectIn>();

		// 冻结信息
		for (DpbFroze frozeInfo : listFrozeInfo) {

			DpUnfrozeObjectIn unfrozeObject = BizUtil.getInstance(DpUnfrozeObjectIn.class);

			unfrozeObject.setCcy_code(frozeInfo.getCcy_code());
			unfrozeObject.setUnfroze_amt(frozeInfo.getFroze_bal());
			unfrozeObject.setAcct_no(frozeInfo.getAcct_no());
			unfrozeObject.setCust_no(frozeInfo.getFroze_object());
			unfrozeObject.setCust_type(frozeInfo.getCust_type());
			unfrozeObject.setFroze_feature_code(frozeInfo.getFroze_feature_code());

			if (frozeInfo.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {

				DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(frozeInfo.getAcct_no(), frozeInfo.getFroze_object(), true);

				unfrozeObject.setProd_id(subAcct.getProd_id());
				unfrozeObject.setCcy_code(subAcct.getCcy_code());
				unfrozeObject.setSub_acct_seq(subAcct.getSub_acct_seq());
			}

			listFrozeObject.add(unfrozeObject);
		}

		// 加到列表中
		cplIn.setList01(listFrozeObject);

		bizlog.method(" DpUnFrozeCheck.getUnfrozeObject end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月15日-下午5:20:08</li>
	 *         <li>功能说明：获取单条未解冻冻结记录</li>
	 *         </p>
	 * @param cplIn
	 *            解冻对象输入信息
	 * @param frozeObjectType
	 *            冻结对象类型
	 * @return 冻结记录
	 */
	private static DpbFroze getForzeInfo(DpUnfrozeObjectIn cplIn, String frozeNo, E_FROZEOBJECT frozeObjectType) {

		bizlog.debug("DpUnfrozeObjectIn=[%s]", cplIn);

		String frozeObj = "";// 冻结对象

		if (frozeObjectType == E_FROZEOBJECT.SUBACCT) {// 指定子账号

			frozeObj = cplIn.getSub_acct_no();
		}
		else if (frozeObjectType == E_FROZEOBJECT.ACCT) {// 指定账号

			frozeObj = cplIn.getAcct_no();
		}
		else if (frozeObjectType == E_FROZEOBJECT.CARD) {// 指定卡号

			// 为了避免换卡带来的影响，卡冻结冻结对象填的是默认账号
			frozeObj = cplIn.getAcct_no();
		}
		else if (frozeObjectType == E_FROZEOBJECT.CUST) {// 指定客户号

			frozeObj = cplIn.getCust_no();
		}
		else {

			throw APPUB.E0026(DpSysDict.A.froze_object_type.getLongName(), frozeObjectType.getValue());
		}

		// 查询冻结登记簿
		DpbFroze frozeInfo = DpbFrozeDao.selectOneWithLock_odb1(frozeNo, frozeObj, false);

		// 冻结编号不存在
		if (frozeInfo == null) {

			throw APPUB.E0024(OdbFactory.getTable(DpbFroze.class).getLongname(), SysDict.A.froze_no.getId(), frozeNo, DpBaseDict.A.froze_object.getId(), frozeObj);
		}

		// 检查冻结编号状态
		if (frozeInfo.getFroze_status() == E_FROZESTATUS.CLOSE) {

			throw DpBase.E0106(frozeInfo.getFroze_no(), frozeObj);
		}

		// 检查卡号的合法性，避免附属卡把主卡给解冻了
		if (frozeObjectType == E_FROZEOBJECT.CARD && !CommUtil.equals(frozeInfo.getCard_no(), cplIn.getCard_no())) {

			// 登记簿登记的可能是旧卡，才导致不相等
			DpaCardNew newCard = DpaCardNewDao.selectOne_odb1(frozeInfo.getCard_no(), false);

			// 附属卡不能解冻主卡，主卡也不能解冻附属卡
			if (newCard == null || !CommUtil.equals(newCard.getNew_card_no(), cplIn.getCard_no())) {
				throw DpErr.Dp.E0392(cplIn.getAcct_no());
			}
		}

		return frozeInfo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月13日-上午11:24:32</li>
	 *         <li>功能说明：添加规则数据缓存</li>
	 *         </p>
	 * @param cplIn
	 *            冻结止付服务输入接口
	 * @param frozeInfo
	 *            冻结信息
	 */
	private static void addDataBuffer(DpMultiUnFrozeIn cplIn, DpbFroze frozeInfo) {

		// 查询冻结类型定义表 , 获取冻结分类码定义
		DppFrozeType frozeTypeDef = DpFrozeParmApi.getFrozeTypeInfo(frozeInfo.getFroze_kind_code());

		// 加载参数数据区
		ApBufferApi.addData(ApConst.PARM_DATA_MART, CommUtil.toMap(frozeTypeDef));

		// 加载输入数据区
		ApBufferApi.addData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

		// 冻结对象基础信息
		DpFrozeObjectBase cplObjectBase = BizUtil.getInstance(DpFrozeObjectBase.class);

		cplObjectBase.setAcct_no(frozeInfo.getAcct_no());
		cplObjectBase.setCard_no(frozeInfo.getCard_no());
		cplObjectBase.setCust_no(frozeInfo.getCust_no());
		cplObjectBase.setCust_type(frozeInfo.getCust_type());

		if (frozeInfo.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {
			cplObjectBase.setSub_acct_no(frozeInfo.getFroze_object());
		}

		// 添加冻结对象数据缓存区
		DpFrozePublic.addFrozeObjectDataBuffer(cplObjectBase);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月7日-下午3:18:16</li>
	 *         <li>功能说明：转换接口</li>
	 *         </p>
	 * @param cplIn
	 *            多个对象解冻输入
	 * @param cplObjIn
	 *            冻结对象
	 */
	private static DpUnfrozeBaseIn switchInterface(DpMultiUnFrozeIn cplIn, DpUnfrozeObjectIn cplObjIn) {

		DpUnfrozeBaseIn cplOut = BizUtil.getInstance(DpUnfrozeBaseIn.class);

		cplOut.setFroze_feature_code(cplObjIn.getFroze_feature_code());
		cplOut.setRemark(cplIn.getRemark());
		cplOut.setFroze_no(cplIn.getFroze_no());
		cplOut.setUnfroze_amt(cplObjIn.getUnfroze_amt());
		cplOut.setUnfroze_reason(cplIn.getUnfroze_reason());
		cplOut.setWithdrawal_busi_type(cplIn.getWithdrawal_busi_type());
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
