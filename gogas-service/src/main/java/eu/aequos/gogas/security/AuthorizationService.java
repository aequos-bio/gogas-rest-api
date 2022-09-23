package eu.aequos.gogas.security;

import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService implements UserDetailsService {

    private final UserRepo userRepo;
    private final OrderManagerRepo orderManagerRepo;
    private final OrderRepo orderRepo;
    private final OrderItemService orderItemService;
    private final ProductRepo productRepo;

    @Override
    public GoGasUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepo.findByUsername(username)
                .map(u -> new GoGasUserDetails(u, !orderManagerRepo.findByUser(u.getId()).isEmpty()))
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    public GoGasUserDetails loadUserById(String userId) throws UsernameNotFoundException {
        return userRepo.findById(userId)
                .map(u -> new GoGasUserDetails(u, !orderManagerRepo.findByUser(u.getId()).isEmpty()))
                .orElseThrow(() -> new UsernameNotFoundException(userId));
    }

    //TODO: retrieve info always from db???
    public GoGasUserDetails getCurrentUser() {
        return (GoGasUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public boolean isCurrentUser(String userId) {
        String currentUserId = getCurrentUser().getId();
        return userId != null && currentUserId != null && currentUserId.equalsIgnoreCase(userId);
    }

    public boolean isUserOrFriend(String userId) {
        String currentUserId = getCurrentUser().getId();
        return userId.equalsIgnoreCase(currentUserId) ||
                userRepo.existsUserByIdAndFriendReferralId(userId, currentUserId);
    }

    public boolean isFriend(String userId) {
        String currentUserId = getCurrentUser().getId();
        return userRepo.existsUserByIdAndFriendReferralId(userId, currentUserId);
    }

    public boolean isOrderManager(String orderId) throws ItemNotFoundException {
        Order order = orderRepo.findByIdWithType(orderId)
                .orElseThrow(() -> new ItemNotFoundException("order", orderId));

        return isOrderTypeManager(order.getOrderType().getId());
    }

    public boolean isOrderTypeManager(String orderTypeId) {
        if (orderTypeId == null)
            return false;

        try {
            String currentUserId = getCurrentUser().getId();
            return !orderManagerRepo.findByUserAndOrderType(currentUserId, orderTypeId).isEmpty();
        } catch (Exception ex) {
            log.error("Error while checking order manager permission", ex);
            return false;
        }
    }

    public boolean isProductManager(String productId, ProductDTO product) {
        Optional<String> orderTypeId = extractOrderTypeId(productId, product);

        try {
            return orderTypeId
                    .map(type -> !orderManagerRepo.findByUserAndOrderType(getCurrentUser().getId(), type).isEmpty())
                    .orElse(false);

        } catch (Exception ex) {
            log.error("Error while checking order manager permission", ex);
            return false;
        }
    }

    private Optional<String> extractOrderTypeId(String productId, ProductDTO product) {
        if (productId != null) {
            return productRepo.findById(productId)
                    .map(Product::getType);
        }

        return Optional.ofNullable(product)
                .map(ProductDTO::getTypeId);
    }

    public boolean isOrderItemOwner(String orderItem) {
        if (orderItem == null)
            return false;

        String currentUserId = getCurrentUser().getId();
        return orderItemService.isOrderItemBelongingToUserOrFriend(orderItem, currentUserId);
    }
}
