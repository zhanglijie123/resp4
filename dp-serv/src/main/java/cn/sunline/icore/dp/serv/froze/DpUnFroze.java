package cn.sunline.icore.dp.serv.froze;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApChannelApi;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.api.ApLimitApi;
import cn.sunline.icore.ap.api.ApRuleApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
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
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpFrozeDao;
import cn.sunline.icore.dp.serv.type.ComDpDayEnd.DpBatchUnFroze;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpUnFrozeOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZEOBJECT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZESTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.base.logging.LogConfigManager.SystemType;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 解冻解止
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2019年12月26日-上午8:55:16</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2019年12月26日-zhoumy：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpUnFroze {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpUnFroze.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月26日-上午8:58:45</li>
	 *         <li>功能说明：解冻解止主代码</li>
	 *         </p>
	 * @param cplIn
	 *            解冻解止输入接口
	 * @return dpForceDeductOut 解冻解止输出接口
	 */
	public static DpUnFrozeOut doMain(DpUnFrozeIn cplIn) {

		bizlog.method("DpUnFroze.doMain begin >>>>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);
		
		// 解冻检查
		DpbFroze frozeInfo = checkMain(cplIn);

		// 接口转换
		DpUnfrozeBaseIn cplUnfrozeBaseIn = switchInterface(cplIn);

		// 解冻维护冻结台账
		BigDecimal unfrozeAmt = DpBaseServiceApi.cancelFroze(frozeInfo, cplUnfrozeBaseIn);

		// 限额处理
		ApLimitApi.process(E_DEPTTRXNEVENT.DP_UNFROZE.getValue(), frozeInfo.getCcy_code(), unfrozeAmt);

		// 准备收费试算
		DpFrozePublic.autoChrg(frozeInfo.getAcct_no(), frozeInfo.getCcy_code(), unfrozeAmt, E_DEPTTRXNEVENT.DP_UNFROZE);

		// 初始化输出接口
		DpUnFrozeOut cplOut = BizUtil.getInstance(DpUnFrozeOut.class);

		cplOut.setAcct_name(cplIn.getAcct_name());
		cplOut.setAcct_no(frozeInfo.getAcct_no());
		cplOut.setCust_no(frozeInfo.getCust_no());
		cplOut.setCust_type(frozeInfo.getCust_type());
		cplOut.setFroze_bal(frozeInfo.getFroze_bal());
		cplOut.setFroze_due_date(frozeInfo.getFroze_due_date());
		cplOut.setFroze_kind_code(frozeInfo.getFroze_kind_code());
		cplOut.setFroze_kind_name(DpFrozeParmApi.getFrozeTypeInfo(frozeInfo.getFroze_kind_code()).getFroze_kind_name());
		cplOut.setFroze_no(frozeInfo.getFroze_no());
		cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplOut.setFroze_status(frozeInfo.getFroze_status());

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpUnFroze.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月31日-上午9:19:38</li>
	 *         <li>功能说明：自助解冻</li>
	 *         <li>用户自己发起的冻结全部解除</li>
	 *         </p>
	 * @param frozeObj
	 *            冻结对象
	 * @param frozeObj
	 *            冻结对象类型
	 * @param frozeChannel
	 *            解冻渠道
	 */
	public static void selfUnFrozen(String frozeObj, E_FROZEOBJECT frozeObjType, String frozeChannel) {

		bizlog.method("DpUnFroze.selfUnFrozen begin >>>>>>>>>>>>>>>");

		// 非空要素检查
		BizUtil.fieldNotNull(frozeObj, DpBaseDict.A.froze_object.getId(), DpBaseDict.A.froze_object.getLongName());
		BizUtil.fieldNotNull(frozeObjType, DpSysDict.A.froze_object_type.getId(), DpSysDict.A.froze_object_type.getLongName());

		// 获取解冻原因
		String unFrozeReason = ApBusinessParmApi.getValue("DP_THAW_REASON", "SELF_HELP_CHANNEL");

		// 检查渠道合法性
		ApChannelApi.exists(frozeChannel);

		if (frozeObjType == E_FROZEOBJECT.CARD) {

			// 获取卡信息
			DpaCard cardInfo = DpToolsApi.getCardInfor(frozeObj, true);

			frozeObj = cardInfo.getAcct_no(); // 卡冻结 、冻结对象是默认账户
		}

		// 获取卡下冻结记录
		List<DpbFroze> frozeList = SqlDpFrozeDao.selFrozeInfoByCard(frozeObj, frozeObjType, E_FROZESTATUS.FROZE, frozeChannel, BizUtil.getTrxRunEnvs().getBusi_org_id(), false);

		for (DpbFroze frozeInfo : frozeList) {

			// 解冻维护冻结台账
			DpUnfrozeBaseIn cplUnfroze = BizUtil.getInstance(DpUnfrozeBaseIn.class);

			cplUnfroze.setUnfroze_amt(frozeInfo.getFroze_bal());
			cplUnfroze.setFroze_feature_code(frozeInfo.getFroze_feature_code());
			cplUnfroze.setFroze_no(frozeInfo.getFroze_no());
			cplUnfroze.setRemark("");
			cplUnfroze.setUnfroze_reason(unFrozeReason);

			DpBaseServiceApi.cancelFroze(frozeInfo, cplUnfroze);
		}

		bizlog.method("DpUnFroze.selfUnFrozen end <<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年9月20日-上午9:19:38</li>
	 *         <li>功能说明：7*24小时联机到期自动解冻</li>
	 *         </p>
	 * @param custNo
	 *            客户号
	 * @return 存在真实解冻动作标志
	 */
	public static boolean matureAutoUnfrozen(String custNo) {

		// 联机交易在可以余额计算及限制状态检查等环节已经考虑了7*24小时解冻问题, 可以不在处理逻辑中实时自动解冻, 这样会提高效率
		if (!ApSystemParmApi.getOFF_ON("REAL_TIME_AUTO_UNFROZE")) {
			return false;
		}

		bizlog.method("DpUnFroze.matureAutoUnfrozen begin >>>>>>>>>");

		String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();
		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 查询客户冻结记录已到达到期日的记录
		List<DpBatchUnFroze> listFrozeRecord = SqlDpFrozeDao.selMatureFrozeRecordByCustomer(custNo, orgId, trxnDate, E_FROZESTATUS.FROZE, false);

		// 无对应待解冻记录，则直接退出
		if (listFrozeRecord.isEmpty() || listFrozeRecord.size() <= 0) {
			return false;
		}

		// 存在解冻标志
		boolean existUnfrozeFlag = false;

		// 备份交易机构和交易渠道
		String trxnBranch = BizUtil.getTrxRunEnvs().getTrxn_branch();
		String trxnChannel = BizUtil.getTrxRunEnvs().getChannel_id();

		// 7*24小时需要联机子系统触发自动解冻，把渠道设为批量渠道，让解冻数据信息登记符合日终解冻情形
		if (SysUtil.getCurrentSystemType() == SystemType.onl) {
			BizUtil.getTrxRunEnvs().setChannel_id(ApSystemParmApi.getValue(ApConst.DAYEND_CHANNEL_ID));
		}

		// 循环处理解冻
		for (DpBatchUnFroze dataItem : listFrozeRecord) {

			// 带锁
			DpbFroze frozeInfo = DpbFrozeDao.selectOneWithLock_odb1(dataItem.getFroze_no(), dataItem.getFroze_object(), true);

			// 到期解冻处理
			boolean flag = matureAutoUnfrozen(frozeInfo);

			if (flag) {
				existUnfrozeFlag = true;
			}
		}

		// 7*24小时需要联机子系统触发自动解冻，处理结束后还原机构和渠道，免得影响后续业务处理
		if (SysUtil.getCurrentSystemType() == SystemType.onl) {

			BizUtil.getTrxRunEnvs().setTrxn_branch(trxnBranch);
			BizUtil.getTrxRunEnvs().setChannel_id(trxnChannel);
		}

		bizlog.method("DpUnFroze.matureAutoUnfrozen end <<<<<<<<");

		return existUnfrozeFlag;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年9月20日-上午9:19:38</li>
	 *         <li>功能说明：日终批量渠道到期自动解冻</li>
	 *         </p>
	 * @param frozeInfo
	 *            冻结记录
	 * @return 存在真实解冻动作标志
	 */
	public static boolean matureAutoUnfrozen(DpbFroze frozeInfo) {

		if (frozeInfo.getFroze_status() == E_FROZESTATUS.CLOSE) {
			return false;
		}

		if (CommUtil.isNull(frozeInfo.getFroze_due_date()) || CommUtil.compare(frozeInfo.getFroze_due_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) > 0) {
			return false;
		}

		// 到期解冻原因
		String unfrozeReason = ApBusinessParmApi.getValue("DP_THAW_REASON", "MATURE_AUTO_THAW");

		// 将公共运行区机构号赋值为冻结机构，避免跨机构解冻; 日终解冻不检查渠道, 7*24小时解冻也不检查渠道
		BizUtil.getTrxRunEnvs().setTrxn_branch(frozeInfo.getFroze_branch());

		/* 日终到期解冻可以视为无条件解冻， 没有特殊情况，可以不用做检查; 日终不冲账，也可以不登记交易流水 */

		DpUnfrozeBaseIn cplIn = BizUtil.getInstance(DpUnfrozeBaseIn.class);

		cplIn.setFroze_feature_code(frozeInfo.getFroze_feature_code());
		cplIn.setUnfroze_amt(frozeInfo.getFroze_bal());
		cplIn.setFroze_no(frozeInfo.getFroze_no());
		cplIn.setRemark("");
		cplIn.setUnfroze_reason(unfrozeReason);

		// 解冻维护冻结台账
		DpBaseServiceApi.cancelFroze(frozeInfo, cplIn);

		/* 日终解冻无附件条件,不用像解冻服务那样考虑收费、限额问题 */

		return true;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年1月10日-上午9:25:22</li>
	 *         <li>功能说明：校验输入值是否合法</li>
	 *         </p>
	 * @param unFrozeIn
	 *            解冻解止输入接口
	 * @return 冻结信息
	 */
	public static DpbFroze checkMain(DpUnFrozeIn cplIn) {

		bizlog.method("DpUnFrozeCheck.checkMain begin <<<<<<<<<<<<<<<<<<<<");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 输入要素检查
		DpbFroze firstFroze = checkInputData(cplIn);

		// 获取冻结对象对应单条冻结记录: 带锁查询
		DpbFroze frozeInfo = getForzeInfo(cplIn, firstFroze);

		// 添加数据缓存区
		addDataBuffer(cplIn, frozeInfo);

		// 接口转换
		DpUnfrozeBaseIn cplUnfrozeBaseIn = switchInterface(cplIn);

		// 解冻许可检查
		DpBaseServiceApi.checkUnfrozeLicense(frozeInfo, cplUnfrozeBaseIn);

		// 交易控制检查: 包括业务规则、属性检查
		ApRuleApi.checkTrxnControl(E_DEPTTRXNEVENT.DP_UNFROZE.getValue());

		bizlog.method("DpUnFrozeCheck.checkMain end <<<<<<<<<<<<<<<<<<<<");
		return frozeInfo;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月13日-下午1:56:35</li>
	 *         <li>功能说明：检查解冻公用输入数据</li>
	 *         </p>
	 * @param cplIn
	 *            解冻解止输入接口
	 */
	private static DpbFroze checkInputData(DpUnFrozeIn cplIn) {

		bizlog.method(" DpUnFroze.checkInputData begin >>>>>>>>>>>>>>>>");

		// 冻结编号不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_no(), SysDict.A.froze_no.getId(), SysDict.A.froze_no.getLongName());

		// 解冻原因不能为空
		BizUtil.fieldNotNull(cplIn.getUnfroze_reason(), SysDict.A.unfroze_reason.getId(), SysDict.A.unfroze_reason.getLongName());

		// 检验冻结原因合法性(下拉字典)
		ApDropListApi.exists(DpConst.UNFROZE_REASON, cplIn.getUnfroze_reason());

		// 查询首条冻结信息
		DpbFroze firstFroze = DpFrozePublic.getFirstForzeInfo(cplIn.getFroze_no());

		// 检查法院解冻必输补充要素是否输入
		if (firstFroze.getFroze_source() == E_FROZESOURCE.EXTERNAL) {
			DpFrozePublic.checkLawFroze(cplIn);
		}

		bizlog.method(" DpUnFroze.checkInputData end <<<<<<<<<<<<<<<<");
		return firstFroze;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年2月15日-下午5:20:08</li>
	 *         <li>功能说明：获取单条未解冻冻结记录</li>
	 *         </p>
	 * @param cplIn
	 *            解冻输入信息
	 * @param firstFroze
	 *            首条冻结记录
	 * @return 冻结记录
	 */
	private static DpbFroze getForzeInfo(DpUnFrozeIn cplIn, DpbFroze firstFroze) {

		String frozeObj = "";// 冻结对象
		String newCardNo = ""; // 新卡号

		// 冻结记录本身就是单条冻结, 则不需要提供冻结对象信息, 直接返回
		if (CommUtil.compare(firstFroze.getTotal_count(), 1L) == 0) {

			frozeObj = firstFroze.getFroze_object();

			if (firstFroze.getFroze_object_type() ==  E_FROZEOBJECT.CARD) {

				DpaCard cardInfo = DpToolsApi.getCardInfor(frozeObj, true);

				newCardNo = cardInfo.getCard_no();
			}
		}
		else if (firstFroze.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {// 指定子账号

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

			frozeObj = accessOut.getSub_acct_no();
		}
		else if (firstFroze.getFroze_object_type() == E_FROZEOBJECT.ACCT) {// 指定账号

			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

			frozeObj = acctInfo.getAcct_no();
		}
		else if (firstFroze.getFroze_object_type() == E_FROZEOBJECT.CARD) {// 指定卡号

			// 卡号不能为空
			BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

			DpaCard cardInfo = DpToolsApi.getCardInfor(cplIn.getAcct_no(), true);

			// 为了避免换卡带来的影响，卡冻结冻结对象填的是默认账号
			frozeObj = cardInfo.getAcct_no();
			newCardNo = cardInfo.getCard_no();
		}
		else if (firstFroze.getFroze_object_type() == E_FROZEOBJECT.CUST) {// 指定客户号

			// 客户号不能为空
			BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

			frozeObj = cplIn.getCust_no();
		}
		else {

			throw APPUB.E0026(DpSysDict.A.froze_object_type.getLongName(), firstFroze.getFroze_object_type().getValue());
		}

		// 查询冻结登记簿
		DpbFroze frozeInfo = DpbFrozeDao.selectOneWithLock_odb1(cplIn.getFroze_no(), frozeObj, false);

		// 冻结编号不存在
		if (frozeInfo == null) {

			throw APPUB.E0024(OdbFactory.getTable(DpbFroze.class).getLongname(), SysDict.A.froze_no.getId(), cplIn.getFroze_no(), DpBaseDict.A.froze_object.getId(), frozeObj);
		}

		// 检查冻结编号状态
		if (frozeInfo.getFroze_status() == E_FROZESTATUS.CLOSE) {

			throw DpBase.E0106(frozeInfo.getFroze_no(), frozeObj);
		}

		// 检查卡号的合法性，避免附属卡把主卡给解冻了
		if (firstFroze.getFroze_object_type() == E_FROZEOBJECT.CARD && !CommUtil.equals(frozeInfo.getCard_no(), newCardNo)) {

			// 登记簿登记的可能是旧卡，才导致不相等
			DpaCardNew newCard = DpaCardNewDao.selectOne_odb1(frozeInfo.getCard_no(), false);

			// 附属卡不能解冻主卡，主卡也不能解冻附属卡
			if (newCard == null || !CommUtil.equals(newCard.getNew_card_no(), newCardNo)) {
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
	 * @param cplObjectBase
	 *            冻结对象基础信息
	 */
	private static void addDataBuffer(DpUnFrozeIn cplIn, DpbFroze frozeInfo) {

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
	 *         <li>2019年12月27日-下午3:18:16</li>
	 *         <li>功能说明：转换接口</li>
	 *         </p>
	 * @param cplIn
	 *            解冻输入
	 */
	private static DpUnfrozeBaseIn switchInterface(DpUnFrozeIn cplIn) {

		DpUnfrozeBaseIn cplOut = BizUtil.getInstance(DpUnfrozeBaseIn.class);

		cplOut.setUnfroze_amt(cplIn.getUnfroze_amt());
		cplOut.setFroze_feature_code(cplIn.getFroze_feature_code());
		cplOut.setFroze_no(cplIn.getFroze_no());
		cplOut.setWithdrawal_busi_type(cplIn.getWithdrawal_busi_type());
		cplOut.setUnfroze_reason(cplIn.getUnfroze_reason());
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
