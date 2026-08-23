const state = {
    csrf: null,
    authenticated: false,
    crewPage: 0,
    crewTotalPages: 0,
    crews: [],
    selectedCrew: null
};

const byId = (id) => document.getElementById(id);
const notice = byId('notice');
const logContainer = byId('api-log');

function formObject(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function compactObject(value) {
    return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== '' && item !== null));
}

function setNotice(message, type = '') {
    notice.textContent = message;
    notice.className = `notice ${type}`.trim();
}

function setSession(authenticated) {
    state.authenticated = authenticated;
    byId('session-dot').className = `status-dot ${authenticated ? 'authenticated' : 'online'}`;
    byId('session-label').textContent = authenticated ? '인증 세션 사용 중' : '서버 연결됨 · 로그인 필요';
}

function addLog(method, path, status, body, elapsed) {
    const entry = document.createElement('article');
    entry.className = 'log-entry';
    const isOk = status >= 200 && status < 300;
    entry.innerHTML = `
        <div class="log-meta">
            <span>${escapeHtml(method)} ${escapeHtml(path)}</span>
            <span class="log-status ${isOk ? 'ok' : 'error'}">${status} · ${elapsed}ms</span>
        </div>
        <pre>${escapeHtml(JSON.stringify(body, null, 2))}</pre>`;
    logContainer.prepend(entry);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function refreshCsrf(log = true) {
    const result = await api('/api/v1/auth/csrf', {method: 'GET'}, {skipCsrf: true, log});
    if (result.ok && result.body.data) {
        state.csrf = result.body.data;
        setNotice('CSRF 토큰이 준비되었습니다. 상태 변경 요청을 실행할 수 있습니다.', 'success');
    }
    return result;
}

async function api(path, options = {}, controls = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    if (!controls.skipCsrf && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        if (!state.csrf) await refreshCsrf(false);
        if (state.csrf) headers.set(state.csrf.headerName, state.csrf.token);
    }
    const started = performance.now();
    let response;
    let body;
    try {
        response = await fetch(path, {...options, method, headers, credentials: 'same-origin'});
        const text = await response.text();
        body = text ? JSON.parse(text) : null;
    } catch (error) {
        body = {status: 0, message: `요청 실패: ${error.message}`, data: null, timestamp: null};
        if (controls.log !== false) addLog(method, path, 0, body, Math.round(performance.now() - started));
        setNotice('서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인하세요.', 'error');
        return {ok: false, status: 0, body};
    }
    if (controls.log !== false) addLog(method, path, response.status, body, Math.round(performance.now() - started));
    if (response.status === 401) setSession(false);
    if (!response.ok) setNotice(body?.message || `요청이 실패했습니다. (${response.status})`, 'error');
    else if (controls.announce !== false) setNotice(body?.message || '요청이 완료되었습니다.', 'success');
    return {ok: response.ok, status: response.status, body, headers: response.headers};
}

async function mutation(path, method, body) {
    return api(path, {method, body: body === undefined ? undefined : JSON.stringify(body)});
}

async function checkSession() {
    const result = await loadCrews(false);
    setSession(result.ok);
    if (!result.ok && result.status !== 401) setNotice('서버 연결 상태를 확인하세요.', 'error');
}

async function loadCrews(announce = true) {
    const sort = encodeURIComponent(byId('crew-sort').value);
    const path = `/api/v1/crews?page=${state.crewPage}&size=20&sort=${sort}`;
    const result = await api(path, {method: 'GET'}, {announce});
    if (!result.ok) {
        renderCrews([]);
        return result;
    }
    state.crews = result.body.data.content;
    state.crewTotalPages = result.body.data.totalPages;
    byId('crew-page-label').textContent = `${state.crewPage + 1} / ${Math.max(state.crewTotalPages, 1)} 페이지`;
    byId('crew-prev').disabled = state.crewPage <= 0;
    byId('crew-next').disabled = state.crewPage + 1 >= state.crewTotalPages;
    renderCrews(state.crews);
    setSession(true);
    return result;
}

function renderCrews(crews) {
    const container = byId('crew-list');
    container.replaceChildren();
    container.className = 'crew-list';
    if (!crews.length) {
        container.classList.add('empty-state');
        container.textContent = state.authenticated ? '조회된 크루가 없습니다.' : '로그인 후 크루 목록을 불러오세요.';
        return;
    }
    const template = byId('crew-card-template');
    crews.forEach((crew) => {
        const card = template.content.firstElementChild.cloneNode(true);
        card.dataset.crewId = crew.id;
        if (state.selectedCrew?.id === crew.id) card.classList.add('selected');
        card.querySelector('.crew-item-id').textContent = `CREW #${crew.id}`;
        card.querySelector('.crew-item-name').textContent = crew.name;
        card.querySelector('.crew-item-users').textContent = `${crew.currentUsers} / ${crew.maxUsers}`;
        card.querySelector('.crew-item-goal').textContent = `${crew.weeklyCertificationGoal}회`;
        card.querySelector('.crew-select').addEventListener('click', () => selectCrew(crew));
        container.append(card);
    });
}

function selectCrew(crew) {
    state.selectedCrew = crew;
    byId('selected-crew-title').textContent = crew.name;
    byId('selected-crew-id').textContent = `ID ${crew.id}`;
    renderCrews(state.crews);
    byId('selected-crew-section').scrollIntoView({behavior: 'smooth', block: 'start'});
    setNotice(`${crew.name} 크루를 선택했습니다.`, 'success');
}

function requireCrew() {
    if (!state.selectedCrew) {
        setNotice('먼저 크루 목록에서 대상 크루를 선택하세요.', 'error');
        return null;
    }
    return state.selectedCrew;
}

async function loadMembers() {
    const crew = requireCrew();
    if (!crew) return;
    const result = await api(`/api/v1/crews/${crew.id}/members?page=0&size=100&sort=id,desc`);
    if (result.ok) renderMembers(result.body.data.content);
}

function renderMembers(members) {
    const container = byId('member-list');
    container.replaceChildren();
    container.className = 'member-list';
    byId('member-count').textContent = `${members.length}명`;
    if (!members.length) {
        container.classList.add('empty-state');
        container.textContent = '조회된 크루원이 없습니다.';
        return;
    }
    members.forEach((member) => {
        const row = document.createElement('div');
        row.className = 'member-row';
        row.innerHTML = `
            <strong>#${member.userId}</strong>
            <span>${escapeHtml(member.nickname)}</span>
            <span class="member-role ${member.role === 'MANAGER' ? 'manager' : ''}">${member.role}</span>
            <button class="button button-ghost copy-id" type="button">ID 복사</button>`;
        row.querySelector('.copy-id').addEventListener('click', async () => {
            await navigator.clipboard.writeText(String(member.userId));
            setNotice(`사용자 ID ${member.userId}을 복사했습니다.`, 'success');
        });
        container.append(row);
    });
}

function bindForm(id, handler) {
    byId(id).addEventListener('submit', async (event) => {
        event.preventDefault();
        const button = event.currentTarget.querySelector('button[type="submit"]');
        button.disabled = true;
        try { await handler(event.currentTarget); } finally { button.disabled = false; }
    });
}

bindForm('signup-form', async (form) => {
    const result = await mutation('/api/v1/users', 'POST', formObject(form));
    if (result.ok) form.reset();
});

bindForm('login-form', async (form) => {
    const result = await mutation('/api/v1/auth/login', 'POST', formObject(form));
    if (result.ok) {
        form.reset();
        setSession(true);
        state.csrf = null;
        await refreshCsrf(false);
        await loadCrews(false);
    }
});

bindForm('nickname-form', async (form) => {
    const result = await mutation('/api/v1/users/me', 'PATCH', formObject(form));
    if (result.ok) form.reset();
});

bindForm('crew-create-form', async (form) => {
    const values = formObject(form);
    values.maxUsers = Number(values.maxUsers);
    values.weeklyCertificationGoal = Number(values.weeklyCertificationGoal);
    const result = await mutation('/api/v1/crews', 'POST', values);
    if (result.ok) {
        form.reset();
        state.crewPage = 0;
        await loadCrews(false);
    }
});

bindForm('join-form', async (form) => {
    const crew = requireCrew();
    if (!crew) return;
    const result = await mutation(`/api/v1/crews/${crew.id}/members`, 'POST', formObject(form));
    if (result.ok) {
        form.reset();
        await loadCrews(false);
        await loadMembers();
    }
});

bindForm('crew-update-form', async (form) => {
    const crew = requireCrew();
    if (!crew) return;
    const values = compactObject(formObject(form));
    if ('maxUsers' in values) values.maxUsers = Number(values.maxUsers);
    if ('weeklyCertificationGoal' in values) values.weeklyCertificationGoal = Number(values.weeklyCertificationGoal);
    if (!Object.keys(values).length) return setNotice('수정할 값을 하나 이상 입력하세요.', 'error');
    const result = await mutation(`/api/v1/crews/${crew.id}`, 'PATCH', values);
    if (result.ok) {
        form.reset();
        await loadCrews(false);
    }
});

bindForm('manager-form', async (form) => {
    const crew = requireCrew();
    if (!crew) return;
    const values = formObject(form);
    const result = await mutation(`/api/v1/crews/${crew.id}/manager`, 'PATCH', {targetUserId: Number(values.targetUserId)});
    if (result.ok) { form.reset(); await loadMembers(); }
});

bindForm('kick-form', async (form) => {
    const crew = requireCrew();
    if (!crew) return;
    const {userId} = formObject(form);
    if (!confirm(`사용자 #${userId}을 크루에서 추방할까요?`)) return;
    const result = await mutation(`/api/v1/crews/${crew.id}/members/${Number(userId)}`, 'DELETE');
    if (result.ok) { form.reset(); await loadMembers(); await loadCrews(false); }
});

byId('logout').addEventListener('click', async () => {
    const result = await mutation('/api/v1/auth/logout', 'POST');
    if (result.ok) {
        state.csrf = null;
        setSession(false);
        state.selectedCrew = null;
        renderCrews([]);
        await refreshCsrf(false);
    }
});

byId('withdraw').addEventListener('click', async () => {
    if (!confirm('회원탈퇴하면 관리 중인 모든 크루도 삭제됩니다. 계속할까요?')) return;
    const result = await mutation('/api/v1/users/me', 'DELETE');
    if (result.ok) {
        state.csrf = null;
        setSession(false);
        state.selectedCrew = null;
        renderCrews([]);
        await refreshCsrf(false);
    }
});

byId('leave-crew').addEventListener('click', async () => {
    const crew = requireCrew();
    if (!crew || !confirm(`${crew.name} 크루에서 탈퇴할까요? 관리자인 경우 크루가 삭제됩니다.`)) return;
    const result = await mutation(`/api/v1/crews/${crew.id}/members/me`, 'DELETE');
    if (result.ok) {
        state.selectedCrew = null;
        byId('selected-crew-title').textContent = '선택된 크루 없음';
        byId('selected-crew-id').textContent = 'ID —';
        renderMembers([]);
        await loadCrews(false);
    }
});

byId('delete-crew').addEventListener('click', async () => {
    const crew = requireCrew();
    if (!crew || !confirm(`${crew.name} 크루와 모든 소속을 삭제할까요?`)) return;
    const result = await mutation(`/api/v1/crews/${crew.id}`, 'DELETE');
    if (result.ok) {
        state.selectedCrew = null;
        byId('selected-crew-title').textContent = '선택된 크루 없음';
        byId('selected-crew-id').textContent = 'ID —';
        renderMembers([]);
        await loadCrews(false);
    }
});

byId('reload-crews').addEventListener('click', () => loadCrews());
byId('load-members').addEventListener('click', loadMembers);
byId('refresh-csrf').addEventListener('click', () => refreshCsrf());
byId('clear-log').addEventListener('click', () => logContainer.replaceChildren());
byId('crew-sort').addEventListener('change', () => { state.crewPage = 0; loadCrews(); });
byId('crew-prev').addEventListener('click', () => { if (state.crewPage > 0) { state.crewPage--; loadCrews(); } });
byId('crew-next').addEventListener('click', () => { if (state.crewPage + 1 < state.crewTotalPages) { state.crewPage++; loadCrews(); } });

(async function initialize() {
    await refreshCsrf();
    await checkSession();
})();
