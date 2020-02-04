package cn.sunline.icore.dp.serv.account.close;

import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelateDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccountDao;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpClearCloseAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseAccountOut;
import cn.sunline.icore.dp.serv.type.ComDpCloseAccout.DpCloseSubAccountIn;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_ACCTBUSITYPE;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpCloseAccount {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCloseAccount.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月11日-下午3:56:50</li>
	 *         <li>功能说明：销户服务主处理程序</li>
	 *         </p>
	 * @param cplIn
	 * @return DpCloseAccountOut
	 */
	public static DpCloseAccountOut doMain(DpCloseAccountIn cplIn) {

		bizlog.method(" DpCloseAccount.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		DpaAccount account = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 销户主调检查
		DpCloseAccountCheck.checkMainMethod(cplIn, account);

		// 属性到期自动刷新
		DpAttrRefresh.refreshAttrValue(account, cplIn.getAcct_no(), E_YESORNO.YES);

		// 销户处理
		doMethod(cplIn, account);

		// 输出接口赋值
		DpCloseAccountOut cplOut = BizUtil.getInstance(DpCloseAccountOut.class);

		cplOut.setCard_no(CommUtil.equals(cplIn.getAcct_no(), account.getAcct_no()) ? null : cplIn.getAcct_no());
		cplOut.setAcct_no(cplIn.getAcct_no());
		cplOut.setAcct_type(cplIn.getAcct_type());
		cplOut.setAcct_name(cplIn.getAcct_name());
		cplOut.setAcct_status(E_ACCTSTATUS.CLOSE);

		bizlog.debug("<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpCloseAccount.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年2月11日-下午3:56:50</li>
	 *         <li>功能说明：销户服务主处理程序</li>
	 *         </p>
	 * @param cplIn
	 *            账户输入信息
	 * @param account
	 *            账户信息
	 */
	public static void doMethod(DpCloseAccountIn cplIn, DpaAccount account) {

		// 1. 关闭台账
		DpBaseServiceApi.closeAccount(cplIn, account);

		// 2. 关闭开户凭证
		if (account.getRef_voch_level() == E_VOCHREFLEVEL.ACCT && account.getCorrelation_voch_ind() == E_YESORNO.YES) {

			DpVoucherIobus.modifyCustVoucherStatus(account, null, cplIn.getRemark());
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年8月6日-下午3:56:50</li>
	 *         <li>功能说明：活期账户销户</li>
	 *         </p>
	 * @param cplIn
	 *            账户输入信息
	 */
	public static void CloseCurrentAccount(DpClearCloseAccountIn cplIn) {

		bizlog.method(" DpCloseAccount.CloseCurrentAccount begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 账户定位
		DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

		// 活期子户集
		List<DpaAccountRelate> listAcctRelate = new ArrayList<DpaAccountRelate>();

		if (CommUtil.isNull(cplIn.getCcy_code())) {

			// 判断是否存在定期子户
			DpaAccountRelate acctRelate = DpaAccountRelateDao.selectFirst_odb7(acctInfo.getAcct_no(), E_DEMANDORTIME.TIME, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT, false);

			if (acctRelate != null) {

				throw DpBase.E0413(acctRelate.getAcct_no());
			}

			listAcctRelate = DpaAccountRelateDao.selectAll_odb7(acctInfo.getAcct_no(), E_DEMANDORTIME.DEMAND, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT, false);
		}
		else {

			// 判断是否存在定期子户
			DpaAccountRelate acctRelate = DpaAccountRelateDao.selectFirst_odb6(acctInfo.getAcct_no(), cplIn.getCcy_code(), E_DEMANDORTIME.TIME, E_ACCTSTATUS.NORMAL,
					E_ACCTBUSITYPE.DEPOSIT, false);

			if (acctRelate != null) {

				throw DpBase.E0413(acctRelate.getAcct_no());
			}

			// 查询指定币种子户
			listAcctRelate = DpaAccountRelateDao.selectAll_odb6(acctInfo.getAcct_no(), cplIn.getCcy_code(), E_DEMANDORTIME.DEMAND, E_ACCTSTATUS.NORMAL, E_ACCTBUSITYPE.DEPOSIT,
					false);
		}

		// 子账户先排序再销户
		BizUtil.listSort(listAcctRelate, true, SysDict.A.default_ind.getId(), SysDict.A.sub_acct_seq.getId());

		// 循环销子户
		for (DpaAccountRelate cplSubAct : listAcctRelate) {

			// 剔除产品不符合要求的活期子户
			if (CommUtil.isNotNull(cplIn.getProd_id()) && !CommUtil.equals(cplIn.getProd_id(), cplSubAct.getProd_id())) {
				continue;
			}

			DpCloseSubAccountIn cplCloseSubIn = BizUtil.getInstance(DpCloseSubAccountIn.class);

			cplCloseSubIn.setAcct_no(acctInfo.getAcct_no());
			cplCloseSubIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
			cplCloseSubIn.setCcy_code(cplSubAct.getCcy_code());
			cplCloseSubIn.setClose_acct_reason(cplIn.getClose_acct_reason());
			cplCloseSubIn.setProd_id(cplSubAct.getProd_id());
			cplCloseSubIn.setSub_acct_seq(cplSubAct.getSub_acct_seq());
			cplCloseSubIn.setSame_close_card_ind(cplIn.getSame_close_card_ind());

			// 获取子账户信息
			DpaSubAccount subAccount = DpaSubAccountDao.selectOneWithLock_odb1(cplSubAct.getAcct_no(), cplSubAct.getSub_acct_no(), false);

			// 3.销子户主调检查
			DpCloseSubAccountCheck.checkMainMethod(cplCloseSubIn, acctInfo, subAccount);

			// 4.销子户主调方法
			DpCloseSubAccount.doMethod(cplCloseSubIn, subAccount, acctInfo);
		}

		bizlog.method(" DpCloseAccount.CloseCurrentAccount end <<<<<<<<<<<<<<<<");
	}
}
