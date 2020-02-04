package cn.sunline.icore.dp.serv.parm;

import java.util.List;

import cn.sunline.icore.ap.dataSync.ApParameterSync;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountProduct;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountProductDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountType;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountTypeDao;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.type.ComDpProductParmBase.DpCopyProductIn;
import cn.sunline.icore.dp.base.type.ComDpProductParmBase.DpProdCurrencyCtrlBase;
import cn.sunline.icore.dp.base.type.ComDpProductParmBase.DpProdInstRateCtrlBase;
import cn.sunline.icore.dp.base.type.ComDpProductParmBase.DpProdOpenCtrlBase;
import cn.sunline.icore.dp.base.type.ComDpProductParmBase.DpProdParmBaseInfo;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_SPECPRODTYPE;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpParameterDao;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpMaintainProdParameterIn;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpMaintainProdParameterOut;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryFuzzyProdIn;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryFuzzyProdOut;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryProdCcyIn;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryProdCcyOut;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryProdOpenIn;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryProdOpenOut;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryProdParameterOut;
import cn.sunline.icore.dp.serv.type.ComDpQueryAcct.DpQueryProdInstIn;
import cn.sunline.icore.dp.serv.type.ComDpQueryAcct.DpQueryProdInstOut;
import cn.sunline.icore.sys.parm.TrxEnvs.RunEnvs;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_PARAMETERTYPE;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：负债产品参数获取方法
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月12日-下午4:42:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpProduct {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpProduct.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年5月26日-下午3:55:31</li>
	 *         <li>功能说明：模糊查询产品列表</li>
	 *         </p>
	 * @param cplIn
	 * @return
	 */
	public static Options<DpQueryFuzzyProdOut> queryProdFuzzyInfo(DpQueryFuzzyProdIn cplIn) {
		bizlog.method(" DpProduct.queryProdFuzzyInfo begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		RunEnvs runEnvs = BizUtil.getTrxRunEnvs();

		Page<DpQueryFuzzyProdOut> page = SqlDpParameterDao.fuzzyQueryProdInfo(cplIn.getProd_id(), runEnvs.getBusi_org_id(), cplIn.getProd_name(), cplIn.getCust_type(),
				cplIn.getDd_td_ind(), cplIn.getAsst_liab_ind(), cplIn.getCard_relationship_ind(), cplIn.getData_status(), runEnvs.getTrxn_date(), runEnvs.getPage_start(),
				runEnvs.getPage_size(), runEnvs.getTotal_count(), false);

		Options<DpQueryFuzzyProdOut> cplOut = new DefaultOptions<DpQueryFuzzyProdOut>();
		runEnvs.setTotal_count(page.getRecordCount());
		cplOut.setValues(page.getRecords());

		bizlog.debug("DpQueryFuzzyProdOut=[%s]", cplOut);
		bizlog.method(" DpProduct.queryProdFuzzyInfo end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：产品利率参数查询</li>
	 *         </p>
	 * @param DpQueryProdInstIn
	 *            产品利率参数查询输入
	 * @return DpQueryProdInstOut
	 */
	public static DpQueryProdInstOut qryProdInstRateInfo(DpQueryProdInstIn cplIn) {

		DpProdInstRateCtrlBase instParmInfo = DpProductFactoryApi.qryProdInstRateInfo(cplIn.getProd_id(), cplIn.getCcy_code(), cplIn.getInst_key_type());

		DpQueryProdInstOut cplOut = BizUtil.getInstance(DpQueryProdInstOut.class);

		CommUtil.copyProperties(cplOut, instParmInfo);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：产品开户参数查询</li>
	 *         </p>
	 * @param DpQueryProdOpenIn
	 *            产品开户参数查询输入
	 * @return DpQueryProdOpenOut
	 */
	public static DpQueryProdOpenOut queryProdOpenInfo(DpQueryProdOpenIn cplIn) {

		DpProdOpenCtrlBase openParmCtrl = DpProductFactoryApi.queryProdOpenCtrl(cplIn.getProd_id(), cplIn.getAcct_type());

		DpQueryProdOpenOut cplOut = BizUtil.getInstance(DpQueryProdOpenOut.class);

		CommUtil.copyProperties(cplOut, openParmCtrl);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：产品币种参数查询</li>
	 *         </p>
	 * @param DpQueryProdCcyIn
	 *            产品币种参数查询输入
	 * @return DpQueryProdCcyOut
	 */
	public static DpQueryProdCcyOut queryProdCcyCodeInfo(DpQueryProdCcyIn cplIn) {

		DpProdCurrencyCtrlBase currencyParmCtrl = DpProductFactoryApi.queryProdCurrencyCtrl(cplIn.getProd_id(), cplIn.getCcy_code());

		DpQueryProdCcyOut cplOut = BizUtil.getInstance(DpQueryProdCcyOut.class);

		CommUtil.copyProperties(cplOut, currencyParmCtrl);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：产品工厂维护</li>
	 *         </p>
	 * @param DpQueryProdCcyIn
	 *            产品币种参数查询输入
	 * @return DpQueryProdCcyOut
	 */
	public static DpMaintainProdParameterOut modifyProdInfo(DpMaintainProdParameterIn cplIn) {

		DpProdParmBaseInfo prodParmInfo = DpProductFactoryApi.modifyProdInfo(cplIn);

		DpMaintainProdParameterOut cplOut = BizUtil.getInstance(DpMaintainProdParameterOut.class);

		CommUtil.copyProperties(cplOut, prodParmInfo);

		ApParameterSync.regParaSyncMiddlePlat(E_PARAMETERTYPE.DP_PRODUCT, cplIn.getProd_id());

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：产品参数复制</li>
	 *         </p>
	 * @param DpCopyProductIn
	 *            产品复制输入
	 */
	public static void copyNewProduct(DpCopyProductIn cplIn) {

		DpProductFactoryApi.copyNewProduct(cplIn);

		// 基础产品信息
		DpfBase baseProdInfo = DpProductFactoryApi.getProdBaseInfo(cplIn.getBase_prod_id());

		// 查询符合条件的账户类型信息
		List<DppAccountType> listAcctType = DppAccountTypeDao.selectAll_odb2(baseProdInfo.getCust_type(), baseProdInfo.getAcct_busi_source(), false);

		// 如果同类型账户类型只有一个，则插入账户类型和产品映射表，账户类型参数就不对外提供维护接口了
		if (CommUtil.isNotNull(listAcctType) && listAcctType.size() == 1) {

			DppAccountProduct acctProd = BizUtil.getInstance(DppAccountProduct.class);

			acctProd.setAcct_type(listAcctType.get(0).getAcct_type());
			acctProd.setProd_id(cplIn.getProd_id());

			if (baseProdInfo.getDd_td_ind() == E_DEMANDORTIME.DEMAND || baseProdInfo.getSpec_dept_type() == E_SPECPRODTYPE.SMART_TIME) {
				acctProd.setSame_acct_max_num(1L);
			}
			else {
				acctProd.setSame_acct_max_num(0L);
			}

			DppAccountProductDao.insert(acctProd);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：产品新增</li>
	 *         </p>
	 * @param DpMaintainProdParameterIn
	 *            产品新增输入
	 */
	public static void addProdInfo(DpMaintainProdParameterIn cplIn) {

		DpProductFactoryApi.addProdInfo(cplIn);

		// 登记同步控制表
		ApParameterSync.regParaSyncMiddlePlat(E_PARAMETERTYPE.DP_PRODUCT, cplIn.getProd_id());
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：产品开户参数查询</li>
	 *         </p>
	 * @param DpQueryProdOpenIn
	 *            产品开户参数查询输入
	 * @return DpQueryProdOpenOut
	 */
	public static DpQueryProdParameterOut queryProdParameterInfo(String prodId) {

		DpProdParmBaseInfo cplProdParmInfo = DpProductFactoryApi.queryProdAllParmInfo(prodId);

		DpQueryProdParameterOut cplOut = BizUtil.getInstance(DpQueryProdParameterOut.class);

		CommUtil.copyProperties(cplOut, cplProdParmInfo);

		return cplOut;
	}

}