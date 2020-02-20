/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package co.chatsdk.ui.activities;

import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import co.chatsdk.core.dao.Keys;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.utils.UserListItemConverter;
import co.chatsdk.ui.R;
import co.chatsdk.ui.R2;
import co.chatsdk.ui.adapters.UsersListAdapter;
import co.chatsdk.ui.databinding.ActivitySelectContactsBinding;
import co.chatsdk.ui.icons.Icons;
import io.reactivex.functions.Predicate;

/**
 * Created by itzik on 6/17/2014.
 */
public abstract class SelectContactActivity extends BaseActivity {

    protected UsersListAdapter adapter;
    protected boolean multiSelectEnabled;

    protected ActivitySelectContactsBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, getLayout());

        Predicate<NetworkEvent> contactChanged = ne -> {
            // Make a filter for user update events
            return NetworkEvent.filterContactsChanged().test(ne) || NetworkEvent.filterType(EventType.UserMetaUpdated).test(ne);
        };

        // Refresh the list when the contacts change
        dm.add(ChatSDK.events().sourceOnMain()
                .filter(contactChanged)
                .subscribe(networkEvent -> loadData()));

        initViews();
        initList();

        setMultiSelectEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, R.id.action_search, 0, getString(R.string.search));
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setIcon(Icons.get(Icons.choose().search, R.color.app_bar_icon_color));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            ChatSDK.ui().startSearchActivity(this);
        }
        return true;
    }

    protected @LayoutRes int getLayout() {
        return R.layout.activity_select_contacts;
    }

    protected void initViews() {
        super.initViews();
        b.fab.setOnClickListener(v -> {
            doneButtonPressed(UserListItemConverter.toUserList(adapter.getSelectedUsers()));
        });
        b.fab.setImageDrawable(Icons.get(Icons.choose().check, R.color.fab_icon_color));
    }

    protected void initList() {
        adapter = new UsersListAdapter(multiSelectEnabled);

        b.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerView.setAdapter(adapter);

        loadData();

        dm.add(adapter.onClickObservable().subscribe(item -> {
            if (item instanceof User) {
                if (multiSelectEnabled) {
                    adapter.toggleSelection(item);
                    userSelectionChanged(getUserList());
                } else {
                        doneButtonPressed(Arrays.asList((User) item));
                }
            }
        }));
    }

    protected List<User> getUserList () {
        return UserListItemConverter.toUserList(adapter.getSelectedUsers());
    }

    protected void loadData () {
        adapter.setUsers(new ArrayList<>(ChatSDK.contact().contacts()), true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected void userSelectionChanged (List<User> users) {
        refreshDoneButtonVisibility();
    }

    abstract protected void doneButtonPressed (List<User> users);

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void setMultiSelectEnabled (boolean enabled) {
        multiSelectEnabled = enabled;
        refreshDoneButtonVisibility();
        adapter.setMultiSelectEnabled(enabled);
    }

    public void refreshDoneButtonVisibility () {
        boolean visible = false;
        if (multiSelectEnabled) {
            visible = adapter.getSelectedCount() > 0;
        }
        b.fab.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

}