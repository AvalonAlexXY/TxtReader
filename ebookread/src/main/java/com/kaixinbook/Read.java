package com.kaixinbook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.kaixinbook.adapter.BookMarksAdapter;
import com.kaixinbook.helper.MarkHelper;
import com.kaixinbook.util.poponDismissListener;
import com.kaixinbook.bean.MarkVo;
import com.zhy.autolayout.AutoLayoutActivity;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// import android.view.Gravity;

public class Read extends AutoLayoutActivity implements OnClickListener, OnSeekBarChangeListener {

    private static final String TAG = "Read2";
    private static int begin = 0;// 记录的书籍开始位置
    public static Canvas mCurPageCanvas, mNextPageCanvas;
    private static String word = "";// 记录当前页面的文字
    private int a = 0, b = 0;// 记录toolpop的位置
    private ImageView bookBtn1, bookBtn3, bookBtn4;
    private String bookPath;// 记录读入书的路径
    protected long count = 1;
    private SharedPreferences.Editor editor;
    private ImageButton imageBtn2, imageBtn3_1, imageBtn3_2;
    private ImageButton imageBtn4_1, imageBtn4_2;
    //	private int light; // 亮度值
    private WindowManager.LayoutParams lp;
    private TextView markEdit4;
    private MarkHelper markhelper;
    private Bitmap mCurPageBitmap, mNextPageBitmap;
    private Context mContext = null;
    private PageWidget mPageWidget;
    private PopupWindow mPopupWindow;
    protected int PAGE = 1;
    private BookPageFactory pagefactory;
    private View bottomView;
    int screenHeight;
    int readHeight; // 电子书显示高度
    int screenWidth;
    private SeekBar seekBar1, seekBar2, seekBar4;
    private Boolean show = false;// popwindow是否显示
    private int size = 30; // 字体大小
    private SharedPreferences sp;
    int defaultSize = 55;
    private int readPageNum = 1;
    // 实例化Handler
    public Handler mHandler = new Handler() {
        // 接收子线程发来的消息，同时更新UI
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    begin = msg.arg1;
                    pagefactory.setM_mbBufBegin(begin);
                    pagefactory.setM_mbBufEnd(begin);
                    postInvalidateUI();
                    break;
                case 1:
                    pagefactory.setM_mbBufBegin(begin);
                    pagefactory.setM_mbBufEnd(begin);
                    postInvalidateUI();
                    break;
                default:
                    break;
            }
        }
    };
    private String titleString;
    private ImageView iv_back;
    private TextView tv_title;
    private ImageView iv_menu1;
    private ImageView iv_menu2;
    private LinearLayout linearlayout_title;
    private RelativeLayout layout_adjust_wordsize;
    private LinearLayout layout_book_tips;
    private TextView tv_size;
    private ImageView iv_minus;
    private ImageView iv_add;
    private ListView lv_book_marks;
    private RelativeLayout layout_jumpto;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.tool_color));
        }

        mContext = getBaseContext();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth= metrics.widthPixels;
        screenHeight = metrics.heightPixels;
