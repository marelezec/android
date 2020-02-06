package mega.privacy.android.app.lollipop.managerSections;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.fragments.MegaNodeBaseFragment;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.SortUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

public class IncomingSharesFragmentLollipop extends MegaNodeBaseFragment {

    @Override
    public void activateActionMode() {
		super.activateActionMode();
		actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionBarCallBack());
    }

	private class ActionBarCallBack implements ActionMode.Callback {
		
		boolean showRename = false;
		boolean showMove = false;
		boolean showLink = false;
		boolean showCopy = false;
		boolean showTrash =false;

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

			List<MegaNode> documents = adapter.getSelectedNodes();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, false);
					break;
				}
				case R.id.cab_menu_trash:{

					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
					break;
				}
				case R.id.cab_menu_rename:{

					if (documents.size()==1){
						((ManagerActivityLollipop) context).showRenameDialog(documents.get(0), documents.get(0).getName());
					}
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					clearSelections();
					hideMultipleSelect();
					break;
				}	
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);

					clearSelections();
					hideMultipleSelect();

					break;
				}
				case R.id.cab_menu_share_link:{

					if (documents.size()==1){
						NodeController nC = new NodeController(context);
						nC.exportLink(documents.get(0));
					}
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_leave_multiple_share: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					((ManagerActivityLollipop) context).showConfirmationLeaveMultipleShares(handleList);
					break;
				}
				case R.id.cab_menu_send_to_chat:{
					logDebug("Send files to chat");
					ArrayList<MegaNode> nodesSelected = adapter.getArrayListSelectedNodes();
					NodeController nC = new NodeController(context);
					nC.checkIfNodesAreMineAndSelectChatsToSendNodes(nodesSelected);
					clearSelections();
					hideMultipleSelect();
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			((ManagerActivityLollipop)context).hideFabButton();
			((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
			((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
			((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
			((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO_DELAY);
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();
			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

			menu.findItem(R.id.cab_menu_send_to_chat).setIcon(mutateIconSecondary(context, R.drawable.ic_send_to_contact, R.color.white));

			boolean showSendToChat = false;

			if (selected.size() != 0) {

				showCopy = true;
				showMove = false;
				showTrash =false;
				showRename=false;
				if(selected.size()==adapter.getItemCount()){
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				  	if(selected.size()==1){
				  		showRename=true;
					}else{
						showRename=false;
					}
					showMove = false;
					showTrash=false;

				}else if(selected.size()==1){

                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

					if((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
						showRename = true;

					}else if(megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK){
						showRename = false;

					}else if(megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READ).getErrorCode() == MegaError.API_OK){
						showRename = false;
					}

 				}else{

                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
					showRename = false;
				}

				if (((ManagerActivityLollipop)context).deepBrowserTreeIncoming == 0){

					showTrash = false;
					showMove = false;
					menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(true);
					menu.findItem(R.id.cab_menu_leave_multiple_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				}else{

					if((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
						showTrash = true;
						showMove = true;
					}else{
						showTrash = false;
						showMove = false;
					}

					menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
					menu.findItem(R.id.cab_menu_leave_multiple_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				}

				boolean areFiles = true;
				for (int i=0; i<selected.size(); i++) {
					if (selected.get(i).isFolder()) {
						areFiles = false;
						break;
					}
				}

				if(areFiles && isChatEnabled()) {
					showSendToChat = true;
				}
				else {
					showSendToChat = false;
				}

				menu.findItem(R.id.cab_menu_send_to_chat).setVisible(showSendToChat);
				menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
			else{
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(true);
			menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);

			menu.findItem(R.id.cab_menu_copy).setVisible(true);
			menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);

			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);

			return false;
		}
	}

	public static IncomingSharesFragmentLollipop newInstance() {
		return new IncomingSharesFragmentLollipop();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		logDebug("Parent Handle: " + ((ManagerActivityLollipop) context).getParentHandleIncoming());

		if (megaApi.getRootNode() == null) {
			return null;
		}

		((ManagerActivityLollipop) context).showFabButton();

		View v;

		if (((ManagerActivityLollipop) context).isList) {
			v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyLinearLayout = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop) context).getParentHandleIncoming(), recyclerView, null, INCOMING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			} else {
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}
		} else {
			v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);

			recyclerView = (NewGridRecyclerView) v.findViewById(R.id.file_grid_view_browser);
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(80, outMetrics));
			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyLinearLayout = (LinearLayout) v.findViewById(R.id.file_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_grid_empty_text_first);
			addSectionTitle(nodes, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop) context).getParentHandleIncoming(), recyclerView, null, INCOMING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			} else {
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}
		}

		adapter.setParentHandle(((ManagerActivityLollipop) context).getParentHandleIncoming());
		adapter.setListFragment(recyclerView);

		if (((ManagerActivityLollipop) context).getParentHandleIncoming() == INVALID_HANDLE) {
			logWarning("ParentHandle -1");
			findNodes();
		} else {
			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleIncoming());
			logDebug("ParentHandle to find children: " + ((ManagerActivityLollipop) context).getParentHandleIncoming());

			nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop) context).orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());
			adapter.setNodes(nodes);
		}

		recyclerView.setClipToPadding(false);
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				checkScroll();
			}
		});

		((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();

		adapter.setMultipleSelect(false);
		recyclerView.setAdapter(adapter);
		fastScroller.setRecyclerView(recyclerView);
		visibilityFastScroller();
		setEmptyView();

		logDebug("Deep browser tree: " + ((ManagerActivityLollipop) context).deepBrowserTreeIncoming);

		return v;
	}

	@Override
	public void refresh (){
		logDebug("refresh");
		MegaNode parentNode = null;

		if (((ManagerActivityLollipop) context).getParentHandleIncoming() == -1
				|| megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleIncoming()) == null) {
			findNodes();
		} else {
			parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleIncoming());
			nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop) context).orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());
			adapter.setNodes(nodes);
		}

		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		visibilityFastScroller();
		clearSelections();
		hideMultipleSelect();
		setEmptyView();
	}

    public void openFile(MegaNode node, int position, int[] screenPosition, ImageView imageView) {
        if (MimeTypeList.typeForName(node.getName()).isImage()) {
            Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
            //Put flag to notify FullScreenImageViewerLollipop.
            intent.putExtra("placeholder", placeholderCount);
            intent.putExtra("position", position);
            intent.putExtra("adapterType", INCOMING_SHARES_ADAPTER);
            intent.putExtra("isFolderLink", false);
            if (megaApi.getParentNode(node).getType() == MegaNode.TYPE_ROOT) {
                intent.putExtra("parentNodeHandle", -1L);
            } else {
                intent.putExtra("parentNodeHandle", megaApi.getParentNode(node).getHandle());
            }

            if (((ManagerActivityLollipop) context).getParentHandleIncoming() == -1) {
                intent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderOthers);
            } else {
                intent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderCloud);
            }

            intent.putExtra("fromShared", true);
            intent.putExtra("screenPosition", screenPosition);
            context.startActivity(intent);
            ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
            imageDrag = imageView;
        } else if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isAudio()) {
            MegaNode file = node;

            String mimeType = MimeTypeList.typeForName(file.getName()).getType();

            Intent mediaIntent;
            boolean internalIntent;
            boolean opusFile = false;
            if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()) {
                mediaIntent = new Intent(Intent.ACTION_VIEW);
                internalIntent = false;
                String[] s = file.getName().split("\\.");
                if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
                    opusFile = true;
                }
            } else {
                internalIntent = true;
                mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
            }
            mediaIntent.putExtra("position", position);
            if (megaApi.getParentNode(node).getType() == MegaNode.TYPE_ROOT) {
                mediaIntent.putExtra("parentNodeHandle", -1L);
            } else {
                mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(node).getHandle());
            }

            if (((ManagerActivityLollipop) context).getParentHandleIncoming() == -1) {
                mediaIntent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderOthers);
            } else {
                mediaIntent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderCloud);
            }
            mediaIntent.putExtra("placeholder", placeholderCount);
            mediaIntent.putExtra("screenPosition", screenPosition);
            mediaIntent.putExtra("adapterType", INCOMING_SHARES_ADAPTER);

            mediaIntent.putExtra("fromShared", true);
            mediaIntent.putExtra("HANDLE", file.getHandle());
            mediaIntent.putExtra("FILENAME", file.getName());
            imageDrag = imageView;
            boolean isOnMegaDownloads = false;
            String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
            File f = new File(downloadLocationDefaultPath, file.getName());
            if (f.exists() && (f.length() == file.getSize())) {
                isOnMegaDownloads = true;
            }
            if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
                File mediaFile = new File(localPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                    mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                } else {
                    mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                }
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart();
                }

                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);

                if (mi.totalMem > BUFFER_COMP) {
					logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                } else {
					logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                }

                String url = megaApi.httpServerGetLocalLink(file);
                mediaIntent.setDataAndType(Uri.parse(url), mimeType);
            }
            if (opusFile) {
                mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
            }
            if (internalIntent) {
                context.startActivity(mediaIntent);
            } else {
                if (isIntentAvailable(context, mediaIntent)) {
                    context.startActivity(mediaIntent);
                } else {
                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);
                    adapter.notifyDataSetChanged();
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    handleList.add(node.getHandle());
                    NodeController nC = new NodeController(context);
                    nC.prepareForDownload(handleList, true);
                }
            }
            ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
        } else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
            MegaNode file = node;

            String mimeType = MimeTypeList.typeForName(file.getName()).getType();

            Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);

            pdfIntent.putExtra("fromShared", true);
            pdfIntent.putExtra("inside", true);
            pdfIntent.putExtra("adapterType", INCOMING_SHARES_ADAPTER);
            boolean isOnMegaDownloads = false;
            String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
            File f = new File(downloadLocationDefaultPath, file.getName());
            if (f.exists() && (f.length() == file.getSize())) {
                isOnMegaDownloads = true;
            }
            if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
                File mediaFile = new File(localPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                    pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                } else {
                    pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart();
                }

                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);

                if (mi.totalMem > BUFFER_COMP) {
					logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                } else {
					logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                }

                String url = megaApi.httpServerGetLocalLink(file);
                pdfIntent.setDataAndType(Uri.parse(url), mimeType);
            }
            pdfIntent.putExtra("HANDLE", file.getHandle());
            pdfIntent.putExtra("screenPosition", screenPosition);
            imageDrag = imageView;
            if (isIntentAvailable(context, pdfIntent)) {
                startActivity(pdfIntent);
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                NodeController nC = new NodeController(context);
                nC.prepareForDownload(handleList, true);
            }
            ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
        } else if (MimeTypeList.typeForName(node.getName()).isURL()) {
			logDebug("Is URL file");
            MegaNode file = node;

            boolean isOnMegaDownloads = false;
            String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
            File f = new File(downloadLocationDefaultPath, file.getName());
            if (f.exists() && (f.length() == file.getSize())) {
                isOnMegaDownloads = true;
            }
			logDebug("isOnMegaDownloads: " + isOnMegaDownloads);
            if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
                File mediaFile = new File(localPath);
                InputStream instream = null;

                try {
                    // open the file for reading
                    instream = new FileInputStream(f.getAbsolutePath());

                    // if file the available for reading
                    if (instream != null) {
                        // prepare the file for reading
                        InputStreamReader inputreader = new InputStreamReader(instream);
                        BufferedReader buffreader = new BufferedReader(inputreader);

                        String line1 = buffreader.readLine();
                        if (line1 != null) {
                            String line2 = buffreader.readLine();

                            String url = line2.replace("URL=", "");

							logDebug("Is URL - launch browser intent");
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                        } else {
                            logError("Not expected format: Exception on processing url file");
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", f), "text/plain");
                            } else {
                                intent.setDataAndType(Uri.fromFile(f), "text/plain");
                            }
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (isIntentAvailable(context, intent)) {
                                startActivity(intent);
                            } else {
                                ArrayList<Long> handleList = new ArrayList<Long>();
                                handleList.add(node.getHandle());
                                NodeController nC = new NodeController(context);
                                nC.prepareForDownload(handleList, true);
                            }
                        }
                    }
                } catch (Exception ex) {

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", f), "text/plain");
                    } else {
                        intent.setDataAndType(Uri.fromFile(f), "text/plain");
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    if (isIntentAvailable(context, intent)) {
                        startActivity(intent);
                    } else {
                        ArrayList<Long> handleList = new ArrayList<Long>();
                        handleList.add(node.getHandle());
                        NodeController nC = new NodeController(context);
                        nC.prepareForDownload(handleList, true);
                    }

                } finally {
                    // close the file.
                    try {
                        instream.close();
                    } catch (IOException e) {
						logDebug("EXCEPTION closing InputStream");
                    }
                }
            } else {
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                NodeController nC = new NodeController(context);
                nC.prepareForDownload(handleList, true);
            }
        } else {
            adapter.notifyDataSetChanged();
            ArrayList<Long> handleList = new ArrayList<Long>();
            handleList.add(node.getHandle());
            NodeController nC = new NodeController(context);
            nC.prepareForDownload(handleList, true);
        }
    }

    @Override
	public void itemClick(int position, int[] screenPosition, ImageView imageView) {
		logDebug("itemClick");

		if (adapter.isMultipleSelect()) {
			logDebug("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0) {
				updateActionModeTitle();
			}
		} else {
			if (nodes.get(position).isFolder()) {
				((ManagerActivityLollipop) context).increaseDeepBrowserTreeIncoming();
				logDebug("Is folder deep: " + ((ManagerActivityLollipop) context).deepBrowserTreeIncoming);

				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;
				if (((ManagerActivityLollipop) context).isList) {
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
				} else {
					lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if (lastFirstVisiblePosition == -1) {
						logDebug("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				logDebug("Push to stack " + lastFirstVisiblePosition + " position");

				lastPositionStack.push(lastFirstVisiblePosition);

				((ManagerActivityLollipop) context).setParentHandleIncoming(n.getHandle());
				((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();
				((ManagerActivityLollipop) context).setToolbarTitle();

				MegaNode infoNode = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleIncoming());
				nodes = megaApi.getChildren(nodes.get(position), ((ManagerActivityLollipop) context).orderCloud);
				addSectionTitle(nodes, adapter.getAdapterType());

				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);
				visibilityFastScroller();
				setEmptyView();
				checkScroll();
				((ManagerActivityLollipop) context).showFabButton();
			} else {
				//Is file
				openFile(nodes.get(position), position, screenPosition, imageView);
			}
		}
	}

	public void findNodes(){
		logDebug("findNodes");
		nodes=megaApi.getInShares();

		if(((ManagerActivityLollipop)context).orderOthers == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByMailDescending(nodes);
		}
		addSectionTitle(nodes,adapter.getAdapterType() );
		adapter.setNodes(nodes);

		setEmptyView();
	}

	@Override
	public int onBackPressed(){
		logDebug("deepBrowserTree:" + ((ManagerActivityLollipop)context).deepBrowserTreeIncoming);

		if (adapter == null){
			return 0;
		}


        if (((ManagerActivityLollipop) context).comesFromNotifications && ((ManagerActivityLollipop) context).comesFromNotificationsLevel == (((ManagerActivityLollipop) context).deepBrowserTreeIncoming)) {
			((ManagerActivityLollipop) context).comesFromNotifications = false;
			((ManagerActivityLollipop) context).comesFromNotificationsLevel = 0;
			((ManagerActivityLollipop) context).comesFromNotificationHandle = -1;
            ((ManagerActivityLollipop) context).selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.NOTIFICATIONS);
			((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(((ManagerActivityLollipop) context).comesFromNotificationDeepBrowserTreeIncoming);
			((ManagerActivityLollipop) context).comesFromNotificationDeepBrowserTreeIncoming = -1;
			((ManagerActivityLollipop) context).setParentHandleIncoming(((ManagerActivityLollipop) context).comesFromNotificationHandleSaved);
			((ManagerActivityLollipop) context).comesFromNotificationHandleSaved = -1;
			((ManagerActivityLollipop) context).refreshIncomingShares();

			return 4;
        }
		else {
			((ManagerActivityLollipop)context).decreaseDeepBrowserTreeIncoming();
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

			if(((ManagerActivityLollipop)context).deepBrowserTreeIncoming==0){
				//In the beginning of the navigation

				logDebug("deepBrowserTree==0");
				((ManagerActivityLollipop) context).setParentHandleIncoming(-1);
				((ManagerActivityLollipop) context).setToolbarTitle();
				findNodes();
				visibilityFastScroller();
				recyclerView.setVisibility(View.VISIBLE);
				int lastVisiblePosition = 0;
				if (!lastPositionStack.empty()) {
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");


				if (lastVisiblePosition >= 0) {

					if (((ManagerActivityLollipop) context).isList) {
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
					else {
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
				}

				((ManagerActivityLollipop) context).showFabButton();

				emptyImageView.setVisibility(View.GONE);
				emptyLinearLayout.setVisibility(View.GONE);

				return 3;
			}
			else if (((ManagerActivityLollipop)context).deepBrowserTreeIncoming>0){
				logDebug("deepTree>0");

				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(((ManagerActivityLollipop)context).getParentHandleIncoming()));

				if (parentNode != null){
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyLinearLayout.setVisibility(View.GONE);

					((ManagerActivityLollipop)context).setParentHandleIncoming(parentNode.getHandle());

					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					((ManagerActivityLollipop)context).setToolbarTitle();

					nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
					addSectionTitle(nodes,adapter.getAdapterType() );

					adapter.setNodes(nodes);
					visibilityFastScroller();
					int lastVisiblePosition = 0;
					if(!lastPositionStack.empty()){
						lastVisiblePosition = lastPositionStack.pop();
						logDebug("Pop of the stack "+lastVisiblePosition+" position");
					}
					logDebug("Scroll to "+lastVisiblePosition+" position");

					if(lastVisiblePosition>=0){

						if(((ManagerActivityLollipop)context).isList){
							mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
						}
						else{
							gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
						}
					}
				}

				((ManagerActivityLollipop) context).showFabButton();
				return 2;
			}
			else{
				logDebug("ELSE deepTree");
				((ManagerActivityLollipop)context).deepBrowserTreeIncoming=0;
				return 0;
			}

		}
	}

	@Override
	public void setNodes(ArrayList<MegaNode> nodes) {
		logDebug("setNodes");
		this.nodes = nodes;
		if (((ManagerActivityLollipop)context).isList) {
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
		} else {
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
		}
		adapter.setNodes(nodes);
	}

	@Override
	protected void setEmptyView() {
		String textToShow;

		if (megaApi.getRootNode().getHandle() == ((ManagerActivityLollipop) context).getParentHandleIncoming()
				|| ((ManagerActivityLollipop) context).getParentHandleIncoming() == -1) {
			if (isScreenInPortrait(context)) {
				emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
			} else {
				emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
			}
			textToShow = String.format(context.getString(R.string.context_empty_incoming));
		} else {
            if (isScreenInPortrait(context)) {
                emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
            } else {
                emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
            }
			textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
		}

		try {
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		} catch (Exception e) {
		}

		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textToShow);
		}
		emptyTextViewFirst.setText(result);

		checkEmptyView();
	}

	public int getDeepBrowserTree(){
		return ((ManagerActivityLollipop)context).deepBrowserTreeIncoming;
	}
}
