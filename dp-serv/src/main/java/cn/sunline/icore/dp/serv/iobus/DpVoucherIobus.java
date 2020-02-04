package cn.sunline.icore.dp.serv.iobus;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountType;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_ALLOW;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCustVoucherInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpVoucherChangeIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpVoucherParmInfo;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpVoucherIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpVoucherIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：客户凭证付出</li>
	 *         </p>
	 * @param accTypeInfo
	 *            账户类型信息
	 * @param sAcctNo
	 *            账号
	 * @param sVochType
	 *            凭证类型
	 * @param sVochNo
	 *            凭证号
	 */
	public static void payOutVoucher(DppAccountType accTypeInfo, String sAcctNo, String sVochType, String sVochNo) {

		if (CommUtil.isNull(sVochType) || CommUtil.isNull(sVochNo)) {
			return;
		}

		if (accTypeInfo.getRef_voch_level() == E_VOCHREFLEVEL.ACCT) {
/*
			IoCmVochPayOutIn payVouchCheckIn = BizUtil.getInstance(IoCmVochPayOutIn.class);

			payVouchCheckIn.setVoch_out_type(E_VOCHOUTTYPE.RELATION_ACCT); // 凭证付出类型
			payVouchCheckIn.setPayout_stock_branch(BizUtil.getTrxRunEnvs().getTrxn_branch()); // 付出机构
			payVouchCheckIn.setPayout_stock_teller(BizUtil.getTrxRunEnvs().getTrxn_teller()); // 付出柜员
			payVouchCheckIn.setVoch_type(sVochType); // 凭证类型
			payVouchCheckIn.setVoch_start_no(sVochNo); // 凭证开始号
			payVouchCheckIn.setVoch_finish_no(sVochNo); // 凭证结束号
			payVouchCheckIn.setQuantity(1l); // 数量
			payVouchCheckIn.setVoch_asso_obj_type(E_VOCHASSOBJTYPE.ACCT); // 凭证关联对象类型
			payVouchCheckIn.setCust_type(accTypeInfo.getCust_type()); // 客户类型
			payVouchCheckIn.setAcct_no(sAcctNo); // 账号

           	SysUtil.getRemoteInstance(SrvIoCmVoucher.class).payOutVoucher(payVouchCheckIn); // 凭证付出		
*/
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：客户凭证付出</li>
	 *         </p>
	 * @param account
	 *            账户信息:必输
	 * @param subAcct
	 *            子账户信息
	 * @param sVochType
	 *            凭证类型
	 * @param sVochNo
	 *            凭证号
	 */
	public static void payOutVoucher(DpaAccount account, DpaSubAccount subAcct, String sVochType, String sVochNo) {

		if (CommUtil.isNull(sVochType) || CommUtil.isNull(sVochNo)) {
			return;
		}

		// 账户与凭证绑定
		if (account.getRef_voch_level() == E_VOCHREFLEVEL.ACCT) {
/*
			IoCmVochPayOutIn payVouchCheckIn = BizUtil.getInstance(IoCmVochPayOutIn.class);

			payVouchCheckIn.setVoch_out_type(E_VOCHOUTTYPE.RELATION_ACCT); // 凭证付出类型
			payVouchCheckIn.setPayout_stock_branch(BizUtil.getTrxRunEnvs().getTrxn_branch()); // 付出机构
			payVouchCheckIn.setPayout_stock_teller(BizUtil.getTrxRunEnvs().getTrxn_teller()); // 付出柜员
			payVouchCheckIn.setVoch_type(sVochType); // 凭证类型
			payVouchCheckIn.setVoch_start_no(sVochNo); // 凭证开始号
			payVouchCheckIn.setVoch_finish_no(sVochNo); // 凭证结束号
			payVouchCheckIn.setQuantity(1l); // 数量
			payVouchCheckIn.setVoch_asso_obj_type(E_VOCHASSOBJTYPE.ACCT); // 凭证关联对象类型
			payVouchCheckIn.setCust_type(account.getCust_type()); // 客户类型
			payVouchCheckIn.setAcct_no(account.getAcct_no()); // 账号

		    SysUtil.getRemoteInstance(SrvIoCmVoucher.class).payOutVoucher(payVouchCheckIn); // 凭证付出
*/
		}

		// 子账户与凭证关联
		if (account.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT && subAcct.getCorrelation_voch_ind() == E_YESORNO.YES) {
/*
			IoCmVochPayOutIn payVouchCheckIn = BizUtil.getInstance(IoCmVochPayOutIn.class);

			payVouchCheckIn.setVoch_out_type(E_VOCHOUTTYPE.RELATION_ACCT); // 凭证付出类型
			payVouchCheckIn.setPayout_stock_branch(BizUtil.getTrxRunEnvs().getTrxn_branch()); // 付出机构
			payVouchCheckIn.setPayout_stock_teller(BizUtil.getTrxRunEnvs().getTrxn_teller()); // 付出柜员
			payVouchCheckIn.setVoch_type(sVochType); // 凭证类型
			payVouchCheckIn.setVoch_start_no(sVochNo); // 凭证开始号
			payVouchCheckIn.setVoch_finish_no(sVochNo); // 凭证结束号
			payVouchCheckIn.setQuantity(1l); // 数量
			payVouchCheckIn.setAcct_no(account.getAcct_no()); // 账号
			payVouchCheckIn.setCcy_code(subAcct.getCcy_code()); // 货币代码
			payVouchCheckIn.setSub_acct_seq(subAcct.getSub_acct_seq()); // 子账户序号
			payVouchCheckIn.setVoch_asso_obj_type(subAcct.getDd_td_ind() == E_DEMANDORTIME.DEMAND ? E_VOCHASSOBJTYPE.DEMA_SUB_ACCT : E_VOCHASSOBJTYPE.FIX_SUB_ACCT); // 凭证关联对象类型
			payVouchCheckIn.setCust_type(subAcct.getCust_type()); // 客户类型

			SysUtil.getRemoteInstance(SrvIoCmVoucher.class).payOutVoucher(payVouchCheckIn); // 凭证付出			
*/
		}

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：维护客户凭证状态</li>
	 *         </p>
	 * @param account
	 *            账户信息:必输
	 * @param subAcct
	 *            子账户信息:可为空
	 * @param trxnRemark
	 *            交易备注
	 */
	public static void modifyCustVoucherStatus(DpaAccount account, DpaSubAccount subAcct, String trxnRemark) {

		// 1. 关闭账户层开户凭证
		if (account.getRef_voch_level() == E_VOCHREFLEVEL.ACCT && account.getCorrelation_voch_ind() == E_YESORNO.YES) {
/*
			IoCmCustVochStatusMntIn cplVochMntIn = BizUtil.getInstance(IoCmCustVochStatusMntIn.class);

			cplVochMntIn.setAcct_no(account.getAcct_no());
			cplVochMntIn.setCust_voch_oper_type(E_CUSTVOCHOPERTYPE.CANCLE);
			cplVochMntIn.setVoch_asso_obj_type(E_VOCHASSOBJTYPE.ACCT);
			cplVochMntIn.setTrxn_remark(trxnRemark);

			SysUtil.getRemoteInstance(SrvIoCmVoucher.class).mntCustVochStatus(cplVochMntIn);			
*/
		}

		// 2. 关闭子账户层开户凭证
		if (account.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT && subAcct.getCorrelation_voch_ind() == E_YESORNO.YES) {
/*
			IoCmCustVochStatusMntIn cplVochMntIn = BizUtil.getInstance(IoCmCustVochStatusMntIn.class);

			cplVochMntIn.setAcct_no(account.getAcct_no());
			cplVochMntIn.setCust_voch_oper_type(E_CUSTVOCHOPERTYPE.CANCLE);
			cplVochMntIn.setSub_acct_seq(subAcct.getSub_acct_seq());
			cplVochMntIn.setCcy_code(subAcct.getCcy_code());
			cplVochMntIn.setVoch_asso_obj_type(subAcct.getDd_td_ind() == E_DEMANDORTIME.DEMAND ? E_VOCHASSOBJTYPE.DEMA_SUB_ACCT : E_VOCHASSOBJTYPE.FIX_SUB_ACCT);
			cplVochMntIn.setTrxn_remark(trxnRemark);

		    SysUtil.getRemoteInstance(SrvIoCmVoucher.class).mntCustVochStatus(cplVochMntIn);
			
*/
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：销户前支票检查</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息:必输
	 * @param trxnRemark
	 *            交易备注
	 */
	public static void checkChequeBeforeClose(DpaSubAccount subAcct) {

		if (subAcct.getUse_cheque_allow_ind() != E_ALLOW.ALLOW) {
			return;
		}
/*
		// 检查是否有支票处于申请状态
		IoStChequeBookApplyListIn qryStatusInput = BizUtil.getInstance(IoStChequeBookApplyListIn.class);
		
		qryStatusInput.setAcct_no(subAcct.getAcct_no()); 
		qryStatusInput.setCcy_code(subAcct.getCcy_code()); 
		
		long num = SysUtil.getRemoteInstance(SrvIoStCheque.class).qryChequeBookApplyStatus(qryStatusInput).size()

		// 存在申请中的支票
		if (num > 0) {

			DpErr.Dp.E0042(subAcct.getAcct_no(), subAcct.getSub_acct_seq());
		}
		else {
			// 申请通过的支票，提示先去做支票停用
			SrvIoDpSettleVoucher settleVoucher = BizUtil.getInstance(SrvIoDpSettleVoucher.class);
			IoDpSettleVoucherNoQryIn querySettleInput = BizUtil.getInstance(IoDpSettleVoucherNoQryIn.class);

			querySettleInput.setAcct_no(subAcct.getAcct_no());
			querySettleInput.setAcct_type(subAcct.getAcct_type());
			querySettleInput.setCcy_code(subAcct.getCcy_code());
			querySettleInput.setProd_id(subAcct.getProd_id());
			querySettleInput.setSettle_voch_type(E_SETTLEVOCHTYPE.CHEQUE);
			querySettleInput.setUsed_ind(E_YESORNO.NO);
			querySettleInput.setStop_use_ind(E_YESORNO.NO);

			IoDpSettleVoucherNoQryOut querySettleOut = settleVoucher.querySettleVoucherNo(querySettleInput);

			if (querySettleOut.getList01().size() > 0) {

				DpErr.Dp.E0043(subAcct.getAcct_no());
			}
		}
*/
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：客户凭证更换</li>
	 *         </p>
	 * @param cplIn
	 *            客户凭证更换输入
	 */
	public static void changeVoucher(DpVoucherChangeIn cplIn) {

/*
		IoCmCustVochReplaceIn cplVochIn = BizUtil.getInstance(IoCmCustVochReplaceIn.class);

		cplVochIn.setAcct_no(cplIn.getAcct_no());
		cplVochIn.setCcy_code(cplIn.getCcy_code());
		cplVochIn.setChg_passbook_reason(cplIn.getChg_passbook_reason());
		cplVochIn.setNew_voch_no(cplIn.getNew_voch_no());
		cplVochIn.setNew_voch_type(cplIn.getNew_voch_type());
		cplVochIn.setSub_acct_seq(cplIn.getSub_acct_seq());
		cplVochIn.setSummary_code(cplIn.getSummary_code);
		cplVochIn.setVoch_asso_obj_type(cplIn.getRef_voch_level() == E_VOCHREFLEVEL.ACCT ? E_VOCHASSOBJTYPE.ACCT : E_VOCHASSOBJTYPE.FIX_SUB_ACCT);

   	   
	    SysUtil.getRemoteInstance(SrvIoCmVoucher.class).changeCustVoucher(cplVochIn);
		
*/
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：凭证参数信息查询</li>
	 *         </p>
	 * @param sVochType
	 *            凭证类型
	 * @return 凭证参数信息
	 */
	public static DpVoucherParmInfo getVoucherParmInfo(String sVochType) {

		// 凭证参数信息
//		IoCmVoucherParmInfo vochParm = SysUtil.getRemoteInstance(SrvIoCmVoucher.class).qryVoucherParmInfo(sVochType);
   	  
		DpVoucherParmInfo cplOut = BizUtil.getInstance(DpVoucherParmInfo.class);

//		cplOut.setVoch_prop(vochParm.getVoch_prop);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：可使用凭证存在判断</li>
	 *         </p>
	 * @param sVochType
	 *            凭证类型
	 * @param sVochNo
	 *            凭证号
	 * @return 凭证参数信息
	 */
	public static void existsUsableVouchers(String sVochType, String sVochNo) {
/*
		IoCmQryVochStockIn cplNewVochIn = BizUtil.getInstance(IoCmQryVochStockIn.class);

		cplNewVochIn.setBranch_id(BizUtil.getTrxRunEnvs().getTrxn_branch());
		cplNewVochIn.setTeller_id(BizUtil.getTrxRunEnvs().getTrxn_teller());
		cplNewVochIn.setTill_id(null);
		cplNewVochIn.setTill_type(E_TILLTYPE.GENERAL);
		cplNewVochIn.setVoch_no(sVochNo);
		cplNewVochIn.setVoch_stock_type(E_VOCHSTOCKTYPE.USE);
		cplNewVochIn.setVoch_type(sVochType);

		// 凭证库存查询
		SysUtil.getRemoteInstance(SrvIoCmVoucher.class).qryVoucherStock(cplNewVochIn);	
*/
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：检查凭证状态</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 */
	public static void checkVouchersStatus(DpaSubAccount subAccount, DpaAccount account) {
/*
		// 关联凭证标志
		boolean correlationVochInd = false;
		Options<IoCmQryCustVochOut> custVochInfo = new DefaultOptions<IoCmQryCustVochOut>();

		IoCmQryCustVochIn qryVochIn = BizUtil.getInstance(IoCmQryCustVochIn.class);

		// 开户凭证检查处理，仅在客户账号有关联开户凭证时检查
		if (account.getCorrelation_voch_ind() == E_YESORNO.YES && account.getRef_voch_level() == E_VOCHREFLEVEL.ACCT) {

			qryVochIn.setAcct_no(account.getAcct_no());
			qryVochIn.setVoch_asso_obj_type(E_VOCHASSOBJTYPE.ACCT);
			qryVochIn.setCust_voch_status(E_CUSTVOCHSTAS.NORMAL);

			custVochInfo = SysUtil.getRemoteInstance(SrvIoCmVoucher.class).qryCustVoucherInfo(qryVochIn);
			
			correlationVochInd = true;
		}
		else if (subAccount.getCorrelation_voch_ind() == E_YESORNO.YES && account.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT) {

			qryVochIn.setAcct_no(subAccount.getAcct_no());
			qryVochIn.setCcy_code(subAccount.getCcy_code());
			qryVochIn.setSub_acct_seq(subAccount.getSub_acct_seq());
			qryVochIn.setVoch_asso_obj_type(subAccount.getDd_td_ind() == E_DEMANDORTIME.DEMAND ? E_VOCHASSOBJTYPE.DEMA_SUB_ACCT : E_VOCHASSOBJTYPE.FIX_SUB_ACCT);
			qryVochIn.setCust_voch_status(E_CUSTVOCHSTAS.NORMAL);

			custVochInfo = SysUtil.getRemoteInstance(SrvIoCmVoucher.class).qryCustVoucherInfo(qryVochIn);
			
			correlationVochInd = true;
		}

		// 账户或子账户有关联凭证,但未查询到凭证信息时报错
		if (custVochInfo.size() == 0 && correlationVochInd) {

			// 账号凭证状态异常
			throw DpErr.Dp.E0135(qryVochIn.getAcct_no());
		}
*/
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：检查凭证状态</li>
	 *         </p>
	 * @param subAccount
	 *            子账户信息
	 * @param account
	 *            账户信息
	 */
	public static DpCustVoucherInfo getCustVouchersInfo(DpaSubAccount subAccount, DpaAccount account) {
/*
		// 凭证信息查询输入实例化
		IoCmQryCustVochIn qryVochIn = BizUtil.getInstance(IoCmQryCustVochIn.class);

		// 凭证信息查询输出
		Options<IoCmQryCustVochOut> qryVochOut = new DefaultOptions<IoCmQryCustVochOut>();

		if (account.getRef_voch_level() == E_VOCHREFLEVEL.SUBACCT && subAccount.getCorrelation_voch_ind() == E_YESORNO.YES) {

			qryVochIn.setAcct_no(subAccount.getAcct_no()); // 账号
			// qryVochIn.setCust_voch_status(E_CUSTVOCHSTAS.NORMAL); // 客户凭证状态
			qryVochIn.setCcy_code(subAccount.getCcy_code()); // 币种
			qryVochIn.setSub_acct_seq(subAccount.getSub_acct_seq()); // 子账户序号
			qryVochIn.setVoch_asso_obj_type(subAccount.getDd_td_ind() == E_DEMANDORTIME.DEMAND ? E_VOCHASSOBJTYPE.DEMA_SUB_ACCT : E_VOCHASSOBJTYPE.FIX_SUB_ACCT);
			
			// 调用公共查询凭证信息	
			qryVochOut = SysUtil.getRemoteInstance(SrvIoCmVoucher.class).qryCustVoucherInfo(qryVochIn);
		}
		else if (account.getRef_voch_level() == E_VOCHREFLEVEL.ACCT && account.getCorrelation_voch_ind() == E_YESORNO.YES) {

			qryVochIn.setAcct_no(account.getAcct_no()); // 账号
			// qryVochIn.setCust_voch_status(E_CUSTVOCHSTAS.NORMAL); // 客户凭证状态
			qryVochIn.setVoch_asso_obj_type(E_VOCHASSOBJTYPE.ACCT);

			// 调用公共查询凭证信息
			qryVochOut = SysUtil.getRemoteInstance(SrvIoCmVoucher.class).qryCustVoucherInfo(qryVochIn);
		}
		
		// 返回凭证信息
		if(qryVochOut.size() > 0){
			
			DpCustVoucherInfo cplCustInfo = BizUtil.getInstance(DpCustVoucherInfo.class);
			
			cplCustInfo.setVoch_type(qryVochOut.get(0).getVoch_type());
			cplCustInfo.setVoch_no(qryVochOut.get(0).getVoch_no());

			return  cplCustInfo;
		}
*/
		
		return null;	
	}
}