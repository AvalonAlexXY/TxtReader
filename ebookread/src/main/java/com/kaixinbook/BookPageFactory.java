package com.kaixinbook;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Vector;

public class BookPageFactory {

	private static final String TAG = "BookPageFactory";
	private File book_file = null;
	private int m_backColor = 0xffff9e85; // 背景颜色
	private int m_bgColor ;
	private Bitmap m_book_bg = null;
	private int m_fontSize = 20;
	private boolean m_isfirstPage, m_islastPage;
	private Vector<String> m_lines = new Vector<String>();
	private MappedByteBuffer m_mbBuf = null;// 内存中的图书字符
	private int m_mbBufBegin = 0;// 当前页起始位置
	private int m_mbBufEnd = 0;// 当前页终点位置

	private int m_mbBufLen = 0; // 图书总长度

	private String title = "标题";

	private String m_strCharsetName = "UTF-8";

	private int m_textColor = Color.rgb(28, 28, 28);
	private int m_titleColor = Color.rgb(204,204,204);

	private int marginHeight = 15; // 上下与边缘的距离
	private int marginWidth = 36; // 左右与边缘的距离
	private int mHeight;
	private int mLineCount; // 每页可以显示的行数
	private Paint mPaint;
	private Paint titlePaint;

	private float mVisibleHeight; // 绘制内容的宽
	private float mVisibleWidth; // 绘制内容的宽
	private int mWidth;

