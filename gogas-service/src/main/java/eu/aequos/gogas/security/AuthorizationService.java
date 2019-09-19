package eu.aequos.gogas.security;

import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.service.OrderManagerService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    private UserRepo userRepo;
    private OrderManagerRepo orderManagerRepo;
    private OrderManagerService orderManagerService;

    public AuthorizationService(UserRepo userRepo, OrderManagerRepo orderManagerRepo,
                                OrderManagerService orderManagerService) {

        this.userRepo = userRepo;
        this.orderManagerRepo = orderManagerRepo;
        this.orderManagerService = orderManagerService;
    }

    @Override
    public GoGasUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepo.findByUsername(username)
                .map(u -> new GoGasUserDetails(u, !orderManagerRepo.findByUser(u.getId()).isEmpty()))
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    //TODO:  retrieve info always from db???
    public GoGasUserDetails getCurrentUser() {
        return (GoGasUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public boolean isUserOrFriend(String userId) {
        String currentUserId = getCurrentUser().getId();
        return userId.equalsIgnoreCase(currentUserId) ||
                userRepo.existsUserByIdAndFriendReferralId(userId, currentUserId);
    }

    public boolean isOrderManager(String orderId) throws ItemNotFoundException {
        String orderTypeId = orderManagerService.getRequiredWithType(orderId).getOrderType().getId();
        return isOrderTypeManager(orderTypeId);
    }

    public boolean isOrderTypeManager(String orderTypeId) {
        if (orderTypeId == null)
            return false;

        String currentUserId = getCurrentUser().getId();
        return !orderManagerRepo.findByUserAndOrderType(currentUserId, orderTypeId).isEmpty();
    }
}
