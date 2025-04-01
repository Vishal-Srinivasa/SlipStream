package com.example.SlipStream.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Repository;

import com.example.SlipStream.model.Page;
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
        
        // Update parent page if there is one
        if (page.getParentPage() != null && !page.getParentPage().isEmpty()) {
            Page parentPage = getPage(page.getParentPage());
            if (parentPage != null) {
                parentPage.addChild(page.getPageId());
                firestore.collection(COLLECTION_NAME).document(parentPage.getPageId()).set(parentPage).get();
            }
        }
        
        // Save the new page
        firestore.collection(COLLECTION_NAME).document(page.getPageId()).set(page).get();
        return page.getPageId();
    }

    @Override
    public Page getPage(String pageId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        DocumentSnapshot document = firestore.collection(COLLECTION_NAME).document(pageId).get().get();
        
        if (document.exists()) {
            return document.toObject(Page.class);
        }
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
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(pageId);
        
        DocumentSnapshot document = docRef.get().get();
        if (!document.exists()) {
            return false;
        }
        
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