package cn.sunline.icore.dp.serv.callback;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import cn.sunline.icore.ap.spi.ITransLogger;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.ltts.base.RequestData;
import cn.sunline.ltts.base.ResponseData;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：
 * </p>
 * 
 * @Author Liubx
 *         <p>
 *         <li>2018年6月26日-下午14:35:50</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>2018年6月28日-Liubx：存款模块扩展点</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpITransLoggerImpl implements ITransLogger {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpITransLoggerImpl.class);

	@Override
	@SuppressWarnings("unchecked")
	public void logTransInfo(String pckgsq, String pckgdt, RequestData request, ResponseData response, Date beginTime, Throwable cause, boolean autonomous) {

/*		RunEnvs runEnvs = SysUtil.getTrxRunEnvs();
		Map<String, Object> inputMap = (Map<String, Object>) request.getRequestContext().getTrxInput();// 输入数据
		Map<String, Object> propertyMap = (Map<String, Object>) request.getRequestContext().getTrxProperty();// 客户数据
		Map<String, Object> outMap = (Map<String, Object>) request.getRequestContext().getTrxOutput();// 卡数据

		bizlog.debug("trxn code [%s] ,channel id [%s] , system type [%s]", runEnvs.getTrxn_code(), BizUtil.getTrxRunEnvs().getChannel_id(), SysUtil.getCurrentSystemType());

		bizlog.debug("inputMap [%s]", inputMap);
		bizlog.debug("propertyMap [%s]", propertyMap);
		bizlog.debug("outMap [%s]", outMap);

		// 必须满足条件才登记反欺诈： 转账、联机、app&visa
		if (ApBusinessParmApi.getValue("REG_APS_FRAUD_TRXN_CODE").contains(runEnvs.getTrxn_code()) && SystemType.onl == SysUtil.getCurrentSystemType()
				&& ApBusinessParmApi.getValue("REG_APS_FRAUD_CHANNEL").contains(runEnvs.getChannel_id())) {

			// 登记反欺诈
			regFraudMonitor(response, runEnvs, inputMap, propertyMap);
		}
*/
	}
	
	/**
	 * @Author Administrator
	 *         <p>
	 *         <li>2018年10月9日-下午4:09:52</li>
	 *         <li>功能说明：登记反欺诈</li>
	 *         </p>
	 * @param response
	 * @param runEnvs
	 * @param inputMap
	 * @param propertyMap
	 */
	private void regFraudMonitor(ResponseData response, RunEnvs runEnvs, Map<String, Object> inputMap, Map<String, Object> propertyMap) {
/*
		final AFraudRegIn fraudMonitorIn = BizUtil.getInstance(AFraudRegIn.class);

		fraudMonitorIn.setTrxn_seq(runEnvs.getTrxn_seq());
		fraudMonitorIn.setTrxn_code(runEnvs.getTrxn_code());
		fraudMonitorIn.setTrxn_desc(runEnvs.getTrxn_desc());
		fraudMonitorIn.setTrxn_class(E_TRXNCLASS.NORMAL);
		fraudMonitorIn.setReturn_code(response.getDataContext().getSystem().getString("erorcd"));
		fraudMonitorIn.setError_text(response.getDataContext().getSystem().getString("erortx"));

		fraudMonitorIn.setTrxn_ccy(parseString(inputMap.get(SysDict.A.trxn_ccy.getId())));
		fraudMonitorIn.setTrxn_amt(parseDecimal(inputMap.get(SysDict.A.trxn_amt.getId())));

		fraudMonitorIn.setCash_trxn_ind(CommUtil.toEnum(E_CASHTRXN.class, propertyMap.get(SysDict.A.cash_trxn_ind.getId())));
		E_ACCOUTANALY debitAcct_analy = CommUtil.toEnum(E_ACCOUTANALY.class, propertyMap.get(SysDict.A.debit_acct_analy.getId()));
		E_ACCOUTANALY creditAnaly = CommUtil.toEnum(E_ACCOUTANALY.class, propertyMap.get(SysDict.A.credit_acct_analy.getId()));

		// 活期转账 借贷双方必有一方是存款账号
		if (debitAcct_analy == E_ACCOUTANALY.DEPOSIT) {

			String debitAcctNo = parseString(inputMap.get(SysDict.A.debit_acct_no.getId()));

			DpaAccount debitAcct = DpToolsApi.locateSingleAccount(debitAcctNo, null, false);

			if (debitAcct.getCard_relationship_ind() == E_YESORNO.YES) {

				List<DpaCardAccount> cardAcct = DpaCardAccountDao.selectAll_odb3(debitAcct.getAcct_no(), false);

				if (!cardAcct.isEmpty()) {

					fraudMonitorIn.setCard_no(cardAcct.get(0).getCard_no());
				}
			}

			fraudMonitorIn.setCust_no(debitAcct.getCust_no());
			fraudMonitorIn.setCust_name(debitAcct.getAcct_name());

			fraudMonitorIn.setAcct_no(debitAcct.getAcct_no());
			fraudMonitorIn.setAcct_name(debitAcct.getAcct_name());

			fraudMonitorIn.setOpp_acct_no(parseString(inputMap.get(SysDict.A.credit_acct_no.getId())));
			fraudMonitorIn.setOpp_acct_name(parseString(propertyMap.get(DpDict.A.in_acct_name.getId())));
			fraudMonitorIn.setDebit_credit(E_DEBITCREDIT.DEBIT);

		}
		else if (creditAnaly == E_ACCOUTANALY.DEPOSIT) {

			String creditAcctNo = parseString(inputMap.get(SysDict.A.credit_acct_no.getId()));

			DpaAccount creditAcct = DpToolsApi.locateSingleAccount(creditAcctNo, null, false);

			if (creditAcct.getCard_relationship_ind() == E_YESORNO.YES) {

				List<DpaCardAccount> cardAcct = DpaCardAccountDao.selectAll_odb3(creditAcct.getAcct_no(), false);

				if (!cardAcct.isEmpty()) {

					fraudMonitorIn.setCard_no(cardAcct.get(0).getCard_no());
				}
			}

			fraudMonitorIn.setCust_no(creditAcct.getCust_no());
			fraudMonitorIn.setCust_name(creditAcct.getAcct_name());

			fraudMonitorIn.setAcct_no(creditAcct.getAcct_no());
			fraudMonitorIn.setAcct_name(creditAcct.getAcct_name());

			fraudMonitorIn.setOpp_acct_no(parseString(inputMap.get(SysDict.A.debit_acct_no.getId())));
			fraudMonitorIn.setOpp_acct_name(parseString(propertyMap.get(DpDict.A.out_acct_name.getId())));

			fraudMonitorIn.setDebit_credit(E_DEBITCREDIT.CREDIT);
		}

		fraudMonitorIn.setRemark(parseString(inputMap.get(SysDict.A.remark.getId())));
		fraudMonitorIn.setTrxn_catalog(fraudMonitorIn.getDebit_credit() == E_DEBITCREDIT.DEBIT ? E_TRXNCATALOG.RETAIL : E_TRXNCATALOG.PAYMENT);

		fraudMonitorIn.setSend_ind(E_YESORNO.NO);
		DaoUtil.executeInNewTransation(new RunnableWithReturn<Void>() {
			@Override
			public Void execute() {
				SrvIoAf fraudMonitor = BizUtil.getInstance(SrvIoAf.class);
				fraudMonitor.register(fraudMonitorIn);
				return null;
			}
		});
*/
	}

	/**
	 * 判断对象是否为空，为null返回"" 不为null，返回toString
	 * 
	 * @param obj
	 * @return
	 */
	private String parseString(Object obj) {
		if (CommUtil.isNull(obj)) {
			return "";
		}
		return obj.toString();
	}

	/**
	 * 返回 BigDecimal对象，不能解析返回0
	 * 
	 * @return
	 */
	private BigDecimal parseDecimal(Object obj) {
		try {
			return new BigDecimal(obj.toString());
		}
		catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}
}
