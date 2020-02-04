package cn.sunline.icore.dp.serv.parm;

import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpFrozeParmApi;
import cn.sunline.icore.dp.base.type.ComDpAcctTypeParmBase.DpAcctTypeBaseInfo;
import cn.sunline.icore.dp.base.type.ComDpFrozeParmBase.DpFrozeParmBaseInfo;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpParameterDao;
import cn.sunline.icore.dp.serv.tables.TabDpParameter.DppGlcodeMapping;
import cn.sunline.icore.dp.serv.tables.TabDpParameter.DppGlcodeMappingDao;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpAcctTypeInfo;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpAcctTypeMaintainIn;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpAcctTypeMaintainOut;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpFrozeKindCodeParamMntIn;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpFrozeKindCodeParamMntOut;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpProdInfo;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQryAcctTypeListIn;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryAcctTypeIn;
import cn.sunline.icore.dp.serv.type.ComDpParm.DpQueryAcctTypeOut;
import cn.sunline.icore.dp.serv.type.ComDpQueryFroze.DpFrozeKindCodeQueryOut;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：负债业务参数应用相关方法
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
public class DpParameter {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpParameter.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月8日-下午12:43:54</li>
	 *         <li>功能说明：账户类型列表查询</li>
	 *         </p>
	 * @param cplIn
	 * @return DpQryAcctTypeListOut
	 */
	public static Options<DpAcctTypeInfo> queryAcctTypeDetail(DpQryAcctTypeListIn cplIn) {

		bizlog.method("DpParameter.queryAcctTypeDetail begin >>>>>>>>>>>>>>>>>");

		// 动态sql查询账户类型参数范围
		List<DpAcctTypeInfo> AcctTypeList = SqlDpParameterDao.selAcctTypeDetailByCustType(BizUtil.getTrxRunEnvs().getBusi_org_id(), cplIn.getCust_type(),
				cplIn.getAcct_busi_source(), cplIn.getDd_td_ind(), cplIn.getCard_relationship_ind(), false);

		// 输出
		Options<DpAcctTypeInfo> opAcctTypeInfo = new DefaultOptions<DpAcctTypeInfo>();

		for (DpAcctTypeInfo acctTypeInfo : AcctTypeList) {

			DpAcctTypeInfo acctType = BizUtil.getInstance(DpAcctTypeInfo.class);

			acctType.setAcct_type(acctTypeInfo.getAcct_type());
			acctType.setAcct_type_name(acctTypeInfo.getAcct_type_name());
			acctType.setDd_td_ind(acctTypeInfo.getDd_td_ind());

			opAcctTypeInfo.add(acctType);
		}

		bizlog.method("DpParameter.queryAcctTypeDetail end <<<<<<<<<<<<<<<<<");

		return opAcctTypeInfo;

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年4月11日-上午10:23:07</li>
	 *         <li>功能说明：根据账户类型获取产品信息</li>
	 *         </p>
	 * @param acctType
	 * @param DdTdInd
	 * @return Options<DpProdInfo>
	 */
	public static Options<DpProdInfo> getProdInfo(String acctType, E_DEMANDORTIME DdTdInd, E_YESORNO agreeInd) {

		Options<DpProdInfo> opProdInfo = new DefaultOptions<DpProdInfo>();

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();
		String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();

		// 动态sql根据账户类型查询产品信息
		List<DpProdInfo> prodInfoList = SqlDpParameterDao.selProdInfoByAcctType(acctType, DdTdInd, agreeInd, orgId, trxnDate, false);

		for (DpProdInfo prodInfo : prodInfoList) {

			DpProdInfo prod = BizUtil.getInstance(DpProdInfo.class);

			prod.setProd_id(prodInfo.getProd_id());
			prod.setProd_name(prodInfo.getProd_name());
			prod.setSame_acct_max_num(prodInfo.getSame_acct_max_num());

			opProdInfo.add(prod);
		}

		return opProdInfo;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：账户类型参数详情查询</li>
	 *         </p>
	 * @param acctType
	 *            账户类型
	 * @return DpQueryAcctTypeOut
	 */
	public static DpQueryAcctTypeOut queryAcctTypeInfo(DpQueryAcctTypeIn cplIn) {

		DpAcctTypeBaseInfo acctTypeParmInfo = DpAccountTypeParmApi.queryAcctTypeInfo(cplIn);

		DpQueryAcctTypeOut cplOut = BizUtil.getInstance(DpQueryAcctTypeOut.class);

		CommUtil.copyProperties(cplOut, acctTypeParmInfo);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：冻结参数详情查询</li>
	 *         </p>
	 * @param frozeKindCode
	 *            冻结分类码
	 * @return DpFrozeKindCodeQueryOut
	 */
	public static DpFrozeKindCodeQueryOut queryFrozeKindCodeInfo(final String frozeKindCode) {

		DpFrozeParmBaseInfo frozeParmInfo = DpFrozeParmApi.queryFrozeParm(frozeKindCode);

		DpFrozeKindCodeQueryOut cplOut = BizUtil.getInstance(DpFrozeKindCodeQueryOut.class);

		CommUtil.copyProperties(cplOut, frozeParmInfo);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：账户类型参数维护</li>
	 *         </p>
	 * @param DpAcctTypeMaintainIn
	 *            账户类型维护输入
	 * @return DpAcctTypeMaintainOut
	 */
	public static DpAcctTypeMaintainOut acctTypeMaintain(DpAcctTypeMaintainIn cplIn) {

		DpAcctTypeBaseInfo acctTypeParmInfo = DpAccountTypeParmApi.acctTypeMaintain(cplIn);

		DpAcctTypeMaintainOut cplOut = BizUtil.getInstance(DpAcctTypeMaintainOut.class);

		CommUtil.copyProperties(cplOut, acctTypeParmInfo);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：冻结分类码参数维护</li>
	 *         </p>
	 * @param DpFrozeKindCodeParamMntIn
	 *            冻结参数维护输入
	 * @return DpFrozeKindCodeParamMntOut
	 */
	public static DpFrozeKindCodeParamMntOut modifyFrozeParameter(DpFrozeKindCodeParamMntIn cplIn) {

		DpFrozeParmBaseInfo frozeParmInfo = DpFrozeParmApi.modifyFrozeParameter(cplIn);

		DpFrozeKindCodeParamMntOut cplOut = BizUtil.getInstance(DpFrozeKindCodeParamMntOut.class);

		CommUtil.copyProperties(cplOut, frozeParmInfo);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：冻结分类码参数新增</li>
	 *         </p>
	 * @param DpFrozeKindCodeParamMntIn
	 *            冻结参数维护输入
	 * @return DpFrozeKindCodeParamMntOut
	 */
	public static DpFrozeKindCodeParamMntOut addFrozeParam(DpFrozeKindCodeParamMntIn cplIn) {

		DpFrozeParmBaseInfo frozeParmInfo = DpFrozeParmApi.addFrozeParam(cplIn);

		DpFrozeKindCodeParamMntOut cplOut = BizUtil.getInstance(DpFrozeKindCodeParamMntOut.class);

		CommUtil.copyProperties(cplOut, frozeParmInfo);

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年1月13日-上午10:23:07</li>
	 *         <li>功能说明：账户类型参数新增</li>
	 *         </p>
	 * @param DpAcctTypeMaintainIn
	 *            账户类型参数维护输入
	 */
	public static void addAcctTypeParm(DpAcctTypeMaintainIn cplIn) {

		DpAccountTypeParmApi.addAcctTypeParm(cplIn);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2020年1月16日-上午10:23:07</li>
	 *         <li>功能说明：获取交易业务编码映射信息</li>
	 *         <li>使用说明：适用于一方是存款账户，通过此方法获取另一方内部户信息</li>
	 *         </p>
	 * @return 交易业务编码映射信息
	 */
	public static DppGlcodeMapping getTrxnGlCodeInfo() {

		return getTrxnGlCodeInfo(BizUtil.getTrxRunEnvs().getTrxn_code(), BizUtil.getTrxRunEnvs().getChannel_id(), BizUtil.getTrxRunEnvs().getExternal_scene_code());
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2020年1月16日-上午10:23:07</li>
	 *         <li>功能说明：获取交易业务编码映射信息</li>
	 *         <li>使用说明：适用于一方是存款账户，通过此方法获取另一方内部户信息</li>
	 *         </p>
	 * @param trxnCode
	 *            交易代码
	 * @param channelId
	 *            渠道代码
	 * @param externalSceneCode
	 *            外部场景代码
	 * @return 交易业务编码映射信息
	 */
	public static DppGlcodeMapping getTrxnGlCodeInfo(String trxnCode, String channelId, String sceneCode) {

		// 外部场景码可能为空
		String externalSceneCode = CommUtil.nvl(sceneCode, ApConst.WILDCARD);

		// 优先读取最严格匹配条件
		DppGlcodeMapping mappInfo = DppGlcodeMappingDao.selectOne_odb1(trxnCode, sceneCode, channelId, false);

		// 其次根据交易码 + 外部场景码匹配
		if (mappInfo == null) {

			mappInfo = DppGlcodeMappingDao.selectOne_odb1(trxnCode, externalSceneCode, ApConst.WILDCARD, false);
		}

		// 再其次根据交易码 + 渠道匹配
		if (mappInfo == null) {

			mappInfo = DppGlcodeMappingDao.selectOne_odb1(trxnCode, ApConst.WILDCARD, channelId, false);
		}

		// 最后根据交易码匹配
		if (mappInfo == null) {

			mappInfo = DppGlcodeMappingDao.selectOne_odb1(trxnCode, ApConst.WILDCARD, ApConst.WILDCARD, false);
		}

		// 最终还是匹配不上则报错
		if (mappInfo == null) {
			throw APPUB.E0005(OdbFactory.getTable(DppGlcodeMapping.class).getId(), SysDict.A.trxn_code.getLongName(), trxnCode);
		}

		return mappInfo;
	}
}
