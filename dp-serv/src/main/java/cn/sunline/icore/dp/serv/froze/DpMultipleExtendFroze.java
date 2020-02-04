package cn.sunline.icore.dp.serv.froze;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFroze;
import cn.sunline.icore.dp.base.tables.TabDpFrozeBase.DpbFrozeDao;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpExtendFrozeBaseIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_FROZESOURCE;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpExtendFrozeObjectIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpExtendFrozeObjectOut;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpMultiExtendFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpMultiExtendFrozeOut;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZEOBJECT;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明： 多对象续冻
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月18日-下午1:39:56</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月18日-zhoumy：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpMultipleExtendFroze {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpMultipleExtendFroze.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月9日-上午11:51:39</li>
	 *         <li>功能说明：续冻处理主程序</li>
	 *         </p>
	 * @param cplIn
	 *            续冻输入接口
	 */
	public static DpMultiExtendFrozeOut doMain(DpMultiExtendFrozeIn cplIn) {

		bizlog.method("DpMultipleExtendFroze.doMain begin <<<<<<<<<<<<<<<<<<<<");
		bizlog.debug(">>>>>>>cplIn=[%s]", cplIn);

		// 续冻检查
		DpbFroze frozeFirstInfo = checkMain(cplIn);

		// 初始化输出
		DpMultiExtendFrozeOut cplOut = BizUtil.getInstance(DpMultiExtendFrozeOut.class);

		// 记录中记录数
		long totalCount = 0;

		// 循环续冻
		for (DpExtendFrozeObjectIn extendFrozeObjIn : cplIn.getList_extendfroze_object()) {

			// 获取续冻信息
			DpbFroze frozeInfo = getExtendFrozeInfo(extendFrozeObjIn, cplIn.getFroze_no(), frozeFirstInfo.getFroze_object_type(), E_YESORNO.NO);

			// 接口转换
			DpExtendFrozeBaseIn cplExtendBaseIn = switchInterface(cplIn, extendFrozeObjIn);

			// 更新冻结解冻登记簿
			DpBaseServiceApi.modifyExtendFroze(frozeInfo, cplExtendBaseIn);

			// 初始化续冻信息输出
			DpExtendFrozeObjectOut extendFrozeObjOut = BizUtil.getInstance(DpExtendFrozeObjectOut.class);

			// 登记相关信息输出
			extendFrozeObjOut.setCust_no(frozeInfo.getCust_no()); // 客户号
			extendFrozeObjOut.setCard_no(frozeInfo.getCard_no()); // 卡号
			extendFrozeObjOut.setAcct_no(frozeInfo.getAcct_no()); // 账号
			extendFrozeObjOut.setCcy_code(frozeInfo.getCcy_code()); // 货币代码
			extendFrozeObjOut.setExtend_froze_amt(extendFrozeObjIn.getExtend_froze_amt()); // 续冻金额
			extendFrozeObjOut.setFroze_due_date(frozeInfo.getFroze_due_date()); // 冻结到期日
			extendFrozeObjOut.setFroze_reason(frozeInfo.getFroze_reason()); // 冻结原因
			extendFrozeObjOut.setFroze_bal(frozeInfo.getFroze_bal()); // 冻结余额
			extendFrozeObjOut.setFroze_status(frozeInfo.getFroze_status());// 冻结状态

			// 查询子账户信息(填入输出接口中)
			if (frozeInfo.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {

				// 查询存款子账户表
				DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOne_odb1(frozeInfo.getAcct_no(), frozeInfo.getFroze_object(), true);

				extendFrozeObjOut.setSub_acct_seq(subAcctInfo.getSub_acct_seq());// 子账号序号
				extendFrozeObjOut.setProd_id(subAcctInfo.getProd_id()); // 产品编号
				extendFrozeObjOut.setAcct_name(subAcctInfo.getSub_acct_name()); // 账户名称
			}
			else if (frozeInfo.getFroze_object_type() == E_FROZEOBJECT.ACCT) {

				// 查询存款子账户表
				DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(frozeInfo.getFroze_object(), true);

				extendFrozeObjOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
			}

			cplOut.getList_extendfroze_object().add(extendFrozeObjOut);// 续冻信息输出列表

			totalCount++;
		}

		cplOut.setFroze_no(cplIn.getFroze_no());// 冻结编号

		BizUtil.getTrxRunEnvs().setTotal_count(totalCount);// 返回总记录数

		bizlog.debug("ExtendFrozeOut=[%s]", cplOut);
		bizlog.method("DpMultipleExtendFroze.doMain end <<<<<<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月9日-上午11:51:39</li>
	 *         <li>功能说明：校验输入值是否合法主程序</li>
	 *         </p>
	 * @param cplIn
	 *            续冻输入接口
	 * @return 首条冻结信息
	 */
	public static DpbFroze checkMain(DpMultiExtendFrozeIn cplIn) {

		bizlog.method("DpMultipleExtendFroze.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>>>cplIn=[%s]", cplIn);

		// 检查续冻公用数据
		DpbFroze frozeFirstInfo = checkInputData(cplIn);

		// 循环检查续冻信息
		for (DpExtendFrozeObjectIn extendFrozeObjIn : cplIn.getList_extendfroze_object()) {

			// 获取续冻信息
			DpbFroze frozeInfo = getExtendFrozeInfo(extendFrozeObjIn, cplIn.getFroze_no(), frozeFirstInfo.getFroze_object_type(), E_YESORNO.YES);

			// 接口转换
			DpExtendFrozeBaseIn cplExtendBaseIn = switchInterface(cplIn, extendFrozeObjIn);

			// 续冻许可检查
			DpBaseServiceApi.checkExtendFrozeLicense(frozeInfo, cplExtendBaseIn);
		}

		bizlog.method("DpMultipleExtendFroze.checkMain end <<<<<<<<<<<<<<<<<<<<");

		return frozeFirstInfo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月16日-下午7:17:17</li>
	 *         <li>功能说明：检查解冻公用输入数据</li>
	 *         </p>
	 * @param cplIn
	 *            续冻输入接口
	 * @return 首条冻结信息
	 */
	private static DpbFroze checkInputData(DpMultiExtendFrozeIn cplIn) {
		bizlog.method(" DpMultipleExtendFroze.checkInputData begin >>>>>>>>>>>>>>>>");

		// 冻结编号不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_no(), SysDict.A.froze_no.getId(), SysDict.A.froze_no.getLongName());

		// 续冻对象不可为null
		if (cplIn.getList_extendfroze_object() == null || cplIn.getList_extendfroze_object().size() == 0) {

			throw DpErr.Dp.E0103();
		}

		for (DpExtendFrozeObjectIn extendFrozeObjIn : cplIn.getList_extendfroze_object()) {

			// 冻结原因不能为空
			BizUtil.fieldNotNull(extendFrozeObjIn.getFroze_reason(), SysDict.A.froze_amt.getId(), SysDict.A.froze_amt.getLongName());

			// 检验冻结原因合法性(下拉字典)
			ApDropListApi.exists(DpConst.FROZE_REASON, extendFrozeObjIn.getFroze_reason());

			// 续冻金额为空 则默认取0
			BigDecimal extendFrozeAmt = CommUtil.nvl(extendFrozeObjIn.getExtend_froze_amt(), BigDecimal.ZERO);

			// 检查续冻金额和冻结到期日是否至少输入一个
			if (CommUtil.equals(extendFrozeAmt, BigDecimal.ZERO) && CommUtil.isNull(extendFrozeObjIn.getFroze_due_date())) {

				throw DpErr.Dp.E0014(DpBaseDict.A.extend_froze_amt.getId(), SysDict.A.froze_due_date.getId());
			}
		}

		// 取出首条冻结信息
		DpbFroze frozeFirstInfo = DpFrozePublic.getFirstForzeInfo(cplIn.getFroze_no());

		// 司法冻结检查司法信息
		if (frozeFirstInfo.getFroze_source() == E_FROZESOURCE.EXTERNAL) {

			DpFrozePublic.checkLawFroze(cplIn);
		}

		bizlog.method(" DpMultipleExtendFroze.checkInputData end <<<<<<<<<<<<<<<<");

		return frozeFirstInfo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月16日-上午10:43:29</li>
	 *         <li>功能说明：获取续冻信息</li>
	 *         </p>
	 * @param cplIn
	 *            续冻对象输入
	 * @param frozeNo
	 *            冻结编号
	 * @param frozeObjectType
	 *            冻结对象类型
	 * @return 冻结记录
	 */
	private static DpbFroze getExtendFrozeInfo(DpExtendFrozeObjectIn cplIn, String frozeNo, E_FROZEOBJECT frozeObjectType, E_YESORNO lockFlag) {

		bizlog.method(" DpMultipleExtendFroze.getExtendFrozeInfo begin >>>>>>>>>>>>>>>>");

		String frozeObj = "";// 冻结对象

		if (frozeObjectType == E_FROZEOBJECT.SUBACCT) {// 指定子账号

			// 账号不能为空
			BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

			// 子账户序号不能为空
			BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());

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
		else if (frozeObjectType == E_FROZEOBJECT.ACCT) {// 指定账号

			// 账号或卡号不能为空
			BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

			frozeObj = acctInfo.getAcct_no();
		}
		else if (frozeObjectType == E_FROZEOBJECT.CARD) {// 指定卡号

			// 卡号不能为空
			BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

			DpaCard cardInfo = DpToolsApi.getCardInfor(cplIn.getAcct_no(), true);

			// 卡冻结是冻结默认账号
			frozeObj = cardInfo.getAcct_no();
		}
		else if (frozeObjectType == E_FROZEOBJECT.CUST) {// 指定客户号

			// 客户号不能为空
			BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

			frozeObj = cplIn.getCust_no();
		}
		else {
			throw APPUB.E0026(DpSysDict.A.froze_object_type.getLongName(), frozeObjectType.getValue());
		}

		DpbFroze frozeInfo;

		// 查询冻结解冻登记簿(带锁)
		if (lockFlag == E_YESORNO.YES) {
			frozeInfo = DpbFrozeDao.selectOneWithLock_odb1(frozeNo, frozeObj, false);
		}
		else {
			frozeInfo = DpbFrozeDao.selectOne_odb1(frozeNo, frozeObj, false);
		}

		// 检查冻结编号是否存在(在数据库中,冻结登记簿)
		if (frozeInfo == null) {
			throw APPUB.E0024(OdbFactory.getTable(DpbFroze.class).getLongname(), SysDict.A.froze_no.getId(), frozeNo, DpBaseDict.A.froze_object.getId(), frozeObj);
		}

		bizlog.method(" DpMultipleExtendFroze.getExtendFrozeInfo end <<<<<<<<<<<<<<<<");
		return frozeInfo;

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年2月7日-下午3:18:16</li>
	 *         <li>功能说明：转换接口</li>
	 *         </p>
	 * @param cplIn
	 *            多个对象续冻输入
	 * @param cplObjIn
	 *            续冻对象
	 */
	private static DpExtendFrozeBaseIn switchInterface(DpMultiExtendFrozeIn cplIn, DpExtendFrozeObjectIn cplObjIn) {

		DpExtendFrozeBaseIn cplOut = BizUtil.getInstance(DpExtendFrozeBaseIn.class);

		cplOut.setFroze_due_date(cplObjIn.getFroze_due_date());
		cplOut.setExtend_froze_amt(cplObjIn.getExtend_froze_amt());
		cplOut.setEnforced_legal_dept(cplIn.getEnforced_legal_dept());
		cplOut.setEnforced_legal_dept_name(cplIn.getEnforced_legal_dept_name());
		cplOut.setFroze_no(cplIn.getFroze_no());
		cplOut.setFroze_reason(cplObjIn.getFroze_reason());
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
