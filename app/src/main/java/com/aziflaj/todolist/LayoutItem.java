package com.aziflaj.todolist;

import android.util.Log;

public class LayoutItem {
    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    /**
     * Indicates if the item is completed
     */
    @com.google.gson.annotations.SerializedName("complete")
    private boolean mComplete;

    /**
     * layout ID in database
     */
    @com.google.gson.annotations.SerializedName("layoutID")
    private String lText;

    /**
     * layout Name in database
     */
    @com.google.gson.annotations.SerializedName("layoutName")
    private String lnText;

    /**
     * Stores User ID
     */
    @com.google.gson.annotations.SerializedName("userID")
    private String userID;

    /**
     * Stores Admin ID
     */
    @com.google.gson.annotations.SerializedName("adminID")
    private String adminID;


    /**
         * Layout constructor
         */
        public LayoutItem() {

        }

        @Override
        public String toString() {
            return getLID();
        }

        /**
         * Initializes a new ToDoItem
         *
         */
        public LayoutItem(String id, String lId, String lName, String devID) {
            this.setId(id);
            this.setLID(lId);
            this.setLName(lName);
            this.setComplete(false);
            this.setUser(devID);
            this.setAdmin(adminID);
        }

    public String getAdmin() {return adminID; }
    public String getId() {
        return mId;
    }
    public String getLID() {
        return lText;
    }
    public String getLName() {
        return lnText;
    }
    public String getUser() {return userID;}
    /**
     * Sets the item id
     */
    public final void setId(String id) {
        mId = id;
    }

    /**
     * Sets the layout id
     */
    public final void setLID(String lId) {
        lText = lId;
    }

    /**
     * Sets the layout name
     */
    public final void setLName(String lName) {
        lnText = lName;
        Log.d("new name check 2", lnText);
    }

    /**
     * Sets the task id
     */

    /**
     * Sets the user ID
     */
    public final void setUser(String devID) {
        userID = devID;
    }

    /**
     * Sets the admin ID
     */
    public final void setAdmin(String AID) {
        adminID = AID;
    }

    /**
     * Indicates if the item is marked as completed
     */
    public boolean isComplete() {
        return mComplete;
    }

    /**
     * Marks the item as completed or incompleted
     */
    public void setComplete(boolean complete) {
        mComplete = complete;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LayoutItem && ((LayoutItem) o).lnText == lnText;
    }
}

