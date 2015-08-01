package yoyo.network.event;

import java.nio.ByteBuffer;

public class CMD_KN_DetectSocket {
	public final short size = 16;
	public long lSendTime;				//发送时间
	public long lRecvTime;				//接收时间
	public CMD_KN_DetectSocket() {
		lSendTime = 0;
		lRecvTime = 0;
	}
	public ByteBuffer ToByteBuffer(){
		ByteBuffer result = ByteBuffer.allocate( size );
		result.putLong(lSendTime);
		result.putLong(lRecvTime);
		return result;
	}
}
