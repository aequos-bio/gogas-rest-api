package eu.aequos.gogas.security;

import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    private UserRepo userRepo;
    private OrderManagerRepo orderManagerRepo;
    private OrderRepo orderRepo;

    public AuthorizationService(UserRepo userRepo, OrderManagerRepo orderManagerRepo,
                                OrderRepo orderRepo) {

        this.userRepo = userRepo;
        this.orderManagerRepo = orderManagerRepo;
        this.orderRepo = orderRepo;
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

    public boolean isOrderManager(String orderId) {
        String orderTypeId = orderRepo.findByIdWithType(orderId).map(o -> o.getOrderType().getId()).get();
        return isOrderTypeManager(orderTypeId);
    }

    public boolean isOrderTypeManager(String orderTypeId) {
        if (orderTypeId == null)
            return false;

        String currentUserId = getCurrentUser().getId();
        return !orderManagerRepo.findByUserAndOrderType(currentUserId, orderTypeId).isEmpty();
    }
}
