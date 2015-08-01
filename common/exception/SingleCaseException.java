package yoyo.common.exception;

public class SingleCaseException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SingleCaseException() {
		super("该类为 单例模式,不可以重复创建,请使用 getInstance()获取");
	}
	
}
