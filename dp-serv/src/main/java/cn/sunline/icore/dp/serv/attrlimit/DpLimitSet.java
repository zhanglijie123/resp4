package cn.sunline.icore.dp.serv.attrlimit;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApLimitApi;
import cn.sunline.icore.ap.tables.TabApAttribute.Apb_limit_statisDao;
import cn.sunline.icore.ap.tables.TabApAttribute.App_limitDao;
import cn.sunline.icore.ap.tables.TabApAttribute.apb_limit_statis;
import cn.sunline.icore.ap.tables.TabApAttribute.app_limit;
import cn.sunline.icore.ap.type.ComApAttr.ApCustomLimitInfo;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAttrLimitApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessIn;
import cn.sunline.icore.dp.base.type.ComDpToolsBase.DpAcctAccessOut;
import cn.sunline.icore.dp.serv.type.ComDpAttrLimit.DpCustomLimitSetIn;
import cn.sunline.icore.dp.serv.type.ComDpAttrLimit.DpQueryCustomLimitInfoIn;
import cn.sunline.icore.dp.serv.type.ComDpAttrLimit.DpQueryCustomLimitInfoOut;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.type.EnumType.E_DEMANDORTIME;
import cn.sunline.icore.sys.type.EnumType.E_OWNERLEVEL;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

public class DpLimitSet {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpLimitSet.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年8月29日-上午9:25:39</li>
	 *         <li>功能说明：负债自定义限额设置</li>
	 *         </p>
	 * @param cplIn
	 *            自定义限额设置输入
	 */
	public static void customLimitSet(DpCustomLimitSetIn cplIn) {

		// 非空要素检查
		BizUtil.fieldNotNull(cplIn.getLimit_no(), SysDict.A.limit_no.getId(), SysDict.A.limit_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getLimit_value(), SysDict.A.limit_value.getId(), SysDict.A.limit_value.getLongName());

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		// 根据限额编号查询出所有相关的限额信息
		List<app_limit> limitList = App_limitDao.selectAll_odb1(cplIn.getLimit_no(), false);

		for (app_limit limit : limitList) {

			if (!ApLimitApi.isValid(trxnDate, limit))
				continue;

			// 限额自定义许可检查
			if (limit.getLimit_custom_allow() == E_YESORNO.NO) {

				throw DpBase.E0365(limit.getLimit_no());
			}

			// 上下限额检查
			if (CommUtil.compare(limit.getMin_limit_value(), cplIn.getLimit_value()) > 0 || CommUtil.compare(limit.getMax_limit_value(), cplIn.getLimit_value()) < 0) {

				throw DpBase.E0367(cplIn.getLimit_value(), limit.getMin_limit_value(), limit.getMax_limit_value());
			}

			// 限额层级为客户
			if (limit.getLimit_level() == E_OWNERLEVEL.CUSTOMER) {

				ApCustomLimitInfo custLimtSet = BizUtil.getInstance(ApCustomLimitInfo.class);

				custLimtSet.setLimit_owner_id(cplIn.getCust_no());
				custLimtSet.setLimit_no(cplIn.getLimit_no());
				custLimtSet.setCustom_limit_value(cplIn.getLimit_value());

				DpAttrLimitApi.modifyCustomLimitValue(custLimtSet);

			}// 限额层级为卡层
			else if (limit.getLimit_level() == E_OWNERLEVEL.CARD) {

				ApCustomLimitInfo custLimitSet = BizUtil.getInstance(ApCustomLimitInfo.class);

				custLimitSet.setLimit_owner_id(cplIn.getAcct_no());
				custLimitSet.setLimit_no(cplIn.getLimit_no());
				custLimitSet.setCustom_limit_value(cplIn.getLimit_value());

				DpAttrLimitApi.modifyCustomLimitValue(custLimitSet);

			}// 限额层级为账户
			else if (limit.getLimit_level() == E_OWNERLEVEL.ACCOUNT) {

				// 定位客户账户: 带锁查询
				DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

				ApCustomLimitInfo custLimtSet = BizUtil.getInstance(ApCustomLimitInfo.class);

				custLimtSet.setLimit_owner_id(acctInfo.getAcct_no());
				custLimtSet.setLimit_no(cplIn.getLimit_no());
				custLimtSet.setCustom_limit_value(cplIn.getLimit_value());

				DpAttrLimitApi.modifyCustomLimitValue(custLimtSet);

			}// 限额层级为子账户
			else if (limit.getLimit_level() == E_OWNERLEVEL.SUB_ACCTOUNT) {

				// 子账户定位输入接口
				DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

				acctAccessIn.setAcct_no(cplIn.getAcct_no());
				acctAccessIn.setAcct_type(cplIn.getAcct_type());
				acctAccessIn.setCcy_code(limit.getLimit_ccy());
				acctAccessIn.setProd_id(cplIn.getProd_id());
				acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

				// 获取存款子账户信息
				DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

				ApCustomLimitInfo custLimtSet = BizUtil.getInstance(ApCustomLimitInfo.class);

				custLimtSet.setLimit_owner_id(acctAccessOut.getSub_acct_no());
				custLimtSet.setLimit_no(cplIn.getLimit_no());
				custLimtSet.setCustom_limit_value(cplIn.getLimit_value());

				DpAttrLimitApi.modifyCustomLimitValue(custLimtSet);

			}
			else {
				throw DpBase.E0368(limit.getLimit_no());
			}

		}
		
		// 限额修改发送邮件
		registerChangeLimitMail(cplIn, limitList);
	}

