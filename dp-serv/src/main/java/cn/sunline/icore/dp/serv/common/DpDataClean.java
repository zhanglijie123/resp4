package cn.sunline.icore.dp.serv.common;

import cn.sunline.icore.ap.api.AbstractDataClean;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.ap.util.DBUtil;
import cn.sunline.icore.dp.serv.namedsql.batch.SqlDpHistDataCleanDao;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpDataClean {
	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDataClean.class);

	public static class cleanAcctTrxnDetail extends AbstractDataClean {

		@Override
		public void process() {

			String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
			int days = this.getDataReserveDays().intValue();
			final String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();
			final String cleanDate = BizUtil.dateAdd("day", trxnDate, -1 * days);
			final String timeCleanDate = BizUtil.dateAdd("day", trxnDate, -5 * days);
			final long maxDeleteRowNum = 1000L;
			long returnRowNum = maxDeleteRowNum;

			// 返回行数小于指定值则表明已经删除完毕
			while(returnRowNum >= maxDeleteRowNum){
				returnRowNum = SqlDpHistDataCleanDao.delAcctTrxnDetail(cleanDate, timeCleanDate, maxDeleteRowNum, orgId);
				DBUtil.commit();
			}
		}
	};
}