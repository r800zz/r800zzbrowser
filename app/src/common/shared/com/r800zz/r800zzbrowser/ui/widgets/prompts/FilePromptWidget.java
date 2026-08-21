package com.r800zz.r800zzbrowser.ui.widgets.prompts;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.r800zz.r800zzbrowser.R;
import com.r800zz.r800zzbrowser.VRBrowserActivity;
import com.r800zz.r800zzbrowser.audio.AudioEngine;
import com.r800zz.r800zzbrowser.databinding.PromptFileBinding;
import com.r800zz.r800zzbrowser.downloads.Download;
import com.r800zz.r800zzbrowser.downloads.DownloadsManager;
import com.r800zz.r800zzbrowser.ui.adapters.FileUploadAdapter;
import com.r800zz.r800zzbrowser.ui.adapters.FileUploadItem;
import com.r800zz.r800zzbrowser.ui.callbacks.FileUploadSelectionCallback;
import com.r800zz.r800zzbrowser.ui.widgets.WidgetPlacement;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.content.ContentUris;

public class FilePromptWidget extends PromptWidget implements DownloadsManager.DownloadsListener {


    public interface FilePromptDelegate extends PromptDelegate {
        void confirm(@NonNull Uri[] uris);
    }

    private AudioEngine mAudio;
    private PromptFileBinding mBinding;
    private FileUploadAdapter mFileUploadAdapter;
    private DownloadsManager mDownloadsManager;

    public FilePromptWidget(Context aContext) {
        super(aContext);
        initialize(aContext);
    }

    public FilePromptWidget(Context aContext, AttributeSet aAttrs) {
        super(aContext, aAttrs);
        initialize(aContext);
    }

    public FilePromptWidget(Context aContext, AttributeSet aAttrs, int aDefStyle) {
        super(aContext, aAttrs, aDefStyle);
        initialize(aContext);
    }

    protected void initialize(Context aContext) {
        mDownloadsManager = ((VRBrowserActivity) getContext()).getServicesProvider().getDownloadsManager();
        mAudio = AudioEngine.fromContext(aContext);

        LayoutInflater inflater = LayoutInflater.from(aContext);
        mBinding = DataBindingUtil.inflate(inflater, R.layout.prompt_file, this, true);

        mLayout = mBinding.layout;

        mFileUploadAdapter = new FileUploadAdapter(mOnSelectionCallback);
        mBinding.filesList.setAdapter(mFileUploadAdapter);
        mBinding.filesList.setHasFixedSize(true);
        mBinding.filesList.setItemViewCacheSize(20);
        // Drawing Cache is deprecated in API level 28: https://developer.android.com/reference/android/view/View#getDrawingCache().
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            mBinding.filesList.setDrawingCacheEnabled(true);
            mBinding.filesList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        }

        onDownloadsUpdate(mDownloadsManager.getDownloads());

        mTitle = findViewById(R.id.promptTitle);
        mMessage = findViewById(R.id.promptMessage);

