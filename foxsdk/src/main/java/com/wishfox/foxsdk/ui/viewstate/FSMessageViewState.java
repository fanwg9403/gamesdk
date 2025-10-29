package com.wishfox.foxsdk.ui.viewstate;

import com.wishfox.foxsdk.data.model.entity.FSMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:59
 */
public abstract class FSMessageViewState implements FoxSdkViewState {

    public static class Init extends FSMessageViewState {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            return "Init{}";
        }
    }

    public static class LoadList extends FSMessageViewState {
        private List<FSMessage> list;
        private boolean isRefresh;
        private boolean hasMore;
        private long timestamp;

        public LoadList() {
            this.list = new ArrayList<>();
            this.isRefresh = false;
            this.hasMore = false;
            this.timestamp = System.currentTimeMillis();
        }

        public LoadList(List<FSMessage> list, boolean isRefresh, boolean hasMore) {
            this.list = list != null ? new ArrayList<>(list) : new ArrayList<>();
            this.isRefresh = isRefresh;
            this.hasMore = hasMore;
            this.timestamp = System.currentTimeMillis();
        }

        public LoadList(List<FSMessage> list, boolean isRefresh, boolean hasMore, long timestamp) {
            this.list = list != null ? new ArrayList<>(list) : new ArrayList<>();
            this.isRefresh = isRefresh;
            this.hasMore = hasMore;
            this.timestamp = timestamp;
        }

        public List<FSMessage> getList() { return list; }
        public void setList(List<FSMessage> list) { this.list = list; }

        public boolean isRefresh() { return isRefresh; }
        public void setRefresh(boolean refresh) { isRefresh = refresh; }

        public boolean isHasMore() { return hasMore; }
        public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoadList loadList = (LoadList) o;
            return isRefresh == loadList.isRefresh &&
                    hasMore == loadList.hasMore &&
                    timestamp == loadList.timestamp &&
                    Objects.equals(list, loadList.list);
        }

        @Override
        public int hashCode() {
            return Objects.hash(list, isRefresh, hasMore, timestamp);
        }

        @Override
        public String toString() {
            return "LoadList{" +
                    "list=" + list +
                    ", isRefresh=" + isRefresh +
                    ", hasMore=" + hasMore +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    public static class Read extends FSMessageViewState {
        private Long id;

        public Read() {
            this.id = null;
        }

        public Read(Long id) {
            this.id = id;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Read read = (Read) o;
            return Objects.equals(id, read.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Read{" +
                    "id=" + id +
                    '}';
        }
    }

    // Static factory methods for convenience
    public static Init Init() {
        return new Init();
    }

    public static LoadList LoadList(List<FSMessage> list, boolean isRefresh, boolean hasMore) {
        return new LoadList(list, isRefresh, hasMore);
    }

    public static Read Read(Long id) {
        return new Read(id);
    }
}
