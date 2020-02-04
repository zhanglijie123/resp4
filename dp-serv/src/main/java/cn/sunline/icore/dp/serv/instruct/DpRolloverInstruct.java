package cn.sunline.icore.dp.serv.instruct;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DATAOPERATE;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBaseDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInstructDao;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbRolloverBook;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbRolloverInstruct;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbRolloverInstructDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpFixedRenewHistoryQueryIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpFixedRenewHistoryQueryOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpQryRolloverInstructIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpQryRolloverInstructInfo;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpQryRolloverInstructOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpRenewHistoryInfo;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpRolloverInstructIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpRolloverInstructInfo;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpRolloverInstructOut;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_ORDERIND;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_ROLLTYPE;
import cn.sunline.icore.dp.sys.dict.DpSysDict;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_SUBACCTSTATUS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_AMTPERTWAY;
import cn.sunline.icore.sys.type.EnumType.E_DEALSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_SAVEORWITHDRAWALIND;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明： 定期转存指令维护与查询服务程序
 * </p>
 * 
 * @Author linshiq
 *         <p>
 *         <li>2017年3月13日-上午10:15:33</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月13日-linshiq：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpRolloverInstruct {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpRolloverInstruct.class);

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年3月6日-下午7:53:18</li>
	 *         <li>功能说明：定期转存指令维护主程序</li>
	 *         </p>
	 * @param cplIn
	 *            定期转存指令输入接口
	 * @return 定期转存指令输出接口
	 */
	public static DpRolloverInstructOut rolloverInstructMnt(DpRolloverInstructIn cplIn) {
		bizlog.method(" DpRolloverInstruct.rolloverInstruct begin >>>>>>>>>>>>>>>>");

		bizlog.debug("cplIn=[%s]", cplIn);

		// 检查传入数据
		DpAcctAccessOut accessOut = checkRolloverInstructData(cplIn);

		String acctNo = accessOut.getAcct_no();// 账号
		String subAcctNo = accessOut.getSub_acct_no();// 取出子账号
		String ccyCode = accessOut.getCcy_code();// 取出货币代号

		boolean addFlag = false;// 新增标志

		long sortNo = DpConst.START_SORT_VALUE - 1;// 初始化序号

		// 进行数据维护操作
		for (DpRolloverInstructInfo instInfo : cplIn.getList01()) {

			// 检查相关列表数据
			checkInputListData(instInfo, ccyCode);

			if (E_DATAOPERATE.ADD == instInfo.getOperater_ind()) {// 增加

				if (!addFlag) {

					String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();// 取出法人代码

					// 查询出存在的最大序号,为空则取默认值
					sortNo = SqlDpInstructDao.selMaxSerail(orgId, acctNo, subAcctNo, instInfo.getRoll_type(), E_DEALSTATUS.NO, false);

					addFlag = true;
				}

				sortNo++; // 序号自增

				addRolloverInstructInfo(cplIn, instInfo, acctNo, subAcctNo, sortNo);// 调用新增方法

			}
			else if (E_DATAOPERATE.MODIFY == instInfo.getOperater_ind()) {// 更新

				modifyRolloverInstructInfo(cplIn, instInfo, acctNo, subAcctNo);
			}
			else if (E_DATAOPERATE.DELETE == instInfo.getOperater_ind()) {// 删除

				delRolloverInstructInfo(instInfo, acctNo, subAcctNo);
			}
			else {
				throw APPUB.E0026(SysDict.A.operater_ind.getLongName(), instInfo.getOperater_ind().getValue());
			}
		}

		// 初始化输出接口
		DpRolloverInstructOut cplOut = BizUtil.getInstance(DpRolloverInstructOut.class);

		cplOut.setCard_no(accessOut.getCard_no());// 卡号
		cplOut.setAcct_no(accessOut.getAcct_no()); // 账号
		cplOut.setAcct_type(accessOut.getAcct_type()); // 账户类型
		cplOut.setAcct_name(accessOut.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(accessOut.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(accessOut.getCcy_code()); // 货币代码

		bizlog.debug("cplOut=[%s]", cplOut);

		bizlog.method(" DpRolloverInstruct.rolloverInstruct end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年3月6日-下午7:58:25</li>
	 *         <li>功能说明：定期转存指令数据检查</li>
	 *         </p>
	 * @param cplIn
	 *            定期转存指令输入接口
	 * @return 账户访问输出接口
	 */
	private static DpAcctAccessOut checkRolloverInstructData(DpRolloverInstructIn cplIn) {
		bizlog.method(" DpRolloverInstruct.checkRolloverInstructData begin >>>>>>>>>>>>>>>>");

		// 账号不可为空
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 子账户序号不可为空
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());

		// 验密标志不可为空
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		// 定位账号
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 查询账户子账户关系表,用于确定定活标志
		DpaAccountRelate acctRelateInfo = DpaAccountRelateDao.selectOne_odb1(acctInfo.getAcct_no(), cplIn.getSub_acct_seq(), false);

		if (acctRelateInfo == null) {

			throw APPUB.E0005(OdbFactory.getTable(DpaAccountRelate.class).getLongname(), SysDict.A.acct_no.getId(), cplIn.getAcct_no());
		}

		if (acctRelateInfo.getDd_td_ind() == E_DEMANDORTIME.DEMAND) {

			throw DpBase.E0280(acctRelateInfo.getAcct_no());
		}
		else {

			if (CommUtil.isNull(cplIn.getCcy_code())) {

				cplIn.setCcy_code(acctRelateInfo.getCcy_code());// 确保货币代号有值,定期户只拥有一个子账户序号
			}
			else {

				ApDropListApi.exists(ApConst.CURRENCY_DATA_MART, cplIn.getCcy_code());
			}
		}

		// 定位子账号信息
		DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOne_odb1(acctRelateInfo.getAcct_no(), acctRelateInfo.getSub_acct_no(), false);

		if (subAcctInfo == null) {

			throw APPUB.E0005(OdbFactory.getTable(DpaAccountRelate.class).getLongname(), SysDict.A.sub_acct_seq.getId(), cplIn.getSub_acct_seq());
		}

		// 检查交易密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 账户没有到期日则不能转存
		if (CommUtil.isNull(subAcctInfo.getDue_date())) {

			throw APPUB.E0001(SysDict.A.due_date.getId(), SysDict.A.due_date.getLongName());
		}

		// 检查账户状态
		if (subAcctInfo.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE) {

			throw DpBase.E0017(subAcctInfo.getAcct_no(), subAcctInfo.getSub_acct_seq());
		}

		// 账户名称不为空时,校验匹配性
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(cplIn.getAcct_name(), subAcctInfo.getSub_acct_name())) {

			throw DpErr.Dp.E0058(cplIn.getAcct_name(), subAcctInfo.getSub_acct_name());
		}

		/*
		 * DpCustVoucherInfo voucherOut =
		 * DpVoucherIobus.getCustVouchersInfo(subAcctInfo, acctInfo);
		 * 
		 * // 账号关联的凭证已做质押, 不允许设置转存指令 if (voucherOut.getCust_voch_status() ==
		 * E_CUSTVOCHSTAS.PLEDGE) {
		 * 
		 * throw DpErr.Dp.E0128(); }
		 */

		// 至少要存在一条定期转存指令信息
		if (cplIn.getList01() == null || cplIn.getList01().size() == 0) {

			throw DpErr.Dp.E0129();
		}

		// 调用输出
		DpAcctAccessOut accessOut = BizUtil.getInstance(DpAcctAccessOut.class);

		accessOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAcctInfo.getAcct_no()) ? null : cplIn.getAcct_no()); // 卡号
		accessOut.setCust_no(subAcctInfo.getCust_no()); // 客户号
		accessOut.setAcct_no(subAcctInfo.getAcct_no()); // 账号
		accessOut.setAcct_status(subAcctInfo.getSub_acct_status() == E_SUBACCTSTATUS.CLOSE ? E_ACCTSTATUS.CLOSE : E_ACCTSTATUS.NORMAL); // 子账户状态
		accessOut.setAcct_branch(subAcctInfo.getSub_acct_branch()); // 账务机构
		accessOut.setAcct_name(subAcctInfo.getSub_acct_name()); // 账户名称
		accessOut.setSub_acct_seq(subAcctInfo.getSub_acct_seq()); // 子账户序号
		accessOut.setCcy_code(subAcctInfo.getCcy_code()); // 货币代码
		accessOut.setSub_acct_no(subAcctInfo.getSub_acct_no()); // 子账号
		accessOut.setProd_id(subAcctInfo.getProd_id()); // 产品编号
		accessOut.setAcct_type(acctInfo.getAcct_type()); // 账户类型
		accessOut.setDd_td_ind(subAcctInfo.getDd_td_ind()); // 定活标志

		bizlog.method(" DpRolloverInstruct.checkRolloverInstructData end <<<<<<<<<<<<<<<<");
		return accessOut;
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年3月7日-上午11:18:59</li>
	 *         <li>功能说明：定期转存指令信息检查</li>
	 *         </p>
	 * @param instInfo
	 *            定期转存指令信息
	 * @param ccyCode
	 *            货币代号
	 */
	private static void checkInputListData(DpRolloverInstructInfo instInfo, String ccyCode) {
		bizlog.method(" DpRolloverInstruct.checkInputListData begin >>>>>>>>>>>>>>>>");

		// 操作标志不可为空
		BizUtil.fieldNotNull(instInfo.getOperater_ind(), SysDict.A.operater_ind.getId(), SysDict.A.operater_ind.getLongName());

		// 序号不可为空
		BizUtil.fieldNotNull(instInfo.getSerial_no(), SysDict.A.serial_no.getId(), SysDict.A.serial_no.getLongName());

		// 转续存类型不可为空
		BizUtil.fieldNotNull(instInfo.getRoll_type(), DpDict.A.roll_type.getId(), DpDict.A.roll_type.getLongName());

		// 转存金额处理方式不可为空
		BizUtil.fieldNotNull(instInfo.getRoll_amt_handling_method(), DpDict.A.roll_amt_handling_method.getId(), DpDict.A.roll_amt_handling_method.getLongName());

		// 转存金额处理方式值不可为空
		BizUtil.fieldNotNull(instInfo.getRoll_amt_handling_value(), DpDict.A.roll_amt_handling_value.getId(), DpDict.A.roll_amt_handling_value.getLongName());

		// 转存金额处理方式为百分比时,转存金额处理方式值需小于等于1大于0
		if (E_AMTPERTWAY.PERCENT == instInfo.getRoll_amt_handling_method()) {

			// if (CommUtil.compare(instInfo.getRoll_amt_handling_value(),
			// BigDecimal.ONE) > 0 ||
			// CommUtil.compare(instInfo.getRoll_amt_handling_value(),
			// BigDecimal.ZERO) <= 0) {

			// throw DpErr.Dp.E0127();
			// }

			instInfo.setRoll_amt_handling_value(BigDecimal.ONE);// TODO:暂时定为百分百
		}
		else {

			throw APPUB.E0026(DpDict.A.roll_amt_handling_method.getLongName(), instInfo.getRoll_amt_handling_method().getValue());
		}

		// // 检查业务是否存在
		// if (!E_ROLLTYPE.isIn(instInfo.getRoll_type())) {
		//
		// throw APPUB.E0026(DpDict.A.roll_type.getLongName(),
		// instInfo.getRoll_type().getValue());
		//
		// }

		// TODO:续存存期检查
		bizlog.method(" DpRolloverInstruct.checkInputListData end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年3月7日-上午11:45:05</li>
	 *         <li>功能说明：定期转存指令增加</li>
	 *         </p>
	 * @param cplIn
	 *            定期转存指令输入接口
	 * @param instInfo
	 *            定期转存指令信息
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 * @param sortNo
	 *            序号
	 */
	private static void addRolloverInstructInfo(DpRolloverInstructIn cplIn, DpRolloverInstructInfo instInfo, String acctNo, String subAcctNo, long sortNo) {
		bizlog.method(" DpRolloverInstruct.addRolloverInstructInfo begin >>>>>>>>>>>>>>>>");

		// 初始化定期转存指令表
		DpbRolloverInstruct rolloverInstInfo = BizUtil.getInstance(DpbRolloverInstruct.class);

		// 检查续存类型
		if (instInfo.getRoll_type() == E_ROLLTYPE.A3 || instInfo.getRoll_type() == E_ROLLTYPE.B3) {// 转续存类型为入内部账号

			// TODO:检查内部账号必须是暂收暂付、延时更新的内部账户

			rolloverInstInfo.setIn_acct_no(instInfo.getIn_acct_no());// 转入账号
			rolloverInstInfo.setIn_ccy_code(instInfo.getIn_ccy_code()); // 转入币种
		}
		else if (instInfo.getRoll_type() == E_ROLLTYPE.A1 || instInfo.getRoll_type() == E_ROLLTYPE.B1) {// 转续存类型为入活期

			// 检查转入子账号必须是活期
			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(instInfo.getIn_acct_no()); // 账号
			accessIn.setSub_acct_seq(null); // 子账户序号
			accessIn.setCcy_code(instInfo.getIn_ccy_code()); // 货币代码
			accessIn.setAcct_type(null); // 账户类型
			accessIn.setProd_id(null); // 产品编号
			accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND); // 定活标志
			accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.SAVE); // 存入支取标志

			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			// 币种不同时,需做结售汇处理
			if (!CommUtil.equals(accessOut.getCcy_code(), cplIn.getCcy_code())) {
				// TODO:
			}

			// 检查账户状态
			if (accessOut.getAcct_status() == E_ACCTSTATUS.CLOSE) {

				throw DpBase.E0017(accessOut.getAcct_no(), accessOut.getSub_acct_seq());
			}

			rolloverInstInfo.setIn_acct_no(instInfo.getIn_acct_no());// 转入账号
			rolloverInstInfo.setIn_ccy_code(instInfo.getIn_ccy_code()); // 转入币种
		}
		else if (instInfo.getRoll_type() == E_ROLLTYPE.A2 || instInfo.getRoll_type() == E_ROLLTYPE.B2) {// 转续存类型为入定期

			// 新开产品号不可为空
			BizUtil.fieldNotNull(instInfo.getNew_open_acct_prod(), DpDict.A.new_open_acct_prod.getId(), DpDict.A.new_open_acct_prod.getLongName());

			// 新开账户存期不可为空
			BizUtil.fieldNotNull(instInfo.getNew_open_acct_term(), DpDict.A.new_open_acct_term.getId(), DpDict.A.new_open_acct_term.getLongName());

			String inAcctNo = instInfo.getIn_acct_no();// 取出转入账号

			// TODO:转入账号为空则取原客户账号
			if (CommUtil.isNull(inAcctNo)) {

				inAcctNo = cplIn.getAcct_no();

				if (CommUtil.isNull(instInfo.getIn_ccy_code())) {// 转入币种为空则取原币种

					instInfo.setIn_ccy_code(cplIn.getCcy_code());
				}
			}

			// 定位账号信息
			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(inAcctNo, null, false);

			// 转入定期户,币种需一致
			if (!CommUtil.equals(cplIn.getCcy_code(), instInfo.getIn_ccy_code())) {

				throw DpErr.Dp.E0124(cplIn.getCcy_code(), instInfo.getIn_ccy_code());
			}

			// 查询产品信息
			DpfBase prodInfo = DpProductFactoryApi.getProdBaseInfo(instInfo.getNew_open_acct_prod());

			// 检查产品是否为定期产品
			if (prodInfo.getDd_td_ind() != E_DEMANDORTIME.TIME) {

				throw DpBase.E0122(instInfo.getNew_open_acct_prod());
			}

			// 查询产品账户类型关系控制
			// dpf_account_type acctType =
			// Dpf_account_typeDao.selectOne_odb1(instInfo.getNew_open_acct_prod(),
			// instInfo.getIn_ccy_code(), acctInfo.getAcct_type(), false);
			//
			// if (acctType == null) {
			//
			// throw
			// APPUB.E0025(OdbFactory.getTable(dpf_account_type.class).getLongname(),
			// SysDict.A.prod_id.getId(), instInfo.getNew_open_acct_prod(),
			// SysDict.A.ccy_code.getId(),
			// instInfo.getIn_ccy_code(), SysDict.A.acct_type.getId(),
			// acctInfo.getAcct_type());
			// }

			rolloverInstInfo.setIn_acct_no(acctInfo.getAcct_no());// 转入账号
			rolloverInstInfo.setIn_ccy_code(instInfo.getIn_ccy_code());// 转入币种
			rolloverInstInfo.setNew_open_acct_prod(instInfo.getNew_open_acct_prod()); // 新开账户产品号
			rolloverInstInfo.setNew_open_acct_term(instInfo.getNew_open_acct_term()); // 新开账户存期
		}
		else if (instInfo.getRoll_type() == E_ROLLTYPE.A4 || instInfo.getRoll_type() == E_ROLLTYPE.B4) {// 转续存类型为本金
			// TODO:业务暂不实现
		}
		else {
			throw APPUB.E0026(DpDict.A.roll_type.getLongName(), instInfo.getRoll_type().getValue());
		}

		rolloverInstInfo.setSub_acct_no(subAcctNo); // 子账号
		rolloverInstInfo.setAcct_no(acctNo); // 账号
		rolloverInstInfo.setRoll_type(instInfo.getRoll_type()); // 转续存类型
		rolloverInstInfo.setSerial_no(sortNo); // 序号
		rolloverInstInfo.setRoll_amt_handling_method(instInfo.getRoll_amt_handling_method()); // 转存金额处理方式
		rolloverInstInfo.setRoll_amt_handling_value(instInfo.getRoll_amt_handling_value()); // 转存金额处理方式值
		rolloverInstInfo.setHandling_status(E_DEALSTATUS.NO); // 处理状态
		rolloverInstInfo.setFailure_reason(null); // 失败原因

		DpbRolloverInstructDao.insert(rolloverInstInfo);

		bizlog.method(" DpRolloverInstruct.addRolloverInstructInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年3月7日-上午11:47:41</li>
	 *         <li>功能说明：定期转存指令更新</li>
	 *         </p>
	 * @param cplIn
	 *            定期转存指令输入接口
	 * @param instInfo
	 *            定期转存指令信息
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 */
	private static void modifyRolloverInstructInfo(DpRolloverInstructIn cplIn, DpRolloverInstructInfo instInfo, String acctNo, String subAcctNo) {
		bizlog.method(" DpRolloverInstruct.modifyRolloverInstructInfo begin >>>>>>>>>>>>>>>>");

		// 维护时必输数据版本号不可为空
		BizUtil.fieldNotNull(instInfo.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

		// 获取定期转存指令表信息
		DpbRolloverInstruct rolloverInstInfo = DpbRolloverInstructDao.selectOneWithLock_odb1(acctNo, subAcctNo, instInfo.getRoll_type(), instInfo.getSerial_no(), false);

		// 复制一份,做审计用
		DpbRolloverInstruct oldRolloverInstInfo = BizUtil.clone(DpbRolloverInstruct.class, rolloverInstInfo);

		if (rolloverInstInfo == null) {

			throw APPUB.E0025(OdbFactory.getTable(DpbRolloverInstruct.class).getLongname(), DpSysDict.A.sub_acct_no.getId(), subAcctNo, DpDict.A.roll_type.getId(), instInfo
					.getRoll_type().getValue(), SysDict.A.serial_no.getId(), "" + instInfo.getSerial_no());
		}

		// 检查数据版本号是否一致
		if (CommUtil.compare(rolloverInstInfo.getData_version(), instInfo.getData_version()) != 0) {

			throw APPUB.E0018(OdbFactory.getTable(DpbRolloverInstruct.class).getLongname());
		}

		// 检查处理状态,不处于未处理状态则抛错
		if (E_DEALSTATUS.NO != rolloverInstInfo.getHandling_status()) {

			throw DpErr.Dp.E0123();
		}

		// 检查续存类型
		if (instInfo.getRoll_type() == E_ROLLTYPE.A3 || instInfo.getRoll_type() == E_ROLLTYPE.B3) {// 转续存类型为入内部账号

			// TODO:检查内部账号必须是暂收暂付、延时更新的内部账户

			rolloverInstInfo.setIn_acct_no(instInfo.getIn_acct_no());// 转入账号
			rolloverInstInfo.setIn_ccy_code(instInfo.getIn_ccy_code()); // 转入币种

		}
		else if (instInfo.getRoll_type() == E_ROLLTYPE.A1 || instInfo.getRoll_type() == E_ROLLTYPE.B1) {// 转续存类型为入活期

			// 检查转入子账号必须是活期
			DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			accessIn.setAcct_no(instInfo.getIn_acct_no()); // 账号
			accessIn.setSub_acct_seq(null); // 子账户序号
			accessIn.setCcy_code(instInfo.getIn_ccy_code()); // 货币代码
			accessIn.setAcct_type(null); // 账户类型
			accessIn.setProd_id(null); // 产品编号
			accessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND); // 定活标志
			accessIn.setSave_or_withdrawal_ind(E_SAVEORWITHDRAWALIND.SAVE); // 存入支取标志

			DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

			// 币种不同时,需做结售汇处理
			if (!CommUtil.equals(accessOut.getCcy_code(), cplIn.getCcy_code())) {
				// TODO:
			}

			// 检查账户状态
			if (accessOut.getAcct_status() == E_ACCTSTATUS.CLOSE) {

				throw DpBase.E0017(accessOut.getAcct_no(), accessOut.getSub_acct_seq());
			}

			rolloverInstInfo.setIn_acct_no(instInfo.getIn_acct_no());// 转入账号
			rolloverInstInfo.setIn_ccy_code(instInfo.getIn_ccy_code()); // 转入币种
		}
		else if (instInfo.getRoll_type() == E_ROLLTYPE.A2 || instInfo.getRoll_type() == E_ROLLTYPE.B2) {// 转续存类型为入定期

			// 新开产品号不可为空
			BizUtil.fieldNotNull(instInfo.getNew_open_acct_prod(), DpDict.A.new_open_acct_prod.getId(), DpDict.A.new_open_acct_prod.getLongName());

			// 新开账户存期不可为空
			BizUtil.fieldNotNull(instInfo.getNew_open_acct_term(), DpDict.A.new_open_acct_term.getId(), DpDict.A.new_open_acct_term.getLongName());

			String inAcctNo = instInfo.getIn_acct_no();// 取出转入账号

			// 转入账号为空则取原客户账号
			if (CommUtil.isNull(inAcctNo)) {

				inAcctNo = cplIn.getAcct_no();

				if (CommUtil.isNull(instInfo.getIn_ccy_code())) {// 转入币种为空则取原币种

					instInfo.setIn_ccy_code(cplIn.getCcy_code());
				}
			}

			// 定位账号信息
			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(inAcctNo, null, false);

			// 转入定期户,币种需一致
			if (!CommUtil.equals(cplIn.getCcy_code(), instInfo.getIn_ccy_code())) {

				throw DpErr.Dp.E0124(cplIn.getCcy_code(), instInfo.getIn_ccy_code());
			}

			// 查询产品信息
			DpfBase prodInfo = DpfBaseDao.selectOne_odb1(instInfo.getNew_open_acct_prod(), false);

			if (prodInfo == null) {

				throw APPUB.E0005(OdbFactory.getTable(DpfBase.class).getLongname(), SysDict.A.prod_id.getId(), instInfo.getNew_open_acct_prod());
			}

			// 检查产品是否为定期产品
			if (prodInfo.getDd_td_ind() != E_DEMANDORTIME.TIME) {

				throw DpBase.E0122(instInfo.getNew_open_acct_prod());
			}

			// 查询产品账户类型关系控制
			// dpf_account_type acctType =
			// Dpf_account_typeDao.selectOne_odb1(instInfo.getNew_open_acct_prod(),
			// instInfo.getIn_ccy_code(), acctInfo.getAcct_type(), false);
			//
			// if (acctType == null) {
			//
			// throw
			// APPUB.E0025(OdbFactory.getTable(dpf_account_type.class).getLongname(),
			// SysDict.A.prod_id.getId(), instInfo.getNew_open_acct_prod(),
			// SysDict.A.ccy_code.getId(),
			// instInfo.getIn_ccy_code(), SysDict.A.acct_type.getId(),
			// acctInfo.getAcct_type());
			// }

			rolloverInstInfo.setIn_acct_no(acctInfo.getAcct_no());// 转入账号
			rolloverInstInfo.setIn_ccy_code(instInfo.getIn_ccy_code());// 转入币种
			rolloverInstInfo.setNew_open_acct_prod(instInfo.getNew_open_acct_prod()); // 新开账户产品号
			rolloverInstInfo.setNew_open_acct_term(instInfo.getNew_open_acct_term()); // 新开账户存期
		}
		else if (instInfo.getRoll_type() == E_ROLLTYPE.A4 || instInfo.getRoll_type() == E_ROLLTYPE.B4) {// 转续存类型为本金
			// TODO:业务暂不实现
		}
		else {
			throw APPUB.E0026(DpDict.A.roll_type.getLongName(), instInfo.getRoll_type().getValue());
		}

		rolloverInstInfo.setRoll_amt_handling_method(instInfo.getRoll_amt_handling_method()); // 转存金额处理方式
		rolloverInstInfo.setRoll_amt_handling_value(instInfo.getRoll_amt_handling_value()); // 转存金额处理方式值

		// 登记审计
		if (0 == ApDataAuditApi.regLogOnUpdateBusiness(oldRolloverInstInfo, rolloverInstInfo)) {
			throw ApPubErr.APPUB.E0023(OdbFactory.getTable(DpbRolloverInstruct.class).getLongname());
		}

		// 维护定期转存指令信息
		DpbRolloverInstructDao.updateOne_odb1(rolloverInstInfo);

		bizlog.method(" DpRolloverInstruct.modifyRolloverInstructInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年3月7日-上午11:47:44</li>
	 *         <li>功能说明：定期转存指令删除</li>
	 *         </p>
	 * @param instInfo
	 *            定期转存指令信息
	 * @param acctNo
	 *            账号
	 * @param subAcctNo
	 *            子账号
	 */
	private static void delRolloverInstructInfo(DpRolloverInstructInfo instInfo, String acctNo, String subAcctNo) {
		bizlog.method(" DpRolloverInstruct.delRolloverInstructInfo begin >>>>>>>>>>>>>>>>");

		// 获取定期转存指令表信息
		DpbRolloverInstruct rolloverInstInfo = DpbRolloverInstructDao.selectOneWithLock_odb1(acctNo, subAcctNo, instInfo.getRoll_type(), instInfo.getSerial_no(), false);

		// 复制一份,做审计用
		DpbRolloverInstruct oldRolloverInstInfo = BizUtil.clone(DpbRolloverInstruct.class, rolloverInstInfo);

		if (rolloverInstInfo == null) {

			throw APPUB.E0025(OdbFactory.getTable(DpbRolloverInstruct.class).getLongname(), DpSysDict.A.sub_acct_no.getId(), subAcctNo, DpDict.A.roll_type.getId(), instInfo
					.getRoll_type().getValue(), SysDict.A.serial_no.getId(), "" + instInfo.getSerial_no());
		}

		rolloverInstInfo.setHandling_status(E_DEALSTATUS.CANCEL);

		// 登记审计
		if (0 == ApDataAuditApi.regLogOnUpdateBusiness(oldRolloverInstInfo, rolloverInstInfo)) {
			throw ApPubErr.APPUB.E0023(OdbFactory.getTable(DpbRolloverInstruct.class).getLongname());
		}

		// 维护定期转存指令信息
		DpbRolloverInstructDao.updateOne_odb1(rolloverInstInfo);

		bizlog.method(" DpRolloverInstruct.delRolloverInstructInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author linshiq
	 *         <p>
	 *         <li>2017年3月8日-上午11:13:04</li>
	 *         <li>功能说明：定期转存指令查询主程序</li>
	 *         </p>
	 * @param cplIn
	 *            定期转存指令查询输入接口
	 * @return 定期转存指令查询输出接口
	 */
	public static DpQryRolloverInstructOut queryRolloverInstruct(DpQryRolloverInstructIn cplIn) {
		bizlog.method(" DpRolloverInstruct.queryRolloverInstruct begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 账号不可为空
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 子账户序号不可为空
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());

		// 验密标志不可为空
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		// 定位子账号
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no()); // 账号
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq()); // 子账户序号
		accessIn.setCcy_code(cplIn.getCcy_code()); // 货币代码
		accessIn.setAcct_type(cplIn.getAcct_type()); // 账户类型
		accessIn.setProd_id(null); // 产品编号
		accessIn.setDd_td_ind(E_DEMANDORTIME.TIME); // 定活标志
		accessIn.setSave_or_withdrawal_ind(null); // 存入支取标志

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		// 检查交易密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());

			// 定位账号
			DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

			// 验证密码
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 获取公共变量

		String orgId = runEnvs.getBusi_org_id();// 取得法人代码

		List<DpbRolloverInstruct> listRollover = DpbRolloverInstructDao.selectAll_odb2(accessOut.getAcct_no(), accessOut.getSub_acct_no(), false);

		if (CommUtil.isNull(listRollover) || listRollover.size() <= 0) {

			throw APPUB.E0024(OdbFactory.getTable(DpbRolloverInstruct.class).getLongname(), SysDict.A.acct_no.getLongName(), accessOut.getAcct_no(),
					DpSysDict.A.sub_acct_no.getLongName(), accessOut.getSub_acct_no());
		}

		// 初始化输出列表
		Options<DpQryRolloverInstructInfo> listRolloverInrtInfo = new DefaultOptions<DpQryRolloverInstructInfo>();

		// 初始化单条数据
		DpQryRolloverInstructInfo rolloverInrt = null;

		// 循环赋值
		for (DpbRolloverInstruct rolloverInrtInfo : listRollover) {

			rolloverInrt = BizUtil.getInstance(DpQryRolloverInstructInfo.class);

			rolloverInrt.setRoll_type(rolloverInrtInfo.getRoll_type()); // 转续存类型
			rolloverInrt.setSerial_no(rolloverInrtInfo.getSerial_no()); // 序号
			rolloverInrt.setRoll_amt_handling_method(rolloverInrtInfo.getRoll_amt_handling_method()); // 转存金额处理方式
			rolloverInrt.setRoll_amt_handling_value(rolloverInrtInfo.getRoll_amt_handling_value()); // 转存金额处理方式值
			rolloverInrt.setIn_acct_no(rolloverInrtInfo.getIn_acct_no()); // 转入账号
			rolloverInrt.setIn_ccy_code(rolloverInrtInfo.getIn_ccy_code()); // 转入币种
			rolloverInrt.setNew_open_acct_prod(rolloverInrtInfo.getNew_open_acct_prod()); // 新开账户产品号
			rolloverInrt.setNew_open_acct_term(rolloverInrtInfo.getNew_open_acct_term()); // 新开账户存期
			rolloverInrt.setHandling_status(rolloverInrtInfo.getHandling_status()); // 处理状态
			rolloverInrt.setData_version(rolloverInrtInfo.getData_version()); // 数据版本号

			listRolloverInrtInfo.add(rolloverInrt);
		}

		// 初始化输出
		DpQryRolloverInstructOut cplOut = BizUtil.getInstance(DpQryRolloverInstructOut.class);

		cplOut.setCard_no(accessOut.getCard_no()); // 卡号
		cplOut.setAcct_no(accessOut.getAcct_no()); // 账号
		cplOut.setAcct_type(accessOut.getAcct_type()); // 账户类型
		cplOut.setAcct_name(accessOut.getAcct_name()); // 账户名称
		cplOut.setSub_acct_seq(accessOut.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(accessOut.getCcy_code()); // 货币代码
		cplOut.setList01(listRolloverInrtInfo); // 定期转存指令信息

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpRolloverInstruct.queryRolloverInstruct end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年7月28日-下午4:59:49</li>
	 *         <li>功能说明：定期转存历史信息查询</li>
	 *         </p>
	 * @param cplIn
	 *            定期转存历史信息查询输入
	 * @return 定期转存历史信息查询输出
	 */
	public static DpFixedRenewHistoryQueryOut qryFixedRenewHistoryInfo(DpFixedRenewHistoryQueryIn cplIn) {
		bizlog.method(" DpRolloverInstruct.qryFixedRenewHistoryInfo begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 账号不可为空
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		// 子账户序号不可为空
		BizUtil.fieldNotNull(cplIn.getSub_acct_seq(), SysDict.A.sub_acct_seq.getId(), SysDict.A.sub_acct_seq.getLongName());

		if (CommUtil.isNotNull(cplIn.getStart_date()) && CommUtil.isNotNull(cplIn.getEnd_date()) && CommUtil.compare(cplIn.getStart_date(), cplIn.getEnd_date()) > 0) {

			throw DpErr.Dp.E0133(cplIn.getStart_date(), cplIn.getStart_date());
		}

		if (CommUtil.isNull(cplIn.getInquiry_order_ind())) {

			cplIn.setInquiry_order_ind(E_ORDERIND.PROPER_ORDER);
		}

		// 定位子账号
		DpAcctAccessIn accessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		accessIn.setAcct_no(cplIn.getAcct_no()); // 账号
		accessIn.setSub_acct_seq(cplIn.getSub_acct_seq()); // 子账户序号
		accessIn.setAcct_type(cplIn.getAcct_type()); // 账户类型
		accessIn.setDd_td_ind(E_DEMANDORTIME.TIME); // 定活标志

		DpAcctAccessOut accessOut = DpToolsApi.locateSingleSubAcct(accessIn);

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 获取公共变量

		Page<DpbRolloverBook> rolloverBookList = SqlDpInstructDao.selFixedHistoryInfo(accessOut.getAcct_no(), accessOut.getSub_acct_no(), cplIn.getStart_date(),
				cplIn.getEnd_date(), cplIn.getInquiry_order_ind(), runEnvs.getBusi_org_id(), runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		// 初始化输出接口
		DpFixedRenewHistoryQueryOut cplOut = BizUtil.getInstance(DpFixedRenewHistoryQueryOut.class);

		if (rolloverBookList != null) {

			runEnvs.setTotal_count(rolloverBookList.getRecordCount());// 返回总记录数

			DpRenewHistoryInfo renewHistory = null;

			for (DpbRolloverBook rolloverBook : rolloverBookList.getRecords()) {

				renewHistory = BizUtil.getInstance(DpRenewHistoryInfo.class);

				String inAcctName = "";

				if (CommUtil.isNotNull(rolloverBook.getIn_acct_no())) {

					inAcctName = DpaAccountDao.selectOne_odb1(rolloverBook.getIn_acct_no(), true).getAcct_name();
				}

				renewHistory.setRoll_no(rolloverBook.getRoll_no());
				renewHistory.setTrxn_date(rolloverBook.getTrxn_date()); //
				renewHistory.setRoll_type(rolloverBook.getRoll_type()); //
				renewHistory.setSerial_no(rolloverBook.getSerial_no());
				renewHistory.setRoll_renew_amount(rolloverBook.getRoll_renew_amount()); //
				renewHistory.setInterest_tax(rolloverBook.getInterest_tax()); //
				renewHistory.setIn_acct_no(rolloverBook.getIn_acct_no()); //
				renewHistory.setIn_ccy_code(rolloverBook.getIn_ccy_code()); //
				renewHistory.setIn_sub_acct_seq(rolloverBook.getIn_sub_acct_seq()); //
				renewHistory.setIn_acct_name(inAcctName); // into
				renewHistory.setTrxn_seq(rolloverBook.getTrxn_seq()); //

				cplOut.getList01().add(renewHistory);
			}
		}
		cplOut.setCust_no(accessOut.getCust_no()); // custome
		cplOut.setCard_no(accessOut.getCard_no()); // card no
		cplOut.setAcct_no(accessOut.getAcct_no()); // account no
		cplOut.setAcct_name(accessOut.getAcct_name()); // account
		cplOut.setSub_acct_seq(accessOut.getSub_acct_seq()); //
		cplOut.setCcy_code(accessOut.getCcy_code()); // currency code

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpRolloverInstruct.qryFixedRenewHistoryInfo end <<<<<<<<<<<<<<<<");
		return cplOut;
	}
}
