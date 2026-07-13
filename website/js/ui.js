/**
 * Loopa Web — UI Rendering Layer v2.0
 * Design System: Warm dark palette, DM Sans, Loopa Amber (#E8A87C)
 */

const UI = {
    // ── Toast ─────────────────────────────────────────────────────────────────
    _toastTimer: null,
    toast(message, type = 'success') {
        const el = document.getElementById('toast');
        const msgEl = document.getElementById('toastMsg');

        if (type === 'error') {
            el.className = 'fixed top-20 left-1/2 -translate-x-1/2 px-5 py-3 rounded-full text-sm font-semibold transform z-[200] flex items-center gap-2 pointer-events-none shadow-lg border opacity-100 translate-y-0 transition-all duration-300 bg-loopSurface text-loopError border-loopError/30';
            msgEl.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> ${message}`;
        } else {
            el.className = 'fixed top-20 left-1/2 -translate-x-1/2 px-5 py-3 rounded-full text-sm font-semibold transform z-[200] flex items-center gap-2 pointer-events-none shadow-lg border opacity-100 translate-y-0 transition-all duration-300 bg-loopSurface text-loopAmber border-loopAmber/30';
            msgEl.innerHTML = `<i class="fa-solid fa-check"></i> ${message}`;
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
                    ${content}
                </div>
            `;
        } else {
            div.innerHTML = `
                <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full bg-loopSurface border border-loopAmber/20 flex items-center justify-center text-loopAmber"><i class="fa-solid fa-wand-magic-sparkles text-[10px]"></i></div>
                    <span class="text-sm font-bold text-textPrimary">Loopa AI</span>
                </div>
                <div class="bg-loopSurface/60 border border-white/5 rounded-2xl rounded-tl-none p-4 text-textPrimary text-sm leading-relaxed inline-block">
                    ${content}
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
        if (!el) return;
        el.addEventListener('mousemove', (e) => {
            const rect = el.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            const centerX = rect.width / 2;
            const centerY = rect.height / 2;
            
            const rotateX = ((y - centerY) / centerY) * -10;
            const rotateY = ((x - centerX) / centerX) * 10;
            
            el.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`;
            el.style.transition = 'none';
            el.style.zIndex = '50';
        });
        
        el.addEventListener('mouseleave', () => {
            el.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
            el.style.transition = 'transform 0.5s ease-out';
            el.style.zIndex = '1';
        });
        
        el.addEventListener('mouseenter', () => {
            el.style.transition = 'transform 0.1s ease-out';
        });
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
        const src = item.posterUrl || item.image_url || this._fallbackPoster(item.title);
        const type = item.mediaType || item.media_type || '';
        const synopsis = item.overview || item.synopsis || item.description || '';
        const synopsisHTML = synopsis ? `
            <div class="absolute inset-0 bg-loopSurface/95 backdrop-blur-sm p-3 text-[11px] text-white/90 leading-relaxed opacity-0 group-hover:opacity-100 transition-opacity duration-300 delay-500 z-20 pointer-events-none text-left flex flex-col justify-start">
                <div class="line-clamp-[10] w-full text-ellipsis overflow-hidden">${synopsis}</div>
            </div>
        ` : '';

        const el = document.createElement('div');
        el.className = 'w-[160px] md:w-[200px] shrink-0 cursor-pointer snap-start group relative';
        el.innerHTML = `
            <div class="media-card w-full aspect-[2/3] rounded-lg overflow-hidden relative bg-loopSurface border border-white/[0.07]">
                <img src="${src}" alt="${item.title}" loading="lazy"
                     class="poster-img w-full h-full object-cover relative z-10">
                ${synopsisHTML}
                <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent z-10 pointer-events-none group-hover:opacity-0 transition-opacity duration-300 delay-500"></div>
                <div class="absolute bottom-0 left-0 right-0 p-3 z-10 pointer-events-none group-hover:opacity-0 transition-opacity duration-300 delay-500">
                    <span class="text-[9px] font-semibold tracking-wider uppercase ${this._typeBadgeClass(type)} block mb-1">${type}</span>
                    <h3 class="text-sm font-semibold text-white leading-tight line-clamp-2">${item.title}</h3>
                </div>
            </div>
        `;
        el.addEventListener('click', () => onClick(item));
        el.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            if (typeof App !== 'undefined' && typeof App.showContextMenu === 'function') {
                App.showContextMenu(e, item, false); // inList = false for row (usually trending/search)
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
                    <i class="fa-solid ${icon}" style="font-size:7px;"></i> ${item.list_name}
                </div>
            `;
        }

        const synopsis = item.overview || item.synopsis || item.description || '';
        const synopsisHTML = synopsis ? `
            <div class="absolute inset-0 bg-loopSurface/95 backdrop-blur-sm p-4 text-xs text-white/90 leading-relaxed opacity-0 group-hover:opacity-100 transition-opacity duration-300 delay-500 z-30 pointer-events-none text-left flex flex-col justify-start">
                <div class="line-clamp-[12] w-full text-ellipsis overflow-hidden">${synopsis}</div>
            </div>
        ` : '';

        const overlayActionIcon = isAI ? 'fa-solid fa-sparkles' :
                                  inList ? 'fa-solid fa-pen-to-square' :
                                  'fa-solid fa-plus';

        const el = document.createElement('div');
        el.className = `media-card w-full aspect-[2/3] relative rounded-lg overflow-hidden cursor-pointer bg-loopSurface border border-white/[0.07] group`;
        el.innerHTML = `
            ${statusBadge}
            <img src="${src}" alt="${item.title}" loading="lazy"
                 class="poster-img w-full h-full object-cover relative z-10">
            ${synopsisHTML}
            <div class="absolute inset-0 bg-loopBase/70 opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex flex-col items-center justify-center p-4 text-center z-20 group-hover:delay-0 group-hover:group-hover:delay-[500ms]:opacity-0">
                <i class="${overlayActionIcon} text-loopAmber text-3xl mb-3"></i>
                <h3 class="text-sm font-semibold text-white leading-snug line-clamp-3">${item.title}</h3>
                ${type ? `<span class="text-[9px] font-semibold text-textMuted mt-1 uppercase tracking-wider">${type}</span>` : ''}
            </div>
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
                <button class="text-textMuted hover:text-loopSuccess text-xs transition-colors cursor-pointer" title="Like" data-like-title="${item.title}">
                    <i class="fa-solid fa-thumbs-up"></i>
                </button>
                <button class="text-textMuted hover:text-loopError text-xs transition-colors cursor-pointer" title="Not Interested" data-dislike-title="${item.title}">
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
                    <span class="text-xs text-textSecondary truncate max-w-[180px]">${currentlyWatching.title}</span>
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
            <img id="hero-bg-img" src="${backdrop}"
                 class="absolute inset-0 w-full h-full object-cover opacity-40 transition-opacity duration-500">
            <div class="absolute inset-0 hero-gradient-top"></div>
            <div class="absolute inset-0 hero-gradient-base"></div>
            <div class="absolute inset-0 hero-gradient-side hidden md:block"></div>

            <div id="hero-content-wrapper" class="absolute bottom-0 left-0 right-0 max-w-[1600px] mx-auto px-5 lg:px-8 pb-8 md:pb-10 flex flex-col transition-opacity duration-300">
                <div id="hero-tracking-container">${trackingHtml}</div>
                <h1 id="hero-title" class="font-bold text-3xl md:text-5xl lg:text-6xl leading-tight mb-4 text-white max-w-3xl">${item.title}</h1>
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

    renderWatchlistGrid(items) {
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
            const card = this.posterCardGrid(dbItem, true, item => App.openDrawerFromDB(item));
            grid.appendChild(card);
        });
    },

    renderAIGrid(items) {
        const grid = document.getElementById('aiGrid');
        grid.innerHTML = '';
        items.forEach(item => grid.appendChild(this.posterCardGrid(item, false, i => App.openDrawer(i), true)));
    },

    // ── Detail Modal ──────────────────────────────────────────────────────────
    renderDrawer(item, dbEntry) {
        const type = item.mediaType || item.media_type;
        const inList = !!dbEntry;
        const backdrop = item.posterUrl || item.backdropUrl || '';

        document.getElementById('modalImageContainer').innerHTML = `
            <img src="${backdrop}" class="w-full h-full object-cover opacity-90">
        `;

        document.getElementById('modalTitle').textContent = item.title;
        document.getElementById('modalRating').textContent = item.score ? `⭐ ${item.score}` : 'NR';
        document.getElementById('modalYear').textContent = item.year || '—';
        document.getElementById('modalType').textContent = (type || 'Unknown').charAt(0).toUpperCase() + (type || 'unknown').slice(1);

        // Genres
        document.getElementById('modalGenres').innerHTML = (item.genres || []).map(g =>
            `<span class="px-2.5 py-1 bg-loopSurface border border-white/[0.08] rounded-md text-[11px] font-medium text-textSecondary">${g}</span>`
        ).join('');

        // Stats
        const stats = [];
        if (item.totalSeasons > 1)  stats.push({ l: 'Seasons',  v: item.totalSeasons });
        if (item.totalEpisodes > 0) stats.push({ l: 'Episodes', v: item.totalEpisodes });
        if (item.runtime)           stats.push({ l: 'Runtime',  v: item.runtime });

        document.getElementById('modalStats').innerHTML = stats.map(s => `
            <div class="bg-loopSurface border border-white/[0.07] rounded-lg p-3 text-center">
                <span class="block text-2xl font-bold text-textPrimary leading-none">${s.v}</span>
                <span class="block text-[10px] font-semibold text-textMuted tracking-wider mt-1 uppercase">${s.l}</span>
            </div>
        `).join('');

        document.getElementById('modalSummary').textContent = item.synopsis || 'No synopsis available.';

        // ── Watchlist Actions
        const wlBtn = document.getElementById('watchlistBtn');

        if (inList) {
            wlBtn.className = 'w-full bg-loopSurface text-textPrimary font-semibold text-sm py-3.5 rounded-full mb-4 border border-white/10 hover:border-loopAmber/30 hover:text-loopAmber flex items-center justify-between px-5 transition-colors';
            wlBtn.innerHTML = `
                <div class="flex items-center gap-2"><i class="fa-solid fa-check text-loopAmber"></i> In your list</div>
                <div id="btnRemoveList"><i class="fa-solid fa-trash text-textMuted hover:text-loopError transition-colors text-xs"></i></div>
            `;
            setTimeout(() => {
                const rm = document.getElementById('btnRemoveList');
                if (rm) rm.onclick = (e) => { e.stopPropagation(); App.removeFromWatchlist(item.id, type); };
            }, 0);

            document.getElementById('statusSelector').classList.remove('hidden');
            document.querySelectorAll('.status-btn').forEach(btn => {
                const isActive = btn.dataset.status === dbEntry.list_name;
                if (isActive) btn.classList.add('active-status');
                else btn.classList.remove('active-status');
                btn.onclick = () => App.updateStatus(btn.dataset.status);
            });

            // Progress (TV / Anime)
            if (type === 'tv' || type === 'anime') {
                document.getElementById('episodeProgressContainer').classList.remove('hidden');
                document.getElementById('progDisplay').textContent = `E ${dbEntry.current_episode || 0}`;
                document.getElementById('btnProgAdd').onclick = () => App.updateProgress('episode', 1);
                document.getElementById('btnProgSub').onclick = () => App.updateProgress('episode', -1);
            } else {
                document.getElementById('episodeProgressContainer').classList.add('hidden');
            }

            // Rating stars — always show for listed items
            document.getElementById('progressSection').classList.remove('hidden');
            const starContainer = document.getElementById('starRating');
            const userRating = dbEntry.user_rating || 0;
            starContainer.innerHTML = Array.from({ length: 5 }, (_, i) => {
                const val = (i + 1) * 2;
                const active = userRating >= val - 1;
                return `<i class="fa-solid fa-star cursor-pointer transition-colors ${active ? 'text-loopAmber' : 'text-loopRaised hover:text-textMuted'}" onclick="App.setRating(${val})"></i>`;
            }).join('');

            // Notes Section
            document.getElementById('notesSection').classList.remove('hidden');
            document.getElementById('personalNotesInput').value = dbEntry.personal_notes || '';
            const saveBtn = document.getElementById('btnSaveNotes');
            saveBtn.onclick = () => {
                const notes = document.getElementById('personalNotesInput').value;
                App.updateNotes(notes);
            };

        } else {
            wlBtn.className = 'btn-primary w-full mb-4 text-sm font-semibold flex items-center justify-center gap-2';
            wlBtn.innerHTML = '<i class="fa-solid fa-plus"></i> <span>Add to List</span>';
            wlBtn.onclick = () => App.addToWatchlist(item, 'To Watch');
            document.getElementById('statusSelector').classList.add('hidden');
            document.getElementById('progressSection').classList.add('hidden');
            document.getElementById('notesSection').classList.add('hidden');
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
                p.innerHTML = `${isLast ? '<i class="fa-solid fa-check mr-2"></i>' : '<span class="inline-block w-4 text-textMuted/40 mr-1">›</span>'}${line}`;
                container.appendChild(p);
                container.scrollTop = container.scrollHeight;
                index++;
                setTimeout(addLine, 200);
            } else {
                setTimeout(onComplete, 200);
            }
        };
        addLine();
    }
};
