# Changes

## 0.2.0 (versionCode 2)

- **Theme**: full Material 3 light and dark scheme around the brand teal (#0F5E6E) and amber; every text colour pair checked at 4.5:1 or better. No dynamic colour. The selected nav label is no longer low-contrast yellow.
- **Data**: calls can have `k` (kind: Concorso, Mobilità…) and `w` (the job alone). Both optional, so older cached files still load. User data adds alert on/off, personal status, last-open time and onboarding flag, all with defaults: v0.1 `user.json` reads unchanged.
- **Cards**: the headline is the job (`w`), otherwise the short title without a repeated "Concorso:" prefix. Below it: kind · ente, place, posts and education labels, a coloured deadline badge ("Scade oggi/domani/tra N giorni"), a bookmark button and a "Nuovo" label for calls changed since the last open.
- **Search**: rounded search field with a clear button, and filter chips (Regione, Provincia, Professione, Titolo di studio, Scadenza) that open searchable bottom sheets. Provinces are limited to the chosen region. Also: Azzera, sort by deadline or newest, result count, "Avvisami per questa ricerca", and an empty state with reset.
- **Detail**: top bar with back, share and bookmark. Key facts with icons, the full title in sentence case behind "Testo completo del titolo", and a pinned "Apri il bando ufficiale su inPA/Gazzetta Ufficiale" button. Also "Aggiungi la scadenza al calendario" (calendar intent, no permission), the ossian.cloud page, the source and update time, and the disclaimer.
- **Salvati**: "Aperti" and "Scaduti" sections, a reminder label, and a personal status ("Da preparare" / "Domanda inviata").
- **Avvisi**: each alert is a card with a readable description, an on/off switch, a count of open matches (tap opens the search) and delete. The empty state explains how to create one. The background check skips alerts that are off.
- **First run**: one skippable screen ("Dove cerchi?", optional province, "Che lavoro?") leading to the matching results, with an offer to turn them into an alert. The notification permission (Android 13+) is asked only when you save a call or create an alert, after one line of explanation.
- **Info**: tidied into cards: who makes it, sources, limits, privacy, site link, version.
- **Code**: MainActivity split into Theme, Components, SearchScreen, DetailScreen, ListScreens and FirstRunScreen. The User-Agent now comes from BuildConfig.VERSION_NAME. Small vector drawables are used instead of material-icons-extended.
- **Tests**: DeadlineTest (badge text/urgency, days, filter matching, headline, sentence case, v0.1 user data). ScreenshotTest captures 14 screens in light and dark. The `ossian.offline` system property (tests only) stops refresh() from replacing the fixture feed.
