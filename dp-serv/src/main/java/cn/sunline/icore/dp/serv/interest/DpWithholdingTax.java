package cn.sunline.icore.dp.serv.interest;

import cn.sunline.icore.ap.api.ApDropListApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.errors.DpBaseErr;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpInterestBase.DphInterestSettled;
import cn.sunline.icore.dp.base.type.ComDpTaxBase.DpInstAndTax;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInterestDao;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpWithholdingTaxDetail;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpWithholdingTaxIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpWithholdingTaxOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustBaseInfo;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpWithholdingTax {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpWithholdingTax.class);

	/**
	 * @Author hongbiao
	 *         <p>
	 *         <li>2019年2月18日-下午2:46:33</li>
	 *         <li>功能说明：客户利息税明细列表查询</li>
	 *         </p>
	 * @param cplin
	 *            输入查询条件
	 * @return
	 */
	public static DpWithholdingTaxOut qryWithholdingTax(DpWithholdingTaxIn cplIn) {

		bizlog.method(" DpWithholdingTax.qryWithholdingTax begin >>>>>>>>>>>>>>>>");
		bizlog.debug(" DpWithholdingTaxIn = [%s]", cplIn);

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		DpWithholdingTaxOut withholdingTaxOut = BizUtil.getInstance(DpWithholdingTaxOut.class);

		String custNo = null;
		DpCustBaseInfo custData = null;
		if (CommUtil.isNotNull(cplIn.getCust_no()) || (CommUtil.isNotNull(cplIn.getDoc_no()) && CommUtil.isNotNull(cplIn.getDoc_type()))) {

			// 客户号+使用证件号+证件类型查询客户号
			custData = DpCustomerIobus.getCustBaseInfo(cplIn.getCust_no(), null, cplIn.getDoc_type(), cplIn.getDoc_no());
			custNo = custData.getCust_no();
		}

		// 获取子户号
		String subAccountNo = null;
		DpaSubAccount subAccount = null;
		if (CommUtil.isNotNull(cplIn.getAcct_no()) && CommUtil.isNotNull(cplIn.getSub_acct_seq())) {

			subAccount = DpaSubAccountDao.selectOne_odb4(cplIn.getAcct_no(), cplIn.getSub_acct_seq(), false);
			if (subAccount == null) {

				throw DpBaseErr.DpBase.E0010(cplIn.getAcct_no(), cplIn.getSub_acct_seq());
			}
			subAccountNo = subAccount.getSub_acct_no();
		}

		// 查询利息结算信息
		Page<DphInterestSettled> taxList = SqlDpInterestDao.selInstTaxList(custNo, cplIn.getAcct_no(), cplIn.getSub_acct_seq(), cplIn.getStart_date(), cplIn.getEnd_date(),
				runEnvs.getBusi_org_id(), runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		runEnvs.setTotal_count(taxList.getRecordCount());

		for (DphInterestSettled taxInfo : taxList.getRecords()) {

			DpWithholdingTaxDetail taxDetail = BizUtil.getInstance(DpWithholdingTaxDetail.class);

			if (CommUtil.isNull(custNo)) {

				DpaSubAccount subAcct = DpaSubAccountDao.selectOneWithLock_odb1(taxInfo.getAcct_no(), taxInfo.getSub_acct_no(), false);

				custData = DpCustomerIobus.getCustBaseInfo(subAcct.getCust_no(), subAcct.getCust_type());
			}

			taxDetail.setCust_no(custData.getCust_no());
			taxDetail.setDoc_no(custData.getDoc_no());
			taxDetail.setInst_paid(taxInfo.getInst_paid());
			taxDetail.setInst_withholding_tax(taxInfo.getInst_withholding_tax());

			String date = taxInfo.getTrxn_date();
			String payInstDate = date.substring(6) + "/" + date.substring(4, 6) + "/" + date.substring(0, 4);
			// 输出日期格式为 28/06/2018
			taxDetail.setPay_inst_date(payInstDate);

			// 新增list01页面返回值后台
			taxDetail.setCust_name(CommUtil.equals("en", cplIn.getLanguage()) ? custData.getCust_name() : custData.getCust_foreign_name());
			String title = ApDropListApi.getText("TITLE", custData.getTitle());

			taxDetail.setTitle_name(title);

			// TODO: 如果需要地址信息，单独提供地址信息查询接口
			/*
			 * for (IoCfAddress addressList : custData.getList02()) {
			 */
			/*
			 * if (CommUtil.equals(addressList.getAddress_type(), "001") ||
			 * CommUtil.equals(addressList.getAddress_type(), "004")) { String
			 * sub_district_desc = ApDropListApi.getText("SUB_DISTRICT",
			 * addressList.getSub_district(), false); String district_desc =
			 * ApDropListApi.getText("DISTRICT", addressList.getDistrict(),
			 * false); String province_desc = ApDropListApi.getText("PROVINCE",
			 * addressList.getProvince(), false); String postcode_desc =
			 * ApDropListApi.getText("POSTCODE", addressList.getPostcode(),
			 * false); /** FSD MVP1 006 Tax V1.1 Address Line 1
			 * <Building/Village> Address Line 2 ชั้น <Floor> เลขที่ <No.>
			 * หมู่ที่ <Moo> ซอย <Soi> ถนน<Road> Address Line 3 <Sub-District>
			 * <District> <Province> Address Line 4 <Post Code>
			 */
			/*
			 * taxDetail.setAddress1(addressList.getBuilding_name()); String
			 * floor = CommUtil.isNotNull(addressList.getFloor_no()) ? "ชั้น " +
			 * addressList.getFloor_no() : ""; String no =
			 * CommUtil.isNotNull(addressList.getAddress_no()) ? " เลขที่ " +
			 * addressList.getAddress_no() : ""; String moo =
			 * CommUtil.isNotNull(addressList.getMoo()) ? " หมู่ที่ " +
			 * addressList.getMoo() : ""; String soi =
			 * CommUtil.isNotNull(addressList.getSoi()) ? " ซอย " +
			 * addressList.getSoi() : ""; String road =
			 * CommUtil.isNotNull(addressList.getRoad()) ? " ถนน " +
			 * addressList.getRoad() : ""; taxDetail.setAddress2(floor + no +
			 * moo + soi + road); taxDetail.setAddress3(sub_district_desc+ " " +
			 * district_desc+ " " + province_desc);
			 * taxDetail.setAddress4(postcode_desc); }
			 * 
			 * }
			 */

			// 子户号为空表示外部未指定子户查询。
			if (subAccountNo == null) {
				subAccount = DpaSubAccountDao.selectOne_odb1(taxInfo.getAcct_no(), taxInfo.getSub_acct_no(), true);
			}

			String acctNo = subAccount.getAcct_no();
			// 账号输出格式为：000-7-00001-5
			String acct_no = acctNo.substring(0, 3) + "-" + acctNo.substring(3, 4) + "-" + acctNo.substring(4, 9) + "-" + acctNo.substring(9, 10);

			taxDetail.setAcct_no(acct_no);
			taxDetail.setProd_id(subAccount.getProd_id());
			taxDetail.setSub_acct_seq(subAccount.getSub_acct_seq());

			withholdingTaxOut.getList01().add(taxDetail);
		}

		DpInstAndTax totalInstTax = SqlDpInterestDao.selTotalInstTax(custNo, cplIn.getAcct_no(), cplIn.getSub_acct_seq(), cplIn.getStart_date(), cplIn.getEnd_date(),
				runEnvs.getBusi_org_id(), false);

		withholdingTaxOut.setTotal_interest(totalInstTax.getInterest());
		withholdingTaxOut.setTotal_tax(totalInstTax.getInterest_tax());

		bizlog.debug(" withholdingTaxOut = [%s]", withholdingTaxOut);
		bizlog.method(" DpWithholdingTax.qryWithholdingTax end <<<<<<<<<<<<<<<<");

		return withholdingTaxOut;
	}
}
