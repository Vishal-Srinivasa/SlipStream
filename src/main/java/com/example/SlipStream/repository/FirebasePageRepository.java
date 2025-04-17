package com.example.SlipStream.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.Collections;

import org.springframework.stereotype.Repository;

import com.example.SlipStream.model.ContainerPage;
import com.example.SlipStream.model.ContentPage;
import com.example.SlipStream.model.PageComponent;
import com.google.cloud.firestore.Query;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.WriteResult;

@Repository
public class FirebasePageRepository implements PageRepository {

    private static final String COLLECTION_NAME = "Pages";

    @Override
    public String createPage(PageComponent page) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        
        // Generate a unique ID if not provided
        if (page.getPageId() == null || page.getPageId().isEmpty()) {
            page.setPageId("page_" + UUID.randomUUID().toString());
        }
        
        // Set created and updated timestamps if not already set
        if (page.getCreatedAt() == null) {
            page.setCreatedAt(new Date());
        }
        page.setLastUpdated(new Date());
        
        // Convert PageComponent to Map for Firestore
        Map<String, Object> pageMap = convertToMap(page);
        
        // Save the new page
        firestore.collection(COLLECTION_NAME).document(page.getPageId()).set(pageMap).get();
        return page.getPageId();
    }

    @Override
    public PageComponent getPage(String pageId) throws ExecutionException, InterruptedException {
        System.out.println("Looking for page with pageId: " + pageId);
        
        Firestore firestore = FirestoreClient.getFirestore();
        
        // Query the page directly by ID
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(pageId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        
        if (document.exists()) {
            return convertToPageComponent(document);
        }
        
        // If not found by direct ID, try querying by pageId field
        Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("pageId", pageId);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
        System.out.println("Query complete. Found " + documents.size() + " matching documents");
        
        if (!documents.isEmpty()) {
            return convertToPageComponent(documents.get(0));
        }
        
        System.out.println("No document found with pageId: " + pageId);
        return null;
    }

    @Override
    public List<PageComponent> getAllPages() throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        List<PageComponent> pages = new ArrayList<>();
        
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        for (QueryDocumentSnapshot document : documents) {
            pages.add(convertToPageComponent(document));
        }
        
        return pages;
    }

    @Override
    public List<PageComponent> getChildPages(String parentPageId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        List<PageComponent> childPages = new ArrayList<>();
        
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
            .whereEqualTo("parentPageId", parentPageId)
            .get();
            
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        for (QueryDocumentSnapshot document : documents) {
            childPages.add(convertToPageComponent(document));
        }
        
        return childPages;
    }

    @Override
    public boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        
        // Try to get the document directly
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(pageId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        
        if (document.exists()) {
            // Update content and last updated timestamp
            docRef.update("content", newContent, "lastUpdated", new Date()).get();
            return true;
        }
        
        // If not found, try with the query approach
        Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("pageId", pageId);
        QuerySnapshot querySnapshot = query.get().get();
        
        if (querySnapshot.isEmpty()) {
            return false;
        }
        
        // Get the first document matching the query
        docRef = querySnapshot.getDocuments().get(0).getReference();
        
        // Update content and last updated timestamp
        docRef.update("content", newContent, "lastUpdated", new Date()).get();
        return true;
    }

    @Override
    public boolean updatePage(PageComponent page) throws ExecutionException, InterruptedException {
        if (page == null || page.getPageId() == null || page.getPageId().isEmpty()) {
            return false;
        }
        
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(page.getPageId());
        
        // Check if document exists
        DocumentSnapshot document = docRef.get().get();
        if (!document.exists()) {
            return false;
        }
        
        // Convert to Map and update the entire page document
        Map<String, Object> pageMap = convertToMap(page);
        ApiFuture<WriteResult> result = docRef.set(pageMap);
        result.get(); // Wait for operation to complete
        
        return true;
    }

    @Override
    public boolean deletePage(String pageId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        
        // Get the page first
        PageComponent page = getPage(pageId);
        if (page == null) {
            return false;
        }
        
        // If this page has a parent, remove this page from parent's children list
        if (page.getParentPageId() != null && !page.getParentPageId().isEmpty()) {
            PageComponent parentPage = getPage(page.getParentPageId());
            if (parentPage instanceof ContainerPage) {
                ContainerPage container = (ContainerPage) parentPage;
                List<String> childrenIds = container.getChildrenIds();
                if (childrenIds != null) {
                    childrenIds.remove(pageId);
                    updatePage(container);
                }
            }
        }
        
        // Delete any child pages recursively
        if (page instanceof ContainerPage) {
            ContainerPage container = (ContainerPage) page;
            List<String> childrenIds = container.getChildrenIds();
            if (childrenIds != null) {
                for (String childId : new ArrayList<>(childrenIds)) {  // Use a copy to avoid ConcurrentModificationException
                    deletePage(childId);
                }
            }
        }
        
        // Delete the page
        firestore.collection(COLLECTION_NAME).document(pageId).delete().get();
        return true;
    }
    
    // Helper methods for conversion
    private Map<String, Object> convertToMap(PageComponent page) {
        Map<String, Object> map = new HashMap<>();
        map.put("pageId", page.getPageId());
        map.put("title", page.getTitle());
        map.put("parentPageId", page.getParentPageId());
        map.put("owner", page.getOwner());
        map.put("createdAt", page.getCreatedAt());
        map.put("lastUpdated", page.getLastUpdated());
        map.put("isLeaf", page.isLeaf());
        map.put("isPublished", page.isPublished());
        map.put("sharingInfo", page.getSharingInfo() != null ? page.getSharingInfo() : new HashMap<>());

        // Content depends on page type
        map.put("content", page.getContent());
        
        // For ContainerPage, we only store the IDs of children, not the entire objects
        if (!page.isLeaf() && page instanceof ContainerPage) {
            List<String> childrenIds = ((ContainerPage) page).getChildrenIds();
            map.put("childrenIds", childrenIds != null ? childrenIds : new ArrayList<String>());
        } else {
            map.put("childrenIds", new ArrayList<String>());
        }
        
        return map;
    }
    
    private PageComponent convertToPageComponent(DocumentSnapshot document) {
        Map<String, Object> data = document.getData();
        if (data == null) return null;
        
        Boolean isLeaf = (Boolean) data.get("isLeaf");
        PageComponent component;
        
        if (isLeaf != null && isLeaf) {
            // Create ContentPage
            component = new ContentPage();
            String content = (String) data.get("content");
            ((ContentPage) component).setContent(content);
        } else {
            // Create ContainerPage
            component = new ContainerPage();
            String summary = (String) data.get("content");
            ((ContainerPage) component).setSummary(summary);
            
            // Set childrenIds from the database
            List<String> childrenIds = (List<String>) data.get("childrenIds");
            if (childrenIds != null) {
                ((ContainerPage) component).setChildrenIds(childrenIds);
            }
        }
        
        // Set common properties
        component.setPageId((String) data.get("pageId"));
        component.setTitle((String) data.get("title"));
        component.setOwner((String) data.get("owner"));
        component.setParentPageId((String) data.get("parentPageId"));
        component.setPublished(data.get("isPublished") != null ? (Boolean) data.get("isPublished") : false);
        
        // Handle sharingInfo
        Map<String, String> sharingInfo = (Map<String, String>) data.get("sharingInfo");
        component.setSharingInfo(sharingInfo != null ? sharingInfo : new HashMap<>());
        
        // Handle dates
        if (data.get("createdAt") != null) {
            component.setCreatedAt(((com.google.cloud.Timestamp) data.get("createdAt")).toDate());
        }
        if (data.get("lastUpdated") != null) {
            component.setLastUpdated(((com.google.cloud.Timestamp) data.get("lastUpdated")).toDate());
        }
        
        return component;
    }
}