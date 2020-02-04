package cn.sunline.icore.dp.serv.passbook;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpPassbookDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookLine;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookPrint;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPassbookPrcInfo;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpQryPassbookPrcIn;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpQryPassbookPrcOut;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpQueryPassbookLineIn;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpQueryPassbookLineOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustVoucherInfo;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpPassbookQuery {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPassbookQuery.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月19日-上午10:31:47</li>
	 *         <li>功能说明：存折行位查询</li>
	 *         </p>
	 * @param queryPassbookLineIn
	 * @return DpQueryPassbookLineOut
	 */
	public static DpQueryPassbookLineOut queryPassbookLine(DpQueryPassbookLineIn cplIn) {

		bizlog.method("DpPassbookQuery.queryPassbookLine begin >>>>>>>>>>>>>>");
		// 非空检查
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
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

		// 未登折打印行位控制信息
		DpbPassbookLine passbookLine = DpPassbook.getPassbookLine(qryVochOut.getVoch_type(), qryVochOut.getVoch_no());

		// 输出
		DpQueryPassbookLineOut cplOut = BizUtil.getInstance(DpQueryPassbookLineOut.class);

		cplOut.setVoch_type(qryVochOut.getVoch_type()); // 凭证类型
		cplOut.setVoch_no(qryVochOut.getVoch_no()); // 凭证号码
		cplOut.setAcct_no(cplIn.getAcct_no()); // 账号
		cplOut.setNext_passbook_line_no(passbookLine.getNext_passbook_line_no()); // 下一打印行号
		cplOut.setNext_passbook_page_no(passbookLine.getNext_passbook_page_no()); // 下一打印页号
		cplOut.setPassbook_page_no(passbookLine.getPassbook_page_no());
		cplOut.setPassbook_line_no(passbookLine.getPassbook_line_no());
		cplOut.setData_version(passbookLine.getData_version()); // 数据版本号

		bizlog.method("DpPassbookQuery.queryPassbookLine end <<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月1日-上午10:04:04</li>
	 *         <li>功能说明：存折已登折信息查询</li>
	 *         </p>
	 * @param cplIn
	 *            存折已登折信息查询输入
	 * @return 存折已登折信息查询输出
	 */
	public static DpQryPassbookPrcOut qryPassbookPrcInfo(DpQryPassbookPrcIn cplIn) {
		bizlog.method(" DpPassbookQuery.qryPassbookPrcInfo begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 非空校验
		BizUtil.fieldNotNull(cplIn.getVoch_type(), SysDict.A.voch_type.getId(), SysDict.A.voch_type.getLongName());
		BizUtil.fieldNotNull(cplIn.getPassbook_no(), DpDict.A.passbook_no.getId(), DpDict.A.passbook_no.getLongName());

		// 账号不为空 则校验
		if (CommUtil.isNotNull(cplIn.getAcct_no())) {
			DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);
		}

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 获取公共变量

		String orgId = runEnvs.getBusi_org_id();// 取得法人代码

		DpQryPassbookPrcOut cplOut = BizUtil.getInstance(DpQryPassbookPrcOut.class);

		Page<DpbPassbookPrint> prtPassbookList = SqlDpPassbookDao.selAlreadyPrintedPassbookInfo(cplIn.getVoch_type(), cplIn.getPassbook_no(), cplIn.getAcct_no(),
				cplIn.getPassbook_start_page_no(), cplIn.getPassbook_start_line_no(), cplIn.getPassbook_end_page_no(), cplIn.getPassbook_end_line_no(), orgId,
				runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		if (prtPassbookList == null || prtPassbookList.getRecords().size() <= 0) {
			return cplOut;
		}

		runEnvs.setTotal_count(prtPassbookList.getRecordCount());// 返回总记录数

		// 组织输出
		String acctNo = prtPassbookList.getRecords().get(0).getAcct_no();// 默认取第一条账号
		String subAcctNo = prtPassbookList.getRecords().get(0).getSub_acct_no();// 默认取第一条子账号

		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(acctNo, true);
		DpaSubAccount subAcctInfo = DpaSubAccountDao.selectOne_odb1(acctNo, subAcctNo, true);

		cplOut.setCust_no(acctInfo.getCust_no()); // customer number
		cplOut.setAcct_no(acctInfo.getAcct_no()); // account no
		cplOut.setAcct_name(acctInfo.getAcct_name()); // account name
		cplOut.setRef_voch_level(acctInfo.getRef_voch_level()); // refer vouche
		cplOut.setSub_acct_seq(subAcctInfo.getSub_acct_seq()); // sub-account
		cplOut.setCcy_code(subAcctInfo.getCcy_code()); // currency code
		cplOut.setVoch_type(cplIn.getVoch_type()); // voucher type
		cplOut.setVoch_no(cplIn.getPassbook_no()); // voucher number

		DpPassbookPrcInfo cplSubOut = null;

		for (DpbPassbookPrint singleInfo : prtPassbookList.getRecords()) {

			cplSubOut = BizUtil.getInstance(DpPassbookPrcInfo.class);

			cplSubOut.setPassbook_page_no(singleInfo.getPassbook_page_no()); // page
			cplSubOut.setPassbook_line_no(singleInfo.getPassbook_line_no()); // line
			cplSubOut.setCompress_print_ind(singleInfo.getCompress_print_ind()); // print
			cplSubOut.setCompress_count(singleInfo.getCompress_count()); // compress
			cplSubOut.setSub_acct_seq(singleInfo.getSub_acct_seq()); // sub-account
			cplSubOut.setSerial_no(singleInfo.getSerial_no()); // serial no
			cplSubOut.setTrxn_date(singleInfo.getTrxn_date()); // transaction
			cplSubOut.setBack_value_date(singleInfo.getBack_value_date()); // back
			cplSubOut.setCash_trxn_ind(singleInfo.getCash_trxn_ind()); // cash
			cplSubOut.setDebit_credit(singleInfo.getDebit_credit()); // debit
			cplSubOut.setTrxn_ccy(singleInfo.getTrxn_ccy()); // transaction ccy
			cplSubOut.setTrxn_amt(singleInfo.getTrxn_amt()); // transaction
			cplSubOut.setBal_after_trxn(singleInfo.getBal_after_trxn()); // balance
			cplSubOut.setInterest(singleInfo.getInterest()); // interest
			cplSubOut.setInterest_tax(singleInfo.getInterest_tax()); // interest
			cplSubOut.setInst_rate(singleInfo.getInst_rate()); // interest rate
			cplSubOut.setTrxn_branch(singleInfo.getTrxn_branch()); // transaction
			cplSubOut.setTrxn_teller(singleInfo.getTrxn_teller()); // transaction
			cplSubOut.setOpp_card_no(singleInfo.getOpp_card_no()); // opponent
			cplSubOut.setOpp_acct_no(singleInfo.getOpp_acct_no()); // opponent
			cplSubOut.setOpp_acct_name(singleInfo.getOpp_acct_name()); // opponent
			cplSubOut.setTrxn_remark(singleInfo.getTrxn_remark()); // transaction
			cplSubOut.setCustomer_remark(singleInfo.getCustomer_remark()); // customer
			cplSubOut.setSummary_code(singleInfo.getSummary_code()); // summary
			cplSubOut.setSummary_name(singleInfo.getSummary_name()); // summary
			cplSubOut.setTrxn_status(singleInfo.getTrxn_status()); // transaction
			cplSubOut.setTrxn_channel(singleInfo.getTrxn_channel()); // trxn
			cplSubOut.setThird_party_date(singleInfo.getThird_party_date()); // third

			if (CommUtil.isNotNull(singleInfo.getOpp_acct_no())) {

				DpaAccount oppAcctInfo = DpaAccountDao.selectOne_odb1(singleInfo.getOpp_acct_no(), false);

				if (CommUtil.isNotNull(oppAcctInfo)) {

					cplSubOut.setOpp_branch_id(oppAcctInfo.getAcct_branch()); // opponent
					cplSubOut.setOpp_branch_name(ApBranchApi.getItem(oppAcctInfo.getAcct_branch()).getBranch_name()); // opponent
				}

			}

			cplOut.getList01().add(cplSubOut);
		}

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpPassbookQuery.qryPassbookPrcInfo end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

}
