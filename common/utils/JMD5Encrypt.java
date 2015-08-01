package yoyo.common.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class JMD5Encrypt {

	//
	private static MessageDigest messageDigest = null;
	/**
	 * MD5加密字符串
	 * @param pszSrcData	加密内容
	 * @return	加密结果
	 */
	public static String _EncryptData(String pszSrcData){
		return _EncryptData(pszSrcData,"GB2312");
	}
	/**
	 * MD5加密字符串
	 * @param pszSrcData	加密内容
	 * @param charset	编码方式  【中文使用UTF-8和GB2312加密结果是不同的】
	 * @return	MD5 加密结果
	 */
	public static String _EncryptData(String pszSrcData,String charset){
		if(pszSrcData==null) return null;
		
		if(messageDigest==null){
			try {
				messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		
		messageDigest.reset();
		try {
			messageDigest.update(pszSrcData.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return _bytes2String( messageDigest.digest() );
	}
	/**
	 * 字节转字符串
	 * @param bBytes
	 * @return
	 */
	private static String _bytes2String(byte[] bBytes){
		String szHex;
		StringBuffer md5StrBuff = new StringBuffer();
		for (int i = 0; i < bBytes.length; i++){
			szHex = Integer.toHexString(0xFF & bBytes[i]);
			if (szHex.length() == 2) {
				md5StrBuff.append(szHex);
			} else{
				md5StrBuff.append("0").append(szHex);
			}
		}
		return md5StrBuff.toString();
	}
}
