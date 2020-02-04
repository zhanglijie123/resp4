package cn.sunline.icore.dp.serv.passbook;

import java.math.BigDecimal;
import java.util.List;

import cn.sunline.clwj.msap.sys.type.MsEnumType.E_YESORNO;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccount;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaAccountRelate;
import cn.sunline.icore.dp.base.tables.TabDpAccountBase.DpaSubAccount;
import cn.sunline.icore.dp.serv.namedsql.online.SqlDpPassbookDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookLine;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookLineDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookMark;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DpbPassbookMarkDao;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DppPassbook;
import cn.sunline.icore.dp.serv.tables.TabDpPassBookPrint.DppPassbookDao;
import cn.sunline.icore.dp.serv.type.ComDpPassbookPrint.DpPageLineRet;
import cn.sunline.icore.dp.serv.type.DpEnumType.E_PRINTCOMPRESSFLAG;
import cn.sunline.icore.sys.dict.SysDict;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.ltts.base.odb.OdbFactory;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：
 * </p>
 * 
 * @Author yangdl
 *         <p>
 *         <li>2017年3月21日-下午1:38:55</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>2017年3月21日-yangdl：存折补登折</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpPassbook {

	private static final BizLog bizlog = BizLogUtil.getBizLog(DpPassbook.class);

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月23日-下午6:32:13</li>
	 *         <li>功能说明：查询存折账单戳信息，为空则初始化</li>
	 *         <li>功能说明：子账户账单戳信息初始化</li>
	 *         </p>
	 * @param vochType
	 *            凭证类型
	 * @param vochNo
	 *            凭证号码
	 * @param subAcctNo
	 *            子账户信息
	 * @return 存折账单戳信息
	 */
	public static DpbPassbookMark getPassbookMark(String vochType, String vochNo, DpaSubAccount subAccount) {

		bizlog.method("DpPassbook.getPassbookMark begin  >>>>>>>>>>>>>>>");

		DpbPassbookMark passbookMark = DpbPassbookMarkDao.selectOne_odb1(vochType, vochNo, subAccount.getSub_acct_no(), false);

		if (CommUtil.isNull(passbookMark)) {

			passbookMark = BizUtil.getInstance(DpbPassbookMark.class);

			passbookMark.setVoch_type(vochType);
			passbookMark.setVoch_no(vochNo);
			passbookMark.setSub_acct_no(subAccount.getSub_acct_no()); // 子账户号
			passbookMark.setSerial_no(0l); // 序号
			passbookMark.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date()); // 交易日期
			passbookMark.setTrxn_seq("0"); // 交易流水
			passbookMark.setAcct_no(subAccount.getAcct_no()); // 账号
			passbookMark.setPassbook_balance(BigDecimal.ZERO); // 存折余额
			// 登记存折账单戳初始化信息
			DpbPassbookMarkDao.insert(passbookMark);
		}

		bizlog.method("DpPassbook.getPassbookMark end  <<<<<<<<<<<<<<<<");

		return passbookMark;

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月23日-下午6:32:13</li>
	 *         <li>功能说明：初始化存折账单戳信息</li>
	 *         <li>功能说明：凭证关联在账户层，初始化账户下子户凭证账单戳信息</li>
	 *         </p>
	 * @param vochType
	 *            凭证类型
	 * @param vochNo
	 *            凭证号
	 * @param acctInfo
	 *            账户信息
	 */
	public static void initPassbookMark(String vochType, String vochNo, DpaAccount acctInfo) {

		bizlog.method("DpPassbook.initPassbookMark begin  >>>>>>>>>>>>>>>");

		// 获取子户列表
		List<DpaAccountRelate> acctRelateList = SqlDpPassbookDao.selInitPassbookMarkData(acctInfo.getAcct_no(), BizUtil.getTrxRunEnvs().getBusi_org_id(), false);

		for (DpaAccountRelate acctRelate : acctRelateList) {

			DpbPassbookMark passbookMark = DpbPassbookMarkDao.selectOne_odb1(vochType, vochNo, acctRelate.getSub_acct_no(), false);

			// 存折账单戳无记录则初始化
			if (CommUtil.isNull(passbookMark)) {

				passbookMark = BizUtil.getInstance(DpbPassbookMark.class);

				passbookMark.setVoch_type(vochType);
				passbookMark.setVoch_no(vochNo);
				passbookMark.setSub_acct_no(acctRelate.getSub_acct_no()); // 子账户号
				passbookMark.setSerial_no(0l); // 序号
				passbookMark.setTrxn_date(BizUtil.getTrxRunEnvs().getTrxn_date()); // 交易日期
				passbookMark.setTrxn_seq("0"); // 交易流水
				passbookMark.setAcct_no(acctRelate.getAcct_no()); // 账号
				passbookMark.setPassbook_balance(BigDecimal.ZERO); // 存折余额

				// 登记存折打印账单戳
				DpbPassbookMarkDao.insert(passbookMark);
			}

		}

		bizlog.method("DpPassbook.initPassbookMark end  <<<<<<<<<<<<<<<<");

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月23日-下午5:35:53</li>
	 *         <li>功能说明： 返回未登折打印行位控制信息 ，为空则初始化</li>
	 *         </p>
	 * @param vochType
	 *            凭证类型
	 * @param vochNo
	 *            凭证号码
	 */
	public static DpbPassbookLine getPassbookLine(String vochType, String vochNo) {

		bizlog.method("DpPassbook.getPassbookLine begin >>>>>>>>>>>>>>");

		// 未登折打印行位控制信息
		DpbPassbookLine passbookLine = DpbPassbookLineDao.selectOne_odb1(vochType, vochNo, false);

		if (CommUtil.isNull(passbookLine)) {

			passbookLine = BizUtil.getInstance(DpbPassbookLine.class);

			passbookLine.setVoch_type(vochType);
			passbookLine.setVoch_no(vochNo);
			passbookLine.setPassbook_page_no(0L); // 凭证当前页号
			passbookLine.setPassbook_line_no(0L); // 凭证当前行号
			passbookLine.setNext_passbook_page_no(1L); // 下一打印页号
			passbookLine.setNext_passbook_line_no(1L); // 下一打印行号
			passbookLine.setRemark(null);
			passbookLine.setFull_page_ind(E_YESORNO.NO);

			// 登记未登折打印行位控制初始化信息
			DpbPassbookLineDao.insert(passbookLine);
		}

		bizlog.method("DpPassbook.getPassbookLine end <<<<<<<<<<<<<<<<<");

		return passbookLine;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月24日-上午11:28:28</li>
	 *         <li>功能说明：获取存折类打印控制信息</li>
	 *         </p>
	 * @param vochType
	 *            凭证类型
	 * @return DppPassbook 存折类打印控制信息
	 */
	public static DppPassbook getPassbook(String vochType) {

		// 获取存折类打印控制信息
		DppPassbook passbook = DppPassbookDao.selectOne_odb1(vochType, false);

		if (passbook == null) {
			throw APPUB.E0005(OdbFactory.getTable(DppPassbook.class).getLongname(), SysDict.A.voch_type.getLongName(), vochType);
		}

		return passbook;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年8月21日-下午6:54:21</li>
	 *         <li>功能说明：获取存折总行数</li>
	 *         </p>
	 * @param vochType
	 *            凭证类型
	 */
	public static long getPassbookTotalNum(String vochType) {

		DppPassbook passbook = DpPassbook.getPassbook(vochType);

		return passbook.getBook_pages_count() * passbook.getPage_line_count();

	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月29日-下午4:44:36</li>
	 *         <li>功能说明：计算存折当前页号 、当前行号、下一页数、行数，空白行数</li>
	 *         </p>
	 * @param passbookLine
	 * @param passbook
	 * @param lineCount
	 * @return DpPageLineRet
	 */
	public static DpPageLineRet calcPageLine(DpbPassbookLine passbookLine, DppPassbook passbook, long count) {

		DpPageLineRet pageLineRet = BizUtil.getInstance(DpPageLineRet.class);

		long nextPageNo = 0; // 下一页号
		long nextLineNo = 0; // 下一行号
		if ((passbookLine.getNext_passbook_line_no() + count) % passbook.getPage_line_count() == 0) {
			// 下一页号
			nextPageNo = passbookLine.getNext_passbook_page_no() + (passbookLine.getNext_passbook_line_no() + count) / passbook.getPage_line_count() - 1;
			// 下一行号
			nextLineNo = passbook.getPage_line_count();
		}
		else {
			// 下一页号
			nextPageNo = passbookLine.getNext_passbook_page_no() + (passbookLine.getNext_passbook_line_no() + count) / passbook.getPage_line_count();
			// 下一行号
			nextLineNo = (passbookLine.getNext_passbook_line_no() + count) % passbook.getPage_line_count();

		}

		// 获取存折总行数
		long passbookTotalNum = DpPassbook.getPassbookTotalNum(passbook.getVoch_type());

		// 存折空白行数 = 总行数 - （下一打印页号 * 每页行数 + 下一打印行号 -1）
		long leftBlankLine = passbookTotalNum - ((passbookLine.getNext_passbook_page_no() - 1) * passbook.getPage_line_count() + passbookLine.getNext_passbook_line_no() - 1);

		pageLineRet.setNext_passbook_page_no(nextPageNo); // 下一页号
		pageLineRet.setNext_passbook_line_no(nextLineNo); // 下一行号
		pageLineRet.setPassbook_blank_line_num(leftBlankLine); // 存折空白行号

		return pageLineRet;
	}

	/**
	 * @Author yangdl
	 *         <p>
	 *         <li>2017年3月22日-下午4:30:18</li>
	 *         <li>功能说明：根据条件判断是否需要压缩</li>
	 *         </p>
	 * @param passbook
	 * @param passbookMark
	 * @param count
	 *            行数
	 * @return boolean
	 */
	public static boolean isCompress(DppPassbook passbook, DpbPassbookMark passbookMark, long count) {

		bizlog.method("DpPassbook.isCompress begin >>>>>>>>>>>>>>");

		if (passbook.getPrint_compress_print_ind() == E_PRINTCOMPRESSFLAG.NONE) {
			// 不压缩
			return false;
		}
		else if (passbook.getPrint_compress_print_ind() == E_PRINTCOMPRESSFLAG.DAYS) {
			// 超过压缩天数阀值压缩、否则不压缩
			return BizUtil.dateDiff("dd", passbookMark.getTrxn_date(), BizUtil.getTrxRunEnvs().getTrxn_date()) > passbook.getPrint_compress_threshold() ? true : false;
		}
		else if (passbook.getPrint_compress_print_ind() == E_PRINTCOMPRESSFLAG.LINES) {
			// 超过压缩行数阀值压缩、否则不压缩
			return CommUtil.compare(count, passbook.getPrint_compress_threshold()) > 0 ? true : false;
		}

		bizlog.method("DpPassbook.isCompress end <<<<<<<<<<<<<<<");

		return false;
	}

}
