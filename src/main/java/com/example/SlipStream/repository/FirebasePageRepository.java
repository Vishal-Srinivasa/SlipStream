// package com.example.SlipStream.repository;

// import java.util.ArrayList;
// import java.util.Date;
// import java.util.List;
// import java.util.UUID;
// import java.util.concurrent.ExecutionException;
// import com.google.cloud.Timestamp;

// import org.springframework.stereotype.Repository;

// import com.example.SlipStream.model.Page;
// import com.google.cloud.firestore.Query;
// import com.google.api.core.ApiFuture;
// import com.google.cloud.firestore.DocumentReference;
// import com.google.cloud.firestore.DocumentSnapshot;
// import com.google.cloud.firestore.Firestore;
// import com.google.cloud.firestore.QueryDocumentSnapshot;
// import com.google.cloud.firestore.QuerySnapshot;
// import com.google.firebase.cloud.FirestoreClient;

// @Repository
// public class FirebasePageRepository implements PageRepository {

//     private static final String COLLECTION_NAME = "pages";

//     @Override
// public String createPage(Page page) throws ExecutionException, InterruptedException {
//     Firestore firestore = FirestoreClient.getFirestore();
    
//     // Generate a unique ID if not provided
//     if (page.getPageId() == null || page.getPageId().isEmpty()) {
//         page.setPageId("page_" + UUID.randomUUID().toString());
//     }
    
//     // Set created and updated timestamps
    
//         Date now = new Date();
//         page.setCreatedAt(now);
//         page.setLastUpdated(now);
//         page.setLastUpdated(now);
    
//     // Initialize children list if null
//     if (page.getChildren() == null) {
//         page.setChildren(new ArrayList<>());
//     }
    
//     // Update parent page if there is one
//     if (page.getParentPage() != null && !page.getParentPage().isEmpty() && !page.getParentPage().equals("0")) {
//         DocumentReference parentRef = firestore.collection("Pages").document(page.getParentPage());
//         ApiFuture<DocumentSnapshot> future = parentRef.get();
//         DocumentSnapshot document = future.get();
        
//         if (document.exists()) {
//             Page parentPage = document.toObject(Page.class);
//             if (parentPage != null) {
//                 // Initialize children list if null
//                 if (parentPage.getChildren() == null) {
//                     parentPage.setChildren(new ArrayList<>());
//                 }
//                 parentPage.getChildren().add(page.getPageId());
//                 parentRef.set(parentPage).get();
//             }
//         }
//     }
    
//     // Save the new page
//     firestore.collection("Pages").document(page.getPageId()).set(page).get();
//     return page.getPageId();
// }




//     @Override
// public Page getPage(String pageId) throws ExecutionException, InterruptedException {
//     System.out.println("Looking for page with pageId: " + pageId);
    
//     Firestore firestore = FirestoreClient.getFirestore();
    
//     // Keep the original query using whereEqualTo
//     Query query = firestore.collection("Pages").whereEqualTo("pageId", pageId);
//     ApiFuture<QuerySnapshot> querySnapshot = query.get();
    
//     List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
//     System.out.println("Query complete. Found " + documents.size() + " matching documents");
    
//     if (!documents.isEmpty()) {
//         QueryDocumentSnapshot document = documents.get(0); // ✅ Get first document
        
//         // Extract title manually before converting to object
//         String title = document.getString("title");  
        
        
//         // Convert to Page object
//         Page page = document.toObject(Page.class);
//         System.out.println("Page Title Retrieved: " + page.getTitle());
//         System.out.println("Title Retrieved Directly: " + title);
//         System.out.println("Retrieved page: " + page);
        
//         return page;
//     }
    
//     System.out.println("No document found with pageId: " + pageId);
//     return null;
// }

    

//     @Override
//     public List<Page> getAllPages() throws ExecutionException, InterruptedException {
//         Firestore firestore = FirestoreClient.getFirestore();
//         List<Page> pages = new ArrayList<>();
        
