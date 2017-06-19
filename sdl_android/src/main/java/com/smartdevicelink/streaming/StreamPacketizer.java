package com.smartdevicelink.streaming;

import java.io.IOException;
import java.io.InputStream;

import com.smartdevicelink.SdlConnection.SdlConnection;
import com.smartdevicelink.SdlConnection.SdlSession;
import com.smartdevicelink.protocol.ProtocolMessage;
import com.smartdevicelink.protocol.enums.SessionType;

public class StreamPacketizer extends AbstractPacketizer implements Runnable{

	public final static String TAG = "StreamPacketizer";

	private Thread t = null;


	private final static int TLS_MAX_RECORD_SIZE = 16384;
	private final static int TLS_RECORD_HEADER_SIZE = 5;
	private final static int TLS_RECORD_MES_AUTH_CDE_SIZE = 32;
	private final static int TLS_MAX_RECORD_PADDING_SIZE = 256;


	private final static int BUFF_READ_SIZE = TLS_MAX_RECORD_SIZE - TLS_RECORD_HEADER_SIZE - TLS_RECORD_MES_AUTH_CDE_SIZE - TLS_MAX_RECORD_PADDING_SIZE;

	public SdlConnection sdlConnection = null;
    private Object mPauseLock;
    private boolean mPaused;
    private boolean isServiceProtected = false;
    
	public StreamPacketizer(IStreamListener streamListener, InputStream is, SessionType sType, byte rpcSessionID, SdlSession session) throws IOException {
		super(streamListener, is, sType, rpcSessionID, session);
        mPauseLock = new Object();
        mPaused = false;
        isServiceProtected = _session.isServiceProtected(_serviceType);
		if(isServiceProtected){ //If our service is encrypted we can only use 1024 as the max buffer size. 
			bufferSize = BUFF_READ_SIZE;
			buffer = new byte[bufferSize];
		}
		
	}

	public void start() throws IOException {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	public void stop() {

		if (t != null)
		{
			t.interrupt();
			t = null;
		}

	}

	public void run() {
		int length;
		try 
		{
			while (t != null && !t.isInterrupted()) 
			{
				synchronized(mPauseLock)
				{
					while (mPaused)
                    {
						try
                        {
							mPauseLock.wait();
                        }
                        catch (InterruptedException e) {}
                    }
                }

				length = is.read(buffer, 0, bufferSize);
				
				if (length >= 0) 
				{
					ProtocolMessage pm = new ProtocolMessage();
					pm.setSessionID(_rpcSessionID);
					pm.setSessionType(_serviceType);
					pm.setFunctionID(0);
					pm.setCorrID(0);
					pm.setData(buffer, length);
					pm.setPayloadProtected(isServiceProtected);
										
					if (t != null && !t.isInterrupted())
						_streamListener.sendStreamPacket(pm);
				}
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			 if (sdlConnection != null)
			 {
				 sdlConnection.endService(_serviceType, _rpcSessionID);
			 }

		}
	}

    @Override
	public void pause() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }

    @Override
    public void resume() {
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }
}
