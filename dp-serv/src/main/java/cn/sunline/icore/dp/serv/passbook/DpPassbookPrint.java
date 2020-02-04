package cn.sunline.icore.dp.serv.passbook;

import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpPassbookDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookLine;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookLineDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookMark;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookMarkDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookPrint;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookPrintDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DppPassbook;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPageLineRet;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookPrcIn;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookPrcInfo;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookPrcOut;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookPrintInfo;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookRePrintIn;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookRePrintOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustVoucherInfo;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ACCTBUSITYPE;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：
 * </p>
 * 
 * @Author yangdl
 *         <p>
 *         <li>2017年3月21日-下午1:38:55</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月21日-yangdl：存折相关</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpPassbookPrint {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPassbook.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月16日-下午2:12:47</li>
	 *         <li>功能说明：存折补登折处理</li>
	 *         </p>
	 * @param passbookPrintIn
	 * @return DpPassbookPrintOut
	 */
	public static DpPassbookPrcOut passbookPrint(DpPassbookPrcIn cplIn) {

		bizlog.method("DpPassbookPrint.passbookPrint begin >>>>>>>>>>>>>>>>");

		E_YESORNO chgPassbookInd = E_YESORNO.NO; // 是否换折标志
		E_YESORNO pageChangeInd = E_YESORNO.NO; // 换页标志

		long currentPage = 0; // 记录当前页号
		long currentLine = 0; // 记录当前行号

		// 定位客户账户
		DpaAccount acctInfo = DpToolsApi.accountInquery(cplIn.getAcct_no(), null);

		// 获取预处理补登折数据
		DpPassbookPrcOut prePrintData = DpPassbookPrePrint.preparePassbookPrint(cplIn);

		// 不存在未打印的信息记录
		if (CommUtil.isNull(prePrintData.getPrint_info_list()) || prePrintData.getPrint_info_list().size() == 0) {
			throw DpErr.Dp.E0212(acctInfo.getAcct_no());
		}

		long blankCount = 0; // 空白行数
		int i = 1;

		// 存折总行数
		long passbookTotalNum = DpPassbook.getPassbookTotalNum(prePrintData.getVoch_type());

		DppPassbook passbook = DpPassbook.getPassbook(prePrintData.getVoch_type());

		// 输出
		DpPassbookPrcOut cplOut = BizUtil.getInstance(DpPassbookPrcOut.class);

		// 循环处理打印明细记录
		for (DpPassbookPrintInfo printInfo : prePrintData.getPrint_info_list()) {

			// 初始页号
			// long initPage =
			// prePrintData.getPrint_info_list().get(0).getPassbook_page_no();//
			long initLine = prePrintData.getPrint_info_list().get(0).getPassbook_line_no();// 初始行号

			currentPage = printInfo.getPassbook_page_no(); // 当前页号
			currentLine = printInfo.getPassbook_line_no(); // 当前行号

			DpPassbookPrintInfo passbookPrintInfo = BizUtil.getInstance(DpPassbookPrintInfo.class);

			// 第十三行前补2行空行
			if (initLine > 13) {
				blankCount = 2;
			}
			// 第十三行前补2行空行
			if (currentLine == 13) {
				cplOut.getPrint_info_list().add(BizUtil.getInstance(DpPassbookPrintInfo.class));
				cplOut.getPrint_info_list().add(BizUtil.getInstance(DpPassbookPrintInfo.class));
			}
			// 一次只返回一页数据，前面不足的补空行
			while (i < initLine + blankCount) {
				cplOut.getPrint_info_list().add(BizUtil.getInstance(DpPassbookPrintInfo.class));
				i++;
			}

			passbookPrintInfo.setVoch_type(printInfo.getVoch_type());
			passbookPrintInfo.setVoch_no(printInfo.getVoch_no());
			passbookPrintInfo.setPassbook_page_no(printInfo.getPassbook_page_no());
			passbookPrintInfo.setPassbook_line_no(printInfo.getPassbook_line_no());
			passbookPrintInfo.setCompress_print_ind(printInfo.getCompress_print_ind());
			passbookPrintInfo.setCompress_count(printInfo.getCompress_count());
			passbookPrintInfo.setSub_acct_no(printInfo.getSub_acct_no());
			passbookPrintInfo.setSerial_no(printInfo.getSerial_no());
			passbookPrintInfo.setTrxn_date(printInfo.getTrxn_date());
			passbookPrintInfo.setBack_value_date(printInfo.getBack_value_date());
			passbookPrintInfo.setCash_trxn_ind(printInfo.getCash_trxn_ind());
			passbookPrintInfo.setTrxn_ccy(printInfo.getTrxn_ccy());
			passbookPrintInfo.setTrxn_amt(printInfo.getTrxn_amt());
			passbookPrintInfo.setPrint_amt((printInfo.getDebit_credit() == E_DEBITCREDIT.DEBIT ? "-" : "+") + printInfo.getTrxn_amt());
			passbookPrintInfo.setBal_after_trxn(printInfo.getBal_after_trxn());
			passbookPrintInfo.setInterest(printInfo.getInterest());
			passbookPrintInfo.setInterest_tax(printInfo.getInterest_tax());
			passbookPrintInfo.setInst_rate(printInfo.getInst_rate());
			passbookPrintInfo.setTrxn_branch(printInfo.getTrxn_branch());
			passbookPrintInfo.setTrxn_teller(printInfo.getTrxn_teller());
			passbookPrintInfo.setOpp_card_no(printInfo.getOpp_card_no());
			passbookPrintInfo.setOpp_acct_no(printInfo.getOpp_acct_no());
			passbookPrintInfo.setTrxn_remark(printInfo.getTrxn_remark());
			passbookPrintInfo.setCustomer_remark(printInfo.getCustomer_remark());
			passbookPrintInfo.setSummary_code(printInfo.getSummary_code());
			passbookPrintInfo.setSummary_name(printInfo.getSummary_name());
			passbookPrintInfo.setTrxn_status(printInfo.getTrxn_status());
			passbookPrintInfo.setTrxn_channel(printInfo.getTrxn_channel());
			passbookPrintInfo.setThird_party_date(printInfo.getThird_party_date());

			cplOut.getPrint_info_list().add(passbookPrintInfo);

			// 登记存折补登登记薄
			regPassbookPrint(printInfo);

			// 总行数 - 已打印行数 = 空白行数
			if (passbookTotalNum - ((currentPage - 1) * passbook.getPage_line_count() + currentLine) <= 0) {

				chgPassbookInd = E_YESORNO.YES; // 换折标志
				pageChangeInd = E_YESORNO.YES; // 换页标志
				// 换折跳出循环
				break;
			}

			// 换页
			if (currentLine == passbook.getPage_line_count()) {

				pageChangeInd = E_YESORNO.YES; // 换页标志
				break;
			}

		}

		// 更新存折账单戳
		updatePassbookMark(prePrintData.getVoch_type(), prePrintData.getVoch_no(), cplIn.getAcct_no());

		// 更新存折行位信息
		DpbPassbookLine passbookLine = DpbPassbookLineDao.selectOne_odb1(prePrintData.getVoch_type(), prePrintData.getVoch_no(), true);

		passbookLine.setPassbook_page_no(currentPage); // 存折页号
		passbookLine.setPassbook_line_no(currentLine); // 存折行号
		passbookLine.setFull_page_ind(chgPassbookInd == E_YESORNO.YES ? E_YESORNO.YES : E_YESORNO.NO); // 满页标志

		// 计算下一页号、行号
		if (currentLine + 1 > passbook.getPage_line_count()) {

			passbookLine.setNext_passbook_page_no(currentPage + 1);
			passbookLine.setNext_passbook_line_no(1L);
		}
		else {
			passbookLine.setNext_passbook_page_no(currentPage);
			passbookLine.setNext_passbook_line_no(currentLine + 1);
		}

		// 更新存折行位控制表
		DpbPassbookLineDao.updateOne_odb1(passbookLine);

		// 补充输出
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名称
		cplOut.setRef_voch_level(acctInfo.getRef_voch_level()); // 关联凭证层级
		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setVoch_type(prePrintData.getVoch_type()); // 凭证类型
		cplOut.setVoch_no(prePrintData.getVoch_no()); // 凭证号
		cplOut.setForce_chg_passbook_ind(chgPassbookInd); // 换折标志
		cplOut.setPage_change_ind(pageChangeInd); // 是否换页

		bizlog.debug("cplOut = [%s]", cplOut);
		bizlog.method("DpPassbookPrint.passbookPrint end  <<<<<<<<<<<<<<<<<<");

		return cplOut;

	}

	private static void regPassbookPrint(DpPassbookPrintInfo printInfo) {

		DpbPassbookPrint passbookPrint = BizUtil.getInstance(DpbPassbookPrint.class);

		passbookPrint.setVoch_type(printInfo.getVoch_type()); // voucher type
		passbookPrint.setVoch_no(printInfo.getVoch_no()); // voucher number
		passbookPrint.setPassbook_page_no(printInfo.getPassbook_page_no());
		passbookPrint.setPassbook_line_no(printInfo.getPassbook_line_no());
		passbookPrint.setCompress_print_ind(printInfo.getCompress_print_ind());
		passbookPrint.setCompress_count(printInfo.getCompress_count());
		passbookPrint.setSub_acct_no(printInfo.getSub_acct_no());
		passbookPrint.setSerial_no(printInfo.getSerial_no()); // serial no
		passbookPrint.setBack_value_date(printInfo.getBack_value_date());
		passbookPrint.setCash_trxn_ind(printInfo.getCash_trxn_ind());
		passbookPrint.setDebit_credit(printInfo.getDebit_credit());
		passbookPrint.setTrxn_ccy(printInfo.getTrxn_ccy()); // transaction ccy
		passbookPrint.setTrxn_amt(printInfo.getTrxn_amt());
		passbookPrint.setBal_after_trxn(printInfo.getBal_after_trxn());
		passbookPrint.setCard_no(printInfo.getCard_no()); // card no
		passbookPrint.setAcct_no(printInfo.getAcct_no()); // account no
		passbookPrint.setSub_acct_seq(printInfo.getSub_acct_seq());
		passbookPrint.setInit_inst_start_date(printInfo.getInit_inst_start_date());
		passbookPrint.setTerm_code(printInfo.getTerm_code()); // term code
		passbookPrint.setDue_date(printInfo.getDue_date()); // due date
		passbookPrint.setInterest(printInfo.getInterest()); // interest
		passbookPrint.setInterest_tax(printInfo.getInterest_tax());
		passbookPrint.setInst_rate(printInfo.getInst_rate()); // interest rate
		passbookPrint.setOpp_card_no(printInfo.getOpp_card_no());
		passbookPrint.setOpp_acct_no(printInfo.getOpp_acct_no());
		passbookPrint.setOpp_acct_name(printInfo.getOpp_acct_name());
		passbookPrint.setTrxn_remark(printInfo.getTrxn_remark());
		passbookPrint.setCustomer_remark(printInfo.getCustomer_remark());
		passbookPrint.setSummary_code(printInfo.getSummary_code());
		passbookPrint.setSummary_name(printInfo.getSummary_name());
		passbookPrint.setTrxn_code(printInfo.getTrxn_code());
		passbookPrint.setRecon_code(printInfo.getRecon_code()); // recon code
		passbookPrint.setThird_party_date(printInfo.getThird_party_date());
		passbookPrint.setPrint_ind(E_YESORNO.YES);
		passbookPrint.setReprint_ind(E_YESORNO.NO);
		passbookPrint.setTrxn_channel(printInfo.getTrxn_channel());
		passbookPrint.setTrxn_status(printInfo.getTrxn_status());
		passbookPrint.setTrxn_date(printInfo.getTrxn_date());
		passbookPrint.setTrxn_seq(printInfo.getTrxn_seq());
		passbookPrint.setBusi_seq(printInfo.getBusi_seq()); // buisness sequence
		passbookPrint.setTrxn_time(printInfo.getTrxn_time());
		passbookPrint.setTrxn_branch(printInfo.getTrxn_branch());
		passbookPrint.setTrxn_teller(printInfo.getTrxn_teller());
		passbookPrint.setOrg_id(printInfo.getOrg_id()); // organization id
		passbookPrint.setData_create_user(printInfo.getData_create_user());
		passbookPrint.setData_create_time(printInfo.getData_create_time());
		passbookPrint.setData_update_user(printInfo.getData_update_user());
		passbookPrint.setData_update_time(printInfo.getData_update_time());
		passbookPrint.setData_version(printInfo.getData_version());

		// 登记存折补登登记薄
		DpbPassbookPrintDao.insert(passbookPrint);

	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月1日-上午10:07:59</li>
	 *         <li>功能说明：存折重新打印</li>
	 *         </p>
	 * @param cplIn
	 *            存折重新打印输入
	 * @return 存折重新打印输出
	 */
	public static DpPassbookRePrintOut passbookRePrint(DpPassbookRePrintIn cplIn) {
		bizlog.method(" DpPassbookPrint.passbookRePrint begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 非空检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getPassbook_start_page_no(), DpDict.A.passbook_start_page_no.getId(), DpDict.A.passbook_start_page_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getPassbook_start_line_no(), DpDict.A.passbook_start_line_no.getId(), DpDict.A.passbook_start_line_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getPrint_page_no(), DpDict.A.print_page_no.getId(), DpDict.A.print_page_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getPrint_start_line_no(), DpDict.A.print_start_line_no.getId(), DpDict.A.print_start_line_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		E_YESORNO chgPassbookInd = E_YESORNO.NO; // 是否换折标志
		E_YESORNO pageChangeInd = E_YESORNO.NO; // 换页标志

		// 定位客户账户
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		// 封装子账户定位输入接口
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setProd_id(cplIn.getProd_id());
		acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

		// 获取存款子账户信息
		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 查询子账户信息：带锁
		DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

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

		// 获取存折打印控制信息
		DppPassbook passbook = DpPassbook.getPassbook(qryVochOut.getVoch_type());

		// 获取存折行位信息
		DpbPassbookLine passbookLine = DpPassbook.getPassbookLine(qryVochOut.getVoch_type(), qryVochOut.getVoch_no());

		// 存折重打检查
		passbookRePrintCheck(cplIn, passbook, passbookLine, qryVochOut);

		// 打印行位有变化，调整行位控制表
		if (passbookLine.getPassbook_page_no() != cplIn.getPrint_page_no() || passbookLine.getPassbook_line_no() != cplIn.getPrint_start_line_no()) {

			passbookLine.setNext_passbook_page_no(cplIn.getPrint_page_no());
			passbookLine.setNext_passbook_line_no(cplIn.getPrint_start_line_no());

		}

		String qryVochType = qryVochOut.getVoch_type();

		// 输入存折编号不为空，取原凭证号（表示从旧存折取数据打印到新折上）
		String qryVochNo = CommUtil.isNotNull(cplIn.getPassbook_no()) ? cplIn.getPassbook_no() : qryVochOut.getVoch_no();

		// 获取已打印信息
		List<DpPassbookPrintInfo> printData = SqlDpPassbookDao.selRePrintPassbookRecord(qryVochType, qryVochNo, cplIn.getPassbook_start_page_no(),
				cplIn.getPassbook_start_line_no(), passbook.getPage_line_count(), runEnvs.getBusi_org_id(), false);

		// 无数据则报错
		if (CommUtil.isNull(printData) || printData.size() <= 0) {
			throw DpErr.Dp.E0370();
		}

		// 输出
		DpPassbookRePrintOut cplOut = BizUtil.getInstance(DpPassbookRePrintOut.class);

		long currentPage = 0; // 记录当前页号
		long currentLine = 0; // 记录当前行号
		long blankCount = 0; // 第一页扉页行数
		// int remainder =
		// E_PAGECHANGE_IND.SIGCHANGE.getValue().equals(ApBusinessParmApi.getValue("PASSBOOK_CHANGE_METHOD"))
		// ? 1 : 0;// 取余数

		long i = 1;

		int j = 1; // 用于空白行数计数
		// 存折总行数
		long passbookTotalNum = DpPassbook.getPassbookTotalNum(qryVochOut.getVoch_type());

		for (DpPassbookPrintInfo printInfo : printData) {

			long initPage = cplIn.getPrint_page_no();//
			long initLine = cplIn.getPrint_start_line_no();// 初始行号

			DpPassbookPrcInfo passbookPrc = BizUtil.getInstance(DpPassbookPrcInfo.class);

			DpPageLineRet pageLineRet = DpPassbook.calcPageLine(passbookLine, passbook, i - 1);

			currentPage = pageLineRet.getNext_passbook_page_no(); // 当前页号
			currentLine = pageLineRet.getNext_passbook_line_no(); // 当前行号

			// 第十三行前补2行空行
			if (initLine > 13) {
				blankCount = 2;
			}
			// 第十三行前补2行空行
			if (currentLine == 13) {
				cplOut.getList01().add(BizUtil.getInstance(DpPassbookPrcInfo.class));
				cplOut.getList01().add(BizUtil.getInstance(DpPassbookPrcInfo.class));
			}

			// 一次只返回一页数据，前面不足的补空行
			while (j < initLine + blankCount) {
				cplOut.getList01().add(BizUtil.getInstance(DpPassbookPrcInfo.class));
				j++;
			}
			bizlog.debug("currentPage [%s]", currentPage);
			bizlog.debug("currentLine [%s]", currentLine);

			passbookPrc.setPassbook_page_no(currentPage); // 打印当前页号
			passbookPrc.setPassbook_line_no(currentLine); // 打印当前行号
			passbookPrc.setSub_acct_seq(printInfo.getSub_acct_seq());
			passbookPrc.setTrxn_date(printInfo.getTrxn_date());
			passbookPrc.setCash_trxn_ind(printInfo.getCash_trxn_ind());
			passbookPrc.setDebit_credit(printInfo.getDebit_credit());
			passbookPrc.setTrxn_ccy(printInfo.getTrxn_ccy());
			passbookPrc.setTrxn_amt(printInfo.getTrxn_amt());
			passbookPrc.setPrint_amt((printInfo.getDebit_credit() == E_DEBITCREDIT.DEBIT ? "-" : "+") + printInfo.getTrxn_amt());
			passbookPrc.setBal_after_trxn(printInfo.getBal_after_trxn());
			passbookPrc.setTrxn_teller(printInfo.getTrxn_teller());
			passbookPrc.setSummary_code(printInfo.getSummary_code());
			passbookPrc.setSummary_name(printInfo.getSummary_name());
			passbookPrc.setTrxn_status(printInfo.getTrxn_status());

			// 登记新的打印记录
			cplOut.getList01().add(passbookPrc);

			i++;// 用于计算页号、行号

			// 总行数 - 已打印行数 = 空白行数
			if (passbookTotalNum - ((currentPage - 1) * passbook.getPage_line_count() + currentLine) <= 0) {

				chgPassbookInd = E_YESORNO.YES; // 换折标志
				pageChangeInd = E_YESORNO.YES; // 换页标志
				// 换折跳出循环
				break;
			}

			// 换页
			if (currentLine == passbook.getPage_line_count()) {

				pageChangeInd = E_YESORNO.YES; // 换页标志
				break;
			}

		}

		// 重打新的行位更新
		if (currentPage > passbookLine.getPassbook_page_no() || (currentPage == passbookLine.getPassbook_page_no() && currentLine > passbookLine.getPassbook_line_no())) {
			// 调整行位信息
			passbookLine.setPassbook_page_no(currentPage); // 存折页号
			passbookLine.setPassbook_line_no(currentLine); // 存折行号
			passbookLine.setFull_page_ind(chgPassbookInd == E_YESORNO.YES ? E_YESORNO.YES : E_YESORNO.NO); // 满页标志

			// 计算下一页号、行号
			if (currentLine + 1 > passbook.getPage_line_count()) {

				passbookLine.setNext_passbook_page_no(currentPage + 1);
				passbookLine.setNext_passbook_line_no(1L);
			}
			else {
				passbookLine.setNext_passbook_page_no(currentPage);
				passbookLine.setNext_passbook_line_no(currentLine + 1);
			}

			// 更新存折行位控制表
			DpbPassbookLineDao.updateOne_odb1(passbookLine);

		}

		cplOut.setCust_no(acctInfo.getCust_no()); // customer number
		cplOut.setAcct_no(acctInfo.getAcct_no()); // account no
		cplOut.setAcct_name(acctInfo.getAcct_name()); // account name
		cplOut.setRef_voch_level(acctInfo.getRef_voch_level()); // refer vouche
		cplOut.setPage_change_ind(pageChangeInd);
		cplOut.setForce_chg_passbook_ind(chgPassbookInd);
		cplOut.setVoch_type(qryVochOut.getVoch_type()); // voucher type
		cplOut.setVoch_no(qryVochOut.getVoch_no()); // voucher number

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpPassbookPrint.passbookRePrint end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月28日-上午11:05:57</li>
	 *         <li>功能说明：更新存折账单戳信息</li>
	 *         </p>
	 * @param passbookMark
	 * @param subAccount
	 */
	private static void updatePassbookMark(String vochType, String vochNo, String acctNo) {

		bizlog.method("DpPassbookPrint.updatePassbookMark begin >>>>>>>>>>>>>>>>>>>");

		List<DpaAccountRelate> acctRelateList = DpaAccountRelateDao.selectAll_odb3(acctNo, E_ACCTBUSITYPE.DEPOSIT, true);

		//
		for (DpaAccountRelate acctRelate : acctRelateList) {

			// 获取子账户信息
			DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctRelate.getAcct_no(), acctRelate.getSub_acct_no(), false);

			// 获取存折账单戳信息
			DpbPassbookMark passbookMark = DpbPassbookMarkDao.selectOne_odb1(vochType, vochNo, subAcct.getSub_acct_no(), false);

			// 获取最大交易序号对应的未登折信息
			DpPassbookPrintInfo printInfo = SqlDpPassbookDao.selMaxSortNoPassbookPrint(acctRelate.getSub_acct_no(), vochType, vochNo, BizUtil.getTrxRunEnvs().getBusi_org_id(),
					false);

			// 未取到最大交易序号，说明该子户不存在打印记录
			if (CommUtil.isNull(printInfo)) {
				continue;
			}

			passbookMark.setSerial_no(printInfo.getSerial_no());// 序号
			passbookMark.setTrxn_date(printInfo.getTrxn_date()); // 交易日期
			passbookMark.setTrxn_seq(printInfo.getTrxn_seq()); // 交易流水
			passbookMark.setAcct_no(printInfo.getAcct_no()); // 账号
			passbookMark.setPassbook_balance(printInfo.getBal_after_trxn()); // 存折余额
			// 存在更新存折账单戳信息
			DpbPassbookMarkDao.updateOne_odb1(passbookMark);

		}

		bizlog.method("DpPassbookPrint.updatePassbookMark end <<<<<<<<<<<<<<<<<<<");

	}

	// 存折重打输入检查
	private static void passbookRePrintCheck(DpPassbookRePrintIn cplIn, DppPassbook passbook, DpbPassbookLine passbookLine, DpCustVoucherInfo qryVochOut) {

		// 录入凭证类型与账户凭证类型检查
		if (CommUtil.isNotNull(cplIn.getVoch_type()) && !CommUtil.equals(cplIn.getVoch_type(), qryVochOut.getVoch_type())) {
			throw DpErr.Dp.E0198(cplIn.getVoch_type(), qryVochOut.getVoch_type());
		}

		// 录入凭证号码与账户凭证号码检查
		if (CommUtil.isNotNull(cplIn.getVoch_no()) && !CommUtil.equals(cplIn.getVoch_no(), qryVochOut.getVoch_no())) {
			throw DpErr.Dp.E0199(cplIn.getVoch_no(), qryVochOut.getVoch_no());
		}

		if (CommUtil.isNotNull(cplIn.getPassbook_start_page_no()) && cplIn.getPassbook_start_page_no() > passbook.getBook_pages_count()) {
			throw DpErr.Dp.E0373();
		}

		// 起始行数必须小于当页总行数
		if (CommUtil.isNotNull(cplIn.getPassbook_start_line_no()) && cplIn.getPassbook_start_line_no() > passbook.getPage_line_count()) {
			throw DpErr.Dp.E0374();
		}

		// 打印页数必须小于总页数
		if (CommUtil.isNotNull(cplIn.getPrint_page_no()) && cplIn.getPrint_page_no() > passbook.getBook_pages_count()) {
			throw DpErr.Dp.E0373();
		}

		// 打印行数必须小于当页总行数
		if (CommUtil.isNotNull(cplIn.getPrint_start_line_no()) && cplIn.getPrint_start_line_no() > passbook.getPage_line_count()) {
			throw DpErr.Dp.E0374();
		}

	}

}
