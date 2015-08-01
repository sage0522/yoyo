package yoyo.common.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class JUtils {
	public static int _AddressToInt(String ip) {
		if (ip.split(".").length == 3) {
			ip = ip + ".0";
		}
		String[] strs = ip.split("\\.");
		return (Integer.parseUnsignedInt(strs[0]) << 24)
				+ (Integer.parseUnsignedInt(strs[1]) << 16)
				+ (Integer.parseUnsignedInt(strs[2]) << 8)
				+ (Integer.parseUnsignedInt(strs[3]));
	}
	public static String _InetNtoa(int dwServerAddr) {
		int b0 = (dwServerAddr >> 24)& 0x000000ff;
		int b1 = (dwServerAddr >> 16)& 0x000000ff; 
		int b2 = (dwServerAddr >> 8) & 0x000000ff;
		int b3 = dwServerAddr & 0x000000ff;
		String result =  b0 + "." + b1 + "." + b2 + "." + b3;
		return result;
	}
	/**
	 * 获取 Socket Index 值
	 * @param dwSocketID
	 * @return
	 */
	public static short _loword(int dwSocketID)		
	{
		return (short) (dwSocketID & 0xffff);
	}
	/**
	 * 获取 Socket RoundID 值
	 * @param dwSocketID
	 * @return
	 */
	public static short _hiword(int dwSocketID)
	{
		return (short) ((dwSocketID >> 16) & 0xffff);
	}
	
	public static short _max(short a, short b)
	{
		if (a >= b)
			return a;
		return b;
	}
	
	public static void _putStringBuffer(ByteBuffer bytebuffer,String value,int length){
		
		int postion = bytebuffer.position();
		value = value==null?"":value;
		try {
			bytebuffer.put(value.getBytes("gb2312"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		postion = bytebuffer.position() - postion;
		while(postion<length){
			postion++;
			bytebuffer.put((byte) 0);
		}
	}
	public static String _getStringBuffer(ByteBuffer pBuffer,int length){
		byte[] bytes = new byte[length];
		pBuffer.get(bytes);
		String result = null; 
		try {
			result = new String(bytes, "gb2312");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result.trim();
	}
	
	public static void _zeroArray(boolean[] values){
		for(int i = 0;i<values.length;i++)
		{
			values[i] = false;
		}
	}
	public static void _zeroArray(byte[] values){
		for(int i = 0;i<values.length;i++)
		{
			values[i] = 0;
		}
	}
	public static void _zeroArray(int[] values){
		for(int i = 0;i<values.length;i++)
		{
			values[i] = 0;
		}
	}
	public static void _zeroArray(short[] values){
		for(int i = 0;i<values.length;i++)
		{
			values[i] = 0;
		}
	}
	public static void _zeroArray(double[] values){
		for(int i = 0;i<values.length;i++)
		{
			values[i] = 0;
		}
	}
	public static void _zeroTwoDimension(byte[][] bytes){
		for(int i = 0;i<bytes.length;i++)
		{
			for(int j = 0;j<bytes[i].length;j++){
				bytes[i][j] = 0;
			}
		}
	}
}
