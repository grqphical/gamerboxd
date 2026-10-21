# Gamerboxd

## Team Members: 

Noah Kay: **nka131** (computing), **frikallo** (discord & github)   
Nathan Jacobson: **nmj8** (computing), **grqphical** (discord & github)  
Milad Abdi: **mka321** (computing), **milad4515** (discord), **Milad244** (github)  
Anton Bulatov: **adb18** (computing), **jerrythegirrafe** (discord), **a-bulatov1** (github)  
Travis Jr Kong-Sloat: **Tjk9** (computing), **.schloss** (discord) **TJK-S** (github)

GitHub Link: [https\://github.com/grqphical/gamerboxd](https://github.com/grqphical/gamerboxd)  
Render URL: [https\://gamerboxd.onrender.com](https://gamerboxd.onrender.com) 

## General Information

**Application Name:** GamerBoxd

### Abstract
Crowdsourced media review sites such as Letterboxd have become super popular nowadays, so our group's project idea is to build a similar website to Letterboxd but for video games. Users will be able to sign up/log in, connect their Steam account (Steam is the largest PC gaming storefront), and leave reviews on video games they've played or browse reviews for games they may be considering to buy. This feature is already built into Steam, however what makes GamerBoxd different is the fact that social network features are built into our application. Users can follow other users, curate a feed of game reviews from other users, showcase what games they've played, and create "playlists" of games that can be shared with other users.

### How is this problem currently solved? (if at all) 
Steam already does reviews on their own platform, but their main focus is a game marketplace rather than crowd sourced reviews. Another site, IGDB, has a user reviews system but it's not the main feature of the site, focus is on games database. There also exists a similar site called Backloggd but they do not have features such as Steam account integration and a review feed.  

### How will this project make life better? Is it educational or just for entertainment?
Customers can visit our website which offers reviews for games they may wish to   
buy. The reviews offer insight to the customer which may influence their decision on if a game is worth purchasing or not, by helping them steer clear of games which the community has reviewed poorly, or find hidden gems that they may have missed if our website didn't exist. This website is for entertainment purposes only, and wishes to be unlike commercial game reviewer companies like IGN. Instead our focus is on community with features such as following users and a review feed. Also, steam does not have every game available, so we provide a larger, cross-platform game review system.

### Who is the target audience?
Our scope is to create a community based video game review site with some social network aspects, such as following, liking, and browsing feeds. We intend to integrate Steam, a video game marketplace into our site to improve onboarding and usability. Our scope does not include isolated, twitter-like posts, only reviews for games. We also don't intend to develop an algorithmic per-user feed, such as what Instagram or Tik-Tok has. Along with that, we are not making a "game recommender," since Steam has a well-established system already.

## Proposed Epics/Features (w/descriptions not stories)

1. **Account & Profiles**  
Users will be able to log in / sign up (email \+ Google/Steam Auth) for an account in our website which will have sessions, password reset, and secure password storage. With this account users can have a profile page including a bio, profile picture, favourite games, stats (games played), reviews written (not added until epic 3), followers, likes, and pc specs/platform (xbox, playstation). There will be account settings so a user can switch their profile to either public or private. Private profiles can still use all of the features of the website without worrying about their information being displayed publicly. We will need some additions to our database such as users, auth providers, user\_platform (includes PC specs as optional fields), so that we can implement this feature.

2. **Game Catalog & Steam Integration**  
Our site will use APIs such as Steam, GameBrain, or IGDB to gather and cache metadata about displayed games, such as their cover images, descriptions, genre tags, release dates, and existing ratings from other sites (Steam reviews, IGN, etc). To further integrate Steam's existing games ecosystem, we will add an option to link your Steam account to your Gamerboxd account to import your library, update playtime automatically, and smooth out the onboarding process. To implement this epic, we will add games, owned games, and tracked games to the database.

3. **Posts (Reviews & Lists)**  
Our site will include a feature for writing, editing and deleting reviews. Reviews will be simple 5 star ratings with a description by the user. These reviews will be visible from any game's page (2nd epic). Users can mark their reviewed games as "played", "to play", or "dropped", with separate profile tabs showing each category for users with many played games. Users will also be able to create collections of games similar to Spotify "playlists," to allow further organization and relating games to one another. Users reading the reviews or viewing the collections will be able to like and reply to them to give a means of discussion. Our database will have reviews, collections, collection items, likes, and comments added.

4. **Social and Feed**  
Our website will feature one discovery page. The top of the page shows popular games, new releases, and trending reviews (most engaged with). The rest of the page is a personalized feed of activity from followed users. In the scenario where a user is not logged in then the rest of the page will show currently trending games and popular reviews for that week instead of the personalized feed. A user that is logged in will be able to follow and unfollow other users. Users will receive a notification that they got a new follower, as well as receive notifications for likes and replies on their reviews. The notification indicator will be displayed on the main page, possibly on the navigation bar. Database additions such as followers and notifications will be added to make this feature possible.

5. **Search**  
Users can search for games, users, and game lists, using a search bar in the navigation bar. There will be a separate page for search results where results can be filtered. Users can add filters to their search such as genre, year, rating, etc.

This split allows for delegation and parallel work so no one is waiting on someone else's database changes, and conflicts are minimized.
