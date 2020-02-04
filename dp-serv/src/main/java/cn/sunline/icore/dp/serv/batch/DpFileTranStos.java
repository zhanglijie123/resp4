package cn.sunline.icore.dp.serv.batch;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_CASHTRXN;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApFileApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
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
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.serv.iobus.DpChargeIobus;
import cn.sunline.icore.dp.serv.namedsql.batch.SqlDpFileBatchDao;
import cn.sunline.icore.dp.serv.tables.TabDpFileBatch.DpbFileTransferStos;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpBatchLoadData;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpFileHeadInfo;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpFileRetHeadInfo;
import cn.sunline.icore.dp.serv.type.ComDpBatchFile.DpFileTranStosData;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgCalcOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgInDetails;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgManualIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrg.IoCmChrgManualOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrgBase.IoCmChrgCalcIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmChrgBase.IoCmChrgDefQryIn;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_CHRGASSOOBJTYPE;
import cn.sunline.icore.sys.type.EnumType.E_FILEDETAILDEALSTATUS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.busi.sdk.util.DaoUtil;
import cn.sunline.ltts.core.api.dao.CursorHandler;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;
import cn.sunline.ltts.dao.Params;

public class DpFileTranStos {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpFileTranStos.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月20日-下午3:12:43</li>
	 *         <li>功能说明：一对一转账文件导入处理</li>
	 *         </p>
	 * @param dataItem
	 */

