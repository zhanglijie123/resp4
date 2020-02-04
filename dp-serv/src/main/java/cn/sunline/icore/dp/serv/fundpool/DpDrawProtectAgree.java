package cn.sunline.icore.dp.serv.fundpool;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApDataAuditApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.common.DpPublicCheck;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpInstructDao;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtect;
import cn.sunline.icore.dp.serv.tables.TabDpProtocol.DpbWithdrawlProtectDao;
import cn.sunline.icore.dp.serv.type.ComDpCommon.DpCheckPassWord;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpMntWithdrawProtectAgreeIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpMntWithdrawProtectAgreeOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpQryWithdrawProtectAgreeIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpQryWithdrawProtectAgreeOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpWithdrawProtectAgreeAdjustIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpWithdrawProtectAgreeAdjustOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpWithdrawProtectAgreeIn;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpWithdrawProtectAgreeOut;
import cn.sunline.icore.dp.serv.type.ComDpInstruct.DpWithdrawProtectOrderAdjust;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_PROTECTTYPE;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

public class DpDrawProtectAgree {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDrawProtectAgree.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月3日-上午11:28:17</li>
	 *         <li>功能说明：支取保护签约</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpWithdrawProtectAgreeOut withdrawProtectAgree(DpWithdrawProtectAgreeIn cplIn) {
		bizlog.method(" DpDrawProtectAgree.withdrawProtectAgree begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 1.相关检查
		// 1.1 非空校验
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getProtect_acct_no(), DpDict.A.protect_acct_no.getId(), DpDict.A.protect_acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getProtect_ccy(), DpDict.A.protect_ccy.getId(), DpDict.A.protect_ccy.getLongName());

		// 取得被保护账号信息
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		// 1.2 同名检查
		if (CommUtil.isNotNull(cplIn.getAcct_name()) && !CommUtil.equals(acctInfo.getAcct_name(), cplIn.getAcct_name())) {

			throw DpErr.Dp.E0058(cplIn.getAcct_name(), acctInfo.getAcct_name());
		}

		// 1.3 验密
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 1.4 保护类型信息加工
		E_DEMANDORTIME ddTimeFlag = E_DEMANDORTIME.DEMAND;// 默认定活标志为活期,以便检查保护账号信息

		if (cplIn.getProtect_type() == null) {

			cplIn.setProtect_type(E_PROTECTTYPE.COMMON_DEMAND);
		}
		else if (cplIn.getProtect_type() == E_PROTECTTYPE.INTELLIGENT) {

			BizUtil.fieldNotNull(cplIn.getProtect_sub_acct_seq(), DpDict.A.protect_sub_acct_seq.getId(), DpDict.A.protect_sub_acct_seq.getLongName());

			ddTimeFlag = E_DEMANDORTIME.TIME;// 智能存款账户只能是定期户
		}

		// 1.7 校验失效日期
		if (CommUtil.isNotNull(cplIn.getExpiry_date())) {

			// 校验日期格式
			if (!BizUtil.isDateString(cplIn.getExpiry_date())) {

				throw APPUB.E0011(cplIn.getExpiry_date());
			}

			if (CommUtil.compare(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {

				throw DpBase.E0294(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
			}

		}
		else {

			cplIn.setExpiry_date(ApConst.DEFAULT_MAX_DATE);
		}

		// 2.定位被保护账号信息
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		// 2.1 定位被保护子账户信息
		DpaSubAccount subAccount = DpaSubAccountDao.selectOne_odb1(acctAccessOut.getAcct_no(), acctAccessOut.getSub_acct_no(), true);

		// 2.2 定位保护账号信息
		DpAcctAccessIn protAcctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		protAcctAccessIn.setAcct_no(cplIn.getProtect_acct_no());
		protAcctAccessIn.setCcy_code(cplIn.getProtect_ccy());
		protAcctAccessIn.setSub_acct_seq(cplIn.getProtect_sub_acct_seq());
		protAcctAccessIn.setDd_td_ind(ddTimeFlag);

		DpAcctAccessOut protAcctAccessOut = DpToolsApi.locateSingleSubAcct(protAcctAccessIn);

		// 2.3 检查保护账号与被保护账号是否一致
		if (CommUtil.equals(subAccount.getSub_acct_no(), protAcctAccessOut.getSub_acct_no())) {

			throw DpErr.Dp.E0353(protAcctAccessOut.getAcct_no(), protAcctAccessOut.getSub_acct_seq(), subAccount.getAcct_no(), subAccount.getSub_acct_seq());
		}

		// 2.4 账户已经作为保护账户就不能再做被保护账户
		DpbWithdrawlProtect existProtect = DpbWithdrawlProtectDao.selectFirst_odb5(acctAccessOut.getAcct_no(), acctAccessOut.getCcy_code(), E_STATUS.VALID, false);

		if (CommUtil.isNotNull(existProtect)) {
			throw DpErr.Dp.E0363(acctAccessOut.getAcct_no());
		}

		// 2.5 账户已经作为被保护账户就不能再做保护账户
		DpbWithdrawlProtect existAcct = DpbWithdrawlProtectDao.selectFirst_odb4(protAcctAccessOut.getAcct_no(), protAcctAccessOut.getCcy_code(), E_STATUS.VALID, false);

		if (CommUtil.isNotNull(existAcct)) {
			throw DpErr.Dp.E0364(protAcctAccessOut.getAcct_no());
		}

		// 3.插入相关表数据
		String protOrder = "1";// TODO:保护次序默认都取1

		// 3.1 插入支取保护协议表
		DpbWithdrawlProtect drawProtInfo = BizUtil.getInstance(DpbWithdrawlProtect.class);

		drawProtInfo.setAgree_no(ApSeqApi.genSeq("AGREE_NO")); // agreement no.
		drawProtInfo.setAcct_no(acctAccessOut.getAcct_no()); // account no
		drawProtInfo.setCcy_code(acctAccessOut.getCcy_code()); // currency code
		drawProtInfo.setProtect_type(cplIn.getProtect_type()); // protect type
		drawProtInfo.setProtect_acct_no(protAcctAccessOut.getAcct_no()); //
		drawProtInfo.setProtect_ccy(protAcctAccessOut.getCcy_code()); // protect
																		// currency
		drawProtInfo.setProtect_sub_acct_seq(CommUtil.nvl(cplIn.getProtect_sub_acct_seq(), protAcctAccessOut.getSub_acct_seq())); //
		drawProtInfo.setProtect_order(protOrder); // protect order
		drawProtInfo.setAgree_status(E_STATUS.VALID); // agree status
		drawProtInfo.setSign_date(BizUtil.getTrxRunEnvs().getTrxn_date()); // resignation
		drawProtInfo.setSign_seq(BizUtil.getTrxRunEnvs().getBusi_seq()); // sign
		drawProtInfo.setEffect_date(CommUtil.nvl(cplIn.getEffect_date(), BizUtil.getTrxRunEnvs().getTrxn_date()));
		drawProtInfo.setExpiry_date(CommUtil.nvl(cplIn.getExpiry_date(), ApConst.DEFAULT_MAX_DATE)); // expiry
																										// date
		drawProtInfo.setStop_use_ind(E_YESORNO.NO);

		DpbWithdrawlProtectDao.insert(drawProtInfo);

		// 4.组织输出
		DpWithdrawProtectAgreeOut cplOut = BizUtil.getInstance(DpWithdrawProtectAgreeOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), subAccount.getAcct_no()) ? null : cplIn.getAcct_no()); //
		cplOut.setAcct_no(drawProtInfo.getAcct_no()); // account no
		cplOut.setAcct_type(acctInfo.getAcct_type()); // account type
		cplOut.setAcct_name(acctInfo.getAcct_name()); // account name
		cplOut.setCcy_code(drawProtInfo.getCcy_code()); // currency code
		cplOut.setAgree_no(drawProtInfo.getAgree_no()); // agreement no.
		cplOut.setProtect_type(drawProtInfo.getProtect_type()); // protect type
		cplOut.setProtect_acct_no(drawProtInfo.getProtect_acct_no()); // protect
		cplOut.setProtect_ccy(drawProtInfo.getProtect_ccy()); // protect
		cplOut.setProtect_sub_acct_seq(drawProtInfo.getProtect_sub_acct_seq()); // protect
		cplOut.setOpp_acct_name(DpaAccountDao.selectOne_odb1(drawProtInfo.getProtect_acct_no(), true).getAcct_name()); //
		cplOut.setEffect_date(drawProtInfo.getSign_date()); // effect date
		cplOut.setExpiry_date(drawProtInfo.getExpiry_date()); // expiry date

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpDrawProtectAgree.withdrawProtectAgree end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月3日-下午2:56:16</li>
	 *         <li>功能说明：支取保护协议维护</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpMntWithdrawProtectAgreeOut withdrawProtectAgreeMnt(DpMntWithdrawProtectAgreeIn cplIn) {
		bizlog.method(" DpDrawProtectAgree.withdrawProtectAgreeMnt begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 1.相关检查
		// 1.1 非空校验
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getAgree_no(), SysDict.A.agree_no.getId(), SysDict.A.agree_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getCancle_agree_ind(), DpBaseDict.A.cancle_agree_ind.getId(), DpBaseDict.A.cancle_agree_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

		// 1.2 检查解约标志
		if (cplIn.getCancle_agree_ind() == E_YESORNO.YES) {

			if (CommUtil.isNotNull(cplIn.getEffect_date())) {

				throw DpErr.Dp.E0350();
			}

			if (CommUtil.isNotNull(cplIn.getExpiry_date())) {

				throw DpErr.Dp.E0351();
			}
		}

		// 1.3 检查生效日期与失效日期合法性
		if (CommUtil.isNotNull(cplIn.getEffect_date()) || CommUtil.isNotNull(cplIn.getExpiry_date())) {

			if (CommUtil.isNotNull(cplIn.getEffect_date()) && CommUtil.isNotNull(cplIn.getExpiry_date())) {

				BizUtil.checkEffectDate(cplIn.getEffect_date(), cplIn.getExpiry_date());

			}

			if (CommUtil.isNotNull(cplIn.getExpiry_date()) && CommUtil.compare(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) < 0) {

				throw DpBase.E0294(cplIn.getExpiry_date(), BizUtil.getTrxRunEnvs().getTrxn_date());
			}

		}

		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), null, false);

		// 2.获取需维护的记录
		DpbWithdrawlProtect drawlProtInfo = DpbWithdrawlProtectDao.selectOneWithLock_odb1(acctInfo.getAcct_no(), cplIn.getAgree_no(), false);

		if (drawlProtInfo == null) {

			throw APPUB.E0005(OdbFactory.getTable(DpbWithdrawlProtect.class).getLongname(), SysDict.A.agree_no.getId(), cplIn.getAgree_no());
		}

		// 2.1 校验数据版本号
		if (CommUtil.compare(drawlProtInfo.getData_version(), cplIn.getData_version()) != 0) {

			throw ApPubErr.APPUB.E0018(DpbWithdrawlProtect.class.getName());
		}

		// 2.2 校验协议是否生效
		if (drawlProtInfo.getAgree_status() == E_STATUS.INVALID) {

			throw DpBase.E0296(drawlProtInfo.getAgree_no());
		}

		// 2.3 验密
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			checkIn.setTrxn_password(cplIn.getTrxn_password());
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 3. 开始数据维护
		// 3.1 复制一份,做审计用
		DpbWithdrawlProtect oldDrawlProtInfo = BizUtil.clone(DpbWithdrawlProtect.class, drawlProtInfo);

		// 3.2 根据解约标志进行数据维护
		if (cplIn.getCancle_agree_ind() == E_YESORNO.YES) {// 解约续维护协议状态,解约日期,解约流水

			drawlProtInfo.setAgree_status(E_STATUS.INVALID);
			drawlProtInfo.setCancel_date(BizUtil.getTrxRunEnvs().getTrxn_date());
			drawlProtInfo.setCancel_seq(BizUtil.getTrxRunEnvs().getBusi_seq());
		}
		else {

			drawlProtInfo.setStop_use_ind(CommUtil.nvl(cplIn.getStop_use_ind(), drawlProtInfo.getStop_use_ind()));
			drawlProtInfo.setEffect_date(CommUtil.nvl(cplIn.getEffect_date(), drawlProtInfo.getEffect_date()));
			drawlProtInfo.setExpiry_date(CommUtil.nvl(cplIn.getExpiry_date(), ApConst.DEFAULT_MAX_DATE));
		}

		// 3.3 登记审计,并检查数据是否发生变化,无变化则报错
		if (0 == ApDataAuditApi.regLogOnUpdateBusiness(oldDrawlProtInfo, drawlProtInfo)) {

			throw ApPubErr.APPUB.E0023(OdbFactory.getTable(DpbWithdrawlProtect.class).getLongname());
		}

		// 3.4 更新数据
		DpbWithdrawlProtectDao.updateOne_odb1(drawlProtInfo);

		// 4.组织输出
		DpMntWithdrawProtectAgreeOut cplOut = BizUtil.getInstance(DpMntWithdrawProtectAgreeOut.class);

		cplOut.setAcct_no(drawlProtInfo.getAcct_no()); // account no
		cplOut.setAcct_type(acctInfo.getAcct_type()); // account type
		cplOut.setAcct_name(acctInfo.getAcct_name()); // account name
		cplOut.setCcy_code(drawlProtInfo.getCcy_code()); // currency code
		cplOut.setAgree_no(drawlProtInfo.getAgree_no()); // agreement no.
		cplOut.setEffect_date(drawlProtInfo.getSign_date()); // effect date
		cplOut.setExpiry_date(drawlProtInfo.getExpiry_date()); // expiry date
		cplOut.setAgree_status(drawlProtInfo.getAgree_status()); // agree status

		bizlog.debug("cplIn=[%s]", cplOut);
		bizlog.method(" DpDrawProtectAgree.withdrawProtectAgreeMnt end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月3日-下午2:56:31</li>
	 *         <li>功能说明：支取保护协议查询</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static Options<DpQryWithdrawProtectAgreeOut> qryWithdrawProtectAgree(DpQryWithdrawProtectAgreeIn cplIn) {
		bizlog.method(" DpDrawProtectAgree.qryWithdrawProtectAgree begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		if (CommUtil.isNull(cplIn.getAcct_no()) && CommUtil.isNull(cplIn.getAgree_no())) {

			throw DpErr.Dp.E0297();
		}

		DpaAccount acctInfo = null;

		// 获取账号信息
		if (CommUtil.isNotNull(cplIn.getAcct_no())) {

			acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

			cplIn.setAcct_no(acctInfo.getAcct_no());
		}

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();// 获取公共变量

		String orgId = runEnvs.getBusi_org_id();// 取得法人代码

		Page<DpbWithdrawlProtect> drawProtList = SqlDpInstructDao.selWithdrawProtectInfoByCustomize(orgId, cplIn.getAgree_no(), cplIn.getAcct_no(), cplIn.getCcy_code(),
				cplIn.getProtect_type(), cplIn.getProtect_acct_no(), cplIn.getAgree_status(), runEnvs.getPage_start(), runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		if (drawProtList == null || drawProtList.getRecords().size() <= 0) {

			return null;
		}

		if (acctInfo == null) {

			acctInfo = DpaAccountDao.selectOne_odb1(drawProtList.getRecords().get(0).getAcct_no(), true);
		}

		runEnvs.setTotal_count(drawProtList.getRecordCount());// 返回总记录数

		Options<DpQryWithdrawProtectAgreeOut> cplOut = new DefaultOptions<DpQryWithdrawProtectAgreeOut>();

		DpQryWithdrawProtectAgreeOut cplSubOut = null;

		// 补充输出
		for (DpbWithdrawlProtect singleDetl : drawProtList.getRecords()) {

			cplSubOut = BizUtil.getInstance(DpQryWithdrawProtectAgreeOut.class);

			cplSubOut.setAgree_no(singleDetl.getAgree_no()); // agreement no.
			cplSubOut.setAcct_no(singleDetl.getAcct_no()); // account no
			cplSubOut.setAcct_type(acctInfo.getAcct_type()); // account type
			cplSubOut.setAcct_name(acctInfo.getAcct_name()); // account name
			cplSubOut.setCcy_code(singleDetl.getCcy_code()); // currency code
			cplSubOut.setProtect_acct_no(singleDetl.getProtect_acct_no()); //
			cplSubOut.setProtect_ccy(singleDetl.getProtect_ccy()); //
			cplSubOut.setProtect_sub_acct_seq(singleDetl.getProtect_sub_acct_seq()); //
			cplSubOut.setExpiry_date(singleDetl.getExpiry_date()); //
			cplSubOut.setProtect_order(singleDetl.getProtect_order()); //
			cplSubOut.setAgree_status(singleDetl.getAgree_status()); //
			cplSubOut.setSign_date(singleDetl.getSign_date()); // sign date
			cplSubOut.setSign_seq(singleDetl.getSign_seq()); // sign seq
			cplSubOut.setCancel_date(singleDetl.getCancel_date()); //
			cplSubOut.setCancel_seq(singleDetl.getCancel_seq()); // cancel seq
			cplSubOut.setData_version(singleDetl.getData_version());

			cplOut.add(cplSubOut);
		}

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpDrawProtectAgree.qryWithdrawProtectAgree end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2017年8月9日-上午9:25:55</li>
	 *         <li>功能说明：账户保护次序调整</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static DpWithdrawProtectAgreeAdjustOut withdrawProtectAgreeAdjust(DpWithdrawProtectAgreeAdjustIn cplIn) {
		bizlog.method(" DpDrawProtectAgree.withdrawProtectAgreeAdjust begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);
		// 1.相关检查
		// 1.1 非空校验
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCheck_password_ind(), SysDict.A.check_password_ind.getId(), SysDict.A.check_password_ind.getLongName());
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 1.2 校验保护次序列表
		if (cplIn.getList01() == null || cplIn.getList01().size() <= 0) {

			throw DpErr.Dp.E0354();
		}

		// 定位账号
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), false);

		// 1.3 验密
		if (cplIn.getCheck_password_ind() == E_YESORNO.YES) {

			DpCheckPassWord checkIn = BizUtil.getInstance(DpCheckPassWord.class);
			
			checkIn.setTrxn_password(cplIn.getTrxn_password());
			
			DpPublicCheck.checkPassWord(acctInfo, checkIn);
		}

		// 定位相关子账号
		DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

		acctAccessIn.setAcct_no(cplIn.getAcct_no());
		acctAccessIn.setCcy_code(cplIn.getCcy_code());
		acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

		DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

		String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();// 取出法人代码
		String acctNo = acctAccessOut.getAcct_no();// 取出账号
		int totalModify = 0;

		// 循环处理保护次序
		for (DpWithdrawProtectOrderAdjust orderDetl : cplIn.getList01()) {

			// 列表非空校验
			BizUtil.fieldNotNull(orderDetl.getProtect_order(), DpDict.A.protect_order.getId(), DpDict.A.protect_order.getLongName());
			BizUtil.fieldNotNull(orderDetl.getAgree_no(), SysDict.A.agree_no.getId(), SysDict.A.agree_no.getLongName());
			BizUtil.fieldNotNull(orderDetl.getData_version(), SysDict.A.data_version.getId(), SysDict.A.data_version.getLongName());

			DpbWithdrawlProtect drawProtInfo = DpbWithdrawlProtectDao.selectOne_odb1(acctInfo.getAcct_no(), orderDetl.getAgree_no(), false);

			if (drawProtInfo == null || drawProtInfo.getAgree_status() != E_STATUS.VALID || !CommUtil.equals(acctNo, drawProtInfo.getAcct_no())) {

				throw APPUB.E0025(OdbFactory.getTable(DpbWithdrawlProtect.class).getLongname(), SysDict.A.agree_no.getId(), orderDetl.getAgree_no(),
						SysDict.A.acct_no.getId(), cplIn.getAcct_no(), DpBaseDict.A.agree_status.getId(), E_STATUS.VALID.getValue());
			}

			// 校验数据版本号
			if (CommUtil.compare(drawProtInfo.getData_version(), orderDetl.getData_version()) != 0) {

				throw ApPubErr.APPUB.E0018(DpbWithdrawlProtect.class.getName());
			}

			// 3.1 复制一份,做审计用drawProtInfo
			DpbWithdrawlProtect oldDrawlProtInfo = BizUtil.clone(DpbWithdrawlProtect.class, drawProtInfo);

			// 开始数据更新
			drawProtInfo.setProtect_order(orderDetl.getProtect_order());

			int auditInd = ApDataAuditApi.regLogOnUpdateParameter(oldDrawlProtInfo, drawProtInfo);
			// 登记审计,并检查数据是否发生变化,无变化则报错
			if (0 == auditInd) {

				// throw
				// ApPubErr.APPUB.E0023(OdbFactory.getTable(DpbWithdrawlProtect.class).getLongname());

				// 无变化继续下一条处理
				continue;
			}
			totalModify++;

			// 更新数据
			DpbWithdrawlProtectDao.updateOne_odb1(drawProtInfo);

		}
		// 无变化则报错
		if (totalModify == 0) {

			throw ApPubErr.APPUB.E0023(OdbFactory.getTable(DpbWithdrawlProtect.class).getLongname());
		}

		// 组织输出
		DpWithdrawProtectAgreeAdjustOut cplOut = BizUtil.getInstance(DpWithdrawProtectAgreeAdjustOut.class);

		cplOut.setAcct_no(acctAccessOut.getAcct_no()); //
		cplOut.setAcct_type(acctAccessOut.getAcct_type()); //
		cplOut.setAcct_name(acctAccessOut.getAcct_name()); //
		cplOut.setCcy_code(acctAccessOut.getCcy_code()); //

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpDrawProtectAgree.withdrawProtectAgreeAdjust end <<<<<<<<<<<<<<<<");
		return cplOut;
	}
}
