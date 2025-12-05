package com.example.myapp.ui.dashboard;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.List;

public class MusicAdapter extends BaseAdapter {
    private Context context;
    private List<Music> musicList;

    public MusicAdapter(Context context, List<Music> musicList) {
        this.context = context;
        this.musicList = musicList;
    }

    @Override
    public int getCount() { return musicList.size(); }

    @Override
    public Object getItem(int position) { return musicList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        TextView tv = convertView.findViewById(android.R.id.text1);
        tv.setText(musicList.get(position).getTitle());
        return convertView;
    }
}
