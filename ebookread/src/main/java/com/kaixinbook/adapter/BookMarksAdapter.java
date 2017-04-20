package com.kaixinbook.adapter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kaixinbook.R;
import com.kaixinbook.helper.MarkHelper;
import com.kaixinbook.bean.MarkVo;
import com.zhy.autolayout.utils.AutoUtils;

import java.util.ArrayList;

/**
 * Created by adks on 2016/12/21.
 */

public class BookMarksAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<MarkVo> list = null;
    private MarkHelper markhelper;

    private BookPageDeleteListener bookPageDeleteListener;

    public interface BookPageDeleteListener{
        public void deleteBookPage(String pageFirstStr);
    }

    public void setOnBookPageDeleteListener(BookPageDeleteListener bookPageDeleteListener){
        this.bookPageDeleteListener = bookPageDeleteListener;
    }



    public BookMarksAdapter(Context mContext, ArrayList<MarkVo> list) {
        this.mContext = mContext;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_book_mark,parent,false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
            AutoUtils.autoSize(convertView);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tv_word.setText(list.get(position).getText());
        holder.tv_date.setText(list.get(position).getTime());
        holder.tv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markhelper = new MarkHelper(mContext);
                String s = list.get(position).getBookPath();
                String s1 = list.get(position).getTime();
                SQLiteDatabase db2 = markhelper.getWritableDatabase();
                db2.delete("markhelper", "path='" + s + "' and time ='" + s1
                        + "'", null);
                db2.close();
                list.remove(position);

                bookPageDeleteListener.deleteBookPage(s);

                notifyDataSetChanged();
            }
        });
        return convertView;
    }

    private static class ViewHolder{
        private TextView tv_word;
        private TextView tv_date;
        private TextView tv_delete;
        public ViewHolder(View view){
            tv_word= (TextView) view.findViewById(R.id.tv_mark_word);
            tv_date = (TextView) view.findViewById(R.id.tv_mark_time);
            tv_delete = (TextView) view.findViewById(R.id.tv_delete);
        }
    }
}
