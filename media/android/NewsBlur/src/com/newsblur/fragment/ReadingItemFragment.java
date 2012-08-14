package com.newsblur.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.newsblur.R;
import com.newsblur.activity.NewsBlurApplication;
import com.newsblur.activity.Profile;
import com.newsblur.database.FeedProvider;
import com.newsblur.domain.Comment;
import com.newsblur.domain.Story;
import com.newsblur.domain.UserProfile;
import com.newsblur.network.APIManager;
import com.newsblur.network.domain.ProfileResponse;
import com.newsblur.util.ImageLoader;
import com.newsblur.util.UIUtils;

public class ReadingItemFragment extends Fragment {

	private static final String TAG = "ReadingItemFragment";
	public Story story;
	private LayoutInflater inflater;
	private APIManager apiManager;
	private ImageLoader imageLoader;
	private String feedColor;
	private String feedFade;
	private HashMap<String, UserProfile> friendUserMap = new HashMap<String, UserProfile>();
	private HashMap<String, UserProfile> publicUserMap = new HashMap<String, UserProfile>();
	private ContentResolver resolver;

	public static ReadingItemFragment newInstance(Story story, String feedFaviconColor, String feedFaviconFade) { 
		ReadingItemFragment readingFragment = new ReadingItemFragment();

		Bundle args = new Bundle();
		args.putSerializable("story", story);
		args.putString("feedColor", feedFaviconColor);
		args.putString("feedFade", feedFaviconFade);
		readingFragment.setArguments(args);

		return readingFragment;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		imageLoader = ((NewsBlurApplication) getActivity().getApplicationContext()).getImageLoader();
		apiManager = new APIManager(getActivity());
		story = getArguments() != null ? (Story) getArguments().getSerializable("story") : null;

		resolver = getActivity().getContentResolver();
		inflater = getActivity().getLayoutInflater();
		
		feedColor = getArguments().getString("feedColor");
		feedFade = getArguments().getString("feedFade");
	}

	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		this.inflater = inflater;

		View view = inflater.inflate(R.layout.fragment_readingitem, null);


		WebView web = (WebView) view.findViewById(R.id.reading_webview);
		setupWebview(web);
		setupItemMetadata(view);
		if (story.sharedUserIds.length > 0 || story.commentCount > 0 ) {
			view.findViewById(R.id.reading_shared_container).setVisibility(View.VISIBLE);
			setupItemCommentsAndShares(view);
		}

