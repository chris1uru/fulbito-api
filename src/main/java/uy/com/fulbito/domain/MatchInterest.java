package uy.com.fulbito.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "match_interests")
public class MatchInterest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_request_id", nullable = false) private MatchRequest matchRequest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false) private AppUser player;
    @Column(name = "player_name_snapshot", nullable = false, length = 161) private String playerNameSnapshot;
    @Column(name = "player_phone_snapshot", nullable = false, length = 20) private String playerPhoneSnapshot;

    public MatchRequest getMatchRequest() { return matchRequest; }
    public void setMatchRequest(MatchRequest matchRequest) { this.matchRequest = matchRequest; }
    public AppUser getPlayer() { return player; }
    public void setPlayer(AppUser player) { this.player = player; }
    public String getPlayerNameSnapshot() { return playerNameSnapshot; }
    public void setPlayerNameSnapshot(String playerNameSnapshot) { this.playerNameSnapshot = playerNameSnapshot; }
    public String getPlayerPhoneSnapshot() { return playerPhoneSnapshot; }
    public void setPlayerPhoneSnapshot(String playerPhoneSnapshot) { this.playerPhoneSnapshot = playerPhoneSnapshot; }
}
