package com.example.helloworld;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.helloworld.ml.LiteModelOnDeviceVisionClassifierLandmarksClassifierAsiaV11;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class activity_add_diary extends AppCompatActivity {
    private MaterialToolbar mToolBar;
    private TextInputEditText mTitle, mLocation, mWrite;
    private ImageView image;
    private MaterialButton mSaveButton;
    private String[]storagePermissions;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private int count = 0;

    //firebase variables #session4
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    //Views variables
    private CircularProgressIndicator progressIndicator; //#session4
    //other variables
    private Uri selectedImage;
    private Intent data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_diary); // Connect the java file with its XML file(activity_add_diary.xml) so that we cans ee the UI
        mToolBar = findViewById(R.id.add_diary_toolbar);
        mTitle = findViewById(R.id.diary_title);
        mLocation = findViewById(R.id.add_location);
        mWrite = findViewById(R.id.second_text_input);
        image = findViewById(R.id.image);
        mSaveButton = findViewById(R.id.save_button);
        mTitle.requestFocus(); //Works only if isFocusable() returns true
        mToolBar.getMenu().getItem(0).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                displayBuilder();
                return false;
            }
        });
        // Session 4
        firebaseAuth = FirebaseAuth.getInstance(); // "Authentication" object in firebase stores UID for each user
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();// Return the "current user" instance using the application
        firebaseDatabase = FirebaseDatabase.getInstance("https://helloworld-e832a-default-rtdb.firebaseio.com/");// Return an instance of firebase database
        databaseReference = firebaseDatabase.getReference(); // Refers to helloworld-e832a-default-rtdb in our firebase real-time database
        //add premission in manifest file as well
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //find views of AddDiary xml file
        progressIndicator = findViewById(R.id.add_diary_progress_bar);
        mToolBar = findViewById(R.id.add_diary_toolbar);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                finish();
            }
        });
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //first need to get all user input in edit texts and if there is an image
                String title = mTitle.getText().toString();
                String note = mWrite.getText().toString();
                String loc = mLocation.getText().toString();
                // before saving to database we need to check if any compulsory field are empty or not
                // title and note are compulsory not to be empty for both of cases if user wants to save diary with image or without image.
                if (!title.isEmpty() && !note.isEmpty()) {
                    // now we check if the user wants to add image diary or textDiary
                    if (isTextDiary(loc)) {
                        // another case to be checked before saving data to database is to check that firebase user is not null
                        if (firebaseUser != null) {
                            // we need to disable the button from another click in case if user clicks the button 2 times so we dont save the same data again
                            mSaveButton.setClickable(false);
                            //enable progress bar
                            progressIndicator.setVisibility(View.VISIBLE);
                            progressIndicator.setProgressCompat(500, true);
                            // we also pass userId to our method to save the diary which belongs to our firebaseUser
                            saveTextDiaryToDatabase(title, note, firebaseUser.getUid());
                        }
                    } else if (isImageDiary(loc)) {
                        if (firebaseUser != null) {
                            // we need to disable the button from another click in case if user clicks the button 2 times so we dont save the same data again
                            mSaveButton.setClickable(false);
                            progressIndicator.setVisibility(View.VISIBLE);
                            progressIndicator.setProgressCompat(300, true);
                            saveImageDiaryToStorage(title, note, loc, selectedImage, firebaseUser.getUid());
                        }
                    } else {
                        Toast.makeText(activity_add_diary.this, "Please fill up all fields", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(activity_add_diary.this, "Please enter both title and note.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private boolean isTextDiary(String loc){
        // if (title and note are not empty) and no image is selected plus the loc is empty means user wants to save only a textDiary
        return selectedImage==null && loc.length()==0;
    }

    private boolean isImageDiary(String loc){
        // if user is saving a textDiary or imageDiary note and title are compulsory to not be empty
        return !loc.isEmpty() && selectedImage!=null;
    }

    private void saveTextDiaryToDatabase(String title,String note,String userId){
        //based on structure of firebase database we will make a hashmap to save our data
        // hashmap consist of keys and values for example key:title has value as the title string that the user entered.
        //the hashmap key type should be String and value can be any object type such as :boolean,integer,string and ect.
        HashMap<String,Object> diaryHashmap=new HashMap<>(); // create instance of hashmap
        diaryHashmap.put("title",title);
        diaryHashmap.put("note",note);
        diaryHashmap.put("type","text");
        //now that we have our hashmap ready we insert our hashmap in database
        // we save the data using the database reference we initialized in onCreate() method
        //each node in the database reference is consider as a child
        // to differentiate the users from each other we need to save their dairies with their specific identical id.
        //the unique identical is wrapped in firebaseUser so we use it as to make node child
        DatabaseReference diaryNode = databaseReference.child(userId).push();
        // the diaryNode is a path or database reference that we want to save the diary in that contains a unique id as well to differentiate each diary from each other.
        String diaryNodeId=diaryNode.getKey(); //getKey() returns the id of that Node we created in line 166
        diaryHashmap.put("diaryId",diaryNodeId); //in case if later when we want to read data from firebase we will need diary Id
        //now finally we save the hashmap that holds our data in to the database to the diaryNode we created
        progressIndicator.setVisibility(View.VISIBLE); // show progress bar
        progressIndicator.setProgressCompat(100,true);
        diaryNode.updateChildren(diaryHashmap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<Void> task) {
                //we check if task is successfull
                //task is the operation we requested to firebase real-time database to do.
                if(task.isSuccessful()){
                    // we show a message to user the diary added successfully and we redirect the user back to MainActivity.class
                    progressIndicator.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(),"Diary added Successfully",Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP); //We shouldn't let the user to press on back button
                    startActivity(intent);
                    finish(); //we finish the AddDiary activity lifecycle
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @NotNull Exception e) {
                //in case that any failure happens we show an error message to user.
                // error message is wrapped in e variable in onFailuer method and we use that message.
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                //need to make the saveDiary button back to clickable state
                mSaveButton.setClickable(true);
            }
        });
    }

    private void saveImageDiaryToStorage(String title, String note, String loc, Uri image, String userId){
        // for image diary we need to save the image into firebase storage first and retrive the link [url] to that storage file
        //only then save the image URL along with title,note and loc to the firebase real-time database
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference=firebaseStorage.getReference(); //similar to databaseReference
        //now we make a file named as userId in our bucket of firebase storage
        //first child is user folder named as userId and second child is the name of selected image by user
        // lastly we upload the selected image into storage by using putFile()
        storageReference.child(userId).child(image.getLastPathSegment()).putFile(image).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<UploadTask.TaskSnapshot> task) {
                //check first if the operation we requested to firebase storage is successfull
                if(task.isSuccessful()){
                    // after upload to storage we need to retrieve back the url path that the image is stored
                    //another listener is required to access the file meta data reference in order to retrieve the image URL
                    task.getResult().getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull @NotNull Task<Uri> task) {
                            if(task.isSuccessful()){
                                String imageURL=task.getResult().toString();
                                //now we need to save all data in firebase real-time database
                                saveImageDiaryToDatabase(title,note,loc,imageURL,userId);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {
                            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                            mSaveButton.setClickable(true);
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @NotNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                mSaveButton.setClickable(true);
            }
        });
count++;
    }
    private void saveImageDiaryToDatabase(String title,String note,String loc,String imageURL,String userId){
        //now the process is the same as we did for textDiary
        HashMap<String,Object> diaryHashmap=new HashMap<>();
        diaryHashmap.put("title",title);
        diaryHashmap.put("note",note);
        diaryHashmap.put("type","image");
        diaryHashmap.put("image",imageURL);
        diaryHashmap.put("placeName",loc);
        DatabaseReference diaryNode = databaseReference.child(userId).push(); //diaryNode
        String diaryNodeId=diaryNode.getKey(); //diary unique Id
        diaryHashmap.put("diaryId",diaryNodeId);
        diaryNode.updateChildren(diaryHashmap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<Void> task) {
                // check the task operation is successfull
                if(task.isSuccessful()){
                    // we show a message to user the diary added successfully and we redirect the user back to MainActivity.class
                    progressIndicator.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(),"Diary added Successfully",Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP); //We shouldn't let the user to press on back button
                    startActivity(intent);
                    finish(); //we finish the AddDiary activity lifecycle
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @NotNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                //need to make the saveDiary button back to clickable state
                mSaveButton.setClickable(true);
            }
        });

    }

    private void displayBuilder() {
        //this builder is an alert card it shows options to user to choose from where they want the image from
        String[] options = {"Gallery"};
        //here we only use gallery option if you would like to add camera option add another element to the options array
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //need to check which item from options is selected here we only have one option
                if (which == 0) {
                    //first we check if user granted the permission to acces the phone storage
                    if (!checkStoragePermission()) {
                        //if is not granted we will request permission
                        requestStoragePermission();
                    } else {
                        //when permission is granted we open the gallery
                        pickFromGallery();
                    }
                }
            }
        });
        builder.create().show();
    }

    private void pickFromGallery() {
        //send intent to open gallery with a request code
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    //when user allows or denies the permission this method will be triggered to either grant or reject access
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    //if the access granted then we open gallery
                    if (storageAccepted) {
                        pickFromGallery();
                    } else {
                        //if not granted we send request again for access and
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);

                    }
                }
            }
            break;
        }
    }

    //when user selects image from gallery this method will be triggered
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //need to check if the result code is ok and request code we sent is the same as we defined earlier
        if ( resultCode== RESULT_OK && requestCode == IMAGE_PICK_GALLERY_CODE) {
            if (data != null) {
                //the selected image is wrapped in data variable
                selectedImage = data.getData();
                predictLandMark(selectedImage);

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void predictLandMark(Uri img){
        try {
            //convert selected image uri to bitmap
            InputStream imageStream=getApplication().getContentResolver().openInputStream(selectedImage); //Always access Uri through Content Provider using Content Resolver
            Bitmap bitmap =  BitmapFactory.decodeStream(imageStream); // Create bitmap object from a stream
            // need to preprocess the image before feeding to model based on input constraint
            ImageProcessor imageProcessor=new ImageProcessor
                    .Builder() // Create an instance of ImageProcessor
                    .add(new ResizeOp(321,321, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)) //size 321x321
                    .add(new CastOp(DataType.UINT8)) //the type of image input should be UINT8 which currently our bitmap data type is float32
                    .build(); // Completes building process and returns an ImageProcessor
            //load the model
            LiteModelOnDeviceVisionClassifierLandmarksClassifierAsiaV11 model = LiteModelOnDeviceVisionClassifierLandmarksClassifierAsiaV11.newInstance(this);
            //initialize tensor image object with the required input data type which is UINT8
            TensorImage tensorImage=new TensorImage(DataType.UINT8);
            // Initialize Tensor buffer object in order to feed it to the model (Use to store the bytebuffers that holds image daa)
            // Based on input contraint the required shape is [1,321,321,3]
            TensorBuffer tensorBuffer=TensorBuffer.createFixedSize(new int[]{1,321,321,3},DataType.UINT8); // "1" image has "321x321" size & each pixel has "3" values (RGB)
            //now we load our bitmap into tensor image we created earlier
            tensorImage.load(bitmap);
            //then need to preprocess the tensor image from bitmap image we load above
            TensorImage processedTensorImage=imageProcessor.process(tensorImage);
            // now need to load the processed tensor image buffer into the tensor buffer object we created in line 400
            tensorBuffer.loadBuffer(processedTensorImage.getBuffer());
            //now we feed the tensorbuffer into the model to make prediction
            LiteModelOnDeviceVisionClassifierLandmarksClassifierAsiaV11.Outputs outputs = model.process(tensorBuffer);
            List<Category> probability=outputs.getProbabilityAsCategoryList();
//          Releases model resources if no longer used.
            model.close(); // After finishing interpretation
            // next step is to find the label that has highest probability
            float max=0.0f;
            int bestPredictIndex=0;
            for (int i=0;i<probability.size();i++){
                if(probability.get(i).getScore()>max){
                    max=probability.get(i).getScore();
                    bestPredictIndex=i;
                }
            }
        //we use Glide to upload the image in our app
        Glide.with(this)
                .load(selectedImage) //the uri of the image
                .transform(new FitCenter())//to fit properly in our image view size
                .transition(DrawableTransitionOptions.withCrossFade()) //with a nice transition (animation) for user experience
                .into(image); //the image view that needs to be place in
        image.setVisibility(View.VISIBLE);
        mLocation.setVisibility(View.VISIBLE);
            Log.d("PROB","highest probability:"+probability.get(bestPredictIndex).getScore()); // check in debugger the highest probability
            mLocation.setText(probability.get(bestPredictIndex).getLabel());
            mLocation.requestFocus(mLocation.getTextDirection()); // enable the edit text focus in case if prediction is wrong user must know it is editable by user.
        } catch (IOException e) {
            // if the prediction fails due to any reason
            mLocation.setText("Could not predict the name of your selected image");
        }
    }
}
