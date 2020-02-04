package cn.sunline.icore.dp.serv.interest;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpProductFactoryApi;
import cn.sunline.icore.dp.base.api.DpRateBasicApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfBase;
import cn.sunline.icore.dp.base.tables.TabDpProductFactory.DpfInterest;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpLayerInrt;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryIn;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_INSTKEYTYPE;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_MATURESUREWAY;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpProdInrtTriaIn;
import cn.sunline.icore.dp.serv.type.ComDpInterest.DpProdInrtTriaOut;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_FLOATWAY;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;

/**
 * <p>
 * 文件功能说明：存款产品利率试算
 * </p>
 * 
 * @Author shenxy
 *         <p>
 *         <li>2017年7月5日-下午1:30:55</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年7月5日-shenxy：创建文件</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpProdInrtTrial {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpProdInrtTrial.class);

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月5日-下午1:32:20</li>
	 *         <li>功能说明：存款产品利率试算主程序</li>
	 *         </p>
	 * @param cplIn
	 *            存款产品利率试算输入
	 * @return DpProdInrtTriaOut 存款产品利率试算输入
	 */
	public static DpProdInrtTriaOut prodInrtTrial(DpProdInrtTriaIn cplIn) {
		bizlog.method(" DpProdInrtTrial.prodInrtTrial begin >>>>>>>>>>>>>>>>");
		bizlog.debug("cplIn=[%s]", cplIn);

		// 产品编号不能为空
		BizUtil.fieldNotNull(cplIn.getProd_id(), SysDict.A.prod_id.getId(), SysDict.A.prod_id.getLongName());

		// 货币代码不能为空
		BizUtil.fieldNotNull(cplIn.getCcy_code(), SysDict.A.ccy_code.getId(), SysDict.A.ccy_code.getLongName());

		// 利率浮动方式非必输，默认为不浮动
		cplIn.setInrt_float_method(CommUtil.nvl(cplIn.getInrt_float_method(), E_FLOATWAY.NONE));

		// 查询产品基础属性
		DpfBase prodBaseInfo = DpProductFactoryApi.getProdBaseInfo(cplIn.getProd_id());

		// 定期产品开户必输检查
		if (prodBaseInfo.getDd_td_ind() == E_DEMANDORTIME.TIME) {

			// 定期产品存期必输
			if (prodBaseInfo.getDue_date_confirm_method() == E_MATURESUREWAY.TERM) {
				BizUtil.fieldNotNull(cplIn.getTerm_code(), SysDict.A.term_code.getId(), SysDict.A.term_code.getLongName());
			}

			// 定期产品开户金额必输
			BizUtil.fieldNotNull(cplIn.getFirst_deposit_amt(), DpBaseDict.A.first_deposit_amt.getId(), DpBaseDict.A.first_deposit_amt.getLongName());
		}
		else {
			// 活期存期默认为0D
			cplIn.setTerm_code("0D");
		}

		if (prodBaseInfo.getDue_date_confirm_method() == E_MATURESUREWAY.APPOINT) {
			// 到期日必输
			BizUtil.fieldNotNull(cplIn.getDue_date(), SysDict.A.due_date.getId(), SysDict.A.due_date.getLongName());
		}

		// 初始化输出接口
		DpProdInrtTriaOut cplOut = BizUtil.getInstance(DpProdInrtTriaOut.class);

		// 产品不计息，无计息信息定义
		if (prodBaseInfo.getInst_ind() == E_YESORNO.NO) {
			return cplOut;
		}

		// 产品计息信息
		DpfInterest prodInstInfo = DpProductFactoryApi.getProdInterestDefine(cplIn.getProd_id(), cplIn.getCcy_code(), E_INSTKEYTYPE.NORMAL, true);

		cplOut.setCcy_code(cplIn.getCcy_code());// 货币代号
		cplOut.setInst_rate_file_way(prodInstInfo.getInst_rate_file_way());// 利率靠档方式

		// 查询利率信息
		DpInrtQryIn qryIn = BizUtil.getInstance(DpInrtQryIn.class);

		qryIn.setInrt_code(prodInstInfo.getInrt_code());// 利率编号
		qryIn.setCcy_code(cplIn.getCcy_code());// 货币代号
		qryIn.setTerm_code(cplIn.getTerm_code());// 存期
		qryIn.setStart_inst_date(CommUtil.nvl(cplIn.getStart_inst_date(), BizUtil.getTrxRunEnvs().getTrxn_date()));// 没输入则默认为交易日期
		qryIn.setEnd_date(cplIn.getDue_date());// 截止日期
		qryIn.setTrxn_amt(cplIn.getFirst_deposit_amt());// 交易金额
		qryIn.setInrt_float_method(cplIn.getInrt_float_method());// 利率浮动方式
		qryIn.setInrt_float_value(cplIn.getInrt_float_value());// 利率浮动值
		qryIn.setInrt_reset_method(prodInstInfo.getInrt_reset_method());
		qryIn.setInst_rate_file_way(prodInstInfo.getInst_rate_file_way());
		qryIn.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date());

		DpInrtQryOut instRateInfo = DpRateBasicApi.getInstRateInfo(qryIn);
		Options<DpLayerInrt> listLayerInrt = instRateInfo.getListLayerInrt();

		// 封装输出
		cplOut.setInrt_float_method(instRateInfo.getInrt_float_method());// 利率浮动方式
		cplOut.setInrt_float_value(instRateInfo.getInrt_float_value());// 利率浮动值
		cplOut.setTier_method(instRateInfo.getTier_method());// 分层方式

		if (CommUtil.isNull(listLayerInrt)) {
			cplOut.setBank_base_inrt(instRateInfo.getBank_base_inrt());// 行内基准利率
			cplOut.setEfft_inrt(instRateInfo.getEfft_inrt());// 账户执行利率
		}
		else {
			cplOut.setListLayerInrt(listLayerInrt);
		}

		bizlog.debug("cplOut=[%s]", cplOut);
		bizlog.method(" DpProdInrtTrial.prodInrtTrial end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

}
