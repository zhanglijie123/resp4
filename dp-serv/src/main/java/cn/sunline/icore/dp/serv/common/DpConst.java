package cn.sunline.icore.dp.serv.common;

import cn.sunline.icore.dp.base.api.DpBaseConst;

/**
 * <p>
 * 文件功能说明：负债模块全局常量定义
 * </p>
 * 
 * @Author zhoumy
 *         <p>
 *         <li>2017年1月12日-下午4:42:22</li>
 *         <li>修改记录</li>
 *         <li>-----------------------------------------------------------</li>
 *         </p>
 */
public interface DpConst extends DpBaseConst {

	/** 最小日期 **/
	public static final String THE_SMALLEST_DATE = "20010101";

	/** 最小时间 **/
	public static final String THE_SMALLEST_TIME = "00:00:00 000";

	/** 脱敏处理字符串 */
	public static final String SENSITIVE_STRING = "********";

	/************************
	 * drop list begin
	 *************************************************************/

	/** 解冻原因下拉字典类型 */
	public static final String UNFROZE_REASON = "UNFROZE_REASON";

	/** 执法部门下拉字典类型 */
	public static final String ENFORCED_LEGAL_DEPT = "ENFORCED_LEGAL_DEPT";

	/** 执法人员证件种类下拉字典类型 */
	public static final String LAW_MAN_DOC_TYPE = "LAW_MAN_DOC_TYPE";

	/** 银行代码下拉字典类型 */
	public static final String BANK_ID = "BANK_ID";

	/** 证件类型下拉字典类型 */
	public static final String DOC_TYPE = "DOC_TYPE";

	/** 联系人名关系类型下拉字典类型 */
	public static final String JOINT_PERSON_RELATIONSHIP = "ACCOUNT_RELATIONSHIP";

	/** 开户目的下拉字典类型 */
	public static final String FUND_USE_WAY = "FUND_USE_WAY";

	/** 资金来源下拉字典类型 */
	public static final String FUND_SOURCE = "FUND_SOURCE";

	/************************
	 * other parameter begin
	 *************************************************************/

	/** 默认业务编码mainKey */
	public static final String BUSINESS_CODE = "BUSINESS_CODE";

	/** 默认机构规则mainKey:ACCT-账户机构、TRXN-交易机构 FIXED-固定机构 */
	public static final String BRANCH_ID_RULE = "BRANCH_ID_RULE";

	/** 固定机构规则mainKey */
	public static final String FIXED_BRANCH = "FIXED_BRANCH";

	/** 自选账户生成规则 */
	public static final String SELF_ACCOUNT_SEQ_CODE = "SELF_ACCOUNT_NO";

	/** 账户生成规则 */
	public static final String ACCOUNT_SEQ_CODE = "ACCOUNT_NO";

	/** 冻结编号生成规则 */
	public static final String FROZE_NO = "FROZE_NO";

	/** 存款印花税代码定位规则场景代码 */
	public static final String STAMP_TAX_CODE_RULE_SCENE_CODE = "DP_STAMP_TAX_CODE";

	/** 收入来源 */
	public static final String INCOME_SOURCE = "INCOME_SOURCE";

	/** 司法冻结司法信息检查标志 */
	public static final String FROZE_LAW_INFO_CHECK = "FROZE_LAW_INFO_CHECK";

}
