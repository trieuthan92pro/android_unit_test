package com.example.soundcloud.search;

import com.example.soundcloud.data.model.Genre;
import com.example.soundcloud.data.model.History;
import com.example.soundcloud.data.model.Song;
import com.example.soundcloud.data.source.SearchHistoryDataSource;
import com.example.soundcloud.data.source.SearchHistoryRepository;
import com.example.soundcloud.data.source.SongDataSource;
import com.example.soundcloud.data.source.SongRepository;

import java.util.ArrayList;
import java.util.List;

public class SearchPresenter implements SearchContract.Presenter {
    private static final int LIMIT = 50;
    private static final String LIMIT_SEARCH_DEFAULT = "12";
    private static final String GENRE = "SEARCH";
    private static final String MSG_DOWNLOAD_DISABLE = "YOU CANNOT DOWNLOAD THIS SONG!";
    private SearchHistoryRepository mHistoryRepository;
    private SearchContract.View mView;
    private SongRepository mSongRepository;
    private List<History> mSearchHistories;
    private List<History> mRecentSearch;
    private String searchKey;
    private Genre mGenre;
    private boolean mIsAdding;

    public SearchPresenter(SearchHistoryRepository searchHistoryRepository,
                           SearchContract.View view,
                           SongRepository songRepository) {
        mHistoryRepository = searchHistoryRepository;
        mView = view;
        mSongRepository = songRepository;
        mRecentSearch = new ArrayList<>();
    }

    @Override
    public void loadHistorySearch(String limit) {
        mHistoryRepository.getHistories(limit,
                new SearchHistoryDataSource.HistorySearchCallback() {
                    public void onSuccess(List<History> searchHistories) {
                        mSearchHistories = searchHistories;
                        mView.showSearchHistory(searchHistories);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        mView.showError(e.getMessage());
                    }
                });
    }

    @Override
    public void loadSearchResult(String searchKey) {
        mSongRepository.searchSong(searchKey, LIMIT,
                new SongDataSource.LoadSongCallback() {
                    @Override
                    public void onSongsLoaded(List<Song> songs) {
                        mView.showProgressBar(false);
                        mView.showSearchResult(songs);
                        mGenre = new Genre(GENRE, songs);
                    }

                    @Override
                    public void onDataNotAvailable(Exception e) {
                        mView.showProgressBar(false);
                        mView.showError(e.getMessage());
                    }
                });
    }

    @Override
    public void saveRecentSearch() {
        mHistoryRepository.saveHistories(mRecentSearch,
                new SearchHistoryDataSource.CallBack() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailed(Exception e) {
                        mView.showError(e.getMessage());
                    }
                });
    }

    @Override
    public void clearSearchHistory() {
        mHistoryRepository.clearHistories(
                new SearchHistoryDataSource.CallBack() {
                    @Override
                    public void onSuccess() {
                        if(mSearchHistories != null){
                            mSearchHistories.clear();
                        }
                        mView.showSearchHistory(mSearchHistories);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        mView.showError(e.getMessage());
                    }
                });
    }

    @Override
    public void start() {
        loadHistorySearch(LIMIT_SEARCH_DEFAULT);
    }

    @Override
    public List<History> getSearchHistories() {
        return mSearchHistories;
    }

    @Override
    public void addSearchKey(History searchHistory) {
        mSearchHistories.add(searchHistory);
    }

    @Override
    public void addSearchKey(History searchHistory, boolean isAdding) {
        if (isAdding) {
            mRecentSearch.add(searchHistory);
            mSearchHistories.add(searchHistory);
        }
    }

    @Override
    public Genre getGenre() {
        return mGenre;
    }

    @Override
    public void onQueryTextSubmit(String query) {
        setSearchKey(query);
        loadSearchResult(query);
        addSearchKey(new History(query), mIsAdding);
        mView.showProgressBar(true);
    }

    @Override
    public String getSearchKey() {
        return searchKey;
    }

    @Override
    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    @Override
    public void setAddSearchKey(boolean isAdding) {
        mIsAdding = isAdding;
    }

    @Override
    public void prepareDownload(int position) {
        final Song song = mGenre.getSongs().get(position);
        mSongRepository.getSongById(song.getId(),
                new SongDataSource.DownloadCallback() {
            @Override
            public void onSuccess(String message) {
                mView.showError(message);
            }

            @Override
            public void onFailed(Exception e) {
                if (isDownloadAble(song)) {
                    download(song);
                } else {
                    mView.showError(MSG_DOWNLOAD_DISABLE);
                }
            }
        });
    }

    private void download(Song song) {
        mSongRepository.download(song, new SongDataSource.DownloadCallback() {
            @Override
            public void onSuccess(String message) {
                song.setDownloadURL(message);
                saveDownload(song);
            }

            @Override
            public void onFailed(Exception e) {
                mView.showError(e.getMessage());
            }
        });
    }

    private boolean isDownloadAble(Song song) {
        return song.isDownloadable();
    }

    private void saveDownload(Song song) {
        mSongRepository.addDownloadedSong(song, new SongDataSource.DownloadCallback() {
            @Override
            public void onSuccess(String message) {
                mView.showSuccess(message);
            }

            @Override
            public void onFailed(Exception e) {
                mView.showError(e.getMessage());
            }
        });
    }
}
