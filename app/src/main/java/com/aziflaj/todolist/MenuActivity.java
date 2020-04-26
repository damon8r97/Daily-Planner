package com.aziflaj.todolist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.val;
//import static java.util.stream.Collectors.collectingAndThen;

public class MenuActivity extends AppCompatActivity {

    /**
     * Client reference
     */
    private MobileServiceClient mClient;

    /**
     * Table used to access data from the mobile app backend.
     */
    private MobileServiceTable<LayoutItem> layoutTable;

    //adapter for layout items
    private LayoutItemAdapter mAdapter;

    /**
     * Progress spinner to use for table operations
     */

    private ProgressBar mProgressBar;
    private EditText mTextEdit;
    private Button mButton;
    private Button mRButton;
    private ImageView mImage;

    private String devID;
    private String LayoutID = "";
    private String m_Text = "";
    private String LayoutName;
    private String flag = "";
    private String AdminID = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_layout);

        devID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        AdminID = devID;

        mProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
        // Initialize the progress bar
        mProgressBar.setVisibility(ProgressBar.GONE);

        mTextEdit = findViewById(R.id.newLayoutName);
        mTextEdit.setVisibility(EditText.GONE);


        mButton = findViewById(R.id.newLayoutButton);
        mButton.setVisibility(Button.GONE);
        mRButton = findViewById(R.id.removeBtn);


        try {
            // Create the client instance, using the provided mobile app URL.
            mClient = new MobileServiceClient(
                    "https://layout441.azurewebsites.net",
                    this).withFilter(new com.aziflaj.todolist.MenuActivity.ProgressFilter());

            // Extend timeout from default of 10s to 20s
            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.SECONDS);
                    client.setWriteTimeout(20, TimeUnit.SECONDS);
                    return client;
                }
            });

            // Get the remote table instance to use.
            layoutTable = mClient.getTable("layoutTable", LayoutItem.class);

            //Init local storage
            initLocalStore().get();

            // Load the items from the mobile app backend.
            refreshItemsFromTable();

            mAdapter = new LayoutItemAdapter(this, R.layout.row_layout_to_do, devID);
            ListView listViewToDo = (ListView) findViewById(R.id.layoutViewToDo);
            listViewToDo.setAdapter(mAdapter);

        } catch (MalformedURLException e) {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        } catch (Exception e) {
            createAndShowDialog(e, "Error");
        }
    }


    public void checkItemInTable(LayoutItem item) throws ExecutionException, InterruptedException {
        layoutTable.update(item).get();
    }

    public void checkItem(final LayoutItem item) {
        if (mClient == null) {
            return;
        }


        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    checkItemInTable(item);
                        LayoutID = item.getLID();
                        LayoutName = item.getLName();
                        AdminID = item.getAdmin();

//                    change button to say delete item
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                          if(AdminID.equals(devID)){
                            mRButton.setText("Delete");
                          }
                          else{
                              mRButton.setText("Remove");
                          }
                        }
                    });


                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        runAsyncTask(task);

    }


    private void createAndShowDialogFromTask(final Exception exception, String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAndShowDialog(exception, "Error");
            }
        });
    }

    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }
    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    /**
     * Initializes the activity menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.layout_menu_todo, menu);
        return true;
    }

    /**
     * Select an option from the menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshItemsFromTable();
        }

        return true;
    }



    //allows for async task run by the appropriate executor
    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }



    private void refreshItemsFromTable() {

        // Get the layout names that for the user and add them in the
        // adapter

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final List<LayoutItem> results = refreshItemsFromMobileServiceTable();
                    //Offline Sync
                    //final List<ToDoItem> results = refreshItemsFromMobileServiceTableSyncTable();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.clear();
                            for (LayoutItem item: results) {
                                mAdapter.add(item);
                            }
                        }
                    });
                } catch (final Exception e){
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        runAsyncTask(task);
    }

    private List<LayoutItem> refreshItemsFromMobileServiceTable() throws ExecutionException, InterruptedException {
    //search the database for any objects that have the correct userID (From this device)
          List<LayoutItem> results =  layoutTable
                                     .where()
                                     .field("userID").eq(devID)
                                     .execute()
                                     .get();
    //initialize blank array of strings, every time we see a layoutID more than once, we remove that layout from the  display
          List<String> copies = new ArrayList<String>();
          for(LayoutItem item : results){
              if(copies.contains(item.getLName())){
                  results.remove(item);
              }
              else {
                  copies.add(item.getLName());
              }
          }
          return results;
    }



    private AsyncTask<Void, Void, Void> initLocalStore() throws MobileServiceLocalStoreException, ExecutionException, InterruptedException {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    MobileServiceSyncContext syncContext = mClient.getSyncContext();

                    if (syncContext.isInitialized())
                        return null;

                    SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "OfflineStore", null, 1);

                    Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("complete", ColumnDataType.Boolean);
                    tableDefinition.put("layoutID", ColumnDataType.String);
                    tableDefinition.put("layoutName", ColumnDataType.String);
                    tableDefinition.put("userID", ColumnDataType.String);
                    tableDefinition.put("adminID", ColumnDataType.String);

                    localStore.defineTable("LayoutItem", tableDefinition);

                    SimpleSyncHandler handler = new SimpleSyncHandler();

                    syncContext.initialize(localStore, handler).get();

                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        return runAsyncTask(task);
    }

    public void menuRefresh(MenuItem item) {
        mRButton.setText("Remove");
        refreshItemsFromTable();
    }


    private class ProgressFilter implements ServiceFilter {

        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

            final SettableFuture<ServiceFilterResponse> resultFuture = SettableFuture.create();


            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });

            ListenableFuture<ServiceFilterResponse> future = nextServiceFilterCallback.onNext(request);

            Futures.addCallback(future, new FutureCallback<ServiceFilterResponse>() {
                @Override
                public void onFailure(Throwable e) {
                    resultFuture.setException(e);
                }

                @Override
                public void onSuccess(ServiceFilterResponse response) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.GONE);
                        }
                    });

                    resultFuture.set(response);
                }
            });

            return resultFuture;
        }
    }


    public void createNewLayout(View view) {

        if (mClient == null) {
            return;
        }

        if(checkValue(m_Text)) {

            final LayoutItem newItem = new LayoutItem();
            newItem.setLID(generateLayoutID());
            LayoutID = newItem.getLID();
            newItem.setLName(m_Text);
            LayoutName = m_Text;
            newItem.setComplete(false);
            newItem.setUser(devID);
            newItem.setAdmin(devID);

            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        final LayoutItem entity = addItemInTable(newItem);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!entity.isComplete()) {
                                    mAdapter.add(entity);
                                }
                            }
                        });
                    } catch (final Exception e) {
                        createAndShowDialogFromTask(e, "Error");
                    }
                    return null;
                }
            };

            runAsyncTask(task);
        }
        else
            return;
    }



    public LayoutItem addItemInTable(LayoutItem item) throws ExecutionException, InterruptedException {
        LayoutItem entity = layoutTable.insert(item).get();
        return entity;
    }

    public boolean checkValue(String input){
        if(input!= null && input.matches("\\A\\p{ASCII}*\\z")){
            return true;
        }
        else{
            return false;
        }
    }
    public String generateLayoutID() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        // this will convert any number sequence into 6 character.
        return String.format("%06d", number);
    }

    public void generateLayoutName(View view) {
        flag = "create";
        mTextEdit.setVisibility(EditText.VISIBLE);
        mButton.setVisibility(Button.VISIBLE);
        //makes the input visible, when confirm is pressed, getInputL is called.
    }

    public void getInputL(View view){
        m_Text = mTextEdit.getText().toString();
        mTextEdit.setVisibility(EditText.GONE);
        mButton.setVisibility(Button.GONE);
        if(flag == "create") {
            createNewLayout(view);
        }
        else if(flag == "join"){
            joinNewLayout(view);
        }
        mTextEdit.setText("");
    }


    public void joinNewLayout(View view) {
        final LayoutItem joinLayout = new LayoutItem();

        try {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {

                    try {
                        List<LayoutItem> results = layoutTable
                                .where()
                                .field("layoutID").eq(m_Text)
                                .execute()
                                .get();

                        if (!results.isEmpty()) {
                            joinLayout.setUser(devID);
                            joinLayout.setLID(m_Text);
                            joinLayout.setLName(results.get(0).getLName());
                            joinLayout.setAdmin(results.get(0).getAdmin());


                            try {
                                final LayoutItem entity = addItemInTable(joinLayout);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!entity.isComplete()) {
                                            mAdapter.add(entity);
                                        }
                                    }
                                });
                            } catch (final Exception e) {
                                createAndShowDialogFromTask(e, "Error");
                            }

                        } else {
                            createAndShowDialog("Cannot Join: ", "Incorrect Layout ID");
                        }

                        return null;
                    } catch (final Exception e) {
                        createAndShowDialogFromTask(e, "Error");
                    }

                    return null;
                }

            };
            runAsyncTask(task);
        } catch (final Exception e) {
            createAndShowDialogFromTask(e, "Error");
        }

    }




    public void generateLayoutID(View view) {
        flag = "join";
        mTextEdit.setVisibility(EditText.VISIBLE);
        mButton.setVisibility(Button.VISIBLE);
        //makes the input visible, when confirm is pressed, getInputL is called.

    }


    public void checkDelete(View view){
        if(AdminID.equals(devID)){
            deleteItemAdmin(view);
        }
        else{
            deleteItem(view);
        }
    }

    public void deleteItem(View view) {
        try {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        List<LayoutItem> results = layoutTable
                                .where()
                                .field("layoutID").eq(LayoutID)
                                .and()
                                .field("userID").eq(devID)
                                .execute()
                                .get();
                        if (!results.isEmpty()) {
                            Log.d("There were results", "I think");
                            Log.d("check", results.get(0).getLName());
                            layoutTable.delete(results.get(0));

                            refreshItemsFromTable();

                        } else {
                            createAndShowDialog("Please reload your menu.", "Error");
                        }

                        return null;
                    } catch (final Exception e) {
                        createAndShowDialogFromTask(e, "Error");
                    }

                    return null;
                }

            };
            runAsyncTask(task);
        } catch (final Exception e) {
            createAndShowDialogFromTask(e, "Error");
        }


    }



    public void deleteItemAdmin(View view) {
        try {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        List<LayoutItem> results = layoutTable
                                .where()
                                .field("layoutID").eq(LayoutID)
                                .execute()
                                .get();
                        if (!results.isEmpty()) {
                            for(LayoutItem L : results)
                            layoutTable.delete(L);
                            refreshItemsFromTable();

                        } else {
                            createAndShowDialog("Please reload your menu.", "Error");
                        }

                        return null;
                    } catch (final Exception e) {
                        createAndShowDialogFromTask(e, "Error");
                    }

                    return null;
                }

            };
            runAsyncTask(task);
        } catch (final Exception e) {
            createAndShowDialogFromTask(e, "Error");
        }


    }





    public void launchLayoutActivity(View view) {
        Intent layoutIntent = new Intent(this, LayoutActivity.class);

        //Create the bundle
        Bundle bundle = new Bundle();

        //Add data to bundle

        bundle.putString("LayoutID", LayoutID);
        bundle.putString("devID", devID);
        bundle.putString("LayoutName", LayoutName);

        //Add the bundle to the intent
        layoutIntent.putExtras(bundle);

        //Fire menu activity
        startActivity(layoutIntent);
    }



}


