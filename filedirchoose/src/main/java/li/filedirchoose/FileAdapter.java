package li.filedirchoose;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建时间: 2017/11/28
 * 创建人: Administrator
 * 功能描述:文件路径选择适配器
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private List<String> data;
    private Context context;
    private int layoutId;
    private Event event;
    private ArrayList<String> selectData;

    public FileAdapter(List<String> data, Context context, int layoutId) {
        this.data = new ArrayList<>();
        this.selectData = new ArrayList<>();
        this.data.clear();
        this.data.addAll(data);
        this.context = context;
        this.layoutId = layoutId;
    }

    public void setData(List<String> data) {
        this.data.clear();
        this.data.addAll(data);
        Log.d("ZWS", "data size " + data.size());
        notifyDataSetChanged();
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void addData(String content) {
        this.data.add(content);
        notifyDataSetChanged();
    }

    public String getDataByPostion(int postion) {
        if (data.size() <= postion)
            return "";
        return data.get(postion);
    }

    public List<String> getAllData() {
        return data;
    }

    public void clearAllData() {
        this.data.clear();
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final String path = data.get(position);
        boolean isChecked = false;
        for (String s : selectData
                ) {
            if (s.trim().equals(path)) {
                isChecked = true;
                break;
            }
        }
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (selectData.contains(path))
                        return;
                    else
                        selectData.add(path);
                } else {
                    if (selectData.contains(path))
                        selectData.remove(path);
                    else
                        return;
                }
                Log.d("ZWW", "selectData size " + selectData.size());
            }
        });
        holder.checkBox.setChecked(isChecked);
        holder.tvFileDir.setText(path.substring(path.lastIndexOf("/") + 1));
        holder.rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!holder.checkBox.isChecked()) {
                    event.enterNextDir(data.get(position));
                } else {
                    Toast.makeText(context, context.getString(R.string.filechoose_already), Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    public ArrayList<String> getSelectData() {
        return selectData;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFileDir;
        private ConstraintLayout rootLayout;
        private CheckBox checkBox;

        public ViewHolder(View view) {
            super(view);
            tvFileDir = view.findViewById(R.id.tvFileDir);
            rootLayout = view.findViewById(R.id.rootLayout);
            checkBox = view.findViewById(R.id.checkBox);


        }
    }

    public interface Event {
        void enterNextDir(String path);
    }
}
