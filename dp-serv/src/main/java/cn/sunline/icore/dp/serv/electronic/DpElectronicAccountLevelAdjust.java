package cn.sunline.icore.dp.serv.electronic;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApChannelApi;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ACCTLEVEL;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpElectronicAccountDao;
import cn.sunline.icore.dp.serv.tables.TabDpTwoThreeTypeAccount.DpbLevelChange;
import cn.sunline.icore.dp.serv.tables.TabDpTwoThreeTypeAccount.DpbLevelChangeDao;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpAccountLevelChangeInfo;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpAccountLevelChangeInfoQueryIn;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpElecAcctDowngradeInput;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpElecAcctUpgradeInput;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpElecAcctUpgradeResult;
import cn.sunline.icore.dp.serv.type.ComDpElectronicAccount.DpEleccAcctUpgradeManualAuditIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_ACCTLEVELCHANGETYPE;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_LEVELCHANGESTATUS;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_OCRCHECKRESULT;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：电子账户级别调整
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2019年11月28日-下午15:05:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpElectronicAccountLevelAdjust {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpElectronicAccountLevelAdjust.class);
	private static final String ELEC_ACCT_COUNT_LIMIT = "ELEC_ACCT_COUNT_LIMIT"; // 电子账户数量限制

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月18日-上午11:43:40</li>
	 *         <li>功能说明：电子账户降级</li>
	 *         </p>
	 * @param cplIn
	 *            电子账户降级输入
	 */
	public static void downgrade(DpElecAcctDowngradeInput cplIn) {
		bizlog.method(" DpElectronicAccountLevelAdjust.downgrade begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 检查必输项
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getAcct_level(), DpBaseDict.A.acct_level.getId(), DpBaseDict.A.acct_level.getLongName());

		// 带锁定位账户信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, true);

		if (E_ACCTSTATUS.CLOSE == acctInfo.getAcct_status()) {
			throw DpBase.E0008(cplIn.getAcct_no());
		}

		// 当前账户级别已是指定级别，无需重复降级
		if (acctInfo.getAcct_level() == cplIn.getAcct_level()) {
			throw DpErr.Dp.E0503();
		}

		// 账户级别越高数值越小， 当级别数字调小说明是升级而不是降级
		if (CommUtil.compare(acctInfo.getAcct_level().getValue(), cplIn.getAcct_level().getValue()) > 0) {
			throw DpErr.Dp.E0502();
		}

		// 原账户级别
		E_ACCTLEVEL oldAcctLevel = acctInfo.getAcct_level();

		// 更新账户表账户级别
		modifyAccountLevel(acctInfo, cplIn.getAcct_level());

		// 登记电子账户级别变更登记簿（降级）
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		DpbLevelChange levelChange = BizUtil.getInstance(DpbLevelChange.class);

		levelChange.setAcct_no(acctInfo.getAcct_no());// 账号
		levelChange.setRequest_seq(runEnvs.getInitiator_seq());// 申请流水
		levelChange.setRequest_date(runEnvs.getInitiator_date());// 申请日期
		levelChange.setAcct_level_change_type(E_ACCTLEVELCHANGETYPE.DOWNGRADE);// 账户级别变更类型
		levelChange.setBefore_acct_level(oldAcctLevel);// 变更前账户级别
		levelChange.setAfter_acct_level(cplIn.getAcct_level());// 变更后账户级别
		levelChange.setId_check_result(null);// 身份核查结果
		levelChange.setFace_check_result(null);// 人脸识别结果
		levelChange.setFace_ind(null);// 面签标志
		levelChange.setLevel_change_status(E_LEVELCHANGESTATUS.SUCCESS);// 级别调整状态
		levelChange.setFailure_reason(null);// 失败原因
		levelChange.setTrxn_channel(runEnvs.getChannel_id());// 交易渠道
		levelChange.setTrxn_date(runEnvs.getTrxn_date());// 交易日期
		levelChange.setTrxn_seq(runEnvs.getTrxn_seq());// 交易流水
		levelChange.setTrxn_branch(runEnvs.getTrxn_branch());// 交易机构
		levelChange.setTrxn_teller(runEnvs.getTrxn_teller());// 交易柜员
		levelChange.setTrxn_time(runEnvs.getComputer_time());// 交易时间

		DpbLevelChangeDao.insert(levelChange);

		bizlog.method(" DpElectronicAccountLevelAdjust.downgrade end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月18日-上午11:42:46</li>
	 *         <li>功能说明：电子账户面签</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 */
	public static void faceSignature(String acctNo) {
		bizlog.method(" DpElectronicAccountLevelAdjust.faceSignature begin >>>>>>>>>>>>>>>>");

		// 检查必输项
		BizUtil.fieldNotNull(acctNo, SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 带锁定位账户信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(acctNo, null, true);

		if (E_ACCTSTATUS.CLOSE == acctInfo.getAcct_status()) {
			throw DpBase.E0008(acctNo);
		}

		// 已经面签过，无需重复面签
		if (acctInfo.getFace_ind() == E_YESORNO.YES) {
			throw DpErr.Dp.E0499();
		}

		// 先保留旧账户信息
		DpaAccount oldAcctInfo = BizUtil.clone(DpaAccount.class, acctInfo);

		// 更新账户面签标志
		acctInfo.setFace_ind(E_YESORNO.YES);

		DpaAccountDao.updateOne_odb1(acctInfo);

		// 登记审计日志
		ApDataAuditApi.regLogOnUpdateBusiness(oldAcctInfo, acctInfo);

		bizlog.method(" DpElectronicAccountLevelAdjust.faceSignature end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月18日-上午11:45:16</li>
	 *         <li>功能说明：电子账户升级</li>
	 *         </p>
	 * @param cplIn
	 *            电子账户升级输入
	 * @return 级别调整状态
	 */
	public static DpElecAcctUpgradeResult upgrade(DpElecAcctUpgradeInput cplIn) {
		bizlog.method(" DpElectronicAccountLevelAdjust.clientUpgrade begin >>>>>>>>>>>>>>>>");

		// 必输检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号
		BizUtil.fieldNotNull(cplIn.getId_check_result(), DpDict.A.id_check_result.getId(), DpDict.A.id_check_result.getLongName());// 身份核查结果
		BizUtil.fieldNotNull(cplIn.getFace_check_result(), DpDict.A.face_check_result.getId(), DpDict.A.face_check_result.getLongName());// 人脸识别结果
		BizUtil.fieldNotNull(cplIn.getAcct_level(), DpBaseDict.A.acct_level.getId(), DpBaseDict.A.acct_level.getLongName());// 账户级别

		// 是柜面管理端则面签标志需要录入
		if (ApChannelApi.isCounter(BizUtil.getTrxRunEnvs().getChannel_id())) {

			BizUtil.fieldNotNull(cplIn.getFace_ind(), DpBaseDict.A.face_ind.getId(), DpBaseDict.A.face_ind.getLongName());// 账户级别
		}
		else {

			// 只有管理端才能升级成全功能类账户
			if (E_ACCTLEVEL.FULL_FUNCTION == cplIn.getAcct_level()) {
				throw DpErr.Dp.E0500();
			}
		}

		// 带锁定位账户信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, true);

		if (E_ACCTSTATUS.CLOSE == acctInfo.getAcct_status()) {
			throw DpBase.E0008(cplIn.getAcct_no());
		}

		// 当前账户级别已是指定级别，无需重复升级
		if (acctInfo.getAcct_level() == cplIn.getAcct_level()) {
			throw DpErr.Dp.E0501();
		}

		// 账户级别越高数值越小， 当级别数字调大说明是降级而不是升级
		if (CommUtil.compare(acctInfo.getAcct_level().getValue(), cplIn.getAcct_level().getValue()) < 0) {
			throw DpErr.Dp.E0502();
		}

		// 查询是否有受理中的记录，如果有，提示已受理，不用重复发起
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		
		DpbLevelChange acctLevelInfo = SqlDpElectronicAccountDao.selAcctLevelChangeAcceptedOne(cplIn.getAcct_no(), 
				runEnvs.getBusi_org_id(), false);
		
		if(CommUtil.isNotNull(acctLevelInfo)) {
			DpErr.Dp.E0511();
		}

		// 登记账户升级结果
		DpElecAcctUpgradeResult upgradeResult = registerUpgradeResult(cplIn, acctInfo);

		// 更新账户表账户级别
		if (upgradeResult.getLevel_change_status() == E_LEVELCHANGESTATUS.SUCCESS) {

			modifyAccountLevel(acctInfo, cplIn.getAcct_level());
		}

		bizlog.method(" DpElectronicAccountLevelAdjust.clientUpgrade end <<<<<<<<<<<<<<<<");

		return upgradeResult;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年11月28日-下午17:45:16</li>
	 *         <li>功能说明：分析电子账户升级结果</li>
	 *         </p>
	 * @param cplIn
	 *            电子账户升级输入
	 * @param acctInfo
	 *            账户信息
	 * @return 级别调整结果分析
	 */
	private static DpElecAcctUpgradeResult analysisUpgradeResult(DpElecAcctUpgradeInput cplIn, DpaAccount acctInfo) {

		// 实例化分析结果
		DpElecAcctUpgradeResult cplResult = BizUtil.getInstance(DpElecAcctUpgradeResult.class);

		if (acctInfo.getAcct_status() == E_ACCTSTATUS.CLOSE) {

			cplResult.setLevel_change_status(E_LEVELCHANGESTATUS.FAIL);
			cplResult.setFailure_reason("001"); // 账户状态不正常

			return cplResult;
		}

		// 优先检查账户升级后，同级别账户数量是否会突破监管要求
		long LimitNum = ApBusinessParmApi.getIntValue(ELEC_ACCT_COUNT_LIMIT, cplIn.getAcct_level().getId());

		// 查询客户下此级别账户活跃数
		long activeNum = SqlDpElectronicAccountDao.selAppointAcctLevelNum(acctInfo.getCust_no(), cplIn.getAcct_level(), BizUtil.getTrxRunEnvs().getBusi_org_id(), false);

		// 同级别账户数已到监管上限，不能再升级账户
		if (activeNum >= LimitNum) {

			cplResult.setLevel_change_status(E_LEVELCHANGESTATUS.FAIL);
			cplResult.setFailure_reason("002"); // 账户数量不符合监管要求

			return cplResult;
		}

		// 升级全功能类账户
		if (cplIn.getAcct_level() == E_ACCTLEVEL.FULL_FUNCTION) {

			// 在柜面处理，任何认证失败都会导致升级失败
			if (cplIn.getFace_check_result() == E_OCRCHECKRESULT.FAIL || cplIn.getId_check_result() == E_OCRCHECKRESULT.FAIL || cplIn.getFace_ind() == E_YESORNO.NO) {

				cplResult.setLevel_change_status(E_LEVELCHANGESTATUS.FAIL);
				cplResult.setFailure_reason("003"); // OCR审核不通过
				
				return cplResult;
			}
			// 在柜面处理，任何认证还在验证中都会导致升级排队受理
			else if (cplIn.getFace_check_result() == E_OCRCHECKRESULT.CHECK || cplIn.getId_check_result() == E_OCRCHECKRESULT.CHECK) {

				cplResult.setLevel_change_status(E_LEVELCHANGESTATUS.ACCEPTED);
				return cplResult;
			}

			
		}
		// 升级理财功能类户
		else if (cplIn.getAcct_level() == E_ACCTLEVEL.FINANCIAL_MANAGEMENT) {

			// 升级二类户不用考虑面签是否通过
			if (cplIn.getFace_check_result() == E_OCRCHECKRESULT.FAIL || cplIn.getId_check_result() == E_OCRCHECKRESULT.FAIL) {

				cplResult.setLevel_change_status(E_LEVELCHANGESTATUS.FAIL);
				cplResult.setFailure_reason("003"); // OCR审核不通过
				
				return cplResult;
			}
			// 任何认证还在验证中都会导致升级排队受理
			else if (cplIn.getFace_check_result() == E_OCRCHECKRESULT.CHECK || cplIn.getId_check_result() == E_OCRCHECKRESULT.CHECK) {

				cplResult.setLevel_change_status(E_LEVELCHANGESTATUS.ACCEPTED);
				return cplResult;
			}

			
		}

		// 运行到此处认为升级成功
		cplResult.setLevel_change_status(E_LEVELCHANGESTATUS.SUCCESS);

		return cplResult;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年11月18日-上午11:45:16</li>
	 *         <li>功能说明：登记电子账户升级结果</li>
	 *         </p>
	 * @param cplIn
	 *            电子账户升级输入
	 * @param oldAcctInfo
	 *            原账户信息
	 * @return 级别调整状态
	 */
	private static DpElecAcctUpgradeResult registerUpgradeResult(DpElecAcctUpgradeInput cplIn, DpaAccount oldAcctInfo) {

		// 分析升级结果
		DpElecAcctUpgradeResult upgradeResult = analysisUpgradeResult(cplIn, oldAcctInfo);

		// 登记电子账户级别变更登记簿（升级）
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		DpbLevelChange levelChange = BizUtil.getInstance(DpbLevelChange.class);

		levelChange.setAcct_no(oldAcctInfo.getAcct_no());// 账号
		levelChange.setRequest_seq(runEnvs.getInitiator_seq());// 申请流水
		levelChange.setRequest_date(runEnvs.getInitiator_date());// 申请日期
		levelChange.setAcct_level_change_type(E_ACCTLEVELCHANGETYPE.UPGRADE);// 账户级别变更类型
		levelChange.setBefore_acct_level(oldAcctInfo.getAcct_level());// 变更前账户级别
		levelChange.setAfter_acct_level(cplIn.getAcct_level());// 变更后账户级别
		levelChange.setId_check_result(cplIn.getId_check_result());// 身份核查结果
		levelChange.setFace_check_result(cplIn.getFace_check_result());// 人脸识别结果
		levelChange.setFace_ind(cplIn.getFace_ind());// 面签标志
		levelChange.setLevel_change_status(upgradeResult.getLevel_change_status());// 级别调整状态
		levelChange.setFailure_reason(upgradeResult.getFailure_reason());// 失败原因
		levelChange.setTrxn_channel(runEnvs.getChannel_id());// 交易渠道
		levelChange.setTrxn_date(runEnvs.getTrxn_date());// 交易日期
		levelChange.setTrxn_seq(runEnvs.getTrxn_seq());// 交易流水
		levelChange.setTrxn_branch(runEnvs.getTrxn_branch());// 交易机构
		levelChange.setTrxn_teller(runEnvs.getTrxn_teller());// 交易柜员
		levelChange.setTrxn_time(runEnvs.getComputer_time());// 交易时间

		DpbLevelChangeDao.insert(levelChange);

		return upgradeResult;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年11月28日-上午11:45:16</li>
	 *         <li>功能说明：电子账户级别变更</li>
	 *         </p>
	 * @param acctInfo
	 *            账户信息
	 * @param targetLevel
	 *            目标账户级别
	 */
	private static void modifyAccountLevel(DpaAccount acctInfo, E_ACCTLEVEL targetLevel) {

		// 克隆，保留旧对象
		DpaAccount oldAcctInfo = BizUtil.clone(DpaAccount.class, acctInfo);

		// 更新账户级别
		acctInfo.setAcct_level(targetLevel);

		DpaAccountDao.updateOne_odb1(acctInfo);

		// 登记审计日志
		ApDataAuditApi.regLogOnUpdateBusiness(oldAcctInfo, acctInfo);
	}

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月26日-上午11:21:56</li>
	 *         <li>功能说明：电子账户升级人工审核</li>
	 *         </p>
	 * @param cplIn
	 */
	public static void upgradeManualAudit(DpEleccAcctUpgradeManualAuditIn cplIn) {
		bizlog.method(" DpElectronicAccountLevelAdjust.upgradeManualAudit begin >>>>>>>>>>>>>>>>");

		// 必输检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号
		BizUtil.fieldNotNull(cplIn.getId_check_result(), DpDict.A.id_check_result.getId(), DpDict.A.id_check_result.getLongName());// 身份核查结果
		BizUtil.fieldNotNull(cplIn.getFace_check_result(), DpDict.A.face_check_result.getId(), DpDict.A.face_check_result.getLongName());// 人脸识别结果

		// 这是人工终审，OCR要么通过要么不通过，不能是核查中
		if (cplIn.getFace_check_result() == E_OCRCHECKRESULT.CHECK || cplIn.getId_check_result() == E_OCRCHECKRESULT.CHECK) {

			throw DpErr.Dp.E0504();
		}

		// 带锁定位账户信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, true);

		// 查询是否有受理中的记录，如果没有，则报错
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		
		DpbLevelChange acctLevelInfo = SqlDpElectronicAccountDao.selAcctLevelChangeAcceptedOne(cplIn.getAcct_no(), 
				runEnvs.getBusi_org_id(), false);
		
		if(CommUtil.isNull(acctLevelInfo)) {
			DpErr.Dp.E0510();
		}

		// 分析升级审核结果
		DpElecAcctUpgradeInput cplAnalyIn = BizUtil.getInstance(DpElecAcctUpgradeInput.class);

		cplAnalyIn.setAcct_no(acctInfo.getAcct_no());
		cplAnalyIn.setAcct_level(acctLevelInfo.getAfter_acct_level());
		cplAnalyIn.setFace_check_result(cplIn.getFace_check_result());
		cplAnalyIn.setFace_ind(E_YESORNO.NO);
		cplAnalyIn.setId_check_result(cplIn.getId_check_result());

		DpElecAcctUpgradeResult upgradeResult = analysisUpgradeResult(cplAnalyIn, acctInfo);

		// 更新级别调整状态
		acctLevelInfo.setLevel_change_status(upgradeResult.getLevel_change_status());
		acctLevelInfo.setId_check_result(cplIn.getId_check_result());
		acctLevelInfo.setFace_check_result(cplIn.getFace_check_result());
		acctLevelInfo.setFailure_reason(upgradeResult.getFailure_reason());

		DpbLevelChangeDao.updateOne_odb1(acctLevelInfo);

		// 更新账户表账户级别
		if (upgradeResult.getLevel_change_status() == E_LEVELCHANGESTATUS.SUCCESS) {

			modifyAccountLevel(acctInfo, acctLevelInfo.getAfter_acct_level());
		}

		bizlog.method(" DpElectronicAccountLevelAdjust.upgradeManualAudit end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年11月26日-上午11:21:56</li>
	 *         <li>功能说明：电子账户升级撤销</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 */
	public static void upgradeCancel(String acctNo) {
		bizlog.method(" DpElectronicAccountLevelAdjust.upgradeCancel begin >>>>>>>>>>>>>>>>");

		// 必输检查
		BizUtil.fieldNotNull(acctNo, SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号

		// 带锁定位账户信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(acctNo, null, false);

		// 查询是否有受理中的记录，如果没有，则报未有可撤回记录
		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		
		DpbLevelChange acctLevelInfo = SqlDpElectronicAccountDao.selAcctLevelChangeAcceptedOne(acctNo, 
				runEnvs.getBusi_org_id(), false);
		
		if(CommUtil.isNull(acctLevelInfo)) {
			DpErr.Dp.E0512();
		}

		// 更新级别调整状态
		acctLevelInfo.setLevel_change_status(E_LEVELCHANGESTATUS.CANCEL);

		DpbLevelChangeDao.updateOne_odb1(acctLevelInfo);
		
		// 更新账户表账户级别
		modifyAccountLevel(acctInfo, acctLevelInfo.getBefore_acct_level());

		bizlog.method(" DpElectronicAccountLevelAdjust.upgradeCancel end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author jiefeng
	 *         <p>
	 *         <li>2019年11月26日-上午11:21:56</li>
	 *         <li>功能说明：电子账户级别调整信息查询</li>
	 *         </p>
	 * @param cplIn
	 *            电子账户级别调整信息查询输入
	 * @return 电子账户级别调整信息查询输出
	 */
	public static Options<DpAccountLevelChangeInfo> levelChangeQuery(DpAccountLevelChangeInfoQueryIn cplIn) {

		bizlog.method(" DpElectronicAccountLevelAdjust.levelChangeQuery begin >>>>>>>>>>>>>>>>");

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String acctNo = "";

		if (CommUtil.isNotNull(cplIn.getAcct_no())) {

			DpaAccount acctInfo = DpToolsApi.accountInquery(cplIn.getAcct_no(), null);

			acctNo = acctInfo.getAcct_no();
		}

		// 翻页查询
		Page<DpAccountLevelChangeInfo> page = SqlDpElectronicAccountDao.selElectronicAccountLevelChangeList(acctNo, cplIn.getAcct_level_change_type(), cplIn.getRequest_date(),
				cplIn.getLevel_change_status(), runEnvs.getBusi_org_id(), runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(page.getRecordCount());

		// 输出对象集合实例化
		Options<DpAccountLevelChangeInfo> listOut = new DefaultOptions<DpAccountLevelChangeInfo>();

		// 添加对象到集合
		listOut.addAll(page.getRecords());

		bizlog.method(" DpElectronicAccountQuery.levelChangeQuery end <<<<<<<<<<<<<<<<");

		// 返回集合对象
		return listOut;
	}
}
