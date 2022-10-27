package com.example.helloworld;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    private final Context context;
    private final ArrayList<Diary> diariesList;

    public DiaryAdapter(Context context, ArrayList<Diary> diariesList) {
        this.context = context;
        this.diariesList = diariesList;
    }

    @NonNull
    @Override
    // To set the View visible & create viewHolder to hold the View
    public DiaryAdapter.DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);// To create an instance of the LayoutInflater from specific context
        View view = layoutInflater.inflate(R.layout.diary_layout /*Name of the XML file*/, parent, false);
        return new DiaryViewHolder(view); // For diary_laout.xml, view = CardView
    }

    @Override
    // To modify the current contents hold by the current viewHolder
    public void onBindViewHolder(@NonNull DiaryAdapter.DiaryViewHolder holder, int position/*Different position of the objects to be displayed*/) {
        Diary diary = diariesList.get(position);
        holder.diaryTitle.setText(diary.getTitle());
        holder.diaryNote.setText(diary.getNote());
        // Check the diary type to set contents for image & text
        String diaryType = diary.getType();
        if (diaryType.equals("text")) {
            holder.diaryLoc.setVisibility(View.GONE); // diaryLoc is invisible & does not take up any spaces in the layout
            // Set the default image if there is no image uploaded
            Glide.with(context).load(R.drawable.login_back_ground_image).transform(new RoundedCorners(15),
                    new CenterCrop()).transition(DrawableTransitionOptions.withCrossFade()).into(holder.diaryImage);
        } else if (diaryType.equals("image")) {
            holder.diaryLoc.setText(diary.getPlaceName());
            String imageURL = diary.getImage();
            // Make sure the URL is not empty to avoid crash
            if (!imageURL.isEmpty()) {
                Glide.with(context)
                        .load(imageURL)
                        .transform(new RoundedCorners(15), new CenterCrop()) /* How the image looks like / appearance of image*/
                        .transition(DrawableTransitionOptions.withCrossFade()) /* How the image appears / disappear on the placeholder*/
                        .into(holder.diaryImage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return diariesList.size();
    }

    // Each content in the object is hold by respective viewHolder
    public class DiaryViewHolder extends RecyclerView.ViewHolder {

        private final TextView diaryTitle;
        private final TextView diaryNote;
        private final TextView diaryLoc;
        private final ImageView diaryImage;


        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            diaryTitle = itemView.findViewById(R.id.adapter_diary_title);
            diaryNote = itemView.findViewById(R.id.adapter_diary_note);
            diaryLoc = itemView.findViewById(R.id.adapter_diary_location);
            diaryImage = itemView.findViewById(R.id.adapter_diary_image);
        }
    }
}