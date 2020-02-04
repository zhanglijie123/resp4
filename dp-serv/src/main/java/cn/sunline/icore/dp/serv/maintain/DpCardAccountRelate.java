package cn.sunline.icore.dp.serv.maintain;

import java.util.List;

import cn.sunline.clwj.msap.core.parameter.MsOrg;
import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApAcctRouteApi;
import cn.sunline.icore.ap.api.ApAttributeApi;
import cn.sunline.icore.ap.api.ApBusinessParmApi;
import cn.sunline.icore.ap.api.ApSystemParmApi;
import cn.sunline.icore.ap.namedsql.ApAttributeDao;
import cn.sunline.icore.ap.type.ComApAttr.ApAttrListOut;
import cn.sunline.icore.ap.type.ComApAttr.ApAttrListResult;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpAttrLimitApi;
import cn.sunline.icore.dp.base.api.DpBaseConst;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpToolsApi;
import cn.sunline.icore.dp.base.dict.DpBaseDict;
import cn.sunline.icore.dp.base.errors.DpBaseErr.DpBase;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCard;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardNew;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaCardNewDao;
import cn.sunline.icore.dp.base.type.ComDpAttrLimitBase.DpCommonAttributeSet;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_CARDACCTMDY;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_CARDSTATUS;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.froze.DpUnFroze;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpAcctQueryDao;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpCommonDao;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctInfo;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctQueryIn;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctQueryOut;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctSetIn;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctSetInfo;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpCardAcctSetOut;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpSwitchCardIn;
import cn.sunline.icore.dp.serv.type.ComDpCardAccount.DpSwitchCardOut;
import cn.sunline.icore.dp.serv.type.ComDpQueryAcct.DpCardStatusList;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_CARDACCTQUERYWAY;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_CARDACCTSET;
import cn.sunline.icore.dp.sys.type.DpSysEnumType.E_FROZEOBJECT;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_ACCTLIMITSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_ACCTROUTETYPE;
import cn.sunline.icore.sys.type.EnumType.E_ACCTSTATUS;
import cn.sunline.icore.sys.type.EnumType.E_CUSTOMERTYPE;
import cn.sunline.icore.sys.type.EnumType.E_OWNERLEVEL;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.lang.Page;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;
import cn.sunline.ltts.core.api.model.dm.Options;
import cn.sunline.ltts.core.api.model.dm.internal.DefaultOptions;