//         ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
//         List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
//         for (QueryDocumentSnapshot document : documents) {
//             pages.add(document.toObject(Page.class));
//         }
        
//         return pages;
//     }

//     @Override
//     public List<Page> getChildPages(String parentPageId) throws ExecutionException, InterruptedException {
//         Firestore firestore = FirestoreClient.getFirestore();
//         List<Page> childPages = new ArrayList<>();
        
//         ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
//             .whereEqualTo("parentPage", parentPageId)
//             .get();
            
//         List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
//         for (QueryDocumentSnapshot document : documents) {
//             childPages.add(document.toObject(Page.class));
//         }
        
//         return childPages;
//     }

//     @Override
// public boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException {
//     Firestore firestore = FirestoreClient.getFirestore();
    
//     // Use collection reference with a query
//     Query query = firestore.collection("Pages").whereEqualTo("pageId", pageId);
    
//     System.out.println("hello");
//     // Execute query
//     QuerySnapshot querySnapshot = query.get().get();
    
//     if (querySnapshot.isEmpty()) {
//         return false;
//     }
    
//     // Get the first document matching the query
//     DocumentSnapshot document = querySnapshot.getDocuments().get(0);
//     DocumentReference docRef = document.getReference();
    
//     // Update content and last updated timestamp
//     docRef.update("content", newContent, "lastUpdated", new Date()).get();
//     return true;
// }

//     @Override
//     public boolean deletePage(String pageId) throws ExecutionException, InterruptedException {
//         Firestore firestore = FirestoreClient.getFirestore();
        
//         // Get the page first
//         Page page = getPage(pageId);
//         if (page == null) {
//             return false;
//         }
        
//         // If this page has a parent, remove this page from parent's children list
//         if (page.getParentPage() != null && !page.getParentPage().isEmpty()) {
//             Page parentPage = getPage(page.getParentPage());
//             if (parentPage != null && parentPage.getChildren() != null) {
//                 parentPage.getChildren().remove(pageId);
//                 firestore.collection(COLLECTION_NAME).document(parentPage.getPageId()).set(parentPage).get();
//             }
//         }
        
//         // Delete the page
//         firestore.collection(COLLECTION_NAME).document(pageId).delete().get();
//         return true;
//     }
// }




//update 1

// package com.example.SlipStream.repository;

// import java.util.ArrayList;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
// import java.util.concurrent.ExecutionException;

// import org.springframework.stereotype.Repository;

// import com.example.SlipStream.model.ContainerPage;
// import com.example.SlipStream.model.ContentPage;
// import com.example.SlipStream.model.PageComponent;
// import com.google.cloud.firestore.Query;
// import com.google.api.core.ApiFuture;
// import com.google.cloud.firestore.DocumentReference;
// import com.google.cloud.firestore.DocumentSnapshot;
// import com.google.cloud.firestore.Firestore;
// import com.google.cloud.firestore.QueryDocumentSnapshot;
// import com.google.cloud.firestore.QuerySnapshot;
// import com.google.firebase.cloud.FirestoreClient;
// import com.google.cloud.firestore.WriteResult;


// @Repository
// public class FirebasePageRepository implements PageRepository {

//     private static final String COLLECTION_NAME = "Pages";

//     @Override
//     public String createPage(PageComponent page) throws ExecutionException, InterruptedException {
//         Firestore firestore = FirestoreClient.getFirestore();
        
//         // Generate a unique ID if not provided
//         if (page.getPageId() == null || page.getPageId().isEmpty()) {
//             page.setPageId("page_" + UUID.randomUUID().toString());
//         }
        
//         // Set created and updated timestamps if not already set
//         if (page.getCreatedAt() == null) {
//             page.setCreatedAt(new Date());
//         }
//         page.setLastUpdated(new Date());
        
//         // Convert PageComponent to Map for Firestore
//         Map<String, Object> pageMap = convertToMap(page);
        
