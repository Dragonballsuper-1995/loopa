/**
 * Loopa Web — Application Controller
 * Manages state and integrates API, Supabase, and UI for the Loopa design.
 */

const App = {
    s: {
        user: null,
        isGuest: false,
        view: 'search',
        watchlist: [],
        searchResults: [],
        searchFilter: 'all',
        wlFilter: 'all',
        drawerItem: null,
        drawerDBEntry: null,
        searchDebounce: null,
        searchAbortController: null,
        searchSeqId: 0,
        aiLoaded: false,
        aiRecommendations: [],
        suggestionIndex: -1,
        currentSuggestions: [],
        trendingList: [],
        top10List: [],
        animeList: [],
        moviesList: [],
        tvList: [],
        // Performance flags
        dashboardLoaded: false,
        heroPaused: false,
        heroInterval: null,
        dashboardRefreshTimer: null,
        rowObservers: [],
    },

    async init() {
        const session = await SBAuth.getSession();
        if (session?.user) {
            this.s.user = session.user;
            this._boot();
        } else {
            document.getElementById('authModal').classList.add('active');
            setTimeout(() => document.getElementById('authContent').style.transform = 'translateY(0)', 10);
        }

        SBAuth.onAuthStateChange((event, session) => {
            if (event === 'SIGNED_IN' && session?.user && !this.s.user) {
                this.s.user = session.user;
                this._boot();
            }
            if (event === 'SIGNED_OUT') {
                this.s.user = null;
                document.getElementById('appContainer').classList.add('opacity-0');
                document.getElementById('authModal').classList.add('active');
                setTimeout(() => document.getElementById('authContent').style.transform = 'translateY(0)', 10);
            }
        });

        this._bindAll();
        PWAInstaller.init();
    },

    async _boot() {
        document.getElementById('authModal').classList.remove('active');
        document.getElementById('appContainer').classList.remove('opacity-0');
        
        const email = this.s.user?.email || 'GUEST';
        const handle = email.split('@')[0].toUpperCase().substring(0, 10);
        const handleHTML = `<span class="unskew">${handle}</span>`;
        const uH = document.getElementById('userHandle');
        const uHD = document.getElementById('userHandleDesktop');
        if(uH) uH.innerHTML = handleHTML;
        if(uHD) uHD.innerHTML = handleHTML;
        const dropdownEmail = document.getElementById('dropdownEmail');
        if(dropdownEmail) dropdownEmail.textContent = this.s.user?.email || 'GUEST PROTOCOL';
        this._updateSyncTime();
        
        if (!this.s.isGuest && this.s.user?.id) {
            this.s.watchlist = await SBList.getAll(this.s.user.id);
            if(this.s.view === 'watchlist') this._updateWLUI();

            const handleUpsert = async (newRow) => {
                    const idx = this.s.watchlist.findIndex(i => i.id === newRow.id && i.media_type === newRow.media_type);
                    if (idx >= 0) this.s.watchlist[idx] = newRow;
                    else this.s.watchlist.unshift(newRow);
                    if (this.s.view === 'watchlist') this._updateWLUI();
                    if (this.s.drawerDBEntry && this.s.drawerDBEntry.id === newRow.id && this.s.drawerDBEntry.media_type === newRow.media_type) {
                        this.s.drawerDBEntry = newRow;
                        UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry);
                    }
                };
            SBList.subscribeToChanges(
                this.s.user.id,
                handleUpsert,
                handleUpsert,
                async (oldRow) => {
                    this.s.watchlist = this.s.watchlist.filter(i => !(i.id === oldRow.id && i.media_type === oldRow.media_type));
                    if (this.s.view === 'watchlist') this._updateWLUI();
                    if (this.s.drawerDBEntry && this.s.drawerDBEntry.id === oldRow.id && this.s.drawerDBEntry.media_type === oldRow.media_type) {
                        this.s.drawerDBEntry = null;
                        UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry);
                    }
                }
            );
        }
        
        const savedView = sessionStorage.getItem('loopa_last_view') || 'search';
        this.navigateTo(savedView);
    },

    _updateSyncTime() {
        const lastSync = localStorage.getItem('lastSyncTime');
        const el = document.getElementById('dropdownSyncTime');
        if (el) {
            el.textContent = this._formatRelativeTime(lastSync ? parseInt(lastSync, 10) : 0);
        }
    },

    _formatRelativeTime(timestamp) {
        if (!timestamp) return 'Never synced';
        const diffMs = Date.now() - timestamp;
        const diffMins = Math.floor(diffMs / 60000);
        if (diffMins < 1) return 'Last synced: Just now';
        if (diffMins < 60) return `Last synced: ${diffMins} ${diffMins === 1 ? 'minute' : 'minutes'} ago`;
        const diffHours = Math.floor(diffMins / 60);
        if (diffHours < 24) return `Last synced: ${diffHours} ${diffHours === 1 ? 'hour' : 'hours'} ago`;
        const diffDays = Math.floor(diffHours / 24);
        return `Last synced: ${diffDays} ${diffDays === 1 ? 'day' : 'days'} ago`;
    },



    _bindAll() {
        // Auth
        const btnLogin = document.getElementById('btnLogin');
        if (btnLogin) btnLogin.addEventListener('click', () => this._login());
        
        const btnSignup = document.getElementById('btnSignup');
        if (btnSignup) btnSignup.addEventListener('click', () => this._signup());
        
        const btnGuest = document.getElementById('btnGuest');
        if (btnGuest) btnGuest.addEventListener('click', () => this._enterGuest());
        const bso = document.getElementById('btnSignOut');
        if(bso) bso.addEventListener('click', () => this._signOut());
        const bsod = document.getElementById('btnSignOutDesktop');
        if(bsod) bsod.addEventListener('click', () => this._signOut());

        // Dropdown toggle
        const trigger = document.getElementById('userDropdownTrigger');
        const dropdown = document.getElementById('settingsDropdown');
        if (trigger && dropdown) {
            trigger.addEventListener('click', (e) => {
                e.stopPropagation();
                this._updateSyncTime();
                dropdown.classList.toggle('hidden');
            });
            document.addEventListener('click', (e) => {
                if (!trigger.contains(e.target) && !dropdown.contains(e.target)) {
                    dropdown.classList.add('hidden');
                }
            });
        }

        // PJAX Overlay for Static Pages
        document.querySelectorAll('.static-page-link').forEach(link => {
            link.addEventListener('click', async (e) => {
                e.preventDefault();
                const url = link.getAttribute('href');
                const overlay = document.getElementById('staticPageOverlay');
                if(dropdown) dropdown.classList.add('hidden'); // Close dropdown
                overlay.innerHTML = '<div class="flex items-center justify-center min-h-screen text-loopAmber"><i class="fa-solid fa-circle-notch fa-spin text-2xl"></i></div>';
                overlay.classList.remove('hidden');
                requestAnimationFrame(() => overlay.classList.remove('opacity-0'));
                
                try {
                    const res = await fetch(url);
                    const html = await res.text();
                    const doc = new DOMParser().parseFromString(html, 'text/html');
                    
                    overlay.innerHTML = doc.body.innerHTML;
                    
                    const backBtn = overlay.querySelector('a[href="index.html"]');
                    if (backBtn) {
                        backBtn.addEventListener('click', (ev) => {
                            ev.preventDefault();
                            overlay.classList.add('opacity-0');
                            setTimeout(() => {
                                overlay.classList.add('hidden');
                                overlay.innerHTML = '';
                            }, 300);
                        });
                    }
                } catch(err) {
                    overlay.innerHTML = '<div class="p-10 text-loopError text-center font-bold mt-20">Failed to load page.</div>';
                }
            });
        });

        
        // Context Menu outside click
        document.addEventListener('click', (e) => {
            const cm = document.getElementById('contextMenu');
            if (cm && !cm.contains(e.target)) {
                cm.classList.remove('opacity-100');
                cm.classList.add('opacity-0', 'pointer-events-none');
                setTimeout(() => cm.classList.add('hidden'), 200);
            }
        });

        // Dropdown Sync DB Action
        const bds = document.getElementById('btnDropdownSync');
        if (bds) {
            bds.addEventListener('click', async () => {
                if (this.s.isGuest) {
                    UI.toast('AUTH REQUIRED', 'error');
                    return;
                }
                UI.toast('SYNCING DATABASE', 'info');
                try {
                    await this._loadTerminal();
                    UI.toast('SYNC COMPLETE');
                } catch (e) {
                    UI.toast('SYNC FAILED', 'error');
                }
            });
        }

        // Data Portability Suite (Export & Import)
        const btnExpJSON = document.getElementById('btnExportJSON');
        if (btnExpJSON) {
            btnExpJSON.addEventListener('click', () => {
                const list = this.s.watchlist || [];
                if (list.length === 0) {
                    UI.toast('WATCHLIST IS EMPTY', 'error');
                    return;
                }
                Portability.exportJSON(list);
                UI.toast('EXPORTED WATCHLIST (JSON)');
            });
        }

        const btnExpCSV = document.getElementById('btnExportCSV');
        if (btnExpCSV) {
            btnExpCSV.addEventListener('click', () => {
                const list = this.s.watchlist || [];
                if (list.length === 0) {
                    UI.toast('WATCHLIST IS EMPTY', 'error');
                    return;
                }
                Portability.exportCSV(list);
                UI.toast('EXPORTED WATCHLIST (CSV)');
            });
        }

        const btnImp = document.getElementById('btnImportWatchlist');
        const fileImp = document.getElementById('importFileInput');
        if (btnImp && fileImp) {
            btnImp.addEventListener('click', () => fileImp.click());
            fileImp.addEventListener('change', async (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                try {
                    const text = await file.text();
                    const candidates = Portability.parseContent(text, file.name);
                    if (!candidates || candidates.length === 0) {
                        UI.toast('NO VALID ITEMS FOUND IN FILE', 'error');
                        fileImp.value = '';
                        return;
                    }

                    const modal = document.getElementById('importModal');
                    const statusText = document.getElementById('importStatusText');
                    const pBar = document.getElementById('importProgressBar');
                    const pStats = document.getElementById('importProgressStats');

                    if (modal) {
                        modal.classList.remove('opacity-0', 'pointer-events-none');
                    }

                    const userId = this.s.user?.id || 'guest';
                    const result = await Portability.importWatchlist(candidates, userId, (cur, tot, title) => {
                        if (statusText) statusText.textContent = `Importing: ${title}`;
                        if (pBar) pBar.style.width = `${Math.round((cur / tot) * 100)}%`;
                        if (pStats) pStats.textContent = `${cur} / ${tot}`;
                    });

                    if (modal) {
                        modal.classList.add('opacity-0', 'pointer-events-none');
                    }

                    this.s.watchlist = await SBList.getAll(userId);
                    if (this.s.view === 'watchlist') this._updateWLUI();
                    UI.toast(`IMPORTED ${result.imported} OF ${result.total} ITEMS`);
                } catch (err) {
                    console.error('[Import] Error:', err);
                    UI.toast('IMPORT FAILED: ' + err.message, 'error');
                    const modal = document.getElementById('importModal');
                    if (modal) modal.classList.add('opacity-0', 'pointer-events-none');
                } finally {
                    fileImp.value = '';
                }
            });
        }

        // Notification toggle
        const nt = document.getElementById('notificationToggle');
        if (nt) {
            const enabled = localStorage.getItem('notificationProtocol') !== 'false';
            nt.checked = enabled;
            nt.addEventListener('change', (e) => {
                localStorage.setItem('notificationProtocol', e.target.checked);
                UI.toast(e.target.checked ? 'NOTIFICATIONS ENABLED' : 'NOTIFICATIONS DISABLED');
            });
        }

        // Nav
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.addEventListener('click', () => this.navigateTo(btn.dataset.nav));
        });

        // Search
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            const triggerSearch = (immediate = false) => {
                const q = searchInput.value.trim();
                clearTimeout(this.s.searchDebounce);

                if (this.s.view !== 'search' && q.length > 0) {
                    this.navigateTo('search', true);
                }

                const grid = document.getElementById('searchResults');
                const browseContent = document.getElementById('radar-browse-content');
                const resultsContainer = document.getElementById('search-results-container');
                
                if (q.length === 0) {
                    if (this.s.searchAbortController) {
                        this.s.searchAbortController.abort();
                        this.s.searchAbortController = null;
                    }
                    if (browseContent) browseContent.classList.remove('hidden');
                    if (resultsContainer) resultsContainer.classList.add('hidden');
                    this.s.searchResults = [];
                    return;
                } else {
                    if (browseContent) browseContent.classList.add('hidden');
                    if (resultsContainer) resultsContainer.classList.remove('hidden');
                }

                if (q.length < 2) {
                    if (grid) grid.innerHTML = '';
                    this.s.searchResults = [];
                    return;
                }

                if (grid && (!grid.innerHTML.trim() || immediate)) {
                    grid.innerHTML = UI.skeletonGrid(8);
                }

                if (immediate) {
                    this._doSearch(q);
                } else {
                    this.s.searchDebounce = setTimeout(() => {
                        this._doSearch(q);
                    }, 300);
                }
            };

            searchInput.addEventListener('input', () => triggerSearch(false));
            searchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    triggerSearch(true);
                }
            });
        }

        // See All links (Phase 4C + W3 Fix)
        document.querySelectorAll('[data-see-all]').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const cat = link.dataset.seeAll;
                this.navigateTo('search', true);

                const browseContent = document.getElementById('radar-browse-content');
                const resultsContainer = document.getElementById('search-results-container');

                if (browseContent) browseContent.classList.add('hidden');
                if (resultsContainer) resultsContainer.classList.remove('hidden');

                const sfType = cat === 'trending' ? 'all' : cat === 'anime' ? 'anime' : cat === 'movies' ? 'movie' : cat === 'tv' ? 'tv' : 'all';
                this.s.searchFilter = sfType;

                document.querySelectorAll('[data-sf]').forEach(b => {
                    if (b.dataset.sf === sfType) {
                        b.classList.add('active');
                    } else {
                        b.classList.remove('active');
                    }
                });

                const list = cat === 'anime' ? this.s.animeList
                    : cat === 'movies' ? this.s.moviesList
                    : cat === 'tv' ? this.s.tvList
                    : this.s.trendingList;

                UI.renderSearchGrid(list.length ? list : this.s.trendingList);
            });
        });

        // Filters
        document.querySelectorAll('[data-sf]').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('[data-sf]').forEach(b => {
                    b.classList.remove('bg-vibrantCyan', 'text-cineCharcoal');
                    b.classList.add('bg-cineSurface', 'text-gray-400');
                });
                btn.classList.remove('bg-cineSurface', 'text-gray-400');
                btn.classList.add('bg-vibrantCyan', 'text-cineCharcoal');
                this.s.searchFilter = btn.dataset.sf;
                const list = this.s.searchFilter === 'all' ? this.s.searchResults : this.s.searchResults.filter(i => i.mediaType === this.s.searchFilter);
                UI.renderSearchGrid(list);
            });
        });

        document.querySelectorAll('[data-wf]').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('[data-wf]').forEach(b => {
                    b.classList.remove('active');
                });
                btn.classList.add('active');
                this.s.wlFilter = btn.dataset.wf;
                this._updateWLUI();
            });
        });

        // AI
        const btnRefreshAI = document.getElementById('btnRefreshAI');
        if (btnRefreshAI) btnRefreshAI.addEventListener('click', () => this._loadAI(true));

        // Radar Refresh Button
        const btnRefreshRadar = document.getElementById('btnRefreshRadar');
        if (btnRefreshRadar) btnRefreshRadar.addEventListener('click', () => this.refreshDashboard());
 

        // Modals
        const btnModalClose = document.getElementById('btnModalClose');
        if (btnModalClose) btnModalClose.addEventListener('click', () => this.closeDrawer());
        
        const detailModal = document.getElementById('detailModal');
        if (detailModal) {
            detailModal.addEventListener('click', (e) => {
                if (e.target === detailModal) {
                    this.closeDrawer();
                }
            });
        }

        // Auth inputs Enter key listener
        const authEmail = document.getElementById('authEmail');
        const authPassword = document.getElementById('authPassword');
        const handleAuthEnter = (e) => {
            if (e.key === 'Enter') {
                this._login();
            }
        };
        if (authEmail) authEmail.addEventListener('keypress', handleAuthEnter);
        if (authPassword) authPassword.addEventListener('keypress', handleAuthEnter);

        // Global Keyboard Shortcuts
        document.addEventListener('keydown', (e) => {
            // Escape key handler
            if (e.key === 'Escape') {
                // 1. Close Detail Modal if active
                const detailModal = document.getElementById('detailModal');
                if (detailModal && detailModal.classList.contains('active')) {
                    this.closeDrawer();
                }
                // 2. Close Context Menu if visible
                const cm = document.getElementById('contextMenu');
                if (cm && !cm.classList.contains('hidden')) {
                    this.closeContextMenu();
                }
                // 3. Clear and blur Search Input if focused
                const searchInput = document.getElementById('searchInput');
                if (searchInput && document.activeElement === searchInput) {
                    searchInput.value = '';
                    searchInput.blur();
                    searchInput.dispatchEvent(new Event('input'));
                }
            }

            // "/" key to focus Search Input
            if (e.key === '/') {
                const searchInput = document.getElementById('searchInput');
                if (searchInput && document.activeElement !== searchInput) {
                    const activeTag = document.activeElement.tagName;
                    if (activeTag !== 'INPUT' && activeTag !== 'TEXTAREA') {
                        e.preventDefault();
                        searchInput.focus();
                        searchInput.select();
                    }
                }
            }

            // Ctrl/Cmd + Enter to save personal notes inside Detail Modal
            if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                const notesInput = document.getElementById('personalNotesInput');
                if (notesInput && document.activeElement === notesInput) {
                    const btnSaveNotes = document.getElementById('btnSaveNotes');
                    if (btnSaveNotes) btnSaveNotes.click();
                }
            }
        });

        // Phase 2B: Scroll-transparent header — fade header over hero, restore when scrolled past
        const header = document.querySelector('header');
        const radarTab = document.getElementById('tab-search');
        if (header) {
            const updateHeaderTransparency = () => {
                const heroSection = document.getElementById('hero-section');
                const isRadarVisible = radarTab && !radarTab.classList.contains('hidden');
                if (isRadarVisible && heroSection) {
                    const scrollY = window.scrollY || document.documentElement.scrollTop;
                    const heroHeight = heroSection.offsetHeight;
                    // Transparent when within top 60% of hero height
                    if (scrollY < heroHeight * 0.6) {
                        header.classList.add('header-transparent');
                    } else {
                        header.classList.remove('header-transparent');
                    }
                } else {
                    header.classList.remove('header-transparent');
                }
            };
            window.addEventListener('scroll', updateHeaderTransparency, { passive: true });
            // Run once immediately so state is correct on load
            requestAnimationFrame(updateHeaderTransparency);
        }
    },

    // ── Auth ──────────────────────────────────────────────────────────────────
    async _login() {
        const email = document.getElementById('authEmail').value.trim();
        const password = document.getElementById('authPassword').value;
        if (!email || !password) return UI.toast('MISSING CREDENTIALS', 'error');
        try {
            this.s.user = await SBAuth.signIn(email, password);
            this._boot();
            UI.toast('ACCESS GRANTED');
        } catch (err) {
            UI.toast('ACCESS DENIED', 'error');
        }
    },

    async _signup() {
        const email = document.getElementById('authEmail').value.trim();
        const password = document.getElementById('authPassword').value;
        if (!email || password.length < 6) return UI.toast('INVALID CREDENTIALS', 'error');
        try {
            await SBAuth.signUp(email, password);
            UI.toast('VERIFY EMAIL TO ACTIVATE');
        } catch (err) {
            UI.toast(err.message, 'error');
        }
    },

    _enterGuest() {
        this.s.isGuest = true;
        this.s.user = { id: 'guest', email: 'guest@loopa' };
        this._boot();
        UI.toast('GUEST PROTOCOL ACTIVE');
    },

    async _signOut() {
        if (!this.s.isGuest) { try { await SBAuth.signOut(); } catch {} }
        location.reload();
    },

    // ── Navigation ────────────────────────────────────────────────────────────
    navigateTo(view, fromSearchInput = false) {
        sessionStorage.setItem('loopa_last_view', view);
        
        if (view !== 'search') {
            // Pause hero carousel when leaving Radar tab (don't destroy the interval)
            this.s.heroPaused = true;
        } else {
            // Resume hero when returning to Radar (if page is visible)
            if (!document.hidden) {
                this.s.heroPaused = false;
            }
        }
        this.s.view = view;
        ['search', 'watchlist', 'ai'].forEach(v => {
            const el = document.getElementById(`tab-${v}`);
            if (el) el.classList.add('hidden');
        });
        const activeEl = document.getElementById(`tab-${view}`);
        if (activeEl) activeEl.classList.remove('hidden');

        document.querySelectorAll('.nav-btn').forEach(btn => {
            const isActive = btn.dataset.nav === view;
            btn.classList.toggle('active-nav', isActive);
        });

        // Phase 2B: Re-evaluate header transparency on tab switch
        window.dispatchEvent(new Event('scroll'));

        if (view === 'search') {
            if (!fromSearchInput) {
                const searchInput = document.getElementById('searchInput');
                if (searchInput) {
                    const savedSearch = sessionStorage.getItem('loopa_last_search');
                    if (savedSearch) {
                        searchInput.value = savedSearch;
                        // Trigger search automatically if we had one
                        if (savedSearch.trim().length > 0) {
                            // Let the input event handle it, or just set it
                            setTimeout(() => {
                                searchInput.dispatchEvent(new Event('input', { bubbles: true }));
                            }, 50);
                        }
                    } else {
                        searchInput.value = '';
                    }
                }
                const browseContent = document.getElementById('radar-browse-content');
                const resultsContainer = document.getElementById('search-results-container');
                if (browseContent) browseContent.classList.remove('hidden');
                if (resultsContainer) resultsContainer.classList.add('hidden');
                this._loadDashboard();
            }
        }
        if (view === 'watchlist') this._loadTerminal();
        if (view === 'ai' && !this.s.aiLoaded) this._loadAI();

    },

    // ── Data Loading ──────────────────────────────────────────────────────────
    


    async _loadDashboard(forceRefresh = false) {
        // ── Session State Recovery ───────────────────────────────────────────
        if (!this.s.dashboardLoaded && !forceRefresh) {
            const cached = sessionStorage.getItem('loopa_dashboard');
            if (cached) {
                try {
                    const data = JSON.parse(cached);
                    this.s.trendingList = data.trendingList || [];
                    this.s.top10List = data.top10List || [];
                    this.s.animeList = data.animeList || [];
                    this.s.moviesList = data.moviesList || [];
                    this.s.tvList = data.tvList || [];
                    this.s.dashboardLoaded = true;
                } catch(e) {}
            }
        }

        // ── Session cache guard ───────────────────────────────────────────────
        // If dashboard was already loaded and we're not forcing a refresh,
        // re-render from in-memory lists instantly (zero network cost).
        if (this.s.dashboardLoaded && !forceRefresh) {
            this._startHeroCarousel(this.s.trendingList.filter(i => i.backdropUrl || i.posterUrl).slice(0, 5));
            if (this.s.trendingList.length) {
                UI.renderScrollRow('row-trending', this.s.trendingList.slice(0, 10));
            }
            if (this.s.top10List.length) {
                UI.renderTop10Row(this.s.top10List);
            }
            if (this.s.animeList.length)   UI.renderScrollRow('row-anime',    this.s.animeList.slice(0, 10));
            if (this.s.moviesList.length)  UI.renderScrollRow('row-movies',   this.s.moviesList.slice(0, 10));
            if (this.s.tvList.length)      UI.renderScrollRow('row-tv',       this.s.tvList.slice(0, 10));
            // Tier-3 rows: re-attach IntersectionObserver so they still lazy-load
            this._attachTier3Observers();
            return;
        }

        // Clear any previous row observers
        this.s.rowObservers.forEach(obs => obs.disconnect());
        this.s.rowObservers = [];

        // ── Tier 1: Critical — renders hero immediately ───────────────────────
        document.getElementById('row-trending').innerHTML = UI.skeletonRow(6);
        const top10Container = document.getElementById('row-top10');
        if (top10Container) top10Container.innerHTML = UI.skeletonRow(6);

        const [trending, top10, wl] = await Promise.allSettled([
            API.fetchTrending(),
            API.fetchTop10Today(),
            this.s.isGuest ? Promise.resolve([]) : SBList.getAll(this.s.user.id)
        ]);

        const t = trending.status === 'fulfilled' ? trending.value : [];
        const top10List = top10.status === 'fulfilled' ? top10.value : [];
        this.s.trendingList = t;
        this.s.top10List = top10List;

        if (wl.status === 'fulfilled') {
            this.s.watchlist = wl.value;
            if (!this.s.isGuest) {
                localStorage.setItem('lastSyncTime', Date.now());
                this._updateSyncTime();
            }
        }

        this._reindexSearch();

        const heroItems = t.filter(i => i.backdropUrl || i.posterUrl).slice(0, 5);
        this._startHeroCarousel(heroItems);
        if (t.length) {
            UI.renderScrollRow('row-trending', t.slice(0, 10));
        }
        if (top10List.length) {
            UI.renderTop10Row(top10List);
        }

        // ── Tier 2: Above-fold rows — fire shortly after hero renders ─────────
        setTimeout(async () => {
            document.getElementById('row-anime').innerHTML  = UI.skeletonRow(6);
            document.getElementById('row-movies').innerHTML = UI.skeletonRow(6);
            document.getElementById('row-tv').innerHTML     = UI.skeletonRow(6);

            const [anime, movies, tv] = await Promise.allSettled([
                API.fetchTopAnime(),
                API.fetchPopularMovies(),
                API.fetchPopularTV(),
            ]);

            this.s.animeList  = anime.status  === 'fulfilled' ? anime.value  : [];
            this.s.moviesList = movies.status === 'fulfilled' ? movies.value : [];
            this.s.tvList     = tv.status     === 'fulfilled' ? tv.value     : [];

            this._reindexSearch();

            if (anime.status  === 'fulfilled') UI.renderScrollRow('row-anime',  anime.value.slice(0, 10));
            if (movies.status === 'fulfilled') UI.renderScrollRow('row-movies', movies.value.slice(0, 10));
            if (tv.status     === 'fulfilled') UI.renderScrollRow('row-tv',     tv.value.slice(0, 10));
            
            this._saveDashboardCache();
        }, 100);

        // ── Tier 3: Below-fold — IntersectionObserver lazy-load ──────────────
        this._attachTier3Observers();

        // Mark as loaded and schedule auto-refresh
        this.s.dashboardLoaded = true;
        this._scheduleAutoRefresh();
    },
    
    _saveDashboardCache() {
        try {
            sessionStorage.setItem('loopa_dashboard', JSON.stringify({
                trendingList: this.s.trendingList,
                top10List: this.s.top10List,
                animeList: this.s.animeList,
                moviesList: this.s.moviesList,
                tvList: this.s.tvList
            }));
        } catch(e) {}
    },

    // ── Hero Carousel Management ─────────────────────────────────────────────
    _startHeroCarousel(heroItems) {
        clearInterval(this.s.heroInterval);
        this.s.heroInterval = null;

        if (!heroItems.length) return;

        let heroIndex = 0;
        UI.renderHero(heroItems[heroIndex], heroItems, heroIndex);

        this.s.heroInterval = setInterval(() => {
            // Don't advance carousel if hero is paused (modal open / tab hidden)
            if (this.s.heroPaused) return;
            heroIndex = (heroIndex + 1) % heroItems.length;
            UI.renderHero(heroItems[heroIndex], heroItems, heroIndex);
        }, 6000);  // Phase 2B: 6s — longer dwell time for cinematic hero

        // Pause hero when page is hidden (user switches browser tab) or modal is open
        if (!this.s._visibilityBound) {
            this.s._visibilityBound = true;
            document.addEventListener('visibilitychange', () => {
                const isModalOpen = document.getElementById('detailModal')?.classList.contains('active');
                this.s.heroPaused = document.hidden || isModalOpen;
            });
        }

        // Store ref to heroItems for jump method
        this.s._heroItems = heroItems;
        this.s._heroIndexRef = { value: heroIndex };

        // Phase 2B: _jumpHero — jump to a specific slide from dot click
        this._jumpHero = (idx) => {
            if (idx < 0 || idx >= heroItems.length) return;
            heroIndex = idx;
            clearInterval(this.s.heroInterval);
            UI.renderHero(heroItems[heroIndex], heroItems, heroIndex);
            // Restart interval so the next auto-advance starts fresh from this slide
            this.s.heroInterval = setInterval(() => {
                if (this.s.heroPaused) return;
                heroIndex = (heroIndex + 1) % heroItems.length;
                UI.renderHero(heroItems[heroIndex], heroItems, heroIndex);
            }, 6000);
        };
    },

    // ── Tier-3 IntersectionObserver lazy rows ────────────────────────────────
    _attachTier3Observers() {
        const tier3 = [
            { rowId: 'row-top-movies',      fetch: () => API.fetchTopRatedMovies()    },
            { rowId: 'row-upcoming-movies', fetch: () => API.fetchUpcomingMovies()    },
            { rowId: 'row-top-tv',          fetch: () => API.fetchTopRatedTV()        },
            { rowId: 'row-airing-tv',       fetch: () => API.fetchAiringTodayTV()     },
            { rowId: 'row-upcoming-anime',  fetch: () => API.fetchUpcomingAnime()     },
        ];

        tier3.forEach(({ rowId, fetch }) => {
            const rowEl = document.getElementById(rowId);
            if (!rowEl) return;

            // If already populated, skip
            if (rowEl.children.length > 0 && !rowEl.querySelector('.skeleton')) return;

            // Start with skeleton
            rowEl.innerHTML = UI.skeletonRow(6);

            const section = rowEl.closest('section') || rowEl;
            const obs = new IntersectionObserver(async (entries) => {
                if (!entries[0].isIntersecting) return;
                obs.disconnect();
                try {
                    const items = await fetch();
                    UI.renderScrollRow(rowId, items.slice(0, 10));
                } catch (e) {
                    rowEl.innerHTML = '';
                }
            }, { rootMargin: '200px' }); // pre-load 200px before viewport

            obs.observe(section);
            this.s.rowObservers.push(obs);
        });
    },

    // ── Dashboard Auto-Refresh (30 min) ──────────────────────────────────────
    _scheduleAutoRefresh() {
        clearTimeout(this.s.dashboardRefreshTimer);
        const THIRTY_MIN = 30 * 60 * 1000;
        this.s.dashboardRefreshTimer = setTimeout(() => {
            // Only auto-refresh if Radar tab is currently visible
            if (this.s.view === 'search' && !document.hidden) {
                this.refreshDashboard();
            } else {
                // Reset loaded flag so next visit to Radar triggers a fresh load
                this.s.dashboardLoaded = false;
                API.clearCache();
            }
        }, THIRTY_MIN);
    },

    async refreshDashboard() {
        this.s.dashboardLoaded = false;
        API.clearCache();
        await this._loadDashboard();
        UI.toast('CONTENT REFRESHED');
    },


    async _doSearch(q) {
        // Abort previous in-flight search requests and increment sequence ID
        if (this.s.searchAbortController) {
            this.s.searchAbortController.abort();
        }
        this.s.searchAbortController = new AbortController();
        const signal = this.s.searchAbortController.signal;
        const currentSeq = ++this.s.searchSeqId;

        try {
            let actualQuery = q;
            const cached = LoopaSearchEngine.getCachedCorrection(q);
            if (cached) {
                actualQuery = cached;
            }

            // 1. Perform instant fast search with unified Edge API (~20-80ms)
            const fastResults = await API.searchFast(actualQuery, signal);
            if (currentSeq !== this.s.searchSeqId) return; // Stale query discarded

            // Fetch fuzzy matches from local watchlist index
            const localHits = LoopaSearchEngine.getSuggestions(q, 4);

            const renderResults = (remoteItems) => {
                if (currentSeq !== this.s.searchSeqId) return; // Stale query discarded
                const merged = [];
                const seen = new Set();
                const normalizedQ = q.toLowerCase().trim();
                const normalizedAQ = actualQuery.toLowerCase().trim();

                // 1. Exact local matches first
                localHits.forEach(item => {
                    const title = (item.title || '').toLowerCase().trim();
                    if (title === normalizedQ || title === normalizedAQ) {
                        const key = item.id + '_' + (item.mediaType || item.media_type);
                        if (!seen.has(key)) {
                            merged.push(item);
                            seen.add(key);
                        }
                    }
                });

                // 2. Remote API results
                remoteItems.forEach(item => {
                    const key = item.id + '_' + (item.mediaType || item.media_type);
                    if (!seen.has(key)) {
                        merged.push(item);
                        seen.add(key);
                    }
                });

                // 3. Other local fuzzy matches
                localHits.forEach(item => {
                    const key = item.id + '_' + (item.mediaType || item.media_type);
                    if (!seen.has(key)) {
                        merged.push(item);
                        seen.add(key);
                    }
                });

                this.s.searchResults = merged;
                const filter = this.s.searchFilter || 'all';
                const list = filter === 'all' ? merged : merged.filter(i => (i.mediaType || i.media_type) === filter);
                UI.renderSearchGrid(list);
            };

            // RENDER INSTANTLY NOW (< 50ms)!
            renderResults(fastResults);

            // 2. Asynchronously run AI Semantic Smart Search in background
            const isNaturalLanguage = q.split(/\s+/).length >= 3 || /(like|about|vibe|dystopian|cyberpunk|anime|movie|show|recommend|similar|dark|funny|robot|car|cook)/i.test(q);
            if (isNaturalLanguage || (fastResults.length < 3 && q.length >= 4)) {
                (async () => {
                    try {
                        const semanticResults = await API.searchSemantic(q, signal);
                        if (currentSeq !== this.s.searchSeqId) return;
                        if (semanticResults && semanticResults.length > 0) {
                            renderResults([...semanticResults, ...fastResults]);
                            return;
                        }
                    } catch (err) {
                        if (err.name === 'AbortError') return;
                    }

                    // Fallback to spelling correction if semantic returned no extra results
                    if (!cached && fastResults.length < 3 && CONFIG.RECOMMENDATIONS_URL) {
                        try {
                            const prompt = `Correct any typos or spelling errors in this media search query: '${q}'. Return ONLY a raw JSON object: {"correctedQuery":"<corrected title>"}. Example: "Moha" -> {"correctedQuery":"Mohabbatein"}.`;
                            const aiRes = await fetch(CONFIG.RECOMMENDATIONS_URL, {
                                method: 'POST',
                                signal: signal,
                                headers: {
                                    'Content-Type': 'application/json',
                                    'X-Loopa-Client-Key': CONFIG.CLIENT_KEY
                                },
                                body: JSON.stringify({ prompt })
                            });
                            if (currentSeq !== this.s.searchSeqId) return;
                            if (aiRes.ok) {
                                let text = await aiRes.text();
                                text = text.replace(/```json?/g, '').replace(/```/g, '').trim();
                                const parsed = JSON.parse(text);
                                if (parsed.correctedQuery && parsed.correctedQuery.toLowerCase() !== q.toLowerCase()) {
                                    LoopaSearchEngine.cacheCorrection(q, parsed.correctedQuery);
                                    const extraRes = await API.searchFast(parsed.correctedQuery, signal);
                                    if (currentSeq !== this.s.searchSeqId) return;
                                    if (extraRes.length > 0) {
                                        renderResults([...fastResults, ...extraRes]);
                                    }
                                }
                            }
                        } catch (err) {
                            if (err.name === 'AbortError') return;
                        }
                    }
                })();
            }
        } catch (err) {
            if (err.name === 'AbortError') return;
            if (currentSeq !== this.s.searchSeqId) return;
            document.getElementById('searchResults').innerHTML = `
                <div class="col-span-full py-16 text-center">
                    <p class="font-headers text-base text-red-400">ERROR: ${err.message}</p>
                </div>
            `;
        }
    },

    async _loadTerminal() {
        if (this.s.isGuest) return this._updateWLUI();
        document.getElementById('watchlistGrid').innerHTML = UI.skeletonGrid(6);
        try {
            this.s.watchlist = await SBList.getAll(this.s.user.id);
            localStorage.setItem('lastSyncTime', Date.now());
            this._updateSyncTime();
            this._reindexSearch();
            this._updateWLUI();
        } catch (err) {
            UI.toast('SYNC FAILED', 'error');
        }
    },

    _updateWLUI() {
        document.getElementById('watchlistCount').textContent = `${this.s.watchlist.length} Titles`;
        const filter = this.s.wlFilter || 'all';
        const list = filter === 'all' 
            ? this.s.watchlist 
            : this.s.watchlist.filter(i => (i.media_type || i.mediaType) === filter);
        UI.renderWatchlistGrid(list, this.s.watchlist);
    },

    _reindexSearch() {
        if (!window.LoopaSearchEngine) return;
        LoopaSearchEngine.clearIndex();
        
        if (this.s.trendingList) LoopaSearchEngine.indexMediaItems(this.s.trendingList);
        if (this.s.top10List) LoopaSearchEngine.indexMediaItems(this.s.top10List);
        if (this.s.animeList) LoopaSearchEngine.indexMediaItems(this.s.animeList);
        if (this.s.moviesList) LoopaSearchEngine.indexMediaItems(this.s.moviesList);
        if (this.s.tvList) LoopaSearchEngine.indexMediaItems(this.s.tvList);
        
        if (this.s.watchlist && this.s.watchlist.length > 0) {
            const normalizedWL = this.s.watchlist.map(dbItem => ({
                id: dbItem.id,
                mediaType: dbItem.media_type,
                title: dbItem.title,
                posterUrl: dbItem.image_url,
                backdropUrl: dbItem.image_url,
                year: dbItem.date ? String(dbItem.date).substring(0, 4) : '',
                score: dbItem.score,
                synopsis: dbItem.personal_notes || '',
                genres: [],
                totalEpisodes: dbItem.total_episodes || 0,
                totalSeasons: dbItem.total_seasons || 0,
                status: dbItem.list_name,
                inWatchlist: true
            }));
            LoopaSearchEngine.indexMediaItems(normalizedWL);
        }
    },

    _selectSuggestion(item) {
        const searchInput = document.getElementById('searchInput');
        const suggestionsDropdown = document.getElementById('searchSuggestionsDropdown');
        if (searchInput) searchInput.value = item.title;
        if (suggestionsDropdown) suggestionsDropdown.classList.add('hidden');
        
        // Populate view-search state
        const browseContent = document.getElementById('radar-browse-content');
        const resultsContainer = document.getElementById('search-results-container');
        if (browseContent) browseContent.classList.add('hidden');
        if (resultsContainer) resultsContainer.classList.remove('hidden');
        
        this.openDrawer(item);
    },

    async _loadAI() {
        if (this.s.aiLoaded) return;
        this.s.aiLoaded = true;

        if (this.s.isGuest) {
            const chatLog = document.getElementById('aiChatLog');
            chatLog.innerHTML = '';
            chatLog.appendChild(UI.renderAiMessage('model', 'GUEST PROTOCOL: AI Assistant requires an active account. Please sign in.'));
            return;
        }

        // Initialize state
        this.s.chatHistory = [];
        this.s.isAiThinking = false;
        
        // Bind events
        const btnSend = document.getElementById('btnAiSend');
        const input = document.getElementById('aiChatInput');
        
        const sendMsg = () => {
            const msg = input.value.trim();
            if (msg) this._handleAiChat(msg);
            input.value = '';
        };

        if (btnSend) btnSend.addEventListener('click', sendMsg);
        if (input) {
            input.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') sendMsg();
            });
        }

        // Bind Mood / Vibe Starter Chips
        document.querySelectorAll('#aiMoodChips .ai-chip').forEach(btn => {
            btn.addEventListener('click', () => {
                const prompt = btn.dataset.prompt;
                if (prompt) this._handleAiChat(prompt);
            });
        });
        
        const btnClear = document.getElementById('btnAiClearChat');
        if (btnClear) {
            btnClear.addEventListener('click', () => {
                this.s.chatHistory = [];
                const chatLog = document.getElementById('aiChatLog');
                if (chatLog) chatLog.innerHTML = '';
                this._handleAiChat('Give me an exciting, unexpected movie or anime recommendation to surprise me tonight', true);
            });
        }

        // Auto-fetch initial curated daily mix on first load
        const chatLog = document.getElementById('aiChatLog');
        if (chatLog) chatLog.innerHTML = '';
        this._handleAiChat('Curated picks for you tonight', true);
    },

    async _handleAiChat(message, isInitialAuto = false) {
        if (this.s.isAiThinking) return;
        this.s.isAiThinking = true;

        const chatLog = document.getElementById('aiChatLog');
        
        // Render User Message if not an automatic background greeting
        if (!isInitialAuto) {
            chatLog.appendChild(UI.renderAiMessage('user', message));
            chatLog.scrollTop = chatLog.scrollHeight;
        }
        
        // Add loading state
        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'flex items-center gap-3 self-start max-w-4xl animate-fade-in-up mt-2 mb-2';
        loadingDiv.innerHTML = `
            <div class="w-8 h-8 rounded-full bg-loopSurface border border-loopAmber/20 flex items-center justify-center text-loopAmber"><i class="fa-solid fa-wand-magic-sparkles text-[10px]"></i></div>
            <div class="flex items-center gap-1 bg-loopSurface/60 rounded-2xl rounded-tl-none px-4 py-3 h-[42px]">
                <div class="w-2 h-2 bg-loopAmber/50 rounded-full animate-bounce"></div>
                <div class="w-2 h-2 bg-loopAmber/50 rounded-full animate-bounce" style="animation-delay: 0.1s"></div>
                <div class="w-2 h-2 bg-loopAmber/50 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
            </div>
        `;
        chatLog.appendChild(loadingDiv);
        chatLog.scrollTop = chatLog.scrollHeight;

        try {
            if (!this.s.watchlist.length && !this.s.isGuest && this.s.user) {
                this.s.watchlist = await SBList.getAll(this.s.user.id);
            }
            let targets = this.s.watchlist.filter(i => i.list_name === 'Watched');
            if (targets.length === 0) targets = this.s.watchlist;
            
            const liked = JSON.parse(localStorage.getItem('oracle_liked_titles') || '[]');
            const disliked = JSON.parse(localStorage.getItem('oracle_disliked_titles') || '[]');

            // Push to history if user prompt
            if (!isInitialAuto) {
                this.s.chatHistory.push({ role: 'user', content: message });
            }
            
            const recs = await API.getAIRecommendations(targets, liked, disliked, this.s.chatHistory);
            
            // Enrich results
            const enriched = await Promise.allSettled(recs.map(async rec => {
                let r;
                if (rec.mediaType === 'anime') r = await API.searchAnime(rec.title);
                else if (rec.mediaType === 'movie') r = await API.searchMovies(rec.title);
                else r = await API.searchTV(rec.title);
                return r && r[0]
                    ? { ...r[0], reason: rec.reasoning, reasoning: rec.reasoning,
                                 genre: rec.genre, releaseYear: rec.releaseYear }
                    : null;
            }));

            const valid = enriched.filter(r => r.status === 'fulfilled' && r.value).map(r => r.value);
            
            // Generate response text
            let responseText = isInitialAuto
                ? (targets.length > 0 
                    ? "Welcome back! Based on your watchlist history, here is your curated daily mix:" 
                    : "Hello! Here are top curated picks across Movies, TV Shows, and Anime to kickstart your journey:")
                : "Here are some recommendations based on what you asked:";

            if (valid.length === 0) {
                responseText = "I couldn't find any good matches for that. Could you try asking in a different way?";
            }
            
            this.s.chatHistory.push({ role: 'model', content: responseText });
            
            // Remove loading
            if (chatLog.contains(loadingDiv)) {
                chatLog.removeChild(loadingDiv);
            }
            
            // Render Model Message
            chatLog.appendChild(UI.renderAiMessage('model', responseText));
            
            if (valid.length > 0) {
                // Render grid inside chat
                const gridDiv = document.createElement('div');
                gridDiv.className = 'grid grid-cols-2 sm:grid-cols-4 gap-4 mt-2 max-w-4xl w-full self-start';
                valid.slice(0, 4).forEach(item => {
                    gridDiv.appendChild(UI.posterCardGrid(item, false, i => this.openDrawer(i), true));
                });
                chatLog.appendChild(gridDiv);
            }
            
            chatLog.scrollTop = chatLog.scrollHeight;
            
        } catch (err) {
            if (chatLog.contains(loadingDiv)) {
                chatLog.removeChild(loadingDiv);
            }
            chatLog.appendChild(UI.renderAiMessage('model', `Sorry, I encountered an error: ${err.message}`));
            chatLog.scrollTop = chatLog.scrollHeight;
        } finally {
            this.s.isAiThinking = false;
        }
    },

    // ── Drawer & Watchlist Actions ────────────────────────────────────────────
    async openDrawer(item) {
        this.s.heroPaused = true; // Pause hero carousel while modal is open
        this.s.drawerItem = item;
        this.s.drawerDBEntry = this.s.watchlist.find(w => w.id === item.id && w.media_type === item.mediaType) || null;
        this.s.drawerWatchedEpisodes = [];
        if (!this.s.isGuest && this.s.user) {
            this.s.drawerWatchedEpisodes = await SBWatchedEpisodes.getForMedia(this.s.user.id, item.id, item.mediaType);
        }
        UI.renderDrawer(item, this.s.drawerDBEntry, this.s.drawerWatchedEpisodes);
        document.getElementById('detailModal').classList.add('active');
        document.body.style.overflow = 'hidden'; // prevent background scroll

        try {
            if (item.id > 0) {
                const full = await API.fetchDetails(item.id, item.mediaType || item.media_type, item.provider);
                if (full) {
                    this.s.drawerItem = { ...item, ...full };
                    UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry, this.s.drawerWatchedEpisodes);
                }
            }
        } catch {}

        // Phase 3C: Fetch and render More Like This row after details load
        UI._fetchAndRenderSimilar(item.id, item.mediaType || item.media_type);
    },

    async openDrawerFromDB(dbItem) {
        const basic = {
            id: dbItem.id, mediaType: dbItem.media_type, title: dbItem.title, posterUrl: dbItem.image_url,
            year: dbItem.date, score: dbItem.score, totalEpisodes: dbItem.total_episodes, totalSeasons: dbItem.total_seasons,
            provider: dbItem.director_studio ? 'tmdb' : null
        };
        this.s.heroPaused = true; // Pause hero carousel while modal is open
        this.s.drawerItem = basic;
        this.s.drawerDBEntry = dbItem;
        this.s.drawerWatchedEpisodes = [];
        if (!this.s.isGuest && this.s.user) {
            this.s.drawerWatchedEpisodes = await SBWatchedEpisodes.getForMedia(this.s.user.id, dbItem.id, dbItem.media_type);
        }
        UI.renderDrawer(basic, dbItem, this.s.drawerWatchedEpisodes);
        document.getElementById('detailModal').classList.add('active');
        document.body.style.overflow = 'hidden'; // prevent background scroll

        try {
            if (dbItem.id > 0) {
                const full = await API.fetchDetails(dbItem.id, dbItem.media_type, basic.provider);
                if (full) {
                    this.s.drawerItem = { ...basic, ...full };
                    UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry, this.s.drawerWatchedEpisodes);
                }
            }
        } catch {}

        // Phase 3C: Fetch and render More Like This row after details load
        UI._fetchAndRenderSimilar(dbItem.id, dbItem.media_type);
    },

    closeDrawer() {
        document.getElementById('detailModal').classList.remove('active');
        document.body.style.overflow = ''; // restore scroll
        // Resume hero carousel (only if not hidden by tab switch)
        if (!document.hidden) {
            this.s.heroPaused = false;
        }
    },

    async addToWatchlist(item, listName) {
        if (this.s.isGuest) return UI.toast('AUTH REQUIRED', 'error');
        try {
            // Ensure full details (runtime, genres, etc.) are fetched for stats before saving
            if (!item.runtime && item.id) {
                const full = await API.fetchDetails(item.id, item.mediaType || item.media_type, item.provider);
                if (full) item = { ...item, ...full };
            }
            const [row] = await SBList.add(this.s.user.id, item, listName);
            const idx = this.s.watchlist.findIndex(w => w.id === row.id && w.media_type === row.media_type);
            if (idx >= 0) this.s.watchlist[idx] = row;
            else this.s.watchlist.unshift(row);

            if (listName === 'Watched') {
                await this.markAllEpisodesWatched(item, row);
            }

            this.s.drawerItem = item;
            this.s.drawerDBEntry = row;
            UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry, this.s.drawerWatchedEpisodes);
            this._updateWLUI();
            this._reindexSearch();
            UI.toast('TARGET ACQUIRED');
        } catch (e) {
            console.error('addToWatchlist failed:', e);
            UI.toast('SYSTEM ERROR', 'error');
        }
    },

    async removeFromWatchlist(id, type) {
        if (this.s.isGuest) return;
        try {
            await SBList.remove(this.s.user.id, id, type);
            this.s.watchlist = this.s.watchlist.filter(w => !(w.id === id && w.media_type === type));
            this.s.drawerDBEntry = null;
            this.s.drawerWatchedEpisodes = [];
            UI.renderDrawer(this.s.drawerItem, null, []);
            this._updateWLUI();
            this._reindexSearch();
            UI.toast('TARGET PURGED');
        } catch (e) { UI.toast('SYSTEM ERROR', 'error'); }
    },

    async markAllEpisodesWatched(item, dbEntry) {
        if (!dbEntry || this.s.isGuest || !this.s.user) return;
        const mediaType = (dbEntry.media_type || item?.mediaType || '').toLowerCase();
        if (mediaType !== 'tv' && mediaType !== 'anime') return;

        let totalSeasons = item?.totalSeasons || dbEntry.total_seasons || 1;
        let isAnime = mediaType === 'anime' || item?.provider === 'anilist' || item?.provider === 'jikan' || item?.provider === 'kitsu';

        const currentData = this.s.drawerWatchedEpisodes || [];
        if (currentData.length > 0) {
            await SBList.update(this.s.user.id, dbEntry.id, dbEntry.media_type, { progress_backup: JSON.stringify(currentData) });
            dbEntry.progress_backup = JSON.stringify(currentData);
        }

        const episodesToAdd = [];

        if (isAnime) {
            let epCount = dbEntry.total_episodes || item?.totalEpisodes || 0;
            for (let ep = 1; ep <= epCount; ep++) {
                episodesToAdd.push({ season_number: 1, episode_number: ep });
            }
        } else {
            const seasonPromises = [];
            for (let s = 1; s <= totalSeasons; s++) {
                seasonPromises.push(API.fetchTVSeasonDetails(dbEntry.id, s).catch(() => null));
            }
            const seasonsData = await Promise.all(seasonPromises);
            for (let s = 1; s <= totalSeasons; s++) {
                const seasonData = seasonsData[s - 1];
                let epCount = seasonData?.episodes?.length || 0;
                for (let ep = 1; ep <= epCount; ep++) {
                    episodesToAdd.push({ season_number: s, episode_number: ep });
                }
            }
        }

        if (episodesToAdd.length > 0) {
            await SBWatchedEpisodes.addBulk(this.s.user.id, dbEntry.id, dbEntry.media_type, episodesToAdd);
        }

        this.s.drawerWatchedEpisodes = JSON.parse(
            localStorage.getItem(`loopa_episodes_${this.s.user.id}_${dbEntry.id}`) || '[]'
        );
        const newCount = this.s.drawerWatchedEpisodes.length;
        const idx = this.s.watchlist.findIndex(w => w.id === dbEntry.id && w.media_type === dbEntry.media_type);
        if (idx >= 0) {
            this.s.watchlist[idx].current_episode = newCount;
            this.s.drawerDBEntry.current_episode = newCount;
        }
    },

    async markSeasonWatched(item, dbEntry, seasonNum, epCount) {
        if (!dbEntry || this.s.isGuest || !epCount) return;
        const episodesToAdd = [];
        for (let ep = 1; ep <= epCount; ep++) {
            episodesToAdd.push({ season_number: seasonNum, episode_number: ep });
        }
        await SBWatchedEpisodes.addBulk(this.s.user.id, dbEntry.id, dbEntry.media_type, episodesToAdd);
        
        this.s.drawerWatchedEpisodes = await SBWatchedEpisodes.getForMedia(this.s.user.id, dbEntry.id, dbEntry.media_type);
        const newCount = this.s.drawerWatchedEpisodes.length;
        const idx = this.s.watchlist.findIndex(w => w.id === dbEntry.id && w.media_type === dbEntry.media_type);
        
        // Auto-shift status
        const totalEps = dbEntry.total_episodes || item?.totalEpisodes || 0;
        let newStatus = dbEntry.list_name;
        if (newCount > 0 && newCount < totalEps) newStatus = 'Watching';
        else if (newCount > 0 && newCount >= totalEps) newStatus = 'Watched';
        else if (newCount === 0) newStatus = 'Watching';

        if (newStatus !== dbEntry.list_name) {
            await this.updateStatus(newStatus);
        }

        if (idx >= 0) {
            this.s.watchlist[idx].current_episode = newCount;
            this.s.drawerDBEntry.current_episode = newCount;
        }
        UI.renderDrawer(item, dbEntry, this.s.drawerWatchedEpisodes);
        UI.toast(`SEASON ${seasonNum} WATCHED`);
    },

    async updateStatus(status) {
        const e = this.s.drawerDBEntry;
        if (!e || this.s.isGuest) return;
        try {
            const prevStatus = e.list_name;
            await SBList.update(this.s.user.id, e.id, e.media_type, { list_name: status });
            e.list_name = status;
            const idx = this.s.watchlist.findIndex(w => w.id === e.id && w.media_type === e.media_type);
            if (idx >= 0) this.s.watchlist[idx].list_name = status;
            
            if (status === 'Watched') {
                await this.markAllEpisodesWatched(this.s.drawerItem, e);
            } else if (status === 'Watching' && prevStatus === 'Watched') {
                const backupRaw = e.progress_backup;
                if (backupRaw) {
                    await SBWatchedEpisodes.removeAll(this.s.user.id, e.id, e.media_type);
                    const backup = JSON.parse(backupRaw);
                    if (backup.length > 0) {
                        await SBWatchedEpisodes.addBulk(this.s.user.id, e.id, e.media_type, backup);
                    }
                    this.s.drawerWatchedEpisodes = backup;
                    e.current_episode = backup.length;
                    if (idx >= 0) this.s.watchlist[idx].current_episode = backup.length;
                    
                    await SBList.update(this.s.user.id, e.id, e.media_type, { progress_backup: null });
                    e.progress_backup = null;
                }
            }

            UI.renderDrawer(this.s.drawerItem, e, this.s.drawerWatchedEpisodes);
            UI.toast('STATUS UPDATED');
        } catch (err) {}
    },

    // updateProgress removed as requested in redesign

    async toggleEpisodeWatched(seasonNum, episodeNum, isWatched) {
        const e = this.s.drawerDBEntry;
        if (!e || this.s.isGuest) return;
        try {
            if (isWatched) {
                await SBWatchedEpisodes.add(this.s.user.id, e.id, e.media_type, seasonNum, episodeNum);
            } else {
                await SBWatchedEpisodes.remove(this.s.user.id, e.id, e.media_type, seasonNum, episodeNum);
            }
            // Refresh local episode count
            this.s.drawerWatchedEpisodes = await SBWatchedEpisodes.getForMedia(this.s.user.id, e.id, e.media_type);
            const newCount = this.s.drawerWatchedEpisodes.length;
            const idx = this.s.watchlist.findIndex(w => w.id === e.id && w.media_type === e.media_type);
            
            // Auto-shift status
            const totalEps = e.total_episodes || this.s.drawerItem?.totalEpisodes || 0;
            let newStatus = e.list_name;
            if (newCount > 0 && newCount < totalEps) newStatus = 'Watching';
            else if (newCount > 0 && newCount >= totalEps) newStatus = 'Watched';
            else if (newCount === 0) newStatus = 'Watching';

            if (newStatus !== e.list_name) {
                await this.updateStatus(newStatus);
            }

            if (idx >= 0) {
                this.s.watchlist[idx].current_episode = newCount;
                this.s.drawerDBEntry.current_episode = newCount;
            }
        } catch (err) {
            console.error(err);
        }
    },

    async setRating(rating) {
        const e = this.s.drawerDBEntry;
        if (!e || this.s.isGuest) return;
        e.user_rating = rating;
        
        const stars = document.getElementById('starRating').children;
        Array.from(stars).forEach((star, i) => {
            const val = (i + 1) * 2;
            const active = rating >= val - 1;
            star.className = `fa-solid fa-star cursor-pointer transition-colors ${active ? 'text-neonOrange drop-shadow-[0_0_8px_rgba(255,69,0,0.8)]' : 'text-gray-700 hover:text-gray-400'}`;
        });

        try {
            await SBList.update(this.s.user.id, e.id, e.media_type, { user_rating: rating });
            UI.toast(`RATED ${rating}/10`);
        } catch {}
    },

    async updateNotes(notes) {
        const e = this.s.drawerDBEntry;
        if (!e || this.s.isGuest) return;
        try {
            await SBList.update(this.s.user.id, e.id, e.media_type, { personal_notes: notes });
            e.personal_notes = notes;
            const idx = this.s.watchlist.findIndex(w => w.id === e.id && w.media_type === e.media_type);
            if (idx >= 0) this.s.watchlist[idx].personal_notes = notes;
            UI.toast('NOTES SAVED');
        } catch (err) {
            UI.toast('FAILED TO SAVE NOTES', 'error');
        }
    },

    likeRecommendation(title) {
        const liked = JSON.parse(localStorage.getItem('oracle_liked_titles') || '[]');
        if (!liked.includes(title)) {
            liked.push(title);
            localStorage.setItem('oracle_liked_titles', JSON.stringify(liked));
        }
        UI.toast('LOGGED TO ORACLE CORE');
    },

    dislikeRecommendation(title) {
        const disliked = JSON.parse(localStorage.getItem('oracle_disliked_titles') || '[]');
        if (!disliked.includes(title)) {
            disliked.push(title);
            localStorage.setItem('oracle_disliked_titles', JSON.stringify(disliked));
        }
        UI.toast('Removed from recommendations');
    },

    showContextMenu(e, item, inList) {
        const cm = document.getElementById('contextMenu');
        if (!cm) return;

        cm.innerHTML = '';
        
        const dbItem = this.s.watchlist.find(d => String(d.tmdb_id) === String(item.id) || String(d.id) === String(item.id));
        const actualInList = inList || !!dbItem;
        const actualItem = dbItem || item;
        const listName = actualItem.list_name || '';

        // View Details
        const btnDetails = document.createElement('div');
        btnDetails.className = 'px-4 py-2 hover:bg-loopRaised cursor-pointer flex items-center gap-3 transition-colors';
        btnDetails.innerHTML = '<i class="fa-solid fa-circle-info w-4 text-center text-textMuted"></i> <span>View Details</span>';
        btnDetails.onclick = () => { this.closeContextMenu(); actualInList ? this.openDrawerFromDB(actualItem) : this.openDrawer(item); };
        cm.appendChild(btnDetails);

        if (!actualInList && !this.s.isGuest) {
            const btnAdd = document.createElement('div');
            btnAdd.className = 'px-4 py-2 hover:bg-loopRaised cursor-pointer flex items-center gap-3 transition-colors';
            btnAdd.innerHTML = '<i class="fa-solid fa-plus w-4 text-center text-loopAmber"></i> <span>Add to Watchlist</span>';
            btnAdd.onclick = () => { this.closeContextMenu(); this.addToWatchlist(item, 'Watching'); };
            cm.appendChild(btnAdd);
        } else if (actualInList && !this.s.isGuest) {
            if (listName !== 'Watched') {
                const btnWatched = document.createElement('div');
                btnWatched.className = 'px-4 py-2 hover:bg-loopRaised cursor-pointer flex items-center gap-3 transition-colors';
                btnWatched.innerHTML = '<i class="fa-solid fa-check w-4 text-center text-loopSuccess"></i> <span>Mark as Watched</span>';
                btnWatched.onclick = () => { 
                    this.closeContextMenu(); 
                    this.s.drawerDBEntry = actualItem; // Temporarily set for updateStatus
                    this.updateStatus('Watched'); 
                    this.s.drawerDBEntry = null;
                };
                cm.appendChild(btnWatched);
            }

            const btnRemove = document.createElement('div');
            btnRemove.className = 'px-4 py-2 hover:bg-loopRaised cursor-pointer flex items-center gap-3 transition-colors text-loopError';
            btnRemove.innerHTML = '<i class="fa-solid fa-trash w-4 text-center"></i> <span>Remove from Watchlist</span>';
            btnRemove.onclick = () => { this.closeContextMenu(); this.removeFromWatchlist(actualItem.id, actualItem.media_type || actualItem.mediaType); };
            cm.appendChild(btnRemove);
        }

        cm.classList.remove('hidden');
        
        // Calculate position
        const rect = cm.getBoundingClientRect();
        let x = e.clientX;
        let y = e.clientY;
        
        if (x + 192 > window.innerWidth) x = window.innerWidth - 192 - 8;
        if (y + cm.offsetHeight > window.innerHeight) y = window.innerHeight - cm.offsetHeight - 8;

        cm.style.left = `${x}px`;
        cm.style.top = `${y}px`;

        // Force reflow before animating
        void cm.offsetWidth;
        cm.classList.remove('opacity-0', 'pointer-events-none');
        cm.classList.add('opacity-100');
    },

    closeContextMenu() {
        const cm = document.getElementById('contextMenu');
        if (cm) {
            cm.classList.remove('opacity-100');
            cm.classList.add('opacity-0', 'pointer-events-none');
            setTimeout(() => cm.classList.add('hidden'), 200);
        }
    },

    scrollRow(id, dir) {
        const row = document.getElementById(id);
        if (row) {
            const amount = row.clientWidth * 0.75;
            row.scrollBy({
                left: dir === 'left' ? -amount : amount,
                behavior: 'smooth'
            });
        }
    }
};

