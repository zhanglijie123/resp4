package cn.sunline.icore.dp.serv.common;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.sunline.common.util.DateUtil;
import cn.sunline.icore.ap.util.BizUtil;
import cn.sunline.icore.dp.serv.errors.DpErr;
import cn.sunline.icore.sys.errors.ApPubErr.APPUB;
import cn.sunline.icore.sys.type.EnumType.E_CYCLETYPE;
import cn.sunline.ltts.biz.global.CommUtil;
import cn.sunline.ltts.biz.global.SysUtil;
import cn.sunline.ltts.core.api.logging.BizLog;
import cn.sunline.ltts.core.api.logging.BizLogUtil;

/**
 * <p>
 * 文件功能说明：数据格式化程序
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年11月24日-上午9:49:44</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         <li>标记：修订内容</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public class DpDataFormat {
	
	private static final BizLog bizlog = BizLogUtil.getBizLog(DpDataFormat.class);

	/**
	 * 功能说明：对金额字符串化处理：去掉小数点，不足长度补零
	 * 
	 * @param amout
	 *            金额
	 * @param totalLength
	 *            总长度
	 * @param pointLength
	 *            小数点位数
	 * @return
	 */
	public static String getAmountFormat(BigDecimal amount, int totalLength, int pointLength) {

		/*
		 * 12345.99 should formatted as 00000000000000000001234599; 12345.00
		 * should formatted as 00000000000000000001234500; 12345 should
		 * formatted as 00000000000000000001234500
		 */
		final String FORMAT_SYMBOL = "0";
		String amountFormat = amount.toString();
		String amountSymol = "";

		if (CommUtil.compare(amount, BigDecimal.ZERO) < 0) {
			amountSymol = "-";
			amountFormat = amountFormat.substring(1, amountFormat.length());
		}

		// 小数点之后的数字长度： 默认为没有小数点
		int afterPointLength = 0;

		if (amountFormat.contains(".")) {

			afterPointLength = amountFormat.length() - amountFormat.indexOf(".") - 1;
		}

		// 小数点后的数值不够位数先补零
		if (afterPointLength < pointLength) {

			String rightAddValue = CommUtil.rpad("", pointLength - afterPointLength, FORMAT_SYMBOL);

			amountFormat = amountFormat.concat(rightAddValue);
		}
		else if (afterPointLength > pointLength) {

			int diffValue = afterPointLength - pointLength;

			amountFormat = amountFormat.substring(0, amountFormat.length() - diffValue);
		}

		// 去掉小数点，左补零
		if (CommUtil.isNull(amountSymol)) {
			amountFormat = CommUtil.lpad(amountFormat.replace(".", ""), totalLength, FORMAT_SYMBOL);
		}
		else {
			amountFormat = amountSymol + CommUtil.lpad(amountFormat.replace(".", ""), totalLength - 1, FORMAT_SYMBOL);
		}

		return amountFormat;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月8日-上午9:57:21</li>
	 *         <li>功能说明：计算下一执行日期: 包括参考日期所在周期</li>
	 * @param refDate
	 *            参考日期： 参考日期通常比指定日期要小
	 * @param curtDate
	 *            指定日期
	 * @param cycle
	 *            周期
	 * @param dateAid
	 *            日期定位辅助域(日期简明字符)：可以为空
	 * @return 下一执行日期
	 */
	public static String calcNextDate(String refDate, String curtDate, String cycle, String dateAid) {

		return calcNextDate(refDate, curtDate, cycle, dateAid, true);
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月8日-上午9:57:21</li>
	 *         <li>功能说明：计算下一执行日期</li>
	 * @param refDate
	 *            参考日期： 参考日期通常比指定日期要小
	 * @param curtDate
	 *            指定日期
	 * @param cycle
	 *            周期
	 * @param dateAid
	 *            日期定位辅助域(日期简明字符)：可以为空
	 * @param containCurCycle
	 *            是否包括参考日期所在当前：true-包括 false-不包括
	 * @return 下一执行日期
	 */
	public static String calcNextDate(String refDate, String curtDate, String cycle, String dateAid, boolean containCurCycle) {

		// dateAid: 日期需要确定诸如“月尾”、星期几这种，为避免歧义会用到此域。
		// 比如2月28日作为下次执行日，它是表示每月28日还是表示每月月底？

		if (CommUtil.isNull(dateAid)) {
			return BizUtil.calcDateByReference(refDate, curtDate, cycle);
		}

		if (!BizUtil.isCycleString(cycle)) {
			throw APPUB.E0012(cycle);
		}

		if (!BizUtil.isDateString(refDate)) {
			throw APPUB.E0011(refDate);
		}

		if (!BizUtil.isDateString(curtDate)) {
			throw APPUB.E0011(curtDate);
		}

		// 周期单位
		String cycUnit = cycle.substring(cycle.length() - 1);

		// 周期单位为“日” 且日期定位辅助域不为空
		if (CommUtil.equals(cycUnit, E_CYCLETYPE.DAY.getValue()) && CommUtil.isNotNull(dateAid)) {
			throw DpErr.Dp.E0420(dateAid);
		}

		// 日期辅助域: “周”
		if (CommUtil.equals(cycUnit, E_CYCLETYPE.WEEK.getValue())) {

			if (between(dateAid, 0, 6) == false) {
				throw DpErr.Dp.E0420(dateAid);
			}

			// 获取参考日所在指定星期几
			String dayOfWeek = BizUtil.calcDateByCycle(BizUtil.firstDay(E_CYCLETYPE.WEEK.getValue(), refDate), dateAid.concat("D"));

			// 如果包括参考日期当前周期，则参考日指定星期几大于当前日期直接退出
			if (containCurCycle && CommUtil.compare(dayOfWeek, curtDate) > 0) {

				return dayOfWeek;
			}
			else {
				// 不包括当前周期且所处周期指定日大于当前日期，则往后推一个周期
				if (CommUtil.compare(dayOfWeek, curtDate) > 0) {

					return BizUtil.calcDateByCycle(dayOfWeek, cycle);
				}
				else {

					return BizUtil.calcDateByReference(dayOfWeek, curtDate, cycle);
				}
			}
		}
		// 日期辅助域: “月”
		else if (CommUtil.equals(cycUnit, E_CYCLETYPE.MONTH.getValue())) {

			// 不管是1M, 2M, 还是3M, 日期辅助字段都指向周期起始月份
			if (between(dateAid, 1, 31) == false && !CommUtil.in(dateAid, "F", "E")) {
				throw DpErr.Dp.E0420(dateAid);
			}

			// 获取参考日所在月份指定日
			String dayOfMonth = getMonthAppointDate(refDate, dateAid);

			// 如果包括参考日期当前周期，则参考日指定日期大于当前日期直接退出
			if (containCurCycle && CommUtil.compare(dayOfMonth, curtDate) > 0) {

				return dayOfMonth;
			}
			else {

				String date = "";

				// 不包括当前周期且所处周期指定日大于当前日期，则往后推一个周期
				if (CommUtil.compare(dayOfMonth, curtDate) > 0) {

					date = BizUtil.calcDateByCycle(dayOfMonth, cycle);
				}
				else {

					date = BizUtil.calcDateByReference(dayOfMonth, curtDate, cycle);
				}

				// 再次取指定日，这是为了兼容大小月月底
				return getMonthAppointDate(date, dateAid);
			}
		}
		// 日期辅助域: “季”, eg： 21E（最后月份21号）、FF（起始月份1号）
		else if (CommUtil.equals(cycUnit, E_CYCLETYPE.QUARTER.getValue())) {

			if ((between(dateAid.substring(0, dateAid.length() - 1), 1, 31) == false && !CommUtil.in(dateAid.substring(0, dateAid.length() - 1), "F", "E"))
					|| !CommUtil.in(dateAid.substring(dateAid.length() - 1), "F", "M", "E")) {
				throw DpErr.Dp.E0420(dateAid);
			}

			// 先确定季度所在起点
			String date = BizUtil.firstDay(E_CYCLETYPE.QUARTER.getValue(), refDate);

			if (CommUtil.equals(dateAid.substring(dateAid.length() - 1), "M")) {
				date = BizUtil.dateAdd("mm", date, 1);
			}
			else if (CommUtil.equals(dateAid.substring(dateAid.length() - 1), "E")) {
				date = BizUtil.dateAdd("mm", date, 2);
			}

			// 获取参考日所在季度指定月份指定日
			String dayofQuarter = getMonthAppointDate(date, dateAid.substring(0, dateAid.length() - 1));

			// 如果包括参考日期当前周期，则参考日指定日期大于当前日期直接退出
			if (containCurCycle && CommUtil.compare(dayofQuarter, curtDate) > 0) {

				return dayofQuarter;
			}
			else {

				// 不包括当前周期且所处周期指定日大于当前日期，则往后推一个周期
				if (CommUtil.compare(dayofQuarter, curtDate) > 0) {

					date = BizUtil.calcDateByCycle(dayofQuarter, cycle);
				}
				else {

					date = BizUtil.calcDateByReference(dayofQuarter, curtDate, cycle);
				}

				// 再次取指定日，这是为了兼容大小月月底
				return getMonthAppointDate(date, dateAid.substring(0, dateAid.length() - 1));
			}
		}
		// 日期辅助域: “半年”, eg: FF、EE、21F(第一个月21号)
		else if (CommUtil.equals(cycUnit, E_CYCLETYPE.HALF_YEAR.getValue())) {

			if ((between(dateAid.substring(0, dateAid.length() - 1), 1, 31) == false && !CommUtil.in(dateAid.substring(0, dateAid.length() - 1), "F", "E"))
					|| !CommUtil.in(dateAid.substring(dateAid.length() - 1), "F", "E", "1", "2", "3", "4", "5", "6")) {
				throw DpErr.Dp.E0420(dateAid);
			}

			// 确定参考日期所在半年起点日
			String date = BizUtil.firstDay(E_CYCLETYPE.HALF_YEAR.getValue(), refDate);

			if (CommUtil.in(dateAid.substring(dateAid.length() - 1), "F", "1")) {
				;
			}
			else if (CommUtil.in(dateAid.substring(dateAid.length() - 1), "E", "6")) {
				date = BizUtil.firstDay(E_CYCLETYPE.MONTH.getValue(), BizUtil.lastDay(E_CYCLETYPE.HALF_YEAR.getValue(), refDate));
			}
			else {
				date = BizUtil.dateAdd("mm", date, Integer.parseInt(dateAid.substring(dateAid.length() - 1)) - 1);
			}

			// 获取参考日所在半年指定月份指定日
			String dayOfHalf = getMonthAppointDate(date, dateAid.substring(0, dateAid.length() - 1));

			// 如果包括参考日期当前周期，则参考日指定日期大于当前日期直接退出
			if (containCurCycle && CommUtil.compare(dayOfHalf, curtDate) > 0) {

				return dayOfHalf;
			}
			else {

				// 不包括当前周期且所处周期指定日大于当前日期，则往后推一个周期
				if (CommUtil.compare(dayOfHalf, curtDate) > 0) {

					date = BizUtil.calcDateByCycle(dayOfHalf, cycle);
				}
				else {

					date = BizUtil.calcDateByReference(dayOfHalf, curtDate, cycle);
				}

				// 再次取指定日，这是为了兼容大小月月底
				return getMonthAppointDate(date, dateAid.substring(0, dateAid.length() - 1));
			}
		}
		// 日期辅助域: “年”， eg: 0521(5月21日), 300(第300天)
		else if (CommUtil.equals(cycUnit, E_CYCLETYPE.YEAR.getValue())) {

			// 参考日所在年份指定天数
			String dayOfYear = getYearAppointDate(refDate, dateAid);

			// 如果包括参考日期当前周期，则参考日指定日期大于当前日期直接退出
			if (containCurCycle && CommUtil.compare(dayOfYear, curtDate) > 0) {

				return dayOfYear;
			}
			else {

				String date = "";

				// 不包括当前周期且所处周期指定日大于当前日期，则往后推一个周期
				if (CommUtil.compare(dayOfYear, curtDate) > 0) {

					date = BizUtil.calcDateByCycle(dayOfYear, cycle);
				}
				else {

					date = BizUtil.calcDateByReference(dayOfYear, curtDate, cycle);
				}

				// 再次取指定日，这是为了兼容大小月月底
				return getYearAppointDate(date, dateAid);
			}
		}
		else {
			throw APPUB.E0026("cycle unit type", cycUnit);
		}
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月8日-上午9:57:21</li>
	 *         <li>功能说明：获得月份指定日期</li>
	 * @param date
	 *            所在月份日期基准点
	 * @param dateAid
	 *            日期辅助域
	 * @return 指定日期
	 */
	private static String getMonthAppointDate(String date, String dateAid) {

		if (between(dateAid, 1, 31) == false && !CommUtil.in(dateAid, "F", "E")) {
			throw DpErr.Dp.E0420(dateAid);
		}

		String appointDate = "";

		if (CommUtil.in(dateAid, "F")) {
			appointDate = BizUtil.firstDay(E_CYCLETYPE.MONTH.getValue(), date);
		}
		else if (between(dateAid, 1, 9)) {
			appointDate = date.substring(0, 6).concat("0").concat(dateAid);
		}
		else if (CommUtil.in(dateAid, "31", "E")) {

			appointDate = BizUtil.lastDay(E_CYCLETYPE.MONTH.getValue(), date);
		}
		else {

			appointDate = date.substring(0, 6).concat(dateAid);

			// 遇到2月份
			if (!BizUtil.isDateString(appointDate)) {

				appointDate = BizUtil.lastDay(E_CYCLETYPE.MONTH.getValue(), date);
			}
		}

		return appointDate;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月8日-上午9:57:21</li>
	 *         <li>功能说明：获得年指定日期</li>
	 * @param date
	 *            所在年份日期基准点
	 * @param dateAid
	 *            日期辅助域
	 * @return 指定日期
	 */
	private static String getYearAppointDate(String date, String dateAid) {

		if (!BizUtil.isDateString("2000".concat(dateAid)) && between(dateAid, 1, 366) == false && !CommUtil.in(dateAid, "F", "E")) {
			throw DpErr.Dp.E0420(dateAid);
		}

		// 参考日所在年份指定天数
		String dayOfYear = "";

		if (BizUtil.isDateString("2000".concat(dateAid))) {

			dayOfYear = date.substring(0, 4).concat(dateAid);

			// 平年2月月底
			if (!BizUtil.isDateString(dayOfYear)) {
				dayOfYear = date.substring(0, 4).concat("0228");
			}
		}
		else {

			if (CommUtil.in(dateAid, "365", "366", "E")) {
				dayOfYear = BizUtil.lastDay(E_CYCLETYPE.YEAR.getValue(), date);
			}
			else if (CommUtil.in(dateAid, "1", "F")) {
				dayOfYear = BizUtil.firstDay(E_CYCLETYPE.YEAR.getValue(), date);
			}
			else {
				dayOfYear = BizUtil.calcDateByCycle(BizUtil.firstDay(E_CYCLETYPE.YEAR.getValue(), date), (Integer.parseInt(dateAid) - 1) + "D");
			}
		}

		return dayOfYear;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月8日-上午9:57:21</li>
	 *         <li>功能说明：判断字符串是否在指定数值之间</li>
	 * @param value
	 *            待比较值
	 * @param start
	 *            开始数字
	 * @param end
	 *            结尾数字
	 * @return true-在范围中 false-不在范围中
	 */
	public static boolean between(String value, int start, int end) {

		try {
			int number = new Integer(value);

			if (number < start || number > end) {
				return false;
			}
		}
		catch (NumberFormatException e) {
			return false;
		}

		return true;
	}

	/**
	 * @Author zhoumy
	 *         <p>
	 *         <li>2018年1月8日-上午9:57:21</li>
	 *         <li>功能说明：将字符串反序列化成指定List类型</li>
	 * @param value
	 *            字符串值
	 * @param type
	 *            指定复合类型类
	 * @return list类数据
	 */

	@SuppressWarnings("unchecked")
	public static <T> List<T> deserializeList(String value, Class<T> type) {

		List<T> listOut = new ArrayList<T>();

		List<Object> listObject = SysUtil.deserialize(value, ArrayList.class);

		for (Object info : listObject) {

			String infoString = SysUtil.serialize(info);

			T outInfo = BizUtil.parseEntity(infoString, type);

			listOut.add(outInfo);
		}

		return listOut;
	}
	
	/**
	 * @Author hongbiao
	 *         <p>
	 *         <li>2019年2月28日-下午12:08:48</li>
	 *         <li>功能说明：根据BAY需求格式化邮件要素</li>
	 *         <li>1.Mobile number masking format: example: 089-999-1111      Masking XXX-XXX-1111</li>
	 *         <li>2.Promptpay ID: Mobile phone: 089-999-1111                 Masking XXX-XXX-1111</li>
	 *         <li>3.Citizen ID and TAX ID: 1-2399-00099-99-9         		Masking X-XXXX-XXXX9-99-9 </li>
	 *         <li>4.Account number masking: e.g. 111-7-11101-0 				Masking 1XX-7-XXX01-0</li>
	 *         <li>5.Transaction date: DD MMM YYYY (Month, B.E. and A.D depends on customer preference language) e.g. 9 Jan 2019 (A.D.)  (EN) or 9 ม.ค. 2562 (B.E.) (TH)</li>
	 *         <li>6.Transaction time: HH:MM (24 hours) e.g. 9:30, 15:25, 22:15</li>
	 *         <li>7.Amount: 2 decimals with comma e.g. Amount (THB): 124,500.25</li>
	 *         </p>
	 * @param formatObj
	 * @return
	 */
	public static String messageFormat(Object formatObj,String fomartType,String language) {
		
		bizlog.method(" DpAccounting.messageFormat begin >>>>>>>>>>>>>>>>");
		bizlog.debug(" formatObj = [%s],fomartType = [%s]",formatObj,fomartType);
		
		StringBuilder fomatStr = new StringBuilder();
		
		// 为空不处理或者交易日期超过8位说明已经格式化不处理
		if(CommUtil.isNull(formatObj)){
			return fomatStr.toString();
		}
		
		fomatStr.append(formatObj);
		
		// 1.Mobile number masking format: example: 089-999-1111      Masking XXX-XXX-1111
		// 2.Promptpay ID: Mobile phone: 089-999-1111                 Masking XXX-XXX-1111
		if(CommUtil.in(fomartType,"mobileNo", "promptpay") ){
			if(fomatStr.length() >= 7){
				fomatStr = fomatStr.replace(0, 3, "XXX-").replace(4, 7, "XXX-");
			}else if(fomatStr.length() > 3){
				fomatStr = fomatStr.replace(0, 3, "XXX-");
			}
		}
		// 3.Citizen ID and TAX ID: 1-2399-00099-99-9         		Masking X-XXXX-XXXX9-99-9
		else if(CommUtil.equals("docNo", fomartType)){
			if(fomatStr.length() >= 15){
				fomatStr = fomatStr.replace(0, 1, "X-").replace(2, 6, "XXXX-").replace(7, 11, "XXXX").insert(12, "-").insert(15, "-");
			}else if(fomatStr.length() > 3){
				fomatStr = fomatStr.replace(0, 3, "XXX-");
			}
		}
		// 4.Account number masking: e.g. 111-7-11101-0 				Masking 1XX-7-XXX01-0
		else if(CommUtil.equals("acctNo", fomartType)){
			
			fomatStr = fomatStr.replace(1, 3, "XX-").replace(5, 8, "-XXX").insert(fomatStr.length()-1, "-");
			
		}
		// 5.Transaction date: DD MMM YYYY (Month, B.E. and A.D depends on customer preference language) e.g. 9 Jan 2019 (A.D.)  (EN) or 9 ม.ค. 2562 (B.E.) (TH)
		else if(CommUtil.equals("trxnDate", fomartType)){
			
			Calendar calendar = Calendar.getInstance();		 
			try {
				calendar.setTime(DateUtil.parseDate(fomatStr.toString(),"yyyyMMdd"));
			}
			catch (Exception e) {
				bizlog.error("trxn data error : [%s]", e,e.getMessage());
			}
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale(language));
			
			if(CommUtil.equals("th", language)){

				calendar.add(Calendar.YEAR, 543);
			}
			
			fomatStr = new StringBuilder(simpleDateFormat.format(calendar.getTime()));
		}
		// 6.Transaction time: HH:MM (24 hours) e.g. 9:30, 15:25, 22:15
		/*else if(CommUtil.equals("mobileNo", fomartType)){
			
		}*/
		// 7.Amount: 2 decimals with comma e.g. Amount (THB): 124,500.25
		else if(CommUtil.equals("amount", fomartType)){

			fomatStr = new StringBuilder(amountFormat(fomatStr,"###,##0.00"));
		}

		bizlog.debug(" fomatStr = [%s]",fomatStr);
		bizlog.method(" DpAccounting.messageFormat end <<<<<<<<<<<<<<<<");
		
		return fomatStr.toString();	
	}
	
	/**
	 * @Author hongbiao
	 *         <p>
	 *         <li>2019年4月6日-下午6:22:30</li>
	 *         <li>功能说明：金额格式化</li>
	 *         </p>
	 * @param formatObj
	 * @param fomartType
	 * @param language
	 * @return
	 */
	private static String amountFormat(Object formatObj,String fomartPattern) {
		bizlog.debug(" amount Format format object = [%s]", formatObj);
		
		String formatStr = formatObj + "";
		
		if(CommUtil.isNull(formatObj)){
			formatStr = "0";
		}
		
		DecimalFormat df = new DecimalFormat(fomartPattern);
		
		formatStr = df.format(new BigDecimal(formatStr));
		bizlog.debug(" amount Format format return = [%s]", formatStr);
		
		return formatStr;
	}
}