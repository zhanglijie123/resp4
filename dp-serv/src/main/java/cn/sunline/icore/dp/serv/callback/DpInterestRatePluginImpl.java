package cn.sunline.icore.dp.serv.callback;

import java.math.BigDecimal;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.cm.sys.type.CmSysEnumType.E_INRTTYPE;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.plugin.DpInterestRatePlugin;
import cn.sunline.icore.dp.base.type.ComDpBaseServiceInterface.DpLayerInrt;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtCodeDefine;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryIn;
import cn.sunline.icore.dp.base.type.ComDpInterestBasic.DpInrtQryOut;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_RATERESETWAY;
import cn.sunline.icore.iobus.cm.servicetype.SrvIoCmInterestRateBase;
import cn.sunline.icore.iobus.cm.type.ComIoCmInterestRate.IoCmInrCodeListIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmInterestRate.IoCmInrCodeListOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmInterestRate.IoCmInrtCodeDef;
import cn.sunline.icore.iobus.cm.type.ComIoCmInterestRate.IoCmInrtQryIn;
import cn.sunline.icore.iobus.cm.type.ComIoCmInterestRate.IoCmInrtQryOut;
import cn.sunline.icore.iobus.cm.type.ComIoCmInterestRate.IoCmSimpleInrt;
import cn.sunline.icore.iobus.cm.type.ComIoCmInterestRate.IoCmTierInrt;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_FLOATWAY;
import cn.sunline.icore.sys.type.EnumType.E_INRTDIRECTION;
import cn.sunline.icore.sys.type.EnumType.E_INRTSTAS;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明：利率供底层调用扩展点
 * </p>
 * 
 * @Author 周明易
 *         <p>
 *         <li>2019年3月29日-下午14:35:50</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>2019年3月29日-周明易：存款模块透支额度相关扩展点</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpInterestRatePluginImpl implements DpInterestRatePlugin {

	/**
	 * 获取利率代码定义信息
	 * 
	 * @param inrtCode
	 *            利率编号
	 * @return 利率代码定义信息
	 */
	@Override
	@SuppressWarnings("unchecked")
	public DpInrtCodeDefine getInrtCodeDefine(String inrtCode) {

		// 外调公共服务
		IoCmInrtCodeDef cplDef = SysUtil.getRemoteInstance(SrvIoCmInterestRateBase.class).qryInrtCodeDefine(inrtCode);

		DpInrtCodeDefine cplOut = BizUtil.getInstance(DpInrtCodeDefine.class);

		cplOut.setInrt_code(cplDef.getInrt_code());
		cplOut.setInrt_code_direction(cplDef.getInrt_code_direction());
		cplOut.setInrt_code_name(cplDef.getInrt_code_name());
		cplOut.setInrt_code_status(cplDef.getInrt_code_status());
		cplOut.setInrt_tier_type(cplDef.getInrt_tier_type());
		cplOut.setTier_method(cplDef.getTier_method());

		return cplOut;
	}

	/**
	 * 获取利率代码列表
	 * 
	 * @param inrtCodeDirect
	 *            利率代码指向
	 * @return 利率代码定义列表信息
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Options<DpInrtCodeDefine> getInrtCodeList(E_INRTDIRECTION inrtCodeDirect) {

		// 合同利率信息
		Options<DpInrtCodeDefine> listRateCode = new DefaultOptions<DpInrtCodeDefine>();

		// 传入不分页标志
		IoCmInrCodeListIn cplIn = BizUtil.getInstance(IoCmInrCodeListIn.class);

		cplIn.setPage_ind(E_YESORNO.NO);
		cplIn.setInrt_code_direction(inrtCodeDirect);
		cplIn.setInrt_code_status(E_INRTSTAS.NORMAL);
		cplIn.setInrt_type(E_INRTTYPE.DEPOSIT);

		// 查询利率编号列表
		Options<IoCmInrCodeListOut> InrtCodeList = SysUtil.getRemoteInstance(SrvIoCmInterestRateBase.class).qryInrtCodeList(cplIn);

		for (IoCmInrCodeListOut inrtCodeInfo : InrtCodeList) {

			DpInrtCodeDefine cplInrtCodeDefine = BizUtil.getInstance(DpInrtCodeDefine.class);

			cplInrtCodeDefine.setInrt_code(inrtCodeInfo.getInrt_code());
			cplInrtCodeDefine.setInrt_code_direction(inrtCodeInfo.getInrt_code_direction());
			cplInrtCodeDefine.setInrt_code_name(inrtCodeInfo.getInrt_code_name());
			cplInrtCodeDefine.setInrt_code_status(inrtCodeInfo.getInrt_code_status());
			cplInrtCodeDefine.setInrt_tier_type(inrtCodeInfo.getInrt_tier_type());
			cplInrtCodeDefine.setTier_method(inrtCodeInfo.getTier_method());

			listRateCode.add(cplInrtCodeDefine);
		}

		return listRateCode;
	}

	/**
	 * 获取执行利率
	 * 
	 * @param cplIn
	 *            利率查询输入
	 * @return 利率代码定义列表信息
	 */
	@Override
	@SuppressWarnings("unchecked")
	public DpInrtQryOut getEffectInrtInfo(DpInrtQryIn cplIn) {

		// 公共模块利率参数查询接口
		IoCmInrtQryIn cplInrtQryIn = BizUtil.getInstance(IoCmInrtQryIn.class);

		cplInrtQryIn.setCcy_code(cplIn.getCcy_code());
		cplInrtQryIn.setEnd_date(cplIn.getEnd_date());
		cplInrtQryIn.setInrt_code(cplIn.getInrt_code());
		cplInrtQryIn.setStart_inst_date(cplIn.getStart_inst_date());
		cplInrtQryIn.setTerm_code(cplIn.getInrt_reset_method() == E_RATERESETWAY.NEAR ? null : cplIn.getTerm_code());
		cplInrtQryIn.setTrxn_amt(cplIn.getTrxn_amt());
		cplInrtQryIn.setTrxn_date(cplIn.getTrxn_date());
		cplInrtQryIn.setInst_rate_file_way(cplIn.getInst_rate_file_way());

		IoCmInrtQryOut ioCmInrtOut = SysUtil.getRemoteInstance(SrvIoCmInterestRateBase.class).qryEffeInrtByDate(cplInrtQryIn);

		// 输出结果
		DpInrtQryOut cplInrtOut = BizUtil.getInstance(DpInrtQryOut.class);

		// 简单利率加工
		if (ioCmInrtOut.getInrt_code_direction() == E_INRTDIRECTION.SIMPLE) {

			cplInrtOut = getSimpleInrtInfo(cplIn, ioCmInrtOut.getSimpleInrt());
		}
		// 分层利率加工
		else if (ioCmInrtOut.getInrt_code_direction() == E_INRTDIRECTION.LAYER) {

			cplInrtOut = getLayerInrtInfo(cplIn, ioCmInrtOut.getListLayerInrt());
		}
		// 简单合同利率加工
		else if (ioCmInrtOut.getInrt_code_direction() == E_INRTDIRECTION.SIMPLE_CONT) {

			cplInrtOut = getSimpleContractInrtInfo(cplIn);
		}
		// 分层合同利率加工
		else if (ioCmInrtOut.getInrt_code_direction() == E_INRTDIRECTION.LAYER_CONT) {

			cplInrtOut = getLayerContractInrtInfo(cplIn);
		}
		else {

			throw DpBase.E0218(cplIn.getInrt_code());
		}

		// 补充输出信息
		cplInrtOut.setTier_method(ioCmInrtOut.getTier_method());
		cplInrtOut.setCcy_code(cplIn.getCcy_code());
		cplInrtOut.setInrt_code(cplIn.getInrt_code());
		cplInrtOut.setInrt_code_direction(ioCmInrtOut.getInrt_code_direction());
		cplInrtOut.setInrt_code_name(ioCmInrtOut.getInrt_code_name());
		cplInrtOut.setInrt_float_method(CommUtil.nvl(cplIn.getInrt_float_method(), E_FLOATWAY.NONE));
		cplInrtOut.setInrt_float_value(CommUtil.nvl(cplIn.getInrt_float_value(), BigDecimal.ZERO));

		return cplInrtOut;
	}

	/**
	 * 获取简单利率信息
	 * 
	 * @param cplIn
	 *            利率查询输入
	 * @param cplInfo
	 *            简单利率信息
	 * @return 存款利率信息输出
	 */
	private static DpInrtQryOut getSimpleInrtInfo(DpInrtQryIn cplIn, IoCmSimpleInrt cplInrtInfo) {

		// 输出
		DpInrtQryOut cplInrtOut = BizUtil.getInstance(DpInrtQryOut.class);

		cplInrtOut.setSingle_layer_ind(E_YESORNO.YES);
		cplInrtOut.setCcy_code(cplIn.getCcy_code());
		cplInrtOut.setInrt_code(cplIn.getInrt_code());
		cplInrtOut.setBand_amount(BigDecimal.ZERO);
		cplInrtOut.setBand_term(null);
		cplInrtOut.setBank_base_inrt(cplInrtInfo.getInst_rate());
		cplInrtOut.setInrt_float_method(CommUtil.nvl(cplIn.getInrt_float_method(), E_FLOATWAY.NONE));
		cplInrtOut.setInrt_float_value(CommUtil.nvl(cplIn.getInrt_float_value(), BigDecimal.ZERO));
		cplInrtOut.setEfft_inrt(cplInrtInfo.getInst_rate());

		// 处理客户化浮动
		if (cplIn.getInrt_float_method() == E_FLOATWAY.PERCENT) {

			BigDecimal addValue = cplInrtOut.getEfft_inrt().multiply(cplIn.getInrt_float_value()).divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);

			cplInrtOut.setEfft_inrt(cplInrtOut.getEfft_inrt().add(addValue));
		}
		else if (cplIn.getInrt_float_method() == E_FLOATWAY.VALUE) {
			cplInrtOut.setEfft_inrt(cplInrtOut.getEfft_inrt().add(cplIn.getInrt_float_value()));
		}

		return cplInrtOut;
	}

	/**
	 * 获取简单合同利率信息
	 * 
	 * @param cplIn
	 *            利率查询输入
	 * @return 存款利率信息输出
	 */
	private static DpInrtQryOut getSimpleContractInrtInfo(DpInrtQryIn cplIn) {

		// 检查合同利率是否为空， 为负数也可以
		BizUtil.fieldNotNull(cplIn.getContract_inrt(), DpBaseDict.A.contract_inrt.getId(), DpBaseDict.A.contract_inrt.getLongName());

		// 输出
		DpInrtQryOut cplInrtOut = BizUtil.getInstance(DpInrtQryOut.class);

		cplInrtOut.setSingle_layer_ind(E_YESORNO.YES);
		cplInrtOut.setCcy_code(cplIn.getCcy_code());
		cplInrtOut.setInrt_code(cplIn.getInrt_code());
		cplInrtOut.setBand_amount(BigDecimal.ZERO);
		cplInrtOut.setBand_term(null);
		cplInrtOut.setBank_base_inrt(cplIn.getContract_inrt());
		cplInrtOut.setInrt_float_method(CommUtil.nvl(cplIn.getInrt_float_method(), E_FLOATWAY.NONE));
		cplInrtOut.setInrt_float_value(CommUtil.nvl(cplIn.getInrt_float_value(), BigDecimal.ZERO));
		cplInrtOut.setEfft_inrt(cplIn.getContract_inrt());

		// 合同利率也准许浮动， 要控制不能浮动的话调用此方法之前在外面控制
		if (cplIn.getInrt_float_method() == E_FLOATWAY.PERCENT) {

			BigDecimal addValue = cplIn.getContract_inrt().multiply(cplIn.getInrt_float_value()).divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);

			cplInrtOut.setEfft_inrt(cplIn.getContract_inrt().add(addValue));
		}
		else if (cplIn.getInrt_float_method() == E_FLOATWAY.VALUE) {
			cplInrtOut.setEfft_inrt(cplIn.getContract_inrt().add(cplIn.getInrt_float_value()));
		}

		return cplInrtOut;
	}

	/**
	 * 获取分层合同利率信息
	 * 
	 * @param cplIn
	 *            利率查询输入
	 * @return 存款利率信息输出
	 */
	private static DpInrtQryOut getLayerContractInrtInfo(DpInrtQryIn cplIn) {

		// 检查合同利率列表是否为空， 为负数也可以
		if (cplIn.getList_contract_inrt().isEmpty()) {

			BizUtil.fieldNotNull(cplIn.getContract_inrt(), DpBaseDict.A.contract_inrt.getId(), DpBaseDict.A.contract_inrt.getLongName());
		}

		// 输出
		DpInrtQryOut cplInrtOut = BizUtil.getInstance(DpInrtQryOut.class);

		cplInrtOut.setSingle_layer_ind(E_YESORNO.NO);
		cplInrtOut.setCcy_code(cplIn.getCcy_code());
		cplInrtOut.setInrt_code(cplIn.getInrt_code());
		cplInrtOut.setInrt_float_method(CommUtil.nvl(cplIn.getInrt_float_method(), E_FLOATWAY.NONE));
		cplInrtOut.setInrt_float_value(CommUtil.nvl(cplIn.getInrt_float_value(), BigDecimal.ZERO));

		// 合同利率也准许浮动， 要控制不能浮动的话调用此方法之前在外面控制
		for (DpLayerInrt inrtInfo : cplIn.getList_contract_inrt()) {

			BizUtil.fieldNotNull(inrtInfo.getEfft_inrt(), SysDict.A.efft_inrt.getId(), SysDict.A.efft_inrt.getLongName());

			DpLayerInrt layerInrt = BizUtil.getInstance(DpLayerInrt.class);

			layerInrt.setBand_amount(inrtInfo.getBand_amount());
			layerInrt.setBand_term(inrtInfo.getBand_term());
			layerInrt.setBank_base_inrt(inrtInfo.getEfft_inrt());
			layerInrt.setEfft_inrt(inrtInfo.getEfft_inrt());
			layerInrt.setLayer_no(inrtInfo.getLayer_no());

			if (cplIn.getInrt_float_method() == E_FLOATWAY.PERCENT) {

				BigDecimal addValue = inrtInfo.getEfft_inrt().multiply(cplIn.getInrt_float_value()).divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);

				layerInrt.setEfft_inrt(inrtInfo.getEfft_inrt().add(addValue));
			}
			else if (cplIn.getInrt_float_method() == E_FLOATWAY.VALUE) {
				layerInrt.setEfft_inrt(inrtInfo.getEfft_inrt().add(cplIn.getInrt_float_value()));
			}

			cplInrtOut.getListLayerInrt().add(layerInrt);
		}

		// 虽然是分层利率，但仍然把第一层赋值到外面散字段
		cplInrtOut.setBand_amount(cplInrtOut.getListLayerInrt().get(0).getBand_amount());
		cplInrtOut.setBand_term(cplInrtOut.getListLayerInrt().get(0).getBand_term());
		cplInrtOut.setBank_base_inrt(cplInrtOut.getListLayerInrt().get(0).getBank_base_inrt());
		cplInrtOut.setEfft_inrt(cplInrtOut.getListLayerInrt().get(0).getEfft_inrt());

		return cplInrtOut;
	}

	/**
	 * 获取分层利率信息
	 * 
	 * @param cplIn
	 *            利率查询输入
	 * @param listInrtInfo
	 *            分层利率信息
	 * @return 存款利率信息输出
	 */
	private static DpInrtQryOut getLayerInrtInfo(DpInrtQryIn cplIn, Options<IoCmTierInrt> listInrtInfo) {

		// 输出
		DpInrtQryOut cplInrtOut = BizUtil.getInstance(DpInrtQryOut.class);

		cplInrtOut.setCcy_code(cplIn.getCcy_code());
		cplInrtOut.setInrt_code(cplIn.getInrt_code());
		cplInrtOut.setInrt_float_method(CommUtil.nvl(cplIn.getInrt_float_method(), E_FLOATWAY.NONE));
		cplInrtOut.setInrt_float_value(CommUtil.nvl(cplIn.getInrt_float_value(), BigDecimal.ZERO));

		// 层次数大于1表示非单层
		if (listInrtInfo.size() > 1) {

			cplInrtOut.setSingle_layer_ind(E_YESORNO.NO);

			for (IoCmTierInrt inrtInfo : listInrtInfo) {

				DpLayerInrt layerInrt = BizUtil.getInstance(DpLayerInrt.class);

				layerInrt.setBand_amount(inrtInfo.getBand_amount());
				layerInrt.setBand_term(inrtInfo.getTerm_code());
				layerInrt.setBank_base_inrt(inrtInfo.getInst_rate());
				layerInrt.setEfft_inrt(inrtInfo.getInst_rate());
				layerInrt.setLayer_no(inrtInfo.getSequence_no());

				if (cplIn.getInrt_float_method() == E_FLOATWAY.PERCENT) {

					BigDecimal addValue = inrtInfo.getInst_rate().multiply(cplIn.getInrt_float_value()).divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);

					layerInrt.setEfft_inrt(inrtInfo.getInst_rate().add(addValue));
				}
				else if (cplIn.getInrt_float_method() == E_FLOATWAY.VALUE) {
					layerInrt.setEfft_inrt(inrtInfo.getInst_rate().add(cplIn.getInrt_float_value()));
				}

				cplInrtOut.getListLayerInrt().add(layerInrt);
			}
		}
		else {

			cplInrtOut.setSingle_layer_ind(E_YESORNO.YES);
			cplInrtOut.setBand_amount(BigDecimal.ZERO);
			cplInrtOut.setBand_term(null);
			cplInrtOut.setBank_base_inrt(listInrtInfo.get(0).getInst_rate());
			cplInrtOut.setEfft_inrt(listInrtInfo.get(0).getInst_rate());

			if (cplIn.getInrt_float_method() == E_FLOATWAY.PERCENT) {

				BigDecimal addValue = cplInrtOut.getEfft_inrt().multiply(cplIn.getInrt_float_value()).divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);

				cplInrtOut.setEfft_inrt(cplInrtOut.getEfft_inrt().add(addValue));
			}
			else if (cplIn.getInrt_float_method() == E_FLOATWAY.VALUE) {
				cplInrtOut.setEfft_inrt(cplInrtOut.getEfft_inrt().add(cplIn.getInrt_float_value()));
			}
		}

		return cplInrtOut;
	}
}
