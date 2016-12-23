package com.example.nanmu.mail;

/**
 * Created by nanmu on 2016/12/11.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**

 */
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class FileRecyclerAdapter extends RecyclerView.Adapter<FileRecyclerAdapter.MyViewHolder> {

    public void setmDatas(List<String> mDatas) {
        this.mDatas = mDatas;
    }

    private List<String> mDatas;
    private Context mContext;
    private LayoutInflater inflater;
    private OnMailItemClickListener listener;

    public FileRecyclerAdapter(Context context, List<String> datas){
        this. mContext=context;
        this. mDatas=datas;
        inflater=LayoutInflater. from(mContext);
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    //填充onCreateViewHolder方法返回的holder中的控件
    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {

        holder.tv_file_name.setText(mDatas.get(position));
        holder.ll_file_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(position);
            }
        });

    }

    //重写onCreateViewHolder方法，返回一个自定义的ViewHolder
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.file_item,parent, false);
        MyViewHolder holder= new MyViewHolder(view);
        return holder;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tv_file_name;
        LinearLayout ll_file_item;

        public MyViewHolder(View view) {
            super(view);
            tv_file_name=(TextView) view.findViewById(R.id.tv_file_name);
            ll_file_item= (LinearLayout) view.findViewById(R.id.ll_file_item);
        }

    }

    public void setOnMailItemClickListener(OnMailItemClickListener listener)
    {
        this.listener=listener;
    }

    public interface OnMailItemClickListener {
        void onItemClick(int position);
    }

}
