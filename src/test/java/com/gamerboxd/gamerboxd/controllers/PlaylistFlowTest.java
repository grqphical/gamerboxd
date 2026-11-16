package com.gamerboxd.gamerboxd.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.Playlist;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserPrivacy;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.PlaylistItemRepository;
import com.gamerboxd.gamerboxd.repositories.PlaylistLikeRepository;
import com.gamerboxd.gamerboxd.repositories.PlaylistRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.PlaylistService;
import com.gamerboxd.gamerboxd.services.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlaylistFlowTest {
    private static final int GAME_ID = 113112;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private PlaylistItemRepository itemRepository;
    @Autowired
    private PlaylistLikeRepository likeRepository;
    @MockitoBean
    private GameService gameService;

    private User john;
    private User olivia;
    private Playlist playlist;

    @BeforeEach
    void setUp() {
        playlistRepository.deleteAll();
        userRepository.deleteAll();
        john = userRepository.save(new User(0, "john", "john@example.com", "pw", UserRole.USER));
        olivia = userRepository.save(new User(0, "olivia", "olivia@example.com", "pw", UserRole.USER));

        Game hades = new Game(GAME_ID, "Hades", null, null, null, null, List.of(), List.of(), List.of(), null);
        when(gameService.getGameByID(GAME_ID)).thenReturn(Optional.of(hades));
        when(gameService.getGamesByIDs(any())).thenReturn(Map.of(GAME_ID, hades));

        playlist = playlistService.create(john.getId(), " Roguelikes ", "Run it back");
    }

    @Test
    void ownerAddsGame_anyoneCanViewAndBrowseIt() throws Exception {
        mockMvc.perform(post("/playlists/" + playlist.getId() + "/items")
                .param("gameId", "" + GAME_ID).param("category", "Favourites").param("rating", "5")
                .with(user(new AuthenticatedUser(john))).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/playlists/" + playlist.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Roguelikes")))
                .andExpect(content().string(containsString("Favourites")))
                .andExpect(content().string(containsString("Hades")))
                .andExpect(content().string(containsString("★★★★★")));
        mockMvc.perform(get("/playlists"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 game")));
        // the owner's view renders the editing controls
        mockMvc.perform(get("/playlists/" + playlist.getId()).with(user(new AuthenticatedUser(john))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Search for a game to add")));
    }

    @Test
    void addGame_rejectsOtherUsersDuplicatesAndBadRatings() throws Exception {
        mockMvc.perform(post("/playlists/" + playlist.getId() + "/items").param("gameId", "" + GAME_ID)
                .with(user(new AuthenticatedUser(olivia))).with(csrf()))
                .andExpect(status().isForbidden());

        playlistService.addGame(playlist.getId(), john.getId(), GAME_ID, " ", null);
        assertThrows(IllegalArgumentException.class,
                () -> playlistService.addGame(playlist.getId(), john.getId(), GAME_ID, null, null));
        int itemId = itemRepository.findAll().get(0).getId();
        assertThrows(IllegalArgumentException.class,
                () -> playlistService.updateItem(playlist.getId(), john.getId(), itemId, null, 6));
        assertEquals(1, itemRepository.count());
    }

    @Test
    void privateOwnersPlaylist_isHiddenFromOthers() throws Exception {
        john.setPrivacy(UserPrivacy.PRIVATE);
        userRepository.save(john);

        mockMvc.perform(get("/playlists/" + playlist.getId()).with(user(new AuthenticatedUser(olivia))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/playlists"))
                .andExpect(content().string(not(containsString("Roguelikes"))));
        mockMvc.perform(get("/playlists/" + playlist.getId()).with(user(new AuthenticatedUser(john))))
                .andExpect(status().isOk());
    }

    @Test
    void likeToggles_andDeletingTheOwnerRemovesEverything() {
        playlistService.addGame(playlist.getId(), john.getId(), GAME_ID, null, 4);
        assertTrue(playlistService.toggleLike(playlist.getId(), olivia.getId()));
        assertFalse(playlistService.toggleLike(playlist.getId(), olivia.getId()));
        assertTrue(playlistService.toggleLike(playlist.getId(), olivia.getId()));
        assertEquals(1, playlistService.getLikeCount(playlist.getId()));

        userService.deleteUser(john.getId());

        assertEquals(0, playlistRepository.count());
        assertEquals(0, itemRepository.count());
        assertEquals(0, likeRepository.count());
    }
}
