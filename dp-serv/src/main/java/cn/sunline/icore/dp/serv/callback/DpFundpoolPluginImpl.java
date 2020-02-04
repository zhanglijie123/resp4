package cn.sunline.icore.dp.serv.callback;

import java.math.BigDecimal;

import cn.sunline.icore.dp.base.plugin.DpFundpoolPlugin;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.serv.fundpool.DpPool;

/**
 * <p>
 * 文件功能说明：资金池供底层调用扩展点
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
public class DpFundpoolPluginImpl implements DpFundpoolPlugin {

	/**
	 * 资金池可用余额计算
	 * 
	 * @param subAcct
	 *            子账户信息
	 * @return 可用余额
	 */
	@Override
	@SuppressWarnings("unchecked")
	public BigDecimal getBanlance(DpaSubAccount subAcct) {

		return DpPool.getBanlance(subAcct);
	}
}
