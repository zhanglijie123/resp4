package cn.sunline.icore.dp.serv.froze;

import java.math.BigDecimal;

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
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpExtendFrozeIn;
import cn.sunline.icore.dp.serv.type.ComDpFroze.DpExtendFrozeOut;
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
 * 文件功能说明： 续冻
 * </p>
 * 
 * @Author linshiq
 *         <p>
 *         <li>2017年1月18日-下午1:39:56</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年1月18日-linshiq：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpExtendFroze {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpExtendFroze.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月26日-下午7:17:17</li>
	 *         <li>功能说明：续冻处理主程序</li>
	 *         </p>
	 * @param cplIn
	 *            续冻输入接口
	 */
	public static DpExtendFrozeOut doMain(DpExtendFrozeIn cplIn) {

		bizlog.method("DpExtendFroze.doMain begin <<<<<<<<<<<<<<<<<<<<");
		bizlog.debug(">>>>>>>cplIn=[%s]", cplIn);

		// 续冻检查
		DpbFroze frozeInfo = checkMain(cplIn);

		// 接口转换
		DpExtendFrozeBaseIn cplExtendBaseIn = switchInterface(cplIn);

		// 续冻信息处理
		DpBaseServiceApi.modifyExtendFroze(frozeInfo, cplExtendBaseIn);

		// 初始化输出
		DpExtendFrozeOut cplOut = BizUtil.getInstance(DpExtendFrozeOut.class);

		cplOut.setFroze_no(cplIn.getFroze_no());// 冻结编号
		cplOut.setCust_no(frozeInfo.getCust_no()); // 客户号
		cplOut.setAcct_no(frozeInfo.getAcct_no()); // 账号
		cplOut.setFroze_due_date(frozeInfo.getFroze_due_date()); // 冻结到期日
		cplOut.setFroze_bal(frozeInfo.getFroze_bal()); // 冻结余额

		if (CommUtil.isNotNull(frozeInfo.getAcct_no())) {

			DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(frozeInfo.getFroze_object(), true);

			cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		}

		// 查询子账户信息(填入输出接口中)
		if (frozeInfo.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {

			DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOne_odb1(frozeInfo.getAcct_no(), frozeInfo.getFroze_object(), true);

			cplOut.setSub_acct_seq(subAcctInfo.getSub_acct_seq());// 子账号序号
		}

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method("DpExtendFroze.doMain end <<<<<<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月26日-下午7:17:17</li>
	 *         <li>功能说明：校验输入值是否合法主程序</li>
	 *         </p>
	 * @param cplIn
	 *            续冻输入接口
	 */
	public static DpbFroze checkMain(DpExtendFrozeIn cplIn) {

		bizlog.method("DpExtendFroze.checkMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>>>cplIn=[%s]", cplIn);

		// 检查续冻公用数据
		DpbFroze firstFrozeInfo = checkInputData(cplIn);

		// 获取续冻信息
		DpbFroze frozeInfo = getExtendFrozeInfo(cplIn, firstFrozeInfo);

		// 接口转换
		DpExtendFrozeBaseIn cplExtendBaseIn = switchInterface(cplIn);

		// 续冻许可检查
		DpBaseServiceApi.checkExtendFrozeLicense(frozeInfo, cplExtendBaseIn);

		bizlog.method("DpExtendFroze.checkMain end <<<<<<<<<<<<<<<<<<<<");

		return frozeInfo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月26日-下午7:17:17</li>
	 *         <li>功能说明：检查解冻公用输入数据</li>
	 *         </p>
	 * @param cplIn
	 *            续冻输入接口
	 * @return 第一条冻结记录
	 */
	private static DpbFroze checkInputData(DpExtendFrozeIn cplIn) {

		bizlog.method(" DpExtendFroze.checkInputData begin >>>>>>>>>>>>>>>>");

		// 冻结编号不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_no(), SysDict.A.froze_no.getId(), SysDict.A.froze_no.getLongName());

		// 冻结原因不能为空
		BizUtil.fieldNotNull(cplIn.getFroze_reason(), SysDict.A.froze_amt.getId(), SysDict.A.froze_amt.getLongName());

		// 检验冻结原因合法性(下拉字典)
		ApDropListApi.exists(DpConst.FROZE_REASON, cplIn.getFroze_reason());

		// 续冻金额为空 则默认取0
		BigDecimal extendFrozeAmt = CommUtil.nvl(cplIn.getExtend_froze_amt(), BigDecimal.ZERO);

		// 检查续冻金额和冻结到期日是否至少输入一个
		if (CommUtil.equals(extendFrozeAmt, BigDecimal.ZERO) && CommUtil.isNull(cplIn.getFroze_due_date())) {

			throw DpErr.Dp.E0014(DpBaseDict.A.extend_froze_amt.getId(), SysDict.A.froze_due_date.getId());
		}

		// 取出首条冻结信息
		DpbFroze frozeFirstInfo = DpFrozePublic.getFirstForzeInfo(cplIn.getFroze_no());

		// 检查法院续冻必输补充要素是否输入
		if (frozeFirstInfo.getFroze_source() == E_FROZESOURCE.EXTERNAL) {

			DpFrozePublic.checkLawFroze(cplIn);
		}

		bizlog.method(" DpExtendFroze.checkInputData end <<<<<<<<<<<<<<<<");

		return frozeFirstInfo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年12月26日-下午7:17:17</li>
	 *         <li>功能说明：获取续冻信息</li>
	 *         </p>
	 * @param cplIn
	 *            续冻对象输入
	 * @param firstFrozeInfo
	 *            首条冻结记录
	 * @return 冻结记录
	 */
	private static DpbFroze getExtendFrozeInfo(DpExtendFrozeIn cplIn, DpbFroze firstFrozeInfo) {

		bizlog.method(" DpExtendFroze.getExtendFrozeInfo begin >>>>>>>>>>>>>>>>");

		// 冻结记录本身就是单条冻结, 则不需要提供冻结对象信息, 直接返回
		if (CommUtil.compare(firstFrozeInfo.getTotal_count(), 1L) == 0) {

			bizlog.method(" DpExtendFroze.getExtendFrozeInfo end <<<<<<<<<<<<<<<<");
			return DpbFrozeDao.selectOneWithLock_odb1(firstFrozeInfo.getFroze_no(), firstFrozeInfo.getFroze_object(), false);
		}

		String frozeObj = "";// 冻结对象

		if (firstFrozeInfo.getFroze_object_type() == E_FROZEOBJECT.SUBACCT) {// 指定子账号

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
		else if (firstFrozeInfo.getFroze_object_type() == E_FROZEOBJECT.ACCT) {// 指定账号

			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

			frozeObj = acctInfo.getAcct_no();
		}
		else if (firstFrozeInfo.getFroze_object_type() == E_FROZEOBJECT.CARD) {// 指定卡号

			// 卡号不能为空
			BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

			DpaCard cardInfo = DpToolsApi.getCardInfor(cplIn.getAcct_no(), true);

			// 卡冻结是冻结默认账号
			frozeObj = cardInfo.getAcct_no();
		}
		else if (firstFrozeInfo.getFroze_object_type() == E_FROZEOBJECT.CUST) {// 指定客户号

			// 客户号不能为空
			BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

			frozeObj = cplIn.getCust_no();
		}
		else {
			throw APPUB.E0026(DpSysDict.A.froze_object_type.getLongName(), firstFrozeInfo.getFroze_object_type().getValue());
		}

		DpbFroze frozeInfo = DpbFrozeDao.selectOneWithLock_odb1(cplIn.getFroze_no(), frozeObj, false);

		// 检查冻结编号是否存在(在数据库中,冻结登记簿)
		if (frozeInfo == null) {
			throw APPUB.E0024(OdbFactory.getTable(DpbFroze.class).getLongname(), SysDict.A.froze_no.getId(), cplIn.getFroze_no(), DpBaseDict.A.froze_object.getId(), frozeObj);
		}

		bizlog.method(" DpExtendFroze.getExtendFrozeInfo end <<<<<<<<<<<<<<<<");
		return frozeInfo;
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
	private static DpExtendFrozeBaseIn switchInterface(DpExtendFrozeIn cplIn) {

		DpExtendFrozeBaseIn cplOut = BizUtil.getInstance(DpExtendFrozeBaseIn.class);

		cplOut.setExtend_froze_amt(cplIn.getExtend_froze_amt());
		cplOut.setFroze_due_date(cplIn.getFroze_due_date());
		cplOut.setFroze_no(cplIn.getFroze_no());
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
