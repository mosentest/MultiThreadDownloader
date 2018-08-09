package cn.aigestudio.downloader.bizs;

class DLThreadInfo {
    String id;
    String baseUrl;
    int start, end;
    boolean isStop;

    DLThreadInfo(String id, String baseUrl, int start, int end) {
        this.id = id;
        this.baseUrl = baseUrl;
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DLThreadInfo that = (DLThreadInfo) o;

        if (start != that.start) return false;
        if (end != that.end) return false;
        if (isStop != that.isStop) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return baseUrl != null ? baseUrl.equals(that.baseUrl) : that.baseUrl == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (baseUrl != null ? baseUrl.hashCode() : 0);
        result = 31 * result + start;
        result = 31 * result + end;
        result = 31 * result + (isStop ? 1 : 0);
        return result;
    }
}