/**
 * PWA Installation Controller
 * Handles manual PWA installation triggers from settings and native browser integration.
 */
const PWAInstaller = {
    deferredPrompt: null,
    isStandalone: false,
    isIOS: false,

    init() {
        this.isStandalone = window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
        this.isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;

        // If already installed and running standalone, suppress prompts
        if (this.isStandalone) {
            console.log('[PWA] Running in standalone mode');
            this.hideAll();
            return;
        }

        // 1. Capture beforeinstallprompt event
        window.addEventListener('beforeinstallprompt', (e) => {
            e.preventDefault();
            this.deferredPrompt = e;
            console.log('[PWA] beforeinstallprompt captured');

            // Show install option in settings dropdown
            const btnDropdownInstall = document.getElementById('btnInstallApp');
            if (btnDropdownInstall) {
                btnDropdownInstall.classList.remove('hidden');
            }
        });

        // 2. Listen for appinstalled
        window.addEventListener('appinstalled', () => {
            this.deferredPrompt = null;
            this.hideAll();
            UI.toast('LOOPA INSTALLED SUCCESSFULLY!');
            console.log('[PWA] App installed successfully');
        });

        this.bindEvents();
    },

    bindEvents() {
        const btnDropdownInstall = document.getElementById('btnInstallApp');
        const btnCloseIos = document.getElementById('btnCloseIosInstall');
        const modalIos = document.getElementById('iosInstallModal');

        if (btnDropdownInstall) {
            btnDropdownInstall.addEventListener('click', () => {
                const settingsDropdown = document.getElementById('settingsDropdown');
                if (settingsDropdown) settingsDropdown.classList.add('hidden');
                this.triggerInstall();
            });
        }

        if (btnCloseIos) {
            btnCloseIos.addEventListener('click', () => this.hideIosModal());
        }

        if (modalIos) {
            modalIos.addEventListener('click', (e) => {
                if (e.target === modalIos) this.hideIosModal();
            });
        }
    },

    showIosModal() {
        const modal = document.getElementById('iosInstallModal');
        if (modal) {
            modal.classList.remove('opacity-0', 'pointer-events-none');
            modal.classList.add('opacity-100');
        }
    },

    hideIosModal() {
        const modal = document.getElementById('iosInstallModal');
        if (modal) {
            modal.classList.remove('opacity-100');
            modal.classList.add('opacity-0', 'pointer-events-none');
        }
    },

    async triggerInstall() {
        if (this.isStandalone) {
            UI.toast('LOOPA IS ALREADY INSTALLED');
            return;
        }

        if (this.deferredPrompt) {
            try {
                this.deferredPrompt.prompt();
                const { outcome } = await this.deferredPrompt.userChoice;
                if (outcome === 'accepted') {
                    UI.toast('INSTALLING LOOPA...');
                } else {
                    console.log('[PWA] User dismissed prompt');
                }
                this.deferredPrompt = null;
            } catch (err) {
                console.warn('[PWA] Prompt error:', err);
            }
        } else if (this.isIOS) {
            this.showIosModal();
        } else {
            // For desktop/android browsers before or without beforeinstallprompt
            UI.toast('USE BROWSER MENU (⋮) > "INSTALL APP"');
        }
    },

    hideAll() {
        const settingsBtn = document.getElementById('btnInstallApp');
        if (settingsBtn && this.isStandalone) {
            settingsBtn.parentElement?.parentElement?.classList.add('hidden');
        }
        this.hideIosModal();
    }
};

document.addEventListener('DOMContentLoaded', () => App.init());

