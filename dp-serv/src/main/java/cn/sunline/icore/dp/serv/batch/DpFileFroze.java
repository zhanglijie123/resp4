package cn.sunline.icore.dp.serv.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApFileApi;
import cn.sunline.icore.ap.api.LocalFileProcessor;
import cn.sunline.icore.ap.batch.ApBatch;
import cn.sunline.icore.ap.batch.ApFileSend;
import cn.sunline.icore.ap.tables.TabApFile.ApbBatchRequest;
import cn.sunline.icore.ap.tables.TabApFile.ApbBatchRequestDao;
import cn.sunline.icore.ap.tables.TabApFile.AppBatch;
import cn.sunline.icore.ap.tables.TabApFile.AppBatchDao;
import cn.sunline.icore.ap.type.ComApFile.ApSetRequestData;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.ap.util.DBUtil;
import cn.sunline.icore.dp.serv.namedsql.batch.SqlDpFileBatchDao;
import cn.sunline.icore.dp.serv.tables.TabDpFileBatch.DpbFileFroze;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpBatchLoadData;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpFileFrozeData;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpFileHeadInfo;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpFileRetHeadInfo;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_FILEDETAILDEALSTATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.dao.CursorHandler;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.dao.Params;

/**
 * <p>
 * 文件功能说明：
 * </p>
 * 
 * @Author yangdl
 *         <p>
 *         <li>2017年3月30日-下午6:28:53</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月30日-yangdl：文件批量冻结解冻</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpFileFroze {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpFileFroze.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月30日-下午5:13:27</li>
	 *         <li>功能说明： 文件冻结解冻导入</li>
	 *         </p>
	 * @param dataItem
	 */
	public static void prcFrozeFileLoad(DpBatchLoadData dataItem) {

		bizlog.method("DpFileFroze.prcFrozeFileLoad start >>>>>>>>>>>>>>");

		// 本地文件名
		String localFileName = ApFileApi.getFileFullPath(dataItem.getFile_local_path(), dataItem.getFile_name());

		// 读取文件列表
		List<String> fileList = ApFileApi.readFile(new File(localFileName));

		// 读取文件为空
		if (fileList.size() == 0) {
			return;
		}

		// 头文件信息
		DpFileHeadInfo headInfo = SysUtil.deserialize(fileList.get(0), DpFileHeadInfo.class);

		// 文件头格式不符
		if (CommUtil.isNull(headInfo.getHead_total_count()) || CommUtil.isNull(headInfo.getHead_total_amt())) {

			// 更新文件请求登记薄文件处理状态
			ApBatch.setFormatErrorByImport(dataItem.getBusi_batch_code());

			return; // 头文件格式不符直接返回
		}

		// 文件体信息
		List<String> fileBody = fileList.subList(1, fileList.size());

		List<DpbFileFroze> listFileFroze = new ArrayList<DpbFileFroze>();

		for (String sJson : fileBody) {

			// 反序列化对象
			DpbFileFroze fileFroze = SysUtil.deserialize(sJson, DpbFileFroze.class);

			fileFroze.setBusi_batch_code(dataItem.getBusi_batch_code());
			// fileFroze.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());
			fileFroze.setFile_detail_handling_status(E_FILEDETAILDEALSTATUS.WAIT);
			fileFroze.setHash_value(BizUtil.getGroupHashValue("REQUEST_HASH_VALUE", dataItem.getFile_id()));

			listFileFroze.add(fileFroze);

			if (listFileFroze.size() == 50) {

				try {
					// 文件明细信息批量插入表
					DaoUtil.insertBatch(DpbFileFroze.class, listFileFroze);
					listFileFroze.clear();
				}
				catch (Exception e) {

					bizlog.error("Froze File Load faile=[%s]",e, e.getMessage());
					
					DBUtil.rollBack();
					// 导入明细表异常、更新状态
					ApBatch.setInsertErrorByImport(dataItem.getBusi_batch_code());
					return;

				}

			}
		}

		// 存有数据
		if (listFileFroze.size() > 0) {
			try {
				// 文件明细信息批量插入表
				DaoUtil.insertBatch(DpbFileFroze.class, listFileFroze);
				listFileFroze.clear();

			}
			catch (Exception e) {

				bizlog.error("Froze File Load faile=[%s]",e, e.getMessage());

				DBUtil.rollBack();
				// 导入明细表异常、更新状态
				ApBatch.setInsertErrorByImport(dataItem.getBusi_batch_code());
				return;
			}

		}

		// 获取冻结解冻明细汇总信息
		DpFileRetHeadInfo retHeadInfo = SqlDpFileBatchDao.selFrozeHeadInfo(BizUtil.getTrxRunEnvs().getBusi_org_id(), dataItem.getBusi_batch_code(), true);

		// 校验头体数据、更新状态
		ApBatch.setStatusByImport(dataItem.getBusi_batch_code(), headInfo.getHead_total_count(), headInfo.getHead_total_amt(), retHeadInfo.getTotal_count(),
				retHeadInfo.getTotal_amt());

		bizlog.method("DpFileFroze.prcFrozeFileLoad end  <<<<<<<<<<<<<<<");

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月30日-下午5:32:13</li>
	 *         <li>功能说明：文件冻结解冻回盘</li>
	 *         </p>
	 * @param batchCode
	 */
	public static void prcFrozeFileRet(String batchCode) {

		bizlog.method("DpFileFroze.prcFrozeFileRet begin >>>>>>>>>>>>>>>>");

		// 获取文件请求登记薄信息
		ApbBatchRequest batchReqTab = ApbBatchRequestDao.selectOne_odb1(batchCode, true);

		// 返回文件名称
		String fileName = batchReqTab.getFile_name() + "_result";

		// 获取文件批量业务定义信息
		AppBatch appBatch = AppBatchDao.selectOne_odb1(batchReqTab.getBusi_batch_id(), true);

		// 获取本地路径
		String localPath = ApFileApi.getFullPath(appBatch.getLocal_dir_code());

		// 获取文件头信息
		DpFileRetHeadInfo retHeadInfo = SqlDpFileBatchDao.selFrozeHeadInfo(BizUtil.getTrxRunEnvs().getBusi_org_id(), batchCode, true);

		// 转换成json格式
		String headJson = BizUtil.toJson(retHeadInfo);

		final LocalFileProcessor processor = new LocalFileProcessor(localPath, fileName, "UTF-8");

		processor.open(true);// 打开文件

		try {

			final StringBuffer files = new StringBuffer();
			// 文件头信息写入文件
			files.append(headJson).append(ApConst.FILE_LINEFEEDS);

			// 执行游标处理
			bizlog.debug("Execute cursor begin >>>>>>>>>>>");

			Params para = new Params();

			para.add(SysDict.A.busi_batch_code.toString(), batchCode);
			para.add(SysDict.A.org_id.toString(), BizUtil.getTrxRunEnvs().getBusi_org_id());

			// 文件体写入文件
			DaoUtil.selectList(SqlDpFileBatchDao.namedsql_selFileFrozeRecord, para, new CursorHandler<DpFileFrozeData>() {

				@Override
				public boolean handle(int index, DpFileFrozeData fileFrozeData) {

					// 转换成json格式
					String frozeJson = BizUtil.toJson(fileFrozeData);

					files.append(frozeJson).append(ApConst.FILE_LINEFEEDS);

					// 写入文件
					processor.write(files.toString());

					return true;
				}
			});

		}
		finally {
			// 关闭文件
			processor.close();
		}

		// 登记文件发送薄
		String fileId = ApFileSend.register(fileName, appBatch.getRemote_dir_code(), appBatch.getLocal_dir_code(), E_YESORNO.NO);

		ApSetRequestData setSucessReqData = BizUtil.getInstance(ApSetRequestData.class);

		setSucessReqData.setReturn_file_id(fileId);
		setSucessReqData.setSuccess_total_count(retHeadInfo.getSuccess_total_count());
		setSucessReqData.setSuccess_total_amt(retHeadInfo.getSuccess_total_amt());
		
		// 文件处理成功、更新请求登记薄信息
		ApBatch.setStatusByExecute(batchCode, setSucessReqData);
		// 文件处理成功、更新请求登记薄信息

		bizlog.method("DpFileTranStos.prcTranStosFileRet end <<<<<<<<<<<<<<<<");

	}
}
