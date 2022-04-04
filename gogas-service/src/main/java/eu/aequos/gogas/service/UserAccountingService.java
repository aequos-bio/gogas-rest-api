package eu.aequos.gogas.service;

import eu.aequos.gogas.persistence.entity.AccountingEntry;
import eu.aequos.gogas.persistence.entity.AccountingEntryReason;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.AccountingReasonRepo;
import eu.aequos.gogas.persistence.repository.AccountingRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.persistence.utils.UserTransactionFull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

//TODO: check if we can merge with other services
@RequiredArgsConstructor
@Service
public class UserAccountingService {

    private final AccountingRepo userTransactionRepo;
    private final AccountingReasonRepo reasonRepo;
    private final UserRepo userRepo;

    public List<UserTransactionFull> getUserTransactions(String userId) {
        List<AccountingEntryReason> reasons = reasonRepo.findAllByOrderByDescription();
        Map<String, AccountingEntryReason> reasonDict = new HashMap<>();
        for (AccountingEntryReason r : reasons)
            reasonDict.put(r.getReasonCode(), r);

            List<AccountingEntry> transactions = userTransactionRepo.getUserTransactions(userId, userId);
            List<UserTransactionFull> result = new ArrayList<>();
            for (AccountingEntry t : transactions) {
                if (t.isConfirmed()) {
                    UserTransactionFull utf = new UserTransactionFull(t, reasonDict.get(t.getReason().getReasonCode()));
                    if (!utf.getUserId().equals(userId)) {
                        Optional<User> friend = userRepo.findById(utf.getUserId());
                        utf.setFriend(friend.get().getUsername());
                    }
                    result.add(utf);
            }
        }
        return result;
    }
}