/**
 * <p>
 * 文件功能说明： 卡账关系
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年3月23日-下午3:46:26</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月23日-zhoumy：创建注释模板</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpCardAccountRelate {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpCardAccountRelate.class);

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2017年1月7日-下午1:58:31</li>
	 *         <li>功能说明：卡升级或降级</li>
	 *         </p>
	 * @param custNo
	 *            客户号
	 * @param upgradeOrDegrade
	 *            升级或降级 ： YES-升级 NO-降级
	 */
	public static void cardUpgradeOrDegrade(String custNo, E_YESORNO upgradeOrDegrade) {

		bizlog.method(" DpCardAccountRelate.cardUpgradeOrDegrade begin >>>>>>>>>>>>>>>>");

		/*
		 * 卡模块处理 String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();
		 * E_YESORNO oldGrade = (upgradeOrDegrade == E_YESORNO.YES) ?
		 * E_YESORNO.NO : E_YESORNO.YES;
		 * 
		 * // CIMB需求： 客户开借记卡，客户下的预付卡升级标志设为“YES” List<DpaCard> listCardInfo =
		 * DpCardAcctRelateDao.selUpgradeOrDegradePrepaidCard(custNo, orgId,
		 * E_CARDSTATUS.NORMAL, oldGrade.getValue(), false);
		 * 
		 * for (DpaCard cardInfo : listCardInfo) {
		 * 
		 * // 卡升级标志已经是维护后的值 if (cardInfo.getCard_upgraded_ind() ==
		 * upgradeOrDegrade) { continue; }
		 * 
		 * // 更新卡信息之前先带锁读取 DpaCard cardData =
		 * DpaCardDao.selectOneWithLock_odb1(cardInfo.getCard_no(), true);
		 * 
		 * cardData.setCard_upgraded_ind(upgradeOrDegrade);
		 * 
		 * DpaCardDao.updateOne_odb1(cardData); }
		 */

		bizlog.method(" DpCardAccountRelate.cardUpgradeOrDegrade end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月6日-上午9:55:32</li>
	 *         <li>功能说明：附属卡卡账管理</li>
	 *         </p>
	 * @param cplIn
	 *            卡账关系设置输入
	 * @return DpCardAcctSetOut 卡账关系设置输出
	 */
	public static DpCardAcctSetOut cardAccountSet(DpCardAcctSetIn cplIn) {
		bizlog.method(" DpCardAccountRelate.cardAccountSet begin >>>>>>>>>>>>>>>>");
		bizlog.parm(">>>>>>cplIn=[%s]", cplIn);

		// 必输性检查
		BizUtil.fieldNotNull(cplIn.getCard_no(), SysDict.A.card_no.getId(), SysDict.A.card_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

		// 获取卡账关系设置信息
		Options<DpCardAcctSetInfo> options = cplIn.getList01();

		// 查询附属卡信息: 第一次绑账户，卡号在核心没记录
		DpaCard cardInfo = DpaCardDao.selectOne_odb1(cplIn.getCard_no(), false);

		// 默认第一条记录的账号为默认账号
		String defaultAccount = options.get(0).getAcct_no();

		// 附属卡账管理检查
		for (DpCardAcctSetInfo cardSetInfo : options) {

			// 必输检查
			BizUtil.fieldNotNull(cardSetInfo.getCard_acct_set_type(), DpDict.A.card_acct_set_type.getId(), DpDict.A.card_acct_set_type.getLongName());
			BizUtil.fieldNotNull(cardSetInfo.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

			DpaAccount account = DpaAccountDao.selectOne_odb1(cardSetInfo.getAcct_no(), false);

			if (CommUtil.isNull(account)) {
				throw DpErr.Dp.E0004(cardSetInfo.getAcct_no());
			}
			else if (account.getAcct_status() == E_ACCTSTATUS.CLOSE) {
				throw DpBase.E0008(cardSetInfo.getAcct_no());
			}

			// 客户号不一致
			if (!CommUtil.equals(account.getCust_no(), cplIn.getCust_no())) {
				// TODO:
			}

			// 查询卡账关系对照表
			DpaCardAccount cardAccount = DpaCardAccountDao.selectOne_odb1(cplIn.getCard_no(), cardSetInfo.getAcct_no(), false);

			// 卡账关系设置类型为1则解除绑定
			if (cardSetInfo.getCard_acct_set_type() == E_CARDACCTSET.RELIEVE) {

				// 第一次做附属卡管理只能是绑定
				if (CommUtil.isNull(cardInfo)) {
					// TODO: throw
				}

				// 卡账关系表不存在这条记录则报错
				if (CommUtil.isNull(cardAccount)) {
					throw APPUB.E0024(OdbFactory.getTable(DpaCardAccount.class).getLongname(), SysDict.A.card_no.getLongName(), cplIn.getCard_no(),
							SysDict.A.acct_no.getLongName(), cardSetInfo.getAcct_no());
				}

				// 查询是否为卡下默认账户，若为默认账户则需最后解除
				if (CommUtil.equals(cardSetInfo.getAcct_no(), cardInfo.getAcct_no())) {

					// 查询卡下所有绑定的账号
					List<DpaCardAccount> list = DpaCardAccountDao.selectAll_odb2(cplIn.getCard_no(), false);

					if (list.size() > 1) {
						throw DpErr.Dp.E0277(cardSetInfo.getAcct_no(), cplIn.getCard_no());
					}
				}
			}
			else { // 绑定

				BizUtil.fieldNotNull(cardSetInfo.getSet_default_acct_ind(), DpDict.A.set_default_acct_ind.getId(), DpDict.A.set_default_acct_ind.getLongName());

				// 卡账关系表存在这条记录则报错
				if (CommUtil.isNotNull(cardAccount)) {
					throw APPUB.E0032(OdbFactory.getTable(DpaCardAccount.class).getLongname(), cardSetInfo.getAcct_no(), cplIn.getCard_no());
				}

				if (cardSetInfo.getSet_default_acct_ind() == E_YESORNO.YES) {
					defaultAccount = cardSetInfo.getAcct_no();
				}
			}
		}

		// 附属卡卡账管理处理
		for (DpCardAcctSetInfo cardSetInfo : options) {

			// 解除绑定
			if (cardSetInfo.getCard_acct_set_type() == E_CARDACCTSET.RELIEVE) {

				DpaCardAccountDao.deleteOne_odb1(cplIn.getCard_no(), cardSetInfo.getAcct_no());

				// 登记卡账关系变更登记簿
				if (CommUtil.equals(cardSetInfo.getAcct_no(), cardInfo.getAcct_no())) {

					DpBaseServiceApi.registerCardChangeBook(cplIn.getCard_no(), cardSetInfo.getAcct_no(), E_CARDACCTMDY.CLOSE_DEFAULT);
				}
				else {

					DpBaseServiceApi.registerCardChangeBook(cplIn.getCard_no(), cardSetInfo.getAcct_no(), E_CARDACCTMDY.CLOSE);
				}
			}
			else { // 绑定

				// 设置卡账关系
				DpaCardAccount cardAccount = BizUtil.getInstance(DpaCardAccount.class);

				cardAccount.setAcct_no(cardSetInfo.getAcct_no());
				cardAccount.setCard_no(cplIn.getCard_no());

				DpaCardAccountDao.insert(cardAccount);

				// 登记卡账关系变更登记簿
				if (cardSetInfo.getSet_default_acct_ind() == E_YESORNO.YES) {

					DpBaseServiceApi.registerCardChangeBook(cplIn.getCard_no(), cardSetInfo.getAcct_no(), E_CARDACCTMDY.CREATE_DEFAULT);
				}
				else {

					DpBaseServiceApi.registerCardChangeBook(cplIn.getCard_no(), cardSetInfo.getAcct_no(), E_CARDACCTMDY.CREATE);
				}
			}
		}

		// 卡信息尚未登记则登记
		if (CommUtil.isNull(cardInfo)) {

			cardInfo = BizUtil.getInstance(DpaCard.class);

			cardInfo.setCard_no(cplIn.getCard_no());
			cardInfo.setCust_no(cplIn.getCust_no());
			cardInfo.setCust_type(E_CUSTOMERTYPE.PERSONAL);
			cardInfo.setAcct_no(defaultAccount);
			cardInfo.setCard_limit_status(E_ACCTLIMITSTATUS.NONE);
			cardInfo.setAttr_value(ApAttributeApi.getAttrDefineString(E_OWNERLEVEL.CARD));
			cardInfo.setCard_status(E_CARDSTATUS.NORMAL);

			DpaCardDao.insert(cardInfo);

			ApAcctRouteApi.register(cplIn.getCard_no(), E_ACCTROUTETYPE.CARD);
		}
		else {

			// 更新默认账号
			if (!CommUtil.equals(defaultAccount, cardInfo.getAcct_no())) {

				cardInfo.setAcct_no(defaultAccount);

				DpaCardDao.updateOne_odb1(cardInfo);
			}
		}

		String cardAttr = cardInfo.getAttr_value();

		// 卡属性设置
		if (CommUtil.isNotNull(cplIn.getList02()) && cplIn.getList02().size() > 0) {

			// 再查询一遍
			cardInfo = DpaCardDao.selectOne_odb1(cplIn.getCard_no(), true);

			DpCommonAttributeSet cplCommonSetIn = BizUtil.getInstance(DpCommonAttributeSet.class);

			cplCommonSetIn.setAttr_level(E_OWNERLEVEL.CARD);
			cplCommonSetIn.setAttr_owner_id(cplIn.getCard_no());
			cplCommonSetIn.setData_version(cardInfo.getData_version());
			cplCommonSetIn.setListattr(cplIn.getList02());

			ApAttrListResult cplCommonSetResult = DpAttrLimitApi.modifyAttr(cplCommonSetIn);

			// 更新后的属性
			cardAttr = cplCommonSetResult.getAttr_value();
		}

		// 卡账关系查询
		DpCardAcctQueryIn queryIn = BizUtil.getInstance(DpCardAcctQueryIn.class);

		queryIn.setAcct_no(cplIn.getCard_no());
		queryIn.setCard_acct_query_way(E_CARDACCTQUERYWAY.CARD);

		DpCardAcctQueryOut queryOut = cardAccountQuery(queryIn);

		// 属性值转换为列表展现
		Options<ApAttrListOut> attrList = DpAttrLimitApi.attrValueSwitch(E_OWNERLEVEL.CARD, cplIn.getCard_no(), cardAttr);

		// 输出
		DpCardAcctSetOut cplOut = BizUtil.getInstance(DpCardAcctSetOut.class);

		cplOut.setCust_no(cplIn.getCust_no());// 客户号
		cplOut.setCard_no(cplIn.getCard_no());// 卡号
		cplOut.setList01(queryOut.getList01());
		cplOut.setList02(attrList);

		bizlog.method(" DpCardAccountRelate.cardAccountSet end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月6日-上午9:55:32</li>
	 *         <li>功能说明：换卡</li>
	 *         <li>补充说明：此服务给对接第三方卡系统时使用，必定是换卡又换号</li>
	 *         </p>
	 * @param cplIn
	 *            换卡输入
	 * @return 换卡输出
	 */
	public static DpSwitchCardOut switchCard(DpSwitchCardIn cplIn) {

		bizlog.method(" DpCardAccountRelate.switchCard begin >>>>>>>>>>>>>>>>");
		bizlog.parm(">>>>>>cplIn=[%s]", cplIn);

		// 1.1 卡号、新卡必输
		BizUtil.fieldNotNull(cplIn.getCard_no(), SysDict.A.card_no.getId(), SysDict.A.card_no.getLongName());
		BizUtil.fieldNotNull(cplIn.getNew_card_no(), DpBaseDict.A.new_card_no.getId(), DpBaseDict.A.new_card_no.getLongName());

		// 1.2 查询旧卡信息: 带锁
		DpaCard card = DpaCardDao.selectOneWithLock_odb1(cplIn.getCard_no(), false);

		if (CommUtil.isNull(card)) {
			throw APPUB.E0005(OdbFactory.getTable(DpaCard.class).getLongname(), SysDict.A.card_no.getLocalLongName(), cplIn.getCard_no());
		}

		// 1.3 查询新卡信息
		DpaCard newCard = DpaCardDao.selectOne_odb1(cplIn.getNew_card_no(), false);

		// 卡信息表不能存在在新卡号，否则报错
		if (!CommUtil.isNull(newCard)) {
			throw APPUB.E0019(OdbFactory.getTable(DpaCard.class).getLongname(), cplIn.getNew_card_no());
		}

		// 2.1 拷贝旧卡信息入新卡
		newCard = BizUtil.clone(DpaCard.class, card);

		newCard.setCard_no(cplIn.getNew_card_no());

		DpaCardDao.insert(newCard);

		// 2.2 新增新旧卡号对照表
		DpaCardNew cardNew = BizUtil.getInstance(DpaCardNew.class);

		cardNew.setCard_no(cplIn.getCard_no());
		cardNew.setNew_card_no(cplIn.getNew_card_no());

		DpaCardNewDao.insert(cardNew);

		// 2.3 更新旧新卡对照表中原登记记录
		SqlDpCommonDao.updateCardNew(card.getOrg_id(), cplIn.getCard_no(), cplIn.getNew_card_no());

		// 2.4.add the DpaCardAccount
		List<DpaCardAccount> listOrgAcct = DpaCardAccountDao.selectAll_odb2(cplIn.getCard_no(), false);

		for (DpaCardAccount orgAcct : listOrgAcct) {

			orgAcct.setCard_no(cplIn.getNew_card_no());

			DpaCardAccountDao.insert(orgAcct);
		}

		// 2.5 更新卡属性到期日登记簿: 卡号变更了，登记簿也要变
		ApAttributeDao.updateAttrDueOwnerId(card.getOrg_id(), E_OWNERLEVEL.CARD, cplIn.getCard_no(), cplIn.getNew_card_no());

		// 2.6 登记路由
		ApAcctRouteApi.register(cplIn.getNew_card_no(), E_ACCTROUTETYPE.CARD);

		// 3 自助冻结解冻
		String frozeChannel = ApBusinessParmApi.getValue("SELF_FROZEN_THAW_CHANNEL");

		// 调用自助解冻
		if (frozeChannel.contains(BizUtil.getTrxRunEnvs().getChannel_id())) {
			DpUnFroze.selfUnFrozen(cplIn.getCard_no(), E_FROZEOBJECT.CARD, frozeChannel);
		}

		// 4.输出设置
		DpSwitchCardOut cplOut = BizUtil.getInstance(DpSwitchCardOut.class);

		cplOut.setCust_no(card.getCust_no());// 客户号
		cplOut.setCard_no(card.getCard_no());// 卡号
		cplOut.setNew_card_no(cplIn.getNew_card_no());

		bizlog.method(" DpCardAccountRelate.switchCard end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月6日-下午2:37:20</li>
	 *         <li>功能说明：卡账关系查询</li>
	 *         </p>
	 * @param cplIn
	 *            卡账关系查询输入
	 * @return
	 */
	public static DpCardAcctQueryOut cardAccountQuery(DpCardAcctQueryIn cplIn) {
		bizlog.method(" DpCardAccountRelate.cardAccountQuery begin >>>>>>>>>>>>>>>>");

		// 卡账关系查询方式必输
		BizUtil.fieldNotNull(cplIn.getCard_acct_query_way(), DpDict.A.card_acct_query_way.getId(), DpDict.A.card_acct_query_way.getLongName());

		// 账号必输
		BizUtil.fieldNotNull(cplIn.getAcct_no(), SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLongName());

		// 定义输出接口
		DpCardAcctQueryOut cplOut = null;

		// 卡号查询方式
		if (cplIn.getCard_acct_query_way() == E_CARDACCTQUERYWAY.CARD) {

			cplOut = queryByCardNo(cplIn.getAcct_no());
		}
		else {

			cplOut = queryByAcctNo(cplIn.getAcct_no(), cplIn.getAcct_type());
		}

		bizlog.method(" DpCardAccountRelate.cardAccountQuery end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月10日-上午10:59:24</li>
	 *         <li>功能说明：根据账号查询卡账关系</li>
	 *         </p>
	 * @param cplIn
	 *            卡账关系查询输入
	 * @return DpCardAcctQueryOut 卡账关系查询输出
	 */
	private static DpCardAcctQueryOut queryByAcctNo(String acctNo, String acctType) {
		bizlog.method(" DpCardAccountRelate.queryByAcctNo begin >>>>>>>>>>>>>>>>");

		// 初始化输出接口
		DpCardAcctQueryOut cplOut = BizUtil.getInstance(DpCardAcctQueryOut.class);

		// 定位账号
		DpaAccount account = DpToolsApi.locateSingleAccount(acctNo, acctType, false);

		// 设置客户号
		cplOut.setCust_no(account.getCust_no());

		Options<DpCardAcctInfo> list01 = cplOut.getList01();

		// 查询卡账关系表
		List<DpaCardAccount> list = DpaCardAccountDao.selectAll_odb3(account.getAcct_no(), false);

		for (DpaCardAccount cardAccount : list) {

			// 初始化卡账关系信息
			DpCardAcctInfo cardAcctInfo = BizUtil.getInstance(DpCardAcctInfo.class);

			cardAcctInfo.setCard_no(cardAccount.getCard_no());// 卡号
			cardAcctInfo.setAcct_no(account.getAcct_no());// 账号
			cardAcctInfo.setAcct_type(account.getAcct_type());
			cardAcctInfo.setAcct_name(account.getAcct_name());// 账户名称
			cardAcctInfo.setAcct_status(account.getAcct_status());// 账户状态
			cardAcctInfo.setData_version(account.getData_version());// 数据库版本号

			list01.add(cardAcctInfo);
		}

		cplOut.setList01(list01);

		bizlog.method(" DpCardAccountRelate.queryByAcctNo end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author shenxy
	 *         <p>
	 *         <li>2017年7月10日-上午11:00:04</li>
	 *         <li>功能说明：根据卡号查询卡账关系</li>
	 *         </p>
	 * @param cardNo
	 *            卡号
	 * @return DpCardAcctQueryOut 卡账关系查询输出
	 */
	private static DpCardAcctQueryOut queryByCardNo(String cardNo) {
		bizlog.method(" DpCardAccountRelate.queryByCardNo begin >>>>>>>>>>>>>>>>");

		// 初始化输出接口
		DpCardAcctQueryOut cplOut = BizUtil.getInstance(DpCardAcctQueryOut.class);

		String queryCardNo = cardNo; // 查询卡号

		// 查询卡信息
		DpaCard card = DpaCardDao.selectOne_odb1(cardNo, false);

		// 查不到卡则报错
		if (CommUtil.isNull(card)) {

			DpaCardNew cardNew = DpaCardNewDao.selectOne_odb1(cardNo, false);

			if (CommUtil.isNull(cardNew)) {
				throw DpErr.Dp.E0004(cardNo);
			}

			queryCardNo = cardNew.getNew_card_no();
		}

		// 再次查询卡信息
		card = DpaCardDao.selectOne_odb1(queryCardNo, false);

		// 查询默认账户信息
		DpaAccount defaultAcct = DpaAccountDao.selectOne_odb1(card.getAcct_no(), false);

		if (CommUtil.isNull(defaultAcct)) {
			throw DpErr.Dp.E0004(card.getAcct_no());
		}

		cplOut.setAcct_no(defaultAcct.getAcct_no());
		cplOut.setAcct_type(defaultAcct.getAcct_type());
		cplOut.setCust_no(card.getCust_no());
		Options<DpCardAcctInfo> list01 = cplOut.getList01();

		// 查询卡下所有账号
		List<DpaCardAccount> cardAccounts = DpaCardAccountDao.selectAll_odb2(queryCardNo, false);

		for (DpaCardAccount cardAccount : cardAccounts) {

			DpaAccount account = DpaAccountDao.selectOne_odb1(cardAccount.getAcct_no(), false);

			if (CommUtil.isNull(account)) {
				throw DpErr.Dp.E0004(cardAccount.getAcct_no());
			}

			// 初始化卡账关系信息
			DpCardAcctInfo cardAcctInfo = BizUtil.getInstance(DpCardAcctInfo.class);

			cardAcctInfo.setCard_no(queryCardNo);// 卡号
			cardAcctInfo.setAcct_no(account.getAcct_no());// 账号
			cardAcctInfo.setAcct_type(account.getAcct_type());
			cardAcctInfo.setAcct_name(account.getAcct_name());// 账户名称
			cardAcctInfo.setAcct_status(account.getAcct_status());// 账户状态
			cardAcctInfo.setData_version(cardAccount.getData_version());// 数据库版本号

			list01.add(cardAcctInfo);
		}

		cplOut.setList01(list01);
		bizlog.method(" DpCardAccountRelate.queryByCardNo end <<<<<<<<<<<<<<<<");
		return cplOut;
	}

	/**
	 * @Author huangjj
	 *         <p>
	 *         <li>2017年12月7日-下午8:02:15</li>
	 *         <li>功能说明：空卡数据查询</li>
	 *         </p>
	 * @param card_no
	 *            卡号
	 * @param cust_no
	 *            客户号
	 * @return DpQryEmptyCardOut 空卡数据输出
	 */
	public static Options<DpCardStatusList> qryEmptyCard(String cardNo, String custNo) {
		bizlog.method(" DpCardAccountRelate.qryEmptyCard begin >>>>>>>>>>>>>>>>");

		// 动态sql查询空卡数据信息
		// RunEnvs runEnvs = BizUtil.getTrxRunEnvs();
		String orgId = BizUtil.getTrxRunEnvs().getBusi_org_id();

		Page<DpCardStatusList> page = SqlDpAcctQueryDao.selEmptyCardStatusList(orgId, cardNo, custNo, BizUtil.getTrxRunEnvs().getPage_start(), BizUtil.getTrxRunEnvs()
				.getPage_size(), BizUtil.getTrxRunEnvs().getTotal_count(), false);

		BizUtil.getTrxRunEnvs().setTotal_count(page.getPageCount());

		// 初始化输出接口
		Options<DpCardStatusList> cardStatusList = new DefaultOptions<DpCardStatusList>();

		cardStatusList.setValues(page.getRecords());

		bizlog.method(" DpCardAccountRelate.qryEmptyCard end <<<<<<<<<<<<<<<<");
		return cardStatusList;
	}

	/**
	 * @Author huangjj
	 *         <p>
	 *         <li>2017年12月11日-下午5:19:06</li>
	 *         <li>功能说明：银行卡手工关闭</li>
	 *         </p>
	 * @param cardNo
	 *            卡号
	 */
	public static void cardClosedByHand(String cardNo) {

		bizlog.method(" DpCardAccountRelate.cardClosedByHand begin >>>>>>>>>>>>>>>>");
		/*
		 * 卡模块处理 // 查询卡信息 DpaCard cardInfo = DpaCardDao.selectOne_odb1(cardNo,
		 * false);
		 * 
		 * // 查不到卡则报错 if (CommUtil.isNull(cardInfo)) {
		 * 
		 * DpaCardNew cardNew = DpaCardNewDao.selectOne_odb1(cardNo, false);
		 * 
		 * if (CommUtil.isNull(cardNew)) { throw DpErr.Dp.E0004(cardNo); }
		 * 
		 * // 通过新卡号查询卡信息 cardInfo =
		 * DpaCardDao.selectOne_odb1(cardNew.getNew_card_no(), true); }
		 * 
		 * // 如果该卡 卡状态已经关闭，报错 if (cardInfo.getCard_status() !=
		 * E_CARDSTATUS.NORMAL) {
		 * 
		 * throw DpErr.Dp.E0409(cardNo); }
		 * 
		 * // 获取默认账户 DpaAccount acct =
		 * DpaAccountDao.selectOne_odb1(cardInfo.getAcct_no(), true);
		 * 
		 * // 默认账户的账户状态为正常，报错 if (acct.getAcct_status() == E_ACCTSTATUS.NORMAL)
		 * {
		 * 
		 * throw DpErr.Dp.E0410(cardNo); }
		 * 
		 * // 更新卡信息 cardInfo.setCard_status(E_CARDSTATUS.CLOSE);
		 * 
		 * DpaCardDao.updateOne_odb1(cardInfo);
		 */

		bizlog.method(" DpCardAccountRelate.cardClosedByHand end <<<<<<<<<<<<<<<<");

	}

	/**
	 * @Author wengxt
	 *         <p>
	 *         <li>2018年4月27日-下午5:22:39</li>
	 *         <li>功能说明：卡账关系变更</li>
	 *         <li>补充说明：系统内对接卡模块使用</li>
	 *         </p>
	 * @param card_no
	 * @param new_card_no
	 */
	public static void cardAccountChange(String cardNo, String newCard) {
		bizlog.method("DpCardAccountRelate.cardAccountChange begin >>>>>>>>>>>>>>");
		bizlog.debug("cardNo[%s],newCard[%s]", cardNo, newCard);

		// 1. add the DpaCardNew
		DpaCardNew cardNew = BizUtil.getInstance(DpaCardNew.class);

		cardNew.setCard_no(cardNo);
		cardNew.setNew_card_no(newCard);

		DpaCardNewDao.insert(cardNew);

		// 2.change the card and account relation
		SqlDpCommonDao.updateCardNew(MsOrg.getReferenceOrgId(DpaCardNew.class), cardNo, newCard);

		// 3.add the DpaCardAccount
		List<DpaCardAccount> listOrgAcct = DpaCardAccountDao.selectAll_odb2(cardNo, false);

		for (DpaCardAccount orgAcct : listOrgAcct) {

			orgAcct.setCard_no(newCard);

			DpaCardAccountDao.insert(orgAcct);
		}

		bizlog.method("DpCardAccountRelate.cardAccountChange end >>>>>>>>>>>>>>");

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2018年5月21日-下午7:32:56</li>
	 *         <li>功能说明：绑定卡账关系</li>
	 *         </p>
	 * @param cardNo
	 * @param acctNo
	 * @param acctType
	 */
	public static void CardRelatedAccount(String cardNo, String acctNo, String acctType) {

		// 输入检查
		BizUtil.fieldNotNull(cardNo, SysDict.A.card_no.getId(), SysDict.A.card_no.getLocalLongName());
		BizUtil.fieldNotNull(acctNo, SysDict.A.acct_no.getId(), SysDict.A.acct_no.getLocalLongName());

		DpaAccount acctInfo = DpaAccountDao.selectOne_odb1(acctNo, false);

		if (acctInfo == null) {

			throw ApPubErr.APPUB.E0005(OdbFactory.getTable(DpaAccount.class).getLongname(), SysDict.A.acct_no.getLongName(), acctNo);

		}

		if (CommUtil.isNotNull(acctType) && CommUtil.compare(acctType, acctInfo.getAcct_type()) != 0) {

			// 检查账户类型
			DpAccountTypeParmApi.getAcctTypeInfo(acctType);

			acctInfo.setAcct_type(acctType);

			DpaAccountDao.updateOne_odb1(acctInfo);
		}

		// 登记卡账关系
		if (ApSystemParmApi.exists(DpBaseConst.Third_Party_Card_System) && CommUtil.equals(ApSystemParmApi.getValue(DpBaseConst.Third_Party_Card_System), E_YESORNO.YES.getValue())) {
			DpBaseServiceApi.registerCardInfo(acctInfo, cardNo, null);
		}
	}

}
