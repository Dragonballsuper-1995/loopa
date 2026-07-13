/**
 * Loopa Web — Application Controller
 * Manages state and integrates API, Supabase, and UI for the Loopa design.
 */

const App = {
    s: {
        user: null,
        isGuest: false,
        view: 'home',
        watchlist: [],
        searchResults: [],
        searchFilter: 'all',
        wlFilter: 'all',
        drawerItem: null,
        drawerDBEntry: null,
        searchDebounce: null,
        aiLoaded: false,
        aiRecommendations: [],
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
    },

    _boot() {
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
            SBList.subscribeToChanges(
                this.s.user.id,
                async () => {
                    this.s.watchlist = await SBList.getAll(this.s.user.id);
                    if(this.s.view === 'watchlist') this._updateWLUI();
                },
                async (newRow) => {
                    this.s.watchlist = await SBList.getAll(this.s.user.id);
                    if(this.s.view === 'watchlist') this._updateWLUI();
                    if(this.s.drawerDBEntry && this.s.drawerDBEntry.id === newRow.id) {
                        this.s.drawerDBEntry = newRow;
                        UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry);
                    }
                },
                async (oldRow) => {
                    this.s.watchlist = await SBList.getAll(this.s.user.id);
                    if(this.s.view === 'watchlist') this._updateWLUI();
                    if(this.s.drawerDBEntry && this.s.drawerDBEntry.id === oldRow.id) {
                        this.s.drawerDBEntry = null;
                        UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry);
                    }
                }
            );
        }
        
        this.navigateTo('home');
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
            searchInput.addEventListener('input', e => {
                const q = e.target.value.trim();
                clearTimeout(this.s.searchDebounce);
                const grid = document.getElementById('searchResults');
                const state = document.getElementById('searchState');
                
                if (q.length < 2) {
                    if (grid) grid.innerHTML = '';
                    if (state) {
                        state.classList.remove('hidden');
                        state.innerHTML = '<i class="fa-solid fa-radar text-4xl text-gray-800 mb-4 animate-pulse"></i><p class="font-headers text-2xl text-gray-600">AWAITING INPUT</p>';
                    }
                    this.s.searchResults = [];
                    return;
                }
                if (state) state.classList.add('hidden');
                if (grid) grid.innerHTML = UI.skeletonGrid(8);
                this.s.searchDebounce = setTimeout(() => this._doSearch(q), 420);
            });
        }

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
                    b.classList.remove('bg-neonOrange', 'text-white');
                    b.classList.add('bg-cineSurface', 'text-gray-400');
                });
                btn.classList.remove('bg-cineSurface', 'text-gray-400');
                btn.classList.add('bg-neonOrange', 'text-white');
                this.s.wlFilter = btn.dataset.wf;
                const list = this.s.wlFilter === 'all' ? this.s.watchlist : this.s.watchlist.filter(i => i.list_name === this.s.wlFilter);
                UI.renderWatchlistGrid(list);
            });
        });

        // AI
        const btnRefreshAI = document.getElementById('btnRefreshAI');
        if (btnRefreshAI) btnRefreshAI.addEventListener('click', () => this._loadAI(true));
 
        // Modals
        const btnModalClose = document.getElementById('btnModalClose');
        if (btnModalClose) btnModalClose.addEventListener('click', () => this.closeDrawer());
        
        const detailModalBg = document.getElementById('detailModalBg');
        if (detailModalBg) detailModalBg.addEventListener('click', () => this.closeDrawer());
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
    navigateTo(view) {
        if (view !== 'home') {
            clearInterval(this.s.heroInterval);
        }
        this.s.view = view;
        ['home', 'search', 'watchlist', 'ai'].forEach(v => {
            document.getElementById(`tab-${v}`).classList.add('hidden');
        });
        document.getElementById(`tab-${view}`).classList.remove('hidden');

        document.querySelectorAll('.nav-btn').forEach(btn => {
            const isActive = btn.dataset.nav === view;
            if (isActive) {
                btn.classList.add('bg-neonOrange', 'text-white');
                btn.classList.remove('bg-transparent', 'text-gray-400', 'hover:bg-white/10', 'hover:text-white', 'hover:text-vibrantCyan');
            } else {
                btn.classList.remove('bg-neonOrange', 'text-white');
                btn.classList.add('bg-transparent', 'text-gray-400', 'hover:bg-white/10');
                if (btn.dataset.nav === 'ai') btn.classList.add('hover:text-vibrantCyan');
                else btn.classList.add('hover:text-white');
            }
        });

        if (view === 'home') this._loadDashboard();
        if (view === 'watchlist') this._loadTerminal();
        if (view === 'ai' && !this.s.aiLoaded) this._loadAI();
    },

    // ── Data Loading ──────────────────────────────────────────────────────────
    async _loadDashboard() {
        document.getElementById('row-trending').innerHTML = UI.skeletonRow(6);
        document.getElementById('row-anime').innerHTML = UI.skeletonRow(6);
        document.getElementById('row-movies').innerHTML = UI.skeletonRow(6);
        document.getElementById('row-tv').innerHTML = UI.skeletonRow(6);

        const [trending, anime, movies, tv, wl] = await Promise.allSettled([
            API.fetchTrending(), API.fetchTopAnime(), API.fetchPopularMovies(), API.fetchPopularTV(),
            this.s.isGuest ? Promise.resolve([]) : SBList.getAll(this.s.user.id)
        ]);

        const t = trending.status === 'fulfilled' ? trending.value : [];
        if (wl.status === 'fulfilled') {
            this.s.watchlist = wl.value;
            if (!this.s.isGuest) {
                localStorage.setItem('lastSyncTime', Date.now());
                this._updateSyncTime();
            }
        }

        const heroItems = t.filter(i => i.backdropUrl || i.posterUrl).slice(0, 5);
        const watching = this.s.watchlist.find(i => i.list_name === 'Watching');
        
        clearInterval(this.s.heroInterval);
        if (heroItems.length) {
            let heroIndex = 0;
            UI.renderHero(heroItems[heroIndex], watching);
            this.s.heroInterval = setInterval(() => {
                heroIndex = (heroIndex + 1) % heroItems.length;
                UI.renderHero(heroItems[heroIndex], watching);
            }, 5000);
        }

        if (t.length) UI.renderScrollRow('row-trending', t.slice(0, 10));
        if (anime.status === 'fulfilled') UI.renderScrollRow('row-anime', anime.value.slice(0, 10));
        if (movies.status === 'fulfilled') UI.renderScrollRow('row-movies', movies.value.slice(0, 10));
        if (tv.status === 'fulfilled') UI.renderScrollRow('row-tv', tv.value.slice(0, 10));
    },

    async _doSearch(q) {
        try {
            const res = await API.searchAll(q);
            this.s.searchResults = res;
            const list = this.s.searchFilter === 'all' ? res : res.filter(i => i.mediaType === this.s.searchFilter);
            UI.renderSearchGrid(list);
        } catch (err) {
            document.getElementById('searchResults').innerHTML = '';
            document.getElementById('searchState').classList.remove('hidden');
            document.getElementById('searchState').innerHTML = `<p class="font-headers text-xl text-red-500">ERROR: ${err.message}</p>`;
        }
    },

    async _loadTerminal() {
        if (this.s.isGuest) return this._updateWLUI();
        document.getElementById('watchlistGrid').innerHTML = UI.skeletonGrid(6);
        try {
            this.s.watchlist = await SBList.getAll(this.s.user.id);
            localStorage.setItem('lastSyncTime', Date.now());
            this._updateSyncTime();
            this._updateWLUI();
        } catch (err) {
            UI.toast('SYNC FAILED', 'error');
        }
    },

    _updateWLUI() {
        document.getElementById('watchlistCount').textContent = `${this.s.watchlist.length} TARGETS ACQUIRED`;
        const list = this.s.wlFilter === 'all' ? this.s.watchlist : this.s.watchlist.filter(i => i.list_name === this.s.wlFilter);
        UI.renderWatchlistGrid(list);
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

        btnSend.addEventListener('click', sendMsg);
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendMsg();
        });
        
        const btnClear = document.getElementById('btnAiClearChat');
        if(btnClear) {
            btnClear.addEventListener('click', () => {
                this.s.chatHistory = [];
                document.getElementById('aiChatLog').innerHTML = `
                    <div class="flex flex-col gap-2 w-full max-w-4xl animate-fade-in-up">
                        <div class="flex items-center gap-3">
                            <div class="w-8 h-8 rounded-full bg-loopSurface border border-loopAmber/20 flex items-center justify-center text-loopAmber"><i class="fa-solid fa-wand-magic-sparkles text-[10px]"></i></div>
                            <span class="text-sm font-bold text-textPrimary">Loopa AI</span>
                        </div>
                        <div class="bg-loopSurface/60 border border-white/5 rounded-2xl rounded-tl-none p-4 text-textPrimary text-sm leading-relaxed inline-block self-start">
                            Hello! I have access to your watchlist. What kind of movie, show, or anime are you in the mood for today?
                        </div>
                    </div>
                `;
            });
        }
    },

    async _handleAiChat(message) {
        if (this.s.isAiThinking) return;
        this.s.isAiThinking = true;

        const chatLog = document.getElementById('aiChatLog');
        
        // Render User Message
        chatLog.appendChild(UI.renderAiMessage('user', message));
        chatLog.scrollTop = chatLog.scrollHeight;
        
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
            if (!this.s.watchlist.length) this.s.watchlist = await SBList.getAll(this.s.user.id);
            let targets = this.s.watchlist.filter(i => i.list_name === 'Watched');
            if (targets.length === 0) targets = this.s.watchlist;
            
            const liked = JSON.parse(localStorage.getItem('oracle_liked_titles') || '[]');
            const disliked = JSON.parse(localStorage.getItem('oracle_disliked_titles') || '[]');

            // Push to history
            this.s.chatHistory.push({ role: 'user', content: message });
            
            const recs = await API.getAIRecommendations(targets, liked, disliked, this.s.chatHistory);
            
            // Enrich results
            const enriched = await Promise.allSettled(recs.map(async rec => {
                let r;
                if(rec.type==='anime') r = await API.searchAnime(rec.title);
                else if(rec.type==='movie') r = await API.searchMovies(rec.title);
                else r = await API.searchTV(rec.title);
                return r[0] ? {...r[0], reason: rec.reason} : null;
            }));

            const valid = enriched.filter(r => r.status === 'fulfilled' && r.value).map(r => r.value);
            
            // Generate response text
            let responseText = "Here are some recommendations based on what you asked:";
            if (valid.length === 0) responseText = "I couldn't find any good matches for that. Could you try asking in a different way?";
            
            this.s.chatHistory.push({ role: 'model', content: responseText });
            
            // Remove loading
            chatLog.removeChild(loadingDiv);
            
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
            chatLog.removeChild(loadingDiv);
            chatLog.appendChild(UI.renderAiMessage('model', `Sorry, I encountered an error: ${err.message}`));
            chatLog.scrollTop = chatLog.scrollHeight;
        } finally {
            this.s.isAiThinking = false;
        }
    },

    // ── Drawer & Watchlist Actions ────────────────────────────────────────────
    async openDrawer(item) {
        this.s.drawerItem = item;
        this.s.drawerDBEntry = this.s.watchlist.find(w => w.id === item.id && w.media_type === item.mediaType) || null;
        UI.renderDrawer(item, this.s.drawerDBEntry);
        document.getElementById('detailModal').classList.add('active');

        try {
            if (item.id > 0) {
                const full = await API.fetchDetails(item.id, item.mediaType);
                if (full) {
                    this.s.drawerItem = { ...item, ...full };
                    UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry);
                }
            }
        } catch {}
    },

    async openDrawerFromDB(dbItem) {
        const basic = {
            id: dbItem.id, mediaType: dbItem.media_type, title: dbItem.title, posterUrl: dbItem.image_url,
            year: dbItem.date, score: dbItem.score, totalEpisodes: dbItem.total_episodes, totalSeasons: dbItem.total_seasons
        };
        this.s.drawerItem = basic;
        this.s.drawerDBEntry = dbItem;
        UI.renderDrawer(basic, dbItem);
        document.getElementById('detailModal').classList.add('active');

        try {
            if (dbItem.id > 0) {
                const full = await API.fetchDetails(dbItem.id, dbItem.media_type);
                if (full) {
                    this.s.drawerItem = { ...basic, ...full };
                    UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry);
                }
            }
        } catch {}
    },

    closeDrawer() {
        document.getElementById('detailModal').classList.remove('active');
    },

    async addToWatchlist(item, listName) {
        if (this.s.isGuest) return UI.toast('AUTH REQUIRED', 'error');
        try {
            await SBList.add(this.s.user.id, item, listName);
            this.s.watchlist = await SBList.getAll(this.s.user.id);
            this.s.drawerDBEntry = this.s.watchlist.find(w => w.id === item.id && w.media_type === item.mediaType);
            UI.renderDrawer(this.s.drawerItem, this.s.drawerDBEntry);
            this._updateWLUI();
            UI.toast('TARGET ACQUIRED');
        } catch (e) { UI.toast('SYSTEM ERROR', 'error'); }
    },

    async removeFromWatchlist(id, type) {
        if (this.s.isGuest) return;
        try {
            await SBList.remove(this.s.user.id, id, type);
            this.s.watchlist = this.s.watchlist.filter(w => !(w.id === id && w.media_type === type));
            this.s.drawerDBEntry = null;
            UI.renderDrawer(this.s.drawerItem, null);
            this._updateWLUI();
            UI.toast('TARGET PURGED');
        } catch (e) { UI.toast('SYSTEM ERROR', 'error'); }
    },

    async updateStatus(status) {
        const e = this.s.drawerDBEntry;
        if (!e || this.s.isGuest) return;
        try {
            await SBList.update(this.s.user.id, e.id, e.media_type, { list_name: status });
            e.list_name = status;
            const idx = this.s.watchlist.findIndex(w => w.id === e.id && w.media_type === e.media_type);
            if (idx >= 0) this.s.watchlist[idx].list_name = status;
            UI.renderDrawer(this.s.drawerItem, e);
            UI.toast('STATUS UPDATED');
        } catch (err) {}
    },

    async updateProgress(field, delta) {
        const e = this.s.drawerDBEntry;
        if (!e || this.s.isGuest) return;
        if (field === 'episode') {
            e.current_episode = Math.max(0, (e.current_episode || 0) + delta);
            document.getElementById('progDisplay').textContent = `E ${e.current_episode}`;
            
            try {
                await SBList.update(this.s.user.id, e.id, e.media_type, { current_episode: e.current_episode });
                const idx = this.s.watchlist.findIndex(w => w.id === e.id && w.media_type === e.media_type);
                if (idx >= 0) this.s.watchlist[idx].current_episode = e.current_episode;
            } catch {}
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
            btnAdd.onclick = () => { this.closeContextMenu(); this.addToWatchlist(item, 'To Watch'); };
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

document.addEventListener('DOMContentLoaded', () => App.init());
