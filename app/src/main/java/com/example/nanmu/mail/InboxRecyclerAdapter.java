package com.example.nanmu.mail;

/**

 */
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;

public class InboxRecyclerAdapter extends RecyclerView.Adapter<InboxRecyclerAdapter.MyViewHolder> {

    private List<ParseEml> mDatas;
    private Context mContext;
    private LayoutInflater inflater;
    private OnMailItemClickListener listener;

    public InboxRecyclerAdapter(Context context, List<ParseEml> datas){
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
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        holder.tv_subject.setText(mDatas.get(position).getMailSubject());
        if (mDatas.get(position).getPersonalName() != null && !mDatas.get(position).getPersonalName().equals(""))
        {
            holder.tv_item_text.setText(mDatas.get(position).getPersonalName().substring(mDatas.get(position).getPersonalName().length()-1));
        }
        holder.tv_from_name.setText(mDatas.get(position).getPersonalName());
        holder.tv_content.setText(mDatas.get(position).getPlainContent());
        holder.iv.setImageResource(R.drawable.mail_item_part1);
        holder.iv.setColorFilter(Color.parseColor("#4DB7EB"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        holder.tv_re_item_date.setText(sdf.format(mDatas.get(position).getDate()));

        holder.ll_mail_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(position);
            }
        });

    }

    //重写onCreateViewHolder方法，返回一个自定义的ViewHolder
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.mail_item,parent, false);
        MyViewHolder holder= new MyViewHolder(view);
        return holder;
    }


    class MyViewHolder extends ViewHolder{

        TextView tv_from_name;
        TextView tv_subject;
        TextView tv_content;
        TextView tv_item_text;
        TextView tv_re_item_date;
        ImageView iv;
        LinearLayout ll_mail_item;

        public MyViewHolder(View view) {
            super(view);
            tv_from_name=(TextView) view.findViewById(R.id.tv_mail_from_name);
            tv_content= (TextView) view.findViewById(R.id.tv_mail_content);
            iv= (ImageView) view.findViewById(R.id.iv_item_image);
            tv_item_text= (TextView) view.findViewById(R.id.tv_item_text);
            tv_subject= (TextView) view.findViewById(R.id.tv_mail_subject);
            tv_re_item_date = (TextView) view.findViewById(R.id.tv_re_item_date);
            ll_mail_item= (LinearLayout) view.findViewById(R.id.ll_mail_item);
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
