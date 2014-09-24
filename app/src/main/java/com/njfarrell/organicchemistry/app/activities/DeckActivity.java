/**
 * Copyright Nate Farrell. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.njfarrell.organicchemistry.app.R;
import com.njfarrell.organicchemistry.app.database.tables.DecksTable;
import com.njfarrell.organicchemistry.app.preferences.AppPreferences;
import com.njfarrell.organicchemistry.app.services.ContentDownloadService;
import com.njfarrell.organicchemistry.app.utilities.AppUtil;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author Nate Farrell <njfarrel@gmail.com>
 *
 * This activity displays a list of flash card decks to choose from as well as sorting options.
 */
public class DeckActivity extends FragmentActivity implements AdapterView.OnItemClickListener,
        AdapterView.OnItemSelectedListener {

    private String api_do;
    private JSONObject api_with;
    private CardDeckResponseReceiver mCardDeckResponseReceiver;
    private String mDeckGroup = "All Decks";
    private String mResponseString;

    private ListView mDeckList;
    private ArrayAdapter<String> mDropdownAdapter;
    private CardDeckAdapter mListAdapter;
    private RelativeLayout mBackgroundView;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        CardDeck card = mListAdapter.getItem(position);
        int deckId = card.getDeckId();
        String deckTitle = card.getName();
        Intent i = new Intent(this, FlashCardActivity.class);
        i.putExtra("deck_id", deckId);
        i.putExtra("deck_title", deckTitle);
        startActivity(i);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (view != null) {
            CharSequence sequence = ((TextView) view).getText();
            if (sequence != null) {
                mDeckGroup = sequence.toString();
                createCardDeckList();
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Broadcast receiver for content download service.
     */
    public class CardDeckResponseReceiver extends BroadcastReceiver {
        public static final String CARD_DECK_RESPONSE = "com.njfarrell.intent.action.cardDeck";

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey(ContentDownloadService.KEY_RESPONSE_JSON)) {
                try {
                    mResponseString = extras.getString(ContentDownloadService.KEY_RESPONSE_JSON);
                    JSONObject response = new JSONObject(mResponseString);
                    if (response.has("success")) {
                        boolean success = (Boolean) response.get("success");
                        if (success && api_do.equals("new_install")) {
                            AppPreferences prefs = AppPreferences.newInstance(DeckActivity.this);
                            prefs.storeBoolean(AppPreferences.KEY_APP_FIRST_RUN, false);
                            api_do = "get_deckgroups";
                            ContentDownloadService.startContentDownloadService(DeckActivity.this,
                                    api_do);
                        } else if (success && api_do.equals("get_deckgroups")) {
                            storeContentIntoDatabase(mResponseString);
                            setupDecksetDropdown();
                            api_do = "get_decks";
                            checkDatabaseForContent();
                        } else if (success && api_do.equals("get_decks")) {
                            storeContentIntoDatabase(mResponseString);
                            createCardDeckList();
                        }
                    }
                } catch (JSONException e) {
                    // Dont handle exception
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_deck_list);

        mBackgroundView = (RelativeLayout) findViewById(R.id.background_view);

        ArrayList<CardDeck> mContentAL = new ArrayList<CardDeck>();
        mListAdapter = new CardDeckAdapter(this, mContentAL);

        mDeckList = (ListView) findViewById(R.id.deck_list);
        mDeckList.setAdapter(mListAdapter);
        mDeckList.setOnItemClickListener(this);

        Spinner dropdown = (Spinner) findViewById(R.id.deckSet);
        mDropdownAdapter = new ArrayAdapter<String>(this, R.layout.deckset_selector);
        mDropdownAdapter.setDropDownViewResource(R.layout.dropdown_deckset);
        dropdown.setAdapter(mDropdownAdapter);
        dropdown.setOnItemSelectedListener(this);

        // Check to see if this is a first run or not
        // If it is a first run, send the device information to server
        AppPreferences mPrefs = AppPreferences.newInstance(this);
        if (mPrefs.getBooleanValue(AppPreferences.KEY_APP_FIRST_RUN, true)) {
            displayFirstRunDialog();
            api_do = "new_install";
            api_with = AppUtil.getDeviceInfo(this);
        } else {
            api_do = "get_deckgroups";
        }

        checkDatabaseForContent();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mBackgroundView.setBackgroundResource(R.drawable.background_image_land);
        } else {
            mBackgroundView.setBackgroundResource(R.drawable.background_image);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register local broadcast receiver for content download service.
        IntentFilter filter = new IntentFilter(CardDeckResponseReceiver.CARD_DECK_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mCardDeckResponseReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister local broadcast receiver
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mCardDeckResponseReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // Launch settings activity
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, 1234);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // When settings activity is finished, check for content and see if new content needs
        // to be downloaded.
        if (requestCode == 1234) {
            api_do = "get_deckgroups";
            checkDatabaseForContent();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Check database for card deck groups as well as card decks. If content doesnt exist or the
     * content has expired, the download service is started.
     */
    private void checkDatabaseForContent() {
        DecksTable decksTable = new DecksTable(this);
        mResponseString = decksTable.queryTableByAPI(api_do);

        if (mResponseString != null) {
            if (api_do.equals("get_deckgroups")) {
                setupDecksetDropdown();
                api_do = "get_decks";
                mResponseString = decksTable.queryTableByAPI(api_do);
                if (mResponseString != null) {
                    createCardDeckList();
                } else {
                    mCardDeckResponseReceiver = new CardDeckResponseReceiver();
                    ContentDownloadService.startContentDownloadService(this, api_do, api_with,
                            null, null);
                }
            } else if (api_do.equals("get_decks")) {
                createCardDeckList();
            }
        } else {
            mCardDeckResponseReceiver = new CardDeckResponseReceiver();
            ContentDownloadService.startContentDownloadService(this, api_do, api_with,
                    null, null);
        }
    }

    /**
     * Stores the json response from the server into the data table.
     *
     * @param responseString response from the server.
     */
    private void storeContentIntoDatabase(String responseString) {
        DecksTable decksTable = new DecksTable(this);
        ContentValues values = new ContentValues();
        int id;
        if (api_do.equals("get_deckgroups")) {
            id = DecksTable.ID_COLUMN_DECK_SET;
        } else {
            id = DecksTable.ID_COLUMN_DECK_LIST;
        }
        values.put(DecksTable.COL_ID, id);
        values.put(DecksTable.API_DO, api_do);
        if (api_with != null) {
            values.put(DecksTable.API_WITH, api_with.toString());
        }
        values.put(DecksTable.RESPONSE, responseString);
        values.put(DecksTable.TIMESTAMP, System.currentTimeMillis());
        decksTable.replaceRow(values);
    }

    /**
     * Create the dropdown spinner for the deck groups.
     */
    private void setupDecksetDropdown() {
        JSONArray dataArray;
        try {
            JSONObject response = new JSONObject(mResponseString);
            mDropdownAdapter.clear();
            mDropdownAdapter.add("All Decks");
            dataArray = response.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject group = new JSONObject(dataArray.get(i).toString());
                String deckGroup = group.optString("deck_group");
                mDropdownAdapter.add(deckGroup);
            }
            mDropdownAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the list of card decks.
     */
    private void createCardDeckList() {
        ArrayList<CardDeck> deckList = new ArrayList<CardDeck>();
        if (AppUtil.isValidString(mResponseString)) {
            JSONArray dataArray;
            try {
                JSONObject response = new JSONObject(mResponseString);
                dataArray = response.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    CardDeck deck = new CardDeck((JSONObject) dataArray.get(i));
                    if (deck.getGroup().equals(mDeckGroup)
                            || mDeckGroup.equals("All Decks")) {
                        deckList.add(deck);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        mListAdapter = new CardDeckAdapter(DeckActivity.this, deckList);
        mDeckList.setAdapter(mListAdapter);
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Dialog that is displayed the first time the app runs.
     */
    private void displayFirstRunDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        alertDialogBuilder.setTitle("Welcome");

        // set dialog message
        alertDialogBuilder
                .setMessage("To save you data and improve your experience, this application " +
                        "will download updates from the Internet only when you need them. "+
                        "Once you view a card side for the first time, we won't download it " +
                        "again. If you would like to use a card deck offline, simply flip "+
                        "through the cards when you're on online, and they'll be available " +
                        "for offline use. You can further control how often we update the " +
                        "decks by changing the settings.")
                .setCancelable(false)
                .setPositiveButton("Got It!",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        dialog.dismiss();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    /**
     * Card Deck object.
     */
    private class CardDeck {
        private String name;
        private String description;
        private String image;
        private String deckGroup;
        private int deckId;

        /**
         * Getter for a card deck name.
         *
         * @return card deck name.
         */
        public String getName() {
            return name;
        }

        /**
         * Getter for card deck image url.
         *
         * @return card deck image url.
         */
        public String getImage() {
            return image;
        }

        /**
         * Getter for card deck group.
         *
         * @return card deck group.
         */
        public String getGroup() {
            return deckGroup;
        }

        /**
         * Getter for card deck id.
         *
         * @return card deck id.
         */
        public int getDeckId() {
            return deckId;
        }

        /**
         * Getter for card deck description.
         *
         * @return card deck description.
         */
        public String getDescription(){
            return description;
        }

        /**
         * Card Deck constructor.
         *
         * @param dataJO JSON object retreived from the servers response.
         */
        public CardDeck(JSONObject dataJO) {
            try {
                if (dataJO.has("deck_title")) {
                    name = dataJO.getString("deck_title");
                }
                if (dataJO.has("description")) {
                    description = dataJO.getString("description");
                }
                if (dataJO.has("cover")) {
                    image = dataJO.getString("cover");
                }
                if (dataJO.has("deck_group")) {
                    deckGroup = dataJO.getString("deck_group");
                }
                if (dataJO.has("id")) {
                    deckId = dataJO.getInt("id");
                }
            } catch (JSONException e) {
                // Do not handle exception, leave values as default values
            }
        }
    }

    /**
     * Custom list adapter for the card deck list.
     */
    public class CardDeckAdapter extends ArrayAdapter<CardDeck> {

        public CardDeckAdapter(Context context, ArrayList<CardDeck> contentAL) {
            super(context, R.layout.layout_deck_item, contentAL);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView;
            if (convertView instanceof RelativeLayout) {
                rowView = convertView;
            } else {
                rowView = inflater.inflate(R.layout.layout_deck_item, parent, false);
            }

            CardDeck deck = getItem(position);

            if (rowView != null) {
                ImageView deckIcon = (ImageView) rowView.findViewById(R.id.cardDeckImage);
                TextView deckTitle = (TextView) rowView.findViewById(R.id.cardDeckName);
                TextView deckDesc = (TextView) rowView.findViewById(R.id.cardDeckDescription);

                deckTitle.setText(deck.getName());
                deckDesc.setText(deck.getDescription());

                if (deck.getImage() != null) {
                    Picasso.with(DeckActivity.this).load(deck.getImage()).into(deckIcon);
                }

                rowView.setTag(deck.getDeckId());
            }

            return rowView;
        }
    }
}
