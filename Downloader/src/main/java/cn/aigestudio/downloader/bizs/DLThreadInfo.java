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

    /**
     * 比较线程id和url
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DLThreadInfo that = (DLThreadInfo) o;
        if (id != null ? !id.equals(that.id) : that.id != null) return false; //这是线程id
        return baseUrl != null ? baseUrl.equals(that.baseUrl) : that.baseUrl == null;
    }

    /**
     * 比较线程id和url
     *
     * @return
     */
    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (baseUrl != null ? baseUrl.hashCode() : 0);
        return result;
    }
}