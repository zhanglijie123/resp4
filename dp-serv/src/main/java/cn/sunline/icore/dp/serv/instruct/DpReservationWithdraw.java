package cn.sunline.icore.dp.serv.instruct;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInstructDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbReservationBook;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbReservationBookDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpReservationInfo;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.LargeReserWithdrawMainIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.LargeReserWithdrawMainOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.LargeReserWithdrawQryIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.LargeReserWithdrawQryOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.LargeReserWithdrawSignIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.LargeReserWithdrawSignOut;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明：
 * </p>
 * 
 * @Author shenxy
 *         <p>
 *         <li>2017年7月25日-下午4:15:14</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年7月25日-shenxy：约定转账相关处理</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpReservationWithdraw {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpReservationWithdraw.class);

	/**
	 * 大额支取预约签约
	 * 
	 * @param cplIn
	 * @return
	 */
	public static LargeReserWithdrawSignOut reservationWithdrawSign(LargeReserWithdrawSignIn cplIn) {
		bizlog.method(" DpReservationWithdraw.reservationWithdrawSign begin >>>>>>>>>>>>>>>>");
		bizlog.debug("DpReservationWithdraw.cplIn = [%s]", cplIn);

		// 检查输入数据
		checkReserInputData(cplIn);

		// 定位账号
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		// 获取存款子账户信息
		// 子账户定位
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setAcct_type(cplIn.getAcct_type());
		acctAccessIn.setProd_id(cplIn.getProd_id());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);
		// 检查交易密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 生成预约号
		String reservationNo = ApSeqApi.genSeq("RESERVATION_NO");

		insertReservationBook(reservationNo, cplIn, acctAccessOut);

		// 初始化输出接口
		LargeReserWithdrawSignOut cplOut = BizUtil.getInstance(LargeReserWithdrawSignOut.class);
		cplOut.setReservation_no(reservationNo);// 预约号
		cplOut.setAcct_no(acctInfo.getAcct_no());// 账号
		cplOut.setReservation_date(cplIn.getReservation_date());// 预约日期
		cplOut.setReservation_amt(cplIn.getReservation_amt());// 预约金额

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpReservationWithdraw.reservationWithdrawSign end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * 预约支取登记簿
	 * 
	 * @param subAccount
	 * @param cplIn
	 * @param agreeTrsfOppInfo
	 * @param agreeNo
	 * @param nextExecDate
	 */
	private static void insertReservationBook(String reservationNo, LargeReserWithdrawSignIn cplIn, DpAcctAccessOut acctAccessOut) {
		bizlog.method(" DpReservationWithdraw.insertReservationBook begin >>>>>>>>>>>>>>>>");

		// 初始化约定转账协议表
		DpbReservationBook reservationInfo = BizUtil.getInstance(DpbReservationBook.class);

		reservationInfo.setReservation_no(reservationNo);// 预约编号
		reservationInfo.setAcct_no(acctAccessOut.getAcct_no());// 账号
		reservationInfo.setSub_acct_seq(acctAccessOut.getSub_acct_seq());// 子账户序号
		reservationInfo.setCcy_code(acctAccessOut.getCcy_code());// 货币代号
		reservationInfo.setCust_no(acctAccessOut.getCust_no());// 客户号
		reservationInfo.setReservation_date(cplIn.getReservation_date());// 预约日期
		reservationInfo.setReservation_amt(cplIn.getReservation_amt());// 预约金额
		reservationInfo.setWithdraw_date(null);// 支取日期
		reservationInfo.setWithdrawn_amt(BigDecimal.ZERO);// 已支取金额
		reservationInfo.setReservation_status(E_STATUS.VALID);// 预约状态
		reservationInfo.setData_create_user(BizUtil.getTrxRunEnvs().getTrxn_teller()); // 数据创建柜员
		reservationInfo.setData_create_time(BizUtil.getTrxRunEnvs().getComputer_time()); // 数据创建时间
		reservationInfo.setData_update_user(null); // 数据更新操作员
		reservationInfo.setData_update_time(null); // 数据更新时间
		reservationInfo.setData_version(0L); // 数据版本号

		DpbReservationBookDao.insert(reservationInfo);

		bizlog.method(" DpReservationWithdraw.insertReservationBook end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author zpw
	 *         <p>
	 *         <li>2018年11月16日-下午4:03:10</li>
	 *         <li>功能说明：预约支取输入检查</li>
	 *         </p>
	 * @param cplIn
	 */
	private static void checkReserInputData(LargeReserWithdrawSignIn cplIn) {
		bizlog.method(" DpReservationWithdraw.checkReserInputData begin >>>>>>>>>>>>>>>>");

		// 账号不为空
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 货币代号不为空
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 验密标志不为空
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		// 预约日期不能为空
		BizUtil.fieldNotNull(cplIn.getReservation_date(), DpDict.A.reservation_date.getId(), DpDict.A.reservation_date.getLongName());

		// 预约金额不能为空
		BizUtil.fieldNotNull(cplIn.getReservation_amt(), DpDict.A.reservation_amt.getId(), DpDict.A.reservation_amt.getLongName());

		// 预约日期需在当前交易日期N个工作日之后

		String days = ApBusinessParmApi.getValue(DpConst.BUSINESS_CODE, "RESERVATION_ADVANCE_TIME");

		String trxDate = BizUtil.dateAdd("day", BizUtil.getTrxRunEnvs().getTrxn_date(), Integer.parseInt(days));

		if (CommUtil.compare(cplIn.getReservation_date(), trxDate) < 0) {

			throw DpErr.Dp.E0459(Long.parseLong(days));
		}

		bizlog.method(" DpReservationWithdraw.checkReserInputData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * 大额支取预约查询
	 * 
	 * @param cplIn
	 * @return
	 */
	public static LargeReserWithdrawQryOut reservationWithdrawQry(LargeReserWithdrawQryIn cplIn) {
		bizlog.method(" DpReservationWithdraw.reservationWithdrawQry begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 获取公共变量

		String orgId = runEnvs.getBusi_org_id();// 取得法人代码

		// 动态查询大额支取预约表
		Page<DpbReservationBook> page = SqlDpInstructDao.selReservationInfo(cplIn.getReservation_no(), cplIn.getAcct_no(), cplIn.getReservation_status(), orgId,
				cplIn.getStart_date(), cplIn.getEnd_date(), runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(page.getRecordCount());// 返回总记录数

		// 初始化输出接口
		LargeReserWithdrawQryOut cplOut = BizUtil.getInstance(LargeReserWithdrawQryOut.class);

		if (CommUtil.isNull(page.getRecords()) && page.getRecords().size() == 0) {
			return cplOut;
		}

		Options<DpReservationInfo> cplInfoList = cplOut.getList01();

		for (DpbReservationBook DpbReservationBook : page.getRecords()) {

			DpReservationInfo reservationInfo = BizUtil.getInstance(DpReservationInfo.class);

			reservationInfo.setAcct_no(DpbReservationBook.getAcct_no());
			reservationInfo.setCcy_code(DpbReservationBook.getCcy_code());
			reservationInfo.setCust_no(DpbReservationBook.getCust_no());
			reservationInfo.setReservation_amt(DpbReservationBook.getReservation_amt());
			reservationInfo.setReservation_date(DpbReservationBook.getReservation_date());
			reservationInfo.setReservation_no(DpbReservationBook.getReservation_no());
			reservationInfo.setReservation_status(DpbReservationBook.getReservation_status());
			reservationInfo.setWithdraw_date(DpbReservationBook.getWithdraw_date());
			reservationInfo.setWithdrawn_amt(DpbReservationBook.getWithdrawn_amt());

			// 获取存款子账户信息
			DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			acctAccessIn.setAcct_no(DpbReservationBook.getAcct_no());
			acctAccessIn.setSub_acct_seq(DpbReservationBook.getSub_acct_seq());
			acctAccessIn.setCcy_code(DpbReservationBook.getCcy_code());

			DpAcctAccessOut acctAccessOut = DpToolsApi.subAcctInquery(acctAccessIn);

			DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

			reservationInfo.setAcct_bal(subAcct.getAcct_bal());
			reservationInfo.setAcct_name(acctAccessOut.getAcct_name());
			reservationInfo.setSub_acct_seq(subAcct.getSub_acct_seq());

			cplInfoList.add(reservationInfo);
		}

		cplOut.setList01(cplInfoList);

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpReservationWithdraw.reservationWithdrawQry end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * 大额支取预约维护
	 * 
	 * @param cplIn
	 * @return
	 */
	public static LargeReserWithdrawMainOut reservationWithdrawMain(LargeReserWithdrawMainIn cplIn) {
		bizlog.method(" DpReservationWithdraw.reservationWithdrawMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 协议号不为空
		BizUtil.fieldNotNull(cplIn.getReservation_no(), DpDict.A.reservation_no.getId(), DpDict.A.reservation_no.getLongName());

		// 验密标志不为空
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		// 预约日期不能为空
		BizUtil.fieldNotNull(cplIn.getReservation_date(), DpDict.A.reservation_date.getId(), DpDict.A.reservation_date.getLongName());

		// 预约金额不为空
		BizUtil.fieldNotNull(cplIn.getReservation_amt(), DpDict.A.reservation_amt.getId(), DpDict.A.reservation_amt.getLongName());

		// 预约货币不为空
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 预约状态不为空
		BizUtil.fieldNotNull(cplIn.getReservation_status(), DpDict.A.reservation_status.getId(), DpDict.A.reservation_status.getLongName());

		// 定位账号
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		// 检查交易密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}
		// 查询预约登记簿
		DpbReservationBook reserInfo = DpbReservationBookDao.selectOne_od1(acctInfo.getAcct_no(), cplIn.getReservation_no(), false);

		if (CommUtil.isNotNull(reserInfo)) {

			if (reserInfo.getReservation_status() == E_STATUS.INVALID) {// 已失效不需要维护

				return null;
			}
			else {

				DpbReservationBook DpbReservationBook = BizUtil.getInstance(DpbReservationBook.class);

				DpbReservationBook.setReservation_no(cplIn.getReservation_no());
				DpbReservationBook.setAcct_no(acctInfo.getAcct_no());
				DpbReservationBook.setCust_no(acctInfo.getCust_no());
				DpbReservationBook.setReservation_amt(cplIn.getReservation_amt());
				DpbReservationBook.setReservation_date(cplIn.getReservation_date());
				DpbReservationBook.setCcy_code(cplIn.getCcy_code());
				DpbReservationBook.setSub_acct_seq(reserInfo.getSub_acct_seq());
				DpbReservationBook.setReservation_status(cplIn.getReservation_status());
				DpbReservationBook.setData_update_user(BizUtil.getTrxRunEnvs().getTrxn_teller());
				DpbReservationBook.setData_update_time(BizUtil.getTrxRunEnvs().getComputer_time());
				DpbReservationBook.setData_create_time(reserInfo.getData_create_time());
				DpbReservationBook.setData_create_user(reserInfo.getData_create_user());
				DpbReservationBook.setData_version(reserInfo.getData_version() + 1L);

				DpbReservationBookDao.updateOne_od1(DpbReservationBook);
			}
		}

		// 初始化输出接口
		LargeReserWithdrawMainOut cplOut = BizUtil.getInstance(LargeReserWithdrawMainOut.class);

		cplOut.setReservation_no(cplIn.getReservation_no());// 预约号
		cplOut.setAcct_no(acctInfo.getAcct_no());// 账号
		cplOut.setReservation_date(cplIn.getReservation_date());// 预约日期
		cplOut.setReservation_amt(cplIn.getReservation_amt());// 预约金额

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpReservationWithdraw.LargeReserWithdrawMainOut end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	public static void updateReservationInfo(LargeReserWithdrawMainIn largeReserWithdrawMainIn) {

		bizlog.method(" DpReservationWithdraw.updateReservationInfo begin <<<<<<<<<<<<<<<<");

		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(largeReserWithdrawMainIn.getAcct_no(), largeReserWithdrawMainIn.getAcct_type(), false);

		DpbReservationBook reserInfo = DpbReservationBookDao.selectOne_od1(acctInfo.getAcct_no(), largeReserWithdrawMainIn.getReservation_no(), false);

		if (reserInfo == null) {
			throw APPUB.E0005(DpbReservationBook.class.getName(), DpDict.A.reservation_no.getLongName(), largeReserWithdrawMainIn.getReservation_no());
		}

		DpbReservationBook DpbReservationBook = BizUtil.getInstance(DpbReservationBook.class);

		DpbReservationBook.setReservation_no(largeReserWithdrawMainIn.getReservation_no());
		DpbReservationBook.setReservation_status(largeReserWithdrawMainIn.getReservation_status());
		DpbReservationBook.setWithdraw_date(largeReserWithdrawMainIn.getWithdraw_date());
		DpbReservationBook.setData_update_user(BizUtil.getTrxRunEnvs().getTrxn_teller());
		DpbReservationBook.setData_update_time(BizUtil.getTrxRunEnvs().getComputer_time());
		DpbReservationBook.setAcct_no(reserInfo.getAcct_no());
		DpbReservationBook.setCust_no(reserInfo.getCust_no());
		DpbReservationBook.setReservation_amt(reserInfo.getReservation_amt());
		DpbReservationBook.setReservation_date(reserInfo.getReservation_date());
		DpbReservationBook.setCcy_code(reserInfo.getCcy_code());
		DpbReservationBook.setSub_acct_seq(reserInfo.getSub_acct_seq());
		DpbReservationBook.setData_version(reserInfo.getData_version() + 1L);

		DpbReservationBookDao.updateOne_od1(DpbReservationBook);

		bizlog.method(" DpReservationWithdraw.updateReservationInfo end <<<<<<<<<<<<<<<<");

	}

}