//         // Update parent page if there is one
//         if (page.getParentPageId() != null && !page.getParentPageId().isEmpty() && !page.getParentPageId().equals("0")) {
//             DocumentReference parentRef = firestore.collection(COLLECTION_NAME).document(page.getParentPageId());
//             ApiFuture<DocumentSnapshot> future = parentRef.get();
//             DocumentSnapshot document = future.get();
            
//             if (document.exists()) {
//                 Map<String, Object> parentData = document.getData();
//                 if (parentData != null) {
//                     // Initialize or update children list
//                     List<String> children = (List<String>) parentData.get("children");
//                     if (children == null) {
//                         children = new ArrayList<>();
//                     }
//                     if (!children.contains(page.getPageId())) {
//                         children.add(page.getPageId());
//                         parentRef.update("children", children).get();
//                     }
//                 }
//             }
//         }
        
//         // Save the new page
//         firestore.collection(COLLECTION_NAME).document(page.getPageId()).set(pageMap).get();
//         return page.getPageId();
//     }

//     @Override
//     public PageComponent getPage(String pageId) throws ExecutionException, InterruptedException {
//         System.out.println("Looking for page with pageId: " + pageId);
        
//         Firestore firestore = FirestoreClient.getFirestore();
        
//         // Query the page directly by ID
//         DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(pageId);
//         ApiFuture<DocumentSnapshot> future = docRef.get();
//         DocumentSnapshot document = future.get();
        
//         if (document.exists()) {
//             return convertToPageComponent(document);
//         }
        
//         // If not found by direct ID, try querying by pageId field
//         Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("pageId", pageId);
//         ApiFuture<QuerySnapshot> querySnapshot = query.get();
        
//         List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
//         System.out.println("Query complete. Found " + documents.size() + " matching documents");
        
//         if (!documents.isEmpty()) {
//             return convertToPageComponent(documents.get(0));
//         }
        
//         System.out.println("No document found with pageId: " + pageId);
//         return null;
//     }

//     @Override
//     public List<PageComponent> getAllPages() throws ExecutionException, InterruptedException {
//         Firestore firestore = FirestoreClient.getFirestore();
//         List<PageComponent> pages = new ArrayList<>();
        
//         ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
//         List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
//         for (QueryDocumentSnapshot document : documents) {
//             pages.add(convertToPageComponent(document));
//         }
        
//         return pages;
//     }

//     @Override
//     public List<PageComponent> getChildPages(String parentPageId) throws ExecutionException, InterruptedException {
//         Firestore firestore = FirestoreClient.getFirestore();
//         List<PageComponent> childPages = new ArrayList<>();
        
//         ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
//             .whereEqualTo("parentPageId", parentPageId)
//             .get();
            
//         List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
//         for (QueryDocumentSnapshot document : documents) {
//             childPages.add(convertToPageComponent(document));
//         }
        
//         return childPages;
//     }

//     @Override
//     public boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException {
//         Firestore firestore = FirestoreClient.getFirestore();
        
//         // Try to get the document directly
//         DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(pageId);
//         ApiFuture<DocumentSnapshot> future = docRef.get();
//         DocumentSnapshot document = future.get();
        
//         if (document.exists()) {
//             // Update content and last updated timestamp
//             docRef.update("content", newContent, "lastUpdated", new Date()).get();
//             return true;
//         }
        
//         // If not found, try with the query approach
//         Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("pageId", pageId);
//         QuerySnapshot querySnapshot = query.get().get();
        
//         if (querySnapshot.isEmpty()) {
//             return false;
//         }
        
//         // Get the first document matching the query
//         docRef = querySnapshot.getDocuments().get(0).getReference();
        
//         // Update content and last updated timestamp
//         docRef.update("content", newContent, "lastUpdated", new Date()).get();
//         return true;
//     }

//     @Override
//     public boolean updatePage(PageComponent page) throws ExecutionException, InterruptedException {
//         if (page == null || page.getPageId() == null || page.getPageId().isEmpty()) {
//             return false;
//         }
        
//         Firestore db = FirestoreClient.getFirestore();
//         DocumentReference docRef = db.collection(COLLECTION_NAME).document(page.getPageId());
        
