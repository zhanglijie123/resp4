package cn.sunline.icore.dp.serv.iobus;

import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtChangeInfo;
import cn.sunline.icore.iobus.cm.servicetype.SrvIoCmInterestRate;
import cn.sunline.icore.iobus.cm.type.ComIoCmInterestRate.IoCmInrtChange;
import cn.sunline.icore.sys.type.EnumType.E_INSTRATEFILEWAY;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

public class DpInterestRateIobus {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpInterestRateIobus.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：检查存期和利率编码匹配性</li>
	 *         </p>
	 * @param inrtCode
	 *            利率代码
	 * @param ccyCode
	 *            币种
	 * @param termCode
	 *            存期
	 * @param inrtFileWay
	 *            利率靠档方式
	 * @return 是否匹配
	 */
	public static void checkTermRateMatch(String inrtCode, String ccyCode, String termCode, E_INSTRATEFILEWAY inrtFileWay) {

		// TODO:

	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年7月30日-下午3:56:50</li>
	 *         <li>功能说明：获取利率参数表有修改的利率信息</li>
	 *         </p>
	 * @param trxnDate
	 *            交易日期
	 * @return 利率变化范围
	 */
	public static Options<DpInrtChangeInfo> getInrtCodeChangeList(String trxnDate) {

		Options<DpInrtChangeInfo> listInrtChange = new DefaultOptions<DpInrtChangeInfo>();

		Options<IoCmInrtChange> listCmInrtChange = SysUtil.getRemoteInstance(SrvIoCmInterestRate.class).qryInrtChangeInfo(trxnDate);

		for (IoCmInrtChange cplCmInrtChange : listCmInrtChange) {

			DpInrtChangeInfo cplInrtChange = BizUtil.getInstance(DpInrtChangeInfo.class);

			cplInrtChange.setInrt_code(cplCmInrtChange.getInrt_code());
			cplInrtChange.setCcy_code(cplCmInrtChange.getCcy_code());
			cplInrtChange.setTerm_code(cplCmInrtChange.getTerm_code());

			listInrtChange.add(cplInrtChange);
		}

		return listInrtChange;
	}
}