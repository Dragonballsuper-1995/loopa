/**
 * Loopa Web — UI Rendering Layer v2.0
 * Design System: Warm dark palette, DM Sans, Loopa Amber (#E8A87C)
 */

const UI = {
    _escapeHTML(str) {
        if (!str) return '';
        return String(str).replace(/[&<>"']/g, (m) => {
            switch (m) {
                case '&': return '&amp;';
                case '<': return '&lt;';
                case '>': return '&gt;';
                case '"': return '&quot;';
                case "'": return '&#039;';
                default: return m;
            }
        });
    },

    // ── Toast ─────────────────────────────────────────────────────────────────
    _toastTimer: null,
    toast(message, type = 'success') {
        const el = document.getElementById('toast');
        const msgEl = document.getElementById('toastMsg');

        if (type === 'error') {
            el.className = 'fixed top-20 left-1/2 -translate-x-1/2 px-5 py-3 rounded-full text-sm font-semibold transform z-[200] flex items-center gap-2 pointer-events-none shadow-lg border opacity-100 translate-y-0 transition-all duration-300 bg-loopSurface text-loopError border-loopError/30';
            msgEl.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> ${this._escapeHTML(message)}`;
        } else {
            el.className = 'fixed top-20 left-1/2 -translate-x-1/2 px-5 py-3 rounded-full text-sm font-semibold transform z-[200] flex items-center gap-2 pointer-events-none shadow-lg border opacity-100 translate-y-0 transition-all duration-300 bg-loopSurface text-loopAmber border-loopAmber/30';
            msgEl.innerHTML = `<i class="fa-solid fa-check"></i> ${this._escapeHTML(message)}`;
        }

        clearTimeout(this._toastTimer);
        this._toastTimer = setTimeout(() => {
            el.classList.add('opacity-0', '-translate-y-3');
            el.classList.remove('opacity-100', 'translate-y-0');
        }, 2500);
    },

    renderAiMessage(role, content) {
        const div = document.createElement('div');
        div.className = `flex flex-col gap-2 w-full max-w-4xl animate-fade-in-up ${role === 'user' ? 'self-end items-end' : 'self-start items-start'}`;
        
        if (role === 'user') {
            div.innerHTML = `
                <div class="flex items-center gap-3 flex-row-reverse">
                    <div class="w-8 h-8 rounded-full bg-loopSurface border border-white/10 flex items-center justify-center text-textSecondary"><i class="fa-solid fa-user text-xs"></i></div>
                    <span class="text-sm font-bold text-textPrimary">You</span>
                </div>
                <div class="bg-loopRaised/60 border border-white/5 rounded-2xl rounded-tr-none p-4 text-textPrimary text-sm leading-relaxed inline-block">
                    ${this._escapeHTML(content)}
                </div>
            `;
        } else {
            div.innerHTML = `
                <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full bg-loopSurface border border-loopAmber/20 flex items-center justify-center text-loopAmber"><i class="fa-solid fa-wand-magic-sparkles text-[10px]"></i></div>
                    <span class="text-sm font-bold text-textPrimary">Loopa AI</span>
                </div>
                <div class="bg-loopSurface/60 border border-white/5 rounded-2xl rounded-tl-none p-4 text-textPrimary text-sm leading-relaxed inline-block">
                    ${this._escapeHTML(content)}
                </div>
            `;
        }
        return div;
    },

    // ── Skeletons ─────────────────────────────────────────────────────────────
    skeletonRow(count = 5) {
        return Array.from({ length: count }, () =>
            `<div class="w-[160px] md:w-[200px] h-[240px] md:h-[300px] bg-loopSurface rounded-lg shrink-0 skeleton"></div>`
        ).join('');
    },

    skeletonGrid(count = 8) {
        return Array.from({ length: count }, () =>
            `<div class="w-full aspect-[2/3] bg-loopSurface rounded-lg skeleton"></div>`
        ).join('');
    },

    // ── Micro-interactions ────────────────────────────────────────────────────
    _applyTiltEffect(el) {
        // 3D Tilt removed
    },

    // ── Helpers ───────────────────────────────────────────────────────────────
    _fallbackPoster(title) {
        return `https://placehold.co/400x600/1A1915/A09990?text=${encodeURIComponent((title || '?').substring(0, 12))}`;
    },

    _typeBadgeClass(type) {
        if (type === 'anime') return 'text-loopAmber';
        if (type === 'tv')    return 'text-textSecondary';
        return 'text-textPrimary';
    },

    _statusBadgeClass(status) {
        if (status === 'Watching') return 'status-badge-watching';
        if (status === 'Watched')  return 'status-badge-watched';
        return 'status-badge-planned';
    },

    // ── Card (Horizontal Row) ─────────────────────────────────────────────────
    posterCardRow(item, onClick) {
        const src  = item.posterUrl || item.image_url || this._fallbackPoster(item.title);
        const type = item.mediaType || item.media_type || '';

        // ── Step 2.4: In-list status badge ────────────────────────────────────
        const wlEntry = (typeof App !== 'undefined' && App.s && App.s.watchlist)
            ? App.s.watchlist.find(w =>
                w.id === item.id &&
                w.media_type === (item.mediaType || item.media_type))
            : null;

        let statusBadge = '';
        if (wlEntry && wlEntry.list_name) {
            const badgeClass = this._statusBadgeClass(wlEntry.list_name);
            const icon = wlEntry.list_name === 'Watching' ? 'fa-play' :
                         wlEntry.list_name === 'Watched'  ? 'fa-check' : 'fa-clock';
            statusBadge = `
                <div class="absolute top-2 left-2 z-20 bg-loopBase/90 backdrop-blur-sm px-2 py-0.5
                            rounded-full border ${badgeClass} flex items-center gap-1
                            text-[8px] font-semibold tracking-wide">
                    <i class="fa-solid ${icon}" style="font-size:6px;"></i>
                    ${this._escapeHTML(wlEntry.list_name)}
                </div>`;
        }

        // ── Step 2.5: Episode progress bar ────────────────────────────────────
        let progressBar = '';
        if (wlEntry && wlEntry.total_episodes > 0) {
            const current = wlEntry.current_episode || 0;
            const pct = Math.max(0, Math.min(100,
                Math.round((current / wlEntry.total_episodes) * 100)));
            progressBar = `
                <div class="absolute bottom-0 left-0 right-0 h-0.5 bg-loopBase/80 z-30">
                    <div class="h-full bg-loopAmber shadow-[0_0_8px_rgba(232,168,124,0.6)]"
                         style="width:${pct}%;"></div>
                </div>`;
        }

        const el = document.createElement('div');
        el.className = 'w-[160px] md:w-[200px] shrink-0 cursor-pointer snap-start group relative';
        el.innerHTML = `
            <div class="media-card w-full aspect-[2/3] rounded-lg overflow-hidden relative bg-loopSurface border border-loopBorderSubtle">
                ${statusBadge}
                <img src="${this._escapeHTML(src)}" alt="${this._escapeHTML(item.title)}" loading="lazy"
                     class="poster-img w-full h-full object-cover relative z-10">
                <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent z-10 pointer-events-none group-hover:opacity-0 transition-opacity duration-300 delay-500"></div>
                <div class="absolute bottom-0 left-0 right-0 p-3 z-10 pointer-events-none group-hover:opacity-0 transition-opacity duration-300 delay-500">
                    <span class="text-[9px] font-semibold tracking-wider uppercase ${this._typeBadgeClass(type)} block mb-1">${this._escapeHTML(type)}</span>
                    <h3 class="text-sm font-semibold text-white leading-tight line-clamp-2">${this._escapeHTML(item.title)}</h3>
                </div>
                ${progressBar}
            </div>
        `;
        el.addEventListener('click', () => onClick(item));
        el.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            if (typeof App !== 'undefined' && typeof App.showContextMenu === 'function') {
                App.showContextMenu(e, item, !!wlEntry);
            }
        });
        this._applyTiltEffect(el.querySelector('.media-card'));
        return el;
    },

    // ── Card (Grid — Search / Watchlist / AI) ─────────────────────────────────
    posterCardGrid(item, inList, onClick, isAI = false) {
        const src = item.posterUrl || item.image_url || this._fallbackPoster(item.title);
        const type = item.mediaType || item.media_type || '';

        let statusBadge = '';
        if (inList && item.list_name) {
            const badgeClass = this._statusBadgeClass(item.list_name);
            const icon = item.list_name === 'Watching' ? 'fa-play' :
                         item.list_name === 'Watched'  ? 'fa-check' : 'fa-clock';
            statusBadge = `
                <div class="absolute top-2 right-2 z-10 bg-loopBase/90 backdrop-blur-sm px-2.5 py-1 rounded-full border ${badgeClass} flex items-center gap-1.5 text-[9px] font-semibold tracking-wide">
                    <i class="fa-solid ${icon}" style="font-size:7px;"></i> ${this._escapeHTML(item.list_name)}
                </div>
            `;
        }

        const overlayActionIcon = isAI ? 'fa-solid fa-sparkles' :
                                  inList ? 'fa-solid fa-pen-to-square' :
                                  'fa-solid fa-plus';

        let progressBar = '';
        if (inList && item.total_episodes > 0) {
            const current = item.current_episode || 0;
            const percent = Math.max(0, Math.min(100, Math.round((current / item.total_episodes) * 100)));
            progressBar = `
                <div class="absolute bottom-0 left-0 right-0 h-1 bg-loopBase/80 z-30">
                    <div class="h-full bg-loopAmber shadow-[0_0_8px_rgba(232,168,124,0.6)]" style="width: ${percent}%;"></div>
                </div>
            `;
        }

        const el = document.createElement('div');
        el.className = `media-card w-full aspect-[2/3] relative rounded-lg overflow-hidden cursor-pointer bg-loopSurface border border-white/[0.07] group`;
        el.innerHTML = `
            ${statusBadge}
            <img src="${this._escapeHTML(src)}" alt="${this._escapeHTML(item.title)}" loading="lazy"
                 class="poster-img w-full h-full object-cover relative z-10">
            <div class="absolute inset-0 bg-loopBase/70 opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex flex-col items-center justify-center p-4 text-center z-20 group-hover:delay-0 group-hover:group-hover:delay-[500ms]:opacity-0">
                <i class="${overlayActionIcon} text-loopAmber text-3xl mb-3"></i>
                <h3 class="text-sm font-semibold text-white leading-snug line-clamp-3">${this._escapeHTML(item.title)}</h3>
                ${type ? `<span class="text-[9px] font-semibold text-textMuted mt-1 uppercase tracking-wider">${this._escapeHTML(type)}</span>` : ''}
            </div>
            ${progressBar}
        `;
        el.addEventListener('click', () => onClick(item));
        el.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            if (typeof App !== 'undefined' && typeof App.showContextMenu === 'function') {
                App.showContextMenu(e, item, inList);
            }
        });
        this._applyTiltEffect(el);

        if (isAI && item.reason) {
            const wrap = document.createElement('div');
            wrap.className = 'w-full flex flex-col gap-1.5';
            wrap.appendChild(el);

            const reason = document.createElement('p');
            reason.className = 'text-[10px] text-textMuted leading-relaxed mt-1.5 px-1 line-clamp-2';
            reason.textContent = item.reason;
            wrap.appendChild(reason);

            // Like / Dislike
            const actions = document.createElement('div');
            actions.className = 'flex justify-end gap-3 px-1 mt-0.5';
            actions.innerHTML = `
                <button class="text-textMuted hover:text-loopSuccess text-xs transition-colors cursor-pointer" title="Like" data-like-title="${this._escapeHTML(item.title)}">
                    <i class="fa-solid fa-thumbs-up"></i>
                </button>
                <button class="text-textMuted hover:text-loopError text-xs transition-colors cursor-pointer" title="Not Interested" data-dislike-title="${this._escapeHTML(item.title)}">
                    <i class="fa-solid fa-thumbs-down"></i>
                </button>
            `;
            actions.querySelector('[data-like-title]').addEventListener('click', (e) => {
                e.stopPropagation();
                App.likeRecommendation(item.title);
            });
            actions.querySelector('[data-dislike-title]').addEventListener('click', (e) => {
                e.stopPropagation();
                App.dislikeRecommendation(item.title);
                wrap.remove();
            });
            wrap.appendChild(actions);
            return wrap;
        }

        return el;
    },

    // ── Hero Banner ───────────────────────────────────────────────────────────
    renderHero(item, currentlyWatching) {
        const container = document.getElementById('hero-section');
        const backdrop = item.backdropUrl || item.posterUrl || '';

        let trackingHtml = '';
        if (currentlyWatching) {
            trackingHtml = `
                <div class="inline-flex items-center gap-2.5 mb-5 bg-loopSurface/80 backdrop-blur border border-white/10 rounded-full px-4 py-2 cursor-pointer hover:border-loopAmber/30 transition-colors group" id="hero-resume-btn">
                    <div class="w-4 h-4 rounded-full bg-loopAmber flex items-center justify-center flex-shrink-0">
                        <i class="fa-solid fa-play text-loopBase" style="font-size:6px; margin-left:1px;"></i>
                    </div>
                    <span class="text-xs font-semibold text-textPrimary">Continue:</span>
                    <span class="text-xs text-textSecondary truncate max-w-[180px]">${this._escapeHTML(currentlyWatching.title)}</span>
                </div>
            `;
        }

        const bgImg = document.getElementById('hero-bg-img');
        if (bgImg) {
            const content = document.getElementById('hero-content-wrapper');
            if (content) content.classList.add('opacity-0');
            bgImg.classList.add('opacity-0');

            setTimeout(() => {
                bgImg.src = backdrop;
                const titleEl = document.getElementById('hero-title');
                if (titleEl) titleEl.textContent = item.title;
                const trackingContainer = document.getElementById('hero-tracking-container');
                if (trackingContainer) trackingContainer.innerHTML = trackingHtml;
                document.getElementById('hero-init-btn').onclick = () => App.openDrawer(item);
                if (currentlyWatching && document.getElementById('hero-resume-btn')) {
                    document.getElementById('hero-resume-btn').onclick = () => App.openDrawerFromDB(currentlyWatching);
                }
                setTimeout(() => {
                    bgImg.classList.remove('opacity-0');
                    if (content) content.classList.remove('opacity-0');
                }, 50);
            }, 300);
            return;
        }

        container.innerHTML = `
            <img id="hero-bg-img" src="${this._escapeHTML(backdrop)}"
                 class="absolute inset-0 w-full h-full object-cover opacity-60 transition-opacity duration-500">
            <div class="absolute inset-0 hero-gradient-top"></div>
            <div class="absolute inset-0 hero-gradient-base"></div>
            <div class="absolute inset-0 hero-gradient-side hidden md:block"></div>

            <div id="hero-content-wrapper" class="absolute bottom-0 left-0 right-0 max-w-[1600px] mx-auto px-5 lg:px-8 pb-8 md:pb-10 flex flex-col transition-opacity duration-300">
                <div id="hero-tracking-container">${trackingHtml}</div>
                <h1 id="hero-title" class="font-bold text-3xl md:text-5xl lg:text-6xl leading-tight mb-4 text-white max-w-3xl">${this._escapeHTML(item.title)}</h1>
                <div class="flex gap-3">
                    <button id="hero-init-btn" class="btn-primary text-sm flex items-center gap-2">
                        <i class="fa-solid fa-circle-play"></i> View Details
                    </button>
                </div>
            </div>
        `;

        document.getElementById('hero-init-btn').onclick = () => App.openDrawer(item);
        if (currentlyWatching && document.getElementById('hero-resume-btn')) {
            document.getElementById('hero-resume-btn').onclick = () => App.openDrawerFromDB(currentlyWatching);
        }
    },

    // ── Row / Grid Renderers ──────────────────────────────────────────────────
    renderScrollRow(containerId, items) {
        const c = document.getElementById(containerId);
        if (!c) return;
        c.innerHTML = '';
        items.forEach(item => c.appendChild(this.posterCardRow(item, i => App.openDrawer(i))));
    },

    renderSearchGrid(items) {
        const grid = document.getElementById('searchResults');
        const state = document.getElementById('searchState');

        if (items.length === 0) {
            grid.innerHTML = '';
            state.innerHTML = `
                <i class="fa-regular fa-face-sad-tear text-4xl text-textMuted mb-3 block"></i>
                <p class="text-textMuted text-base">No results found</p>
            `;
            state.classList.remove('hidden');
            return;
        }
        state.classList.add('hidden');
        grid.innerHTML = '';
        items.forEach(item => grid.appendChild(this.posterCardGrid(item, false, i => App.openDrawer(i))));
    },

    renderWatchlistGrid(items, fullWatchlist) {
        const grid = document.getElementById('watchlistGrid');
        const empty = document.getElementById('wlEmptyState');

        if (items.length === 0) {
            grid.innerHTML = '';
            empty.classList.remove('hidden');
            return;
        }
        empty.classList.add('hidden');
        grid.innerHTML = '';
        items.forEach(dbItem => {
            const card = this.posterCardGrid(dbItem, true, item => App.openDrawerFromDB(dbItem));
            grid.appendChild(card);
        });
    },

    renderAIGrid(items) {
        const grid = document.getElementById('aiGrid');
        grid.innerHTML = '';
        items.forEach(item => grid.appendChild(this.posterCardGrid(item, false, i => App.openDrawer(i), true)));
    },

    // ── Detail Modal ──────────────────────────────────────────────────────────
    renderDrawer(item, dbEntry, watchedEpisodes = []) {
        const type = item.mediaType || item.media_type;
        const inList = !!dbEntry;
        const backdrop = item.posterUrl || item.backdropUrl || '';

        document.getElementById('modalImageContainer').innerHTML = `
            <img src="${this._escapeHTML(backdrop)}" class="w-full h-full object-cover opacity-90">
        `;

        document.getElementById('modalTitle').textContent = item.title;
        document.getElementById('modalRating').textContent = item.score ? `⭐ ${item.score}` : 'NR';
        document.getElementById('modalYear').textContent = item.year || '—';
        document.getElementById('modalType').textContent = (type || 'Unknown').charAt(0).toUpperCase() + (type || 'unknown').slice(1);

        // Episodes & Runtime in Tags row next to release year
        const epEl = document.getElementById('modalEpisodes');
        if (epEl) {
            if (item.totalEpisodes > 0) {
                epEl.textContent = `${item.totalEpisodes} ${item.totalEpisodes === 1 ? 'Episode' : 'Episodes'}`;
                epEl.classList.remove('hidden');
            } else {
                epEl.classList.add('hidden');
            }
        }

        const rtEl = document.getElementById('modalRuntime');
        if (rtEl) {
            if (item.runtime) {
                const rtVal = String(item.runtime).replace(/\s*min\s*/gi, '').trim();
                rtEl.textContent = `${rtVal} min`;
                rtEl.classList.remove('hidden');
            } else {
                rtEl.classList.add('hidden');
            }
        }

        // Genres
        document.getElementById('modalGenres').innerHTML = (item.genres || []).map(g =>
            `<span class="px-2.5 py-1 bg-loopSurface border border-white/[0.08] rounded-md text-[11px] font-medium text-textSecondary">${this._escapeHTML(g)}</span>`
        ).join('');

        document.getElementById('modalSummary').textContent = item.synopsis || 'No synopsis available.';

        // ── Watchlist Actions
        const wlBtn = document.getElementById('watchlistBtn');
        const delBtn = document.getElementById('btnDeleteWatchlist');

        if (inList) {
            wlBtn.classList.add('hidden');
            delBtn.classList.remove('hidden');
            delBtn.onclick = (e) => { e.stopPropagation(); App.removeFromWatchlist(item.id, type); };

            document.getElementById('statusSelector').classList.remove('hidden');
            document.querySelectorAll('.status-btn').forEach(btn => {
                const isActive = btn.dataset.status === dbEntry.list_name;
                if (isActive) btn.classList.add('active-status');
                else btn.classList.remove('active-status');
                btn.onclick = () => App.updateStatus(btn.dataset.status);
            });

            // Unified User Control Panel (Rating + Notes)
            document.getElementById('progressSection').classList.remove('hidden');
            const starContainer = document.getElementById('starRating');
            const userRating = dbEntry.user_rating || 0;
            starContainer.innerHTML = `
                <div class="flex items-center gap-2.5">
                    ${userRating > 0 ? `<span class="text-[10px] font-bold text-loopAmber px-2 py-0.5 rounded-md bg-loopAmber/10 border border-loopAmber/20">${userRating}/10</span>` : '<span class="text-[10px] text-textMuted font-medium">Unrated</span>'}
                    <div class="flex gap-1 text-base">
                        ${Array.from({ length: 5 }, (_, i) => {
                            const val = (i + 1) * 2;
                            const active = userRating >= val - 1;
                            return `<i class="fa-solid fa-star cursor-pointer transition-all duration-150 ${active ? 'text-loopAmber drop-shadow-[0_0_6px_rgba(232,168,124,0.5)]' : 'text-loopRaised hover:text-textMuted'}" onclick="App.setRating(${val})"></i>`;
                        }).join('')}
                    </div>
                </div>
            `;

            // Personal Notes
            document.getElementById('personalNotesInput').value = dbEntry.personal_notes || '';
            const saveBtn = document.getElementById('btnSaveNotes');
            saveBtn.onclick = () => {
                const notes = document.getElementById('personalNotesInput').value;
                App.updateNotes(notes);
            };

        } else {
            wlBtn.classList.remove('hidden');
            delBtn.classList.add('hidden');
            wlBtn.className = 'btn-primary w-full mb-4 text-sm font-semibold flex items-center justify-center gap-2';
            wlBtn.innerHTML = '<i class="fa-solid fa-plus"></i> <span>Add to List</span>';
            wlBtn.onclick = () => App.addToWatchlist(item, 'To Watch');
            document.getElementById('statusSelector').classList.add('hidden');
            document.getElementById('progressSection').classList.add('hidden');
        }

        const checklistSection = document.getElementById('seasonChecklistSection');
        if (checklistSection) {
            checklistSection.classList.add('hidden');
        }

        // Season / Episode Checklist
        if ((type?.toLowerCase() === 'tv' || type?.toLowerCase() === 'anime') && inList) {
            document.getElementById('seasonChecklistSection').classList.remove('hidden');
            
            const sectionBtn = document.getElementById('btnToggleEpisodesSection');
            const sectionContent = document.getElementById('episodesSectionContent');
            const sectionIcon = document.getElementById('episodesSectionIcon');
            const badge = document.getElementById('episodesProgressBadge');

            const dropdownBtn = document.getElementById('seasonDropdownBtn');
            const dropdownLabel = document.getElementById('seasonDropdownLabel');
            const dropdownIcon = document.getElementById('seasonDropdownIcon');
            const dropdownMenu = document.getElementById('seasonDropdownMenu');
            const totalSeasons = item.totalSeasons || dbEntry?.total_seasons || 1;

            let currentSeason = 1;
            let curSeasonEpCount = item.totalEpisodes || dbEntry?.total_episodes || 0;

            const updateProgressVisuals = (epCount = curSeasonEpCount) => {
                curSeasonEpCount = epCount;
                const watchedInSeason = watchedEpisodes.filter(we => we.season_number == currentSeason).length;
                const epPercent = epCount > 0 ? Math.min(100, Math.round((watchedInSeason / epCount) * 100)) : 0;

                const seasonFraction = totalSeasons > 0 ? (currentSeason / totalSeasons) : 0;
                const seasonOffset = (56.55 * (1 - seasonFraction)).toFixed(2);

                const epFraction = epCount > 0 ? Math.min(1, watchedInSeason / epCount) : 0;
                const epOffset = (56.55 * (1 - epFraction)).toFixed(2);

                // 1. Header Micro Badges with SVG Progress Rings
                if (badge) {
                    badge.innerHTML = `
                        <!-- Season Ring Pill -->
                        <div class="px-2.5 py-1 rounded-lg bg-loopBase border border-white/10 flex items-center gap-2 text-[10px] font-semibold">
                            <div class="relative w-4 h-4 flex items-center justify-center shrink-0">
                                <svg class="w-4 h-4 -rotate-90 transform" viewBox="0 0 24 24">
                                    <circle cx="12" cy="12" r="9" stroke="rgba(255,255,255,0.1)" stroke-width="3" fill="none"/>
                                    <circle cx="12" cy="12" r="9" stroke="#A09990" stroke-width="3" stroke-linecap="round" fill="none" stroke-dasharray="56.55" stroke-dashoffset="${seasonOffset}" class="transition-all duration-300"/>
                                </svg>
                            </div>
                            <span class="text-textPrimary font-bold">S <span class="text-textPrimary">${currentSeason}</span><span class="text-textMuted font-normal">/${totalSeasons}</span></span>
                        </div>

                        <!-- Episode Ring Pill -->
                        <div class="px-2.5 py-1 rounded-lg bg-loopBase border border-loopAmber/30 flex items-center gap-2 text-[10px] font-semibold shadow-[0_0_10px_rgba(232,168,124,0.15)]">
                            <div class="relative w-4 h-4 flex items-center justify-center shrink-0">
                                <svg class="w-4 h-4 -rotate-90 transform" viewBox="0 0 24 24">
                                    <circle cx="12" cy="12" r="9" stroke="rgba(232,168,124,0.15)" stroke-width="3" fill="none"/>
                                    <circle cx="12" cy="12" r="9" stroke="#E8A87C" stroke-width="3" stroke-linecap="round" fill="none" stroke-dasharray="56.55" stroke-dashoffset="${epOffset}" class="transition-all duration-300 drop-shadow-[0_0_4px_rgba(232,168,124,0.6)]"/>
                                </svg>
                            </div>
                            <span class="text-textPrimary font-bold">EP <span class="text-textPrimary">${watchedInSeason}</span><span class="text-textMuted font-normal">/${epCount || '?'}</span></span>
                        </div>
                    `;
                }

                // 2. Expanded Dashboard Visuals
                const visualSeasonText = document.getElementById('visualSeasonText');
                const visualSeasonSegments = document.getElementById('visualSeasonSegments');
                const visualEpisodeText = document.getElementById('visualEpisodeText');
                const visualEpisodeBar = document.getElementById('visualEpisodeBar');

                if (visualSeasonText) {
                    visualSeasonText.textContent = `S${currentSeason} of ${totalSeasons}`;
                }

                if (visualSeasonSegments) {
                    visualSeasonSegments.innerHTML = Array.from({ length: totalSeasons }, (_, i) => {
                        const s = i + 1;
                        const isCurrent = s === currentSeason;
                        const watchedInS = watchedEpisodes.filter(we => we.season_number == s).length;
                        let bgClass = 'bg-loopSurface/80 border-white/5';
                        if (isCurrent) {
                            bgClass = 'bg-gradient-to-r from-loopAmber to-loopAmberStrong border-loopAmber/40 shadow-[0_0_8px_rgba(232,168,124,0.4)]';
                        } else if (watchedInS > 0) {
                            bgClass = 'bg-loopAmber/40 border-loopAmber/20';
                        }
                        return `<div class="flex-1 h-full rounded-full border transition-all duration-200 ${bgClass}" title="Season ${s}"></div>`;
                    }).join('');
                }

                if (visualEpisodeText) {
                    visualEpisodeText.textContent = `${watchedInSeason} / ${epCount || '?'} (${epPercent}%)`;
                }

                if (visualEpisodeBar) {
                    visualEpisodeBar.style.width = `${epPercent}%`;
                }
            };

            updateProgressVisuals();

            if (sectionBtn && sectionContent) {
                sectionBtn.onclick = () => {
                    const isHidden = sectionContent.classList.contains('hidden');
                    if (isHidden) {
                        sectionContent.classList.remove('hidden');
                        sectionIcon?.classList.add('rotate-180');
                    } else {
                        sectionContent.classList.add('hidden');
                        sectionIcon?.classList.remove('rotate-180');
                    }
                };
            }

            if (dropdownLabel) dropdownLabel.textContent = `Season 1`;

            const renderEpisodes = async (seasonNum) => {
                const epContainer = document.getElementById('episodeList');
                epContainer.innerHTML = '<div class="text-textMuted text-xs p-3 text-center">Loading episodes...</div>';
                try {
                    let episodes = [];
                    const isAnime = (type || '').toLowerCase() === 'anime';
                    if (isAnime) {
                        const totalEps = item.totalEpisodes || dbEntry?.total_episodes || 25;
                        episodes = Array.from({ length: totalEps }, (_, i) => ({
                            episode_number: i + 1,
                            name: `Episode ${i + 1}`
                        }));
                    } else {
                        const seasonData = await API.fetchTVSeasonDetails(item.id, seasonNum);
                        episodes = seasonData?.episodes || [];
                    }
                    if (episodes.length === 0) {
                        epContainer.innerHTML = '<div class="text-textMuted text-xs p-3 text-center">No episode data found for this season.</div>';
                        updateProgressVisuals(0);
                        return;
                    }
                    updateProgressVisuals(episodes.length);
                    epContainer.innerHTML = episodes.map(ep => {
                        const isWatched = watchedEpisodes.some(we => we.season_number == seasonNum && we.episode_number == ep.episode_number);
                        return `
                            <label class="flex items-center justify-between p-3 rounded-xl bg-loopBase/60 border border-white/[0.06] hover:bg-loopRaised/70 hover:border-loopAmber/30 cursor-pointer transition-all duration-150 group" data-season="${seasonNum}" data-ep="${ep.episode_number}">
                                <div class="flex items-center gap-3">
                                    <div class="ep-checkbox w-5 h-5 rounded-md border ${isWatched ? 'bg-loopAmber border-loopAmber text-loopBase shadow-[0_0_8px_rgba(232,168,124,0.4)]' : 'border-white/20 bg-loopSurface group-hover:border-loopAmber/50'} flex items-center justify-center transition-all shrink-0">
                                        <i class="fa-solid fa-check text-[10px] ${isWatched ? 'opacity-100' : 'opacity-0'}"></i>
                                    </div>
                                    <span class="ep-title text-xs font-semibold ${isWatched ? 'text-textMuted line-through opacity-70' : 'text-textPrimary group-hover:text-loopAmber'} transition-colors">
                                        ${ep.episode_number}. ${this._escapeHTML(ep.name || 'Episode ' + ep.episode_number)}
                                    </span>
                                </div>
                            </label>
                        `;
                    }).join('');
                    
                    epContainer.querySelectorAll('label[data-season]').forEach(label => {
                        label.addEventListener('click', async (e) => {
                            // Prevent browser from scroll-into-view on native checkbox focus
                            e.preventDefault();
                            e.stopPropagation();

                            const s = parseInt(label.dataset.season);
                            const ep = parseInt(label.dataset.ep);
                            const icon = label.querySelector('.fa-check');
                            const box = label.querySelector('.ep-checkbox');
                            const titleSpan = label.querySelector('.ep-title');

                            const alreadyWatched = watchedEpisodes.some(we => we.season_number == s && we.episode_number == ep);
                            const willBeWatched = !alreadyWatched;

                            if (willBeWatched) {
                                titleSpan.classList.add('text-textMuted', 'line-through', 'opacity-70');
                                titleSpan.classList.remove('text-textPrimary', 'group-hover:text-loopAmber');
                                box.classList.add('bg-loopAmber', 'border-loopAmber', 'text-loopBase', 'shadow-[0_0_8px_rgba(232,168,124,0.4)]');
                                box.classList.remove('border-white/20', 'bg-loopSurface');
                                icon.classList.remove('opacity-0');
                                watchedEpisodes.push({ season_number: s, episode_number: ep });
                                updateProgressVisuals(episodes.length);
                            } else {
                                titleSpan.classList.remove('text-textMuted', 'line-through', 'opacity-70');
                                titleSpan.classList.add('text-textPrimary', 'group-hover:text-loopAmber');
                                box.classList.remove('bg-loopAmber', 'border-loopAmber', 'text-loopBase', 'shadow-[0_0_8px_rgba(232,168,124,0.4)]');
                                box.classList.add('border-white/20', 'bg-loopSurface');
                                icon.classList.add('opacity-0');
                                const idx = watchedEpisodes.findIndex(we => we.season_number == s && we.episode_number == ep);
                                if (idx > -1) watchedEpisodes.splice(idx, 1);
                                updateProgressVisuals(episodes.length);
                            }
                            if (typeof App !== 'undefined' && App.toggleEpisodeWatched) {
                                await App.toggleEpisodeWatched(s, ep, willBeWatched);
                            }
                        });
                    });
                } catch (e) {
                    epContainer.innerHTML = '<div class="text-loopError text-xs p-3 text-center">Failed to load episodes.</div>';
                }
            };

            const updateDropdownMenu = () => {
                if (!dropdownMenu) return;
                dropdownMenu.innerHTML = Array.from({ length: totalSeasons }, (_, i) => {
                    const s = i + 1;
                    const isSelected = s === currentSeason;
                    return `
                        <button type="button" data-season="${s}" class="w-full text-left px-3.5 py-2 rounded-lg text-xs font-semibold transition-all flex items-center justify-between ${isSelected ? 'bg-loopAmberSubtle text-loopAmber border border-loopAmber/20' : 'text-textSecondary hover:bg-white/5 hover:text-textPrimary'}">
                            <span>Season ${s}</span>
                            ${isSelected ? '<i class="fa-solid fa-check text-[10px] text-loopAmber"></i>' : ''}
                        </button>
                    `;
                }).join('');

                dropdownMenu.querySelectorAll('[data-season]').forEach(btn => {
                    btn.onclick = (e) => {
                        e.stopPropagation();
                        currentSeason = parseInt(btn.dataset.season);
                        if (dropdownLabel) dropdownLabel.textContent = `Season ${currentSeason}`;
                        dropdownMenu.classList.add('hidden');
                        if (dropdownIcon) dropdownIcon.classList.remove('rotate-180');
                        updateDropdownMenu();
                        updateProgressVisuals();
                        renderEpisodes(currentSeason);
                    };
                });
            };

            if (dropdownBtn) {
                dropdownBtn.onclick = (e) => {
                    e.stopPropagation();
                    const isHidden = dropdownMenu.classList.contains('hidden');
                    if (isHidden) {
                        dropdownMenu.classList.remove('hidden');
                        dropdownIcon?.classList.add('rotate-180');
                    } else {
                        dropdownMenu.classList.add('hidden');
                        dropdownIcon?.classList.remove('rotate-180');
                    }
                };
            }

            document.onclick = (e) => {
                if (!document.getElementById('seasonDropdownWrapper')?.contains(e.target)) {
                    dropdownMenu?.classList.add('hidden');
                    dropdownIcon?.classList.remove('rotate-180');
                }
            };

            updateDropdownMenu();
            renderEpisodes(1);
        } else {
            document.getElementById('seasonChecklistSection').classList.add('hidden');
        }
    },

    // ── AI Loading Logs ────────────────────────────────────────────────────────
    startTerminalLogs(onComplete) {
        const container = document.getElementById('terminalLogs');
        if (!container) return onComplete();
        container.innerHTML = '';

        const lines = [
            "Scanning your watch history…",
            "Analysing genre preferences…",
            "Reviewing ratings and feedback…",
            "Mapping taste to content graph…",
            "Querying Gemini AI…",
            "Fetching artwork from TMDB…",
            "Enriching with metadata…",
            "Recommendations ready.",
        ];

        let index = 0;
        const addLine = () => {
            if (index < lines.length) {
                const line = lines[index];
                const p = document.createElement('p');
                const isLast = index === lines.length - 1;
                p.className = `text-xs leading-6 ${isLast ? 'text-loopAmber font-semibold' : 'text-textMuted'}`;
                p.innerHTML = `${isLast ? '<i class="fa-solid fa-check mr-2"></i>' : '<span class="inline-block w-4 text-textMuted/40 mr-1">›</span>'}${this._escapeHTML(line)}`;
                container.appendChild(p);
                container.scrollTop = container.scrollHeight;
                index++;
                setTimeout(addLine, 200);
            } else {
                setTimeout(onComplete, 200);
            }
        };
        addLine();
    },

};

window.UI = UI;
