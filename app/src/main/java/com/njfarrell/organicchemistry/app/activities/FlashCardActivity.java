/**
 * Copyright Nate Farrell. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.activities;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.njfarrell.organicchemistry.app.R;
import com.njfarrell.organicchemistry.app.animation.FlipAnimation;
import com.njfarrell.organicchemistry.app.database.tables.FlashCardsTable;
import com.njfarrell.organicchemistry.app.preferences.AppPreferences;
import com.njfarrell.organicchemistry.app.services.ContentDownloadService;
import com.njfarrell.organicchemistry.app.utilities.AppUtil;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/**
 * @author Nate Farrell <njfarrel@gmail.com>
 *
 * Creates and displays flash cards that exist within a flash card deck.
 */
public class FlashCardActivity extends ActionBarActivity implements View.OnClickListener,
        Animation.AnimationListener {

    private FlashCardResponseReceiver mFlashCardResponseReceiver;
    private ArrayList<FlashCard> mFrontCardAL;
    private ArrayList<FlashCard> mReverseCardAL;
    private int mCardPosition = 0;
    private boolean mDefaultReverse;
    private boolean mCardReverse = false;
    private int mAnimationType = -1;

    private String api_do;
    private JSONObject api_with;
    private int deckId;

    private RelativeLayout mBackgroundView;
    private LinearLayout mFlashCard;
    private RelativeLayout mLeftEdge;
    private RelativeLayout mRightEdge;
    private TextView mCardPositionView;
    private Animation mAnimationOutLeft;
    private Animation mAnimationInLeft;
    private Animation mAnimationOutRight;
    private Animation mAnimationInRight;
    private GestureDetector mGestureDetector;

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.next_button:
                if (mCardPosition < (mFrontCardAL.size() - 1)) {
                    mCardPosition++;
                    mCardReverse = mDefaultReverse;
                    mAnimationType = 1;
                    startFlashCardAnimation(mAnimationOutLeft);
                } else {
                    displayViewEdge(mRightEdge);
                }
                break;
            case R.id.previous_button:
                if (mCardPosition > 0) {
                    mCardPosition--;
                    mCardReverse = mDefaultReverse;
                    mAnimationType = 2;
                    startFlashCardAnimation(mAnimationOutRight);
                } else {
                    displayViewEdge(mLeftEdge);
                }
                break;
            case R.id.flip_button:
            case R.id.card_view:
                mCardReverse = !mCardReverse;
                mAnimationType = 0;
                startFlashCardAnimation(0, 90);
                break;
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // Ignore
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        displayCard();
        if (mAnimationType == 0) {
            startFlashCardAnimation(-90, 0);
        }
        else if (mAnimationType == 1) {
            displayCard();
            startFlashCardAnimation(mAnimationInRight);
        } else if (mAnimationType == 2) {
            displayCard();
            startFlashCardAnimation(mAnimationInLeft);
        }
        mAnimationType = -1;
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // Ignore
    }

    /**
     * Broadcast receiver for downloading the flash card deck content.
     */
    public class FlashCardResponseReceiver extends BroadcastReceiver {
        public static final String FLASH_CARD_RESPONSE = "com.njfarrell.intent.action.flashCard";

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey(ContentDownloadService.KEY_RESPONSE_JSON)) {
                try {
                    String responseString = extras.getString(ContentDownloadService.KEY_RESPONSE_JSON);
                    JSONObject response = new JSONObject(responseString);
                    if (response.has("success")) {
                        boolean success = (Boolean) response.get("success");
                        if (success) {
                            generateFlashCard(responseString);
                            storeFlashCardsToDatabase(responseString);
                        }
                    } else {
                        Toast.makeText(FlashCardActivity.this, "You need internet access in " +
                                "order to download the current flash card deck",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    // Dont handle exception
                }
            }
        }
    }

    /**
     * Gesture listener to handle swipe gestures.
     */
    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE &&
                    Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                //From Right to Left
                if (mCardPosition < (mFrontCardAL.size() - 1)) {
                    mCardPosition++;
                    mCardReverse = mDefaultReverse;
                    mAnimationType = 1;
                    startFlashCardAnimation(mAnimationOutLeft);
                } else {
                    displayViewEdge(mRightEdge);
                }
                return true;
            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE &&
                    Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                //From Left to Right
                if (mCardPosition > 0) {
                    mCardPosition--;
                    mCardReverse = mDefaultReverse;
                    mAnimationType = 2;
                    startFlashCardAnimation(mAnimationOutRight);
                } else {
                    displayViewEdge(mLeftEdge);
                }
                return true;
            }
            return false;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            //always return true since all gestures always begin with onDown and<br>
            //if this returns false, the framework won't try to pick up onFling for example.
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_flash_card);

        mBackgroundView = (RelativeLayout) findViewById(R.id.background_view);

        AppPreferences prefs = AppPreferences.newInstance(this);
        mDefaultReverse = prefs.getBooleanValue(AppPreferences.KEY_REVERSE_MODE, false);
        mCardReverse = mDefaultReverse;

        if (savedInstanceState != null) {
            mCardPosition = savedInstanceState.getInt("flash_card_position", 0);
            mCardReverse = savedInstanceState.getBoolean("flash_card_reverse", false);
        }

        mFlashCard = (LinearLayout) findViewById(R.id.card_view);
        mFlashCard.setOnClickListener(this);

        mGestureDetector = new GestureDetector(new GestureListener());
        mFlashCard.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {

                return mGestureDetector.onTouchEvent(event);
            }
        });

        Button next = (Button) findViewById(R.id.next_button);
        if (next != null) {
            next.setOnClickListener(this);
        }
        Button previous = (Button) findViewById(R.id.previous_button);
        if (previous != null) {
            previous.setOnClickListener(this);
        }
        Button flip = (Button) findViewById(R.id.flip_button);
        if (flip != null) {
            flip.setOnClickListener(this);
        }
        mLeftEdge = (RelativeLayout) findViewById(R.id.edge_left);
        mRightEdge = (RelativeLayout) findViewById(R.id.edge_right);

        mCardPositionView = (TextView) findViewById(R.id.card_position);

        mAnimationOutRight = AnimationUtils.loadAnimation(this, R.anim.out_to_right);
        if (mAnimationOutRight != null) {
            mAnimationOutRight.setAnimationListener(this);
        }
        mAnimationInRight = AnimationUtils.loadAnimation(this, R.anim.in_from_right);
        if (mAnimationInRight != null) {
            mAnimationInRight.setAnimationListener(this);
        }
        mAnimationOutLeft = AnimationUtils.loadAnimation(this, R.anim.out_to_left);
        if (mAnimationOutLeft != null) {
            mAnimationOutLeft.setAnimationListener(this);
        }
        mAnimationInLeft = AnimationUtils.loadAnimation(this, R.anim.in_from_left);
        if (mAnimationInLeft != null) {
            mAnimationInLeft.setAnimationListener(this);
        }

        mFlashCardResponseReceiver = new FlashCardResponseReceiver();

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            deckId = extras.getInt("deck_id");
            String deckTitle = extras.getString("deck_title");

            if (AppUtil.isValidString(deckTitle)) {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setTitle(deckTitle);
            }

            checkDatabaseForContent();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("flash_card_position", mCardPosition);
        outState.putBoolean("flash_card_reverse", mCardReverse);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(FlashCardResponseReceiver.FLASH_CARD_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mFlashCardResponseReceiver, filter);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mBackgroundView.setBackgroundResource(R.drawable.background_image_land);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                    mFlashCard.getLayoutParams();
            if (params != null) {
                params.weight = 0.85f;
                params.setMargins(100, 0, 100, 0);
                mFlashCard.setLayoutParams(params);
                LinearLayout flashCardButtons = (LinearLayout) findViewById(R.id.flashcard_button);
                flashCardButtons.setVisibility(View.GONE);
            }
        } else {
            mBackgroundView.setBackgroundResource(R.drawable.background_image);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                    mFlashCard.getLayoutParams();
            if (params != null) {
                params.weight = 0.6f;
                params.setMargins(0, 0, 0, 0);
                mFlashCard.setLayoutParams(params);
                LinearLayout flashCardButtons = (LinearLayout) findViewById(R.id.flashcard_button);
                flashCardButtons.setVisibility(View.VISIBLE);
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mFlashCardResponseReceiver != null) {
            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
            manager.unregisterReceiver(mFlashCardResponseReceiver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.flashcard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_shuffle) {
            if (mFrontCardAL != null && mFrontCardAL.size() > 0) {
                generateRandomCard();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Check database for available content.
     */
    private void checkDatabaseForContent() {
        FlashCardsTable flashCardsTable = new FlashCardsTable(this);
        String responseString = flashCardsTable.queryTableByDeckId(deckId);
        if (responseString != null) {
            generateFlashCard(responseString);
        } else {
            api_do = "get_cards";
            api_with = new JSONObject();
            try {
                api_with.put("deck", deckId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ContentDownloadService.startContentDownloadService(this, api_do, api_with);
        }
    }

    /**
     * Store flash card deck to data table.
     *
     * @param responseString flash card deck response from server.
     */
    private void storeFlashCardsToDatabase(String responseString) {
        ContentValues values = new ContentValues();
        FlashCardsTable flashCardsTable = new FlashCardsTable(
                FlashCardActivity.this);

        values.put(FlashCardsTable.COL_DECK_ID, deckId);
        values.put(FlashCardsTable.API_DO, api_do);
        values.put(FlashCardsTable.API_WITH, api_with.toString());
        values.put(FlashCardsTable.RESPONSE, responseString);
        values.put(FlashCardsTable.TIMESTAMP, System.currentTimeMillis());

        flashCardsTable.replaceRow(values);
    }

    /**
     * Displays a random flash card when shuffle is pressed.
     */
    private void generateRandomCard() {
        Random rand = new Random();
        int position = rand.nextInt(mFrontCardAL.size());
        mCardReverse = mDefaultReverse;
        if (position > mCardPosition) {
            mAnimationType = 1;
            mCardPosition = position;
            startFlashCardAnimation(mAnimationOutLeft);
        } else if (position < mCardPosition) {
            mAnimationType = 2;
            mCardPosition = position;
            startFlashCardAnimation(mAnimationOutRight);
        } else {
            generateRandomCard();
        }
    }

    /**
     * Generate the list of flash cards from the response from server.
     *
     * @param responseString response string from the server.
     */
    private void generateFlashCard(String responseString) {
        JSONArray flashCardArray;
        try {
            JSONObject response = new JSONObject(responseString);
            flashCardArray = response.getJSONArray("data");
            for (int i = 0; i < flashCardArray.length(); i++) {
                JSONObject flashCard = new JSONObject(flashCardArray.get(i)
                        .toString());
                FlashCard questionCard = new FlashCard(flashCard.optString("title"),
                        flashCard.optString("front_img"),
                        flashCard.optString("front_caption"));
                FlashCard answerCard = new FlashCard(flashCard.optString("title"),
                        flashCard.optString("back_img"),
                        flashCard.optString("back_caption"));
                // Load images as soon as possible, even before cards are displayed
                if (questionCard.image != null) {
                    Picasso.with(this).load(questionCard.image).into(new ImageView(this));
                }
                if (answerCard.image != null) {
                    Picasso.with(this).load(answerCard.image).into(new ImageView(this));
                }
                if (mFrontCardAL == null) {
                    mFrontCardAL = new ArrayList<FlashCard>();
                }
                mFrontCardAL.add(questionCard);
                if (mReverseCardAL == null) {
                    mReverseCardAL = new ArrayList<FlashCard>();
                }
                mReverseCardAL.add(answerCard);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        displayCard();
    }

    @Override
    public void onBackPressed() {
        mCardPosition = 0;
        mCardReverse = mDefaultReverse;
        super.onBackPressed();
    }

    /**
     * Display current flash card based on the position and which side the flash card is on.
     */
    private void displayCard() {
        ArrayList<FlashCard> content;
        if (mCardReverse) {
            content = mReverseCardAL;
        } else {
            content = mFrontCardAL;
        }
        if (mFlashCard != null && content != null && content.size() > 0) {
            int position = mCardPosition + 1;
            mCardPositionView.setText(String.format("Card %d out of %d", position,
                    content.size()));
            TextView flashCardTitle = (TextView) mFlashCard.findViewById(R.id.title);
            TextView flashCardDesc = (TextView) mFlashCard.findViewById(R.id.description);
            ImageView flashCardImage = (ImageView) mFlashCard.findViewById(R.id.card_image);

            flashCardTitle.setText(content.get(mCardPosition).getTitle());
            flashCardDesc.setText(content.get(mCardPosition).getDescription());

            if (content.get(mCardPosition).getImage() != null) {
                Picasso.with(FlashCardActivity.this).load(content.get(mCardPosition).getImage())
                        .into(flashCardImage);
            }
        }
    }

    /**
     * Start the flash card transition animation.
     *
     * @param anim animation for transition.
     */
    private void startFlashCardAnimation(Animation anim) {
        mFlashCard.clearAnimation();
        mFlashCard.setAnimation(anim);
        mFlashCard.startAnimation(anim);
    }

    /**
     * Start the flash card flip animation.
     *
     * @param fromDegree degree the flip animation starts from.
     * @param toDegree degree the flip animation ends at.
     */
    private void startFlashCardAnimation(float fromDegree, float toDegree) {
        final FlipAnimation flipAnimation = new FlipAnimation(fromDegree, toDegree,
                mFlashCard.getWidth() / 2f, mFlashCard.getHeight() / 2f);
        flipAnimation.setFillAfter(true);
        flipAnimation.setDuration(150);
        flipAnimation.setInterpolator(new AccelerateInterpolator());
        flipAnimation.setAnimationListener(this);
        mFlashCard.startAnimation(flipAnimation);
    }

    /**
     * Displays a glow on the side when you have hit the end of the deck.
     *
     * @param edgeView the view that gets displayed.
     */
    private void displayViewEdge(final RelativeLayout edgeView) {
        edgeView.setVisibility(View.VISIBLE);

        new CountDownTimer(300,100) {

            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                edgeView.setVisibility(View.GONE);
            }
        }.start();
    }

    /**
     * Flash card object.
     */
    private class FlashCard {
        private String title;
        private String image;
        private String description;

        /**
         * Constructor for a flash card object.
         *
         * @param title flash card title.
         * @param image flash card image url.
         * @param description flash card description.
         */
        public FlashCard(String title, String image, String description) {
            this.title = title;
            this.image = image;
            this.description = description;
        }

        /**
         * Getter for the flash card title.
         *
         * @return flash card title.
         */
        public String getTitle() {
            return this.title;
        }

        /**
         * Getter for the flash card image url.
         *
         * @return flash card image url.
         */
        public String getImage() {
            return this.image;
        }

        /**
         * Getter for the flash card description.
         *
         * @return the flash card description.
         */
        public String getDescription() {
            return this.description;
        }
    }
}
