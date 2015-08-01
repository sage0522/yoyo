package yoyo.common.utils;

public class HashHelper {
	public HashHelper() {
		
	}
	/**
	 * DJB Hash
	 */
	public static int DJBHash(String str){
		if(str == null) return 0;
		int hash = 5381;
		int i = 0,len = str.length();
		while (i<len)
		{
			hash += (hash << 5) + str.codePointAt(i);
			i++;
		}
		return hash&0x7FFFFFFF;
	}
}