//		defaultSize = (screenWidth * 20) / 320;
        readHeight = screenHeight - (20* screenWidth) / 320;

        mCurPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        mNextPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        mCurPageCanvas = new Canvas(mCurPageBitmap);
        mNextPageCanvas = new Canvas(mNextPageBitmap);

        mPageWidget = new PageWidget(this,screenWidth,readHeight);// 页面

        setContentView(R.layout.read);

        initView();

        receiveIntent();

        initTitle();

        initWidget();

        initFactory();

        setPop();
    }

    private void initView() {
        RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.readlayout);
        rlayout.addView(mPageWidget);

        findViewById(R.id.tv_show_pop).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (show) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                    show = false;
                    mPopupWindow.dismiss();
                    bottomDismiss();
                    linearlayout_title.setVisibility(View.GONE);
                } else {
                    linearlayout_title.setVisibility(View.VISIBLE);
                    show = true;
                    pop();
                }
            }
        });
    }

    private void initFactory() {
        // 提取记录在sharedpreferences的各种状态
        sp = getSharedPreferences("config", MODE_PRIVATE);
        editor = sp.edit();
        getSize();// 获取配置文件中的size大小
        count = sp.getLong(bookPath + "count", 1);

        pagefactory = new BookPageFactory(screenWidth, readHeight);// 书工厂
        pagefactory.setM_bgColor(getResources().getColor(R.color.bg_color));
        pagefactory.setM_textColor(Color.rgb(28, 28, 28));
        begin = sp.getInt(bookPath + "begin", 0);
        try {
            pagefactory.setTitle(titleString);
            pagefactory.openbook(bookPath, begin);// 从指定位置打开书籍，默认从开始打开
            pagefactory.setM_fontSize(size);
            pagefactory.onDraw(mCurPageCanvas);
        } catch (IOException e1) {
            Log.e(TAG, "打开电子书失败", e1);
            Toast.makeText(this, "打开电子书失败", Toast.LENGTH_SHORT).show();
        }
        markhelper = new MarkHelper(this);
    }

    private void initWidget() {
        mPageWidget.setBitmaps(mCurPageBitmap, mCurPageBitmap);
        mPageWidget.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                boolean ret = false;
                if (v == mPageWidget) {
                    if (!show) {
                        if (e.getAction() == MotionEvent.ACTION_DOWN) {
                            if (e.getY() > readHeight) {// 超出范围了，表示单击到广告条，则不做翻页
                                return false;
                            }
                            mPageWidget.abortAnimation();
                            mPageWidget.calcCornerXY(e.getX(), e.getY());
                            pagefactory.onDraw(mCurPageCanvas);
                            if (mPageWidget.DragToRight()) {// 左翻
                                readPageNum ++;
                                try {
                                    pagefactory.prePage();
                                    begin = pagefactory.getM_mbBufBegin();// 获取当前阅读位置
                                    word = pagefactory.getFirstLineText();// 获取当前阅读位置的首行文字
                                } catch (IOException e1) {
                                    Log.e(TAG, "onTouch->prePage error", e1);
                                }
                                if (pagefactory.isfirstPage()) {
                                    Toast.makeText(mContext, "当前是第一页", Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                                iv_menu1.setVisibility(View.VISIBLE);
                                iv_menu2.setVisibility(View.GONE);
                                pagefactory.onDraw(mNextPageCanvas);
                            } else {// 右翻
                                readPageNum ++;
                                try {
                                    pagefactory.nextPage();
                                    begin = pagefactory.getM_mbBufBegin();// 获取当前阅读位置
                                    word = pagefactory.getFirstLineText();// 获取当前阅读位置的首行文字
                                } catch (IOException e1) {
                                    Log.e(TAG, "onTouch->nextPage error", e1);
                                }
                                if (pagefactory.islastPage()) {
                                    Toast.makeText(mContext, "已经是最后一页了", Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                                iv_menu1.setVisibility(View.VISIBLE);
                                iv_menu2.setVisibility(View.GONE);
                                pagefactory.onDraw(mNextPageCanvas);
                            }
                            mPageWidget.setBitmaps(mCurPageBitmap, mNextPageBitmap);
                        }
                        editor.putInt(bookPath + "begin", begin).commit();
                        ret = mPageWidget.doTouchEvent(e);
                        return ret;
                    }
                }
                return false;
            }
        });
    }

    private void receiveIntent() {
        Intent intent = getIntent();
        bookPath = intent.getStringExtra("path");
        titleString = intent.getStringExtra("title");
    }

    private void initTitle() {
        linearlayout_title = (LinearLayout) findViewById(R.id.linearlayout_title);
        iv_back = (ImageView) findViewById(R.id.ic_readbar_back);
        tv_title = (TextView) findViewById(R.id.tv_readbar_title);
        iv_menu1 = (ImageView) findViewById(R.id.ic_readbar_menu1);
        iv_menu2 = (ImageView) findViewById(R.id.ic_readbar_menu2);

        if(titleString!=null && titleString.length()>0){
            if(titleString.contains(".")){
                tv_title.setText(titleString.substring(0, titleString.lastIndexOf(".")));
            }else {
                tv_title.setText(titleString);
            }
        }

        iv_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.putExtra("pagenum", readPageNum);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        iv_menu1.setOnClickListener(this);
        iv_menu2.setOnClickListener(this);
    }

    /**
     * popupwindow的弹出,工具栏
     */
    public void pop() {
        mPopupWindow.showAtLocation(mPageWidget, Gravity.BOTTOM, 0, 0);
        bookBtn1 = (ImageView) bottomView.findViewById(R.id.bookBtn1);
        bookBtn3 = (ImageView) bottomView.findViewById(R.id.bookBtn3);
        bookBtn4 = (ImageView) bottomView.findViewById(R.id.bookBtn4);
        bookBtn1.setOnClickListener(this);
        bookBtn3.setOnClickListener(this);
        bookBtn4.setOnClickListener(this);
    }

    /**
     * 关闭55个弹出pop
     */
    public void bottomDismiss() {
        layout_jumpto.setVisibility(View.GONE);
        layout_book_tips.setVisibility(View.GONE);
        layout_adjust_wordsize.setVisibility(View.GONE);
    }

    /**
     * 读取配置文件中字体大小
     */
    private void getSize() {
        size = sp.getInt("size", defaultSize);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.bookBtn1) {
            if(layout_adjust_wordsize.getVisibility() == View.VISIBLE){
                layout_adjust_wordsize.setVisibility(View.GONE);
            }else{
                bottomDismiss();
                adjustWordSize();//改变字体大小
                layout_adjust_wordsize.setVisibility(View.VISIBLE);
            }
        } else if (i == R.id.bookBtn3) {
            if(layout_book_tips.getVisibility() == View.VISIBLE){
                layout_book_tips.setVisibility(View.GONE);
            }else{
                bottomDismiss();
                showBookTips();  //通过数据库添加列表
                layout_book_tips.setVisibility(View.VISIBLE);
            }
        } else if (i == R.id.bookBtn4) {
            if(layout_jumpto.getVisibility() == View.VISIBLE){
                layout_jumpto.setVisibility(View.GONE);
            }else{
                bottomDismiss();
                jumpTo(); //跳转到指定地点
                layout_jumpto.setVisibility(View.VISIBLE);
            }
        } else if( i == R.id.ic_readbar_menu1){
            addTipToMine();
        } else if(i == R.id.ic_readbar_menu2){
            deleteTipToMine();
        }
    }

    private void jumpTo() {
        float fPercent = (float) (begin * 1.0 / pagefactory.getM_mbBufLen());
        DecimalFormat df = new DecimalFormat("#0");
        String strPercent = df.format(fPercent * 100) + "%";
        markEdit4.setText(strPercent);
        seekBar4.setProgress(Integer.parseInt(df.format(fPercent * 100)));
        seekBar4.setOnSeekBarChangeListener(this);
        try {
            pagefactory.nextPage();
            pagefactory.prePage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showBookTips() {
        final ArrayList<MarkVo> markList = getBookPageList();
        BookMarksAdapter bookMarksAdapter = new BookMarksAdapter(Read.this,markList);
        lv_book_marks.setAdapter(bookMarksAdapter);

        bookMarksAdapter.setOnBookPageDeleteListener(new BookMarksAdapter.BookPageDeleteListener() {
            @Override
            public void deleteBookPage(String pageFirstStr) {
                ArrayList<MarkVo> markList = getBookPageList();
                boolean hasBookPage = false;
                for (int i = 0; i <markList.size() ; i++) {
                    if(markList.get(i).getBookPath().equals(pageFirstStr)){
                        hasBookPage = true;
                    }
                }
                if(!hasBookPage){
                    iv_menu1.setVisibility(View.VISIBLE);
                    iv_menu2.setVisibility(View.GONE);
                }
            }
        });



        /**
         * 点击列表项目，跳转到书签页面
         */
        lv_book_marks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                begin = markList.get(position).getBegin();
                System.out.println(begin);
                Message msg = new Message();
                msg.what = 0;
                msg.arg1 = begin;
                mHandler.sendMessage(msg);
                layout_book_tips.setVisibility(View.GONE);
            }
        });
    }

    private ArrayList<MarkVo> getBookPageList(){
        final ArrayList<MarkVo> markList = new ArrayList<MarkVo>();
        SQLiteDatabase dbSelect = markhelper.getReadableDatabase();
        String col[] = {"begin", "word", "time"};
        Cursor cur = dbSelect.query("markhelper", col, "path = '" + bookPath + "'", null, null, null, null);
        Integer num = cur.getCount();
        if (num == 0) {
            markList.clear();
            Toast.makeText(Read.this, "您还没有书签", Toast.LENGTH_SHORT).show();
        } else {
            while (cur.moveToNext()) {
                String s1 = cur.getString(cur.getColumnIndex("word"));
                String s2 = cur.getString(cur.getColumnIndex("time"));
                int b1 = cur.getInt(cur.getColumnIndex("begin"));
                int p = 0;
                int count = 10;
                MarkVo mv = new MarkVo(s1, p, count, b1, s2, bookPath);
                markList.add(mv);
            }
        }
        dbSelect.close();
        cur.close();
        return markList;
    }

    private void adjustWordSize() {
        size = sp.getInt("size", 50);
        tv_size.setText((size - 30)+ "号字");

        iv_minus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(size>40){
                    size --;
                    setSize();
                    tv_size.setText((size - 30) + "号字");
                }else{
                    Toast.makeText(Read.this,"请保持较大字体",Toast.LENGTH_SHORT).show();
                }
            }
        });

        iv_add.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(size<54) {
                    size ++;
                    setSize();
                    tv_size.setText((size - 30) + "号字");
                }
                else{
                    Toast.makeText(Read.this,"请保持较小字体",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteTipToMine() {
        SQLiteDatabase dbSelect = markhelper.getReadableDatabase();
        String col[] = {"begin", "word", "time"};
        Cursor cur = dbSelect.query("markhelper", col, "path = '" + bookPath + "'", null, null, null, null);
        Integer num = cur.getCount();
        if (num == 0) {
            dbSelect.delete("markhelper", "path='" + bookPath , null);
            Toast.makeText(Read.this, "您还没有书签", Toast.LENGTH_SHORT).show();
        } else {
            while (cur.moveToNext()) {
                String s1 = cur.getString(cur.getColumnIndex("word"));
                if(word.equals(s1)){
                    dbSelect.delete("markhelper", "path='" + bookPath + "' and begin ='" + begin
                            + "'", null);
                    Toast.makeText(Read.this, "书签删除成功", Toast.LENGTH_SHORT).show();
                }
            }
        }
        dbSelect.close();
        cur.close();
        layout_book_tips.setVisibility(View.GONE);
        iv_menu1.setVisibility(View.VISIBLE);
        iv_menu2.setVisibility(View.GONE);
    }

    private void addTipToMine() {
        SQLiteDatabase db = markhelper.getWritableDatabase();
        try {
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm ss");
            String time = sf.format(new Date());
            db.execSQL("insert into markhelper (path ,begin,word,time) values (?,?,?,?)",
                    new String[]{bookPath, begin + "", word, time});
            db.close();
            Toast.makeText(Read.this, "书签添加成功", Toast.LENGTH_SHORT).show();
        } catch (SQLException e) {
            Toast.makeText(Read.this, "该书签已存在", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(Read.this, "添加书签失败", Toast.LENGTH_SHORT).show();
        }
        layout_book_tips.setVisibility(View.GONE);
        iv_menu1.setVisibility(View.GONE);
        iv_menu2.setVisibility(View.VISIBLE);
    }

    /**
     * 初始化所有POPUPWINDOW
     */
    private void setPop() {
        bottomView = this.getLayoutInflater().inflate(R.layout.bookpop, null);
        mPopupWindow = new PopupWindow(bottomView, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        mPopupWindow.setOnDismissListener(new poponDismissListener(this));

        initAdjustWordSize();

        initBookTips();

        initJumpTo();


//        toolpop4 = this.getLayoutInflater().inflate(R.layout.tool44, null);
//		mToolpop4 = new PopupWindow(toolpop4, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
//		mToolpop4.setOnDismissListener(new poponDismissListener(this));

//
//
//		mToolpop4 = new PopupWindow(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
//		mToolpop4.setContentView(toolpop4);
//		mToolpop4.setFocusable(true);
//		mToolpop4.setBackgroundDrawable(getResources().getDrawable(R.drawable.read_dialog_bg));
//		mToolpop4.setTouchInterceptor(new OnTouchListener() {
//			@Override
//			public boolean onTouch(View arg0, MotionEvent event) {
//				if(event.getAction() == KeyEvent.KEYCODE_BACK || event.getAction() == MotionEvent.ACTION_OUTSIDE){
//					mToolpop4.dismiss();
//					bottomDismiss();
//					backgroundAlpha(1.0f);
//					return true;
//				}
//				return false;
//			}
//		});
    }

    private void initJumpTo() {
        layout_jumpto = (RelativeLayout) bottomView.findViewById(R.id.layout_jumpto);
        seekBar4 = (SeekBar) bottomView.findViewById(R.id.seekBar4);
        seekBar4.setMax(100);
        seekBar4.setOnSeekBarChangeListener(this);
        markEdit4 = (TextView) bottomView.findViewById(R.id.tv_turn_progress);
    }

    private void initBookTips() {
        layout_book_tips = (LinearLayout) bottomView.findViewById(R.id.layout_book_tips);
        lv_book_marks = (ListView) bottomView.findViewById(R.id.lv_bookcard);
    }

    private void initAdjustWordSize() {
        layout_adjust_wordsize = (RelativeLayout) bottomView.findViewById(R.id.layout_adjust_wordsize);
        tv_size = (TextView) bottomView.findViewById(R.id.tv_word_size);
        iv_minus = (ImageView) bottomView.findViewById(R.id.iv_size_minus);
        iv_add = (ImageView) bottomView.findViewById(R.id.iv_size_add);
    }

    /**
     * 记录配置文件中字体大小
     */
    private void setSize() {
        try {
            editor.putInt("size", size);
            editor.commit();
            pagefactory.setM_fontSize(size);
            pagefactory.setM_mbBufBegin(begin);
            pagefactory.setM_mbBufEnd(begin);
            postInvalidateUI();
        } catch (Exception e) {
            Log.e(TAG, "setSize-> Exception error", e);
        }
    }

    /**
     * 刷新界面
     */
    public void postInvalidateUI() {
        mPageWidget.abortAnimation();
        pagefactory.onDraw(mCurPageCanvas);
        try {
            pagefactory.currentPage();
            begin = pagefactory.getM_mbBufBegin();// 获取当前阅读位置
            word = pagefactory.getFirstLineText();// 获取当前阅读位置的首行文字
            pagefactory.nextPage();
            pagefactory.prePage();
        } catch (IOException e1) {
            Log.e(TAG, "postInvalidateUI->IOException error", e1);
        }
        pagefactory.onDraw(mNextPageCanvas);
        mPageWidget.setBitmaps(mCurPageBitmap, mNextPageBitmap);
        mPageWidget.postInvalidate();
    }

    /**
     * 改善activity亮度
     * @param bgAlpha
     */
    private void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        getWindow().setAttributes(lp);
    }


    /**
     * 判断是从哪个界面进入的READ
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent=new Intent();
            intent.putExtra("pagenum", readPageNum);
            setResult(RESULT_OK, intent);
            if (show) {// 如果popwindow正在显示
                bottomDismiss();
                linearlayout_title.setVisibility(View.GONE);
                backgroundAlpha(1.0f);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                show = false;
                mPopupWindow.dismiss();
            } else {
                Read.this.finish();
            }
        }
        return true;
    }

    /**
     * 添加对menu按钮的监听
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (show) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                show = false;
                mPopupWindow.dismiss();
                bottomDismiss();
                backgroundAlpha(1.0f);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                show = true;
                pop();
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int i = seekBar.getId();
       if (i == R.id.seekBar4) {
            int s = seekBar4.getProgress();
            readPageNum ++;
            markEdit4.setText(s + "%");
            begin = (pagefactory.getM_mbBufLen() * s) / 100;
            iv_menu2.setVisibility(View.GONE);
            iv_menu1.setVisibility(View.VISIBLE);
            editor.putInt(bookPath + "begin", begin).commit();
            pagefactory.setM_mbBufBegin(begin);
            pagefactory.setM_mbBufEnd(begin);
            try {
                pagefactory.prePage();
                pagefactory.nextPage();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (s == 100) {
                    pagefactory.prePage();
                    begin = pagefactory.getM_mbBufEnd();
                    pagefactory.setM_mbBufBegin(begin);;
                }else if(s == 0){
                    pagefactory.openbook(bookPath, 0);
                }
            } catch (IOException e) {
                Log.e(TAG, "onProgressChanged seekBar4-> IOException error", e);
            }
            postInvalidateUI();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(seekBar.getId() == R.id.seekBar4){
            int s  = seekBar4.getProgress();
            markEdit4.setText(s + "%");
            begin = (pagefactory.getM_mbBufLen() * s) / 100;
            editor.putInt(bookPath + "begin", begin).commit();
            pagefactory.setM_mbBufBegin(begin);
            pagefactory.setM_mbBufEnd(begin);
            try {
                if (s == 100) {
                    pagefactory.prePage();
                    pagefactory.getM_mbBufBegin();
                    begin = pagefactory.getM_mbBufEnd();
                    pagefactory.setM_mbBufBegin(begin);
                    pagefactory.setM_mbBufBegin(begin);
                }
            } catch (IOException e) {
                Log.e(TAG, "onProgressChanged seekBar4-> IOException error", e);
            }

            try {
                pagefactory.prePage();
            } catch (IOException e1) {
                Log.e(TAG, "onTouch->prePage error", e1);
            }
            pagefactory.onDraw(mNextPageCanvas);
            try {
                pagefactory.nextPage();
            } catch (IOException e1) {
                Log.e(TAG, "onTouch->nextPage error", e1);
            }
            pagefactory.onDraw(mNextPageCanvas);
            mPageWidget.setBitmaps(mCurPageBitmap,
                    mNextPageBitmap);

            postInvalidateUI();
            float fPercent = (float) (begin * 1.0 / pagefactory
                    .getM_mbBufLen());
            DecimalFormat df = new DecimalFormat("#0");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pagefactory = null;
        mPageWidget = null;
        finish();
    }

    public int getReadPageNum() {
        return readPageNum;
    }

    public void setReadPageNum(int readPageNum) {
        this.readPageNum = readPageNum;
    }
}