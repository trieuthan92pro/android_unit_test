package com.example.soundcloud.search;

import com.example.soundcloud.data.model.Genre;
import com.example.soundcloud.data.model.History;
import com.example.soundcloud.data.model.Song;
import com.example.soundcloud.data.source.SearchHistoryDataSource;
import com.example.soundcloud.data.source.SearchHistoryRepository;
import com.example.soundcloud.data.source.SongDataSource;
import com.example.soundcloud.data.source.SongRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;


public class SearchPresenterTest {
    private static final String sSearchKey = "Hello";
    private static final int LIMIT = 50;
    private static final String LIMIT_SEARCH_DEFAULT = "12";
    private static final String GENRE = "SEARCH";
    private static final String MSG_DOWNLOAD_DISABLE = "YOU CANNOT DOWNLOAD THIS SONG!";
    @Mock
    private SearchHistoryRepository mHistoryRepository;
    @Mock
    private SearchContract.View mView;
    @Mock
    private SongRepository mSongRepository;

    private List<History> mSearchHistories;
    private List<History> mRecentSearch;
    private List<Song> mSongs;
    private String searchKey;
    private Genre mGenre;
    private boolean mIsAdding;

    private SearchPresenter mSearchPresenter;

    @Captor
    private ArgumentCaptor<SearchHistoryDataSource.HistorySearchCallback> mHistorySearchCallbackCaptor;
    @Captor
    private ArgumentCaptor<SongDataSource.LoadSongCallback> mLoadSongCallbackCaptor;
    @Captor
    private ArgumentCaptor<SearchHistoryDataSource.CallBack> mCallBackCaptor;

    @Before
    public void setupSearchPresenter() {
        MockitoAnnotations.initMocks(this);
        mSongs = new ArrayList<>();
        mSearchHistories = new ArrayList<>();
        mSearchHistories.add(new History(sSearchKey));
        mSearchPresenter = new SearchPresenter(mHistoryRepository, mView, mSongRepository);
        mRecentSearch = new ArrayList<>();
    }

    @Test
    public void getHistoryFromRepositoryAndLoadIntoView() {
        mSearchPresenter.loadHistorySearch(LIMIT_SEARCH_DEFAULT);
        Mockito.verify(mHistoryRepository).getHistories(eq(LIMIT_SEARCH_DEFAULT),
                mHistorySearchCallbackCaptor.capture());
        mHistorySearchCallbackCaptor.getValue().onSuccess(mSearchHistories);
        Mockito.verify(mView).showSearchHistory(mSearchHistories);
    }

    @Test
    public void getSearchResultFromRepositoryAndLoadIntoView() {
        mSearchPresenter.loadSearchResult(eq(sSearchKey));
        Mockito.verify(mSongRepository).searchSong(eq(sSearchKey), eq(LIMIT),
                mLoadSongCallbackCaptor.capture());
        mLoadSongCallbackCaptor.getValue().onSongsLoaded(mSongs);
        Mockito.verify(mView).showProgressBar(false);
        Mockito.verify(mView).showSearchResult(mSongs);
    }

    @Test
    public void saveHistoryFailed() {
        Exception e = new Exception();
        mSearchPresenter.saveRecentSearch();
        Mockito.verify(mHistoryRepository).saveHistories(eq(new ArrayList<>()), mCallBackCaptor.capture());
        mCallBackCaptor.getValue().onFailed(e);
        Mockito.verify(mView).showError(e.getMessage());
    }

    @Test
    public void clearHistory() {
        mSearchPresenter.clearSearchHistory();
        Mockito.verify(mHistoryRepository).clearHistories(mCallBackCaptor.capture());
        mCallBackCaptor.getValue().onSuccess();
        Mockito.verify(mView).showSearchHistory(null);
    }

    @Test
    public void clearHistoryFailed() {
        Exception e = new Exception();
        mSearchPresenter.clearSearchHistory();
        Mockito.verify(mHistoryRepository).clearHistories(mCallBackCaptor.capture());
        mCallBackCaptor.getValue().onFailed(e);
        Mockito.verify(mView).showError(e.getMessage());
    }
}
