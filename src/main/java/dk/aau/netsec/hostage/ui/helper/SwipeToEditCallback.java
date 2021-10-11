package dk.aau.netsec.hostage.ui.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import dk.aau.netsec.hostage.R;

/**
 * Animate on left swipe of item in ProfileManagerFragment (swipe-to-edit). Display
 * coloured background and pen icon.
 *
 * @author Filip Adamik
 * Created on 09-06-2021
 */
public abstract class SwipeToEditCallback extends ItemTouchHelper.SimpleCallback {

    private final Drawable deleteIcon;
    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final ColorDrawable background;
    private final int backgroundColor;
    private final Paint clearPaint;

    public SwipeToEditCallback(Context context) {
        super(0, ItemTouchHelper.LEFT);

        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit);
        deleteIcon.setTint(context.getColor(R.color.colorOnPrimary));

        intrinsicWidth = deleteIcon.getIntrinsicWidth();
        intrinsicHeight = deleteIcon.getIntrinsicHeight();
        background = new ColorDrawable();
        backgroundColor = ContextCompat.getColor(context, R.color.colorPrimaryVariant);
        clearPaint = new Paint();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Disable swiping on certain rows (for example a hint row or non-editable profile).
     */
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        //TODO once swipe hint is implemented, this should be enabled or removed
        /*
          To disable "swipe" for specific item return 0 here.
          For example:
          if (viewHolder?.itemViewType == YourAdapter.SOME_TYPE) return 0
          if (viewHolder?.adapterPosition == 0) return 0
         */
//        if (viewHolder?.adapterPosition == 10) return 0

        return super.getMovementFlags(recyclerView, viewHolder);
    }


    /**
     * {@inheritDoc}
     * <p>
     * Disable dragging items up and down
     */
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Draw green background and pen icon on swipe.
     */
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCanceled = dX == 0f && !isCurrentlyActive;

        // If user abandoned swipe, return to previous state
        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        background.setColor(backgroundColor);
        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        background.draw(c);

        int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int deleteIconMargin = (itemHeight - intrinsicHeight) / 2;
        int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
        int deleteIconRight = itemView.getRight() - deleteIconMargin;
        int deleteIconBottom = deleteIconTop + intrinsicHeight;

        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
        deleteIcon.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    /**
     * Clear canvas and restore item view to its previous state (before the swipe)
     */
    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        c.drawRect(left, top, right, bottom, clearPaint);
    }

}
