package cn.sunline.icore.dp.serv.passbook;

import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookLine;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookLineDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookMark;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookMarkDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DppPassbook;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookAjustIn;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookAjustOut;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookChangeIn;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookChangeOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustVoucherInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpVoucherChangeIn;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_CUSTVOCHSTAS;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpPassbookMaintain {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPassbookMaintain.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月15日-上午9:58:36</li>
	 *         <li>功能说明：存折行位调整</li>
	 *         </p>
	 * @param DpPassbookAjustIn
	 * @return DpPassbookAjustOut
	 */
	public static DpPassbookAjustOut passbookAdjust(DpPassbookAjustIn cplIn) {

		bizlog.method("DpPassbookMaintain.passbookAdjust begin >>>>>>>>>>>>>>>>");

		// 非空字段判断
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());// 验密标志
		BizUtil.fieldNotNull(cplIn.getNext_passbook_page_no(), DpDict.A.next_passbook_page_no.getId(), DpDict.A.next_passbook_page_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getNext_passbook_line_no(), DpDict.A.next_passbook_line_no.getId(), DpDict.A.next_passbook_line_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

		// 定位客户账户
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		DpaSubAccount subAcct = BizUtil.getInstance(DpaSubAccount.class);

		// 关联在子账户层才读取子账户信息
		if (acctInfo.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT) {
			// 封装子账户定位输入接口
			DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			acctAccessIn.setAcct_no(cplIn.getAcct_no());
			acctAccessIn.setCcy_code(cplIn.getCcy_code());
			acctAccessIn.setProd_id(cplIn.getProd_id());
			acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

			// 获取存款子账户信息
			DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

			// 查询子账户信息
			subAcct = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);
		}
		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 上送账户名称有值，校验账户名称一致性
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(acctInfo.getAcct_name(), cplIn.getAcct_name())) {

			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctInfo.getAcct_name());
		}

		// 获取凭证信息
		DpCustVoucherInfo qryVochOut = DpPassbookPrePrint.getVochInfo(acctInfo, subAcct);

		// 正式挂失报错
		if (CommUtil.isNotNull(qryVochOut.getCust_voch_status()) && (qryVochOut.getCust_voch_status() == E_CUSTVOCHSTAS.NORMAL_RPLS)) {
			throw DpErr.Dp.E0426();
		}
		// 口头挂失报错
		if (CommUtil.isNotNull(qryVochOut.getCust_voch_status()) && (qryVochOut.getCust_voch_status() == E_CUSTVOCHSTAS.ORAL_RPLS)) {
			throw DpErr.Dp.E0424();
		}

		// 行位调整
		modifyPassbookLine(qryVochOut.getVoch_type(), qryVochOut.getVoch_no(), cplIn);

		DpPassbookAjustOut cplOut = BizUtil.getInstance(DpPassbookAjustOut.class);

		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setRef_voch_level(acctInfo.getRef_voch_level()); // 凭证关联层级

		cplOut.setVoch_type(qryVochOut.getVoch_type()); // 凭证种类
		cplOut.setVoch_no(qryVochOut.getVoch_no()); // 凭证号码
		cplOut.setNext_passbook_page_no(cplIn.getNext_passbook_page_no()); // 下一打印页号
		cplOut.setNext_passbook_line_no(cplIn.getNext_passbook_line_no()); // 下一打印行号

		bizlog.method("DpPassbookMaintain.passbookAdjust end  <<<<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年5月21日-下午11:33:35</li>
	 *         <li>功能说明：存折存单更换</li>
	 *         </p>
	 * @param cplIn
	 *            存折存单更换输入
	 * @return cplOut 存折存单更换输出
	 */
	public static DpPassbookChangeOut passbookChange(DpPassbookChangeIn cplIn) {

		bizlog.method("DpPassbookMaintain.passbookChange begin  >>>>>>>>>>>>");

		// 输入要素检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getNew_voch_no(), DpDict.A.new_voch_no.getId(), DpDict.A.new_voch_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getChg_passbook_reason(), DpDict.A.chg_passbook_reason.getId(), DpDict.A.chg_passbook_reason.getLongName());
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		// 定位客户账户
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		DpaSubAccount subAcct = BizUtil.getInstance(DpaSubAccount.class);

		// 关联在子账户层才读取子账户信息
		if (acctInfo.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT) {
			// 封装子账户定位输入接口
			DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			acctAccessIn.setAcct_no(cplIn.getAcct_no());
			acctAccessIn.setCcy_code(cplIn.getCcy_code());
			acctAccessIn.setProd_id(cplIn.getProd_id());
			acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

			// 获取存款子账户信息
			DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

			// 查询子账户信息
			subAcct = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);
		}
		// 验证密码
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);

			checkIn.setTrxn_password(cplIn.getTrxn_password());
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 获取凭证信息
		DpCustVoucherInfo qryVochOut = DpPassbookPrePrint.getVochInfo(acctInfo, subAcct);
		// 凭证一致性检查
		if (CommUtil.isNotNull(cplIn.getVoch_type()) && !CommUtil.equals(cplIn.getVoch_type(), qryVochOut.getVoch_type())) {
			throw DpErr.Dp.E0198(cplIn.getVoch_type(), qryVochOut.getVoch_type());
		}

		if (CommUtil.isNotNull(cplIn.getVoch_no()) && !CommUtil.equals(cplIn.getVoch_no(), qryVochOut.getVoch_no())) {
			throw DpErr.Dp.E0199(cplIn.getVoch_no(), qryVochOut.getVoch_no());
		}
		if (CommUtil.isNotNull(qryVochOut.getCust_voch_status()) && (qryVochOut.getCust_voch_status() == E_CUSTVOCHSTAS.ORAL_RPLS)) {
			throw DpErr.Dp.E0424();
		}

		DpVoucherChangeIn cplVochIn = BizUtil.getInstance(DpVoucherChangeIn.class);

		cplVochIn.setAcct_no(acctInfo.getAcct_no());
		cplVochIn.setCcy_code(subAcct.getCcy_code());
		cplVochIn.setChg_passbook_reason(cplIn.getChg_passbook_reason());
		cplVochIn.setNew_voch_no(cplIn.getNew_voch_no());
		cplVochIn.setNew_voch_type(cplIn.getVoch_type());
		cplVochIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplVochIn.setSummary_code(cplIn.getSummary_code());
		cplVochIn.setRef_voch_level(acctInfo.getRef_voch_level());

		// 调用公共凭证更换
		DpVoucherIobus.changeVoucher(cplVochIn);

		// 旧存折的账单戳信息赋给新凭证
		List<DpbPassbookMark> passbookMarkList = DpbPassbookMarkDao.selectAll_odb2(qryVochOut.getVoch_type(), qryVochOut.getVoch_no(), true);

		for (DpbPassbookMark passbookMark : passbookMarkList) {

			DpbPassbookMark newPassbookMark = DpbPassbookMarkDao.selectOne_odb1(passbookMark.getVoch_type(), passbookMark.getVoch_no(), passbookMark.getSub_acct_no(), true);

			newPassbookMark.setVoch_no(cplIn.getNew_voch_no());

			// 登记新的存折账单戳
			DpbPassbookMarkDao.insert(newPassbookMark);
		}

		// 输出
		DpPassbookChangeOut cplOut = BizUtil.getInstance(DpPassbookChangeOut.class);

		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setRef_voch_level(acctInfo.getRef_voch_level()); // 凭证关联级别
		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账号序号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 币种
		cplOut.setVoch_type(qryVochOut.getVoch_type()); // 凭证类型
		cplOut.setVoch_no(qryVochOut.getVoch_no()); // 凭证号
		cplOut.setNew_voch_no(cplIn.getNew_voch_no()); // 新凭证号

		bizlog.method("DpPassbookMaintain.passbookChange end  <<<<<<<<<<<<<<");

		return cplOut;

	}

	// 存折行位调整检查
	private static void passbookAdjustCheck(DppPassbook passbook, DpbPassbookLine passbookLine, DpPassbookAjustIn cplIn) {

		// 校验数据版本号
		if (CommUtil.compare(passbookLine.getData_version(), cplIn.getData_version()) != 0) {

			throw ApPubErr.APPUB.E0018(DpaAccount.class.getName());
		}

		// 输入凭证一致性检查
		if (CommUtil.isNotNull(cplIn.getVoch_type()) && !CommUtil.equals(cplIn.getVoch_type(), passbookLine.getVoch_type())) {
			throw DpErr.Dp.E0198(cplIn.getVoch_type(), passbookLine.getVoch_type());
		}

		if (CommUtil.isNotNull(cplIn.getVoch_no()) && !CommUtil.equals(cplIn.getVoch_no(), passbookLine.getVoch_no())) {
			throw DpErr.Dp.E0199(cplIn.getVoch_no(), passbookLine.getVoch_no());
		}

		// 输入存折下一打印页号不能超过存折总页数
		if (CommUtil.compare(cplIn.getNext_passbook_page_no(), passbook.getBook_pages_count()) > 0) {
			throw DpErr.Dp.E0319();
		}

		// 输入存折下一打印行号不能超过存折每页总行数
		if (CommUtil.compare(cplIn.getNext_passbook_line_no(), passbook.getPage_line_count()) > 0) {
			throw DpErr.Dp.E0320();
		}

		// 输入存折下一打印页号不能小于当前下一打印页号
		if (CommUtil.compare(cplIn.getNext_passbook_page_no(), passbookLine.getNext_passbook_page_no()) < 0) {
			throw DpErr.Dp.E0251();
		}

		// 输入存折下一打印行号不能小于当前下一打印行号（页号不变）
		if (CommUtil.compare(cplIn.getNext_passbook_page_no(), passbookLine.getNext_passbook_page_no()) == 0
				&& CommUtil.compare(cplIn.getNext_passbook_line_no(), passbookLine.getNext_passbook_line_no()) < 0) {
			throw DpErr.Dp.E0252();
		}

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年12月25日-下午4:35:07</li>
	 *         <li>功能说明：存折行位更新</li>
	 *         </p>
	 * @param voch_type
	 * @param voch_no
	 * @param cplIn
	 */
	private static void modifyPassbookLine(String voch_type, String voch_no, DpPassbookAjustIn cplIn) {

		DppPassbook passbook = DpPassbook.getPassbook(voch_type);

		// 未登折打印行位控制信息
		DpbPassbookLine passbookLine = DpbPassbookLineDao.selectOne_odb1(voch_type, voch_no, true);

		// 存折行位调整检查
		passbookAdjustCheck(passbook, passbookLine, cplIn);

		passbookLine.setNext_passbook_page_no(cplIn.getNext_passbook_page_no());
		passbookLine.setNext_passbook_line_no(cplIn.getNext_passbook_line_no());

		// 更新未登折行位控制信息
		DpbPassbookLineDao.updateOne_odb1(passbookLine);

	}
}
