package eu.aequos.gogas.security;

import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.order.GoGasOrderFactory;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.service.OrderItemService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    private UserRepo userRepo;
    private OrderManagerRepo orderManagerRepo;
    private OrderItemService orderItemService;
    private GoGasOrderFactory orderFactory;

    public AuthorizationService(UserRepo userRepo, OrderManagerRepo orderManagerRepo,
                                OrderItemService orderItemService, GoGasOrderFactory orderFactory) {

        this.userRepo = userRepo;
        this.orderManagerRepo = orderManagerRepo;
        this.orderItemService = orderItemService;
        this.orderFactory = orderFactory;
    }

    @Override
    public GoGasUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepo.findByUsername(username)
                .map(u -> new GoGasUserDetails(u, !orderManagerRepo.findByUser(u.getId()).isEmpty()))
                .orElseThrow(() -> new UsernameNotFoundException(username));
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
        String orderTypeId = orderFactory.initOrder(orderId).getOrderTypeId();
        return isOrderTypeManager(orderTypeId);
    }

    public boolean isOrderTypeManager(String orderTypeId) {
        if (orderTypeId == null)
            return false;

        String currentUserId = getCurrentUser().getId();
        return !orderManagerRepo.findByUserAndOrderType(currentUserId, orderTypeId).isEmpty();
    }

    public boolean isOrderItemOwner(String orderItem) {
        if (orderItem == null)
            return false;

        String currentUserId = getCurrentUser().getId();
        return orderItemService.isOrderItemBelongingToUserOrFriend(orderItem, currentUserId);
    }
}
