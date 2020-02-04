package cn.sunline.icore.dp.serv.account.open;

import java.util.List;
import java.util.Random;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.api.ApBranchApi;
import cn.sunline.icore.ap.api.ApBufferApi;
import cn.sunline.icore.ap.api.ApSeqApi;
import cn.sunline.icore.ap.util.ApConst;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.api.DpAccountTypeParmApi;
import cn.sunline.icore.dp.base.api.DpBaseServiceApi;
import cn.sunline.icore.dp.base.api.DpTechParmApi;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountDao;
import cn.sunline.icore.dp.base.tables.TabDpAccountType.DppAccountType;
import cn.sunline.icore.dp.base.tables.TabDpTechPara.DppConditionFeature;
import cn.sunline.icore.dp.base.type.DpBaseEnumType.E_VOCHREFLEVEL;
import cn.sunline.icore.dp.serv.attrlimit.DpAttrRefresh;
import cn.sunline.icore.dp.serv.common.DpConst;
import cn.sunline.icore.dp.serv.dict.DpDict;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.dp.serv.iobus.DpOtherIobus;
import cn.sunline.icore.dp.serv.iobus.DpVoucherIobus;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpCommonDao;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpbAccountCust;
import cn.sunline.icore.dp.serv.tables.TabDpBusiMain.DpbAccountCustDao;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbAccountSignature;
import cn.sunline.icore.dp.serv.tables.TabDpRegister.DpbAccountSignatureDao;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpOpenAccountIn;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpOpenAccountOut;
import cn.sunline.icore.dp.serv.type.ComDpOpenAccount.DpOpenAccountSignature;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_SELFOPTNUMBERIND;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

//import cn.sunline.ltts.aplt.sms.ApSms;

