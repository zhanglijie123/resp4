package cn.sunline.icore.dp.serv.batch;

import java.io.File;
import java.math.BigDecimal;
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
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.ap.util.DBUtil;
import cn.sunline.icore.dp.serv.namedsql.batch.SqlDpFileBatchDao;
import cn.sunline.icore.dp.serv.tables.TabDpFileBatch.DpbFileOpen;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpBatchLoadData;
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

public class DpFileOpenAcct {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpFileOpenAcct.class);

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2018年3月5日-上午11:20:05</li>
	 *         <li>功能说明：批量开户导入处理</li>
	 *         </p>
	 * @param dataItem
	 */
	public static void prcOpenFileLoad(DpBatchLoadData dataItem) {

		bizlog.method(" DpFileOpenAcct.prcOpenFileLoad begin >>>>>>>>>>>>>>>>");

		// 本地文件名
		String localFileName = ApFileApi.getFileFullPath(dataItem.getFile_local_path(), dataItem.getFile_name());

		localFileName = ApFileApi.getLocalHome(localFileName);

		// 读取文件列表
		List<String> fileList = ApFileApi.readFile(new File(localFileName));

		// 读取文件为空
		if (fileList.size() == 0) {
			return;
		}

		// 头文件信息
		DpFileHeadInfo headInfo = SysUtil.deserialize(fileList.get(0), DpFileHeadInfo.class);

		// 文件头格式不符
		if (CommUtil.isNull(headInfo.getHead_total_count())) {

			// 更新文件请求登记薄文件处理状态
			ApBatch.setFormatErrorByImport(dataItem.getBusi_batch_code());

			return; // 头文件格式不符直接返回
		}

		// 文件体信息
		List<String> fileBody = fileList.subList(1, fileList.size());

		List<DpbFileOpen> listFileTran = new ArrayList<DpbFileOpen>();

		for (String sJson : fileBody) {

			// 反序列化对象
			DpbFileOpen fileOpen = SysUtil.deserialize(sJson, DpbFileOpen.class);

			fileOpen.setBusi_batch_code(dataItem.getBusi_batch_code());
			fileOpen.setFile_detail_handling_status(E_FILEDETAILDEALSTATUS.WAIT);
			fileOpen.setHash_value(BizUtil.getGroupHashValue("REQUEST_HASH_VALUE", dataItem.getFile_id()));

			// 非空要素校验
			prcFieldCover(fileOpen);

			listFileTran.add(fileOpen);

			if (listFileTran.size() == 50) {

				try {
					// 文件明细信息批量插入表
					DaoUtil.insertBatch(DpbFileOpen.class, listFileTran);
					listFileTran.clear();
				}
				catch (Exception e) {

					DBUtil.rollBack();
					// 导入明细表异常、更新状态
					ApBatch.setInsertErrorByImport(dataItem.getBusi_batch_code(), e.toString());
					return;

				}

			}
		}

		// 存有数据
		if (listFileTran.size() > 0) {
			try {
				// 文件明细信息批量插入表
				DaoUtil.insertBatch(DpbFileOpen.class, listFileTran);
				listFileTran.clear();

			}
			catch (Exception e) {

				DBUtil.rollBack();
				// 导入明细表异常、更新状态
				ApBatch.setInsertErrorByImport(dataItem.getBusi_batch_code());
				return;
			}

		}

		// 获取一对一明细汇总信息
		DpFileRetHeadInfo retHeadInfo = SqlDpFileBatchDao.selOpenAcctHeadInfo(BizUtil.getTrxRunEnvs().getBusi_org_id(), dataItem.getBusi_batch_code(), true);

		// 校验头体数据、更新状态
		ApBatch.setStatusByImport(dataItem.getBusi_batch_code(), headInfo.getHead_total_count(), CommUtil.nvl(headInfo.getHead_total_amt(), BigDecimal.ZERO),
				retHeadInfo.getTotal_count(), CommUtil.nvl(retHeadInfo.getTotal_amt(), BigDecimal.ZERO));

		bizlog.method(" DpFileOpenAcct.prcOpenFileLoad end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月18日-下午3:26:47</li>
	 *         <li>功能说明：批量开户明细非空字段赋缺省值</li>
	 *         </p>
	 * @param transfStos
	 */
	private static void prcFieldCover(DpbFileOpen openFile) {

		if (CommUtil.isNull(openFile.getBusi_batch_code())) {
			BizUtil.fieldNotNull(openFile.getBusi_batch_code(), SysDict.A.busi_batch_code.getId(), SysDict.A.busi_batch_code.getLongName());
		}

		if (CommUtil.isNull(openFile.getBusi_seq())) {
			BizUtil.fieldNotNull(openFile.getBusi_seq(), SysDict.A.busi_seq.getId(), SysDict.A.busi_seq.getLongName());
		}

		if (CommUtil.isNull(openFile.getAcct_type())) {
			BizUtil.fieldNotNull(openFile.getAcct_type(), SysDict.A.acct_type.getId(), SysDict.A.acct_type.getLongName());
		}

		if (CommUtil.isNull(openFile.getProd_id())) {
			BizUtil.fieldNotNull(openFile.getProd_id(), SysDict.A.prod_id.getId(), SysDict.A.prod_id.getLongName());
		}

		if (CommUtil.isNull(openFile.getCcy_code())) {
			BizUtil.fieldNotNull(openFile.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());
		}

		if (CommUtil.isNull(openFile.getAcct_branch())) {
			BizUtil.fieldNotNull(openFile.getAcct_branch(), SysDict.A.acct_branch.getId(), SysDict.A.acct_branch.getLongName());
		}

	}

	/**
	 * @Author Linshiq
	 *         <p>
	 *         <li>2018年3月6日-上午9:47:22</li>
	 *         <li>功能说明：开户回盘处理</li>
	 *         </p>
	 * @param batchCode
	 *            批量处理码
	 */
	public static void prcOpenAcctFileRet(String batchCode) {
		bizlog.method(" DpFileOpenAcct.prcOpenAcctFileRet begin >>>>>>>>>>>>>>>>");

		// 获取文件请求登记薄信息
		ApbBatchRequest batchReqTab = ApbBatchRequestDao.selectOne_odb1(batchCode, true);

		// 返回文件名称
		String fileName = batchReqTab.getFile_name().substring(0, batchReqTab.getFile_name().indexOf(".")) + "_result";// 拼接后缀_result

		fileName = String.format("%s.txt", fileName);

		// 获取文件批量业务定义信息
		AppBatch appBatch = AppBatchDao.selectOne_odb1(batchReqTab.getBusi_batch_id(), true);

		// 获取本地路径
		String localPath = ApFileApi.getFullPath(appBatch.getLocal_dir_code());

		// 获取文件头信息
		DpFileRetHeadInfo retHeadInfo = SqlDpFileBatchDao.selOpenAcctHeadInfo(BizUtil.getTrxRunEnvs().getBusi_org_id(), batchCode, true);

		// 转换成json格式
		String headJson = BizUtil.toJson(retHeadInfo);

		final LocalFileProcessor processor = new LocalFileProcessor(localPath, fileName, "UTF-8");

		processor.open(true);// 打开文件

		try {

			// 文件头信息写入文件
			processor.write(headJson);

			// 执行游标处理
			bizlog.debug("Execute cursor begin >>>>>>>>>>>");

			Params para = new Params();

			para.add(SysDict.A.busi_batch_code.toString(), batchCode);
			para.add(SysDict.A.org_id.toString(), BizUtil.getTrxRunEnvs().getBusi_org_id());

			// 文件体写入文件
			DaoUtil.selectList(SqlDpFileBatchDao.namedsql_selFileOpenAcctRecord, para, new CursorHandler<DpbFileOpen>() {

				@Override
				public boolean handle(int index, DpbFileOpen fileOpen) {

					// 转换成json格式
					String tranStosJson = BizUtil.toJson(fileOpen);

					// 写入文件
					processor.write(tranStosJson);

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
		setSucessReqData.setSuccess_total_amt(CommUtil.nvl(retHeadInfo.getSuccess_total_amt(), BigDecimal.ZERO));

		// 文件处理成功、更新请求登记薄信息
		ApBatch.setStatusByExecute(batchCode, setSucessReqData);

		bizlog.method(" DpFileOpenAcct.prcOpenAcctFileRet end <<<<<<<<<<<<<<<<");
	}
}
