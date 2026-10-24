/**
 * Loopa Web — UI Rendering Layer v2.0
 * Design System: Obsidian & Electric Coral (#FF5A36), Plus Jakarta Sans & Outfit
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
        div.className = `flex flex-col gap-2.5 w-full max-w-4xl animate-fade-in-up ${role === 'user' ? 'self-end items-end' : 'self-start items-start'}`;
        
        if (role === 'user') {
            div.innerHTML = `
                <div class="flex items-center gap-2.5 flex-row-reverse">
                    <div class="w-7 h-7 rounded-full bg-loopRaised border border-white/10 flex items-center justify-center text-textSecondary text-[11px]"><i class="fa-solid fa-user"></i></div>
                    <span class="text-xs font-bold text-textPrimary uppercase tracking-wider">You</span>
                </div>
                <div class="bg-loopRaised/90 border border-white/10 rounded-2xl rounded-tr-none px-5 py-3.5 text-textPrimary text-sm leading-relaxed inline-block shadow-lg">
                    ${this._escapeHTML(content)}
                </div>
            `;
        } else {
            div.innerHTML = `
                <div class="flex items-center gap-2.5">
                    <div class="w-7 h-7 rounded-full bg-loopAmber/15 border border-loopAmber/30 flex items-center justify-center text-loopAmber text-[11px] shadow-[0_0_12px_rgba(255,90,54,0.35)]">
                        <i class="fa-solid fa-wand-magic-sparkles"></i>
                    </div>
                    <span class="text-xs font-bold text-textPrimary uppercase tracking-wider">Loopa AI Concierge</span>
                </div>
                <div class="bg-loopSurface/90 border border-white/10 rounded-2xl rounded-tl-none px-5 py-4 text-textPrimary text-sm leading-relaxed inline-block shadow-xl">
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

    // ── Helpers ───────────────────────────────────────────────────────────────
    _fallbackPoster(title) {
        return `https://placehold.co/400x600/1A1915/A09990?text=${encodeURIComponent((title || '?').substring(0, 12))}`;
    },

    _handleImgError(img, title) {
        if (!img || img.dataset.failed) return;
        const currentSrc = img.src || '';
        if (currentSrc.includes('image.tmdb.org/t/p/') && typeof CONFIG !== 'undefined' && CONFIG.TMDB_IMG_PROXY_BASE) {
            img.dataset.failed = 'proxy';
            const path = currentSrc.substring(currentSrc.indexOf('/t/p/') + 5);
            img.src = `${CONFIG.TMDB_IMG_PROXY_BASE}${path}`;
        } else {
            img.dataset.failed = 'complete';
            img.src = this._fallbackPoster(title);
        }
    },

    _typeBadgeClass(type) {
        if (type === 'anime') return 'text-loopAmber';
        if (type === 'tv')    return 'text-textSecondary';
        return 'text-textPrimary';
    },

    _statusBadgeClass(status) {
        if (status === 'Watched')  return 'bg-loopSurface/90 border-white/10 text-textSecondary';
        return 'bg-loopAmberSubtle/90 border-loopAmber/30 text-loopAmber';
    },

    // ── Card (Horizontal Row) ─────────────────────────────────────────────────
    posterCardRow(item, onClick) {
        const src  = item.posterUrl || item.image_url || this._fallbackPoster(item.title);
        const type = item.mediaType || item.media_type || '';

        const wlEntry = (typeof App !== 'undefined' && App.s && App.s.watchlist)
            ? App.s.watchlist.find(w =>
                w.id === item.id &&
                w.media_type === (item.mediaType || item.media_type))
            : null;

        // Episode tracking removed per design refresh

        let statusBadge = '';
        if (wlEntry && wlEntry.list_name) {
            const badgeClass = this._statusBadgeClass(wlEntry.list_name);
            const isWatching = wlEntry.list_name === 'Watching';
            const icon = isWatching ? 'fa-play' : 'fa-check';
            statusBadge = `
                <div class="absolute top-2 right-2 z-20 backdrop-blur-sm px-2 py-0.5 rounded-full border ${badgeClass} flex items-center gap-1 text-[9px] font-semibold tracking-wide shadow-md">
                    <i class="fa-solid ${icon}" style="font-size:7px;"></i> ${isWatching ? 'Watching' : 'Watched'}
                </div>
            `;
        }

        const year = (item.releaseDate || item.firstAirDate || item.year || '').toString().slice(0, 4);
        const rating = item.score || item.voteAverage;
        const ratingHtml = rating && Number(rating) > 0
            ? `<span class="ml-auto text-loopAmber text-[11px] font-bold">★ ${Number(rating).toFixed(1)}</span>`
            : '';

        const el = document.createElement('div');
        el.className = 'w-[160px] md:w-[200px] shrink-0 cursor-pointer snap-start group relative';
        el.innerHTML = `
            <div class="media-card w-full aspect-[2/3] rounded-lg overflow-hidden relative bg-loopSurface border border-loopBorderSubtle">
                ${statusBadge}
                <img src="${this._escapeHTML(src)}" alt="${this._escapeHTML(item.title)}" loading="lazy" decoding="async"
                     onerror="UI._handleImgError(this, '${this._escapeHTML(item.title)}')"
                     class="poster-img w-full h-full object-cover relative z-10">
                <!-- Default bottom label — fades out on hover -->
                <div class="card-default-label absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent z-10 pointer-events-none transition-opacity duration-200 group-hover:opacity-0"></div>
                <div class="card-default-label absolute bottom-0 left-0 right-0 p-3 z-10 pointer-events-none transition-opacity duration-200 group-hover:opacity-0">
                    <span class="text-[9px] font-semibold tracking-wider uppercase ${this._typeBadgeClass(type)} block mb-1">${this._escapeHTML(type)}</span>
                    <h3 class="text-sm font-semibold text-white leading-tight line-clamp-2">${this._escapeHTML(item.title)}</h3>
                </div>
                <!-- New sleek fade-in hover panel -->
                <div class="card-hover-overlay absolute inset-0 bg-gradient-to-t from-loopBase/95 via-loopBase/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-20 flex flex-col justify-end p-3 pointer-events-none">
                    <p class="text-textPrimary text-xs font-bold leading-tight line-clamp-2 mb-0.5">${this._escapeHTML(item.title)}</p>
                    <p class="text-textMuted text-[10px] font-medium mb-2.5">${year ? year + ' · ' : ''}${this._escapeHTML(type.toUpperCase())}</p>
                    <div class="card-action-row pointer-events-auto">
                        <button class="card-action-btn primary" title="Track This" data-hover-track>
                            <i class="fa-solid fa-plus"></i>
                        </button>
                        <button class="card-action-btn" title="More Info" data-hover-info>
                            <i class="fa-solid fa-circle-info"></i>
                        </button>
                        ${ratingHtml}
                    </div>
                </div>
            </div>
        `;
        el.addEventListener('click', () => onClick(item));
        el.querySelector('[data-hover-track]')?.addEventListener('click', (e) => {
            e.stopPropagation();
            if (typeof App !== 'undefined') App.openDrawer(item);
        });
        el.querySelector('[data-hover-info]')?.addEventListener('click', (e) => {
            e.stopPropagation();
            if (typeof App !== 'undefined') App.openDrawer(item);
        });
        el.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            if (typeof App !== 'undefined' && typeof App.showContextMenu === 'function') {
                App.showContextMenu(e, item, !!wlEntry);
            }
        });
        return el;
    },

    // ── Card (Grid — Search / Watchlist / AI) ─────────────────────────────────
    posterCardGrid(item, inList, onClick, isAI = false) {
        const src = item.posterUrl || item.image_url || this._fallbackPoster(item.title);
        const type = item.mediaType || item.media_type || '';

        let statusBadge = '';
        if (inList && item.list_name) {
            const badgeClass = this._statusBadgeClass(item.list_name);
            const isWatching = item.list_name === 'Watching';
            const icon = isWatching ? 'fa-play' : 'fa-check';
            statusBadge = `
                <div class="absolute top-2 right-2 z-20 backdrop-blur-sm px-2.5 py-1 rounded-full border ${badgeClass} flex items-center gap-1.5 text-[9px] font-semibold tracking-wide shadow-md">
                    <i class="fa-solid ${icon}" style="font-size:7px;"></i> ${isWatching ? 'Watching' : 'Watched'}
                </div>
            `;
        } else if (item.isAiMatch) {
            statusBadge = `
                <div class="absolute top-2 left-2 z-20 bg-loopAmber text-loopBase px-2 py-0.5 rounded-full font-bold text-[8px] tracking-wider uppercase flex items-center gap-1 shadow-md">
                    <i class="fa-solid fa-sparkles text-[7px]"></i> AI Match
                </div>
            `;
        }

        const overlayActionIcon = (isAI || item.isAiMatch) ? 'fa-solid fa-sparkles' :
                                  inList ? 'fa-solid fa-pen-to-square' :
                                  'fa-solid fa-plus';

        const el = document.createElement('div');
        el.className = `media-card w-full aspect-[2/3] relative rounded-lg overflow-hidden cursor-pointer bg-loopSurface border border-white/[0.07] group`;
        el.innerHTML = `
            ${statusBadge}
            <img src="${this._escapeHTML(src)}" alt="${this._escapeHTML(item.title)}" loading="lazy" decoding="async"
                 onerror="UI._handleImgError(this, '${this._escapeHTML(item.title)}')"
                 class="poster-img w-full h-full object-cover relative z-10">
            <div class="absolute inset-0 bg-loopBase/70 opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex flex-col items-center justify-center p-4 text-center z-20 group-hover:delay-0 group-hover:group-hover:delay-[500ms]:opacity-0">
                <i class="${overlayActionIcon} text-loopAmber text-3xl mb-3"></i>
                <h3 class="text-sm font-semibold text-white leading-snug line-clamp-3">${this._escapeHTML(item.title)}</h3>
                ${type ? `<span class="text-[9px] font-semibold text-textMuted mt-1 uppercase tracking-wider">${this._escapeHTML(type)}</span>` : ''}
            </div>
        `;
        el.addEventListener('click', () => onClick(item));
        el.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            if (typeof App !== 'undefined' && typeof App.showContextMenu === 'function') {
                App.showContextMenu(e, item, inList);
            }
        });

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

    // ── Hero Banner — Phase 2 Cinematic Spotlight Upgrade ─────────────────────
    _heroItems: [],
    _heroCurrentIndex: 0,

    renderHero(item, heroItems, currentIndex) {
        const container = document.getElementById('hero-section');
        if (!container) return;

        // Store for pagination dot clicks
        if (heroItems) this._heroItems = heroItems;
        if (currentIndex !== undefined) this._heroCurrentIndex = currentIndex;

        const backdrop = item.backdropUrl || item.posterUrl || '';
        const year = (item.year || item.releaseDate || item.release_date || item.firstAirDate || item.first_air_date || '').toString().slice(0, 4);
        const type = (item.mediaType || item.media_type || (item.name ? 'tv' : 'movie')).toUpperCase();
        const vote = (item.score !== undefined && item.score !== null) ? item.score : (item.voteAverage || item.vote_average);
        
        const ratingBadge = vote && Number(vote) > 0
            ? `<span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-black/60 border border-accentGold/40 text-accentGold text-xs font-bold shadow-sm backdrop-blur-md"><i class="fa-solid fa-star text-[10px]"></i> ${Number(vote).toFixed(1)}</span>`
            : '';

        const metaBadges = [];
        if (ratingBadge) metaBadges.push(ratingBadge);
        if (year) metaBadges.push(`<span class="text-xs font-semibold text-textSecondary px-2.5 py-0.5 rounded-full bg-white/5 border border-white/10">${year}</span>`);
        if (type) metaBadges.push(`<span class="text-[11px] font-bold tracking-wider uppercase text-textPrimary px-2.5 py-0.5 rounded-full bg-white/10 border border-white/15">${this._escapeHTML(type)}</span>`);
        metaBadges.push(`<span class="text-[10px] font-extrabold tracking-wider px-2 py-0.5 rounded bg-white/10 border border-white/15 text-white/90">4K UHD</span>`);
        metaBadges.push(`<span class="text-[10px] font-extrabold tracking-wider px-2 py-0.5 rounded bg-white/10 border border-white/15 text-accentCyan">DOLBY</span>`);

        const metaHtml = metaBadges.join('');
        const synopsis = item.overview || item.synopsis || '';

        // Pagination dots
        const totalItems = (heroItems || this._heroItems).length;
        const activeIdx = currentIndex !== undefined ? currentIndex : this._heroCurrentIndex;
        const dotsHtml = totalItems > 1 ? `
            <div class="flex items-center gap-2 mt-2" id="hero-dots">
                ${Array.from({ length: totalItems }, (_, i) => `
                    <button
                        class="hero-dot h-[6px] rounded-full transition-all duration-300 ${i === activeIdx ? 'w-[26px] bg-loopAmber shadow-[0_0_12px_rgba(255,90,54,0.6)]' : 'w-[7px] bg-white/25 hover:bg-white/50'}"
                        data-hero-dot="${i}"
                        aria-label="Go to slide ${i + 1}"
                    ></button>
                `).join('')}
            </div>
        ` : '';

        // ── Fast update path: hero already rendered, just swap content ─────────
        const bgImg = document.getElementById('hero-bg-img');
        if (bgImg) {
            const content = document.getElementById('hero-content-wrapper');
            if (content) content.classList.add('opacity-0');
            bgImg.classList.add('opacity-0');

            const preloader = new Image();
            preloader.src = backdrop;

            const revealNewHero = () => {
                bgImg.src = backdrop;

                const titleEl = document.getElementById('hero-title');
                if (titleEl) titleEl.textContent = item.title;

                const metaEl = document.getElementById('hero-meta-row');
                if (metaEl) metaEl.innerHTML = metaHtml;

                const descEl = document.getElementById('hero-overview');
                if (descEl) descEl.textContent = synopsis;

                const dotsContainer = document.getElementById('hero-dots-wrapper');
                if (dotsContainer) dotsContainer.innerHTML = dotsHtml;

                const trackBtn = document.getElementById('hero-track-btn');
                if (trackBtn) trackBtn.onclick = () => App.openDrawer(item);
                const infoBtn = document.getElementById('hero-info-btn');
                if (infoBtn) infoBtn.onclick = () => App.openDrawer(item);
                const detailsBtn = document.getElementById('hero-details-btn');
                if (detailsBtn) detailsBtn.onclick = () => App.openDrawer(item);

                this._bindHeroDots(heroItems || this._heroItems);

                bgImg.onerror = () => this._handleImgError(bgImg, item.title);
                requestAnimationFrame(() => {
                    bgImg.classList.remove('opacity-0');
                    if (content) content.classList.remove('opacity-0');
                });
            };

            let revealed = false;
            preloader.onload = () => {
                if (!revealed) {
                    revealed = true;
                    setTimeout(revealNewHero, 150);
                }
            };
            setTimeout(() => {
                if (!revealed) {
                    revealed = true;
                    revealNewHero();
                }
            }, 400);
            return;
        }

        // ── Initial render ────────────────────────────────────────────────────
        container.innerHTML = `
            <img id="hero-bg-img" src="${this._escapeHTML(backdrop)}" decoding="async"
                 onerror="UI._handleImgError(this, '${this._escapeHTML(item.title)}')"
                 class="absolute inset-0 w-full h-full object-cover object-[center_15%] transition-opacity duration-500 scale-105"
                 alt="${this._escapeHTML(item.title)}">

            <!-- Deep cinematic vignette layers -->
            <div class="absolute inset-0 bg-gradient-to-t from-loopBase via-loopBase/50 to-transparent"></div>
            <div class="absolute inset-0 bg-gradient-to-r from-loopBase via-loopBase/60 to-transparent hidden md:block"></div>
            <div class="absolute inset-0 bg-radial-gradient from-transparent via-black/20 to-black/60 pointer-events-none"></div>

            <!-- Content overlay — bottom-left aligned -->
            <div id="hero-content-wrapper" class="absolute bottom-0 left-0 right-0 max-w-[1600px] mx-auto px-5 md:px-8 lg:px-14 pb-8 md:pb-12 flex flex-col transition-opacity duration-300 z-20">

                <!-- Metadata row: Rating · Year · Type · 4K · Dolby -->
                <div id="hero-meta-row" class="flex items-center gap-2 mb-3 flex-wrap">
                    ${metaHtml}
                </div>

                <!-- Title -->
                <h1 id="hero-title" class="hero-title text-textPrimary max-w-3xl mb-3 line-clamp-2">${this._escapeHTML(item.title)}</h1>

                <!-- Synopsis Overview Snippet -->
                <p id="hero-overview" class="text-sm md:text-base text-textSecondary/90 max-w-2xl line-clamp-2 md:line-clamp-3 mb-6 leading-relaxed font-normal">${this._escapeHTML(synopsis)}</p>

                <!-- Dual CTA Action Row -->
                <div class="flex items-center gap-3 flex-wrap">
                    <button id="hero-track-btn" class="btn-primary text-sm flex items-center gap-2.5 px-7 py-3 shadow-lg shadow-loopAmber/25 hover:scale-105 transition-all">
                        <i class="fa-solid fa-play text-xs"></i> Watch Trailer
                    </button>
                    <button id="hero-info-btn" class="btn-secondary text-sm flex items-center gap-2 px-6 py-3 hover:border-white/30 transition-all">
                        <i class="fa-solid fa-plus text-xs"></i> Add to List
                    </button>
                    <button id="hero-details-btn" class="header-nav-btn !w-11 !h-11 !rounded-full text-white/80 hover:text-white" title="More Details">
                        <i class="fa-solid fa-circle-info text-sm"></i>
                    </button>
                </div>

                <!-- Pagination dots -->
                <div id="hero-dots-wrapper" class="mt-4">${dotsHtml}</div>
            </div>
        `;

        document.getElementById('hero-track-btn').onclick = () => App.openDrawer(item);
        document.getElementById('hero-info-btn').onclick = () => App.openDrawer(item);
        document.getElementById('hero-details-btn').onclick = () => App.openDrawer(item);

        this._bindHeroDots(heroItems || this._heroItems);
    },

    _bindHeroDots(heroItems) {
        document.querySelectorAll('[data-hero-dot]').forEach(btn => {
            btn.onclick = () => {
                const idx = parseInt(btn.dataset.heroDot, 10);
                if (App && App._jumpHero) App._jumpHero(idx);
            };
        });
    },



    // ── Row / Grid Renderers ──────────────────────────────────────────────────
    renderScrollRow(containerId, items) {
        const c = document.getElementById(containerId);
        if (!c) return;
        c.innerHTML = '';
        items.forEach(item => c.appendChild(this.posterCardRow(item, i => App.openDrawer(i))));
        c.scrollLeft = 0;
    },

    // Top 10 Row with giant 3D rank numbers and Cineby-style ribbons
    renderTop10Row(items) {
        const c = document.getElementById('row-top10');
        if (!c) return;
        c.innerHTML = '';
        items.slice(0, 10).forEach((item, idx) => {
            const rank = idx + 1;
            const wrapper = document.createElement('div');
            wrapper.className = 'top10-card-wrapper group';
            wrapper.innerHTML = `
                <span class="top10-rank ${rank <= 3 ? 'top3' : ''}">${rank}</span>
            `;
            const card = this.posterCardRow(item, i => App.openDrawer(i));
            
            // Append Cineby-style TOP 01 ribbon
            const ribbon = document.createElement('div');
            ribbon.className = `top-rank-ribbon ${rank > 3 ? 'subtle' : ''}`;
            ribbon.textContent = rank < 10 ? `TOP 0${rank}` : `TOP ${rank}`;
            const mediaCardEl = card.querySelector('.media-card');
            if (mediaCardEl) {
                mediaCardEl.appendChild(ribbon);
            } else {
                card.appendChild(ribbon);
            }

            wrapper.appendChild(card);
            c.appendChild(wrapper);
        });
        c.scrollLeft = 0;
    },

    renderSearchGrid(items) {
        const grid = document.getElementById('searchResults');
        if (!grid) return;

        if (items.length === 0) {
            grid.innerHTML = `
                <div class="col-span-full py-16 text-center">
                    <i class="fa-regular fa-face-sad-tear text-4xl text-textMuted mb-3 block"></i>
                    <p class="text-textMuted text-base font-medium">No results found</p>
                </div>
            `;
            return;
        }
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

        let currentIndex = 0;
        const chunkSize = 20;

        // Create sentinel element for IntersectionObserver
        const sentinel = document.createElement('div');
        sentinel.className = 'w-full h-10 col-span-full';

        const loadNextChunk = () => {
            const chunk = items.slice(currentIndex, currentIndex + chunkSize);
            // Remove sentinel temporarily to append actual items correctly
            if (sentinel.parentNode === grid) {
                grid.removeChild(sentinel);
            }
            
            chunk.forEach(dbItem => {
                const card = this.posterCardGrid(dbItem, true, item => App.openDrawerFromDB(dbItem));
                grid.appendChild(card);
            });
            
            currentIndex += chunkSize;

            // Re-append sentinel if there are more items
            if (currentIndex < items.length) {
                grid.appendChild(sentinel);
            }
        };

        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                loadNextChunk();
            }
        }, { rootMargin: '200px' });
        
        observer.observe(sentinel);

        // Load first chunk
        loadNextChunk();
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

        // Phase 3C: Cinematic backdrop
        const backdropSrc = item.backdropUrl || item.posterUrl || '';
        const backdropImg = document.getElementById('modalBackdropImg');
        if (backdropImg) {
            backdropImg.decoding = 'async';
            backdropImg.onerror = () => this._handleImgError(backdropImg, item.title);
            backdropImg.src = this._escapeHTML(backdropSrc);
            backdropImg.alt = this._escapeHTML(item.title);
        }

        // Title overlay
        document.getElementById('modalTitle').textContent = item.title;

        // Tags row (rating · year · type · 4K UHD · DOLBY · tracking badge)
        const typeLabel = (type || 'Unknown').charAt(0).toUpperCase() + (type || 'unknown').slice(1);
        const trackingBadge = inList
            ? `<span class="text-[10px] font-bold tracking-wider px-2.5 py-1 rounded-md bg-loopAmberSubtle text-loopAmber border border-loopAmber/30 flex items-center gap-1.5 shadow-[0_0_10px_rgba(255,90,54,0.2)]"><span class="w-1.5 h-1.5 rounded-full bg-loopAmber animate-pulse"></span> Tracking</span>`
            : '';
        const tagsRow = document.getElementById('modalTagsRow');
        if (tagsRow) {
            let metadataBadges = [];
            if (item.score) {
                metadataBadges.push(`<span class="text-[10px] font-bold px-2.5 py-1 rounded-md bg-black/60 text-accentGold border border-accentGold/30 flex items-center gap-1 shadow-sm"><i class="fa-solid fa-star text-[9px]"></i> ${item.score}</span>`);
            }
            if (item.year) {
                metadataBadges.push(`<span class="text-[10px] font-semibold px-2.5 py-1 rounded-md bg-white/10 text-textPrimary border border-white/10">${this._escapeHTML(String(item.year))}</span>`);
            }
            if (typeLabel) {
                metadataBadges.push(`<span class="text-[10px] font-bold tracking-wider px-2.5 py-1 rounded-md bg-white/10 text-textSecondary uppercase border border-white/10">${this._escapeHTML(typeLabel)}</span>`);
            }
            
            // High-tech badges
            metadataBadges.push(`<span class="text-[9px] font-extrabold tracking-wider px-2 py-1 rounded-md bg-white/10 text-white/90 border border-white/15">4K UHD</span>`);
            metadataBadges.push(`<span class="text-[9px] font-extrabold tracking-wider px-2 py-1 rounded-md bg-white/10 text-accentCyan border border-accentCyan/30">DOLBY ATMOS</span>`);

            // Runtime or Episode Counts
            if (type === 'movie' && item.runtime) {
                const rtVal = String(item.runtime).replace(/\s*min\s*/gi, '').trim();
                metadataBadges.push(`<span class="text-[10px] font-semibold px-2.5 py-1 rounded-md bg-white/10 text-textSecondary border border-white/10"><i class="fa-regular fa-clock mr-1 text-loopAmber/80"></i>${rtVal} min</span>`);
            } else if ((type === 'tv' || type === 'anime') && item.totalEpisodes > 0) {
                const seasonsText = item.totalSeasons ? `${item.totalSeasons} Season${item.totalSeasons > 1 ? 's' : ''} · ` : '';
                metadataBadges.push(`<span class="text-[10px] font-semibold px-2.5 py-1 rounded-md bg-white/10 text-textSecondary border border-white/10"><i class="fa-solid fa-list-ol mr-1 text-loopAmber/80"></i>${seasonsText}${item.totalEpisodes} Eps</span>`);
            }

            if (trackingBadge) metadataBadges.push(trackingBadge);

            tagsRow.innerHTML = metadataBadges.join('');
        }

        // Genre pills
        const genrePills = document.getElementById('modalGenrePills');
        if (genrePills) {
            genrePills.innerHTML = (item.genres || []).map(g =>
                `<span class="genre-pill">${this._escapeHTML(g)}</span>`
            ).join('');
            genrePills.classList.toggle('hidden', !item.genres?.length);
        }

        // Cast & Crew Bento Card Fetch & Render
        const castRow = document.getElementById('modalCastRow');
        const castTile = document.getElementById('modalCastTile');
        if (castRow && castTile) {
            castRow.innerHTML = `
                <div class="flex items-center gap-2 text-xs text-textMuted py-2">
                    <i class="fa-solid fa-circle-notch fa-spin text-loopAmber text-xs"></i>
                    <span>Loading cast…</span>
                </div>
            `;
            if (item.id) {
                API.fetchCredits(item.id, type).then(cast => {
                    if (cast && cast.length > 0) {
                        castRow.innerHTML = cast.map(c => `
                            <div class="flex flex-col items-center text-center w-[72px] shrink-0 group">
                                <div class="w-12 h-12 rounded-full overflow-hidden border border-white/15 bg-loopBase group-hover:border-loopAmber/50 transition-all duration-200 shadow-md">
                                    ${c.profileUrl 
                                        ? `<img src="${c.profileUrl}" alt="${this._escapeHTML(c.name)}" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300">`
                                        : `<div class="w-full h-full flex items-center justify-center text-xs font-bold text-textSecondary bg-loopRaised">${this._escapeHTML(c.name.charAt(0))}</div>`}
                                </div>
                                <span class="text-[11px] font-bold text-textPrimary truncate w-full mt-1.5 group-hover:text-loopAmber transition-colors">${this._escapeHTML(c.name)}</span>
                                <span class="text-[9px] text-textMuted truncate w-full">${this._escapeHTML(c.character || '')}</span>
                            </div>
                        `).join('');
                        castTile.classList.remove('hidden');
                    } else {
                        castTile.classList.add('hidden');
                    }
                }).catch(() => {
                    castTile.classList.add('hidden');
                });
            } else {
                castTile.classList.add('hidden');
            }
        }

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
                            return `<i class="fa-solid fa-star cursor-pointer transition-all duration-150 ${active ? 'text-loopAmber drop-shadow-[0_0_6px_rgba(255,90,54,0.5)]' : 'text-loopRaised hover:text-textMuted'}" onclick="App.setRating(${val})"></i>`;
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
            wlBtn.className = 'btn-primary text-xs md:text-sm font-semibold px-4 py-2.5 rounded-full flex items-center justify-center gap-2 shadow-lg hover:shadow-glowCoral transition-all';
            wlBtn.innerHTML = '<i class="fa-solid fa-plus"></i> <span>Add to List</span>';
            wlBtn.onclick = () => App.addToWatchlist(item, 'Watching');
            document.getElementById('statusSelector').classList.add('hidden');
            document.getElementById('progressSection').classList.add('hidden');
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

    // ── Phase 3C: More Like This row ─────────────────────────────────────────
    async _fetchAndRenderSimilar(id, mediaType) {
        const section = document.getElementById('moreLikeThisSection');
        const row = document.getElementById('moreLikeThisRow');
        if (!section || !row) return;

        // Show section with skeleton placeholders
        section.classList.remove('hidden');
        row.innerHTML = Array.from({ length: 5 }, () =>
            `<div class="w-[110px] flex-shrink-0 aspect-[2/3] rounded-lg skeleton"></div>`
        ).join('');

        try {
            const similar = await API.fetchSimilar(id, mediaType);
            if (!similar.length) {
                section.classList.add('hidden');
                return;
            }
            row.innerHTML = '';
            similar.forEach(item => {
                const card = this.posterCardRow(item, (i) => {
                    if (typeof App !== 'undefined') App.openDrawer(i);
                });
                // Compact width for More Like This row
                card.className = 'flex-shrink-0 cursor-pointer snap-start group relative';
                card.style.width = '110px';
                row.appendChild(card);
            });
        } catch {
            section.classList.add('hidden');
        }
    },

};

window.UI = UI;