	/**
	 * @Author hongb
	 *         <p>
	 *         <li>2018年8月8日-下午1:31:54</li>
	 *         <li>功能说明：限额定义邮件登记</li>
	 *         </p>
	 * @param cplIn
	 *            限额定义输入接口
	 * @param limitList
	 *            限额列表
	 */
	private static void registerChangeLimitMail(DpCustomLimitSetIn cplIn, List<app_limit> limitList) {
		bizlog.method(" DpLimitSet.registerChangeLimitMail begin >>>>>>>>>>>>>>>>");
		// TODO Administrator Auto-generated method stub

		bizlog.method(" DpLimitSet.registerChangeLimitMail end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年8月30日-上午8:36:07</li>
	 *         <li>功能说明：负债自定义限额查询</li>
	 *         </p>
	 * @param DpQueryCustomLimitInfoIn
	 *            自定义限额查询输入
	 * @return DpQueryCustomLimitInfoOut 自定义限额查询输出
	 */
	public static DpQueryCustomLimitInfoOut queryCustomLimitInfo(DpQueryCustomLimitInfoIn cplIn) {

		// 非空要素检查
		BizUtil.fieldNotNull(cplIn.getLimit_no(), SysDict.A.limit_no.getId(), SysDict.A.limit_no.getLongName());

		String trxnDate = BizUtil.getTrxRunEnvs().getTrxn_date();

		DpQueryCustomLimitInfoOut cplOut = BizUtil.getInstance(DpQueryCustomLimitInfoOut.class);

		// 根据限额编号查询出所有相关的限额信息
		List<app_limit> limitList = App_limitDao.selectAll_odb1(cplIn.getLimit_no(), false);

		String ownerId = "";

		for (app_limit limit : limitList) {

			if (!ApLimitApi.isValid(trxnDate, limit))
				continue;

			// 限额层级为客户
			if (limit.getLimit_level() == E_OWNERLEVEL.CUSTOMER) {

				ownerId = cplIn.getCust_no();

			}// 限额层级为卡层
			else if (limit.getLimit_level() == E_OWNERLEVEL.CARD) {

				ownerId = cplIn.getAcct_no();

			}// 限额层级为账户
			else if (limit.getLimit_level() == E_OWNERLEVEL.ACCOUNT) {

				// 定位客户账户: 带锁查询
				DpaAccount acctInfo = DpToolsApi.locateSingleAccount(cplIn.getAcct_no(), cplIn.getAcct_type(), true);

				ownerId = acctInfo.getAcct_no();
			}// 限额层级为子账户
			else if (limit.getLimit_level() == E_OWNERLEVEL.SUB_ACCTOUNT) {

				// 子账户定位输入接口
				DpAcctAccessIn acctAccessIn = BizUtil.getInstance(DpAcctAccessIn.class);

				acctAccessIn.setAcct_no(cplIn.getAcct_no());
				acctAccessIn.setAcct_type(cplIn.getAcct_type());
				acctAccessIn.setCcy_code(limit.getLimit_ccy());
				acctAccessIn.setDd_td_ind(E_DEMANDORTIME.DEMAND);

				// 获取存款子账户信息
				DpAcctAccessOut acctAccessOut = DpToolsApi.locateSingleSubAcct(acctAccessIn);

				ownerId = acctAccessOut.getSub_acct_no();

			}
			else {
				throw DpBase.E0368(limit.getLimit_no());
			}

			// 获取限额值
			BigDecimal limitValue = ApLimitApi.getCustomLimit(limit, ownerId);

			cplOut.setUsed_limit(BigDecimal.ZERO); // 已用限额

			// 带锁查询,防止脏读
			apb_limit_statis limitStatis = Apb_limit_statisDao.selectOneWithLock_odb1(ownerId, limit.getLimit_statis_no(), false);

			if (CommUtil.isNotNull(limitStatis)) {
				// 在同一个统计周期
				if (CommUtil.isNull(limit.getLimit_reset_cycle())
						|| CommUtil.compare(limitStatis.getLimit_reset_date(), ApLimitApi.getResetDate(limit.getLimit_reset_cycle(), trxnDate)) == 0) {
					cplOut.setUsed_limit(limitStatis.getUsed_limit()); // 已用限额
				}
				else {
					cplOut.setUsed_limit(BigDecimal.ZERO);
				}
			}

			cplOut.setLimit_ccy(limit.getLimit_ccy()); // 限额币种
			cplOut.setLimit_value(limitValue); // 限额值
			cplOut.setMax_limit_value(limit.getMax_limit_value()); // 限额最大值
			cplOut.setMin_limit_value(limit.getMin_limit_value()); // 限额最小值
			cplOut.setLimit_reset_cycle(limit.getLimit_reset_cycle()); // 限额重置周期
			
			BigDecimal usedLimitValue = cplOut.getUsed_limit();
			cplOut.setRemain_limit(limitValue.subtract(usedLimitValue));//剩余限额
			
		}

		return cplOut;
	}
}
