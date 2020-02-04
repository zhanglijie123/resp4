package cn.sunline.icore.dp.serv.passbook;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_DEBITCREDIT;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_OPENREFVOCHFLAG;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpPassbookDao;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpsBill;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookLine;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookMark;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DppPassbook;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPageLineRet;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookPrcIn;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookPrcOut;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookPrintInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustVoucherInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_PRINTCOMPRESSWAY;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

public class DpPassbookPrePrint {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPassbookPrePrint.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月21日-下午1:39:17</li>
	 *         <li>功能说明：存折补登折预处理</li>
	 *         </p>
	 * @param cplIn
	 * @return DpPassbookPrcOut
	 */

	public static DpPassbookPrcOut preparePassbookPrint(DpPassbookPrcIn cplIn) {

		bizlog.method("DpPassbookPrePrint.preparePassbookPrint begin >>>>>>>>>>>>>>>>");

		// 非空字段检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());// 账号
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());// 验密标志

		// 定位客户账户
		DpaAccount acctInfo = DpToolsApi.accountInquery(cplIn.getAcct_no(), null);

		// 存折补登折预处理检查
		DpPassbookPrePrint.prePrintCheck(cplIn, acctInfo);

		// 存折补登折预处理
		DpPassbookPrcOut cplOut = DpPassbookPrePrint.prePrintMain(cplIn, acctInfo);

		// 补充输出
		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名
		cplOut.setRef_voch_level(acctInfo.getRef_voch_level()); // 开户关联凭证

		bizlog.method("DpPassbookPrePrint.preparePassbookPrint end  <<<<<<<<<<<<<<<<<<");

		return cplOut;

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年12月21日-下午4:34:32</li>
	 *         <li>功能说明：存折补登折预处理主方法</li>
	 *         </p>
	 * @param cplIn
	 *            存折预处理出入
	 * @param acctInfo
	 *            账户信息
	 * @return 预登折信息
	 */
	private static DpPassbookPrcOut prePrintMain(DpPassbookPrcIn cplIn, DpaAccount acctInfo) {

		bizlog.debug("DpPassbookPrePrint.prePrintMain begin  >>>>>>>>>>>>>>");
		// 初始化输出
		DpPassbookPrcOut cplOut = BizUtil.getInstance(DpPassbookPrcOut.class);

		// 凭证关联在账户层
		if (acctInfo.getRef_voch_level() == E_VOCHREFLEVEL.ACCT) {

			// 凭证关联账户层处理
			cplOut = prePassbookPrintForAcct(cplIn, acctInfo);
		}
		// 凭证关联在子账户层
		else if (acctInfo.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT) {

			// 封装子账户定位输入接口
			DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			acctAccessIn.setAcct_no(cplIn.getAcct_no()); // 账号
			acctAccessIn.setCcy_code(cplIn.getCcy_code()); // 币种
			acctAccessIn.setProd_id(cplIn.getProd_id()); // 产品编号
			acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq()); // 子账户序号

			// 获取存款子账户信息
			DpAcctAccessOut acctAccessOut = DpToolsApi.subAcctInquery(acctAccessIn);

			// 查询子账户信息
			DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

			// 凭证关联子账户层处理
			cplOut = prePassbookPrintForSubAcct(cplIn, acctInfo, subAcct);

		}

		bizlog.debug("DpPassbookPrePrint.prePrintMain end  <<<<<<<<<<<<<<<");

