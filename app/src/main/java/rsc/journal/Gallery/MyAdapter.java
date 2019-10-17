package rsc.journal.Gallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import rsc.journal.GalleryActivity;
import rsc.journal.ImageActivity;
import rsc.journal.R;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private ArrayList<CreateList> galleryList;
    private Context context;

    public MyAdapter(Context context, ArrayList<CreateList> galleryList) {
        this.galleryList = galleryList;
        this.context = context;
    }

    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.gallery_cell_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyAdapter.ViewHolder viewHolder, int i) {

        viewHolder.title.setText(galleryList.get(i).getImage_title());
        viewHolder.img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        viewHolder.img.setTag(R.id.glide_tag, galleryList.get(i).getImage_Location()); //Specific tag resource for avoid Glide error


        Glide
                .with(context)
                .load(galleryList.get(i).getImage_Location())
                .into(viewHolder.img);

        viewHolder.img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = v.getTag(R.id.glide_tag).toString();
                if (path.contains(".jpg")) {
                    Intent intent = new Intent(context, ImageActivity.class);
                    intent.putExtra("path", path);
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(path), "video/*");
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return galleryList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private TextView title;
        private ImageView img;

        ViewHolder(View view) {
            super(view);

            title = view.findViewById(R.id.title);
            img = view.findViewById(R.id.img);

            img.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            //Put in position to future file removing in gallery activity
            menu.add(getAdapterPosition(), 1, 0, "Удалить");
        }
    }
}