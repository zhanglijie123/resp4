package cn.sunline.icore.dp.serv.common;

import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbDataCollection;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbDataCollectionDao;
import cn.sunline.icore.dp.serv.type.ComDpMidPlat.DpDataCollectIn;
import cn.sunline.icore.sys.type.EnumType.E_FILEDETAILDEALSTATUS;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpDaseAcctReg {
	
	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDaseAcctReg.class);

	/**
	 * @Author JSS
	 *         <p>
	 *         <li>2018年12月17日-下午16：15</li>
	 *         <li>功能说明：登记账户数据采集登记簿</li>
	 *         </p>
	 * @param DpDataCollectIn
	 *        
	 */
	public static void dataCollectReg(DpDataCollectIn dataInput) {
		
		bizlog.method(" DpDaseAcctReg.dataCollectReg begin >>>>>>>>>>>>>>>>");
		
		String request = ""; // 请求报文


		DpbDataCollection dataCollect = BizUtil.getInstance(DpbDataCollection.class);
		
		dataCollect.setTrxn_seq(BizUtil.getTrxRunEnvs().getTrxn_seq()); //交易流水号
		dataCollect.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date()); //交易日期
		dataCollect.setInitiator_seq(BizUtil.getTrxRunEnvs().getInitiator_seq()); //调用流水号
		dataCollect.setTrxn_channel(BizUtil.getTrxRunEnvs().getChannel_id()); //渠道
		
		//序列化成字符串
		request = SysUtil.serialize(dataInput);
		
		dataCollect.setRequest(request); //请求报文
		dataCollect.setFile_detail_handling_status(E_FILEDETAILDEALSTATUS.WAIT); //文件明细处理状态
		dataCollect.setBatch_no(""); //批次号

		DpbDataCollectionDao.insert(dataCollect);

		bizlog.method(" DpDaseAcctReg.dataCollectReg end >>>>>>>>>>>>>>>>");

	}
}
