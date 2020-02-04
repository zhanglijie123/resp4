package cn.sunline.icore.dp.serv.froze;

import java.math.BigDecimal;
import java.util.HashMap;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFrozeDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeParameter.DppFrozeType;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeLawBase;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpFrozeObjectBase;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_FROZESOURCE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.iobus.DpChargeIobus;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ACCTBUSITYPE;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.AutoChrgInfo;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.base.logging.LogConfigManager.SystemType;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpFrozePublic {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpFrozePublic.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月7日-下午2:42:38</li>
	 *         <li>功能说明：根据冻结编号获取首条冻结记录</li>
	 *         </p>
	 * @param frozeNo
	 *            解冻服务输入接口
	 * @return 冻结记录
	 */
	public static DpbFroze getFirstForzeInfo(String frozeNo) {

		// 查询冻结登记簿
		DpbFroze frozeInfo = DpbFrozeDao.selectFirst_odb3(frozeNo, false);

		if (frozeInfo == null) {

			throw APPUB.E0005(OdbFactory.getTable(DpbFroze.class).getLongname(), SysDict.A.froze_no.getId(), frozeNo);
		}

		return frozeInfo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月26日-下午7:17:17</li>
	 *         <li>功能说明：检查法律冻结必输要素</li>
	 *         </p>
	 * @param cplIn
	 *            司法信息输入
	 */
	public static void checkLawFroze(DpFrozeLawBase cplIn) {
		bizlog.method(" DpFrozePublic.checkLawFroze begin >>>>>>>>>>>>>>>>");

		// 在海外，大多数银行司法冻结不强制要求输入司法信息
		if (!ApSystemParmApi.getOFF_ON(DpConst.FROZE_LAW_INFO_CHECK)) {
			return;
		}

		// 批量不检查
		if (SysUtil.getCurrentSystemType() == SystemType.batch) {
			return;
		}

		// 法律文书编号不能为空
		BizUtil.fieldNotNull(cplIn.getLegal_notice_no(), DpBaseDict.A.legal_notice_no.getId(), DpBaseDict.A.legal_notice_no.getLongName());

		// 法律文书类别不能为空
		BizUtil.fieldNotNull(cplIn.getLegal_notice_type(), DpBaseDict.A.legal_notice_type.getId(), DpBaseDict.A.legal_notice_type.getLongName());

		// 执法部门不能为空
		BizUtil.fieldNotNull(cplIn.getEnforced_legal_dept(), DpBaseDict.A.enforced_legal_dept.getId(), DpBaseDict.A.enforced_legal_dept.getLongName());

		// TODO:执法部门名称(可考虑使用直接查出来,当为其它时,则可输)
		BizUtil.fieldNotNull(cplIn.getEnforced_legal_dept_name(), DpBaseDict.A.enforced_legal_dept_name.getId(), DpBaseDict.A.enforced_legal_dept_name.getLongName());

		// 执法部门ID原因合法性(下拉字典)
		ApDropListApi.exists(DpConst.ENFORCED_LEGAL_DEPT, cplIn.getEnforced_legal_dept());

		// 执法人员证件种类不为空则检查合法性
		if (CommUtil.isNotNull(cplIn.getOfficer_doc_type())) {

			ApDropListApi.exists(DpConst.LAW_MAN_DOC_TYPE, cplIn.getOfficer_doc_type());
		}

		// 执法人员证件种类2不为空则检查合法性
		if (CommUtil.isNotNull(cplIn.getOfficer2_doc_type())) {

			ApDropListApi.exists(DpConst.LAW_MAN_DOC_TYPE, cplIn.getOfficer2_doc_type());
		}

		bizlog.method(" DpFrozePublic.checkLawFroze end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年6月8日-下午3:48:24</li>
	 *         <li>功能说明：生成冻结编号</li>
	 *         </p>
	 * @param frozeTypeDef
	 *            冻结分类码定义
	 * @return 冻结编号
	 */
	public static String getFrozeNo(DppFrozeType frozeTypeDef) {
		bizlog.method(" DpFrozePublic.getFrozeNo begin >>>>>>>>>>>>>>>>");

		HashMap<String, Object> mapSource = new HashMap<String, Object>();

		mapSource.put(DpBaseDict.A.froze_source.getId(), frozeTypeDef.getFroze_source() == E_FROZESOURCE.INTERIOR ? "2" : "1");

		HashMap<String, Object> mapType = new HashMap<String, Object>();

		mapType.put(DpBaseDict.A.froze_type.getId(), frozeTypeDef.getFroze_type().getValue());

		ApBufferApi.appendData(ApConst.INPUT_DATA_MART, mapSource);
		ApBufferApi.appendData(ApConst.PARM_DATA_MART, mapType);

		String frozeNo = ApSeqApi.genSeq(DpConst.FROZE_NO);

		bizlog.debug("frozeNo=[%s]", frozeNo);
		bizlog.method(" DpFrozePublic.getFrozeNo end <<<<<<<<<<<<<<<<");
		return frozeNo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年3月12日-下午3:02:18</li>
	 *         <li>功能说明：收费试算</li>
	 *         </p>
	 * @param AcctOrCard
	 *            账号或者卡号
	 * @param ccyCode
	 *            币种
	 */
	public static void autoChrg(String AcctOrCard, String ccyCode, BigDecimal trxnAmt, E_DEPTTRXNEVENT type) {

		bizlog.method(" DpFrozePublic.autoChrg begin >>>>>>>>>>>>>>>>");

		// 没有账户信息不能收费
		if (CommUtil.isNull(AcctOrCard)) {
			return;
		}

		// 账号
		String acctNo = "";
		// 子账号
		String subAcctNo = "";

		try {

			if (CommUtil.isNotNull(ccyCode)) {

				// 如果是卡下定期账户来冻结， 不用传账户类型就定位到活期户去扣费
				DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

				accessIn.setAcct_no(AcctOrCard);// 账号
				accessIn.setAcct_type(null);// 账号类型
				accessIn.setCcy_code(ccyCode);// 货币代号
				accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

				DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

				acctNo = accessOut.getAcct_no();
				subAcctNo = accessOut.getSub_acct_no();
			}
			else {

				// 获取账户信息：如果是卡下定期账户来冻结， 不用传账户类型就定位到活期户去扣费
				DpaAccount acctInfo = DpToolsApi.locateSingleAccount(AcctOrCard, null, false);

				DpaAccountRelate settlementAcct = DpaAccountRelateDao.selectFirst_odb4(acctInfo.getAcct_no(), E_DEMANDORTIME.DEMAND, E_YESORNO.YES, E_ACCTBUSITYPE.DEPOSIT, false);

				if (CommUtil.isNotNull(settlementAcct)) {
					acctNo = settlementAcct.getAcct_no();
					subAcctNo = settlementAcct.getSub_acct_no();
				}
			}
		}
		catch (Exception e) {

			bizlog.debug("Freeze charges, locate the current sub-account error, acct_no = [%s]", AcctOrCard);

			// 定位过程中出错，说明没合适子户，退出即可
			return;
		}

		// 没找到活期子户退出
		if (CommUtil.isNull(subAcctNo)) {
			return;
		}

		// 子账户信息
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		// 账户信息
		DpaAccount account = DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), true);

		// 加载数据集
		DpPublicCheck.addDataToCustBuffer(subAcct.getCust_no(), subAcct.getCust_type());

		ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(account));

		ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcct));

		// 场景收费试算
		DpChargeIobus.calcAutoChrg(subAcct, type, trxnAmt);

		// 冻结事件不能在交易后收费，因为全额冻住了交易后收费会失败
		AutoChrgInfo autoChrgIn = BizUtil.getTrxRunEnvs().getAuto_chrg_info();

		if (E_DEPTTRXNEVENT.DP_FROZE == type && CommUtil.isNotNull(autoChrgIn)) {

			DpChargeIobus.prcAutoChrg(autoChrgIn);
		}

		bizlog.method(" DpFrozePublic.autoChrg end <<<<<<<<<<<<<<<<");
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
	public static void addFrozeObjectDataBuffer(DpFrozeObjectBase cplObjectBase) {

		// 账户数据区
		if (CommUtil.isNotNull(cplObjectBase.getAcct_no())) {

			DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(cplObjectBase.getAcct_no(), true);

			ApBufferApi.addData(ApConst.ACCOUNT_DATA_MART, CommUtil.toMap(acctInfo));
		}

		// 子账户数据区
		if (CommUtil.isNotNull(cplObjectBase.getSub_acct_no())) {

			DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOne_odb1(cplObjectBase.getAcct_no(), cplObjectBase.getSub_acct_no(), true);

			ApBufferApi.addData(ApConst.SUB_ACCOUNT_DATA_MART, CommUtil.toMap(subAcctInfo));
		}

		// 卡数据区
		if (CommUtil.isNotNull(cplObjectBase.getCard_no())) {

			DpaCard cardInfo = DpToolsApi.getCardInfor(cplObjectBase.getCard_no(), true);

			ApBufferApi.addData(ApConst.CARD_DATA_MART, CommUtil.toMap(cardInfo));
		}

		// 加载客户数据集
		DpPublicCheck.addDataToCustBuffer(cplObjectBase.getCust_no(), cplObjectBase.getCust_type());
	}
}
