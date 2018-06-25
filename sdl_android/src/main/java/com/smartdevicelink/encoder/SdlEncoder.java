package com.smartdevicelink.encoder;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.smartdevicelink.proxy.interfaces.IVideoStreamListener;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SdlEncoder {

	private static final String TAG = "SdlEncoder";

	// parameters for the encoder
	private static final String _MIME_TYPE = "video/avc"; // H.264/AVC video
	// private static final String MIME_TYPE = "video/mp4v-es"; //MPEG4 video
	private int frameRate = 30;
	private int frameInterval = 5;
	private int frameWidth = 800;
	private int frameHeight = 480;
	private int bitrate = 6000000;

	// encoder state
	private MediaCodec mEncoder;
	private PipedOutputStream mOutputStream;
	private IVideoStreamListener mOutputListener;
	
	// allocate one of these up front so we don't need to do it every time
	private MediaCodec.BufferInfo mBufferInfo;

	// Codec-specific data (SPS and PPS)
	private byte[] mH264CodecSpecificData = null;

	public SdlEncoder () {
	}
	public void setFrameRate(int iVal){
		frameRate = iVal;
	}
	public void setFrameInterval(int iVal){
		frameInterval = iVal;
	}
	public void setFrameWidth(int iVal){
		frameWidth = iVal;	
	}
	public void setFrameHeight(int iVal){
		frameHeight = iVal;
	}
	public void setBitrate(int iVal){
		bitrate = iVal;	
	}
	public void setOutputStream(PipedOutputStream mStream){
		mOutputStream = mStream;
	}
	public void setOutputListener(IVideoStreamListener listener) {
		mOutputListener = listener;
	}
	public Surface prepareEncoder () {

		mBufferInfo = new MediaCodec.BufferInfo();

		MediaFormat format = MediaFormat.createVideoFormat(_MIME_TYPE, frameWidth,
				frameHeight);

		// Set some properties. Failing to specify some of these can cause the
		// MediaCodec
		// configure() call to throw an unhelpful exception.
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
				MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
		format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frameInterval);

		// Create a MediaCodec encoder, and configure it with our format. Get a
		// Surface
		// we can use for input and wrap it with a class that handles the EGL
		// work.
		//
		// If you want to have two EGL contexts -- one for display, one for
		// recording --
		// you will likely want to defer instantiation of CodecInputSurface
		// until after the
		// "display" EGL context is created, then modify the eglCreateContext
		// call to
		// take eglGetCurrentContext() as the share_context argument.
		try {
			mEncoder = MediaCodec.createEncoderByType(_MIME_TYPE);
		} catch (Exception e) {e.printStackTrace();}

		if(mEncoder != null) {
		   mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
           return mEncoder.createInputSurface();
		} else {
			return null;
		}
	}
	
	public void startEncoder () {
		if(mEncoder != null) {
		  mEncoder.start();
		}
	}
	
	/**
	 * Releases encoder resources.
	 */
	public void releaseEncoder() {
		if (mEncoder != null) {
			mEncoder.stop();
			mEncoder.release();
			mEncoder = null;
		}
		if (mOutputStream != null) {
			try {
				mOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mOutputStream = null;
		}
		mH264CodecSpecificData = null;
	}

	/**
	 * Extracts all pending data from the encoder
	 * <p>
	 * If endOfStream is not set, this returns when there is no more data to
	 * drain. If it is set, we send EOS to the encoder, and then iterate until
	 * we see EOS on the output. Calling this with endOfStream set should be
	 * done once, right before stopping the muxer.
	 */
	public void drainEncoder(boolean endOfStream) {
		final int TIMEOUT_USEC = 10000;

		if(mEncoder == null || (mOutputStream == null && mOutputListener == null)) {
		   return;			
		}
		if (endOfStream) {
			  mEncoder.signalEndOfInputStream();
		}

		ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
		while (true) {
			int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo,
					TIMEOUT_USEC);
			if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				if (!endOfStream) {
					break; // out of while
				}
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// not expected for an encoder
				encoderOutputBuffers = mEncoder.getOutputBuffers();
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				if (mH264CodecSpecificData == null) {
					MediaFormat format = mEncoder.getOutputFormat();
					mH264CodecSpecificData = EncoderUtils.getCodecSpecificData(format);
				} else {
					Log.w(TAG, "Output format change notified more than once, ignoring.");
				}
			} else if (encoderStatus < 0) {
			} else {
				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
					// If we already retrieve codec specific data via OUTPUT_FORMAT_CHANGED event,
					// we do not need this data.
					if (mH264CodecSpecificData != null) {
						mBufferInfo.size = 0;
					} else {
						Log.i(TAG, "H264 codec specific data not retrieved yet.");
					}
				}

				if (mBufferInfo.size != 0) {
					ByteBuffer encoderOutputBuffer = encoderOutputBuffers[encoderStatus];
					byte[] dataToWrite = null;
					int dataOffset = 0;

					// append SPS and PPS in front of every IDR NAL unit
					if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
							&& mH264CodecSpecificData != null) {
						dataToWrite = new byte[mH264CodecSpecificData.length + mBufferInfo.size];
						System.arraycopy(mH264CodecSpecificData, 0,
								dataToWrite, 0, mH264CodecSpecificData.length);
						dataOffset = mH264CodecSpecificData.length;
					} else {
						dataToWrite = new byte[mBufferInfo.size];
					}

					try {
						encoderOutputBuffer.position(mBufferInfo.offset);
						encoderOutputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

						encoderOutputBuffer.get(dataToWrite, dataOffset, mBufferInfo.size);

						if (mOutputStream != null) {
							mOutputStream.write(dataToWrite, 0, mBufferInfo.size);
						} else if (mOutputListener != null) {
							mOutputListener.sendFrame(
									dataToWrite, 0, dataToWrite.length, mBufferInfo.presentationTimeUs);
						}
					} catch (Exception e) {}
				}

				mEncoder.releaseOutputBuffer(encoderStatus, false);

				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					break; // out of while
				}
			}
		}
	}
}