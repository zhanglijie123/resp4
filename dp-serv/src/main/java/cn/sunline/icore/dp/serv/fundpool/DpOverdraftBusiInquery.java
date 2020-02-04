package cn.sunline.icore.dp.serv.fundpool;

import java.util.List;

import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraft;
import cn.sunline.icore.dp.base.tables.TabDpOverdraftProduct.DpbOverdraftDao;
import cn.sunline.icore.dp.serv.type.ComDpOverdraft.DpOverdraftAgreeInfo;
import cn.sunline.icore.sys.type.EnumType.E_STATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：透支相关查询
 * </p>
 * 
 * @Author yangdl
 *         <p>
 *         <li>2017年7月8日-上午11:23:06</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年7月8日-yangdl：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */

public class DpOverdraftBusiInquery {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOverdraftBusiInquery.class);

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2018年4月8日-下午2:33:22</li>
	 *         <li>功能说明：通过额度编号查询透支协议信息</li>
	 *         </p>
	 * @param limitCode
	 *            额度编号
	 * @param agreeStatus
	 *            协议状态,不传则查询全部
	 * @return
	 */
	public static Options<DpOverdraftAgreeInfo> qryOverdraftAgreeByLimitCode(String limitCode, E_STATUS agreeStatus) {
		bizlog.method(" DpOverdraftAgree.qryOverdraftAgreeByLimitCode begin >>>>>>>>>>>>>>>>");

		List<DpbOverdraft> dpbOverdraftList = null;
		if (CommUtil.isNotNull(agreeStatus)) {

			dpbOverdraftList = DpbOverdraftDao.selectAll_odb4(limitCode, agreeStatus, false);
		}
		else {

			dpbOverdraftList = DpbOverdraftDao.selectAll_odb6(limitCode, false);
		}

		if (CommUtil.isNull(dpbOverdraftList)) {

			bizlog.method(" DpOverdraftAgree.qryOverdraftAgreeByLimitCode end return null <<<<<<<<<<<<<<<<");
			return null;
		}

		DefaultOptions<DpOverdraftAgreeInfo> options = new DefaultOptions<>();
		for (DpbOverdraft dpbOverdraft : dpbOverdraftList) {
			DpOverdraftAgreeInfo agreeInfo = BizUtil.getInstance(DpOverdraftAgreeInfo.class);

			agreeInfo.setAcct_no(dpbOverdraft.getAcct_no());
			agreeInfo.setAgree_no(dpbOverdraft.getAgree_no());
			agreeInfo.setAgree_status(dpbOverdraft.getAgree_status());
			agreeInfo.setLimit_code(dpbOverdraft.getLimit_code());
			agreeInfo.setEffect_date(dpbOverdraft.getEffect_date());
			agreeInfo.setExpiry_date(dpbOverdraft.getExpiry_date());

			options.add(agreeInfo);
		}

		bizlog.method(" DpOverdraftAgree.qryOverdraftAgreeByLimitCode end <<<<<<<<<<<<<<<<");

		return options;
	}

}
