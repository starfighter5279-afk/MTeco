package com.mteco.managers;

import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.CouncilVote;
import com.mteco.data.CustomRole;
import com.mteco.data.NationData;
import com.mteco.data.NationPermission;
import com.mteco.data.RoleMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CouncilVoteManager {
    private final MTeco plugin;
    private final Map<UUID, CouncilVote> activeVotes = new ConcurrentHashMap<>();

    public CouncilVoteManager(MTeco plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkExpiredVotes, 20L, 20L);
    }

    public CouncilVote createVote(NationData nation, UUID initiatorUUID,
                                   NationPermission permission, String description,
                                   Runnable onPass, Runnable onDeny) {
        CouncilVote vote = new CouncilVote();
        vote.setVoteId(UUID.randomUUID());
        vote.setNationId(nation.getNationId());
        vote.setInitiatorUUID(initiatorUUID);
        vote.setPermission(permission);
        vote.setDescription(description);
        vote.setCreatedTimestamp(System.currentTimeMillis());
        long expirySec = plugin.getRolesConfig().getCouncilVoteExpirySeconds();
        vote.setExpiryTimestamp(System.currentTimeMillis() + (expirySec * 1000L));
        vote.setOnPass(onPass);
        vote.setOnDeny(onDeny);

        List<UUID> voters = new ArrayList<>();
        for (CustomRole role : nation.getCustomRoles()) {
            if (role.getRoleMode() == RoleMode.COUNCIL) {
                for (UUID member : role.getMemberUUIDs()) {
                    if (!voters.contains(member)) voters.add(member);
                }
            }
        }
        vote.setEligibleVoters(voters);
        activeVotes.put(vote.getVoteId(), vote);

        CharacterData c = plugin.getCharacterManager().getCharacter(initiatorUUID);
        String initiatorName = c != null ? c.getFirstName() + " " + c.getLastName() : "Unknown";
        for (UUID voterUUID : voters) {
            Player voter = Bukkit.getPlayer(voterUUID);
            if (voter != null && voter.isOnline()) {
                voter.sendMessage("");
                voter.sendMessage("\u00a76\u00a7l[Council Vote Required]");
                voter.sendMessage("\u00a77Proposed by: \u00a7e" + initiatorName);
                voter.sendMessage("\u00a77Action: \u00a7f" + description);
                voter.sendMessage("\u00a77Use \u00a7e/mtn votes \u00a77to review and cast your vote.");
                voter.sendMessage("");
            }
        }

        return vote;
    }

    public void castVote(UUID voteId, UUID voterUUID, boolean pass) {
        CouncilVote vote = activeVotes.get(voteId);
        if (vote == null) return;
        if (!vote.getEligibleVoters().contains(voterUUID)) return;
        vote.getVotes().put(voterUUID, pass);

        if (vote.getVotes().size() >= vote.getEligibleVoters().size()) {
            resolveVote(vote);
        }
    }

    public List<CouncilVote> getVotesForNation(UUID nationId) {
        List<CouncilVote> votes = new ArrayList<>();
        for (CouncilVote v : activeVotes.values()) {
            if (v.getNationId().equals(nationId)) votes.add(v);
        }
        return votes;
    }

    public List<CouncilVote> getVotesForVoter(UUID voterUUID) {
        List<CouncilVote> votes = new ArrayList<>();
        for (CouncilVote v : activeVotes.values()) {
            if (v.getEligibleVoters().contains(voterUUID)) votes.add(v);
        }
        return votes;
    }

    public CouncilVote getVote(UUID voteId) {
        return activeVotes.get(voteId);
    }

    private void checkExpiredVotes() {
        long now = System.currentTimeMillis();
        List<CouncilVote> expired = new ArrayList<>();
        for (CouncilVote vote : activeVotes.values()) {
            if (now >= vote.getExpiryTimestamp()) {
                expired.add(vote);
            }
        }
        for (CouncilVote vote : expired) {
            resolveVote(vote);
        }
    }

    private void resolveVote(CouncilVote vote) {
        activeVotes.remove(vote.getVoteId());

        double passThreshold = plugin.getRolesConfig().getCouncilVotePassThreshold();
        int totalVoters = vote.getEligibleVoters().size();

        int denyVotes = 0;
        for (Boolean v : vote.getVotes().values()) {
            if (!v) denyVotes++;
        }

        int passVotes = totalVoters - denyVotes;
        boolean passed = totalVoters == 0 || ((double) passVotes / totalVoters) >= passThreshold;

        if (passed) {
            if (vote.getOnPass() != null) {
                Bukkit.getScheduler().runTask(plugin, vote.getOnPass());
            }
            notifyResult(vote, true, passVotes, denyVotes);
        } else {
            if (vote.getOnDeny() != null) {
                Bukkit.getScheduler().runTask(plugin, vote.getOnDeny());
            }
            notifyResult(vote, false, passVotes, denyVotes);
        }
    }

    private void notifyResult(CouncilVote vote, boolean passed, int passVotes, int denyVotes) {
        String status = passed ? "\u00a7a\u00a7lPASSED" : "\u00a7c\u00a7lDENIED";
        String msg = "\u00a76[Council] \u00a77Vote on '\u00a7f" + vote.getDescription() + "\u00a77': " + status +
                " \u00a77(\u00a7a" + passVotes + " pass\u00a77/\u00a7c" + denyVotes + " deny\u00a77)";

        Player initiator = Bukkit.getPlayer(vote.getInitiatorUUID());
        if (initiator != null && initiator.isOnline()) initiator.sendMessage(msg);

        for (UUID voterUUID : vote.getEligibleVoters()) {
            if (!voterUUID.equals(vote.getInitiatorUUID())) {
                Player voter = Bukkit.getPlayer(voterUUID);
                if (voter != null && voter.isOnline()) voter.sendMessage(msg);
            }
        }
    }
}