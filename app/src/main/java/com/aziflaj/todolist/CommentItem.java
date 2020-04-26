package com.aziflaj.todolist;

public class CommentItem {
    /**
     * Comment item content
     */
    @com.google.gson.annotations.SerializedName("comment")
    private String cText;

    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    /**
     * taskID that the comment comes from
     */
    @com.google.gson.annotations.SerializedName("taskID")
    private String tiText;


    /**
     * commentID for look ups
     */
    @com.google.gson.annotations.SerializedName("commentID")
    private String ciText;

    /**
     * layoutID for look ups
     */
    @com.google.gson.annotations.SerializedName("layoutID")
    private String lText;


    /**
     * CommentItem constructor
     */
    public CommentItem(){}


    @Override
    public String toString() { return getText();}

    /**
     * Initializes a new CommentItem
     *
     */
    public CommentItem(String text, String cid, String tid, String lid) {
        this.setText(text);
        this.setCID(cid);
        this.setTID(tid);
        this.setLID(lid);
    }

    //getters
    public String getText() { return cText; }
    public String getId() { return mId; }
    public String getTID() { return tiText; }
    public String getCID() { return ciText; }
    public String getLID() { return lText; }

    //setters
    public final void setText(String text) { cText = text; }
    public final void setId(String id) { mId = id; }
    public final void setTID(String taskID) { tiText = taskID; }
    public final void setCID(String comID) { ciText = comID; }
    public final void setLID(String layoutID) {lText = layoutID;}

    @Override
    public boolean equals(Object o) {
        return o instanceof CommentItem && ((CommentItem) o).mId == mId;
    }
}

