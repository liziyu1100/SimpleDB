package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.Permissions;
import simpledb.common.DbException;
import simpledb.common.DeadlockException;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;
import sun.misc.LRUCache;

import java.io.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 * 
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;

    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50;
    private HeapPage[] pages;
    private int rest_num;
    private LinkedHashMap<HeapPageId,Integer> page_q;
    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        this.pages = new HeapPage[numPages];
        this.rest_num = numPages;
        this.page_q = new LinkedHashMap<>();
    }
    
    public static int getPageSize() {
      return pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
    	BufferPool.pageSize = pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
    	BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        // some code goes here
        int visit_rest = DEFAULT_PAGES - rest_num;
        for (int i = 0;i<pages.length;i++){
            if (pages[i]!=null){
                if (pages[i].getId().equals(pid)){
                    return pages[i];
                }
            }
            if (visit_rest == 0)break;
        }
        HeapPage nhp = (HeapPage) Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);
        if (nhp != null){
            this.addPage(tid,nhp);
            return nhp;
        }
        return null;
    }
    public void addPage(TransactionId tid,Page page ){
        for (int i = 0;i<this.pages.length;i++){
            if (this.pages[i]!=null){
                if (this.pages[i].getId().equals(page.getId())){
                    this.pages[i] = (HeapPage) page;
                    page_q.get(pages[i].getId());
                    return;
                }
            }
        }
        if (this.rest_num>0){
            for (int i = 0;i<this.pages.length;i++){
                if (this.pages[i]==null){
                    this.pages[i]= (HeapPage) page;
                    this.rest_num = this.rest_num-1;
                    page_q.put(pages[i].getId(),1);
                    return;
                }
            }
        }
        else{
            try {
                evictPage();
            }catch (Exception e){
                e.printStackTrace();
            }
            addPage(tid,page);
        }

    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void unsafeReleasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        return false;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit) {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other 
     * pages that are updated (Lock acquisition is not needed for lab2). 
     * May block if the lock(s) cannot be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
//        boolean sign =false;
//        int i = 0;
//        while (!sign){
//            HeapPageId heapPageId = new HeapPageId(tableId,i);
//            HeapPage heapPage = (HeapPage) this.getPage(tid,heapPageId,Permissions.READ_WRITE);
//            try{
//                heapPage.insertTuple(t);
//                heapPage.markDirty(true,tid);
//            }catch (Exception e){
//                i = i+1;
//                continue;
//            }
//            sign = true;
//        }
        DbFile hdf = Database.getCatalog().getDatabaseFile(tableId);
        List<Page> dtp =  hdf.insertTuple(tid,t);
        for (int i =0;i<dtp.size();i++){
            this.addPage(tid,dtp.get(i));
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
//        HeapPageId heapPageId = (HeapPageId) t.getRecordId().getPageId();
//        HeapPage hp = (HeapPage) this.getPage(tid,heapPageId,Permissions.READ_WRITE);
//        hp.deleteTuple(t);
//        hp.markDirty(true,tid);
        int tableid = t.getRecordId().getPageId().getTableId();
        DbFile hdf = Database.getCatalog().getDatabaseFile(tableid);
        List<Page> dtp =  hdf.deleteTuple(tid,t);
        for (int i =0;i<dtp.size();i++){
            this.addPage(tid,dtp.get(i));
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for (int i =0;i<pages.length;i++){
            if (pages[i]!=null){
                if (pages[i].isDirty()!=null){
                    Database.getCatalog().getDatabaseFile(pages[i].getId().getTableId()).writePage(pages[i]);
                    pages[i].markDirty(false,null);
                }
            }
        }
    }

    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
        
        Also used by B+ tree files to ensure that deleted pages
        are removed from the cache so they can be reused safely
    */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        int visit_rest = DEFAULT_PAGES - rest_num;
        for (int i = 0;i<pages.length;i++){
            if (pages[i]!=null){
                if (pages[i].getId().equals(pid) && pages[i].isDirty()!=null){
                    Database.getCatalog().getDatabaseFile(pages[i].getId().getTableId()).writePage(pages[i]);
                    pages[i].markDirty(false,null);
                }
            }
            if (visit_rest == 0)break;
        }

    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1
        HeapPageId key = null;
        for (Map.Entry<HeapPageId, Integer> mapElement : page_q.entrySet()) {
            // 获取键
            key = mapElement.getKey();
            page_q.remove(key);
            break;
        }
        for (int i = 0;i<pages.length;i++){
            if (pages[i].getId().equals(key)){
                if (pages[i].isDirty() != null){
                    try {
                        flushPage(key);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                pages[i] = null;
                rest_num = rest_num + 1;
            }
        }
    }

}
