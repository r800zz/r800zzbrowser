package com.r800zz.r800zzbrowser.ui.adapters;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.r800zz.r800zzbrowser.R;
import com.r800zz.r800zzbrowser.databinding.FileUploadItemBinding;
import com.r800zz.r800zzbrowser.ui.callbacks.FileUploadItemCallback;
import com.r800zz.r800zzbrowser.ui.callbacks.FileUploadSelectionCallback;
import com.r800zz.r800zzbrowser.utils.SystemUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FileUploadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final String LOGTAG = SystemUtils.createLogtag(FileUploadAdapter.class);

    private List<FileUploadItem> mFilesList;
    private Set<FileUploadItem> mSelectedItems = new LinkedHashSet<>();
    private boolean mIsMultipleSelection = false;
    private String[] mMimeTypes = null;

    @Nullable
    private FileUploadSelectionCallback mSelectionCallback = null;

    public FileUploadAdapter(@Nullable FileUploadSelectionCallback selectionCallback) {
        mSelectionCallback = selectionCallback;

        setHasStableIds(true);
    }

    // For the time being, we only allow the user to upload downloaded files.
    // TODO access and upload other files in the system via the MediaStore
    public void setFilesList(@NonNull final List<FileUploadItem> filesList) {
        // Disable filtering for debugging and show all items
        mFilesList = filesList;
        notifyDataSetChanged();
    }

    private boolean isAcceptedItem(FileUploadItem item) {
        // Force accept all items for debugging
        return true;
    }

    public void setIsMultipleSelection(boolean isMultipleSelection) {
        mIsMultipleSelection = isMultipleSelection;
    }

    public void toggleSelected(@NonNull FileUploadItem item, int position) {
        if (mSelectedItems.contains(item)) {
            mSelectedItems.remove(item);
        } else {
            mSelectedItems.add(item);
        }
        notifyItemChanged(position);
    }

    public void setMimeTypes(String[] mimeTypes) {
        // TODO this requires a new call to setFilesList() to take effect
        mMimeTypes = mimeTypes;
    }

    public int getItemPosition(long id) {
        for (int position = 0; position < mFilesList.size(); position++)
            if (mFilesList.get(position).getId() == id)
                return position;
        return 0;
    }

    private FileUploadItemCallback mItemCallback = (view, item) -> {
        view.requestFocusFromTouch();

        if (mIsMultipleSelection) {
            toggleSelected(item, getItemPosition(item.getId()));
        } else if (mSelectionCallback != null) {
            mSelectionCallback.onSelection(new Uri[]{item.getUri()});
        }
    };

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FileUploadItemBinding binding = DataBindingUtil
                .inflate(LayoutInflater.from(parent.getContext()), R.layout.file_upload_item,
                        parent, false);
        binding.setCallback(mItemCallback);

        return new FileUploadViewHolder(binding);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FileUploadItem item = mFilesList.get(position);
        FileUploadViewHolder itemHolder = (FileUploadViewHolder) holder;
        FileUploadItemBinding binding = itemHolder.binding;

        binding.setItem(item);
        final Uri itemUri = item.getUri();
        TaskRunner taskRunner = new TaskRunner();
        ThumbnailTask thumbnailTask = new ThumbnailTask(binding.layout.getContext(), item.getUri(),
                bitmap -> {
                    if (bitmap == null || binding.getItem() == null || !Objects.equals(itemUri, binding.getItem().getUri()))
                        binding.thumbnail.setImageResource(R.drawable.ic_generic_file);
                    else
                        binding.thumbnail.setImageBitmap(bitmap);
                }
        );
        taskRunner.executeAsync(thumbnailTask, (data) -> {
            thumbnailTask.onPostExecute(data);
        });

        boolean isSelected = mSelectedItems.contains(item);
        binding.layout.setSelected(isSelected);
    }

    @Override
    public int getItemCount() {
        return mFilesList == null ? 0 : mFilesList.size();
    }

    @Override
    public long getItemId(int position) {
        FileUploadItem fileItem = mFilesList.get(position);
        return fileItem.getId();
    }

    public Collection<FileUploadItem> getSelectedItems() {
        return mSelectedItems;
    }

    static class FileUploadViewHolder extends RecyclerView.ViewHolder {
        final FileUploadItemBinding binding;

        FileUploadViewHolder(@NonNull FileUploadItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
