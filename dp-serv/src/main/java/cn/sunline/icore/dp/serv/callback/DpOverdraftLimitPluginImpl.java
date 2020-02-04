package cn.sunline.icore.dp.serv.callback;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.plugin.DpOverdraftLimitPlugin;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.base.type.ComDpOverdraftBasic.DpLimitBalanceInfo;
import cn.sunline.icore.dp.base.type.ComDpOverdraftBasic.DpOverdraftLimitBaseInfo;
import cn.sunline.icore.dp.serv.iobus.DpCreditLimitIobus;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitInfo;
import cn.sunline.icore.dp.serv.type.ComDpUseIobus.DpCreditLimitTrialInfo;

/**
 * <p>
 * 文件功能说明：透支额度IOBUS服务底层调用扩展点
 * </p>
 * 
 * @Author 周明易
 *         <p>
 *         <li>2019年3月29日-下午14:35:50</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>2019年3月29日-周明易：存款模块透支额度相关扩展点</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpOverdraftLimitPluginImpl implements DpOverdraftLimitPlugin {

	/**
	 * 透支额度占用
	 * 
	 * @param subAcct
	 *            子账户信息
	 * @param limitCode
	 *            额度编号
	 * @param occupyAmt
	 *            占用金额
	 * @param forceOverdraftFlag
	 *            强制透支标志
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void overdraftLimitOccupy(DpaSubAccount subAcct, String limitCode, BigDecimal occupyAmt, E_YESORNO forceOverdraftFlag) {

/*
		// 组织额度占用数据
		IoClUseIn ioOccupyLimitIn = BizUtil.getInstance(IoClUseIn.class);

		ioOccupyLimitIn.setLimit_code(limitCode);// 额度编号
		ioOccupyLimitIn.setTrxn_amt(occupyAmt);// 占用金额
		ioOccupyLimitIn.setTrxn_ccy(subAcct.getCcy_code());// 交易币种
		ioOccupyLimitIn.setTrxn_event_id(E_DEPTTRXNEVENT.DP_DRAW.getValue());// 交易事件id
		ioOccupyLimitIn.setBusi_body(subAcct.getSub_acct_no());// 业务主体
		ioOccupyLimitIn.setForce_draw_ind(forceOverdraftFlag);// 强制透支

		DpaAccount account = DpaAccountDao.selectOne_odb1(subAcct.getAcct_no(), true);

		if (account.getJoint_acct_ind() == E_YESORNO.NO) {

			ioOccupyLimitIn.setCust_no(account.getCust_no());// 客户号

		}
		else {

			// 查询联名客户信息
			List<DpbJointAccount> jointAcctList = DpbJointAccountDao.selectAll_odb2(account.getAcct_no(), false);

			DefaultOptions<IoClJointCustomer> defaultOptions = new DefaultOptions<>();

			for (DpbJointAccount joinAcct : jointAcctList) {

				IoClJointCustomer jointCustomer = BizUtil.getInstance(IoClJointCustomer.class);

				jointCustomer.setCust_no(joinAcct.getCust_no());

				defaultOptions.add(jointCustomer);
			}
			ioOccupyLimitIn.setJoint_customers(defaultOptions);
		}

		// 调用额度占用
		SysUtil.getRemoteInstance(SrvIoClLimitMgt.class).limitOccupy(ioOccupyLimitIn);
*/
	}

	/**
	 * 透支额度释放
	 * 
	 * @param subAcct
	 *            子账户信息
	 * @param limitCode
	 *            额度编号
	 * @param releaseAmt
	 *            释放金额
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void overdraftLimitRelease(DpaSubAccount subAcct, String limitCode, BigDecimal releaseAmt) {

/*		
		// 组织额度释放数据
		IoClUseIn ioOccupyLimitIn = BizUtil.getInstance(IoClUseIn.class);

		ioOccupyLimitIn.setLimit_code(limitCode);// 额度编号
		ioOccupyLimitIn.setTrxn_amt(releaseAmt);// 释放额度
		ioOccupyLimitIn.setTrxn_ccy(subAcct.getCcy_code());// 交易币种
		ioOccupyLimitIn.setTrxn_event_id(E_DEPTTRXNEVENT.DP_SAVE.getValue());// 交易事件id
		ioOccupyLimitIn.setCust_no(subAcct.getCust_no());// 客户号
		ioOccupyLimitIn.setBusi_body(subAcct.getSub_acct_no());// 业务主体

		// 调用释放占用
		SysUtil.getRemoteInstance(SrvIoClLimitMgt.class).limitRelease(ioOccupyLimitIn);	
*/
	}

	/**
	 * 透支额度信息查询
	 * 
	 * @param limitNo
	 *            额度编号
	 * @return 透支额度基本信息
	 */
	@Override
	@SuppressWarnings("unchecked")
	public DpOverdraftLimitBaseInfo overdraftLimitInquery(String limitNo) {

		DpCreditLimitInfo clAccountInfo = DpCreditLimitIobus.getCreditLimitInfo(limitNo);
		
		DpOverdraftLimitBaseInfo cplOut = BizUtil.getInstance(DpOverdraftLimitBaseInfo.class);

		cplOut.setLimit_code(limitNo);
		cplOut.setStatus(clAccountInfo.getStatus());
		cplOut.setDue_date(clAccountInfo.getDue_date());

		return cplOut;
	}

	/**
	 * 透支额度余额信息计算
	 * 
	 * @param subAcct
	 *            子账户信息
	 * @param limitNo
	 *            额度编号
	 * @return 透支额度余额信息
	 */
	@Override
	@SuppressWarnings("unchecked")
	public DpLimitBalanceInfo overdraftLimitBalTrial(DpaSubAccount subAcct, String limitNo) {

		DpCreditLimitTrialInfo cplLimitQryOut = DpCreditLimitIobus.getCreditLimitTrialInfo(limitNo, subAcct.getCcy_code());
		
		DpLimitBalanceInfo cplOut = BizUtil.getInstance(DpLimitBalanceInfo.class);

		cplOut.setLimit_code(limitNo);
		cplOut.setLimit_bal(cplLimitQryOut.getLimit_bal());
		cplOut.setAvailable_overdraw_amount(cplLimitQryOut.getAvailable_overdraw_amount());

		return cplOut;
	}
}