/**
 * <p>
 * 文件功能说明：负债开户
 * </p>
 * 
 * @Author HongBiao
 *         <p>
 *         <li>2017年1月6日-下午5:07:42</li>
 *         <li>开户服务相关代码</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpOpenAccount {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpOpenAccount.class);

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年1月7日-上午10:02:46</li>
	 *         <li>功能说明：负债开户</li>
	 *         </p>
	 * @param cplIn
	 *            开户输入接口
	 * @return DpOpenAccountOut 开户输出接口
	 */
	public static DpOpenAccountOut doMain(DpOpenAccountIn cplIn) {

		bizlog.method(" DpOpenAccount.doMain begin >>>>>>>>>>>>>>>>");
		bizlog.debug(">>>>>cplIn=[%s]", cplIn);

		// 客户号不能为空
		BizUtil.fieldNotNull(cplIn.getCust_no(), SysDict.A.cust_no.getId(), SysDict.A.cust_no.getLongName());

		// 查询账户类型定义
		DppAccountType acctType = DpAccountTypeParmApi.getAcctTypeInfo(cplIn.getAcct_type());

		// 刷新卡属性、客户属性
		DpAttrRefresh.refreshAttrValue(cplIn.getCard_no(), cplIn.getCust_no(), acctType.getCust_type(), E_YESORNO.YES);

		// 检查开户合法性
		DpOpenAccountCheck.checkMainMethod(cplIn);

		// 开账户主调方法
		DpaAccount acctInfo = doMainMethod(cplIn, acctType);

		// 输出
		DpOpenAccountOut cplOut = BizUtil.getInstance(DpOpenAccountOut.class);

		cplOut.setExternal_acct_no(acctType.getCard_relationship_ind() == E_YESORNO.YES ? cplIn.getCard_no() : acctInfo.getAcct_no());
		cplOut.setCard_no(acctType.getCard_relationship_ind() == E_YESORNO.YES ? cplIn.getCard_no() : null);
		cplOut.setAcct_no(acctInfo.getAcct_no()); // 客户账号
		cplOut.setAcct_name(acctInfo.getAcct_name());
		cplOut.setCust_no(cplIn.getCust_no());
		cplOut.setAcct_type(cplIn.getAcct_type());
		cplOut.setAcct_branch(cplIn.getAcct_branch());
		cplOut.setAcct_date(cplIn.getAcct_date());
		cplOut.setOpen_acct_branch(BizUtil.getTrxRunEnvs().getTrxn_branch());
		cplOut.setBranch_name(ApBranchApi.getItem(BizUtil.getTrxRunEnvs().getTrxn_branch()).getBranch_name());
		cplOut.setOpen_acct_method(acctType.getOpen_acct_method());

		bizlog.debug("<<<<<<cplOut=[%s]", cplOut);
		bizlog.method(" DpOpenAccount.doMain end <<<<<<<<<<<<<<<<");

		return cplOut;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2019年3月27日-下午1:37:19</li>
	 *         <li>功能说明：开账户主方法逻辑</li>
	 *         </p>
	 * @param cplIn
	 *            开账户服务输入接口
	 * @param acctType
	 *            账户类型定义
	 * @return 账户信息
	 */
	public static DpaAccount doMainMethod(DpOpenAccountIn cplIn, DppAccountType acctType) {

		// 生成客户账号: 数据集已在前面准备完毕
		String accountNo = genAccountNo(cplIn, acctType);

		// 登记台账信息
		DpaAccount acctInfo = DpBaseServiceApi.registerAccount(cplIn, accountNo);

		// 登记客户账户对照及登记簿
		registerCustInfo(acctInfo);

		// 设置账户交易密码
		if (acctType.getCard_relationship_ind() != E_YESORNO.YES) {
			setPassPwd(cplIn, acctType, acctInfo.getAcct_no());
		}

		// 关联凭证处理
		referVoch(cplIn, acctType, accountNo);

		// 登记短信服务信息
		// ApSms.parseSms(E_DEPTTRXNEVENT.DP_OPEN_ACCOUNT.toString());
		
		//登记账号签名信息
		if (CommUtil.isNotNull(cplIn.getAcct_sign_info()) || cplIn.getAcct_sign_info().size()>0) {
			
			registerAcctSignInfo(cplIn.getAcct_sign_info(),accountNo,cplIn.getCust_no());
		}
		
		return acctInfo;
	}

	/**
	 * @Author Duxy
	 *         <p>
	 *         <li>2019年4月30日</li>
	 *         <li>功能说明：登记账户签名信息DpbAccountSignature</li>
	 *         </p>
	 * @param acct_sign_info	accountNo	cust_no
	 *            
	 */
	
	private static void registerAcctSignInfo(
			List<DpOpenAccountSignature> acct_sign_info, String accountNo,
			String cust_no) {
		bizlog.method(" registerAcctSignInfo begin >>>>>>>>>>>>>>>>");
		
		DpbAccountSignature dpbAcctSign = null;
		long serial_no = 1;
		
		for (DpOpenAccountSignature accountSignature : acct_sign_info) {
			
			dpbAcctSign =  BizUtil.getInstance(DpbAccountSignature.class);
			
			dpbAcctSign.setAcct_no(accountNo);
			dpbAcctSign.setCust_no(cust_no);
			dpbAcctSign.setSerial_no(serial_no);
			dpbAcctSign.setSign_info(accountSignature.getSign_info());
			dpbAcctSign.setSign_desc(accountSignature.getSign_desc());
			
			DpbAccountSignatureDao.insert(dpbAcctSign);
			serial_no++;
			
		}
			
		bizlog.method(" registerAcctSignInfo end <<<<<<<<<<<<<<<<");
		
	}

	/**
	 * @Author Liubx
	 *         <p>
	 *         <li>2018年6月29日-上午10:28:31</li>
	 *         <li>功能说明：登记客户账户关系的登记簿DpbAccountCust</li>
	 *         </p>
	 * @param acctInfo
	 *            账户信息
	 */
	private static void registerCustInfo(DpaAccount acctInfo) {

		bizlog.method(" DpOpenAccount.registerCustInfo begin >>>>>>>>>>>>>>>>");

		DpbAccountCust custAccount = BizUtil.getInstance(DpbAccountCust.class);

		custAccount.setAcct_no(acctInfo.getAcct_no());
		custAccount.setCust_no(acctInfo.getCust_no());
		custAccount.setData_sync_ind(E_YESORNO.NO);

		DpbAccountCustDao.insert(custAccount);

		bizlog.method(" DpOpenAccount.registerCustInfo end <<<<<<<<<<<<<<<<");
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月23日-下午4:44:26</li>
	 *         <li>功能说明：生成客户账号</li>
	 *         <li>
	 *         1.自选账户【DpConst.SELF_ACCOUNT_SEQ_CODE】与普通账户【DpConst.ACCOUNT_SEQ_CODE
	 *         】生成流水编码不是同一个</li>
	 *         <li>2.修改编号组成定义表的 自选账户【序号长度】【机构号长度】时，请注意修改 业务杂项参数表对应的
	 *         【序号长度】【机构号长度】。</li>
	 *         </p>
	 * @param cplIn
	 *            输入接口
	 * @param acctType
	 *            账户类型
	 * @return 客户账号
	 */
	private static String genAccountNo(DpOpenAccountIn cplIn, DppAccountType acctType) {

		bizlog.method(" DpOpenAccount.genAccountNo begin >>>>>>>>>>>>>>>>");

		String accountNo = ""; // 账号

		// int seqLength =
		// ApBusinessParmApi.getIntValue("OPTIONAL_SERIAL_NUMBER_LENGTH"); //
		// 自选账号生成账号时序号长度
		// int branchLenth =
		// ApBusinessParmApi.getIntValue("GEN_ACCT_NO_BRANCH_LENGTH"); //
		// 自选账号生成账号时取机构号长度
		int seqLength = 7; // 自选账号生成账号时序号长度
		int branchLenth = 2; // 自选账号生成账号时取机构号长度

		String acctNoPlaceholder = CommUtil.rpad("", seqLength + 1, "_"); // 通配占位符（序号长度+校验位）

		String busiOrgId = BizUtil.getTrxRunEnvs().getTrxn_branch();

		bizlog.debug("<<<<<<acctNoPlaceholder=[%s]", acctNoPlaceholder);
		bizlog.debug("<<<<<<busiOrgId=[%s]", busiOrgId);
		bizlog.debug("<<<<<<acctType=[%s]", acctType);

		String dataMappingValue = (busiOrgId.substring(0, branchLenth)).concat(acctType.getAcct_id_code()).concat(acctNoPlaceholder);

		// 不选号
		if (cplIn.getSelf_opt_number_ind() == E_SELFOPTNUMBERIND.NOT_OPTIONAL) {

			// 生成客户账号
			accountNo = ApSeqApi.genSeq(DpConst.ACCOUNT_SEQ_CODE);

			// 检查客户账号是否存在
			DpaAccount account = DpaAccountDao.selectOne_odb1(accountNo, false);

			/* 如果不存在则生产账号成功，如果存在则可能被预选账号或自选序号占用，跳过生成下一个客户账号 */
			if (CommUtil.isNotNull(account)) {

				int acctNoLength = accountNo.length(); // 客户账号长度
				String acctSeq = accountNo.substring(acctNoLength - seqLength - 1, acctNoLength - 1);

				// 找接下来在它之后序号有空位的最小客户账号
				String acctNear = SqlDpCommonDao.selAcctNearbyFree(BizUtil.getTrxRunEnvs().getBusi_org_id(), dataMappingValue, acctSeq, seqLength + 0l, false);

				// 再次生产客户账号
				accountNo = ApSeqApi.genSeq(DpConst.ACCOUNT_SEQ_CODE);

				if (CommUtil.compare(accountNo, acctNear) != 0) {

					// 再次生成的账号还是重复，还再生成一次账号就不会重复了
					accountNo = ApSeqApi.genSeq(DpConst.ACCOUNT_SEQ_CODE);
				}
			}
		}
		// 自选序号
		else if (cplIn.getSelf_opt_number_ind() == E_SELFOPTNUMBERIND.OPTIONAL_SERIAL_NUMBER) {

			String selfOptNumber = cplIn.getSelf_opt_number();

			for (int i = 0; i < selfOptNumber.length(); i++) {
				selfOptNumber = selfOptNumber.replaceFirst("\\*", String.valueOf(new Random().nextInt(10)));
			}

			// 生成客户账号
			accountNo = ApSeqApi.genSeq(DpConst.SELF_ACCOUNT_SEQ_CODE);

			// 检查客户账号是否存在
			DpaAccount account = DpaAccountDao.selectOne_odb1(accountNo, false);

			if (CommUtil.isNotNull(account)) {

				// 手输的序号长度与业务参数定义相同，重复了就直接报错
				if (selfOptNumber.length() == seqLength) {

					throw DpErr.Dp.E0174(accountNo);
				}

				// 重复了，自动找尾数相同的下一个最小序号，比如888被用，就找1888，被用再找2888...以此类推
				String acctNear = SqlDpCommonDao.selAcctNearbyFree(BizUtil.getTrxRunEnvs().getBusi_org_id(), dataMappingValue, selfOptNumber, seqLength + 0l, false);

				bizlog.debug("Repeat the next number [%s]", acctNear);
				// 手输序号过大很可能就找不到符合要求的
				if (CommUtil.isNull(acctNear)) {
					throw DpErr.Dp.E0174(accountNo);
				}

				int acctNoLength = accountNo.length(); // 客户账号长度
				long seqNear = Long.parseLong(acctNear.substring(acctNoLength - seqLength - 1, acctNoLength - 1));
				long num = Long.parseLong(CommUtil.rpad("1", selfOptNumber.length() + 1, "0"));

				String newSeq = seqNear + num + ""; // 新序号

				bizlog.debug("new sequence [%s] = [%s] + [%s]", newSeq, seqNear, num);

				// 将新序号追加到输入缓存区
				cplIn.setSelf_opt_number(newSeq);

				ApBufferApi.appendData(ApConst.INPUT_DATA_MART, CommUtil.toMap(cplIn));

				// 再次生成的账号还是重复，还再生成一次账号就不会重复了
				accountNo = ApSeqApi.genSeq(DpConst.SELF_ACCOUNT_SEQ_CODE);
			}
		}
		else {
			throw APPUB.E0026(DpDict.A.self_opt_number_ind.getLongName(), cplIn.getSelf_opt_number_ind().getValue());
		}

		bizlog.method(" DpOpenAccount.genAccountNo end <<<<<<<<<<<<<<<<");

		return accountNo;
	}

	/**
	 * @Author HongBiao
	 *         <p>
	 *         <li>2017年3月8日-下午3:37:03</li>
	 *         <li>功能说明：账户关联凭证</li>
	 *         </p>
	 * @param cplIn
	 *            交易输入接口
	 * @param acctType
	 *            账户类型
	 * @param acctNo
	 *            账号
	 * @return YES or NO
	 */
	private static void referVoch(DpOpenAccountIn cplIn, DppAccountType acctType, String acctNo) {

		if (CommUtil.isNotNull(cplIn.getVoch_type()) && acctType.getRef_voch_level() == E_VOCHREFLEVEL.ACCT) {

			DpVoucherIobus.payOutVoucher(acctType, acctNo, cplIn.getVoch_type(), cplIn.getVoch_no());
		}

	}

	/**
	 * @Author cxing
	 *         <p>
	 *         <li>2017年7月19日-下午4:14:03</li>
	 *         <li>功能说明：设置密码</li>
	 *         </p>
	 * @param cplIn
	 *            交易输入接口
	 * @param acctNo
	 *            账号
	 */

	private static void setPassPwd(DpOpenAccountIn cplIn, DppAccountType acctType, String acctNo) {

		// 卡密码在卡系统管理
		if (acctType.getCard_relationship_ind() == E_YESORNO.YES) {
			return;
		}

		if (CommUtil.isNull(cplIn.getWithdrawal_cond())) {
			return;
		}

		DppConditionFeature drawCondInfo = DpTechParmApi.getDrawCondInfo(cplIn.getWithdrawal_cond());

		// 不是密码则退出
		if (drawCondInfo.getCheck_password_ind() == E_YESORNO.NO) {
			return;
		}

		// 设置密码
		DpOtherIobus.setPassword(acctNo, cplIn.getTrxn_password());
	}

}