	// 一对一转账文件导入处理
	public static void prcTranStosFileLoad(DpBatchLoadData dataItem) {

		bizlog.method("exec DpFileTranStos.prcTranStosFileLoad start >>>>>>>>>>>>>>");

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
		if (CommUtil.isNull(headInfo.getHead_total_count()) || CommUtil.isNull(headInfo.getHead_total_amt())) {

			// 更新文件请求登记薄文件处理状态
			ApBatch.setFormatErrorByImport(dataItem.getBusi_batch_code());

			return; // 头文件格式不符直接返回
		}

		// 文件体信息
		List<String> fileBody = fileList.subList(1, fileList.size());

		List<DpbFileTransferStos> listFileTran = new ArrayList<DpbFileTransferStos>();

		for (String sJson : fileBody) {

			// 反序列化对象
			DpbFileTransferStos transfStos = SysUtil.deserialize(sJson, DpbFileTransferStos.class);

			transfStos.setBusi_batch_code(dataItem.getBusi_batch_code());
			transfStos.setBatch_detail_type(dataItem.getBatch_detail_type());
			transfStos.setFile_detail_handling_status(E_FILEDETAILDEALSTATUS.WAIT);
			transfStos.setHash_value(BizUtil.getGroupHashValue("REQUEST_HASH_VALUE", dataItem.getFile_id()));
			transfStos.setTrxn_success_amt(BigDecimal.ZERO);

			transfStos.setCredit_ccy_code(CommUtil.isNull(transfStos.getCredit_ccy_code()) ? transfStos.getTrxn_ccy() : transfStos.getCredit_ccy_code());
			transfStos.setDebit_ccy_code(CommUtil.isNull(transfStos.getDebit_ccy_code()) ? transfStos.getTrxn_ccy() : transfStos.getDebit_ccy_code());

			// 非空要素校验
			prcFieldCover(transfStos);

			listFileTran.add(transfStos);

			if (listFileTran.size() == 50) {

				try {
					// 文件明细信息批量插入表
					DaoUtil.insertBatch(DpbFileTransferStos.class, listFileTran);
					listFileTran.clear();
				}
				catch (Exception e) {
					
					bizlog.error("Transfers Stos Load faile=[%s]",e, e.getMessage());

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
				DaoUtil.insertBatch(DpbFileTransferStos.class, listFileTran);
				listFileTran.clear();

			}
			catch (Exception e) {

				bizlog.error("Transfers Stos Load faile=[%s]",e, e.getMessage());
				
				DBUtil.rollBack();
				// 导入明细表异常、更新状态
				ApBatch.setInsertErrorByImport(dataItem.getBusi_batch_code());
				return;
			}

		}

		// 获取一对一明细汇总信息
		DpFileRetHeadInfo retHeadInfo = SqlDpFileBatchDao.selOneByOneHeadInfo(BizUtil.getTrxRunEnvs().getBusi_org_id(), dataItem.getBusi_batch_code(), true);

		// 校验头体数据、更新状态
		ApBatch.setStatusByImport(dataItem.getBusi_batch_code(), headInfo.getHead_total_count(), headInfo.getHead_total_amt(), retHeadInfo.getTotal_count(),
				retHeadInfo.getTotal_amt());

		bizlog.method("exec DpFileTranStos.prcTranStosFileLoad end <<<<<<<<<<<<<<");

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月20日-下午3:49:02</li>
	 *         <li>功能说明：一对一转账文件回盘</li>
	 *         </p>
	 * @param batchCode
	 */
	public static void prcTranStosFileRet(String batchCode) {

		bizlog.method("DpFileTranStos.prcTranStosFileRet begin >>>>>>>>>>>>>>>>");

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
		DpFileRetHeadInfo retHeadInfo = SqlDpFileBatchDao.selOneByOneHeadInfo(BizUtil.getTrxRunEnvs().getBusi_org_id(), batchCode, true);

		IoCmChrgManualOut ChrgManualOut = BizUtil.getInstance(IoCmChrgManualOut.class);

		try {
			// 收费处理
			if (batchReqTab.getBatch_charges_ind() == E_YESORNO.YES && retHeadInfo.getSuccess_total_count() > 0) {

				ChrgManualOut = chargeProcess(batchReqTab, retHeadInfo.getSuccess_total_count());
			}
		}
		catch (Exception e) {
			DaoUtil.rollbackTransaction();
		}

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
			DaoUtil.selectList(SqlDpFileBatchDao.namedsql_selFileOneByOneRecord, para, new CursorHandler<DpFileTranStosData>() {

				@Override
				public boolean handle(int index, DpFileTranStosData TranStosData) {

					// 转换成json格式
					String tranStosJson = BizUtil.toJson(TranStosData);

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
		setSucessReqData.setSuccess_total_amt(retHeadInfo.getSuccess_total_amt());
		setSucessReqData.setCalc_chrg_amt(ChrgManualOut.getDeduct_chrg_total_amt()); // 扣费总金额
		setSucessReqData.setCalc_chrg_ccy(ChrgManualOut.getDeduct_chrg_ccy()); // 扣费币种
		setSucessReqData.setTrxn_seq(ChrgManualOut.getTrxn_seq()); // 扣费流水
		// 文件处理成功、更新请求登记薄信息
		ApBatch.setStatusByExecute(batchCode, setSucessReqData);

		bizlog.method("DpFileTranStos.prcTranStosFileRet end <<<<<<<<<<<<<<<<");

	}

	private static IoCmChrgManualOut chargeProcess(ApbBatchRequest batchReqTab, long quantity) {

		DpaAccount account = DpToolsApi.locateSingleAccount(batchReqTab.getDeduct_chrg_acct(), null, false);

		// 调用公共手工收费试算
		IoCmChrgCalcIn manualChrgCalcIn = BizUtil.getInstance(IoCmChrgCalcIn.class);

		manualChrgCalcIn.setChrg_asso_obj_type(E_CHRGASSOOBJTYPE.ACCT);
		manualChrgCalcIn.setAcct_no(batchReqTab.getDeduct_chrg_acct()); // 收费账号
		manualChrgCalcIn.setChrg_code(batchReqTab.getChrg_code());// 费用编号
		manualChrgCalcIn.setTrxn_ccy(batchReqTab.getDeduct_chrg_ccy()); // 收费币种
		manualChrgCalcIn.setQuantity(quantity); // 数量
		manualChrgCalcIn.setCust_no(account.getCust_no()); // 客户号

		IoCmChrgCalcOut cmChrgManualCalcOut = DpChargeIobus.calcManualChrg(manualChrgCalcIn);

		// 调用公共统一收费
		IoCmChrgManualIn prcManualChrgIn = BizUtil.getInstance(IoCmChrgManualIn.class);

		prcManualChrgIn.setChrg_asso_obj_type(E_CHRGASSOOBJTYPE.ACCT);
		prcManualChrgIn.setAcct_no(batchReqTab.getDeduct_chrg_acct());
		prcManualChrgIn.setCash_trxn_ind(E_CASHTRXN.TRXN);
		prcManualChrgIn.setCcy_code(batchReqTab.getDeduct_chrg_ccy());
		prcManualChrgIn.setDeduct_chrg_acct(batchReqTab.getDeduct_chrg_acct());
		prcManualChrgIn.setDeduct_chrg_ccy(batchReqTab.getDeduct_chrg_ccy());
		prcManualChrgIn.setCust_no(account.getCust_no());

		Options<IoCmChrgInDetails> chrgInDetails = new DefaultOptions<IoCmChrgInDetails>();

		IoCmChrgInDetails cmChrgInDetails = BizUtil.getInstance(IoCmChrgInDetails.class);
		cmChrgInDetails.setChrg_code(batchReqTab.getChrg_code());
		IoCmChrgDefQryIn cmChrgDefQry = BizUtil.getInstance(IoCmChrgDefQryIn.class);
		cmChrgDefQry.setChrg_code(batchReqTab.getChrg_code());

		cmChrgInDetails.setChrg_code_name(cmChrgManualCalcOut.getChrg_code_name());
		cmChrgInDetails.setQuantity(quantity);
		cmChrgInDetails.setCalc_chrg_ccy(cmChrgManualCalcOut.getCalc_chrg_ccy());
		cmChrgInDetails.setCalc_chrg_amt(cmChrgManualCalcOut.getCalc_chrg_amt());
		cmChrgInDetails.setBase_chrg_amt(cmChrgManualCalcOut.getBase_chrg_amt());
		cmChrgInDetails.setFloat_amt(cmChrgManualCalcOut.getFloat_amt());
		cmChrgInDetails.setPromo_amt(cmChrgManualCalcOut.getPromo_amt());
		cmChrgInDetails.setChrg_form_code(cmChrgManualCalcOut.getChrg_form_code());

		cmChrgInDetails.setActual_chrg_amt(cmChrgManualCalcOut.getCalc_chrg_amt());// 实际收费金额

		cmChrgInDetails.setSummary_code(ApSystemParmApi.getSummaryCode("CHARGE"));

		chrgInDetails.add(cmChrgInDetails);

		prcManualChrgIn.setChrgInDetails(chrgInDetails);

		IoCmChrgManualOut ChrgManualOut = DpChargeIobus.prcManualChrgAccounting(prcManualChrgIn);

		return ChrgManualOut;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月18日-下午3:26:47</li>
	 *         <li>功能说明：一对一转账明细非空字段赋缺省值</li>
	 *         </p>
	 * @param transfStos
	 */
	private static void prcFieldCover(DpbFileTransferStos transfStos) {
		
		if (CommUtil.isNull(transfStos.getBusi_batch_code())) {
			BizUtil.fieldNotNull(transfStos.getBusi_batch_code(), SysDict.A.busi_batch_code.getId(),  SysDict.A.busi_batch_code.getLongName());
		}

		if (CommUtil.isNull(transfStos.getBusi_seq())) {
			BizUtil.fieldNotNull(transfStos.getBusi_seq(), SysDict.A.busi_seq.getId(),  SysDict.A.busi_seq.getLongName());
		}

		if (CommUtil.isNull(transfStos.getTrxn_ccy())) {
			BizUtil.fieldNotNull(transfStos.getTrxn_ccy(), SysDict.A.trxn_ccy.getId(),  SysDict.A.trxn_ccy.getLongName());
		}

		if (CommUtil.isNull(transfStos.getTrxn_amt())) {
			BizUtil.fieldNotNull(transfStos.getTrxn_amt(), SysDict.A.trxn_amt.getId(),  SysDict.A.trxn_amt.getLongName());
		}

		// 借方账号是不能为空的
		if (CommUtil.isNull(transfStos.getDebit_acct_no())) {
			BizUtil.fieldNotNull(transfStos.getDebit_acct_no(), SysDict.A.debit_acct_no.getId(), SysDict.A.debit_acct_no.getLongName());
		}

		if (CommUtil.isNull(transfStos.getForce_draw_ind())) {
			transfStos.setForce_draw_ind(E_YESORNO.NO);
		}

		if (CommUtil.isNull(transfStos.getCredit_acct_no())) {
			BizUtil.fieldNotNull(transfStos.getCredit_acct_no(), SysDict.A.credit_acct_no.getId(), SysDict.A.credit_acct_no.getLongName());
		}

		// 摘要码是不能为空的
		if (CommUtil.isNull(transfStos.getSummary_code())) {
			BizUtil.fieldNotNull(transfStos.getSummary_code(), SysDict.A.summary_code.getId(), SysDict.A.summary_code.getLongName());
		}
	}
}
