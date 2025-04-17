package com.example.SlipStream.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.SlipStream.model.ContainerPage;
import com.example.SlipStream.model.ContentPage;
import com.example.SlipStream.model.PageComponent;
import com.example.SlipStream.repository.PageRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class PageService {

    private static final Logger logger = LoggerFactory.getLogger(PageService.class);
    private final PageRepository pageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public PageService(PageRepository pageRepository, SimpMessagingTemplate messagingTemplate) {
        this.pageRepository = pageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public String createContentPage(String title, String content, String parentPageId, String owner)
            throws ExecutionException, InterruptedException {
        String pageOwner = (owner != null && !owner.isEmpty()) ? owner : getCurrentUserEmail();
        if (pageOwner == null) {
            throw new IllegalStateException("Cannot create page without an authenticated owner.");
        }
        ContentPage page = new ContentPage(title, content, parentPageId, pageOwner);
        String pageId = pageRepository.createPage(page);
        logger.info("Created Content Page: ID={}, Title='{}', Owner={}", pageId, title, pageOwner);

        if (parentPageId != null && !parentPageId.isEmpty()) {
            updateParentChildRelationship(parentPageId, pageId);
        }

        return pageId;
    }

    public String createContainerPage(String title, String summary, String parentPageId, String owner)
            throws ExecutionException, InterruptedException {
        String pageOwner = (owner != null && !owner.isEmpty()) ? owner : getCurrentUserEmail();
        if (pageOwner == null) {
            throw new IllegalStateException("Cannot create page without an authenticated owner.");
        }
        ContainerPage page = new ContainerPage(title, summary, parentPageId, pageOwner);
        String pageId = pageRepository.createPage(page);
        logger.info("Created Container Page: ID={}, Title='{}', Owner={}", pageId, title, pageOwner);

        if (parentPageId != null && !parentPageId.isEmpty()) {
            updateParentChildRelationship(parentPageId, pageId);
        }

        return pageId;
    }

    public String createPage(PageComponent page) throws ExecutionException, InterruptedException {
        String pageId = pageRepository.createPage(page);

        String parentPageId = page.getParentPageId();
        if (parentPageId != null && !parentPageId.isEmpty()) {
            updateParentChildRelationship(parentPageId, pageId);
        }

        return pageId;
    }

    private void updateParentChildRelationship(String parentPageId, String childPageId)
            throws ExecutionException, InterruptedException {

        PageComponent parentPage = pageRepository.getPage(parentPageId);

        if (parentPage == null) {
            System.err.println("Warning: Parent page " + parentPageId + " not found when creating child " + childPageId);
            return;
        }

        if (parentPage.isLeaf()) {
            ContentPage contentParent = (ContentPage) parentPage;

            ContainerPage newContainerPage = new ContainerPage(
                    contentParent.getTitle(),
                    contentParent.getContent(),
                    contentParent.getParentPageId(),
                    contentParent.getOwner()
            );

            newContainerPage.setPageId(parentPageId);
            newContainerPage.setCreatedAt(parentPage.getCreatedAt());
            newContainerPage.setSharingInfo(parentPage.getSharingInfo());
            newContainerPage.setPublished(parentPage.isPublished());

            if (newContainerPage.getChildrenIds() == null) {
                newContainerPage.setChildrenIds(new ArrayList<>());
            }
            newContainerPage.getChildrenIds().add(childPageId);

            boolean updated = pageRepository.updatePage(newContainerPage);
            if (updated) {
                broadcastPageUpdate(parentPageId, newContainerPage);
            }
        } else if (parentPage instanceof ContainerPage) {
            ContainerPage containerParent = (ContainerPage) parentPage;

            if (containerParent.getChildrenIds() == null) {
                containerParent.setChildrenIds(new ArrayList<>());
            }

            if (!containerParent.getChildrenIds().contains(childPageId)) {
                containerParent.getChildrenIds().add(childPageId);
                containerParent.setLastUpdated(new Date());

                boolean updated = pageRepository.updatePage(containerParent);
                if (updated) {
                    broadcastPageUpdate(parentPageId, containerParent);
                }
            }
        }
    }

    public PageComponent getPage(String pageId) throws ExecutionException, InterruptedException {
        PageComponent page = pageRepository.getPage(pageId);
        if (page == null) {
            return null;
        }

        String currentUserEmail = getCurrentUserEmail();
        if (!hasAccess(page, currentUserEmail, "view")) {
            logger.warn("Access Denied: User '{}' attempted to view page '{}' (Published: {}, Owner: {}) without permission.",
                    currentUserEmail != null ? currentUserEmail : "anonymous",
                    pageId, page.isPublished(), page.getOwner());
            throw new AccessDeniedException("User " + (currentUserEmail != null ? currentUserEmail : "anonymous") + " does not have view access to page " + pageId);
        }
        logger.debug("Access Granted: User '{}' viewing page '{}'.", currentUserEmail != null ? currentUserEmail : "anonymous", pageId);
        return page;
    }

    public PageComponent getPageForEditing(String pageId) throws ExecutionException, InterruptedException {
        PageComponent page = pageRepository.getPage(pageId);
        if (page == null) {
            return null;
        }

        String currentUserEmail = getCurrentUserEmail();
        if (!hasAccess(page, currentUserEmail, "edit")) {
            logger.warn("Access Denied: User '{}' attempted to edit page '{}' (Owner: {}) without permission.",
                    currentUserEmail != null ? currentUserEmail : "anonymous",
                    pageId, page.getOwner());
            throw new AccessDeniedException("User " + (currentUserEmail != null ? currentUserEmail : "anonymous") + " does not have edit access to page " + pageId);
        }
        logger.debug("Edit Access Granted: User '{}' editing page '{}'.", currentUserEmail, pageId);
        return page;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getPrincipal() != null &&
                !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                return authentication.getName();
            }
        }
        logger.trace("No authenticated user found, returning null for current user email.");
        return null;
    }

    private boolean hasAccess(PageComponent page, String userEmail, String requiredAccessLevel) throws ExecutionException, InterruptedException {
        if (page == null) {
            logger.trace("hasAccess check failed: Page is null.");
            return false;
        }

        if (userEmail != null && userEmail.equals(page.getOwner())) {
            logger.trace("hasAccess check passed for page '{}': User '{}' is owner.", page.getPageId(), userEmail);
            return true;
        }

        if (page.isPublished() && "view".equals(requiredAccessLevel)) {
            logger.trace("hasAccess check passed for page '{}': Page is published and required access is 'view'.", page.getPageId());
            return true;
        }

        if (userEmail != null && page.getSharingInfo() != null) {
            String grantedAccess = page.getSharingInfo().get(userEmail);
            if (grantedAccess != null) {
                if ("edit".equals(grantedAccess)) {
                    logger.trace("hasAccess check passed for page '{}': User '{}' has 'edit' access via sharing.", page.getPageId(), userEmail);
                    return true;
                } else if ("view".equals(grantedAccess) && "view".equals(requiredAccessLevel)) {
                    logger.trace("hasAccess check passed for page '{}': User '{}' has 'view' access via sharing.", page.getPageId(), userEmail);
                    return true;
                }
            }
        }

        if (page.getParentPageId() != null && !page.getParentPageId().isEmpty()) {
            logger.trace("Checking inherited access for page '{}' via parent '{}'.", page.getPageId(), page.getParentPageId());
            try {
                PageComponent parentPage = pageRepository.getPage(page.getParentPageId());
                if (parentPage != null) {
                    return hasAccess(parentPage, userEmail, requiredAccessLevel);
                } else {
                    logger.warn("Parent page '{}' not found during inherited access check for page '{}'.", page.getParentPageId(), page.getPageId());
                }
            } catch (Exception e) {
                logger.error("Error checking parent page access for page '{}': {}", page.getPageId(), e.getMessage());
                return false;
            }
        }

        logger.trace("hasAccess check failed for page '{}': No applicable access rules matched for user '{}' requiring '{}'.", page.getPageId(), userEmail != null ? userEmail : "anonymous", requiredAccessLevel);
        return false;
    }

    public List<PageComponent> getAllPages() throws ExecutionException, InterruptedException {
        return pageRepository.getAllPages();
    }

    public List<PageComponent> getChildPages(String parentPageId) throws ExecutionException, InterruptedException {
        PageComponent parent = getPage(parentPageId);
        if (parent == null || parent.isLeaf()) {
            return new ArrayList<>();
        }

        ContainerPage containerParent = (ContainerPage) parent;
        List<String> childIds = containerParent.getChildrenIds();
        List<PageComponent> children = new ArrayList<>();

        for (String childId : childIds) {
            PageComponent child = pageRepository.getPage(childId);
            if (child != null) {
                children.add(child);
            }
        }

        containerParent.setLoadedChildren(children);

        return children;
    }

    public boolean updatePage(String pageId, String newTitle, String newContent) throws ExecutionException, InterruptedException {
        PageComponent page = getPageForEditing(pageId);
        if (page == null) {
            logger.warn("Attempted to update non-existent or inaccessible page: {}", pageId);
            return false;
        }

        boolean changed = false;

        if (newTitle != null && !newTitle.equals(page.getTitle())) {
            page.setTitle(newTitle);
            changed = true;
            logger.debug("Updating title for page {}: '{}'", pageId, newTitle);
        }

        String currentContent = page.getContent();
        if (newContent != null && !newContent.equals(currentContent)) {
             if (page.isLeaf() && page instanceof ContentPage) {
                 ((ContentPage) page).setContent(newContent);
                 changed = true;
                 logger.debug("Updating content for ContentPage {}", pageId);
             } else if (!page.isLeaf() && page instanceof ContainerPage) {
                 ((ContainerPage) page).setSummary(newContent);
                 changed = true;
                 logger.debug("Updating summary for ContainerPage {}", pageId);
             } else {
                 logger.warn("Attempted to update content/summary on unexpected page type for page {}", pageId);
             }
        }

        if (changed) {
            page.setLastUpdated(new Date());
            boolean success = pageRepository.updatePage(page);
            if (success) {
                logger.info("Successfully updated page {}", pageId);
                broadcastPageUpdate(pageId, page);
            } else {
                logger.error("Repository failed to update page {}", pageId);
            }
            return success;
        } else {
            logger.info("No changes detected for page {}, skipping update.", pageId);
            return true;
        }
    }

    public List<String> deletePage(String pageId) throws ExecutionException, InterruptedException {
        PageComponent pageToDelete;
        try {
            pageToDelete = getPageForEditing(pageId);
        } catch (AccessDeniedException e) {
            logger.warn("Access denied for deleting page {}: {}", pageId, e.getMessage());
            throw e;
        } catch (Exception e) {
             logger.error("Error fetching page {} for deletion check: {}", pageId, e.getMessage());
             return Collections.emptyList();
        }

        if (pageToDelete == null) {
            logger.warn("Attempted to delete non-existent or inaccessible page: {}", pageId);
            return Collections.emptyList();
        }

        List<String> deletedIds = new ArrayList<>();
        return deletePageRecursive(pageToDelete, deletedIds);
    }

    private List<String> deletePageRecursive(PageComponent pageToDelete, List<String> deletedIds) throws ExecutionException, InterruptedException {
        String pageId = pageToDelete.getPageId();
        String parentId = pageToDelete.getParentPageId();

        if (!pageToDelete.isLeaf() && pageToDelete instanceof ContainerPage) {
            ContainerPage containerPage = (ContainerPage) pageToDelete;
            List<String> childIds = containerPage.getChildrenIds();
            if (childIds != null && !childIds.isEmpty()) {
                List<String> childIdsCopy = new ArrayList<>(childIds);
                for (String childId : childIdsCopy) {
                    try {
                        PageComponent childPage = pageRepository.getPage(childId);
                        if (childPage != null) {
                            deletePageRecursive(childPage, deletedIds);
                        } else {
                             logger.warn("Child page {} not found during recursive delete of parent {}.", childId, pageId);
                        }
                    } catch (AccessDeniedException e) {
                        logger.warn("Access denied while trying to recursively delete child page {}. Skipping deletion of this child.", childId);
                    } catch (Exception e) {
                        logger.error("Error recursively deleting child page {}: {}", childId, e.getMessage());
                    }
                }
            }
        }

        if (parentId != null && !parentId.isEmpty()) {
            try {
                PageComponent parentPage = pageRepository.getPage(parentId);
                if (parentPage != null && !parentPage.isLeaf() && parentPage instanceof ContainerPage) {
                    ContainerPage containerParent = (ContainerPage) parentPage;
                    boolean removed = containerParent.getChildrenIds().remove(pageId);
                    if (removed) {
                        pageRepository.updatePage(containerParent);
                        logger.debug("Removed child {} from parent {}", pageId, parentId);
                    }
                }
            } catch (Exception e) {
                logger.error("Error updating parent page {} after deleting child {}: {}", parentId, pageId, e.getMessage());
            }
        }

        boolean deleted = pageRepository.deletePage(pageId);

        if (deleted) {
            logger.info("Successfully deleted page: ID={}, Title='{}'", pageId, pageToDelete.getTitle());
            deletedIds.add(pageId);

            if (parentId != null && !parentId.isEmpty()) {
                String destination = "/topic/pages/" + parentId + "/children/deleted";
                logger.info("Sending WebSocket message to {}: {}", destination, pageId);
                messagingTemplate.convertAndSend(destination, pageId);
            }

        } else {
             logger.error("Repository failed to delete page {}", pageId);
        }

        return deletedIds;
    }

    public boolean hasChildren(String pageId) throws ExecutionException, InterruptedException {
        List<PageComponent> children = getChildPages(pageId);
        return !children.isEmpty();
    }

    public boolean convertToContainerPage(String pageId) throws ExecutionException, InterruptedException {
        PageComponent page = getPage(pageId);

        if (page == null || !page.isLeaf()) {
            return false;
        }

        ContentPage contentPage = (ContentPage) page;

        ContainerPage containerPage = new ContainerPage(
                contentPage.getTitle(),
                contentPage.getContent(),
                contentPage.getParentPageId(),
                contentPage.getOwner()
        );
        containerPage.setPageId(pageId);

        return pageRepository.updatePage(containerPage);
    }

    public boolean sharePage(String pageId, String userEmailToShareWith, String accessLevel) throws ExecutionException, InterruptedException {
        if (!"view".equals(accessLevel) && !"edit".equals(accessLevel)) {
            throw new IllegalArgumentException("Invalid access level. Must be 'view' or 'edit'.");
        }

        PageComponent page = getPageForEditing(pageId);
        if (page == null) {
            return false;
        }

        page.addShare(userEmailToShareWith, accessLevel);
        page.setLastUpdated(new Date());
        boolean success = pageRepository.updatePage(page);
        if (success) {
            logger.info("Page {} shared with {} ({} access).", pageId, userEmailToShareWith, accessLevel);
            broadcastPageUpdate(pageId, page);
        }
        return success;
    }

    public boolean unsharePage(String pageId, String userEmailToUnshare) throws ExecutionException, InterruptedException {
        PageComponent page = getPageForEditing(pageId);
        if (page == null) {
            return false;
        }

        page.removeShare(userEmailToUnshare);
        page.setLastUpdated(new Date());
        boolean success = pageRepository.updatePage(page);
        if (success) {
            logger.info("Sharing removed for user {} from page {}.", userEmailToUnshare, pageId);
            broadcastPageUpdate(pageId, page);
        }
        return success;
    }

    public boolean publishPage(String pageId) throws ExecutionException, InterruptedException {
        PageComponent page = getPageForEditing(pageId);
        if (page == null) {
            return false;
        }

        page.setPublished(true);
        page.setLastUpdated(new Date());
        boolean success = pageRepository.updatePage(page);
        if (success) {
            logger.info("Page {} published successfully.", pageId);
            broadcastPageUpdate(pageId, page);
        }
        return success;
    }

    public boolean unpublishPage(String pageId) throws ExecutionException, InterruptedException {
        PageComponent page = getPageForEditing(pageId);
        if (page == null) {
            return false;
        }

        page.setPublished(false);
        page.setLastUpdated(new Date());
        boolean success = pageRepository.updatePage(page);
        if (success) {
            logger.info("Page {} unpublished successfully.", pageId);
            broadcastPageUpdate(pageId, page);
        }
        return success;
    }

    private void broadcastPageUpdate(String pageId, PageComponent page) {
        String destination = "/topic/pages/" + pageId;
        try {
            Map<String, Object> updatePayload = Map.of(
                "pageId", page.getPageId(),
                "title", page.getTitle(),
                "content", page.getContent(),
                "lastUpdated", page.getLastUpdated(),
                "isPublished", page.isPublished(),
                "sharingInfo", page.getSharingInfo()
            );
            logger.info("Broadcasting update for page {} to {}", pageId, destination);
            messagingTemplate.convertAndSend(destination, updatePayload);
        } catch (Exception e) {
            logger.error("Error broadcasting update for page {}: {}", pageId, e.getMessage(), e);
        }
    }
}