		return cplOut;

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月18日-下午3:28:16</li>
	 *         <li>功能说明：账户层补登折预处理</li>
	 *         </p>
	 * @param cplIn
	 * @param acctInfo
	 * @return DpPassbookPrcOut
	 */
	public static DpPassbookPrcOut prePassbookPrintForAcct(DpPassbookPrcIn cplIn, DpaAccount acctInfo) {

		bizlog.debug("DpPassbookPrePrint.prePassbookPrintForAcct begin  >>>>>>>>>>>>>>");

		// 获取凭证信息
		DpCustVoucherInfo qryVochOut = getVochInfo(acctInfo, null);

		// 输入凭证信息检查
		checkvochInfo(cplIn, qryVochOut);

		// 获取存折类打印控制信息
		DppPassbook passbook = DpPassbook.getPassbook(qryVochOut.getVoch_type());

		// 获取未登折打印行位控制
		DpbPassbookLine passbookLine = DpPassbook.getPassbookLine(qryVochOut.getVoch_type(), qryVochOut.getVoch_no());

		// 存折已满页
		if (passbookLine.getFull_page_ind() == E_YESORNO.YES) {
			throw DpErr.Dp.E0359();
		}

		// 初始化存折账单戳信息
		DpPassbook.initPassbookMark(qryVochOut.getVoch_type(), qryVochOut.getVoch_no(), acctInfo);

		long totalCanPrinCount = 0; // 账户可登折笔数
		long totalNoPrintCount = 0; // 账户未登折笔数

		// 获取未登折账单子账户
		List<String> subacctList = SqlDpPassbookDao.selBillsubAcctNoPrint(acctInfo.getAcct_no(), BizUtil.getTrxRunEnvs().getBusi_org_id(), false);

		Options<DpPassbookPrintInfo> printOptions = new DefaultOptions<DpPassbookPrintInfo>();
		// 单个处理子账户补登折信息
		for (String subAcctNo : subacctList) {
			// 查询子账户信息
			DpaSubAccount subAcct = DpaSubAccountDao.selectOne_odb1(acctInfo.getAcct_no(), subAcctNo, true);

			// 调用单个子账户处理
			List<DpPassbookPrintInfo> printList = repareSiglSubAcctData(qryVochOut.getVoch_type(), qryVochOut.getVoch_no(), subAcct, passbook, passbookLine, totalCanPrinCount);

			totalCanPrinCount = totalCanPrinCount + printList.size();

			printOptions.addAll(printList);

		}
		// 返回下一打印行数，页数，存折空白行数
		DpPageLineRet pageLineRet = DpPassbook.calcPageLine(passbookLine, passbook, totalCanPrinCount);

		E_YESORNO chgPassbookInd = E_YESORNO.NO;

		if (pageLineRet.getPassbook_blank_line_num() - totalCanPrinCount <= 0) {
			// 返回强制换折标志
			chgPassbookInd = E_YESORNO.YES;
			// 空白行数不足，本次可登笔数为空白行数
			totalCanPrinCount = pageLineRet.getPassbook_blank_line_num();
		}

		// 未打印记录数
		totalNoPrintCount = getNoPrintCount(cplIn.getAcct_no());

		// 返回输出
		DpPassbookPrcOut cplOut = BizUtil.getInstance(DpPassbookPrcOut.class);

		cplOut.setCust_no(acctInfo.getCust_no()); // 客户号
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 账号
		cplOut.setAcct_name(acctInfo.getAcct_name()); // 账户名
		cplOut.setRef_voch_level(acctInfo.getRef_voch_level()); // 开户关联凭证
		// cplOut.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
		// cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setVoch_type(qryVochOut.getVoch_type()); // 凭证类型
		cplOut.setVoch_no(qryVochOut.getVoch_no()); // 凭证号
		cplOut.setPassbook_blank_line_num(pageLineRet.getPassbook_blank_line_num()); // 存折空白行数
		cplOut.setNon_print_record_num(totalNoPrintCount); // 未登折笔数
		cplOut.setCan_print_record_num(totalCanPrinCount); // 本次可登折笔数
		cplOut.setForce_chg_passbook_ind(chgPassbookInd); // 换折标识
		cplOut.setPrint_info_list(printOptions); // 打印信息列表

		bizlog.debug("DpPassbookPrePrint.prePassbookPrintForAcct end  <<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月21日-下午1:39:17</li>
	 *         <li>功能说明：单个子账户存折补登折预处理</li>
	 *         </p>
	 * @param cplIn
	 * @return DpPassbookPrcOut
	 */

	public static DpPassbookPrcOut prePassbookPrintForSubAcct(DpPassbookPrcIn cplIn, DpaAccount acctInfo, DpaSubAccount subAcct) {

		bizlog.method("DpPassbookPrePrint.passbookPrintForSubAcct begin >>>>>>>>>>>>>>>>");

		// 获取凭证信息
		DpCustVoucherInfo qryVochOut = getVochInfo(acctInfo, subAcct);

		// 输入凭证信息检查
		checkvochInfo(cplIn, qryVochOut);

		// 获取存折类打印控制信息
		DppPassbook passbook = DpPassbook.getPassbook(qryVochOut.getVoch_type());

		// 获取未登折打印行位控制
		DpbPassbookLine passbookLine = DpPassbook.getPassbookLine(qryVochOut.getVoch_type(), qryVochOut.getVoch_no());

		Options<DpPassbookPrintInfo> printOptions = new DefaultOptions<DpPassbookPrintInfo>();

		// 单个子账户打印数据准备
		List<DpPassbookPrintInfo> printList = repareSiglSubAcctData(qryVochOut.getVoch_type(), qryVochOut.getVoch_no(), subAcct, passbook, passbookLine, 0);

		// 添加到打印信息列表
		printOptions.addAll(printList);

		long totalCanPrinCount = (long) printList.size(); // 账户可登折笔数
		long totalNoPrintCount = 0; // 账户未登折笔数

		// 返回下一打印行数，页数，存折空白行数
		DpPageLineRet pageLineRet = DpPassbook.calcPageLine(passbookLine, passbook, totalCanPrinCount);

		E_YESORNO chgPassbookInd = E_YESORNO.NO; // 是否换折标志

		// 剩余空白行数小于零
		if (pageLineRet.getPassbook_blank_line_num() - printList.size() < 0) {
			// 返回强制换折标志
			chgPassbookInd = E_YESORNO.YES;
		}

		// 未打印记录数
		totalNoPrintCount = getNoPrintCount(cplIn.getAcct_no());
		// 返回输出
		DpPassbookPrcOut cplOut = BizUtil.getInstance(DpPassbookPrcOut.class);

		cplOut.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
		cplOut.setCcy_code(subAcct.getCcy_code()); // 货币代码
		cplOut.setNon_print_record_num(totalCanPrinCount); // 未登折笔数
		cplOut.setCan_print_record_num(totalNoPrintCount);// 本次可登折笔数
		cplOut.setForce_chg_passbook_ind(chgPassbookInd); // 强制换折标识
		cplOut.setPassbook_blank_line_num(pageLineRet.getPassbook_blank_line_num()); // 存折空白行数
		cplOut.setPrint_info_list(printOptions);

		bizlog.method("DpPassbookPrePrint.passbookPrintForSubAcct end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月18日-下午5:07:57</li>
	 *         <li>功能说明：单个子账户补登折数据准备</li>
	 *         </p>
	 * @param voch_typ
	 *            * 凭证类型
	 * @param voch_no
	 *            凭证号
	 * @param subAcct
	 *            子账户户信息
	 * @param passbook
	 *            存折打印控制信息
	 * @param passbookLine
	 *            存折行位信息
	 * @param count
	 *            笔数
	 * @return 存折打印信息列表
	 */
	public static List<DpPassbookPrintInfo> repareSiglSubAcctData(String voch_type, String voch_no, DpaSubAccount subAcct, DppPassbook passbook, DpbPassbookLine passbookLine,
			long count) {

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		// 获取单个子账户存折账单戳信息
		DpbPassbookMark passbookMark = DpPassbook.getPassbookMark(voch_type, voch_no, subAcct);

		// 根据子账号查询未登折帐单信息
		List<DpsBill> dpsBillList = SqlDpPassbookDao.selAcctBillBySubAcctno(subAcct.getAcct_no(), subAcct.getSub_acct_no(), runEnvs.getBusi_org_id(), passbookMark.getSerial_no(),
				false);

		List<DpPassbookPrintInfo> printList = new ArrayList<DpPassbookPrintInfo>();

		if (CommUtil.isNull(dpsBillList)) {

			return printList;
		}

		// 满足压缩条件，则压缩并登记存折打印登记簿
		printList = commpress(dpsBillList, passbookLine, passbook, passbookMark, subAcct, count);

		return printList;

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年12月21日-下午4:43:51</li>
	 *         <li>功能说明：获取未打印笔数</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @return 未打印笔数
	 */
	private static long getNoPrintCount(String acctNo) {

		// 账户总记录数
		long acctBillCount = SqlDpPassbookDao.selAcctBillByAcct(acctNo, BizUtil.getTrxRunEnvs().getBusi_org_id(), false);

		// 已登折记录数
		long printCount = SqlDpPassbookDao.selPrintRecordByAccount(acctNo, BizUtil.getTrxRunEnvs().getBusi_org_id(), false);

		return acctBillCount - printCount;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月16日-上午11:13:56</li>
	 *         <li>功能说明：存折账单压缩，不满足压缩条件不压缩</li>
	 *         </p>
	 * @param dpsBillList
	 *            未登折账单列表
	 * @param passbookLine
	 *            存折行位控制信息
	 * @param passbook
	 *            存折类打印控制信息
	 * @param passbookMark
	 *            存折账单戳信息
	 * @param subAcct
	 *            子账户信息
	 * @return 打印信息列表
	 */
	private static List<DpPassbookPrintInfo> commpress(List<DpsBill> dpsBillList, DpbPassbookLine passbookLine, DppPassbook passbook, DpbPassbookMark passbookMark,
			DpaSubAccount subAcct, long count) {

		List<DpPassbookPrintInfo> printList = new ArrayList<DpPassbookPrintInfo>();
		// 是否满足压缩条件
		boolean commpressFlag = DpPassbook.isCompress(passbook, passbookMark, dpsBillList.size());

		if (commpressFlag) {
			// 按借贷方压缩
			if (passbook.getPrint_compress_method() == E_PRINTCOMPRESSWAY.ASPECT) {
				// 压缩处理
				printList = commpressByDebitCredit(dpsBillList, passbookLine, passbook, subAcct, count);
			}
			else {
				throw APPUB.E0026(DpDict.A.print_compress_method.getLongName(), passbook.getPrint_compress_method().getValue());
			}
		}
		else {
			// 不压缩
			printList = withNoCompress(dpsBillList, passbookLine, passbook, subAcct, count);
		}

		return printList;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月23日-下午4:48:43</li>
	 *         <li>功能说明：未登折信息压缩-按借贷方</li>
	 *         </p>
	 * @param dpsBillList
	 * @param passbookLine
	 * @param passbook
	 * @param subAcct
	 * @return
	 */
	public static List<DpPassbookPrintInfo> commpressByDebitCredit(List<DpsBill> dpsBillList, DpbPassbookLine passbookLine, DppPassbook passbook, DpaSubAccount subAcct, long count) {

		bizlog.method("DpPassbookPrePrint.commpressByDebitCredit begin  >>>>>>>>>>>>>>>");

		// 账户余额
		BigDecimal acctBal = subAcct.getAcct_bal();

		BigDecimal dCreditAmt = BigDecimal.ZERO;// 贷方金额汇总
		BigDecimal dDebitAmt = BigDecimal.ZERO;// 借方金额汇总

		long creditCount = 0; // 贷方压缩笔数
		long debitConut = 0; // 借方压缩笔数

		long maxCrSerialNo = 0; // 贷方压缩最大账单序号
		long maxDrSerialNo = 0; // 借方压缩最大账单序号

		String maxCrTrxnSeq = "0";
		String maxDrTrxnSeq = "0";

		String maxCrTrandt = DpConst.THE_SMALLEST_DATE;
		String maxDrTrandt = DpConst.THE_SMALLEST_DATE;

		String maxCrTime = DpConst.THE_SMALLEST_TIME;
		String maxDrTime = DpConst.THE_SMALLEST_TIME;

		long lCount = 0;// 登记压缩后未登折数

		for (DpsBill tabDpsBill : dpsBillList) {

			// 贷方
			if (tabDpsBill.getDebit_credit() == E_DEBITCREDIT.CREDIT) {
				dCreditAmt = dCreditAmt.add(tabDpsBill.getTrxn_amt());

				// 贷方最大交易日期
				if (CommUtil.compare(tabDpsBill.getTrxn_date(), maxCrTrandt) >= 0) {

					maxCrTrandt = tabDpsBill.getTrxn_date(); // 交易日期

					if (CommUtil.compare(tabDpsBill.getTrxn_time(), maxCrTime) > 0) {

						maxCrTime = tabDpsBill.getTrxn_time(); // 交易时间
						maxCrSerialNo = tabDpsBill.getSerial_no(); // 交易序号
						maxCrTrxnSeq = tabDpsBill.getTrxn_seq(); // 交易流水

					}
				}

				creditCount++;
			}
			// 借方
			else if (tabDpsBill.getDebit_credit() == E_DEBITCREDIT.DEBIT) {
				dDebitAmt = dDebitAmt.add(tabDpsBill.getTrxn_amt());

				// 借方最大交易日期
				if (CommUtil.compare(tabDpsBill.getTrxn_date(), maxDrTrandt) >= 0) {

					maxDrTrandt = tabDpsBill.getTrxn_date();

					if (CommUtil.compare(tabDpsBill.getTrxn_time(), maxDrTime) > 0) {

						maxDrTime = tabDpsBill.getTrxn_time(); // 交易时间
						maxDrSerialNo = tabDpsBill.getSerial_no(); // 交易序号
						maxDrTrxnSeq = tabDpsBill.getTrxn_seq(); // 交易流水

					}
				}

				debitConut++;
			}
		}

		// 打印列表信息
		List<DpPassbookPrintInfo> printList = new ArrayList<DpPassbookPrintInfo>();

		// 打印信息
		DpPassbookPrintInfo printInfo = BizUtil.getInstance(DpPassbookPrintInfo.class);

		// 先贷方
		if (CommUtil.compare(dCreditAmt, BigDecimal.ZERO) != 0) {

			// 获取打印页号、行号
			DpPageLineRet pageLineRet = DpPassbook.calcPageLine(passbookLine, passbook, lCount + count);

			// 公共数据
			repareComField(printInfo, passbookLine, dpsBillList, subAcct);

			printInfo.setPassbook_line_no(pageLineRet.getNext_passbook_line_no());
			printInfo.setPassbook_page_no(pageLineRet.getNext_passbook_page_no());
			printInfo.setTrxn_amt(dCreditAmt); // 贷方交易金额
			printInfo.setDebit_credit(E_DEBITCREDIT.CREDIT); // 借贷标志
			printInfo.setCompress_count(creditCount); // 贷方压缩笔数
			printInfo.setBal_after_trxn(acctBal.add(dCreditAmt)); // 交易后余额
			printInfo.setTrxn_date(maxCrTrandt); // 压缩填最大交易日期
			printInfo.setTrxn_seq(maxCrTrxnSeq); // 压缩填最大交易流水
			printInfo.setTrxn_time(maxCrTime); // 压缩填最大交易时间
			printInfo.setSerial_no(maxCrSerialNo); // 压缩填最大交易序号
			printInfo.setReprint_ind(E_YESORNO.NO);

			lCount++;

			// 添加到打印信息列表
			printList.add(printInfo);

		}

		// 再借方
		if (CommUtil.compare(dDebitAmt, BigDecimal.ZERO) != 0) {

			// 获取打印页号、行号
			DpPageLineRet pageLineRet = DpPassbook.calcPageLine(passbookLine, passbook, lCount + count);

			// 公共数据
			repareComField(printInfo, passbookLine, dpsBillList, subAcct);

			printInfo.setPassbook_line_no(pageLineRet.getNext_passbook_line_no());
			printInfo.setPassbook_page_no(pageLineRet.getNext_passbook_page_no());
			printInfo.setTrxn_amt(dDebitAmt); // 借方交易金额
			printInfo.setDebit_credit(E_DEBITCREDIT.DEBIT); // 借贷标志
			printInfo.setCompress_count(debitConut); // 借方压缩笔数
			printInfo.setBal_after_trxn(acctBal); // 交易后余额
			printInfo.setTrxn_date(maxDrTrandt); // 压缩填最大交易日期
			printInfo.setTrxn_seq(maxDrTrxnSeq);
			printInfo.setTrxn_time(maxDrTime);// 压缩填最大交易时间
			printInfo.setSerial_no(maxDrSerialNo);
			printInfo.setReprint_ind(E_YESORNO.NO);
			lCount++;

			// 添加到打印信息列表
			printList.add(printInfo);

		}
		bizlog.method("DpPassbookPrePrint.commpressByDebitCredit end  <<<<<<<<<<<<<<<<<<");

		return printList;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月24日-下午4:06:01</li>
	 *         <li>功能说明：未登折信息 --未压缩</li>
	 *         </p>
	 * @param dpsBillList
	 * @param tabPassbookLine
	 * @param tabPassbookMark
	 */
	public static List<DpPassbookPrintInfo> withNoCompress(List<DpsBill> dpsBillList, DpbPassbookLine passbookLine, DppPassbook passbook, DpaSubAccount subAcct, long count) {

		bizlog.method("DpPassbookPrePrint.withNoCompress begin  >>>>>>>>>>>>>>>");

		long lineCount = 0;

		// 打印列表信息
		List<DpPassbookPrintInfo> printList = new ArrayList<DpPassbookPrintInfo>();

		for (DpsBill tabDpsBill : dpsBillList) {

			// 获取打印页号、行号
			DpPageLineRet pageLineRet = DpPassbook.calcPageLine(passbookLine, passbook, lineCount + count);

			// 打印信息
			DpPassbookPrintInfo printInfo = BizUtil.getInstance(DpPassbookPrintInfo.class);

			printInfo.setVoch_type(passbookLine.getVoch_type()); // 凭证类型
			printInfo.setVoch_no(passbookLine.getVoch_no()); // 凭证号码
			printInfo.setPrint_ind(E_YESORNO.NO); // 打印标识
			printInfo.setCompress_print_ind(E_YESORNO.NO); // 压缩标识
			printInfo.setSub_acct_no(tabDpsBill.getSub_acct_no()); // 子账号
			printInfo.setSerial_no(tabDpsBill.getSerial_no());
			printInfo.setBack_value_date(tabDpsBill.getBack_value_date()); // 倒起息日
			printInfo.setCash_trxn_ind(tabDpsBill.getCash_trxn_ind());
			printInfo.setDebit_credit(tabDpsBill.getDebit_credit());
			printInfo.setTrxn_ccy(tabDpsBill.getTrxn_ccy());
			printInfo.setTrxn_amt(tabDpsBill.getTrxn_amt());
			printInfo.setBal_after_trxn(tabDpsBill.getBal_after_trxn());
			printInfo.setCard_no(tabDpsBill.getCard_no());
			printInfo.setAcct_no(tabDpsBill.getAcct_no());
			printInfo.setSub_acct_seq(tabDpsBill.getSub_acct_seq());
			printInfo.setInit_inst_start_date(subAcct.getStart_inst_date());
			printInfo.setTerm_code(subAcct.getTerm_code()); // 存期
			printInfo.setDue_date(subAcct.getDue_date()); // 到期日期
			// 利息 利息税 增值税 利率
			printInfo.setInterest(BigDecimal.ZERO); // TODO
			printInfo.setInterest_tax(BigDecimal.ZERO);// TODO
			printInfo.setInst_rate(BigDecimal.ZERO);// TODO
			printInfo.setOpp_card_no(tabDpsBill.getOpp_card_no());
			printInfo.setOpp_acct_no(tabDpsBill.getOpp_acct_no());
			printInfo.setOpp_acct_name(tabDpsBill.getOpp_acct_name());
			printInfo.setTrxn_remark(tabDpsBill.getTrxn_remark());
			printInfo.setCustomer_remark(tabDpsBill.getCustomer_remark());
			printInfo.setSummary_code(tabDpsBill.getSummary_code());
			printInfo.setSummary_name(tabDpsBill.getSummary_name());
			printInfo.setTrxn_code(tabDpsBill.getTrxn_code());
			printInfo.setRecon_code(tabDpsBill.getRecon_code());
			printInfo.setThird_party_date(tabDpsBill.getThird_party_date());
			printInfo.setTrxn_channel(tabDpsBill.getTrxn_channel());
			printInfo.setTrxn_status(tabDpsBill.getTrxn_status());
			printInfo.setTrxn_date(tabDpsBill.getTrxn_date());
			printInfo.setTrxn_time(tabDpsBill.getTrxn_time());
			printInfo.setTrxn_seq(tabDpsBill.getTrxn_seq());
			printInfo.setBusi_seq(tabDpsBill.getBusi_seq());
			printInfo.setTrxn_branch(tabDpsBill.getTrxn_branch());
			printInfo.setTrxn_teller(tabDpsBill.getTrxn_teller());
			printInfo.setReprint_ind(E_YESORNO.NO);

			printInfo.setPassbook_line_no(pageLineRet.getNext_passbook_line_no());
			printInfo.setPassbook_page_no(pageLineRet.getNext_passbook_page_no());

			lineCount++;

			// 添加到打印信息列表
			printList.add(printInfo);

		}

		bizlog.method("DpPassbookPrePrint.withNoCompress end   <<<<<<<<<<<<<<<<<");

		return printList;

	}

	public static DpCustVoucherInfo getVochInfo(DpaAccount acctInfo, DpaSubAccount subAcct) {

		bizlog.method("DpPassbookPrePrint.getVochInfo begin  >>>>>>>>>>>>");

		DpCustVoucherInfo qryVochOut = DpVoucherIobus.getCustVouchersInfo(subAcct, acctInfo);

		// 无对应凭证信息
		if (qryVochOut == null) {

			throw DpErr.Dp.E0135(acctInfo.getAcct_no());
		}

		bizlog.debug("qryVochOut [%s]", qryVochOut);

		bizlog.method("DpPassbookPrePrint.getVochInfo end  <<<<<<<<<<<<<<<");

		return qryVochOut;

	}

	private static void checkvochInfo(DpPassbookPrcIn cplIn, DpCustVoucherInfo qryVochOut) {

		// 录入凭证类型与账户凭证类型检查
		if (CommUtil.isNotNull(cplIn.getVoch_type()) && !CommUtil.equals(cplIn.getVoch_type(), qryVochOut.getVoch_type())) {

			throw DpErr.Dp.E0198(cplIn.getVoch_type(), qryVochOut.getVoch_type());
		}

		// 录入凭证号码与账户凭证号码检查
		if (CommUtil.isNotNull(cplIn.getVoch_no()) && !CommUtil.equals(cplIn.getVoch_no(), qryVochOut.getVoch_no())) {

			throw DpErr.Dp.E0199(cplIn.getVoch_no(), qryVochOut.getVoch_no());

		}

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年12月21日-下午4:51:29</li>
	 *         <li>功能说明：存折补登预处理检查</li>
	 *         </p>
	 * @param cplIn
	 * @param acctInfo
	 */
	private static void prePrintCheck(DpPassbookPrcIn cplIn, DpaAccount acctInfo) {

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

		// 是否关联凭证检查
		if (acctInfo.getOpen_acct_voch_ind() == E_OPENREFVOCHFLAG.NONE) {
			throw DpErr.Dp.E0338(acctInfo.getAcct_no());
		}
	}

	private static void repareComField(DpPassbookPrintInfo printInfo, DpbPassbookLine passbookLine, List<DpsBill> dpsBillList, DpaSubAccount subAcct) {

		// 组织公共数据
		printInfo.setVoch_type(passbookLine.getVoch_type());
		printInfo.setVoch_no(passbookLine.getVoch_no());
		printInfo.setCompress_print_ind(E_YESORNO.YES);
		printInfo.setSub_acct_no(dpsBillList.get(0).getSub_acct_no());
		// passbookPrint.setSerial_no(dpsBillList.get(0).getSerial_no()); //
		// 压缩打印时填最开始序号
		printInfo.setBack_value_date(dpsBillList.get(0).getBack_value_date()); // 到起息日
		printInfo.setTrxn_ccy(dpsBillList.get(0).getTrxn_ccy());
		printInfo.setCard_no(dpsBillList.get(0).getCard_no());
		printInfo.setAcct_no(dpsBillList.get(0).getAcct_no());
		printInfo.setSub_acct_seq(dpsBillList.get(0).getSub_acct_seq()); // 子账号序号
		printInfo.setInit_inst_start_date(subAcct.getStart_inst_date());
		printInfo.setTerm_code(subAcct.getTerm_code()); // 存期
		printInfo.setDue_date(subAcct.getDue_date()); // 到期日期
		printInfo.setPrint_ind(E_YESORNO.NO); // 打印标识
		// 利息 利息税 增值税 利率 对方卡号 对方账号 对方户名 交易备注 客户备注 摘要代码 摘要描述 交易码 对账代码 第三方日期
		// 交易渠道
		// 交易日期 交易流水 业务流水 交易机构 交易柜员 压缩为空值

		printInfo.setInterest(BigDecimal.ZERO); // TODO
		printInfo.setInterest_tax(BigDecimal.ZERO);// TODO
		printInfo.setInst_rate(BigDecimal.ZERO);// TODO
		printInfo.setSummary_code(dpsBillList.get(0).getSummary_code());// TODO
		printInfo.setSummary_name(dpsBillList.get(0).getSummary_name());// TODO

	}
}
