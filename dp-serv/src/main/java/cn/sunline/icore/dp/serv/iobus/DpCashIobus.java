package cn.sunline.icore.dp.serv.iobus;

import java.math.BigDecimal;

import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCashAccountingIn;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCashAccountingOut;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCashChangeOut;
import cn.sunline.icore.sys.type.EnumType.E_LOSSTYPE;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpCashIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCashIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：现金记账</li>
	 *         </p>
	 * @param cplIn
	 *            现金记账输入
	 */
	public static DpCashAccountingOut cashAccounting(DpCashAccountingIn cplIn) {

		// 记账输入数据
		/*
		 * IoCmCashAccountingIn cashAccountingIn =
		 * BizUtil.getInstance(IoCmCashAccountingIn.class);
		 * 
		 * cashAccountingIn.setCash_use_code(null); // 现金用途码
		 * cashAccountingIn.setCcy_code(cplIn.getCcy_code());
		 * cashAccountingIn.setTrxn_amt(cplIn.getTrxn_amt()); // 使用实际支取金额
		 * cashAccountingIn.setDebit_credit(cplIn.getDebit_credit()); // 借贷方向
		 * cashAccountingIn.setSummary_code(cplIn.getSummary_code());
		 * cashAccountingIn.setTrxn_remark(cplIn.getTrxn_remark());
		 * cashAccountingIn.setReversal_type(null);
		 * cashAccountingIn.setClear_accounts_ind(null); // 抹账标志
		 * 
		 * // 真实对手方 cashAccountingIn.setOpp_acct_ccy(cplIn.getOpp_acct_ccy());
		 * cashAccountingIn.setOpp_acct_no(cplIn.getOpp_acct_no());
		 * cashAccountingIn.setOpp_acct_route(cplIn.getOpp_acct_route());
		 * cashAccountingIn.setOpp_branch_id("");
		 * 
		 * // 调用公共现金记账服务
		 * SysUtil.getRemoteInstance(SrvIoCmCash.class).prcCashAccounting
		 * (cashAccountingIn);
		 */
		DpCashAccountingOut cplOut = BizUtil.getInstance(DpCashAccountingOut.class);

		cplOut.setCcy_code(cplIn.getCcy_code());
		cplOut.setAcct_no("");
		
		//现在不支持做现金交易，如果遇到现金交易则抛出提示信息
		boolean flag = true;
		if (flag){
			throw DpErr.Dp.E0513();
		}

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：现金找零</li>
	 *         </p>
	 * @param trxnCcy
	 *            交易币种
	 * @param trxnAmount
	 *            交易金额
	 * @param changeCcy
	 *            找零币种
	 * @param 找零信息
	 */
	public static DpCashChangeOut changeCash(String trxnCcy, BigDecimal trxnAmount, String changeCcy) {

		// 记账输入数据
		/*
		 * // 调用公共现金找零记账服务
		 * SysUtil.getRemoteInstance(SrvIoCmCash.class).prcCashAccounting
		 * (cashAccountingIn);
		 */
		DpCashChangeOut cplOut = BizUtil.getInstance(DpCashChangeOut.class);

		cplOut.setChange_amt(BigDecimal.ZERO);
		cplOut.setChange_loss_amt(BigDecimal.ZERO);
		cplOut.setChange_loss_type(E_LOSSTYPE.PAYOUT);
		cplOut.setWithdrawl_amt(BigDecimal.ZERO);

		return cplOut;
	}

}