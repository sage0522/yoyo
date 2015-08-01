package yoyo.common.exception;

public class ErrorDefine {
	public static final int ERROR_ACCOUNT 				= 1000;	//帐号或密码错误
	public static final int ERROR_MULTI_DEVICE 			= 1002; //账户在其他设备登录
	public static final int ERROR_MULTI_TIMEOUT 		= 1003; //登录超时
	public static final int ERROR_ACCOUNT_DISABLE	 	= 1010; //账户被停用
	
	public static final int ERROR_SIGNED				= 5101; // 今日已签到
	
	public static final int ERROR_MODIFY_MULTI			= 5203; //"个人信息修改成功，但检测到存在多条数据记录!";
	public static final int ERROR_MODIFY_NULL			= 5299;	//"个人信息指令不存在!";
	
	public static final int ERROR_SAFE_PASSWORD			= 5301;	// "保险箱密码错误!";
	public static final int ERROR_SAFE_SAVELESS			= 5302;	// "保险箱存钱失败，身上余额小于存入数量!";
	public static final int ERROR_SAFE_TAKELESS			= 5303;	// "保险箱取钱失败，保险箱余额小于取出数量!";
	public static final int ERROR_SAFE_NULL				= 5399;	//"保险箱信息指令不存在!";
	
	public static final int ERROR_LESS_MONEY			= 5401;	//"水上币不足!";
	public static final int ERROR_LESS_GOLD				= 5402;	//"游戏币不足!";
	public static final int ERROR_PROP_NULL				= 5403;	//"道具不存在!";
	public static final int ERROR_PROP_NULLTARGET		= 5404;	// "赠送对象不存在!";
	public static final int ERROR_PROP_NULLACTION		= 5405;	//"有效期类的道具不能赠送,只能在通过[购买赠送]功能进行赠送!";
	public static final int ERROR_PROP_LESS				= 5406;	//"您身上道具数量不足,无法赠送!";
	public static final int ERROR_PROP_NULLTARGETID		= 5407;	//"赠送用户ID错误,无法赠送!";
	public static final int ERROR_PROP_SHOP_NUM			= 5408;	//"道具购买数量不能为0!";
	public static final int ERROR_PROP_VIP				= 5421;	//"由于您已使用更高级的VIP卡,因此此VIP卡无法携带在身上,同时此VIP卡和赠送的道具已存放到您的道具包中,赠送的游戏币已存入您的帐号!";
	
	
	
	public static final int ERROR_APPLYCHAIR_OCCUPANCY	= 8000;	//"您下手慢了一步,请选择其他坐位";
	public static final int ERROR_APPLYCHAIR_NULLACCOUNT= 8001;	//"您的账号信息有误,无法坐下.";
	public static final int ERROR_APPLYCHAIR_ALREADY	= 8002;	//"您已经坐下,不能重复入座.";
	
	
	public static final int ERROR_UNKNOW				= 9999;	//"系统错误";
	
	//public static final int ERROR_SIGNED				= 10001] = "帐号长度不能超过16位";
	//public static final int ERROR_SIGNED				= 10002] = "帐号密码不能为空";
	//public static final int ERROR_SIGNED				= 10003] = "您尚未登录";
}
