package eu.aequos.gogas.persistence.entity.derived;

public interface ByUserBlacklistCount {
    String getUserId();
    int getBlacklistEntriesCount();
}
