package cn.sunline.icore.dp.serv.iobus;

import java.math.BigDecimal;

import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.cm.sys.type.CmSysEnumType.E_CHRGSTATUS;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpChargeAccountingInfo;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_DEPTTRXNEVENT;
import cn.sunline.icore.iobus.cm.servicetype.SrvIoCmChrg;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgAutoOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgCalcOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgManualIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgManualOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgOweQryIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgOweQryOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrgBase.IoCmChrgCalcIn;
import cn.sunline.icore.sys.parm.TrxEnvs.AutoChrgInfo;
import cn.sunline.icore.sys.type.EnumType.E_CHRGASSOOBJTYPE;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpChargeIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpChargeIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：自动收费试算</li>
	 *         </p>
	 * @param subAcct
	 *            子账户信息
	 * @param trxnEvent
	 *            交易事件
	 * @param trxnAmt
	 *            交易金额
	 */
	public static void calcAutoChrg(DpaSubAccount subAcct, E_DEPTTRXNEVENT trxnEvent, BigDecimal trxnAmt) {

		IoCmChrgCalcIn chrgAutoCalcIn = BizUtil.getInstance(IoCmChrgCalcIn.class);

		chrgAutoCalcIn.setTrxn_event_id(trxnEvent.getValue());
		chrgAutoCalcIn.setCust_no(subAcct.getCust_no());
		chrgAutoCalcIn.setAcct_no(subAcct.getAcct_no());
		chrgAutoCalcIn.setTrxn_ccy(subAcct.getCcy_code());
		chrgAutoCalcIn.setSub_acct_seq(subAcct.getSub_acct_seq());
		chrgAutoCalcIn.setTrxn_amt(trxnAmt);
		chrgAutoCalcIn.setChrg_asso_obj_type(E_CHRGASSOOBJTYPE.ACCT);
		chrgAutoCalcIn.setSummary_code(ApSystemParmApi.getSummaryCode("CHARGE"));

		SysUtil.getRemoteInstance(SrvIoCmChrg.class).calcAutoChrg(chrgAutoCalcIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：自动收费处理</li>
	 *         </p>
	 * @param autoChrgIn
	 *            自动收费信息
	 */
	public static DpChargeAccountingInfo prcAutoChrg(AutoChrgInfo autoChrgIn) {

		IoCmChrgAutoOut cmChrgOut = SysUtil.getRemoteInstance(SrvIoCmChrg.class).prcAutoChrgAccounting(autoChrgIn);

		cmChrgOut = SysUtil.getRemoteInstance(SrvIoCmChrg.class).prcAutoChrgAccounting(autoChrgIn);

		if (CommUtil.isNull(cmChrgOut)) {
			return null;
		}		if (CommUtil.isNull(cmChrgOut.getDeduct_chrg_total_amt())) {
			return null;
		}

		DpChargeAccountingInfo cplOut = BizUtil.getInstance(DpChargeAccountingInfo.class);

		cplOut.setDeduct_chrg_total_amt(cmChrgOut.getDeduct_chrg_total_amt());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：手工收费计算</li>
	 *         </p>
	 * @param cplIn
	 *            手工收费输入
	 * @return 收费信息
	 */
	public static IoCmChrgCalcOut calcManualChrg(IoCmChrgCalcIn cplIn) {

		return SysUtil.getRemoteInstance(SrvIoCmChrg.class).calcManualChrg(cplIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：手工收费处理</li>
	 *         </p>
	 * @param cplIn
	 *            手工收费输入
	 * @return 收费信息
	 */
	public static IoCmChrgManualOut prcManualChrgAccounting(IoCmChrgManualIn cplIn) {

		return SysUtil.getRemoteInstance(SrvIoCmChrg.class).prcManualChrgAccounting(cplIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：判断是否欠费</li>
	 *         </p>
	 * @param acctNo
	 *            账号
	 * @return
	 */
	public static boolean isChrgArrears(String acctNo) {

		// 检查是否有欠费
		IoCmChrgOweQryIn qryChrgCodeIn = BizUtil.getInstance(IoCmChrgOweQryIn.class);

		qryChrgCodeIn.setAcct_no(acctNo);
		qryChrgCodeIn.setChrg_status(E_CHRGSTATUS.ARREARS);

		IoCmChrgOweQryOut oweQryOut = SysUtil.getRemoteInstance(SrvIoCmChrg.class).qryChrgOweList(qryChrgCodeIn);

		if (CommUtil.isNotNull(oweQryOut) && oweQryOut.getList01().size() > 0) {
			return true;
		}
		else {
			return false;
		}
	}
}