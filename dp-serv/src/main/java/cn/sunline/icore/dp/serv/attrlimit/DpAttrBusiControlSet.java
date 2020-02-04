package cn.sunline.icore.dp.serv.attrlimit;

import cn.sunline.icore.ap.type.ComApAttr.ApAttrListResult;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAttrLimitApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpAttrLimitBase.DpCommonAttributeSet;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.iobus.DpCustomerIobus;
import cn.sunline.icore.dp.serv.type.ComDpAttrLimit.DpAttributeControlIn;
import cn.sunline.icore.dp.serv.type.ComDpAttrLimit.DpAttributeControlOut;
import cn.sunline.icore.dp.serv.type.ComDpAttrLimit.DpQueryAttributeControlIn;
import cn.sunline.icore.dp.serv.type.ComDpAttrLimit.DpQueryAttributeControlOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustSimpleInfo;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_OWNERLEVEL;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：属性业务控制设置
 * </p>
 * 
 * @Author wuqiang
 *         <p>
 *         <li>2017年3月10日-上午9:55:03</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpAttrBusiControlSet {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpAttrBusiControlSet.class);

	/**
	 * @Author wuqiang
	 *         <p>
	 *         <li>2017年3月21日-下午1:21:29</li>
	 *         <li>功能说明：属性业务控制设置主入口</li>
	 *         </p>
	 * @param cplIn
	 *            属性控制设置输入接口
	 * @return cplOut 属性控制设置输出接口
	 */
	public static DpAttributeControlOut attributeControl(DpAttributeControlIn cplIn) {

		bizlog.method(" DpAttrBusiControlSet.attributeControl begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 数据版本号不能为空
		// BizUtil.fieldNotNull(cplIn.getData_version(),
		// SysDict.A.data_version.getId(),
		// SysDict.A.data_version.getLongName());

		boolean attrListIsNullFlag = true;

		// 输出
		DpAttributeControlOut cplOut = BizUtil.getInstance(DpAttributeControlOut.class);

		// 子账户属性维护
		if (CommUtil.isNotNull(cplIn.getList01()) && cplIn.getList01().size() > 0) {

			attrListIsNullFlag = false;

			// 子账户定位输入接口
			DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

			acctAccessIn.setAcct_no(cplIn.getAcct_no());
			acctAccessIn.setAcct_type(cplIn.getAcct_type());
			acctAccessIn.setCcy_code(cplIn.getCcy_code());
			acctAccessIn.setDd_td_ind(null);
			acctAccessIn.setProd_id(cplIn.getProd_id());
			acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

			// 获取存款子账户信息
			DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

			// 子账户信息
			DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

			// 通用属性设置
			DpCommonAttributeSet cplCommonSetIn = BizUtil.getInstance(DpCommonAttributeSet.class);

			cplCommonSetIn.setAttr_level(E_OWNERLEVEL.SUB_ACCTOUNT);
			cplCommonSetIn.setAttr_owner_id(subAccount.getSub_acct_no());
			cplCommonSetIn.setAcct_no(subAccount.getAcct_no());
			cplCommonSetIn.setData_version(cplIn.getData_version());
			cplCommonSetIn.setListattr(cplIn.getList01());

			ApAttrListResult cplCommonSetResult = commonAttributeSet(cplCommonSetIn);

			// 输出
			cplOut.setCust_no(acctAccessOut.getCust_no());// 客户号
			cplOut.setCard_no(acctAccessOut.getCard_no());// 卡号
			cplOut.setAcct_no(acctAccessOut.getAcct_no());// 账号
			cplOut.setAcct_name(acctAccessOut.getAcct_name());
			cplOut.setList01(cplCommonSetResult.getList01());
		}

		// 账户属性设置
		if (CommUtil.isNotNull(cplIn.getList02()) && cplIn.getList02().size() > 0) {

			attrListIsNullFlag = false;

			DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

			// 通用属性设置
			DpCommonAttributeSet cplCommonSetIn = BizUtil.getInstance(DpCommonAttributeSet.class);

			cplCommonSetIn.setAttr_level(E_OWNERLEVEL.ACCOUNT);
			cplCommonSetIn.setAttr_owner_id(account.getAcct_no());
			cplCommonSetIn.setData_version(cplIn.getData_version());
			cplCommonSetIn.setListattr(cplIn.getList02());

			ApAttrListResult cplCommonSetResult = commonAttributeSet(cplCommonSetIn);

			// 输出
			cplOut.setCust_no(account.getCust_no());// 客户号
			cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no());// 卡号
			cplOut.setAcct_no(account.getAcct_no());// 账号
			cplOut.setAcct_name(account.getAcct_name());
			cplOut.setList02(cplCommonSetResult.getList01());
		}

		// 卡属性设置
		if (CommUtil.isNotNull(cplIn.getList03()) && cplIn.getList03().size() > 0) {

			attrListIsNullFlag = false;

			DpaCard cardInfo = DpaCardDao.selectOneWithLock_odb1(cplIn.getAcct_no(), false);

			if (cardInfo == null) {
				throw DpBase.E0002(cplIn.getAcct_no());
			}

			// 通用属性设置
			DpCommonAttributeSet cplCommonSetIn = BizUtil.getInstance(DpCommonAttributeSet.class);

			cplCommonSetIn.setAttr_level(E_OWNERLEVEL.CARD);
			cplCommonSetIn.setAttr_owner_id(cardInfo.getCard_no());
			cplCommonSetIn.setData_version(cplIn.getData_version());
			cplCommonSetIn.setListattr(cplIn.getList03());

			ApAttrListResult cplCommonSetResult = commonAttributeSet(cplCommonSetIn);

			// 输出
			cplOut.setCust_no(cardInfo.getCust_no());// 客户号
			cplOut.setCard_no(cplIn.getAcct_no());// 卡号
			cplOut.setList03(cplCommonSetResult.getList01());
		}

		// 客户属性设置
		if (CommUtil.isNotNull(cplIn.getList04()) && cplIn.getList04().size() > 0) {

			attrListIsNullFlag = false;

			String custNo = cplIn.getCust_no();

			if (CommUtil.isNull(custNo)) {

				DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

				custNo = account.getCust_no();

				cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no());// 卡号
				cplOut.setAcct_no(account.getAcct_no());// 账号
				cplOut.setAcct_name(account.getAcct_name());
			}

			// 通用属性设置
			DpCommonAttributeSet cplCommonSetIn = BizUtil.getInstance(DpCommonAttributeSet.class);

			cplCommonSetIn.setAttr_level(E_OWNERLEVEL.CUSTOMER);
			cplCommonSetIn.setAttr_owner_id(custNo);
			cplCommonSetIn.setData_version(cplIn.getData_version());
			cplCommonSetIn.setListattr(cplIn.getList04());

			ApAttrListResult cplCommonSetResult = commonAttributeSet(cplCommonSetIn);

			cplOut.setCust_no(custNo);// 客户号
			cplOut.setList04(cplCommonSetResult.getList01());
		}

		// 属性列表都为空则报错
		if (attrListIsNullFlag == true) {
			throw DpBase.E0111();
		}

		// 补充输出
		cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());// 子账户序号
		cplOut.setCcy_code(cplIn.getCcy_code());// 货币代号
		cplOut.setData_version(cplIn.getData_version());// 版本号

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAttrBusiControlSet.attributeControl end >>>>>>>>>>>>>>>>");

		return cplOut;
	}

	/**
	 * @Author wuqiang
	 *         <p>
	 *         <li>2017年3月21日-下午1:21:29</li>
	 *         <li>功能说明：通用属性设置方法</li>
	 *         </p>
	 * @param cplIn
	 *            通用属性控制设置接口
	 * @return cplOut 属性控制设置输出接口
	 */
	private static ApAttrListResult commonAttributeSet(DpCommonAttributeSet cplIn) {

		bizlog.method(" DpAttrBusiControlSet.commonAttributeSet begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 属性设置信息必须输入
		if (CommUtil.isNull(cplIn.getListattr()) || cplIn.getListattr().size() == 0) {

			throw DpBase.E0111();
		}

		ApAttrListResult cplResult = BizUtil.getInstance(ApAttrListResult.class);

		// 客户、卡、账户、子账户的属性维护调用底层API
		if (CommUtil.in(cplIn.getAttr_level(), E_OWNERLEVEL.CUSTOMER, E_OWNERLEVEL.CARD, E_OWNERLEVEL.ACCOUNT, E_OWNERLEVEL.SUB_ACCTOUNT)) {

			cplResult = DpAttrLimitApi.modifyAttr(cplIn);
		}
		else {
			throw APPUB.E0026(SysDict.A.attr_level.getLongName(), cplIn.getAttr_level().getValue());
		}

		bizlog.debug(">>>>>>cplResult = [%s]", cplResult);
		bizlog.method(" DpAttrBusiControlSet.commonAttributeSet end >>>>>>>>>>>>>>>>");

		return cplResult;
	}

	/**
	 * @Author wuqiang
	 *         <p>
	 *         <li>2017年3月21日-下午1:21:29</li>
	 *         <li>功能说明：属性业务控制查询主入口</li>
	 *         </p>
	 * @param cplIn
	 *            属性控制查询输入接口
	 * @return cplOut 属性控制查询输出接口
	 */
	public static DpQueryAttributeControlOut queryAttributeControl(DpQueryAttributeControlIn cplIn) {

		bizlog.method(" DpAttrBusiControlQuery.queryAttributeControl begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		String custNo = cplIn.getCust_no();
		String cardNo = "";
		String acctNo = "";
		String subAcNo = "";
		String acctName = "";

		if (CommUtil.isNotNull(cplIn.getAcct_no())) {

			DpaAccount dpaAccount = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

			custNo = dpaAccount.getCust_no();
			acctNo = dpaAccount.getAcct_no();
			cardNo = CommUtil.equals(cplIn.getAcct_no(), dpaAccount.getAcct_no()) ? null : cplIn.getAcct_no();
			acctName = dpaAccount.getAcct_name();

			// 定位子户
			if (CommUtil.isNotNull(cplIn.getCcy_code()) || CommUtil.isNotNull(cplIn.getSub_acct_seq())) {

				// 子账户定位输入接口
				DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

				acctAccessIn.setAcct_no(cplIn.getAcct_no());
				acctAccessIn.setAcct_type(cplIn.getAcct_type());
				acctAccessIn.setCcy_code(cplIn.getCcy_code());
				acctAccessIn.setDd_td_ind(null);
				acctAccessIn.setProd_id(cplIn.getProd_id());
				acctAccessIn.setSub_acct_seq(cplIn.getSub_acct_seq());

				// 获取存款子账户信息
				DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

				subAcNo = acctAccessOut.getSub_acct_no();
			}
		}

		// 输出对象
		DpQueryAttributeControlOut cplOut = BizUtil.getInstance(DpQueryAttributeControlOut.class);

		// 子账户属性
		if (CommUtil.isNotNull(subAcNo)) {

			DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctNo, subAcNo, true);

			cplOut.setSub_acct_seq(cplIn.getSub_acct_seq());// 子账户序号
			cplOut.setCcy_code(cplIn.getCcy_code());// 货币代号
			cplOut.setList01(DpAttrLimitApi.attrValueSwitch(E_OWNERLEVEL.SUB_ACCTOUNT, subAcNo, subAccount.getAttr_value()));
		}

		// 账户属性
		if (CommUtil.isNotNull(acctNo)) {

			DpaAccount account = DpaAccountDao.selectOne_odb1(acctNo, true);

			cplOut.setList02(DpAttrLimitApi.attrValueSwitch(E_OWNERLEVEL.ACCOUNT, acctNo, account.getAttr_value()));
		}

		// 卡属性
		if (CommUtil.isNotNull(cardNo)) {

			DpaCard dpaCard = DpaCardDao.selectOne_odb1(cardNo, false);

			cplOut.setList03(DpAttrLimitApi.attrValueSwitch(E_OWNERLEVEL.CARD, cardNo, dpaCard.getAttr_value()));
		}

		// 客户属性
		if (CommUtil.isNotNull(custNo)) {

			// 客户简要信息查询
			DpCustSimpleInfo cplCustInfo = DpCustomerIobus.getCustSimpleInfo(custNo);

			cplOut.setList04(DpAttrLimitApi.attrValueSwitch(E_OWNERLEVEL.CUSTOMER, custNo, cplCustInfo.getAttr_value()));
		}

		cplOut.setCust_no(custNo);// 客户号
		cplOut.setCard_no(cardNo);// 卡号
		cplOut.setAcct_no(acctNo);// 账号
		cplOut.setAcct_name(acctName);

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpAttrBusiControlQuery.queryAttributeControl end >>>>>>>>>>>>>>>>");

		return cplOut;
	}
}
