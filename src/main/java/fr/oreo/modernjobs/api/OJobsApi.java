package fr.oreo.modernjobs.api;

import fr.oreo.modernjobs.models.Job;
import fr.oreo.modernjobs.models.LeaderboardEntry;
import fr.oreo.modernjobs.models.PlayerData;
import fr.oreo.modernjobs.models.PlayerJobData;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface OJobsApi {

    Collection<Job> getJobs();

    Optional<Job> getJob(String jobId);

    Optional<PlayerData> getPlayerData(UUID playerUuid);

    Optional<PlayerJobData> getPlayerJobData(UUID playerUuid, String jobId);

    boolean hasJob(Player player, String jobId);

    boolean joinJob(Player player, String jobId);

    boolean leaveJob(Player player, String jobId);

    void giveXp(Player player, String jobId, double amount);

    void setLevel(UUID playerUuid, String jobId, int level);

    boolean canPrestige(Player player, String jobId);

    boolean prestige(Player player, String jobId);

    List<LeaderboardEntry> getLeaderboard(String jobId);

    List<LeaderboardEntry> getGlobalLeaderboard();

    void refreshLeaderboards();

    void openMainMenu(Player player);

    void openJobMenu(Player player, String jobId);

    void openLeaderboard(Player player);
}