		return view;
	}

	private void setupItemCommentsAndShares(final View view) {
		new AsyncTask<Void, Void, Void>() {
			private List<UserProfile> profiles = new ArrayList<UserProfile>();
			private ArrayList<View> publicCommentViews;
			private ArrayList<View> friendCommentViews;

			@Override
			protected Void doInBackground(Void... arg0) {
				for (String userId : story.sharedUserIds) {
					ProfileResponse user = apiManager.getUser(userId);
					friendUserMap.put(userId, user.user);
				}

				for (String userId : story.friendUserIds) {
					ProfileResponse user = apiManager.getUser(userId);
					friendUserMap.put(userId, user.user);
				}
				
				for (String userId : story.publicUserIds) {
					ProfileResponse user = apiManager.getUser(userId);
					publicUserMap.put(userId, user.user);
				}

				Cursor cursor = resolver.query(FeedProvider.COMMENTS_URI, null, null, new String[] { story.id }, null);

				publicCommentViews = new ArrayList<View>();
				friendCommentViews = new ArrayList<View>();
				
				while (cursor.moveToNext()) {
					Comment comment = Comment.fromCursor(cursor);
						View commentView = inflater.inflate(R.layout.include_comment, null);
						TextView commentText = (TextView) commentView.findViewById(R.id.comment_text);
						commentText.setText(comment.commentText);
						ImageView commentImage = (ImageView) commentView.findViewById(R.id.comment_user_image);
						TextView commentSharedDate = (TextView) commentView.findViewById(R.id.comment_shareddate);
						commentSharedDate.setText(comment.sharedDate);
						
					if (publicUserMap.containsKey(comment.userId)) {
						UserProfile commentUser = publicUserMap.get(comment.userId);
						TextView commentUsername = (TextView) commentView.findViewById(R.id.comment_username);
						commentUsername.setText(commentUser.username);
						String userPhoto = commentUser.photoUrl;
						imageLoader.displayImage(userPhoto, commentImage);
						publicCommentViews.add(commentView);
					} else {
						UserProfile commentUser = friendUserMap.get(comment.userId);
						TextView commentUsername = (TextView) commentView.findViewById(R.id.comment_username);
						commentUsername.setText(commentUser.username);
						String userPhoto = commentUser.photoUrl;
						imageLoader.displayImage(userPhoto, commentImage);
						friendCommentViews.add(commentView);
					}
				}
				return null;
			}

			protected void onPostExecute(Void result) {
				for (final String userId : story.sharedUserIds) {
					ImageView image = new ImageView(getActivity());
					int imageLength = UIUtils.convertDPsToPixels(getActivity(), 25);
					image.setMaxHeight(imageLength);
					image.setMaxWidth(imageLength);
					GridLayout.LayoutParams imageParameters = new GridLayout.LayoutParams();
					imageParameters.height = imageLength;
					imageParameters.width = imageLength;
					image.setLayoutParams(imageParameters);

					GridLayout grid = (GridLayout) view.findViewById(R.id.reading_social_shareimages);					
					imageLoader.displayImageByUid(friendUserMap.get(userId).photoUrl, image);
					image.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							Intent i = new Intent(getActivity(), Profile.class);
							i.putExtra(Profile.USER_ID, userId);
							startActivity(i);
						}
					});
					grid.addView(image);
				}
				
				for (final String userId : story.friendUserIds) {
					ImageView image = new ImageView(getActivity());
					int imageLength = UIUtils.convertDPsToPixels(getActivity(), 25);
					image.setMaxHeight(imageLength);
					image.setMaxWidth(imageLength);
					GridLayout.LayoutParams imageParameters = new GridLayout.LayoutParams();
					imageParameters.height = imageLength;
					imageParameters.width = imageLength;
					image.setLayoutParams(imageParameters);

					GridLayout grid = (GridLayout) view.findViewById(R.id.reading_social_shareimages);					
					imageLoader.displayImageByUid(friendUserMap.get(userId).photoUrl, image);
					image.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							Intent i = new Intent(getActivity(), Profile.class);
							i.putExtra(Profile.USER_ID, userId);
							startActivity(i);
						}
					});
					grid.addView(image);
				}


				for (View comment : publicCommentViews) {
					((LinearLayout) view.findViewById(R.id.reading_public_comment_container)).addView(comment);
				}
				for (View comment : friendCommentViews) {
					((LinearLayout) view.findViewById(R.id.reading_friend_comment_container)).addView(comment);
				}
			};
		}.execute();
	}


	private void setupItemMetadata(View view) {

		View borderOne = view.findViewById(R.id.row_item_favicon_borderbar_1);
		View borderTwo = view.findViewById(R.id.row_item_favicon_borderbar_2);

		if (!TextUtils.equals(feedColor, "#null") && !TextUtils.equals(feedFade, "#null")) {
			borderOne.setBackgroundColor(Color.parseColor(feedColor));
			borderTwo.setBackgroundColor(Color.parseColor(feedFade));
		} else {
			borderOne.setBackgroundColor(Color.GRAY);
			borderTwo.setBackgroundColor(Color.LTGRAY);
		}

		View sidebar = view.findViewById(R.id.row_item_sidebar);
		int storyIntelligence = story.intelligence.intelligenceAuthors + story.intelligence.intelligenceTags + story.intelligence.intelligenceFeed + story.intelligence.intelligenceTitle;
		if (storyIntelligence > 0) {
			sidebar.setBackgroundResource(R.drawable.positive_count_circle);
		} else if (storyIntelligence == 0) {
			sidebar.setBackgroundResource(R.drawable.neutral_count_circle);
		} else {
			sidebar.setBackgroundResource(R.drawable.negative_count_circle);
		}

		TextView itemTitle = (TextView) view.findViewById(R.id.reading_item_title);
		TextView itemDate = (TextView) view.findViewById(R.id.reading_item_date);
		TextView itemAuthors = (TextView) view.findViewById(R.id.reading_item_authors);
		//		GridLayout tagContainer = (GridLayout) view.findViewById(R.id.reading_item_tags);
		//
		//		if (story.tags != null || story.tags.length > 0) {
		//			tagContainer.setVisibility(View.VISIBLE);
		//			for (String tag : story.tags) {
		//				View v = inflater.inflate(R.layout.tag_view, null);
		//				TextView tagText = (TextView) v.findViewById(R.id.tag_text);
		//				tagText.setText(tag);
		//				tagContainer.addView(v);
		//			}
		//		}

		itemDate.setText(story.shortDate);
		itemTitle.setText(story.title);
		itemAuthors.setText(story.authors);
	}

	private void setupWebview(WebView web) {
		web.getSettings().setLoadWithOverviewMode(true);
		web.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		web.getSettings().setDomStorageEnabled(true);
		web.getSettings().setSupportZoom(true);
		web.getSettings().setAppCacheMaxSize(1024*1024*8);
		web.getSettings().setAppCachePath("/data/data/com.newsblur/cache");
		web.getSettings().setAllowFileAccess(true);
		web.getSettings().setAppCacheEnabled(true);
		web.setVerticalScrollBarEnabled(false);
		web.setHorizontalScrollBarEnabled(false);

		StringBuilder builder = new StringBuilder();
		// TODO: Define a better strategy for rescaling the HTML across device screen sizes and storying this HTML as boilerplate somewhere
		builder.append("<html><head><meta name=\"viewport\" content=\"target-densitydpi=device-dpi\" /><link rel=\"stylesheet\" type=\"text/css\" href=\"reading.css\" /></head><body>");
		builder.append(story.content);
		builder.append("</body></html>");
		web.loadDataWithBaseURL("file:///android_asset/", builder.toString(), "text/html", "UTF-8", null);
	}


}
