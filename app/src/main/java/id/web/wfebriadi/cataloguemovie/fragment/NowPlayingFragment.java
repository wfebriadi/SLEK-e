package id.web.wfebriadi.cataloguemovie.fragment;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.List;

import id.web.wfebriadi.cataloguemovie.BuildConfig;
import id.web.wfebriadi.cataloguemovie.R;
import id.web.wfebriadi.cataloguemovie.adapter.MovieAdapter;
import id.web.wfebriadi.cataloguemovie.adapter.PaginationScrollListener;
import id.web.wfebriadi.cataloguemovie.api.ClientAPI;
import id.web.wfebriadi.cataloguemovie.api.MovieInterface;
import id.web.wfebriadi.cataloguemovie.model.ItemResults;
import id.web.wfebriadi.cataloguemovie.model.NowPlayingModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class NowPlayingFragment extends Fragment {

    private RecyclerView recyclerView;
    private Context context;
    private MovieAdapter movieAdapter;
    private ProgressBar progressBar;
    private LinearLayout layoutErrorMsg;
    private Button btnRetry;
    private FloatingActionButton fab;

    private LinearLayoutManager linearLayoutManager;
    private static final int PAGE_START = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int TOTAL_PAGES = 10;
    private int currentPage = PAGE_START;

    public NowPlayingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_now_playing, container, false);
        context = view.getContext();
        progressBar = view.findViewById(R.id.loading_nowplaying);
        recyclerView = view.findViewById(R.id.rv_nowplaying);
        layoutErrorMsg = view.findViewById(R.id.error_msg_nowplaying);
        btnRetry = view.findViewById(R.id.btn_retry_nowplaying);
        fab = view.findViewById(R.id.scrollup_nowplaying);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recyclerView.smoothScrollToPosition(0);
            }
        });

        setupDataList();

        return view;
    }

    private void setupDataList() {
        progressBar.setVisibility(View.VISIBLE);
        movieAdapter = new MovieAdapter(context);
        linearLayoutManager = new LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(movieAdapter);

        recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int visibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();

                super.onScrolled(recyclerView, dx, dy);
                if (visibleItemPosition < 10){
                    fab.hide();
                } else if (dy > 0 && fab.getVisibility() == View.VISIBLE) {
                    fab.hide();
                } else if (dy < 0 && fab.getVisibility() != View.VISIBLE) {
                    fab.show();
                }
            }

            @Override
            protected void loadMoreItem() {
                isLoading = true;
                currentPage +=1;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadNextPage();
                    }
                },1000);
            }

            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });

        loadFirstPage();
    }

    private void loadFirstPage() {
        currentPage =1;
        MovieInterface movieInterface = ClientAPI.getMovieDB().create(MovieInterface.class);
        Call<NowPlayingModel> nowplayingCall = movieInterface.getNowPlayingMovie(BuildConfig.API_KEY,BuildConfig.LANGUAGE,currentPage);
        nowplayingCall.enqueue(new Callback<NowPlayingModel>() {
            @Override
            public void onResponse(@NonNull Call<NowPlayingModel> call, @NonNull Response<NowPlayingModel> response) {
                if (response.isSuccessful()){
                    Log.e("Now Playing", response.toString());
                    List<ItemResults> results = response.body().getResults();
                    movieAdapter.addAll(results);
                    progressBar.setVisibility(View.INVISIBLE);

                    if (currentPage <= TOTAL_PAGES) movieAdapter.addLoadingFooter();
                    else isLastPage = true;
                } else {
                    dispErrorMsg();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NowPlayingModel> call, @NonNull Throwable t) {
                dispErrorMsg();
            }
        });
    }

    private void loadNextPage() {
        MovieInterface movieInterface = ClientAPI.getMovieDB().create(MovieInterface.class);
        Call<NowPlayingModel> nowplayingCall = movieInterface.getNowPlayingMovie(BuildConfig.API_KEY,BuildConfig.LANGUAGE,currentPage);
        nowplayingCall.enqueue(new Callback<NowPlayingModel>() {
            @Override
            public void onResponse(@NonNull Call<NowPlayingModel> call, @NonNull Response<NowPlayingModel> response) {
                Log.e("Now Playing", response.toString());
                movieAdapter.removedLoadingFooter();
                isLoading = false;

                List<ItemResults> results = response.body().getResults();
                movieAdapter.addAll(results);

                if (currentPage != TOTAL_PAGES) movieAdapter.addLoadingFooter();
                else isLastPage = true;
            }

            @Override
            public void onFailure(@NonNull Call<NowPlayingModel> call, @NonNull Throwable t) {
                dispErrorMsg();
            }
        });
    }

    private void dispErrorMsg() {
        progressBar.setVisibility(View.INVISIBLE);
        layoutErrorMsg.setVisibility(View.VISIBLE);
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupDataList();
                layoutErrorMsg.setVisibility(View.INVISIBLE);
            }
        });
    }
}