//         // Check if document exists
//         DocumentSnapshot document = docRef.get().get();
//         if (!document.exists()) {
//             return false;
//         }
        
//         // Update the entire page document
//         ApiFuture<WriteResult> result = docRef.set(page);
//         result.get(); // Wait for operation to complete
        
//         return true;
//     }

//     @Override
//     public boolean deletePage(String pageId) throws ExecutionException, InterruptedException {
//         Firestore firestore = FirestoreClient.getFirestore();
        
//         // Get the page first
//         PageComponent page = getPage(pageId);
//         if (page == null) {
//             return false;
//         }
        
//         // If this page has a parent, remove this page from parent's children list
//         if (page.getParentPageId() != null && !page.getParentPageId().isEmpty()) {
//             DocumentReference parentRef = firestore.collection(COLLECTION_NAME).document(page.getParentPageId());
//             ApiFuture<DocumentSnapshot> future = parentRef.get();
//             DocumentSnapshot document = future.get();
            
//             if (document.exists()) {
//                 Map<String, Object> parentData = document.getData();
//                 if (parentData != null) {
//                     List<String> children = (List<String>) parentData.get("children");
//                     if (children != null) {
//                         children.remove(pageId);
//                         parentRef.update("children", children).get();
//                     }
//                 }
//             }
//         }
        
//         // Delete any child pages recursively
//         List<PageComponent> children = getChildPages(pageId);
//         for (PageComponent child : children) {
//             deletePage(child.getPageId());
//         }
        
//         // Delete the page
//         firestore.collection(COLLECTION_NAME).document(pageId).delete().get();
//         return true;
//     }
    
//     // Helper methods for conversion
//     private Map<String, Object> convertToMap(PageComponent page) {
//         Map<String, Object> map = new HashMap<>();
//         map.put("pageId", page.getPageId());
//         map.put("title", page.getTitle());
//         map.put("content", page.getContent());
//         map.put("parentPageId", page.getParentPageId());
//         map.put("owner", page.getOwner());
//         map.put("createdAt", page.getCreatedAt());
//         map.put("lastUpdated", page.getLastUpdated());
//         map.put("isLeaf", page.isLeaf());
        
//         // For ContainerPage, we need to extract children IDs
//         if (!page.isLeaf()) {
//             List<String> childrenIds = new ArrayList<>();
//             for (PageComponent child : page.getChildren()) {
//                 childrenIds.add(child.getPageId());
//             }
//             map.put("children", childrenIds);
//         } else {
//             map.put("children", new ArrayList<String>());
//         }
        
//         return map;
//     }
    
//     private PageComponent convertToPageComponent(DocumentSnapshot document) {
//         Map<String, Object> data = document.getData();
//         if (data == null) return null;
        
//         Boolean isLeaf = (Boolean) data.get("isLeaf");
//         PageComponent component;
        
//         if (isLeaf != null && isLeaf) {
//             // Create ContentPage
//             component = new ContentPage();
//             String content = (String) data.get("content");
//             ((ContentPage) component).setContent(content);
//         } else {
//             // Create ContainerPage
//             component = new ContainerPage();
//             String summary = (String) data.get("content");
//             ((ContainerPage) component).setSummary(summary);
            
//             // Children IDs are stored but not loaded here to avoid deep recursion
//             // They will be loaded on demand
//         }
        
//         // Set common properties
//         component.setPageId((String) data.get("pageId"));
//         component.setTitle((String) data.get("title"));
//         component.setOwner((String) data.get("owner"));
//         component.setParentPageId((String) data.get("parentPageId"));
        
//         // Handle dates
//         if (data.get("createdAt") != null) {
//             component.setCreatedAt(((com.google.cloud.Timestamp) data.get("createdAt")).toDate());
//         }
//         if (data.get("lastUpdated") != null) {
//             component.setLastUpdated(((com.google.cloud.Timestamp) data.get("lastUpdated")).toDate());
//         }
        
//         return component;
//     }
// }












// update 2
package com.example.SlipStream.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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