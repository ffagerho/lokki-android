/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import cc.softwarefactory.lokki.android.avatar.AvatarLoader;
import cc.softwarefactory.lokki.android.utils.ContactUtils;
import cc.softwarefactory.lokki.android.utils.DefaultContactUtils;
import cc.softwarefactory.lokki.android.utils.PreferenceUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class AddContactsFragment extends Fragment {

    private static final String TAG = "AddContacts";
    public static Set<String> emailsSelected;
    private ContactUtils mContactUtils;
    private ArrayList<String> contactList;
    private AQuery aq;
    private Boolean cancelAsynTasks = false;
    private Context context;
    private AvatarLoader avatarLoader;
    private EditText inputSearch;
    private ArrayAdapter<String> adapter;

    public AddContactsFragment() {
        emailsSelected = new HashSet<>();
        contactList = new ArrayList<>();
        mContactUtils = new DefaultContactUtils();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_add_contacts, container, false);
        aq = new AQuery(getActivity(), rootView);
        cancelAsynTasks = false;
        context = getActivity().getApplicationContext();
        avatarLoader = new AvatarLoader(context);
        inputSearch = (EditText) rootView.findViewById(R.id.inputText);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(R.string.add_contact);
        loadContacts();
        enableSearchFilter();
    }

    private void loadContacts() {
        String[] loadingList = {getResources().getString(R.string.loading)};
        aq.id(R.id.add_contacts_list_view).adapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, loadingList));
        new getAllEmailAddressesAsync().execute();
    }

    private void enableSearchFilter() {

        inputSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                if (adapter != null) {
                    adapter.getFilter().filter(cs);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }
        });
    }


    public void setContactUtils(ContactUtils contactUtils) {
        this.mContactUtils = contactUtils;
    }

    class prepareAdapterAsync extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            Log.e(TAG, "prepareAdapterAsync");
            getContactList();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean contactsExist) {

            if (isAdded()) {
                Log.e(TAG, "prepareAdapterAsync - ContactList: " + contactList);
                setListAdapter();
            }
            super.onPostExecute(contactsExist);
        }
    }

    class getAllEmailAddressesAsync extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Void... params) {
            return mContactUtils.listContacts(context);
        }

        @Override
        protected void onPostExecute(JSONObject contactsResult) {

            if (contactsResult == null || !isAdded() || cancelAsynTasks) {
                super.onPostExecute(MainApplication.contacts);
                return;
            }
            Log.e(TAG, "Number of contacts: " + (contactsResult.length() - 1));
            try {
                MainApplication.contacts = contactsResult;
                MainApplication.mapping = MainApplication.contacts.getJSONObject("mapping");
                PreferenceUtils.setValue(context, PreferenceUtils.KEY_CONTACTS, MainApplication.contacts.toString());

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
            new prepareAdapterAsync().execute();
            super.onPostExecute(MainApplication.contacts);
        }
    }

    private void getContactList() {

        contactList = new ArrayList<>();

        JSONArray keys = MainApplication.mapping.names();

        if (keys == null) {
            return;
        }
        int contactCount = keys.length();

        for (int i = 0; i < contactCount; i++) {
            try {
                contactList.add(keys.getString(i));
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        Collections.sort(contactList);
        Log.e(TAG, "Adapter ContactList: " + contactList);
    }

    private void setListAdapter() {

        adapter = new ArrayAdapter<String>(getActivity(), R.layout.add_people_row_layout, contactList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                ViewHolder holder;

                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.add_people_row_layout, null);
                    holder = new ViewHolder();
                    holder.name = (TextView) convertView.findViewById(R.id.contact_name);
                    holder.email = (TextView) convertView.findViewById(R.id.contact_email);
                    holder.photo = (ImageView) convertView.findViewById(R.id.contact_photo);
                    holder.check = (CheckBox) convertView.findViewById(R.id.contact_selected);
                    convertView.setTag(holder);

                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                try {
                    AQuery aq = new AQuery(convertView);
                    String contactName = getItem(position);
                    String email = MainApplication.mapping.getString(contactName);

                    avatarLoader.load(email, holder.photo);

                    aq.id(holder.name).text(contactName);
                    aq.id(holder.email).text(email);
                    aq.id(holder.check).tag(email).checked(emailsSelected.contains(email));
                    holder.position = position;

                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
                return convertView;
            }
        };

        aq.id(R.id.add_contacts_list_view).adapter(adapter);
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        ImageView photo;
        CheckBox check;
        int position;
    }


}
