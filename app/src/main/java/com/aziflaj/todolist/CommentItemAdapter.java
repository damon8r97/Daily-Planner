package com.aziflaj.todolist;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

/**
 * Adapter to bind a CommentItem List to a view
 */
public class CommentItemAdapter extends ArrayAdapter<CommentItem> {

    /**
     * Adapter context
     */
    Context mContext;

    /**
     * Adapter View layout
     */
    int mLayoutResourceId;

    public CommentItemAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);

        mContext = context;
        mLayoutResourceId = layoutResourceId;
    }

    /**
     * Returns the view for a specific item on the list
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        final CommentItem currentItem = getItem(position);

        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);
        }

        row.setTag(currentItem);
        final CheckBox checkBox = (CheckBox) row.findViewById(R.id.checkCommentItem);
        checkBox.setText(currentItem.getText());
        checkBox.setChecked(false);
        checkBox.setEnabled(true);

        checkBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (checkBox.isChecked()) {
                    if (mContext instanceof CommentActivity) {
                        CommentActivity activity = (CommentActivity) mContext;

                        activity.checkItem(currentItem);

                        //our checkbox is in a list in a viewgroup, so get that viewgroup
                        ViewGroup vg = (ViewGroup) arg0.getParent().getParent();
                        //get the number of children in that viewgroup (each list has one checkbox
                        int size = vg.getChildCount();

                        //print to debug log how many list are in the view group

                        //for every list in the viewgroup
                        for (int i = 0; i < size; i++) {
                            View v = vg.getChildAt(i); //get that list

                            CheckBox cb = v.findViewById(R.id.checkCommentItem); //get the checkbox in that list

                            if (!cb.equals(arg0)) { //if that checkbox isn't the one we just checked
                                cb.setChecked(false); //uncheck it
                            }
                        }
                    }
                }
            }
        });
        return row;
    }

}