	public BookPageFactory(int w, int h) {
		mWidth = w;
		mHeight = h;
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);// 画笔
		titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);// 标题笔
		mPaint.setTextAlign(Align.LEFT);// 做对其
		mPaint.setTextSize(m_fontSize);// 字体大小
		mPaint.setColor(m_textColor);// 字体颜色
		titlePaint.setTextAlign(Align.LEFT);// 做对其
		titlePaint.setTextSize(m_fontSize);// 字体大小
		titlePaint.setColor(m_textColor);// 字体颜色
		mVisibleWidth = mWidth - marginWidth * 2;
		mVisibleHeight = mHeight - marginHeight * 2;
		mLineCount = (int) (mVisibleHeight / (m_fontSize+30)) - 1; // 可显示的行数,-1是因为底部显示进度的位置容易被遮住
	}

	public void setTitle(String title){
		this.title = title;
	}

	public int getM_fontSize() {
		return m_fontSize;
	}

	public int getmLineCount() {
		return mLineCount;
	}

	public boolean isfirstPage() {
		return m_isfirstPage;
	}

	public boolean islastPage() {
		return m_islastPage;
	}

	public int getM_bgColor(){ return m_bgColor;}

	public void setM_bgColor(int bgColor) {this.m_bgColor = bgColor;}

	/**
	 * 向后翻页
	 *
	 * @throws IOException
	 */
	public void nextPage() throws IOException {
		if (m_mbBufEnd >= m_mbBufLen) {
			m_islastPage = true;
			return;
		} else
			m_islastPage = false;
		m_lines.clear();
		m_mbBufBegin = m_mbBufEnd;// 下一页页起始位置=当前页结束位置
		m_lines = pageDown();
	}

	public void currentPage() throws IOException {
		m_lines.clear();
		m_lines = pageDown();
	}

	public void onDraw(Canvas c) {
		mPaint.setTextSize(m_fontSize);
		mPaint.setColor(m_textColor);
		if (m_lines.size() == 0)
			m_lines = pageDown();
		if (m_lines.size() > 0) {
			if (m_book_bg == null)
				c.drawColor(m_bgColor);
			else
				c.drawBitmap(m_book_bg, 0, 0, null);
			int y = marginHeight;
			for (int i = 0; i<m_lines.size();i++) {
				String strLine = m_lines.get(i);
				if(i == 0){
					y += m_fontSize+80;
				}else{
					y += m_fontSize+25;
				}
				c.drawText(strLine, marginWidth, y, mPaint);
			}
		}
		float fPercent = (float) (m_mbBufBegin * 1.0 / m_mbBufLen);
		DecimalFormat df = new DecimalFormat("#0.0");
		String strPercent = df.format(fPercent * 100) + "%";
		int nPercentWidth = (int) mPaint.measureText("999.9%") + 1;
		c.drawText(strPercent, mWidth - nPercentWidth, mHeight - 35, mPaint);
		//上方标题
		titlePaint.setTextSize(42);
		titlePaint.setColor(m_titleColor);
		c.drawText(title,marginWidth,70,titlePaint);
	}

	/**
	 *
	 * @param strFilePath
	 * @param begin
	 *            表示书签记录的位置，读取书签时，将begin值给m_mbBufEnd，在读取nextpage，及成功读取到了书签
	 *            记录时将m_mbBufBegin开始位置作为书签记录
	 *
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	public void openbook(String strFilePath, int begin) throws IOException {
		book_file = new File(strFilePath);
		long lLen = book_file.length();
		m_mbBufLen = (int) lLen;
		m_mbBuf = new RandomAccessFile(book_file, "r").getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, lLen);
		Log.d(TAG, "total lenth：" + m_mbBufLen);
		// 设置已读进度
		if (begin >= 0) {
			m_mbBufBegin = begin;
			m_mbBufEnd = begin;
		} else {
		}
	}

	/**
	 * 输入一段字符流，判断其什么编码格式
	 * @param head
	 * @return
	 */
	private String codetype(byte[] head) {
		String code = "";
		if(head[0] == -29 && head[1] == -128 && head[2] == -128)
			code = "UTF-8";
		else {
			code = "GBK";
		}
		return code;
	}

	/**
	 * 画指定页的下一页
	 *
	 * @return 下一页的内容 Vector<String>
	 */
	protected Vector<String> pageDown() {
		mPaint.setTextSize(m_fontSize);
		mPaint.setColor(m_textColor);
		String strParagraph = "";
		Vector<String> lines = new Vector<String>();

//		byte[] paraBuf1 = readParagraphForward(m_mbBufEnd);
//		m_mbBufEnd += paraBuf1.length;// 每次读取后，记录结束点位置，该位置是段落结束位置
//		byte[] paraBuf2 = readParagraphForward(m_mbBufEnd);
//		m_strCharsetName = codetype(paraBuf2);

		while (lines.size() < mLineCount && m_mbBufEnd < m_mbBufLen) {
			byte[] paraBuf = readParagraphForward(m_mbBufEnd);
			m_mbBufEnd += paraBuf.length;// 每次读取后，记录结束点位置，该位置是段落结束位置
			byte[] paraBuf2 = readParagraphForward(m_mbBufEnd);
			if(paraBuf2.length!=0) {
				m_strCharsetName = codetype(paraBuf2);
			}
			try {
				strParagraph = new String(paraBuf, m_strCharsetName);// 转换成制定GBK编码
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "pageDown->转换编码失败", e);
			}
			String strReturn = "";
			// 替换掉回车换行符
			if (strParagraph.indexOf("\r\n") != -1) {
				strReturn = "\r\n";
				strParagraph = strParagraph.replaceAll("\r\n", "");
			} else if (strParagraph.indexOf("\n") != -1) {
				strReturn = "\n";
				strParagraph = strParagraph.replaceAll("\n", "");
			}

			if (strParagraph.length() == 0) {
				lines.add(strParagraph);
			}
			while (strParagraph.length() > 0) {
				// 画一行文字
				int nSize = mPaint.breakText(strParagraph, true, mVisibleWidth,
						null);
				lines.add(strParagraph.substring(0, nSize));
				strParagraph = strParagraph.substring(nSize);// 得到剩余的文字
				// 超出最大行数则不再画
				if (lines.size() >= mLineCount) {
					break;
				}
			}
			// 如果该页最后一段只显示了一部分，则从新定位结束点位置
			if (strParagraph.length() != 0) {
				try {
					m_mbBufEnd -= (strParagraph + strReturn)
							.getBytes(m_strCharsetName).length;
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, "pageDown->记录结束点位置失败", e);
				}
			}
		}
		return lines;
	}

	/**
	 * 得到上上页的结束位置
	 */
	protected void pageUp() {
		if (m_mbBufBegin < 0)
			m_mbBufBegin = 0;
		Vector<String> lines = new Vector<String>();
		String strParagraph = "";

//		byte[] paraBuf1 = readParagraphForward(m_mbBufEnd);
//		m_mbBufEnd += paraBuf1.length;// 每次读取后，记录结束点位置，该位置是段落结束位置
//		byte[] paraBuf2= readParagraphForward(m_mbBufEnd);
//		m_strCharsetName = codetype(paraBuf2);

		while (lines.size() < mLineCount && m_mbBufBegin > 0) {
			Vector<String> paraLines = new Vector<String>();
			byte[] paraBuf = readParagraphBack(m_mbBufBegin);
			m_mbBufBegin -= paraBuf.length;// 每次读取一段后,记录开始点位置,是段首开始的位置
			byte[] paraBuf2 = readParagraphForward(m_mbBufBegin);
			if(paraBuf2.length!=0) {
				m_strCharsetName = codetype(paraBuf2);
			}
			try {
				strParagraph = new String(paraBuf, m_strCharsetName);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "pageUp->转换编码失败", e);
			}
			strParagraph = strParagraph.replaceAll("\r\n", "");
			strParagraph = strParagraph.replaceAll("\n", "");
			// 如果是空白行，直接添加
			if (strParagraph.length() == 0) {
				paraLines.add(strParagraph);
			}
			while (strParagraph.length() > 0) {
				// 画一行文字
				int nSize = mPaint.breakText(strParagraph, true, mVisibleWidth,
						null);
				paraLines.add(strParagraph.substring(0, nSize));
				strParagraph = strParagraph.substring(nSize);
			}
			lines.addAll(0, paraLines);
		}

		while (lines.size() > mLineCount) {
			try {
				m_mbBufBegin += lines.get(0).getBytes(m_strCharsetName).length;
				lines.remove(0);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "pageUp->记录起始点位置失败", e);
			}
		}
		m_mbBufEnd = m_mbBufBegin;// 上上一页的结束点等于上一页的起始点
		return;
	}

	/**
	 * 向前翻页
	 *
	 * @throws IOException
	 */
	public void prePage() throws IOException {
		if (m_mbBufBegin <= 0) {
			m_mbBufBegin = 0;
			m_isfirstPage = true;
			return;
		} else
			m_isfirstPage = false;
		m_lines.clear();
		pageUp();
		m_lines = pageDown();
	}

	/**
	 * 读取指定位置的上一个段落
	 *
	 * @param nFromPos
	 * @return byte[]
	 */
	protected byte[] readParagraphBack(int nFromPos) {
		int nEnd = nFromPos;
		int i;
		byte b0, b1;
		if (m_strCharsetName.equals("UTF-16LE")) {
			i = nEnd - 2;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				b1 = m_mbBuf.get(i + 1);
				if (b0 == 0x0a && b1 == 0x00 && i != nEnd - 2) {
					i += 2;
					break;
				}
				i--;
			}

		} else if (m_strCharsetName.equals("UTF-16BE")) {
			i = nEnd - 2;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				b1 = m_mbBuf.get(i + 1);
				if (b0 == 0x00 && b1 == 0x0a && i != nEnd - 2) {
					i += 2;
					break;
				}
				i--;
			}
		} else {
			i = nEnd - 1;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				if (b0 == 0x0a && i != nEnd - 1) {// 0x0a表示换行符
					i++;
					break;
				}
				i--;
			}
		}
		if (i < 0)
			i = 0;
		int nParaSize = nEnd - i;
		int j;
		byte[] buf = new byte[nParaSize];
		for (j = 0; j < nParaSize; j++) {
			buf[j] = m_mbBuf.get(i + j);
		}
		return buf;
	}

	/**
	 * 读取指定位置的下一个段落
	 *
	 * @param nFromPos
	 * @return byte[]
	 */
	protected byte[] readParagraphForward(int nFromPos) {
		int nStart = nFromPos;
		int i = nStart;
		byte b0, b1;
		// 根据编码格式判断换行
		if (m_strCharsetName.equals("UTF-16LE")) {
			while (i < m_mbBufLen - 1) {
				b0 = m_mbBuf.get(i++);
				b1 = m_mbBuf.get(i++);
				if (b0 == 0x0a && b1 == 0x00) {
					break;
				}
			}
		} else if (m_strCharsetName.equals("UTF-16BE")) {
			while (i < m_mbBufLen - 1) {
				b0 = m_mbBuf.get(i++);
				b1 = m_mbBuf.get(i++);
				if (b0 == 0x00 && b1 == 0x0a) {
					break;
				}
			}
		} else {
			while (i < m_mbBufLen) {
				b0 = m_mbBuf.get(i++);
				if (b0 == 0x0a) {
					break;
				}
			}
		}
		int nParaSize = i - nStart;
		byte[] buf = new byte[nParaSize];
		for (i = 0; i < nParaSize; i++) {
			buf[i] = m_mbBuf.get(nFromPos + i);
		}
		return buf;
	}

	public void setBgBitmap(Bitmap BG) {
		m_book_bg = BG;
	}

	public void setM_fontSize(int m_fontSize) {
		this.m_fontSize = m_fontSize;
		mLineCount = (int) (mVisibleHeight / (m_fontSize+30)) - 1;
	}

	// 设置页面起始点
	public void setM_mbBufBegin(int m_mbBufBegin) {
		this.m_mbBufBegin = m_mbBufBegin;
	}

	// 设置页面结束点
	public void setM_mbBufEnd(int m_mbBufEnd) {
		this.m_mbBufEnd = m_mbBufEnd;
	}

	public int getM_mbBufBegin() {
		return m_mbBufBegin;
	}

	public String getFirstLineText() {
		return m_lines.size() > 0 ? m_lines.get(0) : "";
	}

	public int getM_textColor() {
		return m_textColor;
	}

	public void setM_textColor(int m_textColor) {
		this.m_textColor = m_textColor;
	}

	public int getM_mbBufLen() {
		return m_mbBufLen;
	}

	public int getM_mbBufEnd() {
		return m_mbBufEnd;
	}

}
