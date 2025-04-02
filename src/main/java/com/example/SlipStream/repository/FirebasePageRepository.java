package com.example.SlipStream.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import com.google.cloud.Timestamp;

import org.springframework.stereotype.Repository;

import com.example.SlipStream.model.Page;
import com.google.cloud.firestore.Query;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

@Repository
public class FirebasePageRepository implements PageRepository {

    private static final String COLLECTION_NAME = "pages";

    @Override
public String createPage(Page page) throws ExecutionException, InterruptedException {
    Firestore firestore = FirestoreClient.getFirestore();
    
    // Generate a unique ID if not provided
    if (page.getPageId() == null || page.getPageId().isEmpty()) {
        page.setPageId("page_" + UUID.randomUUID().toString());
    }
    
    // Set created and updated timestamps
    
        Date now = new Date();
        page.setCreatedAt(now);
        page.setLastUpdated(now);
        page.setLastUpdated(now);
    
    // Initialize children list if null
    if (page.getChildren() == null) {
        page.setChildren(new ArrayList<>());
    }
    
    // Update parent page if there is one
    if (page.getParentPage() != null && !page.getParentPage().isEmpty() && !page.getParentPage().equals("0")) {
        DocumentReference parentRef = firestore.collection("Pages").document(page.getParentPage());
        ApiFuture<DocumentSnapshot> future = parentRef.get();
        DocumentSnapshot document = future.get();
        
        if (document.exists()) {
            Page parentPage = document.toObject(Page.class);
            if (parentPage != null) {
                // Initialize children list if null
                if (parentPage.getChildren() == null) {
                    parentPage.setChildren(new ArrayList<>());
                }
                parentPage.getChildren().add(page.getPageId());
                parentRef.set(parentPage).get();
            }
        }
    }
    
    // Save the new page
    firestore.collection("Pages").document(page.getPageId()).set(page).get();
    return page.getPageId();
}




    @Override
public Page getPage(String pageId) throws ExecutionException, InterruptedException {
    System.out.println("Looking for page with pageId: " + pageId);
    
    Firestore firestore = FirestoreClient.getFirestore();
    
    // Keep the original query using whereEqualTo
    Query query = firestore.collection("Pages").whereEqualTo("pageId", pageId);
    ApiFuture<QuerySnapshot> querySnapshot = query.get();
    
    List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
    System.out.println("Query complete. Found " + documents.size() + " matching documents");
    
    if (!documents.isEmpty()) {
        QueryDocumentSnapshot document = documents.get(0); // ✅ Get first document
        
        // Extract title manually before converting to object
        String title = document.getString("title");  
        
        
        // Convert to Page object
        Page page = document.toObject(Page.class);
        System.out.println("Page Title Retrieved: " + page.getTitle());
        System.out.println("Title Retrieved Directly: " + title);
        System.out.println("Retrieved page: " + page);
        
        return page;
    }
    
    System.out.println("No document found with pageId: " + pageId);
    return null;
}

    

    @Override
    public List<Page> getAllPages() throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        List<Page> pages = new ArrayList<>();
        
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        for (QueryDocumentSnapshot document : documents) {
            pages.add(document.toObject(Page.class));
        }
        
        return pages;
    }

    @Override
    public List<Page> getChildPages(String parentPageId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        List<Page> childPages = new ArrayList<>();
        
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
            .whereEqualTo("parentPage", parentPageId)
            .get();
            
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        for (QueryDocumentSnapshot document : documents) {
            childPages.add(document.toObject(Page.class));
        }
        
        return childPages;
    }

    @Override
public boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException {
    Firestore firestore = FirestoreClient.getFirestore();
    
    // Use collection reference with a query
    Query query = firestore.collection("Pages").whereEqualTo("pageId", pageId);
    
    System.out.println("hello");
    // Execute query
    QuerySnapshot querySnapshot = query.get().get();
    
    if (querySnapshot.isEmpty()) {
        return false;
    }
    
    // Get the first document matching the query
    DocumentSnapshot document = querySnapshot.getDocuments().get(0);
    DocumentReference docRef = document.getReference();
    
    // Update content and last updated timestamp
    docRef.update("content", newContent, "lastUpdated", new Date()).get();
    return true;
}

    @Override
    public boolean deletePage(String pageId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        
        // Get the page first
        Page page = getPage(pageId);
        if (page == null) {
            return false;
        }
        
        // If this page has a parent, remove this page from parent's children list
        if (page.getParentPage() != null && !page.getParentPage().isEmpty()) {
            Page parentPage = getPage(page.getParentPage());
            if (parentPage != null && parentPage.getChildren() != null) {
                parentPage.getChildren().remove(pageId);
                firestore.collection(COLLECTION_NAME).document(parentPage.getPageId()).set(parentPage).get();
            }
        }
        
        // Delete the page
        firestore.collection(COLLECTION_NAME).document(pageId).delete().get();
        return true;
    }
}