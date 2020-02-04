package cn.sunline.icore.dp.serv.callback;

import cn.sunline.icore.dp.base.plugin.DpOutsideParmPlugin;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;

/**
 * <p>
 * 文件功能说明：外部参数供底层调用扩展点
 * </p>
 * 
 * @Author 周明易
 *         <p>
 *         <li>2019年3月29日-下午14:35:50</li>
 *         </p>
 */
public class DpOutsideParmPluginImpl implements DpOutsideParmPlugin {

	/**
	 * 凭证类型合法性检查
	 * 
	 * @param voucherType
	 *            凭证类型
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void checkVoucherTypeVaild(String voucherType) {

		DpVoucherIobus.getVoucherParmInfo(voucherType);
	}
	
	/**
	 * 收费代码合法性检查
	 * 
	 * @param chrgCode
	 *            收费代码
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void checkChargeCodeVaild(String chrgCode) {

		// TODO: 公共还未提供费用编码合法性判断的服务
	}
}
