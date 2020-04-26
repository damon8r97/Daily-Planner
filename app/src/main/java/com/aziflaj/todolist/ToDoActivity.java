package com.aziflaj.todolist;


import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

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
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOperations;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;
import com.squareup.okhttp.OkHttpClient;

import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.*;

public class ToDoActivity extends AppCompatActivity {

    /**
     * Client reference
     */
    private MobileServiceClient mClient;

    /**
     * Table used to access data from the mobile app backend.
     */
    private MobileServiceTable<ToDoItem> mToDoTable;

    //Offline Sync
    /**
     * Table used to store data locally sync with the mobile app backend.
     */
    //private MobileServiceSyncTable<ToDoItem> mToDoTable;

    /**
     * Adapter to sync the items list with the view
     */
    private ToDoItemAdapter mAdapter;

    /**
     * Progress spinner to use for table operations
     */
    private ProgressBar mProgressBar;

    /*
     * To store LayoutID, LayoutName, and BlockVal from previous activity
     */

    private String LayoutID;
    private String LayoutName;
    private String blockVal;
    private String TaskID;
    private String currentStatus = "New";
    private String flag = "";
    private String TaskName;

    //list of statuses available to a task
    List<String> Status = Arrays.asList("New", "In Progress", "Complete");

    //hidden visual elements
    private EditText mEditText;
    private Button mAddButton;
    private RadioGroup mRadioGroup;

    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_do);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        Bundle bundle = getIntent().getExtras();

        LayoutID = bundle.getString("LayoutID");
        blockVal = bundle.getString("blockVal");
        LayoutName = bundle.getString("LayoutName");

        this.setTitle(LayoutName+": " +blockVal);

        mProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
        mEditText = (EditText) findViewById(R.id.newTaskName);
        mAddButton = (Button) findViewById(R.id.newTaskButton);
        mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);

        // Initialize the progress bar
        mProgressBar.setVisibility(ProgressBar.GONE);
        mEditText.setVisibility(EditText.GONE);
        mAddButton.setVisibility(Button.GONE);
        mRadioGroup.setVisibility(RadioGroup.GONE);

        try {
            // Create the client instance, using the provided mobile app URL.
            mClient = new MobileServiceClient(
                    "https://layout441.azurewebsites.net",
                    this).withFilter(new ProgressFilter());

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
            mToDoTable = mClient.getTable("taskTable",ToDoItem.class);

            // Offline sync table instance.
            //mToDoTable = mClient.getSyncTable("ToDoItem", ToDoItem.class);

            //Init local storage
            initLocalStore().get();

            // Create an adapter to bind the items with the view
            mAdapter = new ToDoItemAdapter(this, R.layout.row_list_to_do);
            ListView listViewToDo = (ListView) findViewById(R.id.listViewToDo);
            listViewToDo.setAdapter(mAdapter);

            // Load the items from the mobile app backend.
            refreshItemsFromTable();

        } catch (MalformedURLException e) {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        } catch (Exception e){
            createAndShowDialog(e, "Error");
        }
    }

    /**
     * Initializes the activity menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_todo, menu);
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

    /**
     * Mark an item as completed
     *
     * @param item
     *            The item to mark
     */
    public void checkItem(final ToDoItem item) {
        if (mClient == null) {
            return;
        }
        // Set the item as completed and update it in the table
        if(item.getStatus() == "Complete") {
            item.setComplete(true);
        }

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    checkItemInTable(item);
                    TaskID = item.getTID();
                    TaskName = item.getText();
                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        runAsyncTask(task);

    }

    /**
     * Mark an item as completed in the Mobile Service Table
     *
     * @param item
     *            The item to mark
     */
    public void checkItemInTable(ToDoItem item) throws ExecutionException, InterruptedException {
        mToDoTable.update(item).get();
    }

    /**
     * Add a new item
     *
     * @param view
     *            The view that originated the call
     */
    public void addItem(View view) {
        if (mClient == null) {
            return;
        }

        // Create a new item
        final ToDoItem item = new ToDoItem();

        item.setText(mEditText.getText().toString());
        item.setComplete(false);
        item.setTID(generateTaskID());
        item.setBV(blockVal);
        item.setTStatus(Status.get(0));
        item.setLID(LayoutID);

        // Insert the new item
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final ToDoItem entity = addItemInTable(item);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!entity.isComplete()){
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

    /**
     * Add an item to the Mobile Service Table
     *
     * @param item
     *            The item to Add
     */
    public ToDoItem addItemInTable(ToDoItem item) throws ExecutionException, InterruptedException {
        ToDoItem entity = mToDoTable.insert(item).get();
        return entity;
    }

    /**
     * Refresh the list with the items in the Table
     */
    private void refreshItemsFromTable() {

        // Get the items that weren't marked as completed and add them in the
        // adapter

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final List<ToDoItem> results = refreshItemsFromMobileServiceTable();

                    //Offline Sync
                    //final List<ToDoItem> results = refreshItemsFromMobileServiceTableSyncTable();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.clear();

                            for (ToDoItem item : results) {
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

    /**
     * Refresh the list with the items in the Mobile Service Table
     */

    private List<ToDoItem> refreshItemsFromMobileServiceTable() throws ExecutionException, InterruptedException {
        return mToDoTable
                .where()
                .field("LayoutID").eq(val(LayoutID))
                .and()
                .field("blockVal").eq(val(blockVal))
                .execute()
                .get();
    }

    //Offline Sync
    /**
     * Refresh the list with the items in the Mobile Service Sync Table
     */
    /*private List<ToDoItem> refreshItemsFromMobileServiceTableSyncTable() throws ExecutionException, InterruptedException {
        //sync the data
        sync().get();
        Query query = QueryOperations.field("complete").
                eq(val(false));
        return mToDoTable.read(query).get();
    }*/

    /**
     * Initialize local storage
     * @return
     * @throws MobileServiceLocalStoreException
     * @throws ExecutionException
     * @throws InterruptedException
     */
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
                    tableDefinition.put("taskName", ColumnDataType.String);
                    tableDefinition.put("complete", ColumnDataType.Boolean);

                    tableDefinition.put("taskStatus", ColumnDataType.String);
                    tableDefinition.put("LayoutID", ColumnDataType.String);
                    tableDefinition.put("taskID", ColumnDataType.String);
                    tableDefinition.put("blockVal", ColumnDataType.String);

                    localStore.defineTable("ToDoItem", tableDefinition);

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

    //Offline Sync
    /**
     * Sync the current context and the Mobile Service Sync Table
     * @return
     */
    /*
    private AsyncTask<Void, Void, Void> sync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    MobileServiceSyncContext syncContext = mClient.getSyncContext();
                    syncContext.push().get();
                    mToDoTable.pull(null).get();
                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }
                return null;
            }
        };
        return runAsyncTask(task);
    }
    */

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialogFromTask(final Exception exception, String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAndShowDialog(exception, "Error");
            }
        });
    }


    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message
     *            The dialog message
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    /**
     * Run an ASync task on the corresponding executor
     * @param task
     * @return
     */
    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }

    public void setIP(View view) {
        currentStatus = Status.get(1);
    }

    public void setComp(View view){
        currentStatus = Status.get(2);
    }

    public void removeTask(View view) {
        try {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Log.d("is it", "FROZEN?");
                    try {
                        List<ToDoItem> results = mToDoTable
                                .where()
                                .field("taskID").eq(TaskID)
                                .execute()
                                .get();
                        Log.d("is it", "FROZEN?   2");
                        if (!results.isEmpty()) {
                            mToDoTable.delete(results.get(0));
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

    public void menuRefresh(MenuItem item) {
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

    public void createTask(View view) {
        flag = "create";
        mEditText.setVisibility(EditText.VISIBLE);
        mAddButton.setVisibility(Button.VISIBLE);
    }

    public void getInputT(View view){

        //hide the button
        mAddButton.setVisibility(Button.GONE);
        Log.d("flag", flag);

        if(flag == "create"){
            mEditText.setVisibility(EditText.GONE);
            addItem(view);
            mEditText.setText("");
        }
        else if(flag == "stat"){
            mRadioGroup.setVisibility(RadioGroup.GONE);
            updateItemStatus(view);
            mRadioGroup.clearCheck();
        }

    }


    public void updateItemStatus(View view) {
        try {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {

                    try {
                        List<ToDoItem> results = mToDoTable
                                .where()
                                .field("taskID").eq(TaskID)
                                .execute()
                                .get();
                        if (!results.isEmpty()) {
                            ToDoItem entity = new ToDoItem();
                            entity = results.get(0);
                            Log.d("current status: ", entity.getStatus());
                            entity.setTStatus(currentStatus);
                            Log.d("new status: ", entity.getStatus());
                            if(currentStatus == "Complete"){
                                entity.setComplete(true);
                            }
                            else{
                                entity.setComplete(false);
                            }
                            mToDoTable.update(entity).get();
                            refreshItemsFromTable();
                            currentStatus = "New";
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

    public String generateTaskID() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        // this will convert any number sequence into 6 character.
        return String.format("%06d", number);
    }

    public void updateStatus(View view) {
        flag = "stat";
        Log.d("flag", flag);
        mRadioGroup.setVisibility(RadioGroup.VISIBLE);
        mAddButton.setVisibility(Button.VISIBLE);
    }


    //once comment activity has been defined
    public void launchCommentActivity(View view){
        Intent commentIntent = new Intent(this, CommentActivity.class);

        //Create the bundle
        Bundle bundle = new Bundle();

//        Add data to bundle

        bundle.putString("TaskID", TaskID);
        bundle.putString("LayoutID", LayoutID);
        bundle.putString("TaskName", TaskName);

//        Add the bundle to the intent
        commentIntent.putExtras(bundle);

//        Fire menu activity
        startActivity(commentIntent);
    }

}