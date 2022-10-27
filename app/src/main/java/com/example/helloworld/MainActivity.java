package com.example.helloworld;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private MaterialToolbar mToolBar;
    private ExtendedFloatingActionButton addDiaryButton;
    private RecyclerView recyclerView;
    private LottieAnimationView lottieAnimationView;
    private TextView noDiaryTextView;

    @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mAuth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = mAuth.getCurrentUser();
    mToolBar = findViewById(R.id.main_activity_materialToolbar);
    mToolBar.getMenu().getItem(0).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if(currentUser != null){
                mAuth.signOut();
                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            return true;
        }
    });

    // Start navigating to activity_add_diary activity
    addDiaryButton = findViewById(R.id.add_diary_button);
    addDiaryButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(currentUser != null){
                Intent intent = new Intent(MainActivity.this, activity_add_diary.class);
                startActivity(intent);
            }
        }
    });

    // Session 5
        recyclerView=findViewById(R.id.recycler_view);
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false/* Avoid displaying the view from end to start*/);
        // Each recycler view MUST use a layout manager to function
        // Recycler view allows us to customize the layout arrangements for the child views
        recyclerView.setLayoutManager(horizontalLayoutManager);
        lottieAnimationView = findViewById(R.id.lottie_animation);
        noDiaryTextView = findViewById(R.id.no_diary);
        if(currentUser != null){
            getDaries(currentUser.getUid());
        }
    }

    private void getDaries(String uid) {
        ArrayList<Diary> diariesList = new ArrayList<>();
        DiaryAdapter diaryAdapter = new DiaryAdapter(MainActivity.this, diariesList);
        recyclerView.setAdapter(diaryAdapter);
        DatabaseReference databaseReference  = FirebaseDatabase.getInstance().getReference(); // Refers to the root node of the Realtime database
        databaseReference.child(uid).addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // snapshot will store all the "diaries" of the user with uid "uid" once there is any changes (e.g., update, removal, deletion) in that user
                diariesList.clear(); // To remove the "old list of diaries" before adding in the whole new diaries list
                diaryAdapter.notifyDataSetChanged();
                if(snapshot.exists()){
                    for (DataSnapshot snap:snapshot.getChildren()) {
                        Diary diary = snap.getValue(Diary.class);
                        diariesList.add(diary);
                    }
                    diaryAdapter.notifyDataSetChanged();
                }else{
                    lottieAnimationView.setVisibility(View.VISIBLE);
                    noDiaryTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}