        mBinding.negativeButton.setOnClickListener(view -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            mPromptDelegate.dismiss();
            hide(REMOVE_WIDGET);
        });

        mBinding.positiveButton.setOnClickListener(view -> {
            if (mPromptDelegate instanceof FilePromptDelegate) {
                Collection<FileUploadItem> selectedItems = mFileUploadAdapter.getSelectedItems();
                if (selectedItems.size() > 0) {
                    Uri[] selectedUris = selectedItems.stream().map(FileUploadItem::getUri).toArray(Uri[]::new);
                    ((FilePromptDelegate) mPromptDelegate).confirm(selectedUris);
                } else {
                    mPromptDelegate.dismiss();
                }
            } else {
                Log.w(LOGTAG, "Prompt delegate is not an instance of FilePromptDelegate");
                mPromptDelegate.dismiss();
            }
            hide(REMOVE_WIDGET);
        });
        // hidden unless multiple selection is enabled
        mBinding.positiveButton.setVisibility(GONE);
    }

    public void setIsMultipleSelection(boolean isMultipleSelection) {
        mFileUploadAdapter.setIsMultipleSelection(isMultipleSelection);
        mBinding.positiveButton.setVisibility(isMultipleSelection ? VISIBLE : GONE);

        onDownloadsUpdate(mDownloadsManager.getDownloads());
    }

    public void setMimeTypes(String[] mimeTypes) {
        mFileUploadAdapter.setMimeTypes(mimeTypes);

        onDownloadsUpdate(mDownloadsManager.getDownloads());
    }

    private final FileUploadSelectionCallback mOnSelectionCallback = uris -> {
        ((FilePromptDelegate) mPromptDelegate).confirm(uris);
        hide(REMOVE_WIDGET);
    };

    @Override
    public void show(@ShowFlags int aShowFlags) {
        super.show(aShowFlags);

        onDownloadsUpdate(mDownloadsManager.getDownloads());
    }

    @Override
    public void hide(int aHideFlags) {
        super.hide(aHideFlags);

        mDownloadsManager.removeListener(this);
    }

    @Override
    protected void initializeWidgetPlacement(WidgetPlacement aPlacement) {
        super.initializeWidgetPlacement(aPlacement);
        aPlacement.width = WidgetPlacement.dpDimension(getContext(), R.dimen.prompt_file_width);
        aPlacement.height = WidgetPlacement.dpDimension(getContext(), R.dimen.prompt_file_height);
    }

    private List<FileUploadItem> getFileItemsFromDownloads(@NonNull List<Download> downloads) {
        List<FileUploadItem> items = downloads.
                stream().
                filter(download -> download.getStatus() == Download.SUCCESSFUL).
                map(download -> new FileUploadItem(
                        download.getFilename(),
                        download.getOutputFileUri(),
                        download.getMediaType(),
                        download.getSizeBytes())).
                collect(Collectors.toList());

        // Add files from the system Download directory
        items.addAll(getFileItemsFromStorage());
        return items;
    }

    private List<FileUploadItem> getFileItemsFromStorage() {
        List<FileUploadItem> storageItems = new ArrayList<>();
        String[] paths = {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
        };

        for (String path : paths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && !file.getName().startsWith(".")) {
                            // Force add all files
                            storageItems.add(new FileUploadItem(
                                    file.getName(),
                                    Uri.fromFile(file),
                                    "application/octet-stream", // Re-determined by extension in Adapter
                                    file.length()
                            ));
                        }
                    }
                }
            }
        }

        // Set title for debugging
        if (mTitle != null) {
            mTitle.post(() -> mTitle.setText("Files in Download/DCIM/Pictures"));
        }

        return storageItems;
    }

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type;
    }

    public void onDownloadsUpdate(@NonNull List<Download> downloads) {
        List<FileUploadItem> fileItems = getFileItemsFromDownloads(downloads);
        mFileUploadAdapter.setFilesList(fileItems);
        boolean isEmpty = mFileUploadAdapter.getItemCount() == 0;
        mBinding.setIsEmpty(isEmpty);

        if (isEmpty) {
            // Update UI with diagnostic error message
            mBinding.emptyMessage.setText("Error: Directory access denied. \nPlease check System Settings > Apps > r800zzbrowser > Permissions \nand allow 'All Files access'.");
        }
    }

    public void onDownloadCompleted(@NonNull Download download) {
        List<FileUploadItem> fileItems = getFileItemsFromDownloads(mDownloadsManager.getDownloads());
        mFileUploadAdapter.setFilesList(fileItems);
        mBinding.setIsEmpty(mFileUploadAdapter.getItemCount() == 0);
    }

    public void onDownloadError(@NonNull String error, @NonNull String file) {
        List<FileUploadItem> fileItems = getFileItemsFromDownloads(mDownloadsManager.getDownloads());
        mFileUploadAdapter.setFilesList(fileItems);
        mBinding.setIsEmpty(mFileUploadAdapter.getItemCount() == 0);
    }
}
