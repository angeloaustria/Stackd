package com.stackd.stackd.activities;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.stackd.stackd.R;
import com.stackd.stackd.db.DataManager;
import com.stackd.stackd.db.entities.Resume;
import com.stackd.stackd.db.entities.Tag;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditActivity extends AppCompatActivity {
    public static final String IMAGE_URI_KEY = "imageUri";
    public static final String IMAGE_R_KEY = "rKey";
    private LinearLayout tagListLayout;
    private Map<CheckBox, Tag> checkBoxes = new HashMap<>();
    private Resume resume;
    // Dummy Values
    private final long cId = 1;
    private final long rId = 21;
    private DataManager dataManager = DataManager.getDataManager(cId, rId, getBaseContext());
    private AlertDialog alertBox = null;
    private AlertDialog setCandidateEmailAlertBox = null;
    private int currentId = 100;
    private List<Tag> resumeTags;

    private ImageView resumeView;
    private ImageView resumeViewShadow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        setUpAlertBox(this);
        setUpCandidateEmail(this);

        // Fields needed from other screens
        Bundle extras = getIntent().getExtras();
        String strUri = extras.getString(IMAGE_URI_KEY);
        Uri uri;
        // set picture to the picture of the current resume
        resumeView = (ImageView)findViewById(R.id.current_resume);
        resumeViewShadow = (ImageView)findViewById(R.id.current_resume_shadow);
        if(strUri != null) {
            uri = Uri.parse(extras.getString(IMAGE_URI_KEY));
            resumeView.setImageURI(uri);
            resumeViewShadow.setImageURI(uri);
        }
        else {
            uri = Uri.parse("");
            resumeView.setImageResource(extras.getInt(IMAGE_R_KEY));
            resumeViewShadow.setImageResource(extras.getInt(IMAGE_R_KEY));
        }
        long resumeId = extras.getLong(StackActivity.RESUME_ID_KEY);
        if(resumeId == StackActivity.RESUME_ID_NEW) {
            // increment currentId, so that it is unique for every resume
            currentId++;

            // Initialize the new resume
            resume = new Resume.Builder()
                    .id(currentId)
                    .rid(dataManager.getRecruiter().getRecId())
                    .url(uri.toString())
                    .collectionDate(new SimpleDateFormat("DD-MM-yyyy").format(new Date()))
                    .candidateName("Candidate 1").build();
            resumeTags = new ArrayList<>();
        }
        else {
            // make a resume immutable, since it has already been reviewed
            findViewById(R.id.submit_resume).setClickable(false);
            EditText editText = ((EditText)findViewById(R.id.comment_field));
            editText.setEnabled(false);
            // load an existing resume
            for(Resume r : dataManager.getResumes())
                if(r.getId() == resumeId) {
                    resume = r;
                    editText.setText(resume.getRecruiterComments());
                    if(resume.getTagList() != null)
                        resumeTags = resume.getTagList();
                    else
                        resumeTags = new ArrayList<>();
                    break;
                }
        }


        // Add checkboxes dynamically
        tagListLayout = (LinearLayout) findViewById(R.id.tagListLayout);

        // The company's tags
        final List<Tag> tagList = dataManager.getCompanyTags();
        for (final Tag tag: tagList) {
            final CheckBox cb = new CheckBox(this);
            cb.setId(tagList.indexOf(tag));
            cb.setText(tag.getName());
            cb.setTextColor(Color.WHITE);
            cb.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    resumeTags.add(tag);
                }
            });
            if(resumeId != -1)
                cb.setClickable(false);
            //checkBoxes.put(cb, tag);
            tagListLayout.addView(cb);
        }
        // mark checkboxes as selected, if a resume was already edited
        /*if(resumeId != -1) {
            for(CheckBox cb : checkBoxes)
                if(resumeTags.contains(cb.getText().toString()))
        }*/
    }

    /*
     * Find the URI from the given resID
     */
    public static Uri resIdToUri(Context context, int resId) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resId) + '/' +
                context.getResources().getResourceTypeName(resId) + '/' +
                context.getResources().getResourceEntryName(resId) );

    }

    public void onHighlightBtnClick(View v) {
        if (resumeView.getVisibility() == ImageView.GONE) {
            resumeView.setVisibility(ImageView.VISIBLE);
        } else
            resumeView.setVisibility(ImageView.GONE);
    }

    public void onDoneBtnClick(View v) {
        // add comments and selected tags to the resume
        final EditText commentField = (EditText) findViewById(R.id.comment_field);
        String comments = commentField.getText().toString();
        resume.setRecruiterComments(commentField.getText().toString());
        resume.setTagList(resumeTags);
        alertBox.show();
    }

    public void setUpCandidateEmail(Context context) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.set_candidate_email_dialog, null);
        alertBuilder.setView(dialogView);

        final EditText name = (EditText) dialogView.findViewById(R.id.set_candidate_name);
        final EditText email = (EditText) dialogView.findViewById(R.id.set_candidate_email);
        alertBuilder.setTitle("Candidate Name");
        alertBuilder.setMessage("Enter name and email:");



        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (name != null)
                    resume.setCandidateName(name.getText().toString());
            }
        });
        setCandidateEmailAlertBox = alertBuilder.create();
    }

    public void setUpAlertBox(Context context) {

        // The button icons
        //ImageView no = new ImageView(this);
        //no.setImageResource(R.drawable.splattered);

        // setup the alert builder
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Candidate Status");
        //alertBuilder.setView(no);
        // add a list
        String[] ratings = {"Yes", "No", "Maybe"};

        alertBuilder.setItems(ratings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: resume.setRating(2);// yes
                    case 1: resume.setRating(0);// no
                    case 2: resume.setRating(1);// maybe

                }
                // Insert the resume into the database
                assert(resume != null);
                dataManager.insertResume(resume);
                // Review it and add a rating
                dataManager.addReview(resume.getId(), resume.getCollectionDate(), resume.getRating());

                // Go back to stack view
                Intent i = new Intent(EditActivity.this, StackActivity.class);
                startActivity(i);
            }
        });
        alertBox = alertBuilder.create();
    }

}

