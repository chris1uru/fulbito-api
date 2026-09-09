package uy.com.fulbito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import uy.com.fulbito.domain.AppUser;
import uy.com.fulbito.dto.AuthDtos.ChangePasswordRequest;
import uy.com.fulbito.repository.ReservationRepository;
import uy.com.fulbito.repository.UserRepository;
import uy.com.fulbito.repository.VenueRepository;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository users;
    @Mock private VenueRepository venues;
    @Mock private ReservationRepository reservations;
    @Mock private PasswordEncoder passwords;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(users, venues, reservations, passwords);
    }

    @Test
    void passwordChangePersistsTheDetachedAuthenticatedUser() {
        AppUser user = mock(AppUser.class);
        when(user.getPasswordHash()).thenReturn("old-hash");
        when(user.getAuthVersion()).thenReturn(3);
        when(passwords.matches("old-password", "old-hash")).thenReturn(true);
        when(passwords.matches("new-password", "old-hash")).thenReturn(false);
        when(passwords.encode("new-password")).thenReturn("new-hash");

        service.changePassword(user, new ChangePasswordRequest("old-password", "new-password"));

        verify(user).setPasswordHash("new-hash");
        verify(user).setAuthVersion(4);
        verify(users).save(user);
    }
}
